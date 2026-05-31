import React, { useEffect, useRef, useState } from 'react';
import advertisementService from '../../services/advertisementService';
import adStatsTracker from '../../utils/adStatsTracker';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

/**
 * 分页模式单个广告组件
 * 横幅样式，单独显示一个广告
 */
const PagedSingleAd = ({ ad, onAdClick, style }) => {
  const containerRef = useRef(null);
  const [imageLoaded, setImageLoaded] = useState(false);

  // 曝光统计
  useEffect(() => {
    const el = containerRef.current;
    if (!el || !ad || !ad.id) return undefined;

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;

        if (!adStatsTracker.startImpression(ad.id)) return;

        advertisementService.recordAdImpression(ad.id).catch(() => {
          adStatsTracker.cancelImpression(ad.id);
        });
      });
    }, { threshold: 0.5 });

    observer.observe(el);
    return () => observer.disconnect();
  }, [ad]);

  // 点击处理
  const handleClick = () => {
    if (!ad || !ad.id) return;

    advertisementService.clickAd(ad.id).catch(() => { });

    if (onAdClick) {
      onAdClick(ad);
    }

    if (ad.linkUrl) {
      window.open(ad.linkUrl, '_blank');
    }
  };

  if (!ad) return null;

  return (
    <div
      ref={containerRef}
      onClick={handleClick}
      style={{
        width: '100%',
        borderRadius: '8px',
        overflow: 'hidden',
        cursor: 'pointer',
        backgroundColor: '#f5f5f5',
        ...style
      }}
    >
      {/* 使用固定宽高比容器防止布局抖动 - 匹配视频卡片高度 */}
      <div
        style={{
          position: 'relative',
          width: '100%',
          aspectRatio: '16 / 9', // 与视频卡片保持一致的比例
          backgroundColor: '#1e1e1e'
        }}
      >
        <SecureDecryptedImage
          src={ad.imageUrl}
          alt={ad.title || '广告'}
          priority="high"
          lazyLoad={false}
          objectFit="cover"
          style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' }}
          onLoad={() => setImageLoaded(true)}
        />
      </div>
      {ad.title && (
        <div
          style={{
            padding: '12px',
            fontSize: '14px',
            color: '#333',
            textAlign: 'center',
            backgroundColor: '#fff'
          }}
        >
          {ad.title}
        </div>
      )}
    </div>
  );
};

export default PagedSingleAd;

