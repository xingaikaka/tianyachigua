import React from 'react';
import { useVideoList } from '../../hooks/useCachedApi';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import AdCard from '../AdCard';
import { useNavigate } from 'react-router-dom';
import useListStateManager from '../../hooks/useListStateManager';


/**
 * 带缓存的视频列表组件示例
 * 演示如何使用缓存API Hook
 */
const VideoListWithCache = ({ categoryId = null }) => {
  const navigate = useNavigate();
  
  // 使用缓存Hook
  const {
    videos,
    total,
    currentPage,
    pageSize,
    loading,
    error,
    handlePageChange,
    refresh
  } = useVideoList(categoryId, true);

  // 🔄 集成列表状态管理
  const { navigateToDetail } = useListStateManager(`videoListCache_${categoryId || 'home'}`);

  const handleVideoClick = (videoId) => {
    const currentState = {
      videos,
      total,
      currentPage,
      pageSize,
      categoryId,
      timestamp: Date.now()
    };
    
    // 使用状态管理器导航
    navigateToDetail(`/video/${videoId}`, currentState);
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-5xl">
        <div className="flex justify-center items-center h-64">
          <div className="text-white text-lg">加载中...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-5xl">
        <div className="flex justify-center items-center h-64">
          <div className="text-red-500 text-lg">
            加载失败，请稍后重试
            <button 
              onClick={refresh}
              className="ml-4 bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded"
            >
              重试
            </button>
          </div>
        </div>
      </div>
    );
  }

  const totalPages = Math.ceil(total / pageSize);

  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl">
      {/* 刷新按钮（仅开发环境显示） */}
      {process.env.NODE_ENV === 'development' && (
        <div className="mb-4">
          <button
            onClick={refresh}
            className="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded"
          >
            刷新缓存
          </button>
          <span className="ml-4 text-gray-400 text-sm">
            总计: {total} 个视频，当前页: {currentPage}
          </span>
        </div>
      )}

      {/* 视频网格 */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {videos.map((video, index) => (
          <div key={video.id} className="flex flex-col">
            {/* 视频卡片 */}
            <div
              className="bg-gray-800 rounded-lg overflow-hidden cursor-pointer hover:scale-105 transition-transform duration-200"
              onClick={() => handleVideoClick(video.id)}
            >
              <div className="relative w-full aspect-video bg-gray-700">
                <SecureDecryptedImage
                  src={video.coverImageUrl}
                  alt={video.title}
                  priority={index < 3 ? 'high' : 'normal'}
                  className="w-full h-full object-cover"
                  lazyLoad={true}
                  style={{ backgroundColor: '#1a1a1a' }}
                />
                {video.duration && (
                  <div className="absolute bottom-2 right-2 bg-black bg-opacity-70 text-white text-xs px-2 py-1 rounded">
                    {video.duration}
                  </div>
                )}
              </div>
              
              <div className="p-3">
                <h3 className="line-clamp-2 mb-2 video-list-title" style={{
                  fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                  fontSize: '28px',
                  lineHeight: '32px',
                  fontWeight: '400',
                  fontStyle: 'normal',
                  color: 'rgb(255, 255, 255)'
                }}>
                  {video.title}
                </h3>
                <div className="flex justify-between items-center" style={{
                  fontFamily: 'Consolas, Menlo, Monaco, "lucida console", "Liberation Mono", "Courier New", "andale mono", monospaceX, monospace, sans-serif',
                  fontStyle: 'normal',
                  fontWeight: '400',
                  color: 'rgb(238, 238, 238)',
                  fontSize: '15px',
                  lineHeight: '17px'
                }}>
                  <span>{video.author || '天涯吃瓜小慧'}</span>
                  <span>{video.publishedAt}</span>
                </div>
              </div>
            </div>

            {/* 插入广告 */}
            {index % 6 === 5 && (
              <div className="mt-4">
                <AdCard />
              </div>
            )}
          </div>
        ))}
      </div>

      {/* 分页 */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center mt-8 space-x-2">
          <button
            onClick={() => handlePageChange(Math.max(1, currentPage - 1))}
            disabled={currentPage === 1}
            className={`px-4 py-2 rounded ${
              currentPage === 1
                ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            上一页
          </button>

          <span className="text-white">
            第 {currentPage} 页，共 {totalPages} 页
          </span>

          <button
            onClick={() => handlePageChange(Math.min(totalPages, currentPage + 1))}
            disabled={currentPage === totalPages}
            className={`px-4 py-2 rounded ${
              currentPage === totalPages
                ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
};

export default VideoListWithCache;