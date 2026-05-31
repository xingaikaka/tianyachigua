import { useState, useEffect, useCallback } from 'react';

// 全局加载状态管理
class LoadingProgressManager {
  constructor() {
    this.listeners = new Set();
    this.loadingStates = new Map();
    this.isLoading = false;
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  start(key = 'default') {
    this.loadingStates.set(key, true);
    this.isLoading = true;
    this.notifyListeners();
  }

  finish(key = 'default') {
    this.loadingStates.set(key, false);
    // 检查是否还有其他加载中的任务
    this.isLoading = Array.from(this.loadingStates.values()).some(loading => loading);
    this.notifyListeners();
  }

  notifyListeners() {
    this.listeners.forEach(listener => listener(this.isLoading));
  }
}

const loadingManager = new LoadingProgressManager();

export const useLoadingProgress = () => {
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const unsubscribe = loadingManager.subscribe(setIsLoading);
    return unsubscribe;
  }, []);

  const startLoading = useCallback((key) => {
    loadingManager.start(key);
  }, []);

  const finishLoading = useCallback((key) => {
    loadingManager.finish(key);
  }, []);

  return {
    isLoading,
    startLoading,
    finishLoading
  };
};

export default loadingManager;
