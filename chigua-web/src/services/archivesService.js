import { request } from './api';

const archivesService = {
  /**
   * 根据日期获取往期内容
   * @param {number} pageNum - 页码
   * @param {number} pageSize - 每页大小
   */
  async getArchivesByDate(pageNum = 1, pageSize = 50) {
    try {

      const params = {
        pageNum,
        pageSize
      };

      const response = await request('/web/api/archives/by-date', { params });

      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取标签列表（支持限制数量）
   * @param {number} limit - 限制返回的标签数量，默认18个
   */
  async getAllTags(limit = 18) {
    try {

      const params = { limit };
      const response = await request('/web/api/archives/tags', { params });

      return response;
    } catch (error) {
      
      throw error;
    }
  }
};

export default archivesService;