package com.ruoyi.chigua.domain.vo.web;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 短视频列表项（仅短视频模式使用）
 */
public class WebShortVideoVO {
    private Long id;
    private String title;
    private String coverImageUrl;
    private List<String> tags; // 标签名列表（兼容旧代码）
    private List<Long> tagIds; // 标签ID列表
    private List<TagInfo> tagList; // 标签对象列表（包含isHot字段）

    /** 解析得到的首个视频URL（签名后或原路径） */
    private String firstVideoUrl;

    /** 首帧图片URL（移动端滑动播放首帧占位，与 coverImageUrl 保持一致，待接入真实首帧字段后可分离） */
    private String firstFrameUrl;

    /** 发布时间（可选显示） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date publishedAt;

    /** 是否热门（1 是，0 否） */
    private Integer isHot;

    /** 是否推荐（1 是，0 否） */
    private Integer isRecommended;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }

    public String getFirstVideoUrl() { return firstVideoUrl; }
    public void setFirstVideoUrl(String firstVideoUrl) { this.firstVideoUrl = firstVideoUrl; }

    public String getFirstFrameUrl() { return firstFrameUrl; }
    public void setFirstFrameUrl(String firstFrameUrl) { this.firstFrameUrl = firstFrameUrl; }

    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }

    public List<TagInfo> getTagList() { return tagList; }
    public void setTagList(List<TagInfo> tagList) { this.tagList = tagList; }

    public Integer getIsHot() { return isHot; }
    public void setIsHot(Integer isHot) { this.isHot = isHot; }

    public Integer getIsRecommended() { return isRecommended; }
    public void setIsRecommended(Integer isRecommended) { this.isRecommended = isRecommended; }

    /**
     * 标签信息内部类（包含isHot字段）
     */
    public static class TagInfo {
        private Long id;
        private String name;
        private Integer isHot;

        public TagInfo() {}

        public TagInfo(Long id, String name, Integer isHot) {
            this.id = id;
            this.name = name;
            this.isHot = isHot;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getIsHot() { return isHot; }
        public void setIsHot(Integer isHot) { this.isHot = isHot; }
    }
}


