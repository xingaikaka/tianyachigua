import React, { useState, useEffect, useLayoutEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import userService from '../../services/userService';
import videoStatsService from '../../services/videoStatsService';
import UserXfreeDetail from '../../components/UserVideo/UserXfreeDetail';
import MobileFullScreenPlayer from '../../components/UserVideo/MobileFullScreenPlayer';

const PAGE_SIZE = 50;

const isMobileDevice = () =>
  /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ||
  window.innerWidth < 768;

const LOADING_SCREEN = (
  <div style={{
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    height: '100vh', background: '#080808',
  }}>
    <div style={{ color: '#888', fontSize: 14 }}>加载中...</div>
  </div>
);

const UserVideoPlayer = () => {
  const { username, videoId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [isMobile, setIsMobile] = useState(isMobileDevice);
  const [user, setUser] = useState(null);
  const [userLoading, setUserLoading] = useState(true);

  // 移动端专用：视频列表 + 分页
  const [videos, setVideos] = useState([]);
  const [initialIndex, setInitialIndex] = useState(0);
  const [videosLoading, setVideosLoading] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const currentPageRef = useRef(1);
  const isFetchingRef = useRef(false);

  useEffect(() => {
    const check = () => setIsMobile(isMobileDevice());
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
  }, []);

  // 移动端立即隐藏 Header，useLayoutEffect 在首次绘制前同步执行，
  // 避免进入播放页时导航栏闪现（比 MobileFullScreenPlayer 内部的 useEffect 更早）
  useLayoutEffect(() => {
    if (!isMobile) return;
    document.body.classList.add('user-group-fullscreen-open');
    window.dispatchEvent(new CustomEvent('userGroupFullScreen:change', { detail: { open: true } }));
    return () => {
      document.body.classList.remove('user-group-fullscreen-open');
      window.dispatchEvent(new CustomEvent('userGroupFullScreen:change', { detail: { open: false } }));
    };
  }, [isMobile]);

  // 获取用户信息，用 AbortController 防止组件卸载后 setState
  // 视频维度的浏览埋点（仅在 videoId 变化时上报一次，避免列表内滑动重复触发）
  const lastTrackedVideoIdRef = useRef(null);
  useEffect(() => {
    if (!videoId) return;
    if (lastTrackedVideoIdRef.current === videoId) return;
    lastTrackedVideoIdRef.current = videoId;
    try { videoStatsService.trackRedgifsView(videoId, { username }); } catch (_) {}
  }, [videoId, username]);

  useEffect(() => {
    if (!username) return;
    const controller = new AbortController();
    setUserLoading(true);
    userService.getUserByUsername(username)
      .then(res => {
        if (controller.signal.aborted) return;
        if (res?.code === 200) {
          setUser(res.data);
        } else {
          navigate('/404');
        }
      })
      .catch(() => { if (!controller.signal.aborted) navigate('/404'); })
      .finally(() => { if (!controller.signal.aborted) setUserLoading(false); });
    return () => controller.abort();
  }, [username, navigate]);

  // 移动端：获取视频列表，找到目标视频的初始索引
  useEffect(() => {
    if (!isMobile || !user?.id) return;

    // ── ① 优先使用 list 页透传的 state：直接复用已加载的数据 ──
    //    解决「第 N 页点视频进入播放，滑动下一个跳回第 1 页」的 bug，
    //    并保证 loadMore 从正确的下一页继续。
    const stateVideos = location.state?.videos;
    const statePage = location.state?.currentPage;
    const stateTotal = location.state?.total;
    if (Array.isArray(stateVideos) && stateVideos.length > 0 && statePage) {
      const idx = videoId
        ? stateVideos.findIndex(v => String(v.id) === String(videoId))
        : 0;
      if (idx >= 0) {
        setVideos(stateVideos);
        setInitialIndex(idx);
        currentPageRef.current = statePage;
        setHasMore((statePage * PAGE_SIZE) < (stateTotal || 0));
        setVideosLoading(false);
        return;
      }
    }

    // ── ② Fallback：直接访问 URL / 刷新页面，无 state → 走旧逻辑 ──
    //    注意：此分支只在用户分享链接、刷新等少数场景命中，
    //    无法精确还原原始页码上下文，仅尽力把目标视频展示出来。
    const controller = new AbortController();
    setVideosLoading(true);
    userService.getUserVideos(user.id, 1, PAGE_SIZE)
      .then(res => {
        if (controller.signal.aborted) return;
        if (res?.code === 200) {
          const rows = res.rows || [];
          currentPageRef.current = 1;
          setHasMore(rows.length < (res.total || 0));

          if (!videoId) {
            setVideos(rows);
            setInitialIndex(0);
            return;
          }

          const idx = rows.findIndex(v => String(v.id) === String(videoId));
          if (idx >= 0) {
            setVideos(rows);
            setInitialIndex(idx);
          } else {
            // 不在第一页，单独拉取该视频插入列表头部
            userService.getUserVideoById(videoId)
              .then(res2 => {
                if (controller.signal.aborted) return;
                if (res2?.code === 200 && res2.data) {
                  setVideos([res2.data, ...rows]);
                } else {
                  setVideos(rows);
                }
                setInitialIndex(0);
              })
              .catch(() => {
                if (!controller.signal.aborted) {
                  setVideos(rows);
                  setInitialIndex(0);
                }
              });
          }
        }
      })
      .finally(() => { if (!controller.signal.aborted) setVideosLoading(false); });
    return () => controller.abort();
  }, [isMobile, user?.id, videoId, location.state]);

  // 移动端：加载更多视频
  const handleLoadMore = useCallback(() => {
    if (isFetchingRef.current || !user?.id) return;
    isFetchingRef.current = true;
    setIsLoadingMore(true);
    const nextPage = currentPageRef.current + 1;
    userService.getUserVideos(user.id, nextPage, PAGE_SIZE)
      .then(res => {
        if (res?.code === 200) {
          const rows = res.rows || [];
          const total = res.total || 0;
          setVideos(prev => {
            const merged = [...prev, ...rows];
            setHasMore(merged.length < total);
            return merged;
          });
          currentPageRef.current = nextPage;
        }
      })
      .finally(() => {
        isFetchingRef.current = false;
        setIsLoadingMore(false);
      });
  }, [user?.id]);

  // 视频切换时静默更新 URL，不触发 React Router 重渲染
  const handleVideoChange = useCallback((newVideoId) => {
    window.history.replaceState(null, '', `/user/${username}/video/${newVideoId}`);
  }, [username]);

  // 关闭：浏览器回退（pop，不新增历史记录），保证用户详情页返回时历史栈正确
  // location.key !== 'default' 表示通过应用内导航进入，可以安全 navigate(-1)
  // 否则（直接访问 URL / 新标签页）兜底到用户详情页
  const handleClose = useCallback(() => {
    if (location.key !== 'default') {
      navigate(-1);
    } else {
      navigate(`/user/${username}`);
    }
  }, [navigate, username, location.key]);

  if (userLoading || !user) return LOADING_SCREEN;

  // 移动端全屏 TikTok 播放器
  if (isMobile) {
    if (videosLoading) return LOADING_SCREEN;

    // 视频列表为空时的兜底
    if (!videos.length) {
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          height: '100vh', background: '#080808', gap: 16,
        }}>
          <div style={{ color: '#555', fontSize: 14 }}>暂无视频</div>
          <button
            onClick={handleClose}
            style={{ color: '#aaa', fontSize: 13, background: 'none', border: 'none', cursor: 'pointer' }}
          >
            返回
          </button>
        </div>
      );
    }

    return (
      <MobileFullScreenPlayer
        videos={videos}
        initialIndex={initialIndex}
        onClose={handleClose}
        userName={username}
        onLoadMore={handleLoadMore}
        hasMore={hasMore}
        isLoadingMore={isLoadingMore}
        onVideoChange={handleVideoChange}
      />
    );
  }

  // PC 分屏播放器
  return (
    <UserXfreeDetail
      user={user}
      initialVideoId={videoId}
      onClose={handleClose}
      onVideoChange={handleVideoChange}
    />
  );
};

export default UserVideoPlayer;
