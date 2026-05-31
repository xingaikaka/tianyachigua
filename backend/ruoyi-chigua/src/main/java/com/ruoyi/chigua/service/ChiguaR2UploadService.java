package com.ruoyi.chigua.service;

import com.ruoyi.chigua.config.ChiguaProperties;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Chigua R2 上传服务 - 简化版
 * 
 * 🚨 @Deprecated 此服务已废弃，文件操作已迁移到Cloudflare Worker
 * Worker API: https://chigua-r2-worker.xingaikaka.workers.dev/api/upload
 * 
 * Java后端直接上传到R2存储，支持图片加密
 * 路径规划：51chigua/{type}/{date}/{filename}
 */
@Deprecated
@Service
public class ChiguaR2UploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChiguaR2UploadService.class);
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;
    
    @Autowired
    private ChiguaProperties chiguaProperties;
    
    /**
     * R2存储桶名称
     */
    private static final String R2_BUCKET_NAME = "51chigua";
    
    /**
     * 文件类型目录映射
     */
    private static final String IMAGE_DIR = "images";
    private static final String VIDEO_DIR = "videos";
    private static final String DOCUMENT_DIR = "documents";
    private static final String AUDIO_DIR = "audios";
    
    /**
     * 获取S3客户端（用于R2）
     */
    private S3Client getS3Client() {
        return S3Client.builder()
            .endpointOverride(java.net.URI.create(chiguaProperties.getR2().getEndpoint()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    chiguaProperties.getR2().getAccessKeyId(), 
                    chiguaProperties.getR2().getSecretAccessKey()
                )
            ))
            .region(Region.US_EAST_1) // R2使用us-east-1
            .forcePathStyle(true) // R2需要path style
            .build();
    }
    
    /**
     * 上传文件到R2
     * 
     * @param file 上传的文件
     * @param fileType 文件类型（image/video/document/audio）
     * @return 上传结果
     */
    public AjaxResult uploadFile(MultipartFile file, String fileType) {
        try {
            // 验证文件
            AjaxResult validationResult = validateFile(file, fileType);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }
            
            // 生成文件路径
            String filePath = generateFilePath(file, fileType);
            
            // 获取文件数据
            byte[] fileData = file.getBytes();
            String contentType = file.getContentType();
            
            // 如果是图片且启用加密，进行加密
            if (chiguaProperties.getR2().getImageEncryption().isEnabled() && isImageFile(contentType, file.getOriginalFilename())) {
                logger.info("对图片进行加密: {}", file.getOriginalFilename());
                fileData = encryptImage(fileData);
                contentType = "application/octet-stream"; // 加密后改为二进制类型
            }
            
            // 上传到R2
            boolean uploadSuccess = uploadToR2(filePath, fileData, contentType, file.getOriginalFilename());
            
            if (uploadSuccess) {
                // 生成签名URL（用于预览）
                String signedUrl = chiguaUrlService.generateUrl(filePath, getResourceType(fileType));
                
                return AjaxResult.success("文件上传成功")
                    .put("fileName", filePath) // 数据库保存的路径
                    .put("originalName", file.getOriginalFilename())
                    .put("size", file.getSize())
                    .put("url", signedUrl) // 签名URL用于预览
                    .put("resourceKey", filePath); // 资源键
            } else {
                return AjaxResult.error("文件上传失败");
            }
            
        } catch (Exception e) {
            logger.error("文件上传异常", e);
            return AjaxResult.error("文件上传异常：" + e.getMessage());
        }
    }
    
    /**
     * 验证文件
     */
    private AjaxResult validateFile(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            return AjaxResult.error("文件不能为空");
        }
        
        // 检查文件大小
        long maxSize = getMaxFileSize(fileType);
        if (file.getSize() > maxSize) {
            return AjaxResult.error("文件大小超过限制，最大支持 " + (maxSize / 1024 / 1024) + "MB");
        }
        
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isEmpty(originalFilename)) {
            return AjaxResult.error("文件名不能为空");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!isAllowedExtension(extension, fileType)) {
            return AjaxResult.error("不支持的文件格式：" + extension);
        }
        
        return AjaxResult.success();
    }
    
    /**
     * 生成文件路径
     * 格式：51chigua/{type}/{date}/{timestamp}_{uuid}_{filename}
     */
    private String generateFilePath(MultipartFile file, String fileType) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);
        
        // 清理文件名，只保留安全字符
        String safeName = originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (safeName.length() > 50) {
            safeName = safeName.substring(0, 50);
        }
        
        String directory = getDirectoryByType(fileType);
        
        return String.format("%s/%s/%s/%s_%s_%s", 
            R2_BUCKET_NAME, directory, dateStr, timestamp, uuid, safeName);
    }
    
    /**
     * 根据文件类型获取目录
     */
    private String getDirectoryByType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "image":
                return IMAGE_DIR;
            case "video":
                return VIDEO_DIR;
            case "document":
                return DOCUMENT_DIR;
            case "audio":
                return AUDIO_DIR;
            default:
                return "others";
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1).toLowerCase() : "";
    }
    
    /**
     * 检查是否为允许的文件扩展名
     */
    private boolean isAllowedExtension(String extension, String fileType) {
        switch (fileType.toLowerCase()) {
            case "image":
                return StringUtils.inStringIgnoreCase(extension, "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
            case "video":
                return StringUtils.inStringIgnoreCase(extension, "mp4", "avi", "mov", "wmv", "flv", "webm", "mkv");
            case "document":
                return StringUtils.inStringIgnoreCase(extension, "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");
            case "audio":
                return StringUtils.inStringIgnoreCase(extension, "mp3", "wav", "flac", "aac", "ogg");
            default:
                return false;
        }
    }
    
    /**
     * 获取最大文件大小（字节）
     */
    private long getMaxFileSize(String fileType) {
        switch (fileType.toLowerCase()) {
            case "image":
                return 10 * 1024 * 1024; // 10MB
            case "video":
                return 100 * 1024 * 1024; // 100MB
            case "document":
                return 5 * 1024 * 1024; // 5MB
            case "audio":
                return 20 * 1024 * 1024; // 20MB
            default:
                return 1 * 1024 * 1024; // 1MB
        }
    }
    
    /**
     * 获取资源类型
     */
    private ChiguaUrlService.ResourceType getResourceType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "image":
                return ChiguaUrlService.ResourceType.IMAGE;
            case "video":
                return ChiguaUrlService.ResourceType.VIDEO;
            case "audio":
                return ChiguaUrlService.ResourceType.IMAGE; // 音频文件使用IMAGE类型
            default:
                return ChiguaUrlService.ResourceType.IMAGE; // 文档文件使用IMAGE类型
        }
    }
    
    /**
     * 检查是否为图片文件
     */
    private boolean isImageFile(String contentType, String filename) {
        if (contentType != null && contentType.startsWith("image/")) {
            return true;
        }
        
        if (filename != null) {
            String extension = getFileExtension(filename).toLowerCase();
            return StringUtils.inStringIgnoreCase(extension, "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
        }
        
        return false;
    }
    
    /**
     * 加密图片数据
     */
    private byte[] encryptImage(byte[] imageData) throws Exception {
        if (!chiguaProperties.getR2().getImageEncryption().isEnabled()) {
            return imageData;
        }
        
        // 生成或获取密钥
        SecretKey key = getEncryptionKey();
        
        // 生成随机IV
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        
        // 创建加密器
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        
        // 加密数据
        byte[] encryptedData = cipher.doFinal(imageData);
        
        // 将IV和加密数据合并
        byte[] result = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
        
        logger.info("图片加密完成，原始大小: {} bytes, 加密后大小: {} bytes", 
            imageData.length, result.length);
        
        return result;
    }
    
    /**
     * 获取加密密钥
     */
    private SecretKey getEncryptionKey() throws Exception {
        String encryptionKey = chiguaProperties.getR2().getImageEncryption().getKey();
        if (StringUtils.isNotEmpty(encryptionKey)) {
            // 使用配置的密钥
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            return new SecretKeySpec(keyBytes, "AES");
        } else {
            // 生成新密钥（仅用于测试）
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            return keyGen.generateKey();
        }
    }
    
    /**
     * 上传文件到R2
     */
    private boolean uploadToR2(String filePath, byte[] fileData, String contentType, String originalFilename) {
        try (S3Client s3Client = getS3Client()) {
            
            // 创建上传请求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(chiguaProperties.getR2().getBucketName())
                .key(filePath)
                .contentType(contentType)
                .contentLength((long) fileData.length)
                .build();
            
            // 上传文件
            PutObjectResponse response = s3Client.putObject(
                putObjectRequest, 
                RequestBody.fromInputStream(new ByteArrayInputStream(fileData), fileData.length)
            );
            
            logger.info("文件上传到R2成功: {} -> {}, ETag: {}", 
                originalFilename, filePath, response.eTag());
            
            return true;
            
        } catch (Exception e) {
            logger.error("上传到R2失败: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * 批量上传文件
     */
    public AjaxResult uploadFiles(MultipartFile[] files, String fileType) {
        if (files == null || files.length == 0) {
            return AjaxResult.error("文件列表不能为空");
        }
        
        AjaxResult result = AjaxResult.success();
        String[] fileNames = new String[files.length];
        String[] urls = new String[files.length];
        String[] resourceKeys = new String[files.length];
        
        for (int i = 0; i < files.length; i++) {
            AjaxResult uploadResult = uploadFile(files[i], fileType);
            if (uploadResult.isSuccess()) {
                fileNames[i] = (String) uploadResult.get("fileName");
                urls[i] = (String) uploadResult.get("url");
                resourceKeys[i] = (String) uploadResult.get("resourceKey");
            } else {
                return uploadResult;
            }
        }
        
        result.put("fileNames", String.join(",", fileNames));
        result.put("urls", String.join(",", urls));
        result.put("resourceKeys", String.join(",", resourceKeys));
        
        return result;
    }
} 