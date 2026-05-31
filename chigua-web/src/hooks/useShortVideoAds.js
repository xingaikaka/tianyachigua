import { useState, useCallback, useEffect, useRef } from 'react';
import categoryService from '../services/categoryService';
import advertisementService from '../services/advertisementService';

/**
 * 统一管理短视频广告配置与广告列表的 Hook
 */
const useShortVideoAds = (
  categoryId,
  {
    autoLoad = true,
    initialDisplayMode = 3,
    initialInterval = 5
  } = {}
) => {
  const [state, setState] = useState(() => ({
    categoryConfig: null,
    shortVideoAds: [],
    adDisplayMode: initialDisplayMode,
    adInterval: initialInterval
  }));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const requestIdRef = useRef(0);

  const resetState = useCallback(() => {
    setState({
      categoryConfig: null,
      shortVideoAds: [],
      adDisplayMode: initialDisplayMode,
      adInterval: initialInterval
    });
    setError(null);
  }, [initialDisplayMode, initialInterval]);

  useEffect(() => {
    // 分类切换时立即重置，避免沿用旧配置
    resetState();
  }, [categoryId, resetState]);

  const load = useCallback(async ({ forceRefresh = false } = {}) => {
    if (!categoryId) {
      resetState();
      return {
        categoryConfig: null,
        shortVideoAds: [],
        adDisplayMode: initialDisplayMode,
        adInterval: initialInterval
      };
    }

    requestIdRef.current += 1;
    const currentRequestId = requestIdRef.current;
    setLoading(true);
    setError(null);

    try {
      const configResponse = await categoryService.getCategoryAdConfig(
        categoryId,
        forceRefresh ? { forceRefresh: true } : {}
      );

      if (currentRequestId !== requestIdRef.current) {
        return null;
      }

      let nextState = {
        categoryConfig: null,
        shortVideoAds: [],
        adDisplayMode: initialDisplayMode,
        adInterval: initialInterval
      };

      if (configResponse && configResponse.data) {
        const config = configResponse.data;
        nextState = {
          categoryConfig: config,
          shortVideoAds: [],
          adDisplayMode: config.ad_display_mode || initialDisplayMode,
          adInterval: config.ad_interval || initialInterval
        };

        // 仅在需要展示时加载广告列表
        if (nextState.adDisplayMode !== 3) {
          try {
            const adsResponse = await advertisementService.getShortVideoAds(categoryId);
            if (currentRequestId === requestIdRef.current) {
              nextState.shortVideoAds = Array.isArray(adsResponse?.data)
                ? adsResponse.data
                : [];
            }
          } catch (adError) {
            if (currentRequestId === requestIdRef.current) {
              nextState.shortVideoAds = [];
            }
          }
        }
      }

      if (currentRequestId === requestIdRef.current) {
        setState(nextState);
      }

      return nextState;
    } catch (err) {
      if (currentRequestId === requestIdRef.current) {
        resetState();
        setError(err);
      }
      return {
        categoryConfig: null,
        shortVideoAds: [],
        adDisplayMode: initialDisplayMode,
        adInterval: initialInterval
      };
    } finally {
      if (currentRequestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  }, [categoryId, initialDisplayMode, initialInterval, resetState]);

  useEffect(() => {
    if (!autoLoad) return;
    load();
  }, [autoLoad, load]);

  return {
    ...state,
    loading,
    error,
    reload: load
  };
};

export default useShortVideoAds;
