package com.ruoyi.web.controller.chigua;

import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import java.security.MessageDigest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.chigua.domain.VideoStats;
import com.ruoyi.chigua.service.IVideoStatsService;
import com.ruoyi.chigua.service.UserBehaviorLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;

/**
 * 视频统计Controller
 * 
 * @author ruoyi
 * @date 2024-01-15
 */
@RestController
@RequestMapping("/chigua/stats")
public class VideoStatsController extends BaseController
{
    @Autowired
    private IVideoStatsService videoStatsService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private UserBehaviorLogService userBehaviorLogService;

    // 留存统计相关常量（与 WebAnalyticsController 保持一致）
    private static final java.time.Duration DAILY_KEY_TTL   = java.time.Duration.ofDays(45);
    private static final String NEW_USER_SET_PREFIX          = "new_user_set:";
    private static final String ACTIVE_USER_SET_PREFIX       = "active_user_set:";
    private static final String NEW_USERS_HLL_PREFIX         = "new_users:";
    private static final String USER_FIRST_SEEN_KEY          = "user:first_seen";

    /**
     * 查询视频统计列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:list')")
    @GetMapping("/list")
    public TableDataInfo list(VideoStats videoStats)
    {
        startPage();
        List<VideoStats> list = videoStatsService.selectVideoStatsList(videoStats);
        return getDataTable(list);
    }

    /**
     * 导出视频统计列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:export')")
    @Log(title = "视频统计", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VideoStats videoStats)
    {
        List<VideoStats> list = videoStatsService.selectVideoStatsList(videoStats);
        ExcelUtil<VideoStats> util = new ExcelUtil<VideoStats>(VideoStats.class);
        util.exportExcel(response, list, "视频统计数据");
    }

    /**
     * 获取视频统计详细信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(videoStatsService.selectVideoStatsById(id));
    }

    /**
     * 根据视频ID获取统计信息
     */
    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @GetMapping(value = "/video/{videoId}")
    public AjaxResult getStatsByVideoId(@PathVariable("videoId") Long videoId)
    {
        VideoStats stats = videoStatsService.selectVideoStatsByVideoId(videoId);
        if (stats == null) {
            // 如果没有统计记录，返回默认值
            stats = new VideoStats();
            stats.setVideoId(videoId);
            stats.setViewCount(0);
            stats.setPlayCount(0);
            stats.setLikeCount(0);
            stats.setCommentCount(0);
            stats.setShareCount(0);
        }
        return success(stats);
    }

