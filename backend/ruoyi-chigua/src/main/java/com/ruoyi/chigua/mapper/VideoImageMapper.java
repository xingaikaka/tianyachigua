package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.VideoImage;
import org.apache.ibatis.annotations.Param;

/**
 * 视频图片管理 数据层
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface VideoImageMapper
{
    /**
     * 查询视频图片
     * 
     * @param id 视频图片主键
     * @return 视频图片
     */
    public VideoImage selectVideoImageById(Long id);

    /**
     * 查询视频图片列表
     * 
     * @param videoImage 视频图片
     * @return 视频图片集合
     */
    public List<VideoImage> selectVideoImageList(VideoImage videoImage);

    /**
     * 根据视频ID查询图片列表
     * 
     * @param videoId 视频ID
     * @return 视频图片集合
     */
    public List<VideoImage> selectVideoImagesByVideoId(Long videoId);

    /**
     * 新增视频图片
     * 
     * @param videoImage 视频图片
     * @return 结果
     */
    public int insertVideoImage(VideoImage videoImage);

    /**
     * 修改视频图片
     * 
     * @param videoImage 视频图片
     * @return 结果
     */
    public int updateVideoImage(VideoImage videoImage);

    /**
     * 删除视频图片
     * 
     * @param id 视频图片主键
     * @return 结果
     */
    public int deleteVideoImageById(Long id);

    /**
     * 批量删除视频图片
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVideoImageByIds(Long[] ids);

    /**
     * 根据视频ID删除所有图片
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int deleteVideoImagesByVideoId(Long videoId);

    /**
     * 设置主图（取消其他主图状态）
     * 
     * @param videoId 视频ID
     * @param imageId 图片ID
     * @return 结果
     */
    public int setPrimaryImage(@Param("videoId") Long videoId, @Param("imageId") Long imageId);

    /**
     * 取消视频的所有主图状态
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int clearPrimaryImages(Long videoId);

    /**
     * 查询视频的主图
     * 
     * @param videoId 视频ID
     * @return 视频图片
     */
    public VideoImage selectPrimaryImageByVideoId(Long videoId);

    /**
     * 统计视频图片数量
     * 
     * @param videoId 视频ID
     * @return 图片数量
     */
    public int countImagesByVideoId(Long videoId);

    /**
     * 批量更新图片排序
     * 
     * @param images 图片列表
     * @return 结果
     */
    public int batchUpdateImageSort(List<VideoImage> images);

    /**
     * 检查图片URL是否已存在
     * 
     * @param imageUrl 图片URL
     * @param videoId 视频ID
     * @param excludeId 排除的图片ID（用于编辑时检查）
     * @return 图片信息
     */
    public VideoImage checkImageUrlExists(@Param("imageUrl") String imageUrl, @Param("videoId") Long videoId, @Param("excludeId") Long excludeId);
} 