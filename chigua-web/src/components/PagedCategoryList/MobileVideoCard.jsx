import React from 'react';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

/**
 * 移动端视频卡片组件
 * 用于分页模式手机端的双列网格显示
 */
const MobileVideoCard = ({ video, index, onClick }) => {
  if (!video) {
    return null;
  }

  const handleCardClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (onClick) {
      onClick(video);
    }
  };

  return (
    <div
      className="mobile-video-card"
      onClick={handleCardClick}
    >
      {/* 视频封面 */}
      <div className="mobile-video-card-cover-wrapper">
        {video.coverImageUrl && (
          <SecureDecryptedImage
            src={video.coverImageUrl}
            alt={video.title || '视频封面'}
            className="mobile-video-card-cover"
            lazyLoad={index >= 6}
            priority={index < 6 ? 'high' : 'normal'}
          />
        )}
        
        {/* 热门/推荐标签 */}
        {video.isHot === 1 && (
          <div className="mobile-video-card-badge hot-badge">
            <span className="badge-icon">🔥</span>
            <span className="badge-text">HOT</span>
          </div>
        )}
        {video.isRecommended === 1 && video.isHot !== 1 && (
          <div className="mobile-video-card-badge recommended-badge">
            <span className="badge-icon">⭐</span>
            <span className="badge-text">推荐</span>
          </div>
        )}
      </div>
      
      {/* 视频信息 */}
      <div className="mobile-video-card-info">
        {/* 标题 */}
        <div className="mobile-video-card-title">
          {video.title || '无标题'}
        </div>
        
        {/* 发布时间 */}
        {video.publishedAt && (
          <div className="mobile-video-card-time">
            {new Date(video.publishedAt).toLocaleDateString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit'
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default MobileVideoCard;

