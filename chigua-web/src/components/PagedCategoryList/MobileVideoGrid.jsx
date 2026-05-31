import React from 'react';
import MobileVideoCard from './MobileVideoCard';
import './MobileVideoGrid.css';

/**
 * 移动端双列视频网格组件
 * 专为分页模式手机端设计，每行显示2个视频卡片
 */
const MobileVideoGrid = ({ videos, onVideoClick }) => {
  if (!videos || videos.length === 0) {
    return (
      <div className="mobile-video-grid-empty">
        <p>暂无视频</p>
      </div>
    );
  }

  return (
    <div className="mobile-video-grid">
      {videos.map((video, index) => (
        <div key={video.id || index} className="mobile-video-grid-item">
          <MobileVideoCard
            video={video}
            index={index}
            onClick={onVideoClick}
          />
        </div>
      ))}
    </div>
  );
};

export default MobileVideoGrid;

