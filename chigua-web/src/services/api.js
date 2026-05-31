// 导入解密工具
import dataDecryption from '../utils/dataDecryption';
// 导入API配置
import { API_CONFIG } from '../config/env';
// 导入请求去重服务
import requestDedupeService from './requestDedupeService';

// API基础配置 - 使用统一的环境配置
const API_BASE_URL = API_CONFIG.baseUrl;

/**
 * 发送HTTP请求的通用方法（支持响应解密和去重）
 * @param {string} url - 请求URL
 * @param {object} options - 请求配置
 * @returns {Promise} - 请求结果
 */
async function request(url, options = {}) {
  // 使用请求去重服务
  return await requestDedupeService.dedupeRequest(url, options, async () => {
    return await executeRequest(url, options);
  });
}

/**
 * 执行实际的HTTP请求
 * @param {string} url - 请求URL
 * @param {object} options - 请求配置
 * @returns {Promise} - 请求结果
 */
async function executeRequest(url, options = {}) {
  // 处理URL参数
  let requestUrl = url;
  if (options.params) {
    const searchParams = new URLSearchParams();
    Object.keys(options.params).forEach(key => {
      if (options.params[key] !== null && options.params[key] !== undefined) {
        searchParams.append(key, options.params[key]);
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      requestUrl += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const config = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  };

  // 移除params，避免传递给fetch
  delete config.params;

  try {
    const response = await fetch(`${API_BASE_URL}${requestUrl}`, config);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const rawData = await response.json();
    
    // 检测并处理加密响应
    const finalData = await handleEncryptedResponse(rawData, url);
    
    return finalData;
  } catch (error) {
    throw error;
  }
}

/**
 * 处理加密响应数据
 * @param {object} rawData - 原始响应数据
 * @param {string} url - 请求URL（用于日志）
 * @returns {Promise<object>} - 处理后的数据
 */
async function handleEncryptedResponse(rawData, url) {
  try {
    // 检查是否为加密响应
    if (rawData && rawData.encrypted === true) {

      // 执行解密
      const decryptedData = dataDecryption.decryptResponse(rawData);

      return decryptedData;
    }
    
    // 非加密响应直接返回
    return rawData;
  } catch (decryptError) {

    // 开发环境显示详细错误信息
    if (process.env.NODE_ENV === 'development') {
      
    }
    
    // 检查是否允许降级到原始数据
    const allowFallback = process.env.REACT_APP_ALLOW_DECRYPT_FALLBACK === 'true';
    if (allowFallback && process.env.NODE_ENV === 'development') {
      
      return rawData;
    }
    
    // 生产环境抛出用户友好的错误
    throw new Error('数据处理失败，请刷新页面重试');
  }
}

/**
 * 配置解密参数
 * @param {object} config - 配置参数
 */
function configureDecryption(config) {
  if (config.secretKey) {
    dataDecryption.secretKey = config.secretKey;
  }
  if (config.timestampTolerance) {
    dataDecryption.timestampTolerance = config.timestampTolerance;
  }
}

export { request, API_BASE_URL, configureDecryption };
