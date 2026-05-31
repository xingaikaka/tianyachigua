package com.ruoyi.chigua.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * EFV回调数据DTO
 * 根据实际EFV回调JSON结构定义
 * 
 * @author ruoyi
 * @date 2025-01-22
 */
public class EfvCallbackDto
{
    /** MongoDB ObjectId */
    @JsonProperty("_id")
    private String _id;

    /** 状态 */
    private String status;

    /** 文件大小（字符串格式，如："5497612960"） */
    private String size;

    /** 分类 */
    private String category;

    /** 原始文件名 */
    private String originalname;

    /** 原始标题 */
    private String originaltitle;

    /** 别名 */
    private String aka;

    /** 语言 */
    private String language;

    /** 版本 */
    private String banben;

    /** VTT字幕文件 */
    private String vtt;

    /** 截图数组 */
    private List<String> screenshots;

    /** 海报 */
    private String poster;

    /** 海报2（包含尺寸信息） */
    private Poster2 poster2;

    /** 观看次数 */
    private Integer count;

    /** 是否重试 */
    private Boolean retry;

    /** 是否自动发布 */
    private Boolean autoPublish;

    /** 导演 */
    private List<String> director;

    /** 编剧 */
    private List<String> writer;

    /** 演员 */
    private List<String> stars;

    /** 国家 */
    private List<String> country;

    /** 标签 */
    private List<String> tags;

    /** 文件路径 */
    private String path;

    /** 创建时间 */
    private Date createAt;

    /** M3U8播放列表 */
    private List<M3u8Path> m3u8paths;

    /** 第三方M3U8 */
    private List<String> thirdm3u8;

    /** MongoDB版本字段 */
    @JsonProperty("__v")
    private Integer __v;

    /** 视频高度 */
    private Integer height;

    /** 视频宽度 */
    private Integer width;

    /** 时长（如："97分钟"） */
    private String duration;

    /** 电影路径 */
    private String moviepath;

    /** GIF动图 */
    private String gif;

    /** 预览视频 */
    private String previewvideo;

    /** 首帧截图 */
    private String firstScreen;

    /** 评分 */
    private Double rate;

    /** 年份 */
    private Integer year;

    /** 简介 */
    private String summary;

    /** MD5值 */
    private String md5;

    /**
     * 海报2信息内部类
     */
    public static class Poster2 {
        private String url;
        private Integer height;
        private Integer width;

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
    }

    /**
     * M3U8路径信息内部类
     */
    public static class M3u8Path {
        @JsonProperty("_id")
        private String _id;
        private Integer hd;
        private String path;

        // Getters and Setters
        public String get_id() { return _id; }
        public void set_id(String _id) { this._id = _id; }
        public Integer getHd() { return hd; }
        public void setHd(Integer hd) { this.hd = hd; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    // Getters and Setters
    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOriginalname() {
        return originalname;
    }

    public void setOriginalname(String originalname) {
        this.originalname = originalname;
    }

    public String getOriginaltitle() {
        return originaltitle;
    }

    public void setOriginaltitle(String originaltitle) {
        this.originaltitle = originaltitle;
    }

    public String getAka() {
        return aka;
    }

    public void setAka(String aka) {
        this.aka = aka;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBanben() {
        return banben;
    }

    public void setBanben(String banben) {
        this.banben = banben;
    }

    public String getVtt() {
        return vtt;
    }

    public void setVtt(String vtt) {
        this.vtt = vtt;
    }

    public List<String> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<String> screenshots) {
        this.screenshots = screenshots;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public Poster2 getPoster2() {
        return poster2;
    }

    public void setPoster2(Poster2 poster2) {
        this.poster2 = poster2;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Boolean getRetry() {
        return retry;
    }

    public void setRetry(Boolean retry) {
        this.retry = retry;
    }

    public Boolean getAutoPublish() {
        return autoPublish;
    }

    public void setAutoPublish(Boolean autoPublish) {
        this.autoPublish = autoPublish;
    }

    public List<String> getDirector() {
        return director;
    }

    public void setDirector(List<String> director) {
        this.director = director;
    }

    public List<String> getWriter() {
        return writer;
    }

    public void setWriter(List<String> writer) {
        this.writer = writer;
    }

    public List<String> getStars() {
        return stars;
    }

    public void setStars(List<String> stars) {
        this.stars = stars;
    }

    public List<String> getCountry() {
        return country;
    }

    public void setCountry(List<String> country) {
        this.country = country;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public List<M3u8Path> getM3u8paths() {
        return m3u8paths;
    }

    public void setM3u8paths(List<M3u8Path> m3u8paths) {
        this.m3u8paths = m3u8paths;
    }

    public List<String> getThirdm3u8() {
        return thirdm3u8;
    }

    public void setThirdm3u8(List<String> thirdm3u8) {
        this.thirdm3u8 = thirdm3u8;
    }

    public Integer get__v() {
        return __v;
    }

    public void set__v(Integer __v) {
        this.__v = __v;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getMoviepath() {
        return moviepath;
    }

    public void setMoviepath(String moviepath) {
        this.moviepath = moviepath;
    }

    public String getGif() {
        return gif;
    }

    public void setGif(String gif) {
        this.gif = gif;
    }

    public String getPreviewvideo() {
        return previewvideo;
    }

    public void setPreviewvideo(String previewvideo) {
        this.previewvideo = previewvideo;
    }

    public String getFirstScreen() {
        return firstScreen;
    }

    public void setFirstScreen(String firstScreen) {
        this.firstScreen = firstScreen;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}