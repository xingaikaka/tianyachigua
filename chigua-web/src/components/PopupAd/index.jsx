import React, { useState, useEffect, useRef } from 'react';
import advertisementService from '../../services/advertisementService';
import apiCacheService from '../../services/apiCacheService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

const PopupAd = () => {
  const [popupAds, setPopupAds] = useState([]);
  const [currentAd, setCurrentAd] = useState(null);
  const [isVisible, setIsVisible] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [imageKey, setImageKey] = useState(0);

  // 九宫格相关状态
  const [gridAds, setGridAds] = useState([]);
  const [showGrid, setShowGrid] = useState(false);

  /** 单图 / 九宫格：外层宽度、主内容高度（移动：减安全区，避免贴边/横向溢出） */
  const popupOuterW = isMobile
    ? 'min(72vw, calc(100vw - 32px - env(safe-area-inset-left, 0px) - env(safe-area-inset-right, 0px)))'
    : '440px';
  const popupPictureHeightDesktop = 480;
  /**
   * 移动：弹层顶内边距约为 15vh，主内容高度相应扣除顶底留白 + 关闭区高度，
   * 确保内容不超出屏幕且与底部保持呼吸感。
   */
  const popupPictureMaxHMobile =
    'min(50vh, calc(100vh - 25vh - 4rem - env(safe-area-inset-top, 0px) - env(safe-area-inset-bottom, 0px)))';
  const gridIconSize = isMobile ? 42 : 62;

  /**
   * 遮罩：items-start + paddingTop 控制弹层离顶距离，保证关闭按钮两种弹窗一致。
   * 移动端 15vh ≈ 视口 15%，视觉上居中偏上。
   */
  const popupOverlayStyle = {
    paddingTop: isMobile
      ? 'max(25vh, calc(env(safe-area-inset-top, 0px) + 2rem))'
      : '10vh',
    paddingRight: 'max(1rem, env(safe-area-inset-right, 0px))',
    paddingBottom: 'max(1rem, env(safe-area-inset-bottom, 0px))',
    paddingLeft: 'max(1rem, env(safe-area-inset-left, 0px))',
    overscrollBehavior: 'contain',
    overflow: 'hidden',
  };

  const popupPictureMobileScrollStyle = isMobile
    ? { WebkitOverflowScrolling: 'touch' }
    : {};

  // 预加载状态：图片加载完成前不弹出，避免出现空弹窗
  const [preloadPending, setPreloadPending] = useState(null);
  const preloadPendingRef = useRef(null);
  const safetyTimerRef = useRef(null);

  useEffect(() => {
    const shouldShowPopup = checkShouldShowPopup();
    if (shouldShowPopup) {
      fetchPopupAds();
    }
  }, []);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 768);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 组件卸载时清理安全兜底定时器
  useEffect(() => {
    return () => {
      if (safetyTimerRef.current) {
        clearTimeout(safetyTimerRef.current);
      }
    };
  }, []);

  // 弹窗打开时锁定 body 滚动，关闭时恢复
  useEffect(() => {
    if (isVisible) {
      const prev = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = prev;
      };
    }
  }, [isVisible]);

  const checkShouldShowPopup = () => {
    const currentPath = window.location.pathname;
    if (currentPath !== '/') {
      return false;
    }
    const navigationType = getNavigationType();
    switch (navigationType) {
      case 'reload':
        return true;
      case 'navigate':
        return false;
      case 'first-visit':
        return true;
      default:
        return true;
    }
  };

  const getNavigationType = () => {
    const hasVisited = sessionStorage.getItem('has_visited_site');
    if (!hasVisited) {
      sessionStorage.setItem('has_visited_site', 'true');
      return 'first-visit';
    }
    const performance = window.performance;
    if (performance && performance.navigation) {
      if (performance.navigation.type === 1) {
        return 'reload';
      }
    }
    if (performance && performance.getEntriesByType) {
      const navigationEntries = performance.getEntriesByType('navigation');
      if (navigationEntries.length > 0) {
        const navigationType = navigationEntries[0].type;
        if (navigationType === 'reload') {
          return 'reload';
        }
      }
    }
    const isRouteNavigation = sessionStorage.getItem('route_navigation');
    if (isRouteNavigation) {
      sessionStorage.removeItem('route_navigation');
      return 'navigate';
    }
    return 'first-visit';
  };

  // 图片预加载完成后真正弹出广告
  const doShowPopup = (pending) => {
    if (safetyTimerRef.current) {
      clearTimeout(safetyTimerRef.current);
      safetyTimerRef.current = null;
    }
    preloadPendingRef.current = null;
    setPreloadPending(null);

    if (pending.type === 'single') {
      setPopupAds(pending.ads);
      setGridAds(pending.pendingGridAds || []);
      setCurrentIndex(0);
      setCurrentAd(pending.ad);
      setImageKey(0);
      setIsVisible(true);
      recordAdImpression(pending.ad.id);
    } else if (pending.type === 'grid') {
      setGridAds(pending.gridAds);
      setIsVisible(true);
      setShowGrid(true);
      pending.gridAds.forEach(ad => recordAdImpression(ad.id));
    }
  };

  // 启动预加载，最多等待 6 秒后兜底弹出
  const startPreload = (pending) => {
    // 无图片的单广告直接弹出
    if (pending.type === 'single' && !pending.ad?.imageUrl) {
      doShowPopup(pending);
      return;
    }
    preloadPendingRef.current = pending;
    setPreloadPending(pending);
    safetyTimerRef.current = setTimeout(() => {
      const p = preloadPendingRef.current;
      if (p) doShowPopup(p);
    }, 6000);
  };

  const fetchPopupAds = async () => {
    try {
      // 并行加载单个弹窗广告（type=4）和九宫格广告（type=7）
      const [singleRes, gridRes] = await Promise.allSettled([
        apiCacheService.getApiData(
          'POPUP_ADS',
          {},
          () => advertisementService.getPopupAds(),
          { cacheDuration: 10 * 60 * 1000 }
        ),
        apiCacheService.getApiData(
          'GRID_POPUP_ADS',
          {},
          () => advertisementService.getGridPopupAds(),
          { cacheDuration: 10 * 60 * 1000 }
        )
      ]);

      let parsedGridAds = [];
      if (gridRes.status === 'fulfilled') {
        const gRes = gridRes.value;
        if (gRes && gRes.code === 200 && Array.isArray(gRes.data) && gRes.data.length > 0) {
          parsedGridAds = gRes.data;
        }
      }

      // 解析单个弹窗广告，预加载第一张图后再弹出
      if (singleRes.status === 'fulfilled') {
        const sRes = singleRes.value;
        if (sRes && sRes.code === 200 && Array.isArray(sRes.data) && sRes.data.length > 0) {
          startPreload({
            type: 'single',
            ad: sRes.data[0],
            ads: sRes.data,
            pendingGridAds: parsedGridAds
          });
          return;
        }
      }

      // 没有单个弹窗广告时，预加载九宫格 banner 后再弹出
      if (parsedGridAds.length > 0) {
        startPreload({ type: 'grid', gridAds: parsedGridAds });
      }
    } catch (error) {
    }
  };

  const recordAdImpression = async (adId) => {
    try {
      await advertisementService.recordAdImpression(adId);
    } catch (error) {
    }
  };

  // 处理关闭按钮点击
  const handleClose = () => {
    if (showGrid) {
      // 关闭九宫格，结束所有弹窗
      setIsVisible(false);
      setShowGrid(false);
      setCurrentAd(null);
      return;
    }

    // 单个广告：切换到下一个
    if (popupAds.length > 1 && currentIndex < popupAds.length - 1) {
      const nextIndex = currentIndex + 1;
      setCurrentIndex(nextIndex);
      setCurrentAd(popupAds[nextIndex]);
      setImageKey(prev => prev + 1);
      recordAdImpression(popupAds[nextIndex].id);
    } else {
      // 单个广告全部展示完毕，检查是否有九宫格广告
      if (gridAds.length > 0) {
        setCurrentAd(null);
        setShowGrid(true);
        // 记录九宫格所有广告的曝光
        gridAds.forEach(ad => recordAdImpression(ad.id));
      } else {
        setIsVisible(false);
        setCurrentAd(null);
      }
    }
  };

  const handleOverlayClick = () => {
    // 点击遮罩不关闭广告
  };

  const handleAdClick = async () => {
    if (!currentAd) return;
    try {
      await advertisementService.clickAd(currentAd.id);
      if (currentAd.linkUrl) {
        let url = currentAd.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    } catch (error) {
      if (currentAd.linkUrl) {
        let url = currentAd.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    }
  };

  const handleGridAdClick = async (ad) => {
    if (!ad) return;
    try {
      await advertisementService.clickAd(ad.id);
    } catch (error) {
    }
    if (ad.linkUrl) {
      let url = ad.linkUrl;
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        url = 'https://' + url;
      }
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  };

  if (!isVisible) {
    // 在后台静默预加载图片，图片就绪后才弹出，避免弹出时只见关闭按钮
    if (!preloadPending) return null;
    return (
      <div
        aria-hidden="true"
        style={{ position: 'fixed', left: '-9999px', top: '-9999px', width: 0, height: 0, overflow: 'hidden', pointerEvents: 'none', opacity: 0 }}
      >
        {preloadPending.type === 'single' && (
          <>
            {/* 第一张图加载完毕后触发弹出，其余图片同步预热进缓存 */}
            {preloadPending.ads.map((ad, i) => ad.imageUrl ? (
              <SecureDecryptedImage
                key={`preload-single-${ad.id}`}
                src={ad.imageUrl}
                onLoad={i === 0 ? () => doShowPopup(preloadPending) : undefined}
                onError={i === 0 ? () => doShowPopup(preloadPending) : undefined}
                priority="high"
                lazyLoad={false}
                showLoadingIndicator={false}
              />
            ) : null)}
            {/* 同时预热后续九宫格广告图片 */}
            {preloadPending.pendingGridAds?.map(ad => ad.imageUrl ? (
              <SecureDecryptedImage
                key={`preload-grid-${ad.id}`}
                src={ad.imageUrl}
                priority="high"
                lazyLoad={false}
                showLoadingIndicator={false}
              />
            ) : null)}
          </>
        )}
        {preloadPending.type === 'grid' && (
          <>
            {/* 等 banner 图加载完后弹出 */}
            <img
              src="/popup_header.png"
              onLoad={() => doShowPopup(preloadPending)}
              onError={() => doShowPopup(preloadPending)}
              alt=""
            />
            {/* 同时预热所有九宫格图标 */}
            {preloadPending.gridAds.map(ad => ad.imageUrl ? (
              <SecureDecryptedImage
                key={`preload-grid-${ad.id}`}
                src={ad.imageUrl}
                priority="high"
                lazyLoad={false}
                showLoadingIndicator={false}
              />
            ) : null)}
          </>
        )}
      </div>
    );
  }

  // 九宫格弹窗：外层尺寸、关闭区、主内容框与单个弹窗一致
  if (showGrid) {
    return (
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-start justify-center"
        style={popupOverlayStyle}
        onClick={handleOverlayClick}
      >
        <div
          className="popup-container flex flex-col w-full max-w-lg mx-auto box-border"
          style={{
            width: popupOuterW,
            maxWidth: popupOuterW
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="popup-close flex justify-center py-1.5 shrink-0">
            <button
              type="button"
              onClick={handleClose}
              className="transition-all duration-200 hover:opacity-80 hover:scale-110 touch-manipulation select-none"
            >
              <img
                src="/ads-close.png"
                alt="广告关闭"
                className={isMobile ? 'w-9 h-9' : 'w-7 h-7'}
                draggable={false}
              />
            </button>
          </div>

          <div
            className="popup-picture rounded-lg overflow-hidden shadow-2xl relative w-full"
            style={{
              height: isMobile ? 'auto' : `${popupPictureHeightDesktop}px`,
              maxHeight: isMobile ? popupPictureMaxHMobile : undefined,
              overflow: 'hidden',
              backgroundColor: '#1a1a1a',
            }}
          >
            <div style={{ overflow: 'hidden', lineHeight: 0 }}>
              <img
                src="/popup_header.png"
                alt="精选推荐"
                style={{ width: '100%', display: 'block', objectFit: 'cover' }}
              />
            </div>

            {/* 广告图标网格（4列） */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
                gap: isMobile ? '2px' : '4px',
                padding: isMobile ? '8px 6px 12px' : '12px 8px 16px'
              }}
            >
              {gridAds.map((ad) => (
                <div
                  key={ad.id}
                  onClick={() => handleGridAdClick(ad)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    cursor: ad.linkUrl ? 'pointer' : 'default',
                    padding: isMobile ? '4px 1px' : '6px 2px'
                  }}
                >
                  <div
                    style={{
                      width: `${gridIconSize}px`,
                      height: `${gridIconSize}px`,
                      borderRadius: isMobile ? '10px' : '14px',
                      overflow: 'hidden',
                      backgroundColor: '#2a2a2a',
                      marginBottom: isMobile ? '3px' : '5px',
                      flexShrink: 0,
                      position: 'relative'
                    }}
                  >
                    {ad.imageUrl ? (
                      <SecureDecryptedImage
                        src={ad.imageUrl}
                        alt={ad.title}
                        objectFit="cover"
                        style={{ width: '100%', height: '100%' }}
                        imageStyle={{ width: '100%', height: '100%' }}
                        priority="high"
                        lazyLoad={false}
                      />
                    ) : (
                      <div style={{ width: '100%', height: '100%', backgroundColor: '#333' }} />
                    )}
                  </div>
                  <span
                    style={{
                      color: '#ddd',
                      fontSize: isMobile ? '9px' : '11px',
                      textAlign: 'center',
                      lineHeight: '1.3',
                      width: `${gridIconSize}px`,
                      overflow: 'hidden',
                      display: '-webkit-box',
                      WebkitLineClamp: 1,
                      WebkitBoxOrient: 'vertical'
                    }}
                  >
                    {ad.title}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // 单个弹窗广告渲染
  if (!currentAd) {
    return null;
  }

  return (
    <>
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-start justify-center"
        style={popupOverlayStyle}
        onClick={handleOverlayClick}
      >
        <div
          className="popup-container flex flex-col w-full max-w-lg mx-auto box-border"
          style={{
            width: popupOuterW,
            maxWidth: popupOuterW
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* 关闭按钮区域 */}
          <div className="popup-close flex justify-center py-1.5 shrink-0">
            <button
              type="button"
              onClick={handleClose}
              className="transition-all duration-200 hover:opacity-80 hover:scale-110 touch-manipulation select-none"
            >
              <img
                src="/ads-close.png"
                alt="广告关闭"
                className={isMobile ? 'w-9 h-9' : 'w-7 h-7'}
                draggable={false}
              />
            </button>
          </div>

          {/* 广告图片区域（与九宫格同一外层 class / 移动端高度逻辑，避免第二次弹窗错位） */}
          <div
            className="popup-picture cursor-pointer rounded-lg overflow-hidden shadow-2xl relative w-full"
            style={{
              height: isMobile ? 'auto' : `${popupPictureHeightDesktop}px`,
              maxHeight: isMobile ? popupPictureMaxHMobile : undefined,
              overflow: 'hidden',
            }}
            onClick={handleAdClick}
          >
            {currentAd.imageUrl ? (
              <SecureDecryptedImage
                key={`popup-ad-${currentIndex}-${imageKey}`}
                src={currentAd.imageUrl}
                alt={currentAd.title}
                objectFit={isMobile ? 'contain' : 'cover'}
                imageClassName="w-full"
                imageStyle={{
                  display: 'block',
                  width: '100%',
                  height: isMobile ? 'auto' : '100%',
                  maxHeight: isMobile ? popupPictureMaxHMobile : 'none',
                  objectFit: isMobile ? 'contain' : 'cover',
                }}
                priority="high"
                lazyLoad={false}
              />
            ) : null}
            <div
              className="w-full h-full bg-gray-200 flex items-center justify-center"
              style={{ display: currentAd.imageUrl ? 'none' : 'flex' }}
            >
              <span className="text-gray-500">暂无图片</span>
            </div>

            {/* 多广告指示器 */}
            {popupAds.length > 1 && (
              <div className="absolute bottom-3 left-1/2 transform -translate-x-1/2 flex space-x-2">
                {popupAds.map((_, index) => (
                  <div
                    key={index}
                    className={`w-2 h-2 rounded-full ${
                      index === currentIndex ? 'bg-white shadow-lg' : 'bg-white bg-opacity-60'
                    }`}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default PopupAd;
