import CryptoJS from 'crypto-js';

/**
 * 数据解密工具类
 */
class DataDecryption {
  constructor(secretKey) {
    this.originalKey = secretKey || 'default-secret-key-2024';
    this.secretKey = this.normalizeKey(this.originalKey);
    this.timestampTolerance = 5 * 60 * 1000; // 5分钟容差
  }

  /**
   * 将任意长度的密钥标准化为与后端一致的格式
   * 使用SHA-256哈希，与后端的normalizeKey方法保持一致
   * @param {string} originalKey - 原始密钥
   * @returns {string} 标准化后的密钥
   */
  normalizeKey(originalKey) {
    // 使用CryptoJS的SHA-256哈希，确保与后端一致
    const hashedKey = CryptoJS.SHA256(originalKey);
    return hashedKey.toString(CryptoJS.enc.Hex);
  }

  /**
   * AES解密 - 返回原始JSON字符串（保持与后端签名一致）
   * @param {string} encryptedData - 加密的数据（Base64格式，包含IV+密文）
   * @returns {string} 解密后的原始JSON字符串
   */
  decryptToString(encryptedData) {
    try {
      if (!encryptedData) {
        throw new Error('加密数据为空');
      }

      const encryptedBytes = CryptoJS.enc.Base64.parse(encryptedData);
      const iv = CryptoJS.lib.WordArray.create(encryptedBytes.words.slice(0, 4));
      const ciphertext = CryptoJS.lib.WordArray.create(
        encryptedBytes.words.slice(4),
        encryptedBytes.sigBytes - 16
      );

      const decrypted = CryptoJS.AES.decrypt(
        { ciphertext, salt: CryptoJS.lib.WordArray.create([]) },
        CryptoJS.enc.Hex.parse(this.secretKey),
        { iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7 }
      );

      const decryptedString = decrypted.toString(CryptoJS.enc.Utf8);
      if (!decryptedString) {
        throw new Error('解密结果为空，可能密钥错误或数据格式不正确');
      }
      return decryptedString;
    } catch (error) {
      throw new Error(`数据解密失败: ${error.message}`);
    }
  }

  /**
   * AES解密 - 返回解析后的JavaScript对象
   * @param {string} encryptedData - 加密的数据（Base64格式，包含IV+密文）
   * @returns {any} 解密后的数据
   */
  decrypt(encryptedData) {
    return JSON.parse(this.decryptToString(encryptedData));
  }

  /**
   * 验证HMAC签名
   * @param {any} data - 原始数据
   * @param {number} timestamp - 时间戳
   * @param {string} signature - 签名
   * @returns {boolean} 验证结果
   */
  verifySignature(data, timestamp, signature) {
    try {
      const signData = `${JSON.stringify(data)}|${timestamp}`;
      // 使用标准化的Hex密钥进行HMAC验证，与后端保持一致
      const expectedSignature = CryptoJS.HmacSHA256(signData, CryptoJS.enc.Hex.parse(this.secretKey)).toString();
      return expectedSignature === signature;
    } catch (error) {
      
      return false;
    }
  }

  /**
   * 验证时间戳是否在有效期内
   * @param {number} timestamp - 时间戳
   * @returns {boolean} 是否有效
   */
  isValidTimestamp(timestamp) {
    if (!timestamp || typeof timestamp !== 'number') {
      return false;
    }
    
    const now = Date.now();
    const diff = Math.abs(now - timestamp);
    return diff <= this.timestampTolerance;
  }

  /**
   * 解密完整响应
   * @param {object} encryptedResponse - 加密的响应
   * @returns {object} 解密后的响应
   */
  decryptResponse(encryptedResponse) {
    const { data, timestamp, signature, encrypted, ...rest } = encryptedResponse;

    if (!encrypted) {
      return encryptedResponse;
    }

    if (!this.isValidTimestamp(timestamp)) {
      throw new Error('响应数据已过期，请重新请求');
    }

    // 先解密拿到原始JSON字符串，再用原始字符串做签名验证
    // 避免 JSON.stringify(JSON.parse(x)) != x 导致的签名失败
    // 典型场景：Java float 0.0 → Jackson → "0.0"，JS JSON.parse → 0，JSON.stringify → "0"，不一致
    const decryptedString = this.decryptToString(data);

    const signData = `${decryptedString}|${timestamp}`;
    const expectedSignature = CryptoJS.HmacSHA256(
      signData,
      CryptoJS.enc.Hex.parse(this.secretKey)
    ).toString();

    if (expectedSignature !== signature) {
      throw new Error('响应数据完整性验证失败');
    }

    const decryptedData = JSON.parse(decryptedString);

    const result = { ...rest };

    if (result.hasOwnProperty('code') &&
        result.hasOwnProperty('msg') &&
        result.hasOwnProperty('total') &&
        result.hasOwnProperty('rows')) {
      result.rows = decryptedData;
    } else {
      result.data = decryptedData;
    }

    return result;
  }
}

// 创建全局实例
const dataDecryption = new DataDecryption(
  process.env.REACT_APP_DECRYPT_KEY || 'chigua-web-secret-dev-2024'
);

export default dataDecryption;
export { DataDecryption };
