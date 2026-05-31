/**
 * 滚动位置管理器
 * 
 * 功能：
 * 1. 保存和恢复页面滚动位置
 * 2. 支持多个页面的位置记忆
 * 3. 自动清理过期的位置记录
 * 4. 与浏览器历史记录集成
 */

class ScrollPositionManager {
  constructor() {
    this.positions = new Map(); // 存储各页面的滚动位置
    this.maxRecords = 50;       // 最大记录数
    this.expireTime = 30 * 60 * 1000; // 30分钟过期
    
    // 监听浏览器前进后退
    this.setupHistoryListener();
    
    // 定期清理过期记录
    this.startCleanupTimer();
  }

  /**
   * 生成页面唯一标识
   */
  generatePageKey(pathname, search = '') {
    return `${pathname}${search}`;
  }

  /**
   * 保存当前页面的滚动位置
   */
  savePosition(pathname, search = '', customData = {}) {
    const key = this.generatePageKey(pathname, search);
    const position = {
      scrollTop: window.pageYOffset || document.documentElement.scrollTop,
      scrollLeft: window.pageXOffset || document.documentElement.scrollLeft,
      timestamp: Date.now(),
      pathname,
      search,
      ...customData
    };

    this.positions.set(key, position);
    
    // 限制记录数量
    if (this.positions.size > this.maxRecords) {
      const oldestKey = this.positions.keys().next().value;
      this.positions.delete(oldestKey);
    }

    // 保存到sessionStorage作为备份
    try {
      sessionStorage.setItem(`scroll_${key}`, JSON.stringify(position));
    } catch (error) {
      // 忽略存储错误
    }

    return position;
  }

  /**
   * 恢复页面滚动位置
   */
  restorePosition(pathname, search = '', options = {}) {
    const {
      smooth = false,           // 是否平滑滚动
      delay = 0,               // 延迟时间（ms）
      fallbackTop = 0,         // 找不到记录时的默认位置
      onRestore = null         // 恢复完成回调
    } = options;

    const key = this.generatePageKey(pathname, search);
    let position = this.positions.get(key);

    // 如果内存中没有，尝试从sessionStorage恢复
    if (!position) {
      try {
        const stored = sessionStorage.getItem(`scroll_${key}`);
        if (stored) {
          position = JSON.parse(stored);
          // 检查是否过期
          if (Date.now() - position.timestamp > this.expireTime) {
            position = null;
            sessionStorage.removeItem(`scroll_${key}`);
          } else {
            this.positions.set(key, position);
          }
        }
      } catch (error) {
        // 忽略解析错误
      }
    }

    const targetTop = position ? position.scrollTop : fallbackTop;
    const targetLeft = position ? position.scrollLeft : 0;

    const doRestore = () => {
      if (smooth) {
        window.scrollTo({
          top: targetTop,
          left: targetLeft,
          behavior: 'smooth'
        });
      } else {
        window.scrollTo(targetLeft, targetTop);
      }

      if (onRestore) {
        onRestore(position, targetTop, targetLeft);
      }
    };

    if (delay > 0) {
      setTimeout(doRestore, delay);
    } else {
      doRestore();
    }

    return position;
  }

  /**
   * 删除指定页面的位置记录
   */
  removePosition(pathname, search = '') {
    const key = this.generatePageKey(pathname, search);
    this.positions.delete(key);
    
    try {
      sessionStorage.removeItem(`scroll_${key}`);
    } catch (error) {
      // 忽略删除错误
    }
  }

  /**
   * 获取指定页面的位置记录
   */
  getPosition(pathname, search = '') {
    const key = this.generatePageKey(pathname, search);
    return this.positions.get(key) || null;
  }

  /**
   * 检查是否有位置记录
   */
  hasPosition(pathname, search = '') {
    const key = this.generatePageKey(pathname, search);
    return this.positions.has(key);
  }

  /**
   * 清理过期的位置记录
   */
  cleanup() {
    const now = Date.now();
    const keysToDelete = [];

    for (const [key, position] of this.positions) {
      if (now - position.timestamp > this.expireTime) {
        keysToDelete.push(key);
      }
    }

    keysToDelete.forEach(key => {
      this.positions.delete(key);
      try {
        sessionStorage.removeItem(`scroll_${key}`);
      } catch (error) {
        // 忽略删除错误
      }
    });

    return keysToDelete.length;
  }

  /**
   * 设置浏览器历史监听
   */
  setupHistoryListener() {
    // 监听浏览器前进后退
    window.addEventListener('popstate', (event) => {
      // 延迟恢复，等待页面渲染完成
      setTimeout(() => {
        if (event.state && event.state.scrollPosition) {
          const { top, left } = event.state.scrollPosition;
          window.scrollTo(left || 0, top || 0);
        }
      }, 100);
    });

    // 监听页面卸载，保存当前位置
    window.addEventListener('beforeunload', () => {
      this.savePosition(
        window.location.pathname, 
        window.location.search,
        { isBeforeUnload: true }
      );
    });
  }

  /**
   * 启动清理定时器
   */
  startCleanupTimer() {
    // 每5分钟清理一次过期记录
    this.cleanupTimer = setInterval(() => {
      this.cleanup();
    }, 5 * 60 * 1000);
  }

  /**
   * 停止清理定时器
   */
  stopCleanupTimer() {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer);
      this.cleanupTimer = null;
    }
  }

  /**
   * 获取所有位置记录的统计信息
   */
  getStats() {
    return {
      totalRecords: this.positions.size,
      oldestRecord: this.positions.size > 0 ? 
        Math.min(...Array.from(this.positions.values()).map(p => p.timestamp)) : null,
      newestRecord: this.positions.size > 0 ? 
        Math.max(...Array.from(this.positions.values()).map(p => p.timestamp)) : null
    };
  }

  /**
   * 清除所有位置记录
   */
  clear() {
    this.positions.clear();
    
    // 清除sessionStorage中的记录
    try {
      const keys = Object.keys(sessionStorage);
      keys.forEach(key => {
        if (key.startsWith('scroll_')) {
          sessionStorage.removeItem(key);
        }
      });
    } catch (error) {
      // 忽略清除错误
    }
  }

  /**
   * 销毁管理器
   */
  destroy() {
    this.stopCleanupTimer();
    this.clear();
  }
}

// 全局滚动位置管理器实例
const scrollPositionManager = new ScrollPositionManager();

// 开发环境下暴露到window对象
if (process.env.NODE_ENV === 'development') {
  window.ScrollPositionManager = scrollPositionManager;
}

export default scrollPositionManager;
export { ScrollPositionManager };
