import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useNavigate, useNavigationType } from 'react-router-dom';

import tagDetailService from '../../services/tagDetailService';
import advertisementService from '../../services/advertisementService';
import apiCacheService from '../../services/apiCacheService';
import useListStateManager from '../../hooks/useListStateManager';
import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';
import Pagination from '../../components/ui/Pagination';
import Footer from '../../components/common/Footer';
import MixedContentList from '../../components/common/MixedContentList';
import usePagedContent from '../../hooks/usePagedContent';
import useListAds from '../../hooks/useListAds';
import { isVideoUrlCacheExpired } from '../../constants/cacheConfig';

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
          
          {/* 加载中文字 */}
          <div className="text-xl md:text-2xl font-normal text-white">
            加载中...
          </div>
        </div>
      </div>
    </div>
  );
};

// 标签视频卡片组件 (与SearchVideoCard完全一致)
const TagVideoCard = ({ video, index, navigate, onClick }) => {
  const [isDesktop, setIsDesktop] = React.useState(window.innerWidth >= 768);

  React.useEffect(() => {
    const handleResize = () => {
      setIsDesktop(window.innerWidth >= 768);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);
  // 处理视频点击
  const handleVideoClick = () => {
    if (onClick && video.id) {
      onClick(video.id);
    } else if (navigate && video.id) {
      navigate(`/video/${video.id}`);
    }
  };

  // 视频卡片布局 - 完全复制VideoList的样式
  return (
    <div 
      className="group cursor-pointer transform transition-all duration-300 hover:scale-[1.03] hover:shadow-2xl w-full max-w-none md:max-w-[860px]"
      onClick={handleVideoClick}
    >
      <div className="relative aspect-video md:h-[280px] md:aspect-auto rounded-lg overflow-hidden shadow-lg transition-all duration-300 group-hover:shadow-xl">
        <SecureDecryptedImage
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
        <div className="absolute inset-0 flex flex-col justify-center items-center text-white text-center" style={{ 
          minHeight: '280px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          paddingLeft: '24px',
          paddingRight: '24px'
        }}>
          {/* 标题 */}
                        <h2 className="mb-2 video-list-title" style={{ 
                fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                fontSize: '28px',
                lineHeight: '32px',
                fontWeight: '400',
                fontStyle: 'normal',
                color: 'rgb(255, 255, 255)',
                textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
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
            paddingLeft: '24px',
            paddingRight: '24px',
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
        <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-opacity duration-300" style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '280px'
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

// 标签广告横幅组件
const TagAdBanner = ({ ads }) => {
  if (!ads || ads.length === 0) return null;

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
            {ads.map((ad, index) => (
              <div 
                key={ad.id || index}
                className="cursor-pointer overflow-hidden"
                onClick={() => handleAdClick(ad)}
              >
                <div className="relative shadow-lg overflow-hidden">
                  <SecureDecryptedImage
                    src={ad.imageUrl}
                    alt="标签横幅广告"
                    className="w-full h-auto object-contain md:h-[90px] md:object-cover md:max-h-[90px]"
                    style={{ width: '100%' }}
                    fallbackSrc="/800x100.jpg"
                    lazyLoad={false}
                    priority="normal"
                    onError={(e) => {
                      if (!e.target.src.endsWith('/800x100.jpg')) {
                        e.target.src = '/800x100.jpg';
                      }
                    }}
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

const TagDetail = () => {
  const { tagId } = useParams();
  const navigate = useNavigate();
  const navigationType = useNavigationType();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tagInfo, setTagInfo] = useState(null);
  const [videos, setVideos] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [randomBackgroundImage, setRandomBackgroundImage] = useState('');
  const pageSize = 20; // 每页显示20个视频
  const pageKey = `tag_${tagId || 'empty'}`;
  const skipInitialFetchRef = useRef(false);
  const [restored, setRestored] = useState(false);
  const [isHydrated, setIsHydrated] = useState(false);
  const pendingScrollMetaRef = useRef(null);
  const lastBackgroundTagRef = useRef(null);
  const initialFetchTriggeredRef = useRef(false);
  const { bottomAds, refreshBottomAds, primeAdsState } = useListAds({
    enableTopAds: false,
    enableConfig: false,
    bottomPosition: '7'
  });
  const { restoreListState, autoSaveState, saveListState, captureAnchorSnapshot, restoreAnchorPosition } = useListStateManager(pageKey, { autoSave: false });

  useEffect(() => {
    lastBackgroundTagRef.current = null;
    initialFetchTriggeredRef.current = false;
    setRandomBackgroundImage('');
  }, [tagId]);

  // 从视频结果中获取随机背景图片（单次随机，避免重复闪烁）
  const getRandomBackgroundFromResults = useCallback((videoList) => {
    if (!Array.isArray(videoList) || videoList.length === 0) {
      return;
    }

    const isNewTag = lastBackgroundTagRef.current !== tagId;
    const hasExistingBackground = Boolean(randomBackgroundImage);

    if (!isNewTag && hasExistingBackground) {
      return;
    }

    const videosWithImages = videoList.filter((video) => {
      const url = video?.coverImageUrl;
      return typeof url === 'string' && url.trim() !== '' && url.includes('http');
    });

    if (videosWithImages.length === 0) {
      lastBackgroundTagRef.current = tagId;
      setRandomBackgroundImage('');
      return;
    }

    const randomVideo = videosWithImages[Math.floor(Math.random() * videosWithImages.length)];
    if (randomVideo?.coverImageUrl) {
      lastBackgroundTagRef.current = tagId;
      setRandomBackgroundImage(randomVideo.coverImageUrl);
    }
  }, [randomBackgroundImage, tagId]);

  // 获取标签相关视频
  const fetchTagPage = useCallback(async (page = 1) => {
    if (!tagId) {
      return {
        items: [],
        total: 0,
        tagInfo: null
      };
    }

    const response = await apiCacheService.getApiData(
      'TAG_VIDEOS',
      { tagId, pageNum: page, pageSize },
      (params) => tagDetailService.getVideosByTag(params.tagId, params.pageNum, params.pageSize),
      {
        cacheDuration: 2 * 60 * 1000,
        skipCacheIf: (res) => {
          const ok = res && res.code === 200;
          const rows = res?.data?.list || [];
          return ok && Array.isArray(rows) && rows.length === 0;
        },
        emptyCacheDuration: 15 * 1000
      }
    );

    if (response.code === 200) {
      const videoData = response.data || {};
      return {
        items: videoData.list || [],
        total: videoData.total || 0,
        tagInfo: videoData.tagInfo || null
      };
    }

    const err = new Error(response.msg || '获取标签视频失败');
    err.name = 'TagVideosError';
    throw err;
  }, [tagId, pageSize]);

  const handleTagPageSuccess = useCallback(({ result, page }) => {
    setVideos(result.items || []);
    setTotalCount(result.total || 0);
    setCurrentPage(page);
    setTagInfo(result.tagInfo || null);
    setError('');
    if (page === 1) {
      getRandomBackgroundFromResults(result.items || []);
    }
    setLoading(false);
    if (pendingScrollMetaRef.current) {
      restoreAnchorPosition(pendingScrollMetaRef.current, { headerOffset: 120 });
      pendingScrollMetaRef.current = null;
    }
  }, [getRandomBackgroundFromResults, restoreAnchorPosition]);

  const handleTagPageError = useCallback(({ error }) => {
    setVideos([]);
    setTotalCount(0);
    setError(error?.message || '获取标签视频失败，请重试');
    setLoading(false);
    initialFetchTriggeredRef.current = false;
  }, []);

  const { loadPage: loadTagPage, loading: tagPageLoading } = usePagedContent({
    fetcher: fetchTagPage,
    onSuccess: handleTagPageSuccess,
    onError: handleTagPageError
  });

  useEffect(() => {
    if (tagPageLoading) {
      setLoading(true);
    }
  }, [tagPageLoading]);

  useEffect(() => {
    if (!pendingScrollMetaRef.current) return;
    if (videos.length === 0) return;
    restoreAnchorPosition(pendingScrollMetaRef.current, { headerOffset: 120 });
    pendingScrollMetaRef.current = null;
  }, [videos, restoreAnchorPosition]);

  const fetchTagVideos = useCallback(async (page = 1) => {
    if (!tagId) {
      setVideos([]);
      setTotalCount(0);
      setTagInfo(null);
      setLoading(false);
      return;
    }

    setError('');
    setLoading(true);
    try {
      await loadTagPage(page, { showLoading: true });
    } catch (_) {
      // 错误已在 onError 中处理
    }
  }, [tagId, loadTagPage]);

  useEffect(() => {
    if (videos.length === 0) return;
    autoSaveState({
      videos,
      totalCount,
      currentPage,
      tagInfo,
      bottomAds,
      randomBackgroundImage,
      tagId,
      scrollMeta: captureAnchorSnapshot({
        items: videos,
        getDomId: (item) => `tag-card-${item.id}`,
        headerOffset: 120
      })
    });
  }, [autoSaveState, videos, totalCount, currentPage, tagInfo, bottomAds, randomBackgroundImage, tagId, captureAnchorSnapshot]);

  useEffect(() => {
    const persistState = () => {
      if (videos.length === 0) return;
      const scrollMeta = captureAnchorSnapshot({
        items: videos,
        getDomId: (item) => `tag-card-${item.id}`,
        headerOffset: 120
      });
      const sessionState = {
        videos,
        totalCount,
        currentPage,
        tagInfo,
        bottomAds,
        randomBackgroundImage,
        tagId,
        scrollMeta,
        timestamp: Date.now()
      };
      sessionStorage.setItem(pageKey, JSON.stringify(sessionState));
      saveListState(sessionState);
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
  }, [videos, totalCount, currentPage, tagInfo, bottomAds, randomBackgroundImage, tagId, saveListState, captureAnchorSnapshot]);

  useEffect(() => {
    if (isHydrated) return;
    const restoredState = restoreListState();
    if (restoredState?.scrollMeta) {
      pendingScrollMetaRef.current = restoredState.scrollMeta;
    }
    if (restoredState && String(restoredState.tagId) === String(tagId)) {
      setVideos(restoredState.videos || []);
      setTotalCount(restoredState.totalCount || 0);
      setCurrentPage(restoredState.currentPage || 1);
      setTagInfo(restoredState.tagInfo || null);
      setRandomBackgroundImage(restoredState.randomBackgroundImage || '');
      lastBackgroundTagRef.current = tagId;
      primeAdsState({ bottom: restoredState.bottomAds });
      setLoading(false);
      setIsHydrated(true);
      return;
    }
    setIsHydrated(true);
  }, [restoreListState, tagId, isHydrated]);

  // 页面初始化与滚动恢复
  useEffect(() => {
    if (!tagId || !isHydrated) return;

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

    try {
      const cacheStr = sessionStorage.getItem(pageKey);
      if (cacheStr) {
        const cache = JSON.parse(cacheStr);
        if (cache && Array.isArray(cache.videos) && !isVideoUrlCacheExpired(cache)) {
          setVideos(cache.videos || []);
          setTotalCount(cache.totalCount || 0);
          setCurrentPage(cache.currentPage || 1);
          setTagInfo(cache.tagInfo || null);
          setLoading(false);
          pendingScrollMetaRef.current = cache.scrollMeta || cache;
          setRestored(true);
        } else if (cache && isVideoUrlCacheExpired(cache)) {
          sessionStorage.removeItem(pageKey);
        }
      }
    } catch (_) {}

    if (!skipInitialFetchRef.current && !restored && !initialFetchTriggeredRef.current) {
      if (navigationType !== 'POP') {
        window.scrollTo(0, 0);
      }
      initialFetchTriggeredRef.current = true;
      fetchTagVideos(1);
    }

    refreshBottomAds();

    return () => {
      try { window.history.scrollRestoration = prevScroll || 'auto'; } catch (_) {}
    };
  }, [tagId, navigationType, fetchTagVideos, refreshBottomAds, restored, isHydrated]);

  // 处理分页（显式动作置顶，不影响 POP 返回）
  const handlePageChange = useCallback(async (page) => {
    await fetchTagVideos(page);
    window.scrollTo(0, 0);
  }, [fetchTagVideos]);

  // 处理视频点击（保存定位与列表状态）
  const handleVideoClick = (videoId) => {
    try {
      const scrollMeta = captureAnchorSnapshot({
        items: videos,
        getDomId: (item) => `tag-card-${item.id}`,
        headerOffset: 120
      });
      const sessionState = {
        videos,
        totalCount,
        currentPage,
        tagInfo,
        bottomAds,
        randomBackgroundImage,
        tagId,
        scrollMeta,
        timestamp: Date.now()
      };
      sessionStorage.setItem(pageKey, JSON.stringify(sessionState));
      sessionStorage.setItem(pageKey + ':skip', '1');
      saveListState(sessionState);
    } catch (_) {}
    // 🔍 携带标签名称作为搜索关键词上下文参数
    const tagParam = tagInfo?.name ? `?searchKeyword=${encodeURIComponent(tagInfo.name)}` : '';
    navigate(`/video/${videoId}${tagParam}`, { state: { fromTag: true }, replace: false });
  };

  if (!tagId) {
    return (
      <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
        <div className="pt-28 text-center text-white">
          <h1>标签ID无效</h1>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      
      {/* 标签标题区域 - 带随机背景 */}
      {randomBackgroundImage ? (
        <div 
          className="relative w-full"
          style={{
            height: '250px',
            width: '100%',
            overflow: 'hidden'
          }}
        >
          <SecureDecryptedImage
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
                {loading ? '加载中...' : `${tagInfo?.name || '标签'} 相关视频`}
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
                {loading ? '加载中...' : `${tagInfo?.name || '标签'} 相关视频`}
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

      {/* 主要内容区域 */}
      <div className="pt-12 pb-8">
        
        {/* 加载状态 */}
        {loading && (
          <div className="flex justify-center">
            <div className="w-full" style={{ maxWidth: '860px' }}>
              <div className="space-y-12 px-4">
                {[1, 2, 3].map((index) => (
                  <div key={index} className="flex justify-center">
                    <LoadingVideoCard />
                  </div>
                ))}
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
                onClick={() => fetchTagVideos(1)}
                className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
              >
                重试
              </button>
            </div>
          </div>
        )}

        {/* 标签查询结果列表 */}
        {!loading && !error && (
          <>
            {videos.length > 0 ? (
              <>
                <div className="flex justify-center">
                  <div className="w-full" style={{ maxWidth: '860px' }}>
                    {/* 视频列表 */}
                    <MixedContentList
                      items={videos.map(video => ({ type: 'video', data: video }))}
                      containerClassName="space-y-12 px-4"
                      itemWrapperClassName="flex justify-center"
                      getItemKey={(item, index) => `tag-${item?.data?.id ?? item?.id ?? index}`}
                      getItemWrapperProps={(item) => ({ id: `tag-card-${item?.data?.id ?? item?.id}` })}
                      renderContentItem={(item, videoIdx) => (
                        <TagVideoCard
                          video={{ ...item.data, type: 'video' }}
                          index={videoIdx}
                          navigate={navigate}
                          onClick={handleVideoClick}
                        />
                      )}
                    />
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
                    该标签下暂无相关视频
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
      </div>

      {/* 底部横幅广告 */}
      {bottomAds.length > 0 && (
        <TagAdBanner ads={bottomAds} />
      )}

      {/* 底部区域 */}
      <Footer />
    </div>
  );
};

export default TagDetail;/* Mobile consistency Mon Aug  4 04:40:29 CST 2025 */
/* Mobile full width update Mon Aug  4 04:42:44 CST 2025 */
