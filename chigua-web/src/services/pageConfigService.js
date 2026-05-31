import { request } from './api';

/**
 * 页面配置服务类
 * 带缓存机制，减少重复请求，提升性能
 */
class PageConfigService {
  constructor() {
    this.cache = null;
    this.cacheTime = null;
    this.cacheExpiry = 5 * 60 * 1000; // 5分钟缓存
    this.pendingRequest = null; // 防止并发请求
    // 详细配置缓存（包含remark等字段）
    this.detailedCache = null;
    this.detailedCacheTime = null;
    this.detailedPending = null;
  }

  /**
   * 获取配置 - 带缓存机制
   */
  async getConfigs() {
    // 检查缓存是否有效
    if (this.cache && this.cacheTime && (Date.now() - this.cacheTime < this.cacheExpiry)) {
      return this.cache;
    }

    // 防止并发请求
    if (this.pendingRequest) {
      return await this.pendingRequest;
    }

    // 发起新请求
    this.pendingRequest = this.fetchFromServer();
    try {
      const result = await this.pendingRequest;
      this.cache = result;
      this.cacheTime = Date.now();
      return result;
    } finally {
      this.pendingRequest = null;
    }
  }



  /**
   * 从服务器获取配置
   */
  async fetchFromServer() {
    try {
      const response = await request('/web/api/pageconfig/all');
      
      if (response.code === 200) {
        // 将PageConfig对象转换为简单的键值对
        const configs = {};
        if (response.data.configs) {
          Object.keys(response.data.configs).forEach(key => {
            const config = response.data.configs[key];
            // 根据配置类型选择内容字段
            configs[key] = config.configType === 'rich_text' 
              ? config.richContent 
              : config.basicContent;
          });
        }

        return configs;
      }
      throw new Error('获取配置失败');
    } catch (error) {
      return {};
    }
  }

  /**
   * 获取包含详细字段（如remark）的配置映射（带缓存）
   */
  async getAllDetailed() {
    // 缓存有效直接返回
    if (this.detailedCache && this.detailedCacheTime && (Date.now() - this.detailedCacheTime < this.cacheExpiry)) {
      return this.detailedCache;
    }

    // 并发保护
    if (this.detailedPending) {
      return await this.detailedPending;
    }

    this.detailedPending = (async () => {
      try {
        const response = await request('/web/api/pageconfig/all');
        const detailed = (response && response.data && response.data.configs) ? response.data.configs : {};
        this.detailedCache = detailed;
        this.detailedCacheTime = Date.now();
        return detailed;
      } catch (error) {
        return {};
      } finally {
        this.detailedPending = null;
      }
    })();

    return await this.detailedPending;
  }

  /**
   * 刷新配置 - 清除缓存后重新获取
   */
  async refreshConfigs() {
    this.clearCache();
    return await this.getConfigs();
  }

  /**
   * 强制刷新配置 - 清除缓存并强制从服务器获取
   */
  async forceRefresh() {
    this.clearCache();
    this.pendingRequest = null; // 清除待处理请求
    return await this.fetchFromServer();
  }

  /**
   * 清除缓存
   */
  clearCache() {
    this.cache = null;
    this.cacheTime = null;
  }

  /**
   * 检查缓存是否有效
   */
  isCacheValid() {
    return this.cache && this.cacheTime && (Date.now() - this.cacheTime < this.cacheExpiry);
  }

  /**
   * 获取缓存状态信息
   */
  getCacheInfo() {
    return {
      hasCache: !!this.cache,
      cacheTime: this.cacheTime,
      isValid: this.isCacheValid(),
      expiresIn: this.cacheTime ? Math.max(0, this.cacheExpiry - (Date.now() - this.cacheTime)) : 0
    };
  }

  /**
   * 获取特定配置
   */
  async getConfig(key) {
    const configs = await this.getConfigs();
    return configs[key];
  }

  /**
   * 获取富文本配置
   */
  async getRichTextConfig(key) {
    const config = await this.getConfig(key);
    return config?.richContent || '';
  }

  /**
   * 获取基础配置
   */
  async getBasicConfig(key) {
    const config = await this.getConfig(key);
    return config?.basicContent || '';
  }

  /**
   * 根据配置类型获取配置列表
   */
  async getConfigsByType(configType) {
    try {

      const response = await request(`/web/api/pageconfig/type/${configType}`);

      return response.data || [];
    } catch (error) {
      
      return [];
    }
  }

  /**
   * 根据配置分类获取配置列表
   */
  async getConfigsByCategory(configCategory) {
    try {

      const response = await request(`/web/api/pageconfig/category/${configCategory}`);

      return response.data || [];
    } catch (error) {
      
      return [];
    }
  }

  /**
   * 获取站点基础配置
   */
  async getSiteConfigs() {
    try {

      const response = await request('/web/api/pageconfig/site');

      return response.data || {};
    } catch (error) {
      
      return {};
    }
  }

  /**
   * 获取底部页面信息（已改为从缓存获取）
   * @deprecated 建议直接使用 getConfigs() 获取所有配置
   */
  async getFooterInfo() {
    try {

      const configs = await this.getConfigs();
      const footerInfo = configs['footer_info'];

      // 为了保持API兼容性，包装成原有格式
      return {
        code: 200,
        msg: '获取底部页面信息成功',
        data: {
          noticeContent: footerInfo || ''
        }
      };
    } catch (error) {
      
      throw error;
    }
  }


}

// 创建单例实例
const pageConfigService = new PageConfigService();

export default pageConfigService;