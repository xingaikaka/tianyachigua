package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.VideoStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 视频统计Mapper接口
 * 
 * @author ruoyi
 * @date 2024-01-15
 */
@Mapper
public interface VideoStatsMapper 
{
    /**
     * 查询视频统计
     * 
     * @param id 视频统计主键
     * @return 视频统计
     */
    public VideoStats selectVideoStatsById(Long id);

    /**
     * 根据视频ID查询统计信息
     * 
     * @param videoId 视频ID
     * @return 视频统计
     */
    public VideoStats selectVideoStatsByVideoId(Long videoId);

    /**
     * 查询视频统计列表
     * 
     * @param videoStats 视频统计
     * @return 视频统计集合
     */
    public List<VideoStats> selectVideoStatsList(VideoStats videoStats);

    /**
     * 新增视频统计
     * 
     * @param videoStats 视频统计
     * @return 结果
     */
    public int insertVideoStats(VideoStats videoStats);

    /**
     * 修改视频统计
     * 
     * @param videoStats 视频统计
     * @return 结果
     */
    public int updateVideoStats(VideoStats videoStats);

    /**
     * 删除视频统计
     * 
     * @param id 视频统计主键
     * @return 结果
     */
    public int deleteVideoStatsById(Long id);

    /**
     * 批量删除视频统计
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVideoStatsByIds(Long[] ids);

    /**
     * 增加浏览量（如果记录不存在则创建）
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int incrementViewCount(@Param("videoId") Long videoId);

    /**
     * 增加播放量（如果记录不存在则创建）
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int incrementPlayCount(@Param("videoId") Long videoId);

    /**
     * 增加点赞数量
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int incrementLikeCount(@Param("videoId") Long videoId);

    /**
     * 减少点赞数量
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int decrementLikeCount(@Param("videoId") Long videoId);

    /**
     * 增加分享次数
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int incrementShareCount(@Param("videoId") Long videoId);

    /**
     * 更新评论数量
     * 
     * @param videoId 视频ID
     * @param commentCount 评论数量
     * @return 结果
     */
    public int updateCommentCount(@Param("videoId") Long videoId, @Param("commentCount") Integer commentCount);

    /**
     * 批量增加浏览量（Redis同步专用）
     * 
     * @param videoId 视频ID
     * @param count 增加的数量
     * @return 结果
     */
    public int batchIncrementViewCount(@Param("videoId") Long videoId, @Param("count") Long count);

    /**
     * 批量增加播放量（Redis同步专用）
     * 
     * @param videoId 视频ID
     * @param count 增加的数量
     * @return 结果
     */
    public int batchIncrementPlayCount(@Param("videoId") Long videoId, @Param("count") Long count);
}