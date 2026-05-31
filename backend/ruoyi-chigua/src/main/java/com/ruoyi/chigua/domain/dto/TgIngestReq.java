package com.ruoyi.chigua.domain.dto;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * TG 内容入库请求体
 *
 * POST /open/api/tg/ingest
 */
public class TgIngestReq
{
    /** 关联网站分类 ID（必填） */
    private Integer categoryId;

    /** 同步源原始 ID，用于去重（必填） */
    private String sourceId;

    /** 帖子正文（标题/描述/标签，可选） */
    private String caption;

    /** 内容发布时间，不传则取当前时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date postDate;

    /** 排序权重，越大越靠前，默认 0 */
    private Integer sortOrder;

    /** 媒体列表（至少 1 条） */
    private List<TgMediaItem> media;

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Date getPostDate() { return postDate; }
    public void setPostDate(Date postDate) { this.postDate = postDate; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<TgMediaItem> getMedia() { return media; }
    public void setMedia(List<TgMediaItem> media) { this.media = media; }
}
