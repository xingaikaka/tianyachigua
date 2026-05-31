package com.ruoyi.chigua.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.UUID;

// Apache HttpClient imports (Java 8 compatible)
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * R2文件上传服务
 * 
 * 负责将文件上传到pornhub R2存储，使用与pornhub项目兼容的格式
 * 
 * @author ruoyi
 * @date 2025-01-19
 */
//@Service  // 暂时禁用，避免配置问题
public class R2UploadService {

    private static final Logger logger = LoggerFactory.getLogger(R2UploadService.class);

    @Value("${chigua.domains.main}")
    private String r2Domain;

    /**
     * 上传文件到R2存储
     * 
     * @param file 要上传的文件
     * @param customKey 自定义文件key（可选）
     * @return 上传结果，包含R2中的文件路径
     */
    public R2UploadResult uploadFile(MultipartFile file, String customKey) {
        try {
            // 生成符合pornhub格式的文件key
            String fileKey = customKey != null ? customKey : generateFileKey(file.getOriginalFilename());
            
            // 构建上传URL
            String uploadUrl = r2Domain + "/upload";
            
            // 创建Apache HttpClient
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(uploadUrl);
            
            // 构建multipart/form-data请求体
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            
            // 添加文件
            InputStreamBody fileBody = new InputStreamBody(file.getInputStream(), file.getContentType(), file.getOriginalFilename());
            builder.addPart("file", fileBody);
            
            // 添加key字段（如果提供）
            if (fileKey != null) {
                builder.addPart("key", new StringBody(fileKey, org.apache.http.entity.ContentType.TEXT_PLAIN));
            }
            
            HttpEntity multipart = builder.build();
            post.setEntity(multipart);
            
            // 发送请求
            HttpResponse response = client.execute(post);
            
            if (response.getStatusLine().getStatusCode() == 200) {
                logger.info("文件上传成功: {} -> {}", file.getOriginalFilename(), fileKey);
                return new R2UploadResult(true, fileKey, "上传成功");
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("文件上传失败: {}, 状态码: {}, 响应: {}", 
                    file.getOriginalFilename(), response.getStatusLine().getStatusCode(), responseBody);
                return new R2UploadResult(false, null, "上传失败: " + responseBody);
            }
            
        } catch (Exception e) {
            logger.error("文件上传异常: {}", file.getOriginalFilename(), e);
            return new R2UploadResult(false, null, "上传异常: " + e.getMessage());
        }
    }

    /**
     * 生成符合pornhub格式的文件key
     * 格式: uploads/{timestamp}_{randomId}_{filename}
     */
    private String generateFileKey(String originalFilename) {
        long timestamp = System.currentTimeMillis();
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        
        // 清理文件名，只保留安全字符
        String sanitizedName = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        
        return String.format("uploads/%d_%s_%s", timestamp, randomId, sanitizedName);
    }

    /**
     * R2上传结果
     */
    public static class R2UploadResult {
        private boolean success;
        private String fileKey;
        private String message;

        public R2UploadResult(boolean success, String fileKey, String message) {
            this.success = success;
            this.fileKey = fileKey;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getFileKey() { return fileKey; }
        public String getMessage() { return message; }
    }
} 