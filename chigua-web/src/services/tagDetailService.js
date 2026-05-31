import { request } from './api';

const tagDetailService = {
  /**
   * 根据标签ID获取相关视频
   * @param {number} tagId - 标签ID
   * @param {number} pageNum - 页码
   * @param {number} pageSize - 每页大小
   */
  async getVideosByTag(tagId, pageNum = 1, pageSize = 20) {
    try {

      const params = {
        pageNum,
        pageSize
      };

      const response = await request(`/web/api/tags/${tagId}/videos`, { params });

      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 根据标签ID获取标签信息
   * @param {number} tagId - 标签ID
   */
  async getTagInfo(tagId) {
    try {

      const response = await request(`/web/api/tags/${tagId}/info`);

      return response;
    } catch (error) {
      
      throw error;
    }
  }
};

export default tagDetailService;