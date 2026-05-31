import { useState, useCallback, useRef } from 'react';
import videoService from '../services/videoService';

const useVideoPreload = () => {
  const [preloadCache, setPreloadCache] = useState(new Map());
  const preloadTimeouts = useRef(new Map());
  
  // 预加载视频基本信息
  const preloadVideoBasic = useCallback((videoId) => {
    if (preloadCache.has(videoId)) {
      return preloadCache.get(videoId);
    }
    
    // 清除之前的定时器
    if (preloadTimeouts.current.has(videoId)) {
      clearTimeout(preloadTimeouts.current.get(videoId));
    }
    
    // 延迟预加载，避免用户只是路过
    const timeoutId = setTimeout(async () => {
      try {
        const response = await videoService.getVideoDetail(videoId);
        if (response.code === 200) {
          const basicData = {
            id: response.data.id,
            title: response.data.title,
            publishedAt: response.data.publishedAt,
            videoUrl: response.data.videoUrl,
            thumbnailUrl: response.data.thumbnailUrl,
            // 只预加载关键字段，避免过度请求
          };
          
          setPreloadCache(prev => new Map(prev).set(videoId, basicData));
          preloadTimeouts.current.delete(videoId);
        }
      } catch (error) {
        preloadTimeouts.current.delete(videoId);
      }
    }, 300); // 300ms 延迟，避免频繁触发
    
    preloadTimeouts.current.set(videoId, timeoutId);
  }, [preloadCache]);
  
  // 获取预加载的数据
  const getPreloadedData = useCallback((videoId) => {
    return preloadCache.get(videoId) || null;
  }, [preloadCache]);
  
  // 清理预加载数据
  const clearPreloadCache = useCallback(() => {
    // 清理所有定时器
    preloadTimeouts.current.forEach(timeoutId => clearTimeout(timeoutId));
    preloadTimeouts.current.clear();
    setPreloadCache(new Map());
  }, []);
  
  return {
    preloadVideoBasic,
    getPreloadedData,
    clearPreloadCache,
    hasCachedData: (videoId) => preloadCache.has(videoId)
  };
};

export default useVideoPreload;
