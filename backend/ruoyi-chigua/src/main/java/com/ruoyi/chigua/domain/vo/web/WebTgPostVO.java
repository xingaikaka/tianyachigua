package com.ruoyi.chigua.domain.vo.web;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Telegram 帖子 VO（前端展示用）
 */
public class WebTgPostVO
{
    private Long id;

    /** 帖子正文文字（标题/描述/标签） */
    private String caption;

    /** 发帖时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date postDate;

    /** 媒体总数 */
    private Integer mediaCount;

    /** 图片数量 */
    private Integer photoCount;

    /** 视频数量 */
    private Integer videoCount;

    /** 浏览次数 */
    private Integer views;

    /** 是否置顶：1=是 0=否 */
    private Integer isTop;

    /** 媒体列表（按 sort_order 升序） */
    private List<WebTgMediaVO> media;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Date getPostDate() { return postDate; }
    public void setPostDate(Date postDate) { this.postDate = postDate; }

    public Integer getMediaCount() { return mediaCount; }
    public void setMediaCount(Integer mediaCount) { this.mediaCount = mediaCount; }

    public Integer getPhotoCount() { return photoCount; }
    public void setPhotoCount(Integer photoCount) { this.photoCount = photoCount; }

    public Integer getVideoCount() { return videoCount; }
    public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public Integer getIsTop() { return isTop; }
    public void setIsTop(Integer isTop) { this.isTop = isTop; }

    public List<WebTgMediaVO> getMedia() { return media; }
    public void setMedia(List<WebTgMediaVO> media) { this.media = media; }
}
