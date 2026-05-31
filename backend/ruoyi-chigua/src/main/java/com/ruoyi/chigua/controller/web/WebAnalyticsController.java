package com.ruoyi.chigua.controller.web;

import com.ruoyi.chigua.service.UserBehaviorLogService;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 匿名事件接收（用户端埋点）
 */
@RestController
@RequestMapping("/web/analytics")
public class WebAnalyticsController {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private UserBehaviorLogService userBehaviorLogService;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration DAILY_KEY_TTL = Duration.ofDays(45);
    private static final Duration USER_STATE_TTL = Duration.ofDays(120);
    private static final String NEW_USER_SET_PREFIX = "new_user_set:";
    private static final String ACTIVE_USER_SET_PREFIX = "active_user_set:";
    private static final String NEW_USERS_HLL_PREFIX = "new_users:";
    private static final String USER_FIRST_SEEN_KEY = "user:first_seen";
    private static final String USER_LAST_SEEN_KEY = "user:last_seen";

    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @PostMapping("/event")
    public AjaxResult ingest(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String type = str(payload.get("type"));
        String anonymousId = str(payload.get("anonymousId"));
        String path = str(payload.get("path"));
        String today = LocalDate.now().format(DAY_FMT);

        try {
            String userKey = generateUserKeyFromRequest(request);
            String userId = resolveUserId(anonymousId, userKey);

            if (userId != null && !userId.isEmpty()) {
                boolean firstSeen = markUserSeen(userId, today);

                touchHyperLogLog("uv:" + today, userId);
                if (isActiveAction(type, payload)) {
                    touchHyperLogLog("dau:" + today, userId);
                }

                markActiveUser(today, userId);
                if (firstSeen) {
                    registerNewUser(today, userId);
                }
            }

            // 按小时统计活跃量（所有事件均计入，用于热力图）
            int hour = LocalDateTime.now().getHour();
            String hourKey = "pv:hour:" + today + ":" + String.format("%02d", hour);
            redisTemplate.opsForValue().increment(hourKey);
            Long hourTtl = redisTemplate.getExpire(hourKey, TimeUnit.SECONDS);
            if (hourTtl == null || hourTtl < 0) {
                redisTemplate.expire(hourKey, 8, TimeUnit.DAYS);
            }

            String keyword = null;
            String eventTarget = null;
            if ("page_view".equalsIgnoreCase(type)) {
                incrementCounter("pv:" + today);
                if (path != null && !path.isEmpty()) {
                    incrementCounter("pv:path:" + path + ":" + today, 1);
                }
            } else if ("category_click".equalsIgnoreCase(type)) {
                String categoryId = str(payload.get("categoryId"));
                eventTarget = categoryId;
                if (categoryId != null && !categoryId.isEmpty()) {
                    incrementCounter("category_clicks:" + today);
                    incrementCounter("cat:click:" + categoryId + ":" + today, 1);
                }
            } else if ("search_submit".equalsIgnoreCase(type)) {
                keyword = str(payload.get("keyword"));
                if (keyword != null && !keyword.isEmpty()) {
                    incrementCounter("searches:" + today);
                    redisTemplate.opsForZSet().incrementScore("search:kw:" + today, keyword.trim().toLowerCase(), 1);
                    touchDailyKey("search:kw:" + today);
                }
            } else if ("search_no_result".equalsIgnoreCase(type)) {
                keyword = str(payload.get("keyword"));
                if (keyword != null && !keyword.isEmpty()) {
                    String nrKey = "search:kw:noresult:" + today;
                    redisTemplate.opsForZSet().incrementScore(nrKey, keyword.trim().toLowerCase(), 1);
                    touchDailyKey(nrKey);
                }
            } else if (type != null) {
                // 所有非内置 Redis 计数的事件类型，都尝试提取 targetId / videoId
                // 覆盖：video_*、ad_click、redgifs_*、tg_post_view、tg_media_play 等
                eventTarget = str(payload.get("targetId"));
                if (eventTarget == null || eventTarget.isEmpty()) {
                    eventTarget = str(payload.get("videoId"));
                }
                if (eventTarget == null || eventTarget.isEmpty()) {
                    eventTarget = str(payload.get("postId"));
                }
            }

            // 写入流水表（异步）：所有 event 都进
            try {
                userBehaviorLogService.asyncLog(type, eventTarget, path, anonymousId, keyword, request);
            } catch (Exception ignore) {}

            return AjaxResult.success();
        } catch (Exception e) {
            return AjaxResult.error("event ingest failed: " + e.getMessage());
        }
    }

    private boolean markUserSeen(String userId, String today) {
        Boolean isNew = redisTemplate.opsForHash().putIfAbsent(USER_FIRST_SEEN_KEY, userId, today);
        // 不设 TTL：user:first_seen / user:last_seen 永久保留，防止 120 天后历史用户被误判为新用户
        redisTemplate.opsForHash().put(USER_LAST_SEEN_KEY, userId, today);
        return Boolean.TRUE.equals(isNew);
    }

    private void registerNewUser(String today, String userId) {
        redisTemplate.opsForSet().add(NEW_USER_SET_PREFIX + today, userId);
        touchDailyKey(NEW_USER_SET_PREFIX + today);
        touchHyperLogLog(NEW_USERS_HLL_PREFIX + today, userId);
    }

    private void markActiveUser(String today, String userId) {
        redisTemplate.opsForSet().add(ACTIVE_USER_SET_PREFIX + today, userId);
        touchDailyKey(ACTIVE_USER_SET_PREFIX + today);
    }

    private boolean isActiveAction(String type, Map<String, Object> payload) {
        if (type == null) {
            return false;
        }
        switch (type.toLowerCase()) {
            case "search_submit":
            case "category_click":
                return true;
            case "page_view":
                Object value = payload == null ? null : payload.get("duration");
                if (value instanceof Number) {
                    return ((Number) value).longValue() > 30000L;
                }
                return false;
            default:
                return false;
        }
    }

    private String resolveUserId(String anonymousId, String fallback) {
        if (anonymousId != null) {
            String trimmed = anonymousId.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return fallback;
    }

    private String generateUserKeyFromRequest(HttpServletRequest request) {
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
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }
        String combined = (ip == null ? "unknown" : ip) + "|" + userAgent;
        return md5Hex(combined).substring(0, 16);
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
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

    private void incrementCounter(String key) {
        incrementCounter(key, 1);
    }

    private void incrementCounter(String key, long delta) {
        redisTemplate.opsForValue().increment(key, delta);
        touchDailyKey(key);
    }

    private void touchHyperLogLog(String key, String value) {
        redisTemplate.opsForHyperLogLog().add(key, value);
        touchDailyKey(key);
    }

    private void touchDailyKey(String key) {
        touchKey(key, DAILY_KEY_TTL);
    }

    private void touchKey(String key, Duration ttl) {
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttlSeconds == null || ttlSeconds < 0) {
            redisTemplate.expire(key, ttl);
        }
    }
}
