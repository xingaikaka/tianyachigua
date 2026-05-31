package com.ruoyi.web.controller.monitor;

import com.ruoyi.chigua.domain.UserBehaviorLog;
import com.ruoyi.chigua.service.UserBehaviorLogService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 用户行为管理（系统监控 → 用户行为）
 *
 * @author chigua
 * @date 2026-05-24
 */
@RestController
@RequestMapping("/monitor/behavior")
public class BehaviorAdminController extends BaseController {

    @Autowired
    private UserBehaviorLogService userBehaviorLogService;

    /**
     * 概览数据（默认最近 7 天）
     */
    @PreAuthorize("@ss.hasPermi('monitor:behavior:list')")
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        Date[] range = parseDateRange(startDate, endDate, 7);
        Map<String, Object> overview = userBehaviorLogService.selectOverview(range[0], range[1]);
        List<Map<String, Object>> breakdown = userBehaviorLogService.selectEventTypeBreakdown(range[0], range[1]);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overview", overview == null ? new LinkedHashMap<>() : overview);
        data.put("breakdown", breakdown);
        data.put("queueSize", userBehaviorLogService.getQueueSize());
        data.put("startDate", new SimpleDateFormat("yyyy-MM-dd").format(range[0]));
        data.put("endDate",   new SimpleDateFormat("yyyy-MM-dd").format(range[1]));
        return success(data);
    }

    /**
     * IP 维度的聚合列表
     */
    @PreAuthorize("@ss.hasPermi('monitor:behavior:list')")
    @GetMapping("/ip/list")
    public TableDataInfo ipList(@RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate,
                                @RequestParam(required = false) String ip,
                                @RequestParam(required = false) String ipRegion,
                                @RequestParam(required = false) Integer isChina,
                                @RequestParam(required = false) String deviceType,
                                @RequestParam(required = false) String eventType,
                                @RequestParam(defaultValue = "1")  Integer pageNum,
                                @RequestParam(defaultValue = "20") Integer pageSize) {

        Date[] range = parseDateRange(startDate, endDate, 7);
        Map<String, Object> q = buildQuery(range, ip, ipRegion, isChina, deviceType, eventType, null, null);

        long total = userBehaviorLogService.countIpSummary(q);
        q.put("offset", (pageNum - 1) * pageSize);
        q.put("limit", pageSize);
        List<Map<String, Object>> list = userBehaviorLogService.selectIpSummary(q);

        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(list);
        rsp.setTotal(total);
        return rsp;
    }

    /**
     * 单 IP 的事件时间线
     */
    @PreAuthorize("@ss.hasPermi('monitor:behavior:list')")
    @GetMapping("/ip/{ip}/timeline")
    public AjaxResult ipTimeline(@PathVariable("ip") String ip,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(defaultValue = "500") Integer limit) {
        Date[] range = parseDateRange(startDate, endDate, 7);
        List<UserBehaviorLog> list = userBehaviorLogService.selectByIp(ip, range[0], range[1], Math.min(limit, 2000));
        fillEventTargetTitles(list);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ip", ip);
        data.put("count", list.size());
        data.put("events", list);
        return success(data);
    }

    /**
     * 给事件列表批量填充 eventTargetTitle（视频/帖子/分类/redgifs 的名字）
     */
    private void fillEventTargetTitles(List<UserBehaviorLog> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> videoIds      = new HashSet<>();
        Set<Long> tgIds         = new HashSet<>();
        Set<Long> categoryIds   = new HashSet<>();
        Set<Long> redgifsIds    = new HashSet<>();
        Set<Long> collectionIds = new HashSet<>();
        Set<Long> adIds         = new HashSet<>();
        Set<Long> tagIds        = new HashSet<>();

        // 第一遍：收集要查的 ID
        for (UserBehaviorLog l : list) {
            String t  = l.getEventType();
            String et = l.getEventTarget();
            Long id = parseLongSafe(et);

            if (id != null && t != null) {
                if (t.startsWith("video_"))                                          videoIds.add(id);
                else if ("ad_click".equals(t))                                       adIds.add(id);
                else if ("category_click".equals(t))                                 categoryIds.add(id);
                else if ("collection_view".equals(t))                                collectionIds.add(id);
                else if ("tag_click".equals(t))                                      tagIds.add(id);
                else if ("comment_submit".equals(t))                                 videoIds.add(id);
                else if ("tg_post_view".equals(t) || "tg_media_play".equals(t))     tgIds.add(id);
                else if ("redgifs_view".equals(t) || "redgifs_play".equals(t) || "redgifs_share".equals(t)) redgifsIds.add(id);
            }

            // page_view 路径反查
            Long fromPath = extractIdFromPath(l.getPagePath(), "/tg/post/");
            if (fromPath != null) tgIds.add(fromPath);
            Long fromVideoPath = extractIdFromPath(l.getPagePath(), "/video/");
            if (fromVideoPath != null) videoIds.add(fromVideoPath);
            Long fromCollPath = extractIdFromPath(l.getPagePath(), "/collection/");
            if (fromCollPath != null) collectionIds.add(fromCollPath);
            Long fromTagPath = extractIdFromPath(l.getPagePath(), "/tag/");
            if (fromTagPath != null) tagIds.add(fromTagPath);
            Long fromCatPath = extractIdFromPath(l.getPagePath(), "/category/");
            if (fromCatPath != null) categoryIds.add(fromCatPath);
            Long fromUserVideoPath = extractIdFromUserVideoPath(l.getPagePath());
            if (fromUserVideoPath != null) redgifsIds.add(fromUserVideoPath);
        }

        Map<Long, String> videoMap      = lookupMap(videoIds,      userBehaviorLogService::selectVideoTitles,         "title");
        Map<Long, String> tgMap         = lookupMap(tgIds,         userBehaviorLogService::selectTgPostTitles,        "title");
        Map<Long, String> categoryMap   = lookupMap(categoryIds,   userBehaviorLogService::selectCategoryNames,       "name");
        Map<Long, String> redgifsMap    = lookupMap(redgifsIds,    userBehaviorLogService::selectRedgifsTitles,       "title");
        Map<Long, String> collectionMap = lookupMap(collectionIds, userBehaviorLogService::selectCollectionTitles,    "title");
        Map<Long, String> adMap         = lookupMap(adIds,         userBehaviorLogService::selectAdvertisementNames,  "name");
        Map<Long, String> tagMap        = lookupMap(tagIds,        userBehaviorLogService::selectTagNames,            "name");

        // 第二遍：按优先级填充
        for (UserBehaviorLog l : list) {
            String t  = l.getEventType();
            String et = l.getEventTarget();
            Long id   = parseLongSafe(et);
            String title = null;

            if (id != null && t != null) {
                if (t.startsWith("video_"))                                          title = videoMap.get(id);
                else if ("ad_click".equals(t))                                       title = adMap.get(id);
                else if ("category_click".equals(t))                                 title = categoryMap.get(id);
                else if ("collection_view".equals(t))                                title = collectionMap.get(id);
                else if ("tag_click".equals(t))                                      title = tagMap.get(id);
                else if ("comment_submit".equals(t))                                 title = videoMap.get(id);
                else if ("tg_post_view".equals(t) || "tg_media_play".equals(t))     title = tgMap.get(id);
                else if ("redgifs_view".equals(t) || "redgifs_play".equals(t) || "redgifs_share".equals(t)) title = redgifsMap.get(id);
            }

            // 兜底：从路径反查
            if (title == null) {
                Long pid = extractIdFromPath(l.getPagePath(), "/tg/post/");
                if (pid != null) title = tgMap.get(pid);
            }
            if (title == null) {
                Long pid = extractIdFromPath(l.getPagePath(), "/video/");
                if (pid != null) title = videoMap.get(pid);
            }
            if (title == null) {
                Long pid = extractIdFromPath(l.getPagePath(), "/collection/");
                if (pid != null) title = collectionMap.get(pid);
            }
            if (title == null) {
                Long pid = extractIdFromPath(l.getPagePath(), "/tag/");
                if (pid != null) title = tagMap.get(pid);
            }
            if (title == null) {
                Long pid = extractIdFromPath(l.getPagePath(), "/category/");
                if (pid != null) title = categoryMap.get(pid);
            }
            if (title == null) {
                Long pid = extractIdFromUserVideoPath(l.getPagePath());
                if (pid != null) title = redgifsMap.get(pid);
            }
            if (title != null) l.setEventTargetTitle(title);
        }
    }

    private Long parseLongSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    /** 从 path 中提取数字 ID，prefix 形如 "/video/" 或 "/tg/post/" */
    private Long extractIdFromPath(String path, String prefix) {
        if (path == null || !path.startsWith(prefix)) return null;
        try {
            String tail = path.substring(prefix.length());
            int cut = tail.length();
            for (int i = 0; i < tail.length(); i++) {
                char c = tail.charAt(i);
                if (!Character.isDigit(c)) { cut = i; break; }
            }
            if (cut <= 0) return null;
            return Long.parseLong(tail.substring(0, cut));
        } catch (Exception ignore) { return null; }
    }

    /** /user/xxx/video/{id} → id */
    private Long extractIdFromUserVideoPath(String path) {
        if (path == null || !path.startsWith("/user/")) return null;
        int videoIdx = path.indexOf("/video/");
        if (videoIdx < 0) return null;
        return extractIdFromPath(path.substring(videoIdx), "/video/");
    }

    private Map<Long, String> lookupMap(Set<Long> ids,
                                        java.util.function.Function<List<Long>, List<Map<String, Object>>> fn,
                                        String key) {
        Map<Long, String> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) return result;
        try {
            List<Map<String, Object>> rows = fn.apply(new ArrayList<>(ids));
            if (rows == null) return result;
            for (Map<String, Object> row : rows) {
                Object idVal = row.get("id");
                Object v = row.get(key);
                if (idVal == null) continue;
                Long id = (idVal instanceof Number) ? ((Number) idVal).longValue() : Long.parseLong(String.valueOf(idVal));
                result.put(id, v == null ? null : String.valueOf(v));
            }
        } catch (Exception ignore) {}
        return result;
    }

    /**
     * 原始事件流（细粒度查询）
     */
    @PreAuthorize("@ss.hasPermi('monitor:behavior:list')")
    @GetMapping("/event/list")
    public TableDataInfo eventList(@RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   @RequestParam(required = false) String ip,
                                   @RequestParam(required = false) String eventType,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String pagePath,
                                   @RequestParam(required = false) String eventTarget,
                                   @RequestParam(required = false) Integer isChina,
                                   @RequestParam(required = false) String ipRegion,
                                   @RequestParam(required = false) String deviceType,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "20") Integer pageSize) {

        Date[] range = parseDateRange(startDate, endDate, 1);
        Map<String, Object> q = buildQuery(range, ip, ipRegion, isChina, deviceType, eventType, keyword, pagePath);
        if (eventTarget != null && !eventTarget.isEmpty()) {
            q.put("eventTarget", eventTarget);
        }

        long total = userBehaviorLogService.countEventList(q);
        q.put("offset", (pageNum - 1) * pageSize);
        q.put("limit", pageSize);
        List<UserBehaviorLog> list = userBehaviorLogService.selectEventList(q);
        fillEventTargetTitles(list);

        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(list);
        rsp.setTotal(total);
        return rsp;
    }

    // ============== helpers ==============

    private Date[] parseDateRange(String startDateStr, String endDateStr, int defaultDays) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        Date end;
        Date start;
        try {
            end = (endDateStr == null || endDateStr.isEmpty()) ? new Date() : sdf.parse(endDateStr);
        } catch (ParseException e) { end = new Date(); }
        cal.setTime(end);
        cal.add(Calendar.DAY_OF_MONTH, -defaultDays + 1);
        try {
            start = (startDateStr == null || startDateStr.isEmpty()) ? cal.getTime() : sdf.parse(startDateStr);
        } catch (ParseException e) { start = cal.getTime(); }
        // 标准化到 0 点 / 23:59:59
        Calendar c1 = Calendar.getInstance(); c1.setTime(start);
        c1.set(Calendar.HOUR_OF_DAY,0); c1.set(Calendar.MINUTE,0); c1.set(Calendar.SECOND,0); c1.set(Calendar.MILLISECOND,0);
        Calendar c2 = Calendar.getInstance(); c2.setTime(end);
        c2.set(Calendar.HOUR_OF_DAY,23); c2.set(Calendar.MINUTE,59); c2.set(Calendar.SECOND,59); c2.set(Calendar.MILLISECOND,999);
        return new Date[]{ c1.getTime(), c2.getTime() };
    }

    private Map<String, Object> buildQuery(Date[] range, String ip, String ipRegion, Integer isChina,
                                           String deviceType, String eventType, String keyword, String pagePath) {
        Map<String, Object> q = new HashMap<>();
        q.put("startTime", range[0]);
        q.put("endTime",   range[1]);
        Calendar c1 = Calendar.getInstance(); c1.setTime(range[0]);
        Calendar c2 = Calendar.getInstance(); c2.setTime(range[1]);
        c1.set(Calendar.HOUR_OF_DAY,0); c1.set(Calendar.MINUTE,0); c1.set(Calendar.SECOND,0); c1.set(Calendar.MILLISECOND,0);
        c2.set(Calendar.HOUR_OF_DAY,0); c2.set(Calendar.MINUTE,0); c2.set(Calendar.SECOND,0); c2.set(Calendar.MILLISECOND,0);
        q.put("startDate", c1.getTime());
        q.put("endDate",   c2.getTime());
        if (ip != null && !ip.isEmpty())                 q.put("ip", ip);
        if (ipRegion != null && !ipRegion.isEmpty())     q.put("ipRegion", ipRegion);
        if (isChina != null)                              q.put("isChina", isChina);
        if (deviceType != null && !deviceType.isEmpty()) q.put("deviceType", deviceType);
        if (eventType != null && !eventType.isEmpty())   q.put("eventType", eventType);
        if (keyword != null && !keyword.isEmpty())       q.put("keyword", keyword);
        if (pagePath != null && !pagePath.isEmpty())     q.put("pagePath", pagePath);
        return q;
    }
}
