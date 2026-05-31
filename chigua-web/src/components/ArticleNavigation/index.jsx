import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

const ArticleNavigation = ({ video }) => {
  const navigate = useNavigate();
  const location = useLocation();
  
  // 从props中获取相邻视频信息
  const previousVideo = video?.previousVideo || null;
  const nextVideo = video?.nextVideo || null;

  // 获取当前URL的查询参数（fromCategory, searchKeyword, sortType）
  const getContextParams = () => {
    const params = new URLSearchParams(location.search);
    const contextParams = {};
    
    if (params.has('fromCategory')) {
      contextParams.fromCategory = params.get('fromCategory');
    }
    if (params.has('searchKeyword')) {
      contextParams.searchKeyword = params.get('searchKeyword');
    }
    if (params.has('sortType')) {
      contextParams.sortType = params.get('sortType');
    }
    
    return contextParams;
  };

  // 构建带上下文参数的URL
  const buildUrlWithContext = (videoId) => {
    const contextParams = getContextParams();
    const queryString = new URLSearchParams(contextParams).toString();
    return queryString ? `/video/${videoId}?${queryString}` : `/video/${videoId}`;
  };

  const handlePreviousClick = () => {
    if (previousVideo && previousVideo.id) {
      navigate(buildUrlWithContext(previousVideo.id));
    }
  };

  const handleNextClick = () => {
    if (nextVideo && nextVideo.id) {
      navigate(buildUrlWithContext(nextVideo.id));
    }
  };

  // 如果没有视频数据，不显示组件
  if (!video) {
    return null;
  }

  // 总是显示组件，提供友好的提示

  return (
    <div className="rounded-lg p-6 w-full" style={{ backgroundColor: '#343232' }}>
      {/* 响应式布局：手机端垂直排列，桌面端水平排列 */}
      <div className="flex flex-col md:flex-row md:justify-between md:items-start gap-6">
        {/* 上一篇 */}
        <div className="flex-1">
          {previousVideo ? (
            <button
              onClick={handlePreviousClick}
              className="text-left hover:opacity-80 transition-opacity duration-200 w-full"
              style={{ color: '#F0F0F0' }}
            >
              <div className="mb-2">
                <span style={{ color: '#1ABC9C' }} className="text-sm">上一篇:</span>
              </div>
              <div className="text-lg leading-relaxed">
                {previousVideo.title}
              </div>
            </button>
          ) : (
            <>
              <div className="mb-2">
                <span style={{ color: '#1ABC9C' }} className="text-sm">上一篇:</span>
              </div>
              <div style={{ color: '#888888' }} className="text-base leading-relaxed">
                没有更多了
              </div>
            </>
          )}
        </div>

        {/* 分隔线 - 手机端水平线，桌面端垂直线 */}
        <div className="h-px md:h-auto md:w-px bg-gray-600 md:self-stretch md:mx-4"></div>

        {/* 下一篇 */}
        <div className="flex-1">
          {nextVideo ? (
            <button
              onClick={handleNextClick}
              className="text-left hover:opacity-80 transition-opacity duration-200 w-full"
              style={{ color: '#F0F0F0' }}
            >
              <div className="mb-2">
                <span style={{ color: '#1ABC9C' }} className="text-sm">下一篇:</span>
              </div>
              <div className="text-lg leading-relaxed">
                {nextVideo.title}
              </div>
            </button>
          ) : (
            <>
              <div className="mb-2">
                <span style={{ color: '#1ABC9C' }} className="text-sm">下一篇:</span>
              </div>
              <div style={{ color: '#888888' }} className="text-base leading-relaxed">
                没有更多了
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default ArticleNavigation;