import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import collectionService from '../../services/collectionService';
import advertisementService from '../../services/advertisementService';
import apiCacheService from '../../services/apiCacheService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import AdCard from '../AdCard';
import LogoAds from '../LogoAds';
import Footer from '../common/Footer';
import useIsDesktop from '../../hooks/useIsDesktop';
import useListStateManager from '../../hooks/useListStateManager';
import MixedContentList from '../common/MixedContentList';
import BottomBannerAds from '../common/BottomBannerAds';
import usePagedContent from '../../hooks/usePagedContent';
import useListAds from '../../hooks/useListAds';
import videoStatsService from '../../services/videoStatsService';

const CollectionList = ({ categoryId = null }) => {
  const navigate = useNavigate();
  const isDesktop = useIsDesktop();
  const pageKey = `collectionList_${categoryId || 'all'}`;
  const { restoreListState, autoSaveState, saveListState, captureAnchorSnapshot, restoreAnchorPosition } = useListStateManager(pageKey, { autoSave: false });
  const [collections, setCollections] = useState([]);
  const {
    topAds,
    bottomAds,
    refreshTopAds,
    refreshBottomAds,
    primeAdsState
  } = useListAds({
    categoryId,
    enableConfig: false,
    bottomPosition: '6',
    topPosition: '5'
  });
  const ads = topAds;
  const [loading, setLoading] = useState(true);
  const [isHydrated, setIsHydrated] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    total: 0,
    pageSize: 20
  });
  

  const fetchCollectionsPage = useCallback(async (page = 1) => {
    const response = await apiCacheService.getApiData(
      'COLLECTIONS_BY_CATEGORY',
      { categoryId, page, pageSize: pagination.pageSize },
      (params) => collectionService.getCollectionsByCategory(params.categoryId, params.page, params.pageSize),
      { cacheDuration: 3 * 60 * 1000 }
    );

    if (response.code === 200) {
      const collectionsData = response.data?.rows || response.data || [];
      return {
        items: collectionsData,
        total: response.data?.total || 0
      };
    }

    return { items: [], total: 0 };
  }, [categoryId, pagination.pageSize]);

  const handleCollectionsSuccess = useCallback(({ result, page }) => {
    setCollections(result.items || []);
    setPagination(prev => ({
      ...prev,
      current: page,
      total: result.total || 0
    }));
    setLoading(false);
  }, []);

  const handleCollectionsError = useCallback(() => {
    setCollections([]);
    setLoading(false);
  }, []);

  const { loadPage: loadCollectionsPage, loading: pageLoading } = usePagedContent({
    fetcher: fetchCollectionsPage,
    onSuccess: handleCollectionsSuccess,
    onError: handleCollectionsError
  });

  useEffect(() => {
    if (pageLoading) {
      setLoading(true);
    }
  }, [pageLoading]);

  // 页面变化处理
  const handlePageChange = async (page) => {
    setLoading(true);
    try {
      await loadCollectionsPage(page, { showLoading: true });
    } finally {
      window.scrollTo({ top: 0, behavior: 'auto' });
    }
  };

  const handleCollectionSelect = useCallback((collection) => {
    if (!collection || !collection.id) return;
    try {
      videoStatsService.trackCollectionView(collection.id, {
        categoryId,
        title: collection.title || collection.name || ''
      });
    } catch (_) {}
    saveListState({
      collections,
      ads,
      bottomAds,
      pagination,
      categoryId,
      isDesktop
    });
    const categoryParam = categoryId ? `?fromCategory=${categoryId}` : '';
    navigate(`/collection/${collection.id}${categoryParam}`);
  }, [ads, bottomAds, categoryId, collections, isDesktop, navigate, pagination, saveListState]);

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

  useEffect(() => {
    if (!isHydrated) return;
    if (collections.length === 0) return;
    autoSaveState({
      collections,
      ads,
      bottomAds,
      pagination,
      categoryId,
      isDesktop
    });
  }, [autoSaveState, collections, ads, bottomAds, pagination, categoryId, isDesktop, isHydrated]);

  useEffect(() => {
    const persistState = () => {
      if (collections.length === 0) return;
      saveListState({
        collections,
        ads,
        bottomAds,
        pagination,
        categoryId,
        isDesktop
      });
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
  }, [collections, ads, bottomAds, pagination, categoryId, isDesktop, saveListState]);

  useEffect(() => {
    if (isHydrated) return;
    const restored = restoreListState();
    if (restored && restored.categoryId === categoryId) {
      setCollections(restored.collections || []);
      primeAdsState({
        top: restored.ads,
        bottom: restored.bottomAds
      });
      setPagination(restored.pagination || { current: 1, total: 0, pageSize: 20 });
      setLoading(false);
      setIsHydrated(true);
      return;
    }
    setIsHydrated(true);
  }, [categoryId, restoreListState, isHydrated]);

  useEffect(() => {
    if (!isHydrated) return;
    if (collections.length > 0) return;
    setLoading(true);
    loadCollectionsPage(pagination.current, { showLoading: true }).catch(() => {});
    refreshTopAds();
    refreshBottomAds();
  }, [categoryId, collections.length, refreshTopAds, refreshBottomAds, isHydrated, loadCollectionsPage, pagination.current]);

  // 创建混合内容（合集 + 广告）
  const createMixedContent = () => {
    const mixedContent = [];
    let adIndex = 0;
    
    if (collections.length > 0) {
      collections.forEach((collection, index) => {
        // 添加合集
        mixedContent.push({
          ...collection,
          type: 'collection'
        });
        
        // 每个合集后添加一个广告（如果还有广告）
        if (adIndex < ads.length) {
          mixedContent.push({
            ...ads[adIndex],
            type: 'ad'
          });
          adIndex++;
        }
      });
    }
    
    return mixedContent;
  };

  const mixedContent = createMixedContent();



  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl">
      {/* 合集列表布局 - 每行一个项目，居中显示 */}
      <MixedContentList
        items={mixedContent}
        containerClassName="space-y-12"
        itemWrapperClassName="flex justify-center"
        getItemKey={(item, index) => `${item?.type || 'item'}-${item?.id ?? item?.data?.id ?? index}`}
        renderContentItem={(item, collectionIdx) => (
          <CollectionCard
            collection={item}
            index={collectionIdx}
            navigate={navigate}
            categoryId={categoryId}
            isDesktop={isDesktop}
            onSelect={handleCollectionSelect}
          />
        )}
        renderAdItem={(item, index) => <AdCard ad={item} index={index} />}
      />
      
      {/* 分页组件 */}
      <Pagination 
        current={pagination.current} 
        total={pagination.total} 
        pageSize={pagination.pageSize}
        onChange={handlePageChange}
      />
      
      {/* 底部横幅广告 - 参考视频列表页面样式 */}
      <BottomBannerAds
        ads={bottomAds}
        onAdClick={handleBottomAdClick}
        containerClassName="mb-6 md:mb-12"
        containerStyle={{ backgroundColor: '#2C2A2A' }}
      />

      {/* Logo广告区域 */}
      <div className="flex justify-center mb-12">
        <div className="w-full" style={{ maxWidth: '770px' }}>
          <LogoAds />
        </div>
      </div>

      {/* 底部区域 */}
      <Footer />
    </div>
  );
};

