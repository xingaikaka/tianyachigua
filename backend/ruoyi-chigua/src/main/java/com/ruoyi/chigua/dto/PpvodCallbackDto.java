package com.ruoyi.chigua.dto;

import java.util.List;

/**
 * ppvod回调数据传输对象
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
public class PpvodCallbackDto 
{
    /** 转码任务ID */
    private String transcodeId;
    
    /** 文件MD5值 */
    private String md5;
    
    /** 分享ID */
    private String shareid;
    
    /** 原始文件名 */
    private String orgfile;
    
    /** 原始文件名（用于生成标题） */
    private String originalFilename;
    
    /** 相对路径 */
    private String rpath;
    
    /** 域名 */
    private String domain;
    
    /** 文件路径 */
    private String path;
    
    /** 文件后缀 */
    private String suffix;
    
    /** 分辨率 */
    private String resolution;
    
    /** 码率 */
    private Integer bitrate;
    
    /** 帧率 */
    private Double fps;
    
    /** 视频宽度 */
    private Integer width;
    
    /** 视频高度 */
    private Integer height;
    
    /** 转码结果状态 */
    private String transcodeResult;
    
    /** 文件大小 */
    private Long fileSize;
    
    /** 视频时长（秒） */
    private Integer duration;
    
    /** 缩略图列表 */
    private List<String> thumbnails;
    
    /** 输出格式列表 */
    private List<String> outputFormats;
    
    /** 转码开始时间 */
    private String transcodeBegin;
    
    /** 转码结束时间 */
    private String transcodeEnd;
    
    /** 用户ID */
    private String uid;
    
    /** 视频类型 */
    private String videoType;

    public String getTranscodeId() {
        return transcodeId;
    }

    public void setTranscodeId(String transcodeId) {
        this.transcodeId = transcodeId;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getShareid() {
        return shareid;
    }

    public void setShareid(String shareid) {
        this.shareid = shareid;
    }

    public String getOrgfile() {
        return orgfile;
    }

    public void setOrgfile(String orgfile) {
        this.orgfile = orgfile;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getRpath() {
        return rpath;
    }

    public void setRpath(String rpath) {
        this.rpath = rpath;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Double getFps() {
        return fps;
    }

    public void setFps(Double fps) {
        this.fps = fps;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getTranscodeResult() {
        return transcodeResult;
    }

    public void setTranscodeResult(String transcodeResult) {
        this.transcodeResult = transcodeResult;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public List<String> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<String> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public List<String> getOutputFormats() {
        return outputFormats;
    }

    public void setOutputFormats(List<String> outputFormats) {
        this.outputFormats = outputFormats;
    }

    public String getTranscodeBegin() {
        return transcodeBegin;
    }

    public void setTranscodeBegin(String transcodeBegin) {
        this.transcodeBegin = transcodeBegin;
    }

    public String getTranscodeEnd() {
        return transcodeEnd;
    }

    public void setTranscodeEnd(String transcodeEnd) {
        this.transcodeEnd = transcodeEnd;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    @Override
    public String toString() {
        return "PpvodCallbackDto{" +
                "transcodeId='" + transcodeId + '\'' +
                ", md5='" + md5 + '\'' +
                ", shareid='" + shareid + '\'' +
                ", orgfile='" + orgfile + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", rpath='" + rpath + '\'' +
                ", domain='" + domain + '\'' +
                ", path='" + path + '\'' +
                ", suffix='" + suffix + '\'' +
                ", resolution='" + resolution + '\'' +
                ", bitrate=" + bitrate +
                ", fps=" + fps +
                ", width=" + width +
                ", height=" + height +
                ", transcodeResult='" + transcodeResult + '\'' +
                ", fileSize=" + fileSize +
                ", duration=" + duration +
                ", thumbnails=" + thumbnails +
                ", outputFormats=" + outputFormats +
                ", transcodeBegin='" + transcodeBegin + '\'' +
                ", transcodeEnd='" + transcodeEnd + '\'' +
                ", uid='" + uid + '\'' +
                ", videoType='" + videoType + '\'' +
                '}';
    }
} 