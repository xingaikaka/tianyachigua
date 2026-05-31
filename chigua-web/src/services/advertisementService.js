import { request } from './api';

export const SHORT_VIDEO_AD_TILE_SIZE = 80;
export const SHORT_VIDEO_AD_GRID_COLUMNS = 3;
export const SHORT_VIDEO_AD_GRID_ROWS = 5;
export const SHORT_VIDEO_AD_GRID_CAPACITY = SHORT_VIDEO_AD_GRID_COLUMNS * SHORT_VIDEO_AD_GRID_ROWS;

const createPlaceholderGrid = () => Array.from({ length: SHORT_VIDEO_AD_GRID_CAPACITY }, () => null);

const chunkSequentialAdGrids = (ads) => {
  if (!Array.isArray(ads) || ads.length === 0) {
    return [createPlaceholderGrid()];
  }

  const grids = [];
  let index = 0;

  while (index < ads.length) {
    const slice = ads.slice(index, index + SHORT_VIDEO_AD_GRID_CAPACITY);
    if (slice.length < SHORT_VIDEO_AD_GRID_CAPACITY) {
      slice.push(...Array(SHORT_VIDEO_AD_GRID_CAPACITY - slice.length).fill(null));
    }
    grids.push(slice);
    index += SHORT_VIDEO_AD_GRID_CAPACITY;
  }

  return grids;
};

const buildCircularAdGrid = (ads, startIndex = 0) => {
  if (!Array.isArray(ads) || ads.length === 0) {
    return { grid: createPlaceholderGrid(), nextIndex: 0 };
  }

  const total = ads.length;
  const grid = [];
  const actualCount = Math.min(total, SHORT_VIDEO_AD_GRID_CAPACITY);

  for (let i = 0; i < actualCount; i++) {
    const idx = (startIndex + i) % total;
    grid.push(ads[idx]);
  }

  if (actualCount < SHORT_VIDEO_AD_GRID_CAPACITY) {
    grid.push(...Array(SHORT_VIDEO_AD_GRID_CAPACITY - actualCount).fill(null));
  }

  const nextIndex = total >= SHORT_VIDEO_AD_GRID_CAPACITY
    ? (startIndex + SHORT_VIDEO_AD_GRID_CAPACITY) % total
    : startIndex;

  return { grid, nextIndex };
};

