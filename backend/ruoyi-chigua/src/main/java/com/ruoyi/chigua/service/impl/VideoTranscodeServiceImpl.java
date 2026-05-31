package com.ruoyi.chigua.service.impl;

import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.chigua.mapper.VideoTranscodeMapper;
import com.ruoyi.chigua.domain.VideoTranscode;
import com.ruoyi.chigua.service.IVideoTranscodeService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.dto.PpvodCallbackDto;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 视频转码记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
@Service
public class VideoTranscodeServiceImpl implements IVideoTranscodeService 
{
    private static final Logger logger = LoggerFactory.getLogger(VideoTranscodeServiceImpl.class);

    @Autowired
    private VideoTranscodeMapper videoTranscodeMapper;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    /**
     * 查询视频转码记录
     * 
     * @param id 视频转码记录主键
     * @return 视频转码记录
     */
    @Override
    public VideoTranscode selectVideoTranscodeById(Long id)
    {
        return videoTranscodeMapper.selectVideoTranscodeById(id);
    }

    /**
     * 根据转码ID查询视频转码记录
     * 
     * @param transcodeId 转码ID
     * @return 视频转码记录
     */
    @Override
    public VideoTranscode selectVideoTranscodeByTranscodeId(String transcodeId)
    {
        return videoTranscodeMapper.selectVideoTranscodeByTranscodeId(transcodeId);
    }

    /**
     * 查询视频转码记录列表
     * 
     * @param videoTranscode 视频转码记录
     * @return 视频转码记录
     */
    @Override
    public List<VideoTranscode> selectVideoTranscodeList(VideoTranscode videoTranscode)
    {
        return videoTranscodeMapper.selectVideoTranscodeList(videoTranscode);
    }

    /**
     * 新增视频转码记录
     * 
     * @param videoTranscode 视频转码记录
     * @return 结果
     */
    @Override
    public int insertVideoTranscode(VideoTranscode videoTranscode)
    {
        return videoTranscodeMapper.insertVideoTranscode(videoTranscode);
    }

    /**
     * 修改视频转码记录
     * 
     * @param videoTranscode 视频转码记录
     * @return 结果
     */
    @Override
    public int updateVideoTranscode(VideoTranscode videoTranscode)
    {
        return videoTranscodeMapper.updateVideoTranscode(videoTranscode);
    }

    /**
     * 批量删除视频转码记录
     * 
     * @param ids 需要删除的视频转码记录主键
     * @return 结果
     */
    @Override
    public int deleteVideoTranscodeByIds(Long[] ids)
    {
        return videoTranscodeMapper.deleteVideoTranscodeByIds(ids);
    }

    /**
     * 删除视频转码记录信息
     * 
     * @param id 视频转码记录主键
     * @return 结果
     */
    @Override
    public int deleteVideoTranscodeById(Long id)
    {
        return videoTranscodeMapper.deleteVideoTranscodeById(id);
    }

