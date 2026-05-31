package com.ruoyi.chigua.service;

import com.ruoyi.chigua.domain.UserBehaviorLog;
import com.ruoyi.chigua.mapper.UserBehaviorLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 用户行为日志服务
 *
 * 高性能写入策略：
 *   1) 接收线程（HTTP 业务线程）只往内存队列 push，O(1)
 *   2) 后台单线程 consumer 每 2 秒 / 满 200 条 触发批量 INSERT
 *   3) 队列上限 100000，溢出直接丢弃（避免业务被阻塞）
 *   4) 关闭时 flush 残留
 */
@Service
public class UserBehaviorLogService {

    private static final Logger log = LoggerFactory.getLogger(UserBehaviorLogService.class);

    /** 队列容量上限 */
    private static final int QUEUE_CAPACITY = 100_000;
    /** 单次批量大小 */
    private static final int BATCH_SIZE = 200;
    /** 后台 flush 周期（毫秒） */
    private static final long FLUSH_INTERVAL_MS = 2000L;

    private final LinkedBlockingQueue<UserBehaviorLog> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private Thread consumerThread;
    private volatile boolean running = true;

    @Autowired
    private UserBehaviorLogMapper mapper;

    @Autowired
    private IpRegionService ipRegionService;

    @PostConstruct
    public void start() {
        consumerThread = new Thread(this::consumeLoop, "behavior-log-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("UserBehaviorLogService 启动，队列容量={}，批大小={}，flush={}ms", QUEUE_CAPACITY, BATCH_SIZE, FLUSH_INTERVAL_MS);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
            try {
                consumerThread.join(3000);
            } catch (InterruptedException ignored) {}
        }
        flushAll();
    }

    // ========== 对外：异步入队 ==========

    /**
     * 异步记录一条行为日志（构造好 Log 对象后入队）
     * 队列满直接丢弃；不抛异常、不阻塞业务线程
     */
    public void asyncLog(UserBehaviorLog log) {
        if (log == null) return;
        if (log.getEventTime() == null) log.setEventTime(new Date());
        if (log.getEventDate() == null) {
            Calendar c = Calendar.getInstance();
            c.setTime(log.getEventTime());
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            log.setEventDate(c.getTime());
        }
        // offer 立即返回，不阻塞
        boolean ok = queue.offer(log);
        if (!ok) {
            // 防止刷屏：仅 debug 级
            UserBehaviorLogService.log.debug("行为日志队列满，丢弃事件 type={}", log.getEventType());
        }
    }

    /**
     * 便捷方法：根据 HttpServletRequest 构造常用字段后入队
     */
    public void asyncLog(String eventType,
                         String eventTarget,
                         String pagePath,
                         String anonymousId,
                         String keyword,
                         HttpServletRequest request) {
        try {
            UserBehaviorLog l = new UserBehaviorLog();
            l.setEventType(eventType);
            l.setEventTarget(eventTarget);
            l.setPagePath(truncate(pagePath, 255));
            l.setKeyword(truncate(keyword, 200));
            l.setAnonymousId(truncate(anonymousId, 64));
            fillFromRequest(l, request);
            asyncLog(l);
        } catch (Exception e) {
            log.debug("asyncLog 异常: {}", e.getMessage());
        }
    }

    public void fillFromRequest(UserBehaviorLog l, HttpServletRequest request) {
        if (request == null) return;
        String ua = request.getHeader("User-Agent");
        String ip = ipRegionService.getClientIp(request);
        l.setUserAgent(truncate(ua, 255));
        l.setIp(ip == null ? "0.0.0.0" : truncate(ip, 64));
        l.setReferrer(truncate(request.getHeader("Referer"), 255));
        l.setIpRegion(truncate(ipRegionService.search(l.getIp()), 128));
        l.setIsChina(ipRegionService.isChina(l.getIp()) ? 1 : 0);
        l.setFingerprint(md5Hex((l.getIp() == null ? "" : l.getIp()) + "|" + (ua == null ? "" : ua)).substring(0, 16));

        String[] dev = parseUserAgent(ua);
        l.setDeviceType(dev[0]);
        l.setBrowser(dev[1]);
        l.setOs(dev[2]);

        // session id：从 cookie 兜底
        if (l.getSessionId() == null && request.getSession(false) != null) {
            l.setSessionId(truncate(request.getSession(false).getId(), 64));
        }
    }

    // ========== 内部 consumer ==========

