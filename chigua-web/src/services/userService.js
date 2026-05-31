import { request } from '../services/api';

const userService = {
  // 获取用户列表
  getUsers: async (pageNum = 1, pageSize = 20, username = '') => {
    try {
      const params = {
        pageNum,
        pageSize,
        status: 1 // 默认只查询启用的用户
      };
      if (username && username.trim()) {
        params.username = username.trim();
      }
      const response = await request('/web/api/redgifs/users/list', { params });
      return response;
    } catch (error) {
      console.error('获取用户列表失败:', error);
      throw error;
    }
  },

  // 获取用户详情
  getUserByUsername: async (username) => {
    try {
      const response = await request(`/web/api/redgifs/users/username/${username}`);
      return response;
    } catch (error) {
      console.error('获取用户详情失败:', error);
      throw error;
    }
  },

  // 按视频ID获取单个视频（与用户视频列表格式一致）
  getUserVideoById: async (videoId) => {
    try {
      const response = await request(`/web/api/redgifs/users/video/${videoId}`);
      return response;
    } catch (error) {
      console.error('获取视频详情失败:', error);
      throw error;
    }
  },

  // 获取用户视频列表
  getUserVideos: async (userId, pageNum = 1, pageSize = 20) => {
    try {
      const response = await request(`/web/api/redgifs/users/${userId}/videos`, {
        params: {
          pageNum,
          pageSize
        }
      });
      return response;
    } catch (error) {
      console.error('获取用户视频列表失败:', error);
      throw error;
    }
  }
};

export default userService;
