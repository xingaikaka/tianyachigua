import React, { useState, useEffect } from 'react';
import advertisementService from '../../services/advertisementService';

const LogoAdsOptimized = () => {
  const [logoAds, setLogoAds] = useState({
    1: [], // 热门应用 (91甄选)
    2: [], // 最新上架 (瓜友必备)  
    3: []  // 必备精品 (破解软件)
  });
  const [loading, setLoading] = useState(true);
  const [imageLoadStates, setImageLoadStates] = useState({}); // 跟踪每个图片的加载状态

  useEffect(() => {
    fetchLogoAds();
  }, []);

  const fetchLogoAds = async () => {
    try {
      setLoading(true);
      
      // 并行获取三种类型的Logo广告
      const [hotApps, newApps, essentialApps] = await Promise.all([
        advertisementService.getLogoAdsByAppType(1), // 热门应用
        advertisementService.getLogoAdsByAppType(2), // 最新上架
        advertisementService.getLogoAdsByAppType(3)  // 必备精品
      ]);

      setLogoAds({
        1: hotApps.data || [],
        2: newApps.data || [],
        3: essentialApps.data || []
      });
    } catch (error) {
    } finally {
      setLoading(false);
    }
  };

  const handleLogoClick = async (ad) => {
    try {
      // 记录点击统计
      await advertisementService.clickAd(ad.id);

      // 打开链接
      if (ad.linkUrl) {
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    } catch (error) {
      // 即使统计失败，仍然执行跳转
      if (ad.linkUrl) {
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    }
  };

  const getSectionTitle = (appType) => {
    switch (appType) {
      case 1: return '热门应用';
      case 2: return '最新上架';
      case 3: return '必备精品';
      default: return '';
    }
  };

  // 处理图片加载状态
  const handleImageLoad = (adId) => {
    setImageLoadStates(prev => ({
      ...prev,
      [adId]: { loaded: true, error: false }
    }));
  };

  const handleImageError = (adId) => {
    setImageLoadStates(prev => ({
      ...prev,
      [adId]: { loaded: false, error: true }
    }));
  };

  // 生成占位符数据
  const generatePlaceholders = (count) => {
    return Array.from({ length: count }, (_, index) => ({
      id: `placeholder-${index}`,
      iconName: '',
      imageUrl: '',
      isPlaceholder: true
    }));
  };

  // 获取显示的广告数据（真实数据 + 占位符）
  const getDisplayAds = (appType) => {
    const realAds = logoAds[appType] || [];
    const targetCount = 10; // 每行显示10个
    
    if (realAds.length >= targetCount) {
      return realAds.slice(0, targetCount);
    } else {
      const placeholderCount = targetCount - realAds.length;
      return [...realAds, ...generatePlaceholders(placeholderCount)];
    }
  };

  // 加载状态
  if (loading) {
    return (
      <div className="mb-8">
        {[1, 2, 3].map((appType) => (
          <div key={appType} className="mb-6 sm:mb-8 last:mb-0">
            {/* 类别标题条加载状态 */}
            <div 
              className="bg-gray-700 mb-3 sm:mb-4 animate-pulse"
              style={{ height: '50px', width: '100%', borderTop: '2px solid #444444' }}
            ></div>
            
            {/* Logo网格加载状态 */}
            <div className="grid grid-cols-5 sm:grid-cols-8 lg:grid-cols-10 gap-2 sm:gap-3">
              {[...Array(10)].map((_, i) => (
                <div key={i} className="text-center">
                  <div className="bg-gray-700 w-10 h-10 sm:w-12 sm:h-12 rounded-lg mb-1 sm:mb-2 mx-auto animate-pulse"></div>
                  <div className="bg-gray-700 h-2 sm:h-3 rounded animate-pulse"></div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="mb-8">
      {/* 遍历三种应用类型 - 确保都显示 */}
      {[1, 2, 3].map((appType) => {
        const displayAds = getDisplayAds(appType);

        return (
          <div key={appType} className="mb-6 sm:mb-8 last:mb-0">
            {/* 类别标题条 - 有背景，高度50px，全宽度 */}
            <div 
              className="flex items-center px-4 sm:px-6 mb-3 sm:mb-4"
              style={{ 
                backgroundColor: '#303030',
                height: '50px',
                width: '100%',
                borderTop: '2px solid #444444'
              }}
            >
              <h3 
                className="text-base sm:text-lg font-bold text-left"
                style={{ color: '#FFFFFF' }}
              >
                {getSectionTitle(appType)}
              </h3>
            </div>

            {/* Logo图标网格 - 响应式布局 */}
            <div className="grid grid-cols-5 sm:grid-cols-8 lg:grid-cols-10 gap-2 sm:gap-3">
              {displayAds.map((ad, index) => {
                const imageState = imageLoadStates[ad.id] || { loaded: false, error: false };
                
                return (
                  <div
                    key={ad.id || `${appType}-${index}`}
                    className={`text-center transition-all duration-200 ${
                      ad.isPlaceholder 
                        ? 'cursor-default opacity-50' 
                        : 'cursor-pointer group hover:transform hover:scale-105'
                    }`}
                    onClick={() => !ad.isPlaceholder && handleLogoClick(ad)}
                  >
                    {/* Logo图标 */}
                    <div className="relative mb-1 sm:mb-2">
                      <div 
                        className="w-10 h-10 sm:w-12 sm:h-12 mx-auto rounded-lg transition-all duration-200 relative overflow-hidden"
                        style={{
                          backgroundColor: '#2A2828',
                          border: '1px solid #5a5757'
                        }}
                      >
                        {/* 真实Logo图片 - 直接使用img标签 */}
                        {!ad.isPlaceholder && ad.imageUrl && (
                          <>
                            {/* 加载状态 */}
                            {!imageState.loaded && !imageState.error && (
                              <div className="absolute inset-0 flex items-center justify-center">
                                <div className="w-4 h-4 sm:w-6 sm:h-6 bg-gray-500 rounded animate-pulse"></div>
                              </div>
                            )}
                            
                            {/* 实际图片 */}
                            <img
                              src={ad.imageUrl}
                              alt={ad.iconName}
                              className={`w-full h-full rounded-lg transition-opacity duration-200 ${
                                imageState.loaded ? 'opacity-100' : 'opacity-0'
                              }`}
                              style={{
                                width: '100%',
                                height: '100%',
                                objectFit: 'cover',
                                objectPosition: 'center'
                              }}
                              onLoad={() => handleImageLoad(ad.id)}
                              onError={() => handleImageError(ad.id)}
                              loading="lazy" // 原生懒加载
                            />
                            
                            {/* 错误状态 */}
                            {imageState.error && (
                              <div className="absolute inset-0 flex items-center justify-center">
                                <div className="w-4 h-4 sm:w-6 sm:h-6 bg-gray-600 rounded opacity-50"></div>
                              </div>
                            )}
                          </>
                        )}
                        
                        {/* 占位符内容 */}
                        {ad.isPlaceholder && (
                          <div className="w-full h-full flex items-center justify-center">
                            <div className="w-4 h-4 sm:w-6 sm:h-6 bg-gray-500 rounded opacity-30"></div>
                          </div>
                        )}
                        
                        {/* 悬浮效果 - 只对非占位符生效 */}
                        {!ad.isPlaceholder && (
                          <div className="absolute inset-0 bg-blue-500 bg-opacity-0 group-hover:bg-opacity-20 transition-all duration-200 rounded-lg"></div>
                        )}
                      </div>
                    </div>

                    {/* Logo名称 */}
                    <div 
                      className="text-xs sm:text-sm px-1 leading-tight"
                      style={{ 
                        color: ad.isPlaceholder ? '#666666' : '#CCCCCC',
                        minHeight: '2.5rem',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      {ad.isPlaceholder ? '' : (ad.iconName || '应用')}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default LogoAdsOptimized;
