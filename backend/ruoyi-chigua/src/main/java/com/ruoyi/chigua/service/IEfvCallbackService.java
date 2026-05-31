package com.ruoyi.chigua.service;

import com.ruoyi.chigua.dto.EfvCallbackDto;

/**
 * EFV回调处理Service接口
 * 
 * @author ruoyi
 * @date 2025-01-22
 */
public interface IEfvCallbackService 
{
    /**
     * 处理EFV通用回调（自动识别视频或剧集类型）
     * 
     * @param callbackData EFV回调数据
     * @return 处理结果
     */
    boolean handleCallback(EfvCallbackDto callbackData);

    /**
     * 处理EFV单个视频回调
     * 
     * @param callbackData EFV回调数据
     * @return 处理结果
     */
    boolean handleVideoCallback(EfvCallbackDto callbackData);

    /**
     * 处理EFV剧集回调
     * 
     * @param callbackData EFV回调数据
     * @return 处理结果
     */
    boolean handleSeriesCallback(EfvCallbackDto callbackData);

    /**
     * 将EFV回调数据保存到video_transcodes表
     * 
     * @param callbackData EFV回调数据
     * @return 保存结果
     */
    boolean saveToVideoTranscodes(EfvCallbackDto callbackData);

    /**
     * 处理EFV视频回调并自动更新富文本内容 - 新增功能
     * 保持现有业务不变，在保存转码记录的基础上，根据视频名称查询videos表并更新富文本内容
     * 
     * @param callbackData EFV回调数据
     * @return 处理结果信息
     */
    String handleVideoCallbackWithContentUpdate(EfvCallbackDto callbackData);
}