import React, { useState, useEffect } from 'react';
import advertisementService from '../../services/advertisementService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

const AdCard = ({ ad, index }) => {
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 768);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 组件挂载时上报曝光（每个广告只上报一次）
  useEffect(() => {
    if (ad && ad.id) {
      try { advertisementService.recordAdImpression(ad.id); } catch (_) {}
    }
  }, [ad?.id]);
  // 如果没有图片URL，不显示广告
  if (!ad.imageUrl) {
    return null;
  }

  // 处理广告点击
  const handleAdClick = async () => {
    try {
      // 统计点击
      await advertisementService.clickAd(ad.id);

      // 跳转链接
      if (ad.linkUrl) {
        window.open(ad.linkUrl, '_blank');
      }
    } catch (error) {
      
      // 仍然执行跳转
      if (ad.linkUrl) {
        window.open(ad.linkUrl, '_blank');
      }
    }
  };

  return (
    <div 
      className="group cursor-pointer transform transition-all duration-300 hover:scale-[1.03] hover:shadow-2xl w-full" 
      style={{ maxWidth: '860px' }}
      onClick={handleAdClick}
    >
      <div
        className={`relative rounded-lg overflow-hidden shadow-lg transition-all duration-300 group-hover:shadow-xl ${!isMobile ? '' : 'aspect-video'}`}
        style={{ height: isMobile ? 'auto' : '280px' }}
      >
        <SecureDecryptedImage
          src={ad.imageUrl}
          alt="广告"
          className="w-full h-full"
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'fill',
            display: 'block'
          }}
          fallbackSrc="/loding.jpg"
          lazyLoad={true}
          priority="normal"
        />

        {/* 鼠标悬停时的遮罩效果 */}
        <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-10 transition-opacity duration-300"></div>
      </div>
    </div>
  );
};

export default AdCard; 