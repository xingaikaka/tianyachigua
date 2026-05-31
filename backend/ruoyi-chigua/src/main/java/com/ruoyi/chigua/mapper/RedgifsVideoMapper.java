package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.RedgifsVideo;
import org.apache.ibatis.annotations.Param;

/**
 * RedGifs视频信息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public interface RedgifsVideoMapper 
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
     * 删除RedGifs视频信息
     * 
     * @param id RedGifs视频信息主键
     * @return 结果
     */
    public int deleteRedGifsVideoById(Long id);

    /**
     * 批量删除RedGifs视频信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRedGifsVideoByIds(Long[] ids);

    /**
     * 根据gifId查询RedGifs视频信息
     * @param gifId RedGifs视频ID
     * @return RedGifs视频信息
     */
    public RedgifsVideo selectRedGifsVideoByGifId(String gifId);

    /**
     * 批量查询视频是否存在
     * @param gifIds 视频ID列表
     * @return 存在的视频列表
     */
    public List<RedgifsVideo> selectRedGifsVideosByGifIds(@Param("gifIds") List<String> gifIds);

    /**
     * 更新视频同步状态
     * @param gifId 视频ID
     * @param syncStatus 同步状态
     * @return 结果
     */
    int updateRedGifsVideoSyncStatus(@Param("gifId") String gifId, @Param("syncStatus") Integer syncStatus);
}
