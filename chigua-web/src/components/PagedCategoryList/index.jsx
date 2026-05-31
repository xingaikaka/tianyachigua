import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import categoryService from '../../services/categoryService';
import videoService from '../../services/videoService';
import advertisementService from '../../services/advertisementService';
import usePagedAds from '../../hooks/usePagedAds';
import PagedVideoModal from './PagedVideoModal';
import PagedSingleAd from './PagedSingleAd';
import Pagination from '../ui/Pagination';
import VideoCard from './VideoCard';
import usePagedContent from '../../hooks/usePagedContent';
import HotTags from './HotTags';
import './index.css';

const PAGE_SIZE_SHORT = 50; // 短视频每页大小
const PAGE_SIZE_NORMAL = 20; // 普通视频每页大小

// 存储热点标签容器的React Root实例 - 已移除，使用React直接渲染
// const hotTagsRootMap = new Map();

/**
 * 分页模式分类列表组件
 * 专门用于 isPagination === 1 的分类
 */
const PagedCategoryList = ({ categoryId, category }) => {
  const isShort = category?.isShort === 1;
  const pageSize = isShort ? PAGE_SIZE_SHORT : PAGE_SIZE_NORMAL;

  const [items, setItems] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState({ open: false, index: 0 });

  // 搜索状态
  const [searchKeyword, setSearchKeyword] = useState('');
  const [hasInputText, setHasInputText] = useState(false);
  const searchInputRef = useRef(null);

  // 标签筛选状态
  const [selectedTagIds, setSelectedTagIds] = useState([]);

  // 热点标签状态
  const [hotTags, setHotTags] = useState([]);

  // 广告相关状态
  const {
    pagedAds,
    adDisplayMode,
    adInterval,
    reload: reloadPagedAds
  } = usePagedAds(categoryId, { autoLoad: false });

  // 用 ref 追踪广告状态，避免 handlePageSuccess 因广告数据变化而重新创建，
  // 防止触发初始化 useEffect 中对 loadPage 的依赖循环
  const pagedAdsRef = useRef(pagedAds);
  const adDisplayModeRef = useRef(adDisplayMode);
  const adIntervalRef = useRef(adInterval);
  useEffect(() => { pagedAdsRef.current = pagedAds; }, [pagedAds]);
  useEffect(() => { adDisplayModeRef.current = adDisplayMode; }, [adDisplayMode]);
  useEffect(() => { adIntervalRef.current = adInterval; }, [adInterval]);

  const [mixedItems, setMixedItems] = useState([]); // 混合内容（视频+广告）
  const [videoData, setVideoData] = useState([]); // 纯视频数据，用于VideoModal

  const containerRef = useRef(null);
  const listRef = useRef(null); // 列表容器引用，用于滚动定位
  const requestIdRef = useRef(0);

  // 计算总页数
  const totalPages = useMemo(() => {
    return totalCount > 0 ? Math.ceil(totalCount / pageSize) : 0;
  }, [totalCount, pageSize]);

  // 获取混合内容的唯一键
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
    if (item.type === 'ad') {
      return `ad-${idx}-${item.data?.id || idx}`;
    }
    return `unknown-${idx}`;
  }, []);

  // 获取视频列表数据
  const fetchVideoPage = useCallback(async (page, options = {}) => {
    const tagIds = options.tagIds !== undefined ? options.tagIds : selectedTagIds;
    const title = options.title !== undefined ? options.title : (searchKeyword.trim() || null);
    const { forceRefresh = false } = options;
    const reqId = ++requestIdRef.current;

    try {
      let response;

      if (isShort) {
        response = await categoryService.getShortVideos({
          categoryId,
          title,
          tagIds: tagIds.length > 0 ? tagIds : undefined,
          pageNum: page,
          pageSize,
          _forceRefresh: forceRefresh,
          _timestamp: forceRefresh ? Date.now() : undefined
        });
      } else {
        response = await videoService.getVideosByCategory(categoryId, page, pageSize, null, tagIds, null, [], title);
      }

      if (reqId !== requestIdRef.current) {
        return null;
      }

      if (response && response.code === 200) {
        return {
          items: response?.rows || [],
          total: Number(response?.total || 0)
        };
      }

      return { items: [], total: 0 };
    } catch (error) {
      if (reqId === requestIdRef.current) {
        throw error;
      }
      return null;
    }
  }, [categoryId, isShort, pageSize, selectedTagIds, searchKeyword]);

  // 处理分页成功
  const handlePageSuccess = useCallback(({ result, page }) => {
    if (!result) {
      return;
    }

    const videos = result.items || [];
    const total = result.total || 0;

    setTotalCount(total);
    setCurrentPage(page);

    // 提取热点标签（仅在第一页加载时）
    if (page === 1) {
      const tagMap = new Map(); // 用于去重

      videos.forEach(video => {
        // 优先使用 tagList 字段（包含isHot的对象数组）
        if (video.tagList && Array.isArray(video.tagList) && video.tagList.length > 0) {
          video.tagList.forEach(tag => {
            if (tag && (tag.isHot === 1 || tag.isHot === '1')) {
              const tagId = tag.id;
              const tagName = tag.name;
              if (tagId && !tagMap.has(tagId)) {
                tagMap.set(tagId, {
                  id: tagId,
                  name: tagName,
                  isHot: tag.isHot || 1
                });
              }
            }
          });
        } else {
          // 如果没有 tagList，尝试从 tags 字段提取（字符串数组，需要配合 tagIds）
          // 注意：tags 字段是字符串数组，不包含 isHot，所以这里无法判断热点
          // 但为了兼容性，我们仍然处理
          if (video.tags && Array.isArray(video.tags) && video.tags.length > 0) {
            // 如果 tags 是对象数组（包含 isHot）
            video.tags.forEach(tag => {
              if (tag && typeof tag === 'object' && (tag.isHot === 1 || tag.isHot === '1')) {
                const tagId = tag.id;
                const tagName = tag.name;
                if (tagId && !tagMap.has(tagId)) {
                  tagMap.set(tagId, {
                    id: tagId,
                    name: tagName,
                    isHot: tag.isHot || 1
                  });
                }
              }
            });
          }
        }
      });

      const hotTagsList = Array.from(tagMap.values());
      // 仅在有热点标签时更新，避免覆盖已有结果导致闪现后消失
      if (hotTagsList.length > 0) {
        setHotTags(hotTagsList);
      }
    }

    // 生成混合内容（视频+广告）- 使用 ref 读取最新值，避免 useCallback 依赖这些状态
    const currentAds = pagedAdsRef.current || [];
    const currentDisplayMode = adDisplayModeRef.current ?? 3;
    const currentInterval = adIntervalRef.current ?? 10; // 分页模式默认间隔为10

    // 使用分页模式专用的混合内容方法（单独显示广告）
    const mixedContent = advertisementService.createPagedMixedContent(
      videos,
      currentAds,
      currentDisplayMode,
      currentInterval,
      page // 传入当前页码用于广告轮换
    );

    setMixedItems(mixedContent);

    // 提取纯视频数据用于PagedVideoModal（包含tags字段）
    const pureVideoData = videos.map((v, idx) => {
      // ✅ 优先使用 tagList（包含完整标签信息：id, name, isHot）
      let tags = [];
      if (v.tagList && Array.isArray(v.tagList) && v.tagList.length > 0) {
        // 使用 tagList（对象数组）
        tags = v.tagList;
      } else if (v.tags && Array.isArray(v.tags) && v.tags.length > 0) {
        // 降级使用 tags（字符串数组）
          tags = v.tags;
      }

      const videoData = {
        ...v,
        firstVideoUrl: v.firstVideoUrl || v.videoUrl || v.url,
        tags: tags, // 可能是字符串数组或对象数组
        tagList: v.tagList || [], // 保留 tagList 字段
        tagIds: v.tagIds || [] // 保留 tagIds 字段
      };
      
      return videoData;
    });
    setVideoData(pureVideoData);

    setLoading(false);
  }, []);

  // 处理分页错误
  const handlePageError = useCallback(({ error }) => {
    setLoading(false);
    setItems([]);
    setMixedItems([]);
  }, []);

  // 使用分页Hook
  const { loadPage, loading: pageLoading } = usePagedContent({
    fetcher: fetchVideoPage,
    onSuccess: handlePageSuccess,
    onError: handlePageError
  });

  // 点击热点标签
  const handleHotTagClick = useCallback((tagIds) => {
    const tagIdArray = Array.isArray(tagIds) ? tagIds : [];
    setSelectedTagIds(tagIdArray);
    setSearchKeyword('');
    setCurrentPage(1);
    setLoading(true);
    loadPage(1, {
      showLoading: true,
      forceRefresh: true,
      tagIds: tagIdArray,
      title: null
    });
  }, [loadPage]);

  // 同步loading状态
  useEffect(() => {
    if (pageLoading) {
      setLoading(true);
    }
  }, [pageLoading]);

  // 执行搜索的函数
  const performSearch = useCallback((keyword) => {
    const trimmedKeyword = keyword.trim();
    setSearchKeyword(trimmedKeyword);
    setCurrentPage(1);
    setLoading(true);
    loadPage(1, {
      showLoading: true,
      forceRefresh: true,
      title: trimmedKeyword || null,
      tagIds: selectedTagIds
    });
  }, [loadPage, selectedTagIds]);

  // 重置搜索与筛选条件
  const handleResetFilters = useCallback(() => {
    setSearchKeyword('');
    setSelectedTagIds([]);
    setCurrentPage(1);
    setLoading(true);
    setHasInputText(false);
    window.scrollTo(0, 0);
    if (searchInputRef.current) {
      searchInputRef.current.value = '';
    }
    loadPage(1, {
      showLoading: true,
      forceRefresh: true,
      title: null,
      tagIds: []
    });
  }, [loadPage]);


  // 监听搜索框输入变化（移除自动搜索，只保留Enter键搜索） - 已移除DOM监听，改用React事件
  // useEffect(() => { ... })


  // 页面加载时直接调用标签查询 API，获取热点标签并显示
  useEffect(() => {
    if (!categoryId) return;

    const fetchHotTags = async () => {
      try {
        const resp = await categoryService.getCategoryTags(categoryId);
        if (resp && resp.code === 200 && Array.isArray(resp.data)) {
          const hot = resp.data.filter((tag) => tag && (tag.isHot === 1 || tag.isHot === '1'));
          setHotTags(hot);
        }
      } catch (e) {
        // 静默失败，不阻塞页面
      }
    };

    fetchHotTags();
  }, [categoryId]);

  // 渲染热点标签 - 已移除DOM Portal，改用直接渲染
  // useEffect(() => { ... })

  // 初始化加载 - 确保页面加载时立即执行查询
  useEffect(() => {
    if (!categoryId) {
      return;
    }

    // 立即设置loading状态
    setLoading(true);

    // 加载广告
    reloadPagedAds();

    // 直接调用 loadPage 执行第一页查询
    loadPage(1, { showLoading: true, forceRefresh: true })
      .catch(() => {
        setLoading(false);
      });
  }, [categoryId, reloadPagedAds, loadPage]); // 依赖 loadPage 以确保引用最新

  // 处理页码变化
  const handlePageChange = useCallback(async (page) => {
    if (page < 1 || page > totalPages || page === currentPage) {
      return;
    }

    // ✅ 立即跳转到页面顶部，不使用滚动动画
    window.scrollTo(0, 0);

    setLoading(true);
    await loadPage(page, { showLoading: true, forceRefresh: false });
  }, [currentPage, totalPages, loadPage]);

  // 处理视频卡片点击
  const handleVideoClick = useCallback((video) => {

    if (!video) {
      return;
    }

    // 检查视频URL字段（可能是firstVideoUrl或其他字段）
    const videoUrl = video.firstVideoUrl || video.videoUrl || video.url;
    if (!videoUrl) {
      return;
    }

    // 找到视频在纯视频数据中的索引
    const videoIndex = videoData.findIndex(v => {
      if (!v) return false;
      const vUrl = v.firstVideoUrl || v.videoUrl || v.url;
      return v.id === video.id && vUrl === videoUrl;
    });


    if (videoIndex !== -1) {
      setModal({ open: true, index: videoIndex });
    } else if (videoData.length > 0) {
      // 容错处理：如果找不到，打开第一个视频
      setModal({ open: true, index: 0 });
    } else {
    }
  }, [videoData]);

  // 提取视频URL数组用于VideoModal
  const videoUrls = useMemo(() => {
    const urls = videoData.map(v => v.firstVideoUrl || v.videoUrl || v.url).filter(Boolean);
    return urls;
  }, [videoData]);

  // 视频弹窗内点击标签 → 关闭弹窗并按该标签筛选
  const handleTagClickFromModal = useCallback((tag) => {
    const tagId = tag && typeof tag === 'object' ? tag.id : null;
    if (!tagId) return;
    const tagIdArray = [tagId];
    setSelectedTagIds(tagIdArray);
    setModal({ open: false, index: 0 });
    setCurrentPage(1);
    setLoading(true);
    window.scrollTo(0, 0);
    loadPage(1, {
      showLoading: true,
      forceRefresh: true,
      tagIds: tagIdArray,
      title: null
    });
  }, [loadPage]);

  // 初始加载状态
  if (loading && mixedItems.length === 0) {
    return (
      <div ref={containerRef} className="paged-category-list-loading">
        <div className="loading-spinner"></div>
        <div className="loading-text">内容加载中，请稍候...</div>
      </div>
    );
  }

  return (
    <div ref={containerRef} className="paged-category-list">
      {/* 搜索头部区域 (仅分页模式显示) */}
      <div
        className="flex justify-center category-title-section mt-3 md:mt-12"
        style={{
          backgroundColor: 'rgba(49, 48, 48, 0.9)',
          minHeight: '180px', // 增加高度以容纳搜索框和标签
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          zIndex: 10,
          padding: '20px 0'
        }}
      >
        <div className="w-full text-center" style={{ maxWidth: '770px', padding: '0 16px' }}>
          {/* 分类名称 */}
          <h1
            className="font-normal"
            style={{
              fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
              fontWeight: '300',
              color: 'rgb(255, 255, 255)',
              fontSize: '32px',
              lineHeight: '48px',
              textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
              marginBottom: '16px'
            }}
          >
            {category?.name || 'Loading...'}
          </h1>

          {/* 搜索框区域 */}
          <div className="search-box-container" style={{ display: 'flex', justifyContent: 'center', marginBottom: '8px' }}>
            <div style={{ width: '100%', maxWidth: '700px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div style={{ position: 'relative', flex: '1 1 auto' }}>
              <input
                ref={searchInputRef}
                type="text"
                placeholder="搜索视频..."
                defaultValue={searchKeyword}
                onChange={(e) => setHasInputText(e.currentTarget.value.length > 0)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    performSearch(e.currentTarget.value);
                  }
                }}
                style={{
                  width: '100%',
                  height: '44px',
                  padding: '0 80px 0 20px',
                  borderRadius: '22px',
                  backgroundColor: 'rgba(0, 0, 0, 0.6)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  color: '#fff',
                  fontSize: '16px',
                  outline: 'none',
                  backdropFilter: 'blur(10px)',
                  boxSizing: 'border-box',
                }}
              />
              {/* 叉号清除按钮（有输入内容或有选中标签时显示） */}
              {(hasInputText || selectedTagIds.length > 0) && (
                <button
                  onClick={handleResetFilters}
                  style={{
                    position: 'absolute',
                    right: '46px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'rgba(255,255,255,0.5)',
                    cursor: 'pointer',
                    fontSize: 14,
                    padding: '0 6px',
                    lineHeight: 1,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >✕</button>
              )}
              {/* 搜索图标按钮 */}
              <button
                onClick={() => {
                  if (searchInputRef.current) {
                    performSearch(searchInputRef.current.value);
                  }
                }}
                style={{
                  position: 'absolute',
                  right: '10px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  width: '28px',
                  height: '28px',
                  borderRadius: '14px',
                  backgroundColor: 'transparent',
                  border: 'none',
                  color: '#fff',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transition: 'color 0.2s, background-color 0.15s, transform 0.12s',
                  outline: 'none',
                }}
                onMouseOver={(e) => e.currentTarget.style.color = '#e8e8e8'}
                onMouseOut={(e) => {
                  e.currentTarget.style.color = '#fff';
                  e.currentTarget.style.backgroundColor = 'transparent';
                  e.currentTarget.style.transform = 'translateY(-50%) scale(1)';
                }}
                onMouseDown={(e) => {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.15)';
                  e.currentTarget.style.transform = 'translateY(-50%) scale(0.92)';
                }}
                onMouseUp={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent';
                  e.currentTarget.style.transform = 'translateY(-50%) scale(1)';
                }}
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M21 21L15 15M17 10C17 13.866 13.866 17 10 17C6.13401 17 3 13.866 3 10C3 6.13401 6.13401 3 10 3C13.866 3 17 6.13401 17 10Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>
              </div>
            </div>
          </div>

          {/* 热点标签区域 */}
          {hotTags && hotTags.length > 0 && (
            <div className="hot-tags-wrapper" style={{ display: 'flex', justifyContent: 'center', marginTop: '6px' }}>
              <div style={{ width: '100%', maxWidth: '700px' }}>
                <HotTags
                  hotTags={hotTags}
                  onTagClick={handleHotTagClick}
                  selectedTagIds={selectedTagIds}
                />
              </div>
            </div>
          )}
        </div>
      </div>
      {/* 视频列表区域 */}
      <div className="paged-category-list-content">
        {mixedItems.length === 0 && !loading ? (
          <div className="empty-state">
            <div className="empty-text">暂无内容</div>
          </div>
        ) : (
          <div ref={listRef} className="masonry-grid">
            {mixedItems.map((item, idx) => (
              <div
                className={`masonry-item ${item.type === 'ad-grid' ? 'masonry-item-ad' : ''}`}
                key={getMixedItemKey(item, idx)}
              >
                {item.type === 'video' ? (
                  <VideoCard
                    video={item.data || item}
                    index={idx}
                    onClick={() => handleVideoClick(item.data || item)}
                  />
                ) : item.type === 'single-ad' ? (
                  <PagedSingleAd
                    ad={item.data}
                    onAdClick={() => { }}
                    style={{ marginBottom: '24px' }}
                  />
                ) : null}
              </div>
            ))}
          </div>
        )}

        {/* 加载中状态 */}
        {loading && mixedItems.length > 0 && (
          <div className="loading-more">
            <div className="loading-spinner-small"></div>
            <div className="loading-text-small">加载中...</div>
          </div>
        )}
      </div>

      {/* 分页按钮 */}
      {totalPages > 0 && (
        <div className="paged-category-list-pagination">
          <Pagination
            current={currentPage}
            total={totalCount}
            pageSize={pageSize}
            onChange={handlePageChange}
          />
        </div>
      )}

      {/* 视频模态框 - 使用分页模式专用播放组件 */}
      {modal.open && (
        <PagedVideoModal
          open={modal.open}
          videos={videoUrls}
          videoData={videoData}
          index={modal.index}
          onTagClick={handleTagClickFromModal}
          onClose={() => {
            setModal({ open: false, index: 0 });
          }}
        />
      )}
    </div>
  );
};

export default PagedCategoryList;

