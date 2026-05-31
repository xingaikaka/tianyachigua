import React, { useRef, useState, useEffect, useMemo, useCallback, memo } from 'react';
import { Swiper, SwiperSlide } from 'swiper/react';
import { Mousewheel, Keyboard, Virtual } from 'swiper/modules';
import { FiShare2, FiVolume2, FiVolumeX } from 'react-icons/fi';
import 'swiper/css';
import TikTokIcon from '../common/TikTokIcon';
import TikTokLoading from '../common/TikTokLoading';
import useShortVideoPlayerManager from '../../hooks/useShortVideoPlayerManager';
import videoStatsService from '../../services/videoStatsService';
import blobImageDecryption from '../../utils/blobImageDecryption';
import imageBlobCache from '../../utils/imageBlobCache';

// clip-path 方案：彻底遮住 iOS Safari GPU 合成层，避免 opacity/z-index 失效导致视频闪烁
const CLIP_HIDDEN = 'inset(100%)';
const CLIP_VISIBLE = 'inset(0%)';
const COVER_RANGE = 3; // 预解密封面范围，扩大以覆盖快速滑动

function getCoverSrc(item) {
  return item?.posterUrl || item?.thumbnailUrl || item?.coverUrl || item?.thumbUrl || null;
}

// 提取为静态常量，避免每次渲染重新创建字符串
const SWIPER_STYLES = `
  .user-video-swiper {
    width: 100% !important;
    height: 100vh !important;
  }
  @supports (height: 100dvh) {
    .user-video-swiper { height: 100dvh !important; }
  }
  @supports (height: -webkit-fill-available) {
    .user-video-swiper { min-height: -webkit-fill-available !important; }
  }
  .user-video-viewport-fixed .user-video-swiper { height: 100% !important; min-height: 100% !important; }
  .user-video-swiper .swiper-wrapper {
    height: 100% !important;
    transition-timing-function: cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
  }
  .user-video-swiper .swiper-slide {
    height: 100% !important;
    width: 100% !important;
    overflow: hidden !important;
  }
  .user-video-el::-webkit-media-controls { display: none !important; opacity: 0 !important; }
  .user-video-el::-webkit-media-controls-panel { display: none !important; opacity: 0 !important; }
  .user-video-el::-webkit-media-controls-enclosure { display: none !important; opacity: 0 !important; }
  .user-video-el::-moz-media-controls { display: none !important; opacity: 0 !important; }
`;

// 与短视频移动端一致：仅按钮本身有模糊玻璃样式，无背景条，视频全屏可见
const SHORT_VIDEO_BUTTON_STYLE = {
  width: '40px',
  height: '40px',
  backgroundColor: 'rgba(255, 255, 255, 0.15)',
  backdropFilter: 'blur(20px) saturate(180%)',
  WebkitBackdropFilter: 'blur(20px) saturate(180%)',
  border: '1px solid rgba(255, 255, 255, 0.3)',
  boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center'
};

