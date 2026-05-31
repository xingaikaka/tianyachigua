import React, { useState, useEffect } from 'react';
import { useCachedApi } from '../../hooks/useCachedApi';
import { simpleCache } from '../../utils/simpleCache';

/**
 * 缓存调试组件
 * 开发环境下显示缓存状态和管理
 */
const CacheDebug = () => {
  const [stats, setStats] = useState({});
  const [showDebug, setShowDebug] = useState(false);
  const { getCacheStats, refreshCache } = useCachedApi();
  
  // 更新统计信息
  const updateStats = () => {
    const cacheStats = getCacheStats();
    setStats(cacheStats);
  };
  
  useEffect(() => {
    updateStats();
    
    // 定期更新统计
    const interval = setInterval(updateStats, 5000);
    return () => clearInterval(interval);
  }, [getCacheStats]);
  
  // 只在开发环境显示
  if (process.env.NODE_ENV !== 'development') {
    return null;
  }
  
  if (!showDebug) {
    return (
      <div 
        className="fixed bottom-4 right-4 bg-blue-600 text-white px-3 py-2 rounded cursor-pointer z-50"
        onClick={() => setShowDebug(true)}
      >
        缓存: {stats.active || 0}/{stats.total || 0}
      </div>
    );
  }
  
  return (
    <div className="fixed bottom-4 right-4 bg-white border border-gray-300 rounded-lg shadow-lg p-4 z-50 w-80">
      <div className="flex justify-between items-center mb-3">
        <h3 className="font-bold text-gray-800">缓存调试</h3>
        <button 
          onClick={() => setShowDebug(false)}
          className="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      
      {/* 缓存统计 */}
      <div className="mb-3">
        <h4 className="font-semibold text-sm text-gray-700 mb-2">总体统计</h4>
        <div className="text-xs text-gray-600 space-y-1">
          <div>总条目: {stats.total || 0}</div>
          <div>活跃: {stats.active || 0}</div>
          <div>过期: {stats.expired || 0}</div>
          <div>容量: {stats.total || 0}/{stats.maxSize || 100}</div>
        </div>
      </div>
      
      {/* 分类统计 */}
      {stats.byType && (
        <div className="mb-3">
          <h4 className="font-semibold text-sm text-gray-700 mb-2">分类统计</h4>
          <div className="text-xs text-gray-600 space-y-1">
            {Object.entries(stats.byType).map(([type, count]) => (
              <div key={type} className="flex justify-between">
                <span>{type}:</span>
                <span>{count}</span>
              </div>
            ))}
          </div>
        </div>
      )}
      
      {/* 操作按钮 */}
      <div className="space-y-2">
        <button
          onClick={() => {
            simpleCache.clear();
            updateStats();
          }}
          className="w-full bg-red-500 text-white px-3 py-1 rounded text-xs hover:bg-red-600"
        >
          清空所有缓存
        </button>
        
        <div className="grid grid-cols-2 gap-1">
          <button
            onClick={() => {
              refreshCache('VIDEO_LIST');
              updateStats();
            }}
            className="bg-blue-500 text-white px-2 py-1 rounded text-xs hover:bg-blue-600"
          >
            清理视频列表
          </button>
          
          <button
            onClick={() => {
              refreshCache('COLLECTION_LIST');
              updateStats();
            }}
            className="bg-blue-500 text-white px-2 py-1 rounded text-xs hover:bg-blue-600"
          >
            清理合集列表
          </button>
          
          <button
            onClick={() => {
              refreshCache('CATEGORY_LIST');
              updateStats();
            }}
            className="bg-green-500 text-white px-2 py-1 rounded text-xs hover:bg-green-600"
          >
            清理分类
          </button>
          
          <button
            onClick={() => {
              refreshCache('SEARCH_RESULT');
              updateStats();
            }}
            className="bg-yellow-500 text-white px-2 py-1 rounded text-xs hover:bg-yellow-600"
          >
            清理搜索
          </button>
        </div>
        
        <button
          onClick={updateStats}
          className="w-full bg-gray-500 text-white px-3 py-1 rounded text-xs hover:bg-gray-600"
        >
          刷新统计
        </button>
      </div>
    </div>
  );
};

export default CacheDebug;