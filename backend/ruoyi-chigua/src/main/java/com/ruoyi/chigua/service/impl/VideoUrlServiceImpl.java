package com.ruoyi.chigua.service.impl;

import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.chigua.mapper.VideoUrlMapper;
import com.ruoyi.chigua.domain.VideoUrl;
import com.ruoyi.chigua.service.IVideoUrlService;

/**
 * 视频地址管理Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class VideoUrlServiceImpl implements IVideoUrlService 
{
    @Autowired
    private VideoUrlMapper videoUrlMapper;

    /**
     * 查询视频地址
     * 
     * @param id 视频地址主键
     * @return 视频地址
     */
    @Override
    public VideoUrl selectVideoUrlById(Long id)
    {
        return videoUrlMapper.selectVideoUrlById(id);
    }

    /**
     * 查询视频地址列表
     * 
     * @param videoUrl 视频地址
     * @return 视频地址
     */
    @Override
    public List<VideoUrl> selectVideoUrlList(VideoUrl videoUrl)
    {
        return videoUrlMapper.selectVideoUrlList(videoUrl);
    }

    /**
     * 根据视频ID查询地址列表
     * 
     * @param videoId 视频ID
     * @return 视频地址集合
     */
    @Override
    public List<VideoUrl> selectVideoUrlsByVideoId(Long videoId)
    {
        return videoUrlMapper.selectVideoUrlsByVideoId(videoId);
    }

    /**
     * 新增视频地址
     * 
     * @param videoUrl 视频地址
     * @return 结果
     */
    @Override
    public int insertVideoUrl(VideoUrl videoUrl)
    {
        // 设置默认值
        if (videoUrl.getEpisodeNumber() == null) {
            videoUrl.setEpisodeNumber(1);
        }
        if (videoUrl.getSortOrder() == null) {
            videoUrl.setSortOrder(0);
        }
        if (videoUrl.getIsPrimary() == null) {
            videoUrl.setIsPrimary(0);
        }
        if (videoUrl.getPlayCount() == null) {
            videoUrl.setPlayCount(0);
        }
        if (videoUrl.getStatus() == null) {
            videoUrl.setStatus(1);
        }
        
        return videoUrlMapper.insertVideoUrl(videoUrl);
    }

    /**
     * 修改视频地址
     * 
     * @param videoUrl 视频地址
     * @return 结果
     */
    @Override
    public int updateVideoUrl(VideoUrl videoUrl)
    {
        return videoUrlMapper.updateVideoUrl(videoUrl);
    }

    /**
     * 批量删除视频地址
     * 
     * @param ids 需要删除的视频地址主键
     * @return 结果
     */
    @Override
    public int deleteVideoUrlByIds(Long[] ids)
    {
        return videoUrlMapper.deleteVideoUrlByIds(ids);
    }

    /**
     * 删除视频地址信息
     * 
     * @param id 视频地址主键
     * @return 结果
     */
    @Override
    public int deleteVideoUrlById(Long id)
    {
        return videoUrlMapper.deleteVideoUrlById(id);
    }

    /**
     * 设置主要地址
     * 
     * @param videoId 视频ID
     * @param urlId 地址ID
     * @return 结果
     */
    @Override
    @Transactional
    public int setPrimaryUrl(Long videoId, Long urlId)
    {
        return videoUrlMapper.setPrimaryUrl(videoId, urlId);
    }

    /**
     * 查询视频的主要地址
     * 
     * @param videoId 视频ID
     * @return 视频地址
     */
    @Override
    public VideoUrl selectPrimaryUrlByVideoId(Long videoId)
    {
        return videoUrlMapper.selectPrimaryUrlByVideoId(videoId);
    }

    /**
     * 统计视频地址数量
     * 
     * @param videoId 视频ID
     * @return 地址数量
     */
    @Override
    public int countUrlsByVideoId(Long videoId)
    {
        return videoUrlMapper.countUrlsByVideoId(videoId);
    }

    /**
     * 批量更新地址排序
     * 
     * @param urls 地址列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchUpdateUrlSort(List<VideoUrl> urls)
    {
        return videoUrlMapper.batchUpdateUrlSort(urls);
    }

    /**
     * 检查播放地址是否已存在
     * 
     * @param videoUrl 播放地址
     * @param videoId 视频ID
     * @param excludeId 排除的地址ID（用于编辑时检查）
     * @return 是否存在
     */
    @Override
    public boolean checkVideoUrlExists(String videoUrl, Long videoId, Long excludeId)
    {
        VideoUrl url = videoUrlMapper.checkVideoUrlExists(videoUrl, videoId, excludeId);
        return url != null;
    }

    /**
     * 更新播放次数
     * 
     * @param id 地址ID
     * @return 结果
     */
    @Override
    public int incrementPlayCount(Long id)
    {
        return videoUrlMapper.incrementPlayCount(id);
    }

    /**
     * 根据清晰度查询地址列表
     * 
     * @param videoId 视频ID
     * @param quality 清晰度
     * @return 视频地址集合
     */
    @Override
    public List<VideoUrl> selectVideoUrlsByQuality(Long videoId, String quality)
    {
        return videoUrlMapper.selectVideoUrlsByQuality(videoId, quality);
    }

    /**
     * 根据格式查询地址列表
     * 
     * @param videoId 视频ID
     * @param format 视频格式
     * @return 视频地址集合
     */
    @Override
    public List<VideoUrl> selectVideoUrlsByFormat(Long videoId, String format)
    {
        return videoUrlMapper.selectVideoUrlsByFormat(videoId, format);
    }

    /**
     * 批量导入视频地址
     * 
     * @param videoId 视频ID
     * @param urls 地址列表
     * @return 结果
     */
    @Override
    @Transactional
    public int batchInsertUrls(Long videoId, List<VideoUrl> urls)
    {
        int result = 0;
        for (VideoUrl videoUrl : urls) {
            videoUrl.setVideoId(videoId);
            // 设置默认值
            if (videoUrl.getTitle() == null || videoUrl.getTitle().isEmpty()) {
                videoUrl.setTitle("批量导入地址");
            }
            if (videoUrl.getEpisodeNumber() == null) {
                videoUrl.setEpisodeNumber(result + 1);
            }
            if (videoUrl.getSortOrder() == null) {
                videoUrl.setSortOrder(0);
            }
            if (videoUrl.getIsPrimary() == null) {
                videoUrl.setIsPrimary(0);
            }
            if (videoUrl.getPlayCount() == null) {
                videoUrl.setPlayCount(0);
            }
            if (videoUrl.getStatus() == null) {
                videoUrl.setStatus(1);
            }
            result += insertVideoUrl(videoUrl);
        }
        return result;
    }

    /**
     * 根据视频ID删除所有地址
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    @Override
    public int deleteVideoUrlsByVideoId(Long videoId)
    {
        return videoUrlMapper.deleteVideoUrlsByVideoId(videoId);
    }

    /**
     * 测试链接可用性
     * 
     * @param videoUrl 播放地址
     * @return 测试结果
     */
    @Override
    public boolean testVideoUrl(String videoUrl)
    {
        try {
            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000); // 5秒连接超时
            connection.setReadTimeout(5000); // 5秒读取超时
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            // 2xx状态码表示成功
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            // 连接失败或其他异常
            return false;
        }
    }
} 