import { useState, useEffect } from 'react';
import pageConfigService from '../services/pageConfigService';

/**
 * 页面配置Hook - 优化版本，使用缓存减少重复请求
 * @param {string} configKey - 指定配置键值，如果提供则直接返回该配置
 * @returns {object} 配置数据和操作方法
 */
export const usePageConfig = (configKey = null) => {
  const [configs, setConfigs] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    loadConfigs();
  }, []);
  
  const loadConfigs = async () => {
    try {
      setLoading(true);
      const configData = await pageConfigService.getConfigs();
      setConfigs(configData);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  
  const refreshConfigs = async () => {
    try {
      const configData = await pageConfigService.refreshConfigs();
      setConfigs(configData);
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  };

  const forceRefresh = async () => {
    try {
      const configData = await pageConfigService.forceRefresh();
      setConfigs(configData);
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  };
  
  const getConfig = (key, defaultValue = '') => {
    return configs[key] || defaultValue;
  };
  
  const getRichContent = (key) => {
    const config = configs[key];
    return config?.richContent || '';
  };
  
  const getBasicContent = (key) => {
    const config = configs[key];
    return config?.basicContent || '';
  };
  
  // 获取配置的备注（remark），需要详细配置
  const getRemark = async (key) => {
    try {
      const detailed = await pageConfigService.getAllDetailed();
      const config = detailed && detailed[key];
      return config && typeof config.remark === 'string' ? config.remark : '';
    } catch (_) {
      return '';
    }
  };
  
  // 如果指定了configKey，直接返回该配置
  if (configKey) {
    return {
      config: getConfig(configKey),
      richContent: getRichContent(configKey),
      basicContent: getBasicContent(configKey),
      getRemark: () => getRemark(configKey),
      loading,
      error,
      refresh: refreshConfigs
    };
  }
  
  return {
    configs,
    getConfig,
    getRichContent,
    getBasicContent,
    getRemark,
    loading,
    error,
    loadConfigs,
    refreshConfigs,
    forceRefresh
  };
};

/**
 * 站点配置Hook - 获取站点基础配置
 */
export const useSiteConfig = () => {
  const [siteConfigs, setSiteConfigs] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadSiteConfigs();
  }, []);

  const loadSiteConfigs = async () => {
    try {
      setLoading(true);
      // 使用带缓存的方法获取站点配置
      const allConfigs = await pageConfigService.getConfigs();
      
      // 过滤出站点基础配置 (用于Header/Footer组件)
      const siteConfigKeys = [
        'site_title',
        'site_description', 
        'site_keywords',
        'site_logo',
        'site_favicon',
        'footer_copyright',
        'footer_links',
        'contact_email',
        'contact_phone',
        // 社交/媒体链接与联系信息（用于 Header 图标点击）
        'telegram_url',
        'twitter_url',
        'github_url',
        'qq_number'
      ];
      
      const filteredConfigs = {};
      siteConfigKeys.forEach(key => {
        if (allConfigs[key]) {
          filteredConfigs[key] = allConfigs[key];
        }
      });
      
      setSiteConfigs(filteredConfigs);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getSiteConfig = (key, defaultValue = '') => {
    return siteConfigs[key] || defaultValue;
  };

  return {
    siteConfigs,
    getSiteConfig,
    loading,
    error,
    refresh: loadSiteConfigs
  };
};

/**
 * 底部信息Hook
 */
export const useFooterInfo = () => {
  const [footerInfo, setFooterInfo] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadFooterInfo();
  }, []);

  const loadFooterInfo = async () => {
    try {
      setLoading(true);
      const response = await pageConfigService.getFooterInfo();
      setFooterInfo(response.data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return {
    footerInfo,
    loading,
    error,
    refresh: loadFooterInfo
  };
};

/**
 * 富文本配置Hook
 */
export const useRichTextConfig = (configKey) => {
  const [richContent, setRichContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (configKey) {
      loadRichTextConfig();
    }
  }, [configKey]);

  const loadRichTextConfig = async () => {
    try {
      setLoading(true);
      const content = await pageConfigService.getRichTextConfig(configKey);
      setRichContent(content);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return {
    richContent,
    loading,
    error,
    refresh: loadRichTextConfig
  };
};

/**
 * 基础配置Hook
 */
export const useBasicConfig = (configKey) => {
  const [basicContent, setBasicContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (configKey) {
      loadBasicConfig();
    }
  }, [configKey]);

  const loadBasicConfig = async () => {
    try {
      setLoading(true);
      const content = await pageConfigService.getBasicConfig(configKey);
      setBasicContent(content);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return {
    basicContent,
    loading,
    error,
    refresh: loadBasicConfig
  };
};