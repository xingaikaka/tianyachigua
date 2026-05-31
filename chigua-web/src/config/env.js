/**
 * 环境变量配置整合
 */

// API 配置
export const API_CONFIG = {
  // 根据环境自动选择API地址
  baseUrl: process.env.REACT_APP_API_BASE_URL || (process.env.NODE_ENV === 'production' ? '/prod-api' : 'http://localhost:8080'),
  timeout: parseInt(process.env.REACT_APP_API_TIMEOUT) || 30000,
};

// 数据解密配置
export const DATA_DECRYPTION_CONFIG = {
  secretKey: process.env.REACT_APP_DECRYPT_KEY || 'chigua-web-secret-dev-2024',
  timestampTolerance: parseInt(process.env.REACT_APP_TIMESTAMP_TOLERANCE) || 5 * 60 * 1000,
  allowFallback: process.env.REACT_APP_ALLOW_DECRYPT_FALLBACK === 'true',
  debug: process.env.REACT_APP_DECRYPT_DEBUG === 'true' || process.env.NODE_ENV === 'development',
  maxRetries: parseInt(process.env.REACT_APP_DECRYPT_MAX_RETRIES) || 3,
  retryInterval: parseInt(process.env.REACT_APP_DECRYPT_RETRY_INTERVAL) || 1000
};

// 图片解密配置
export const IMAGE_DECRYPTION_CONFIG = {
  encryptionKey: process.env.REACT_APP_IMAGE_ENCRYPTION_KEY || '',
  debug: process.env.REACT_APP_IMAGE_DECRYPT_DEBUG === 'true' || process.env.NODE_ENV === 'development',
  allowFallback: process.env.REACT_APP_ALLOW_IMAGE_DECRYPT_FALLBACK === 'true',
  canvas: {
    maxWidth: parseInt(process.env.REACT_APP_IMAGE_MAX_WIDTH) || 4096,
    maxHeight: parseInt(process.env.REACT_APP_IMAGE_MAX_HEIGHT) || 4096,
    quality: parseFloat(process.env.REACT_APP_IMAGE_QUALITY) || 0.9,
    smoothing: process.env.REACT_APP_IMAGE_SMOOTHING !== 'false'
  }
};

// 开发环境配置
export const DEV_CONFIG = {
  showDebugInfo: process.env.REACT_APP_SHOW_DEBUG === 'true' || process.env.NODE_ENV === 'development',
  enableConsoleLog: process.env.REACT_APP_ENABLE_CONSOLE_LOG !== 'false',
  mockApi: process.env.REACT_APP_MOCK_API === 'true',
};

// 功能开关
export const FEATURE_FLAGS = {
  enableSecureImage: process.env.REACT_APP_ENABLE_SECURE_IMAGE !== 'false', // 默认启用
  enableDataEncryption: process.env.REACT_APP_ENABLE_DATA_ENCRYPTION !== 'false', // 默认启用
  enableImageDecryption: process.env.REACT_APP_ENABLE_IMAGE_DECRYPTION !== 'false', // 默认启用
};

// 导出统一配置对象
export const ENV_CONFIG = {
  api: API_CONFIG,
  dataDecryption: DATA_DECRYPTION_CONFIG,
  imageDecryption: IMAGE_DECRYPTION_CONFIG,
  dev: DEV_CONFIG,
  features: FEATURE_FLAGS,
};

export default ENV_CONFIG;
