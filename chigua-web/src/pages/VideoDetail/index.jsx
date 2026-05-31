import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import ReactDOM from 'react-dom';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../../utils/hlsUtils';
import blobImageDecryption from '../../utils/blobImageDecryption';
import videoService from '../../services/videoService';
import videoStatsService from '../../services/videoStatsService';
import advertisementService from '../../services/advertisementService';
import apiCacheService from '../../services/apiCacheService';
import statsTracker from '../../utils/statsTracker';

import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';
import HotRecommended from '../../components/HotRecommended';
import ArticleNavigation from '../../components/ArticleNavigation';
import LogoAds from '../../components/LogoAds';
import CommentSection from '../../components/Comment';
import Footer from '../../components/common/Footer';
import { usePageConfig } from '../../hooks/usePageConfig';
import OfficialNotice from '../../components/OfficialNotice';
import richTextImageProcessor from '../../utils/richTextImageProcessor';
import useIsDesktop from '../../hooks/useIsDesktop';

// SEO专用固定域名（用于canonical URL和结构化数据）
const SITE_DOMAIN = 'https://tycg8.com';

const VideoDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  
  // 🚀 获取预加载数据
  const getPreloadData = () => {
    // 1. 优先从Router State获取
    const routerPreload = location.state?.preloadData;
    if (routerPreload && routerPreload.id === parseInt(id)) {
      return routerPreload;
    }
    
    // 2. 从SessionStorage获取（处理刷新情况）
    try {
      const cached = sessionStorage.getItem(`video_preload_${id}`);
      if (cached) {
        const preloadData = JSON.parse(cached);
        // 检查数据是否过期（5分钟内有效）
        if (Date.now() - preloadData.timestamp < 5 * 60 * 1000) {
          return preloadData;
        } else {
          // 清理过期数据
          sessionStorage.removeItem(`video_preload_${id}`);
        }
      }
    } catch (e) {
      // 忽略解析错误
    }
    
    return null;
  };
  
  const preloadData = getPreloadData();
  const [video, setVideo] = useState(preloadData);
  const [detailTopAds, setDetailTopAds] = useState([]);
  const [detailBottomAds, setDetailBottomAds] = useState([]);
  const [adsLoading, setAdsLoading] = useState(true);

  // 初始化时同步获取预期广告数量，用于渲染正确数量的占位图
  // 优先读 apiCacheService 内存缓存（同页面跳转命中），其次读 localStorage（刷新后保留），默认 1
  const getInitialTopAdCount = () => {
    try {
      // 尝试从 URL 或 preloadData 推断 categoryId
      const urlParams = new URLSearchParams(window.location.search);
      const fromCategoryId = urlParams.get('fromCategory');
      const categoryId = fromCategoryId
        ? parseInt(fromCategoryId)
        : (preloadData?.categories?.[0]?.id ?? null);

      if (categoryId) {
        const cacheKey = apiCacheService.generateCacheKey('ADS_DETAIL_PAGE', { categoryId });
        const cached = apiCacheService.cache.get(cacheKey);
        if (cached?.data?.data?.topAds?.length > 0) {
          return cached.data.data.topAds.length;
        }
      }
    } catch (_) {}
    // 回落到 localStorage 记录的上次数量
    try {
      const saved = parseInt(localStorage.getItem('detail_top_ad_count') || '0', 10);
      if (saved > 0) return saved;
    } catch (_) {}
    return 1;
  };

  const [topAdCount, setTopAdCount] = useState(getInitialTopAdCount);
  const [textLinkAds, setTextLinkAds] = useState([]);
  const [selectedAdId, setSelectedAdId] = useState(null);

  const [loading, setLoading] = useState(!preloadData); // 🚀 有预加载数据时不显示loading
  const [shareClicked, setShareClicked] = useState(false);
  const [processedRememberAddress, setProcessedRememberAddress] = useState(null);
  const richTextRef = useRef(null);
  
  // 响应式标题字体大小状态
  const [titleFontSize, setTitleFontSize] = useState(window.innerWidth <= 768 ? '22px' : '36px');
  const [titleLineHeight, setTitleLineHeight] = useState(window.innerWidth <= 768 ? '27px' : '41px');

  const isDesktop = useIsDesktop();
  
  // 视频统计状态 - 已优化：移除无用的显示状态，仅保留统计记录功能
  // const [videoStats, setVideoStats] = useState({
  //   viewCount: 0,
  //   playCount: 0,
  //   likeCount: 0,
  //   commentCount: 0,
  //   shareCount: 0
  // });
  const [hasTrackedPlay, setHasTrackedPlay] = useState(false);
  
  // 使用useRef来防止React严格模式下的重复调用
  const playTrackingRef = useRef(false);
  // 正在执行的请求ref，防止并发请求
  const playRequestRef = useRef(false);
  
  // 图片预览状态
  const previewImagesRef = useRef([]);
  const [imagePreview, setImagePreview] = useState({
    isOpen: false,
    index: 0,
    fallbackSrc: '',
    fallbackAlt: '',
    fallbackTitle: ''
  });

  // Viewport height for fullscreen preview (foldable / mobile browser UI safe)
  const [viewportH, setViewportH] = useState(
    (typeof window !== 'undefined' && window.visualViewport && window.visualViewport.height) ||
    (typeof window !== 'undefined' && window.innerHeight) || 0
  );
  const prevOverflowRef = useRef('');

  useEffect(() => {
    const updateVH = () => {
      try {
        const h = (window.visualViewport && window.visualViewport.height) || window.innerHeight;
        if (h && Math.abs(h - viewportH) > 1) setViewportH(h);
      } catch (_) {}
    };

    if (imagePreview.isOpen) {
      // lock scroll
      try {
        prevOverflowRef.current = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
      } catch (_) {}

      updateVH();
      window.addEventListener('resize', updateVH);
      window.addEventListener('orientationchange', updateVH);
      if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', updateVH);
      }
      return () => {
        // unlock scroll
        try { document.body.style.overflow = prevOverflowRef.current || ''; } catch (_) {}
        window.removeEventListener('resize', updateVH);
        window.removeEventListener('orientationchange', updateVH);
        if (window.visualViewport) {
          try { window.visualViewport.removeEventListener('resize', updateVH); } catch (_) {}
        }
      };
    }
  }, [imagePreview.isOpen, viewportH]);

  // 静默控制台：在布局阶段先于其它副作用执行，屏蔽所有 console 输出
  // 🛡️ 安全映射表：避免在DOM中暴露原始URL
  const [urlMappings] = useState(new Map());

  // 🔐 生成安全ID并存储URL映射
  const generateSecureId = (originalUrl) => {
    const secureId = `secure_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    urlMappings.set(secureId, originalUrl);
    return secureId;
  };

  // 🔍 根据安全ID获取原始URL
  const getOriginalUrl = (secureId) => {
    return urlMappings.get(secureId);
  };



  // 🧹 清理映射表，防止内存泄漏
  useEffect(() => {
    return () => {
      urlMappings.clear();
    };
  }, [urlMappings]);

  // 📱 响应式标题字体大小处理
  useEffect(() => {
    const handleResize = () => {
      const isMobile = window.innerWidth <= 768;
      setTitleFontSize(isMobile ? '22px' : '36px');
      setTitleLineHeight(isMobile ? '27px' : '41px');
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 获取页面配置
  const { configs, getConfig } = usePageConfig();
  



  
  // 移除渐进式加载相关状态

  // 获取视频统计数据 - 已优化：移除无用的统计数据获取
  // const fetchVideoStats = async (videoId) => {
  //   try {
  //     const statsResponse = await videoStatsService.getVideoStats(videoId);
  //     
  //     if (statsResponse.code === 200 && statsResponse.data) {
  //       setVideoStats(statsResponse.data);
  //     } else {
  //       // 设置默认统计数据
  //       setVideoStats({
  //         videoId: videoId,
  //         viewCount: 0,
  //         playCount: 0,
  //         likeCount: 0,
  //         commentCount: 0,
  //         shareCount: 0
  //       });
  //     }
  //   } catch (error) {
  //   }
  // };

  // 增加浏览量统计（使用全局跟踪器防重复调用）
  const trackViewCount = useCallback(async (videoId) => {
    // 使用全局统计跟踪器防止React严格模式下的重复调用
    if (!statsTracker.startTracking(videoId, 'view')) {
      return;
    }
    
    try {
      const response = await videoStatsService.incrementViewCount(videoId);
      
      if (response.code === 200) {
        
        // 标记全局跟踪器统计成功
        statsTracker.finishTracking(videoId, 'view', true);
        
        // 已优化：移除本地状态更新，仅保留后端统计记录
        // setVideoStats(prev => ({
        //   ...prev,
        //   viewCount: (prev.viewCount || 0) + 1
        // }));
      } else {
        // 标记全局跟踪器统计失败
        statsTracker.finishTracking(videoId, 'view', false);
        // no-op
      }
    } catch (error) {
      // 标记全局跟踪器统计失败
      statsTracker.finishTracking(videoId, 'view', false);
      // no-op
    }
  }, []);


  // 获取视频详情（带重试机制和缓存）
  const fetchVideoDetail = async (retryCount = 0) => {
    const maxRetries = 2; // 最多重试2次
    
    try {
      // 🚀 如果没有预加载数据才显示loading
      if (!preloadData) {
        setLoading(true);
      }

      // 🔍 获取URL查询参数（fromCategory, searchKeyword, sortType）
      const urlParams = new URLSearchParams(location.search);
      const contextParams = {};
      if (urlParams.has('fromCategory')) {
        contextParams.fromCategory = urlParams.get('fromCategory');
      }
      if (urlParams.has('searchKeyword')) {
        contextParams.searchKeyword = urlParams.get('searchKeyword');
      }
      if (urlParams.has('sortType')) {
        contextParams.sortType = urlParams.get('sortType');
      }

      // 使用API缓存服务获取视频详情，传递上下文参数
      const response = await apiCacheService.getApiData(
        'VIDEO_DETAIL',
        { id, ...contextParams },
        (params) => videoService.getVideoDetail(params.id, params),
        { 
          cacheDuration: 5 * 60 * 1000, // 5分钟缓存
          forceRefresh: retryCount > 0 // 重试时强制刷新缓存
        }
      );
      
      if (response.code === 200) {
        setVideo(response.data);
        
        // 🚀 并行执行统计和广告加载，不阻塞页面渲染
        const urlParams = new URLSearchParams(location.search);
        const fromCategoryId = urlParams.get('fromCategory');
        
        let categoryId = null;
        if (fromCategoryId) {
          categoryId = parseInt(fromCategoryId);
        } else if (response.data?.categories && response.data.categories.length > 0) {
          categoryId = response.data.categories[0].id;
        }
        
        // 广告立即并行加载（不等浏览器空闲），统计放到空闲时执行
        (categoryId ? fetchDetailPageAds(categoryId) : fetchDetailPageAds(null)).catch(() => {});

        // 统计浏览量放到空闲时执行（不影响显示）
        const runStats = () => { trackViewCount(id).catch(() => {}); };
        if (window.requestIdleCallback) {
          window.requestIdleCallback(runStats, { timeout: 500 });
        } else {
          setTimeout(runStats, 0);
        }
        
        // 🎯 立即停止loading，让页面开始渲染
        // 不等待统计和广告加载完成
      } else {

        
        // 对于非200响应也进行重试
        if (retryCount < maxRetries) {

                  // 🚀 如果没有预加载数据才设置loading
        if (!preloadData) {
          setLoading(false);
        }
        setTimeout(() => {
          fetchVideoDetail(retryCount + 1);
        }, 1000 * (retryCount + 1));
          return;
        }
        
        // 设置错误状态，让用户知道加载失败
        setVideo(null);
      }
    } catch (error) {

      
      // 重试机制：如果还有重试次数，则延迟后重试
      if (retryCount < maxRetries) {

        // 🚀 如果没有预加载数据才设置loading
        if (!preloadData) {
          setLoading(false); // 暂时停止loading
        }
        setTimeout(() => {
          fetchVideoDetail(retryCount + 1);
        }, 1000 * (retryCount + 1)); // 递增延迟：1s, 2s
        return;
      }
      
      // 所有重试都失败了

      setVideo(null);
    } finally {
      // 只有在不重试的情况下才设置loading为false
      // 注意：如果在else或catch块中已经return了，这里不会执行
      setLoading(false);
    }
  };

    // 获取详情页面广告（顶部+底部，根据视频分类，使用缓存）
  const fetchDetailPageAds = async (categoryId) => {
    try {
      
      if (!categoryId) {
        
        // 如果没有分类ID，使用通用广告作为备选（使用缓存）
        const [topResponse, bottomResponse] = await Promise.all([
          apiCacheService.getApiData(
            'ADS_POSITION',
            { position: '3' },
            (params) => advertisementService.getAdsByPosition(params.position),
            { cacheDuration: 5 * 60 * 1000 }
          ),
          apiCacheService.getApiData(
            'ADS_POSITION',
            { position: '4' },
            (params) => advertisementService.getAdsByPosition(params.position),
            { cacheDuration: 5 * 60 * 1000 }
          )
        ]);
        
        const topAdsNoCategory = topResponse.code === 200 ? (topResponse.data || []) : [];
        if (topResponse.code === 200) {
          setDetailTopAds(topAdsNoCategory);
          if (topAdsNoCategory.length > 0) {
            setTopAdCount(topAdsNoCategory.length);
            try { localStorage.setItem('detail_top_ad_count', String(topAdsNoCategory.length)); } catch (_) {}
          }
        }
        if (bottomResponse.code === 200) {
          setDetailBottomAds(bottomResponse.data || []);
        }
        setAdsLoading(false);
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
        if (topAds.length > 0) {
          setTopAdCount(topAds.length);
          try { localStorage.setItem('detail_top_ad_count', String(topAds.length)); } catch (_) {}
        }
      } else {
        setDetailTopAds([]);
        setDetailBottomAds([]);
      }
      setAdsLoading(false);
    } catch (error) {
      setDetailTopAds([]);
      setDetailBottomAds([]);
      setAdsLoading(false);
    }
  };

  // 获取文字链接广告（使用缓存）
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
    setSelectedAdId(ad.id);
    await handleAdClick(ad);
  };

  // 分享点击
  const handleShareClick = async () => {
    if (!video) return;
    const shareTitle = video.title || '精彩视频';
    const shareUrl = window.location.href;
    const shareContent = `${shareTitle}\n\n${shareUrl}\n\n这个视频 你必须看！简直太炸裂了！\n\n复制上面的链接用浏览器打开即可马上吃瓜！`;
    try {
      await navigator.clipboard.writeText(shareContent);
      try { await videoStatsService.incrementShareCount(id); } catch (_) {}
      setShareClicked(true);
      setTimeout(() => setShareClicked(false), 3000);
    } catch (_) {
      try {
        const textArea = document.createElement('textarea');
        textArea.value = shareContent;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        try { await videoStatsService.incrementShareCount(id); } catch (_) {}
        setShareClicked(true);
        setTimeout(() => setShareClicked(false), 3000);
      } catch (_) {}
    }
  };



  // 处理图片预览
  const handleImageClick = useCallback((index) => {
    const list = previewImagesRef.current;
    if (!Array.isArray(list) || list.length === 0) return;
    const total = list.length;
    if (total === 0) return;
    const normalized = ((index % total) + total) % total;
    const item = list[normalized] || {};
    setImagePreview({
      isOpen: true,
      index: normalized,
      fallbackSrc: item.src || '',
      fallbackAlt: item.alt || '图片预览',
      fallbackTitle: item.title || ''
    });
  }, []);

  // 关闭图片预览
  const closeImagePreview = useCallback(() => {
    setImagePreview({
      isOpen: false,
      index: 0,
      fallbackSrc: '',
      fallbackAlt: '',
      fallbackTitle: ''
    });
  }, []);

  // 为页面中所有内容区域的图片添加点击事件
  const addImageClickListeners = useCallback(() => {
    // 事件绑定保护机制已移除

    setTimeout(() => {
      const containerSelectors = [
        '.rich-text-content',
        '.remember-address'
      ];

      const newPreviewList = [];

      containerSelectors.forEach((selector) => {
        const container = document.querySelector(selector);
        if (!container) return;

        const images = container.querySelectorAll('img');
        images.forEach((img) => {
          // 移除之前的监听器（避免重复绑定）
          if (img._clickHandler) {
            img.removeEventListener('click', img._clickHandler);
            img._clickHandler = null;
          }

          let src = img.src;
          if (src && src.includes('data:image/gif;base64,')) {
            const secureId = img.getAttribute('data-secure-id');
            src = getOriginalUrl(secureId) || '';
          }

          if (!src || src.includes('data:image/gif;base64,')) {
            img.style.cursor = 'default';
            return;
          }

          if (!isDesktop) {
            img.style.cursor = 'default';
            img.removeAttribute('data-preview-index');
            return;
          }

          const alt = img.getAttribute('alt') || img.title || '图片预览';
          const title = img.title || img.getAttribute('data-title') || '';
          const previewIndex = newPreviewList.length;
          newPreviewList.push({ src, alt, title });

          const clickHandler = (e) => {
            e.preventDefault();
            e.stopPropagation();
            handleImageClick(previewIndex);
          };

          img._clickHandler = clickHandler;
          img.addEventListener('click', clickHandler);
          img.setAttribute('data-preview-index', String(previewIndex));
          img.style.cursor = 'pointer';
          if (!img.title) {
            img.title = '点击查看大图';
          }
          img.setAttribute('data-event-bound', 'true');
        });
      });

      previewImagesRef.current = isDesktop ? newPreviewList : [];
    }, 100);
  }, [handleImageClick, getOriginalUrl, isDesktop]);

  // 增加播放量统计（使用全局跟踪器防重复调用）
  const trackPlayCount = useCallback(async (videoId) => {
    // 使用全局统计跟踪器防止React严格模式下的重复调用
    if (!statsTracker.startTracking(videoId, 'play')) {
      return;
    }
    
    try {
      // 设置本地状态
      setHasTrackedPlay(true);
      
      const response = await videoStatsService.incrementPlayCount(videoId);
      
      if (response.code === 200) {
        
        // 标记全局跟踪器统计成功
        statsTracker.finishTracking(videoId, 'play', true);
        
        // 已优化：移除本地状态更新，仅保留后端统计记录
        // setVideoStats(prev => ({
        //   ...prev,
        //   playCount: (prev.playCount || 0) + 1
        // }));
      } else {
        // 标记全局跟踪器统计失败
        statsTracker.finishTracking(videoId, 'play', false);
        setHasTrackedPlay(false);
      }
    } catch (error) {
      // 标记全局跟踪器统计失败
      statsTracker.finishTracking(videoId, 'play', false);
      setHasTrackedPlay(false);
    }
  }, []);

  // 立即重置滚动位置 - 在组件开始渲染时
  useEffect(() => {
    // 强制立即滚动到顶部，不等待任何异步操作
    const resetScroll = () => {
      window.scrollTo(0, 0);
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    };
    
    resetScroll();
    // 再次确保滚动位置
    setTimeout(resetScroll, 0);
  }, []);

  // 页面初始化
  useEffect(() => {
    if (id) {

      
      // 再次强制重置滚动位置
      window.scrollTo(0, 0);
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
      
      // 重置统计状态
      statsTracker.resetVideo(id); // 重置全局跟踪器中的视频状态
      playTrackingRef.current = false;
      playRequestRef.current = false;
      setHasTrackedPlay(false);
      // 已优化：移除无用的状态重置
      // setVideoStats({
      //   viewCount: 0,
      //   playCount: 0,
      //   likeCount: 0,
      //   commentCount: 0,
      //   shareCount: 0
      // });
      
      fetchVideoDetail(); // 会自动获取分类相关广告和浏览量统计
      // 注意：浏览量统计已在 trackViewCount 中处理，无需重复调用 trackPageView
      fetchTextLinkAds();
    }
  }, [id]);

  // SEO优化：设置页面meta标签和结构化数据（参考91porna.com结构）
  useEffect(() => {
    if (!video) return;
    
    // 辅助函数：更新或创建meta标签
    const updateMetaTag = (name, content, attribute = 'name') => {
      if (!content) return;
      
      let tag = document.querySelector(`meta[${attribute}="${name}"]`);
      if (!tag) {
        tag = document.createElement('meta');
        tag.setAttribute(attribute, name);
        document.head.appendChild(tag);
      }
      tag.setAttribute('content', content);
    };
    
    // 辅助函数：更新或创建link标签（用于canonical等）
    const updateLinkTag = (rel, href) => {
      if (!href) return;
      
      let tag = document.querySelector(`link[rel="${rel}"]`);
      if (!tag) {
        tag = document.createElement('link');
        tag.setAttribute('rel', rel);
        document.head.appendChild(tag);
      }
      tag.setAttribute('href', href);
    };
    
    // 辅助函数：格式化时长（ISO 8601格式：PT5M30S）
    const formatDuration = (seconds) => {
      if (!seconds) return '';
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;
      let duration = 'PT';
      if (hours > 0) duration += `${hours}H`;
      if (minutes > 0) duration += `${minutes}M`;
      if (secs > 0) duration += `${secs}S`;
      return duration;
    };
    
    // 辅助函数：生成关键词字符串
    const generateKeywords = () => {
      const keywords = [];
      
      // 1. 使用metaKeywords（如果存在）
      if (video.metaKeywords) {
        try {
          // 如果是JSON数组，解析它
          if (typeof video.metaKeywords === 'string' && video.metaKeywords.startsWith('[')) {
            const parsed = JSON.parse(video.metaKeywords);
            if (Array.isArray(parsed)) {
              keywords.push(...parsed);
            }
          } else if (typeof video.metaKeywords === 'string') {
            // 如果是逗号分隔的字符串
            keywords.push(...video.metaKeywords.split(',').map(k => k.trim()));
          }
        } catch (e) {
          // 解析失败，当作普通字符串处理
          keywords.push(video.metaKeywords);
        }
      }
      
      // 2. 从标题中提取关键词
      if (video.title) {
        // 提取标题中的关键词（去除常见停用词）
        const titleWords = video.title.split(/[\s\-_]+/).filter(word => 
          word.length > 1 && 
          !['的', '了', '和', '是', '在', '有', '就', '不', '人', '都', '一', '一个', '上', '也', '很', '到', '说', '要', '去', '你', '会', '着', '没有', '看', '好', '自己', '这'].includes(word)
        );
        keywords.push(...titleWords);
      }
      
      // 3. 添加分类名称（如果有）
      if (video.categoryName) {
        keywords.push(video.categoryName);
      }
      
      // 4. 添加标签（如果有）
      if (video.tags && Array.isArray(video.tags)) {
        video.tags.forEach(tag => {
          if (tag.name) keywords.push(tag.name);
        });
      }
      
      // 5. 添加通用关键词（确保"吃瓜"关键词优先）
      keywords.push('吃瓜', '吃瓜网', '吃瓜网站', '天涯吃瓜', '每日吃瓜', '最新吃瓜', '热门吃瓜', '吃瓜资讯', '吃瓜必备', '黑料');
      
      // 去重并限制长度
      const uniqueKeywords = [...new Set(keywords)].filter(k => k && k.length > 0);
      return uniqueKeywords.slice(0, 20).join(', '); // 最多20个关键词
    };
    
    // 辅助函数：生成面包屑数据
    const generateBreadcrumbList = () => {
      const items = [
        {
          "@type": "ListItem",
          "position": 1,
          "name": "首页",
          "item": window.location.origin
        }
      ];
      
      if (video.categoryName && video.categoryId) {
        items.push({
          "@type": "ListItem",
          "position": 2,
          "name": video.categoryName,
          "item": `${window.location.origin}/category/${video.categoryId}`
        });
      }
      
      items.push({
        "@type": "ListItem",
        "position": items.length + 1,
        "name": video.title,
        "item": `${SITE_DOMAIN}${window.location.pathname}`
      });
      
      return {
        "@context": "https://schema.org",
        "@type": "BreadcrumbList",
        "itemListElement": items
      };
    };
    
    // 辅助函数：生成视频描述（与meta description保持一致的回退逻辑）
    const generateVideoDescription = (videoData) => {
      // 优先使用metaDescription，然后是description，最后使用默认描述
      return videoData.metaDescription || 
             videoData.description || 
             `${videoData.title} - 吃瓜 - 每日吃瓜，每日更新最新最全的吃瓜资讯！`;
    };
    
    // 辅助函数：插入结构化数据
    const insertStructuredData = (videoData) => {
      // 移除旧的structured data
      const oldScripts = [
        document.getElementById('video-structured-data'),
        document.getElementById('article-structured-data'),
        document.getElementById('breadcrumb-structured-data')
      ];
      oldScripts.forEach(script => {
        if (script) script.remove();
      });
      
      // 格式化发布时间
      const uploadDate = videoData.publishedAt 
        ? new Date(videoData.publishedAt).toISOString() 
        : '';
      const modifiedDate = videoData.updatedAt 
        ? new Date(videoData.updatedAt).toISOString() 
        : uploadDate;
      
      // 生成描述（确保始终有有效内容，符合Google结构化数据要求）
      const videoDescription = generateVideoDescription(videoData);
      
      // 1. VideoObject结构化数据
      const videoStructuredData = {
        "@context": "https://schema.org",
        "@type": "VideoObject",
        "name": videoData.title,
        "description": videoDescription,
        "thumbnailUrl": videoData.coverImageUrl || '',
        "uploadDate": uploadDate,
        "duration": formatDuration(videoData.duration),
        "contentUrl": videoData.videoUrl || `${SITE_DOMAIN}${window.location.pathname}`,
        "embedUrl": `${SITE_DOMAIN}${window.location.pathname}`,
        "url": `${SITE_DOMAIN}${window.location.pathname}`,
        "publisher": {
          "@type": "Organization",
          "name": "每日吃瓜",
          "logo": {
            "@type": "ImageObject",
            "url": `${window.location.origin}/logo.png`
          }
        },
        "author": videoData.author ? {
          "@type": "Person",
          "name": videoData.author
        } : {
          "@type": "Organization",
          "name": "每日吃瓜"
        }
      };
      
      // 2. Article结构化数据（参考91porna.com）
      const articleStructuredData = {
        "@context": "https://schema.org",
        "@type": "Article",
        "headline": videoData.title,
        "description": videoDescription,
        "image": videoData.coverImageUrl ? [videoData.coverImageUrl] : [],
        "datePublished": uploadDate,
        "dateModified": modifiedDate,
        "author": videoData.author ? {
          "@type": "Person",
          "name": videoData.author
        } : {
          "@type": "Organization",
          "name": "每日吃瓜"
        },
        "publisher": {
          "@type": "Organization",
          "name": "每日吃瓜",
          "logo": {
            "@type": "ImageObject",
            "url": `${window.location.origin}/logo.png`
          }
        },
        "mainEntityOfPage": {
          "@type": "WebPage",
          "@id": `${SITE_DOMAIN}${window.location.pathname}`
        }
      };
      
      // 添加分类信息
      if (videoData.categoryName) {
        articleStructuredData.articleSection = videoData.categoryName;
      }
      
      // 添加标签
      if (videoData.tags && Array.isArray(videoData.tags) && videoData.tags.length > 0) {
        articleStructuredData.keywords = videoData.tags.map(tag => tag.name).join(', ');
      }
      
      // 3. 面包屑结构化数据
      const breadcrumbData = generateBreadcrumbList();
      
      // 插入所有结构化数据
      const scripts = [
        { id: 'video-structured-data', data: videoStructuredData },
        { id: 'article-structured-data', data: articleStructuredData },
        { id: 'breadcrumb-structured-data', data: breadcrumbData }
      ];
      
      scripts.forEach(({ id, data }) => {
        const script = document.createElement('script');
        script.id = id;
        script.type = 'application/ld+json';
        script.textContent = JSON.stringify(data);
        document.head.appendChild(script);
      });
    };
    
    // 生成关键词
    const keywords = generateKeywords();
    
    // 1. 设置页面标题（确保"吃瓜"关键词突出，参考91porna.com格式）
    const pageTitle = video.metaTitle || `${video.title} - 吃瓜 - 每日吃瓜`;
    document.title = pageTitle;
    
    // 2. 设置或更新 meta description（确保包含"吃瓜"关键词）
    const description = video.metaDescription || video.description || '吃瓜 - 每日吃瓜，每日更新最新最全的吃瓜资讯！';
    updateMetaTag('description', description);
    
    // 3. 设置 keywords meta标签（参考91porna.com）
    if (keywords) {
      updateMetaTag('keywords', keywords);
    }
    
    // 4. 设置 author meta标签（固定为站点名，不使用数据库中的用户名）
    updateMetaTag('author', '每日吃瓜');
    
    // 5. 设置 Open Graph 标签（增强版）
    // 规范化URL：使用固定域名，移除查询参数，避免重复内容问题
    const canonicalUrl = `${SITE_DOMAIN}${window.location.pathname}`;
    
    updateMetaTag('og:title', video.title, 'property');
    updateMetaTag('og:description', description, 'property');
    updateMetaTag('og:image', video.coverImageUrl || '', 'property');
    updateMetaTag('og:url', canonicalUrl, 'property');
    updateMetaTag('og:type', 'video.other', 'property');
    updateMetaTag('og:site_name', '每日吃瓜', 'property');
    updateMetaTag('og:locale', 'zh_CN', 'property');
    
    // 6. 设置 Twitter 卡片（参考91porna.com）
    updateMetaTag('twitter:card', 'summary_large_image');
    updateMetaTag('twitter:title', video.title);
    updateMetaTag('twitter:description', description);
    updateMetaTag('twitter:image', video.coverImageUrl || '');
    
    // 7. 设置canonical链接（使用规范化URL）
    updateLinkTag('canonical', canonicalUrl);
    
    // 8. 插入结构化数据（JSON-LD）
    insertStructuredData(video);
    
    // 清理函数：组件卸载时恢复默认标题和meta标签
    return () => {
      document.title = '天涯吃瓜';
      
      // 移除结构化数据
      const oldScripts = [
        document.getElementById('video-structured-data'),
        document.getElementById('article-structured-data'),
        document.getElementById('breadcrumb-structured-data')
      ];
      oldScripts.forEach(script => {
        if (script) script.remove();
      });
    };
  }, [video]);

  // 处理键盘事件（ESC关闭预览）
  const showRelativePreview = useCallback((delta) => {
    setImagePreview((prev) => {
      if (!prev.isOpen) return prev;
      const list = previewImagesRef.current;
      if (!Array.isArray(list) || list.length === 0) return prev;
      if (list.length === 1) return prev;
      const total = list.length;
      const nextIndex = ((prev.index + delta) % total + total) % total;
      if (nextIndex === prev.index) return prev;
      const item = list[nextIndex] || {};
      return {
        ...prev,
        index: nextIndex,
        fallbackSrc: item.src || prev.fallbackSrc,
        fallbackAlt: item.alt || prev.fallbackAlt,
        fallbackTitle: item.title || prev.fallbackTitle
      };
    });
  }, []);

  useEffect(() => {
    if (!imagePreview.isOpen) return;

    const list = previewImagesRef.current;
    const hasMultiple = Array.isArray(list) && list.length > 1;
    const isDesktop = typeof window !== 'undefined' && window.innerWidth >= 1024;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        closeImagePreview();
        return;
      }
      if (!isDesktop || !hasMultiple) return;
      if (event.key === 'ArrowRight') {
        event.preventDefault();
        showRelativePreview(1);
      } else if (event.key === 'ArrowLeft') {
        event.preventDefault();
        showRelativePreview(-1);
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [imagePreview.isOpen, closeImagePreview, showRelativePreview]);

  useEffect(() => {
    if (!isDesktop && imagePreview.isOpen) {
      closeImagePreview();
    }
  }, [isDesktop, imagePreview.isOpen, closeImagePreview]);

  useEffect(() => {
    if (!imagePreview.isOpen) return;
    const list = previewImagesRef.current;
    if (!Array.isArray(list) || list.length <= 1) return;
    const isDesktop = typeof window !== 'undefined' && window.innerWidth >= 1024;
    if (!isDesktop) return;

    let wheelLocked = false;
    const handleWheel = (event) => {
      if (!imagePreview.isOpen) return;
      if (!Array.isArray(previewImagesRef.current) || previewImagesRef.current.length <= 1) return;
      const delta = Math.abs(event.deltaY) >= Math.abs(event.deltaX)
        ? (event.deltaY > 0 ? 1 : -1)
        : (event.deltaX > 0 ? 1 : -1);
      if (delta === 0) return;
      event.preventDefault();
      if (wheelLocked) return;
      wheelLocked = true;
      showRelativePreview(delta);
      setTimeout(() => { wheelLocked = false; }, 220);
    };

    window.addEventListener('wheel', handleWheel, { passive: false });
    return () => {
      window.removeEventListener('wheel', handleWheel);
    };
  }, [imagePreview.isOpen, showRelativePreview]);

  // 添加视频播放监听器来统计播放量
  useEffect(() => {
    if (!video || hasTrackedPlay) {
      return;
    }

    const handleVideoPlay = (event) => {
      // 立即移除所有监听器防止重复触发
      const allVideos = document.querySelectorAll('video');
      allVideos.forEach(v => {
        v.removeEventListener('play', handleVideoPlay);
        v.removeEventListener('playing', handleVideoPlay);
      });
      
      trackPlayCount(id);
    };

    // 延迟添加监听器，确保视频元素已完全加载和解密
    const timer = setTimeout(() => {
      const videos = document.querySelectorAll('video');
      
      videos.forEach((videoElement, index) => {
        // 检查视频是否已经有播放监听器标记
        if (videoElement.hasAttribute('data-play-listener')) {
          return;
        }
        
        // 标记已添加监听器
        videoElement.setAttribute('data-play-listener', 'true');
        
        // 只监听 play 事件（用户点击播放）
        videoElement.addEventListener('play', handleVideoPlay, { once: true });
      });
    }, 1500); // 增加延迟确保解密完成

    return () => {
      clearTimeout(timer);
      // 清理事件监听器和标记
      const videos = document.querySelectorAll('video');
      videos.forEach(video => {
        video.removeEventListener('play', handleVideoPlay);
        video.removeEventListener('playing', handleVideoPlay);
        video.removeAttribute('data-play-listener');
      });
    };
  }, [video, hasTrackedPlay, id, trackPlayCount]);

  // 预览关闭后重新绑定图片事件
  useEffect(() => {
    if (!imagePreview.isOpen && video && video.videoContent) {
      // 延迟绑定，确保预览关闭动画完成且DOM稳定
      const timer = setTimeout(() => {
        
        addImageClickListeners();
      }, 300);
      
      return () => clearTimeout(timer);
    }
  }, [imagePreview.isOpen, video, addImageClickListeners]);

  // 监听广告状态变化
  useEffect(() => {

  }, [detailTopAds, detailBottomAds]);

  // 格式化日期
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return `${date.getFullYear()} 年 ${String(date.getMonth() + 1).padStart(2, '0')} 月 ${String(date.getDate()).padStart(2, '0')} 日`;
  };

  // 处理富文本内容样式（后台已处理URL拼接）
  const processRichTextContent = (content) => {
    if (!content) return '';

    let processedContent = content;
    


    // 1. 统一图片样式并完全阻止原始URL加载（自适应宽度，保证完整显示）
    // 先处理重复的src属性问题
    processedContent = processedContent.replace(/src="([^"]*?)"\s+src="([^"]*?)"/gi, 'src="$1"');
    
    processedContent = processedContent.replace(/<img([^>]*?)src="([^"]*?)"([^>]*?)>/gi, (match, prefix, src, suffix) => {
      // 自适应宽度，等比缩放，保证完整显示，同时收紧上下间距
      let styleAttr = 'width: 100%; max-width: 770px; height: auto; display: block; margin: 8px auto; border-radius: 4px; object-fit: contain;';
      
      // 移除任何现有的style、width、height属性
      let cleanPrefix = prefix.replace(/style="[^"]*"/gi, '')
                             .replace(/width="[^"]*"/gi, '')
                             .replace(/height="[^"]*"/gi, '');
      let cleanSuffix = suffix.replace(/style="[^"]*"/gi, '')
                             .replace(/width="[^"]*"/gi, '')
                             .replace(/height="[^"]*"/gi, '');
      
      // 🔄 对于所有图片，都使用预加载占位图片
      // 🛡️ 使用安全ID代替直接暴露URL
      const secureId = generateSecureId(src);
      return `<img${cleanPrefix} src="/loding.jpg" data-secure-id="${secureId}"${cleanSuffix} style="${styleAttr}" class="pending-decrypt">`;
    });

    // 2. 统一视频样式并完全阻止封面加载（自适应宽度，保证完整显示）
    processedContent = processedContent.replace(/<video([^>]*?)>/gi, (match, attributes) => {
      // 确保有controls属性与自适应宽度，保证完整显示，同时收紧上下间距
      let hasControls = attributes.includes('controls');
      let controls = hasControls ? '' : ' controls';
      let styleAttr = 'width: 100%; max-width: 770px; height: auto; display: block; margin: 8px auto; border-radius: 4px; object-fit: contain;';
      
      // 🔄 处理poster属性 - 对所有poster都使用占位符阻止浏览器自动加载
      let processedAttributes = attributes.replace(/poster="([^"]*?)"/gi, (match, poster) => {
        const secureId = generateSecureId(poster);
        return `poster="/loding.jpg" data-secure-poster-id="${secureId}"`;
      });
      
      // 🔄 处理src属性 - 直接移除src，保留data-secure-id，避免about:blank请求
      processedAttributes = processedAttributes.replace(/src="([^"]*?)"/gi, (match, src) => {
        const secureId = generateSecureId(src);
        return `data-secure-id="${secureId}"`;
      });
      
      // 移除任何现有的style、width、height属性
      let cleanAttributes = processedAttributes.replace(/style="[^"]*"/gi, '')
                                             .replace(/width="[^"]*"/gi, '')
                                             .replace(/height="[^"]*"/gi, '');
      
      return `<video${cleanAttributes}${controls} style="${styleAttr}" class="pending-decrypt">`;
    });

    // 3. 处理source标签 - 阻止浏览器自动加载视频源
    processedContent = processedContent.replace(/<source([^>]*?)src="([^"]*?)"([^>]*?)>/gi, (match, prefix, src, suffix) => {
      // 🔄 对于所有视频源，移除src，保留安全ID，避免关于about:blank的错误请求
      const secureId = generateSecureId(src);
      return `<source${prefix} data-secure-id="${secureId}"${suffix} class="pending-decrypt">`;
    });

    // 4. 处理文本样式（只为没有样式的元素添加基础样式，保留后台设置的样式）
    // 只为没有style属性的段落添加基础样式
    processedContent = processedContent.replace(/<p([^>]*)>/gi, (match, attributes) => {
      if (attributes.includes('style=')) {
        // 已有样式的段落，保持原样
        return match;
      } else {
        // 没有样式的段落，添加基础样式
        return `<p${attributes} style="margin: 8px 0; color: #BCBCBC; line-height: 1.6;">`;
      }
    });
    
    // 只为没有style属性的标题添加基础样式
    processedContent = processedContent.replace(/<(h[1-6])([^>]*)>/gi, (match, tag, attributes) => {
      if (attributes.includes('style=')) {
        return match;
      } else {
        return `<${tag}${attributes} style="color: #BCBCBC; margin: 20px 0 10px 0;">`;
      }
    });
    
    // 只为没有style属性的链接添加基础样式
    processedContent = processedContent.replace(/<a([^>]*)>/gi, (match, attributes) => {
      if (attributes.includes('style=')) {
        return match;
      } else {
        return `<a${attributes} style="color: #18BD9D; text-decoration: none;">`;
      }
    });

    // 5. 清理重复的style属性
    processedContent = processedContent.replace(/style="([^"]*)" style="([^"]*)"/gi, 'style="$1; $2"');

    // 6. 移除包裹图片/视频的外层元素上的固定高度，避免出现巨大空白
    processedContent = processedContent.replace(/<(p|div)([^>]*)style="([^"]*)"([^>]*)>/gi, (match, tag, pre, style, post) => {
      const newStyle = style
        .replace(/\bheight\s*:\s*[^;]+;?/gi, '')
        .replace(/\bmin-height\s*:\s*[^;]+;?/gi, '');
        // 保留line-height、font-size、text-align、color等文本样式
      return `<${tag}${pre}style="${newStyle}"${post}>`;
    });

    // 7. 兜底：清理所有style中的固定高度属性，防止编辑器注入导致容器不收缩
    processedContent = processedContent.replace(/style="([^"]*)"/gi, (match, style) => {
      const cleaned = style
        .replace(/\bheight\s*:\s*[^;]+;?/gi, '')
        .replace(/\bmin-height\s*:\s*[^;]+;?/gi, '');
      return `style="${cleaned}"`;
    });




    return processedContent;
  };

  // 缓存富文本内容处理结果
  const memoizedRichTextContent = useMemo(() => {
    if (!video || !video.videoContent) return '';

    let processedContent = processRichTextContent(video.videoContent);
    return processedContent;
  }, [video?.videoContent, video?.id]);

  // 响应式横幅广告和文字链接处理
  useEffect(() => {
    const handleResize = () => {
      const isMobile = window.innerWidth < 768;
      const isSmallMobile = window.innerWidth < 480;
      
      // 处理横幅广告
      const bannerImages = document.querySelectorAll('img[alt="横幅广告"], img[alt="详情页底部横幅广告"]');
      bannerImages.forEach(img => {
        if (isMobile) {
          img.style.height = 'auto';
          img.style.objectFit = 'contain';
          img.style.maxHeight = 'none';
        } else {
          img.style.height = '90px';
          img.style.objectFit = 'cover';
          img.style.maxHeight = '90px';
        }
      });
      
      // 处理文字链接区域
      const textLinkSpans = document.querySelectorAll('.text-link-span');
      textLinkSpans.forEach(span => {
        if (isSmallMobile) {
          span.style.fontSize = '10px';
        } else {
          span.style.fontSize = '';
        }
      });
    };

    window.addEventListener('resize', handleResize);
    // 初始化时也执行一次
    handleResize();

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [video]);





  // 新的Blob URL图片和视频处理逻辑
  useEffect(() => {
    // 🚀 只有当获取到完整的视频数据（包含videoContent）时才处理
    if (!video || !video.videoContent) {
      return;
    }

    const processRichTextImages = async () => {
      try {
      // 等待DOM渲染完成
      setTimeout(async () => {
          const richTextContainer = document.querySelector('.rich-text-content');
          if (!richTextContainer) return;

          // 第一步：只注入占位符（不等待实际图片加载），避免阻塞视频
          const processedHtml = await richTextImageProcessor.processRichTextImages(
            richTextContainer.innerHTML,
            { placeholdersOnly: true }
          );
          richTextContainer.innerHTML = processedHtml;
          
          // 处理使用安全映射的图片为Blob URL
          const mappedImgs = richTextContainer.querySelectorAll('img[data-secure-id]');
          const imageDecryptTasks = [];
          for (const img of Array.from(mappedImgs)) {
            const task = (async () => {
              try {
                const sid = img.getAttribute('data-secure-id');
                const originalSrc = getOriginalUrl(sid);
                if (originalSrc) {
                  const blobUrl = await blobImageDecryption.decryptImageToBlob(originalSrc, { priority: 'low' });
                  img.src = blobUrl;
                  img.classList.add('decrypted-image');
                  img.style.maxWidth = '100%';
                  img.style.height = 'auto';
                  img.style.display = 'block';
                  img.style.margin = '8px auto';
                  // 清理：当图片被移除时撤销blob
                  const observer = new MutationObserver((mutations, obs) => {
                    for (const m of mutations) {
                      for (const node of Array.from(m.removedNodes)) {
                        if (node === img && blobUrl && blobUrl.startsWith('blob:')) {
                          blobImageDecryption.revokeBlobUrl(blobUrl);
                          obs.disconnect();
                          return;
                        }
                      }
                    }
                  });
                  observer.observe(richTextContainer, { childList: true, subtree: true });
                }
              } catch (e) {
                img.src = '/loding.jpg';
              }
            })();
            imageDecryptTasks.push(task);
          }
          if (imageDecryptTasks.length) {
            Promise.allSettled(imageDecryptTasks);
          }

          // 处理视频封面
          const videos = richTextContainer.querySelectorAll('video[data-secure-poster-id]');
          for (const video of videos) {
            const secureId = video.getAttribute('data-secure-poster-id');
            const originalPosterSrc = getOriginalUrl(secureId);
            if (originalPosterSrc) {
              // 视频封面异步加载，避免阻塞播放器初始化
              richTextImageProcessor
                .processVideoPoster(video, originalPosterSrc, { priority: 'high' })
                .catch(() => {});
            }
          }

          // 恢复视频源（支持m3u8和HLS.js）
          const videoEls = richTextContainer.querySelectorAll('video.pending-decrypt');
          for (const vid of Array.from(videoEls)) {
            let sourcesUpdated = false;
            let hlsSource = null;
            // 移除video上可能影响播放的blob/http src（poster保留，避免封面丢失），避免覆盖<source>
            try {
              if (vid.hasAttribute('src')) {
                const raw = vid.getAttribute('src') || '';
                if (raw.startsWith('blob:') || raw.startsWith('http://') || raw.startsWith('https://')) {
                  vid.removeAttribute('src');
                }
              }
              // 保留 poster，不做强制移除
            } catch (_) {}
            
            const sourceEls = vid.querySelectorAll('source[data-secure-id]');
            for (const source of Array.from(sourceEls)) {
              const sid = source.getAttribute('data-secure-id');
              const realSrc = getOriginalUrl(sid);
              if (realSrc) {
                if (realSrc.includes('.m3u8')) {
                  hlsSource = realSrc;
                  source.setAttribute('type', 'application/x-mpegURL');
                }
                source.setAttribute('src', realSrc);
                source.classList.remove('pending-decrypt');
                sourcesUpdated = true;
              }
            }
            
            // 如果有HLS源且支持HLS.js，优先使用HLS.js
            if (hlsSource && Hls.isSupported()) {

                     const hls = new Hls({
                debug: false,
                       enableWorker: false,
                autoStartLoad: true,
                       startLevel: -1,
                       maxBufferLength: 30,
                       maxMaxBufferLength: 600,
                xhrSetup: hlsXhrSetup,
                     });

              hls.loadSource(hlsSource);
              hls.attachMedia(vid);
              
              hls.on(Hls.Events.MANIFEST_PARSED, () => {

                vid.classList.remove('poster-loading');
                vid.classList.add('poster-loaded');
                     });
                    
                    hls.on(Hls.Events.ERROR, (event, data) => {

                      if (data.fatal) {
                        switch (data.type) {
                          case Hls.ErrorTypes.NETWORK_ERROR:

                            hls.startLoad();
                            break;
                          case Hls.ErrorTypes.MEDIA_ERROR:

                            hls.recoverMediaError();
                            break;
                          default:

                            hls.destroy();
                            break;
                        }
                      }
                    });
                    
              // 保存HLS实例以便清理
              vid._hlsInstance = hls;
            }
            const videoDataSid = vid.getAttribute('data-secure-id');
            const videoDataSrc = videoDataSid ? getOriginalUrl(videoDataSid) : null;
            if (videoDataSrc) {
              // 若是 blob 源，绝不设置到 video.src，避免覆盖 <source>/HLS
              const isBlob = typeof videoDataSrc === 'string' && videoDataSrc.startsWith('blob:');
              if (!isBlob && videoDataSrc.includes('.m3u8')) {
                if (Hls.isSupported()) {

                   const hls = new Hls({
                    debug: false,
                     enableWorker: false,
                    autoStartLoad: true,
                     startLevel: -1,
                     maxBufferLength: 30,
                     maxMaxBufferLength: 600,
                    xhrSetup: hlsXhrSetup,
                   });

                   hls.loadSource(videoDataSrc);
                  hls.attachMedia(vid);
                  
                  hls.on(Hls.Events.MANIFEST_PARSED, () => {
    
                    vid.classList.remove('poster-loading');
                    vid.classList.add('poster-loaded');
                  });
                  
                  hls.on(Hls.Events.ERROR, (event, data) => {
    
                    if (data.fatal) {
                      switch (data.type) {
                        case Hls.ErrorTypes.NETWORK_ERROR:
    
                          hls.startLoad();
                          break;
                        case Hls.ErrorTypes.MEDIA_ERROR:
    
                          hls.recoverMediaError();
                          break;
                        default:
    
                          hls.destroy();
                          break;
                      }
                    }
                  });
                  
                  // 保存HLS实例以便清理
                  vid._hlsInstance = hls;
                  
                } else if (vid.canPlayType && vid.canPlayType('application/vnd.apple.mpegurl')) {
                  // iOS Safari 原生 HLS：重写 key URI 为绝对后端地址，否则 CDN 域下无法获取 key
                  patchNativeHlsM3u8(videoDataSrc).then(({ url: patchedUrl, isBlob }) => {
                    if (isBlob && vid._nativeBlobUrl) {
                      try { URL.revokeObjectURL(vid._nativeBlobUrl); } catch (_) {}
                    }
                    vid._nativeBlobUrl = isBlob ? patchedUrl : null;
                    vid.setAttribute('src', patchedUrl);
                  });
                } else {

                }
              } else if (!isBlob && !hlsSource) {
                // 非 m3u8 的直链（如 mp4）且没有 HLS 源时，作为兜底设置到 video.src
                // 对于 blob 源不做处理
                vid.setAttribute('src', videoDataSrc);
              }
                sourcesUpdated = true;
            }
            vid.setAttribute('controls', 'true');
            vid.setAttribute('preload', 'metadata');
            // iOS/Safari 与微信内核内联播放与全屏能力增强
            vid.setAttribute('playsinline', 'true');
            vid.setAttribute('webkit-playsinline', 'true');
            vid.setAttribute('x5-playsinline', 'true');
            vid.setAttribute('x5-video-player-type', 'h5');
            vid.setAttribute('x5-video-player-fullscreen', 'true');

            // 为 iOS 明确提供程序化进入全屏的兜底交互（双击切换全屏）
            try {
              const isIOS = /iPad|iPhone|iPod/i.test(navigator.userAgent);
              if (isIOS && !vid._fullscreenBound) {
                vid.addEventListener('dblclick', () => {
                  if (typeof vid.requestFullscreen === 'function') {
                    vid.requestFullscreen().catch(() => {});
                  } else if (typeof vid.webkitEnterFullscreen === 'function') {
                    try { vid.webkitEnterFullscreen(); } catch (_) {}
                  }
                });
                vid._fullscreenBound = true;
              }
            } catch (_) {}
            vid.classList.remove('pending-decrypt');
            if (sourcesUpdated) {
              try { vid.load(); } catch (_) {}
            }
          }
          
          // 第二步：在不影响视频的前提下，异步启动普通图片真实加载
          setTimeout(() => {
            try {
              richTextImageProcessor.processRichTextImages(richTextContainer.innerHTML, { placeholdersOnly: false });
            } catch (_) {}
          }, 0);
          
        }, 100);
      } catch (error) {

      }
    };

    processRichTextImages();
  }, [video?.videoContent]); // 🚀 只有当videoContent变化时才重新处理

  // 处理牢记地址中的加密图片：processRichTextImages 返回含 blob URL 的 HTML 字符串
  const rememberAddressContent = getConfig('remember_address');
  useEffect(() => {
    if (!rememberAddressContent) return;
    richTextImageProcessor.processRichTextImages(rememberAddressContent, { placeholdersOnly: false })
      .then(processed => setProcessedRememberAddress(processed))
      .catch(() => setProcessedRememberAddress(rememberAddressContent));
  }, [rememberAddressContent]);

  // 处理标签点击
  const handleTagClick = (tagName, tagId) => {
    if (tagId) {
      navigate(`/tag/${tagId}`);
    } else {
      navigate(`/search?keyword=${encodeURIComponent(tagName)}`);
    }
  };


  
  // 🧹 页面卸载时清理所有Blob URLs
  useEffect(() => {
    return () => {
      // 清理所有视频封面的Blob URLs
      const videos = document.querySelectorAll('video[data-original-poster]');
      videos.forEach(video => {
        if (video._posterCleanup) {
          video._posterCleanup();
        }
      });
      
      // 清理richTextImageProcessor创建的Blob URLs
      if (richTextImageProcessor && richTextImageProcessor.cleanupAllImages) {
        richTextImageProcessor.cleanupAllImages();
      }
    };
  }, []);




  if (!video && !loading) {
    return (
      <div className="min-h-screen bg-black flex items-center justify-center">
        <div className="text-center">
          <div className="text-white text-xl mb-4">视频加载失败</div>
          <div className="text-gray-400 text-sm mb-6">
            可能是网络连接问题或视频不存在
        </div>
          <button 
            onClick={() => fetchVideoDetail(0)}
            className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition-colors"
          >
            重新加载
          </button>
      </div>
      </div>
    );
  }

  // 不再显示loading页面，直接渲染骨架屏

  const safeTextLinkAds = Array.isArray(textLinkAds) ? textLinkAds : [];

  const previewItems = previewImagesRef.current || [];
  const totalPreviewItems = previewItems.length;
  const safePreviewIndex = totalPreviewItems > 0 ? Math.min(Math.max(imagePreview.index, 0), totalPreviewItems - 1) : 0;
  const activePreviewItem = imagePreview.isOpen && totalPreviewItems > 0 ? previewItems[safePreviewIndex] : null;
  const previewSrc = imagePreview.isOpen ? (activePreviewItem?.src || imagePreview.fallbackSrc) : '';
  const previewAlt = imagePreview.isOpen ? (activePreviewItem?.alt || imagePreview.fallbackAlt || '图片预览') : '';
  const previewTitle = imagePreview.isOpen ? (activePreviewItem?.title || imagePreview.fallbackTitle || '') : '';
  const previewPositionLabel = imagePreview.isOpen && totalPreviewItems > 1 ? `${safePreviewIndex + 1} / ${totalPreviewItems}` : '';
  const isDesktopView = typeof window !== 'undefined' ? window.innerWidth >= 1024 : false;

  return (
    <div className="min-h-screen text-white" style={{ backgroundColor: '#2C2A2A' }}>
      {/* CSS样式 - 牢记地址信息样式 & 富文本容器修正 */}
      <style dangerouslySetInnerHTML={{
        __html: `
          :root { --vd-gap: 24px; }

          @media (max-width: 768px) {
            .vd-mobile-gap { margin-bottom: var(--vd-gap) !important; }
            .vd-mobile-title-gap { margin-bottom: var(--vd-gap) !important; }
          }

          /* 富文本基础样式：保留后台设置的字体大小和对齐方式 */
          .rich-text-content {
            /* 设置基础文本颜色，但不覆盖内联样式 */
            color: #d1d5db; /* text-gray-300 */
            /* 重要：重置文本对齐，防止继承外层容器的text-center */
            text-align: left !important;
          }
          
          /* 强制重置所有富文本元素的对齐方式 */
          .rich-text-content,
          .rich-text-content * {
            text-align: left !important;
          }
          
          /* 富文本段落样式：保留后台设置的对齐和字体 */
          .rich-text-content p {
            /* 保留后台设置的文本对齐、字体大小等样式 */
            margin-bottom: 1em;
            /* 不强制设置字体大小和对齐方式，让内联样式生效 */
          }
          
          /* 富文本文本样式：确保内联样式优先级，不覆盖任何文本样式 */
          .rich-text-content span,
          .rich-text-content div,
          .rich-text-content p {
            /* 内联样式优先级高于类样式，因此会保留后台设置 */
            /* 特别注意：不设置text-align、font-size、line-height等会影响显示的属性 */
          }
          
          /* 重要：确保富文本中带有style属性的元素样式不被覆盖 */
          .rich-text-content *[style] {
            /* 让带有内联样式的元素保持其原有样式 */
          }
          
          /* 特别处理：确保包含text-align样式的元素能正确显示 */
          .rich-text-content *[style*="text-align"] {
            /* 强制让内联text-align样式生效 */
          }
          
          /* 确保段落的内联样式优先级最高 - 使用更高权重的选择器 */
          .rich-text-content p[style*="text-align:left"],
          .rich-text-content p[style*="text-align: left"],
          .rich-text-content div[style*="text-align:left"],
          .rich-text-content div[style*="text-align: left"] {
            text-align: left !important;
          }
          
          .rich-text-content p[style*="text-align:center"],
          .rich-text-content p[style*="text-align: center"],
          .rich-text-content div[style*="text-align:center"],
          .rich-text-content div[style*="text-align: center"] {
            text-align: center !important;
          }
          
          .rich-text-content p[style*="text-align:right"],
          .rich-text-content p[style*="text-align: right"],
          .rich-text-content div[style*="text-align:right"],
          .rich-text-content div[style*="text-align: right"] {
            text-align: right !important;
          }
          
          .rich-text-content p[style*="text-align:justify"],
          .rich-text-content p[style*="text-align: justify"],
          .rich-text-content div[style*="text-align:justify"],
          .rich-text-content div[style*="text-align: justify"] {
            text-align: justify !important;
          }
          
          /* 确保字体大小的内联样式也能正确显示 */
          .rich-text-content *[style*="font-size"] {
            /* 内联font-size样式应该生效 */
          }
          
          /* 特别处理：确保包含font-size的元素字体大小正确 */
          .rich-text-content p[style*="font-size"],
          .rich-text-content div[style*="font-size"],
          .rich-text-content span[style*="font-size"] {
            /* 确保内联字体大小样式生效 */
          }
          
          /* Quill编辑器字体大小类支持 */
          .rich-text-content .ql-size-small {
            font-size: 0.75em !important;
          }
          
          .rich-text-content .ql-size-large {
            font-size: 1.5em !important;
          }
          
          .rich-text-content .ql-size-huge {
            font-size: 2.5em !important;
          }
          
          /* Quill编辑器字体系列类支持 */
          .rich-text-content .ql-font-serif {
            font-family: Georgia, Times, serif !important;
          }
          
          .rich-text-content .ql-font-monospace {
            font-family: Monaco, Courier, monospace !important;
          }
          
          /* 富文本媒体元素统一自适应，外层高度随内容变化 */
          .rich-text-content img,
          .rich-text-content video,
          .rich-text-content canvas {
            display: block !important;
            width: 100% !important;
            max-width: 770px !important;
            height: auto !important;
            margin: 8px auto !important;
          }

          /* 仅包含媒体的容器，去除多余行高/内边距，避免巨大空隙 */
          .rich-text-content p:has(> img),
          .rich-text-content div:has(> img),
          .rich-text-content p:has(> video),
          .rich-text-content div:has(> video),
          .rich-text-content figure:has(img),
          .rich-text-content figure:has(video) {
            line-height: normal !important;
            padding: 0 !important;
            margin: 8px 0 !important;
            height: auto !important;
          }
          
          /* 富文本标题样式保留 */
          .rich-text-content h1,
          .rich-text-content h2,
          .rich-text-content h3,
          .rich-text-content h4,
          .rich-text-content h5,
          .rich-text-content h6 {
            margin-bottom: 0.5em;
            margin-top: 0.5em;
          }
          
          /* 富文本列表样式保留 */
          .rich-text-content ul,
          .rich-text-content ol {
            /* 保留后台设置的列表样式 */
            margin-bottom: 1em;
            padding-left: 1.5em;
          }
          
          /* 富文本链接样式 */
          .rich-text-content a {
            color: #1ABC9C;
            text-decoration: underline;
          }
          .rich-text-content a:hover {
            opacity: 0.8;
          }

          /* 图文容器元素 */
          .rich-text-content figure,
          .rich-text-content figure:has(img),
          .rich-text-content figure:has(video) {
            max-width: 770px;
            margin: 8px auto;
          }

          /* 全局兜底：编辑器注入固定高度一律忽略 */
          .rich-text-content *[style*="height:"],
          .rich-text-content *[style*="min-height:"] {
            height: auto !important;
            min-height: 0 !important;
          }
          
          /* 详情页面分类悬浮效果 */
          .video-category-item:hover .category-detail-line {
            width: 100% !important;
          }
          
          .video-category-item {
            position: relative;
            overflow: hidden;
          }
          
          /* 详情页面标签悬浮效果 */
          .video-tag-item:hover .tag-detail-line {
            width: 100% !important;
          }
          
          .video-tag-item {
            position: relative;
            overflow: hidden;
          }

          /* 牢记地址信息样式 */
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
          .detail-site-address p, .detail-site-address span, .detail-site-address strong {
            color: #AAAAAA !important;
          }
          
          /* 骨架屏动画 */
          .skeleton {
            background: linear-gradient(90deg, #3a3a3a 25%, #4a4a4a 50%, #3a3a3a 75%);
            background-size: 200% 100%;
            animation: skeleton-loading 1.5s infinite;
          }
          
          @keyframes skeleton-loading {
            0% { background-position: 200% 0; }
            100% { background-position: -200% 0; }
          }
          
          .skeleton-title {
            height: 41px;
            border-radius: 4px;
            margin-bottom: 24px;
          }
          
          .skeleton-meta {
            height: 18px;
            border-radius: 4px;
            width: 60%;
            margin: 0 auto;
          }
          
          .skeleton-content {
            height: 400px;
            border-radius: 8px;
            margin: 20px 0;
          }
          
          .skeleton-ad {
            height: 120px;
            border-radius: 8px;
            margin: 20px 0;
          }
        `
      }} />
      
      {/* 主内容区域 */}
      <div className="container mx-auto px-4 max-w-6xl pt-16 sm:pt-20">
        
        {/* 标题 + 元信息 + 分类（对齐上次版本样式） */}
        <div className="flex justify-center vd-mobile-gap md:mb-12 px-1 md:px-4">
          <div className="w-full text-center" style={{ maxWidth: '770px' }}>
            {video?.title ? (
            <h1 
              className="font-normal vd-mobile-title-gap md:mb-6 leading-relaxed video-detail-title" 
              style={{ 
                color: 'rgb(188, 188, 188)',
                fontSize: titleFontSize,
                fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                lineHeight: titleLineHeight,
                fontWeight: '300',
                fontStyle: 'normal',
                wordBreak: 'break-word',
                overflowWrap: 'break-word',
                hyphens: 'auto'
              }}
            >
              {video.title}
            </h1>
            ) : (
              // 🚀 只有在没有预加载数据且正在加载时才显示骨架屏
              !preloadData && loading ? (
                <div className="skeleton skeleton-title"></div>
              ) : (
                <div style={{ height: '41px', marginBottom: '24px' }}></div> // 占位空间
              )
            )}
              <div 
                className="video-meta-info" 
                style={{ 
                  fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                  fontSize: '16px',
                  lineHeight: '18px',
                fontWeight: 400,
                  fontStyle: 'normal',
                  color: 'rgb(188, 188, 188)'
                }}
              >
                <div className="flex flex-col items-center justify-center space-y-1 text-center">
                {video ? (
                  <div className="flex items-center justify-center space-x-3">
                    <span style={{ color: 'rgb(188, 188, 188)' }}>
                      {video.author || '天涯吃瓜小慧'}
                    </span>
                    <span style={{ color: 'rgb(188, 188, 188)' }}>•</span>
                    <span style={{ color: 'rgb(188, 188, 188)' }}>{video.publishedAt ? formatDate(video.publishedAt) : (video.createdAt ? formatDate(video.createdAt) : '')}</span>
                  </div>
                ) : (
                  // 🚀 只有在没有预加载数据且正在加载时才显示骨架屏
                  !preloadData && loading ? (
                    <div className="skeleton skeleton-meta"></div>
                  ) : (
                    <div style={{ height: '18px', width: '60%', margin: '0 auto' }}></div> // 占位空间
                  )
                )}
                {video?.categories && video.categories.length > 0 && (
                    <div className="video-categories-container" style={{ 
                      display: 'flex', 
                      gap: '8px', 
                      alignItems: 'center', 
                      flexWrap: 'wrap',
                      justifyContent: 'center'
                    }}>
                      {video.categories.map((cat, index) => (
                        <React.Fragment key={cat.id}>
                          <span 
                            className="video-category-item"
                            onClick={() => { navigate(`/category/${cat.id}`); }}
                            style={{ 
                              color: 'rgb(188, 188, 188)',
                              cursor: 'pointer',
                              position: 'relative',
                              transition: 'color 0.2s ease',
                              padding: '2px 0'
                            }}

                          >
                            {cat.name}
                            <span 
                              className="category-detail-line"
                              style={{
                                position: 'absolute',
                                bottom: '0',
                                left: '50%',
                                transform: 'translateX(-50%)',
                                width: '0',
                                height: '2px',
                                backgroundColor: '#4EA394',
                                transition: 'width 0.3s ease-out',
                                borderRadius: '1px'
                              }}
                            />
                          </span>
                          {index < video.categories.length - 1 && (
                          <span style={{ color: '#BCBCBC' }}>•</span>
                          )}
                        </React.Fragment>
                      ))}
                    </div>
                  )}
                </div>
              </div>
          </div>
        </div>

        {/* 顶部分享按钮已移除（根据需求） */}


        {/* 顶部横幅广告 */}
        <div className="mb-6 md:mb-12">
          <div className="flex justify-center px-1 md:px-4">
            <div className="w-full" style={{ maxWidth: '770px' }}>
              {/* 叠层结构：底层占位图始终可见，顶层真实图淡入覆盖
                  加载中显示 topAdCount 条占位（与真实数量一致），加载后渲染真实广告 */}
              <div className="space-y-2 md:space-y-4">
                {Array.from({ length: adsLoading ? topAdCount : detailTopAds.length }).map((_, index) => {
                  const ad = detailTopAds[index];
                  const isMobile = window.innerWidth < 768;
                  return (
                    <div
                      key={index}
                      className="overflow-hidden shadow-lg"
                      style={{ position: 'relative' }}
                      onClick={ad ? () => handleBannerAdClick(ad) : undefined}
                    >
                      {/* 底层：占位图，正常文档流撑开容器高度，真实图绝对定位覆盖 */}
                      <img
                        src="/800x100.jpg"
                        alt="横幅广告占位"
                        style={{
                          display: 'block',
                          width: '100%',
                          height: isMobile ? 'auto' : '90px',
                          maxHeight: isMobile ? 'none' : '90px',
                          objectFit: isMobile ? 'contain' : 'cover'
                        }}
                      />
                      {/* 顶层：真实广告图，绝对定位覆盖，opacity 0 → 1 平滑淡入 */}
                      {ad?.imageUrl && (
                        <SecureDecryptedImage
                          src={ad.imageUrl}
                          alt="横幅广告"
                          priority="high"
                          lazyLoad={false}
                          showLoadingIndicator={false}
                          objectFit={isMobile ? 'contain' : 'cover'}
                          style={{
                            position: 'absolute',
                            inset: 0,
                            cursor: ad ? 'pointer' : 'default'
                          }}
                          imageStyle={{
                            opacity: 0,
                            transition: 'opacity 0.4s ease'
                          }}
                          onLoad={(e) => { e.target.style.opacity = 1; }}
                          onError={(e) => { e.target.style.display = 'none'; }}
                        />
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        {/* 文字链接广告 + 站点地址 */}
        <div className="flex justify-center mb-8 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            {/* 文字链接广告（最多16个） */}
            <div className="grid grid-cols-4 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4 lg:gap-5 mb-6 sm:mb-8">
              {safeTextLinkAds.slice(0, 16).map((ad, index) => {
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
              {Array.from({ length: Math.max(0, 16 - safeTextLinkAds.length) }, (_, index) => (
                <div 
                  key={`placeholder-${index}`} 
                  className="border border-[#5E5C5C] rounded text-center flex items-center justify-center min-h-[45px] sm:min-h-[50px] px-2"
                >
                  <span 
                    className="text-gray-300 text-xs sm:text-sm"
                  >
                    占位链接
                  </span>
                </div>
              ))}
            </div>
            
            {/* 网站站点地址区域 - 只有API配置时才显示 */}
            {getConfig('detail_site_address') && (
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
                />
                
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
            )}

          </div>
        </div>

        {/* 视频富文本内容区域 */}
        <div className="flex justify-center mb-4 sm:mb-6 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            {video?.videoContent ? (
              <div 
                ref={richTextRef}
                className="text-gray-300 rich-text-content"
                dangerouslySetInnerHTML={{ 
                  __html: memoizedRichTextContent
                }}
              />
            ) : (
              <div className="skeleton skeleton-content"></div>
            )}
            </div>
          </div>



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

        {/* 标签与最后编辑时间 */}
        <div className="flex justify-center mb-8 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <div className="flex flex-wrap gap-3 mb-6">
              {video?.tags && video.tags.length > 0 ? (
                video.tags.map((tag, index) => (
                  <button
                    key={index}
                    onClick={() => handleTagClick(tag.name, tag.id)}
                    className="video-tag-item px-4 py-1 hover:bg-gray-600 text-gray-300 hover:text-white rounded-full text-sm transition-colors duration-200 cursor-pointer relative"
                    style={{ backgroundColor: '#343232' }}
                  >
                    {tag.name}
                    <span 
                      className="tag-detail-line"
                      style={{ position: 'absolute', bottom: 0, left: '50%', transform: 'translateX(-50%)', width: 0, height: 2, backgroundColor: '#4EA394', transition: 'width 0.3s ease-out', borderRadius: 1 }}
                    />
                  </button>
                ))
              ) : null}
            </div>
            <div className="text-right text-sm mb-6" style={{ color: '#888' }}>
              最后编辑于: {new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })}
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
            <div className="official-notice-scope">
              <style>{`
                /* 仅视频详情页作用域：取消所有固定字号，交由内联/Quill类控制 */
                .official-notice-scope .official-notice-content { font-size: initial !important; }
                .official-notice-scope .official-notice-content h1,
                .official-notice-scope .official-notice-content h2,
                .official-notice-scope .official-notice-content h3,
                .official-notice-scope .official-notice-content h4,
                .official-notice-scope .official-notice-content h5,
                .official-notice-scope .official-notice-content h6 { font-size: inherit !important; }
                /* Quill字号类兼容 */
                .official-notice-scope .official-notice-content .ql-size-small { font-size: 0.75em !important; }
                .official-notice-scope .official-notice-content .ql-size-large { font-size: 1.5em !important; }
                .official-notice-scope .official-notice-content .ql-size-huge { font-size: 2.5em !important; }
                .official-notice-scope .official-notice-content .ql-size-12px { font-size: 12px !important; }
                .official-notice-scope .official-notice-content .ql-size-13px { font-size: 13px !important; }
                .official-notice-scope .official-notice-content .ql-size-14px { font-size: 14px !important; }
                .official-notice-scope .official-notice-content .ql-size-15px { font-size: 15px !important; }
                .official-notice-scope .official-notice-content .ql-size-16px { font-size: 16px !important; }
                .official-notice-scope .official-notice-content .ql-size-18px { font-size: 18px !important; }
                .official-notice-scope .official-notice-content .ql-size-20px { font-size: 20px !important; }
                .official-notice-scope .official-notice-content .ql-size-24px { font-size: 24px !important; }
                .official-notice-scope .official-notice-content .ql-size-28px { font-size: 28px !important; }
                .official-notice-scope .official-notice-content .ql-size-32px { font-size: 32px !important; }
              `}</style>
              <OfficialNotice configKey="notice_info" />
            </div>
            
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
                    {shareClicked ? '你的朋友会感激你的分享！' : '抖音上都在源求这个视频 快点击分享'}
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

        {/* 文章导航区域 */}
        <div className="flex justify-center mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <ArticleNavigation video={video} />
          </div>
        </div>

                {/* 详情底部横幅广告 */}
        {!adsLoading && detailBottomAds.length > 0 && (
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
                      <div className="relative overflow-hidden shadow-md" style={{ minHeight: '50px' }}>
                        <SecureDecryptedImage
                          src={ad.imageUrl || '/800x100.jpg'}
                          alt="详情页底部横幅广告"
                          priority="high"
                          lazyLoad={false}
                          objectFit={window.innerWidth < 768 ? 'contain' : 'cover'}
                          imageStyle={{
                            width: '100%',
                            height: window.innerWidth < 768 ? 'auto' : '90px',
                            maxHeight: window.innerWidth < 768 ? 'none' : '90px',
                            opacity: 0,
                            transition: 'opacity 0.4s ease'
                          }}
                          onLoad={(e) => { e.target.style.opacity = 1; }}
                          onError={(e) => { e.target.style.opacity = 1; }}
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
      
      {/* 页脚 */}
      <Footer />
      
      {imagePreview.isOpen && previewSrc && ReactDOM.createPortal(
        <div
          className="fixed bg-black flex items-center justify-center"
          onClick={closeImagePreview}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            width: '100vw',
            height: viewportH ? `${viewportH}px` : '100dvh',
            zIndex: 999999,
            margin: 0,
            paddingTop: 'env(safe-area-inset-top)',
            paddingRight: 'env(safe-area-inset-right)',
            paddingBottom: 'env(safe-area-inset-bottom)',
            paddingLeft: 'env(safe-area-inset-left)',
            backgroundColor: 'rgba(0,0,0,0.95)'
          }}
        >
          <button
            type="button"
            onClick={(event) => { event.stopPropagation(); closeImagePreview(); }}
            aria-label="关闭图片预览"
            className="absolute text-white"
            style={{
              top: '16px',
              right: '16px',
              zIndex: 1000000,
              backgroundColor: 'rgba(0,0,0,0.65)',
              border: '1px solid rgba(255,255,255,0.35)',
              width: '44px',
              height: '44px',
              borderRadius: '9999px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '24px'
            }}
          >
            ×
          </button>

          <div
            className="relative max-w-full max-h-full p-4 flex flex-col items-center justify-center gap-4"
            onClick={(event) => event.stopPropagation()}
          >
            <img
              src={previewSrc}
              alt={previewAlt}
              className="max-w-full max-h-full object-contain"
              style={{
                maxWidth: '100vw',
                maxHeight: viewportH ? `${viewportH - 80}px` : '100dvh',
                objectFit: 'contain'
              }}
            />

            {(previewTitle || previewPositionLabel) && (
              <div className="text-white text-sm md:text-base text-center space-y-2">
                {previewTitle && <div>{previewTitle}</div>}
                {previewPositionLabel && (
                  <div className="opacity-80">
                    <span>{previewPositionLabel}</span>
                    {isDesktopView && totalPreviewItems > 1 && (
                      <span className="hidden md:inline ml-3">使用方向键或鼠标滚轮可切换图片</span>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
};

export default VideoDetail;
