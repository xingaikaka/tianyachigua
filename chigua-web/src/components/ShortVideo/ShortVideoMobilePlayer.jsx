/**
 * 短视频分类 - 移动端全屏播放器
 *
 * 架构：首帧图 + video 双层，极少 HLS 实例，自然滑动，无回居无截断
 *  - 所有 slide 始终展示首帧图作为占位背景
 *  - 仅当前附近 ±mediaWindow 个 slide 挂载 video/HLS
 *  - 视频 playing 后首帧图淡出，视频淡入
 *  - 回看时旧视频重新创建，首帧图先顶住，不出现黑屏
 */
import React, { useRef, useState, useEffect, useMemo, useCallback } from 'react';
import { Swiper, SwiperSlide } from 'swiper/react';
import { Mousewheel, Keyboard, Virtual } from 'swiper/modules';
import Hls from 'hls.js';
import { FiVolume2, FiVolumeX, FiShuffle } from 'react-icons/fi';
import 'swiper/css';
import TikTokIcon from '../common/TikTokIcon';
import TikTokLoading from '../common/TikTokLoading';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../../utils/hlsUtils';

const BUTTON_STYLE = {
  width: '40px',
  height: '40px',
  backgroundColor: 'rgba(255, 255, 255, 0.15)',
  backdropFilter: 'blur(20px) saturate(180%)',
  WebkitBackdropFilter: 'blur(20px) saturate(180%)',
  border: '1px solid rgba(255, 255, 255, 0.3)',
  boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
};

const isHlsUrl = (url) => /\.m3u8(\?|$)/i.test(url || '');
const DEBUG = false;

const formatTime = (seconds) => {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
};