// ── VideoSlide：每个视频 slide 的渲染单元 ─────────────────────────────────────
// 使用 React.memo + 自定义比较函数，仅在当前 slide 自身数据变化时重渲染，
// 避免因父组件 playingIdx 等状态变化导致所有 slide 全量重渲染。
// 进度条通过 registerProgressBarRef 由 hook 内 rAF 直接 imperative 更新 DOM，
// 不参与 React 渲染链路，零延迟、零节流。
const VideoSlide = memo(({
  item, index, isMuted,
  isCurrentSlide, shouldRevealVideo,
  videoReady, videoPaused, videoDelayed,
  coverBlobUrl,
  isLastSlide, isLoadingMore,
  userName,
  registerVideoRef, registerProgressBarRef,
  handleClick, handleLoadStart, handleLoadedMetadata,
  handleCanPlayEvent, handlePlayingEvent, handlePause, handlePlay,
  handleWaiting, handleError,
}) => (
  <div className="relative w-full h-full bg-black overflow-hidden">
    {/* 视频容器：clip-path 隐藏未就绪帧 */}
    <div
      style={{
        position: 'absolute', inset: 0, zIndex: 1, overflow: 'hidden',
        clipPath: shouldRevealVideo ? CLIP_VISIBLE : CLIP_HIDDEN,
        WebkitClipPath: shouldRevealVideo ? CLIP_VISIBLE : CLIP_HIDDEN,
      }}
    >
      <video
        ref={el => registerVideoRef(index, el)}
        className="user-video-el"
        style={{ position: 'absolute', left: 0, right: 0, top: 0, bottom: 0, width: '100%', height: '100%', objectFit: 'cover', background: '#000' }}
        playsInline muted={isMuted} loop controls={false}
        preload="none" controlsList="nofullscreen noplaybackrate"
        disablePictureInPicture
        onContextMenu={e => e.preventDefault()}
        onClick={() => handleClick(index)}
        onLoadStart={() => handleLoadStart(index)}
        onLoadedMetadata={() => handleLoadedMetadata(index)}
        onCanPlay={() => handleCanPlayEvent(index)}
        onPlaying={() => handlePlayingEvent(index)}
        onPause={() => handlePause(index)}
        onPlay={() => handlePlay(index)}
        onWaiting={() => handleWaiting(index)}
        onError={() => handleError(index)}
      />
    </div>

    {/* 封面遮罩：播放前显示，播放后淡出 */}
    <div
      className="pointer-events-none"
      style={{
        position: 'absolute', inset: 0, zIndex: 300, background: '#000',
        opacity: shouldRevealVideo ? 0 : 1,
        transition: shouldRevealVideo ? 'opacity 0.12s ease-out' : 'none',
      }}
    >
      {coverBlobUrl && (
        <img src={coverBlobUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
      )}
    </div>

    {/* 加载指示器 */}
    {videoDelayed && (
      <div className="pointer-events-none" style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 900 }}>
        <TikTokLoading size={48} dark={false} />
      </div>
    )}

    {/* 暂停图标：仅当前活动 slide 显示 */}
    {isCurrentSlide && videoPaused && videoReady && !videoDelayed && (
      <div className="pointer-events-none" style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 950 }}>
        <div style={{ width: 60, height: 60, backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(10px)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '2px solid rgba(255,255,255,0.3)', boxShadow: '0 4px 20px rgba(0,0,0,0.3)' }}>
          <TikTokIcon size={28} color="#fff" opacity={0.9} />
        </div>
      </div>
    )}

    {/* 底部信息：进度条 + 作者名 */}
    <div
      className="absolute bottom-0 left-0 right-0 p-4 pb-safe"
      style={{ background: 'linear-gradient(to top, rgba(0,0,0,0.8), transparent)', zIndex: 1000, pointerEvents: 'none' }}
    >
      {isCurrentSlide && (
        <div className="mb-3 h-[6px] rounded-full overflow-hidden shadow-[0_0_6px_rgba(0,0,0,0.25)]" style={{ background: 'rgba(255,255,255,0.25)' }}>
          {/* 进度条由 hook 通过 ref imperative 更新 transform: scaleX(p)，
              transformOrigin 设为左侧，初始 scaleX(0) 不可见 */}
          <div
            ref={node => registerProgressBarRef(index, node)}
            className="h-full bg-white"
            style={{
              width: '100%',
              transformOrigin: 'left center',
              transform: 'scaleX(0)',
              willChange: 'transform',
            }}
          />
        </div>
      )}
      <div className="flex items-center justify-between">
        <span className="text-white font-bold">@{userName}</span>
      </div>
    </div>

    {/* 加载更多提示（最后一个视频） */}
    {isLastSlide && isLoadingMore && (
      <div className="absolute bottom-20 left-1/2 -translate-x-1/2 pointer-events-none" style={{ zIndex: 1001 }}>
        <div className="bg-black/70 backdrop-blur-sm px-4 py-2 rounded-full">
          <div className="flex items-center gap-2 text-white text-sm">
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            <span>加载更多视频...</span>
          </div>
        </div>
      </div>
    )}
  </div>
), (prev, next) => (
  // 自定义比较：只有以下 props 变化才重渲染
  prev.shouldRevealVideo === next.shouldRevealVideo &&
  prev.isCurrentSlide    === next.isCurrentSlide    &&
  prev.videoReady        === next.videoReady        &&
  prev.videoPaused       === next.videoPaused       &&
  prev.videoDelayed      === next.videoDelayed      &&
  prev.coverBlobUrl      === next.coverBlobUrl      &&
  prev.isMuted           === next.isMuted           &&
  prev.isLoadingMore     === next.isLoadingMore
  // item / index / handlers 均为稳定引用，无需比较
));

