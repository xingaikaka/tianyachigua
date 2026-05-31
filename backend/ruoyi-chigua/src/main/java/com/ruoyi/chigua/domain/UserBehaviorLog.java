package com.ruoyi.chigua.domain;

import java.util.Date;

/**
 * 用户行为流水（前台用户审计）
 *
 * @author chigua
 * @date 2026-05-24
 */
public class UserBehaviorLog {

    /** 主键 */
    private Long id;

    /** 事件发生时间 */
    private Date eventTime;

    /** 事件日期（分区键） */
    private Date eventDate;

    /** 事件类型 */
    private String eventType;

    /** 目标 ID（videoId/categoryId/adId/postId 等） */
    private String eventTarget;

    /** 搜索关键词（仅搜索事件） */
    private String keyword;

    /** 页面路径 */
    private String pagePath;

    /** 来源页面 */
    private String referrer;

    /** 客户端 IP */
    private String ip;

    /** IP 归属地 */
    private String ipRegion;

    /** 是否中国大陆 IP */
    private Integer isChina;

    /** 指纹 MD5(ip|ua) 前 16 位 */
    private String fingerprint;

    /** 前端 localStorage 持久 ID */
    private String anonymousId;

    /** 前端 sessionStorage ID */
    private String sessionId;

    /** User-Agent（截断 255） */
    private String userAgent;

    /** mobile / desktop / tablet */
    private String deviceType;

    /** 浏览器名 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 停留 / 播放时长（毫秒） */
    private Integer durationMs;

    /** JSON 扩展字段 */
    private String extra;

    /** 目标对象的标题（视频名 / 帖子标题 / 分类名，运行时填充，不入库） */
    private String eventTargetTitle;

    public String getEventTargetTitle() { return eventTargetTitle; }
    public void setEventTargetTitle(String eventTargetTitle) { this.eventTargetTitle = eventTargetTitle; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }

    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventTarget() { return eventTarget; }
    public void setEventTarget(String eventTarget) { this.eventTarget = eventTarget; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getPagePath() { return pagePath; }
    public void setPagePath(String pagePath) { this.pagePath = pagePath; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getIpRegion() { return ipRegion; }
    public void setIpRegion(String ipRegion) { this.ipRegion = ipRegion; }

    public Integer getIsChina() { return isChina; }
    public void setIsChina(Integer isChina) { this.isChina = isChina; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getAnonymousId() { return anonymousId; }
    public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }
}
