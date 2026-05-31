import { request } from './api';
import apiCacheService from './apiCacheService';

const tagsService = {
  /**
   * 获取所有标签 (已废弃，请使用 archivesService.getAllTags)
   * @deprecated 使用 archivesService.getAllTags() 替代
   * @returns {Promise} 标签列表
   */
  async getAllTags() {
    try {
      // 重定向到新的 archives API
      const response = await request('/web/api/archives/tags', { 
        params: { limit: 1000 } // 兼容旧行为，获取大量标签
      });

      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 分页获取标签
   * @param {number} pageNum - 页码
   * @param {number} pageSize - 每页大小
   * @returns {Promise} 分页标签数据
   */
  async getTagsPaginated(pageNum = 1, pageSize = 150) {
    try {
      const params = {
        pageNum,
        pageSize
      };
      // 按页缓存标签（各页独立缓存键）
      const response = await apiCacheService.getApiData(
        'TAGS_PAGE',
        params,
        (p) => request('/web/api/tags/page', { params: p }),
        { cacheDuration: 5 * 60 * 1000 }
      );

      return response;
    } catch (error) {
      
      throw error;
    }
  }
};

export default tagsService;