package com.ruoyi.chigua.domain.vo;

/**
 * 简单的TOP视频统计项
 */
public class TopVideoItem {
    private Long videoId;
    private String title;
    private String author;
    private Long categoryId;
    private String categoryName;
    private Long playCount;
    private Long plays; // 播放量（兼容旧接口）
    private Integer rank;
    private Long growthCount; // 相比昨日的播放量增长值（上升Top专用）

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long playCount) {
        this.playCount = playCount;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getPlays() {
        return plays;
    }

    public void setPlays(Long plays) {
        this.plays = plays;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Long getGrowthCount() {
        return growthCount;
    }

    public void setGrowthCount(Long growthCount) {
        this.growthCount = growthCount;
    }
}


