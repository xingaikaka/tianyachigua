import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { FiMenu, FiSearch, FiX, FiSend, FiShare2 } from 'react-icons/fi';
import { FaMobile, FaQq, FaTelegram, FaGithub, FaTwitter, FaFont, FaEnvelope, FaBell, FaLaptop } from 'react-icons/fa';
import { useLocation, useNavigate } from 'react-router-dom';
import videoStatsService from '../../../services/videoStatsService';

import { useSiteConfig } from '../../../hooks/usePageConfig';
import { useCategories } from '../../../context/CategoriesContext';
import { useCategoryContent } from '../../../context/CategoryContentContext';
import categoryCacheService from '../../../services/categoryCacheService';

const Header = React.memo(() => {
  const location = useLocation();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearchModalOpen, setIsSearchModalOpen] = useState(false);
  const [isSearchExpanded, setIsSearchExpanded] = useState(false);
  const [moreMenuItems, setMoreMenuItems] = useState([]);
  const [showMoreDropdown, setShowMoreDropdown] = useState(false);
  /** 移动端抽屉内「更多」折叠列表（与桌面 showMoreDropdown 分离，避免 isCategoryExpanded 误清） */
  const [showMobileMoreSubmenu, setShowMobileMoreSubmenu] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [isMobileShortModeActive, setIsMobileShortModeActive] = useState(false);
  const [isCategoryExpanded, setIsCategoryExpanded] = useState(false);
  const [collapsedCount, setCollapsedCount] = useState(999);
  const navContainerRef = useRef(null);
  const categoryItemWidthsRef = useRef([]);

  // 获取站点配置和分类数据
  const { getSiteConfig } = useSiteConfig();
  const { categories, loading } = useCategories();

  const categoryIdsKey = useMemo(
    () => (Array.isArray(categories) ? categories.map((c) => c.id).join('-') : ''),
    [categories]
  );

  // 分类列表变化时重新全量测量
  useEffect(() => {
    categoryItemWidthsRef.current = [];
    setCollapsedCount(999);
  }, [categoryIdsKey]);

  const { currentCategoryId, switchCategory, switchToHome } = useCategoryContent();
  
  // 移动端检测
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };
    
    checkMobile();
    window.addEventListener('resize', checkMobile);
    
    return () => window.removeEventListener('resize', checkMobile);
  }, []);
  
  // 判断当前是否为短视频分类
  const currentCategory = categories.find(cat => cat.id === currentCategoryId);
  const isShortVideoCategory = currentCategory && Number(currentCategory.isShort) === 1;
  
  // 派发前的本地推断，用于提前进入短视频模式
  const derivedMobileShortMode = isMobile && isShortVideoCategory;
  
  // 最终沉浸式标记：事件驱动 + 本地推断兜底
  const isMobileShortImmersive = isMobileShortModeActive || derivedMobileShortMode;

  // 用户组全屏播放时隐藏 Header，避免顶部栏遮挡视频（与短视频一致）
  const [isUserGroupFullScreenOpen, setIsUserGroupFullScreenOpen] = useState(false);
  useEffect(() => {
    const handle = (e) => {
      if (e?.detail?.open === true) setIsUserGroupFullScreenOpen(true);
      if (e?.detail?.open === false) setIsUserGroupFullScreenOpen(false);
    };
    window.addEventListener('userGroupFullScreen:change', handle);
    return () => window.removeEventListener('userGroupFullScreen:change', handle);
  }, []);

  // 用户组播放页（/user/:username）移动端：与短视频一致，去除logo，按钮用模糊玻璃样式
  const isUserDetailPage = Boolean(location.pathname.match(/^\/user\/[^/]+$/));
  // 视频播放页（/user/:username/video/:videoId）：同步判断路径，避免等待事件导致 Header 闪一帧
  const isUserVideoPlayerPage = Boolean(location.pathname.match(/^\/user\/[^/]+\/video/));
  
  // 只在移动端短视频分类时显示分享按钮
  const mobileShortModeRef = useRef(null);
  const hasCategoryData = Array.isArray(categories) && categories.length > 0;

  useEffect(() => {
    try {
      const initial = document.body.classList.contains('short-video-mode');
      setIsMobileShortModeActive(initial);
    } catch (_) {}

    const handleShortModeEvent = (event) => {
      if (!event || !event.detail || typeof event.detail.isMobileShort !== 'boolean') {
        return;
      }
      setIsMobileShortModeActive(event.detail.isMobileShort);
    };

    window.addEventListener('categoryView:update', handleShortModeEvent);
    return () => {
      window.removeEventListener('categoryView:update', handleShortModeEvent);
    };
  }, []);

  useEffect(() => {
    if (!hasCategoryData) {
      return;
    }

    const nextShortMode = derivedMobileShortMode;
    if (mobileShortModeRef.current === nextShortMode) {
      return;
    }
    mobileShortModeRef.current = nextShortMode;
    window.dispatchEvent(new CustomEvent('categoryView:update', {
      detail: { isMobileShort: nextShortMode }
    }));
  }, [derivedMobileShortMode, hasCategoryData]);
  

  // 分享功能 - 分享当前视频
  const handleShare = useCallback(async () => {
    try {
      let shareUrl = window.location.href;
      let shareType = '页面';
      
      // 优先尝试获取当前视频信息（不管是否为短视频分类）
      try {
        const currentVideo = window['currentShortVideo'];
        
        if (currentVideo && currentVideo.id) {
          const origin = window.location.origin;
          shareUrl = `${origin}/video/${currentVideo.id}`;
          shareType = '视频';
        } else {
        }
      } catch (error) {
      }
      
      
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(shareUrl);
        alert(`${shareType}链接已复制到剪贴板: ${shareUrl}`);
      } else {
        // 降级方案
        const ta = document.createElement('textarea');
        ta.value = shareUrl;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        const ok = document.execCommand('copy');
        document.body.removeChild(ta);
        if (ok) {
          alert(`${shareType}链接已复制到剪贴板: ${shareUrl}`);
        } else {
          alert('复制失败，请手动复制链接');
        }
      }
    } catch (error) {
      alert('复制失败，请手动复制链接');
    }
  }, [getSiteConfig]);

  useEffect(() => {
    const shareHandler = () => {
      handleShare();
    };
    window.addEventListener('shortVideo:share', shareHandler);
    return () => window.removeEventListener('shortVideo:share', shareHandler);
  }, [handleShare]);
  


  // 处理社交媒体链接跳转
  const handleSocialLink = (platform) => {
    const url = getSiteConfig(`${platform}_url`);
    if (url && url.trim()) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  };

  // 处理邮件链接
  const handleEmailLink = () => {
    const email = getSiteConfig('contact_email');
    if (email && email.trim()) {
      window.location.href = `mailto:${email}`;
    }
  };

  // 处理QQ链接 
  const handleQQLink = () => {
    const qqNumber = getSiteConfig('qq_number');
    if (qqNumber && qqNumber.trim()) {
      // QQ临时会话链接格式
      window.open(`http://wpa.qq.com/msgrd?v=3&uin=${qqNumber}&site=qq&menu=yes`, '_blank');
    }
  };

  // 处理首页导航点击（暂时使用正常路由）
  const handleHomeClick = (e) => {
    e.preventDefault();
    // 标记这是路由导航，用于弹窗控制
    sessionStorage.setItem('route_navigation', 'true');
    try { switchToHome?.(); } catch (_) {}
    try { videoStatsService.trackCategoryClick('all'); } catch (_) {}
    navigate('/', { state: { forceTop: true } });
  };

  // 处理分类导航点击（暂时使用正常路由）
  const handleCategoryClick = (e, categoryId) => {
    e.preventDefault();
    const target = categories.find(cat => cat.id === categoryId);
    if (target) {
      const shouldUseShort = Number(target.isShort) === 1 && isMobile;
      window.dispatchEvent(new CustomEvent('categoryView:update', { detail: { isMobileShort: shouldUseShort } }));
    }
    // 暂时使用正常路由导航
    try { videoStatsService.trackCategoryClick(categoryId); } catch (_) {}
    navigate(`/category/${categoryId}`, { state: { forceTop: true } });
  };

  // 从URL中提取categoryId
  const getCurrentCategoryId = () => {
    const match = location.pathname.match(/\/category\/(\d+)/);
    return match ? parseInt(match[1]) : null;
  };



  // 获取"更多"菜单项（使用缓存）
  useEffect(() => {
    const fetchMoreMenuItems = async () => {
      try {
        const menuItems = await categoryCacheService.getMoreMenuItems();
        setMoreMenuItems(menuItems);
      } catch (error) {
        setMoreMenuItems([]);
      }
    };

    fetchMoreMenuItems();
  }, []);

  // 监听更多菜单数据更新
  useEffect(() => {
    const handleDataUpdate = (event) => {
      const { type, data } = event.detail;
      if (type === 'moreMenu') {
        setMoreMenuItems(data);
      }
    };

    window.addEventListener('categoryDataUpdate', handleDataUpdate);
    
    return () => {
      window.removeEventListener('categoryDataUpdate', handleDataUpdate);
    };
  }, []);

  // 滚动时自动收起分类导航
  useEffect(() => {
    if (!isCategoryExpanded) return;
    const handleScroll = () => {
      if (window.scrollY > 30) setIsCategoryExpanded(false);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [isCategoryExpanded]);

  // 收起首行时隐藏配置「更多」，并关掉下拉避免再次展开时误开
  useEffect(() => {
    if (!isCategoryExpanded) setShowMoreDropdown(false);
  }, [isCategoryExpanded]);

  // 关闭侧栏时收起移动端「更多」列表
  useEffect(() => {
    if (!isMobileMenuOpen) setShowMobileMoreSubmenu(false);
  }, [isMobileMenuOpen]);

  // 计算收起时第一行能放几个分类（两阶段：先判断是否需要「更多」，再按真实按钮宽度预留）
  useEffect(() => {
    if (isCategoryExpanded) return;

    const validLen = Array.isArray(categories)
      ? categories.filter((c) => {
          const n = String(c?.name || '').trim();
          return n && !n.includes('}');
        }).length
      : 0;

    const measure = () => {
      const container = navContainerRef.current;
      if (!container || validLen === 0) return;

      const gap = 4;
      const cw = container.clientWidth;
      const domItems = Array.from(container.querySelectorAll('.cat-item'));

      if (domItems.length === validLen) {
        categoryItemWidthsRef.current = domItems.map((el) => el.offsetWidth);
      }

      const widths = categoryItemWidthsRef.current;
      if (widths.length !== validLen) return;

      let total = 0;
      let countNoReserve = 0;
      for (let i = 0; i < widths.length; i++) {
        const piece = widths[i] + gap;
        if (total + piece > cw + 0.5) break;
        total += piece;
        countNoReserve++;
      }

      if (countNoReserve >= validLen) {
        setCollapsedCount(validLen);
        return;
      }

      const toggleEl = container.querySelector('.cat-toggle-btn');
      const btnReserve = (toggleEl ? toggleEl.offsetWidth : 52) + gap;
      const lineCollapsed = cw - btnReserve;

      total = 0;
      let count = 0;
      for (let i = 0; i < widths.length; i++) {
        const piece = widths[i] + gap;
        if (total + piece > lineCollapsed + 0.5) break;
        total += piece;
        count++;
      }

      setCollapsedCount(count > 0 ? count : 1);
    };

    measure();
    const ro = new ResizeObserver(measure);
    if (navContainerRef.current) ro.observe(navContainerRef.current);
    return () => ro.disconnect();
  }, [categories, isCategoryExpanded, categoryIdsKey]);

  // 点击外部区域关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (showMoreDropdown && !event.target.closest('.header-more-dropdown-container')) {
        setShowMoreDropdown(false);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [showMoreDropdown]);


  // 处理ESC键关闭搜索框
  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && isSearchExpanded) {
        setIsSearchExpanded(false);
        setSearchQuery('');
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isSearchExpanded]);

  const toggleMobileMenu = useCallback((nextState) => {
    setIsMobileMenuOpen(prev => {
      if (typeof nextState === 'boolean') {
        return nextState;
      }
      return !prev;
    });
  }, []);

  useEffect(() => {
    const handleMenuControl = (event) => {
      const action = event?.detail?.action;
      if (action === 'open') {
        toggleMobileMenu(true);
      } else if (action === 'close') {
        toggleMobileMenu(false);
      } else {
        toggleMobileMenu();
      }
    };

    window.addEventListener('mobileMenu:control', handleMenuControl);
    return () => window.removeEventListener('mobileMenu:control', handleMenuControl);
  }, [toggleMobileMenu]);

  // 动态调整主内容区域的padding-top以适应Header高度（含分类展开/收起）
  useEffect(() => {
    const header = document.querySelector('.desktop-header');
    const main = document.querySelector('main.min-h-screen');

    const adjustMainPadding = () => {
      if (header && main && window.innerWidth >= 768) {
        const headerHeight = header instanceof HTMLElement ? header.offsetHeight : 80;
        const paddingTop = headerHeight - 40;
        if (main instanceof HTMLElement) {
          main.style.paddingTop = `${paddingTop}px`;
        }
      }
    };

    // 用 ResizeObserver 监听 header 高度变化（分类展开/收起都会触发）
    const ro = new ResizeObserver(adjustMainPadding);
    if (header) ro.observe(header);

    adjustMainPadding();
    window.addEventListener('resize', adjustMainPadding);
    document.fonts.ready.then(adjustMainPadding);

    return () => {
      ro.disconnect();
      window.removeEventListener('resize', adjustMainPadding);
    };
  }, [categories, isSearchExpanded]); // 当分类数据或搜索框状态变化时重新调整

  const toggleSearchModal = () => {
    setIsSearchModalOpen(!isSearchModalOpen);
  };

  // 切换内联搜索框显示状态
  const toggleSearchExpanded = () => {
    if (!isSearchExpanded) {
      // 展开搜索框
      setIsSearchExpanded(true);
      // 展开时聚焦搜索框
      setTimeout(() => {
        const searchInput = document.getElementById('inline-search-input');
        if (searchInput) {
          searchInput.focus();
        }
      }, 200); // 等待动画完成
    } else {
      // 收起搜索框 - 使用快速淡出
      const container = document.querySelector('.search-expanded-container');
      if (container && container instanceof HTMLElement) {
        try { container.classList.add('closing'); } catch (_) {}
        setTimeout(() => {
          setIsSearchExpanded(false);
          setSearchQuery('');
        }, 200);
      } else {
        setIsSearchExpanded(false);
        setSearchQuery('');
      }
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      // 跳转到搜索结果页面
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
      setIsSearchModalOpen(false);
      setIsSearchExpanded(false); // 收起搜索框
      setIsMobileMenuOpen(false); // 关闭移动端菜单
      setSearchQuery(''); // 清空搜索框
    }
  };

  // 处理"更多"菜单项点击
  const handleMoreMenuClick = (menuItem) => {
    setShowMoreDropdown(false); // 关闭下拉菜单
    
    const url = menuItem.jumpUrl;
    if (!url) return;
    
    // 判断是外部链接还是内部路由
    if (url.startsWith('http://') || url.startsWith('https://')) {
      // 外部链接，在新窗口打开
      window.open(url, '_blank', 'noopener,noreferrer');
    } else {
      // 内部路由，使用navigate跳转
      navigate(url);
    }
  };

  // 处理「更多」展开（桌面下拉 / 移动端抽屉内折叠列表）
  const handleMoreButtonClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!Array.isArray(moreMenuItems) || moreMenuItems.length === 0) return;
    setShowMoreDropdown((v) => !v);
  };

  // 控制 body 类名以推动页面内容
  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.classList.add('mobile-drawer-open');
    } else {
      document.body.classList.remove('mobile-drawer-open');
    }
    return () => document.body.classList.remove('mobile-drawer-open');
  }, [isMobileMenuOpen]);

  return (
    <>
      {/* 添加动画样式 */}
      <style>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(-10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        
        @keyframes gradientShift {
          0% {
            background-position: 0% 50%;
          }
          50% {
            background-position: 100% 50%;
          }
          100% {
            background-position: 0% 50%;
          }
        }
        
        .animated-gradient-text {
          transition: all 0.3s ease;
        }
        
        .animated-gradient-text:hover {
          animation-duration: 1.5s !important;
          transform: scale(1.05);
        }
        /* 推荐分类：蓝-紫-粉-青 渐变并左右流动 + 轻微发光 */
        .recommended-gradient-text {
          display: inline-block;
          background-image: linear-gradient(90deg, #60A5FA, #8B5CF6, #EC4899, #22D3EE);
          background-size: 200% 100%;
          background-position: 0% 50%;
          -webkit-background-clip: text;
          background-clip: text;
          color: transparent;
          -webkit-text-fill-color: transparent;
          animation: recommendedGradient 1.6s ease-in-out infinite alternate, recommendedGlow 1.4s ease-in-out infinite;
          /* 提升层叠优先级，避免被行内样式覆盖 */
          position: relative;
        }
        @keyframes recommendedGradient {
          0% { background-position: 0% 50%; }
          100% { background-position: 100% 50%; }
        }
        @keyframes recommendedGlow {
          0%,100% { filter: drop-shadow(0 0 0 rgba(99, 102, 241, 0.0)); }
          50% { filter: drop-shadow(0 0 6px rgba(99, 102, 241, 0.35)); }
        }
        

        
        @keyframes slideInFromRight {
          0% {
            transform: translateX(100%);
            opacity: 0;
          }
          100% {
            transform: translateX(0);
            opacity: 1;
          }
        }
        @keyframes slideInFromLeft {
          0% {
            transform: scaleX(0);
            opacity: 0;
            transform-origin: left center;
          }
          100% {
            transform: scaleX(1);
            opacity: 1;
            transform-origin: left center;
          }
        }
        
        @keyframes slideOutToRight {
          0% {
            transform: translateX(0);
            opacity: 1;
          }
          100% {
            transform: translateX(100%);
            opacity: 0;
          }
        }
        @keyframes fadeOutQuick {
          0% { opacity: 1; }
          100% { opacity: 0; }
        }
        
        .search-expanded-container {
          overflow: hidden;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        }
        
        .search-expanded-container.closing {
          animation: fadeOutQuick 0.2s ease-out forwards;
        }

        /* PC 右侧控制区：图标隐藏与搜索平滑右侧滑入 */
        .right-controls{position:relative;display:flex;align-items:center;min-width:280px}
        .right-controls .media-icons{display:flex;align-items:center;gap:8px;transition:transform .3s ease,opacity .3s ease}
        .right-controls.search-active .media-icons{transform:translateX(16px);opacity:0;pointer-events:none}
        .right-controls .inline-search{position:absolute;left:0;display:flex;align-items:center;z-index:2}
        
        /* 与 VideoList（container + max-w-5xl + px）同宽，避免顶栏与列表左右基准不一致 */
        .header-content-container {
          margin: 0 auto;
          width: 100%;
        }
        
        /* Header自适应高度 - 根据内容自动调整 */
        .desktop-header {
          min-height: 80px;
          padding: 12px 0;
          /* 让Header高度完全由内容决定 */
          height: auto;
        }
        
        .desktop-header .header-inner {
          min-height: 56px;
          /* 确保内容有足够的垂直空间 */
          padding: 8px 0;
        }
        
        /* 分类导航区域自适应高度 */
        .desktop-header nav {
          /* 让导航区域可以自由换行和扩展高度 */
          min-height: 40px;
          padding: 4px 0;
        }

        /* 未展开「全部分类」时压低顶栏高度（单行摘要）；展开后仍用上面默认间距 */
        .desktop-header.desktop-header-nav-collapsed {
          min-height: 0 !important;
          padding: 8px 0 !important;
        }
        .desktop-header.desktop-header-nav-collapsed .header-inner {
          min-height: 0 !important;
          padding: 4px 0 !important;
        }
        .desktop-header.desktop-header-nav-collapsed nav.category-nav-scroll {
          min-height: 0 !important;
          padding: 0 !important;
        }
        .desktop-header.desktop-header-nav-collapsed .header-category-nav.cat-collapsed .cat-item {
          padding: 4px 8px !important;
          line-height: 1.35 !important;
        }
        .desktop-header.desktop-header-nav-collapsed .header-category-nav.cat-collapsed .cat-toggle-btn {
          padding: 4px 6px !important;
          line-height: 1.35 !important;
        }
        .desktop-header.desktop-header-nav-collapsed .header-logo a {
          padding-top: 3px !important;
          padding-bottom: 3px !important;
        }
        
        /* 分类导航滚动条隐藏（保持滚动功能） */
        .category-nav-scroll {
          scrollbar-width: none; /* Firefox */
          -ms-overflow-style: none; /* IE/Edge */
        }
        
        .category-nav-scroll::-webkit-scrollbar {
          display: none; /* Chrome/Safari */
        }
        
        /* 正常尺寸（大于1024px）：第一行容器内容平铺，分类导航显示在中间 */
        @media (min-width: 1025px) {
          .desktop-header .header-inner {
            flex-direction: row; /* 确保是横向布局 */
            align-items: center; /* 垂直居中 */
          }
          
          .desktop-header .header-inner .header-first-row {
            display: contents; /* 让子元素直接成为header-inner的子元素 */
          }
          
          /* Logo区域：恢复右边距，设置order */
          .desktop-header .header-inner .header-logo {
            margin-right: 24px;
            order: 1;
            flex-shrink: 0; /* 不收缩 */
          }
          
          /* 分类导航容器：显示在中间位置，占据剩余空间，允许换行 */
          .desktop-header .header-inner .header-category-nav {
            order: 2; /* 在logo之后，右侧控制之前 */
            flex: 1;
            min-width: 0;
            max-height: none !important;
            overflow: visible !important;
            width: auto; /* 移除w-full的影响 */
          }
          
          /* 收起：首行分类靠右；展开：多行分类在中间栏内从左排；nav 占满中间栏宽度才在栏内换行 */
          .desktop-header .header-inner .header-category-nav.cat-collapsed nav,
          .desktop-header .header-inner .header-category-nav.cat-expanded nav {
            width: 100% !important;
            max-width: 100% !important;
            box-sizing: border-box !important;
          }
          .desktop-header .header-inner .header-category-nav.cat-collapsed nav {
            max-height: none !important;
            overflow-y: visible !important;
            overflow-x: visible !important;
            flex-wrap: wrap !important;
            justify-content: flex-end !important;
          }
          .desktop-header .header-inner .header-category-nav.cat-expanded nav {
            max-height: none !important;
            overflow-y: visible !important;
            overflow-x: visible !important;
            flex-wrap: wrap !important;
            justify-content: flex-start !important;
          }
          
          /* 右侧控制：恢复左边距，设置order */
          .desktop-header .header-inner .right-controls {
            margin-left: 16px;
            order: 3;
            flex-shrink: 0; /* 不收缩 */
          }

          /* PC 无论是否展开：始终保持一行 flex，Logo | 分类（flex:1，仅栏内换行）| 右侧图标。勿对展开态改用 column，否则分类会整块掉到下一行 */
        }

        /* 展开/收起切换按钮（nav 内普通 flex 子元素，紧跟分类） */
        .desktop-header .cat-toggle-btn {
          display: none;
        }
        @media (min-width: 768px) {
          .desktop-header .cat-toggle-btn {
            display: inline-flex;
            align-items: center;
            flex-shrink: 0;
            background: none;
            border: none;
            cursor: pointer;
            color: rgba(255,255,255,0.5);
            font-size: 14px;
            padding: 0.5rem 0.25rem;
            white-space: nowrap;
            transition: color 0.2s;
            gap: 2px;
            line-height: 1.5;
          }
          .desktop-header .cat-toggle-btn:hover { color: rgba(255,255,255,0.9); }
        }
        
        /* iPad等中等尺寸（768px-1024px）：分类导航换行到logo下方，完全显示并允许换行 */
        @media (min-width: 768px) and (max-width: 1024px) {
          .desktop-header .header-inner {
            flex-direction: column;
            align-items: flex-start;
            gap: 12px;
          }
          
          /* 第一行容器：logo + 右侧控制 */
          .desktop-header .header-inner .header-first-row {
            width: 100%;
            display: flex;
            justify-content: space-between;
            align-items: center;
          }
          
          /* Logo区域：移除右边距 */
          .desktop-header .header-inner .header-logo {
            margin-right: 0;
          }
          
          /* 右侧控制：移除左边距 */
          .desktop-header .header-inner .right-controls {
            margin-left: 0;
          }
          
          /* 分类导航容器：第二行，全宽，允许换行 */
          .desktop-header .header-inner .header-category-nav {
            width: 100%;
            max-height: none;
            overflow: visible;
            margin-top: 0;
          }
          
          /* 分类导航容器：移除高度限制 */
          .desktop-header .header-inner .header-category-nav {
            max-height: none !important;
            overflow: visible !important;
          }
          
          /* 分类导航：平板第二行从左排布 */
          .desktop-header .header-inner .header-category-nav nav {
            max-height: none !important;
            overflow-y: visible !important;
            overflow-x: visible !important;
            flex-wrap: wrap !important;
            display: flex !important;
            width: 100% !important;
            justify-content: flex-start !important;
          }
        }
        
        /* 小尺寸（小于768px）：分类导航换行到logo下方 */
        @media (max-width: 767px) {
          .desktop-header .header-inner {
            flex-direction: column;
            align-items: flex-start;
            gap: 12px;
          }
          
          /* 第一行容器：logo + 右侧控制 */
          .desktop-header .header-inner .header-first-row {
            width: 100%;
            display: flex;
            justify-content: space-between;
            align-items: center;
          }
          
          /* Logo区域：移除右边距 */
          .desktop-header .header-inner .header-logo {
            margin-right: 0;
          }
          
          /* 右侧控制：移除左边距 */
          .desktop-header .header-inner .right-controls {
            margin-left: 0;
          }
          
          /* 分类导航容器：第二行，全宽 */
          .desktop-header .header-inner .header-category-nav {
            width: 100%;
            max-height: none;
            overflow: visible;
            margin-top: 0;
          }
          
          /* 分类导航：移除高度限制，允许自然换行 */
          .desktop-header .header-inner .header-category-nav nav {
            max-height: none;
            overflow-y: visible;
            overflow-x: visible;
          }
        }
        
        /* 主内容区域基础样式 - JavaScript会动态调整padding-top */
        main.min-h-screen {
          padding-top: 80px; /* 默认值，会被JavaScript覆盖 */
        }
        
        /* 移动端保持原有间距 */
        @media (max-width: 767px) {
          main.min-h-screen {
            padding-top: 50px !important; /* 移动端使用固定值 */
          }
        }
        @media (max-width: 767px) {
          html, body { overflow-x: hidden; }
          /* 正常分类和短视频分类都采用相同的滑动效果 */
          body.mobile-drawer-open main { 
            transform: translateX(256px);
            transition: transform 0.3s ease;
          }
          body.mobile-drawer-open header { 
            transform: translateX(256px);
            transition: transform 0.3s ease;
          }
          /* 🔧 修复：短视频顶部固定定位的按钮也要跟随滑动 */
          body.mobile-drawer-open button[aria-label="打开菜单"] {
            transform: translateX(256px);
            transition: transform 0.3s ease;
          }
          body.mobile-drawer-open button[aria-label="随机切换短视频页"] {
            transform: translateX(calc(-50% + 256px));
            transition: transform 0.3s ease;
          }
          body.mobile-drawer-open button[aria-label="分享当前视频"] {
            transform: translateX(256px);
            transition: transform 0.3s ease;
          }
          /* 当抽屉打开时，隐藏页面右侧的内联搜索输入（避免露出占位） */
          body.mobile-drawer-open .inline-header-search { display: none !important; }
          .mobile-drawer { 
            transform: translateX(-256px);
            transition: transform 0.3s ease;
            pointer-events: none;
          }
          body.mobile-drawer-open .mobile-drawer { 
            transform: translateX(0);
            pointer-events: auto;
          }
        }

        body.short-video-mode .desktop-header {
          display: none !important;
        }
        body.short-video-mode #mobile-header {
          opacity: 0;
          pointer-events: none;
          transform: translateY(-100%);
          transition: transform 0.3s ease, opacity 0.3s ease;
        }
        body.short-video-mode.mobile-drawer-open #mobile-header {
          opacity: 1;
          pointer-events: auto;
          transform: translateY(0);
        }
      `}</style>
      
      {/* 桌面端Header */}
      <header 
        className={`hidden md:block fixed top-0 left-0 right-0 z-50 backdrop-blur-sm desktop-header${!isCategoryExpanded ? ' desktop-header-nav-collapsed' : ''}`}
        style={{ backgroundColor: 'rgba(49, 48, 48, 0.9)' }}
      >
        <div className="w-full">
          <div className="header-content-container container mx-auto max-w-5xl px-1 md:px-4">
            <div className="flex items-center w-full relative header-inner">
            {/* 第一行容器：logo + 右侧控制（mini尺寸时显示） */}
            <div className="header-first-row flex items-center justify-between w-full md:w-auto md:contents">
              {/* 左侧：品牌名独立区域 */}
              <div className="flex-shrink-0 mr-6 flex items-center header-logo">
              <a 
                href="/"
                onClick={handleHomeClick}
                className="cursor-pointer whitespace-nowrap transition-all duration-300"
                style={{ 
                  display: 'flex',
                  alignItems: 'center',
                  fontSize: '28px',
                  fontFamily: '"Brush Script MT", "Lucida Handwriting", "Courier New", cursive',
                  fontWeight: 'bold',
                  color: '#7AD8F1',
                  padding: '5.625px 0px',
                  lineHeight: '1.2',
                  textDecoration: 'none',
                  textShadow: '2px 2px 4px rgba(0, 0, 0, 0.3)',
                  letterSpacing: '1px'
                }}

              >
                {/* 天涯吃瓜品牌Logo（图片替换） */}
                <img
                  src="/logo.png"
                  alt="天涯吃瓜"
                  style={{
                    display: 'block',
                    width: '200px',
                    height: 'auto',
                    objectFit: 'contain'
                  }}
                />
              </a>
            </div>

              {/* 右侧：媒体图标 + 搜索（PC 动画） */}
              <div className={`flex-shrink-0 ml-4 flex items-center right-controls ${isSearchExpanded ? 'search-active' : ''}`}>
              {/* 搜索框容器（固定在原位置，绝对定位从左向右动画） */}
              {isSearchExpanded && (
                <div 
                  className="search-expanded-container flex items-center inline-search"
                  style={{
                    height: '40px',
                    backgroundColor: '#212121',
                    borderRadius: '20px',
                    padding: '0 8px',
                    width: '280px',
                    animation: 'slideInFromLeft 0.3s ease-out',
                    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)'
                  }}
                >
                  {/* 搜索图标（白色圆形背景） */}
                  <button 
                    onClick={toggleSearchExpanded}
                    className="search-icon-expanded"
                    style={{
                      width: '28px',
                      height: '28px',
                      borderRadius: '50%',
                      backgroundColor: '#ffffff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: 'none',
                      cursor: 'pointer',
                      marginRight: '12px',
                      transition: 'all 0.2s ease',
                      flexShrink: 0
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.backgroundColor = '#f0f0f0';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.backgroundColor = '#ffffff';
                    }}
                  >
                    <FiSearch size={14} color="#333" />
                  </button>

                  {/* 搜索输入框 */}
                  <form onSubmit={handleSearch} className="flex-1">
                    <input
                      id="inline-search-input"
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="搜索..."
                      style={{ 
                        backgroundColor: 'transparent',
                        border: 'none',
                        outline: 'none',
                        color: '#ffffff',
                        fontSize: '14px',
                        width: '100%',
                        padding: '6px 12px 6px 0'
                      }}
                      className="placeholder-gray-400"
                      autoFocus
                    />
                  </form>
                </div>
              )}

              {/* 媒体图标（在展开时通过样式隐去，不移除 DOM） */}
              <div className="media-icons space-x-2">
                {/* 搜索图标 */}
                <button 
                  onClick={toggleSearchExpanded}
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FiSearch size={16} />
                </button>

                {/* Telegram图标 */}
                <button 
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onClick={() => handleSocialLink('telegram')}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FaTelegram size={16} />
                </button>
                
                {/* X图标 */}
                <button 
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onClick={() => handleSocialLink('twitter')}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FaTwitter size={16} />
                </button>
                
                {/* 腾讯QQ图标 */}
                <button 
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onClick={handleQQLink}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FaQq size={16} />
                </button>
                
                {/* GitHub图标 */}
                <button 
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onClick={() => handleSocialLink('github')}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FaGithub size={16} />
                </button>
              </div>
              </div>
            </div>
              
            {/* 第二行：分类导航（mini尺寸时显示在logo下方） */}
              {(() => {
                const validCategories = Array.isArray(categories)
                  ? categories.filter(c => { const n = String(c?.name || '').trim(); return n && !n.includes('}'); })
                  : [];
                const hasHidden = validCategories.length > collapsedCount;
                const displayedCategories = isCategoryExpanded ? validCategories : validCategories.slice(0, collapsedCount);
                return (
                  <div className={`header-category-nav flex-1 min-w-0 w-full ${isCategoryExpanded ? 'cat-expanded' : 'cat-collapsed'}`}>
                    <nav ref={navContainerRef} className="flex flex-wrap items-center gap-x-0.5 gap-y-0.5 category-nav-scroll">
                      {displayedCategories.map((category) => {
                        const categoryName = String(category?.name || '').trim();
                        const isRecommended = Number(category?.isRecommended) === 1;
                        const showGradient = isRecommended;
                        return (
                          <button
                            key={category.id}
                            className={`cat-item transition-colors duration-200 cursor-pointer ${showGradient ? 'recommended-gradient-text' : ''}`}
                            onClick={(e) => handleCategoryClick(e, category.id)}
                            style={{ 
                              /* 字号略大于参考站 14px，保证可读性；颜色仍与 Mirages 顶栏一致 0.9 / hover 0.75 */
                              fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                              fontSize: '16px',
                              lineHeight: '1.5',
                              fontWeight: '400',
                              fontStyle: 'normal',
                              color: showGradient ? 'transparent' : 'rgba(255,255,255,0.9)',
                              padding: '0.5rem 0.5rem',
                              background: showGradient ? undefined : 'none',
                              border: 'none'
                            }}
                            onMouseEnter={(e) => { if(!showGradient) e.currentTarget.style.color = 'rgba(255,255,255,0.75)'; }}
                            onMouseLeave={(e) => { if(!showGradient) e.currentTarget.style.color = 'rgba(255,255,255,0.9)'; }}
                          >
                            {categoryName}
                          </button>
                        );
                      })}

                      {/* 配置「更多」：展开全部分类后显示在末行；收起首行不占位。无配置项时仍显示文案，不弹出下拉 */}
                      {isCategoryExpanded && (
                        <div className="relative header-more-dropdown-container">
                          <button
                            type="button"
                            onClick={handleMoreButtonClick}
                            className="flex items-center space-x-1 transition-colors duration-200"
                            style={{
                              fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                              fontSize: '18px',
                              lineHeight: '1.5',
                              fontWeight: '400',
                              fontStyle: 'normal',
                              color: showMoreDropdown ? '#1abc9c' : 'rgba(255,255,255,0.9)',
                              padding: '0.5rem 0.5rem',
                              background: 'none',
                              border: 'none',
                              cursor: moreMenuItems.length > 0 ? 'pointer' : 'default'
                            }}
                            onMouseEnter={(e) => {
                              if (!showMoreDropdown && moreMenuItems.length > 0) e.currentTarget.style.color = 'rgba(255,255,255,0.75)';
                            }}
                            onMouseLeave={(e) => {
                              if (!showMoreDropdown && moreMenuItems.length > 0) e.currentTarget.style.color = 'rgba(255,255,255,0.9)';
                            }}
                          >
                            <span>更多</span>
                            <span
                              className="text-xs transition-transform duration-300"
                              style={{ transform: showMoreDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }}
                            >▼</span>
                          </button>
                          {showMoreDropdown && moreMenuItems.length > 0 && (
                            <div
                              className="absolute top-full left-0 mt-2 rounded-lg shadow-xl py-2 z-[200] min-w-[200px] max-w-[min(280px,calc(100vw-48px))]"
                              style={{
                                backgroundColor: '#333232',
                                backdropFilter: 'blur(10px)',
                                animation: 'fadeIn 0.2s ease-out'
                              }}
                            >
                              {moreMenuItems.map((item) => (
                                <button
                                  type="button"
                                  key={item.configId}
                                  onClick={() => handleMoreMenuClick(item)}
                                  className="w-full text-left px-4 py-2.5 text-white transition-colors duration-200"
                                  onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#403E3F'; }}
                                  onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'transparent'; }}
                                  style={{ fontSize: '14px' }}
                                >
                                  {item.basicContent}
                                </button>
                              ))}
                            </div>
                          )}
                        </div>
                      )}

                      {/* 展开/收起全部分类行（不占「更多」下拉语义） */}
                      {(hasHidden || isCategoryExpanded) && (
                        <button
                          type="button"
                          className="cat-toggle-btn"
                          onClick={(e) => { e.stopPropagation(); setIsCategoryExpanded(p => !p); }}
                          title={isCategoryExpanded ? '收起分类' : '展开全部分类'}
                        >
                          <span>{isCategoryExpanded ? '收起' : '展开更多'}</span>
                          <span style={{ fontSize: 12, transition: 'transform 0.2s', transform: isCategoryExpanded ? 'rotate(180deg)' : 'rotate(0deg)', display: 'inline-block' }}>▼</span>
                        </button>
                      )}
                    </nav>
                  </div>
                );
              })()}
            </div>
          </div>
        </div>
      </header>

      {/* 移动端Header：短视频沉浸、用户组全屏播放、视频播放页时隐藏（isUserVideoPlayerPage 同步路径判断，避免事件延迟导致 logo 闪一帧） */}
      {!isMobileShortImmersive && !isUserGroupFullScreenOpen && !isUserVideoPlayerPage && (
        <header
          id="mobile-header"
          className="md:hidden text-white fixed top-0 left-0 right-0 z-50"
          style={{
            backgroundColor: isUserDetailPage ? 'rgba(0, 0, 0, 0.3)' : 'rgba(49, 48, 48, 0.9)',
            backdropFilter: isUserDetailPage ? 'blur(20px) saturate(180%)' : undefined,
            WebkitBackdropFilter: isUserDetailPage ? 'blur(20px) saturate(180%)' : undefined
          }}
        >
          <div className="px-4 py-2">
            <div className="flex items-center justify-between">
              {/* 左侧：用户组页显示返回按钮（模糊玻璃），否则显示菜单 */}
              {isUserDetailPage ? (
                <button
                  onClick={() => {
                    const sp = new URLSearchParams(location.search);
                    const fromPage = sp.get('from');
                    const categoryId = sp.get('categoryId');
                    const page = sp.get('page');
                    const keyword = sp.get('keyword');
                    if (fromPage === 'userlist' && categoryId) {
                      const parts = [];
                      if (page) parts.push(`page=${encodeURIComponent(page)}`);
                      if (keyword) parts.push(`keyword=${encodeURIComponent(keyword)}`);
                      const query = parts.join('&');
                      navigate(`/category/${categoryId}${query ? `?${query}` : ''}`);
                    } else {
                      navigate(-1);
                    }
                  }}
                  className="text-white p-2 rounded-full transition-all duration-200 hover:scale-110 flex items-center justify-center"
                  style={{
                    width: '40px',
                    height: '40px',
                    backgroundColor: 'rgba(255, 255, 255, 0.15)',
                    backdropFilter: 'blur(20px) saturate(180%)',
                    WebkitBackdropFilter: 'blur(20px) saturate(180%)',
                    border: '1px solid rgba(255, 255, 255, 0.3)',
                    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2)'
                  }}
                  aria-label="返回"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
              ) : (
                <button
                  onClick={toggleMobileMenu}
                  className="text-white p-1"
                >
                  <FiMenu size={28} />
                </button>
              )}

              {/* 居中：用户组页显示"作品列表"标题，其他页显示logo */}
              <div className="flex-1 flex justify-center items-center">
                {isUserDetailPage ? (
                  <span className="text-base font-semibold text-white">作品列表</span>
                ) : (
                  <a
                    href="/"
                    onClick={handleHomeClick}
                    className="cursor-pointer"
                  >
                    <img
                      src="/logo.png"
                      alt="天涯吃瓜"
                      style={{
                        height: '30px',
                        width: 'auto',
                        objectFit: 'contain',
                        display: 'block'
                      }}
                    />
                  </a>
                )}
              </div>

              {/* 右侧：用户组页显示分享（模糊玻璃），短视频分类显示分享，否则占位 */}
              <div style={{ width: '60px', display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '8px' }}>
                {isUserDetailPage ? (
                  <button
                    onClick={handleShare}
                    className="text-white p-2 rounded-full transition-all duration-200 hover:scale-110 flex items-center justify-center"
                    title="分享"
                    style={{
                      width: '40px',
                      height: '40px',
                      backgroundColor: 'rgba(255, 255, 255, 0.15)',
                      backdropFilter: 'blur(20px) saturate(180%)',
                      WebkitBackdropFilter: 'blur(20px) saturate(180%)',
                      border: '1px solid rgba(255, 255, 255, 0.3)',
                      boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2)'
                    }}
                  >
                    <FiShare2 size={18} />
                  </button>
                ) : isMobile && location.pathname.includes('/category/') ? (
                  <button
                    onClick={handleShare}
                    className="text-white text-sm px-3 py-1 hover:bg-white/20 rounded transition-colors"
                    title="分享当前视频"
                    style={{
                      border: '1px solid rgba(255,255,255,0.3)',
                      backgroundColor: 'rgba(255,255,255,0.1)'
                    }}
                  >
                    分享
                  </button>
                ) : null}
              </div>
            </div>
          </div>
        </header>
      )}

      {/* 移动端抽屉（滑入动画+推动页面） */}
      {/* 🔧 修复：short-video-mode 下降低 z-index，确保不会覆盖页面，而是推动页面 */}
      <div className="mobile-drawer fixed inset-y-0 left-0 md:hidden" style={{ width: '256px', zIndex: '40' }}>
        {/* 侧边菜单内容（不遮罩，推动页面） */}
        <div 
          className="fixed left-0 top-0 h-full w-64 text-white overflow-y-auto mobile-menu-scrollbar" 
          style={{ backgroundColor: '#191919' }}
        >
            {/* 菜单头部 */}
            <div className="p-4 border-b border-gray-700">
              <div className="flex items-center justify-between mb-4">
                <button
                  onClick={toggleMobileMenu}
                  className="text-white p-1"
                >
                  <FiMenu size={24} />
                </button>
                <div className="flex items-center space-x-4 text-sm">
                  <button
                    type="button"
                  onClick={(e) => {
                    e.preventDefault();
                    // 标记路由导航，关闭抽屉
                    sessionStorage.setItem('route_navigation', 'true');
                    toggleMobileMenu(false);
                    try { switchToHome?.(); } catch (_) {}
                    // 优先使用前端路由跳转
                    try {
                      navigate('/', { state: { forceTop: true } });
                    } catch (_) {}
                      // 兜底：若当前已在首页则强制触发滚动置顶；否则硬跳转
                      try {
                        if (window.location && window.location.pathname === '/') {
                          window.scrollTo(0, 0);
                        } else {
                          setTimeout(() => { try { window.location.assign('/'); } catch (_) {} }, 0);
                        }
                      } catch (_) {}
                    }}
                    className="text-gray-200 hover:text-white transition-colors"
                    style={{ background: 'transparent' }}
                  >
                    {getSiteConfig('site_name') || '天涯吃瓜'}
                  </button>
                </div>
                <button
                  onClick={toggleMobileMenu}
                  className="text-gray-400 hover:text-white"
                >
                  <FiX size={20} />
                </button>
              </div>
              
              {/* 品牌Logo区域 */}
              <div className="text-center mb-4">
                {/* 移动端品牌图标 - removebg-preview.png */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  height: '72px',
                  marginBottom: '0px'
                }}>
                  {/* 移动端天涯吃瓜品牌Logo */}
                  <button
                    type="button"
                  onClick={(e) => {
                    e.preventDefault();
                    sessionStorage.setItem('route_navigation', 'true');
                    toggleMobileMenu(false);
                    try { switchToHome?.(); } catch (_) {}
                    try { navigate('/', { state: { forceTop: true } }); } catch (_) {}
                  }}
                    style={{ border: 'none', padding: 0, cursor: 'pointer', backgroundColor: 'transparent' }}
                  >
                    <img
                      src="/logo.png"
                      alt="天涯吃瓜"
                      style={{ width: '180px', height: 'auto', objectFit: 'contain', display: 'block' }}
                    />
                  </button>
                </div>
              </div>
              
              {/* 搜索框 */}
              <form onSubmit={handleSearch} className="relative">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="搜索..."
                  className="w-full text-white placeholder-gray-400 rounded-full px-4 py-2 pr-10 focus:outline-none"
                  style={{
                    backgroundColor: '#202322',
                    border: '1px solid #4D4F4E'
                  }}
                />
                <button
                  type="submit"
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-white"
                >
                  <FiSearch size={16} />
                </button>
              </form>
            </div>

            {/* 导航菜单 */}
            <nav className="p-4">
              {/* 分类导航 */}
              {categories.map((category) => {
                const isRecommended = Number(category?.isRecommended) === 1;
                return (
                  <button
                    key={category.id}
                    onClick={(e) => {
                      e.preventDefault();
                      handleCategoryClick(e, category.id);
                      toggleMobileMenu(false);
                    }}
                    className={`block w-full text-center py-3 text-lg transition-colors duration-200 ${isRecommended ? 'recommended-gradient-text' : 'text-gray-300 hover:text-white'}`}
                    style={{ color: isRecommended ? 'transparent' : undefined, background: isRecommended ? undefined : 'transparent' }}
                  >
                    {category.name}
                  </button>
                );
              })}

              {/* 更多：可折叠竖向列表，左对齐、展开时标题为青色 + ▲（▼ 旋转） */}
              {moreMenuItems.length > 0 && (
                <div className="mt-1 mb-2">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      setShowMobileMoreSubmenu((v) => !v);
                    }}
                    className="flex w-full items-center gap-1.5 py-2.5 text-left text-base transition-colors"
                    style={{
                      color: showMobileMoreSubmenu ? '#1ABC9C' : '#d1d5db',
                      background: 'transparent',
                      border: 'none',
                      cursor: 'pointer'
                    }}
                  >
                    <span>更多</span>
                    <span
                      className="text-xs leading-none transition-transform duration-200"
                      style={{
                        transform: showMobileMoreSubmenu ? 'rotate(180deg)' : 'rotate(0deg)',
                        display: 'inline-block'
                      }}
                    >
                      ▼
                    </span>
                  </button>
                  {showMobileMoreSubmenu && (
                    <div
                      className="rounded-xl overflow-hidden"
                      style={{
                        backgroundColor: '#2d2d2d',
                        border: '1px solid #404040',
                        boxShadow: '0 8px 24px rgba(0,0,0,0.35)'
                      }}
                    >
                      {moreMenuItems.map((item) => (
                        <button
                          key={item.configId}
                          type="button"
                          onClick={() => {
                            handleMoreMenuClick(item);
                            toggleMobileMenu(false);
                          }}
                          className="block w-full text-left px-4 py-3 text-[15px] leading-snug border-b border-gray-600/50 last:border-b-0 hover:bg-[#403E3F] transition-colors"
                          style={{ color: '#e8e8e8' }}
                        >
                          {item.basicContent}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </nav>

            {/* 底部社交链接 */}
            <div className="p-4 border-t border-gray-700" style={{ backgroundColor: '#131313' }}>
              <div className="flex justify-center space-x-4">


                {/* 邮件 */}
                <button 
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onClick={handleEmailLink}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FaEnvelope size={16} />
                </button>

                {/* Twitter/X */}
                <button 
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onClick={() => handleSocialLink('twitter')}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FaTwitter size={16} />
                </button>

                {/* Telegram */}
                <button 
                  className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
                  style={{ 
                    color: 'rgba(255, 255, 255, 0.9)',
                    backgroundColor: 'rgba(0, 0, 0, 0.3)'
                  }}
                  onClick={() => handleSocialLink('telegram')}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                    e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                    e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                >
                  <FaTelegram size={16} />
                </button>


              </div>
            </div>
          </div>
        </div>

      {isMobileMenuOpen && (
        <div
          className="fixed inset-y-0 md:hidden"
          style={{ left: '256px', right: 0, zIndex: 40, background: 'transparent' }}
          onClick={toggleMobileMenu}
        />
      )}

      {/* 搜索弹窗 */}
      {isSearchModalOpen && (
        <div className="fixed inset-0 z-50">
          {/* 遮罩层 */}
          <div 
            className="fixed inset-0 bg-black bg-opacity-50"
            onClick={toggleSearchModal}
          />
          
          {/* 搜索框容器 */}
          <div className="fixed top-20 left-1/2 transform -translate-x-1/2 w-full max-w-2xl px-4">
            <div className="bg-white rounded-lg shadow-xl p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-4">搜索内容</h3>
              <form onSubmit={handleSearch} className="relative">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="输入你想搜索的关键词..."
                  className="w-full text-white placeholder-gray-400 rounded-full px-6 py-3 pr-12 focus:outline-none text-base"
                  style={{
                    backgroundColor: '#202322',
                    border: '1px solid #4D4F4E'
                  }}
                  autoFocus
                />
                <button
                  type="submit"
                  className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-blue-500"
                >
                  <FiSearch size={20} />
                </button>
              </form>
              
              <div className="mt-4 flex justify-end">
                <button
                  onClick={toggleSearchModal}
                  className="px-4 py-2 text-gray-600 hover:text-gray-800 transition-colors"
                >
                  取消
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
});

Header.displayName = 'Header';

export default Header; /* Updated site name configuration Mon Aug  4 04:29:45 CST 2025 */
/* Mobile search button color changed to white Mon Aug  4 05:30:14 CST 2025 */
