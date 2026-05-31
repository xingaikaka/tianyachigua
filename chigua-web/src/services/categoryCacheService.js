/**
 * 分类数据缓存服务
 * 实现分类导航的智能缓存机制
 */

import categoryService from './categoryService';
import pageConfigService from './pageConfigService';

class CategoryCacheService {
  constructor() {
    this.CACHE_KEYS = {
      CATEGORIES: 'category_navigation_cache',
      MORE_MENU: 'category_more_menu_cache',
      TIMESTAMP: 'category_cache_timestamp',
      VERSION: 'category_cache_version',
      MORE_MENU_TIMESTAMP: 'category_more_menu_timestamp'
    };
    
    this.DISABLE_CACHE = false;

    this.CACHE_CONFIG = {
      CACHE_DURATION: 10 * 60 * 1000, // 10分钟
      CHECK_INTERVAL: 5 * 60 * 1000,
      NETWORK_TIMEOUT: 10 * 1000
    };
    
    this.memoryCache = {
      categories: null,
      version: null,
      timestamp: null,
      moreMenu: null,
      moreMenuTimestamp: null,
      lastVersionCheckTime: 0  // 上次后台 version 检查时间
    };

    // 内存缓存命中时，后台 version 检查的最小间隔（30 秒）
    this.VERSION_CHECK_INTERVAL = 30 * 1000;
    
    // 请求去重
    this.pendingRequests = new Map();
  }

  setMemoryCategories(data, version, timestamp) {
    this.memoryCache.categories = Array.isArray(data) ? data : [];
    this.memoryCache.version = version || null;
    this.memoryCache.timestamp = timestamp || Date.now();
  }

  setMemoryMoreMenu(data, timestamp) {
    this.memoryCache.moreMenu = Array.isArray(data) ? data : [];
    this.memoryCache.moreMenuTimestamp = timestamp || Date.now();
  }

  storeCategories(data, version, timestamp) {
    const payload = JSON.stringify({ data });
    try {
      sessionStorage.setItem(this.CACHE_KEYS.CATEGORIES, payload);
      sessionStorage.setItem(this.CACHE_KEYS.TIMESTAMP, timestamp.toString());
      if (version) {
        sessionStorage.setItem(this.CACHE_KEYS.VERSION, version);
      }
    } catch (error) {
      // ignore session storage errors
    }

    try {
      localStorage.setItem(this.CACHE_KEYS.CATEGORIES, payload);
      localStorage.setItem(this.CACHE_KEYS.TIMESTAMP, timestamp.toString());
      if (version) {
        localStorage.setItem(this.CACHE_KEYS.VERSION, version);
      }
    } catch (error) {
    }
  }

  storeMoreMenu(data, timestamp) {
    const payload = JSON.stringify({ data });
    try {
      sessionStorage.setItem(this.CACHE_KEYS.MORE_MENU, payload);
      sessionStorage.setItem(this.CACHE_KEYS.MORE_MENU_TIMESTAMP, timestamp.toString());
    } catch (error) {
      // ignore session storage errors
    }

    try {
      localStorage.setItem(this.CACHE_KEYS.MORE_MENU, payload);
      localStorage.setItem(this.CACHE_KEYS.MORE_MENU_TIMESTAMP, timestamp.toString());
    } catch (error) {
    }
  }

  async ensureCategoriesFresh(localVersion) {
    if (!localVersion) {
      try {
        await this.fetchCategoriesFromAPI(true);
      } catch (error) {
      }
      return;
    }

    if (this.pendingRequests.has('categories_version')) {
      return;
    }

    const promise = (async () => {
      try {
        const response = await categoryService.getCategoryVersion();
        const serverVersion = response?.data?.version;
        if (!serverVersion) {
          return;
        }

        if (serverVersion === localVersion) {
          this.memoryCache.timestamp = Date.now();
          if (!this.DISABLE_CACHE) {
            try {
              localStorage.setItem(this.CACHE_KEYS.TIMESTAMP, this.memoryCache.timestamp.toString());
            } catch (_) {}
          }
          return;
        }

        await this.fetchCategoriesFromAPI(true);
      } catch (error) {
      }
    })();

    this.pendingRequests.set('categories_version', promise);
    promise.finally(() => this.pendingRequests.delete('categories_version'));
  }

