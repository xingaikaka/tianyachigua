import { useState, useEffect, useCallback } from 'react';

const useProgressiveLoading = () => {
  const [loadingStage, setLoadingStage] = useState('skeleton'); // skeleton, basic, content, complete
  const [preloadedData, setPreloadedData] = useState(null);
  
  // 模拟渐进式加载的时序
  const startProgressiveLoading = useCallback((videoId, preloadData = null) => {
    setLoadingStage('skeleton');
    setPreloadedData(preloadData);
    
    // Stage 1: 显示基本信息 (如果有预加载数据)
    if (preloadData) {
      setTimeout(() => {
        setLoadingStage('basic');
      }, 50);
    }
    
    // Stage 2: 显示主要内容
    setTimeout(() => {
      setLoadingStage('content');
    }, 100);
    
    // Stage 3: 完成所有加载
    setTimeout(() => {
      setLoadingStage('complete');
    }, 200);
  }, []);
  
  const resetLoading = useCallback(() => {
    setLoadingStage('skeleton');
    setPreloadedData(null);
  }, []);
  
  return {
    loadingStage,
    preloadedData,
    startProgressiveLoading,
    resetLoading,
    isSkeletonStage: loadingStage === 'skeleton',
    isBasicStage: loadingStage === 'basic',
    isContentStage: loadingStage === 'content',
    isComplete: loadingStage === 'complete'
  };
};

export default useProgressiveLoading;
