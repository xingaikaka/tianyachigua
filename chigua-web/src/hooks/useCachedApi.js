import { useState, useEffect, useCallback } from 'react';
import { cachedApiService } from '../services/cachedApiService';

/**
 * 缓存API Hook
 * 为组件提供带缓存的API调用能力
 */
export function useCachedApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  /**
   * 视频详情
   */
  const getVideoDetail = useCallback(async (videoId) => {
    if (!videoId) return null;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await cachedApiService.getVideoDetail(videoId);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  /**
   * 视频列表
   */
  const getVideoList = useCallback(async (params = {}) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await cachedApiService.getVideoList(params);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  /**
   * 合集详情
   */
  const getCollectionDetail = useCallback(async (collectionId) => {
    if (!collectionId) return null;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await cachedApiService.getCollectionDetail(collectionId);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  /**
   * 合集列表
   */
  const getCollectionList = useCallback(async (params = {}) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await cachedApiService.getCollectionList(params);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  /**
   * 分类列表
   */
  const getCategoryList = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await cachedApiService.getCategoryList();
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  /**
   * 搜索视频
   */
  const searchVideos = useCallback(async (keyword, pageNum = 1, pageSize = 20) => {
    if (!keyword || keyword.trim() === '') return null;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await cachedApiService.searchVideos(keyword, pageNum, pageSize);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  /**
   * 刷新缓存
   */
  const refreshCache = useCallback((type, params = null) => {
    cachedApiService.refreshCache(type, params);
  }, []);
  
  /**
   * 获取缓存统计
   */
  const getCacheStats = useCallback(() => {
    return cachedApiService.getCacheStats();
  }, []);
  
  return {
    loading,
    error,
    getVideoDetail,
    getVideoList,
    getCollectionDetail,
    getCollectionList,
    getCategoryList,
    searchVideos,
    refreshCache,
    getCacheStats
  };
}

/**
 * 视频列表专用Hook
 */
export function useVideoList(categoryId = null, autoLoad = true) {
  const [videos, setVideos] = useState([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);
  const { loading, error, getVideoList } = useCachedApi();
  
  const loadVideos = useCallback(async (page = 1) => {
    try {
      const params = {
        pageNum: page,
        pageSize: pageSize
      };
      
      if (categoryId) {
        params.categoryId = categoryId;
      }
      
      const response = await getVideoList(params);
      
      if (response && response.code === 200) {
        const rows = (response && response.rows)
          || (response && response.data && response.data.rows)
          || (response && response.data && response.data.list)
          || [];
        const totalVal = (response && typeof response.total !== 'undefined')
          ? response.total
          : (response && response.data && response.data.total) || 0;
        setVideos(rows);
        setTotal(totalVal);
        setCurrentPage(page);
      }
    } catch (err) {
    }
  }, [categoryId, pageSize, getVideoList]);
  
  // 自动加载
  useEffect(() => {
    if (autoLoad) {
      loadVideos(1);
    }
  }, [categoryId, autoLoad, loadVideos]);
  
  // 翻页
  const handlePageChange = useCallback((page) => {
    loadVideos(page);
  }, [loadVideos]);
  
  // 刷新
  const refresh = useCallback(() => {
    loadVideos(currentPage);
  }, [loadVideos, currentPage]);
  
  return {
    videos,
    total,
    currentPage,
    pageSize,
    loading,
    error,
    loadVideos,
    handlePageChange,
    refresh
  };
}

/**
 * 合集列表专用Hook
 */
export function useCollectionList(categoryId = null, autoLoad = true) {
  const [collections, setCollections] = useState([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(20);
  const { loading, error, getCollectionList } = useCachedApi();
  
  const loadCollections = useCallback(async (page = 1) => {
    try {
      const params = {
        page: page,
        pageSize: pageSize
      };
      
      if (categoryId) {
        params.categoryId = categoryId;
      }
      
      const response = await getCollectionList(params);
      
      if (response && response.code === 200) {
        const rows = (response && response.rows)
          || (response && response.data && response.data.rows)
          || (response && response.data && response.data.list)
          || [];
        const totalVal = (response && typeof response.total !== 'undefined')
          ? response.total
          : (response && response.data && response.data.total) || 0;
        setCollections(rows);
        setTotal(totalVal);
        setCurrentPage(page);
      }
    } catch (err) {
    }
  }, [categoryId, pageSize, getCollectionList]);
  
  // 自动加载
  useEffect(() => {
    if (autoLoad) {
      loadCollections(1);
    }
  }, [categoryId, autoLoad, loadCollections]);
  
  // 翻页
  const handlePageChange = useCallback((page) => {
    loadCollections(page);
  }, [loadCollections]);
  
  // 刷新
  const refresh = useCallback(() => {
    loadCollections(currentPage);
  }, [loadCollections, currentPage]);
  
  return {
    collections,
    total,
    currentPage,
    pageSize,
    loading,
    error,
    loadCollections,
    handlePageChange,
    refresh
  };
}

export default useCachedApi;