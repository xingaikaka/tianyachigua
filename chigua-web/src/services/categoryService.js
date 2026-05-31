import { request } from './api';

/**
 * 分类相关API服务
 */
const categoryService = {
  /**
   * 获取分类列表
   * @returns {Promise} 分类列表
   */
  async getCategoryList() {
    try {
      const response = await request('/web/api/category/list');
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取分类版本信息
   */
  async getCategoryVersion() {
    try {
      return await request('/web/api/category/version');
    } catch (error) {
      throw error;
    }
  },

  /**
   * 根据ID获取分类详情
   * @param {number} categoryId - 分类ID
   * @param {object} options - 选项（如 forceRefresh）
   * @returns {Promise} 分类详情
   */
  async getCategoryById(categoryId, options = {}) {
    try {
      const response = await request(`/web/api/category/${categoryId}/detail`, {
        ...options
      });
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 短视频模式：按分类或标签查询短视频列表（带缓存）
   */
  async getShortVideos(params) {
    const { 
      categoryId, 
      title,
      tagId,
      tagIds,
      author,
      authors,
      pageNum = 1, 
      pageSize = 20,
      _forceRefresh = false,
      _timestamp
    } = params || {};
    
    // 导入缓存服务
    const { default: apiCacheService } = await import('./apiCacheService');
    
    // 构建缓存键，按分类、标题、标签和作者区分
    const titleKey = title && title.trim() ? title.trim() : 'all';
    const tagIdsKey = Array.isArray(tagIds) && tagIds.length > 0 ? tagIds.join(',') : (tagId || 'all');
    const authorsKey = Array.isArray(authors) && authors.length > 0 ? authors.join(',') : (author || 'all');
    const cacheKey = _forceRefresh 
      ? `SHORT_VIDEOS_${categoryId || 'all'}_${titleKey}_${tagIdsKey}_${authorsKey}_${pageNum}_${pageSize}_${_timestamp || Date.now()}`
      : `SHORT_VIDEOS_${categoryId || 'all'}_${titleKey}_${tagIdsKey}_${authorsKey}_${pageNum}_${pageSize}`;
    
    // 🔧 强制刷新时先清除现有缓存
    if (_forceRefresh) {
      try {
        const { default: apiCacheService } = await import('./apiCacheService');
        const baseCacheKey = `SHORT_VIDEOS_${categoryId || 'all'}_${titleKey}_${tagIdsKey}_${authorsKey}_${pageNum}_${pageSize}`;
        apiCacheService.clearCache(baseCacheKey);
      } catch (_) {}
    }
    
    try {
      // 使用缓存服务，30秒缓存
      const response = await apiCacheService.getApiData(
        cacheKey,
        { categoryId, title, tagId, tagIds, author, authors, pageNum, pageSize },
        async (params) => {
          const query = new URLSearchParams();
          if (params.categoryId != null) query.set('categoryId', params.categoryId);
          if (params.title != null && params.title.trim()) query.set('title', params.title.trim());
          // 优先使用多个标签ID
          if (Array.isArray(params.tagIds) && params.tagIds.length > 0) {
            query.set('tagIds', params.tagIds.join(','));
          } else if (params.tagId != null) {
            query.set('tagId', params.tagId);
          }
          // 优先使用多个作者
          if (Array.isArray(params.authors) && params.authors.length > 0) {
            query.set('authors', params.authors.join(','));
          } else if (params.author != null && params.author !== '') {
            query.set('author', params.author);
          }
          query.set('pageNum', params.pageNum);
          query.set('pageSize', params.pageSize);
          return await request(`/web/api/category/videos/short?${query.toString()}`);
        },
        {
          cacheDuration: _forceRefresh ? 0 : 30 * 1000, // 强制刷新时不缓存
          forceRefresh: _forceRefresh, // 传递强制刷新标记
          skipCacheIf: (res) => {
            // 如果返回空数据，不缓存
            const rows = res?.rows || [];
            return !Array.isArray(rows) || rows.length === 0;
          }
        }
      );
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 获取分类的广告配置信息
   * @param {number} categoryId - 分类ID
   * @param {object} options - 选项（如 forceRefresh）
   * @returns {Promise} 分类配置信息（包含广告显示模式和间隔）
   */
  async getCategoryAdConfig(categoryId, options = {}) {
    try {
      const response = await this.getCategoryById(categoryId, options);
      if (response && response.data) {
        return {
          ...response,
          data: {
            ad_display_mode: response.data.adDisplayMode || 3,
            ad_interval: response.data.adInterval || 5,
            ...response.data
          }
        };
      }
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 获取分类下所有去重的标签
   * @param {number} categoryId - 分类ID
   * @returns {Promise} 标签列表
   */
  async getCategoryTags(categoryId) {
    try {
      const response = await request(`/web/api/category/${categoryId}/tags`);
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 获取分类下所有去重的作者
   * @param {number} categoryId - 分类ID
   * @returns {Promise} 作者列表
   */
  async getCategoryAuthors(categoryId) {
    try {
      const response = await request(`/web/api/category/${categoryId}/authors`);
      return response;
    } catch (error) {
      throw error;
    }
  }
};

export default categoryService; 
