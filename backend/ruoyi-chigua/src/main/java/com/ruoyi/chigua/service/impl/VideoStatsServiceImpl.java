package com.ruoyi.chigua.service.impl;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.chigua.mapper.VideoStatsMapper;
import com.ruoyi.chigua.mapper.VideoDailyPlaysMapper;
import com.ruoyi.chigua.domain.VideoStats;
import com.ruoyi.chigua.service.IVideoStatsService;

/**
 * 视频统计Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-15
 */
@Service
public class VideoStatsServiceImpl implements IVideoStatsService 
{
    private static final Logger logger = LoggerFactory.getLogger(VideoStatsServiceImpl.class);
    
    @Autowired
    private VideoStatsMapper videoStatsMapper;

    @Autowired
    private VideoDailyPlaysMapper videoDailyPlaysMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    // Redis Key前缀（保留用于其他功能）
    private static final String VIEW_COUNT_KEY = "video:view:count:";
    private static final String PLAY_COUNT_KEY = "video:play:count:";

    /**
     * 查询视频统计
     * 
     * @param id 视频统计主键
     * @return 视频统计
     */
    @Override
    public VideoStats selectVideoStatsById(Long id)
    {
        return videoStatsMapper.selectVideoStatsById(id);
    }

    /**
     * 根据视频ID查询统计信息
     * 
     * @param videoId 视频ID
     * @return 视频统计
     */
    @Override
    public VideoStats selectVideoStatsByVideoId(Long videoId)
    {
        return videoStatsMapper.selectVideoStatsByVideoId(videoId);
    }

    /**
     * 查询视频统计列表
     * 
     * @param videoStats 视频统计
     * @return 视频统计
     */
    @Override
    public List<VideoStats> selectVideoStatsList(VideoStats videoStats)
    {
        return videoStatsMapper.selectVideoStatsList(videoStats);
    }

    /**
     * 新增视频统计
     * 
     * @param videoStats 视频统计
     * @return 结果
     */
    @Override
    public int insertVideoStats(VideoStats videoStats)
    {
        return videoStatsMapper.insertVideoStats(videoStats);
    }

    /**
     * 修改视频统计
     * 
     * @param videoStats 视频统计
     * @return 结果
     */
    @Override
    public int updateVideoStats(VideoStats videoStats)
    {
        return videoStatsMapper.updateVideoStats(videoStats);
    }

    /**
     * 批量删除视频统计
     * 
     * @param ids 需要删除的视频统计主键
     * @return 结果
     */
    @Override
    public int deleteVideoStatsByIds(Long[] ids)
    {
        return videoStatsMapper.deleteVideoStatsByIds(ids);
    }

    /**
     * 删除视频统计信息
     * 
     * @param id 视频统计主键
     * @return 结果
     */
    @Override
    public int deleteVideoStatsById(Long id)
    {
        return videoStatsMapper.deleteVideoStatsById(id);
    }

    /**
     * 增加浏览量（实时写入数据库，无防重复限制）
     * 
     * @param videoId 视频ID
     * @return 是否成功增加
     */
    @Override
    public boolean incrementViewCount(Long videoId)
    {
        try {
            // 🔥 实时写入数据库
            int result = videoStatsMapper.incrementViewCount(videoId);
            
            if (result > 0) {
                logger.debug("浏览量实时写入成功: videoId={}", videoId);
                return true;
            } else {
                logger.warn("浏览量数据库写入失败: videoId={}", videoId);
                return false;
            }
        } catch (Exception e) {
            logger.error("增加浏览量失败: videoId={}, error={}", videoId, e.getMessage());
            return false;
        }
    }

    /**
     * 增加播放量（实时写入数据库，同时记录每日播放数据）
     * 
     * @param videoId 视频ID
     * @return 是否成功增加
     */
    @Override
    @Transactional
    public boolean incrementPlayCount(Long videoId)
    {
        try {
            // 🔥 1. 实时写入数据库
            int result = videoStatsMapper.incrementPlayCount(videoId);
            
            if (result > 0) {
                // 🔥 2. 记录当日播放数据
                recordDailyPlay(videoId);
                
                logger.debug("播放量实时写入成功: videoId={}", videoId);
                return true;
            } else {
                logger.warn("播放量数据库写入失败: videoId={}", videoId);
                return false;
            }
        } catch (Exception e) {
            logger.error("增加播放量失败: videoId={}, error={}", videoId, e.getMessage());
            return false;
        }
    }

    /**
     * 记录当日播放数据
     * 
     * @param videoId 视频ID
     */
    private void recordDailyPlay(Long videoId) {
        try {
            LocalDate today = LocalDate.now();
            int result = videoDailyPlaysMapper.recordDailyPlay(videoId, today);
            logger.debug("记录当日播放数据: videoId={}, date={}, result={}", videoId, today, result);
        } catch (Exception e) {
            logger.error("记录当日播放数据失败: videoId={}, error={}", videoId, e.getMessage());
            // 不抛出异常，避免影响主要的播放量统计
        }
    }

    /**
     * 增加点赞数量
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    public int incrementLikeCount(Long videoId)
    {
        return videoStatsMapper.incrementLikeCount(videoId);
    }

    /**
     * 减少点赞数量
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    public int decrementLikeCount(Long videoId)
    {
        return videoStatsMapper.decrementLikeCount(videoId);
    }

    /**
     * 增加分享次数
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    public int incrementShareCount(Long videoId)
    {
        return videoStatsMapper.incrementShareCount(videoId);
    }

    /**
     * 同步评论数量（从评论表统计）
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    public int syncCommentCount(Long videoId)
    {
        // TODO: 这里需要从评论表统计具体的评论数量
        // 暂时简化实现，实际应该查询comments表
        int commentCount = 0; // commentMapper.countByVideoId(videoId);
        return videoStatsMapper.updateCommentCount(videoId, commentCount);
    }

    // 🔥 已移除定时同步任务，改为实时写入数据库模式
}