import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import categoryService from '../../services/categoryService';
import advertisementService from '../../services/advertisementService';
import useShortVideoAds from '../../hooks/useShortVideoAds';
import VideoModal from './VideoModal';
import ShortVideoMobilePlayer from './MobileVideoFeed';
import ShortVideoAdGrid from './ShortVideoAd';
import useIsDesktop from '../../hooks/useIsDesktop';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import Pagination from '../ui/Pagination';

const PAGE_SIZE = 50;

const MasonryGrid = ({ categoryId, tagId }) => {
  const isDesktop = useIsDesktop();
  const [items, setItems] = useState([]);
  const [pageNum, setPageNum] = useState(1);
  const [loading, setLoading] = useState(false);
  const [modal, setModal] = useState({ open: false, index: 0 });
  const [selectedTag, setSelectedTag] = useState(tagId || null);
  const [totalCount, setTotalCount] = useState(0);
  const requestIdRef = useRef(0); // 用于丢弃过期请求
  const isApplyingFromHistoryRef = useRef(false); // 区分用户点击 vs 历史回退
  const containerRef = useRef(null);
  
  // 广告相关状态
  const {
    shortVideoAds,
    adDisplayMode,
    adInterval,
    reload: reloadShortVideoAds
  } = useShortVideoAds(categoryId, { autoLoad: false });
  const [mixedItems, setMixedItems] = useState([]); // 混合内容（视频+广告）
  const [videoData, setVideoData] = useState([]); // 纯视频数据，用于VideoModal

  const totalPages = useMemo(() => (totalCount > 0 ? Math.ceil(totalCount / PAGE_SIZE) : 0), [totalCount]);

  const getMixedItemKey = useCallback((item, idx) => {
    if (!item) {
      return `mixed-null-${idx}`;
    }

    if (item.type === 'video') {
      const video = item.data || item;
      const idSegment = video?.id ? `id-${video.id}` : `idx-${idx}`;
      const urlSegment = video?.firstVideoUrl ? `url-${video.firstVideoUrl}` : '';
      return `video-${idSegment}-${urlSegment}-${idx}`;
    }

    if (item.type === 'ad-grid') {
      const ids = Array.isArray(item.data)
        ? item.data
            .filter(Boolean)
            .map((ad) => ad.id || 'p')
            .join('-')
        : 'placeholder';
      return `ad-grid-${ids}-${idx}`;
    }

    return `${item.type || 'unknown'}-${idx}`;
  }, []);

  const loadPage = async (page, adsState = null, forceLoad = false, replace = false, allowRetryOnEmpty = true) => {
    if (loading && !forceLoad && !replace) {
      return;
    }

    setLoading(true);
    try {
      const reqId = ++requestIdRef.current;
      
      const res = await categoryService.getShortVideos({
        categoryId,
        tagId: selectedTag,
        pageNum: page,
        pageSize: PAGE_SIZE,
        _forceRefresh: forceLoad,
        _timestamp: forceLoad ? Date.now() : undefined
      });


      if (reqId !== requestIdRef.current) {
        return; // 已有更新请求发起，丢弃旧响应
      }
      if (res && res.code === 200) {
        const rawRows = res?.rows;
        const rows = Array.isArray(rawRows) ? rawRows : [];
        const totalFromResponse = Number(res?.total || 0);

        const shouldRetryEmpty = !forceLoad && allowRetryOnEmpty && (!Array.isArray(rawRows) || rows.length === 0) && totalFromResponse === 0;
        if (shouldRetryEmpty) {
          setLoading(false);
          await loadPage(page, adsState, true, true, false);
          return;
        }

        const isEmptyAfterForce = forceLoad && !allowRetryOnEmpty && (!Array.isArray(rawRows) || rows.length === 0) && totalFromResponse === 0;
        if (isEmptyAfterForce) {
          const nextPage = page + 1;
          if (nextPage > page) {
            setLoading(false);
            setPageNum(nextPage);
            await loadPage(nextPage, adsState, true, true, false);
            return;
          }
        }

        // 使用传入的广告配置或 Hook 状态
        const adsContext = adsState || {
          shortVideoAds,
          adDisplayMode,
          adInterval
        };
        const currentAds = adsContext.shortVideoAds || [];
        const currentDisplayMode = adsContext.adDisplayMode ?? adDisplayMode;
        const currentInterval = adsContext.adInterval ?? adInterval;
        
        // 生成混合内容（视频+广告）
        const currentItemsCount = (replace || page === 1) ? 0 : mixedItems.length;
        const mixedContent = advertisementService.createShortVideoMixedContent(
          rows, 
          currentAds, 
          currentDisplayMode, 
          currentInterval, 
          currentItemsCount
        );
        
        setMixedItems(mixedContent);
        setItems(rows);
        setVideoData(rows.map((video, index) => ({ ...video, originalIndex: index })));

        const total = totalFromResponse;
        setTotalCount(total);
      }
    } finally {
      setLoading(false);
    }
  };

  // 分类切换时的完整初始化流程
  useEffect(() => {
    const initializeCategory = async () => {
      // 重置状态和请求ID
      setItems([]);
      setMixedItems([]);
      setVideoData([]);
      setPageNum(1);
      setLoading(true);
      setTotalCount(0);
      requestIdRef.current = 0; // 重置请求ID，确保新的分类请求不会被旧的ID影响
      
      try {
        // 清除可能的缓存，确保获取最新数据
        try {
          const { default: apiCacheService } = await import('../../services/apiCacheService');
          // 清除当前分类的所有缓存
          const cachePattern = `SHORT_VIDEOS_${categoryId || 'all'}`;
          apiCacheService.clearCache(cachePattern);
          // 额外清除可能的旧分类缓存
          apiCacheService.clearCache('SHORT_VIDEOS_');
          
          // 清除全局请求去重服务的缓存
          const { default: requestDedupeService } = await import('../../services/requestDedupeService');
          requestDedupeService.clearAll();
        } catch (cacheError) {
        }
        
        // 先加载分类配置和广告数据
        const adConfig = await reloadShortVideoAds({ forceRefresh: true });

        // 直接使用返回的广告配置加载视频数据，强制加载
        await loadPage(1, adConfig || undefined, true, true);
      } catch (error) {
        setLoading(false);
      }
    };
    
    initializeCategory();
  }, [categoryId, selectedTag]);

  // 封装：应用一个标签筛选，并按需写历史
  const applyTagFilter = (tag, pushHistory) => {
    if (pushHistory) {
      try {
        const prev = window.history.state && window.history.state.depth ? window.history.state.depth : 0;
        window.history.pushState({ scope: 'pcShort', categoryId, selectedTag: tag, depth: (prev || 0) + 1 }, '');
      } catch (_) {}
    }
    setSelectedTag(tag);
    setItems([]);
    setPageNum(1);
    setTotalCount(0);
    try { window.scrollTo({ top: 0, behavior: 'auto' }); } catch (_) {}
  };

  const handlePageChange = useCallback(async (page) => {
    if (loading || page === pageNum) return;
    setPageNum(page);
    await loadPage(page, null, true, true);
    try { window.scrollTo({ top: 0, behavior: 'auto' }); } catch (_) {}
  }, [loading, pageNum, loadPage]);

  // 初始化与分类切换时，建立初始历史状态
  useEffect(() => {
    try {
      window.history.replaceState({ scope: 'pcShort', categoryId, selectedTag: null, depth: 0 }, '');
    } catch (_) {}
    // 监听浏览器返回
    const onPop = (e) => {
      const st = e.state;
      if (!st || st.scope !== 'pcShort' || st.categoryId !== categoryId) return;
      isApplyingFromHistoryRef.current = true;
      applyTagFilter(st.selectedTag ?? null, false);
      isApplyingFromHistoryRef.current = false;
    };
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, [categoryId]);

  // 处理混合内容的视频数据映射
  // 🔧 优化：从混合内容中提取纯视频数据，确保索引映射准确
  const processedVideoData = useMemo(() => {
    const result = [];
    let videoIndex = 0;
    
    mixedItems.forEach((item, originalIndex) => {
      if (item.type === 'video' && (item.data?.firstVideoUrl || item.firstVideoUrl)) {
        const videoData = item.data || item;
        result.push({
          ...videoData,
          originalIndex,
          videoIndex: videoIndex++,
          // 🔧 添加唯一标识符用于精确匹配
          uniqueKey: `${videoData.id}-${videoData.firstVideoUrl}`
        });
      }
    });
    
    
    return result;
  }, [mixedItems, adDisplayMode, adInterval]);

  // 提取视频URL数组用于VideoModal
  const videoUrls = useMemo(() => {
    return processedVideoData.map(v => v.firstVideoUrl);
  }, [processedVideoData]);

  // 初始加载：只显示“加载中”，不渲染任何页面框架
  if (loading && mixedItems.length === 0) {
    return (
      <div ref={containerRef} style={{ minHeight: '70vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ textAlign: 'center' }}>
          <div
            style={{
              width: 52,
              height: 52,
              border: '4px solid rgba(255,255,255,0.35)',
              borderTopColor: '#fff',
              borderRadius: '50%',
              margin: '0 auto 16px',
              animation: 'pc-only-spin 1s linear infinite'
            }}
          />
          <div style={{ color: '#BCBCBC', fontSize: 16 }}>内容加载中，请稍候...</div>
          <style>{`@keyframes pc-only-spin{from{transform:rotate(0)}to{transform:rotate(360deg)}}`}</style>
        </div>
      </div>
    );
  }

  return (
    <>
    <div ref={containerRef} className="px-3 md:px-0">
      <style>{`
        /* 改用网格布局，确保从左到右、从上到下的顺序 */
        .masonry-wrap { 
          --cell: 260px; 
          --gap: 16px; 
          max-width: calc(6 * var(--cell) + 5 * var(--gap));
          padding-left: calc(var(--cell) + var(--gap));
          padding-right: calc(var(--cell) + var(--gap));
          margin: 0 auto;
        }
        .masonry { 
          display: grid;
          gap: 12px;
          grid-template-columns: 1fr;
          align-items: start; /* 重要：让项目顶部对齐，创造不规则效果 */
          justify-content: center; /* 居中网格，配合左右预留1列实现6列布局 */
        }
        @media (min-width: 768px) { 
          .masonry { 
            grid-template-columns: repeat(2, var(--cell));
            gap: 14px;
            justify-content: center;
          } 
        }
        @media (min-width: 1280px) { 
          .masonry { 
            grid-template-columns: repeat(4, var(--cell)); /* 中间固定4列，每列固定宽度 */
            gap: var(--gap);
            justify-content: center;
          } 
        }
        .masonry-item { 
          /* 让内容的自然高度差异创造瀑布流效果 */
          transition: transform 0.3s ease;
        }
        .masonry-item:hover {
          transform: scale(1.02);
        }
        /* 通过内容的自然高度差异（标签数量、标题长度）形成瀑布流效果 */
        .card-cover { width: 100%; display: block; border-bottom: 1px solid rgba(255,255,255,0.06); }
        /* PC端短视频封面自适应：保持完整画面，黑底信箱*/
        .card-cover-fit { 
          aspect-ratio: 9 / 16; 
          object-fit: cover; /* 与中间四列卡片一致占满卡片区域 */
          width: 100%;
          height: auto;
          background-color: #000; 
          display: block;
        }
        /* 骨架/加载动画 */
        @keyframes mg-skeleton { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
        .mg-skeleton {
          background: linear-gradient(90deg, #2f2f2f 25%, #3a3a3a 50%, #2f2f2f 75%);
          background-size: 200% 100%;
          animation: mg-skeleton 1.4s infinite;
          border-radius: 8px;
        }
      `}</style>

      <div className="masonry-wrap">
        <div className="masonry">
        {mixedItems.length === 0 && loading && (
          <div style={{ gridColumn: '1 / -1', padding: '48px 0' }}>
            <div className="mg-skeleton" style={{ width: '100%', maxWidth: '620px', margin: '0 auto', height: '12px', borderRadius: 6 }}></div>
            <div className="mg-skeleton" style={{ width: '100%', maxWidth: '620px', margin: '12px auto 24px', height: '12px', borderRadius: 6 }}></div>
            <div style={{ display: 'grid', gap: '14px', gridTemplateColumns: 'repeat(2, var(--cell))' }}>
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={`sk-${i}`} className="mg-skeleton" style={{ aspectRatio: '9/16' }}></div>
              ))}
            </div>
            <div style={{ textAlign: 'center', color: '#BCBCBC', marginTop: 16, fontSize: 16 }}>内容加载中，请稍候...</div>
          </div>
        )}
        {mixedItems.map((item, idx) => (
          <div className="masonry-item" key={getMixedItemKey(item, idx)}>
            {item.type === 'video' ? (
              // 视频卡片渲染
              <div
                className="bg-[#1e1e1e] rounded-md overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
                onClick={() => { 
                  const currentVideo = item.data || item;
                  if (currentVideo.firstVideoUrl) {
                    // 🔧 修复索引映射：通过唯一标识符精确匹配
                    const currentUniqueKey = `${currentVideo.id}-${currentVideo.firstVideoUrl}`;
                    const matchedVideo = processedVideoData.find(v => v.uniqueKey === currentUniqueKey);
                    const videoIndex = matchedVideo ? matchedVideo.videoIndex : -1;
                    
                    if (videoIndex !== -1) {
                      setModal({ open: true, index: videoIndex });
                    } else {
                      // 🔧 容错处理：尝试使用第一个可用视频
                      if (videoUrls.length > 0) {
                        setModal({ open: true, index: 0 });
                      }
                    }
                  } else {
                  }
                }}
              >
                {(item.data || item).coverImageUrl && (
                  <SecureDecryptedImage
                    src={(item.data || item).coverImageUrl}
                    alt={(item.data || item).title || '视频封面'}
                    className="card-cover card-cover-fit"
                    lazyLoad={false}
                    priority={idx < 6 ? 'high' : 'normal'}
                  />
                )}
                <div className="p-3">
                  {/* 标签在上，标题在下 */}
                  <div className="flex flex-wrap gap-1 mb-2">
                    {((item.data || item).tags || []).slice(0, 6).map((t, tagIdx) => (
                      <button
                        key={tagIdx}
                        className="text-xs text-white/80 bg-white/10 rounded px-2 py-0.5"
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          const tagIdFromItem = Array.isArray((item.data || item).tagIds) ? (item.data || item).tagIds[tagIdx] : null;
                          applyTagFilter(tagIdFromItem, true);
                        }}
                      >{t}</button>
                    ))}
                  </div>
                  <div className="text-white text-sm line-clamp-2" style={{ minHeight: '34px' }}>{(item.data || item).title}</div>
                </div>
              </div>
            ) : (
              // 广告卡片渲染（九宫格）
              <ShortVideoAdGrid
                ads={item.data}
                onAdClick={() => {}}
                variant="bare"
              />
            )}
          </div>
        ))}
        </div>
      </div>

      {loading && mixedItems.length > 0 && (
        <div className="flex justify-center py-6">
          <div style={{ textAlign: 'center' }}>
            <div
              style={{
                width: 40,
                height: 40,
                border: '3px solid rgba(148, 163, 255, 0.35)',
                borderTopColor: '#60A5FA',
                borderRadius: '50%',
                margin: '0 auto 12px',
                animation: 'pc-inline-spin 0.9s linear infinite'
              }}
            />
            <div style={{ color: '#C5D0FF', fontSize: 14, letterSpacing: '0.02em' }}>加载中...</div>
            <style>{`@keyframes pc-inline-spin{from{transform:rotate(0)}to{transform:rotate(360deg)}}`}</style>
          </div>
        </div>
      )}

      {/* 标准分页 */}
      {!loading && totalCount > 0 && (
        <div className="py-8">
          <Pagination
            current={pageNum}
            total={totalCount}
            pageSize={PAGE_SIZE}
            onChange={handlePageChange}
          />
        </div>
      )}

      {/* 桌面端：Lightbox 弹层播放器 */}
      {isDesktop && (
        <VideoModal
          open={modal.open}
          videos={videoUrls}
          videoData={videoData}
          index={modal.index}
          onClose={() => setModal({ open: false, index: 0 })}
        />
      )}

      {/* 移动端：全屏 TikTok 式播放器 */}
      {!isDesktop && modal.open && (
        <ShortVideoMobilePlayer
          videos={processedVideoData}
          initialIndex={modal.index}
          onClose={() => setModal({ open: false, index: 0 })}
          hasMore={false}
          isLoadingMore={false}
        />
      )}
    </div>
    </>
  );
};

export default MasonryGrid;
