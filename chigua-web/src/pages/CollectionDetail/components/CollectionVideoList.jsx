import React from 'react';
import { useNavigate } from 'react-router-dom';
import SecureDecryptedImage from '../../../components/common/SecureDecryptedImage';

const CollectionVideoList = ({ videos = [] }) => {
  const navigate = useNavigate();

  // 处理视频点击
  const handleVideoClick = (videoId) => {
    navigate(`/video/${videoId}`);
  };

  if (!videos || videos.length === 0) {
    return (
      <div className="text-center text-gray-400 py-8">
        <p>暂无视频</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 标题 */}
      <div className="text-white text-lg font-medium mb-6">
        合集视频列表 ({videos.length})
      </div>
      
      {/* 视频列表 */}
      <div className="space-y-3">
        {videos.map((video, index) => (
          <div
            key={video.id}
            className="group flex items-center bg-gray-800 bg-opacity-40 rounded-lg p-3 hover:bg-opacity-60 cursor-pointer transition-all duration-200"
            onClick={() => handleVideoClick(video.id)}
          >
            {/* 序号 */}
            <div className="flex-shrink-0 w-8 text-center">
              <span className="text-gray-400 text-sm font-medium">
                {String(index + 1).padStart(2, '0')}
              </span>
            </div>

            {/* 视频缩略图 */}
            <div className="flex-shrink-0 ml-3">
              <SecureDecryptedImage
                src={video.coverImage || video.coverImageUrl}
                alt={video.title}
                className="rounded-md object-cover"
                lazyLoad={false}
                style={{ 
                  width: '120px', 
                  height: '68px' 
                }}
              >
                {/* 播放按钮覆盖层 */}
                <div className="absolute inset-0 flex items-center justify-center bg-black bg-opacity-0 group-hover:bg-opacity-30 transition-all duration-200">
                  <div className="w-8 h-8 bg-white bg-opacity-80 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                    <svg className="w-4 h-4 text-gray-800 ml-0.5" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M8 5v14l11-7z"/>
                    </svg>
                  </div>
                </div>
              </SecureDecryptedImage>
            </div>

            {/* 视频信息 */}
            <div className="flex-1 ml-4 min-w-0">
              <h3 className="text-white text-sm font-medium line-clamp-2 mb-1">
                {video.title}
              </h3>
              
              <div className="flex items-center text-xs text-gray-400 space-x-3">
                {/* 时长 */}
                {video.duration && (
                  <span>
                    {Math.floor(video.duration / 60)}:{String(video.duration % 60).padStart(2, '0')}
                  </span>
                )}
                
                {/* 观看次数已移除 */}
                
                {/* 发布时间 */}
                {video.publishedAt && (
                  <span>
                    {new Date(video.publishedAt).toLocaleDateString()}
                  </span>
                )}
              </div>
            </div>

            {/* 右侧箭头 */}
            <div className="flex-shrink-0 ml-3">
              <svg 
                className="w-5 h-5 text-gray-500 group-hover:text-white transition-colors duration-200" 
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default CollectionVideoList; 