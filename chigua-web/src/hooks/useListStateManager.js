/**
 * 列表状态管理Hook
 * 
 * 功能：
 * 1. 保存和恢复列表数据状态
 * 2. 管理滚动位置
 * 3. 缓存分页信息
 * 4. 与路由导航集成
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import scrollPositionManager from '../utils/scrollPositionManager';

// 全局状态缓存
const globalStateCache = new Map();

const useListStateManager = (cacheKey, options = {}) => {
  const {
    autoSave = true,           // 是否自动保存状态
    saveInterval = 2000,       // 自动保存间隔（ms）
    maxCacheAge = 10 * 60 * 1000, // 缓存最大存活时间（10分钟）
    restoreScrollDelay = 100,  // 滚动位置恢复延迟
    onStateRestore = null,     // 状态恢复回调
    onStateSave = null,        // 状态保存回调
    defaultAnchorOffset = 120, // 默认头部偏移量
    anchorRestoreAttempts = 5, // 恢复锚点最大尝试次数
    anchorRestoreDelay = 120   // 恢复锚点重试延迟(ms)
  } = options;

  const location = useLocation();
  const navigate = useNavigate();
  
  // 状态引用
  const [isRestoring, setIsRestoring] = useState(false);
  const [hasRestoredState, setHasRestoredState] = useState(false);
  const saveTimerRef = useRef(null);
  const stateRef = useRef({});

  // 生成缓存键
  const generateCacheKey = useCallback(() => {
    return `${cacheKey}_${location.pathname}${location.search}`;
  }, [cacheKey, location.pathname, location.search]);

  /**
   * 保存列表状态
   */
  const saveListState = useCallback((state) => {
    const key = generateCacheKey();
    const cacheData = {
      ...state,
      timestamp: Date.now(),
      pathname: location.pathname,
      search: location.search,
      scrollPosition: {
        top: window.pageYOffset || document.documentElement.scrollTop,
        left: window.pageXOffset || document.documentElement.scrollLeft
      }
    };

    // 保存到内存缓存
    globalStateCache.set(key, cacheData);
    
    // 保存滚动位置
    scrollPositionManager.savePosition(
      location.pathname, 
      location.search, 
      { listState: true, cacheKey }
    );

    // 保存到sessionStorage作为备份
    try {
      sessionStorage.setItem(`listState_${key}`, JSON.stringify(cacheData));
    } catch (error) {
    }

    // 更新状态引用
    stateRef.current = cacheData;

    if (onStateSave) {
      onStateSave(cacheData);
    }

    return cacheData;
  }, [generateCacheKey, location.pathname, location.search, onStateSave]);

  const captureAnchorSnapshot = useCallback((config = {}) => {
    const {
      items = [],
      getDomId,
      headerOffset = defaultAnchorOffset
    } = config || {};

    const snapshot = {
      anchorId: null,
      anchorOffset: 0,
      scrollY: typeof window !== 'undefined' ? (window.pageYOffset || document.documentElement.scrollTop || 0) : 0,
      timestamp: Date.now()
    };

    if (!Array.isArray(items) || items.length === 0 || typeof window === 'undefined') {
      return snapshot;
    }

    const resolveDomId = (item, index) => {
      if (typeof getDomId === 'function') {
        return getDomId(item, index);
      }
      if (typeof item === 'string') return item;
      if (item && item.id != null) {
        return String(item.id);
      }
      return null;
    };

    for (let i = 0; i < items.length; i += 1) {
      const id = resolveDomId(items[i], i);
      if (!id) continue;
      const el = document.getElementById(id);
      if (!el) continue;
      const rect = el.getBoundingClientRect();
      if (rect.bottom > headerOffset) {
        snapshot.anchorId = id;
        snapshot.anchorOffset = Math.max(0, headerOffset - rect.top);
        break;
      }
    }

    return snapshot;
  }, [defaultAnchorOffset]);

  const restoreAnchorPosition = useCallback((scrollMeta, options = {}) => {
    if (!scrollMeta || typeof window === 'undefined') return false;
    const {
      anchorId,
      anchorOffset = 0,
      scrollY = 0
    } = scrollMeta;

    const {
      headerOffset = defaultAnchorOffset,
      attempts = anchorRestoreAttempts,
      retryDelay = anchorRestoreDelay,
      fallbackScrollY = scrollY
    } = options;

    let attempt = 0;

    const attemptRestore = () => {
      const el = anchorId ? document.getElementById(anchorId) : null;
      if (el) {
        const rect = el.getBoundingClientRect();
        const top = rect.top + (window.pageYOffset || document.documentElement.scrollTop || 0) - headerOffset + (anchorOffset || 0);
        window.scrollTo(0, Math.max(0, top));
        return true;
      }
      attempt += 1;
      if (attempt <= attempts) {
        setTimeout(attemptRestore, retryDelay);
        return false;
      }
      if (typeof fallbackScrollY === 'number') {
        window.scrollTo(0, Math.max(0, fallbackScrollY));
      }
      return false;
    };

    return attemptRestore();
  }, [anchorRestoreAttempts, anchorRestoreDelay, defaultAnchorOffset]);

  /**
   * 恢复列表状态
   */
  const restoreListState = useCallback(() => {
    const key = generateCacheKey();
    let cachedState = globalStateCache.get(key);

    // 如果内存中没有，尝试从sessionStorage恢复
    if (!cachedState) {
      try {
        const stored = sessionStorage.getItem(`listState_${key}`);
        if (stored) {
          cachedState = JSON.parse(stored);
          
          // 检查是否过期
          if (Date.now() - cachedState.timestamp > maxCacheAge) {
            cachedState = null;
            sessionStorage.removeItem(`listState_${key}`);
          } else {
            globalStateCache.set(key, cachedState);
          }
        }
      } catch (error) {
      }
    }

    if (cachedState) {
      setIsRestoring(true);
      
      // 恢复滚动位置
      setTimeout(() => {
        if (cachedState.scrollPosition) {
          window.scrollTo(
            cachedState.scrollPosition.left || 0,
            cachedState.scrollPosition.top || 0
          );
        }
        
        setIsRestoring(false);
        setHasRestoredState(true);
        
        if (onStateRestore) {
          onStateRestore(cachedState);
        }
      }, restoreScrollDelay);

      return cachedState;
    }

    return null;
  }, [generateCacheKey, maxCacheAge, restoreScrollDelay, onStateRestore]);

  /**
   * 清除列表状态
   */
  const clearListState = useCallback(() => {
    const key = generateCacheKey();
    
    globalStateCache.delete(key);
    scrollPositionManager.removePosition(location.pathname, location.search);
    
    try {
      sessionStorage.removeItem(`listState_${key}`);
    } catch (error) {
    }

    stateRef.current = {};
    setHasRestoredState(false);
  }, [generateCacheKey, location.pathname, location.search]);

  /**
   * 检查是否有缓存状态
   */
  const hasListState = useCallback(() => {
    const key = generateCacheKey();
    return globalStateCache.has(key) || 
           (sessionStorage.getItem(`listState_${key}`) !== null);
  }, [cacheKey, location.pathname, location.search]); // 🔧 修复依赖项

  /**
   * 自动保存状态（防抖）
   */
  const autoSaveState = useCallback((state) => {
    if (!autoSave) return;

    // 清除之前的定时器
    if (saveTimerRef.current) {
      clearTimeout(saveTimerRef.current);
    }

    // 设置新的定时器
    saveTimerRef.current = setTimeout(() => {
      saveListState(state);
    }, saveInterval);
  }, [autoSave, saveInterval, saveListState]);

  /**
   * 导航到详情页（保存当前状态）
   */
  const navigateToDetail = useCallback((detailPath, currentState) => {
    // 立即保存当前状态
    saveListState(currentState);
    
    // 导航到详情页
    navigate(detailPath);
  }, [navigate, saveListState]);

  /**
   * 从详情页返回（恢复状态）
   */
  const navigateBack = useCallback(() => {
    navigate(-1);
  }, [navigate]);

  // 页面卸载时保存状态
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (stateRef.current && Object.keys(stateRef.current).length > 0) {
        saveListState(stateRef.current);
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      
      // 清理定时器
      if (saveTimerRef.current) {
        clearTimeout(saveTimerRef.current);
      }
    };
  }, [saveListState]);

  // 定期清理过期缓存
  useEffect(() => {
    const cleanupTimer = setInterval(() => {
      const now = Date.now();
      const keysToDelete = [];

      for (const [key, state] of globalStateCache) {
        if (now - state.timestamp > maxCacheAge) {
          keysToDelete.push(key);
        }
      }

      keysToDelete.forEach(key => {
        globalStateCache.delete(key);
        try {
          sessionStorage.removeItem(`listState_${key}`);
        } catch (error) {
          // 忽略删除错误
        }
      });
    }, 5 * 60 * 1000); // 每5分钟清理一次

    return () => clearInterval(cleanupTimer);
  }, [maxCacheAge]);

  return {
    // 状态管理
    saveListState,
    restoreListState,
    clearListState,
    hasListState,
    autoSaveState,
    
    // 导航管理
    navigateToDetail,
    navigateBack,
    
    // 状态标识
    isRestoring,
    hasRestoredState,
    
    // 工具方法
    generateCacheKey,
    captureAnchorSnapshot,
    restoreAnchorPosition
  };
};

export default useListStateManager;
