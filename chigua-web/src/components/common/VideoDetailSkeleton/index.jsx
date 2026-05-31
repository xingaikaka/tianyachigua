import React from 'react';
import './VideoDetailSkeleton.css';

const VideoDetailSkeleton = ({ title, showProgress = false }) => {
  return (
    <div className="min-h-screen text-white" style={{ backgroundColor: '#2C2A2A' }}>
      {/* 页面标题 - 立即显示 */}
      <div className="container mx-auto px-4 max-w-6xl pt-8 sm:pt-12">
        <div className="flex justify-center mb-12">
          <div className="w-full text-center" style={{ maxWidth: '770px' }}>
            {title ? (
              <h1 className="text-3xl md:text-4xl font-normal mb-6 leading-relaxed animate-fade-in" 
                  style={{ color: '#BCBCBC' }}>
                {title}
              </h1>
            ) : (
              <div className="skeleton-title"></div>
            )}
            
            {/* 基本信息骨架 */}
            <div className="skeleton-info">
              <div className="skeleton-date"></div>
              <div className="skeleton-stats"></div>
            </div>
          </div>
        </div>

        {/* 视频播放器区域骨架 */}
        <div className="flex justify-center mb-12">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <div className="skeleton-player">
              {showProgress && (
                <div className="skeleton-progress">
                  <div className="skeleton-progress-bar"></div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 内容区域骨架 */}
        <div className="flex justify-center mb-12">
          <div className="w-full" style={{ maxWidth: '770px' }}>
            <div className="skeleton-content">
              <div className="skeleton-line"></div>
              <div className="skeleton-line"></div>
              <div className="skeleton-line short"></div>
            </div>
          </div>
        </div>

        {/* 侧边栏和评论区域骨架 */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
          <div className="lg:col-span-2">
            <div className="skeleton-comments">
              <div className="skeleton-comment-title"></div>
              <div className="skeleton-comment-item"></div>
              <div className="skeleton-comment-item"></div>
            </div>
          </div>
          <div className="lg:col-span-1">
            <div className="skeleton-sidebar">
              <div className="skeleton-sidebar-item"></div>
              <div className="skeleton-sidebar-item"></div>
              <div className="skeleton-sidebar-item"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VideoDetailSkeleton;
