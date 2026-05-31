package com.ruoyi.chigua.service.impl;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.chigua.mapper.RedgifsVideoMapper;
import com.ruoyi.chigua.mapper.RedgifsUserMapper;
import com.ruoyi.chigua.domain.RedgifsVideo;
import com.ruoyi.chigua.domain.RedgifsUser;
import com.ruoyi.chigua.service.IRedgifsVideoService;

/**
 * RedGifs视频信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
@Service
public class RedgifsVideoServiceImpl implements IRedgifsVideoService 
{
    @Autowired
    private RedgifsVideoMapper redgifsVideoMapper;
    
    @Autowired
    private RedgifsUserMapper redgifsUserMapper;

    /**
     * 查询RedGifs视频信息
     * 
     * @param id RedGifs视频信息主键
     * @return RedGifs视频信息
     */
    @Override
    public RedgifsVideo selectRedGifsVideoById(Long id)
    {
        return redgifsVideoMapper.selectRedGifsVideoById(id);
    }

    /**
     * 查询RedGifs视频信息列表
     * 
     * @param redgifsVideo RedGifs视频信息
     * @return RedGifs视频信息
     */
    @Override
    public List<RedgifsVideo> selectRedGifsVideoList(RedgifsVideo redgifsVideo)
    {
        return redgifsVideoMapper.selectRedGifsVideoList(redgifsVideo);
    }

    /**
     * 新增RedGifs视频信息
     * 
     * @param redgifsVideo RedGifs视频信息
     * @return 结果
     */
    @Override
    public int insertRedGifsVideo(RedgifsVideo redgifsVideo)
    {
        return redgifsVideoMapper.insertRedGifsVideo(redgifsVideo);
    }

    /**
     * 修改RedGifs视频信息
     * 
     * @param redgifsVideo RedGifs视频信息
     * @return 结果
     */
    @Override
    public int updateRedGifsVideo(RedgifsVideo redgifsVideo)
    {
        return redgifsVideoMapper.updateRedGifsVideo(redgifsVideo);
    }

    /**
     * 批量删除RedGifs视频信息
     * 
     * @param ids 需要删除的RedGifs视频信息主键
     * @return 结果
     */
    @Override
    public int deleteRedGifsVideoByIds(Long[] ids)
    {
        return redgifsVideoMapper.deleteRedGifsVideoByIds(ids);
    }

    /**
     * 删除RedGifs视频信息信息
     * 
     * @param id RedGifs视频信息主键
     * @return 结果
     */
    @Override
    public int deleteRedGifsVideoById(Long id)
    {
        return redgifsVideoMapper.deleteRedGifsVideoById(id);
    }

    /**
     * 根据gifId查询RedGifs视频信息
     * @param gifId RedGifs视频ID
     * @return RedGifs视频信息
     */
    @Override
    public RedgifsVideo selectRedGifsVideoByGifId(String gifId)
    {
        return redgifsVideoMapper.selectRedGifsVideoByGifId(gifId);
    }

    /**
     * 批量检查视频是否存在
     * @param gifIds 视频ID列表
     * @return Map<String, Map<String, Object>> 包含gifId, exists, videoId
     */
    @Override
    public Map<String, Map<String, Object>> checkRedgifsVideosExist(List<String> gifIds)
    {
        Map<String, Map<String, Object>> result = new HashMap<>();
        
        if (gifIds == null || gifIds.isEmpty()) {
            return result;
        }
        
        List<RedgifsVideo> existingVideos = redgifsVideoMapper.selectRedGifsVideosByGifIds(gifIds);
        Set<String> existingGifIds = new HashSet<>();
        Map<String, Long> gifIdVideoIdMap = new HashMap<>();
        
        for (RedgifsVideo video : existingVideos) {
            existingGifIds.add(video.getGifId());
            gifIdVideoIdMap.put(video.getGifId(), video.getId());
        }
        
        for (String gifId : gifIds) {
            Map<String, Object> videoInfo = new HashMap<>();
            boolean exists = existingGifIds.contains(gifId);
            videoInfo.put("exists", exists);
            if (exists) {
                videoInfo.put("videoId", gifIdVideoIdMap.get(gifId));
            }
            result.put(gifId, videoInfo);
        }
        
        return result;
    }

    /**
     * 入库RedGifs视频
     * @param redgifsVideo 视频信息
     * @return Map<String, Object> 包含success, videoId, exists, message
     */
    @Override
    public Map<String, Object> ingestRedgifsVideo(RedgifsVideo redgifsVideo)
    {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证 userId 是否有效
            if (redgifsVideo.getUserId() == null || redgifsVideo.getUserId() <= 0) {
                result.put("success", false);
                result.put("exists", false);
                result.put("message", "用户ID无效");
                return result;
            }
            
            // 验证关联的用户是否存在
            RedgifsUser user = redgifsUserMapper.selectRedGifsUserById(Long.valueOf(redgifsVideo.getUserId()));
            if (user == null) {
                result.put("success", false);
                result.put("exists", false);
                result.put("message", "关联的用户不存在，用户ID：" + redgifsVideo.getUserId());
                return result;
            }
            
            // 检查视频是否已存在
            RedgifsVideo existingVideo = redgifsVideoMapper.selectRedGifsVideoByGifId(redgifsVideo.getGifId());
            
            if (existingVideo != null) {
                // 视频已存在，更新信息
                redgifsVideo.setId(existingVideo.getId());
                int updateResult = redgifsVideoMapper.updateRedGifsVideo(redgifsVideo);
                
                result.put("success", updateResult > 0);
                result.put("videoId", existingVideo.getId());
                result.put("exists", true);
                result.put("message", updateResult > 0 ? "视频信息已更新" : "视频信息更新失败");
            } else {
                // 新视频，插入数据库
                int insertResult = redgifsVideoMapper.insertRedGifsVideo(redgifsVideo);
                
                result.put("success", insertResult > 0);
                result.put("videoId", redgifsVideo.getId());
                result.put("exists", false);
                result.put("message", insertResult > 0 ? "视频信息已入库" : "视频信息入库失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("exists", false);
            result.put("message", "入库失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 将视频封面原始路径同步为所属用户的 profile_image_url
     */
    @Override
    public Map<String, Object> syncPosterToUserAvatar(Long videoId)
    {
        Map<String, Object> result = new HashMap<>();
        try {
            RedgifsVideo video = redgifsVideoMapper.selectRedGifsVideoById(videoId);
            if (video == null) {
                result.put("success", false);
                result.put("message", "视频不存在");
                return result;
            }
            String posterUrl = video.getPosterUrl();
            if (posterUrl == null || posterUrl.isEmpty()) {
                result.put("success", false);
                result.put("message", "该视频没有封面地址");
                return result;
            }
            if (video.getUserId() == null) {
                result.put("success", false);
                result.put("message", "该视频没有关联用户");
                return result;
            }
            int rows = redgifsUserMapper.updateProfileImageUrl(Long.valueOf(video.getUserId()), posterUrl);
            if (rows > 0) {
                result.put("success", true);
                result.put("message", "头像已更新");
            } else {
                result.put("success", false);
                result.put("message", "用户不存在或更新失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败：" + e.getMessage());
        }
        return result;
    }
}
