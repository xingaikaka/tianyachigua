package com.ruoyi.chigua.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.chigua.domain.VideoTranscode;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.mapper.VideoMapper;
import com.ruoyi.chigua.dto.EfvCallbackDto;
import com.ruoyi.chigua.mapper.VideoTranscodeMapper;
import com.ruoyi.chigua.service.IEfvCallbackService;
import com.ruoyi.chigua.service.IVideoTranscodeService;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.common.utils.StringUtils;

/**
 * EFV回调处理Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-22
 */
@Service
public class EfvCallbackServiceImpl implements IEfvCallbackService 
{
    private static final Logger logger = LoggerFactory.getLogger(EfvCallbackServiceImpl.class);

    @Autowired
    private IVideoTranscodeService videoTranscodeService;

    @Autowired
    private VideoTranscodeMapper videoTranscodeMapper;

    @Autowired
    private IVideoService videoService;

    @Autowired
    private VideoMapper videoMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理EFV通用回调（自动识别视频或剧集类型）
     */
    @Override
    @Transactional
    public boolean handleCallback(EfvCallbackDto callbackData) 
    {
        logger.info("🎬 处理EFV通用回调: videoId={}, title={}", 
            callbackData.get_id(), callbackData.getOriginalname());

        try {
            // 保存到video_transcodes表
            boolean saveResult = saveToVideoTranscodes(callbackData);
            
            if (saveResult) {
                logger.info("✅ EFV回调数据保存成功: videoId={}", callbackData.get_id());
                return true;
            } else {
                logger.error("❌ EFV回调数据保存失败: videoId={}", callbackData.get_id());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ EFV回调处理异常: videoId={}, 错误: {}", 
                callbackData.get_id(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理EFV单个视频回调
     */
    @Override
    @Transactional
    public boolean handleVideoCallback(EfvCallbackDto callbackData) 
    {
        logger.info("🎬 处理EFV视频回调: videoId={}, title={}", 
            callbackData.get_id(), callbackData.getOriginalname());
        
        // 单个视频处理逻辑与通用回调相同，并补充写入 first_video_url
        boolean ok = handleCallback(callbackData);
        try {
            persistFirstVideoUrlFromCallback(callbackData);
        } catch (Exception e) {
            logger.warn("⚠️ 写入first_video_url失败(视频回调): {}", e.getMessage());
        }
        return ok;
    }

    /**
     * 处理EFV剧集回调
     */
    @Override
    @Transactional
    public boolean handleSeriesCallback(EfvCallbackDto callbackData) 
    {
        logger.info("🎬 处理EFV剧集回调: seriesId={}, title={}", 
            callbackData.get_id(), callbackData.getOriginalname());
        
        // 剧集处理逻辑与通用回调相同
        return handleCallback(callbackData);
    }

    /**
     * 将EFV回调数据保存到video_transcodes表
     */
    @Override
    @Transactional
    public boolean saveToVideoTranscodes(EfvCallbackDto callbackData) 
    {
        try {
            logger.info("💾 开始保存EFV数据到video_transcodes: videoId={}", callbackData.get_id());
            
            // 检查是否已存在相同的转码记录
            VideoTranscode existingRecord = videoTranscodeMapper.selectVideoTranscodeByTranscodeId(callbackData.get_id());
            if (existingRecord != null) {
                logger.info("🔄 转码记录已存在，进行更新: transcodeId={}", callbackData.get_id());
                return updateExistingRecord(existingRecord, callbackData);
            }

            // 创建新的转码记录
            VideoTranscode transcode = convertToVideoTranscode(callbackData);
            
            try {
                int result = videoTranscodeMapper.insertVideoTranscode(transcode);
                
                if (result > 0) {
                    logger.info("✅ EFV数据保存成功: transcodeId={}, title={}", 
                        callbackData.get_id(), callbackData.getOriginalname());
                    return true;
                } else {
                    logger.error("❌ EFV数据保存失败: transcodeId={}", callbackData.get_id());
                    return false;
                }
                
            } catch (DuplicateKeyException e) {
                // 并发情况下的重复键异常 - 幂等性处理
                logger.warn("⚠️ 检测到重复键异常，可能是并发插入: transcodeId={}, 尝试查询现有记录", callbackData.get_id());
                
                // 再次查询确认记录已存在
                VideoTranscode duplicateRecord = videoTranscodeMapper.selectVideoTranscodeByTranscodeId(callbackData.get_id());
                if (duplicateRecord != null) {
                    logger.info("✅ 记录已存在，幂等性处理成功: transcodeId={}", callbackData.get_id());
                    return true; // 幂等性：记录已存在视为成功
                } else {
                    logger.error("❌ 重复键异常但无法找到现有记录: transcodeId={}", callbackData.get_id());
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ 保存EFV数据到video_transcodes异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新已存在的转码记录
     */
    private boolean updateExistingRecord(VideoTranscode existingRecord, EfvCallbackDto callbackData) 
    {
        try {
            // 更新关键字段
            mapCallbackDataToTranscode(existingRecord, callbackData);
            existingRecord.setUpdatedAt(new Date());
            
            int result = videoTranscodeMapper.updateVideoTranscode(existingRecord);
            
            if (result > 0) {
                logger.info("✅ 转码记录更新成功: transcodeId={}", callbackData.get_id());
                return true;
            } else {
                logger.error("❌ 转码记录更新失败: transcodeId={}", callbackData.get_id());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ 更新转码记录异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将EFV回调数据转换为VideoTranscode对象
     */
    private VideoTranscode convertToVideoTranscode(EfvCallbackDto callbackData) 
    {
        VideoTranscode transcode = new VideoTranscode();
        
        // 设置基础字段
        transcode.setTranscodeId(callbackData.get_id());
        transcode.setCreatedAt(new Date());
        transcode.setUpdatedAt(new Date());
        
        // 映射回调数据到转码记录
        mapCallbackDataToTranscode(transcode, callbackData);
        
        return transcode;
    }

    /**
     * 映射EFV回调数据到VideoTranscode对象
     */
    private void mapCallbackDataToTranscode(VideoTranscode transcode, EfvCallbackDto callbackData) 
    {
        try {
            // 必需字段设置
            transcode.setUid("efv"); // EFV系统用户ID
            transcode.setVideoType("long"); // EFV主要处理长视频
            
            // 处理标题：去掉文件扩展名，便于与videos表中的标题匹配
            String title = callbackData.getOriginalname();
            if (title != null && title.toLowerCase().endsWith(".mp4")) {
                title = title.substring(0, title.length() - 4);
            }
            transcode.setTitle(title);
            transcode.setDescription(buildDescription(callbackData));
            transcode.setLanguage(callbackData.getLanguage() != null ? callbackData.getLanguage() : "zh");
            // 设置MD5 - 如果EFV没有提供，则生成一个默认值
            if (StringUtils.isNotEmpty(callbackData.getMd5())) {
                transcode.setMd5(callbackData.getMd5());
            } else {
                transcode.setMd5("efv_" + callbackData.get_id()); // 使用EFV ID作为MD5替代
            }
            transcode.setOrgfile(callbackData.getOriginalname());
            
            // 设置必需的默认值
            transcode.setDomain("default");
            transcode.setSuffix("mp4"); // 默认后缀  
            transcode.setShareid(callbackData.get_id()); // 使用EFV的_id作为shareid
            
            // 文件信息
            if (StringUtils.isNotEmpty(callbackData.getSize())) {
                try {
                    transcode.setFileSize(Long.parseLong(callbackData.getSize()));
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ 无法解析文件大小: {}", callbackData.getSize());
                }
            }
            
            // 路径信息
            // 设置path - 使用EFV的path或创建默认路径
            if (StringUtils.isNotEmpty(callbackData.getPath())) {
                transcode.setPath(callbackData.getPath());
            } else {
                transcode.setPath("efv_" + callbackData.get_id()); // 默认文件路径
            }
            
            // 设置rpath - 应该是M3U8播放列表路径，而不是原始视频路径
            String m3u8Path = getM3u8Path(callbackData);
            if (StringUtils.isNotEmpty(m3u8Path)) {
                transcode.setRpath(m3u8Path);
                logger.info("📺 设置M3U8路径: transcodeId={}, rpath={}", callbackData.get_id(), m3u8Path);
            } else {
                // 如果没有M3U8路径，使用moviepath作为备选
                if (StringUtils.isNotEmpty(callbackData.getMoviepath())) {
                    transcode.setRpath(callbackData.getMoviepath());
                    logger.info("📁 使用原始视频路径: transcodeId={}, rpath={}", callbackData.get_id(), callbackData.getMoviepath());
                } else {
                    transcode.setRpath("/videos/efv/" + callbackData.get_id()); // 默认路径
                    logger.info("🔧 使用默认路径: transcodeId={}, rpath={}", callbackData.get_id(), "/videos/efv/" + callbackData.get_id());
                }
            }
            
            // 封面图片 - 优先使用poster，备选poster2
            String coverImageUrl = getCoverImageUrl(callbackData);
            if (coverImageUrl != null) {
                transcode.setCoverImage(coverImageUrl);
            }
            
            // 视频尺寸
            if (callbackData.getWidth() != null) {
                transcode.setWidth(callbackData.getWidth());
            }
            if (callbackData.getHeight() != null) {
                transcode.setHeight(callbackData.getHeight());
            }
            
            // 解析视频时长
            if (StringUtils.isNotEmpty(callbackData.getDuration())) {
                Integer durationSeconds = parseDurationToSeconds(callbackData.getDuration());
                if (durationSeconds != null) {
                    transcode.setDuration(durationSeconds);
                }
            }
            
            // 观看次数
            if (callbackData.getCount() != null) {
                transcode.setViewsCount(callbackData.getCount().longValue());
            }
            
            // 截图信息
            if (callbackData.getScreenshots() != null && !callbackData.getScreenshots().isEmpty()) {
                try {
                    String screenshotsJson = objectMapper.writeValueAsString(callbackData.getScreenshots());
                    transcode.setThumbnails(screenshotsJson);
                } catch (Exception e) {
                    logger.warn("⚠️ 序列化截图数据失败: {}", e.getMessage());
                    transcode.setThumbnails(null);
                }
            } else {
                transcode.setThumbnails(null);
            }
            
            // SEO信息
            if (StringUtils.isNotEmpty(callbackData.getOriginaltitle())) {
                transcode.setMetaTitle(callbackData.getOriginaltitle());
            }
            if (StringUtils.isNotEmpty(callbackData.getSummary())) {
                transcode.setMetaDescription(callbackData.getSummary());
            }
            
            // 标签信息 - 序列化为JSON格式
            if (callbackData.getTags() != null && !callbackData.getTags().isEmpty()) {
                try {
                    String tagsJson = objectMapper.writeValueAsString(callbackData.getTags());
                    transcode.setMetaKeywords(tagsJson);
                } catch (Exception e) {
                    logger.warn("⚠️ 序列化标签数据失败: {}", e.getMessage());
                    transcode.setMetaKeywords(null);
                }
            } else {
                transcode.setMetaKeywords(null);
            }
            
            // 国家信息 - 序列化为JSON格式
            if (callbackData.getCountry() != null && !callbackData.getCountry().isEmpty()) {
                try {
                    String countryJson = objectMapper.writeValueAsString(callbackData.getCountry());
                    transcode.setAllowedCountries(countryJson);
                } catch (Exception e) {
                    logger.warn("⚠️ 序列化国家数据失败: {}", e.getMessage());
                    transcode.setAllowedCountries(null);
                }
            } else {
                transcode.setAllowedCountries(null);
            }
            
            // 智能状态映射（明确状态优先）
            String efvStatus = callbackData.getStatus();
            boolean hasVideoData = (callbackData.getDuration() != null && !callbackData.getDuration().isEmpty()) ||
                                 (callbackData.getWidth() != null && callbackData.getHeight() != null) ||
                                 (callbackData.getM3u8paths() != null && !callbackData.getM3u8paths().isEmpty()) ||
                                 (callbackData.getScreenshots() != null && !callbackData.getScreenshots().isEmpty());
            
            if ("failed".equals(efvStatus)) {
                // 明确失败状态
                transcode.setStatus("failed");
                transcode.setTranscodeResult("failed");
                transcode.setTranscodeEnd(new Date());
                logger.info("❌ EFV视频标记为失败: transcodeId={}, status={}", 
                    callbackData.get_id(), efvStatus);
            } else if ("processing".equals(efvStatus)) {
                // 明确处理中状态
                transcode.setStatus("processing");
                transcode.setTranscodeResult("pending");
                transcode.setTranscodeBegin(new Date());
                logger.info("⏳ EFV视频标记为处理中: transcodeId={}, status={}", 
                    callbackData.get_id(), efvStatus);
            } else if ("finished".equals(efvStatus) || hasVideoData) {
                // 明确完成状态 或 有关键视频数据则认为已完成
                transcode.setStatus("completed");
                transcode.setTranscodeResult("ok");
                transcode.setTranscodeEnd(new Date());
                logger.info("✅ EFV视频标记为完成: transcodeId={}, status={}, hasVideoData={}", 
                    callbackData.get_id(), efvStatus, hasVideoData);
            } else {
                // 未知状态，默认为完成（EFV回调一般表示已处理完成）
                transcode.setStatus("completed");
                transcode.setTranscodeResult("ok");
                transcode.setTranscodeEnd(new Date());
                logger.info("✅ EFV未知状态默认为完成: transcodeId={}, status={}", 
                    callbackData.get_id(), efvStatus);
            }
            
            // 质量信息
            if (StringUtils.isNotEmpty(callbackData.getBanben())) {
                String quality = mapBanbenToQuality(callbackData.getBanben());
                transcode.setQuality(quality);
            }
            
            // 分辨率信息
            if (callbackData.getWidth() != null && callbackData.getHeight() != null) {
                transcode.setResolution(callbackData.getWidth() + "x" + callbackData.getHeight());
            } else {
                transcode.setResolution("1920x1080"); // 默认分辨率
            }
            
            // M3U8路径信息
            if (callbackData.getM3u8paths() != null && !callbackData.getM3u8paths().isEmpty()) {
                try {
                    String m3u8Json = objectMapper.writeValueAsString(callbackData.getM3u8paths());
                    transcode.setOutputFormats(m3u8Json);
                } catch (Exception e) {
                    logger.warn("⚠️ 序列化M3U8路径数据失败: {}", e.getMessage());
                    transcode.setOutputFormats(null);
                }
            } else {
                transcode.setOutputFormats(null);
            }
            
            logger.debug("✅ EFV数据映射完成: transcodeId={}, title={}", 
                callbackData.get_id(), callbackData.getOriginalname());
                
        } catch (Exception e) {
            logger.error("❌ 映射EFV数据异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 解析时长字符串为秒数
     */
    private Integer parseDurationToSeconds(String duration) 
    {
        if (StringUtils.isEmpty(duration)) {
            return null;
        }
        
        try {
            // 处理 "97分钟" 格式
            if (duration.contains("分钟")) {
                String minutesStr = duration.replace("分钟", "").trim();
                int minutes = Integer.parseInt(minutesStr);
                return minutes * 60;
            }
            
            // 处理 "01:37:25" 格式
            if (duration.contains(":")) {
                String[] parts = duration.split(":");
                if (parts.length == 3) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    int seconds = Integer.parseInt(parts[2]);
                    return hours * 3600 + minutes * 60 + seconds;
                } else if (parts.length == 2) {
                    int minutes = Integer.parseInt(parts[0]);
                    int seconds = Integer.parseInt(parts[1]);
                    return minutes * 60 + seconds;
                }
            }
            
            // 直接解析为秒数
            return Integer.parseInt(duration);
            
        } catch (Exception e) {
            logger.warn("⚠️ 无法解析时长: {}", duration);
            return null;
        }
    }

    /**
     * 获取M3U8播放列表路径
     */
    private String getM3u8Path(EfvCallbackDto callbackData) 
    {
        // 优先从m3u8paths中获取最高质量的路径
        if (callbackData.getM3u8paths() != null && !callbackData.getM3u8paths().isEmpty()) {
            // 按照分辨率降序排序，选择最高质量的M3U8
            com.ruoyi.chigua.dto.EfvCallbackDto.M3u8Path bestQuality = callbackData.getM3u8paths().stream()
                .max((p1, p2) -> Integer.compare(
                    p1.getHd() != null ? p1.getHd() : 0, 
                    p2.getHd() != null ? p2.getHd() : 0
                ))
                .orElse(null);
            
            if (bestQuality != null && StringUtils.isNotEmpty(bestQuality.getPath())) {
                String m3u8Path = bestQuality.getPath();
                
                // 清理路径格式 - 移除前导的 "./" 和确保路径格式正确
                if (m3u8Path.startsWith("./")) {
                    m3u8Path = m3u8Path.substring(2);
                }
                if (m3u8Path.startsWith("/")) {
                    m3u8Path = m3u8Path.substring(1);
                }
                
                // 移除最后的 "/index.m3u8"，只保留目录路径
                if (m3u8Path.endsWith("/index.m3u8")) {
                    m3u8Path = m3u8Path.substring(0, m3u8Path.length() - "/index.m3u8".length());
                } else if (m3u8Path.endsWith("index.m3u8")) {
                    // 如果没有前导斜杠的情况
                    m3u8Path = m3u8Path.substring(0, m3u8Path.length() - "index.m3u8".length());
                    if (m3u8Path.endsWith("/")) {
                        m3u8Path = m3u8Path.substring(0, m3u8Path.length() - 1);
                    }
                }
                
                // 移除最前面的 "public/" 目录
                if (m3u8Path.startsWith("public/")) {
                    m3u8Path = m3u8Path.substring("public/".length());
                }
                
                logger.info("🎯 选择最高质量M3U8目录: hd={}, rpath={} (已移除index.m3u8)", bestQuality.getHd(), m3u8Path);
                return m3u8Path;
            }
        }
        
        return null;
    }

    /**
     * 映射版本信息到质量
     */
    private String mapBanbenToQuality(String banben) 
    {
        if (StringUtils.isEmpty(banben)) {
            return "hd";
        }
        
        String lowerBanben = banben.toLowerCase();
        if (lowerBanben.contains("4k") || lowerBanben.contains("2160")) {
            return "4k";
        } else if (lowerBanben.contains("1080") || lowerBanben.contains("fhd")) {
            return "fhd";
        } else if (lowerBanben.contains("720") || lowerBanben.contains("hd")) {
            return "hd";
        } else if (lowerBanben.contains("480") || lowerBanben.contains("sd")) {
            return "sd";
        } else {
            return "hd"; // 默认高清
        }
    }
    
    /**
     * 构建完整的描述信息
     */
    private String buildDescription(EfvCallbackDto callbackData) 
    {
        StringBuilder description = new StringBuilder();
        
        // 主要描述
        if (StringUtils.isNotEmpty(callbackData.getSummary())) {
            description.append(callbackData.getSummary());
        }
        
        // 分类信息
        if (StringUtils.isNotEmpty(callbackData.getCategory())) {
            if (description.length() > 0) description.append("\n\n");
            description.append("分类: ").append(callbackData.getCategory());
        }
        
        // 导演信息
        if (callbackData.getDirector() != null && !callbackData.getDirector().isEmpty()) {
            if (description.length() > 0) description.append("\n");
            description.append("导演: ").append(String.join(", ", callbackData.getDirector()));
        }
        
        // 编剧信息
        if (callbackData.getWriter() != null && !callbackData.getWriter().isEmpty()) {
            if (description.length() > 0) description.append("\n");
            description.append("编剧: ").append(String.join(", ", callbackData.getWriter()));
        }
        
        // 演员信息
        if (callbackData.getStars() != null && !callbackData.getStars().isEmpty()) {
            if (description.length() > 0) description.append("\n");
            description.append("主演: ").append(String.join(", ", callbackData.getStars()));
        }
        
        // 年份和评分
        if (callbackData.getYear() != null || callbackData.getRate() != null) {
            if (description.length() > 0) description.append("\n");
            if (callbackData.getYear() != null) {
                description.append("年份: ").append(callbackData.getYear());
            }
            if (callbackData.getRate() != null) {
                if (callbackData.getYear() != null) description.append(" | ");
                description.append("评分: ").append(callbackData.getRate());
            }
        }
        
        return description.toString();
    }
    
    /**
     * 获取封面图片URL
     */
    private String getCoverImageUrl(EfvCallbackDto callbackData) 
    {
        // 优先使用poster
        if (StringUtils.isNotEmpty(callbackData.getPoster())) {
            return callbackData.getPoster();
        }
        
        // 备选poster2
        if (callbackData.getPoster2() != null && StringUtils.isNotEmpty(callbackData.getPoster2().getUrl())) {
            return callbackData.getPoster2().getUrl();
        }
        
        // 使用firstScreen
        if (StringUtils.isNotEmpty(callbackData.getFirstScreen())) {
            return callbackData.getFirstScreen();
        }
        
        // 最后使用截图数组的第一张
        if (callbackData.getScreenshots() != null && !callbackData.getScreenshots().isEmpty()) {
            return callbackData.getScreenshots().get(0);
        }
        
        return null;
    }

    /**
     * 处理EFV视频回调并自动更新富文本内容 - 新增功能
     * 保持现有业务不变，在保存转码记录的基础上，根据视频名称查询videos表并更新富文本内容
     */
    @Override
    @Transactional
    public String handleVideoCallbackWithContentUpdate(EfvCallbackDto callbackData) 
    {
        logger.info("🎬 开始处理EFV回调富文本更新: videoId={}, title={}", 
            callbackData.get_id(), callbackData.getOriginalname());
        
        try {
            // 1. 先执行原有业务：保存转码记录到video_transcodes表
            boolean transcodeResult = saveToVideoTranscodes(callbackData);
            
            if (!transcodeResult) {
                logger.error("保存转码记录失败: videoId={}", callbackData.get_id());
                return "失败：保存转码记录失败";
            }
            
            logger.info("✅ 转码记录保存成功: videoId={}", callbackData.get_id());
            
            // 2. 新增业务：根据视频名称查询并更新富文本内容
            try {
                // 将EFV回调数据转换为ppvod格式，以便复用现有的富文本更新逻辑
                com.ruoyi.chigua.dto.PpvodCallbackDto ppvodData = convertEfvToPpvodCallback(callbackData);
                String videoUpdateResult = videoService.updateVideoContentByTranscodeCallback(ppvodData);
                try {
                    persistFirstVideoUrlFromCallback(callbackData);
                } catch (Exception e) {
                    logger.warn("⚠️ 写入first_video_url失败(带富文本更新回调): {}", e.getMessage());
                }
                
                logger.info("✅ 富文本更新处理完成: videoId={}, 结果={}", 
                    callbackData.get_id(), videoUpdateResult);
                return videoUpdateResult;
                
            } catch (Exception e) {
                logger.error("❌ 富文本更新处理异常: videoId={}, 错误: {}", 
                    callbackData.get_id(), e.getMessage(), e);
                // 富文本更新失败不影响主要业务，仅记录日志
                return "转码记录保存成功，但富文本更新失败: " + e.getMessage();
            }
            
        } catch (Exception e) {
            logger.error("EFV回调处理异常（带富文本更新）: videoId={}, 错误: {}", 
                callbackData.get_id(), e.getMessage(), e);
            return "失败：" + e.getMessage();
        }
    }

    /**
     * 将EFV回调中的播放路径提取为相对路径并写入 videos.first_video_url
     */
    private void persistFirstVideoUrlFromCallback(EfvCallbackDto callbackData) {
        try {
            // 优先使用 video_transcodes.rpath（相对路径或以/开头）
            String relative = null;
            VideoTranscode transcode = videoTranscodeService.selectVideoTranscodeByTranscodeId(callbackData.get_id());
            if (transcode != null && transcode.getRpath() != null && !transcode.getRpath().trim().isEmpty()) {
                relative = transcode.getRpath();
            } else if (callbackData.getMoviepath() != null && !callbackData.getMoviepath().trim().isEmpty()) {
                relative = callbackData.getMoviepath();
            }
            if (relative == null || relative.trim().isEmpty()) {
                return;
            }
            // 规范化为不以/开头的相对路径
            if (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            // 规范化为具体播放列表：目录路径追加 /index.m3u8
            String finalRelative = relative;
            try {
                String lower = finalRelative.toLowerCase();
                if (!(lower.endsWith(".m3u8") || lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv"))) {
                    finalRelative = finalRelative.endsWith("/") ? (finalRelative + "index.m3u8") : (finalRelative + "/index.m3u8");
                }
            } catch (Exception ignore) {}

            // 通过标题模糊匹配定位视频（与现有富文本更新逻辑保持一致）
            String title = callbackData.getOriginalname();
            if (title != null && title.toLowerCase().endsWith(".mp4")) {
                title = title.substring(0, title.length() - 4);
            }
            Video probe = new Video();
            probe.setTitle(title);
            probe.setStatus(1);
            List<Video> matched = videoMapper.selectVideoListByTitleLike(probe);
            if (matched == null || matched.isEmpty()) {
                return;
            }
            Video target = matched.get(0);
            // 仅在为空或变化时更新
            Video upd = new Video();
            upd.setId(target.getId());
            if (target.getFirstVideoUrl() == null || !target.getFirstVideoUrl().equals(finalRelative)) {
                upd.setFirstVideoUrl(finalRelative);
                upd.setLastEditedAt(new Date());
                int n = videoMapper.updateVideo(upd);
                if (n > 0) {
                    logger.info("✅ 已写入first_video_url: videoId={}, url={}", target.getId(), finalRelative);
                }
            }
        } catch (Exception e) {
            logger.warn("写入first_video_url异常: {}", e.getMessage());
        }
    }

    /**
     * 将EFV回调数据转换为PpvodCallbackDto格式，以便复用现有的富文本更新逻辑
     */
    private com.ruoyi.chigua.dto.PpvodCallbackDto convertEfvToPpvodCallback(EfvCallbackDto efvData) {
        com.ruoyi.chigua.dto.PpvodCallbackDto ppvodData = new com.ruoyi.chigua.dto.PpvodCallbackDto();
        
        // 映射基本字段
        ppvodData.setTranscodeId(efvData.get_id());
        ppvodData.setOrgfile(efvData.getOriginalname());
        ppvodData.setPath(efvData.getMoviepath());
        
        // EFV没有domain字段，设置默认值
        ppvodData.setDomain("efv.domain.com");
        
        // EFV没有resolution字段，根据width和height构建
        if (efvData.getWidth() != null && efvData.getHeight() != null) {
            ppvodData.setResolution(efvData.getWidth() + "x" + efvData.getHeight());
        } else {
            ppvodData.setResolution("1920x1080"); // 默认分辨率
        }
        
        // 处理duration字段（EFV是字符串格式，如"97分钟"）
        String durationStr = efvData.getDuration();
        if (durationStr != null) {
            try {
                // 尝试从"97分钟"格式中提取数字
                String numStr = durationStr.replaceAll("[^0-9]", "");
                if (!numStr.isEmpty()) {
                    ppvodData.setDuration(Integer.parseInt(numStr) * 60); // 转换为秒
                }
            } catch (Exception e) {
                logger.warn("无法解析duration: {}", durationStr);
            }
        }
        
        // 处理fileSize字段（EFV的size字段是字符串）
        String sizeStr = efvData.getSize();
        if (sizeStr != null) {
            try {
                ppvodData.setFileSize(Long.parseLong(sizeStr));
            } catch (Exception e) {
                logger.warn("无法解析fileSize: {}", sizeStr);
            }
        }
        
        // 设置转码结果
        if ("success".equals(efvData.getStatus()) || "completed".equals(efvData.getStatus())) {
            ppvodData.setTranscodeResult("ok");
        } else {
            ppvodData.setTranscodeResult("failed");
        }
        
        // 设置缩略图
        String firstScreen = getCoverImageUrl(efvData);
        if (firstScreen != null) {
            ppvodData.setThumbnails(java.util.Arrays.asList(firstScreen));
        }
        
        return ppvodData;
    }
}