// 合集卡片组件
const CollectionCard = ({ collection, index, navigate, categoryId, isDesktop, onSelect }) => {
  // 处理合集点击
  const handleCollectionClick = () => {
    if (!collection || !collection.id) return;
    if (onSelect) {
      onSelect(collection);
      return;
    }
    if (navigate) {
      const categoryParam = categoryId ? `?fromCategory=${categoryId}` : '';
      navigate(`/collection/${collection.id}${categoryParam}`);
    }
  };

  if (collection.type === 'collection') {
    // 合集卡片布局 - 图片背景 + 居中文字
    return (
      <div 
        className="group cursor-pointer transform transition-all duration-300 hover:scale-[1.03] hover:shadow-2xl w-full max-w-none md:max-w-[860px]"
        onClick={handleCollectionClick}
      >
        <div 
          className={`relative rounded-lg overflow-hidden shadow-lg transition-all duration-300 group-hover:shadow-xl ${!isDesktop ? 'aspect-video' : ''}`} 
          style={{ height: isDesktop ? '280px' : 'auto' }}
        >
          <SecureDecryptedImage
            src={collection.coverUrl || collection.coverImage}
            alt={collection.title}
            className="w-full h-full object-cover"
            lazyLoad={false}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              display: 'block'
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
              {collection.title}
            </h2>
            
            {/* 作者、日期、视频数量等信息 */}
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
                {collection.author || '天涯吃瓜小慧'}
              </span>
              <span className="mx-1 md:mx-2">•</span>
              {/* 日期 */}
              <span>{new Date(collection.createdAt).getFullYear()} 年 {String(new Date(collection.createdAt).getMonth() + 1).padStart(2, '0')} 月 {String(new Date(collection.createdAt).getDate()).padStart(2, '0')} 日</span>
              <span className="mx-1 md:mx-2">•</span>
              {/* 视频数量 */}
              <span>{collection.videoCount || 0} 个视频</span>
            </div>
            
            {/* 描述已移除 */}
            
            {/* 观看次数已移除 */}
          </div>
        </div>
      </div>
    );
  }

  return null;
};

// 分页组件
const Pagination = ({ current, total, pageSize, onChange }) => {
  const totalPages = Math.ceil(total / pageSize);
  
  return (
    <div className="flex justify-center items-center mt-16 mb-8">
      <div className="flex items-center space-x-3">
        {/* 上一页按钮 */}
        <button 
          className="w-9 h-9 bg-transparent text-white border border-gray-400 hover:border-white hover:bg-gray-700 transition-all duration-200 flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={() => {
            if (current > 1) {
              onChange(current - 1);
            }
          }}
          disabled={current <= 1}
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
          </svg>
        </button>
        
        {/* 页码信息 */}
        <span className="text-white text-base">{current}/{totalPages}</span>
        
        {/* 页码输入框 */}
        <input 
          type="number" 
          min="1"
          max={totalPages}
          placeholder={current}
          className="w-20 h-9 px-3 text-base bg-transparent text-white border border-gray-400 focus:outline-none focus:border-white"
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
          className="px-4 py-2 text-base bg-transparent text-white border border-gray-400 hover:border-white hover:bg-gray-700 transition-all duration-200"
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
          className="w-9 h-9 bg-transparent text-white border border-gray-400 hover:border-white hover:bg-gray-700 transition-all duration-200 flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={() => {
            if (current < totalPages) {
              onChange(current + 1);
            }
          }}
          disabled={current >= totalPages}
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"/>
          </svg>
        </button>
      </div>
    </div>
  );
};

export default CollectionList; /* Mobile consistency Mon Aug  4 04:40:29 CST 2025 */
/* Mobile full width update Mon Aug  4 04:42:44 CST 2025 */
