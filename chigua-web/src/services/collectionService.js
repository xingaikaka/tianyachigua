import { request } from './api';

/**
 * 合集相关API服务
 */
const collectionService = {
  /**
   * 根据分类ID获取合集列表
   * @param {number} categoryId - 分类ID
   * @param {number} page - 页码
   * @param {number} pageSize - 每页大小
   * @returns {Promise} 合集列表
   */
  async getCollectionsByCategory(categoryId, page = 1, pageSize = 20) {
    try {
      const response = await request('/web/api/collection/list', {
        method: 'GET',
        params: {
          categoryId,
          page,
          pageSize
        }
      });
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 根据ID获取合集详情
   * @param {number} collectionId - 合集ID
   * @returns {Promise} 合集详情
   */
  async getCollectionById(collectionId) {
    try {
      const response = await request(`/web/api/collection/${collectionId}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取合集中的视频列表
   * @param {number} collectionId - 合集ID
   * @returns {Promise} 视频列表
   */
  async getCollectionVideos(collectionId) {
    try {
      const response = await request(`/web/api/collection/${collectionId}/videos`);
      return response;
    } catch (error) {
      
      throw error;
    }
  }
};

export default collectionService; 