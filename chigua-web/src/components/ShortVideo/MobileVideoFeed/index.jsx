/**
 * MobileVideoFeed — 移动端短视频全屏播放器
 *
 * Safari iOS <video> 被提升为 GPU 硬件合成层，无视 z-index / opacity。
 * 用 clip-path 控制 GPU 层可见性（CLIP_HIDDEN / CLIP_VISIBLE），封面层覆盖其上。
 *
 * 时序设计（防止黑帧穿透 + 封面不在动画中提前淡出）：
 *   onPlaying 触发时：
 *     - 若 Swiper 动画未结束 → 记录 pendingRevealIdxRef，等 transitionEnd 后再触发
 *     - 若动画已结束 → 立即开始封面淡出（0.4s）
 *   封面淡出完成后 420ms → clip-path 打开，视频显现
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Swiper, SwiperSlide } from 'swiper/react';
import { Mousewheel, Keyboard, Virtual } from 'swiper/modules';
import { FiMenu, FiShare2, FiVolume2, FiVolumeX } from 'react-icons/fi';
import 'swiper/css';
import Hls from 'hls.js';
import TikTokLoading from '../../common/TikTokLoading';
import blobImageDecryption from '../../../utils/blobImageDecryption';
import imageBlobCache from '../../../utils/imageBlobCache';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../../../utils/hlsUtils';

// ─── 常量 ────────────────────────────────────────────────────────────────────

const KEEP_RANGE      = 1;    // 保留 activeIndex ±1 的视频，超出释放
const PRELOAD_PREV_MS = 300;  // 预加载上一个的延迟（非 Safari）
const COVER_RANGE     = 3;    // 封面渲染范围 ±3

const IS_SAFARI =
  typeof navigator !== 'undefined' &&
  /Safari/i.test(navigator.userAgent) &&
  !/Chrome|CriOS|Edg|EdgiOS|Android/i.test(navigator.userAgent);

// clip-path 施加在视频的父 wrapper div 上（而非 video 元素本身）。
// video GPU 合成层始终在 wrapper 内全尺寸渲染，clip-path 仅控制 wrapper 可见区域。
// reveal 时 wrapper 直接展开，GPU 层不需要重建 → 不产生原始尺寸闪烁。
const CLIP_HIDDEN  = 'inset(0 0 100% 0)';
const CLIP_VISIBLE = 'inset(0px)';

// 毛玻璃按钮基础样式
const GLASS_BTN = {
  width: 40, height: 40, borderRadius: '50%',
  background: 'rgba(255,255,255,0.15)',           // 单用 background，避免与 backgroundColor 冲突
  backdropFilter: 'blur(20px) saturate(180%)',
  WebkitBackdropFilter: 'blur(20px) saturate(180%)',
  border: '1px solid rgba(255,255,255,0.3)',
  boxShadow: '0 8px 32px rgba(0,0,0,0.1), inset 0 1px 0 rgba(255,255,255,0.2)',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  color: '#fff', cursor: 'pointer', flexShrink: 0,
  padding: 0, outline: 'none',
};

function getThumbUrl(video) {
  return video?.coverImageUrl || video?.posterUrl || video?.thumbnailUrl || video?.coverUrl || video?.thumbUrl || null;
}

// ─── 子组件 ──────────────────────────────────────────────────────────────────

function VideoStateIcon({ videoEl }) {
  const [paused, setPaused] = useState(videoEl?.paused ?? false);
  useEffect(() => {
    if (!videoEl) return;
    const onPause  = () => setPaused(true);
    const onResume = () => setPaused(false);
    videoEl.addEventListener('pause',   onPause);
    videoEl.addEventListener('play',    onResume);
    videoEl.addEventListener('playing', onResume);
    setPaused(videoEl.paused);
    return () => {
      videoEl.removeEventListener('pause',   onPause);
      videoEl.removeEventListener('play',    onResume);
      videoEl.removeEventListener('playing', onResume);
    };
  }, [videoEl]);

  if (!paused) return null;
  return (
    <div style={{
      position: 'absolute', inset: 0, zIndex: 15,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      pointerEvents: 'none',
    }}>
      <div style={{
        width: 64, height: 64, borderRadius: '50%',
        background: 'rgba(0,0,0,0.55)',
        backdropFilter: 'blur(10px)', WebkitBackdropFilter: 'blur(10px)',
        border: '2px solid rgba(255,255,255,0.25)',
        boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <svg width="26" height="26" viewBox="0 0 24 24" fill="rgba(255,255,255,0.92)">
          <polygon points="5,3 19,12 5,21" />
        </svg>
      </div>
    </div>
  );
}

function ProgressBar({ videoEl }) {
  const [pct, setPct] = useState(0);
  useEffect(() => {
    if (!videoEl) return;
    let raf = 0;
    const onTime = () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => {
        if (videoEl.duration > 0) setPct(Math.min(100, (videoEl.currentTime / videoEl.duration) * 100));
      });
    };
    videoEl.addEventListener('timeupdate', onTime, { passive: true });
    return () => { videoEl.removeEventListener('timeupdate', onTime); cancelAnimationFrame(raf); };
  }, [videoEl]);

  if (pct <= 0) return null;
  return (
    <div style={{ height: 3, borderRadius: 9999, marginBottom: 10, background: 'rgba(255,255,255,0.25)', overflow: 'hidden' }}>
      <div style={{ height: '100%', background: '#fff', width: `${pct}%`, transition: 'width 0.12s linear' }} />
    </div>
  );
}

function InlineSpinner() {
  return (
    <>
      <style>{`@keyframes mvf-spin{to{transform:rotate(360deg)}}`}</style>
      <div style={{
        width: 14, height: 14, flexShrink: 0,
        border: '2px solid rgba(255,255,255,0.3)',
        borderTopColor: '#fff', borderRadius: '50%',
        animation: 'mvf-spin 0.9s linear infinite',
      }} />
    </>
  );
}

// ─── 主组件 ──────────────────────────────────────────────────────────────────

const MobileVideoFeed = ({
  videos = [],
  initialIndex = 0,
  onClose,
  onLoadMore,
  hasMore = false,
  isLoadingMore = false,
}) => {

  const [vph, setVph] = useState(() =>
    typeof window !== 'undefined' ? window.innerHeight : 812
  );

  const [activeIndex, setActiveIndex] = useState(initialIndex);
  const activeIdxRef = useRef(initialIndex);

  const [isMuted, setIsMuted] = useState(false);
  const isMutedRef = useRef(false);

  const [states, setStates] = useState({});
  const statesRef = useRef({});

  const setState = useCallback((idx, s) => {
    if (statesRef.current[idx] === s) return;
    statesRef.current[idx] = s;
    setStates(prev => ({ ...prev, [idx]: s }));
  }, []);

  // 封面淡出状态
  const [playingIdx, setPlayingIdx] = useState(-1);
  // clip-path 打开状态（比封面淡出晚 420ms，确保封面彻底消失后视频再显现）
  const [clipVisibleMap, setClipVisibleMap] = useState({});

  // 父组件集中管理封面 blobUrl（避免子组件各自排队解密、命中缓存延迟等问题）
  const [coverBlobUrlMap, setCoverBlobUrlMap] = useState({});

  // videos 的最新引用（用 ref 避免在回调/effect 中产生过时闭包）
  const videosRef = useRef(videos);
  videosRef.current = videos;

  // 定时器 refs
  const revealTimerRef = useRef(null);
  const clipTimerRef   = useRef(null);

  // Swiper 动画状态 refs（防止动画进行中封面提前淡出）
  const transitionDoneRef    = useRef(true);  // true = 动画已完成
  const pendingRevealIdxRef  = useRef(null);  // 动画中触发的 onPlaying 暂存

  const videoRefs       = useRef({});
  const hlsMap          = useRef({});
  const nativeBlobUrls  = useRef({}); // 旧版 iOS 原生 HLS：重写 m3u8 产生的 Blob URL，释放时需 revoke

  // ── 工具函数 ────────────────────────────────────────────────────────────────

  const getUrl = useCallback((idx) => {
    const d = videosRef.current[idx];
    return d?.firstVideoUrl || d?.hdUrl || d?.sdUrl || '';
  }, []); // 通过 videosRef 读取最新 videos，不需要将 videos 列为依赖

  const destroyHls = useCallback((idx) => {
    const hls = hlsMap.current[idx];
    if (!hls) return;
    try { hls.stopLoad(); } catch (_) {}
    try { hls.destroy(); } catch (_) {}
    delete hlsMap.current[idx];
  }, []);

  const releaseVideo = useCallback((idx) => {
    destroyHls(idx);
    // 清理原生 HLS 的 Blob URL（如有）
    if (nativeBlobUrls.current[idx]) {
      try { URL.revokeObjectURL(nativeBlobUrls.current[idx]); } catch (_) {}
      delete nativeBlobUrls.current[idx];
    }
    const v = videoRefs.current[idx];
    if (!v) return;
    try { v.pause(); } catch (_) {}
    try { v.removeAttribute('src'); v.load(); } catch (_) {}
    setState(idx, 'idle');
  }, [destroyHls, setState]);

  const safePlay = useCallback(async (v) => {
    if (!v) return;
    v.muted = isMutedRef.current;
    try { await v.play(); } catch (_) {}
  }, []);

  const loadVideo = useCallback((idx, autoPlay) => {
    const v = videoRefs.current[idx];
    if (!v) return;
    const url = getUrl(idx);
    if (!url) return;

    destroyHls(idx);
    setState(idx, 'loading');

    const isM3u8 = /\.m3u8(\?|$)/i.test(url);

    if (isM3u8 && Hls.isSupported()) {
      // 优先使用 hls.js（适用于 Android Chrome、iOS 17+ Safari 等所有支持 MSE 的环境）
      // xhrSetup 拦截 key 请求并重定向到后端 API，确保加密视频正常播放。
      // 注意：即使 IS_SAFARI=true（DevTools 模拟）也应走此路径，原生 HLS 无法处理 key 重定向。
      const hls = new Hls({ enableWorker: true, lowLatencyMode: false, xhrSetup: hlsXhrSetup });
      hlsMap.current[idx] = hls;
      hls.loadSource(url);
      hls.attachMedia(v);
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        if (autoPlay) safePlay(v);
      });
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) setState(idx, 'idle');
      });
    } else if (isM3u8 && v.canPlayType('application/vnd.apple.mpegurl')) {
      // 旧版 iOS Safari（iOS ≤ 16，hls.js 不支持）：原生 HLS 无法使用 xhrSetup，
      // 需先重写 m3u8 中的 key URI 为绝对后端地址，否则浏览器会向 CDN 请求 key 导致 404。
      patchNativeHlsM3u8(url).then(({ url: patchedUrl, isBlob }) => {
        // 异步完成后确认该 video 元素还在使用（避免 src 切换后写入旧元素）
        if (videoRefs.current[idx] !== v) {
          if (isBlob) URL.revokeObjectURL(patchedUrl);
          return;
        }
        // 清理上一次的 Blob URL（如有）
        if (nativeBlobUrls.current[idx]) {
          try { URL.revokeObjectURL(nativeBlobUrls.current[idx]); } catch (_) {}
        }
        if (isBlob) nativeBlobUrls.current[idx] = patchedUrl;
        v.src = patchedUrl;
        v.load();
        if (autoPlay) safePlay(v);
      });
    } else {
      // 普通 mp4 / 无法播放 m3u8 的兜底处理
      v.src = url;
      v.load();
      if (autoPlay) safePlay(v);
    }
  }, [getUrl, destroyHls, setState, safePlay]);

  const playVideo = useCallback((idx) => {
    const v = videoRefs.current[idx];
    if (!v) return;
    const hasSrc = v.getAttribute('src') || v.currentSrc;
    if (!hasSrc) { loadVideo(idx, true); return; }
    safePlay(v);
  }, [loadVideo, safePlay]);

  const pauseVideo = useCallback((idx) => {
    try { videoRefs.current[idx]?.pause(); } catch (_) {}
  }, []);

  // 触发封面淡出 + 延迟打开 clip-path
  const startReveal = useCallback((idx) => {
    if (revealTimerRef.current) { clearTimeout(revealTimerRef.current); revealTimerRef.current = null; }
    revealTimerRef.current = setTimeout(() => {
      revealTimerRef.current = null;
      if (idx !== activeIdxRef.current) return;
      setPlayingIdx(idx);
      if (clipTimerRef.current) clearTimeout(clipTimerRef.current);
      clipTimerRef.current = setTimeout(() => {
        clipTimerRef.current = null;
        if (idx === activeIdxRef.current) {
          setClipVisibleMap(prev => ({ ...prev, [idx]: true }));
        }
      }, 420);
    }, IS_SAFARI ? 80 : 20);
  }, []);

  // ── Swiper 事件 ─────────────────────────────────────────────────────────────

  // touchStart：用户触屏瞬间开始预加载下一个，比 slideChange 更早
  const handleTouchStart = useCallback(() => {
    const nextIdx = activeIdxRef.current + 1;
    if (nextIdx < videosRef.current.length) {
      const v = videoRefs.current[nextIdx];
      if (v && !v.getAttribute('src') && !hlsMap.current[nextIdx]) {
        loadVideo(nextIdx, false);
      }
    }
  }, [loadVideo]); // videosRef 始终最新，不需要 videos.length 作为依赖

  const handleSlideChange = useCallback((swiper) => {
    const next = swiper.activeIndex;
    activeIdxRef.current = next;
    setActiveIndex(next);

    // 标记动画进行中，onPlaying 触发时暂缓 reveal
    transitionDoneRef.current = false;
    pendingRevealIdxRef.current = null;

    // 清理定时器，重置封面/视频状态
    if (revealTimerRef.current) { clearTimeout(revealTimerRef.current); revealTimerRef.current = null; }
    if (clipTimerRef.current) { clearTimeout(clipTimerRef.current); clipTimerRef.current = null; }
    setPlayingIdx(-1);
    setClipVisibleMap({});

    // 合并为一次遍历：释放远端视频 + 暂停近端非活跃视频
    Object.keys(videoRefs.current).forEach(k => {
      const i = Number(k);
      if (Math.abs(i - next) > KEEP_RANGE) {
        releaseVideo(i);
      } else if (i !== next) {
        pauseVideo(i);
      }
    });

    // 立即加载目标视频（利用 Swiper 动画的 380ms 并行缓冲）
    const vNext = videoRefs.current[next];
    if (vNext && !vNext.getAttribute('src') && !hlsMap.current[next]) {
      loadVideo(next, false);
    }

    // 预加载相邻（动画期间并行，互不阻塞）
    const preloadIfNeeded = (i, delay) => {
      if (i >= 0 && i < videosRef.current.length) {
        setTimeout(() => {
          const v = videoRefs.current[i];
          if (!v?.getAttribute('src') && !hlsMap.current[i]) loadVideo(i, false);
        }, delay);
      }
    };
    preloadIfNeeded(next + 1, 80);
    if (!IS_SAFARI) preloadIfNeeded(next - 1, PRELOAD_PREV_MS);

    if (hasMore && !isLoadingMore && onLoadMore && videosRef.current.length - next <= 2) onLoadMore();
  }, [hasMore, isLoadingMore, onLoadMore, loadVideo, pauseVideo, releaseVideo]); // 移除 videos 依赖，用 videosRef 读取

  const handleTransitionEnd = useCallback(() => {
    transitionDoneRef.current = true;
    const idx = activeIdxRef.current;

    // 若 onPlaying 在动画期间触发，动画结束后补触 reveal
    if (pendingRevealIdxRef.current === idx) {
      pendingRevealIdxRef.current = null;
      startReveal(idx);
    }

    // 双 RAF 确保 GPU 布局稳定后再 play（Safari 必要）
    requestAnimationFrame(() => {
      requestAnimationFrame(() => { playVideo(idx); });
    });
  }, [playVideo, startReveal]);

  // ── 生命周期 ─────────────────────────────────────────────────────────────────

  useEffect(() => {
    if (!videos.length) return;
    const t1 = setTimeout(() => loadVideo(initialIndex, true), 80);
    const t2 = setTimeout(() => {
      if (initialIndex + 1 < videos.length) loadVideo(initialIndex + 1, false);
      if (initialIndex - 1 >= 0) loadVideo(initialIndex - 1, false);
    }, 200);
    return () => { clearTimeout(t1); clearTimeout(t2); };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // 批量预解密封面：activeIndex 变化时，主动解密周边 ±COVER_RANGE 张封面
  // 不依赖子组件各自挂载排队，确保滑动时封面已就绪
  // 通过 videosRef 读取最新 videos，去掉 videos 引用依赖，避免每次追加新页都全量重跑此 effect
  useEffect(() => {
    const vids = videosRef.current;
    if (!vids.length) return;
    const start = Math.max(0, activeIndex - COVER_RANGE);
    const end   = Math.min(vids.length - 1, activeIndex + COVER_RANGE);

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
          // blob URL 的底层内存由 imageBlobCache LRU 驱逐时统一 revoke
        }
      });
      return changed ? next : prev;
    });

    // ── 2. 解密窗口内封面 ────────────────────────────────────────────────────
    for (let i = start; i <= end; i++) {
      const src = getThumbUrl(vids[i]);
      if (!src) continue;
      // 已有结果则直接同步写入 state
      const cached = imageBlobCache.get(src);
      if (cached) {
        setCoverBlobUrlMap(prev => prev[i] === cached ? prev : { ...prev, [i]: cached });
        continue;
      }
      const iCopy = i, srcCopy = src;
      blobImageDecryption.decryptImageToBlob(srcCopy, {
        priority: Math.abs(i - activeIndex) <= 1 ? 'high' : 'normal',
      }).then(url => {
        if (!url) return;
        try { imageBlobCache.set(srcCopy, url); } catch (_) {}
        setCoverBlobUrlMap(prev => prev[iCopy] === url ? prev : { ...prev, [iCopy]: url });
      }).catch(() => {});
    }
  }, [activeIndex]); // eslint-disable-line react-hooks/exhaustive-deps
  // 依赖仅 activeIndex：videos 通过 videosRef 读取（无需列入依赖），
  // 避免追加新页时此 effect 全量重跑导致不必要的解密和 setState

  useEffect(() => () => {
    if (revealTimerRef.current) clearTimeout(revealTimerRef.current);
    if (clipTimerRef.current) clearTimeout(clipTimerRef.current);
  }, []);

  useEffect(() => {
    return () => {
      Object.keys(hlsMap.current).forEach(k => destroyHls(Number(k)));
      Object.values(nativeBlobUrls.current).forEach(blobUrl => {
        try { URL.revokeObjectURL(blobUrl); } catch (_) {}
      });
      Object.values(videoRefs.current).forEach(v => {
        if (!v) return;
        try { v.pause(); v.removeAttribute('src'); v.load(); } catch (_) {}
      });
    };
  }, [destroyHls]);

  useEffect(() => {
    document.body.classList.add('user-group-fullscreen-open');
    window.dispatchEvent(new CustomEvent('userGroupFullScreen:change', { detail: { open: true } }));
    return () => {
      document.body.classList.remove('user-group-fullscreen-open');
      window.dispatchEvent(new CustomEvent('userGroupFullScreen:change', { detail: { open: false } }));
    };
  }, []);

  useEffect(() => {
    const update = () => setVph(window.innerHeight);
    window.addEventListener('resize', update, { passive: true });
    return () => window.removeEventListener('resize', update);
  }, []);

  useEffect(() => {
    isMutedRef.current = isMuted;
    const v = videoRefs.current[activeIndex];
    if (v) v.muted = isMuted;
  }, [activeIndex, isMuted]);

  // ── UI 动作 ──────────────────────────────────────────────────────────────────

  const toggleMute = useCallback(() => {
    setIsMuted(prev => {
      const next = !prev;
      isMutedRef.current = next;
      try {
        const v = videoRefs.current[activeIdxRef.current];
        if (v) v.muted = next;
      } catch (_) {}
      return next;
    });
  }, []);

  const handleShare = useCallback(async () => {
    try {
      const url = window.location.href;
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(url);
        alert('链接已复制到剪贴板');
      } else {
        const ta = document.createElement('textarea');
        ta.value = url;
        ta.style.cssText = 'position:fixed;opacity:0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        alert('链接已复制到剪贴板');
      }
    } catch (_) {}
  }, []);

  const handleOpenMenu = useCallback(() => {
    document.body.classList.add('short-video-mode');
    window.dispatchEvent(new CustomEvent('mobileMenu:control', {
      detail: { action: 'open', source: 'shortVideo' },
    }));
  }, []);

  // ── 渲染 ─────────────────────────────────────────────────────────────────────

  return (
    <>
      <style>{`
        .mvf-swiper {
          position: absolute !important;
          top: 0 !important; left: 0 !important;
          right: 0 !important; bottom: 0 !important;
          width: 100% !important; height: 100% !important;
        }
        .mvf-swiper .swiper-wrapper {
          height: 100% !important;
          transition-timing-function: cubic-bezier(0.32, 0.72, 0, 1) !important;
        }
        .mvf-slide {
          position: relative !important;
          width: 100% !important;
          height: 100vh !important;
          overflow: hidden !important;
          will-change: transform;
        }
        @supports (height: 100dvh) {
          .mvf-slide { height: 100dvh !important; }
        }
        .mvf-video::-webkit-media-controls,
        .mvf-video::-webkit-media-controls-panel,
        .mvf-video::-webkit-media-controls-enclosure { display: none !important; opacity: 0 !important; }
        .mvf-video::-moz-media-controls { display: none !important; opacity: 0 !important; }
      `}</style>

      <div style={{
        position: 'fixed', inset: 0, zIndex: 99999,
        background: '#000',
        overscrollBehavior: 'none',
        WebkitOverflowScrolling: 'touch',
      }}>

        {/* 返回按钮 */}
        <button
          onClick={e => { e.stopPropagation(); onClose?.(); }}
          style={{ ...GLASS_BTN, position: 'fixed', top: 16, left: 16, zIndex: 100000 }}
          aria-label="返回"
        >
          <svg width="20" height="20" fill="none" stroke="currentColor" strokeWidth={2.5} viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        {/* 右侧按钮组 */}
        <div style={{ position: 'fixed', top: 16, right: 16, zIndex: 100000, display: 'flex', alignItems: 'center', gap: 8 }}>
          <button onClick={e => { e.stopPropagation(); toggleMute(); }} style={GLASS_BTN} aria-label={isMuted ? '开启声音' : '关闭声音'}>
            {isMuted ? <FiVolumeX size={18} /> : <FiVolume2 size={18} />}
          </button>
          <button onClick={e => { e.stopPropagation(); handleOpenMenu(); }} style={GLASS_BTN} aria-label="菜单">
            <FiMenu size={18} />
          </button>
          <button onClick={e => { e.stopPropagation(); handleShare(); }} style={GLASS_BTN} aria-label="分享">
            <FiShare2 size={18} />
          </button>
        </div>

        {/* Swiper */}
        <Swiper
          direction="vertical"
          slidesPerView={1}
          spaceBetween={0}
          mousewheel
          keyboard
          modules={[Mousewheel, Keyboard, Virtual]}
          virtual={{ addSlidesBefore: 1, addSlidesAfter: 1 }}
          initialSlide={initialIndex}
          speed={400}
          threshold={8}
          longSwipesRatio={0.25}
          touchRatio={1}
          resistance={false}
          touchReleaseOnEdges
          onTouchStart={handleTouchStart}
          onSlideChange={handleSlideChange}
          onTransitionEnd={handleTransitionEnd}
          className="mvf-swiper"
          style={{ height: vph }}
        >
          {videos.map((video, idx) => {
            const isActive      = idx === activeIndex;
            const slideState    = states[idx] || 'idle';
            const shouldReveal  = playingIdx === idx;
            const clipVisible   = !!clipVisibleMap[idx];
            const coverBlobUrl  = coverBlobUrlMap[idx] || null;
            const showSpinner   = isActive && slideState === 'loading';
            const isNear        = Math.abs(idx - activeIndex) <= COVER_RANGE;

            /** @type {React.CSSProperties} */
            const coverFade = {
              opacity: shouldReveal ? 0 : 1,
              transition: shouldReveal ? 'opacity 0.4s cubic-bezier(0.32, 0.72, 0, 1)' : 'none',
              pointerEvents: 'none',
            };

            return (
              <SwiperSlide key={video.id ?? idx} virtualIndex={idx} className="mvf-slide" style={{ height: vph }}>

                {/* 层0：黑色底板 */}
                <div style={{ position: 'absolute', inset: 0, background: '#000', zIndex: 0 }} />

                {/* 层1：模糊背景封面 */}
                {coverBlobUrl && isNear && (
                  <div style={{ position: 'absolute', inset: 0, zIndex: 1, overflow: 'hidden', ...coverFade }}>
                    <img src={coverBlobUrl} alt="" draggable={false} style={{
                      position: 'absolute', inset: 0, width: '100%', height: '100%',
                      objectFit: 'cover', filter: 'blur(28px) brightness(0.5)', transform: 'scale(1.08)',
                    }} />
                  </div>
                )}

                {/* 层2：video
                    clip-path 直接施加在 video 元素上（iOS Safari GPU 层只响应自身的 clip-path）。
                    reveal 时加 60ms transition：GPU 层在逐渐展开的裁切窗口中稳定尺寸，
                    完全展开时已是正确的 CSS 尺寸，不会出现原始分辨率闪烁。
                    poster 使用封面 blob URL，视频暂停/未播放时由 Safari 原生渲染封面图（CSS 尺寸正确）。 */}
                <video
                  ref={el => { videoRefs.current[idx] = el; }}
                  className="mvf-video"
                  playsInline muted={isMuted} loop controls={false}
                  poster={coverBlobUrl || undefined}
                  preload={Math.abs(idx - activeIndex) <= 1 ? 'auto' : 'none'}
                  controlsList="nofullscreen nodownload noplaybackrate"
                  disablePictureInPicture
                  onContextMenu={e => e.preventDefault()}
                  onClick={() => {
                    const v = videoRefs.current[idx];
                    if (!v) return;
                    if (v.paused) safePlay(v); else v.pause();
                  }}
                  onCanPlay={() => {
                    if (idx === activeIdxRef.current) safePlay(videoRefs.current[idx]);
                  }}
                  onPlaying={() => {
                    setState(idx, 'ready');
                    if (idx !== activeIdxRef.current) return;
                    if (!transitionDoneRef.current) {
                      pendingRevealIdxRef.current = idx;
                      return;
                    }
                    startReveal(idx);
                  }}
                  onWaiting={() => {
                    if (idx === activeIdxRef.current && statesRef.current[idx] === 'ready') setState(idx, 'loading');
                  }}
                  onTimeUpdate={() => {
                    if (idx === activeIdxRef.current && statesRef.current[idx] === 'loading') setState(idx, 'ready');
                  }}
                  onError={() => setState(idx, 'idle')}
                  style={{
                    position: 'absolute', inset: 0,
                    width: '100%', height: '100%',
                    objectFit: 'contain', background: 'transparent',
                    zIndex: 2, display: 'block',
                    clipPath: clipVisible ? CLIP_VISIBLE : CLIP_HIDDEN,
                    WebkitClipPath: clipVisible ? CLIP_VISIBLE : CLIP_HIDDEN,
                    // reveal 时 60ms 过渡：GPU 层在裁切窗口逐渐打开过程中稳定至正确尺寸
                    transition: clipVisible ? 'clip-path 0.06s ease, -webkit-clip-path 0.06s ease' : 'none',
                    WebkitTransition: clipVisible ? '-webkit-clip-path 0.06s ease' : 'none',
                  }}
                />
                {/* 层3：清晰封面缩略图 */}
                {coverBlobUrl && isNear && (
                  <div style={{ position: 'absolute', inset: 0, zIndex: 3, overflow: 'hidden', ...coverFade }}>
                    <img src={coverBlobUrl} alt="" draggable={false} style={{
                      position: 'absolute', inset: 0, width: '100%', height: '100%',
                      objectFit: 'contain',
                    }} />
                  </div>
                )}

                {/* 层10：加载指示器（500ms 防抖，短暂 buffering 不显示） */}
                <div style={{
                  position: 'absolute', inset: 0, zIndex: 10,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  pointerEvents: 'none',
                  opacity: (showSpinner && !shouldReveal) ? 1 : 0,
                  transition: (showSpinner && !shouldReveal)
                    ? 'opacity 0.25s ease 0.5s'
                    : 'opacity 0.2s ease',
                }}>
                  <TikTokLoading size={48} dark={false} />
                </div>

                {/* 层15：暂停图标（视频可见后才渲染） */}
                {clipVisible && isActive && (
                  <VideoStateIcon videoEl={videoRefs.current[idx]} />
                )}

                {/* 层20：底部信息区 */}
                <div style={{
                  position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 20,
                  padding: '12px 16px',
                  paddingBottom: 'max(12px, env(safe-area-inset-bottom, 12px))',
                  background: 'linear-gradient(to top, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0) 100%)',
                  pointerEvents: 'none',
                }}>
                  {isActive && <ProgressBar videoEl={videoRefs.current[idx]} />}
                </div>

                {/* 加载更多提示（最后一个 slide） */}
                {idx === videos.length - 1 && isLoadingMore && (
                  <div style={{
                    position: 'absolute', zIndex: 25,
                    bottom: 80, left: '50%', transform: 'translateX(-50%)',
                    pointerEvents: 'none',
                  }}>
                    <div style={{
                      background: 'rgba(0,0,0,0.7)',
                      backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)',
                      borderRadius: 9999, padding: '8px 16px',
                      display: 'flex', alignItems: 'center', gap: 8,
                      color: '#fff', fontSize: 13, whiteSpace: 'nowrap',
                    }}>
                      <InlineSpinner />
                      加载更多视频...
                    </div>
                  </div>
                )}

              </SwiperSlide>
            );
          })}
        </Swiper>
      </div>
    </>
  );
};

export default MobileVideoFeed;
