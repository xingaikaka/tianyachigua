package com.ruoyi.chigua.service;

import java.util.List;
import com.ruoyi.chigua.domain.VideoStats;

/**
 * 视频统计Service接口
 * 
 * @author ruoyi
 * @date 2024-01-15
 */
public interface IVideoStatsService 
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
     * 批量删除视频统计
     * 
     * @param ids 需要删除的视频统计主键集合
     * @return 结果
     */
    public int deleteVideoStatsByIds(Long[] ids);

    /**
     * 删除视频统计信息
     * 
     * @param id 视频统计主键
     * @return 结果
     */
    public int deleteVideoStatsById(Long id);

    /**
     * 增加浏览量
     * 
     * @param videoId 视频ID
     * @return 是否成功增加
     */
    public boolean incrementViewCount(Long videoId);

    /**
     * 增加播放量
     * 
     * @param videoId 视频ID
     * @return 是否成功增加
     */
    public boolean incrementPlayCount(Long videoId);

    /**
     * 增加点赞数量
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int incrementLikeCount(Long videoId);

    /**
     * 减少点赞数量
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int decrementLikeCount(Long videoId);

    /**
     * 增加分享次数
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int incrementShareCount(Long videoId);

    /**
     * 同步评论数量（从评论表统计）
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int syncCommentCount(Long videoId);
}