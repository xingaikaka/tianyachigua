/**
 * Image Encryption Module for Chigua R2 Worker
 * 
 * Features:
 * - AES-256-GCM encryption for image files
 * - Unique encryption key per file (derived from master key + file path)
 * - Authenticated encryption with integrity verification
 * - Transparent encryption/decryption
 */

export interface EncryptionConfig {
  enabled: boolean;
  masterKey: string;
  algorithm: 'AES-GCM';
  keyLength: 256;
}

export interface EncryptedFileMetadata {
  encrypted: boolean;
  algorithm?: string;
  keyDerivation?: string;
  iv?: string; // Base64 encoded
}

export class ImageEncryption {
  private config: EncryptionConfig;
  private textEncoder = new TextEncoder();
  private textDecoder = new TextDecoder();

  constructor(config: EncryptionConfig) {
    this.config = config;
  }

  /**
   * 检查文件是否应该加密（仅图片文件）
   */
  shouldEncrypt(filePath: string, contentType: string): boolean {
    if (!this.config.enabled) {
      return false;
    }

    // 只加密图片文件
    const imageTypes = [
      'image/jpeg', 'image/jpg', 'image/png', 'image/gif', 
      'image/webp', 'image/bmp', 'image/svg+xml'
    ];
    
    return imageTypes.includes(contentType) || this.isImageFile(filePath);
  }

  /**
   * 加密文件内容
   */
  async encryptFile(fileContent: ArrayBuffer, filePath: string): Promise<{
    encryptedContent: ArrayBuffer;
    metadata: EncryptedFileMetadata;
  }> {
    if (!this.config.enabled) {
      throw new Error('加密功能未启用');
    }

    console.log('🔐 开始加密文件:', filePath);

    // 1. 生成文件专用的加密密钥
    const fileKey = await this.deriveFileKey(filePath);
    
    // 2. 生成随机IV
    const iv = crypto.getRandomValues(new Uint8Array(12)); // GCM推荐12字节IV
    
    // 3. 导入密钥
    const cryptoKey = await crypto.subtle.importKey(
      'raw',
      fileKey,
      { name: 'AES-GCM' },
      false,
      ['encrypt']
    );

    // 4. 加密文件内容
    const encryptedBuffer = await crypto.subtle.encrypt(
      {
        name: 'AES-GCM',
        iv: iv,
        tagLength: 128 // 16字节认证标签
      },
      cryptoKey,
      fileContent
    );

    // 5. 组合IV和加密内容
    const combinedBuffer = new Uint8Array(iv.length + encryptedBuffer.byteLength);
    combinedBuffer.set(iv, 0);
    combinedBuffer.set(new Uint8Array(encryptedBuffer), iv.length);

    const metadata: EncryptedFileMetadata = {
      encrypted: true,
      algorithm: 'AES-256-GCM',
      keyDerivation: 'PBKDF2-SHA256',
      iv: this.arrayBufferToBase64(iv)
    };

    console.log('✅ 文件加密完成:', filePath, '原始大小:', fileContent.byteLength, '加密后大小:', combinedBuffer.byteLength);

    return {
      encryptedContent: combinedBuffer.buffer,
      metadata
    };
  }

  /**
   * 解密文件内容
   */
  async decryptFile(encryptedContent: ArrayBuffer, filePath: string, metadata: EncryptedFileMetadata): Promise<ArrayBuffer> {
    if (!metadata.encrypted) {
      // 文件未加密，直接返回
      return encryptedContent;
    }

    if (!this.config.enabled) {
      throw new Error('加密功能未启用，无法解密文件');
    }

    console.log('🔓 开始解密文件:', filePath);

    // 1. 分离IV和加密内容
    const encryptedBuffer = new Uint8Array(encryptedContent);
    const iv = encryptedBuffer.slice(0, 12); // 前12字节是IV
    const ciphertext = encryptedBuffer.slice(12); // 剩余部分是密文

    // 2. 生成文件专用的解密密钥
    const fileKey = await this.deriveFileKey(filePath);
    
    // 3. 导入密钥
    const cryptoKey = await crypto.subtle.importKey(
      'raw',
      fileKey,
      { name: 'AES-GCM' },
      false,
      ['decrypt']
    );

    // 4. 解密文件内容
    try {
      const decryptedBuffer = await crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv: iv,
          tagLength: 128
        },
        cryptoKey,
        ciphertext
      );

      console.log('✅ 文件解密完成:', filePath, '解密后大小:', decryptedBuffer.byteLength);
      return decryptedBuffer;

    } catch (error) {
      console.error('❌ 文件解密失败:', filePath, error);
      throw new Error('文件解密失败，可能文件已损坏或密钥不正确');
    }
  }

  /**
   * 从文件路径和主密钥派生文件专用密钥
   */
  private async deriveFileKey(filePath: string): Promise<ArrayBuffer> {
    // 使用PBKDF2从主密钥和文件路径派生唯一密钥
    const masterKeyBuffer = this.textEncoder.encode(this.config.masterKey);
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
      ['encrypt', 'decrypt']
    );

    // 导出为原始密钥数据
    const exportedKey = await crypto.subtle.exportKey('raw', fileKey);
    return exportedKey as ArrayBuffer;
  }

  /**
   * 检查是否是图片文件（基于文件扩展名）
   */
  private isImageFile(filePath: string): boolean {
    const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'];
    const extension = filePath.split('.').pop()?.toLowerCase();
    return extension ? imageExtensions.includes(extension) : false;
  }

  /**
   * ArrayBuffer转Base64
   */
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
  }

  /**
   * Base64转ArrayBuffer
   */
  private base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
  }

  /**
   * 验证加密配置
   */
  validateConfig(): { valid: boolean; error?: string } {
    if (!this.config.masterKey || this.config.masterKey.length < 32) {
      return { valid: false, error: '主密钥长度必须至少32字符' };
    }

    if (this.config.algorithm !== 'AES-GCM') {
      return { valid: false, error: '不支持的加密算法' };
    }

    return { valid: true };
  }
} 