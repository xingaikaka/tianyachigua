package com.ruoyi.chigua.dto;

import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import java.util.List;

/**
 * 包含URL信息的视频DTO
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
public class VideoWithUrlsDto extends Video 
{
    private static final long serialVersionUID = 1L;

    /** 生成的视频播放URL (MP4) */
    private String videoUrl;

    /** 生成的M3U8播放URL (HLS) */
    private String m3u8Url;

    /** 生成的封面图片URL */
    private String coverUrl;

    /** 生成的缩略图URL列表 */
    private List<String> thumbnailUrls;

    /** 视频播放器HTML代码 */
    private String videoPlayerHtml;

    /** 图片画廊HTML代码 */
    private String imageGalleryHtml;

    /** 技术信息HTML代码 */
    private String technicalInfoHtml;

    /** 完整的富文本内容（包含播放器+画廊+技术信息） */
    private String enhancedVideoContent;

    /** URL可用性状态 */
    private Boolean videoUrlAvailable;
    private Boolean m3u8UrlAvailable;

    public VideoWithUrlsDto() {
        super();
    }

    public VideoWithUrlsDto(Video video) {
        // 复制所有基础属性
        this.setId(video.getId());
        this.setTitle(video.getTitle());
        this.setSubtitle(video.getSubtitle());
        this.setDescription(video.getDescription());
        this.setVideoContent(video.getVideoContent());
        this.setCoverImage(video.getCoverImage());
        this.setDuration(video.getDuration());
        this.setFileSize(video.getFileSize());
        this.setAuthor(video.getAuthor());
        this.setCategoryId(video.getCategoryId());
        this.setViewCount(video.getViewCount());
        this.setCommentCount(video.getCommentCount());
        this.setLikeCount(video.getLikeCount());
        this.setShareCount(video.getShareCount());
        this.setSortOrder(video.getSortOrder());
        this.setIsRecommended(video.getIsRecommended());
        this.setIsHot(video.getIsHot());
        this.setStatus(video.getStatus());
        this.setPublishedAt(video.getPublishedAt());
        this.setLastEditedAt(video.getLastEditedAt());
        this.setCreatedAt(video.getCreatedAt());
        this.setUpdatedAt(video.getUpdatedAt());

        // 复制ppvod字段
        this.setTranscodeId(video.getTranscodeId());
        this.setMd5(video.getMd5());
        this.setShareid(video.getShareid());
        this.setOrgfile(video.getOrgfile());
        this.setRpath(video.getRpath());
        this.setDomain(video.getDomain());
        this.setPath(video.getPath());
        this.setSuffix(video.getSuffix());
        this.setResolution(video.getResolution());
        this.setBitrate(video.getBitrate());
        this.setFps(video.getFps());
        this.setWidth(video.getWidth());
        this.setHeight(video.getHeight());
        this.setTranscodeStatus(video.getTranscodeStatus());
        this.setThumbnails(video.getThumbnails());
        this.setOutputFormats(video.getOutputFormats());

        // 复制SEO字段
        this.setSlug(video.getSlug());
        this.setMetaTitle(video.getMetaTitle());
        this.setMetaDescription(video.getMetaDescription());
        this.setMetaKeywords(video.getMetaKeywords());

        // 复制内容管理字段
        this.setVideoType(video.getVideoType());
        this.setQuality(video.getQuality());
        this.setIsFeatured(video.getIsFeatured());
        this.setAllowComments(video.getAllowComments());
        this.setLanguage(video.getLanguage());

        // 复制关联信息
        this.setCategories(video.getCategories());
        this.setTags(video.getTags());
        this.setCategoryIds(video.getCategoryIds());
        this.setTagIds(video.getTagIds());
        this.setCategoryName(video.getCategoryName());
        this.setImageCount(video.getImageCount());
        this.setUrlCount(video.getUrlCount());
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getM3u8Url() {
        return m3u8Url;
    }

    public void setM3u8Url(String m3u8Url) {
        this.m3u8Url = m3u8Url;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public List<String> getThumbnailUrls() {
        return thumbnailUrls;
    }

    public void setThumbnailUrls(List<String> thumbnailUrls) {
        this.thumbnailUrls = thumbnailUrls;
    }

    public String getVideoPlayerHtml() {
        return videoPlayerHtml;
    }

    public void setVideoPlayerHtml(String videoPlayerHtml) {
        this.videoPlayerHtml = videoPlayerHtml;
    }

    public String getImageGalleryHtml() {
        return imageGalleryHtml;
    }

    public void setImageGalleryHtml(String imageGalleryHtml) {
        this.imageGalleryHtml = imageGalleryHtml;
    }

    public String getTechnicalInfoHtml() {
        return technicalInfoHtml;
    }

    public void setTechnicalInfoHtml(String technicalInfoHtml) {
        this.technicalInfoHtml = technicalInfoHtml;
    }

    public String getEnhancedVideoContent() {
        return enhancedVideoContent;
    }

    public void setEnhancedVideoContent(String enhancedVideoContent) {
        this.enhancedVideoContent = enhancedVideoContent;
    }

    public Boolean getVideoUrlAvailable() {
        return videoUrlAvailable;
    }

    public void setVideoUrlAvailable(Boolean videoUrlAvailable) {
        this.videoUrlAvailable = videoUrlAvailable;
    }

    public Boolean getM3u8UrlAvailable() {
        return m3u8UrlAvailable;
    }

    public void setM3u8UrlAvailable(Boolean m3u8UrlAvailable) {
        this.m3u8UrlAvailable = m3u8UrlAvailable;
    }
} 