  /**
   * 获取分类列表（带缓存）
   */
  async getCategories(options = {}) {
    const { forceRefresh = false } = options;
    try {
      if (!forceRefresh && this.memoryCache.categories && this.isCacheValid(this.memoryCache.timestamp)) {
        // 内存缓存命中时，每隔 30 秒后台静默检查一次 version，确保后端修改后能及时感知
        const now = Date.now();
        if (now - this.memoryCache.lastVersionCheckTime > this.VERSION_CHECK_INTERVAL) {
          this.memoryCache.lastVersionCheckTime = now;
          this.ensureCategoriesFresh(this.memoryCache.version);
        }
        return this.memoryCache.categories;
      }

      if (!forceRefresh) {
        const storedCategories = this.getCachedData(this.CACHE_KEYS.CATEGORIES);
        const storedVersion = this.getCachedVersion();
        const storedTimestamp = this.getCachedTimestamp();

        if (storedCategories && storedVersion) {
          this.setMemoryCategories(storedCategories, storedVersion, storedTimestamp);
          this.ensureCategoriesFresh(storedVersion);
          return storedCategories;
        }
      }

      return await this.fetchCategoriesFromAPI(forceRefresh);
    } catch (error) {

      const fallbackData = this.memoryCache.categories || this.getCachedData(this.CACHE_KEYS.CATEGORIES);
      if (fallbackData) {
        return fallbackData;
      }

      return this.getDefaultCategories();
    }
  }

  /**
   * 获取更多菜单项（带缓存）
   */
  async getMoreMenuItems(options = {}) {
    const { forceRefresh = false } = options;
    try {
      if (!forceRefresh && this.memoryCache.moreMenu && this.isCacheValid(this.memoryCache.moreMenuTimestamp)) {
        return this.memoryCache.moreMenu;
      }

      if (!forceRefresh) {
        const storedMoreMenu = this.getCachedData(this.CACHE_KEYS.MORE_MENU);
        const storedTimestamp = this.getCachedMoreMenuTimestamp();
        if (storedMoreMenu && storedTimestamp) {
          this.setMemoryMoreMenu(storedMoreMenu, storedTimestamp);
          this.backgroundRefresh('moreMenu');
          return storedMoreMenu;
        }
      }

      return await this.fetchMoreMenuFromAPI(forceRefresh);
    } catch (error) {
      return [];
    }
  }

  /**
   * 从API获取分类数据
   */
  async fetchCategoriesFromAPI(forceRefresh = false) {
    const pendingKey = 'categories';

    if (!forceRefresh && this.pendingRequests.has(pendingKey)) {
      return this.pendingRequests.get(pendingKey);
    }

    const promise = this.doFetchCategories();
    this.pendingRequests.set(pendingKey, promise);

    try {
      const result = await promise;
      return result;
    } finally {
      this.pendingRequests.delete(pendingKey);
    }
  }

  /**
   * 执行分类数据获取
   */
  async doFetchCategories() {
    const response = await Promise.race([
      categoryService.getCategoryList(),
      this.createTimeoutPromise(this.CACHE_CONFIG.NETWORK_TIMEOUT)
    ]);

    const backendData = response?.data;
    const rawList = Array.isArray(backendData?.categories)
      ? backendData.categories
      : Array.isArray(backendData)
        ? backendData
        : [];
    const backendList = rawList.filter(Boolean);
    const backendVersion = backendData?.version || null;

    if (response.code === 200 && Array.isArray(backendList) && backendList.length > 0) {
      const allCategory = {
        id: null,
        categoryName: '全部',
        sortOrder: 999
      };

      const sortedCategories = [allCategory, ...backendList.sort((a, b) => (b.sortOrder || 0) - (a.sortOrder || 0))];

      this.updateCache(sortedCategories, 'categories', backendVersion);

      return sortedCategories;
    }

    throw new Error('API返回数据格式错误');
  }

  /**
   * 从API获取更多菜单数据
   */
  async fetchMoreMenuFromAPI(forceRefresh = false) {
    const pendingKey = 'moreMenu';

    if (!forceRefresh && this.pendingRequests.has(pendingKey)) {
      return this.pendingRequests.get(pendingKey);
    }

    const promise = this.doFetchMoreMenu();
    this.pendingRequests.set(pendingKey, promise);

    try {
      const result = await promise;
      return result;
    } finally {
      this.pendingRequests.delete(pendingKey);
    }
  }

  /**
   * 执行更多菜单数据获取
   */
  async doFetchMoreMenu() {
    const menuItems = await Promise.race([
      pageConfigService.getConfigsByType('category_more'),
      this.createTimeoutPromise(this.CACHE_CONFIG.NETWORK_TIMEOUT)
    ]);

    // 按排序字段排序
    const sortedItems = menuItems.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
    
    // 更新缓存
    this.updateCache(sortedItems, 'moreMenu');
    
    return sortedItems;
  }

  /**
   * 后台刷新数据
   */
  async backgroundRefresh(type) {
    // 检查是否需要刷新
    const lastCheck = this.getLastCheckTime();
    const now = Date.now();
    
    if (now - lastCheck < this.CACHE_CONFIG.CHECK_INTERVAL) {
      return; // 还没到检查时间
    }
    
    // 更新检查时间
    this.setLastCheckTime(now);
    
    try {
      
      let newData;
      if (type === 'categories') {
        newData = await this.doFetchCategories();
      } else if (type === 'moreMenu') {
        newData = await this.doFetchMoreMenu();
      }
      
      // 检查数据是否有变化
      const currentData = type === 'categories' ? this.memoryCache.categories : this.memoryCache.moreMenu;
      if (this.hasDataChanged(currentData, newData)) {
        // 触发数据更新事件
        this.notifyDataUpdate(type, newData);
      } else {
      }
      
    } catch (error) {
    }
  }

