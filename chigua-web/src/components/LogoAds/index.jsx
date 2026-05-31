import React, { useState, useEffect } from 'react';
import advertisementService from '../../services/advertisementService';
import logoAdsCacheService from '../../services/logoAdsCacheService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

const LOGO_PLACEHOLDER = '/logo_ad.png';

const LogoAds = React.memo(({ hideWhileLoading = false }) => {
  const [logoAds, setLogoAds] = useState({
    1: [], // 热门应用 (91甄选)
    2: [], // 最新上架 (瓜友必备)  
    3: []  // 必备精品 (破解软件)
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 预加载广告logo，避免首次渲染时闪烁
    try {
      const preImg = new Image();
      preImg.src = LOGO_PLACEHOLDER;
      preImg.decoding = 'async';
      preImg.loading = 'eager';
    } catch (_) {}
    fetchLogoAds();
  }, []);

  const fetchLogoAds = async () => {
    try {
      setLoading(true);
      
      // 使用缓存服务并行获取三种类型的Logo广告
      const [hotApps, newApps, essentialApps] = await Promise.all([
        logoAdsCacheService.getLogoAds(1, advertisementService.getLogoAdsByAppType), // 热门应用
        logoAdsCacheService.getLogoAds(2, advertisementService.getLogoAdsByAppType), // 最新上架
        logoAdsCacheService.getLogoAds(3, advertisementService.getLogoAdsByAppType)  // 必备精品
      ]);

      const ads1 = hotApps.data || []
      const ads2 = newApps.data || []
      const ads3 = essentialApps.data || []
      setLogoAds({ 1: ads1, 2: ads2, 3: ads3 });
      // 批量上报曝光（异步静默，不阻塞渲染）
      const allAds = [...ads1, ...ads2, ...ads3];
      allAds.forEach(ad => {
        if (ad && ad.id) {
          try { advertisementService.recordAdImpression(ad.id); } catch (_) {}
        }
      });
    } catch (error) {
      // 静默处理错误
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

  // 生成占位符数据
  const generatePlaceholders = (count = 10) => {
    return Array.from({ length: count }, (_, index) => ({
      id: `placeholder-${index}`,
      iconName: '敬请期待',
      imageUrl: '',
      linkUrl: '',
      isPlaceholder: true
    }));
  };

  // 获取显示的广告数据（确保有10个）
  const getDisplayAds = (appType) => {
    const ads = logoAds[appType] || [];
    const actualAds = ads.slice(0, 10);
    const placeholderCount = Math.max(0, 10 - actualAds.length);
    const placeholders = generatePlaceholders(placeholderCount);
    return [...actualAds, ...placeholders];
  };

  if (loading) {
    // 可选：在加载期间完全隐藏，避免页面闪烁
    if (hideWhileLoading) {
      return null;
    }
    return (
      <div className="mb-8">
        <div className="animate-pulse">
          {[1, 2, 3].map((appType) => (
            <div key={appType} className="mb-6 sm:mb-8 last:mb-0">
              {/* 类别标题条加载状态 */}
              <div 
                className="bg-gray-700 mb-3 sm:mb-4"
                style={{ height: '50px', width: '100%', borderTop: '2px solid #444444' }}
              ></div>
              
              {/* Logo网格加载状态 */}
              <div className="grid grid-cols-5 sm:grid-cols-8 lg:grid-cols-10 gap-2 sm:gap-3">
                {[...Array(10)].map((_, i) => (
                  <div key={i} className="text-center">
                    <div className="bg-gray-700 w-10 h-10 sm:w-12 sm:h-12 rounded-lg mb-1 sm:mb-2 mx-auto"></div>
                    <div className="bg-gray-700 h-2 sm:h-3 rounded"></div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
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
              {displayAds.map((ad, index) => (
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
                        border: '1px solid #5a5757',
                        backgroundImage: !ad.isPlaceholder && ad.imageUrl ? `url(${LOGO_PLACEHOLDER})` : undefined,
                        backgroundSize: 'contain',
                        backgroundPosition: 'center',
                        backgroundRepeat: 'no-repeat'
                      }}
                    >
                      {/* 真实Logo图片 */}
                      {!ad.isPlaceholder && ad.imageUrl && (
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt={ad.iconName}
                          className="w-full h-full rounded-lg"
                          style={{
                            width: '100%',
                            height: '100%',
                            objectFit: 'cover',
                            objectPosition: 'center'
                          }}
                          fallbackSrc={LOGO_PLACEHOLDER}
                          lazyLoad={true}
                          priority="low"
                          showLoadingIndicator={false}
                        />
                      )}
                      
                      {/* 占位符内容 */}
                      {ad.isPlaceholder && (
                        <img
                          src={LOGO_PLACEHOLDER}
                          alt="logo placeholder"
                          className="w-full h-full object-contain"
                          style={{
                            filter: 'grayscale(60%)',
                            opacity: 0.6
                          }}
                          loading="lazy"
                        />
                      )}
                      
                      {/* 悬浮效果 - 只对非占位符生效 */}
                      {!ad.isPlaceholder && (
                        <div className="absolute inset-0 bg-blue-500 bg-opacity-0 group-hover:bg-opacity-20 transition-all duration-200 rounded-lg"></div>
                      )}
                    </div>
                  </div>

                  {/* 应用名称 */}
                  <div 
                    className={`text-xs leading-tight transition-colors duration-200 px-1 ${
                      ad.isPlaceholder 
                        ? 'text-gray-500' 
                        : 'text-gray-300 group-hover:text-blue-400'
                    }`}
                    style={{ fontSize: '10px' }}
                    title={ad.iconName}
                  >
                    {ad.iconName}
                  </div>
                </div>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
});

LogoAds.displayName = 'LogoAds';

export default LogoAds; 
