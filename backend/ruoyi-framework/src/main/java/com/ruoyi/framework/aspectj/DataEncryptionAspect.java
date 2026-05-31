package com.ruoyi.framework.aspectj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.EncryptedTableDataInfo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.DataEncryptionUtil;
import com.ruoyi.common.utils.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据加密AOP切面
 * 
 * 拦截带有 @EncryptResponse 注解的方法，对响应数据进行加密处理
 * 
 * @author ruoyi
 */
@Aspect
@Component
public class DataEncryptionAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(DataEncryptionAspect.class);
    
    @Value("${data.encryption.enabled:false}")
    private boolean encryptionEnabled;
    
    @Value("${data.encryption.secret.key:}")
    private String configSecretKey;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 定义切点：所有带有 @EncryptResponse 注解的方法
     */
    @Pointcut("@annotation(com.ruoyi.common.annotation.EncryptResponse)")
    public void encryptResponsePointcut() {}
    
    /**
     * 环绕通知：处理方法执行前后的加密逻辑
     * 
     * @param joinPoint 连接点
     * @return 处理后的结果
     * @throws Throwable 异常
     */
    @Around("encryptResponsePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行原方法
        Object result = joinPoint.proceed();
        
        // 如果全局加密未启用，直接返回原结果
        if (!encryptionEnabled) {
            logger.debug("数据加密未启用，返回原始响应");
            return result;
        }
        
        try {
            // 获取方法上的注解
            EncryptResponse encryptAnnotation = getEncryptResponseAnnotation(joinPoint);
            if (encryptAnnotation == null || !encryptAnnotation.value()) {
                logger.debug("方法未启用加密或注解value为false，返回原始响应");
                return result;
            }
            
            // 执行加密处理
            return processEncryption(result, encryptAnnotation);
            
        } catch (Exception e) {
            logger.error("响应数据加密处理失败", e);
            
            // 获取注解配置
            EncryptResponse encryptAnnotation = getEncryptResponseAnnotation(joinPoint);
            boolean forceEncrypt = encryptAnnotation != null && encryptAnnotation.forceEncrypt();
            
            if (forceEncrypt) {
                // 强制加密模式：抛出异常
                throw new RuntimeException("响应数据加密失败", e);
            } else {
                // 非强制模式：返回原始数据并记录警告
                logger.warn("响应数据加密失败，返回原始数据");
                return result;
            }
        }
    }
    
    /**
     * 处理加密逻辑
     * 
     * @param result 原始结果
     * @param annotation 加密注解
     * @return 加密后的结果
     */
    private Object processEncryption(Object result, EncryptResponse annotation) {
        if (result == null) {
            return null;
        }
        
        // 获取密钥
        String secretKey = getSecretKey(annotation);
        
        // 处理不同类型的返回值
        if (result instanceof AjaxResult) {
            return encryptAjaxResult((AjaxResult) result, annotation, secretKey);
        } else if (result instanceof TableDataInfo) {
            return encryptTableDataInfo((TableDataInfo) result, annotation, secretKey);
        } else {
            // 其他类型直接加密整个对象
            return encryptDirectly(result, secretKey);
        }
    }
    
    /**
     * 加密AjaxResult类型的响应
     * 
     * @param ajaxResult 原始AjaxResult
     * @param annotation 加密注解
     * @param secretKey 密钥
     * @return 加密后的AjaxResult
     */
    private AjaxResult encryptAjaxResult(AjaxResult ajaxResult, EncryptResponse annotation, String secretKey) {
        try {
            String[] encryptFields = annotation.encryptFields();
            
            // 如果只需要加密data字段（默认情况）
            if (encryptFields.length == 1 && "data".equals(encryptFields[0])) {
                Object originalData = ajaxResult.get("data");
                if (originalData != null) {
                    // 加密data字段
                    Map<String, Object> encryptedData = DataEncryptionUtil.encryptResponseData(originalData, secretKey);
                    
                    // 构建新的响应
                    AjaxResult encryptedResult = new AjaxResult();
                    encryptedResult.put("code", ajaxResult.get("code"));
                    encryptedResult.put("msg", ajaxResult.get("msg"));
                    encryptedResult.put("data", encryptedData.get("data"));
                    encryptedResult.put("encrypted", encryptedData.get("encrypted"));
                    encryptedResult.put("timestamp", encryptedData.get("timestamp"));
                    encryptedResult.put("signature", encryptedData.get("signature"));
                    
                    logger.debug("AjaxResult data字段加密完成");
                    return encryptedResult;
                }
            } else {
                // 多字段加密（较少使用）
                return encryptMultipleFields(ajaxResult, encryptFields, secretKey);
            }
            
            return ajaxResult;
        } catch (Exception e) {
            logger.error("AjaxResult加密失败", e);
            throw new RuntimeException("AjaxResult加密失败", e);
        }
    }
    
    /**
     * 加密TableDataInfo类型的响应
     * 
     * @param tableDataInfo 原始TableDataInfo
     * @param annotation 加密注解
     * @param secretKey 密钥
     * @return 加密后的EncryptedTableDataInfo
     */
    private EncryptedTableDataInfo encryptTableDataInfo(TableDataInfo tableDataInfo, EncryptResponse annotation, String secretKey) {
        try {
            logger.debug("开始加密TableDataInfo响应");
            
            // 创建加密版本的TableDataInfo
            EncryptedTableDataInfo encryptedResult = new EncryptedTableDataInfo(tableDataInfo);
            
            // 加密rows数据
            Object originalRows = tableDataInfo.getRows();
            
            if (originalRows != null) {
                Map<String, Object> encryptedData = DataEncryptionUtil.encryptResponseData(originalRows, secretKey);
                
                // 清空原始rows数据，将加密数据存储在data字段中
                encryptedResult.setRows(null);
                encryptedResult.setData(encryptedData.get("data"));
                
                // 添加加密标识
                encryptedResult.setEncrypted((Boolean) encryptedData.get("encrypted"));
                encryptedResult.setTimestamp((Long) encryptedData.get("timestamp"));
                encryptedResult.setSignature((String) encryptedData.get("signature"));
                
                logger.debug("TableDataInfo加密完成");
            } else {
                logger.debug("TableDataInfo的rows为空，返回未加密的结构");
            }
            
            return encryptedResult;
        } catch (Exception e) {
            logger.error("TableDataInfo加密失败", e);
            throw new RuntimeException("TableDataInfo加密失败", e);
        }
    }
    
    /**
     * 直接加密整个对象
     * 
     * @param result 原始对象
     * @param secretKey 密钥
     * @return 加密后的Map
     */
    private Map<String, Object> encryptDirectly(Object result, String secretKey) {
        Map<String, Object> encryptedData = DataEncryptionUtil.encryptResponseData(result, secretKey);
        logger.debug("对象直接加密完成");
        return encryptedData;
    }
    
    /**
     * 多字段加密处理
     * 
     * @param ajaxResult 原始结果
     * @param encryptFields 需要加密的字段
     * @param secretKey 密钥
     * @return 加密后的结果
     */
    private AjaxResult encryptMultipleFields(AjaxResult ajaxResult, String[] encryptFields, String secretKey) {
        AjaxResult result = new AjaxResult();
        
        // 复制所有字段
        ajaxResult.forEach(result::put);
        
        // 加密指定字段
        for (String field : encryptFields) {
            Object fieldValue = result.get(field);
            if (fieldValue != null) {
                Map<String, Object> encryptedData = DataEncryptionUtil.encryptResponseData(fieldValue, secretKey);
                result.put(field, encryptedData.get("data"));
                
                // 只在第一个字段加密时添加加密标识
                if (field.equals(encryptFields[0])) {
                    result.put("encrypted", encryptedData.get("encrypted"));
                    result.put("timestamp", encryptedData.get("timestamp"));
                    result.put("signature", encryptedData.get("signature"));
                }
            }
        }
        
        logger.debug("多字段加密完成，字段: {}", String.join(",", encryptFields));
        return result;
    }
    
    /**
     * 获取方法上的EncryptResponse注解
     * 
     * @param joinPoint 连接点
     * @return EncryptResponse注解
     */
    private EncryptResponse getEncryptResponseAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
            return method.getAnnotation(EncryptResponse.class);
        } catch (Exception e) {
            logger.error("获取EncryptResponse注解失败", e);
            return null;
        }
    }
    
    /**
     * 获取加密密钥
     * 
     * @param annotation 加密注解
     * @return 密钥字符串
     */
    private String getSecretKey(EncryptResponse annotation) {
        // 1. 优先使用注解指定的配置key
        String secretKeyConfig = annotation.secretKeyConfig();
        if (StringUtils.isNotEmpty(secretKeyConfig)) {
            // 这里可以从配置文件中读取
            logger.debug("使用注解指定的密钥配置: {}", secretKeyConfig);
        }
        
        // 2. 使用配置文件中的密钥
        if (StringUtils.isNotEmpty(configSecretKey)) {
            return configSecretKey;
        }
        
        // 3. 使用工具类默认逻辑获取密钥
        return DataEncryptionUtil.getSecretKey();
    }
}