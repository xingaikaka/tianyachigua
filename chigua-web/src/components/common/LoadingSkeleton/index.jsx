import React from 'react';
import './LoadingSkeleton.css';

// 视频详情页骨架屏
export const VideoDetailSkeleton = () => {
  return (
    <div className="video-detail-skeleton">
      <div className="container mx-auto px-4 py-8 max-w-5xl">
        {/* 视频标题骨架屏 */}
        <div className="skeleton skeleton-title mb-6"></div>
        
        {/* 视频内容区域骨架屏 */}
        <div className="skeleton skeleton-video-content mb-8"></div>
        
        {/* 统计信息骨架屏 */}
        <div className="flex gap-4 mb-6">
          <div className="skeleton skeleton-stat"></div>
          <div className="skeleton skeleton-stat"></div>
          <div className="skeleton skeleton-stat"></div>
        </div>
        
        {/* 富文本内容骨架屏 */}
        <div className="space-y-3 mb-8">
          <div className="skeleton skeleton-text-line"></div>
          <div className="skeleton skeleton-text-line"></div>
          <div className="skeleton skeleton-text-line short"></div>
        </div>
        
        {/* 推荐视频骨架屏 */}
        <div className="skeleton skeleton-recommendations"></div>
      </div>
    </div>
  );
};

// 视频列表骨架屏
export const VideoListSkeleton = () => {
  return (
    <div className="video-list-skeleton">
      <div className="container mx-auto px-4 py-8 max-w-5xl">
        <div className="space-y-8">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="skeleton skeleton-video-card"></div>
          ))}
        </div>
      </div>
    </div>
  );
};

// 通用骨架屏组件
const LoadingSkeleton = ({ type = 'default', className = '' }) => {
  const renderSkeleton = () => {
    switch (type) {
      case 'video-detail':
        return <VideoDetailSkeleton />;
      case 'video-list':
        return <VideoListSkeleton />;
      default:
        return <div className={`skeleton ${className}`}></div>;
    }
  };

  return renderSkeleton();
};

export default LoadingSkeleton;
