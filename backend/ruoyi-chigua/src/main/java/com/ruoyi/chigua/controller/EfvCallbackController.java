package com.ruoyi.chigua.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.chigua.dto.EfvCallbackDto;
import com.ruoyi.chigua.dto.EfvCallbackWrapperDto;
import com.ruoyi.chigua.service.IEfvCallbackService;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * EFV视频同步回调Controller
 * 根据 https://docs.efvcms.com/docs/api-interface-usage-tutorial/video-transcode-sync-callback/
 * 
 * @author ruoyi
 * @date 2025-01-16
 */
@RestController
@RequestMapping("/chigua/video/efv")
public class EfvCallbackController extends BaseController
{
    private static final Logger logger = LoggerFactory.getLogger(EfvCallbackController.class);

        @Autowired
    private IEfvCallbackService efvCallbackService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 调试接口：接收原始JSON字符串
     */
    @PostMapping("/callback/debug")
    public AjaxResult debugCallback(@RequestBody String rawJson) 
    {
        logger.info("🔍 EFV原始JSON数据: {}", rawJson);
        return AjaxResult.success("原始数据已记录");
    }
    
    /**
     * 单视频调试接口：接收原始JSON字符串
     */
    @PostMapping("/callback/video/debug")
    public AjaxResult debugVideoCallback(@RequestBody String rawJson) 
    {
        logger.info("🔍 EFV单视频原始JSON数据: {}", rawJson);
        return AjaxResult.success("单视频原始数据已记录");
    }
    
    /**
     * 手动处理请求体的调试接口
     */
    @PostMapping("/callback/video/raw")
    public AjaxResult rawVideoCallback(HttpServletRequest request) throws Exception
    {
        logger.info("🔍 ===== 手动处理EFV请求开始 =====");
        
        // 读取原始请求体
        StringBuilder requestBody = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        
        String rawJson = requestBody.toString();
        logger.info("🔍 EFV原始请求体长度: {} 字节", rawJson.length());
        logger.info("🔍 EFV原始请求体内容: {}", rawJson);
        
        // 打印请求信息
        logger.info("🔗 Content-Type: {}", request.getContentType());
        logger.info("🔗 Content-Length: {}", request.getContentLength());
        logger.info("🔗 Character-Encoding: {}", request.getCharacterEncoding());
        
        logger.info("🔍 ===== 手动处理EFV请求结束 =====");
        
        return AjaxResult.success("原始请求体已记录");
    }
    
    /**
     * 手动JSON解析测试接口
     */
    @PostMapping("/callback/video/parse")
    public AjaxResult parseVideoCallback(HttpServletRequest request) throws Exception
    {
        logger.info("🔍 ===== 手动JSON解析测试开始 =====");
        
        // 读取原始请求体
        StringBuilder requestBody = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        
        String rawJson = requestBody.toString();
        logger.info("🔍 原始JSON长度: {} 字节", rawJson.length());
        
        try {
            // 手动解析JSON到DTO对象
            EfvCallbackDto callbackData;
            
            // 先解析为JsonNode来检查结构
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(rawJson);
            
            if (rootNode.has("movie")) {
                // 如果有movie包装，提取movie节点
                com.fasterxml.jackson.databind.JsonNode movieNode = rootNode.get("movie");
                logger.info("🔍 检测到movie包装结构，提取movie节点");
                callbackData = objectMapper.treeToValue(movieNode, EfvCallbackDto.class);
            } else {
                // 如果没有movie包装，直接解析
                logger.info("🔍 检测到直接结构，直接解析");
                callbackData = objectMapper.readValue(rawJson, EfvCallbackDto.class);
            }
            
            logger.info("✅ JSON解析成功！");
            logger.info("🔍 解析后的DTO数据:");
            logger.info("    _id: {}", callbackData.get_id());
            logger.info("    originalname: {}", callbackData.getOriginalname());
            logger.info("    status: {}", callbackData.getStatus());
            logger.info("    size: {}", callbackData.getSize());
            logger.info("    category: {}", callbackData.getCategory());
            logger.info("    language: {}", callbackData.getLanguage());
            logger.info("    banben: {}", callbackData.getBanben());
            logger.info("    width: {}", callbackData.getWidth());
            logger.info("    height: {}", callbackData.getHeight());
            logger.info("    duration: {}", callbackData.getDuration());
            logger.info("    moviepath: {}", callbackData.getMoviepath());
            logger.info("    poster: {}", callbackData.getPoster());
            logger.info("    rate: {}", callbackData.getRate());
            logger.info("    year: {}", callbackData.getYear());
            logger.info("    md5: {}", callbackData.getMd5());
            logger.info("    poster2: {}", callbackData.getPoster2());
            logger.info("    screenshots: {}", callbackData.getScreenshots());
            logger.info("    m3u8paths: {}", callbackData.getM3u8paths());
            logger.info("    country: {}", callbackData.getCountry());
            logger.info("    tags: {}", callbackData.getTags());
            
            // 尝试调用业务逻辑
            boolean success = efvCallbackService.handleVideoCallback(callbackData);
            
            if (success) {
                logger.info("✅ 业务处理成功！");
                return AjaxResult.success("手动解析和处理成功");
            } else {
                logger.error("❌ 业务处理失败");
                return AjaxResult.error("业务处理失败");
            }
            
        } catch (Exception e) {
            logger.error("❌ JSON解析失败: {}", e.getMessage(), e);
            return AjaxResult.error("JSON解析失败: " + e.getMessage());
        }
        
        finally {
            logger.info("🔍 ===== 手动JSON解析测试结束 =====");
        }
    }
    
