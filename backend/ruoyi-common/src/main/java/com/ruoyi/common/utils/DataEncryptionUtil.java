package com.ruoyi.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据加密工具类
 * 
 * @author ruoyi
 */
public class DataEncryptionUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(DataEncryptionUtil.class);
    
    // AES加密算法
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_KEY_ALGORITHM = "AES";
    
    // HMAC签名算法
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    // 默认密钥（生产环境请使用配置文件）
    private static final String DEFAULT_SECRET_KEY = "chigua-web-secret-dev-2024";
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 将任意长度的密钥转换为32字节的AES密钥
     * 
     * @param originalKey 原始密钥
     * @return 32字节的密钥
     */
    private static byte[] normalizeKey(String originalKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(originalKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("密钥标准化失败", e);
            throw new RuntimeException("密钥标准化失败", e);
        }
    }
    
    /**
     * AES加密
     * 
     * @param data 待加密的数据
     * @param secretKey 密钥
     * @return 加密后的字符串
     */
    public static String aesEncrypt(String data, String secretKey) {
        try {
            // 生成16字节的随机IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // 创建标准化的32字节密钥
            byte[] normalizedKey = normalizeKey(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(normalizedKey, AES_KEY_ALGORITHM);
            
            // 创建加密器
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            // 加密数据
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // 将IV和加密数据合并
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            
            // Base64编码
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            logger.error("AES加密失败", e);
            throw new RuntimeException("数据加密失败", e);
        }
    }
    
    /**
     * 生成HMAC签名
     * 
     * @param data 待签名的数据
     * @param timestamp 时间戳
     * @param secretKey 密钥
     * @return 签名字符串
     */
    public static String generateSignature(String data, long timestamp, String secretKey) {
        try {
            String signData = data + "|" + timestamp;
            // 使用标准化的密钥进行HMAC签名
            byte[] normalizedKey = normalizeKey(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(normalizedKey, HMAC_ALGORITHM);
            
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            
            byte[] signature = mac.doFinal(signData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signature);
        } catch (Exception e) {
            logger.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
    
    /**
     * 加密响应数据
     * 
     * @param originalData 原始数据
     * @param secretKey 密钥
     * @return 加密后的响应Map
     */
    public static Map<String, Object> encryptResponseData(Object originalData, String secretKey) {
        try {
            // 将原始数据转换为JSON字符串
            String dataJson = objectMapper.writeValueAsString(originalData);
            
            // 生成时间戳
            long timestamp = System.currentTimeMillis();
            
            // AES加密数据
            String encryptedData = aesEncrypt(dataJson, secretKey);
            
            // 生成签名
            String signature = generateSignature(dataJson, timestamp, secretKey);
            
            // 构建加密响应
            Map<String, Object> encryptedResponse = new HashMap<>();
            encryptedResponse.put("data", encryptedData);
            encryptedResponse.put("encrypted", true);
            encryptedResponse.put("timestamp", timestamp);
            encryptedResponse.put("signature", signature);
            
            logger.debug("数据加密成功，timestamp: {}", timestamp);
            return encryptedResponse;
        } catch (Exception e) {
            logger.error("响应数据加密失败", e);
            throw new RuntimeException("响应数据加密失败", e);
        }
    }
    
    /**
     * 加密响应数据（使用默认密钥）
     * 
     * @param originalData 原始数据
     * @return 加密后的响应Map
     */
    public static Map<String, Object> encryptResponseData(Object originalData) {
        return encryptResponseData(originalData, DEFAULT_SECRET_KEY);
    }
    
    /**
     * 字节数组转十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 获取配置的密钥
     * 
     * @return 密钥字符串
     */
    public static String getSecretKey() {
        // 优先从系统属性获取
        String key = System.getProperty("data.encryption.secret.key");
        if (key != null && !key.trim().isEmpty()) {
            return key;
        }
        
        // 从环境变量获取
        key = System.getenv("DATA_ENCRYPTION_SECRET_KEY");
        if (key != null && !key.trim().isEmpty()) {
            return key;
        }
        
        // 返回默认密钥
        logger.warn("未配置加密密钥，使用默认密钥（生产环境请配置）");
        return DEFAULT_SECRET_KEY;
    }
}
