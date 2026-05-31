/**
 * 性能监控组件
 * 显示图片加载统计和Worker状态
 */

import React, { useState, useEffect } from 'react';
// Worker功能已移除

const PerformanceMonitor = ({ showDetails = false }) => {
  const [workerStats, setWorkerStats] = useState(null);
  const [isVisible, setIsVisible] = useState(false);

  // 获取Worker状态
  useEffect(() => {
    const updateWorkerStats = () => {
      // Worker功能已移除
      setWorkerStats({ initialized: false, workerCount: 0, status: 'disabled' });
    };

    updateWorkerStats();
    const interval = setInterval(updateWorkerStats, 2000);

    return () => clearInterval(interval);
  }, []);

  // 开发环境下显示性能监控
  const isDevelopment = process.env.NODE_ENV === 'development';

  if (!isDevelopment && !showDetails) {
    return null;
  }

  if (!isVisible) {
    return (
      <div 
        className="fixed bottom-4 right-4 z-50"
        style={{ opacity: 0.7 }}
      >
        <button
          onClick={() => setIsVisible(true)}
          className="bg-blue-600 text-white px-3 py-2 rounded text-xs hover:bg-blue-700"
        >
          📊 性能
        </button>
      </div>
    );
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 bg-black bg-opacity-90 text-white p-4 rounded-lg text-xs max-w-sm">
      <div className="flex justify-between items-center mb-2">
        <h3 className="font-bold text-sm">性能监控</h3>
        <button
          onClick={() => setIsVisible(false)}
          className="text-gray-400 hover:text-white"
        >
          ✕
        </button>
      </div>



      {/* Worker状态 */}
      {workerStats && (
        <div className="mb-3">
          <div className="font-semibold mb-1">⚡ Worker状态</div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>初始化: {workerStats.initialized ? '✅' : '❌'}</div>
            <div>Worker数: {workerStats.workerCount}</div>
            <div>忙碌中: {workerStats.busyWorkers}</div>
            <div>队列: {workerStats.queueLength}</div>
            <div className="col-span-2">
              待处理: <span className="text-yellow-400">{workerStats.pendingTasks}</span>
            </div>
            {workerStats.hasEncryptionKey !== undefined && (
              <div className="col-span-2">
                密钥状态: {workerStats.hasEncryptionKey ? '🔑 已配置' : '❌ 未配置'}
              </div>
            )}
          </div>
          

        </div>
      )}

      {/* 性能提示 */}
      <div className="text-xs text-gray-400 mt-2 pt-2 border-t border-gray-600">
        {workerStats && workerStats.initialized ? (
          <div className="text-green-400">⚡ Worker解密已启用</div>
        ) : (
          <div className="text-gray-400">🔄 主线程解密模式</div>
        )}
      </div>
    </div>
  );
};

export default PerformanceMonitor;
