import { useCallback, useEffect, useState } from 'react';
import advertisementService from '../services/advertisementService';
import apiCacheService from '../services/apiCacheService';

const useListAds = ({
  categoryId = null,
  enableTopAds = true,
  enableBottomAds = true,
  enableConfig = true,
  bottomPosition = '6',
  topPosition = '5',
  cacheDuration = 5 * 60 * 1000,
  initialTopAds = [],
  initialBottomAds = [],
  initialConfig = null
} = {}) => {
  const [topAds, setTopAds] = useState(initialTopAds);
  const [bottomAds, setBottomAds] = useState(initialBottomAds);
  const [adConfig, setAdConfig] = useState(initialConfig);

  const fetchAdConfig = useCallback(async () => {
    if (!enableConfig) return;

    if (!categoryId) {
      setAdConfig({
        categoryId: 0,
        categoryName: '首页',
        adDisplayMode: 2,
        adInterval: 1,
        adDisplayModeName: '交替显示'
      });
      return;
    }

    try {
      const response = await apiCacheService.getApiData(
        'CATEGORY_AD_CONFIG',
        { categoryId },
        (params) => advertisementService.getCategoryAdConfig(params.categoryId),
        { cacheDuration }
      );

      if (response.code === 200) {
        setAdConfig(response.data);
      } else {
        setAdConfig({
          categoryId,
          adDisplayMode: 1,
          adInterval: 3,
          adDisplayModeName: '集中显示'
        });
      }
    } catch (error) {
      setAdConfig({
        categoryId,
        adDisplayMode: 1,
        adInterval: 3,
        adDisplayModeName: '集中显示'
      });
    }
  }, [categoryId, cacheDuration, enableConfig]);

  const fetchTopAds = useCallback(async () => {
    if (!enableTopAds) return;
    try {
      let response;
      if (categoryId) {
        response = await apiCacheService.getApiData(
          'CATEGORY_ADS',
          { categoryId },
          (params) => advertisementService.getCategoryAds(params.categoryId),
          { cacheDuration }
        );
      } else {
        response = await apiCacheService.getApiData(
          'ADS_BY_POSITION',
          { position: topPosition },
          (params) => advertisementService.getAdsByPosition(params.position),
          { cacheDuration }
        );
      }

      if (response.code === 200) {
        setTopAds(response.data || []);
      } else {
        setTopAds([]);
      }
    } catch (error) {
      setTopAds([]);
    }
  }, [categoryId, cacheDuration, enableTopAds, topPosition]);

  const fetchBottomAds = useCallback(async () => {
    if (!enableBottomAds) return;
    try {
      let response;
      if (categoryId) {
        response = await apiCacheService.getApiData(
          'CATEGORY_BOTTOM_ADS',
          { categoryId },
          (params) => advertisementService.getCategoryBottomAds(params.categoryId),
          { cacheDuration }
        );
      } else {
        response = await apiCacheService.getApiData(
          'ADS_BY_POSITION',
          { position: bottomPosition },
          (params) => advertisementService.getAdsByPosition(params.position),
          { cacheDuration }
        );
      }

      if (response.code === 200) {
        setBottomAds(response.data || []);
      } else {
        setBottomAds([]);
      }
    } catch (error) {
      setBottomAds([]);
    }
  }, [categoryId, cacheDuration, enableBottomAds, bottomPosition]);

  const primeAdsState = useCallback(({ top = null, bottom = null, config = null } = {}) => {
    if (Array.isArray(top)) setTopAds(top);
    if (Array.isArray(bottom)) setBottomAds(bottom);
    if (config !== null) setAdConfig(config);
  }, []);

  useEffect(() => {
    if (enableConfig) {
      fetchAdConfig();
    }
    if (enableTopAds) {
      fetchTopAds();
    }
    if (enableBottomAds) {
      fetchBottomAds();
    }
  }, [fetchAdConfig, fetchTopAds, fetchBottomAds, enableConfig, enableTopAds, enableBottomAds]);

  return {
    topAds,
    bottomAds,
    adConfig,
    refreshTopAds: fetchTopAds,
    refreshBottomAds: fetchBottomAds,
    refreshAdConfig: fetchAdConfig,
    primeAdsState
  };
};

export default useListAds;