  /**
   * 更新缓存
   */
  updateCache(data, type, version = null) {
    if (type === 'categories') {
      const previous = this.memoryCache.categories;
      const changed = this.hasDataChanged(previous, data);
      const timestamp = Date.now();
      this.setMemoryCategories(data, version, timestamp);

      if (!this.DISABLE_CACHE) {
        this.storeCategories(data, version, timestamp);
      }

      if (changed) {
        this.notifyDataUpdate('categories', data);
      }
    } else if (type === 'moreMenu') {
      const previous = this.memoryCache.moreMenu;
      const changed = this.hasDataChanged(previous, data);
      const timestamp = Date.now();
      this.setMemoryMoreMenu(data, timestamp);

      if (!this.DISABLE_CACHE) {
        this.storeMoreMenu(data, timestamp);
      }

      if (changed) {
        this.notifyDataUpdate('moreMenu', data);
      }
    }
  }

  /**
   * 获取缓存数据
   */
  getCachedData(key) {
    try {
      // 优先从sessionStorage获取
      let raw = sessionStorage.getItem(key);
      if (!raw) {
        raw = localStorage.getItem(key);
      }
      if (!raw) {
        return null;
      }

      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object' && Array.isArray(parsed.data)) {
        return parsed.data;
      }
      return parsed;
    } catch (error) {
      return null;
    }
  }

  /**
   * 获取缓存时间戳
   */
  getCachedTimestamp() {
    try {
      const timestamp = localStorage.getItem(this.CACHE_KEYS.TIMESTAMP);
      return timestamp ? parseInt(timestamp) : 0;
    } catch (error) {
      return 0;
    }
  }

  getCachedVersion() {
    try {
      return sessionStorage.getItem(this.CACHE_KEYS.VERSION) || localStorage.getItem(this.CACHE_KEYS.VERSION) || null;
    } catch (error) {
      return null;
    }
  }

  getCachedMoreMenuTimestamp() {
    try {
      const timestamp = sessionStorage.getItem(this.CACHE_KEYS.MORE_MENU_TIMESTAMP) || localStorage.getItem(this.CACHE_KEYS.MORE_MENU_TIMESTAMP);
      return timestamp ? parseInt(timestamp) : 0;
    } catch (error) {
      return 0;
    }
  }

  /**
   * 检查缓存是否有效
   */
  isCacheValid(timestamp) {
    if (!timestamp) return false;
    return Date.now() - timestamp < this.CACHE_CONFIG.CACHE_DURATION;
  }

  /**
   * 获取/设置最后检查时间
   */
  getLastCheckTime() {
    try {
      const time = sessionStorage.getItem('category_last_check');
      return time ? parseInt(time) : 0;
    } catch (error) {
      return 0;
    }
  }

  setLastCheckTime(time) {
    try {
      sessionStorage.setItem('category_last_check', time.toString());
    } catch (error) {
      // 忽略存储错误
    }
  }

  /**
   * 检查数据是否有变化
   */
  hasDataChanged(oldData, newData) {
    if (!oldData || !newData) return true;
    return JSON.stringify(oldData) !== JSON.stringify(newData);
  }

  /**
   * 创建超时Promise
   */
  createTimeoutPromise(timeout) {
    return new Promise((_, reject) => {
      setTimeout(() => reject(new Error('网络请求超时')), timeout);
    });
  }

  /**
   * 获取默认分类
   */
  getDefaultCategories() {
    return [
      {
        id: null,
        categoryName: '全部',
        sortOrder: 999
      }
    ];
  }

  /**
   * 通知数据更新
   */
  notifyDataUpdate(type, data) {
    // 触发自定义事件
    const event = new CustomEvent('categoryDataUpdate', {
      detail: { type, data }
    });
    window.dispatchEvent(event);
  }

  /**
   * 清理缓存
   */
  clearCache() {
    // 清理内存缓存
    this.memoryCache = {
      categories: null,
      version: null,
      timestamp: null,
      moreMenu: null,
      moreMenuTimestamp: null,
      lastVersionCheckTime: 0
    };
    
    // 清理本地存储
    try {
      Object.values(this.CACHE_KEYS).forEach(key => {
        localStorage.removeItem(key);
        sessionStorage.removeItem(key);
      });
      sessionStorage.removeItem('category_last_check');
    } catch (error) {
    }
  }

  /**
   * 强制刷新数据
   */
  async forceRefresh() {
    this.clearCache();
    
    // 并行获取数据
    const [categories, moreMenu] = await Promise.all([
      this.fetchCategoriesFromAPI(),
      this.fetchMoreMenuFromAPI()
    ]);
    
    return { categories, moreMenu };
  }
}

// 创建全局实例
const categoryCacheService = new CategoryCacheService();

export default categoryCacheService;
