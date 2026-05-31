import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams, useNavigate, useNavigationType } from 'react-router-dom';
import searchService from '../../services/searchApi';
import videoStatsService from '../../services/videoStatsService';
import advertisementService from '../../services/advertisementService';
import apiCacheService from '../../services/apiCacheService';
import useIsDesktop from '../../hooks/useIsDesktop';
import useListStateManager from '../../hooks/useListStateManager';

import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';
import Pagination from '../../components/ui/Pagination';
import Footer from '../../components/common/Footer';
import { usePageConfig } from '../../hooks/usePageConfig';
import MixedContentList from '../../components/common/MixedContentList';
import useListAds from '../../hooks/useListAds';
import usePagedContent from '../../hooks/usePagedContent';
import { isVideoUrlCacheExpired } from '../../constants/cacheConfig';
// 兼容TS类型检查的简单包装，避免props类型限制
const DecryptedImage = (props) => <SecureDecryptedImage {...props} />;

const SearchResults = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const navigationType = useNavigationType();
  const keyword = searchParams.get('q') || '';
  const isDesktop = useIsDesktop();
  const pageSize = 20;
  const pageKey = `search_${keyword || 'empty'}`;
  const { restoreListState, autoSaveState, saveListState, captureAnchorSnapshot, restoreAnchorPosition } = useListStateManager(pageKey, { autoSave: false });
  
  // 初始化：尝试同步读取缓存，避免首帧闪烁（含视频 URL 签名有效期检查）
  const initializeFromCache = () => {
    try {
      const cacheStr = sessionStorage.getItem(`search_${keyword || 'empty'}`);
      if (cacheStr) {
        const cache = JSON.parse(cacheStr);
        if (cache && cache.keyword === keyword && Array.isArray(cache.videos) && !isVideoUrlCacheExpired(cache)) {
          return {
            videos: cache.videos,
            totalCount: cache.totalCount || 0,
            currentPage: cache.currentPage || 1
          };
        }
        if (cache && isVideoUrlCacheExpired(cache)) {
          sessionStorage.removeItem(`search_${keyword || 'empty'}`);
        }
      }
    } catch (_) {}
    return null;
  };
  const cachedInit = initializeFromCache();

  const [videos, setVideos] = useState(cachedInit?.videos || []);
  const [loading, setLoading] = useState(!cachedInit);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(cachedInit?.currentPage || 1);
  const [totalCount, setTotalCount] = useState(cachedInit?.totalCount || 0);
  const [randomBackgroundImage, setRandomBackgroundImage] = useState('');
  const {
    bottomAds,
    refreshBottomAds
  } = useListAds({
    enableTopAds: false,
    enableConfig: false,
    bottomPosition: '7'
  });
  const [restored, setRestored] = useState(!!cachedInit);
  const skipInitialFetchRef = useRef(false);
  const pendingScrollMetaRef = useRef(null);
  
  // 获取页面配置
  const { getConfig } = usePageConfig();
  
  

  // 从搜索结果中获取随机背景图片
  const getRandomBackgroundFromResults = useCallback((videoList) => {

    if (videoList && videoList.length > 0) {

      // 检查每个视频的封面图片字段
      videoList.forEach((video, index) => {
        
      });
      
      const videosWithImages = videoList.filter(video => video.coverImageUrl && video.coverImageUrl.trim() !== '');

      if (videosWithImages.length > 0) {
        const randomIndex = Math.floor(Math.random() * videosWithImages.length);
        const randomVideo = videosWithImages[randomIndex];

        // 验证URL格式
        if (randomVideo.coverImageUrl && randomVideo.coverImageUrl.includes('http')) {
          setRandomBackgroundImage(randomVideo.coverImageUrl);
          
        } else {
          
          // 尝试下一个视频
          if (videosWithImages.length > 1) {
            const nextIndex = (randomIndex + 1) % videosWithImages.length;
            const nextVideo = videosWithImages[nextIndex];
            if (nextVideo.coverImageUrl && nextVideo.coverImageUrl.includes('http')) {
              setRandomBackgroundImage(nextVideo.coverImageUrl);
              
            }
          }
        }
        
        // 验证状态更新
        setTimeout(() => {
          
        }, 100);
      } else {

      }
    } else {
      
    }
  }, []);

  useEffect(() => {
    try {
      const flag = sessionStorage.getItem(pageKey + ':skip');
      if (flag) {
        skipInitialFetchRef.current = true;
        sessionStorage.removeItem(pageKey + ':skip');
      }
    } catch (_) {}

    const prevScroll = window.history.scrollRestoration;
    if (navigationType !== 'POP') {
      try { window.history.scrollRestoration = 'manual'; } catch (_) {}
    }

    const restoredFromList = restoreListState();
    if (restoredFromList?.scrollMeta) {
      pendingScrollMetaRef.current = restoredFromList.scrollMeta;
    }

    let cleanup = null;
    try {
      const cacheStr = sessionStorage.getItem(pageKey);
      if (cacheStr) {
        const cache = JSON.parse(cacheStr);
        if (cache && cache.keyword === keyword && Array.isArray(cache.videos) && !isVideoUrlCacheExpired(cache)) {
          setVideos(cache.videos);
          setTotalCount(cache.totalCount || 0);
          setCurrentPage(cache.currentPage || 1);
          setLoading(false);
          pendingScrollMetaRef.current = cache.scrollMeta || cache;
          setRestored(true);
        } else if (cache && isVideoUrlCacheExpired(cache)) {
          sessionStorage.removeItem(pageKey);
        }
      }
    } catch (_) {}

    refreshBottomAds();

    return () => {
      try { window.history.scrollRestoration = prevScroll || 'auto'; } catch (_) {}
      if (cleanup) cleanup();
    };
  }, [refreshBottomAds]);

  useEffect(() => {
    if (videos.length === 0) return;
    autoSaveState({
      videos,
      totalCount,
      currentPage,
      keyword,
      bottomAds,
      scrollMeta: captureAnchorSnapshot({
        items: videos,
        getDomId: (item) => `search-card-${item.id}`,
        headerOffset: 120
      })
    });
  }, [autoSaveState, videos, totalCount, currentPage, keyword, bottomAds, captureAnchorSnapshot]);

  useEffect(() => {
    if (!pendingScrollMetaRef.current) return;
    if (videos.length === 0) return;
    restoreAnchorPosition(pendingScrollMetaRef.current, { headerOffset: 120 });
    pendingScrollMetaRef.current = null;
  }, [videos, restoreAnchorPosition]);

  useEffect(() => {
    const persistState = () => {
      if (videos.length === 0) return;
      const scrollMeta = captureAnchorSnapshot({
        items: videos,
        getDomId: (item) => `search-card-${item.id}`,
        headerOffset: 120
      });
      saveListState({
        videos,
        totalCount,
        currentPage,
        keyword,
        bottomAds,
        scrollMeta
      });
    };

    const beforeUnload = () => persistState();
    const onVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        persistState();
      }
    };

    window.addEventListener('beforeunload', beforeUnload);
    document.addEventListener('visibilitychange', onVisibilityChange);

    return () => {
      window.removeEventListener('beforeunload', beforeUnload);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [videos, totalCount, currentPage, keyword, bottomAds, saveListState, captureAnchorSnapshot]);

  // 搜索视频
  const fetchSearchPage = useCallback(async (page = 1) => {
    const trimmed = keyword.trim();
    if (!trimmed) {
      return {
        items: [],
        total: 0
      };
    }

    const response = await apiCacheService.getApiData(
      'SEARCH_VIDEOS',
      { keyword: trimmed, pageNum: page, pageSize },
      () => searchService.searchVideos(trimmed, page, pageSize),
      {
        cacheDuration: 2 * 60 * 1000,
        skipCacheIf: (res) => {
          const ok = res && res.code === 200;
          const rows = (res && res.rows) || [];
          return ok && Array.isArray(rows) && rows.length === 0;
        },
        emptyCacheDuration: 15 * 1000
      }
    );

    if (response.code === 200) {
      const videoList = response.rows || [];
      return {
        items: videoList,
        total: response.total || 0
      };
    }

    const err = new Error(response.msg || '搜索失败');
    err.name = 'SearchError';
    throw err;
  }, [keyword, pageSize]);

  const handleSearchSuccess = useCallback(({ result, page }) => {
    const items = result.items || [];
    setVideos(items);
    setTotalCount(result.total || 0);
    setCurrentPage(page);
    setError(null);
    // 只在首屏上报一次搜索事件，避免下拉加载更多被误记为"重复搜索"
    if (page === 1 && keyword.trim()) {
      try { videoStatsService.trackSearchSubmit(keyword); } catch (_) {}
      if (items.length === 0) {
        try { videoStatsService.trackSearchNoResult(keyword); } catch (_) {}
      }
    }
    if (page === 1) {
      getRandomBackgroundFromResults(result.items || []);
    }
    setLoading(false);
    if (pendingScrollMetaRef.current) {
      restoreAnchorPosition(pendingScrollMetaRef.current, { headerOffset: 120 });
      pendingScrollMetaRef.current = null;
    }
  }, [keyword, getRandomBackgroundFromResults, restoreAnchorPosition]);

  const handleSearchError = useCallback(({ error }) => {
    setVideos([]);
    setError(error?.message || '搜索时发生错误，请稍后重试');
    setLoading(false);
  }, []);

  const { loadPage: loadSearchPage, loading: searchPageLoading } = usePagedContent({
    fetcher: fetchSearchPage,
    onSuccess: handleSearchSuccess,
    onError: handleSearchError
  });

  useEffect(() => {
    if (searchPageLoading) {
      setLoading(true);
    }
  }, [searchPageLoading]);

  const searchVideos = useCallback(async (page = 1) => {
    if (!keyword.trim()) {
      setVideos([]);
      setTotalCount(0);
      setCurrentPage(1);
      setLoading(false);
      return;
    }

    setError(null);
    setLoading(true);
    try {
      await loadSearchPage(page, { showLoading: true });
    } catch (_) {
      // 错误已在 onError 中处理
    }
  }, [keyword, loadSearchPage]);

  // 处理分页
  const handlePageChange = useCallback(async (page) => {
    await searchVideos(page);
    try {
      const listEl = document.querySelector('[data-search-list]');
      if (listEl) {
        const rect = listEl.getBoundingClientRect();
        const absoluteTop = rect.top + window.pageYOffset - 120; // 预留Header
        window.scrollTo(0, Math.max(0, absoluteTop));
      } else {
        window.scrollTo(0, 0);
      }
    } catch (_) {
      window.scrollTo(0, 0);
    }
  }, [searchVideos]);

  // 保存当前搜索页面状态并跳转到详情
  const navigateToDetail = (videoId) => {
    try {
      const scrollMeta = captureAnchorSnapshot({
        items: videos,
        getDomId: (item) => `search-card-${item.id}`,
        headerOffset: 120
      });
      const sessionState = {
        videos,
        totalCount,
        currentPage,
        keyword,
        bottomAds,
        scrollMeta,
        timestamp: Date.now()
      };
      sessionStorage.setItem(pageKey, JSON.stringify(sessionState));
      sessionStorage.setItem(pageKey + ':skip', '1');
      saveListState(sessionState);
    } catch (_) {}
    // 🔍 携带搜索关键词作为上下文参数
    const searchParam = keyword ? `?searchKeyword=${encodeURIComponent(keyword)}` : '';
    navigate(`/video/${videoId}${searchParam}`, { state: { fromSearch: true }, replace: false });
  };

  // 监控背景图片状态变化
  useEffect(() => {
    
    if (randomBackgroundImage) {
      
    } else {
      
    }
  }, [randomBackgroundImage]);

  // 页面初始化
  useEffect(() => {
    
    // 搜索页：显式要求首屏置顶（首次搜索或关键词变化），但从详情后退（POP）不置顶
    if (navigationType !== 'POP' && !skipInitialFetchRef.current) {
      window.scrollTo(0, 0);
    }
    
    // 获取底部广告
    refreshBottomAds();
    // 注意：页面 PV 埋点已由 App.jsx 全局路由监听统一上报，此处不再重复
    
    if (skipInitialFetchRef.current) {
      // 跳过一次初始化请求
      skipInitialFetchRef.current = false;
    } else if (restored) {
      // 本次渲染已从缓存恢复，跳过立即搜索，下一次交互再触发
      setRestored(false);
    } else if (keyword.trim()) {
      searchVideos(1);
    } else {
      setLoading(false);
    }
  }, [keyword, navigationType, refreshBottomAds, searchVideos, restored]);

  // 如果没有关键词，重定向到首页
  if (!keyword.trim()) {
    return (
      <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
        <div className="pt-28 md:pt-24 flex items-center justify-center">
          <div className="text-white text-center">
            <h2 className="text-2xl mb-4">请输入搜索关键词</h2>
            <button 
              onClick={() => navigate('/')}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
            >
              返回首页
            </button>
          </div>
        </div>
      </div>
    );
  }

  // 在渲染时记录状态

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      
      {/* 搜索标题区域 - 带随机背景 */}
      {randomBackgroundImage ? (
        <div 
          className="relative w-full"
          style={{
            height: '250px',
            width: '100%',
            overflow: 'hidden'
          }}
        >
          <DecryptedImage
            src={randomBackgroundImage}
            asBackground={true}
            className="absolute inset-0 w-full h-full"
            style={{
              width: '100%',
              height: '100%'
            }}
          />
          
          {/* 遮罩层 - 确保充满整个区域 */}
          <div 
            className="absolute inset-0 w-full h-full bg-black"
            style={{ 
              opacity: 0.6,
              top: 0,
              left: 0,
              right: 0,
              bottom: 0
            }}
          ></div>
          
          {/* 标题内容 */}
          <div className="absolute inset-0 w-full h-full flex justify-center items-center px-4 pt-28 md:pt-24 pb-8" style={{ zIndex: 10 }}>
            <div className="text-center w-full">
              <h1 
                className="text-3xl md:text-4xl font-bold text-white mb-4"
                style={{ 
                  textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
                  letterSpacing: '1px'
                }}
              >
                包含关键字 "{keyword}" 的文章
              </h1>
              {!loading && (
                <p className="text-gray-200 text-lg">
                  共找到 {totalCount} 条相关结果
                </p>
              )}
            </div>
          </div>
        </div>
      ) : (
        <div 
          className="relative w-full pt-28 md:pt-24 pb-8"
          style={{
            background: '#2C2A2A',
            height: '250px',
            width: '100%',
            display: 'block'
          }}
        >
          {/* 遮罩层 - 确保充满整个区域 */}
          <div 
            className="absolute inset-0 w-full h-full bg-black"
            style={{ 
              opacity: 0.3,
              top: 0,
              left: 0,
              right: 0,
              bottom: 0
            }}
          ></div>
          
          {/* 标题内容 */}
          <div className="relative z-10 w-full h-full flex justify-center items-center px-4" style={{ minHeight: '250px' }}>
            <div className="text-center w-full">
              <h1 
                className="text-3xl md:text-4xl font-bold text-white mb-4"
                style={{ 
                  textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
                  letterSpacing: '1px'
                }}
              >
                包含关键字 "{keyword}" 的文章
              </h1>
              {!loading && (
                <p className="text-gray-200 text-lg">
                  共找到 {totalCount} 条相关结果
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 顶部加载提示（点击搜索后立即在标题下方显示） */}
      {loading && (
        <div className="flex justify-center" style={{ backgroundColor: '#2C2A2A' }}>
          <div className="w-full text-center text-gray-300 py-3" style={{ maxWidth: '860px' }}>
            搜索中，请稍候...
          </div>
        </div>
      )}

      {/* 主要内容区域 */}
      <div className="pt-12 pb-8">
        
        {/* 加载状态 */}
        {loading && videos.length === 0 && (
          <div className="flex justify-center">
            <div className="w-full" style={{ maxWidth: '860px' }}>
              <div className="px-4 py-12">
                <div className="flex justify-center items-center" style={{ height: '220px', backgroundColor: '#1F1D1D', borderRadius: '12px' }}>
                  <div className="text-gray-300 text-base md:text-lg">搜索中，请稍候...</div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 错误状态 */}
        {error && !loading && (
          <div className="flex justify-center py-12">
            <div className="text-center">
              <div className="text-red-400 text-lg mb-4">{error}</div>
              <button 
                onClick={() => searchVideos(1)}
                className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
              >
                重试
              </button>
            </div>
          </div>
        )}

        {/* 搜索结果列表 */}
        {!loading && !error && (
          <>
            {videos.length > 0 ? (
              <>
                <div className="flex justify-center">
                  <div className="w-full" style={{ maxWidth: '860px' }}>
                    {/* 移动端：图片模式（与普通分类列表一致，图上文下）；PC：原叠加卡片 */}
                    {!isDesktop ? (
                      <div
                        data-search-list
                        style={{ display: 'flex', flexDirection: 'column', gap: '10px', padding: '0 8px 10px' }}
                      >
                        {videos.map((video, idx) => (
                          <SearchImageCard
                            key={`search-img-${video?.id ?? idx}`}
                            video={video}
                            index={idx}
                            onClick={navigateToDetail}
                          />
                        ))}
                      </div>
                    ) : (
                      <MixedContentList
                        items={videos.map(video => ({ type: 'video', data: video }))}
                        containerClassName="space-y-12 px-4"
                        dataAttribute="data-search-list"
                        itemWrapperClassName="flex justify-center"
                        getItemKey={(item, index) => `search-${item?.data?.id ?? item?.id ?? index}`}
                        getItemWrapperProps={(item) => ({ id: `search-card-${item?.data?.id ?? item?.id}` })}
                        renderContentItem={(item, videoIdx) => (
                          <SearchVideoCard
                            video={{ ...item.data, type: 'video' }}
                            index={videoIdx}
                            onClick={navigateToDetail}
                            isDesktop={isDesktop}
                          />
                        )}
                      />
                    )}
                  </div>
                </div>

                {/* 分页组件 */}
                {totalCount > pageSize && (
                  <div className="flex justify-center mt-12">
                    <Pagination
                      current={currentPage}
                      total={totalCount}
                      pageSize={pageSize}
                      onChange={handlePageChange}
                    />
                  </div>
                )}
              </>
            ) : (
              <div className="flex justify-center py-12">
                <div className="text-center">
                  <div className="text-gray-400 text-lg mb-4">
                    没有找到包含 "{keyword}" 的视频
                  </div>
                  <button 
                    onClick={() => navigate('/')}
                    className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
                  >
                    浏览其他内容
                  </button>
                </div>
              </div>
            )}
          </>
        )}

        {/* 底部横幅广告 */}
        <div className="mt-12 mb-8">
          <SearchAdBanner bottomAds={bottomAds} />
        </div>

        {/* 底部区域 */}
        <Footer />
      </div>
    </div>
  );
};

// 加载中的视频卡片组件
const LoadingVideoCard = () => {
  return (
    <div 
      className="group cursor-pointer transform transition-all duration-300 hover:scale-[1.03] hover:shadow-2xl w-full" 
      style={{ maxWidth: '860px' }}
    >
      <div
        className="relative rounded-lg overflow-hidden shadow-lg transition-all duration-300 group-hover:shadow-xl"
        style={{
          height: '280px',
          background: 'linear-gradient(135deg, #4a5568 0%, #2d3748 100%)'
        }}
      >
        {/* 默认背景透明遮罩 - 确保文字可读性 */}
        <div className="absolute inset-0 bg-black bg-opacity-40"></div>
        
        {/* 加载中文字内容居中叠加 - 完全按照VideoList的样式 */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-white text-center p-6 z-10">
          {/* 加载动画 */}
          <div className="flex items-center justify-center mb-4">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white opacity-75"></div>
          </div>
          
          {/* 搜索中文字 */}
          <div className="text-xl md:text-2xl font-normal text-white">
            搜索中...
          </div>
        </div>
      </div>
    </div>
  );
};

// 移动端图片模式卡片（与普通分类列表 VideoList 图片模式保持一致：16:9 封面在上 + 标题下方深色条）
const SearchImageCard = ({ video, index, onClick }) => {
  const handleClick = () => {
    if (onClick && video.id) onClick(video.id);
  };

  return (
    <div
      id={`search-card-${video.id}`}
      className="cursor-pointer w-full overflow-hidden"
      style={{ backgroundColor: '#1e1e1e', borderRadius: '8px', scrollMarginTop: '112px' }}
      onClick={handleClick}
    >
      {/* 16:9 横屏封面，黑底 */}
      <div className="relative overflow-hidden" style={{ width: '100%', aspectRatio: '16/9', background: '#000', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
        {video.coverImageUrl && (
          <DecryptedImage
            src={video.coverImageUrl}
            alt={video.title || ''}
            objectFit="cover"
            style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
            lazyLoad={index >= 4}
          />
        )}
      </div>
      {/* 标题区：深色背景 + 2 行截断 */}
      <div style={{ padding: '10px 8px', background: 'rgba(0,0,0,0.7)', minHeight: '68px' }}>
        <div style={{ fontSize: 16, fontWeight: 600, color: '#fff', lineHeight: 1.5, letterSpacing: '0.02em', overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', wordBreak: 'break-word' }}>
          {video.title || ''}
        </div>
      </div>
    </div>
  );
};

// 搜索结果视频卡片组件
const SearchVideoCard = ({ video, index, onClick, isDesktop }) => {
  // 处理视频点击
  const handleVideoClick = () => {
    if (onClick && video.id) onClick(video.id);
  };

  // 视频卡片布局 - 完全复制VideoList的样式
  return (
    <div 
      className="group cursor-pointer transform transition-all duration-300 hover:scale-[1.03] hover:shadow-2xl w-full max-w-none md:max-w-[860px]"
      onClick={handleVideoClick}
    >
      <div className="relative aspect-video md:h-[280px] md:aspect-auto rounded-lg overflow-hidden shadow-lg transition-all duration-300 group-hover:shadow-xl">
        <DecryptedImage
          src={video.coverImageUrl}
          alt={video.title}
          priority={index < 3 ? 'high' : 'normal'}
          className="w-full h-full object-cover"
                        lazyLoad={false}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            display: 'block'
          }}
        />
                  {/* 暗色遮挡层 - 确保文字可读性 */}
          <div className="absolute inset-0 bg-black bg-opacity-20"></div>
        
        {/* 文字内容居中叠加 - 参考VideoList的样式 */}
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
            fontSize: isDesktop ? '28px' : '24px',
            lineHeight: isDesktop ? '32px' : '28px',
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
          <div style={{ 
            fontFamily: 'Consolas, Menlo, Monaco, "lucida console", "Liberation Mono", "Courier New", "andale mono", monospaceX, monospace, sans-serif',
            fontStyle: 'normal',
            fontWeight: '400',
            color: 'rgb(238, 238, 238)',
            fontSize: isDesktop ? '15px' : '13px',
            lineHeight: isDesktop ? '17px' : '15px',
            textShadow: '1px 1px 3px rgba(0,0,0,0.8)',
            wordBreak: 'break-word',
            whiteSpace: 'normal',
            paddingLeft: isDesktop ? '24px' : '16px',
            paddingRight: isDesktop ? '24px' : '16px',
            width: '100%',
            boxSizing: 'border-box'
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
        
        {/* 播放按钮覆盖层 - 参考VideoList的样式 */}
        <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-opacity duration-300 flex items-center justify-center" style={{
          minHeight: isDesktop ? '280px' : '200px'
        }}>
          <div className="w-16 h-16 bg-white bg-opacity-0 group-hover:bg-opacity-95 rounded-full transform scale-0 group-hover:scale-100 transition-all duration-300" style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg className="w-8 h-8 text-gray-800 ml-1" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z"/>
            </svg>
          </div>
        </div>
        
        {/* 悬浮效果覆盖层 - 已移除透明度效果 */}
      </div>
    </div>
  );
};

// 搜索页面广告横幅组件
const SearchAdBanner = ({ bottomAds = [] }) => {
  // 处理广告点击
  const handleAdClick = async (ad) => {
    try {
      // 统计点击
      await advertisementService.clickAd(ad.id);

      // 跳转链接
      if (ad.linkUrl) {
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    } catch (error) {
      
      // 仍然执行跳转
      if (ad.linkUrl) {
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    }
  };

  return (
    <div className="mb-6 md:mb-12" style={{ backgroundColor: '#2C2A2A' }}>
      <div className="flex justify-center py-3 md:py-6 px-1 md:px-4">
        <div className="w-full" style={{ maxWidth: '770px' }}>
          <div className="space-y-2 md:space-y-4">
            {bottomAds.length > 0 && bottomAds.filter(ad => ad.imageUrl).map((ad, index) => (
              <div 
                key={ad.id || index}
                className="cursor-pointer overflow-hidden"
                onClick={() => handleAdClick(ad)}
              >
                <div className="relative shadow-lg overflow-hidden">
                  <SecureDecryptedImage
                    src={ad.imageUrl}
                    alt="搜索页底部横幅广告"
                    priority="high"
                    lazyLoad={false}
                    objectFit={window.innerWidth < 768 ? 'contain' : 'cover'}
                    imageStyle={{
                      height: window.innerWidth < 768 ? 'auto' : '90px',
                      maxHeight: window.innerWidth < 768 ? 'none' : '90px'
                    }}
                    fallbackSrc="/800x100.jpg"
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SearchResults;
/* Mobile consistency Mon Aug  4 04:40:29 CST 2025 */
/* Mobile full width update Mon Aug  4 04:42:44 CST 2025 */
