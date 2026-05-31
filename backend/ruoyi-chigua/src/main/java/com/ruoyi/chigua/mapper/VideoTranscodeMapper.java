package com.ruoyi.chigua.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.chigua.domain.VideoTranscode;

/**
 * 视频转码记录Mapper接口
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
public interface VideoTranscodeMapper 
{
    /**
     * 查询视频转码记录
     * 
     * @param id 视频转码记录主键
     * @return 视频转码记录
     */
    public VideoTranscode selectVideoTranscodeById(Long id);

    /**
     * 根据转码ID查询视频转码记录
     * 
     * @param transcodeId 转码ID
     * @return 视频转码记录
     */
    public VideoTranscode selectVideoTranscodeByTranscodeId(String transcodeId);

    /**
     * 查询视频转码记录列表
     * 
     * @param videoTranscode 视频转码记录
     * @return 视频转码记录集合
     */
    public List<VideoTranscode> selectVideoTranscodeList(VideoTranscode videoTranscode);

    /**
     * 新增视频转码记录
     * 
     * @param videoTranscode 视频转码记录
     * @return 结果
     */
    public int insertVideoTranscode(VideoTranscode videoTranscode);

    /**
     * 修改视频转码记录
     * 
     * @param videoTranscode 视频转码记录
     * @return 结果
     */
    public int updateVideoTranscode(VideoTranscode videoTranscode);

    /**
     * 删除视频转码记录
     * 
     * @param id 视频转码记录主键
     * @return 结果
     */
    public int deleteVideoTranscodeById(Long id);

    /**
     * 批量删除视频转码记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVideoTranscodeByIds(Long[] ids);

    /**
     * 查询未使用的转码记录
     * 
     * @param videoTranscode 查询条件
     * @return 转码记录集合
     */
    public List<VideoTranscode> selectUnusedTranscodes(VideoTranscode videoTranscode);

    /**
     * 查询所有转码记录并标记使用状态
     * 
     * @param videoTranscode 查询条件
     * @return 转码记录集合（包含使用状态标记）
     */
    public List<VideoTranscode> selectAllTranscodesWithUsageStatus(VideoTranscode videoTranscode);

    /**
     * 标记转码记录为已使用
     * 
     * @param transcodeId 转码ID
     * @param usedVideoId 关联的视频ID
     * @return 结果
     */
    public int markTranscodeAsUsed(@Param("transcodeId") String transcodeId, @Param("usedVideoId") Long usedVideoId);

    /**
     * 测试查询：检查关联关系
     * 
     * @return 统计结果
     */
    public List<java.util.Map<String, Object>> testAssociationQuery();
} 