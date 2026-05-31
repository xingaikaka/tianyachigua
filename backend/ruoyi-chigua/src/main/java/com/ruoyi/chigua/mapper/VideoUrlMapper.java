package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.VideoUrl;
import org.apache.ibatis.annotations.Param;

/**
 * 视频地址管理 数据层
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface VideoUrlMapper
{
    /**
     * 查询视频地址
     * 
     * @param id 视频地址主键
     * @return 视频地址
     */
    public VideoUrl selectVideoUrlById(Long id);

    /**
     * 查询视频地址列表
     * 
     * @param videoUrl 视频地址
     * @return 视频地址集合
     */
    public List<VideoUrl> selectVideoUrlList(VideoUrl videoUrl);

    /**
     * 根据视频ID查询地址列表
     * 
     * @param videoId 视频ID
     * @return 视频地址集合
     */
    public List<VideoUrl> selectVideoUrlsByVideoId(Long videoId);

    /**
     * 新增视频地址
     * 
     * @param videoUrl 视频地址
     * @return 结果
     */
    public int insertVideoUrl(VideoUrl videoUrl);

    /**
     * 修改视频地址
     * 
     * @param videoUrl 视频地址
     * @return 结果
     */
    public int updateVideoUrl(VideoUrl videoUrl);

    /**
     * 删除视频地址
     * 
     * @param id 视频地址主键
     * @return 结果
     */
    public int deleteVideoUrlById(Long id);

    /**
     * 批量删除视频地址
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVideoUrlByIds(Long[] ids);

    /**
     * 根据视频ID删除所有地址
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int deleteVideoUrlsByVideoId(Long videoId);

    /**
     * 设置主要地址（取消其他主要地址状态）
     * 
     * @param videoId 视频ID
     * @param urlId 地址ID
     * @return 结果
     */
    public int setPrimaryUrl(@Param("videoId") Long videoId, @Param("urlId") Long urlId);

    /**
     * 取消视频的所有主要地址状态
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int clearPrimaryUrls(Long videoId);

    /**
     * 查询视频的主要地址
     * 
     * @param videoId 视频ID
     * @return 视频地址
     */
    public VideoUrl selectPrimaryUrlByVideoId(Long videoId);

    /**
     * 统计视频地址数量
     * 
     * @param videoId 视频ID
     * @return 地址数量
     */
    public int countUrlsByVideoId(Long videoId);

    /**
     * 批量更新地址排序
     * 
     * @param urls 地址列表
     * @return 结果
     */
    public int batchUpdateUrlSort(List<VideoUrl> urls);

    /**
     * 检查播放地址是否已存在
     * 
     * @param videoUrl 播放地址
     * @param videoId 视频ID
     * @param excludeId 排除的地址ID（用于编辑时检查）
     * @return 地址信息
     */
    public VideoUrl checkVideoUrlExists(@Param("videoUrl") String videoUrl, @Param("videoId") Long videoId, @Param("excludeId") Long excludeId);

    /**
     * 更新播放次数
     * 
     * @param id 地址ID
     * @return 结果
     */
    public int incrementPlayCount(Long id);

    /**
     * 根据清晰度查询地址列表
     * 
     * @param videoId 视频ID
     * @param quality 清晰度
     * @return 视频地址集合
     */
    public List<VideoUrl> selectVideoUrlsByQuality(@Param("videoId") Long videoId, @Param("quality") String quality);

    /**
     * 根据格式查询地址列表
     * 
     * @param videoId 视频ID
     * @param format 视频格式
     * @return 视频地址集合
     */
    public List<VideoUrl> selectVideoUrlsByFormat(@Param("videoId") Long videoId, @Param("format") String format);
} 