    /**
     * 处理EFV通用回调（使用@RequestBody + 包装DTO）
     */
    @PostMapping("/callback")
    public AjaxResult handleEfvCallback(@RequestBody EfvCallbackWrapperDto wrapperData, HttpServletRequest request)
    {
        // 打印客户端信息
        String clientIP = getClientRealIP(request);
        logger.info("🔗 客户端IP: {}, Content-Type: {}", clientIP, request.getContentType());
        
        try {
            // 从包装器中提取实际的回调数据
            EfvCallbackDto callbackData = wrapperData.getCallbackData();
            
            if (callbackData == null) {
                logger.error("❌ 无法从包装器中提取回调数据");
                return AjaxResult.error("无效的回调数据格式");
            }
            
            logger.info("收到EFV回调请求: videoId={}, title={}", 
                callbackData.get_id(), callbackData.getOriginalname());
            
            // 调用业务逻辑
            boolean success = efvCallbackService.handleCallback(callbackData);
            
            if (success) {
                logger.info("EFV回调处理成功: videoId={}, title={}", 
                    callbackData.get_id(), callbackData.getOriginalname());
                return AjaxResult.success("回调处理成功");
            } else {
                logger.error("EFV回调处理失败: videoId={}, title={}", 
                    callbackData.get_id(), callbackData.getOriginalname());
                return AjaxResult.error("回调处理失败");
            }
            
        } catch (Exception e) {
            logger.error("EFV回调处理异常: 错误: {}", e.getMessage(), e);
            return AjaxResult.error("回调处理异常: " + e.getMessage());
        }
    }

    /**
     * 处理EFV单个视频回调（使用@RequestBody + 包装DTO）
     */
    @PostMapping("/callback/video")
    public AjaxResult handleVideoCallback(@RequestBody EfvCallbackWrapperDto wrapperData, HttpServletRequest request) 
    {
        // 打印客户端信息
        String clientIP = getClientRealIP(request);
        logger.info("🔗 客户端IP: {}, Content-Type: {}", clientIP, request.getContentType());
        
        try {
            // 从包装器中提取实际的回调数据
            EfvCallbackDto callbackData = wrapperData.getCallbackData();
            
            if (callbackData == null) {
                logger.error("❌ 无法从包装器中提取回调数据");
                return AjaxResult.error("无效的回调数据格式");
            }
            
            logger.info("收到EFV视频回调: videoId={}, title={}, status={}, moviepath={}", 
                callbackData.get_id(), callbackData.getOriginalname(), 
                callbackData.getStatus(), callbackData.getMoviepath());
            
            // 调用业务逻辑
            boolean success = efvCallbackService.handleVideoCallback(callbackData);
            
            if (success) {
                logger.info("EFV视频回调处理成功: videoId={}, title={}", 
                    callbackData.get_id(), callbackData.getOriginalname());
                return AjaxResult.success("视频回调处理成功");
            } else {
                logger.error("EFV视频回调处理失败: videoId={}, title={}", 
                    callbackData.get_id(), callbackData.getOriginalname());
                return AjaxResult.error("视频回调处理失败");
            }
            
        } catch (Exception e) {
            logger.error("EFV视频回调处理异常: 错误: {}", e.getMessage(), e);
            return AjaxResult.error("视频回调处理异常: " + e.getMessage());
        }
    }