    /**
     * 新增视频统计
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:add')")
    @Log(title = "视频统计", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VideoStats videoStats)
    {
        return toAjax(videoStatsService.insertVideoStats(videoStats));
    }

    /**
     * 修改视频统计
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:edit')")
    @Log(title = "视频统计", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VideoStats videoStats)
    {
        return toAjax(videoStatsService.updateVideoStats(videoStats));
    }

    /**
     * 删除视频统计
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:remove')")
    @Log(title = "视频统计", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(videoStatsService.deleteVideoStatsByIds(ids));
    }

    // ==================== 前端统计API ====================

    /**
     * 增加浏览量
     */
    @RateLimiter(time = 60, count = 15, limitType = LimitType.IP)
    @PostMapping("/view/{videoId}")
    public AjaxResult incrementView(@PathVariable Long videoId,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            HttpServletRequest request)
    {
        boolean success = videoStatsService.incrementViewCount(videoId);
        
        // 同时记录到Redis当日统计（用于统计面板）
        if (success) {
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                String day = today.toString().replaceAll("-", "");
                
                // PV统计
                redisTemplate.opsForValue().increment("pv:" + day, 1);
                
                // 优先使用前端传入的 anonymousId，与 WebAnalyticsController 保持一致，避免重复计数
                String anonymousId = body != null ? (String) body.get("anonymousId") : null;
                String userKey = resolveUserId(anonymousId, generateUserKeyFromRequest(request));
                redisTemplate.opsForHyperLogLog().add("uv:" + day, userKey);
                redisTemplate.opsForHyperLogLog().add("dau:" + day, userKey);

                // 视频浏览算强活跃行为，同步写入留存 Set，与 WebAnalyticsController 保持一致
                boolean firstSeen = markUserSeenForRetention(userKey, day);
                markActiveUserForRetention(day, userKey);
                if (firstSeen) {
                    registerNewUserForRetention(day, userKey);
                }
                
            } catch (Exception ignored) {}

            try {
                String anonymousId = body != null ? (String) body.get("anonymousId") : null;
                userBehaviorLogService.asyncLog("video_view", String.valueOf(videoId), null, anonymousId, null, request);
            } catch (Exception ignored) {}

            return success("浏览量增加成功");
        } else {
            return error("浏览量增加失败");
        }
    }

    /**
     * 增加播放量
     */
    @RateLimiter(time = 60, count = 15, limitType = LimitType.IP)
    @PostMapping("/play/{videoId}")
    public AjaxResult incrementPlay(@PathVariable Long videoId,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            HttpServletRequest request)
    {
        boolean success = videoStatsService.incrementPlayCount(videoId);
        
        // 同时记录到Redis当日统计（用于统计面板）
        if (success) {
            try {
                java.time.LocalDate today = java.time.LocalDate.now();
                String day = today.toString().replaceAll("-", "");
                
                // 播放统计
                redisTemplate.opsForValue().increment("plays:" + day, 1);
                
                // 优先使用前端传入的 anonymousId，与 WebAnalyticsController 保持一致，避免重复计数
                String anonymousId = body != null ? (String) body.get("anonymousId") : null;
                String userKey = resolveUserId(anonymousId, generateUserKeyFromRequest(request));
                redisTemplate.opsForHyperLogLog().add("dau:" + day, userKey);

                // 视频播放算强活跃行为，同步写入 active_user_set（新用户注册由 view 接口处理，不重复）
                markActiveUserForRetention(day, userKey);
                
            } catch (Exception ignored) {}

            try {
                String anonymousId = body != null ? (String) body.get("anonymousId") : null;
                userBehaviorLogService.asyncLog("video_play", String.valueOf(videoId), null, anonymousId, null, request);
            } catch (Exception ignored) {}

            return success("播放量增加成功");
        } else {
            return error("播放量增加失败");
        }
    }

    /**
     * 增加点赞量
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/like/{videoId}")
    public AjaxResult incrementLike(@PathVariable Long videoId, HttpServletRequest request)
    {
        int result = videoStatsService.incrementLikeCount(videoId);
        try {
            userBehaviorLogService.asyncLog("video_like", String.valueOf(videoId), null, null, null, request);
        } catch (Exception ignored) {}
        return toAjax(result);
    }

    /**
     * 减少点赞量
     */
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/unlike/{videoId}")
    public AjaxResult decrementLike(@PathVariable Long videoId, HttpServletRequest request)
    {
        int result = videoStatsService.decrementLikeCount(videoId);
        try {
            userBehaviorLogService.asyncLog("video_unlike", String.valueOf(videoId), null, null, null, request);
        } catch (Exception ignored) {}
        return toAjax(result);
    }

    /**
     * 增加分享次数
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/share/{videoId}")
    public AjaxResult incrementShare(@PathVariable Long videoId, HttpServletRequest request)
    {
        int result = videoStatsService.incrementShareCount(videoId);
        
        // 🔥 同时记录到Redis当日统计（用于统计面板）
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            String day = today.toString().replaceAll("-", "");
            redisTemplate.opsForValue().increment("shares:" + day, 1);
        } catch (Exception ignored) {}

        try {
            userBehaviorLogService.asyncLog("video_share", String.valueOf(videoId), null, null, null, request);
        } catch (Exception ignored) {}

        return toAjax(result);
    }

    /**
     * 同步评论数量
     */
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/sync-comment/{videoId}")
    public AjaxResult syncCommentCount(@PathVariable Long videoId)
    {
        int result = videoStatsService.syncCommentCount(videoId);
        return toAjax(result);
    }

    // ==================== 辅助方法 ====================



    /**
     * 获取客户端IP地址
     */
    private String getClientIP(HttpServletRequest request)
    {
        String clientIP = request.getHeader("X-Forwarded-For");
        if (StringUtils.isEmpty(clientIP) || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isEmpty(clientIP) || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(clientIP) || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(clientIP) || "unknown".equalsIgnoreCase(clientIP)) {
            clientIP = request.getRemoteAddr();
        }
        
        // 处理多级代理的情况，取第一个IP
        if (StringUtils.isNotEmpty(clientIP) && clientIP.contains(",")) {
            clientIP = clientIP.split(",")[0].trim();
        }
        
        return clientIP;
    }

    /**
     * 获取Redis统计缓存数据（调试用）
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:debug')")
    @GetMapping("/redis/debug")
    public AjaxResult getRedisDebugInfo()
    {
        try {
            // 获取浏览量计数器
            Set<Object> viewKeys = redisTemplate.keys("video:view:count:*");
            Set<Object> playKeys = redisTemplate.keys("video:play:count:*");
            
            java.util.Map<String, Object> debugInfo = new java.util.HashMap<>();
            debugInfo.put("view_counters", viewKeys != null ? viewKeys.size() : 0);
            debugInfo.put("play_counters", playKeys != null ? playKeys.size() : 0);
            
            // 获取前5个key的详细信息
            java.util.List<java.util.Map<String, Object>> viewDetails = new java.util.ArrayList<>();
            if (viewKeys != null) {
                int count = 0;
                for (Object keyObj : viewKeys) {
                    if (count++ >= 5) break;
                    String key = (String) keyObj;
                    java.util.Map<String, Object> detail = new java.util.HashMap<>();
                    detail.put("key", key);
                    detail.put("value", redisTemplate.opsForValue().get(key));
                    detail.put("ttl", redisTemplate.getExpire(key));
                    viewDetails.add(detail);
                }
            }
            debugInfo.put("view_details", viewDetails);
            
            java.util.List<java.util.Map<String, Object>> playDetails = new java.util.ArrayList<>();
            if (playKeys != null) {
                int count = 0;
                for (Object keyObj : playKeys) {
                    if (count++ >= 5) break;
                    String key = (String) keyObj;
                    java.util.Map<String, Object> detail = new java.util.HashMap<>();
                    detail.put("key", key);
                    detail.put("value", redisTemplate.opsForValue().get(key));
                    detail.put("ttl", redisTemplate.getExpire(key));
                    playDetails.add(detail);
                }
            }
            debugInfo.put("play_details", playDetails);
            
            return success(debugInfo);
        } catch (Exception e) {
            return error("获取Redis调试信息失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发统计同步（已废弃 - 现在使用实时写入）
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:sync')")
    @PostMapping("/redis/sync")
    public AjaxResult manualSync()
    {
        return success("统计数据已改为实时写入模式，无需手动同步");
    }

    /**
     * 清除Redis统计缓存（调试用）
     */
    @PreAuthorize("@ss.hasPermi('chigua:stats:clear')")
    @DeleteMapping("/redis/clear")
    public AjaxResult clearRedisCache()
    {
        try {
            Set<Object> viewKeys = redisTemplate.keys("video:view:count:*");
            Set<Object> playKeys = redisTemplate.keys("video:play:count:*");
            
            int cleared = 0;
            if (viewKeys != null && !viewKeys.isEmpty()) {
                redisTemplate.delete(viewKeys);
                cleared += viewKeys.size();
            }
            if (playKeys != null && !playKeys.isEmpty()) {
                redisTemplate.delete(playKeys);
                cleared += playKeys.size();
            }
            
            return success("已清除 " + cleared + " 个Redis缓存");
        } catch (Exception e) {
            return error("清除Redis缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析用户标识：优先使用前端传入的 anonymousId，否则回退到 IP+UA hash
     */
    private String resolveUserId(String anonymousId, String fallback) {
        if (anonymousId != null && !anonymousId.trim().isEmpty()) {
            return anonymousId.trim();
        }
        return fallback;
    }

    /**
     * 基于IP和User-Agent生成用户标识
     */
    private String generateUserKeyFromRequest(HttpServletRequest request) {
        // 获取真实IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        // 获取User-Agent
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "unknown";
        
        // 生成用户标识
        String combined = ip + "|" + userAgent;
        return md5Hex(combined).substring(0, 16);
    }
    
    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    // ==================== 留存统计辅助方法 ====================

    /**
     * 标记用户首次访问（不设 TTL，让 Hash 永久保留，防止历史用户被误判为新用户）
     * @return true 表示该用户今天首次出现（新用户）
     */
    private boolean markUserSeenForRetention(String userId, String today) {
        Boolean isNew = redisTemplate.opsForHash().putIfAbsent(USER_FIRST_SEEN_KEY, userId, today);
        return Boolean.TRUE.equals(isNew);
    }

    /**
     * 将用户加入当日活跃用户 Set（供留存计算使用）
     */
    private void markActiveUserForRetention(String day, String userId) {
        redisTemplate.opsForSet().add(ACTIVE_USER_SET_PREFIX + day, userId);
        Long ttl = redisTemplate.getExpire(ACTIVE_USER_SET_PREFIX + day, java.util.concurrent.TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            redisTemplate.expire(ACTIVE_USER_SET_PREFIX + day, DAILY_KEY_TTL);
        }
    }

    /**
     * 将新用户加入当日新用户 Set 和 HLL（供留存计算使用）
     */
    private void registerNewUserForRetention(String day, String userId) {
        redisTemplate.opsForSet().add(NEW_USER_SET_PREFIX + day, userId);
        Long ttl = redisTemplate.getExpire(NEW_USER_SET_PREFIX + day, java.util.concurrent.TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            redisTemplate.expire(NEW_USER_SET_PREFIX + day, DAILY_KEY_TTL);
        }
        redisTemplate.opsForHyperLogLog().add(NEW_USERS_HLL_PREFIX + day, userId);
    }
}