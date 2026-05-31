package com.ruoyi.chigua.service.impl;

import com.ruoyi.chigua.service.StatsService;
import com.ruoyi.chigua.domain.vo.TopVideoItem;
import com.ruoyi.chigua.domain.vo.CategoryClickItem;
import com.ruoyi.chigua.domain.DailyTopVideo;
import com.ruoyi.chigua.mapper.VideoDailyPlaysMapper;
import com.ruoyi.chigua.mapper.VideoDailyPlaysMapper.PlayDistribution;
import com.ruoyi.chigua.mapper.VideoDailyPlaysMapper.CategoryPlayStats;
import com.ruoyi.chigua.mapper.VideoDailyPlaysMapper.DailyPlayTrend;
import com.ruoyi.chigua.service.ICategoryService;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class StatsServiceImpl implements StatsService {

    private static final Logger logger = LoggerFactory.getLogger(StatsServiceImpl.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private ICategoryService categoryService;

    @Resource
    private VideoDailyPlaysMapper videoDailyPlaysMapper;

    private static final String KEY_NEW_USER_SET_PREFIX = "new_user_set:";
    private static final String KEY_ACTIVE_USER_SET_PREFIX = "active_user_set:";
    private static final String KEY_NEW_USERS_HLL_PREFIX = "new_users:";
    private static final String KEY_USER_FIRST_SEEN_HASH = "user:first_seen";
    private static final String KEY_USER_LAST_SEEN_HASH = "user:last_seen";

    @Override
    public Map<String, Object> getOverview(LocalDate date) {
        logger.info("📊 获取统计概览: date={}", date);
        Map<String, Object> map = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        if (date.equals(today)) {
            // 🔥 今天的数据：直接从Redis读取
            String day = date.toString().replaceAll("-", "");
            logger.info("📊 从Redis读取今日数据: day={}", day);
            
            Long dau = redisTemplate.opsForHyperLogLog().size("dau:" + day);
            Long uv = redisTemplate.opsForHyperLogLog().size("uv:" + day);
            Long pv = asLong(redisTemplate.opsForValue().get("pv:" + day));
            Long searches = asLong(redisTemplate.opsForValue().get("searches:" + day));
            Long categoryClicks = asLong(redisTemplate.opsForValue().get("category_clicks:" + day));
            Long plays = asLong(redisTemplate.opsForValue().get("plays:" + day));
            Long shares = asLong(redisTemplate.opsForValue().get("shares:" + day));
            
            logger.info("📊 Redis数据: dau={}, uv={}, pv={}, searches={}, categoryClicks={}, plays={}, shares={}", 
                dau, uv, pv, searches, categoryClicks, plays, shares);
            
            map.put("dau", dau == null ? 0 : dau);
            map.put("uv", uv == null ? 0 : uv);
            map.put("view_count", pv == null ? 0 : pv);  // 页面浏览量就是视频浏览量
            map.put("searches", searches == null ? 0 : searches);
            map.put("category_clicks", categoryClicks == null ? 0 : categoryClicks);
            map.put("play_starts", plays == null ? 0 : plays);
            map.put("shares", shares == null ? 0 : shares);
            
        } else {
            // 🔥 历史数据：从stats_daily_overview表读取
            try {
                Map<String, Object> dailyStats = jdbcTemplate.queryForMap(
                    "SELECT dau, uv, pv, searches, category_clicks, play_starts, shares " +
                    "FROM stats_daily_overview WHERE date = ?", date);
                
                map.put("dau", dailyStats.get("dau"));
                map.put("uv", dailyStats.get("uv"));
                map.put("view_count", dailyStats.get("pv"));  // 页面浏览量就是视频浏览量
                map.put("searches", dailyStats.get("searches"));
                map.put("category_clicks", dailyStats.get("category_clicks"));
                map.put("play_starts", dailyStats.get("play_starts"));
                map.put("shares", dailyStats.get("shares"));
                
            } catch (Exception e) {
                // 如果数据库中没有数据，返回0值
                logger.warn("未找到日期 {} 的统计数据，返回默认值", date);
                map.put("dau", 0);
                map.put("uv", 0);
                map.put("view_count", 0);
                map.put("searches", 0);
                map.put("category_clicks", 0);
                map.put("play_starts", 0);
                map.put("shares", 0);
            }
        }
        
        map.put("date", date.toString());
        map.put("sessions", 0); // 暂不统计会话数
        
        return map;
    }

    @Override
    public List<TopVideoItem> getTopVideos(LocalDate date, String range, int limit) {
        // 🔥 使用每日播放记录表查询Top视频
        return getTopVideosFromDailyPlays(date, range, limit);
    }
    
    /**
     * 从每日播放记录表获取Top视频
     */
    private List<TopVideoItem> getTopVideosFromDailyPlays(LocalDate date, String range, int limit) {
        List<TopVideoItem> result = new ArrayList<>();
        
        try {
            if ("today".equalsIgnoreCase(range) || range == null) {
                // 获取指定日期的Top视频
                List<DailyTopVideo> dailyTopVideos = videoDailyPlaysMapper.selectDailyTop100(date, limit);
                result = convertToTopVideoItems(dailyTopVideos);
            } else if ("yesterday".equalsIgnoreCase(range)) {
                // 获取昨天的Top视频
                LocalDate yesterday = date.minusDays(1);
                List<DailyTopVideo> dailyTopVideos = videoDailyPlaysMapper.selectDailyTop100(yesterday, limit);
                result = convertToTopVideoItems(dailyTopVideos);
            } else if ("7d".equalsIgnoreCase(range)) {
                // 获取7天内播放量增长最快的视频
                List<DailyTopVideo> risingVideos = videoDailyPlaysMapper.selectRisingVideos(date, limit);
                result = convertToTopVideoItems(risingVideos);
            } else {
                // 默认返回今天的数据
                List<DailyTopVideo> dailyTopVideos = videoDailyPlaysMapper.selectDailyTop100(date, limit);
                result = convertToTopVideoItems(dailyTopVideos);
            }
            
            logger.info("📊 获取Top{}视频成功: date={}, range={}, count={}", limit, date, range, result.size());
            
        } catch (Exception e) {
            logger.error("获取Top视频失败: date={}, range={}, limit={}", date, range, limit, e);
        }
        
        return result;
    }
    
    /**
     * 转换DailyTopVideo为TopVideoItem
     */
    private List<TopVideoItem> convertToTopVideoItems(List<DailyTopVideo> dailyTopVideos) {
        List<TopVideoItem> result = new ArrayList<>();
        
        for (int i = 0; i < dailyTopVideos.size(); i++) {
            DailyTopVideo daily = dailyTopVideos.get(i);
            TopVideoItem item = new TopVideoItem();
            item.setVideoId(daily.getVideoId());
            item.setTitle(daily.getTitle());
            item.setAuthor(daily.getAuthor());
            item.setCategoryId(daily.getCategoryId());
            item.setCategoryName(daily.getCategoryName());
            item.setPlays(daily.getDailyPlayCount().longValue());
            item.setRank(i + 1);
            if (daily.getGrowthCount() != null) {
                item.setGrowthCount(daily.getGrowthCount().longValue());
            }
            result.add(item);
        }
        
        return result;
    }
    
    private List<TopVideoItem> getTopVideosFromRedis(int limit) {
        List<TopVideoItem> result = new ArrayList<>();
        
        try {
            // 获取所有视频播放计数的Redis key
            Set<Object> playKeys = redisTemplate.keys("video:play:count:*");
            if (playKeys == null || playKeys.isEmpty()) {
                return result;
            }
            
            // 获取所有播放计数并排序
            Map<Long, Long> videoPlays = new HashMap<>();
            for (Object keyObj : playKeys) {
                String key = (String) keyObj;
                try {
                    Long videoId = Long.parseLong(key.replace("video:play:count:", ""));
                    Long plays = asLong(redisTemplate.opsForValue().get(key));
                    if (plays != null && plays > 0) {
                        videoPlays.put(videoId, plays);
                    }
                } catch (NumberFormatException e) {
                    logger.debug("解析视频ID失败: {}", key);
                }
            }
            
            // 按播放量排序并获取视频信息
            videoPlays.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .forEach(entry -> {
                        try {
                            // 查询视频基本信息
                            Map<String, Object> videoInfo = jdbcTemplate.queryForMap(
                                "SELECT id, title, category_id FROM videos WHERE id = ? AND status = 1",
                                entry.getKey());
                            
                            TopVideoItem item = new TopVideoItem();
                            item.setVideoId(entry.getKey());
                            item.setTitle((String) videoInfo.get("title"));
                            item.setCategoryId(((Number) videoInfo.get("category_id")).longValue());
                            item.setPlayCount(entry.getValue());
                            result.add(item);
                        } catch (Exception e) {
                            logger.debug("获取视频 {} 信息失败", entry.getKey());
                        }
                    });
                    
        } catch (Exception e) {
            logger.error("从Redis获取Top视频失败", e);
        }
        
        return result;
    }
    
    private List<TopVideoItem> getTopVideosFromDatabase(LocalDate date, String range, int limit) {
        // 计算日期范围
        LocalDate startDate = date;
        if ("yesterday".equalsIgnoreCase(range)) {
            startDate = date.minusDays(1);
        } else if ("7d".equalsIgnoreCase(range)) {
            startDate = date.minusDays(6);
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        String sql = "select v.id as video_id, v.title, v.category_id, sum(s.play_count) as plays " +
                "from video_statistics s join videos v on v.id = s.video_id " +
                "where s.updated_at >= ? and s.updated_at < ? " +
                "group by v.id, v.title, v.category_id order by plays desc limit ?";

        List<TopVideoItem> list = jdbcTemplate.query(sql,
                new Object[]{Date.from(start.atZone(ZoneId.systemDefault()).toInstant()),
                        Date.from(end.atZone(ZoneId.systemDefault()).toInstant()), limit},
                (rs, i) -> {
                    TopVideoItem item = new TopVideoItem();
                    item.setVideoId(rs.getLong("video_id"));
                    item.setTitle(rs.getString("title"));
                    item.setCategoryId(rs.getLong("category_id"));
                    item.setPlayCount(rs.getLong("plays"));
                    return item;
                });
        return list;
    }

    @Override
    public List<CategoryClickItem> getCategoryRanking(String period, int limit) {
        // 支持 day(当天), week(近7天), month(近30天)
        LocalDate today = LocalDate.now();
        int span = 1;
        if ("week".equalsIgnoreCase(period)) span = 7;
        else if ("month".equalsIgnoreCase(period)) span = 30;

        Map<Long, Long> catToClicks = new HashMap<>();
        List<com.ruoyi.chigua.domain.Category> categories = categoryService.selectCategoryListForFrontend();

        // 构建分类 id→name 映射
        Map<Long, String> idToName = new HashMap<>();
        for (com.ruoyi.chigua.domain.Category c : categories) {
            idToName.put(c.getId(), c.getName());
        }

        if (span == 1 && "day".equalsIgnoreCase(period)) {
            // ── 单日查询：先读 Redis，Redis 无数据时降级读 MySQL ──
            String todayKey = today.toString().replaceAll("-", "");
            boolean redisHasData = false;

            for (com.ruoyi.chigua.domain.Category c : categories) {
                Long clicks = asLong(redisTemplate.opsForValue().get("cat:click:" + c.getId() + ":" + todayKey));
                if (clicks != null && clicks > 0) {
                    catToClicks.put(c.getId(), clicks);
                    redisHasData = true;
                }
            }

            if (!redisHasData) {
                // Redis 无数据（可能已清理），降级读 MySQL 当日行
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT category_id, clicks FROM stats_daily_category_clicks WHERE `date` = ?",
                        java.sql.Date.valueOf(today));
                    for (Map<String, Object> row : rows) {
                        Long catId = asLong(row.get("category_id"));
                        Long clicks = asLong(row.get("clicks"));
                        if (catId != null && clicks != null && clicks > 0) {
                            catToClicks.put(catId, clicks);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("分类点击 MySQL 降级查询失败: {}", e.getMessage());
                }
            }
        } else {
            // ── 多日查询：一次 SQL 拉历史汇总，再叠加今日 Redis ──
            // 历史范围：startDate（不含今日）到 yesterday
            LocalDate yesterday = today.minusDays(1);
            LocalDate startDate = today.minusDays(span - 1); // week→6天前, month→29天前

            if (!startDate.isAfter(yesterday)) {
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT category_id, SUM(clicks) AS total FROM stats_daily_category_clicks " +
                        "WHERE `date` BETWEEN ? AND ? GROUP BY category_id",
                        java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(yesterday));
                    for (Map<String, Object> row : rows) {
                        Long catId = asLong(row.get("category_id"));
                        Long total = asLong(row.get("total"));
                        if (catId != null && total != null && total > 0) {
                            catToClicks.put(catId, total);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("分类点击历史聚合查询失败: {}", e.getMessage());
                }
            }

            // 叠加今日 Redis（当日尚未聚合入库）
            String todayKey = today.toString().replaceAll("-", "");
            for (com.ruoyi.chigua.domain.Category c : categories) {
                Long clicks = asLong(redisTemplate.opsForValue().get("cat:click:" + c.getId() + ":" + todayKey));
                if (clicks != null && clicks > 0) {
                    catToClicks.merge(c.getId(), clicks, Long::sum);
                }
            }
        }

        // 排序取前N
        List<Map.Entry<Long, Long>> list = new ArrayList<>(catToClicks.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        if (list.size() > limit) list = list.subList(0, limit);

        // 查询对应时段各分类的播放数（video_daily_plays JOIN videos）
        Map<Long, Long> catToPlays = new HashMap<>();
        try {
            LocalDate playEnd = today;
            LocalDate playStart = today.minusDays(span - 1);
            List<Map<String, Object>> playRows = jdbcTemplate.queryForList(
                "SELECT v.category_id, SUM(vdp.play_count) AS plays " +
                "FROM video_daily_plays vdp " +
                "INNER JOIN videos v ON vdp.video_id = v.id " +
                "WHERE vdp.play_date BETWEEN ? AND ? " +
                "GROUP BY v.category_id",
                java.sql.Date.valueOf(playStart), java.sql.Date.valueOf(playEnd));
            for (Map<String, Object> row : playRows) {
                Long catId = asLong(row.get("category_id"));
                Long plays = asLong(row.get("plays"));
                if (catId != null && plays != null) {
                    catToPlays.put(catId, plays);
                }
            }
        } catch (Exception e) {
            logger.warn("分类播放数查询失败: {}", e.getMessage());
        }

        List<CategoryClickItem> result = new ArrayList<>();
        for (Map.Entry<Long, Long> e : list) {
            CategoryClickItem item = new CategoryClickItem();
            item.setCategoryId(e.getKey());
            item.setCategoryName(idToName.getOrDefault(e.getKey(), String.valueOf(e.getKey())));
            item.setClicks(e.getValue());
            item.setPlays(catToPlays.getOrDefault(e.getKey(), 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getTimeSeries(String metric, String period) {
        Map<String, Object> res = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        if ("month".equalsIgnoreCase(period)) {
            // 最近12个月，含本月
            YearMonth ym = YearMonth.now();
            for (int i = 11; i >= 0; i--) {
                YearMonth cur = ym.minusMonths(i);
                String label = cur.toString(); // YYYY-MM
                labels.add(label);
                values.add(fetchMonthly(metric, cur));
            }
        } else {
            // 默认 day：最近7天，含今天
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                String keyDay = d.toString().replaceAll("-", "");
                labels.add(d.toString());
                values.add(fetchDaily(metric, keyDay));
            }
        }

        res.put("labels", labels);
        res.put("values", values);
        return res;
    }

    private Long fetchDaily(String metric, String day) {
        // 🔥 混合查询逻辑：今天用Redis，历史用数据库
        LocalDate queryDate = LocalDate.parse(day.substring(0, 4) + "-" + day.substring(4, 6) + "-" + day.substring(6, 8));
        LocalDate today = LocalDate.now();
        
        if (queryDate.equals(today)) {
            // 今天：从Redis读取
            return fetchDailyFromRedis(metric, day);
        } else {
            // 历史：从数据库读取
            return fetchDailyFromDatabase(metric, queryDate);
        }
    }
    
    private Long fetchDailyFromRedis(String metric, String day) {
        switch (metric.toLowerCase()) {
            case "dau":
                {
                    Long size = redisTemplate.opsForHyperLogLog().size("dau:" + day);
                    return size == null ? 0L : size;
                }
            case "uv":
                {
                    Long size = redisTemplate.opsForHyperLogLog().size("uv:" + day);
                    return size == null ? 0L : size;
                }
            case "pv":
                {
                    Long v = asLong(redisTemplate.opsForValue().get("pv:" + day));
                    return v == null ? 0L : v;
                }
            case "searches":
                {
                    Long v = asLong(redisTemplate.opsForValue().get("searches:" + day));
                    return v == null ? 0L : v;
                }
            case "category_clicks":
                {
                    Long v = asLong(redisTemplate.opsForValue().get("category_clicks:" + day));
                    return v == null ? 0L : v;
                }
            case "play_starts":
                {
                    Long v = asLong(redisTemplate.opsForValue().get("plays:" + day));
                    return v == null ? 0L : v;
                }
            case "new_users":
                {
                    Long size = redisTemplate.opsForHyperLogLog().size("new_users:" + day);
                    return size == null ? 0L : size;
                }
            default:
                return 0L;
        }
    }
    
    private Long fetchDailyFromDatabase(String metric, LocalDate date) {
        try {
            String sql = "SELECT " + getDatabaseColumnName(metric) + " FROM stats_daily_overview WHERE date = ?";
            Long result = jdbcTemplate.queryForObject(sql, new Object[]{date}, Long.class);
            return result == null ? 0L : result;
        } catch (Exception e) {
            logger.debug("未找到日期 {} 的 {} 统计数据", date, metric);
            return 0L;
        }
    }
    
    private String getDatabaseColumnName(String metric) {
        switch (metric.toLowerCase()) {
            case "dau": return "dau";
            case "uv": return "uv";
            case "pv": return "pv";
            case "searches": return "searches";
            case "category_clicks": return "category_clicks";
            case "play_starts": return "play_starts";
            case "new_users": return "new_users";
            default: return "pv"; // 默认返回pv
        }
    }

    private Long fetchMonthly(String metric, YearMonth ym) {
        // 如果存在日汇总表，则优先走数据库聚合
        try {
            if ("dau".equalsIgnoreCase(metric) || "uv".equalsIgnoreCase(metric) ||
                "pv".equalsIgnoreCase(metric) || "searches".equalsIgnoreCase(metric) ||
                "category_clicks".equalsIgnoreCase(metric) || "play_starts".equalsIgnoreCase(metric) ||
                "shares".equalsIgnoreCase(metric) || "new_users".equalsIgnoreCase(metric)) {
                String col = metric.toLowerCase();
                Long sumDb = jdbcTemplate.queryForObject(
                        "select coalesce(sum(" + col + "),0) from stats_daily_overview where date >= ? and date < ?",
                        new Object[]{java.sql.Date.valueOf(ym.atDay(1)), java.sql.Date.valueOf(ym.plusMonths(1).atDay(1))}, Long.class);
                if (sumDb != null && sumDb > 0) return sumDb;
            }
        } catch (Exception ignored) {}
        // 回退 Redis：逐日累加，与数据库 SUM 路径语义一致
        long sum = 0L;
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            sum += fetchDaily(metric, d.toString().replaceAll("-", ""));
        }
        return sum;
    }
    private Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (Exception ignored) {}
        }
        return null;
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> result = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    byte[] rawKey = cursor.next();
                    String key = redisTemplate.getStringSerializer().deserialize(rawKey);
                    if (key != null) {
                        keys.add(key);
                    }
                }
            }
            return keys;
        });
        return result == null ? Collections.emptySet() : result;
    }

    private long getSetSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    private void processSetMembers(String key, Consumer<String> consumer) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return;
        }
        ScanOptions options = ScanOptions.scanOptions().count(1000).build();
        try (Cursor<Object> cursor = redisTemplate.opsForSet().scan(key, options)) {
            while (cursor.hasNext()) {
                Object raw = cursor.next();
                if (raw != null) {
                    consumer.accept(String.valueOf(raw));
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ 遍历集合 {} 失败: {}", key, e.getMessage());
        }
    }

    private void updateActiveUsersLastSeen(LocalDate date, String activeUserSetKey) {
        processSetMembers(activeUserSetKey, userId -> {
            jdbcTemplate.update(
                "INSERT INTO user_first_active(user_id, first_date, last_seen_date) VALUES(?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_seen_date = VALUES(last_seen_date), first_date = LEAST(first_date, VALUES(first_date))",
                userId,
                java.sql.Date.valueOf(date),
                java.sql.Date.valueOf(date)
            );
        });
    }

    private void updateRetentionForOffset(LocalDate currentDate, String activeUserSetKey, int offsetDays) {
        if (offsetDays <= 0) {
            return;
        }
        LocalDate cohortDate = currentDate.minusDays(offsetDays);
        if (cohortDate.isAfter(currentDate)) {
            return;
        }
        String cohortKey = KEY_NEW_USER_SET_PREFIX + cohortDate.toString().replaceAll("-", "");
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(cohortKey))) {
            return;
        }

        long retained = countRetainedMembers(cohortKey, activeUserSetKey);

        Long newUsers = null;
        try {
            newUsers = jdbcTemplate.queryForObject(
                "SELECT new_users FROM stats_daily_retention WHERE date = ?",
                new Object[]{java.sql.Date.valueOf(cohortDate)},
                Long.class
            );
        } catch (Exception ignored) {}

        double base = newUsers != null ? newUsers : getSetSize(cohortKey);
        double rate = base > 0 ? retained / base : 0.0;

        String retainedColumn;
        String rateColumn;
        if (offsetDays == 1) {
            retainedColumn = "d1_retained";
            rateColumn = "d1_rate";
        } else if (offsetDays == 7) {
            retainedColumn = "d7_retained";
            rateColumn = "d7_rate";
        } else if (offsetDays == 30) {
            retainedColumn = "d30_retained";
            rateColumn = "d30_rate";
        } else {
            return;
        }

        jdbcTemplate.update(
            "INSERT INTO stats_daily_retention(`date`, new_users, " + retainedColumn + ", " + rateColumn + ") VALUES(?, 0, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " + retainedColumn + " = ?, " + rateColumn + " = ?",
            java.sql.Date.valueOf(cohortDate),
            retained,
            rate,
            retained,
            rate
        );

        if (offsetDays == 30) {
            redisTemplate.delete(cohortKey);
        }
    }

    private long countRetainedMembers(String cohortKey, String activeUserSetKey) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(cohortKey)) ||
            !Boolean.TRUE.equals(redisTemplate.hasKey(activeUserSetKey))) {
            return 0L;
        }
        final long[] counter = {0L};
        processSetMembers(cohortKey, userId -> {
            if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(activeUserSetKey, userId))) {
                counter[0]++;
            }
        });
        return counter[0];
    }

    @Override
    public List<Map<String, Object>> getRetentionSeries(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(29) : startDate;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        String sql = "SELECT `date`, new_users, d1_retained, d7_retained, d30_retained, d1_rate, d7_rate, d30_rate " +
            "FROM stats_daily_retention WHERE date BETWEEN ? AND ? ORDER BY date";

        java.sql.Date startSql = java.sql.Date.valueOf(start);
        java.sql.Date endSql = java.sql.Date.valueOf(end);
        return jdbcTemplate.query(sql, new Object[]{startSql, endSql}, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            LocalDate d = rs.getDate("date").toLocalDate();
            map.put("date", d.toString());
            map.put("newUsers", rs.getLong("new_users"));
            map.put("d1Retained", rs.getLong("d1_retained"));
            map.put("d7Retained", rs.getLong("d7_retained"));
            map.put("d30Retained", rs.getLong("d30_retained"));
            map.put("d1Rate", rs.getBigDecimal("d1_rate") != null ? rs.getBigDecimal("d1_rate").doubleValue() : 0.0);
            map.put("d7Rate", rs.getBigDecimal("d7_rate") != null ? rs.getBigDecimal("d7_rate").doubleValue() : 0.0);
            map.put("d30Rate", rs.getBigDecimal("d30_rate") != null ? rs.getBigDecimal("d30_rate").doubleValue() : 0.0);
            return map;
        });
    }

    @Override
    public Map<String, Object> getRetentionSummary(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(29) : startDate;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        String sql = "SELECT COALESCE(SUM(new_users),0) AS new_users, " +
            "COALESCE(SUM(d1_retained),0) AS d1_retained, " +
            "COALESCE(SUM(d7_retained),0) AS d7_retained, " +
            "COALESCE(SUM(d30_retained),0) AS d30_retained " +
            "FROM stats_daily_retention WHERE date BETWEEN ? AND ?";

        LocalDate finalStart = start;
        LocalDate finalEnd = end;
        java.sql.Date startSql = java.sql.Date.valueOf(finalStart);
        java.sql.Date endSql = java.sql.Date.valueOf(finalEnd);

        Map<String, Object> summary = jdbcTemplate.query(sql, new Object[]{startSql, endSql}, rs -> {
            Map<String, Object> map = new HashMap<>();
            if (rs.next()) {
                long totalNew = rs.getLong("new_users");
                long d1Retained = rs.getLong("d1_retained");
                long d7Retained = rs.getLong("d7_retained");
                long d30Retained = rs.getLong("d30_retained");

                map.put("totalNewUsers", totalNew);
                map.put("totalD1Retained", d1Retained);
                map.put("totalD7Retained", d7Retained);
                map.put("totalD30Retained", d30Retained);
                map.put("d1Rate", totalNew > 0 ? (double) d1Retained / totalNew : 0.0);
                map.put("d7Rate", totalNew > 0 ? (double) d7Retained / totalNew : 0.0);
                map.put("d30Rate", totalNew > 0 ? (double) d30Retained / totalNew : 0.0);
            } else {
                map.put("totalNewUsers", 0L);
                map.put("totalD1Retained", 0L);
                map.put("totalD7Retained", 0L);
                map.put("totalD30Retained", 0L);
                map.put("d1Rate", 0.0);
                map.put("d7Rate", 0.0);
                map.put("d30Rate", 0.0);
            }
            map.put("startDate", finalStart.toString());
            map.put("endDate", finalEnd.toString());
            return map;
        });

        return summary != null ? summary : new HashMap<>();
    }

    @Override
    public void aggregateDailyToMySQL(LocalDate date) {
        String day = date.toString().replaceAll("-", "");
        logger.info("🔄 开始聚合日期 {} 的统计数据到数据库", date);
        
        String lockKey = "stats:lock:aggregate:" + day;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.MINUTES);
        if (locked == null || !locked) {
            logger.warn("⚠️ 聚合任务已在执行中，跳过日期 {}", date);
            return;
        }

        try {
            // 🔥 读取Redis当日值
            Long dau = redisTemplate.opsForHyperLogLog().size("dau:" + day); if (dau == null) dau = 0L;
            Long uv = redisTemplate.opsForHyperLogLog().size("uv:" + day); if (uv == null) uv = 0L;
            Long pv = asLong(redisTemplate.opsForValue().get("pv:" + day)); if (pv == null) pv = 0L;
            Long searches = asLong(redisTemplate.opsForValue().get("searches:" + day)); if (searches == null) searches = 0L;
            Long catClicks = asLong(redisTemplate.opsForValue().get("category_clicks:" + day)); if (catClicks == null) catClicks = 0L;
            Long plays = asLong(redisTemplate.opsForValue().get("plays:" + day)); if (plays == null) plays = 0L;
            Long shares = asLong(redisTemplate.opsForValue().get("shares:" + day)); if (shares == null) shares = 0L;

            String newUserSetKey = KEY_NEW_USER_SET_PREFIX + day;
            String activeUserSetKey = KEY_ACTIVE_USER_SET_PREFIX + day;
            long newUsersCount = getSetSize(newUserSetKey);

            // 🔥 概览表 upsert（含新用户数，用于历史趋势图）
            // 🛡️ 安全策略：只在新值 > 已有值时才更新，避免0值覆盖正确数据
            jdbcTemplate.update(
                "INSERT INTO stats_daily_overview(`date`, dau, uv, pv, searches, category_clicks, play_starts, shares, new_users) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "dau=GREATEST(dau, VALUES(dau)), " +
                "uv=GREATEST(uv, VALUES(uv)), " +
                "pv=GREATEST(pv, VALUES(pv)), " +
                "searches=GREATEST(searches, VALUES(searches)), " +
                "category_clicks=GREATEST(category_clicks, VALUES(category_clicks)), " +
                "play_starts=GREATEST(play_starts, VALUES(play_starts)), " +
                "shares=GREATEST(shares, VALUES(shares)), " +
                "new_users=GREATEST(new_users, VALUES(new_users))",
                java.sql.Date.valueOf(date), dau, uv, pv, searches, catClicks, plays, shares, newUsersCount
            );
            logger.info("✅ 概览数据聚合完成: DAU={}, UV={}, PV={}, 搜索={}, 分类点击={}, 播放={}, 分享={}, 新用户={}",
                       dau, uv, pv, searches, catClicks, plays, shares, newUsersCount);

            jdbcTemplate.update(
                "INSERT INTO stats_daily_retention(`date`, new_users) VALUES(?, ?) " +
                    "ON DUPLICATE KEY UPDATE new_users = VALUES(new_users)",
                java.sql.Date.valueOf(date), newUsersCount
            );

            updateActiveUsersLastSeen(date, activeUserSetKey);

            updateRetentionForOffset(date, activeUserSetKey, 1);
            updateRetentionForOffset(date, activeUserSetKey, 7);
            updateRetentionForOffset(date, activeUserSetKey, 30);

            // 🔥 分类点击明细：遍历分类表，读取 cat:click:{id}:{day}
            List<com.ruoyi.chigua.domain.Category> categories = categoryService.selectCategoryListForFrontend();
            int categoryCount = 0;
            for (com.ruoyi.chigua.domain.Category c : categories) {
                Long cnt = asLong(redisTemplate.opsForValue().get("cat:click:" + c.getId() + ":" + day));
                if (cnt == null || cnt <= 0) {
                    continue;
                }

                jdbcTemplate.update(
                    "INSERT INTO stats_daily_category_clicks(`date`, category_id, clicks) VALUES(?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE clicks=GREATEST(clicks, VALUES(clicks))",
                    java.sql.Date.valueOf(date), c.getId(), cnt
                );
                categoryCount++;
            }
            logger.info("✅ 分类点击数据聚合完成: 处理了{}个分类", categoryCount);

            // 🔥 搜索关键词明细：读取 search:kw:{day} ZSet Top 200，持久化到 stats_search_keywords
            try {
                Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> kwEntries =
                    redisTemplate.opsForZSet().reverseRangeWithScores("search:kw:" + day, 0, 199);
                int kwCount = 0;
                if (kwEntries != null) {
                    for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> e : kwEntries) {
                        String kw = String.valueOf(e.getValue());
                        Double score = e.getScore();
                        if (kw == null || kw.isEmpty() || score == null) continue;
                        long cnt = score.longValue();
                        jdbcTemplate.update(
                            "INSERT INTO stats_search_keywords(stat_date, keyword, search_count) VALUES(?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE search_count = GREATEST(search_count, VALUES(search_count))",
                            java.sql.Date.valueOf(date), kw, cnt);
                        kwCount++;
                    }
                }
                logger.info("✅ 搜索关键词数据聚合完成: 持久化了{}个关键词", kwCount);
            } catch (Exception e) {
                logger.error("❌ 搜索关键词数据聚合失败: {}", e.getMessage(), e);
            }

            // 🔥 只清理历史数据的Redis，不清理当天数据
            LocalDate today = LocalDate.now();
            if (!date.equals(today)) {
                logger.info("🧹 清理历史日期 {} 的Redis数据", date);
                cleanupRedisDataForDate(date);
            } else {
                logger.info("⚠️ 跳过清理当天 {} 的Redis数据，保持实时统计", date);
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * 清理指定日期的Redis统计数据
     */
    private void cleanupRedisDataForDate(LocalDate date) {
        String day = date.toString().replaceAll("-", "");
        logger.info("🧹 开始清理日期 {} 的Redis统计数据", date);
        
        // 清理基础统计数据
        String[] basicKeys = {
            "dau:" + day,
            "uv:" + day,
            "pv:" + day,
            "searches:" + day,
            "category_clicks:" + day,
            "plays:" + day,
            "shares:" + day
        };
        
        int cleanedCount = 0;
        for (String key : basicKeys) {
            if (cleanupRedisKey(key)) {
                cleanedCount++;
            }
        }

        if (cleanupRedisKey(KEY_ACTIVE_USER_SET_PREFIX + day)) {
            cleanedCount++;
        }
        // 保留 new_user_set:{day} 以及 new_users:{day}，以便后续 D1/D7/D30 留存计算使用；
        // TTL 会在 WebAnalyticsController 中自动设置，无需主动清理。

        // 清理分类点击数据
        Set<String> categoryKeys = scanKeys("cat:click:*:" + day);
        if (!categoryKeys.isEmpty()) {
            redisTemplate.delete(categoryKeys);
            cleanedCount += categoryKeys.size();
            logger.info("🗑️ 清理了{}个分类点击Redis key", categoryKeys.size());
        }
        
        logger.info("✅ 日期 {} 的Redis数据清理完成，共清理 {} 个key", date, cleanedCount);
    }

    @Override
    public boolean cleanupRedisKey(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            return deleted != null && deleted;
        } catch (Exception e) {
            logger.error("清理Redis key失败: {}, 错误: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public int cleanupRedisStatsForDate(LocalDate date) {
        String day = date.toString().replaceAll("-", ""); // "20250825"
        
        logger.info("🧹 开始清理日期 {} 的Redis统计数据", date);
        
        // 需要清理的Redis key列表
        String[] keysToClean = {
            "dau:" + day,
            "uv:" + day,
            "pv:" + day,
            "searches:" + day,
            "category_clicks:" + day,
            "plays:" + day,
            "shares:" + day
        };
        
        int cleanedCount = 0;
        for (String key : keysToClean) {
            try {
                if (cleanupRedisKey(key)) {
                    cleanedCount++;
                    logger.debug("🗑️ 已清理Redis key: {}", key);
                }
            } catch (Exception e) {
                logger.warn("⚠️ 清理Redis key失败: {}, 错误: {}", key, e.getMessage());
            }
        }

        if (cleanupRedisKey(KEY_ACTIVE_USER_SET_PREFIX + day)) {
            cleanedCount++;
        }
        // new_user_set:{day} 和 new_users:{day} 需要保留多天以支撑留存计算，交由 TTL 过期。

        // 清理分类点击的详细数据 cat:click:{categoryId}:{day}
        try {
            Set<String> catKeys = scanKeys("cat:click:*:" + day);
            if (!catKeys.isEmpty()) {
                redisTemplate.delete(catKeys);
                cleanedCount += catKeys.size();
                logger.debug("🗑️ 已清理 {} 个分类点击Redis key", catKeys.size());
            }
        } catch (Exception e) {
            logger.warn("⚠️ 清理分类点击Redis数据失败: {}", e.getMessage());
        }
        
        // 清理路径PV数据 pv:path:{path}:{day}
        try {
            // 使用通配符删除所有路径PV数据
            String pathPattern = "pv:path:*:" + day;
            Set<String> pathKeys = scanKeys(pathPattern);
            if (!pathKeys.isEmpty()) {
                redisTemplate.delete(pathKeys);
                cleanedCount += pathKeys.size();
                logger.debug("🗑️ 已清理 {} 个路径PV Redis key", pathKeys.size());
            }
        } catch (Exception e) {
            logger.warn("⚠️ 清理路径PV Redis数据失败: {}", e.getMessage());
        }
        
        // 清理搜索关键词热度数据 search:kw:{day}
        try {
            String searchKwKey = "search:kw:" + day;
            if (cleanupRedisKey(searchKwKey)) {
                cleanedCount++;
                logger.debug("🗑️ 已清理搜索关键词Redis key: {}", searchKwKey);
            }
        } catch (Exception e) {
            logger.warn("⚠️ 清理搜索关键词Redis数据失败: {}", e.getMessage());
        }
        
        logger.info("✅ 日期 {} 的Redis数据清理完成，共清理 {} 个key", date, cleanedCount);
        return cleanedCount;
    }

    // ========== 新增Top100相关统计方法实现 ==========
    
    @Override
    public Map<String, Object> getTop100Dashboard(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> dashboard = new HashMap<>();
        
        try {
            // 1. 基础统计（指定日期范围数据）
            Map<String, Object> basicStats = getActiveVideoStatsDateRange(startDate, endDate);
            dashboard.put("basicStats", basicStats);
            
            // 2. Top100排行榜
            Map<String, Object> rankings = new HashMap<>();
            rankings.put("todayTop10", getTopVideos(endDate, "today", 30));
            rankings.put("risingTop10", getTopVideosDateRange(startDate, endDate, 30));
            dashboard.put("rankings", rankings);
            
            // 3. 分类统计（指定日期范围数据）
            List<Map<String, Object>> categoryStats = getCategoryPlayStatsDateRange(startDate, endDate);
            dashboard.put("categoryStats", categoryStats);
            
            // 4. 播放量趋势（指定日期范围数据）
            Map<String, Object> trends = getPlayTrendDateRange(startDate, endDate);
            dashboard.put("trends", trends);
            
            logger.info("📊 获取Top100综合面板数据成功: startDate={}, endDate={}", startDate, endDate);
            
        } catch (Exception e) {
            logger.error("获取Top100综合面板数据失败: startDate={}, endDate={}", startDate, endDate, e);
        }
        
        return dashboard;
    }
    
    /**
     * 获取指定日期范围内的活跃视频统计
     */
    private Map<String, Object> getActiveVideoStatsDateRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 查询指定日期范围内的统计数据
            Integer activeVideoCount = videoDailyPlaysMapper.countActiveVideos7Days(startDate, endDate);
            Long totalPlays = videoDailyPlaysMapper.sumDailyPlays7Days(startDate, endDate);
            
            stats.put("activeVideoCount", activeVideoCount != null ? activeVideoCount : 0);
            stats.put("totalPlays", totalPlays != null ? totalPlays : 0L);
            stats.put("avgPlays", activeVideoCount != null && activeVideoCount > 0 ? 
                (totalPlays != null ? totalPlays / activeVideoCount : 0) : 0);
            
        } catch (Exception e) {
            logger.error("获取日期范围活跃视频统计失败: startDate={}, endDate={}", startDate, endDate, e);
            stats.put("activeVideoCount", 0);
            stats.put("totalPlays", 0L);
            stats.put("avgPlays", 0);
        }
        
        return stats;
    }
    
    /**
     * 获取指定日期范围内的Top视频
     */
    private List<TopVideoItem> getTopVideosDateRange(LocalDate startDate, LocalDate endDate, int limit) {
        try {
            // 使用快速上升视频查询（基于日期范围的播放量增长）
            List<DailyTopVideo> risingVideos = videoDailyPlaysMapper.selectRisingVideos(endDate, limit);
            return convertToTopVideoItems(risingVideos);
        } catch (Exception e) {
            logger.error("获取日期范围Top视频失败: startDate={}, endDate={}, limit={}", startDate, endDate, limit, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取指定日期范围内的分类播放统计
     */
    private List<Map<String, Object>> getCategoryPlayStatsDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            List<VideoDailyPlaysMapper.CategoryPlayStats> categoryStats = 
                videoDailyPlaysMapper.selectCategoryPlayStats7Days(startDate, endDate);
            
            // 转换为Map格式
            List<Map<String, Object>> result = new ArrayList<>();
            for (VideoDailyPlaysMapper.CategoryPlayStats stat : categoryStats) {
                Map<String, Object> map = new HashMap<>();
                map.put("categoryId", stat.getCategoryId());
                map.put("categoryName", stat.getCategoryName());
                map.put("videoCount", stat.getVideoCount());
                map.put("totalPlays", stat.getTotalPlays());
                map.put("avgPlays", stat.getAvgPlays());
                result.add(map);
            }
            return result;
        } catch (Exception e) {
            logger.error("获取日期范围分类播放统计失败: startDate={}, endDate={}", startDate, endDate, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取指定日期范围内的播放量趋势
     */
    private Map<String, Object> getPlayTrendDateRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> trends = new HashMap<>();
        
        try {
            List<VideoDailyPlaysMapper.DailyPlayTrend> trendData = 
                videoDailyPlaysMapper.selectDailyPlayTrend(startDate, endDate);
            trends.put("details", trendData);
        } catch (Exception e) {
            logger.error("获取日期范围播放量趋势失败: startDate={}, endDate={}", startDate, endDate, e);
            trends.put("details", new ArrayList<>());
        }
        
        return trends;
    }
    
    @Override
    public List<Map<String, Object>> getDailyPlayTrend(LocalDate startDate, LocalDate endDate) {
        try {
            List<VideoDailyPlaysMapper.DailyPlayTrend> trendData = 
                videoDailyPlaysMapper.selectDailyPlayTrend(startDate, endDate);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (VideoDailyPlaysMapper.DailyPlayTrend trend : trendData) {
                Map<String, Object> map = new HashMap<>();
                map.put("playDate", trend.getPlayDate().toString());
                map.put("activeVideoCount", trend.getActiveVideoCount());
                map.put("totalPlays", trend.getTotalPlays());
                map.put("avgPlays", trend.getAvgPlays());
                result.add(map);
            }

            if (result.isEmpty()) {
                logger.info("📊 没有播放趋势数据: startDate={}, endDate={}", startDate, endDate);
            }

            logger.info("📊 获取每日播放趋势成功: startDate={}, endDate={}, count={}", 
                startDate, endDate, result.size());
            return result;
        } catch (Exception e) {
            logger.error("获取每日播放趋势失败: startDate={}, endDate={}", startDate, endDate, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Object> getActiveVideoStats7Days(LocalDate date) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            LocalDate startDate = date.minusDays(6); // 7日内数据
            LocalDate endDate = date;
            
            // 7日内活跃视频数量
            int activeVideoCount = videoDailyPlaysMapper.countActiveVideos7Days(startDate, endDate);
            stats.put("activeVideoCount", activeVideoCount);
            
            // 7日内总播放量
            Long totalPlays = videoDailyPlaysMapper.sumDailyPlays7Days(startDate, endDate);
            stats.put("totalPlays", totalPlays != null ? totalPlays : 0L);
            
            // 平均播放量
            double avgPlays = activeVideoCount > 0 ? (totalPlays != null ? totalPlays : 0L) / (double) activeVideoCount : 0.0;
            stats.put("avgPlays", Math.round(avgPlays * 100.0) / 100.0);
            
            // 对比前7日数据
            LocalDate prevStartDate = date.minusDays(13);
            LocalDate prevEndDate = date.minusDays(7);
            int prevActiveCount = videoDailyPlaysMapper.countActiveVideos7Days(prevStartDate, prevEndDate);
            Long prevTotalPlays = videoDailyPlaysMapper.sumDailyPlays7Days(prevStartDate, prevEndDate);
            
            stats.put("activeVideoCountChange", activeVideoCount - prevActiveCount);
            stats.put("totalPlaysChange", (totalPlays != null ? totalPlays : 0L) - (prevTotalPlays != null ? prevTotalPlays : 0L));
            
            // 每日平均播放量
            double dailyAvgPlays = (totalPlays != null ? totalPlays : 0L) / 7.0;
            stats.put("dailyAvgPlays", Math.round(dailyAvgPlays * 100.0) / 100.0);
            
            logger.debug("📊 获取7日活跃视频统计成功: date={}, activeCount={}, totalPlays={}", 
                        date, activeVideoCount, totalPlays);
            
        } catch (Exception e) {
            logger.error("获取7日活跃视频统计失败: date={}", date, e);
        }
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getPlayCountDistribution7Days(LocalDate date) {
        Map<String, Object> distribution = new HashMap<>();
        
        try {
            LocalDate startDate = date.minusDays(6); // 7日内数据
            LocalDate endDate = date;
            
            List<PlayDistribution> playDistributions = videoDailyPlaysMapper.selectPlayDistribution7Days(startDate, endDate);
            
            List<String> ranges = new ArrayList<>();
            List<Integer> videoCounts = new ArrayList<>();
            List<Long> totalPlays = new ArrayList<>();
            
            for (PlayDistribution pd : playDistributions) {
                ranges.add(pd.getPlayRange());
                videoCounts.add(pd.getVideoCount());
                totalPlays.add(pd.getTotalPlays());
            }
            
            distribution.put("ranges", ranges);
            distribution.put("videoCounts", videoCounts);
            distribution.put("totalPlays", totalPlays);
            distribution.put("details", playDistributions);
            
            logger.debug("📊 获取7日播放量分布统计成功: date={}, 分布区间数={}", date, playDistributions.size());
            
        } catch (Exception e) {
            logger.error("获取7日播放量分布统计失败: date={}", date, e);
        }
        
        return distribution;
    }
    
    @Override
    public Map<String, Object> getPlayTrend7Days(LocalDate date) {
        Map<String, Object> trend = new HashMap<>();
        
        try {
            LocalDate startDate = date.minusDays(6); // 固定7天
            LocalDate endDate = date;
            
            List<DailyPlayTrend> dailyTrends = videoDailyPlaysMapper.selectDailyPlayTrend(startDate, endDate);
            
            List<String> dates = new ArrayList<>();
            List<Integer> activeVideoCounts = new ArrayList<>();
            List<Long> totalPlays = new ArrayList<>();
            List<Double> avgPlays = new ArrayList<>();
            
            for (DailyPlayTrend trend1 : dailyTrends) {
                dates.add(trend1.getPlayDate().toString());
                activeVideoCounts.add(trend1.getActiveVideoCount());
                totalPlays.add(trend1.getTotalPlays());
                avgPlays.add(trend1.getAvgPlays());
            }
            
            trend.put("dates", dates);
            trend.put("activeVideoCounts", activeVideoCounts);
            trend.put("totalPlays", totalPlays);
            trend.put("avgPlays", avgPlays);
            trend.put("details", dailyTrends);
            
            logger.debug("📊 获取7日播放趋势数据成功: date={}, 数据点数={}", date, dailyTrends.size());
            
        } catch (Exception e) {
            logger.error("获取7日播放趋势数据失败: date={}", date, e);
        }
        
        return trend;
    }
    
    @Override
    public List<TopVideoItem> getNewVideoRanking(LocalDate date, int limit) {
        List<TopVideoItem> result = new ArrayList<>();
        
        try {
            List<DailyTopVideo> newVideos = videoDailyPlaysMapper.selectNewVideoRanking(date, limit);
            result = convertToTopVideoItems(newVideos);
            
            logger.debug("📊 获取新视频排行榜成功: date={}, limit={}, count={}", date, limit, result.size());
            
        } catch (Exception e) {
            logger.error("获取新视频排行榜失败: date={}, limit={}", date, limit, e);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getCategoryPlayStats7Days(LocalDate date) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            LocalDate startDate = date.minusDays(6); // 7日内数据
            LocalDate endDate = date;
            
            List<CategoryPlayStats> categoryStats = videoDailyPlaysMapper.selectCategoryPlayStats7Days(startDate, endDate);
            
            // 计算总播放量用于计算占比
            long totalPlaysSum = categoryStats.stream()
                    .mapToLong(CategoryPlayStats::getTotalPlays)
                    .sum();
            
            for (CategoryPlayStats stats : categoryStats) {
                Map<String, Object> item = new HashMap<>();
                item.put("categoryId", stats.getCategoryId());
                item.put("categoryName", stats.getCategoryName());
                item.put("videoCount", stats.getVideoCount());
                item.put("totalPlays", stats.getTotalPlays());
                item.put("avgPlays", Math.round(stats.getAvgPlays() != null ? stats.getAvgPlays() : 0.0));
                
                // 计算占比
                double percentage = totalPlaysSum > 0 ? (stats.getTotalPlays() * 100.0 / totalPlaysSum) : 0.0;
                item.put("percentage", Math.round(percentage * 100.0) / 100.0);
                
                result.add(item);
            }
            
            logger.debug("📊 获取7日分类播放统计成功: date={}, 分类数={}", date, result.size());
            
        } catch (Exception e) {
            logger.error("获取7日分类播放统计失败: date={}", date, e);
        }
        
        return result;
    }
    
    @Override
    public int cleanupOldPlayRecords() {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(7); // 保留7天数据
            int deletedCount = videoDailyPlaysMapper.deleteOldRecords(cutoffDate);
            
            logger.info("🗑️ 清理7天前的播放记录完成: 删除{}条记录, 截止日期={}", deletedCount, cutoffDate);
            
            return deletedCount;
        } catch (Exception e) {
            logger.error("清理旧播放记录失败", e);
            return 0;
        }
    }

    @Override
    public List<Map<String, Object>> getSearchKeywords(LocalDate date, LocalDate startDate, LocalDate endDate, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (date != null) {
                // 单日：优先读 Redis ZSet（实时），降级读 MySQL
                String day = date.toString().replaceAll("-", "");
                String redisKey = "search:kw:" + day;
                Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> entries =
                    redisTemplate.opsForZSet().reverseRangeWithScores(redisKey, 0, limit - 1);
                if (entries != null && !entries.isEmpty()) {
                    int rank = 1;
                    for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> e : entries) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("keyword", String.valueOf(e.getValue()));
                        item.put("count", e.getScore() != null ? e.getScore().longValue() : 0L);
                        item.put("rank", rank++);
                        result.add(item);
                    }
                    logger.debug("📊 搜索关键词（Redis）: date={}, 词数={}", date, result.size());
                    return result;
                }
                // Redis 无数据，降级查 MySQL 当日
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT keyword, search_count AS count FROM stats_search_keywords " +
                    "WHERE stat_date = ? ORDER BY search_count DESC LIMIT ?",
                    java.sql.Date.valueOf(date), limit);
                int rank = 1;
                for (Map<String, Object> row : rows) {
                    row.put("rank", rank++);
                    result.add(row);
                }
            } else if (startDate != null && endDate != null) {
                // 日期范围：查 MySQL 聚合
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT keyword, SUM(search_count) AS count FROM stats_search_keywords " +
                    "WHERE stat_date BETWEEN ? AND ? GROUP BY keyword ORDER BY count DESC LIMIT ?",
                    java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate), limit);
                int rank = 1;
                for (Map<String, Object> row : rows) {
                    row.put("rank", rank++);
                    result.add(row);
                }
                logger.debug("📊 搜索关键词（MySQL范围）: {}-{}, 词数={}", startDate, endDate, result.size());
            }
        } catch (Exception e) {
            logger.error("获取搜索关键词排行失败", e);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getHourlyActivity(LocalDate date) {
        String day = date.toString().replaceAll("-", "");
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            String hourKey = "pv:hour:" + day + ":" + String.format("%02d", h);
            Long count = asLong(redisTemplate.opsForValue().get(hourKey));
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("count", count == null ? 0L : count);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getNoResultKeywords(LocalDate date, int limit) {
        String day = date.toString().replaceAll("-", "");
        String nrKey = "search:kw:noresult:" + day;
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            java.util.Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> entries =
                redisTemplate.opsForZSet().reverseRangeWithScores(nrKey, 0, limit - 1);
            if (entries != null) {
                int rank = 1;
                for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> e : entries) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("keyword", String.valueOf(e.getValue()));
                    item.put("count", e.getScore() == null ? 0L : e.getScore().longValue());
                    item.put("rank", rank++);
                    result.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("获取零结果关键词排行失败", e);
        }
        return result;
    }

    @Override
    public Map<String, Object> getContentHealth(int days) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " +
                "  SUM(CASE WHEN total_plays = 0 THEN 1 ELSE 0 END) AS zero_plays, " +
                "  SUM(CASE WHEN total_plays BETWEEN 1 AND 10 THEN 1 ELSE 0 END) AS low_plays, " +
                "  SUM(CASE WHEN total_plays BETWEEN 11 AND 100 THEN 1 ELSE 0 END) AS mid_plays, " +
                "  SUM(CASE WHEN total_plays > 100 THEN 1 ELSE 0 END) AS high_plays, " +
                "  COUNT(*) AS total_active " +
                "FROM (" +
                "  SELECT video_id, SUM(play_count) AS total_plays " +
                "  FROM video_daily_plays " +
                "  WHERE play_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "  GROUP BY video_id" +
                ") t",
                days);
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                result.put("zeroPlays",  asLong(row.get("zero_plays")));
                result.put("lowPlays",   asLong(row.get("low_plays")));
                result.put("midPlays",   asLong(row.get("mid_plays")));
                result.put("highPlays",  asLong(row.get("high_plays")));
                result.put("totalActive", asLong(row.get("total_active")));
            }
        } catch (Exception e) {
            logger.error("获取内容健康度失败", e);
        }
        result.putIfAbsent("zeroPlays", 0L);
        result.putIfAbsent("lowPlays", 0L);
        result.putIfAbsent("midPlays", 0L);
        result.putIfAbsent("highPlays", 0L);
        result.putIfAbsent("totalActive", 0L);
        result.put("days", days);
        return result;
    }

}
