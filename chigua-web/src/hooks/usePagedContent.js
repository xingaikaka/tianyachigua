import { useCallback, useRef, useState } from 'react';

/**
 * 通用分页请求Hook，负责处理并发请求竞态、loading 状态与取消。
 *
 * fetcher: (page, options) => Promise<any>
 * onSuccess: ({ result, page, options }) => void
 * onError: ({ error, page, options }) => void
 */
const usePagedContent = ({ fetcher, onSuccess, onError } = {}) => {
  const requestTokenRef = useRef(0);
  const [loading, setLoading] = useState(false);

  const loadPage = useCallback(async (page, options = {}) => {
    if (typeof fetcher !== 'function') {
      throw new Error('usePagedContent requires a fetcher function');
    }

    const {
      showLoading = true
    } = options;

    const token = requestTokenRef.current + 1;
    requestTokenRef.current = token;

    if (showLoading) {
      setLoading(true);
    }

    try {
      const result = await fetcher(page, options);
      if (requestTokenRef.current !== token) {
        return null;
      }
      onSuccess?.({ result, page, options });
      return result;
    } catch (error) {
      if (requestTokenRef.current === token) {
        onError?.({ error, page, options });
      }
      throw error;
    } finally {
      if (requestTokenRef.current === token && showLoading) {
        setLoading(false);
      }
    }
  }, [fetcher, onSuccess, onError]);

  const cancelPending = useCallback(() => {
    requestTokenRef.current += 1;
    setLoading(false);
  }, []);

  return {
    loadPage,
    loading,
    cancelPending
  };
};

export default usePagedContent;
