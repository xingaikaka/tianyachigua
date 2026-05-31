import React, { useState, useEffect, useCallback, useRef, memo } from 'react';
import { createPortal } from 'react-dom';
import Hls from 'hls.js';
import userService from '../../services/userService';
import videoStatsService from '../../services/videoStatsService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../../utils/hlsUtils';

const PAGE_SIZE = 50;
const BG_FPS = 12;
// timeupdate 节流：进度变化超过此阈值才 setState（避免每秒几百次重渲染）
const PROGRESS_THRESHOLD = 0.3;

const formatNumber = (num) => {
  if (!num) return '0';
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
  return num.toString();
};

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
};

const UserXfreeDetail = ({ user, onClose, initialVideoId, onVideoChange }) => {
  const [userDetail, setUserDetail] = useState(user);
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [currentVideoIndex, setCurrentVideoIndex] = useState(null);
  const [currentVideo, setCurrentVideo] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [isVideoLoading, setIsVideoLoading] = useState(false);
  // 记录已渲染出首帧的视频 ID，派生封面遮罩（无异步间隙）
  const [playingVideoId, setPlayingVideoId] = useState(null);

  // ── 派生值（渲染时同步计算，无需 effect）
  const hasMore = videos.length < total && total > 0;
  const showPosterOverlay = currentVideo
    ? (currentVideo.id !== playingVideoId || isVideoLoading)
    : false;

  // ── Refs（直接在渲染时赋值，替代4个 useEffect 同步）
  const videosRef = useRef(videos);
  const currentVideoIndexRef = useRef(currentVideoIndex);
  const hasMoreRef = useRef(hasMore);
  const currentPageRef = useRef(currentPage);
  videosRef.current = videos;
  currentVideoIndexRef.current = currentVideoIndex;
  hasMoreRef.current = hasMore;
  currentPageRef.current = currentPage;

  const videoRef = useRef(null);
  const bgCanvasRef = useRef(null);
  const bgAnimRef = useRef(null);
  const bgLastFrameRef = useRef(0);
  const hlsRef = useRef(null);
  const nativeBlobUrlRef = useRef(null);
  const listEndRef = useRef(null);
  const rightPanelRef = useRef(null);
  const controlsTimerRef = useRef(null);
  const isFetchingRef = useRef(false);
  const pendingNextRef = useRef(false);
  // timeupdate 节流：记录上次已设置的进度值
  const lastProgressRef = useRef(0);
  const durationSetRef = useRef(false); // duration 只需设置一次

  // ─── 拉取完整用户信息 ──────────────────────────────────
  useEffect(() => {
    if (!user?.username) return;
    userService.getUserByUsername(user.username)
      .then(res => { if (res?.code === 200) setUserDetail(res.data); })
      .catch(() => {});
  }, [user?.username]);

  // ─── 拉取用户视频 ────────────────────────────────────
  const fetchVideos = useCallback(async (pageNum, reset = false) => {
    if (isFetchingRef.current) return;
    const userId = user?.id;
    if (!userId) return;
    isFetchingRef.current = true;
    if (reset) setLoading(true);
    else setLoadingMore(true);
    try {
      const res = await userService.getUserVideos(userId, pageNum, PAGE_SIZE);
      if (res?.code === 200) {
        const rows = res.rows || [];
        setVideos(prev => reset ? rows : [...prev, ...rows]);
        setTotal(res.total || 0);
        setCurrentPage(pageNum);
        if (reset && rows.length > 0 && currentVideoIndexRef.current === null) {
          const targetIdx = initialVideoId
            ? rows.findIndex(v => String(v.id) === String(initialVideoId))
            : -1;

          if (targetIdx >= 0) {
            // 在第一页找到：正常定位，并滚动列表到该位置
            setCurrentVideoIndex(targetIdx);
            setCurrentVideo(rows[targetIdx]);
            scrollToVideoInList(targetIdx);
          } else if (initialVideoId) {
            // 不在第一页：插到列表第0位，高亮第0条（方案B）
            userService.getUserVideoById(initialVideoId)
              .then(res2 => {
                if (res2?.code === 200 && res2.data) {
                  const sharedVideo = res2.data;
                  setVideos([sharedVideo, ...rows]);
                  setCurrentVideoIndex(0);
                  setCurrentVideo(sharedVideo);
                } else {
                  setCurrentVideoIndex(0);
                  setCurrentVideo(rows[0]);
                }
              })
              .catch(() => {
                setCurrentVideoIndex(0);
                setCurrentVideo(rows[0]);
              });
          } else {
            setCurrentVideoIndex(0);
            setCurrentVideo(rows[0]);
          }
        }
      }
    } finally {
      isFetchingRef.current = false;
      if (reset) setLoading(false);
      else setLoadingMore(false);
    }
  }, [user?.id, initialVideoId]);

  useEffect(() => {
    fetchVideos(1, true);
  }, [user?.id]);

  // 加载更多后，若有待跳转则继续
  useEffect(() => {
    if (!pendingNextRef.current) return;
    const nextIdx = (currentVideoIndexRef.current ?? -1) + 1;
    if (nextIdx < videosRef.current.length) {
      pendingNextRef.current = false;
      setCurrentVideoIndex(nextIdx);
      setCurrentVideo(videosRef.current[nextIdx]);
      scrollToVideoInList(nextIdx);
    }
  }, [videos]);

  // ─── 无限滚动 ────────────────────────────────────────
  useEffect(() => {
    const el = listEndRef.current;
    const container = rightPanelRef.current;
    if (!el || !container) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMoreRef.current && !isFetchingRef.current) {
          fetchVideos(currentPageRef.current + 1, false);
        }
      },
      { root: container, threshold: 0, rootMargin: '0px 0px 100px 0px' }
    );
    observer.observe(el);
    return () => observer.disconnect();
    // loadingMore 移除：每次 loadingMore 变化不需要重建 observer，有 isFetchingRef 保护
  }, [fetchVideos, loading]);

  // ─── Canvas 背景帧绘制（12fps，blur 背景不需要高帧率）────
  useEffect(() => {
    const interval = 1000 / BG_FPS;
    const draw = (timestamp) => {
      const video = videoRef.current;
      const canvas = bgCanvasRef.current;
      if (video && canvas && video.readyState >= 2 && !video.paused && !video.ended) {
        if (timestamp - bgLastFrameRef.current >= interval) {
          try {
            canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
          } catch (_) { /* CORS 静默忽略，降级到封面图背景 */ }
          bgLastFrameRef.current = timestamp;
        }
      }
      bgAnimRef.current = requestAnimationFrame(draw);
    };
    bgAnimRef.current = requestAnimationFrame(draw);
    return () => { if (bgAnimRef.current) cancelAnimationFrame(bgAnimRef.current); };
  }, []);

  // ─── HLS / 视频切换播放 ──────────────────────────────
  useEffect(() => {
    if (!currentVideo || !videoRef.current) return;
    const url = currentVideo.hdUrl || currentVideo.sdUrl;
    if (!url) return;

    setIsVideoLoading(true);
    setIsPlaying(false);
    setProgress(0);
    setDuration(0);
    lastProgressRef.current = 0;
    durationSetRef.current = false;

    if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; }
    if (nativeBlobUrlRef.current) { URL.revokeObjectURL(nativeBlobUrlRef.current); nativeBlobUrlRef.current = null; }

    const video = videoRef.current;
    video.pause();
    video.removeAttribute('src');
    video.load();

    const onCanPlay = () => {
      setIsVideoLoading(false);
      video.play().catch(() => {});
    };
    const thisVideoId = currentVideo.id;
    let played = false;
    const onPlay = () => {
      setIsPlaying(true);
      // 用户行为埋点：仅首次播放上报一次（避免暂停/继续重复触发）
      if (!played) {
        played = true;
        try { videoStatsService.trackRedgifsPlay(thisVideoId, { username: currentVideo?.userName }); } catch (_) {}
      }
      requestAnimationFrame(() => {
        requestAnimationFrame(() => setPlayingVideoId(thisVideoId));
      });
    };
    const onPause = () => setIsPlaying(false);

    // durationchange：专门监听 duration 就绪（HLS 初期 duration 为 NaN，
    // 片段加载后才会变成有效值，timeupdate 里的 duration <= 0 不能过滤 NaN）
    const onDurationChange = () => {
      const vid = videoRef.current;
      if (!vid) return;
      const dur = vid.duration;
      if (isFinite(dur) && dur > 0) {
        durationSetRef.current = true;
        setDuration(dur);
      }
    };

    const onTimeUpdate = () => {
      const vid = videoRef.current;
      if (!vid) return;
      const dur = vid.duration;
      // isFinite 同时过滤 NaN 和 Infinity（HLS 直播流为 Infinity）
      if (!isFinite(dur) || dur <= 0) return;
      // duration 尚未通过 durationchange 设置时，在这里补设
      if (!durationSetRef.current) {
        durationSetRef.current = true;
        setDuration(dur);
      }
      // 进度节流：变化超过阈值才触发 setState
      const pct = (vid.currentTime / dur) * 100;
      if (Math.abs(pct - lastProgressRef.current) >= PROGRESS_THRESHOLD) {
        lastProgressRef.current = pct;
        setProgress(pct);
      }
    };
    const onEnded = () => setIsPlaying(false);
    const onError = () => setIsVideoLoading(false);

    video.addEventListener('canplay', onCanPlay);
    video.addEventListener('play', onPlay);
    video.addEventListener('pause', onPause);
    video.addEventListener('durationchange', onDurationChange);
    video.addEventListener('timeupdate', onTimeUpdate);
    video.addEventListener('ended', onEnded);
    video.addEventListener('error', onError);

    if (url.includes('.m3u8')) {
      if (Hls.isSupported()) {
        const hls = new Hls({ xhrSetup: hlsXhrSetup, startLevel: -1 });
        hlsRef.current = hls;
        hls.loadSource(url);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => { video.play().catch(() => {}); });
        hls.on(Hls.Events.ERROR, (_, data) => { if (data.fatal) setIsVideoLoading(false); });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        patchNativeHlsM3u8(url).then(({ url: patchedUrl, isBlob }) => {
          if (isBlob) nativeBlobUrlRef.current = patchedUrl;
          video.src = patchedUrl;
          video.play().catch(() => {});
        });
      }
    } else {
      video.src = url;
    }

    return () => {
      video.removeEventListener('canplay', onCanPlay);
      video.removeEventListener('play', onPlay);
      video.removeEventListener('pause', onPause);
      video.removeEventListener('durationchange', onDurationChange);
      video.removeEventListener('timeupdate', onTimeUpdate);
      video.removeEventListener('ended', onEnded);
      video.removeEventListener('error', onError);
    };
  }, [currentVideo?.id]);

  // ─── 卸载清理（合并所有清理逻辑到一处）────────────────
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => {
      if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; }
      if (nativeBlobUrlRef.current) { URL.revokeObjectURL(nativeBlobUrlRef.current); nativeBlobUrlRef.current = null; }
      if (bgAnimRef.current) cancelAnimationFrame(bgAnimRef.current);
      if (controlsTimerRef.current) clearTimeout(controlsTimerRef.current);
      document.body.style.overflow = '';
    };
  }, []);

  // ─── 控制栏自动隐藏 ──────────────────────────────────
  const showControlsTemporarily = useCallback(() => {
    setShowControls(true);
    clearTimeout(controlsTimerRef.current);
    controlsTimerRef.current = setTimeout(() => setShowControls(false), 3000);
  }, []);

  // ─── 操作处理 ────────────────────────────────────────
  const handlePlayPause = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) video.play().catch(() => {});
    else video.pause();
  }, []);

  const handlePrev = useCallback(() => {
    const idx = currentVideoIndexRef.current;
    if (idx === null || idx <= 0) return;
    const next = idx - 1;
    setCurrentVideoIndex(next);
    const video = videosRef.current[next];
    setCurrentVideo(video);
    scrollToVideoInList(next);
    if (onVideoChange && video?.id) onVideoChange(video.id);
  }, [onVideoChange]);

  const handleNext = useCallback(() => {
    const idx = currentVideoIndexRef.current;
    const vids = videosRef.current;
    if (idx === null) return;
    if (idx < vids.length - 1) {
      const next = idx + 1;
      setCurrentVideoIndex(next);
      const video = vids[next];
      setCurrentVideo(video);
      scrollToVideoInList(next);
      if (onVideoChange && video?.id) onVideoChange(video.id);
    } else if (hasMoreRef.current && !isFetchingRef.current) {
      pendingNextRef.current = true;
      fetchVideos(currentPageRef.current + 1, false);
    }
  }, [fetchVideos, onVideoChange]);

  // ESC / 方向键（置于 handlePrev/handleNext 定义之后，避免 TDZ 错误）
  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'Escape') { onClose(); return; }
      if (e.key === 'ArrowUp') { e.preventDefault(); handlePrev(); }
      if (e.key === 'ArrowDown') { e.preventDefault(); handleNext(); }
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose, handlePrev, handleNext]);

  const handleMuteToggle = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = !video.muted;
    setIsMuted(video.muted);
  }, []);

  const handleProgressClick = useCallback((e) => {
    const video = videoRef.current;
    if (!video || !video.duration) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const fraction = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    video.currentTime = fraction * video.duration;
    const pct = fraction * 100;
    lastProgressRef.current = pct;
    setProgress(pct);
  }, []);

  const scrollToVideoInList = (idx) => {
    setTimeout(() => {
      const el = document.getElementById(`xfree-video-${idx}`);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 80);
  };

  const handleVideoClick = useCallback((video, idx) => {
    setCurrentVideoIndex(idx);
    setCurrentVideo(video);
    scrollToVideoInList(idx);
    if (onVideoChange && video?.id) onVideoChange(video.id);
  }, [onVideoChange]);

  const handleShare = useCallback(async () => {
    const url = window.location.href;
    if (currentVideo?.id) {
      try { videoStatsService.trackRedgifsShare(currentVideo.id, { username: currentVideo?.userName }); } catch (_) {}
    }
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(url);
      } else {
        const ta = document.createElement('textarea');
        ta.value = url;
        ta.style.cssText = 'position:fixed;opacity:0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
      }
      // 临时显示复制成功提示
      const tip = document.createElement('div');
      tip.textContent = '链接已复制';
      tip.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:rgba(0,0,0,0.85);color:#fff;padding:18px 40px;border-radius:28px;font-size:18px;font-weight:500;letter-spacing:1px;z-index:999999;pointer-events:none;';
      document.body.appendChild(tip);
      setTimeout(() => document.body.removeChild(tip), 2000);
    } catch (e) {
      console.error('Share failed:', e);
    }
  }, []);

  const poster = currentVideo?.posterUrl || currentVideo?.thumbnailUrl;
  const isAtLast = currentVideoIndex !== null && currentVideoIndex >= videos.length - 1;
  const nextBtnLoading = isAtLast && hasMore && loadingMore;
  const nextBtnDisabled = isAtLast && !hasMore;

  const btnBase = {
    width: 40, height: 40, borderRadius: '50%',
    border: '1px solid rgba(255,255,255,0.25)',
    background: 'rgba(0,0,0,0.6)',
    backdropFilter: 'blur(12px)',
    WebkitBackdropFilter: 'blur(12px)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    cursor: 'pointer', color: '#fff',
    transition: 'all 0.2s', outline: 'none', flexShrink: 0,
  };

  const navBtn = (disabled) => ({
    ...btnBase, width: 44, height: 44,
    opacity: disabled ? 0.25 : 1,
    cursor: disabled ? 'not-allowed' : 'pointer',
  });

  return createPortal(
    <div
      style={{
        position: 'fixed',
        top: 0, left: 0, right: 0, bottom: 0,
        width: '100vw', height: '100vh',
        zIndex: 99999,
        display: 'flex',
        background: '#080808',
        fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
        overflow: 'hidden',
      }}
    >
      {/* ════ 左侧：视频播放区 ════ */}
      <div
        style={{
          flex: 1,
          position: 'relative',
          overflow: 'hidden',
          background: '#000',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
        onMouseMove={showControlsTemporarily}
        onMouseEnter={showControlsTemporarily}
        onMouseLeave={() => {
          clearTimeout(controlsTimerRef.current);
          controlsTimerRef.current = setTimeout(() => setShowControls(false), 1200);
        }}
      >
        {/* 层0：封面图模糊背景（兜底） */}
        {poster && (
          <div style={{ position: 'absolute', inset: 0, zIndex: 0, overflow: 'hidden' }}>
            <SecureDecryptedImage
              src={poster}
              alt=""
              lazyLoad={false}
              showLoadingIndicator={false}
              className="absolute inset-0 w-full h-full"
              imageStyle={{
                objectFit: 'cover',
                filter: 'blur(40px)',
                transform: 'scale(1.12)',
                opacity: 0.85,
              }}
            />
          </div>
        )}

        {/* 层1：Canvas 实时视频帧模糊背景 */}
        <canvas
          ref={bgCanvasRef}
          width={320}
          height={180}
          style={{
            position: 'absolute', inset: 0,
            width: '100%', height: '100%',
            objectFit: 'cover',
            filter: 'blur(40px)',
            transform: 'scale(1.12)',
            opacity: 1,
            zIndex: 1,
            pointerEvents: 'none',
          }}
        />

        {/* 层2：主视频 */}
        <video
          ref={videoRef}
          playsInline
          muted={isMuted}
          onClick={handlePlayPause}
          style={{
            position: 'relative', zIndex: 2,
            maxWidth: '100%', maxHeight: '100%',
            width: '100%', height: '100%',
            objectFit: 'contain',
            cursor: 'pointer',
            display: currentVideo ? 'block' : 'none',
          }}
        />

        {/* 层3：封面图遮罩（opacity 过渡，无闪烁） */}
        {currentVideo && poster && (
          <div
            style={{
              position: 'absolute', inset: 0, zIndex: 3,
              background: '#000',
              opacity: showPosterOverlay ? 1 : 0,
              transition: 'opacity 0.45s ease',
              pointerEvents: 'none',
            }}
          >
            <SecureDecryptedImage
              src={poster}
              alt={currentVideo.title || ''}
              lazyLoad={false}
              showLoadingIndicator={false}
              className="absolute inset-0 w-full h-full"
              imageStyle={{ objectFit: 'contain' }}
            />
            {isVideoLoading && (
              <div style={{
                position: 'absolute', inset: 0, zIndex: 1,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <div style={{
                  width: 52, height: 52, borderRadius: '50%',
                  border: '3px solid rgba(255,255,255,0.15)',
                  borderTopColor: 'rgba(255,255,255,0.85)',
                  animation: 'xf-spin 0.75s linear infinite',
                }} />
              </div>
            )}
          </div>
        )}

        {/* 无视频占位 */}
        {!currentVideo && !loading && (
          <div style={{ position: 'relative', zIndex: 5, color: '#555', textAlign: 'center' }}>
            <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2"
              style={{ margin: '0 auto 12px', display: 'block', opacity: 0.3 }}>
              <polygon points="5 3 19 12 5 21 5 3" />
            </svg>
            <div style={{ fontSize: 14, color: '#444' }}>点击右侧作品开始播放</div>
          </div>
        )}

        {/* 暂停播放按钮 */}
        {!isPlaying && currentVideo && !isVideoLoading && !showPosterOverlay && (
          <div
            onClick={handlePlayPause}
            style={{
              position: 'absolute', zIndex: 5,
              top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
              width: 72, height: 72, borderRadius: '50%',
              background: 'rgba(0,0,0,0.6)',
              backdropFilter: 'blur(12px)',
              WebkitBackdropFilter: 'blur(12px)',
              border: '1px solid rgba(255,255,255,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer',
            }}
          >
            <svg width="28" height="28" viewBox="0 0 24 24" fill="white">
              <polygon points="5 3 19 12 5 21 5 3" />
            </svg>
          </div>
        )}

        {/* 左上角：用户头像（始终显示） */}
        <div
          style={{
            position: 'absolute', top: 16, left: 18, zIndex: 11,
            display: 'flex', alignItems: 'center', gap: 10,
          }}
        >
          <div style={{
            width: 44, height: 44, borderRadius: '50%', overflow: 'hidden', flexShrink: 0,
            border: '2px solid rgba(255,255,255,0.35)',
            boxShadow: '0 2px 12px rgba(0,0,0,0.5)',
            background: '#222',
          }}>
            <SecureDecryptedImage
              src={userDetail?.profileImageUrl}
              alt={userDetail?.name || userDetail?.username}
              lazyLoad={false}
              showLoadingIndicator={false}
              className="w-full h-full"
              imageStyle={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          </div>
          <div style={{ lineHeight: 1.3 }}>
            <div style={{
              color: '#fff', fontSize: 13, fontWeight: 600,
              textShadow: '0 1px 6px rgba(0,0,0,0.9)',
              maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>
              {userDetail?.name || userDetail?.username}
            </div>
            {userDetail?.username && userDetail?.name && (
              <div style={{
                color: 'rgba(255,255,255,0.55)', fontSize: 11,
                maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
              }}>
                @{userDetail.username}
              </div>
            )}
          </div>
        </div>

        {/* 顶部控制栏（视频标题，鼠标悬停显示） */}
        <div
          style={{
            position: 'absolute', top: 0, left: 0, right: 0, zIndex: 10,
            display: 'flex', alignItems: 'center', gap: 12,
            padding: '20px 22px',
            paddingLeft: 180,
            background: 'linear-gradient(to bottom, rgba(0,0,0,0.75) 0%, transparent 100%)',
            opacity: showControls ? 1 : 0,
            transition: 'opacity 0.25s ease',
            pointerEvents: showControls ? 'auto' : 'none',
          }}
        >
          {currentVideo?.title && (
            <span style={{
              flex: 1, color: 'rgba(255,255,255,0.9)', fontSize: 13, fontWeight: 500,
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
              textShadow: '0 1px 6px rgba(0,0,0,0.9)',
            }}>
              {currentVideo.title}
            </span>
          )}

          <button
            onClick={handleShare}
            disabled={!currentVideo}
            style={{ ...btnBase, opacity: currentVideo ? 1 : 0.3 }}
            title="复制分享链接"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </button>
          <button onClick={handleMuteToggle} style={btnBase} title={isMuted ? '取消静音' : '静音'}>
            {isMuted ? (
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
                <line x1="23" y1="9" x2="17" y2="15" /><line x1="17" y1="9" x2="23" y2="15" />
              </svg>
            ) : (
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
                <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
                <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
              </svg>
            )}
          </button>
          <button onClick={onClose} style={btnBase} title="关闭 (ESC)">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* 底部进度条 */}
        {currentVideo && (
          <div
            style={{
              position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 10,
              padding: '0 22px 22px',
              background: 'linear-gradient(to top, rgba(0,0,0,0.75) 0%, transparent 100%)',
              opacity: showControls ? 1 : 0,
              transition: 'opacity 0.25s ease',
              pointerEvents: showControls ? 'auto' : 'none',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 7 }}>
              <span style={{ color: 'rgba(255,255,255,0.65)', fontSize: 12 }}>
                {formatTime((progress / 100) * duration)} / {formatTime(duration)}
              </span>
            </div>
            <div
              onClick={handleProgressClick}
              style={{
                height: 4, background: 'rgba(255,255,255,0.18)', borderRadius: 4,
                cursor: 'pointer', position: 'relative',
              }}
            >
              <div style={{
                height: '100%', borderRadius: 4,
                background: 'linear-gradient(90deg, #a855f7, #ec4899)',
                width: `${progress}%`,
                transition: 'width 0.12s linear',
              }} />
            </div>
          </div>
        )}

        {/* 右侧按钮组：上下切换 */}
        <div
          style={{
            position: 'absolute', right: 20, top: '50%', transform: 'translateY(-50%)',
            display: 'flex', flexDirection: 'column', gap: 10, zIndex: 10,
          }}
        >
          <button
            onClick={handlePrev}
            disabled={currentVideoIndex === null || currentVideoIndex <= 0}
            style={navBtn(currentVideoIndex === null || currentVideoIndex <= 0)}
            title="上一个 (↑)"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <polyline points="18 15 12 9 6 15" />
            </svg>
          </button>
          <button
            onClick={handleNext}
            disabled={nextBtnDisabled}
            style={navBtn(nextBtnDisabled)}
            title="下一个 (↓)"
          >
            {nextBtnLoading ? (
              <div style={{
                width: 16, height: 16, borderRadius: '50%',
                border: '2px solid rgba(255,255,255,0.2)',
                borderTopColor: '#fff',
                animation: 'xf-spin 0.7s linear infinite',
              }} />
            ) : (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            )}
          </button>
        </div>
      </div>

      {/* ════ 右侧：用户信息 + 作品列表 ════ */}
      <div
        style={{
          flex: '0 0 calc(38% * 4 / 5)',
          background: '#111',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          borderLeft: '1px solid #1c1c1c',
        }}
      >
        {/* 用户信息 */}
        <div
          style={{
            padding: '24px 22px 20px',
            borderBottom: '1px solid #1c1c1c',
            flexShrink: 0,
            background: 'linear-gradient(135deg, #141414 0%, #1a1a2a 100%)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{
              width: 68, height: 68, borderRadius: '50%', flexShrink: 0,
              border: '2px solid #7c3aed',
              boxShadow: '0 0 0 3px rgba(124,58,237,0.18), 0 4px 16px rgba(0,0,0,0.5)',
              overflow: 'hidden', background: '#222',
            }}>
              <SecureDecryptedImage
                src={userDetail?.profileImageUrl}
                alt={userDetail?.name}
                className="w-full h-full"
                imageStyle={{ objectFit: 'cover' }}
                lazyLoad={false}
                showLoadingIndicator={false}
              />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 3 }}>
                <h2 style={{
                  color: '#fff', fontSize: 17, fontWeight: 700, margin: 0,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                }}>
                  {userDetail?.name || userDetail?.username}
                </h2>
                {userDetail?.verified === 1 && (
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    width: 18, height: 18, borderRadius: '50%',
                    background: '#7c3aed', flexShrink: 0,
                  }}>
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                  </span>
                )}
              </div>
              <p style={{ color: '#666', fontSize: 12, margin: '0 0 11px' }}>
                @{userDetail?.username}
              </p>
              <div style={{ display: 'flex', gap: 20 }}>
                <StatItem value={formatNumber(userDetail?.followers)} label="粉丝" />
                <StatItem value={formatNumber(userDetail?.publishedGifsCount || userDetail?.gifsCount)} label="视频" />
                <StatItem value={formatNumber(userDetail?.views)} label="播放" />
              </div>
            </div>
          </div>
        </div>

        {/* 作品列表 */}
        <div
          ref={rightPanelRef}
          style={{
            flex: 1, overflowY: 'auto', padding: '12px 10px',
            scrollbarWidth: 'thin', scrollbarColor: '#2a2a2a transparent',
          }}
        >
          {loading ? (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 220 }}>
              <div style={{
                width: 34, height: 34, borderRadius: '50%',
                border: '3px solid rgba(255,255,255,0.08)',
                borderTopColor: '#7c3aed',
                animation: 'xf-spin 0.8s linear infinite',
              }} />
            </div>
          ) : (
            <>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 4 }}>
                {videos.map((video, idx) => (
                  <VideoThumb
                    key={video.id}
                    id={`xfree-video-${idx}`}
                    video={video}
                    isActive={currentVideoIndex === idx}
                    onClick={handleVideoClick}
                    index={idx}
                  />
                ))}
              </div>

              <div ref={listEndRef} style={{ height: 1 }} />

              {loadingMore && (
                <div style={{ display: 'flex', justifyContent: 'center', padding: '14px 0' }}>
                  <div style={{
                    width: 26, height: 26, borderRadius: '50%',
                    border: '2px solid rgba(255,255,255,0.08)',
                    borderTopColor: '#7c3aed',
                    animation: 'xf-spin 0.8s linear infinite',
                  }} />
                </div>
              )}

              {!hasMore && videos.length > 0 && (
                <div style={{ textAlign: 'center', color: '#333', fontSize: 11, padding: '14px 0' }}>
                  共 {total} 个作品
                </div>
              )}

              {videos.length === 0 && (
                <div style={{ textAlign: 'center', color: '#444', padding: '60px 0', fontSize: 14 }}>
                  暂无作品
                </div>
              )}
            </>
          )}
        </div>
      </div>

      <style>{`
        @keyframes xf-spin { to { transform: rotate(360deg); } }
        .xfthumb:hover .xfthumb-ov { opacity: 1 !important; }
      `}</style>
    </div>,
    document.body
  );
};

