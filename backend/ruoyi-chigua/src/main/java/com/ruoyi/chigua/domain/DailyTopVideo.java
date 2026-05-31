package com.ruoyi.chigua.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日Top视频对象
 * 
 * @author ruoyi
 * @date 2025-08-25
 */
public class DailyTopVideo {
    
    /** 视频ID */
    private Long videoId;
    
    /** 视频标题 */
    private String title;
    
    /** 作者 */
    private String author;
    
    /** 分类ID */
    private Long categoryId;
    
    /** 分类名称 */
    private String categoryName;
    
    /** 当日播放量 */
    private Integer dailyPlayCount;
    
    /** 总播放量 */
    private Integer totalPlayCount;
    
    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
    
    /** 播放日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate playDate;
    
    /** 当日首次播放时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstPlayAt;
    
    /** 最后播放时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPlayAt;
    
    /** 排名（可选，用于显示） */
    private Integer rank;
    
    /** 相比昨日的播放量增长值（上升Top专用） */
    private Integer growthCount;
    
    public DailyTopVideo() {}
    
    public DailyTopVideo(Long videoId, String title, String author) {
        this.videoId = videoId;
        this.title = title;
        this.author = author;
    }
    
    // Getters and Setters
    public Long getVideoId() {
        return videoId;
    }
    
    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public Integer getDailyPlayCount() {
        return dailyPlayCount;
    }
    
    public void setDailyPlayCount(Integer dailyPlayCount) {
        this.dailyPlayCount = dailyPlayCount;
    }
    
    public Integer getTotalPlayCount() {
        return totalPlayCount;
    }
    
    public void setTotalPlayCount(Integer totalPlayCount) {
        this.totalPlayCount = totalPlayCount;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public LocalDate getPlayDate() {
        return playDate;
    }
    
    public void setPlayDate(LocalDate playDate) {
        this.playDate = playDate;
    }
    
    public LocalDateTime getFirstPlayAt() {
        return firstPlayAt;
    }
    
    public void setFirstPlayAt(LocalDateTime firstPlayAt) {
        this.firstPlayAt = firstPlayAt;
    }
    
    public LocalDateTime getLastPlayAt() {
        return lastPlayAt;
    }
    
    public void setLastPlayAt(LocalDateTime lastPlayAt) {
        this.lastPlayAt = lastPlayAt;
    }
    
    public Integer getRank() {
        return rank;
    }
    
    public void setRank(Integer rank) {
        this.rank = rank;
    }
    
    public Integer getGrowthCount() {
        return growthCount;
    }
    
    public void setGrowthCount(Integer growthCount) {
        this.growthCount = growthCount;
    }
    
    @Override
    public String toString() {
        return "DailyTopVideo{" +
                "videoId=" + videoId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", dailyPlayCount=" + dailyPlayCount +
                ", totalPlayCount=" + totalPlayCount +
                ", publishedAt=" + publishedAt +
                ", playDate=" + playDate +
                ", firstPlayAt=" + firstPlayAt +
                ", lastPlayAt=" + lastPlayAt +
                ", rank=" + rank +
                '}';
    }
}
