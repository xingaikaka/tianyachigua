package com.ruoyi.chigua.utils;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 参数缓存Key生成器
 */
@Component("parameterKeyGenerator")
public class CacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加方法名
        keyBuilder.append(method.getName());
        
        // 处理参数
        for (Object param : params) {
            if (param != null) {
                if (param instanceof Map) {
                    // Map参数标准化处理
                    keyBuilder.append(":").append(normalizeMapParam((Map<?, ?>) param));
                } else {
                    keyBuilder.append(":").append(param.toString());
                }
            }
        }
        
        // 生成MD5哈希作为最终key
        return DigestUtils.md5DigestAsHex(keyBuilder.toString().getBytes());
    }
    
    /**
     * 标准化Map参数
     */
    private String normalizeMapParam(Map<?, ?> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().toString().isEmpty())
                .sorted((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}