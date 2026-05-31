import { request } from './api';

const videoService = {
  /**
   * 获取视频列表
   * @param {Object} params - 查询参数
   * @param {number|null} params.categoryId - 分类ID，可选
   * @param {number|null} params.tagId - 标签ID，可选（兼容旧接口）
   * @param {Array<number>|null} params.tagIds - 标签ID列表，可选（多标签筛选）
   * @param {string|null} params.author - 作者，可选（兼容旧接口）
   * @param {Array<string>|null} params.authors - 作者列表，可选（多作者筛选）
   * @param {number} params.pageNum - 页码，默认1
   * @param {number} params.pageSize - 每页大小，默认20
   */
  async getVideoList(params = {}) {
    try {
      const { categoryId, title, tagId, tagIds, author, authors, pageNum = 1, pageSize = 20 } = params;
      
      // 构建查询参数
      const queryParams = new URLSearchParams({
        pageNum: pageNum.toString(),
        pageSize: pageSize.toString()
      });
      
      // 如果有分类ID，添加到查询参数中
      if (categoryId) {
        queryParams.append('categoryId', categoryId.toString());
      }
      
      // 如果有标题搜索，添加到查询参数中
      if (title && title.trim()) {
        queryParams.append('title', title.trim());
      }
      
      // 优先使用多个标签ID
      if (Array.isArray(tagIds) && tagIds.length > 0) {
        queryParams.append('tagIds', tagIds.join(','));
      } else if (tagId) {
        queryParams.append('tagId', tagId.toString());
      }
      
      // 优先使用多个作者
      if (Array.isArray(authors) && authors.length > 0) {
        queryParams.append('authors', authors.join(','));
      } else if (author && author !== '') {
        queryParams.append('author', author);
      }
      
      // 使用优化版接口，返回包含tags和firstVideoUrl的数据
      const response = await request(`/web/api/category/videos/optimized?${queryParams}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 根据分类获取视频列表（分页模式使用优化版接口）
   * @param {number} categoryId - 分类ID
   * @param {number} pageNum - 页码
   * @param {number} pageSize - 每页大小
   * @param {number|null} tagId - 标签ID，可选（兼容旧接口）
   * @param {Array<number>|null} tagIds - 标签ID列表，可选（多标签筛选）
   * @param {string|null} author - 作者，可选（兼容旧接口）
   * @param {Array<string>|null} authors - 作者列表，可选（多作者筛选）
   * @param {string|null} title - 标题搜索，可选
   */
  async getVideosByCategory(categoryId, pageNum = 1, pageSize = 20, tagId = null, tagIds = null, author = null, authors = null, title = null) {
    return this.getVideoList({ categoryId, title, tagId, tagIds, author, authors, pageNum, pageSize });
  },

  /**
   * 获取所有视频列表（不区分分类）
   * @param {number} pageNum - 页码
   * @param {number} pageSize - 每页大小
   */
  async getAllVideos(pageNum = 1, pageSize = 20) {
    return this.getVideoList({ pageNum, pageSize });
  },

  /**
   * 获取视频详情
   * @param {number|string} id - 视频ID
   * @param {Object} contextParams - 可选的上下文参数（fromCategory, searchKeyword, sortType）
   */
  async getVideoDetail(id, contextParams = {}) {
    try {
      // 构建查询参数
      const queryParams = new URLSearchParams();
      
      // 添加上下文参数（如果存在）
      if (contextParams.fromCategory) {
        queryParams.append('fromCategory', contextParams.fromCategory);
      }
      if (contextParams.searchKeyword) {
        queryParams.append('searchKeyword', contextParams.searchKeyword);
      }
      if (contextParams.sortType) {
        queryParams.append('sortType', contextParams.sortType);
      }
      
      // 构建完整的URL
      const queryString = queryParams.toString();
      const url = queryString 
        ? `/web/api/category/video/detail/${id}?${queryString}`
        : `/web/api/category/video/detail/${id}`;
      
      const response = await request(url);
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 获取热门推荐视频列表
   * @param {number} pageNum - 页码
   * @param {number} pageSize - 每页大小
   */
  async getHotRecommendedVideos(pageNum = 1, pageSize = 10) {
    try {
      const queryParams = new URLSearchParams({
        pageNum: pageNum.toString(),
        pageSize: pageSize.toString()
      });
      
      const response = await request(`/web/api/category/videos/hot-recommended?${queryParams}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  // 注意：相邻视频(上一篇/下一篇)信息现在直接通过视频详情API返回
  // 不再需要单独的API调用，这样可以减少请求次数，提升性能
};

export default videoService; 