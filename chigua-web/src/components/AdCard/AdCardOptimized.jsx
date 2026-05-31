import React, { useState, useEffect } from 'react';
import advertisementService from '../../services/advertisementService';

const AdCardOptimized = ({ ad, index }) => {
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [imageLoaded, setImageLoaded] = useState(false);
  const [imageError, setImageError] = useState(false);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 768);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

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

  // 图片加载成功处理
  const handleImageLoad = () => {
    setImageLoaded(true);
    setImageError(false);
  };

  // 图片加载失败处理
  const handleImageError = () => {
    setImageError(true);
    setImageLoaded(false);
    
    // 可以在这里添加备用广告逻辑
    // 比如记录失败的广告URL，用于后续优化
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
        {/* 加载状态显示 */}
        {!imageLoaded && !imageError && (
          <div 
            className="absolute inset-0 flex items-center justify-center bg-gray-800"
            style={{ backgroundColor: '#2A2828' }}
          >
            <div className="animate-pulse">
              <div className="w-16 h-16 bg-gray-600 rounded-lg"></div>
            </div>
          </div>
        )}

        {/* 错误状态显示 */}
        {imageError && (
          <div 
            className="absolute inset-0 flex items-center justify-center bg-gray-800"
            style={{ backgroundColor: '#2A2828' }}
          >
            <img
              src="/loding.jpg"
              alt="广告加载失败"
              className="w-full h-full object-fill"
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'fill',
                display: 'block'
              }}
            />
          </div>
        )}

        {/* 广告图片 - 直接使用img标签 */}
        <img
          src={ad.imageUrl}
          alt="广告"
          className={`w-full h-full transition-opacity duration-300 ${
            imageLoaded ? 'opacity-100' : 'opacity-0'
          }`}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'fill',
            display: 'block'
          }}
          onLoad={handleImageLoad}
          onError={handleImageError}
          loading="lazy" // 原生懒加载
        />

        {/* 鼠标悬停时的遮罩效果 */}
        <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-10 transition-opacity duration-300"></div>
      </div>
    </div>
  );
};

export default AdCardOptimized;
