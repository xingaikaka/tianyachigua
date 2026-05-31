import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation, useParams } from 'react-router-dom';
import advertisementService from '../../services/advertisementService';
import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';
import apiCacheService from '../../services/apiCacheService';
import richTextImageProcessor from '../../utils/richTextImageProcessor';

import OfficialNotice from '../../components/OfficialNotice';
import LogoAds from '../../components/LogoAds';
import CommentSection from '../../components/Comment';
import Footer from '../../components/common/Footer';
import { usePageConfig, useSiteConfig } from '../../hooks/usePageConfig';

const HomeWay = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { categoryId } = useParams(); // 获取分类ID参数
  
  // 获取站点配置
  const { getSiteConfig } = useSiteConfig();
  const [detailTopAds, setDetailTopAds] = useState([]);
  const [detailBottomAds, setDetailBottomAds] = useState([]);
  const [textLinkAds, setTextLinkAds] = useState([]);
  const [selectedAdId, setSelectedAdId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [shareClicked, setShareClicked] = useState(false);
  const [processedRememberAddress, setProcessedRememberAddress] = useState(null);

  // 获取页面配置
  const { getConfig } = usePageConfig();

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
        const shareTitle = `回家的路 - ${getSiteConfig('site_name') || '天涯吃瓜'}`;  
    const shareUrl = window.location.href;
    const shareContent = `${shareTitle}\n\n${shareUrl}\n\n最新网址导航！回家的路！\n\n复制上面的链接用浏览器打开即可马上查看！`;
    
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

  // 处理牢记地址中的加密图片
  const rememberAddressContent = getConfig('remember_address');
  useEffect(() => {
    if (!rememberAddressContent) return;
    richTextImageProcessor.processRichTextImages(rememberAddressContent, { placeholdersOnly: false })
      .then(processed => setProcessedRememberAddress(processed))
      .catch(() => setProcessedRememberAddress(rememberAddressContent));
  }, [rememberAddressContent]);

  if (loading) {
    return (
      <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
        <div className="pt-28 md:pt-24 flex items-center justify-center">
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
      {/* CSS样式 - 牢记地址信息样式 */}
      <style dangerouslySetInnerHTML={{
        __html: `
          .remember-address a {
            color: #1ABC9C !important;
            text-decoration: none !important;
            transition: opacity 0.2s ease !important;
          }
          .remember-address a:hover {
            opacity: 0.8 !important;
          }
          .remember-address p {
            margin-bottom: 8px !important;
            text-align: left !important;
            color: #AAAAAA !important;
            font-size: 18px !important;
          }
          .remember-address span {
            color: #AAAAAA !important;
            font-size: 18px !important;
          }
          .remember-address strong {
            color: #AAAAAA !important;
            font-weight: 500 !important;
            font-size: 18px !important;
          }
          .remember-address div {
            font-size: 18px !important;
          }
        `
      }} />
      
      {/* 主要内容区域 */}
      <div className="pt-16 md:pt-20 pb-8">
        <div className="container mx-auto px-4 max-w-6xl pt-2 sm:pt-4">
        
        {/* 标题区域 - 修改为回家的路 */}
        <div className="flex justify-center mb-8 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <h1 
              className="text-4xl font-bold mb-4 text-center"
              style={{ 
                color: '#BCBCBC',
                textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
                letterSpacing: '2px'
              }}
            >
              回家的路
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
                        style={{ height: 'auto' }}
                      >
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt="横幅广告"
                          priority="high"
                          lazyLoad={false}
                          objectFit="contain"
                          imageStyle={{
                            maxHeight: '120px'
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 文字链接广告区域 */}
        <div className="flex justify-center mb-8 sm:mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            {/* 与视频详情保持一致的文字链接布局与样式 */}
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
              
              {/* 如果广告不足16个，用空占位符填充 */}
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

        {/* 牢记地址信息 */}
        {rememberAddressContent && (
          <div className="flex justify-center mb-6 px-1 md:px-4">
            <div className="w-full text-left" style={{ maxWidth: '770px' }}>
              <div 
                className="text-lg leading-relaxed remember-address" 
                style={{ 
                  color: '#AAAAAA',
                  textAlign: 'left',
                  fontSize: '18px'
                }}
                dangerouslySetInnerHTML={{ __html: processedRememberAddress || rememberAddressContent }}
              />
            </div>
          </div>
        )}

        {/* 投稿区域 */}
        <div className="flex justify-center mb-8 sm:mb-12 px-1 md:px-4">
          <div className="w-full homeway-notice-scope" style={{ maxWidth: '770px' }}>
            <style>{`
              .homeway-notice-scope .official-notice-content { font-size: initial !important; }
              .homeway-notice-scope .official-notice-content h1,
              .homeway-notice-scope .official-notice-content h2,
              .homeway-notice-scope .official-notice-content h3,
              .homeway-notice-scope .official-notice-content h4,
              .homeway-notice-scope .official-notice-content h5,
              .homeway-notice-scope .official-notice-content h6 { font-size: inherit !important; }
              .homeway-notice-scope .official-notice-content .ql-size-small { font-size: 0.75em !important; }
              .homeway-notice-scope .official-notice-content .ql-size-large { font-size: 1.5em !important; }
              .homeway-notice-scope .official-notice-content .ql-size-huge { font-size: 2.5em !important; }
              .homeway-notice-scope .official-notice-content .ql-size-12px { font-size: 12px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-13px { font-size: 13px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-14px { font-size: 14px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-15px { font-size: 15px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-16px { font-size: 16px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-18px { font-size: 18px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-20px { font-size: 20px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-24px { font-size: 24px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-28px { font-size: 28px !important; }
              .homeway-notice-scope .official-notice-content .ql-size-32px { font-size: 32px !important; }
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
          <div className="w-full homeway-notice-scope" style={{ maxWidth: '770px' }}>
            <OfficialNotice categoryType="回家地址" />
          </div>
        </div>

        {/* 官方公告区域 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full homeway-notice-scope" style={{ maxWidth: '770px' }}>
            <OfficialNotice categoryType="公告" />
            
            {/* 分享功能区域 */}
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
                {/* 发光效果背景已移除 */}
                
                {/* 文字内容 */}
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
                    {shareClicked ? '你的朋友会感激你的分享！' : '网址导航！回家的路！快点击分享'}
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
                
                {/* 装饰性光效已移除 */}
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
                        style={{ height: 'auto' }}
                      >
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt="横幅广告"
                          priority="high"
                          lazyLoad={false}
                          objectFit="contain"
                          imageStyle={{
                            maxHeight: '120px'
                          }}
                        />
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

export default HomeWay;