const ShortVideoMobilePlayer = ({
  videos = [],
  initialIndex = 0,
  onClose,
  onLoadMore,
  hasMore = false,
  isLoadingMore = false,
  onRandomJump,
}) => {
  const [activeIndex, setActiveIndex] = useState(initialIndex);
  const [isMuted, setIsMuted] = useState(false);
  const [progress, setProgress] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);
  const [drawerMsg, setDrawerMsg] = useState('加载中，请稍候');
  const [viewportH, setViewportH] = useState(() => {
    if (typeof window === 'undefined') return 0;
    return window.visualViewport?.height || window.innerHeight || 0;
  });

  const isIosSafari = useMemo(() => {
    if (typeof navigator === 'undefined') return false;
    const ua = navigator.userAgent || '';
    return /iP(hone|ad|od)/i.test(ua) || (/Macintosh/i.test(ua) && /Mobile/i.test(ua));
  }, []);

  // iOS 最多保留 current±1=3 个实例，桌面/Android 保留 current±2=5 个
  const mediaWindow = isIosSafari ? 1 : 2;
  // 封面层渲染窗口：只渲染 ±5 内的 slide 封面，减少请求数，下一 slide 必在范围内
  const frameWindow = 5;

  // DOM refs
  const swiperRef = useRef(null);
  const videoRefs = useRef({});  // index -> <video> element
  const imgRefs = useRef({});    // index -> <img> element
  const hlsRefs = useRef({});    // index -> Hls instance
  const nativeBlobUrlsRef = useRef({}); // index -> iOS 原生 HLS blob URL

  // 值 refs（始终保持最新）
  const activeIndexRef = useRef(initialIndex);
  const isMutedRef = useRef(false);
  const progressRafRef = useRef(0);
  const drawerTimerRef = useRef(null);
  const playLockRef = useRef(new Set()); // 防止同一 index 重复 setup
  const progressBarRef = useRef(null);

  // 同步 isMuted ref
  useEffect(() => { isMutedRef.current = isMuted; }, [isMuted]);

  // 标准化 items
  const items = useMemo(() => videos.map(v => {
    const d = v?.data || v;
    return {
      ...d,
      id: d?.id ?? v?.id,
      videoUrl: d?.firstVideoUrl || d?.hdUrl || d?.sdUrl || '',
      frameUrl: d?.firstFrameUrl || d?.coverImageUrl || d?.posterUrl || d?.coverUrl || '',
    };
  }), [videos]);

  const itemsRef = useRef(items);
  itemsRef.current = items;

  useEffect(() => {
    if (!DEBUG || items.length === 0) return;
    const raw = videos[0];
    const sample = items.slice(0, 5).map((it, i) => ({
      i,
      id: it.id,
      hasFrame: !!it.frameUrl,
      frameLen: (it.frameUrl || '').length,
      framePreview: it.frameUrl ? it.frameUrl.slice(0, 60) + '...' : 'null',
    }));
    console.log('[ShortVideo] items 归一化', {
      total: items.length,
      sample,
      rawKeys: raw ? Object.keys(raw) : [],
      rawFirstFrame: raw?.firstFrameUrl ?? raw?.first_frame_url ?? 'N/A',
      rawCover: raw?.coverImageUrl ?? raw?.cover_image_url ?? 'N/A',
    });
  }, [items, videos]);

  // ─── DOM 直接操作（避免 React 渲染开销）───────────────────────────────────
  //
  // 层次结构（z-index 低→高）：
  //   #111 父背景 → video(z:1, opacity:1, poster=封面) → img(z:2, opacity:1)
  //
  // 视频播放前：img 盖在 video 上，用户看到封面图
  // 视频播放后：img.opacity 设为 0，video 内容从下面露出
  // img URL 失败：onError 设 img.opacity=0，video poster 作为第二道保障
  // video poster 也失败：显示 #111 深灰背景

  const showVideoLayer = useCallback((index) => {
    // 仅隐藏 img 层，video 层始终 opacity:1 不需要操作
    const img = imgRefs.current[index];
    if (img) img.style.opacity = '0';
  }, []);

  const showFrameLayer = useCallback((index) => {
    // 恢复 img 层显示（覆盖 video）
    const img = imgRefs.current[index];
    if (img) img.style.opacity = '1';
  }, []);

  // ─── 进度条 ───────────────────────────────────────────────────────────────

  const stopProgress = useCallback(() => {
    if (progressRafRef.current) {
      cancelAnimationFrame(progressRafRef.current);
      progressRafRef.current = 0;
    }
  }, []);

  // 进度条点击/拖动快进
  const seekToRatio = useCallback((ratio) => {
    const v = videoRefs.current[activeIndexRef.current];
    if (!v || !Number.isFinite(v.duration) || v.duration <= 0) return;
    const r = Math.max(0, Math.min(1, ratio));
    v.currentTime = r * v.duration;
    setProgress(r);
    setCurrentTime(r * v.duration);
  }, []);

  const getSeekRatioFromEvent = useCallback((e) => {
    const bar = progressBarRef.current;
    if (!bar) return null;
    const rect = bar.getBoundingClientRect();
    const x = e.touches?.[0] ? e.touches[0].clientX : e.clientX;
    if (x == null) return null;
    return Math.max(0, Math.min(1, (x - rect.left) / rect.width));
  }, []);

  const handleProgressSeek = useCallback((e) => {
    e.stopPropagation();
    e.preventDefault();
    const ratio = getSeekRatioFromEvent(e);
    if (ratio != null) seekToRatio(ratio);
  }, [getSeekRatioFromEvent, seekToRatio]);

  const handleProgressPointerDown = useCallback((e) => {
    e.stopPropagation();
    handleProgressSeek(e);
    const onMove = (e2) => {
      e2.preventDefault();
      const ratio = getSeekRatioFromEvent(e2);
      if (ratio != null) seekToRatio(ratio);
    };
    const onUp = () => {
      document.removeEventListener('touchmove', onMove, { capture: true });
      document.removeEventListener('touchend', onUp);
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
    };
    document.addEventListener('touchmove', onMove, { passive: false, capture: true });
    document.addEventListener('touchend', onUp, { once: true });
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp, { once: true });
  }, [handleProgressSeek, getSeekRatioFromEvent, seekToRatio]);

  const startProgress = useCallback(() => {
    stopProgress();
    const loop = () => {
      const v = videoRefs.current[activeIndexRef.current];
      if (v && v.duration > 0) {
        const ct = v.currentTime;
        const dur = v.duration;
        setProgress(Math.min(1, Math.max(0, ct / dur)));
        setCurrentTime(ct);
        setDuration(dur);
      }
      progressRafRef.current = requestAnimationFrame(loop);
    };
    progressRafRef.current = requestAnimationFrame(loop);
  }, [stopProgress]);

  // ─── 底部抽屉 ─────────────────────────────────────────────────────────────

  const hideDrawer = useCallback(() => {
    if (drawerTimerRef.current) {
      clearTimeout(drawerTimerRef.current);
      drawerTimerRef.current = null;
    }
    setShowDrawer(false);
  }, []);

  // 延迟显示抽屉：如果在 delay ms 内视频已就绪则不显示
  const scheduleDrawer = useCallback((message, delay = 300) => {
    hideDrawer();
    drawerTimerRef.current = setTimeout(() => {
      drawerTimerRef.current = null;
      const v = videoRefs.current[activeIndexRef.current];
      if (v && v.readyState >= 2) return; // 已就绪，无需提示
      setDrawerMsg(message);
      setShowDrawer(true);
    }, delay);
  }, [hideDrawer]);

  // ─── HLS / 媒体管理 ───────────────────────────────────────────────────────

  const createHls = useCallback((index, videoEl, url) => {
    if (hlsRefs.current[index]) return hlsRefs.current[index];
    const hls = new Hls({
      maxBufferLength: isIosSafari ? 6 : 10,
      maxMaxBufferLength: isIosSafari ? 10 : 20,
      xhrSetup: hlsXhrSetup,
    });
    hlsRefs.current[index] = hls;
    hls.loadSource(url);
    hls.attachMedia(videoEl);
    hls.once(Hls.Events.ERROR, (_, data) => {
      if (data.fatal) {
        try { hls.destroy(); } catch (_) {}
        delete hlsRefs.current[index];
      }
    });
    return hls;
  }, [isIosSafari]);

  // 释放媒体资源（保证不释放当前播放的）
  const releaseMedia = useCallback((index) => {
    if (index === activeIndexRef.current) return;
    const hls = hlsRefs.current[index];
    if (hls) {
      try { hls.destroy(); } catch (_) {}
      delete hlsRefs.current[index];
    }
    if (nativeBlobUrlsRef.current[index]) {
      try { URL.revokeObjectURL(nativeBlobUrlsRef.current[index]); } catch (_) {}
      delete nativeBlobUrlsRef.current[index];
    }
    const v = videoRefs.current[index];
    if (v) {
      try { v.pause(); } catch (_) {}
      try { v.removeAttribute('src'); v.load(); } catch (_) {}
      showFrameLayer(index); // 还原为首帧图显示
    }
    playLockRef.current.delete(index);
  }, [showFrameLayer]);

  // 预加载指定 index 的媒体（不播放）
  const preloadMedia = useCallback((index) => {
    const item = itemsRef.current[index];
    if (!item?.videoUrl) return;
    const v = videoRefs.current[index];
    if (!v) return;
    const url = item.videoUrl;
    if (isHlsUrl(url) && Hls.isSupported()) {
      createHls(index, v, url);
    } else if (isHlsUrl(url) && v.canPlayType && v.canPlayType('application/vnd.apple.mpegurl')) {
      // iOS Safari 原生 HLS：重写 key URI
      if (!v.src) {
        patchNativeHlsM3u8(url).then(({ url: patchedUrl, isBlob }) => {
          if (nativeBlobUrlsRef.current[index]) {
            try { URL.revokeObjectURL(nativeBlobUrlsRef.current[index]); } catch (_) {}
          }
          nativeBlobUrlsRef.current[index] = isBlob ? patchedUrl : null;
          v.src = patchedUrl;
          v.load();
        });
      }
    } else if (!v.src) {
      v.src = url;
      v.load();
    }
  }, [createHls]);

  // 播放指定 index 的视频
  const playVideoAt = useCallback((index) => {
    if (playLockRef.current.has(index)) return;
    playLockRef.current.add(index);

    const item = itemsRef.current[index];
    if (!item?.videoUrl) { playLockRef.current.delete(index); return; }
    const v = videoRefs.current[index];
    if (!v) { playLockRef.current.delete(index); return; }

    const onPlaying = () => {
      if (activeIndexRef.current !== index) return;
      hideDrawer();
      // 等视频真正渲染出首帧后再隐藏封面，避免黑屏闪烁
      // Safari：首帧渲染时机更晚，需等待 2 帧 + 更长淡出，参考抖音/快手做法
      const hideCover = () => {
        if (activeIndexRef.current !== index) return;
        showVideoLayer(index);
        startProgress();
        setIsPaused(false);
      };
      const scheduleHide = (fn) => {
        if (v.requestVideoFrameCallback) {
          v.requestVideoFrameCallback(fn);
        } else {
          requestAnimationFrame(() => requestAnimationFrame(fn));
        }
      };
      if (isIosSafari) {
        // Safari：等 2 帧再隐藏，首帧可能是 poster/空白；淡出用 0.18s 过渡
        scheduleHide(() => scheduleHide(hideCover));
      } else {
        scheduleHide(hideCover);
      }
    };

    const doPlay = () => {
      if (activeIndexRef.current !== index) return;
      v.muted = isMutedRef.current;
      v.addEventListener('playing', onPlaying, { once: true });
      v.play().catch(() => {
        // 自动播放被阻止时静音重试
        if (!v.muted) {
          v.muted = true;
          v.play().catch(() => {});
        }
      });
    };

    const videoUrl = item.videoUrl;
    if (isHlsUrl(videoUrl) && Hls.isSupported()) {
      createHls(index, v, videoUrl);
      if (v.readyState >= 2) {
        doPlay();
      } else {
        v.addEventListener('canplay', doPlay, { once: true });
      }
    } else if (isHlsUrl(videoUrl) && v.canPlayType && v.canPlayType('application/vnd.apple.mpegurl')) {
      // iOS Safari 原生 HLS：重写 key URI，然后播放
      const setupAndPlay = (src) => {
        if (!v.src) { v.src = src; v.load(); }
        if (v.readyState >= 2) { doPlay(); }
        else { v.addEventListener('canplay', doPlay, { once: true }); }
      };
      if (v.src) {
        setupAndPlay(v.src);
      } else {
        patchNativeHlsM3u8(videoUrl).then(({ url: patchedUrl, isBlob }) => {
          if (nativeBlobUrlsRef.current[index]) {
            try { URL.revokeObjectURL(nativeBlobUrlsRef.current[index]); } catch (_) {}
          }
          nativeBlobUrlsRef.current[index] = isBlob ? patchedUrl : null;
          setupAndPlay(patchedUrl);
        });
      }
    } else {
      if (!v.src) { v.src = videoUrl; v.load(); }
      if (v.readyState >= 2) {
        doPlay();
      } else {
        v.addEventListener('canplay', doPlay, { once: true });
      }
    }
  }, [createHls, hideDrawer, isIosSafari, showVideoLayer, startProgress]);

  // ─── Effects ──────────────────────────────────────────────────────────────

  // 主 effect：切换 activeIndex 后播放 + 预加载邻居
  // 注意：不把 items.length 列为依赖，避免追加新页时重新调用 playVideoAt（会打断正在播放的视频）
  // 边界检查改用 itemsRef.current.length，始终读取最新值
  useEffect(() => {
    if (!itemsRef.current.length) return;
    const rafId = requestAnimationFrame(() => {
      playVideoAt(activeIndex);
      const len = itemsRef.current.length;
      if (activeIndex + 1 < len) preloadMedia(activeIndex + 1);
      if (activeIndex - 1 >= 0) preloadMedia(activeIndex - 1);
    });
    // 延迟预加载更远处（低优先级）
    const t = setTimeout(() => {
      const len = itemsRef.current.length;
      if (activeIndex + 2 < len) preloadMedia(activeIndex + 2);
    }, 600);
    return () => {
      cancelAnimationFrame(rafId);
      clearTimeout(t);
      stopProgress();
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeIndex, playVideoAt, preloadMedia, stopProgress]);

  // 释放媒体窗口外的 HLS/video 实例
  useEffect(() => {
    const lo = Math.max(0, activeIndex - mediaWindow);
    const hi = Math.min(items.length - 1, activeIndex + mediaWindow);

    // 销毁窗口外的 HLS
    Object.keys(hlsRefs.current).forEach(k => {
      const idx = Number(k);
      if (idx < lo || idx > hi) releaseMedia(idx);
    });

    // 释放窗口外 video 的 src（对没有 HLS 的情况）
    Object.keys(videoRefs.current).forEach(k => {
      const idx = Number(k);
      if (idx < lo || idx > hi) {
        const v = videoRefs.current[idx];
        if (v && (v.src || v.currentSrc)) {
          try { v.pause(); v.removeAttribute('src'); v.load(); } catch (_) {}
          showFrameLayer(idx);
          playLockRef.current.delete(idx);
        }
      }
    });
  }, [activeIndex, items.length, mediaWindow, releaseMedia, showFrameLayer]);

  // 同步静音状态到当前视频
  useEffect(() => {
    const v = videoRefs.current[activeIndex];
    if (v) v.muted = isMuted;
  }, [isMuted, activeIndex]);

  // Viewport 高度（iOS Safari 地址栏修正）
  useEffect(() => {
    const updateH = () => {
      try {
        const h = window.visualViewport?.height ?? window.innerHeight;
        if (h && h > 0) setViewportH(h);
      } catch (_) {}
    };
    updateH();
    window.visualViewport?.addEventListener('resize', updateH);
    return () => window.visualViewport?.removeEventListener('resize', updateH);
  }, []);

  // 全屏标记 + 卸载清理
  useEffect(() => {
    document.body.classList.add('user-group-fullscreen-open');
    window.dispatchEvent(new CustomEvent('userGroupFullScreen:change', { detail: { open: true } }));
    return () => {
      document.body.classList.remove('user-group-fullscreen-open');
      window.dispatchEvent(new CustomEvent('userGroupFullScreen:change', { detail: { open: false } }));
      Object.values(hlsRefs.current).forEach(hls => { try { hls?.destroy(); } catch (_) {} });
      hlsRefs.current = {};
      Object.values(nativeBlobUrlsRef.current).forEach(u => { try { URL.revokeObjectURL(u); } catch (_) {} });
      nativeBlobUrlsRef.current = {};
      stopProgress();
      if (drawerTimerRef.current) clearTimeout(drawerTimerRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 触底加载更多
  useEffect(() => {
    if (hasMore && !isLoadingMore && onLoadMore) {
      if (items.length - activeIndex - 1 <= 3) onLoadMore();
    }
  }, [activeIndex, hasMore, isLoadingMore, items.length, onLoadMore]);

  // 全局当前视频信息
  useEffect(() => {
    const item = items[activeIndex];
    window['currentShortVideo'] = item?.id ? { id: item.id, title: item.title } : null;
    return () => { window['currentShortVideo'] = null; };
  }, [activeIndex, items]);

  // ─── 事件处理 ─────────────────────────────────────────────────────────────

  // 滑动开始时：直接操作 DOM 把目标 slide 置顶（避免 React setState 延迟）
  const handleSlideTransitionStart = useCallback((swiper) => {
    const toIdx = swiper.activeIndex;
    const slides = swiper.slides || [];
    slides.forEach((el) => {
      if (!el || !el.style) return;
      // Virtual 模式下用 data-swiper-slide-index 读取虚拟序号（而非 DOM 数组下标）
      const vIdx = parseInt(el.getAttribute('data-swiper-slide-index') ?? '-1', 10);
      el.style.zIndex = vIdx === toIdx ? 100 : 0;
    });
  }, []);

  // 滑动结束后切换 activeIndex（不回居，让 Swiper 自然滑动）
  const handleSlideChange = useCallback((swiper) => {
    const newIndex = swiper.activeIndex;
    if (newIndex === activeIndexRef.current) return;

    // 重置 slide z-index
    const slides = swiper.slides || [];
    slides.forEach((el) => { if (el && el.style) el.style.zIndex = ''; });

    if (DEBUG) {
      const item = itemsRef.current[newIndex];
      console.log('[ShortVideo] 滑动结束', {
        newIndex,
        hasFrameUrl: !!item?.frameUrl,
        hasVideo: !!videoRefs.current[newIndex],
      });
    }

    const isBack = newIndex < activeIndexRef.current;

    // 暂停所有其他视频
    Object.keys(videoRefs.current).forEach(k => {
      const idx = Number(k);
      if (idx !== newIndex) {
        const v = videoRefs.current[idx];
        if (v && !v.paused) try { v.pause(); } catch (_) {}
      }
    });

    // 清除所有 play lock，让新 index 可以重新 setup
    playLockRef.current.clear();
    stopProgress();
    setProgress(0);
    setCurrentTime(0);
    setDuration(0);
    setIsPaused(false);
    setActiveIndex(newIndex);
    activeIndexRef.current = newIndex;

    // 如果目标视频未就绪，延迟显示抽屉（300ms 内就绪则不显示）
    const v = videoRefs.current[newIndex];
    if (!v || v.readyState < 2) {
      scheduleDrawer(isBack ? '视频重新加载中' : '加载中，请稍候', 300);
    } else {
      hideDrawer();
    }
  }, [stopProgress, scheduleDrawer, hideDrawer]);

  const handleVideoClick = useCallback(() => {
    const v = videoRefs.current[activeIndexRef.current];
    if (!v) return;
    if (v.paused) {
      v.play().catch(() => {});
      setIsPaused(false);
    } else {
      v.pause();
      setIsPaused(true);
      stopProgress();
    }
  }, [stopProgress]);

  const toggleMute = useCallback(() => setIsMuted(prev => !prev), []);

  const handleRandom = useCallback(() => {
    if (onRandomJump) {
      onRandomJump();
    } else {
      const swiper = swiperRef.current?.swiper ?? swiperRef.current;
      if (!swiper || items.length <= 1) return;
      let t = Math.floor(Math.random() * items.length);
      if (t === activeIndexRef.current && items.length > 1) t = (t + 1) % items.length;
      swiper.slideTo(t, 400);
    }
  }, [onRandomJump, items.length]);


  const currentItem = items[activeIndex];

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <>
      <style>{`
        .sv-swiper { width: 100% !important; height: 100vh !important; }
        @supports (height: 100dvh) { .sv-swiper { height: 100dvh !important; } }
        @supports (height: -webkit-fill-available) { .sv-swiper { min-height: -webkit-fill-available !important; } }
        .sv-viewport-fixed .sv-swiper { height: 100% !important; }
        .sv-swiper .swiper-wrapper {
          height: 100% !important;
          transition-timing-function: cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
        }
        .sv-swiper .swiper-slide { height: 100% !important; width: 100% !important; overflow: hidden !important; }
        .sv-video::-webkit-media-controls,
        .sv-video::-webkit-media-controls-panel,
        .sv-video::-webkit-media-controls-enclosure { display: none !important; opacity: 0 !important; }
      `}</style>

      <div
        className={`fixed inset-0 z-[99999] bg-black ${viewportH > 0 ? 'sv-viewport-fixed' : ''}`}
        style={viewportH > 0 ? { height: viewportH, minHeight: viewportH } : undefined}
        onClick={handleVideoClick}
      >
        {/* 返回按钮 */}
        <button
          onClick={(e) => { e.stopPropagation(); onClose?.(); }}
          className="fixed top-4 left-4 z-[100000] text-white p-2 rounded-full"
          style={BUTTON_STYLE}
          aria-label="返回"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>

        {/* 随机按钮（居中顶部） */}
        <button
          onClick={(e) => { e.stopPropagation(); handleRandom(); }}
          className="fixed top-4 left-1/2 -translate-x-1/2 z-[100000] text-white p-2 rounded-full"
          style={BUTTON_STYLE}
          aria-label="随机切换"
        >
          <FiShuffle size={16} />
        </button>

        {/* 右上角：静音 + 分享 */}
        <div className="fixed top-4 right-4 z-[100000] flex items-center gap-2">
          <button
            onClick={(e) => { e.stopPropagation(); toggleMute(); }}
            className="text-white p-2 rounded-full"
            style={BUTTON_STYLE}
            aria-label={isMuted ? '开启声音' : '关闭声音'}
          >
            {isMuted ? <FiVolumeX size={20} /> : <FiVolume2 size={20} />}
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); window.dispatchEvent(new CustomEvent('shortVideo:share')); }}
            className="text-white p-2 rounded-full"
            style={BUTTON_STYLE}
            aria-label="分享"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M18 8C19.6569 8 21 6.65685 21 5C21 3.34315 19.6569 2 18 2C16.3431 2 15 3.34315 15 5C15 5.18703 15.0124 5.37138 15.0361 5.55111L8.35589 9.8914C7.74927 9.33481 6.9123 9 6 9C4.34315 9 3 10.3431 3 12C3 13.6569 4.34315 15 6 15C6.9123 15 7.74927 14.6652 8.35589 14.1086L15.0361 18.4489C15.0124 18.6286 15 18.813 15 19C15 20.6569 16.3431 22 18 22C19.6569 22 21 20.6569 21 19C21 17.3431 19.6569 16 18 16C17.0877 16 16.2507 16.3348 15.6441 16.8914L8.96389 12.5511C8.98762 12.3714 9 12.187 9 12C9 11.813 8.98762 11.6286 8.96389 11.4489L15.6441 7.1086C16.2507 7.66519 17.0877 8 18 8Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </button>
        </div>

        {/* Swiper 主体 */}
        <Swiper
          ref={swiperRef}
          direction="vertical"
          slidesPerView={1}
          spaceBetween={0}
          speed={400}
          mousewheel={true}
          keyboard={true}
          allowTouchMove={true}
          modules={[Mousewheel, Keyboard, Virtual]}
          virtual={{ addSlidesBefore: 1, addSlidesAfter: 1 }}
          initialSlide={initialIndex}
          onSlideChangeTransitionStart={handleSlideTransitionStart}
          onSlideChangeTransitionEnd={handleSlideChange}
          className="sv-swiper"
        >
          {items.map((item, index) => {
            const inMediaWindow = Math.abs(index - activeIndex) <= mediaWindow;
            const inFrameWindow = Math.abs(index - activeIndex) <= frameWindow;

            return (
              <SwiperSlide key={`${item.id ?? 'v'}-${index}`} virtualIndex={index}>
                {/*
                  父容器用深灰 #111 作为永久兜底背景。
                  仅 frameWindow(±5) 内渲染封面层，减少请求数；下一 slide 必在范围内。
                */}
                <div className="relative w-full h-full" style={{ background: '#111' }}>

                  {/*
                    层 1：封面图（用 img，在 Swiper 过渡时渲染更可靠）
                    - 强制 GPU 层 transform+will-change，避免过渡时不绘制
                    - 视频 playing 后：showVideoLayer() 把 opacity 设为 0
                  */}
                  {inFrameWindow && item.frameUrl ? (
                    <div
                      ref={el => { if (el) imgRefs.current[index] = el; else delete imgRefs.current[index]; }}
                      style={{
                        position: 'absolute',
                        inset: 0,
                        width: '100%',
                        height: '100%',
                        zIndex: 2,
                        transform: 'translateZ(0)',
                        willChange: 'opacity',
                        transition: isIosSafari ? 'opacity 0.18s ease-out' : 'opacity 0.1s ease-out',
                      }}
                    >
                      <SecureDecryptedImage
                        src={item.frameUrl}
                        lazyLoad={false}
                        showLoadingIndicator={false}
                        objectFit="contain"
                        style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
                        imageStyle={{
                          backgroundColor: '#111',
                          transition: 'opacity 0.35s ease',
                          pointerEvents: 'none',
                        }}
                      />
                    </div>
                  ) : (
                    <div
                      ref={el => { if (el) imgRefs.current[index] = el; else delete imgRefs.current[index]; }}
                      style={{ position: 'absolute', inset: 0, background: '#111', zIndex: 2, transition: isIosSafari ? 'opacity 0.18s ease-out' : 'opacity 0.1s ease-out' }}
                    />
                  )}

                  {/* 层 2：video（仅媒体窗口内挂载）
                      z-index:1，始终 opacity:1，poster=封面图
                      视频未播放时：poster 可见（作为第二道封面保障）
                      视频播放后：浏览器自动用视频帧替换 poster
                      img 层（z:2）盖在上面；播放后 img.opacity→0 露出本层
                  */}
                  {inMediaWindow && item.videoUrl && (
                    <video
                      ref={el => {
                        if (el) videoRefs.current[index] = el;
                        else delete videoRefs.current[index];
                      }}
                      className="sv-video"
                      poster={item.frameUrl || undefined}
                      style={{
                        position: 'absolute',
                        inset: 0,
                        width: '100%',
                        height: '100%',
                        objectFit: 'contain',
                        background: 'transparent',
                        transform: 'translateZ(0)',
                        zIndex: 1,   // 底层，img(z:2) 盖在上面
                      }}
                      src={!isHlsUrl(item.videoUrl) ? item.videoUrl : undefined}
                      playsInline
                      muted
                      loop
                      controls={false}
                      preload="auto"
                      disablePictureInPicture
                      onContextMenu={e => e.preventDefault()}
                      controlsList="nofullscreen noplaybackrate"
                    />
                  )}
                </div>
              </SwiperSlide>
            );
          })}
        </Swiper>

        {/* 底部抽屉加载提示（仅在视频未就绪超过 300ms 时出现） */}
        <div
          className="pointer-events-none absolute left-0 right-0 z-[1005]"
          style={{
            bottom: 0,
            height: showDrawer ? 72 : 0,
            overflow: 'hidden',
            opacity: showDrawer ? 1 : 0,
            transition: 'height 0.25s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.2s ease',
          }}
        >
          <div style={{
            minHeight: 56,
            paddingBottom: 'max(env(safe-area-inset-bottom), 8px)',
            paddingTop: 14,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 10,
            background: 'rgba(0, 0, 0, 0.88)',
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            borderTopLeftRadius: 16,
            borderTopRightRadius: 16,
            boxShadow: '0 -4px 24px rgba(0,0,0,0.3)',
          }}>
            <TikTokLoading size={28} dark={false} />
            <span className="text-white text-sm font-medium">{drawerMsg}</span>
          </div>
        </div>

        {/* 暂停图标 */}
        {isPaused && (
          <div
            className="pointer-events-none absolute inset-0 flex items-center justify-center"
            style={{ zIndex: 950 }}
          >
            <div style={{
              width: 60,
              height: 60,
              backgroundColor: 'rgba(0, 0, 0, 0.6)',
              backdropFilter: 'blur(10px)',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '2px solid rgba(255, 255, 255, 0.3)',
            }}>
              <TikTokIcon size={28} color="#fff" opacity={0.9} />
            </div>
          </div>
        )}

        {/* 底部：标题 + 进度条 + 时长 */}
        <div
          className="absolute bottom-0 left-0 right-0 p-4 pb-safe"
          style={{
            background: 'linear-gradient(to top, rgba(0,0,0,0.8), transparent)',
            zIndex: 1000,
          }}
        >
          {currentItem?.title && (
            <div className="text-white text-sm line-clamp-2 pointer-events-none mb-3" style={{ opacity: 0.95, textAlign: 'left' }}>
              {currentItem.title}
            </div>
          )}
          <div className="flex items-center justify-between mb-2 text-white/90 text-xs">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
          <div
            ref={progressBarRef}
            className="h-[6px] rounded-full overflow-hidden shadow-[0_0_6px_rgba(0,0,0,0.25)] cursor-pointer touch-none"
            style={{ background: 'rgba(255,255,255,0.25)' }}
            onClick={handleProgressSeek}
            onMouseDown={handleProgressPointerDown}
            onTouchStart={handleProgressPointerDown}
            role="slider"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(progress * 100)}
            aria-label="视频进度"
          >
            <div
              className="h-full bg-white transition-all duration-150 pointer-events-none"
              style={{ width: `${Math.round(progress * 100)}%` }}
            />
          </div>
        </div>

        {/* 加载更多提示 */}
        {activeIndex === items.length - 1 && isLoadingMore && (
          <div
            className="absolute bottom-20 left-1/2 -translate-x-1/2 pointer-events-none"
            style={{ zIndex: 1001 }}
          >
            <div className="bg-black/70 backdrop-blur-sm px-4 py-2 rounded-full flex items-center gap-2 text-white text-sm">
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              <span>加载更多视频...</span>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default ShortVideoMobilePlayer;