    /**
     * 从ppvod回调数据创建转码记录
     * 
     * @param callbackData ppvod回调数据
     * @return 转码记录
     */
    @Override
    public VideoTranscode createFromPpvodCallback(PpvodCallbackDto callbackData)
    {
        VideoTranscode videoTranscode = new VideoTranscode();
        
        // 必填字段设置默认值（参考现有数据模式）
        videoTranscode.setUid(callbackData.getUid() != null ? callbackData.getUid() : "system");
        videoTranscode.setVideoType(callbackData.getVideoType() != null ? callbackData.getVideoType() : "short");
        
        // 从orgfile提取标题（去掉.mp4后缀）
        String title = callbackData.getOrgfile();
        if (title != null && title.endsWith(".mp4")) {
            title = title.substring(0, title.length() - 4);
        }
        videoTranscode.setTitle(title != null ? title : "转码视频");
        
        // 基本信息（必填字段，需要保证唯一性）
        String uuid = java.util.UUID.randomUUID().toString();
        videoTranscode.setTranscodeId(callbackData.getTranscodeId() != null ? callbackData.getTranscodeId() : "unknown-" + uuid);
        videoTranscode.setMd5(callbackData.getMd5() != null ? callbackData.getMd5() : "unknown-md5-" + uuid);
        videoTranscode.setShareid(callbackData.getShareid() != null ? callbackData.getShareid() : "unknown-share-" + uuid);
        videoTranscode.setOrgfile(callbackData.getOrgfile() != null ? callbackData.getOrgfile() : "unknown.mp4");
        
        // rpath设置默认值（如果没有提供）
        String rpath = callbackData.getRpath();
        if (rpath == null || rpath.trim().isEmpty()) {
            rpath = "videos/" + java.util.UUID.randomUUID().toString();
        }
        videoTranscode.setRpath(rpath);
        
        videoTranscode.setDomain(callbackData.getDomain() != null ? callbackData.getDomain() : "unknown.domain.com");
        videoTranscode.setPath(callbackData.getPath() != null ? callbackData.getPath() : "/unknown/path");
        videoTranscode.setSuffix(callbackData.getSuffix() != null ? callbackData.getSuffix() : "mp4");
        
        // resolution是必填字段
        videoTranscode.setResolution(callbackData.getResolution() != null ? callbackData.getResolution() : "1920x1080");
        
        // 其他技术参数
        videoTranscode.setWidth(callbackData.getWidth());
        videoTranscode.setHeight(callbackData.getHeight());
        videoTranscode.setDuration(callbackData.getDuration());
        videoTranscode.setBitrate(callbackData.getBitrate());
        // FPS转换为BigDecimal
        if (callbackData.getFps() != null) {
            videoTranscode.setFps(BigDecimal.valueOf(callbackData.getFps()));
        }
        videoTranscode.setFileSize(callbackData.getFileSize());
        
        // 转码状态
        videoTranscode.setTranscodeResult(callbackData.getTranscodeResult());
        
        // 时间处理
        if (callbackData.getTranscodeBegin() != null) {
            videoTranscode.setTranscodeBegin(parseTimeString(callbackData.getTranscodeBegin()));
        }
        if (callbackData.getTranscodeEnd() != null) {
            videoTranscode.setTranscodeEnd(parseTimeString(callbackData.getTranscodeEnd()));
        }
        
        // JSON数据处理
        if (callbackData.getThumbnails() != null) {
            videoTranscode.setThumbnails(JSON.toJSONString(callbackData.getThumbnails()));
        }
        if (callbackData.getOutputFormats() != null) {
            videoTranscode.setOutputFormats(JSON.toJSONString(callbackData.getOutputFormats()));
        }
        
        // 质量判断
        videoTranscode.setQuality(determineQuality(callbackData.getResolution(), callbackData.getBitrate()));
        
        // 状态设置
        if ("ok".equals(callbackData.getTranscodeResult())) {
            videoTranscode.setStatus("completed");
        } else if ("failed".equals(callbackData.getTranscodeResult())) {
            videoTranscode.setStatus("failed");
        } else {
            videoTranscode.setStatus("processing");
        }
        
        // isUsed字段已从表结构中移除（表结构已调整为与pronhub一致）
        
        return videoTranscode;
    }

