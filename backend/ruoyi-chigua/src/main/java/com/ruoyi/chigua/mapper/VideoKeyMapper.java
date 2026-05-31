package com.ruoyi.chigua.mapper;

import com.ruoyi.chigua.domain.VideoKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 视频加密密钥 Mapper
 */
@Mapper
public interface VideoKeyMapper {

    /**
     * 插入密钥记录
     */
    int insertVideoKey(VideoKey videoKey);

    /**
     * 按 videoId 查询密钥
     */
    VideoKey selectByVideoId(@Param("videoId") Long videoId);

    /**
     * 按 sourceId 查询密钥（爬虫写入时 videoId 可能为 null，用 sourceId 检索）
     */
    VideoKey selectBySourceId(@Param("sourceId") String sourceId);
}
