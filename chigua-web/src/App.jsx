import React, { useEffect, useRef } from 'react';
import { FiShuffle } from 'react-icons/fi';
import { BrowserRouter as Router, useLocation } from 'react-router-dom';
import StaticHeader from './components/common/StaticHeader';
import ContentRouter from './components/common/ContentRouter';
import SmartContentSwitcher from './components/common/SmartContentSwitcher';
import LoadingBar from './components/common/LoadingBar';
import PopupAd from './components/PopupAd';
import MobileAppCta from './components/MobileAppCta';
import videoStatsService from './services/videoStatsService';

import { CategoriesProvider } from './context/CategoriesContext';
import { CategoryContentProvider } from './context/CategoryContentContext';
import './App.css';
import './styles/components.css';
import './styles/globals.css';
import './components/common/PageTransition/PageTransition.css';
import './components/common/VideoDetailSkeleton/VideoDetailSkeleton.css';
import './components/common/SmartContentSwitcher/SmartContentSwitcher.css';

// 内部App组件，可以使用useLocation
const AppContent = () => {
  const location = useLocation();
  const [isMobile, setIsMobile] = React.useState(false);
  const [isMobileShortVideo, setIsMobileShortVideo] = React.useState(false);
  
  // 移动端检测
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };
    
    checkMobile();
    window.addEventListener('resize', checkMobile);
    
    return () => window.removeEventListener('resize', checkMobile);
  }, []);
  
  useEffect(() => {
    const handleCategoryViewUpdate = (event) => {
      const { isMobileShort } = event.detail || {};
      setIsMobileShortVideo(Boolean(isMobileShort));
    };
    window.addEventListener('categoryView:update', handleCategoryViewUpdate);
    return () => {
      window.removeEventListener('categoryView:update', handleCategoryViewUpdate);
    };
  }, []);

  useEffect(() => {
    if (!location.pathname.match(/^\/category\/(\d+)$/)) {
      setIsMobileShortVideo(false);
    }
  }, [location.pathname]);

  // 全局路由 PV 埋点：所有页面切换都自动上报，避免在各页面重复埋点
  // - 防重复：同一 path+search 在 1.5s 内不重复发送（React 双渲染 / 快速跳转）
  // - 节流：路由切换后 300ms 延迟发送，避免快速连点产生大量请求
  const lastPvKeyRef = useRef('');
  const lastPvAtRef  = useRef(0);
  useEffect(() => {
    const fullPath = location.pathname + (location.search || '');
    const now = Date.now();
    if (fullPath === lastPvKeyRef.current && now - lastPvAtRef.current < 1500) {
      return;
    }
    const timer = setTimeout(() => {
      lastPvKeyRef.current = fullPath;
      lastPvAtRef.current  = Date.now();
      try { videoStatsService.trackPageView(fullPath); } catch (_) {}
    }, 300);
    return () => clearTimeout(timer);
  }, [location.pathname, location.search]);
  
  // 使用浏览器原生滚动恢复策略
  useEffect(() => {
    if ('scrollRestoration' in window.history) {
      try {
        window.history.scrollRestoration = 'auto';
      } catch (_) {}
    }
  }, []);

  // 🚀 启用浏览器Back-Forward Cache优化
  useEffect(() => {
    // 监听页面显示事件（包括从bfcache恢复）
    const handlePageShow = (event) => {
      if (event.persisted) {
        // 页面从bfcache恢复，不需要重新加载数据
      }
    };

    // 监听页面隐藏事件
    const handlePageHide = (event) => {
      // 确保页面能被缓存到bfcache
    };

    // 添加事件监听器
    window.addEventListener('pageshow', handlePageShow);
    window.addEventListener('pagehide', handlePageHide);

    return () => {
      window.removeEventListener('pageshow', handlePageShow);
      window.removeEventListener('pagehide', handlePageHide);
    };
  }, []);
  
  // 判断是否是需要显示详情页面或其他特殊页面的路由
  const isDetailPage = location.pathname.includes('/video/') || 
                      location.pathname.includes('/collection/') ||
                      location.pathname.includes('/search') ||
                      location.pathname.includes('/archives') ||
                      location.pathname.includes('/tags') ||
                      location.pathname.includes('/tag/') ||
                      location.pathname.includes('/keyword/') ||
                      location.pathname.includes('/submission') ||
                      location.pathname.includes('/homeway');
  
  // 判断是否是首页或分类页面（使用SmartContentSwitcher）
  const isListPage = location.pathname === '/' || 
                    location.pathname.match(/^\/category\/\d+$/);
 
  React.useLayoutEffect(() => {
    const className = 'short-video-mode';
    if (isMobileShortVideo) {
      document.body.classList.add(className);
    } else {
      document.body.classList.remove(className);
    }
    return () => document.body.classList.remove(className);
  }, [isMobileShortVideo]);

  
  return (
    <div className="App min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      <LoadingBar />
      <StaticHeader />

      {isMobileShortVideo && (
        <>
          <button
            className="fixed top-4 left-4 z-50 text-white p-2 rounded-full transition-all duration-200 hover:scale-110"
            style={{ 
              width: '40px',
              height: '40px',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              backdropFilter: 'blur(20px) saturate(180%)',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
            aria-label="打开菜单"
            onClick={() => {
              document.body.classList.add('short-video-mode');
              window.dispatchEvent(new CustomEvent('mobileMenu:control', { detail: { action: 'open', source: 'shortVideo' } }));
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3 12H21M3 6H21M3 18H21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </button>

          <button
            className="fixed top-4 left-1/2 -translate-x-1/2 z-50 text-white p-2 rounded-full transition-all duration-200 hover:scale-110"
            style={{ 
              width: '40px',
              height: '40px',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              backdropFilter: 'blur(20px) saturate(180%)',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
            aria-label="随机切换短视频页"
            onClick={() => {
              window.dispatchEvent(new CustomEvent('shortVideo:randomPage'));
            }}
          >
            <FiShuffle size={16} />
          </button>

          <button
            className="fixed top-4 right-4 z-50 text-white p-2 rounded-full text-sm transition-all duration-200 hover:scale-110"
            style={{ 
              width: '40px',
              height: '40px',
              backgroundColor: 'rgba(255, 255, 255, 0.15)',
              backdropFilter: 'blur(20px) saturate(180%)',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
            aria-label="分享当前视频"
            onClick={() => {
              window.dispatchEvent(new CustomEvent('shortVideo:share'));
            }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 8C19.6569 8 21 6.65685 21 5C21 3.34315 19.6569 2 18 2C16.3431 2 15 3.34315 15 5C15 5.18703 15.0124 5.37138 15.0361 5.55111L8.35589 9.8914C7.74927 9.33481 6.9123 9 6 9C4.34315 9 3 10.3431 3 12C3 13.6569 4.34315 15 6 15C6.9123 15 7.74927 14.6652 8.35589 14.1086L15.0361 18.4489C15.0124 18.6286 15 18.813 15 19C15 20.6569 16.3431 22 18 22C19.6569 22 21 20.6569 21 19C21 17.3431 19.6569 16 18 16C17.0877 16 16.2507 16.3348 15.6441 16.8914L8.96389 12.5511C8.98762 12.3714 9 12.187 9 12C9 11.813 8.98762 11.6286 8.96389 11.4489L15.6441 7.1086C16.2507 7.66519 17.0877 8 18 8Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </button>
        </>
      )}
    
      {/* 主要内容区域 - 移动端短视频页面去除顶部间距 */}
      <main 
        className={`min-h-screen ${isMobileShortVideo ? 'pt-0' : 'pt-16 md:pt-20'}`} 
        style={{ backgroundColor: '#2C2A2A' }}
      >
        <ContentRouter />
        {/* 暂时隐藏SmartContentSwitcher，让我们先确保基本路由能工作 */}
        {/* <SmartContentSwitcher /> */}
      </main>
    
      {/* 弹窗广告 */}
      <PopupAd />

      {/* 移动端底部 APP 引流悬浮按钮（短视频沉浸式模式自动隐藏） */}
      <MobileAppCta isMobileShortVideo={isMobileShortVideo} />
      
      {/* 缓存调试组件（已禁用） */}
      {/* <CacheDebug /> */}
      
    </div>
  );
};

function App() {
  return (
    <Router>
      <CategoriesProvider>
        <CategoryContentProvider>
          <AppContent />
        </CategoryContentProvider>
      </CategoriesProvider>
    </Router>
  );
}

export default App;