const advertisementService = {
  /**
   * 根据应用类型获取Logo广告
   * @param {number} appType - 应用类型：1-热门应用，2-最新上架，3-必备精品
   */
  async getLogoAdsByAppType(appType) {
    try {
      const response = await request(`/web/api/advertisement/logo/${appType}`);
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 根据广告位置获取广告
   * @param {string} position - 广告位置
   */
  async getAdsByPosition(position) {
    try {
      const response = await request(`/web/api/advertisement/position/${position}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 根据广告类型获取广告
   * @param {string} adType - 广告类型
   */
  async getAdsByType(adType) {
    try {
      const response = await request(`/web/api/advertisement/type/${adType}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取分类广告
   * @param {number} categoryId - 分类ID
   */
  async getCategoryAds(categoryId) {
    try {
      const response = await request(`/web/api/advertisement/category/${categoryId}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取分类底部广告
   * @param {number} categoryId - 分类ID
   */
  async getCategoryBottomAds(categoryId) {
    try {
      const response = await request(`/web/api/advertisement/category/${categoryId}/bottom`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取详情页面广告（顶部+底部）
   * @param {number} categoryId - 分类ID
   */
  async getDetailPageAds(categoryId) {
    try {
      const response = await request(`/web/api/advertisement/detail/${categoryId}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 记录广告点击统计
   * @param {number} adId - 广告ID
   */
  async clickAd(adId, extra) {
    try {
      try {
        const mod = await import('./videoStatsService');
        const svc = mod && (mod.default || mod);
        if (svc && typeof svc.trackAdClick === 'function') {
          svc.trackAdClick(adId, extra || {});
        }
      } catch (_) {}
      const response = await request(`/web/api/advertisement/click/${adId}`, {
        method: 'POST'
      });
      return response;
    } catch (error) {

      throw error;
    }
  },

  /**
   * 记录广告曝光统计
   * @param {number} adId - 广告ID
   */
  async recordAdImpression(adId) {
    // 曝光上报暂时关闭，避免频繁触发后端限流
    return Promise.resolve();
    // try {
    //   const response = await request(`/web/api/advertisement/impression/${adId}`, {
    //     method: 'POST'
    //   });
    //   return response;
    // } catch (error) {
    //   throw error;
    // }
  },

  /**
   * 获取弹窗广告（单个类型，ad_type=4）
   * @returns {Promise} 弹窗广告列表
   */
  async getPopupAds() {
    try {
      const response = await request('/web/api/advertisement/type/4'); // 广告类型4为弹窗广告
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取九宫格弹窗广告（ad_type=7）
   * @returns {Promise} 九宫格弹窗广告列表
   */
  async getGridPopupAds() {
    try {
      const response = await request('/web/api/advertisement/type/7');
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 获取分类的广告显示配置
   * @param {number} categoryId - 分类ID
   */
  async getCategoryAdConfig(categoryId) {
    try {
      const response = await request(`/web/api/category/${categoryId}/ad-config`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 获取分类的混合内容（广告+内容，根据显示模式）
   * @param {number} categoryId - 分类ID
   * @param {number} page - 页码
   * @param {number} pageSize - 每页大小
   */
  async getCategoryMixedContent(categoryId, page = 1, pageSize = 20) {
    try {
      const response = await request(`/web/api/category/${categoryId}/mixed-content?page=${page}&pageSize=${pageSize}`);
      return response;
    } catch (error) {
      
      throw error;
    }
  },

  /**
   * 创建混合内容列表（前端实用方法）
   * @param {Array} content - 内容列表（视频、合集等）
   * @param {Array} ads - 广告列表
   * @param {number} adDisplayMode - 广告显示模式（1-集中显示，2-交替显示，3-不显示）
   * @param {number} adInterval - 广告间隔（交替显示模式使用）
   */
  createMixedContentList(content, ads, adDisplayMode, adInterval = 3) {
    // 模式3：不显示广告
    if (adDisplayMode === 3 || !ads || ads.length === 0) {
      return content.map(item => ({ type: 'content', data: item }));
    }

    // 模式1：集中显示（所有广告在前）
    if (adDisplayMode === 1) {
      const adsItems = ads.map(ad => ({ type: 'ad', data: ad }));
      const contentItems = content.map(item => ({ type: 'content', data: item }));
      return [...adsItems, ...contentItems];
    }

    // 模式2：交替显示
    if (adDisplayMode === 2) {
      const mixedItems = [];
      let adIndex = 0;
      
      for (let i = 0; i < content.length; i++) {
        // 添加内容
        mixedItems.push({ type: 'content', data: content[i] });
        
        // 检查是否需要插入广告
        if ((i + 1) % adInterval === 0 && adIndex < ads.length) {
          mixedItems.push({ type: 'ad', data: ads[adIndex] });
          adIndex++;
        }
      }
      
      // 如果还有剩余广告，添加到末尾
      while (adIndex < ads.length) {
        mixedItems.push({ type: 'ad', data: ads[adIndex] });
        adIndex++;
      }
      
      return mixedItems;
    }

    // 默认返回纯内容
    return content.map(item => ({ type: 'content', data: item }));
  },

  /**
   * 获取短视频广告
   * @param {number} categoryId - 分类ID
   */
  async getShortVideoAds(categoryId) {
    try {
      const response = await request(`/web/api/advertisement/short-video/${categoryId}`);
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 获取分页模式广告
   * @param {number} categoryId - 分类ID
   */
  async getPagedAds(categoryId) {
    try {
      const response = await request(`/web/api/advertisement/paged/${categoryId}`);
      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * 创建循环混合内容列表（短视频专用）
   * @param {Array} videos - 视频列表
   * @param {Array} ads - 广告列表  
   * @param {number} adDisplayMode - 广告显示模式
   * @param {number} adInterval - 广告间隔
   * @param {number} existingItemsCount - 已有内容数量
   */
  createShortVideoMixedContent(videos, ads, adDisplayMode, adInterval = 5, existingItemsCount = 0) {
    // 模式3：不显示广告
    if (adDisplayMode === 3 || !ads || ads.length === 0) {
      return videos.map(video => ({ type: 'video', data: video }));
    }

    // 模式1：集中显示（仅第一页且没有已有内容时显示所有广告）
    if (adDisplayMode === 1 && existingItemsCount === 0) {
      const adGrids = chunkSequentialAdGrids(ads).map(group => ({ type: 'ad-grid', data: group }));
      const videoItems = videos.map(video => ({ type: 'video', data: video }));
      return [...adGrids, ...videoItems];
    }

    // 模式2：交替显示（循环广告）
    if (adDisplayMode === 2) {
      const mixedItems = [];
      let adPointer = 0;
      
      for (let i = 0; i < videos.length; i++) {
        // 添加视频
        mixedItems.push({ type: 'video', data: videos[i] });
        
        // 计算全局位置（考虑之前已加载的内容）
        const globalPosition = existingItemsCount + mixedItems.length;
        
        // 检查是否需要插入广告（根据间隔）
        if (globalPosition % (adInterval + 1) === 0) {
          const { grid, nextIndex } = buildCircularAdGrid(ads, adPointer);
          adPointer = nextIndex;
          mixedItems.push({ type: 'ad-grid', data: grid });
        }
      }
      
      return mixedItems;
    }

    // 默认返回纯视频内容
    return videos.map(video => ({ type: 'video', data: video }));
  },

  /**
   * 创建分页模式混合内容列表（单独显示广告）
   * @param {Array} videos - 视频列表
   * @param {Array} ads - 广告列表
   * @param {number} adDisplayMode - 广告显示模式
   * @param {number} adInterval - 广告间隔（每隔多少个视频显示一个广告）
   * @param {number} currentPage - 当前页码（用于计算广告轮换）
   */
  createPagedMixedContent(videos, ads, adDisplayMode, adInterval = 10, currentPage = 1) {
    // 模式3：不显示广告
    if (adDisplayMode === 3 || !ads || ads.length === 0) {
      return videos.map(video => ({ type: 'video', data: video }));
    }

    const videoItems = videos.map(video => ({ type: 'video', data: video }));

    // 模式1：集中显示（仅在列表顶部显示一个广告）
    if (adDisplayMode === 1) {
      // 根据页码轮换显示不同的广告
      const adIndex = (currentPage - 1) % ads.length;
      const selectedAd = ads[adIndex];
      return [
        { type: 'single-ad', data: selectedAd },
        ...videoItems
      ];
    }

    // 模式2：交替显示（在视频列表中按间隔插入单个广告）
    if (adDisplayMode === 2) {
      const mixedItems = [];
      let adIndex = 0;

      for (let i = 0; i < videos.length; i++) {
        // 添加视频
        mixedItems.push({ type: 'video', data: videos[i] });

        // 每隔 adInterval 个视频插入一个广告
        if ((i + 1) % adInterval === 0 && adIndex < ads.length) {
          // 循环使用广告列表
          const selectedAd = ads[adIndex % ads.length];
          mixedItems.push({ type: 'single-ad', data: selectedAd });
          adIndex++;
        }
      }

      return mixedItems;
    }

    // 默认返回纯视频内容
    return videoItems;
  }
};

export default advertisementService; 
