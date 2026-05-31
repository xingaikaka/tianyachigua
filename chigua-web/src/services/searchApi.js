import { request } from './api';

const searchService = {
  /**
   * 根据关键词搜索视频
   * @param {string} keyword - 搜索关键词
   * @param {number} pageNum - 页码
   * @param {number} pageSize - 每页大小
   */
  async searchVideos(keyword, pageNum = 1, pageSize = 20) {
    try {

      const params = {
        keyword: keyword.trim(),
        pageNum,
        pageSize
      };
      
      const response = await request('/web/api/search/videos', { params });

      return response;
    } catch (error) {
      
      throw error;
    }
  }
};

export default searchService;
