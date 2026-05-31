import { request } from './api';

const noticeService = {
  /**
   * 获取底部描述信息
   */
  async getBottomDescription() {
    try {

      const response = await request('/web/api/notice/bottom-description');

      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取最新公告信息（根据分类类型）
   */
  async getLatestAnnouncement(categoryType = '公告') {
    try {

      const response = await request(`/web/api/notice/latest?categoryType=${encodeURIComponent(categoryType)}`);

      return response;
    } catch (error) {
      
      throw error;
    }
  }
};

export default noticeService;