    /**
     * 处理EFV剧集回调
     */
    @PostMapping("/callback/series")
    public AjaxResult handleSeriesCallback(@RequestBody EfvCallbackDto callbackData) 
    {
        logger.info("收到EFV剧集回调: seriesId={}, title={}", 
            callbackData.get_id(), callbackData.getOriginalname());
        
        try {
            boolean success = efvCallbackService.handleSeriesCallback(callbackData);
            
            if (success) {
                logger.info("EFV剧集回调处理成功: seriesId={}, title={}", 
                    callbackData.get_id(), callbackData.getOriginalname());
                return AjaxResult.success("剧集回调处理成功");
            } else {
                logger.error("EFV剧集回调处理失败: seriesId={}, title={}", 
                    callbackData.get_id(), callbackData.getOriginalname());
                return AjaxResult.error("剧集回调处理失败");
            }
        } catch (Exception e) {
            logger.error("EFV剧集回调处理异常: seriesId={}, title={}, 错误: {}", 
                callbackData.get_id(), callbackData.getOriginalname(), e.getMessage(), e);
            return AjaxResult.error("剧集回调处理异常: " + e.getMessage());
        }
    }

    /**
     * 处理EFV视频回调并自动更新富文本内容 - 新增功能（异步优化版）
     * 立即返回响应，避免超时，实际处理在后台异步执行
     */
    @PostMapping("/callback/video-with-content-update")
    public AjaxResult handleVideoCallbackWithContentUpdate(@RequestBody EfvCallbackWrapperDto wrapperData, HttpServletRequest request)
    {
        // 打印客户端信息
        String clientIP = getClientRealIP(request);
        logger.info("🔗 客户端IP: {}, Content-Type: {}", clientIP, request.getContentType());
        
        try {
            // 从包装器中提取实际的回调数据
            EfvCallbackDto callbackData = wrapperData.getCallbackData();
            
            if (callbackData == null) {
                logger.error("❌ 无法从包装器中提取回调数据");
                return AjaxResult.error("无效的回调数据格式");
            }
            
            logger.info("收到EFV视频回调（带富文本更新）: videoId={}, title={}, status={}, moviepath={}", 
                callbackData.get_id(), callbackData.getOriginalname(), 
                callbackData.getStatus(), callbackData.getMoviepath());
            
            // 🚀 立即返回成功响应，避免超时
            // 实际处理逻辑在后台线程中异步执行
            final EfvCallbackDto finalCallbackData = callbackData;
            new Thread(() -> {
                try {
                    logger.info("⚡ 开始异步处理EFV回调: videoId={}, title={}", 
                        finalCallbackData.get_id(), finalCallbackData.getOriginalname());
                    
                    String result = efvCallbackService.handleVideoCallbackWithContentUpdate(finalCallbackData);
                    
                    logger.info("✅ 异步处理完成: videoId={}, 结果={}", 
                        finalCallbackData.get_id(), result);
                        
                } catch (Exception e) {
                    logger.error("❌ 异步处理异常: videoId={}, 错误: {}", 
                        finalCallbackData.get_id(), e.getMessage(), e);
                }
            }, "EFV-Callback-" + callbackData.get_id()).start();
            
            // 构建立即返回的结果
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("message", "回调已接收，正在后台处理");
            resultMap.put("videoId", callbackData.get_id());
            resultMap.put("status", callbackData.getStatus());
            // EFV没有resolution字段，根据width和height构建
            String resolution = "未知";
            if (callbackData.getWidth() != null && callbackData.getHeight() != null) {
                resolution = callbackData.getWidth() + "x" + callbackData.getHeight();
            }
            resultMap.put("resolution", resolution);
            resultMap.put("duration", callbackData.getDuration());
            resultMap.put("fileSize", callbackData.getSize());
            resultMap.put("asyncProcessing", true);
            
            logger.info("🎬 EFV视频回调已接收并启动异步处理: videoId={}, title={}", 
                callbackData.get_id(), callbackData.getOriginalname());
            return AjaxResult.success(resultMap);
            
        } catch (Exception e) {
            logger.error("EFV视频回调（带富文本更新）处理异常: 错误: {}", e.getMessage(), e);
            return AjaxResult.error("回调处理异常: " + e.getMessage());
        }
    }

    /**
     * 测试EFV回调连接
     */
    @GetMapping("/callback/test")
    public AjaxResult testCallback() 
    {
        logger.info("EFV回调测试接口被调用");
        return AjaxResult.success("EFV回调接口连通正常");
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientRealIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}