package com.ruoyi.chigua.domain.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 51吃瓜视频入库请求DTO
 */
@Data
public class Cg51IngestReq {
    /** 视频标题 */
    private String title;
    
    /** 视频来源ID（用于标识爬虫来源的视频唯一标识） */
    private String sourceId;
    
    /** 视频描述 */
    private String description;
    
    /** 封面图片R2资源键 */
    private String coverImage;
    
    /** 视频内容（富文本HTML） */
    private String videoContent;
    
    /** 第一个视频的R2 URL（用于first_video_url字段） */
    private String firstVideoUrl;

    /** 视频首帧图片URL */
    private String firstFrameUrl;
    
    /** 视频URL映射 {原URL: R2 URL} */
    private Map<String, String> videoUrlMapping;
    
    /** 图片URL映射 {原URL: R2资源键} */
    private Map<String, String> imageUrlMapping;
    
    /** 原始HTML内容 */
    private String contentHtml;
    
    /** 主分类ID */
    private Long categoryId;
    
    /** 分类ID列表（支持多个分类） */
    private List<Long> categoryIds;
    
    /** 分类名称列表（用于匹配数据库中的分类） */
    private List<String> categoryNames;
    
    /** 标签列表 */
    private List<String> tags;
    
    /** 作者/用户名 */
    private String username;
    
    /** 视频时长（秒） */
    private Integer duration;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private Date publishedAt;

    /**
     * AES-128 密钥十六进制字符串（32位hex，无前缀）
     * 仅当视频有 #EXT-X-KEY 加密时才传入
     * 示例：aabbccddeeff00112233445566778899
     */
    private String keyHex;

    /**
     * IV 十六进制字符串，从 m3u8 EXT-X-KEY 行提取
     * 示例：0x419b2bcf93abbc847c7e73cf5b3c7eef
     */
    private String keyIv;
}
