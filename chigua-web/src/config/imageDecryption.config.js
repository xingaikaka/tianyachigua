/**
 * 图片解密配置
 */
export const IMAGE_DECRYPTION_CONFIG = {
  // 图片解密密钥（必须与R2 Worker的IMAGE_ENCRYPTION_KEY保持一致）
  encryptionKey: process.env.REACT_APP_IMAGE_ENCRYPTION_KEY || '',
  
  // 是否启用图片解密调试日志
  debug: process.env.REACT_APP_IMAGE_DECRYPT_DEBUG === 'true' || process.env.NODE_ENV === 'development',
  
  // 是否允许解密失败时降级到原始图片（仅开发环境）
  allowFallback: process.env.REACT_APP_ALLOW_IMAGE_DECRYPT_FALLBACK === 'true',
  
  // Canvas渲染配置
  canvas: {
    // 最大渲染尺寸（防止内存溢出）
    maxWidth: parseInt(process.env.REACT_APP_IMAGE_MAX_WIDTH) || 4096,
    maxHeight: parseInt(process.env.REACT_APP_IMAGE_MAX_HEIGHT) || 4096,
    
    // 图片质量（Canvas导出时使用）
    quality: parseFloat(process.env.REACT_APP_IMAGE_QUALITY) || 0.9,
    
    // 是否启用平滑缩放
    smoothing: process.env.REACT_APP_IMAGE_SMOOTHING !== 'false'
  }
};

/**
 * 环境变量配置说明
 * 
 * 在项目根目录创建 .env.local 文件，添加以下配置：
 * 
 * # 图片解密密钥（必须与R2 Worker保持一致）
 * REACT_APP_IMAGE_ENCRYPTION_KEY=your-encryption-key-here
 * 
 * # 是否启用图片解密调试
 * REACT_APP_IMAGE_DECRYPT_DEBUG=true
 * 
 * # 是否允许解密失败降级（开发环境可设为true）
 * REACT_APP_ALLOW_IMAGE_DECRYPT_FALLBACK=true
 * 
 * # Canvas渲染限制
 * REACT_APP_IMAGE_MAX_WIDTH=4096
 * REACT_APP_IMAGE_MAX_HEIGHT=4096
 * REACT_APP_IMAGE_QUALITY=0.9
 * REACT_APP_IMAGE_SMOOTHING=true
 */

export default IMAGE_DECRYPTION_CONFIG;