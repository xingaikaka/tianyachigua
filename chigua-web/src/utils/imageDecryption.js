/**
 * 前端图片解密工具
 * 与 R2 Worker 的 AES-GCM 加密算法保持一致
 */

class ImageDecryption {
  constructor(masterKey) {
    this.masterKey = masterKey || process.env.REACT_APP_IMAGE_ENCRYPTION_KEY || '';
    this.textEncoder = new TextEncoder();
    this.textDecoder = new TextDecoder();
    
    // 🔍 调试信息：构造函数
    // 构造函数 - 移除打印信息
    
    if (!this.masterKey) {
      
    }
  }

  /**
   * 检查文件是否是加密的图片
   */
  isEncryptedImage(contentType, customMetadata = {}) {
    // 检查响应头标识
    if (contentType === 'application/x-chigua-encrypted') {
      return true;
    }
    
    // 检查自定义元数据
    if (customMetadata.encrypted === 'true') {
      return true;
    }
    
    return false;
  }

  /**
   * 从文件路径和主密钥派生文件专用密钥
   * 与 R2 Worker 的 deriveFileKey 方法保持一致
   */
  async deriveFileKey(filePath) {
    try {
      // 使用PBKDF2从主密钥和文件路径派生唯一密钥
      const masterKeyBuffer = this.textEncoder.encode(this.masterKey);
      const saltBuffer = this.textEncoder.encode(filePath + '_chigua_salt_2025');

      // 导入主密钥
      const masterKey = await crypto.subtle.importKey(
        'raw',
        masterKeyBuffer,
        { name: 'PBKDF2' },
        false,
        ['deriveKey']
      );

      // 派生文件专用密钥
      const fileKey = await crypto.subtle.deriveKey(
        {
          name: 'PBKDF2',
          salt: saltBuffer,
          iterations: 100000, // 10万次迭代
          hash: 'SHA-256'
        },
        masterKey,
        { name: 'AES-GCM', length: 256 },
        true,
        ['decrypt']
      );

      // 导出为原始密钥数据
      const exportedKey = await crypto.subtle.exportKey('raw', fileKey);
      return exportedKey;
    } catch (error) {
      throw new Error('图片解密密钥派生失败');
    }
  }

  /**
   * 解密图片文件
   * 与 R2 Worker 的 decryptFile 方法保持一致
   */
  async decryptImage(encryptedBuffer, filePath, metadata = {}) {
    // 开始解密图片
    
    if (!metadata.encrypted) {
      // 图片未加密，直接返回
      // 图片未加密，直接返回
      return encryptedBuffer;
    }

    if (!this.masterKey) {
      throw new Error('图片解密密钥未配置');
    }

    try {

      // 1. 分离IV和加密内容
      const encryptedArray = new Uint8Array(encryptedBuffer);
      const iv = encryptedArray.slice(0, 12); // 前12字节是IV (GCM推荐12字节)
      const ciphertext = encryptedArray.slice(12); // 剩余部分是密文
      
      // 解析IV和密文

      // 2. 生成文件专用的解密密钥
      const fileKeyBuffer = await this.deriveFileKey(filePath);
      // 文件密钥生成完成
      
      // 3. 导入密钥
      const cryptoKey = await crypto.subtle.importKey(
        'raw',
        fileKeyBuffer,
        { name: 'AES-GCM' },
        false,
        ['decrypt']
      );
      // 密钥导入成功

      // 4. 解密文件内容
      // 开始AES-GCM解密
      const decryptedBuffer = await crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv: iv,
          tagLength: 128 // 16字节认证标签
        },
        cryptoKey,
        ciphertext
      );

      // 解密成功
      return decryptedBuffer;

    } catch (error) {
      
      throw new Error('图片解密失败，可能文件已损坏或密钥不正确');
    }
  }

  /**
   * 从URL中提取文件路径（用于密钥派生）
   */
  extractFilePathFromUrl(url) {
    try {
      const urlObj = new URL(url);
      
      // 从key参数中获取
      const keyParam = urlObj.searchParams.get('key');
      if (keyParam) {
        return keyParam;
      }
      
      // 从路径中提取
      let path = urlObj.pathname;
      if (path.startsWith('/files/')) {
        path = path.substring(7); // 移除 '/files/' 前缀
      }
      
      return decodeURIComponent(path);
    } catch (error) {
      
      return '';
    }
  }

  /**
   * 检查是否支持图片解密
   */
  isSupported() {
    return !!(window.crypto && window.crypto.subtle && this.masterKey);
  }

  /**
   * 获取错误信息
   */
  getErrorMessage() {
    if (!window.crypto || !window.crypto.subtle) {
      return '浏览器不支持Web Crypto API，无法解密图片';
    }
    
    if (!this.masterKey) {
      return '图片解密密钥未配置，请联系管理员';
    }
    
    return '未知错误';
  }
}

/**
 * 处理图片URL（兼容函数）
 * 移除decrypt参数，确保获取的是加密数据
 */
export function processImageUrl(url) {
  if (!url) return url;
  
  try {
    const urlObj = new URL(url);
    // 移除decrypt参数，确保获取加密数据
    urlObj.searchParams.delete('decrypt');
    return urlObj.toString();
  } catch (error) {
    
    return url;
  }
}

/**
 * 获取解密后的图片URL（兼容函数）
 * 注意：此函数仍会创建blob URL，建议迁移到SecureImage组件
 */
export async function getDecryptedImageUrl(url) {
  if (!url) return url;
  
  try {
    // 如果不包含签名参数，直接返回
    if (!url.includes('signature=') && !url.includes('key=')) {
      return url;
    }

    // 检查是否支持解密
    if (!imageDecryption.isSupported()) {
      
      return processImageUrl(url);
    }

    // 获取加密的图片数据
    const processedUrl = processImageUrl(url);
    const response = await fetch(processedUrl);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const contentType = response.headers.get('content-type') || '';
    const isEncrypted = response.headers.get('x-encrypted') === 'true';
    const originalContentType = response.headers.get('x-original-content-type') || 'image/jpeg';

    const imageBuffer = await response.arrayBuffer();
    let finalBuffer = imageBuffer;
    let finalContentType = contentType;

    // 如果是加密图片，进行解密
    if (isEncrypted || imageDecryption.isEncryptedImage(contentType)) {
      const filePath = imageDecryption.extractFilePathFromUrl(url);
      finalBuffer = await imageDecryption.decryptImage(
        imageBuffer, 
        filePath, 
        { encrypted: true }
      );
      finalContentType = originalContentType;
    }

    // 创建blob URL（不安全，但保持兼容性）
    const blob = new Blob([finalBuffer], { type: finalContentType });
    const blobUrl = URL.createObjectURL(blob);

    return blobUrl;
    
  } catch (error) {
    
    return processImageUrl(url);
  }
}

// 创建全局实例（确保传递密钥）
const imageDecryption = new ImageDecryption(process.env.REACT_APP_IMAGE_ENCRYPTION_KEY);

export default imageDecryption;
export { ImageDecryption };