    private void consumeLoop() {
        List<UserBehaviorLog> buf = new ArrayList<>(BATCH_SIZE);
        long lastFlush = System.currentTimeMillis();
        while (running) {
            try {
                UserBehaviorLog item = queue.poll(500, TimeUnit.MILLISECONDS);
                if (item != null) {
                    buf.add(item);
                    queue.drainTo(buf, BATCH_SIZE - buf.size());
                }
                long now = System.currentTimeMillis();
                if (!buf.isEmpty() && (buf.size() >= BATCH_SIZE || now - lastFlush >= FLUSH_INTERVAL_MS)) {
                    flush(buf);
                    buf.clear();
                    lastFlush = now;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("behavior-log-consumer 异常: {}", e.getMessage());
                buf.clear();
            }
        }
    }

    private void flushAll() {
        List<UserBehaviorLog> rest = new ArrayList<>();
        queue.drainTo(rest);
        if (!rest.isEmpty()) {
            flush(rest);
        }
    }

    private void flush(List<UserBehaviorLog> list) {
        try {
            mapper.batchInsert(list);
        } catch (Exception e) {
            log.warn("user_behavior_log 批量写入失败 size={} : {}", list.size(), e.getMessage());
        }
    }

    // ========== 公共查询委托（给 Admin Controller 用） ==========

    public List<UserBehaviorLog> selectEventList(Map<String, Object> q) {
        return mapper.selectEventList(q);
    }

    public long countEventList(Map<String, Object> q) {
        return mapper.countEventList(q);
    }

    public List<Map<String, Object>> selectIpSummary(Map<String, Object> q) {
        return mapper.selectIpSummary(q);
    }

    public long countIpSummary(Map<String, Object> q) {
        return mapper.countIpSummary(q);
    }

    public List<UserBehaviorLog> selectByIp(String ip, Date start, Date end, int limit) {
        return mapper.selectByIp(ip, start, end, limit);
    }

    public Map<String, Object> selectOverview(Date startDate, Date endDate) {
        return mapper.selectOverview(startDate, endDate);
    }

    public List<Map<String, Object>> selectEventTypeBreakdown(Date startDate, Date endDate) {
        return mapper.selectEventTypeBreakdown(startDate, endDate);
    }

    /** 队列当前堆积量（监控指标） */
    public int getQueueSize() {
        return queue.size();
    }

    public List<Map<String, Object>> selectVideoTitles(List<Long> ids)         { return mapper.selectVideoTitles(ids); }
    public List<Map<String, Object>> selectTgPostTitles(List<Long> ids)        { return mapper.selectTgPostTitles(ids); }
    public List<Map<String, Object>> selectCategoryNames(List<Long> ids)       { return mapper.selectCategoryNames(ids); }
    public List<Map<String, Object>> selectRedgifsTitles(List<Long> ids)       { return mapper.selectRedgifsTitles(ids); }
    public List<Map<String, Object>> selectCollectionTitles(List<Long> ids)    { return mapper.selectCollectionTitles(ids); }
    public List<Map<String, Object>> selectAdvertisementNames(List<Long> ids)  { return mapper.selectAdvertisementNames(ids); }
    public List<Map<String, Object>> selectTagNames(List<Long> ids)            { return mapper.selectTagNames(ids); }

    // ========== 工具方法 ==========

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    /** 极简 UA 解析：[deviceType, browser, os] */
    private String[] parseUserAgent(String ua) {
        String device = "desktop", browser = "unknown", os = "unknown";
        if (ua == null) return new String[]{device, browser, os};
        String u = ua.toLowerCase();
        if (u.contains("ipad") || u.contains("tablet")) device = "tablet";
        else if (u.contains("mobi") || u.contains("android") || u.contains("iphone")) device = "mobile";

        if      (u.contains("edg/"))      browser = "Edge";
        else if (u.contains("chrome/"))   browser = "Chrome";
        else if (u.contains("safari/"))   browser = "Safari";
        else if (u.contains("firefox/"))  browser = "Firefox";
        else if (u.contains("opera"))     browser = "Opera";
        else if (u.contains("bot") || u.contains("spider") || u.contains("crawler")) browser = "Bot";

        if      (u.contains("windows"))    os = "Windows";
        else if (u.contains("mac os x") || u.contains("macintosh")) os = "macOS";
        else if (u.contains("android"))    os = "Android";
        else if (u.contains("iphone") || u.contains("ipad") || u.contains("ios")) os = "iOS";
        else if (u.contains("linux"))      os = "Linux";

        return new String[]{device, browser, os};
    }

    // ========== 定时任务：分区/清理 ==========

    /**
     * 每天 03:30 清理 90 天前的旧数据
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanOldData() {
        try {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -90);
            int n = mapper.deleteBeforeDate(c.getTime());
            log.info("user_behavior_log 清理 90 天前数据: {} 行", n);
        } catch (Exception e) {
            log.warn("cleanOldData 失败: {}", e.getMessage());
        }
    }
}
