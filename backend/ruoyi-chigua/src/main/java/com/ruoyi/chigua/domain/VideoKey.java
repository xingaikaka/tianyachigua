package com.ruoyi.chigua.domain;

import lombok.Data;
import java.util.Date;

/**
 * 视频 AES-128 加密密钥实体
 * 对应 video_keys 表
 */
@Data
public class VideoKey {

    /** 主键 */
    private Long id;

    /** 关联 videos 表的 id */
    private Long videoId;

    /** 爬虫来源唯一标识（如 missav_start-408） */
    private String sourceId;

    /**
     * AES-128 密钥原始字节（16字节明文）
     * 数据库字段类型：VARBINARY(16)
     */
    private byte[] keyData;

    /**
     * IV 十六进制字符串，从 m3u8 EXT-X-KEY 行提取
     * 示例：0x419b2bcf93abbc847c7e73cf5b3c7eef
     */
    private String keyIv;

    /** 创建时间 */
    private Date createdAt;
}
