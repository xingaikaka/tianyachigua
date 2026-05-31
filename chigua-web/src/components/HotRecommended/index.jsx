import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import videoService from '../../services/videoService';
import apiCacheService from '../../services/apiCacheService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

const HotRecommended = () => {
  const navigate = useNavigate();
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [videosPerPage, setVideosPerPage] = useState(window.innerWidth >= 640 ? 2 : 1);
  const scrollContainerRef = useRef(null);
  const sliderBodyRef = useRef(null);
  const firstCardRef = useRef(null);
  const firstCardCoverRef = useRef(null);
  const isProgrammaticScrollRef = useRef(false);
  const scrollRafRef = useRef(null);
  const arrowUpdateRafRef = useRef(null);
  const coverResizeObserverRef = useRef(null);
  const [arrowOffsetTop, setArrowOffsetTop] = useState(null);
  const [firstCardCoverEl, setFirstCardCoverEl] = useState(null);
  // 移除过渡状态，简化逻辑

  useEffect(() => {
    fetchHotRecommendedVideos();
    
    // 监听窗口大小变化
    const handleResize = () => {
      const newVideosPerPage = window.innerWidth >= 640 ? 2 : 1;
      setVideosPerPage(prev => {
        if (prev === newVideosPerPage) return prev;
        return newVideosPerPage;
      });
      setCurrentIndex(prev => {
        const maxIndex = Math.max(0, videos.length - (newVideosPerPage === 1 ? 1 : newVideosPerPage));
        return Math.min(prev, maxIndex);
      });
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [videos.length]);

  const fetchHotRecommendedVideos = async () => {
    try {
      setLoading(true);
      // 使用缓存服务获取热门推荐视频
      const response = await apiCacheService.getApiData(
        'HOT_RECOMMENDED_VIDEOS',
        { pageNum: 1, pageSize: 20 },
        (params) => videoService.getHotRecommendedVideos(params.pageNum, params.pageSize),
        { cacheDuration: 3 * 60 * 1000 } // 3分钟缓存
      );
      if (response.code === 200) {
        setVideos(response.rows || []);
      }
    } catch (error) {
      
    } finally {
      setLoading(false);
    }
  };

  const handleVideoClick = (videoId) => {
    navigate(`/video/${videoId}`);
  };

  const isMobileView = videosPerPage === 1;

  const handlePrevious = () => {
    const step = isMobileView ? 1 : videosPerPage;
    isProgrammaticScrollRef.current = true;
    setCurrentIndex(prev => Math.max(0, prev - step));
  };

  const handleNext = () => {
    const step = isMobileView ? 1 : videosPerPage;
    const maxIndex = Math.max(0, videos.length - step);
    isProgrammaticScrollRef.current = true;
    setCurrentIndex(prev => Math.min(maxIndex, prev + step));
  };

  const handleDotClick = (index) => {
    isProgrammaticScrollRef.current = true;
    if (isMobileView) {
      const clamped = Math.min(index, Math.max(0, videos.length - 1));
      setCurrentIndex(clamped);
    } else {
      setCurrentIndex(index * videosPerPage);
    }
  };

  useEffect(() => {
    const maxIndex = Math.max(0, videos.length - (isMobileView ? 1 : videosPerPage));
    if (currentIndex > maxIndex) {
      setCurrentIndex(maxIndex);
      return;
    }

    if (isMobileView && scrollContainerRef.current && isProgrammaticScrollRef.current) {
      const container = scrollContainerRef.current;
      const target = container.children[currentIndex];
      if (target && typeof target.scrollIntoView === 'function') {
        target.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
      }
      setTimeout(() => {
        isProgrammaticScrollRef.current = false;
      }, 220);
    } else if (!isMobileView) {
      isProgrammaticScrollRef.current = false;
    }
  }, [currentIndex, videos.length, videosPerPage, isMobileView]);

  useEffect(() => {
    if (!isMobileView) return;
    const container = scrollContainerRef.current;
    if (!container) return;

    const handleScroll = () => {
      if (isProgrammaticScrollRef.current) return;
      if (scrollRafRef.current) cancelAnimationFrame(scrollRafRef.current);
      scrollRafRef.current = requestAnimationFrame(() => {
        const children = container.children;
        if (!children || children.length === 0) return;
        const containerRect = container.getBoundingClientRect();
        const containerCenter = containerRect.left + containerRect.width / 2;
        let nearestIndex = 0;
        let minDistance = Infinity;
        Array.from(children).forEach((child, idx) => {
          const rect = child.getBoundingClientRect();
          const childCenter = rect.left + rect.width / 2;
          const distance = Math.abs(childCenter - containerCenter);
          if (distance < minDistance) {
            minDistance = distance;
            nearestIndex = idx;
          }
        });
        setCurrentIndex(prev => (prev === nearestIndex ? prev : nearestIndex));
      });
    };

    container.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      container.removeEventListener('scroll', handleScroll);
      if (scrollRafRef.current) cancelAnimationFrame(scrollRafRef.current);
    };
  }, [isMobileView, videos.length]);

  const visibleVideos = videos.slice(currentIndex, currentIndex + videosPerPage);
  const itemsToRender = isMobileView ? videos : visibleVideos;
  const totalPages = isMobileView ? videos.length : Math.ceil(videos.length / videosPerPage);
  const activePage = isMobileView ? currentIndex : Math.floor(currentIndex / videosPerPage);

  const updateArrowPosition = useCallback(() => {
    const bodyEl = sliderBodyRef.current;
    const cardEl = firstCardRef.current;
    const coverEl = firstCardCoverRef.current;
    if (!bodyEl || !cardEl) return;

    const bodyRect = bodyEl.getBoundingClientRect();
    if (!bodyRect || Number.isNaN(bodyRect.top)) return;

    const targetRect = (coverEl || cardEl).getBoundingClientRect();

    if (!targetRect || targetRect.height === 0) {
      if (bodyRect.height > 0) {
        setArrowOffsetTop(bodyRect.height / 2);
      }
      return;
    }

    setArrowOffsetTop(targetRect.top - bodyRect.top + targetRect.height / 2);
  }, []);

  const scheduleArrowPositionUpdate = useCallback(() => {
    if (arrowUpdateRafRef.current) {
      cancelAnimationFrame(arrowUpdateRafRef.current);
    }
    arrowUpdateRafRef.current = requestAnimationFrame(() => {
      updateArrowPosition();
    });
  }, [updateArrowPosition]);

  const handleCardImageLoad = useCallback(() => {
    scheduleArrowPositionUpdate();
  }, [scheduleArrowPositionUpdate]);

  const handleFirstCardCoverRef = useCallback((node) => {
    firstCardCoverRef.current = node;
    setFirstCardCoverEl(node || null);
    if (node) {
      scheduleArrowPositionUpdate();
    }
  }, [scheduleArrowPositionUpdate]);

  const handleFirstCardRef = useCallback((node) => {
    firstCardRef.current = node;
    if (node) {
      scheduleArrowPositionUpdate();
    }
  }, [scheduleArrowPositionUpdate]);

  useEffect(() => {
    scheduleArrowPositionUpdate();
  }, [scheduleArrowPositionUpdate, videosPerPage, isMobileView, videos.length, currentIndex]);

  useEffect(() => {
    const resizeHandler = () => {
      scheduleArrowPositionUpdate();
    };
    window.addEventListener('resize', resizeHandler);
    return () => {
      window.removeEventListener('resize', resizeHandler);
    };
  }, [scheduleArrowPositionUpdate]);

  useEffect(() => () => {
    if (arrowUpdateRafRef.current) {
      cancelAnimationFrame(arrowUpdateRafRef.current);
    }
  }, []);

  useEffect(() => {
    if (loading) {
      firstCardCoverRef.current = null;
      scheduleArrowPositionUpdate();
    }
  }, [loading, scheduleArrowPositionUpdate]);

  useEffect(() => {
    if (coverResizeObserverRef.current) {
      coverResizeObserverRef.current.disconnect();
      coverResizeObserverRef.current = null;
    }

    if (!firstCardCoverEl || typeof ResizeObserver === 'undefined') {
      return undefined;
    }

    const observer = new ResizeObserver(() => {
      scheduleArrowPositionUpdate();
    });
    observer.observe(firstCardCoverEl);
    coverResizeObserverRef.current = observer;

    return () => {
      observer.disconnect();
      coverResizeObserverRef.current = null;
    };
  }, [firstCardCoverEl, scheduleArrowPositionUpdate]);

  // 数据加载完成但没有可展示的推荐时不渲染组件
  if (!loading && videos.length === 0) {
    return null;
  }

  return (
    <div
      className="rounded-lg p-3 sm:p-6 mb-8"
      style={{ 
        maxWidth: '770px', // 最大宽度
        width: '100%', // 响应式宽度
        minHeight: '300px', // 最小高度，移动端自适应
        height: 'auto', // 自适应高度
        margin: '0 auto', 
        backgroundColor: '#343232'
      }}
    >
      <div className="mb-4 sm:mb-6">
        <h2 className="text-xl sm:text-2xl font-bold px-2 sm:px-0" style={{ color: '#BCBCBC' }}>热门推荐</h2>
      </div>

      <div className="relative h-full">
        {/* 左右导航按钮 - 只在有数据且不是加载状态时显示 */}
        {!loading && currentIndex > 0 && (
          <button
            onClick={handlePrevious}
            className={`absolute left-0 z-20 text-white flex items-center justify-center transition-opacity duration-200 ${
              isMobileView
                ? '-translate-y-1/2 -translate-x-3 bg-black/50 rounded-full w-9 h-9'
                : '-translate-y-1/2 -translate-x-4 bg-black hover:bg-gray-800 rounded-full w-12 h-12'
            }`}
            style={arrowOffsetTop != null ? { top: arrowOffsetTop } : { top: '50%' }}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              className={isMobileView ? 'w-5 h-5' : 'w-6 h-6'}
            >
              <path
                d="M15.75 19.5L8.25 12l7.5-7.5"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        )}

        {!loading && currentIndex + videosPerPage < videos.length && (
          <button
            onClick={handleNext}
            className={`absolute right-0 z-20 text-white flex items-center justify-center transition-opacity duration-200 ${
              isMobileView
                ? '-translate-y-1/2 translate-x-3 bg-black/50 rounded-full w-9 h-9'
                : '-translate-y-1/2 translate-x-4 bg-black hover:bg-gray-800 rounded-full w-12 h-12'
            }`}
            style={arrowOffsetTop != null ? { top: arrowOffsetTop } : { top: '50%' }}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              className={isMobileView ? 'w-5 h-5' : 'w-6 h-6'}
            >
              <path
                d="M8.25 4.5L15.75 12l-7.5 7.5"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        )}

        {/* 视频网格 - 移动端优化 */}
        <style>{`
          .hot-recommend-scroll { -webkit-overflow-scrolling: touch; scrollbar-width: none; }
          .hot-recommend-scroll::-webkit-scrollbar { display: none; }
        `}</style>
        <div
          ref={(el) => {
            scrollContainerRef.current = el;
            sliderBodyRef.current = el;
          }}
          className={
            isMobileView
              ? 'hot-recommend-scroll flex gap-3 px-2 overflow-x-auto snap-x snap-mandatory scroll-smooth'
              : 'grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 px-2 sm:px-0'
          }
          style={isMobileView ? { WebkitOverflowScrolling: 'touch', scrollbarWidth: 'none' } : undefined}
        >
          {loading ? (
            // 加载状态：图片和标题骨架屏
            Array.from({ length: isMobileView ? 1 : videosPerPage }).map((_, i) => (
              <div key={i} className="animate-pulse">
                {/* 视频封面骨架 */}
                <div className="relative aspect-video rounded-lg overflow-hidden mb-3 bg-gray-700"></div>
                
                {/* 标题骨架 */}
                <div className="px-1">
                  <div className="bg-gray-700 h-4 rounded mb-2"></div>
                  <div className="bg-gray-700 h-4 rounded w-3/4"></div>
                </div>
              </div>
            ))
          ) : (
            // 实际内容
            itemsToRender.map((video, index) => {
              const realIndex = isMobileView ? index : currentIndex + index;
              return (
                <div
                  key={`${video.id}-${realIndex}`}
                  className={`cursor-pointer ${isMobileView ? 'snap-center' : ''}`}
                  onClick={() => handleVideoClick(video.id)}
                  style={isMobileView ? { flex: '0 0 100%' } : undefined}
                  ref={index === 0 ? handleFirstCardRef : null}
                >
                  <div
                    className="relative aspect-video rounded-lg overflow-hidden mb-3"
                    data-hot-recommend-cover
                    ref={index === 0 ? handleFirstCardCoverRef : null}
                  >
                    <SecureDecryptedImage
                      src={video.coverImageUrl}
                      alt={video.title}
                      className="w-full h-full object-cover"
                      lazyLoad={true}
                      onLoad={handleCardImageLoad}
                      style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                        display: 'block',
                        borderRadius: 'inherit'
                      }}
                      fallbackSrc="/loding.jpg"
                    />
                  </div>

                  <h3 className="text-white text-sm sm:text-base font-medium px-1" style={{
                    textShadow: '1px 1px 2px rgba(0,0,0,0.5)',
                    lineHeight: 1.4,
                    wordBreak: 'normal',
                    overflowWrap: 'break-word',
                    hyphens: 'auto'
                  }}>
                    {video.title}
                  </h3>
                </div>
              );
            })
          )}
        </div>

        {/* 进度指示器 */}
        <div className="flex justify-center mt-4 space-x-2">
          {loading ? (
            // 加载状态的进度指示器骨架
            [1, 2, 3].map(i => (
              <div
                key={i}
                className={`w-2 h-2 rounded-full ${i === 2 ? 'bg-blue-500' : 'bg-gray-600'}`}
              ></div>
            ))
          ) : totalPages > 1 ? (
            // 实际的进度指示器
            Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                onClick={() => handleDotClick(i)}
                className={`w-2 h-2 rounded-full transition-colors duration-200 cursor-pointer ${
                  activePage === i
                    ? 'bg-blue-500'
                    : 'bg-gray-600 hover:bg-gray-500'
                }`}
              />
            ))
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default HotRecommended; 
