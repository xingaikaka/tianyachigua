package com.ruoyi.chigua.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.chigua.mapper.VideoImageMapper;
import com.ruoyi.chigua.domain.VideoImage;
import com.ruoyi.chigua.service.IVideoImageService;

/**
 * 视频图片管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class VideoImageServiceImpl implements IVideoImageService 
{
    @Autowired
    private VideoImageMapper videoImageMapper;

    /**
     * 查询视频图片
     * 
     * @param id 视频图片主键
     * @return 视频图片
     */
    @Override
    public VideoImage selectVideoImageById(Long id)
    {
        return videoImageMapper.selectVideoImageById(id);
    }

    /**
     * 查询视频图片列表
     * 
     * @param videoImage 视频图片
     * @return 视频图片
     */
    @Override
    public List<VideoImage> selectVideoImageList(VideoImage videoImage)
    {
        return videoImageMapper.selectVideoImageList(videoImage);
    }

    /**
     * 根据视频ID查询图片列表
     * 
     * @param videoId 视频ID
     * @return 视频图片集合
     */
    @Override
    public List<VideoImage> selectVideoImagesByVideoId(Long videoId)
    {
        return videoImageMapper.selectVideoImagesByVideoId(videoId);
    }

    /**
     * 新增视频图片
     * 
     * @param videoImage 视频图片
     * @return 结果
     */
    @Override
    public int insertVideoImage(VideoImage videoImage)
    {
        // 设置默认值
        if (videoImage.getSortOrder() == null) {
            videoImage.setSortOrder(0);
        }
        if (videoImage.getIsPrimary() == null) {
            videoImage.setIsPrimary(0);
        }
        if (videoImage.getStatus() == null) {
            videoImage.setStatus(1);
        }
        
        return videoImageMapper.insertVideoImage(videoImage);
    }

    /**
     * 修改视频图片
     * 
     * @param videoImage 视频图片
     * @return 结果
     */
    @Override
    public int updateVideoImage(VideoImage videoImage)
    {
        return videoImageMapper.updateVideoImage(videoImage);
    }

    /**
     * 批量删除视频图片
     * 
     * @param ids 需要删除的视频图片主键
     * @return 结果
     */
    @Override
    public int deleteVideoImageByIds(Long[] ids)
    {
        return videoImageMapper.deleteVideoImageByIds(ids);
    }

    /**
     * 删除视频图片信息
     * 
     * @param id 视频图片主键
     * @return 结果
     */
    @Override
    public int deleteVideoImageById(Long id)
    {
        return videoImageMapper.deleteVideoImageById(id);
    }

    /**
     * 设置主图
     * 
     * @param videoId 视频ID
     * @param imageId 图片ID
     * @return 结果
     */
    @Override
    @Transactional
    public int setPrimaryImage(Long videoId, Long imageId)
    {
        return videoImageMapper.setPrimaryImage(videoId, imageId);
    }

    /**
     * 查询视频的主图
     * 
     * @param videoId 视频ID
     * @return 视频图片
     */
    @Override
    public VideoImage selectPrimaryImageByVideoId(Long videoId)
    {
        return videoImageMapper.selectPrimaryImageByVideoId(videoId);
    }

    /**
     * 统计视频图片数量
     * 
     * @param videoId 视频ID
     * @return 图片数量
     */
    @Override
    public int countImagesByVideoId(Long videoId)
    {
        return videoImageMapper.countImagesByVideoId(videoId);
    }

    /**
     * 批量更新图片排序
     * 
     * @param images 图片列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchUpdateImageSort(List<VideoImage> images)
    {
        return videoImageMapper.batchUpdateImageSort(images);
    }

    /**
     * 检查图片URL是否已存在
     * 
     * @param imageUrl 图片URL
     * @param videoId 视频ID
     * @param excludeId 排除的图片ID（用于编辑时检查）
     * @return 是否存在
     */
    @Override
    public boolean checkImageUrlExists(String imageUrl, Long videoId, Long excludeId)
    {
        VideoImage image = videoImageMapper.checkImageUrlExists(imageUrl, videoId, excludeId);
        return image != null;
    }

    /**
     * 批量上传图片
     * 
     * @param videoId 视频ID
     * @param imageUrls 图片URL列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchInsertImages(Long videoId, List<String> imageUrls)
    {
        int result = 0;
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            VideoImage videoImage = new VideoImage();
            videoImage.setVideoId(videoId);
            videoImage.setImageUrl(imageUrl);
            videoImage.setTitle("批量上传图片" + (i + 1));
            videoImage.setSortOrder(i);
            videoImage.setIsPrimary(0);
            videoImage.setStatus(1);
            result += videoImageMapper.insertVideoImage(videoImage);
        }
        return result;
    }

    /**
     * 根据视频ID删除所有图片
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    public int deleteVideoImagesByVideoId(Long videoId)
    {
        return videoImageMapper.deleteVideoImagesByVideoId(videoId);
    }
} 