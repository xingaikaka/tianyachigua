import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import userService from '../../services/userService';
import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';
import Pagination from '../../components/ui/Pagination';

// ── 缓存工具（sessionStorage，5 分钟有效）────────────────────────────────────
const CACHE_TTL = 5 * 60 * 1000;
const cacheKey = (u) => `ud_cache_${u}`;

function readCache(username) {
  try {
    const raw = sessionStorage.getItem(cacheKey(username));
    if (!raw) return null;
    const d = JSON.parse(raw);
    if (Date.now() - d.ts > CACHE_TTL) {
      sessionStorage.removeItem(cacheKey(username));
      return null;
    }
    return d;
  } catch {
    return null;
  }
}

function writeCache(username, user, videos, pagination, scrollY) {
  try {
    sessionStorage.setItem(
      cacheKey(username),
      JSON.stringify({ user, videos, pagination, scrollY, ts: Date.now() })
    );
  } catch {}
}

// ── 组件 ─────────────────────────────────────────────────────────────────────
const UserDetail = () => {
  const { username } = useParams();
  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [videoLoading, setVideoLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 50, total: 0 });

  // 用 ref 保存最新 state，避免 handleVideoClick 的 stale closure 问题
  const stateRef = useRef({ user: null, videos: [], pagination: { current: 1, pageSize: 50, total: 0 } });
  stateRef.current = { user, videos, pagination };

  // 获取视频列表，写入缓存
  const fetchVideos = useCallback(async (userId, pageNum, pageSize, shouldScrollTop = true) => {
    setVideoLoading(true);
    try {
      const response = await userService.getUserVideos(userId, pageNum, pageSize);
      if (response && response.code === 200) {
        const newVideos = response.rows || [];
        const newPagination = { current: pageNum, pageSize, total: response.total || 0 };
        setVideos(newVideos);
        setPagination(newPagination);
        // 翻页时更新缓存（scrollY = 0，因为翻页后回到顶部）
        const u = stateRef.current.user;
        if (u) writeCache(username, u, newVideos, newPagination, 0);
        if (shouldScrollTop) window.scrollTo(0, 0);
      }
    } catch (error) {
      console.error('Failed to fetch videos:', error);
    } finally {
      setVideoLoading(false);
    }
  }, [username]);

  // 初始化：优先读缓存，缓存命中则恢复滚动位置，否则请求数据
  useEffect(() => {
    const cached = readCache(username);
    if (cached) {
      setUser(cached.user);
      setVideos(cached.videos);
      setPagination(cached.pagination);
      setLoading(false);
      // 恢复滚动位置（rAF 确保 DOM 已渲染）
      if (cached.scrollY > 0) {
        requestAnimationFrame(() => window.scrollTo(0, cached.scrollY));
      }
      return;
    }

    // 无缓存，走正常请求流程
    window.scrollTo(0, 0);
    setLoading(true);

    const controller = new AbortController();

    const fetchUser = async () => {
      try {
        const response = await userService.getUserByUsername(username);
        if (controller.signal.aborted) return;
        if (response && response.code === 200) {
          const userData = response.data;
          setUser(userData);
          stateRef.current.user = userData;
          await fetchVideos(userData.id, 1, 50, false);
          window.scrollTo(0, 0);
        } else {
          navigate('/404');
        }
      } catch (error) {
        if (!controller.signal.aborted) console.error('Failed to fetch user:', error);
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };

    if (username) fetchUser();
    return () => controller.abort();
  }, [username, navigate, fetchVideos]);

  // 翻页
  const handlePageChange = (page) => {
    const { user: u, pagination: p } = stateRef.current;
    if (u && !videoLoading) {
      fetchVideos(u.id, page, p.pageSize, true);
    }
  };

  // 视频点击：先缓存当前状态（含滚动位置），再跳转
  // 通过 location.state 把当前页的视频列表 + 页码透传给播放页，
  // 避免播放页强行从第 1 页开始拉数据导致「第 N 页点视频，下一个跳到第 1 页」的 bug。
  const handleVideoClick = (video) => {
    const { user: u, videos: vs, pagination: p } = stateRef.current;
    if (u && vs.length > 0) {
      writeCache(username, u, vs, p, window.scrollY);
    }
    navigate(`/user/${username}/video/${video.id}`, {
      state: {
        videos: vs,
        currentPage: p.current,
        total: p.total,
      },
    });
  };

  if (loading && !user) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-[#121212]">
        <div className="text-white">加载中...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#121212] text-white pb-10">

      {/* 视频列表 (Masonry / Grid) */}
      <div className="mx-auto px-2 md:px-4 max-w-[850px] pt-3">
        <div className="grid grid-cols-3 md:grid-cols-4 gap-2 md:gap-4">
          {videos.map((video) => (
            <div
              key={video.id}
              className="relative group cursor-pointer aspect-[9/16] bg-[#2a2a2a] rounded overflow-hidden min-w-0"
              onClick={() => handleVideoClick(video)}
            >
              <SecureDecryptedImage
                src={video.posterUrl || video.thumbnailUrl}
                alt={video.title}
                className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
              />

              {/* 遮罩和信息 */}
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-3">
                <h3 className="text-sm font-medium line-clamp-2 mb-1">{video.title}</h3>
                <div className="flex justify-between items-center text-xs text-gray-400">
                  <span>{formatNumber(video.views)} 次播放</span>
                  <span>{formatNumber(video.likes)} 点赞</span>
                </div>
              </div>

              {/* 播放图标 */}
              <div className="absolute top-2 right-2 bg-black/50 rounded-full p-1">
                <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z"/>
                </svg>
              </div>
            </div>
          ))}
        </div>

        {/* 标准分页 */}
        {videoLoading && (
          <div className="flex justify-center mt-8 mb-4">
            <div className="text-gray-400 text-sm">加载中...</div>
          </div>
        )}
        {!videoLoading && pagination.total > 0 && (
          <div className="mt-8">
            <Pagination
              current={pagination.current}
              total={pagination.total}
              pageSize={pagination.pageSize}
              onChange={handlePageChange}
            />
          </div>
        )}
      </div>

    </div>
  );
};

const formatNumber = (num) => {
  if (!num) return '0';
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
  return num.toString();
};

export default UserDetail;
