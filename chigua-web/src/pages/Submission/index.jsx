import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation, useParams } from 'react-router-dom';
import advertisementService from '../../services/advertisementService';
import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';
import apiCacheService from '../../services/apiCacheService';

import OfficialNotice from '../../components/OfficialNotice';
import LogoAds from '../../components/LogoAds';
import CommentSection from '../../components/Comment';
import Footer from '../../components/common/Footer';
import { useSiteConfig, usePageConfig } from '../../hooks/usePageConfig';

const Submission = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { categoryId } = useParams(); // 获取分类ID参数
  
  // 获取站点配置
  const { getSiteConfig } = useSiteConfig();
  const { getConfig } = usePageConfig();
  const [detailTopAds, setDetailTopAds] = useState([]);
  const [detailBottomAds, setDetailBottomAds] = useState([]);
  const [textLinkAds, setTextLinkAds] = useState([]);
  const [selectedAdId, setSelectedAdId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [shareClicked, setShareClicked] = useState(false);

  // 横幅容器自适应：按图片真实宽高比计算容器高度，保证完整显示不裁剪
  const topBannerRefs = useRef({});
  const bottomBannerRefs = useRef({});
  const topBannerRatios = useRef({}); // index -> h/w
  const bottomBannerRatios = useRef({});
  const [topBannerHeights, setTopBannerHeights] = useState({}); // index -> px
  const [bottomBannerHeights, setBottomBannerHeights] = useState({});
  // 顶部与底部横幅保持同一套计算方式：各自按图片宽高比自适应

  const recalcHeights = () => {
    // 计算并限制在 [50, 140] 范围内，避免过小过大
    const clamp = (v) => Math.max(50, Math.min(v, 140));

    const newTop = { ...topBannerHeights };
    Object.keys(topBannerRefs.current).forEach((key) => {
      const ref = topBannerRefs.current[key];
      const ratio = topBannerRatios.current[key];
      if (ref && ratio) {
        const width = ref.clientWidth || 0;
        newTop[key] = clamp(width * ratio);
      }
    });
    setTopBannerHeights(newTop);

    const newBottom = { ...bottomBannerHeights };
    Object.keys(bottomBannerRefs.current).forEach((key) => {
      const ref = bottomBannerRefs.current[key];
      const ratio = bottomBannerRatios.current[key];
      if (ref && ratio) {
        const width = ref.clientWidth || 0;
        newBottom[key] = clamp(width * ratio);
      }
    });
    setBottomBannerHeights(newBottom);
  };

  useEffect(() => {
    const onResize = () => recalcHeights();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [detailTopAds.length, detailBottomAds.length]);

  // 获取投稿回家顶部横幅广告
  const fetchTopBannerAds = async () => {
    try {
      const response = await advertisementService.getAdsByPosition('投稿回家顶部横幅');

      if (response.code === 200) {
        const ads = response.data || [];
        setDetailTopAds(ads);

      } else {
        
        setDetailTopAds([]);
      }
    } catch (error) {
      
      setDetailTopAds([]);
    }
  };

  // 获取投稿回家底部横幅广告
  const fetchBottomBannerAds = async () => {
    try {
      const response = await advertisementService.getAdsByPosition('投稿回家底部横幅');

      if (response.code === 200) {
        const ads = response.data || [];
        setDetailBottomAds(ads);

      } else {
        
        setDetailBottomAds([]);
      }
    } catch (error) {
      
      setDetailBottomAds([]);
    }
  };

  // 获取文字链接广告
  const fetchTextLinkAds = async () => {
    try {
      // 使用缓存服务获取文字链接广告
      const response = await apiCacheService.getApiData(
        'ADS_TEXT_LINK',
        { type: '3' },
        (params) => advertisementService.getAdsByType(params.type),
        { cacheDuration: 5 * 60 * 1000 }
      );
      if (response.code === 200) {
        setTextLinkAds(Array.isArray(response.data) ? response.data : []);
        
      } else {
        
      }
    } catch (error) {
      
    }
  };

  // 处理广告点击
  const handleAdClick = async (ad) => {
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

  // 处理横幅广告点击（专门用于顶部和底部横幅广告）
  const handleBannerAdClick = async (ad) => {
    try {
      await advertisementService.clickAd(ad.id);

      // 横幅广告强制在新标签页打开
      if (ad.linkUrl) {
        // 确保URL包含协议
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    } catch (error) {
      
      // 即使统计失败，仍然执行跳转
      if (ad.linkUrl) {
        // 确保URL包含协议
        let url = ad.linkUrl;
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
          url = 'https://' + url;
        }
        
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    }
  };

  // 处理文字链接广告点击
  const handleTextAdClick = async (ad) => {
    // 设置选中状态
    setSelectedAdId(ad.id);
    
    // 执行广告点击逻辑
    await handleAdClick(ad);
  };

  // 处理分享点击
  const handleShareClick = async () => {
    // 生成分享内容
        const shareTitle = `投稿求瓜 - ${getSiteConfig('site_name') || '天涯吃瓜'}`;  
    const shareUrl = window.location.href;
    const shareContent = `${shareTitle}\n\n${shareUrl}\n\n有料爆料快来投稿！热瓜等你来分享！\n\n复制上面的链接用浏览器打开即可马上投稿求瓜！`;
    
    try {
      // 复制到剪贴板
      await navigator.clipboard.writeText(shareContent);
      
      // 切换状态
      setShareClicked(true);
      
      // 3秒后恢复初始状态
      setTimeout(() => {
        setShareClicked(false);
      }, 3000);
      
    } catch (error) {
      
      // 降级方案：使用旧版API
      try {
        const textArea = document.createElement('textarea');
        textArea.value = shareContent;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        
        setShareClicked(true);
        setTimeout(() => {
          setShareClicked(false);
        }, 3000);
      } catch (fallbackError) {
        
      }
    }
  };

  // 页面初始化
  useEffect(() => {
    
    // 滚动到页面顶部
    window.scrollTo(0, 0);
    // 获取投稿回家专用广告
    fetchTopBannerAds();
    fetchBottomBannerAds();
    fetchTextLinkAds();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
        <div className="pt-20 md:pt-24 flex items-center justify-center">
          <div className="text-white">加载中...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      {/* CSS样式 - 网站地址信息样式 */}
      <style dangerouslySetInnerHTML={{
        __html: `
          .detail-site-address a {
            color: #1ABC9C !important;
            text-decoration: none !important;
            transition: opacity 0.2s ease !important;
          }
          .detail-site-address a:hover {
            opacity: 0.8 !important;
          }
          .detail-site-address p, .detail-site-address span, .detail-site-address strong {
            color: #AAAAAA !important;
          }
        `
      }} />
      
      {/* 主要内容区域 */}
      <div className="pt-16 md:pt-20 pb-8">
        <div className="container mx-auto px-4 max-w-6xl pt-2 sm:pt-4">
        
        {/* 标题区域 - 硬编码为投稿求瓜 */}
        <div className="flex justify-center mb-8 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <h1 
              className="font-bold mb-4 text-center text-3xl sm:text-4xl"
              style={{ 
                color: '#BCBCBC',
                textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
                letterSpacing: '2px'
              }}
            >
              投稿求瓜
            </h1>
          </div>
        </div>

        {/* 详情顶部横幅广告 */}
        {detailTopAds.length > 0 && (
          <div className="mb-6 md:mb-12">
            <div className="flex justify-center px-1 md:px-4">
              <div className="w-full" style={{ maxWidth: '770px' }}>
                <div className="space-y-2 md:space-y-4">
                  {detailTopAds.map((ad, index) => (
                    <div 
                      key={ad.id || index}
                      className="cursor-pointer overflow-hidden"
                      onClick={() => handleBannerAdClick(ad)}
                    >
                      <div 
                        className="relative shadow-lg overflow-hidden"
                        ref={(el) => (topBannerRefs.current[index] = el)}
                        style={{ 
                          width: '100%',
                          height: topBannerHeights[index] ? `${topBannerHeights[index]}px` : undefined,
                          minHeight: '50px'
                        }}
                      >
                        {/* 背景移除模糊，避免视觉模糊问题 */}
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt="横幅广告"
                          priority="high"
                          lazyLoad={false}
                          objectFit="contain"
                          imageClassName="w-full h-full"
                          imageStyle={{
                            objectPosition: 'center'
                          }}
                          onLoad={(e) => {
                            const img = e.target;
                            const ratio = (img.naturalHeight || 1) / (img.naturalWidth || 1);
                            topBannerRatios.current[index] = ratio;
                            recalcHeights();
                          }}
                        />
                        
                        {/* 底部渐变 */}
                        <div className="absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-black via-black/50 to-transparent opacity-60"></div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 文字链接广告区域（与视频详情一致） */}
        <div className="flex justify-center mb-8 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <div className="grid grid-cols-4 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4 lg:gap-5 mb-6 sm:mb-8">
              {(Array.isArray(textLinkAds) ? textLinkAds : []).slice(0, 16).map((ad, index) => {
                const isSelected = selectedAdId === ad.id;
                return (
                  <div 
                    key={ad.id || index}
                    className={`border rounded text-center cursor-pointer transition-colors duration-200 flex items-center justify-center min-h-[45px] sm:min-h-[50px] px-2 ${
                      isSelected 
                        ? 'border-[#18BD9D]' 
                        : 'border-[#5E5C5C] hover:border-[#1ABCA0]'
                    }`}
                    onClick={() => handleTextAdClick(ad)}
                  >
                    <span 
                      className={`text-xs sm:text-sm transition-colors duration-200 leading-tight text-center break-words ${
                        isSelected ? 'text-[#18BD9D]' : 'text-gray-300 hover:text-[#1ABCA0]'
                      }`}
                    >
                      {ad.linkText || ad.title}
                    </span>
                  </div>
                );
              })}
              {Array.from({ length: Math.max(0, 16 - ((Array.isArray(textLinkAds) ? textLinkAds : []).length)) }, (_, index) => (
                <div 
                  key={`placeholder-${index}`} 
                  className="border border-[#5E5C5C] rounded text-center flex items-center justify-center min-h-[45px] sm:min-h-[50px] px-2"
                >
                  <span className="text-gray-300 text-xs sm:text-sm">占位链接</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* 网站站点地址区域 - 只有API配置时才显示 */}
        {getConfig('detail_site_address') && (
          <div className="flex justify-center mb-8 sm:mb-12 px-1 md:px-4">
            <div className="w-full" style={{ maxWidth: '770px' }}>
              <div 
                style={{
                  backgroundColor: '#383636',
                  borderRadius: '8px',
                  padding: '20px',
                  position: 'relative',
                  paddingLeft: '30px'
                }}
              >
                {/* 左侧竖线 */}
                <div 
                  style={{
                    position: 'absolute',
                    left: '0',
                    top: '0',
                    bottom: '0',
                    width: '4px',
                    backgroundColor: '#5F5F5F',
                    borderTopLeftRadius: '8px',
                    borderBottomLeftRadius: '8px'
                  }}
                ></div>
                
                {/* 内容区域 - 仅显示API配置的内容 */}
                <div 
                  className="detail-site-address" 
                  style={{ 
                    color: '#AAAAAA',
                    fontSize: '16px', 
                    lineHeight: '1.8', 
                    textAlign: 'left' 
                  }}
                  dangerouslySetInnerHTML={{ 
                    __html: getConfig('detail_site_address')
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* 投稿区域 */}
        <div className="flex justify-center mb-8 sm:mb-12 px-1 md:px-4">
          <div className="w-full submission-notice-scope" style={{ maxWidth: '770px' }}>
            <style>{`
              .submission-notice-scope .official-notice-content { font-size: initial !important; }
              .submission-notice-scope .official-notice-content h1,
              .submission-notice-scope .official-notice-content h2,
              .submission-notice-scope .official-notice-content h3,
              .submission-notice-scope .official-notice-content h4,
              .submission-notice-scope .official-notice-content h5,
              .submission-notice-scope .official-notice-content h6 { font-size: inherit !important; }
              .submission-notice-scope .official-notice-content .ql-size-small { font-size: 0.75em !important; }
              .submission-notice-scope .official-notice-content .ql-size-large { font-size: 1.5em !important; }
              .submission-notice-scope .official-notice-content .ql-size-huge { font-size: 2.5em !important; }
              .submission-notice-scope .official-notice-content .ql-size-12px { font-size: 12px !important; }
              .submission-notice-scope .official-notice-content .ql-size-13px { font-size: 13px !important; }
              .submission-notice-scope .official-notice-content .ql-size-14px { font-size: 14px !important; }
              .submission-notice-scope .official-notice-content .ql-size-15px { font-size: 15px !important; }
              .submission-notice-scope .official-notice-content .ql-size-16px { font-size: 16px !important; }
              .submission-notice-scope .official-notice-content .ql-size-18px { font-size: 18px !important; }
              .submission-notice-scope .official-notice-content .ql-size-20px { font-size: 20px !important; }
              .submission-notice-scope .official-notice-content .ql-size-24px { font-size: 24px !important; }
              .submission-notice-scope .official-notice-content .ql-size-28px { font-size: 28px !important; }
              .submission-notice-scope .official-notice-content .ql-size-32px { font-size: 32px !important; }
            `}</style>
            <OfficialNotice categoryType="投稿" />
          </div>
        </div>

        {/* 感谢支持文字 */}
        <div className="flex justify-center mb-6 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <div
              style={{
                backgroundColor: '#383737',
                borderRadius: '8px',
                padding: '16px 20px',
                position: 'relative',
                paddingLeft: '30px'
              }}
            >
              {/* 左侧青绿色竖线 */}
              <div
                style={{
                  position: 'absolute',
                  left: '0',
                  top: '0',
                  bottom: '0',
                  width: '4px',
                  backgroundColor: '#1ABC9C',
                  borderTopLeftRadius: '8px',
                  borderBottomLeftRadius: '8px'
                }}
              ></div>
              
              {/* 感谢文字内容 */}
              <div style={{ color: '#ffffff', fontSize: '16px', lineHeight: '1.5', textAlign: 'left' }}>
                天涯吃瓜感谢您的一路支持【您的分享】是天涯吃瓜不断挖掘黑料的最大动力
              </div>
            </div>
          </div>
        </div>

        {/* 回家地址区域 */}
        <div className="flex justify-center mb-8 sm:mb-12 px-1 md:px-4">
          <div className="w-full submission-notice-scope" style={{ maxWidth: '770px' }}>
            <OfficialNotice categoryType="回家地址" />
          </div>
        </div>

        {/* 官方公告区域 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full submission-notice-scope" style={{ maxWidth: '770px' }}>
            <OfficialNotice categoryType="公告" />
            
            {/* 分享功能区域（对齐详情页样式） */}
            <div className="mt-6">
              <div 
                className="rounded-lg p-8 text-center cursor-pointer"
                style={{
                  background: 'transparent',
                  borderRadius: '16px',
                  position: 'relative',
                  overflow: 'hidden'
                }}
                onClick={handleShareClick}
              >
                <div className="relative z-10">
                  <h3 
                    className="text-4xl font-bold mb-4"
                    style={{
                      color: shareClicked ? '#FFFFFF' : '#FFF6A9',
                      textShadow: shareClicked 
                        ? '0 0 12px #66173B, 0 0 24px #66173B, 0 0 36px #66173B' 
                        : '0 0 12px #CF7B01, 0 0 24px #CF7B01, 0 0 36px #CF7B01',
                      letterSpacing: '2px',
                      filter: shareClicked ? 'drop-shadow(0 0 6px #66173B)' : 'drop-shadow(0 0 6px #CF7B01)'
                    }}
                  >
                    {shareClicked ? '你的朋友会感激你的分享！' : '有料爆料快来投稿求瓜 快点击分享'}
                  </h3>
                  
                  {!shareClicked && (
                    <p 
                      className="text-3xl font-bold"
                      style={{
                        color: '#FFF6A9',
                        textShadow: '0 0 12px #CF7B01, 0 0 24px #CF7B01, 0 0 36px #CF7B01',
                        letterSpacing: '1px',
                        filter: 'drop-shadow(0 0 6px #CF7B01)'
                      }}
                    >
                      吧！
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 详情底部横幅广告 */}
        {detailBottomAds.length > 0 && (
          <div className="mb-6 md:mb-12">
            <div className="flex justify-center px-1 md:px-4">
              <div className="w-full" style={{ maxWidth: '770px' }}>
                <div className="space-y-2 md:space-y-4">
                  {detailBottomAds.map((ad, index) => (
                    <div 
                      key={ad.id || index}
                      className="cursor-pointer overflow-hidden"
                      onClick={() => handleBannerAdClick(ad)}
                    >
                      <div 
                        className="relative shadow-lg overflow-hidden"
                        ref={(el) => (bottomBannerRefs.current[index] = el)}
                        style={{ 
                          width: '100%',
                          height: bottomBannerHeights[index] ? `${bottomBannerHeights[index]}px` : undefined,
                          minHeight: '50px'
                        }}
                      >
                        {/* 背景移除模糊，避免视觉模糊问题 */}
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt="横幅广告"
                          priority="high"
                          lazyLoad={false}
                          objectFit="contain"
                          imageClassName="w-full h-full"
                          imageStyle={{
                            objectPosition: 'center'
                          }}
                          onLoad={(e) => {
                            const img = e.target;
                            const ratio = (img.naturalHeight || 1) / (img.naturalWidth || 1);
                            bottomBannerRatios.current[index] = ratio;
                            recalcHeights();
                          }}
                        />
                        
                        {/* 底部渐变 */}
                        <div className="absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-black via-black/50 to-transparent opacity-60"></div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Logo广告区域 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <LogoAds />
          </div>
        </div>

        {/* 评论区域 */}
        <div className="flex justify-center mb-8 sm:mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <CommentSection commentType="submission" />
          </div>
        </div>

        {/* 页面底部区域 */}
        <Footer />

        </div>
      </div>
    </div>
  );
};

export default Submission;