    /**
     * 保存ppvod回调数据到转码记录
     * 
     * @param callbackData ppvod回调数据
     * @return 结果
     */
    @Override
    public int savePpvodCallback(PpvodCallbackDto callbackData)
    {
        try {
            // 检查是否已存在
            VideoTranscode existingRecord = selectVideoTranscodeByTranscodeId(callbackData.getTranscodeId());
            
            if (existingRecord != null) {
                // 更新现有记录
                VideoTranscode updateRecord = createFromPpvodCallback(callbackData);
                updateRecord.setId(existingRecord.getId());
                updateRecord.setCreatedAt(existingRecord.getCreatedAt()); // 保持原创建时间
                return updateVideoTranscode(updateRecord);
            } else {
                // 创建新记录
                VideoTranscode newRecord = createFromPpvodCallback(callbackData);
                return insertVideoTranscode(newRecord);
            }
        } catch (Exception e) {
            logger.error("保存ppvod回调数据到转码记录失败: transcodeId={}, 错误: {}", 
                callbackData.getTranscodeId(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 查询未使用的转码记录
     * 
     * @param videoTranscode 查询条件
     * @return 转码记录集合
     */
    @Override
    public List<VideoTranscode> selectUnusedTranscodes(VideoTranscode videoTranscode)
    {
        return videoTranscodeMapper.selectUnusedTranscodes(videoTranscode);
    }

    /**
     * 查询所有转码记录并标记使用状态
     * 
     * @param videoTranscode 查询条件
     * @return 转码记录集合（包含使用状态标记）
     */
    @Override
    public List<VideoTranscode> selectAllTranscodesWithUsageStatus(VideoTranscode videoTranscode)
    {
        return videoTranscodeMapper.selectAllTranscodesWithUsageStatus(videoTranscode);
    }

    /**
     * 标记转码记录为已使用
     * 
     * @param transcodeId 转码ID
     * @param usedVideoId 关联的视频ID
     * @return 结果
     */
    @Override
    public int markTranscodeAsUsed(String transcodeId, Long usedVideoId)
    {
        return videoTranscodeMapper.markTranscodeAsUsed(transcodeId, usedVideoId);
    }

    /**
     * 解析时间字符串
     */
    private Date parseTimeString(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 尝试多种时间格式
            SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            };
            
            for (SimpleDateFormat format : formats) {
                try {
                    return format.parse(timeString);
                } catch (ParseException e) {
                    // 继续尝试下一种格式
                }
            }
            
            logger.warn("无法解析时间字符串: {}", timeString);
            return null;
        } catch (Exception e) {
            logger.error("解析时间字符串异常: {}", timeString, e);
            return null;
        }
    }

    /**
     * 判断视频质量
     */
    private String determineQuality(String resolution, Integer bitrate) {
        if (resolution == null) return "hd";
        
        if (resolution.contains("1920x1080") || resolution.contains("1080")) return "fhd";
        if (resolution.contains("1280x720") || resolution.contains("720")) return "hd";
        if (resolution.contains("3840x2160") || resolution.contains("4K")) return "4k";
        if (resolution.contains("7680x4320") || resolution.contains("8K")) return "8k";
        return "sd";
    }

    /**
     * 为富文本生成视频HTML代码（已更新以兼容新的URL API）
     * 
     * @param transcodeId 转码ID（兼容性支持）
     * @return 视频HTML代码
     */
    @Override
    public String generateVideoHtmlForRichText(String transcodeId) {
        try {
            VideoTranscode transcode = videoTranscodeMapper.selectVideoTranscodeByTranscodeId(transcodeId);
            if (transcode == null) {
                throw new RuntimeException("转码记录不存在: " + transcodeId);
            }

            return generateVideoHtmlForRichText(transcode);
        } catch (Exception e) {
            logger.error("生成视频HTML失败: {}", transcodeId, e);
            throw new RuntimeException("生成视频HTML失败: " + e.getMessage());
        }
    }

    /**
     * 为富文本生成视频HTML代码（使用转码记录对象）
     * 与新的VideoTranscodeUrlApiController兼容
     * 
     * @param transcode 转码记录对象
     * @return 视频HTML代码
     */
    public String generateVideoHtmlForRichText(VideoTranscode transcode) {
        try {
            if (transcode == null) {
                throw new RuntimeException("转码记录不能为空");
            }

            if (!"ok".equals(transcode.getTranscodeResult()) || !"completed".equals(transcode.getStatus())) {
                throw new RuntimeException("转码记录未完成，无法生成视频HTML");
            }

            // 生成视频HTML代码 - 使用数据库ID而不是transcodeId，与新API兼容
            StringBuilder html = new StringBuilder();
            html.append("<div class=\"rich-text-video\" data-video-id=\"").append(transcode.getId()).append("\"")
                .append(" data-transcode-id=\"").append(transcode.getTranscodeId()).append("\"") // 保持向后兼容
                .append(" data-orgfile=\"").append(escapeHtml(transcode.getOrgfile())).append("\"")
                .append(" data-resolution=\"").append(escapeHtml(transcode.getResolution())).append("\"")
                .append(" data-duration=\"").append(transcode.getDuration() != null ? transcode.getDuration() : 0).append("\"")
                .append(">");
            
            // 使用占位符，前端会动态替换为实际URL，使用数据库ID
            html.append("<video controls width=\"100%\" style=\"max-width: 600px;\" data-video-placeholder=\"true\">");
            
            // 支持M3U8优先，MP4备用
            html.append("<source data-src-placeholder=\"").append(transcode.getId()).append("\" type=\"application/x-mpegURL\">");
            html.append("<source data-src-placeholder=\"").append(transcode.getId()).append("\" type=\"video/mp4\">");
            html.append("您的浏览器不支持视频播放。");
            html.append("</video>");
            
            html.append("<div class=\"video-info\">");
            html.append("<p><strong>视频：</strong>").append(escapeHtml(transcode.getOrgfile())).append("</p>");
            html.append("<p><strong>分辨率：</strong>").append(escapeHtml(transcode.getResolution())).append("</p>");
            html.append("<p><strong>时长：</strong>").append(formatDuration(transcode.getDuration())).append("</p>");
            if (transcode.getQuality() != null) {
                html.append("<p><strong>质量：</strong>").append(escapeHtml(transcode.getQuality().toUpperCase())).append("</p>");
            }
            html.append("</div>");
            html.append("</div>");

            return html.toString();
        } catch (Exception e) {
            logger.error("生成视频HTML失败: transcodeId={}", transcode.getTranscodeId(), e);
            throw new RuntimeException("生成视频HTML失败: " + e.getMessage());
        }
    }

    /**
     * 更新富文本中的视频URL签名
     * 
     * @param richContent 富文本内容
     * @return 更新签名后的富文本内容
     */
    @Override
    public String updateVideoSignaturesInRichText(String richContent) {
        if (richContent == null || richContent.trim().isEmpty()) {
            return richContent;
        }

        try {
            // 使用正则表达式匹配富文本中的视频元素
            Pattern videoPattern = Pattern.compile(
                "<div[^>]*class=\"[^\"]*rich-text-video[^\"]*\"[^>]*data-transcode-id=\"([^\"]+)\"[^>]*>.*?</div>",
                Pattern.DOTALL
            );
            
            Matcher matcher = videoPattern.matcher(richContent);
            StringBuffer updatedContent = new StringBuffer();

            while (matcher.find()) {
                String transcodeId = matcher.group(1);
                try {
                    // 重新生成带新签名的视频HTML
                    String newVideoHtml = generateVideoHtmlForRichText(transcodeId);
                    matcher.appendReplacement(updatedContent, Matcher.quoteReplacement(newVideoHtml));
                } catch (Exception e) {
                    logger.warn("更新视频签名失败，保留原内容: {}", transcodeId, e);
                    // 如果单个视频更新失败，保留原内容
                }
            }
            matcher.appendTail(updatedContent);

            return updatedContent.toString();
        } catch (Exception e) {
            logger.error("更新富文本视频签名失败", e);
            return richContent; // 出错时返回原内容
        }
    }

    /**
     * 格式化视频时长
     */
    private String formatDuration(Integer duration) {
        if (duration == null || duration <= 0) {
            return "未知";
        }
        
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * HTML转义，防止XSS攻击
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * 获取转码视频播放URL（带签名）
     * 
     * @param transcodeId 转码ID
     * @return 视频播放URL
     */
    @Override
    public String getVideoUrlForRichText(String transcodeId) {
        try {
            VideoTranscode transcode = videoTranscodeMapper.selectVideoTranscodeByTranscodeId(transcodeId);
            if (transcode == null) {
                throw new RuntimeException("转码记录不存在: " + transcodeId);
            }

            if (!"ok".equals(transcode.getTranscodeResult()) || !"completed".equals(transcode.getStatus())) {
                throw new RuntimeException("转码记录未完成，无法获取视频URL");
            }

            return chiguaUrlService.generateVideoUrl(transcode);
        } catch (Exception e) {
            logger.error("获取转码视频URL失败: {}", transcodeId, e);
            throw new RuntimeException("获取转码视频URL失败: " + e.getMessage());
        }
    }

    /**
     * 获取转码视频封面URL（带签名）
     * 
     * @param transcodeId 转码ID
     * @return 视频封面URL
     */
    @Override
    public String getPosterUrlForRichText(String transcodeId) {
        try {
            VideoTranscode transcode = videoTranscodeMapper.selectVideoTranscodeByTranscodeId(transcodeId);
            if (transcode == null) {
                return null; // 封面不存在不抛异常
            }

            return chiguaUrlService.generatePosterUrl(transcode);
        } catch (Exception e) {
            logger.warn("获取转码视频封面URL失败: {}", transcodeId, e);
            return null; // 封面获取失败不影响视频播放
        }
    }
} 