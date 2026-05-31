/**
 * 页面状态缓存组件
 * 用于在页面切换时保持DOM状态，避免重新渲染导致的闪烁
 */

import React, { useRef, useEffect } from 'react';

const PageStateCache = ({ 
  cacheKey, 
  children, 
  shouldCache = true,
  onCache = null,
  onRestore = null 
}) => {
  const containerRef = useRef(null);
  const cachedContentRef = useRef(null);
  const isRestoringRef = useRef(false);

  // 全局缓存存储
  if (!window.pageStateCache) {
    window.pageStateCache = new Map();
  }

  // 保存页面DOM状态（包含Canvas数据）
  const cachePageState = () => {
    if (!shouldCache || !containerRef.current) return;

    const container = containerRef.current;
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;

    // 克隆整个DOM结构
    const clonedContent = container.cloneNode(true);
    
    // 保存Canvas内容
    const canvasElements = container.querySelectorAll('canvas');
    const canvasData = [];
    
    canvasElements.forEach((canvas, index) => {
      try {
        const dataURL = canvas.toDataURL();
        canvasData.push({
          index,
          dataURL,
          width: canvas.width,
          height: canvas.height,
          style: canvas.style.cssText
        });
      } catch (error) {
        canvasData.push({
          index,
          dataURL: null,
          width: canvas.width,
          height: canvas.height,
          style: canvas.style.cssText
        });
      }
    });
    
    const cacheData = {
      domContent: clonedContent,
      canvasData: canvasData,
      scrollPosition: { top: scrollTop, left: scrollLeft },
      timestamp: Date.now(),
      cacheKey
    };

    window.pageStateCache.set(cacheKey, cacheData);
    
    if (onCache) {
      onCache(cacheData);
    }

  };

  // 恢复页面DOM状态（包含Canvas重绘）
  const restorePageState = () => {
    if (!window.pageStateCache.has(cacheKey)) return false;

    const cachedData = window.pageStateCache.get(cacheKey);
    if (!cachedData || !containerRef.current) return false;

    isRestoringRef.current = true;

    // 直接替换DOM内容
    const container = containerRef.current;
    container.innerHTML = '';
    
    // 复制缓存的DOM结构
    const cachedContent = cachedData.domContent.cloneNode(true);
    while (cachedContent.firstChild) {
      container.appendChild(cachedContent.firstChild);
    }

    // 恢复Canvas内容
    if (cachedData.canvasData && cachedData.canvasData.length > 0) {
      const restoredCanvases = container.querySelectorAll('canvas');
      
      cachedData.canvasData.forEach((canvasInfo) => {
        if (canvasInfo.index < restoredCanvases.length && canvasInfo.dataURL) {
          const canvas = restoredCanvases[canvasInfo.index];
          const ctx = canvas.getContext('2d');
          
          // 设置Canvas尺寸和样式
          canvas.width = canvasInfo.width;
          canvas.height = canvasInfo.height;
          canvas.style.cssText = canvasInfo.style;
          
          // 重新绘制Canvas内容
          const img = new Image();
          img.onload = () => {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.drawImage(img, 0, 0);
          };
          img.src = canvasInfo.dataURL;
        }
      });
      
    }

    // 恢复滚动位置
    if (cachedData.scrollPosition) {
      // 延迟设置滚动位置，等待Canvas重绘完成
      setTimeout(() => {
        window.scrollTo(
          cachedData.scrollPosition.left || 0,
          cachedData.scrollPosition.top || 0
        );
      }, 50);
    }

    if (onRestore) {
      onRestore(cachedData);
    }

    
    setTimeout(() => {
      isRestoringRef.current = false;
    }, 100);

    return true;
  };

  // 检查是否有缓存
  const hasCache = () => {
    return window.pageStateCache.has(cacheKey);
  };

  // 清除缓存
  const clearCache = () => {
    window.pageStateCache.delete(cacheKey);
  };

  // 页面卸载时自动缓存
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (shouldCache && !isRestoringRef.current) {
        cachePageState();
      }
    };

    // 监听页面卸载
    window.addEventListener('beforeunload', handleBeforeUnload);
    
    // 监听路由变化（如果使用React Router）
    const handlePopState = () => {
      if (shouldCache && !isRestoringRef.current) {
        cachePageState();
      }
    };
    
    window.addEventListener('popstate', handlePopState);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      window.removeEventListener('popstate', handlePopState);
    };
  }, [cacheKey, shouldCache]);

  // 组件挂载时尝试恢复
  useEffect(() => {
    if (hasCache()) {
      const restored = restorePageState();
      if (restored) {
        return; // 成功恢复，不渲染children
      }
    }
  }, [cacheKey]);

  // 暴露方法到ref
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.cachePageState = cachePageState;
      containerRef.current.restorePageState = restorePageState;
      containerRef.current.hasCache = hasCache;
      containerRef.current.clearCache = clearCache;
    }
  });

  return (
    <div 
      ref={containerRef}
      data-cache-key={cacheKey}
      style={{
        // 确保容器样式稳定
        minHeight: '100vh',
        transition: 'none'
      }}
    >
      {children}
    </div>
  );
};

export default PageStateCache;
