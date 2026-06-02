import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import videoStatsService from '../../services/videoStatsService';
import './MobileAppCta.css';

/**
 * 移动端底部悬浮 APP 引流按钮
 * - 仅移动端（< 768px）显示
 * - 短视频沉浸式模式（isMobileShortVideo）下不显示
 * - 点击关闭按钮：仍触发跳转，按钮保持显示（不隐藏）
 * - 点击 → 新标签打开 51 APP 落地页，并上报埋点
 */
const CTA_LINK = 'https://ldyrk.xn--dqrx9oo71b.xn--fiqs8s?channelCode=tylt';

const MobileAppCta = ({ isMobileShortVideo = false }) => {
  const location = useLocation();
  const [isMobile, setIsMobile] = useState(typeof window !== 'undefined' && window.innerWidth < 768);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const onResize = () => setIsMobile(window.innerWidth < 768);
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  useEffect(() => {
    const t = setTimeout(() => setMounted(true), 60);
    return () => clearTimeout(t);
  }, []);

  const isShortVideoRoute = /^\/category\/\d+$/.test(location.pathname);
  const hideOnRoute = isMobileShortVideo && isShortVideoRoute;
  const isUserGroupFullScreen = /^\/user\/[^/]+\/video/.test(location.pathname);

  if (!isMobile || hideOnRoute || isUserGroupFullScreen) return null;

  const openCta = (source) => {
    try {
      videoStatsService.trackEvent('app_cta_click', {
        targetId: source === 'close' ? 'mobile_bottom_close' : 'mobile_bottom',
        path: location.pathname + (location.search || '')
      });
    } catch (_) {}
    try {
      window.open(CTA_LINK, '_blank', 'noopener,noreferrer');
    } catch (_) {
      window.location.href = CTA_LINK;
    }
  };

  const handleClose = (e) => {
    e.stopPropagation();
    e.preventDefault();
    openCta('close');
  };

  const handleClick = () => {
    openCta('cta');
  };

  return (
    <div
      className={`mobile-app-cta ${mounted ? 'mobile-app-cta--in' : ''}`}
      role="button"
      tabIndex={0}
      onClick={handleClick}
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleClick(); }}
      aria-label="天涯吃瓜 APP 惊艳上线"
    >
      <div className="mobile-app-cta__logo" aria-hidden="true">
        <span className="mobile-app-cta__logo-text">瓜</span>
      </div>
      <span className="mobile-app-cta__text">天涯吃瓜APP惊艳上线</span>
      <button
        className="mobile-app-cta__close"
        type="button"
        aria-label="关闭"
        onClick={handleClose}
      >
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
          <path d="M6 6L18 18M6 18L18 6" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
        </svg>
      </button>
    </div>
  );
};

export default MobileAppCta;
