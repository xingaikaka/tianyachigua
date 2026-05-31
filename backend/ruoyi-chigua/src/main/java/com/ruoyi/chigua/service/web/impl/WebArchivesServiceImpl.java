package com.ruoyi.chigua.service.web.impl;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.service.ITagService;
import com.ruoyi.chigua.service.web.IWebArchivesService;
import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.cache.annotation.Cacheable;

/**
 * Web端往期内容Service业务层处理
 */
@Service
public class WebArchivesServiceImpl implements IWebArchivesService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebArchivesServiceImpl.class);
    
    @Autowired
    private VideoMapper videoMapper;
    
    @Autowired
    private ITagService tagService;

    /**
     * 根据日期获取往期内容（带缓存）
     */
    @Override
    @Cacheable(value = "archivesList", key = "'archives_' + #pageNum + '_' + #pageSize", unless = "#result == null")
    public AjaxResult getArchivesByDate(Integer pageNum, Integer pageSize) {
        try {
            logger.info("📅 获取往期内容: pageNum={}, pageSize={}", pageNum, pageSize);
            
            // 设置分页
            PageHelper.startPage(pageNum, pageSize);
            
            // 查询所有已发布的视频，按创建时间倒序
            Video queryVideo = new Video();
            queryVideo.setStatus(1); // 只查询已发布的视频
            Map<String, Object> params = new HashMap<>();
            params.put("excludeShortCategories", Boolean.TRUE);
            queryVideo.setParams(params);
            List<Video> videos = videoMapper.selectVideoList(queryVideo);
            
            PageInfo<Video> pageInfo = new PageInfo<>(videos);
            
            logger.info("📊 查询到视频总数: {}, 当前页视频数: {}", pageInfo.getTotal(), videos.size());
            
            // 按年月分组
            Map<String, List<Map<String, Object>>> archivesByMonth = new LinkedHashMap<>();
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy年MM月");
            SimpleDateFormat dayFormat = new SimpleDateFormat("MM-dd");
            
            for (Video video : videos) {
                if (video.getCreatedAt() != null) {
                    String monthKey = monthFormat.format(video.getCreatedAt());
                    String dayDisplay = dayFormat.format(video.getCreatedAt());
                    
                    Map<String, Object> videoInfo = new HashMap<>();
                    videoInfo.put("id", video.getId());
                    videoInfo.put("title", video.getTitle());
                    videoInfo.put("dateDisplay", dayDisplay);
                    videoInfo.put("createTime", video.getCreatedAt());
                    
                    archivesByMonth.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(videoInfo);
                }
            }
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("archives", archivesByMonth);
            responseData.put("total", pageInfo.getTotal());
            responseData.put("pageNum", pageInfo.getPageNum());
            responseData.put("pageSize", pageInfo.getPageSize());
            responseData.put("pages", pageInfo.getPages());
            
            logger.info("✅ 往期内容获取成功: 共{}个月份", archivesByMonth.size());
            
            return AjaxResult.success(responseData);
            
        } catch (Exception e) {
            logger.error("❌ 获取往期内容失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取往期内容失败");
        }
    }

    /**
     * 获取标签列表（带缓存，支持限制数量）
     */
    @Override
    @Cacheable(value = "tagsList", key = "'archives_tags_limit_' + #limit", unless = "#result == null")
    public AjaxResult getAllTags(Integer limit) {
        try {
            logger.info("🏷️ 获取标签列表: limit={}", limit);
            
            // 使用分页限制查询数量
            PageHelper.startPage(1, limit);
            
            // 查询启用的标签
            Tag queryTag = new Tag();
            queryTag.setStatus(1); // 只查询启用的标签
            List<Tag> tags = tagService.selectTagList(queryTag);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> tagList = tags.stream()
                .map(tag -> {
                    Map<String, Object> tagInfo = new HashMap<>();
                    tagInfo.put("id", tag.getId());
                    tagInfo.put("name", tag.getName());
                    tagInfo.put("color", tag.getColor());
                    return tagInfo;
                })
                .collect(Collectors.toList());
            
            logger.info("✅ 获取标签成功: {}个标签 (限制{}个)", tagList.size(), limit);
            
            return AjaxResult.success(tagList);
            
        } catch (Exception e) {
            logger.error("❌ 获取标签失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取标签失败");
        }
    }
}
