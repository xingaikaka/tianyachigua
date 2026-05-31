import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import collectionService from '../../services/collectionService';
import advertisementService from '../../services/advertisementService';
import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';
import apiCacheService from '../../services/apiCacheService';

import HotRecommended from '../../components/HotRecommended';
import OfficialNotice from '../../components/OfficialNotice';
import ArticleNavigation from '../../components/ArticleNavigation';
import LogoAds from '../../components/LogoAds';
import CommentSection from '../../components/Comment';
import { usePageConfig } from '../../hooks/usePageConfig';

const CollectionDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [collection, setCollection] = useState(null);
  const [collectionVideos, setCollectionVideos] = useState([]);
  const [detailTopAds, setDetailTopAds] = useState([]);
  const [detailBottomAds, setDetailBottomAds] = useState([]);
  const [textLinkAds, setTextLinkAds] = useState([]);
  const [selectedAdId, setSelectedAdId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isDesktop, setIsDesktop] = useState(window.innerWidth >= 768);

  // 获取页面配置
  const { getConfig } = usePageConfig();
  const siteAddressContent = getConfig('detail_site_address');

  const siteAddressHtml = useMemo(() => {
    if (!siteAddressContent) return '';

    const applyLinkify = (text) => {
      if (!text) return '';
      return text
        .replace(/(https?:\/\/[^\s]+)/gi, (match) => {
          const sanitizedMatch = match.replace(/[),.;!?]+$/g, '');
          const trailing = match.slice(sanitizedMatch.length);
          return `<a href="${sanitizedMatch}" target="_blank" rel="noopener noreferrer" style="color:#27D2C3;text-decoration:none;">${sanitizedMatch}</a>${trailing}`;
        })
        .replace(/([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})/g, (match) => {
          const sanitizedMatch = match.replace(/[),.;!?]+$/g, '');
          const trailing = match.slice(sanitizedMatch.length);
          return `<a href="mailto:${sanitizedMatch}" style="color:#27D2C3;text-decoration:none;">${sanitizedMatch}</a>${trailing}`;
        });
    };

    const normalized = siteAddressContent.replace(/\r\n/g, '\n');
    const containsHtml = /<[^>]+>/.test(siteAddressContent);

    if (!containsHtml) {
      return applyLinkify(normalized.trim()).replace(/\n/g, '<br />');
    }

    if (typeof window === 'undefined' || typeof window.DOMParser === 'undefined') {
      return applyLinkify(normalized);
    }

    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(`<div>${normalized}</div>`, 'text/html');
      const container = doc.body || doc;
      const walker = doc.createTreeWalker(container, NodeFilter.SHOW_TEXT, null);
      const textNodes = [];
      while (walker.nextNode()) {
        textNodes.push(walker.currentNode);
      }

      textNodes.forEach((node) => {
        const original = node.nodeValue;
        const transformed = applyLinkify(original);
        if (transformed !== original) {
          const tempWrapper = doc.createElement('span');
          tempWrapper.innerHTML = transformed;
          const fragments = Array.from(tempWrapper.childNodes);
          const parent = node.parentNode;
          if (parent) {
            fragments.forEach((child) => {
              parent.insertBefore(child, node);
            });
            parent.removeChild(node);
          }
        }
      });

      return container.innerHTML;
    } catch (_) {
      return applyLinkify(normalized);
    }
  }, [siteAddressContent]);

  // 获取合集详情
  const fetchCollectionDetail = async () => {
    try {
      setLoading(true);
      const response = await collectionService.getCollectionById(parseInt(id));
      
      if (response.code === 200) {
        
        setCollection(response.data);
        
        // 优先使用URL查询参数中的分类ID（用户点击来源的分类）
        const urlParams = new URLSearchParams(location.search);
        const fromCategoryId = urlParams.get('fromCategory');
        
        let categoryId = null;
        if (fromCategoryId) {
          // 使用用户点击来源的分类ID
          categoryId = parseInt(fromCategoryId);
          
        } else if (response.data?.categoryId) {
          // 降级：使用合集本身的分类ID（如果有多个分类，只取第一个）
          const categoryIdStr = String(response.data.categoryId).split(',')[0].trim();
          categoryId = parseInt(categoryIdStr);
          
        }
        
        if (categoryId) {
          await fetchDetailPageAds(categoryId);
          
        } else {
          
          await fetchDetailPageAds(null);
        }
      } else {
        
      }
    } catch (error) {
      
    } finally {
      setLoading(false);
    }
  };

  // 获取合集中的视频列表
  const fetchCollectionVideos = async () => {
    try {
      
      const response = await collectionService.getCollectionVideos(parseInt(id));

      if (response.code === 200) {
        
        setCollectionVideos(response.data || []);
      } else {
        
      }
    } catch (error) {
      
    }
  };

  // 获取详情页面广告（顶部+底部，根据合集分类）
  const fetchDetailPageAds = async (categoryId) => {
    try {

      if (!categoryId) {
        
        // 如果没有分类ID，使用通用广告作为备选
        const [topResponse, bottomResponse] = await Promise.all([
          advertisementService.getAdsByPosition('3'),
          advertisementService.getAdsByPosition('4')
        ]);
        
        if (topResponse.code === 200) {
          setDetailTopAds(topResponse.data || []);
        }
        if (bottomResponse.code === 200) {
          setDetailBottomAds(bottomResponse.data || []);
        }
        return;
      }

      // 使用缓存服务获取详情页广告
      const response = await apiCacheService.getApiData(
        'ADS_DETAIL_PAGE',
        { categoryId },
        (params) => advertisementService.getDetailPageAds(params.categoryId),
        { cacheDuration: 5 * 60 * 1000 }
      );
      
      if (response.code === 200) {
        const { topAds = [], bottomAds = [] } = response.data || {};

        setDetailTopAds(topAds);
        setDetailBottomAds(bottomAds);

      } else {
        
        setDetailTopAds([]);
        setDetailBottomAds([]);
      }
    } catch (error) {
      
      setDetailTopAds([]);
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
        
        setTextLinkAds([]);
      }
    } catch (error) {
      
      setTextLinkAds([]);
    }
  };

  // 处理广告点击
  const handleAdClick = async (ad) => {
    try {
      await advertisementService.clickAd(ad.id);

      if (ad.linkUrl) {
        window.open(ad.linkUrl, '_blank');
      }
    } catch (error) {
      
      if (ad.linkUrl) {
        window.open(ad.linkUrl, '_blank');
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

  // 页面初始化
  useEffect(() => {
    if (id) {
      
      // 滚动到页面顶部
      window.scrollTo(0, 0);
      fetchCollectionDetail(); // 会自动获取分类相关广告
      fetchCollectionVideos();
      fetchTextLinkAds();
    }
  }, [id]);

  // 监听广告状态变化
  useEffect(() => {

  }, [detailTopAds, detailBottomAds]);

  // 监听窗口大小变化
  useEffect(() => {
    const handleResize = () => {
      setIsDesktop(window.innerWidth >= 768);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 格式化日期
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return `${date.getFullYear()} 年 ${String(date.getMonth() + 1).padStart(2, '0')} 月 ${String(date.getDate()).padStart(2, '0')} 日`;
  };



  if (!collection && !loading) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <div className="text-white text-xl">合集不存在</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen text-white" style={{ backgroundColor: '#2C2A2A' }}>
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
          .detail-site-address a {
            color: #1ABC9C !important;
            text-decoration: none !important;
            transition: opacity 0.2s ease !important;
          }
          .detail-site-address a:hover {
            opacity: 0.8 !important;
          }
          .detail-site-address p,
          .detail-site-address span,
          .detail-site-address strong {
            color: #AAAAAA !important;
          }
        `
      }} />


      {/* 主内容区域 */}
      <div className="container mx-auto px-4 max-w-6xl pt-8 sm:pt-12">
        
        {/* 合集标题 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full text-center" style={{ maxWidth: '770px' }}>
            <h1 
              className="font-normal mb-6 leading-relaxed collection-detail-title" 
              style={{ 
                color: 'rgb(188, 188, 188)',
                fontSize: '36px',
                fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                lineHeight: '41px',
                fontWeight: '300',
                fontStyle: 'normal',
                wordBreak: 'break-word',
                overflowWrap: 'break-word',
                hyphens: 'auto'
              }}
            >
              {collection?.title || ''}
            </h1>
            
            {/* 合集小标题/描述信息 */}
            <div 
              className="collection-meta-info" 
              style={{ 
                fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                fontSize: '16px',
                lineHeight: '18px',
                fontWeight: '400',
                fontStyle: 'normal',
                color: 'rgb(188, 188, 188)'
              }}
            >
              {/* 作者、日期、视频数量信息 - 响应式布局，自适应居中 */}
              <div className="flex flex-col items-center justify-center space-y-1 text-center">
                {/* 第一行：作者和日期 */}
                <div className="flex items-center justify-center space-x-3">
                  {/* 作者 */}
                  <span style={{ color: 'rgb(188, 188, 188)' }}>
                    {collection?.author || '天涯吃瓜小慧'}
                  </span>
                  <span style={{ color: 'rgb(188, 188, 188)' }}>•</span>
                  {/* 日期 */}
                  <span style={{ color: 'rgb(188, 188, 188)' }}>{collection?.createdAt ? formatDate(collection.createdAt) : ''}</span>
                </div>
                
                {/* 第二行：视频数量信息 */}
                <div className="flex items-center justify-center">
                  <span style={{ color: 'rgb(188, 188, 188)' }}>{collection?.videoCount || 0} 个视频</span>
                </div>
              </div>
            </div>
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
                      <div className="relative shadow-lg overflow-hidden">
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt="横幅广告"
                          priority="high"
                          lazyLoad={false}
                          objectFit="contain"
                          imageStyle={{ maxHeight: '120px' }}
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
            
            {/* 网站站点地址区域 - 只有API配置时才显示 */}
            {siteAddressContent && (
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
                    __html: siteAddressHtml
                  }}
                />
              </div>
            )}
          </div>
        </div>

        {/* 合集视频列表区域 - 替换原来的富文本内容区域 */}
        {collectionVideos.length > 0 && (
          <div className="flex justify-center mb-8 px-1 md:px-4">
            <div className="w-full" style={{ maxWidth: '770px' }}>
              {/* 单列布局，每行一个视频，完全按照图片样式 */}
              <div className="space-y-8">
                {collectionVideos.map((video, index) => (
                  <div key={video.id || index} className="group">
                    {/* TOP排名标识 - 左上角 */}
                    <div className="mb-4">
                      <span className="text-white text-lg font-bold">
                        TOP {index + 1} 🔥🔥🔥
                      </span>
                    </div>
                    
                    {/* 圆角按钮样式的标题栏 */}
                    <div 
                      className="cursor-pointer mb-4 px-6 py-3 rounded-full border border-gray-500 hover:border-gray-400 transition-colors duration-200 group"
                      onClick={() => {
                        // 传递合集的分类ID作为上下文（如果有多个分类，只取第一个）
                        let categoryParam = '';
                        if (collection?.categoryId) {
                          const firstCategoryId = String(collection.categoryId).split(',')[0].trim();
                          categoryParam = `?fromCategory=${firstCategoryId}`;
                        }
                        navigate(`/video/${video.id}${categoryParam}`);
                      }}
                      style={{ backgroundColor: 'rgba(0, 0, 0, 0.3)' }}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-white text-sm flex-1 truncate pr-4">
                          {video.title}
                        </span>
                        <span className="text-gray-300 text-sm flex-shrink-0 group-hover:text-white transition-colors duration-200">
                          点击查看详情 →
                        </span>
                      </div>
                    </div>
                    
                    {/* 单张图片显示 */}
                    <div className="mb-4">
                      <div 
                        className="w-full relative overflow-hidden rounded cursor-pointer hover:scale-[1.02] transition-transform duration-300"
                        onClick={() => {
                          // 传递合集的分类ID作为上下文（如果有多个分类，只取第一个）
                          let categoryParam = '';
                          if (collection?.categoryId) {
                            const firstCategoryId = String(collection.categoryId).split(',')[0].trim();
                            categoryParam = `?fromCategory=${firstCategoryId}`;
                          }
                          navigate(`/video/${video.id}${categoryParam}`);
                        }}
                        style={{ aspectRatio: '16/9' }}
                      >
                        <SecureDecryptedImage
                          src={video.coverUrl || video.coverImage}
                          alt={video.title}
                          className="w-full h-full object-cover"
                          lazyLoad={false}
                        >
                          <div></div>
                        </SecureDecryptedImage>
                      </div>
                    </div>
                    
                    {/* 简要描述已移除 */}
                    
                    {/* 虚线分隔 - 除了最后一个 */}
                    {index < collectionVideos.length - 1 && (
                      <div 
                        className="border-t border-dashed border-gray-500 my-8"
                        style={{ borderColor: '#666' }}
                      ></div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* 标签和最后编辑时间区域 */}
        <div className="flex justify-center mb-8 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            {/* 标签区域 */}
            <div className="flex flex-wrap gap-3 mb-6">
              {/* 使用合集相关的默认标签 */}
              {['合集', '视频集合', '专题', '系列', '精选'].map((tagName, index) => (
                <button
                  key={index}
                  className="px-4 py-1 hover:bg-gray-600 text-gray-300 hover:text-white rounded-full text-sm transition-colors duration-200 cursor-pointer"
                  style={{ backgroundColor: '#343232' }}
                >
                  {tagName}
                </button>
              ))}
            </div>

            {/* 最后编辑时间 */}
            <div className="text-right text-sm mb-6" style={{ color: '#888' }}>
              最后编辑于: {new Date().toLocaleDateString('zh-CN', { 
                year: 'numeric', 
                month: '2-digit', 
                day: '2-digit' 
              })}
            </div>
          </div>
        </div>

        {/* 热门推荐区域 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <HotRecommended />
        </div>

        {/* 官方公告区域 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <OfficialNotice categoryType="公告" />
          </div>
        </div>

        {/* 文章导航区域 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <ArticleNavigation video={collection} />
          </div>
        </div>

        {/* 详情底部横幅广告 */}
        {detailBottomAds.length > 0 && (
          <div className="mb-6 md:mb-12" style={{ backgroundColor: '#2C2A2A' }}>
            <div className="flex justify-center py-3 md:py-6 px-1 md:px-4">
              <div className="w-full" style={{ maxWidth: '770px' }}>
                <div className="space-y-2 md:space-y-4">
                  {detailBottomAds.map((ad, index) => (
                    <div 
                      key={ad.id || index}
                      className="cursor-pointer"
                      onClick={() => handleBannerAdClick(ad)}
                    >
                      <div className="relative overflow-hidden shadow-md">
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt="详情页底部横幅广告"
                          priority="high"
                          lazyLoad={false}
                          objectFit="contain"
                          imageStyle={{ maxHeight: '120px' }}
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
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <CommentSection videoId={id} commentType="video" />
          </div>
        </div>

      </div>
    </div>
  );
};

export default CollectionDetail; 
