/**
 * 数据解密配置
 */
export const DECRYPTION_CONFIG = {
  // 解密密钥（生产环境请使用环境变量）
  secretKey: process.env.REACT_APP_DECRYPT_KEY || 'chigua-web-secret-dev-2024',
  
  // 时间戳容差（毫秒）
  timestampTolerance: parseInt(process.env.REACT_APP_TIMESTAMP_TOLERANCE) || 5 * 60 * 1000,
  
  // 是否允许解密失败时降级到原始数据（仅开发环境）
  allowFallback: process.env.REACT_APP_ALLOW_DECRYPT_FALLBACK === 'true',
  
  // 是否启用解密调试日志
  debug: process.env.REACT_APP_DECRYPT_DEBUG === 'true' || process.env.NODE_ENV === 'development',
  
  // 最大重试次数
  maxRetries: parseInt(process.env.REACT_APP_DECRYPT_MAX_RETRIES) || 3,
  
  // 重试间隔（毫秒）
  retryInterval: parseInt(process.env.REACT_APP_DECRYPT_RETRY_INTERVAL) || 1000
};

/**
 * 环境变量配置说明
 * 
 * 在项目根目录创建 .env.local 文件，添加以下配置：
 * 
 * # 数据解密密钥（必须与后端保持一致）
 * REACT_APP_DECRYPT_KEY=your-secret-key-here
 * 
 * # 时间戳容差（默认5分钟）
 * REACT_APP_TIMESTAMP_TOLERANCE=300000
 * 
 * # 是否允许解密失败降级（开发环境可设为true）
 * REACT_APP_ALLOW_DECRYPT_FALLBACK=true
 * 
 * # 是否启用解密调试
 * REACT_APP_DECRYPT_DEBUG=true
 * 
 * # 解密重试配置
 * REACT_APP_DECRYPT_MAX_RETRIES=3
 * REACT_APP_DECRYPT_RETRY_INTERVAL=1000
 */

export default DECRYPTION_CONFIG;
