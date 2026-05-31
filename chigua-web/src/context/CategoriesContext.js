import React, { createContext, useContext, useState, useEffect } from 'react';
import categoryCacheService from '../services/categoryCacheService';

const CategoriesContext = createContext(null);

export const useCategories = () => {
  const context = useContext(CategoriesContext);
  if (!context) {
    throw new Error('useCategories must be used within a CategoriesProvider');
  }
  return context;
};

export const CategoriesProvider = ({ children }) => {
  // 初始化时立即检查缓存，避免闪烁
  const initializeCategories = () => {
    // 尝试从内存缓存立即获取数据
    const cachedCategories = categoryCacheService.memoryCache.categories;
    const cacheTimestamp = categoryCacheService.memoryCache.timestamp;
    
    // 检查内存缓存是否有效
    const isCacheValid = cacheTimestamp && 
      (Date.now() - cacheTimestamp < categoryCacheService.CACHE_CONFIG.CACHE_DURATION);
    
    if (isCacheValid && cachedCategories) {
      // 转换数据格式
        const transformedCategories = cachedCategories
          .filter(category => category.id !== null)
          .map(category => ({
            id: category.id,
            name: category.categoryName,
            isRecommended: category.isRecommended,
            isShort: category.isShort,
            isUserGroup: category.isUserGroup
          }));
      
      return {
        categories: transformedCategories,
        loading: false
      };
    }
    
    // 尝试从本地存储获取
    try {
      const localCategories = categoryCacheService.getCachedData(categoryCacheService.CACHE_KEYS.CATEGORIES);
      const localTimestamp = categoryCacheService.getCachedTimestamp();
      
      const isLocalCacheValid = localTimestamp && 
        (Date.now() - localTimestamp < categoryCacheService.CACHE_CONFIG.CACHE_DURATION);
      
      if (isLocalCacheValid && localCategories) {
        // 转换数据格式
        const transformedCategories = localCategories
          .filter(category => category.id !== null)
          .map(category => ({
            id: category.id,
            name: category.categoryName,
            isRecommended: category.isRecommended,
            isShort: category.isShort,
            isUserGroup: category.isUserGroup
          }));
        
        return {
          categories: transformedCategories,
          loading: false
        };
      }
    } catch (error) {
    }
    
    // 没有有效缓存，需要从API获取
    return {
      categories: [],
      loading: true
    };
  };

  // 使用初始化数据
  const initialData = initializeCategories();
  const [categories, setCategories] = useState(initialData.categories);
  const [loading, setLoading] = useState(initialData.loading);
  const [error, setError] = useState(null);

  const fetchCategories = async () => {
    try {
      // 如果已经有缓存数据，就不显示加载状态
      if (categories.length === 0) {
        setLoading(true);
      }
      setError(null);
      
      // 使用缓存服务获取分类数据
      const cachedCategories = await categoryCacheService.getCategories();
      
      // 转换数据格式以保持兼容性
        const transformedCategories = cachedCategories
          .filter(category => category.id !== null) // 过滤掉"全部"选项
          .map(category => ({
            id: category.id,
            name: category.categoryName,
            isRecommended: category.isRecommended,
            isShort: category.isShort,
            isUserGroup: category.isUserGroup
          }));
      
      setCategories(transformedCategories);
      
    } catch (error) {
      setError(error.message);
      
      // 只有在没有任何数据时才设置默认分类
      if (categories.length === 0) {
        setCategories([
          { id: 1, name: '今日吃瓜', isRecommended: 0, isShort: 0, isUserGroup: 0 },
          { id: 2, name: '最高点击', isRecommended: 0, isShort: 0, isUserGroup: 0 }
        ]);
      }
    } finally {
      setLoading(false);
    }
  };

  // 监听缓存数据更新
  useEffect(() => {
    const handleDataUpdate = (event) => {
      const { type, data } = event.detail;
      if (type === 'categories') {
        // 转换数据格式
        const transformedCategories = data
          .filter(category => category.id !== null)
          .map(category => ({
            id: category.id,
            name: category.categoryName,
            isRecommended: category.isRecommended,
            isShort: category.isShort,
            isUserGroup: category.isUserGroup
          }));
        setCategories(transformedCategories);
      }
    };

    window.addEventListener('categoryDataUpdate', handleDataUpdate);
    
    return () => {
      window.removeEventListener('categoryDataUpdate', handleDataUpdate);
    };
  }, []);

  useEffect(() => {
    // 如果初始化时没有获取到缓存数据，才需要异步获取
    if (initialData.loading) {
      fetchCategories();
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const value = {
    categories,
    loading,
    error,
    refetch: fetchCategories
  };

  return (
    <CategoriesContext.Provider value={value}>
      {children}
    </CategoriesContext.Provider>
  );
};
