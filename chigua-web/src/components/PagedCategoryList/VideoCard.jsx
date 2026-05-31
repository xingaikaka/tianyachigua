import React from 'react';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

/**
 * 视频卡片组件
 * 用于分页模式列表中的视频卡片显示
 */
const VideoCard = ({ video, index, onClick }) => {
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
      className="paged-video-card"
      onClick={handleCardClick}
    >
      {/* 视频封面 */}
      {video.coverImageUrl && (
        <SecureDecryptedImage
          src={video.coverImageUrl}
          alt={video.title || '视频封面'}
          className="paged-video-card-cover"
          lazyLoad={index >= 6}
          priority={index < 6 ? 'high' : 'normal'}
        />
      )}
      
      {/* 视频信息 */}
      <div className="paged-video-card-info">
        {/* 标题 */}
        <div className="paged-video-card-title">
          {video.title || '无标题'}
        </div>
      </div>
    </div>
  );
};

export default VideoCard;

