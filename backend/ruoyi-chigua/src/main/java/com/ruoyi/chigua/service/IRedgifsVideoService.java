package com.ruoyi.chigua.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.RedgifsVideo;

/**
 * RedGifs视频信息Service接口
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public interface IRedgifsVideoService 
{
    /**
     * 查询RedGifs视频信息
     * 
     * @param id RedGifs视频信息主键
     * @return RedGifs视频信息
     */
    public RedgifsVideo selectRedGifsVideoById(Long id);

    /**
     * 查询RedGifs视频信息列表
     * 
     * @param redgifsVideo RedGifs视频信息
     * @return RedGifs视频信息集合
     */
    public List<RedgifsVideo> selectRedGifsVideoList(RedgifsVideo redgifsVideo);

    /**
     * 新增RedGifs视频信息
     * 
     * @param redgifsVideo RedGifs视频信息
     * @return 结果
     */
    public int insertRedGifsVideo(RedgifsVideo redgifsVideo);

    /**
     * 修改RedGifs视频信息
     * 
     * @param redgifsVideo RedGifs视频信息
     * @return 结果
     */
    public int updateRedGifsVideo(RedgifsVideo redgifsVideo);

    /**
     * 批量删除RedGifs视频信息
     * 
     * @param ids 需要删除的RedGifs视频信息主键集合
     * @return 结果
     */
    public int deleteRedGifsVideoByIds(Long[] ids);

    /**
     * 删除RedGifs视频信息信息
     * 
     * @param id RedGifs视频信息主键
     * @return 结果
     */
    public int deleteRedGifsVideoById(Long id);

    /**
     * 根据gifId查询RedGifs视频信息
     * @param gifId RedGifs视频ID
     * @return RedGifs视频信息
     */
    RedgifsVideo selectRedGifsVideoByGifId(String gifId);

    /**
     * 批量检查视频是否存在
     * @param gifIds 视频ID列表
     * @return Map<String, Map<String, Object>> 包含gifId, exists, videoId
     */
    Map<String, Map<String, Object>> checkRedgifsVideosExist(List<String> gifIds);

    /**
     * 入库RedGifs视频
     * @param redgifsVideo 视频信息
     * @return Map<String, Object> 包含success, videoId, exists, message
     */
    Map<String, Object> ingestRedgifsVideo(RedgifsVideo redgifsVideo);

    /**
     * 将视频封面（poster_url 原始路径）同步为所属用户的头像
     * @param videoId 视频主键ID
     * @return Map 包含 success, message
     */
    Map<String, Object> syncPosterToUserAvatar(Long videoId);
}