/* ── 统计项 ── */
const StatItem = ({ value, label }) => (
  <div style={{ textAlign: 'center' }}>
    <div style={{ color: '#a78bfa', fontSize: 15, fontWeight: 700, lineHeight: 1.2 }}>{value}</div>
    <div style={{ color: '#555', fontSize: 11, marginTop: 2 }}>{label}</div>
  </div>
);

/* ── 视频缩略图卡片（memo：只有 isActive 变化时才重渲染）── */
const VideoThumb = memo(({ id, video, isActive, onClick, index }) => {
  const poster = video.posterUrl || video.thumbnailUrl;
  return (
    <div
      id={id}
      className="xfthumb"
      onClick={() => onClick(video, index)}
      style={{
        position: 'relative',
        aspectRatio: '9/14',
        borderRadius: 5,
        overflow: 'hidden',
        cursor: 'pointer',
        background: '#1a1a1a',
        border: isActive ? '2px solid #a855f7' : '2px solid transparent',
        transition: 'border-color 0.18s, box-shadow 0.18s, transform 0.15s',
        transform: isActive ? 'scale(1.03)' : 'scale(1)',
        boxShadow: isActive ? '0 0 14px rgba(168,85,247,0.55)' : 'none',
      }}
    >
      <SecureDecryptedImage
        src={poster}
        alt={video.title}
        className="absolute inset-0 w-full h-full"
        imageStyle={{ objectFit: 'cover' }}
        lazyLoad={true}
        showLoadingIndicator={false}
      />

      <div
        className="xfthumb-ov"
        style={{
          position: 'absolute', inset: 0,
          background: 'linear-gradient(to top, rgba(0,0,0,0.88) 0%, rgba(0,0,0,0.05) 55%, transparent 100%)',
          opacity: isActive ? 1 : 0,
          transition: 'opacity 0.18s',
          display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', padding: 5,
        }}
      >
        {video.views > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
            <svg width="9" height="9" viewBox="0 0 24 24" fill="white" opacity="0.75">
              <polygon points="5 3 19 12 5 21 5 3" />
            </svg>
            <span style={{ color: 'rgba(255,255,255,0.75)', fontSize: 9 }}>
              {formatNumber(video.views)}
            </span>
          </div>
        )}
      </div>

      {isActive && (
        <div style={{
          position: 'absolute', top: 5, right: 5,
          width: 18, height: 18, borderRadius: '50%',
          background: 'rgba(168,85,247,0.92)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="8" height="8" viewBox="0 0 24 24" fill="white">
            <polygon points="5 3 19 12 5 21 5 3" />
          </svg>
        </div>
      )}
    </div>
  );
});
VideoThumb.displayName = 'VideoThumb';

export default UserXfreeDetail;
