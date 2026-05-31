import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useLocation, useNavigationType } from 'react-router-dom';
import videoService from '../../services/videoService';
import advertisementService from '../../services/advertisementService';
import apiCacheService from '../../services/apiCacheService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import globalStatePreloader from '../../utils/globalStatePreloader';
import { isVideoUrlCacheExpired } from '../../constants/cacheConfig';
import AdCard from '../AdCard';
import LogoAds from '../LogoAds';
import CommonFooter from '../common/Footer';
import useListStateManager from '../../hooks/useListStateManager';
import useIsDesktop from '../../hooks/useIsDesktop';
import MixedContentList from '../common/MixedContentList';
import BottomBannerAds from '../common/BottomBannerAds';
import usePagedContent from '../../hooks/usePagedContent';
import useListAds from '../../hooks/useListAds';

const VideoList = ({ categoryId = null }) => {
  const navigate = useNavigate();
  const location = useLocation();
  // 移动端视图模式：text（文字卡片）/ image（图片网格）
  const [viewMode, setViewMode] = useState(() => {
    try { return localStorage.getItem('videolist_view_mode') || 'image'; } catch { return 'image'; }
  });
  const navigationType = useNavigationType();
  const forceTop = !!(location && location.state && location.state.forceTop);
  const pageKey = `videoList_${categoryId || 'home'}`;
  const restoreTimersRef = useRef([]);
  const userInteractedRef = useRef(false);
  const pendingScrollMetaRef = useRef(null);
  const shouldScrollToTopRef = useRef(false);
  // 🔧 修复：添加分页加载状态保护，防止分页加载时被重置
  const loadingPageRef = useRef(null);
  // 🔧 修复：使用 ref 存储 pageSize，避免函数引用变化
  const pageSizeRef = useRef(20);
  // 🔧 修复：跟踪空页面重试次数，避免无限循环
  const emptyPageRetryRef = useRef(0);
  // 🔧 修复：使用 ref 存储 loadVideoPage，避免循环依赖
  const loadVideoPageRef = useRef(null);
  // 从缓存恢复后是否已触发过后台刷新（stale-while-revalidate）
  const bgRefreshDoneRef = useRef(false);
  const isDesktop = useIsDesktop();
  const {
    restoreListState,
    autoSaveState,
    saveListState,
    captureAnchorSnapshot,
    restoreAnchorPosition
  } = useListStateManager(pageKey, { autoSave: false });

  // 强制置顶处理
  useEffect(() => {
    // 仅在非 POP 导航且显式标记时置顶；返回列表（POP）不置顶
    const shouldForceTop = forceTop && navigationType !== 'POP';
    if (!shouldForceTop) return;
    window.scrollTo(0, 0);
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
    try {
      navigate(`${location.pathname}${location.search}${location.hash}`, { replace: true, state: {} });
    } catch (_) {}
  }, [forceTop, navigationType]);

  // POP 返回时恢复滚动到进入前位置（单次恢复，避免顶部抖动）
  useEffect(() => {
    if (navigationType !== 'POP') return;
    try {
      const cacheStr = sessionStorage.getItem(pageKey);
      if (!cacheStr) return;
      const cache = JSON.parse(cacheStr);
      const headerOffset = 120;

      const restoreOnce = () => {
        if (userInteractedRef.current) return;
        const docEl = document.documentElement;
        const prevScrollBehavior = docEl.style.scrollBehavior;
        // 临时禁用平滑滚动，防止视觉抖动
        docEl.style.scrollBehavior = 'auto';
        try {
          let done = false;
          const scrollMeta = cache?.scrollMeta || cache;
          if (scrollMeta && scrollMeta.anchorId) {
            const el = document.getElementById(scrollMeta.anchorId);
            if (el) {
              const top = el.getBoundingClientRect().top + window.pageYOffset - headerOffset + (scrollMeta.anchorOffset || 0);
              const maxScrollable = Math.max(0, (document.documentElement.scrollHeight || document.body.scrollHeight) - window.innerHeight);
              window.scrollTo(0, Math.min(Math.max(0, top), maxScrollable));
              done = true;
            }
          }
          if (!done) {
            const maxScrollable = Math.max(0, (document.documentElement.scrollHeight || document.body.scrollHeight) - window.innerHeight);
            const target = Math.min((scrollMeta && scrollMeta.scrollY) || 0, maxScrollable);
            window.scrollTo(0, target);
          }
        } finally {
          // 下一帧恢复原设置
          requestAnimationFrame(() => { docEl.style.scrollBehavior = prevScrollBehavior || ''; });
        }
      };

      // 使用 rAF 单次恢复，外加 200ms 兜底
      restoreTimersRef.current = [];
      const id1 = requestAnimationFrame(restoreOnce);
      const id2 = setTimeout(restoreOnce, 200);
      restoreTimersRef.current.push(id1);
      restoreTimersRef.current.push(id2);

      return () => {
        restoreTimersRef.current.forEach((id) => {
          if (typeof id === 'number') cancelAnimationFrame(id);
          try { clearTimeout(id); } catch (_) {}
        });
        restoreTimersRef.current = [];
      };
    } catch (_) {}
  }, [navigationType]);

  // 缓存初始化（含视频 URL 签名有效期检查，避免使用过期签名）
  const initializeFromCache = () => {
    const preloadedState = globalStatePreloader.getPreloadedState(pageKey);
    if (preloadedState && preloadedState.videos && preloadedState.videos.length > 0 && !isVideoUrlCacheExpired(preloadedState)) {
      return preloadedState;
    }
    try {
      const stored = sessionStorage.getItem(pageKey);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed && Array.isArray(parsed.videos) && parsed.videos.length > 0 && !isVideoUrlCacheExpired(parsed)) {
          return parsed;
        }
        // 已过期则清理，避免下次误用
        if (parsed && isVideoUrlCacheExpired(parsed)) {
          sessionStorage.removeItem(pageKey);
        }
      }
    } catch (_) {}
    return null;
  };

  const cachedState = initializeFromCache();
  if (cachedState?.scrollMeta && !pendingScrollMetaRef.current) {
    pendingScrollMetaRef.current = cachedState.scrollMeta;
  }
  const hasCachedBootstrap = Boolean(cachedState);
  
  // 🔧 修复：初始化 pageSizeRef，使用缓存值或默认值
  const initialPageSize = cachedState?.pagination?.pageSize || 20;
  if (pageSizeRef.current !== initialPageSize) {
    pageSizeRef.current = initialPageSize;
  }
  
  const [videos, setVideos] = useState(cachedState?.videos || []);
  const {
    topAds,
    bottomAds,
    adConfig,
    refreshTopAds,
    refreshBottomAds,
    refreshAdConfig,
    primeAdsState
  } = useListAds({
    categoryId,
    initialTopAds: cachedState?.ads || [],
    initialBottomAds: cachedState?.bottomAds || [],
    initialConfig: cachedState?.adConfig || null
  });
  const ads = topAds;
  const [loading, setLoading] = useState(!cachedState);
  const [pagination, setPagination] = useState(cachedState?.pagination || {
    current: 1,
    total: 0,
    pageSize: initialPageSize
  });
  const [isHydratedFromCache, setIsHydratedFromCache] = useState(!!cachedState);
  
  // 🔧 修复：同步更新 pageSizeRef，确保 ref 始终是最新值
  useEffect(() => {
    pageSizeRef.current = pagination.pageSize;
  }, [pagination.pageSize]);

  // 滚动到列表顶部的辅助函数
  const scrollToListTop = useCallback(() => {
    try {
      window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    } catch (_) {
      window.scrollTo(0, 0);
    }
  }, []);

  const clearScrollMetaCache = useCallback(() => {
    try {
      const stored = sessionStorage.getItem(pageKey);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (parsed && parsed.scrollMeta) {
          delete parsed.scrollMeta;
          sessionStorage.setItem(pageKey, JSON.stringify(parsed));
        }
      }
    } catch (_) {}

    try {
      sessionStorage.removeItem(pageKey + ':skip');
    } catch (_) {}

    try {
      if (globalStatePreloader?.clearPreloadedState) {
        globalStatePreloader.clearPreloadedState(pageKey);
      }
    } catch (_) {}
  }, [pageKey]);

  // 智能预加载下一页数据（后台执行，不阻塞用户操作）
  const preloadNextPage = useCallback((currentPage, totalCount) => {
    try {
      const nextPage = currentPage + 1;
      const totalPages = Math.ceil(totalCount / pageSizeRef.current);
      
      if (nextPage <= totalPages) {
        setTimeout(async () => {
          try {
            const cacheOptions = {
              cacheDuration: 5 * 60 * 1000,
              skipCacheIf: (res) => {
                const ok = res && res.code === 200;
                const rows = (res && res.rows)
                  || (res && res.data && (res.data.rows || res.data.list))
                  || [];
                return ok && Array.isArray(rows) && rows.length === 0;
              },
              emptyCacheDuration: 15 * 1000
            };

            if (categoryId) {
              await apiCacheService.getApiData(
                'VIDEOS_BY_CATEGORY',
                { categoryId, pageNum: nextPage, pageSize: pageSizeRef.current },
                (params) => videoService.getVideosByCategory(params.categoryId, params.pageNum, params.pageSize),
                cacheOptions
              );
            } else {
              await apiCacheService.getApiData(
                'ALL_VIDEOS',
                { pageNum: nextPage, pageSize: pageSizeRef.current },
                (params) => videoService.getAllVideos(params.pageNum, params.pageSize),
                cacheOptions
              );
            }
          } catch (error) {
            // 预加载失败不影响主流程
          }
        }, 1000);
      }
    } catch (error) {
      // 预加载逻辑出错不影响主流程
    }
  }, [categoryId]);

  // 状态保存函数
  const saveCurrentState = (scrollMetaOverride = null) => {
    const scrollMeta = scrollMetaOverride || captureAnchorSnapshot({
      items: videos,
      getDomId: (item) => `card-${item.id}`,
      headerOffset: 120
    });
    const currentState = {
      videos,
      ads,
      bottomAds,
      adConfig,
      pagination,
      isDesktop,
      scrollMeta,
      timestamp: Date.now()
    };
    globalStatePreloader.setPreloadedState(pageKey, currentState);
    return currentState;
  };

  // 获取分类广告配置
  // 获取视频列表（含空页处理）
  // 🔧 修复：使用 pageSizeRef 替代 pagination.pageSize，避免函数引用变化
  const fetchVideoPage = useCallback(async (pageNum = 1, options = {}) => {
    const { forceRefresh = false } = options;

    const listCacheMs = 5 * 60 * 1000;
    const cacheOptions = {
      cacheDuration: listCacheMs,
      skipCacheIf: (res) => {
        const ok = res && res.code === 200;
        const rows = (res && res.rows)
          || (res && res.data && (res.data.rows || res.data.list))
          || [];
        return ok && Array.isArray(rows) && rows.length === 0;
      },
      emptyCacheDuration: 15 * 1000,
      forceRefresh
    };

    const buildParams = () => {
      const baseParams = {
        pageNum,
        pageSize: pageSizeRef.current  // 🔧 修复：从 ref 读取，避免依赖 state
      };
      if (categoryId) {
        baseParams.categoryId = categoryId;
      }
      if (forceRefresh) {
        baseParams._forceRefresh = true;
        baseParams._timestamp = Date.now();
      }
      return baseParams;
    };

    let response;
    if (categoryId) {
      response = await apiCacheService.getApiData(
        'VIDEOS_BY_CATEGORY',
        buildParams(),
        (params) => videoService.getVideosByCategory(params.categoryId, params.pageNum, params.pageSize),
        cacheOptions
      );
    } else {
      response = await apiCacheService.getApiData(
        'ALL_VIDEOS',
        buildParams(),
        (params) => videoService.getAllVideos(params.pageNum, params.pageSize),
        cacheOptions
      );
    }

    if (response.code === 200) {
      const rows = (response && response.rows)
        || (response && response.data && response.data.rows)
        || (response && response.data && response.data.list)
        || [];
      const totalVal = (response && typeof response.total !== 'undefined')
        ? response.total
        : (response && response.data && response.data.total) || 0;

      return {
        items: rows,
        total: totalVal
      };
    }

    return {
      items: [],
      total: 0
    };
  }, [categoryId]);  // 🔧 修复：移除 pagination.pageSize 依赖

  const handleVideoPageSuccess = useCallback(({ result, page }) => {
    const nextVideos = result.items || [];
    const total = result.total || 0;
    const pageSize = pageSizeRef.current;
    const totalPages = Math.ceil(total / pageSize);
    
    // 🔧 修复：如果当前页为空但总数>0，自动加载下一页
    if (nextVideos.length === 0 && total > 0 && page < totalPages) {
      // 检查重试次数，避免无限循环（最多重试5次）
      if (emptyPageRetryRef.current < 5) {
        emptyPageRetryRef.current += 1;
        const nextPage = page + 1;
        console.log(`⚠️ 页面 ${page} 没有数据，自动跳转到第 ${nextPage} 页`);
        
        // 设置加载状态，防止 useEffect 重置逻辑干扰
        loadingPageRef.current = nextPage;
        
        // 🔧 修复：使用 loadVideoPageRef 避免循环依赖
        // 延迟加载下一页，确保当前状态更新完成
        setTimeout(() => {
          // 使用 loadVideoPageRef 来调用，避免循环依赖
          if (loadVideoPageRef.current) {
            loadVideoPageRef.current(nextPage, { showLoading: true, forceRefresh: true }).catch(() => {
              // 如果加载失败，重置重试计数
              emptyPageRetryRef.current = 0;
              loadingPageRef.current = null;
            });
          }
        }, 100);
        
        return; // 不设置空数据，直接返回
      } else {
        // 超过重试次数，重置计数器并显示空页面
        console.warn(`⚠️ 连续 ${emptyPageRetryRef.current} 页没有数据，停止自动跳转`);
        emptyPageRetryRef.current = 0;
      }
    } else {
      // 有数据或已经是最后一页，重置重试计数
      emptyPageRetryRef.current = 0;
    }
    
    setVideos(nextVideos);
    setPagination(prev => ({
      ...prev,
      current: page,
      total: total
    }));
    preloadNextPage(page, total);
    setLoading(false);
    if (pendingScrollMetaRef.current) {
      restoreAnchorPosition(pendingScrollMetaRef.current, {
        headerOffset: 120,
        fallbackScrollY: pendingScrollMetaRef.current.scrollY
      });
      pendingScrollMetaRef.current = null;
    }
  }, [preloadNextPage, restoreAnchorPosition]);

  // 🔧 修复：改进错误处理，不清空已有数据，避免触发重置逻辑
  const handleVideoPageError = useCallback(() => {
    // 不清空 videos，保留当前数据，避免触发 useEffect 重置逻辑
    // setVideos([]);  // ❌ 删除这行，避免清空数据
    setLoading(false);
    // 可以在这里添加错误提示，但不重置数据
  }, []);

  const { loadPage: loadVideoPage, loading: pageLoading } = usePagedContent({
    fetcher: fetchVideoPage,
    onSuccess: handleVideoPageSuccess,
    onError: handleVideoPageError
  });

  // 🔧 修复：将 loadVideoPage 存储到 ref，避免循环依赖
  useEffect(() => {
    loadVideoPageRef.current = loadVideoPage;
  }, [loadVideoPage]);

  const fetchVideos = useCallback(async (pageNum = 1, isInitialLoad = false, options = {}) => {
    if (isInitialLoad) {
      setLoading(true);
    }
    try {
      await loadVideoPage(pageNum, { showLoading: !isInitialLoad, ...options });
    } catch (error) {
      if (isInitialLoad) {
        setLoading(false);
      }
      throw error;
    }
  }, [loadVideoPage]);

  useEffect(() => {
    if (pageLoading) {
      setLoading(true);
    }
  }, [pageLoading]);

  useEffect(() => {
    if (!pendingScrollMetaRef.current) return;
    if (videos.length === 0) return;
    restoreAnchorPosition(pendingScrollMetaRef.current, { headerOffset: 120 });
    pendingScrollMetaRef.current = null;
  }, [videos, restoreAnchorPosition]);

  // categoryId 切换时重置后台刷新标记，确保新分类也能触发 stale-while-revalidate
  useEffect(() => {
    bgRefreshDoneRef.current = false;
  }, [categoryId]);

  // 数据加载逻辑
  // 🔧 修复：添加分页加载状态保护，防止分页加载时被重置
  useEffect(() => {
    if (!isHydratedFromCache) {
      const restoredState = restoreListState();
    if (restoredState?.scrollMeta) {
      pendingScrollMetaRef.current = restoredState.scrollMeta;
    }
    if (restoredState && restoredState.categoryId === categoryId && Array.isArray(restoredState.videos) && restoredState.videos.length > 0) {
      setVideos(restoredState.videos || []);
        primeAdsState({
          top: restoredState.ads,
          bottom: restoredState.bottomAds,
          config: restoredState.adConfig
        });
        setPagination(restoredState.pagination || { current: 1, total: 0, pageSize: 20 });
        setLoading(false);
        setIsHydratedFromCache(true);
        return;
      }
      setIsHydratedFromCache(true);
    }

    if (forceTop) {
      pendingScrollMetaRef.current = null;
      shouldScrollToTopRef.current = true;
      clearScrollMetaCache();
      setVideos([]);
      primeAdsState({ top: [], bottom: [], config: null });
      setPagination({ current: 1, total: 0, pageSize: 20 });

      const loadData = async () => {
        try {
          setLoading(true);
          await fetchVideos(1, true);
        } catch (error) {
          // 静默处理错误
        } finally {
          setLoading(false);
          refreshAdConfig();
          refreshTopAds();
          refreshBottomAds();
        }
      };

      loadData();
      return;
    }

    // 🔧 修复：关键保护逻辑 - 如果正在加载任何页面，不执行重置逻辑
    if (loadingPageRef.current !== null) {
      // 正在加载任何页面（包括第一页），不重置，避免覆盖正在加载的页面
      return;
    }

    // 🔧 修复：如果已有数据或已有缓存，不执行重置逻辑
    if (videos.length > 0 || hasCachedBootstrap) {
      setLoading(false);
      // stale-while-revalidate：从缓存恢复后立即后台刷新签名URL
      // 解决"sessionStorage timestamp 记录的是保存时间而非URL生成时间"导致的签名过期问题
      // 用户在列表页停留超过1小时不翻页，签名URL可能已过期，但sessionStorage仍认为缓存有效
      if (hasCachedBootstrap && !bgRefreshDoneRef.current) {
        bgRefreshDoneRef.current = true;
        const refreshPage = pagination?.current || 1;
        setTimeout(() => {
          if (loadVideoPageRef.current) {
            loadVideoPageRef.current(refreshPage, { showLoading: false, forceRefresh: false }).catch(() => {});
          }
        }, 300);
      }
      return;
    }

    // 🔧 修复：只在真正需要初始化时才加载第一页
    // 确保不是分页加载过程中（loadingPageRef 已清除但 setVideos 还未完成的情况）
    if (!hasCachedBootstrap && videos.length === 0 && loadingPageRef.current === null) {
      const loadData = async () => {
        try {
          setLoading(true);
          await fetchVideos(1, true);
        } catch (error) {
        } finally {
          setLoading(false);
          refreshAdConfig();
          refreshTopAds();
          refreshBottomAds();
        }
      };

      loadData();
    } else {
      setLoading(false);
    }
  }, [categoryId, forceTop, fetchVideos, restoreListState, isHydratedFromCache, hasCachedBootstrap, refreshAdConfig, refreshTopAds, refreshBottomAds, primeAdsState, clearScrollMetaCache]);
  // 🔧 修复：移除 videos.length 依赖，避免频繁触发 useEffect


  useEffect(() => {
    if (shouldScrollToTopRef.current && !loading) {
      shouldScrollToTopRef.current = false;
      scrollToListTop();
    }
  }, [videos, loading, scrollToListTop]);


  // 页码改变处理：记录意图，待数据更新后再滚动
  // 🔧 修复：添加加载状态保护，防止 useEffect 重置逻辑干扰
  const handlePageChange = async (pageNum) => {
    try {
      // 🔧 修复：重置空页面重试计数（用户主动切换页面时）
      emptyPageRetryRef.current = 0;
      
      // 🔧 修复：设置加载状态，防止 useEffect 重置逻辑执行
      loadingPageRef.current = pageNum;
      pendingScrollMetaRef.current = null;
      shouldScrollToTopRef.current = true;
      clearScrollMetaCache();
      await fetchVideos(pageNum, false);
      
      // 🔧 修复：延迟清除 loadingPageRef，确保 setVideos 完成后再清除
      // 使用 setTimeout 确保在下一个事件循环中清除，避免与 useEffect 竞争
      setTimeout(() => {
        // 只有在当前页面加载完成且没有新的加载请求时才清除
        if (loadingPageRef.current === pageNum) {
          loadingPageRef.current = null;
        }
      }, 200);
    } catch (error) {
      // 错误时立即清除，避免阻塞后续操作
      emptyPageRetryRef.current = 0;
      loadingPageRef.current = null;
      shouldScrollToTopRef.current = false;
      scrollToListTop();
    }
  };

  // 视频点击处理
  const handleVideoClick = (videoId) => {
    const scrollMeta = captureAnchorSnapshot({
      items: videos,
      getDomId: (item) => `card-${item.id}`,
      headerOffset: 120
    });
    const sessionState = {
      videos,
      ads,
      bottomAds,
      adConfig,
      pagination,
      categoryId,
      isDesktop,
      scrollMeta,
      timestamp: Date.now()
    };
    sessionStorage.setItem(pageKey, JSON.stringify(sessionState));
    sessionStorage.setItem(pageKey + ':skip', '1');
    saveCurrentState(scrollMeta);
    saveListState(sessionState);
    
    // 🔍 构建带上下文参数的URL
    // 当 categoryId 为 null（首页）时，不传递 fromCategory，让后端使用默认的全局视频列表逻辑
    const categoryParam = categoryId ? `?fromCategory=${categoryId}` : '';
    
    // 🚀 查找视频数据用于预加载
    const video = videos.find(v => v.id === videoId);
    if (video) {
      // 准备预加载数据
      const preloadData = {
        id: video.id,
        title: video.title,
        subtitle: video.subtitle,
        categories: video.categories || [],
        author: video.author,
        publishedAt: video.publishedAt,
        createdAt: video.createdAt,
        viewCount: video.viewCount,
        thumbnail: video.thumbnail
      };
      
      // 🎯 Router State传递（优先级最高），携带上下文参数
      navigate(`/video/${videoId}${categoryParam}`, { 
        state: { preloadData } 
      });
      
      // 💾 SessionStorage备份（处理刷新情况）
      try {
        sessionStorage.setItem(`video_preload_${videoId}`, JSON.stringify({
          ...preloadData,
          timestamp: Date.now()
        }));
      } catch (e) {
        // 忽略存储错误，不影响主要功能
      }
    } else {
      // 降级处理：没有找到视频数据时的普通导航，也携带上下文参数
      navigate(`/video/${videoId}${categoryParam}`);
    }
  };

  // 监听窗口大小变化
  // 页面卸载时保存状态
  useEffect(() => {
    const persistState = () => {
      if (videos.length === 0) return;
      const scrollMeta = captureAnchorSnapshot({
        items: videos,
        getDomId: (item) => `card-${item.id}`,
        headerOffset: 120
      });
      const sessionState = {
        videos,
        ads,
        bottomAds,
        adConfig,
        pagination,
        categoryId,
        isDesktop,
        scrollMeta,
        timestamp: Date.now()
      };
      sessionStorage.setItem(pageKey, JSON.stringify(sessionState));
      saveCurrentState(scrollMeta);
      saveListState(sessionState);
    };

    const handleBeforeUnload = () => {
      persistState();
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        persistState();
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [videos, ads, bottomAds, adConfig, pagination, isDesktop, saveListState, categoryId, captureAnchorSnapshot]);

  useEffect(() => {
    if (!isHydratedFromCache) return;
    if (videos.length === 0) return;
    const scrollMeta = captureAnchorSnapshot({
      items: videos,
      getDomId: (item) => `card-${item.id}`,
      headerOffset: 120
    });
    autoSaveState({
      videos,
      ads,
      bottomAds,
      adConfig,
      pagination,
      categoryId,
      isDesktop,
      scrollMeta
    });
  }, [autoSaveState, videos, ads, bottomAds, adConfig, pagination, categoryId, isDesktop, isHydratedFromCache, captureAnchorSnapshot]);

  // 处理横幅广告点击（底部广告）
  const handleBottomAdClick = async (ad) => {
    try {
      await advertisementService.clickAd(ad.id);

      if (ad.linkUrl) {
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    } catch (error) {
      
      if (ad.linkUrl) {
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    }
  };



  // 创建混合内容（视频+广告） - 使用配置化显示模式
  const createMixedContent = () => {
    // 如果没有配置信息，返回纯视频内容
    if (!adConfig) {
      return videos.map(video => ({ ...video, type: 'video' }));
    }

    // 使用advertisementService的混合内容创建方法
    const mixedItems = advertisementService.createMixedContentList(
      videos,
      ads,
      adConfig.adDisplayMode,
      adConfig.adInterval
    );

    // 转换为组件需要的格式
    return mixedItems.map(item => ({
      ...item.data,
      type: item.type === 'ad' ? 'ad' : 'video'
    }));
  };

  const mixedContent = createMixedContent();

  // 注意：移除了恢复状态的特殊显示，直接使用正常渲染避免闪烁

  // 首次进入且视频数据未到时，展示简洁的“加载中”提示，避免显示页面框架
  if (loading && videos.length === 0) {
    return (
      <div 
        className="container mx-auto px-1 md:px-4 pb-8 max-w-5xl"
        style={{ minHeight: '50vh' }}
      >
        <div className="flex justify-center items-center" style={{ height: '220px' }}>
          <div className="text-gray-300 text-base md:text-lg">内容加载中，请稍候...</div>
        </div>
      </div>
    );
  }

  return (
      <div 
        className="container mx-auto px-1 md:px-4 pb-8 max-w-5xl"
        style={{
          // 防止页面跳动的样式
          minHeight: '100vh',
          transition: 'none' // 禁用过渡动画，避免闪烁
        }}
      >
      {/* 热门角标样式，仅此处注入一次 */}
      <style>{`
        .hot-wrap{width:100%;height:100%;position:absolute;top:-8px;left:8px;overflow:hidden;font-size:17px;font-weight:400;color:#fff;opacity:.6;transition:.3s ease all;pointer-events:none;z-index:40}
        .hot-wrap:hover{opacity:.9}
        .hot-ribbon{display:inline-block;text-align:center;width:200px;height:30px;line-height:27px;position:absolute;top:30px;right:-50px;z-index:41;overflow:hidden;transform:rotate(45deg);border:1px dashed;box-shadow:0 0 0 1px #000,0 21px 5px -18px rgba(0,0,0,.6);background:rgb(1,132,127)}
      `}</style>
      {/* 移动端视图切换按钮（仅移动端显示） */}
      {!isDesktop && (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 12px 10px' }}>
          <div style={{
            display: 'inline-flex',
            background: 'rgba(255,255,255,0.06)',
            borderRadius: '20px',
            overflow: 'hidden',
            border: '1px solid rgba(255,255,255,0.1)',
            padding: '3px',
            gap: '2px',
          }}>
            {[{ key: 'image', label: '图片' }, { key: 'text', label: '文字' }].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => {
                  setViewMode(key);
                  try { localStorage.setItem('videolist_view_mode', key); } catch {}
                }}
                style={{
                  padding: '5px 20px',
                  fontSize: '13px',
                  border: 'none',
                  cursor: 'pointer',
                  borderRadius: '16px',
                  background: viewMode === key
                    ? 'linear-gradient(135deg, rgba(255,255,255,0.22) 0%, rgba(255,255,255,0.12) 100%)'
                    : 'transparent',
                  color: viewMode === key ? '#fff' : 'rgba(255,255,255,0.4)',
                  fontWeight: viewMode === key ? '500' : '400',
                  letterSpacing: '1px',
                  boxShadow: viewMode === key ? '0 1px 6px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.15)' : 'none',
                  transition: 'all 0.2s ease',
                }}
              >{label}</button>
            ))}
          </div>
        </div>
      )}

      {/* 图片模式（仅移动端） */}
      {!isDesktop && viewMode === 'image' ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', padding: '0 0 10px' }}>
          {mixedContent.map((item, idx) => {
            if (item.type === 'ad') {
              return <div key={`ad-${item?.id ?? idx}`}><AdCard ad={item} index={idx} /></div>;
            }
            return (
              <div key={`img-${item?.id ?? idx}`}
                className="cursor-pointer w-full overflow-hidden"
                style={{ backgroundColor: '#1e1e1e', borderRadius: '8px' }}
                onClick={() => handleVideoClick(item.id)}>
                {/* 16:9 横屏封面，背景 #000 与分页模式一致 */}
                <div className="relative overflow-hidden" style={{ width: '100%', aspectRatio: '16/9', background: '#000', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
                  {item.coverImageUrl && (
                    <SecureDecryptedImage
                      src={item.coverImageUrl}
                      alt={item.title || ''}
                      objectFit="cover"
                      style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
                      lazyLoad={idx >= 4}
                    />
                  )}
                  {/* HOT 角标 */}
                  {item.isHot === 1 && (
                    <div className="hot-wrap" aria-label="热门视频">
                      <div className="hot-ribbon">热搜 HOT</div>
                    </div>
                  )}
                </div>
                {/* 标题区，与分页模式移动端一致：深色背景 + 圆角内边距 */}
                <div style={{ padding: '10px 8px', background: 'rgba(0,0,0,0.7)', minHeight: '68px' }}>
                  <div style={{ fontSize: 16, fontWeight: 600, color: '#fff', lineHeight: 1.5, letterSpacing: '0.02em', overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', wordBreak: 'break-word' }}>
                    {item.title || ''}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
      /* 文字模式 / PC端：原始列表布局 */
      <MixedContentList
        items={mixedContent}
        isDesktop={isDesktop}
        gapDesktop="48px"
        gapMobile="10px"
        containerClassName="flex flex-col"
        dataAttribute="data-video-list"
        itemWrapperClassName="flex justify-center"
        getItemKey={(item, index) => `${item?.type || 'item'}-${item?.id ?? item?.data?.id ?? index}`}
        renderContentItem={(item, videoIdx) => (
          <VideoCard
            video={item}
            index={videoIdx}
            navigate={navigate}
            categoryId={categoryId}
            isDesktop={isDesktop}
            onVideoClick={handleVideoClick}
          />
        )}
        renderAdItem={(item, index) => <AdCard ad={item} index={index} />}
      />
      )}
      
      {/* 分页组件：仅在有列表数据后再显示，避免 1/0 闪烁 */}
      {videos.length > 0 && (
        <PaginationComponent 
          current={pagination.current} 
          total={pagination.total} 
          pageSize={pagination.pageSize}
          onChange={handlePageChange}
        />
      )}
      
      {/* 底部横幅广告 - 参考详情页面样式 */}
      <BottomBannerAds ads={bottomAds} onAdClick={handleBottomAdClick} />

      {/* Logo广告区域：等列表已展示后再渲染，避免先看到Logo再看到列表的体验问题 */}
      {videos.length > 0 && (
        <div className="flex justify-center mb-12">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <LogoAds hideWhileLoading={true} />
          </div>
        </div>
      )}

      {/* 底部区域 */}
      <CommonFooter />
      

      </div>
  );
};

// 视频卡片组件
const VideoCard = ({ video, index, navigate, categoryId, isDesktop, onVideoClick }) => {
  const cardRef = useRef(null);

  // 处理视频点击
  const handleVideoClick = () => {
    if (onVideoClick) {
      // 使用传入的点击处理函数（支持状态保存）
      onVideoClick(video.id);
    } else if (navigate && video.id) {
      // 降级处理：直接导航（不保存状态）
      const categoryParam = categoryId ? `?fromCategory=${categoryId}` : '';
      navigate(`/video/${video.id}${categoryParam}`);
    }
  };

  // 处理鼠标悬停
  const handleMouseEnter = () => {
    // 可以在这里添加悬停效果
  };

  if (video.type === 'video') {
    // 视频卡片布局 - 图片背景 + 居中文字
    return (
      <div 
        ref={cardRef}
        id={`card-${video.id}`}
        className={isDesktop ? "group cursor-pointer transform transition-all duration-300 hover:scale-[1.03] hover:shadow-2xl w-full max-w-none md:max-w-[860px]" : "cursor-pointer w-full max-w-none md:max-w-[860px]"}
        onClick={handleVideoClick}
        onMouseEnter={handleMouseEnter}
        style={{ scrollMarginTop: '112px' }}
      >
        <div
          className={`relative overflow-hidden shadow-lg ${isDesktop ? 'rounded-lg transition-all duration-300 group-hover:shadow-xl' : 'rounded'} ${!isDesktop ? 'aspect-video' : ''}`}
          style={{ height: isDesktop ? '280px' : 'auto' }}
        >
          {/* 热门角标（isHot === 1 时显示） */}
          {video && Number(video.isHot) === 1 && (
            <div className="hot-wrap" aria-label="热门视频">
              <div className="hot-ribbon">热搜 HOT</div>
            </div>
          )}
          <SecureDecryptedImage
            src={video.coverImageUrl}
            alt={video.title}
            priority="normal"
            className="w-full h-full"
            lazyLoad={false}
            objectFit="cover"
            style={{
              width: '100%',
              height: '100%',
              display: 'block',
              backgroundColor: '#1F1D1D'
            }}
          />
          
          {/* 暗色遮挡层 */}
          <div className="absolute inset-0 bg-black bg-opacity-20"></div>
          
          {/* 文字内容居中叠加 */}
          <div className="absolute inset-0 flex flex-col justify-center items-center text-white text-center px-6 py-4" style={{ 
            minHeight: isDesktop ? '280px' : '200px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            alignItems: 'center'
          }}>
            {/* 标题 */}
            <h2 className="mb-2 video-list-title" style={{ 
              fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
              fontSize: '28px',
              lineHeight: '32px',
              fontWeight: '400',
              fontStyle: 'normal',
              color: 'rgb(255, 255, 255)',
              textShadow: '1px 1px 3px rgba(0,0,0,0.9), 2px 2px 6px rgba(0,0,0,0.6)',
              wordBreak: 'normal',
              overflowWrap: 'break-word',
              hyphens: 'auto'
            }}>
              {video.title}
            </h2>
            
            {/* 作者、日期、分类信息 */}
            <div className="w-full text-center" style={{ 
              fontFamily: 'Consolas, Menlo, Monaco, "lucida console", "Liberation Mono", "Courier New", "andale mono", monospaceX, monospace, sans-serif',
              fontStyle: 'normal',
              fontWeight: '400',
              color: 'rgb(238, 238, 238)',
              fontSize: isDesktop ? '15px' : '13px',
              lineHeight: isDesktop ? '17px' : '15px',
              textShadow: '1px 1px 3px rgba(0,0,0,0.9), 2px 2px 6px rgba(0,0,0,0.5)',
              wordBreak: 'break-word',
              whiteSpace: 'normal'
            }}>
              {/* 作者 */}
              <span className="font-medium">
                {video.author || '天涯吃瓜小慧'}
              </span>
              <span className="mx-1 md:mx-2">•</span>
              {/* 日期 */}
              <span>{new Date(video.publishedAt).getFullYear()} 年 {String(new Date(video.publishedAt).getMonth() + 1).padStart(2, '0')} 月 {String(new Date(video.publishedAt).getDate()).padStart(2, '0')} 日</span>
              <span className="mx-1 md:mx-2">•</span>
              {/* 分类信息 */}
              <span>
                {video.categories && video.categories.length > 0 ? (
                  video.categories.map(category => category.name).join(' • ')
                ) : (
                  video.categoryName || '今日吃瓜'
                )}
              </span>
            </div>
          </div>
          
          {/* 播放按钮覆盖层（仅桌面端显示；移动端移除以避免点击时出现图标与动画） */}
          {isDesktop && (
            <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-opacity duration-300 flex items-center justify-center" style={{
              minHeight: isDesktop ? '280px' : '200px'
            }}>
              <div className="w-16 h-16 bg-white bg-opacity-0 group-hover:bg-opacity-95 rounded-full transform scale-0 group-hover:scale-100 transition-all duration-300 flex items-center justify-center">
                <svg className="w-8 h-8 text-gray-800 ml-1" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z"/>
                </svg>
              </div>
            </div>
          )}
          
          {/* 悬浮效果覆盖层（仅桌面端显示） */}
          {isDesktop && (
            <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-0 group-hover:opacity-30 transition-opacity duration-300"></div>
          )}
        </div>
      </div>
    );
  } else if (video.type === 'ad') {
    // 广告横幅布局
    return (
      <div className="cursor-pointer w-full" style={{ maxWidth: '860px' }}>
        <div 
          className="relative rounded-lg overflow-hidden shadow-lg"
          style={{
            height: '280px',
            backgroundImage: `linear-gradient(135deg, rgba(147, 51, 234, 0.8), rgba(79, 70, 229, 0.8)), url(${video.backgroundImage})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center'
          }}
        >
          {/* 内容区域 */}
          <div className="relative h-full flex items-center justify-between p-8 text-white">
            {/* 左侧内容 */}
            <div className="flex-1">
              {/* 主标题 */}
              <h1 className="text-4xl md:text-5xl font-bold mb-3">
                {video.title}
              </h1>
              
              {/* 副标题 */}
              <h2 className="text-2xl md:text-3xl font-semibold mb-6">
                {video.subtitle}
              </h2>
              
              {/* 标签圆圈 */}
              <div className="flex flex-wrap gap-4 mb-6">
                {video.tags.map((tag, tagIndex) => (
                  <div key={tagIndex} className="w-16 h-16 bg-transparent rounded-full flex items-center justify-center">
                    <span className="text-sm font-medium">{tag}</span>
                  </div>
                ))}
              </div>
              
              {/* CTA按钮 */}
              <button className="bg-gradient-to-r from-pink-500 to-red-500 text-white font-bold py-3 px-8 rounded-full shadow-lg">
                {video.buttonText}
              </button>
            </div>
            
            {/* 右侧装饰 */}
            <div className="hidden md:block">
              {/* HOT标签 */}
              {video.hotTag && (
                <div className="absolute top-4 right-4 bg-green-500 text-white text-sm font-bold px-3 py-1 rounded-full transform rotate-12">
                  {video.hotTag}
                </div>
              )}
            </div>
          </div>
          
          {/* 悬浮效果覆盖层 */}
          <div className="absolute inset-0 bg-gradient-to-r from-purple-500 to-blue-500 opacity-20"></div>
        </div>
      </div>
    );
  }
};

// 分页组件
const PaginationComponent = ({ current, total, pageSize, onChange }) => {
  const totalPages = Math.ceil(total / pageSize);
  
  return (
    <div className="flex justify-center items-center mt-16 mb-8">
      <div className="flex items-center space-x-3">
        {/* 上一页按钮 */}
        <button 
          className="px-4 h-9 bg-transparent text-white border border-white rounded-md hover:bg-white/10 transition-colors duration-150 flex items-center justify-center whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={() => {
            if (current > 1) {
              onChange(current - 1);
            }
          }}
          disabled={current <= 1}
        >
          上一页
        </button>
        
        {/* 页码信息 */}
        <span className="text-white text-base">{current}/{totalPages}</span>
        
        {/* 页码输入框 */}
        <input 
          type="number" 
          min="1"
          max={totalPages}
          placeholder={current}
          className="w-20 h-9 px-3 text-base bg-transparent text-white border border-white rounded-md focus:outline-none focus:border-white/90"
          onKeyPress={(e) => {
            if (e.key === 'Enter') {
              const pageNum = parseInt(e.currentTarget.value);
              if (pageNum >= 1 && pageNum <= totalPages) {
                onChange(pageNum);
                e.currentTarget.value = ''; // 清空输入框
              }
            }
          }}
        />
        
        {/* 跳转按钮 */}
        <button 
          className="px-4 h-9 text-base bg-transparent text-white border border-white rounded-md hover:bg-white/10 focus:bg-white/10 focus:border-white transition-colors duration-150 whitespace-nowrap"
          onClick={(e) => {
            const inputElement = e.currentTarget.previousElementSibling;
            if (inputElement && inputElement.nodeName === 'INPUT') {
              const pageNum = parseInt(inputElement['value'] || '0');
              if (pageNum >= 1 && pageNum <= totalPages) {
                onChange(pageNum);
                inputElement['value'] = ''; // 清空输入框
              }
            }
          }}
        >
          跳转
        </button>
        
        {/* 下一页按钮 */}
        <button 
          className="px-4 h-9 bg-transparent text-white border border-white rounded-md hover:bg-white/10 transition-colors duration-150 flex items-center justify-center whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={() => {
            if (current < totalPages) {
              onChange(current + 1);
            }
          }}
          disabled={current >= totalPages}
        >
          下一页
        </button>
      </div>
    </div>
  );
};

export default VideoList; 
/* Force update Mon Aug  4 04:18:37 CST 2025 */
/* Mobile display update Mon Aug  4 04:40:22 CST 2025 */
/* Mobile full width update Mon Aug  4 04:42:44 CST 2025 */
/* Fixed responsive width - desktop preserved Mon Aug  4 04:45:01 CST 2025 */
/* VideoList switched to img mode Mon Aug  4 04:50:57 CST 2025 */
/* All components switched to aspect-video Mon Aug  4 05:00:02 CST 2025 */
/* Desktop height aligned with ads 280px Mon Aug  4 05:24:59 CST 2025 */
/* Reduced overlay opacity and enhanced text shadow Mon Aug  4 05:29:13 CST 2025 */
