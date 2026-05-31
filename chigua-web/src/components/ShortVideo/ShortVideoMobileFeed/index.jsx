/**
 * 短视频分类 - 移动端入口
 * 点击分类直接进入播放页面，无列表
 */
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import categoryService from '../../../services/categoryService';
import ShortVideoMobilePlayer from '../ShortVideoMobilePlayer';

const PAGE_SIZE = 20;

const ShortVideoMobileFeed = ({ categoryId }) => {
  const navigate = useNavigate();
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const [totalCount, setTotalCount] = useState(0);
  const [replaceKey, setReplaceKey] = useState(0);
  const [initialIndex, setInitialIndex] = useState(0);
  const pageRef = useRef(1);
  const loadingRef = useRef(false);
  const videosRef = useRef([]);
  videosRef.current = videos;

  const loadPage = useCallback(async (page = 1, append = false, forceRefresh = false) => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    try {
      const res = await categoryService.getShortVideos({
        categoryId: categoryId || null,
        pageNum: page,
        pageSize: PAGE_SIZE,
        _forceRefresh: forceRefresh,
      });
      const rows = Array.isArray(res?.rows) ? res.rows : [];
      const total = Number(res?.total || 0);

      if (append) {
        setVideos(prev => [...prev, ...rows]);
      } else {
        setVideos(rows);
      }
      setTotalCount(total);
      setHasMore(page * PAGE_SIZE < total);
      pageRef.current = page;
    } catch (_) {
      setHasMore(false);
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, [categoryId]);

  useEffect(() => {
    loadPage(1, false, true); // 强制刷新获取最新签名 URL，避免首个视频 XHR 失败
  }, [categoryId, loadPage]);

  const handleClosePlayer = () => {
    navigate(-1);
  };

  const handleLoadMore = useCallback(() => {
    if (!loadingRef.current && hasMore) {
      loadPage(pageRef.current + 1, true);
    }
  }, [hasMore, loadPage]);

  const handleRandomJump = useCallback(async () => {
    if (totalCount <= 0) return;
    const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE));
    if (totalPages <= 1) return;

    const targetPage = Math.floor(Math.random() * totalPages) + 1;
    await loadPage(targetPage, false, true);
    setInitialIndex(0);
    setReplaceKey(prev => prev + 1);
  }, [totalCount, loadPage]);

  useEffect(() => {
    const handler = () => handleRandomJump();
    window.addEventListener('shortVideo:randomPage', handler);
    return () => window.removeEventListener('shortVideo:randomPage', handler);
  }, [handleRandomJump]);

  if (loading && videos.length === 0) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-black">
        <div className="text-white">加载中...</div>
      </div>
    );
  }

  if (videos.length === 0) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-black">
        <div className="text-white text-center">暂无视频</div>
      </div>
    );
  }

  return (
    <ShortVideoMobilePlayer
      key={replaceKey}
      videos={videos}
      initialIndex={initialIndex}
      onClose={handleClosePlayer}
      onLoadMore={handleLoadMore}
      hasMore={hasMore}
      isLoadingMore={loading && videos.length > 0}
      onRandomJump={handleRandomJump}
    />
  );
};

export default ShortVideoMobileFeed;
