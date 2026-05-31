package com.ruoyi.chigua.service;

import java.util.List;
import com.ruoyi.chigua.domain.VideoTranscode;
import com.ruoyi.chigua.dto.PpvodCallbackDto;

/**
 * 视频转码记录Service接口
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
public interface IVideoTranscodeService 
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
     * 批量删除视频转码记录
     * 
     * @param ids 需要删除的视频转码记录主键集合
     * @return 结果
     */
    public int deleteVideoTranscodeByIds(Long[] ids);

    /**
     * 删除视频转码记录信息
     * 
     * @param id 视频转码记录主键
     * @return 结果
     */
    public int deleteVideoTranscodeById(Long id);

    /**
     * 从ppvod回调数据创建转码记录
     * 
     * @param callbackData ppvod回调数据
     * @return 转码记录
     */
    public VideoTranscode createFromPpvodCallback(PpvodCallbackDto callbackData);

    /**
     * 保存ppvod回调数据到转码记录
     * 
     * @param callbackData ppvod回调数据
     * @return 结果
     */
    public int savePpvodCallback(PpvodCallbackDto callbackData);

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
    public int markTranscodeAsUsed(String transcodeId, Long usedVideoId);

    /**
     * 为富文本生成视频HTML代码
     * 
     * @param transcodeId 转码ID
     * @return 视频HTML代码
     */
    public String generateVideoHtmlForRichText(String transcodeId);

    /**
     * 更新富文本中的视频URL签名
     * 
     * @param richContent 富文本内容
     * @return 更新签名后的富文本内容
     */
    public String updateVideoSignaturesInRichText(String richContent);

    /**
     * 获取转码视频播放URL（带签名）
     * 
     * @param transcodeId 转码ID
     * @return 视频播放URL
     */
    public String getVideoUrlForRichText(String transcodeId);

    /**
     * 获取转码视频封面URL（带签名）
     * 
     * @param transcodeId 转码ID
     * @return 视频封面URL
     */
    public String getPosterUrlForRichText(String transcodeId);
} 