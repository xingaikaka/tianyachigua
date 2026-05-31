import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import pageConfigService from '../services/pageConfigService';

const PageConfigContext = createContext();

export const usePageConfigContext = () => {
  const context = useContext(PageConfigContext);
  if (!context) {
    throw new Error('usePageConfigContext must be used within a PageConfigProvider');
  }
  return context;
};

export const PageConfigProvider = ({ children }) => {
  const [configs, setConfigs] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [initialized, setInitialized] = useState(false);

  // 加载配置
  const loadConfigs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const configData = await pageConfigService.getConfigs();
      setConfigs(configData);
      setInitialized(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // 刷新配置
  const refreshConfigs = useCallback(async () => {
    try {
      setError(null);
      const configData = await pageConfigService.refreshConfigs();
      setConfigs(configData);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  // 强制刷新配置
  const forceRefresh = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const configData = await pageConfigService.forceRefresh();
      setConfigs(configData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // 获取单个配置
  const getConfig = useCallback((key, defaultValue = '') => {
    return configs[key] || defaultValue;
  }, [configs]);

  // 初始化加载
  useEffect(() => {
    if (!initialized) {
      loadConfigs();
    }
  }, [loadConfigs, initialized]);

  const value = {
    configs,
    loading,
    error,
    initialized,
    getConfig,
    loadConfigs,
    refreshConfigs,
    forceRefresh,
    // 缓存信息
    getCacheInfo: () => pageConfigService.getCacheInfo()
  };

  return (
    <PageConfigContext.Provider value={value}>
      {children}
    </PageConfigContext.Provider>
  );
};
