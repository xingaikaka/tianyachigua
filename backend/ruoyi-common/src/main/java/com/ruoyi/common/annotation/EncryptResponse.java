package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 响应数据加密注解
 * 
 * 用于标记需要对响应数据进行加密的Controller方法
 * 
 * 使用示例：
 * <pre>
 * &#64;GetMapping("/list")
 * &#64;EncryptResponse
 * public AjaxResult getList() {
 *     return AjaxResult.success(data);
 * }
 * 
 * &#64;GetMapping("/sensitive")
 * &#64;EncryptResponse(value = true, encryptFields = {"data", "userInfo"})
 * public AjaxResult getSensitiveData() {
 *     return AjaxResult.success(data);
 * }
 * </pre>
 * 
 * @author ruoyi
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptResponse {
    
    /**
     * 是否启用加密
     * 
     * @return true-启用加密，false-不加密
     */
    boolean value() default true;
    
    /**
     * 需要加密的字段名称
     * 默认只加密 "data" 字段
     * 
     * @return 字段名称数组
     */
    String[] encryptFields() default {"data"};
    
    /**
     * 加密密钥来源
     * 可以指定配置文件中的key名称
     * 
     * @return 密钥配置key
     */
    String secretKeyConfig() default "";
    
    /**
     * 是否强制加密
     * true: 加密失败时抛出异常
     * false: 加密失败时返回原始数据并记录日志
     * 
     * @return 是否强制加密
     */
    boolean forceEncrypt() default true;
}