/**
 * 移动端全屏视频播放器
 * 支持上下滑动切换视频，TikTok 风格
 * 播放逻辑与短视频分类移动端（ShortVideoMobilePlayer）保持一致，共用 useShortVideoPlayerManager
 */
const MobileFullScreenPlayer = ({ 
  videos = [], 
  initialIndex = 0, 
  onClose,
  userName,
  onLoadMore,
  hasMore = false,
  isLoadingMore = false,
  onVideoChange,
}) => {
  const [activeIndex, setActiveIndex] = useState(initialIndex);
  const activeIndexRef = useRef(initialIndex);
  const [isMuted, setIsMuted] = useState(false);
  // 移动端视口高度：解决部分手机 100vh 底部留空（浏览器地址栏/导航栏导致）
  const [viewportH, setViewportH] = useState(() =>
    (typeof window !== 'undefined' && window.visualViewport?.height) || window.innerHeight || 0
  );
  const [playingIdx, setPlayingIdx] = useState(-1); // 真正开始播放后才 reveal 视频，避免非就绪帧闪现
  const [coverBlobUrlMap, setCoverBlobUrlMap] = useState({});
  const revealTimerRef = useRef(null);
  const shareTipTimerRef = useRef(null); // 分享提示 DOM 清理 timer
  const initialCoversDecryptedRef = useRef(false); // 初始封面预解密只执行一次
  const isSafariRef = useRef(
    typeof navigator !== 'undefined' &&
    /Safari/i.test(navigator.userAgent) &&
    !/Chrome|CriOS|Edg|EdgiOS|Android/i.test(navigator.userAgent)
  );

  // 将 videos 转换为 hook 期望的格式（firstVideoUrl），保持原始字段不变
  const items = useMemo(() => videos.map(video => ({
    ...video,
    firstVideoUrl: video.hdUrl || video.sdUrl,
    type: 'video',
  })), [videos]);

  // items / videos 最新引用，供 effect 内读取，避免将 items/videos 列为依赖
  const itemsRef2 = useRef(items);
  itemsRef2.current = items;
  const videosRef = useRef(videos);
  videosRef.current = videos;

  // 主动预解密封面：在 activeIndex ± COVER_RANGE 范围内提前解密，滑动时封面即时可用
  // 通过 itemsRef2 读取最新 items，避免将 items 列为依赖（items 随每次 videos 追加而重建，
  // 导致此 effect 在加载新页时全量重跑所有封面解密）
  useEffect(() => {
    const its = itemsRef2.current;
    if (!its || its.length === 0) return;
    const start = Math.max(0, activeIndex - COVER_RANGE);
    const end   = Math.min(its.length - 1, activeIndex + COVER_RANGE);

    // ── 1. 裁剪超出范围的旧条目，释放 React state 占用 ──────────────────────
    setCoverBlobUrlMap(prev => {
      let changed = false;
      const next = {};
      Object.keys(prev).forEach(k => {
        const i = Number(k);
        if (i >= start && i <= end) {
          next[i] = prev[i];
        } else {
          changed = true;
          // blob URL 底层内存由 imageBlobCache LRU 驱逐时统一 revoke
        }
      });
      return changed ? next : prev;
    });

    // ── 2. 解密窗口内封面 ────────────────────────────────────────────────────
    // 首次进入时额外预解密前 5 条（仅一次），后续只解密滑动窗口内封面
    const initialEnd = initialCoversDecryptedRef.current
      ? -1
      : Math.min(its.length - 1, 4);
    if (!initialCoversDecryptedRef.current) initialCoversDecryptedRef.current = true;
    const decryptEnd = Math.max(end, initialEnd);
    for (let i = start; i <= decryptEnd; i++) {
      const item = its[i];
      if (!item) continue;
      const src = getCoverSrc(item);
      if (!src) continue;
      const cached = imageBlobCache.get(src);
      if (cached) {
        setCoverBlobUrlMap(prev => prev[i] === cached ? prev : { ...prev, [i]: cached });
        continue;
      }
      const iCopy = i;
      const srcCopy = src;
      blobImageDecryption.decryptImageToBlob(srcCopy, {
        priority: Math.abs(i - activeIndex) <= 2 ? 'high' : 'normal',
      }).then(url => {
        if (!url) return;
        try { imageBlobCache.set(srcCopy, url); } catch (_) {}
        setCoverBlobUrlMap(prev => prev[iCopy] === url ? prev : { ...prev, [iCopy]: url });
      }).catch(() => {});
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeIndex]); // 依赖仅 activeIndex；items 通过 itemsRef2 读取，无需列入依赖

  // ── 播放管理 hook（与短视频分类移动端完全一致的参数）──
  const {
    registerProgressBarRef,
    getVideoState: getVS,
    ensureVideoState: ensureVS,
    playAt,
    registerVideoRef,
    handleLoadStart,
    handleLoadedMetadata,
    handleCanPlay,
    handlePlaying,
    handlePause,
    handlePlay,
    handleWaiting,
    handleError,
    handleClick,
    stopProgressLoop,
    syncMutedStateForIndex,
    cleanupPlayer,
  } = useShortVideoPlayerManager({
    items,
    isMuted,
    activeIndexRef,
    autoPlayDelayMs: 60,
    farRecycleDistance: 1,
    quickSwitchThresholdMs: 300,
    loadingIndicatorDelayMs: 800,
    prefetchNextDelayMs: 150
  });

  // 用 ref 存储函数引用，避免 useEffect 闭包过期
  const playAtRef = useRef(playAt);
  const stopProgressLoopRef = useRef(stopProgressLoop);
  useEffect(() => {
    playAtRef.current = playAt;
    stopProgressLoopRef.current = stopProgressLoop;
  }, [playAt, stopProgressLoop]);

  // 用 ref 读取最新的 hasMore / isLoadingMore / onLoadMore，
  // 避免将它们列入 activeIndex useEffect 依赖，防止加载更多时重置 playingIdx
  const hasMoreRef = useRef(hasMore);
  const isLoadingMoreRef = useRef(isLoadingMore);
  const onLoadMoreRef = useRef(onLoadMore);
  hasMoreRef.current = hasMore;
  isLoadingMoreRef.current = isLoadingMore;
  onLoadMoreRef.current = onLoadMore;

  // 监听 visualViewport 变化，解决移动端浏览器 UI 显示/隐藏时的底部留空
  useEffect(() => {
    const updateH = () => {
      try {
        const h = window.visualViewport?.height ?? window.innerHeight;
        if (h && h > 0) setViewportH(h);
      } catch (_) {}
    };
    updateH();
    window.visualViewport?.addEventListener('resize', updateH);
    window.visualViewport?.addEventListener('scroll', updateH);
    return () => {
      window.visualViewport?.removeEventListener('resize', updateH);
      window.visualViewport?.removeEventListener('scroll', updateH);
    };
  }, []);

  // 注：全屏 class（user-group-fullscreen-open）由父组件 UserVideoPlayer 通过
  // useLayoutEffect 在首次绘制前统一管理，此处不再重复添加，避免双重触发。

  // 组件卸载时清理所有播放器资源及 DOM 定时器
  useEffect(() => {
    return () => {
      cleanupPlayer();
      if (revealTimerRef.current) {
        clearTimeout(revealTimerRef.current);
        revealTimerRef.current = null;
      }
      if (shareTipTimerRef.current) {
        clearTimeout(shareTipTimerRef.current);
        shareTipTimerRef.current = null;
      }
    };
  }, [cleanupPlayer]);

  // items 列表长度变化时（新一页追加）仅初始化新增条目的视频状态
  const lastItemsLengthRef = useRef(0);
  useEffect(() => {
    const its = itemsRef2.current;
    if (!its || its.length === 0) return;
    if (its.length !== lastItemsLengthRef.current) {
      // 只对新增的条目调用 ensureVS，避免对已有条目重复初始化
      const prevLen = lastItemsLengthRef.current;
      lastItemsLengthRef.current = its.length;
      for (let idx = prevLen; idx < its.length; idx++) ensureVS(idx);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items.length, ensureVS]);

  // 用户行为埋点：切到不同视频时上报 redgifs_view + redgifs_play
  const trackedIdsRef = useRef(new Set());
  useEffect(() => {
    if (items.length === 0) return;
    if (activeIndex < 0 || activeIndex >= items.length) return;
    const it = items[activeIndex];
    const vid = it?.id;
    if (!vid) return;
    const key = String(vid);
    if (trackedIdsRef.current.has(key)) return;
    trackedIdsRef.current.add(key);
    try { videoStatsService.trackRedgifsView(vid, { username: userName }); } catch (_) {}
    try { videoStatsService.trackRedgifsPlay(vid, { username: userName }); } catch (_) {}
  }, [activeIndex, items, userName]);

  // activeIndex 变化时触发播放
  const playAtTimerRef = useRef(null);
  useEffect(() => {
    if (items.length === 0) return;
    if (activeIndex < 0 || activeIndex >= items.length) return;

    if (revealTimerRef.current) {
      clearTimeout(revealTimerRef.current);
      revealTimerRef.current = null;
    }
    setPlayingIdx(-1);

    if (playAtTimerRef.current) {
      cancelAnimationFrame(playAtTimerRef.current);
      playAtTimerRef.current = null;
    }

    stopProgressLoopRef.current();

    playAtTimerRef.current = requestAnimationFrame(() => {
      if (activeIndexRef.current === activeIndex) {
        playAtRef.current(activeIndex);
      }
      playAtTimerRef.current = null;
    });

    // 临近末尾时自动加载更多（通过 ref 读取，避免将 hasMore/isLoadingMore 列为依赖）
    if (hasMoreRef.current && !isLoadingMoreRef.current && onLoadMoreRef.current) {
      const remaining = videosRef.current.length - activeIndex;
      if (remaining <= 2) {
        onLoadMoreRef.current();
      }
    }

    return () => {
      if (playAtTimerRef.current) {
        cancelAnimationFrame(playAtTimerRef.current);
        playAtTimerRef.current = null;
      }
    };
  // 仅依赖 activeIndex：hasMore/isLoadingMore 通过 ref 读取，
  // 避免它们变化时重跑此 effect 并执行 setPlayingIdx(-1) 导致正在播放的视频闪烁
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeIndex]);

  // 静音状态变化时同步到当前视频
  useEffect(() => {
    syncMutedStateForIndex(activeIndex);
  }, [activeIndex, isMuted, syncMutedStateForIndex]);

  // Swiper 切换事件（useCallback 避免 Swiper 因函数引用变化触发不必要更新）
  const handleSlideChange = useCallback((swiper) => {
    const newIndex = swiper.activeIndex;
    activeIndexRef.current = newIndex;
    stopProgressLoopRef.current();
    setActiveIndex(newIndex);
    const vid = itemsRef2.current[newIndex]?.id;
    if (vid && onVideoChange) onVideoChange(vid);
  }, [onVideoChange]);

  // 切换静音
  const toggleMute = useCallback(() => {
    setIsMuted(prev => !prev);
  }, []);

  // reveal 视频统一入口：playing/canplay 任一事件触发即立即揭示，
  // 不再硬等 80/260ms。playing 事件已保证 video 元素绘制了首帧；
  // canplay 事件保证 readyState ≥ HAVE_FUTURE_DATA，揭示即可见首帧。
  // setTimeout 0 让出主线程给浏览器完成 video 合成层提交，
  // Safari 上多给 40ms 规避 GPU 合成层闪烁的偶发问题。
  const revealVideoSlide = useCallback((index) => {
    if (index !== activeIndexRef.current) return;
    if (revealTimerRef.current) {
      clearTimeout(revealTimerRef.current);
      revealTimerRef.current = null;
    }
    revealTimerRef.current = setTimeout(() => {
      if (index === activeIndexRef.current) setPlayingIdx(index);
      revealTimerRef.current = null;
    }, isSafariRef.current ? 40 : 0);
  }, [activeIndexRef, isSafariRef]);

  // playing 事件：视频已真正开始播放，更新内部状态 + 立即揭示
  const handlePlayingEvent = useCallback((index) => {
    handlePlaying(index);
    revealVideoSlide(index);
  }, [handlePlaying, revealVideoSlide]);

  // canplay 事件：比 playing 早 ~20-50ms 触发，提前揭示视频以缩短封面占屏时间
  const handleCanPlayEvent = useCallback((index) => {
    handleCanPlay(index);
    revealVideoSlide(index);
  }, [handleCanPlay, revealVideoSlide]);

  // 分享当前视频
  const handleShare = useCallback(async () => {
    const currentVideoId = itemsRef2.current[activeIndexRef.current]?.id;
    if (currentVideoId) {
      try { videoStatsService.trackRedgifsShare(currentVideoId, { username: userName }); } catch (_) {}
    }
    const url = userName && currentVideoId
      ? `${window.location.origin}/user/${userName}/video/${currentVideoId}`
      : window.location.href;
    try {
      if (navigator.share) {
        await navigator.share({ url });
        return;
      }
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
      const tip = document.createElement('div');
      tip.textContent = '链接已复制';
      tip.style.cssText = 'position:fixed;bottom:80px;left:50%;transform:translateX(-50%);background:rgba(0,0,0,0.8);color:#fff;padding:8px 20px;border-radius:20px;font-size:13px;z-index:999999;pointer-events:none;';
      document.body.appendChild(tip);
      // 用 ref 追踪 timer，组件卸载时可安全清理
      if (shareTipTimerRef.current) clearTimeout(shareTipTimerRef.current);
      shareTipTimerRef.current = setTimeout(() => {
        if (document.body.contains(tip)) document.body.removeChild(tip);
        shareTipTimerRef.current = null;
      }, 2000);
    } catch (e) {
      console.error('Share failed:', e);
    }
  }, [userName]);

  return (
    <>
    <style>{SWIPER_STYLES}</style>
    <div
      className={`fixed top-0 right-0 bottom-0 left-0 z-[99999] bg-black ${viewportH > 0 ? 'user-video-viewport-fixed' : ''}`}
      style={viewportH > 0 ? { height: `${viewportH}px`, minHeight: `${viewportH}px` } : undefined}
    >

      {/* 顶部按钮：与短视频移动端一致，仅浮动圆形按钮，无背景条，视频全屏可见 */}
      <button
        onClick={(e) => { e.stopPropagation(); onClose?.(); }}
        className="fixed top-4 left-4 z-[100000] text-white p-2 rounded-full transition-all duration-200 hover:scale-110"
        style={SHORT_VIDEO_BUTTON_STYLE}
        aria-label="返回"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      {/* 右侧按钮组 */}
      <div className="fixed top-4 right-4 z-[100000] flex items-center gap-2">
        <button
          onClick={(e) => { e.stopPropagation(); toggleMute(); }}
          className="text-white p-2 rounded-full transition-all duration-200 hover:scale-110"
          style={SHORT_VIDEO_BUTTON_STYLE}
          aria-label={isMuted ? '开启声音' : '关闭声音'}
        >
          {isMuted ? <FiVolumeX size={20} /> : <FiVolume2 size={20} />}
        </button>
        <button
          onClick={(e) => { e.stopPropagation(); handleShare(); }}
          className="text-white p-2 rounded-full transition-all duration-200 hover:scale-110"
          style={SHORT_VIDEO_BUTTON_STYLE}
          aria-label="分享"
        >
          <FiShare2 size={18} />
        </button>
      </div>

      {/* Swiper 容器 */}
      <Swiper

        direction="vertical"
        slidesPerView={1}
        spaceBetween={0}
        speed={250}
        threshold={5}
        touchRatio={1.15}
        longSwipesRatio={0.18}
        longSwipesMs={180}
        followFinger={true}
        mousewheel={{ thresholdDelta: 12, sensitivity: 1.2 }}
        keyboard={true}
        modules={[Mousewheel, Keyboard, Virtual]}
        virtual={{ addSlidesBefore: 1, addSlidesAfter: 1 }}
        initialSlide={initialIndex}
        onSlideChange={handleSlideChange}
        className="user-video-swiper"
      >
        {items.map((item, index) => {
          const vs = getVS(index);
          const isCurrentSlide = index === activeIndex;
          return (
            <SwiperSlide key={item.id || index} virtualIndex={index}>
              <VideoSlide
                item={item}
                index={index}
                isMuted={isMuted}
                isCurrentSlide={isCurrentSlide}
                shouldRevealVideo={isCurrentSlide && index === playingIdx}
                videoReady={vs.ready   ?? false}
                videoPaused={vs.paused ?? false}
                videoDelayed={vs.delayed ?? false}
                coverBlobUrl={coverBlobUrlMap[index] || null}
                isLastSlide={index === videos.length - 1}
                isLoadingMore={isLoadingMore}
                userName={userName}
                registerVideoRef={registerVideoRef}
                registerProgressBarRef={registerProgressBarRef}
                handleClick={handleClick}
                handleLoadStart={handleLoadStart}
                handleLoadedMetadata={handleLoadedMetadata}
                handleCanPlayEvent={handleCanPlayEvent}
                handlePlayingEvent={handlePlayingEvent}
                handlePause={handlePause}
                handlePlay={handlePlay}
                handleWaiting={handleWaiting}
                handleError={handleError}
              />
            </SwiperSlide>
          );
        })}
      </Swiper>
    </div>
    </>
  );
};

export default MobileFullScreenPlayer;
