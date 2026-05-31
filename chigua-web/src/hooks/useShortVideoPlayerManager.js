import { useCallback, useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../utils/hlsUtils';

const DEFAULT_VIDEO_STATE = {
  ready: false,
  paused: false,
  loading: false,
  delayed: false
};

const HAVE_CURRENT_DATA = typeof HTMLMediaElement !== 'undefined' && HTMLMediaElement
  ? HTMLMediaElement.HAVE_CURRENT_DATA
  : 2;
const HAVE_ENOUGH_DATA = typeof HTMLMediaElement !== 'undefined' && HTMLMediaElement
  ? HTMLMediaElement.HAVE_ENOUGH_DATA
  : 4;

/**
 * hls.js 播放实例的公共配置。
 * maxBufferLength：目标缓冲时长（秒），达到后暂停下载 TS。
 * maxMaxBufferLength：绝对上限，防止慢网络时过度缓冲占用内存。
 * maxBufferSize：字节上限（0 = 不限，由 maxBufferLength 控制）。
 * 短视频场景：视频较短，缓冲 10 秒已足够流畅；超出部分用户大概率会切换视频，
 * 下载过多是对带宽和内存的浪费。
 */
const HLS_PLAYBACK_CONFIG = {
  maxBufferLength: 10,       // 目标缓冲 10 秒
  maxMaxBufferLength: 15,    // 最多缓冲 15 秒
  maxBufferSize: 0,          // 不用字节数限制，由秒数控制
  startLevel: -1,            // 自动选画质
  capLevelToPlayerSize: true,
};

/** 预加载实例的 buffer 配置更保守：只需要首帧数据，省内存 */
const HLS_PREFETCH_CONFIG = (isSafari) => ({
  maxBufferLength: isSafari ? 2 : 3,
  maxMaxBufferLength: isSafari ? 3 : 5,
  maxBufferSize: 0,
  startLevel: -1,
  capLevelToPlayerSize: true,
});

/**
 * 管理短视频播放器的播放、进度、预加载、HLS 以及 UI 状态。
 */
const useShortVideoPlayerManager = ({
  items,
  isMuted,
  activeIndexRef,
  autoPlayDelayMs = 60,
  farRecycleDistance = 1,
  quickSwitchThresholdMs = 300,
  loadingIndicatorDelayMs = 800,
  prefetchNextDelayMs = 150
}) => {
  const videoRefs = useRef([]);
  const hlsMapRef = useRef(new Map());
  const hlsPrefetchRef = useRef(new Map());
  const errorRecoverTriedRef = useRef(new Set());
  const loadingTimersRef = useRef(new Map());
  const preloadTimersRef = useRef(new Map());
  const preloadedVideosRef = useRef(new Set()); // 已开始预加载的视频
  const preloadedReadyRef = useRef(new Set()); // 预加载完成且已准备好播放的视频（manifest已解析）
  const preloadedFirstFrameRef = useRef(new Set()); // 预解码首帧完成的视频
  const hiddenVideoRefs = useRef(new Map()); // 用于预解码的隐藏 video 元素
  const videoCacheRef = useRef(new Map());
  const videoStateRef = useRef({});
  const lastSwitchTimeRef = useRef(0);
  const progressRafRef = useRef(0);
  const progressRef = useRef(0); // 使用 ref 存储进度，避免频繁状态更新
  const progressBarRefs = useRef({}); // idx → 进度条内层 DOM 节点（imperative 更新，绕过 React 渲染）
  const itemsRef = useRef(items || []);
  const isMutedRef = useRef(isMuted);
  // Safari 检测：预加载时不对非活跃视频调用 play()，避免 GPU 合成层覆盖黑色遮罩
  const isSafariRef = useRef(
    typeof navigator !== 'undefined' &&
    /Safari/i.test(navigator.userAgent) &&
    !/Chrome|CriOS|Edg|EdgiOS|Android/i.test(navigator.userAgent)
  );
  const firstFrameSetRef = useRef(new Set()); // 🔧 跟踪已经设置过第一帧的视频索引，避免重复设置导致闪烁
  const nativeBlobUrlsRef = useRef(new Map()); // 旧版 iOS 原生 HLS 重写 m3u8 产生的 Blob URL，需 revoke
  const playTimeoutsRef = useRef(new Map()); // 🔧 跟踪每个视频的播放超时，用于快速切换时清理
  const playEventListenersRef = useRef(new Map()); // 🔧 跟踪每个视频的事件监听器，用于快速切换时清理
  const playCheckTimersRef = useRef(new Map()); // 🔧 跟踪每个视频的播放检查定时器，用于检测视频是否卡住
  const [videoState, setVideoState] = useState({});

  useEffect(() => {
    itemsRef.current = items || [];
  }, [items]);

  useEffect(() => {
    isMutedRef.current = isMuted;
  }, [isMuted]);

  const getVideoState = useCallback((idx) => {
    // 优先从 ref 读取，避免依赖 videoState 导致频繁重新渲染
    const stateFromRef = videoStateRef.current[idx];
    if (stateFromRef) {
      return stateFromRef;
    }
    // 如果 ref 中没有，再从 state 读取（用于初始化）
    return (videoState && videoState[idx]) || {};
  }, [videoState]);

  const setVideoStateAt = useCallback((idx, partial) => {
    // 先更新 ref（所有路径的即时数据源）
    const currentState = videoStateRef.current[idx] || DEFAULT_VIDEO_STATE;
    let hasChange = false;
    for (const key in partial) {
      if (currentState[key] !== partial[key]) { hasChange = true; break; }
    }
    if (!hasChange) return;

    const nextForIdx = { ...currentState, ...partial };
    videoStateRef.current = { ...videoStateRef.current, [idx]: nextForIdx };

    // 只对可见范围（activeIndex ±1）的 slide 触发 React re-render；
    // 非可见 slide 已通过 ref 更新，下次自然渲染时会读到最新值。
    if (Math.abs(idx - activeIndexRef.current) <= 1) {
      setVideoState(prev => ({ ...(prev || {}), [idx]: nextForIdx }));
    }
  }, []); // activeIndexRef 是 ref（稳定引用），无需列入依赖

  const ensureVideoState = useCallback((idx) => {
    setVideoState(prevState => {
      const baseState = prevState || {};
      if (baseState[idx]) return baseState;
      const nextState = { ...baseState, [idx]: { ...DEFAULT_VIDEO_STATE } };
      videoStateRef.current = nextState;
      return nextState;
    });
  }, []);

  // 注册进度条 DOM 节点（imperative 更新入口）
  const registerProgressBarRef = useCallback((idx, node) => {
    if (node) {
      progressBarRefs.current[idx] = node;
      // 挂载时立即重置为 0，避免复用 DOM 时残留旧值
      node.style.transform = 'scaleX(0)';
    } else {
      delete progressBarRefs.current[idx];
    }
  }, []);

  // 停止进度循环：直接通过 DOM 把所有进度条清零，无 React state 参与，
  // 完全规避 startTransition 在主线程繁忙时被推迟、setState 滞后导致的"新 slide 不显示进度条"问题。
  const stopProgressLoop = useCallback(() => {
    try { cancelAnimationFrame(progressRafRef.current); } catch (_) {}
    progressRafRef.current = 0;
    progressRef.current = 0;
    // 立即把所有进度条 DOM 归零（imperative，零延迟）
    const bars = progressBarRefs.current;
    for (const key in bars) {
      const node = bars[key];
      if (node) node.style.transform = 'scaleX(0)';
    }
  }, []);

  // 启动进度循环：每帧 rAF 内直接修改 DOM transform，
  // 不走 React state、不走 setTimeout 节流、不走 CSS transition，对齐 TikTok 的逐帧贴合体感。
  const startProgressLoop = useCallback((idx) => {
    try { cancelAnimationFrame(progressRafRef.current); } catch (_) {}
    progressRafRef.current = 0;

    const tick = () => {
      try {
        if (activeIndexRef.current !== idx) {
          progressRafRef.current = 0;
          return;
        }
        const v = videoRefs.current[idx];
        if (!v) {
          progressRafRef.current = 0;
          return;
        }
        const d = v.duration || 0;
        const c = v.currentTime || 0;
        const newProgress = d > 0 ? Math.min(1, Math.max(0, c / d)) : 0;
        progressRef.current = newProgress;

        // imperative 更新进度条 DOM：transform: scaleX(p) + transformOrigin: left
        // 比 width 更便宜（GPU 合成层，不触发 layout），且不受 React 调度影响
        const bar = progressBarRefs.current[idx];
        if (bar) bar.style.transform = `scaleX(${newProgress})`;

        if (activeIndexRef.current === idx) {
          progressRafRef.current = requestAnimationFrame(tick);
        } else {
          progressRafRef.current = 0;
        }
      } catch (_) {
        progressRafRef.current = 0;
      }
    };
    progressRafRef.current = requestAnimationFrame(tick);
  }, [activeIndexRef]);

  const updateVideoCache = useCallback((idx, loaded = true) => {
    const cache = videoCacheRef.current;
    cache.set(idx, { timestamp: Date.now(), loaded });
    if (cache.size > 5) {
      const entries = Array.from(cache.entries());
      entries.sort((a, b) => b[1].timestamp - a[1].timestamp);
      cache.clear();
      entries.slice(0, 5).forEach(([key, value]) => cache.set(key, value));
    }
  }, []);

  // 🔧 统一管理静音恢复定时器，避免冲突
  const unmuteTimersRef = useRef(new Map());

  const playWithCatch = useCallback((video, idx = null) => {
    try {
      // 🔧 修复：如果提供了索引，清除该视频之前的静音恢复定时器
      if (idx !== null) {
        const existingTimer = unmuteTimersRef.current.get(idx);
        if (existingTimer) {
          clearTimeout(existingTimer);
          unmuteTimersRef.current.delete(idx);
        }
      }

      const attempt = () => {
        const p = video && video.play && video.play();
        if (p && typeof p.then === 'function') {
          p.catch(() => {
            try {
              // 🔧 修复：只有在用户没有手动静音时才自动静音播放
              // 🔧 修复：如果视频已经设置了 muted = false，说明是快进等操作设置的，不应该覆盖
              // 🔧 修复：第一个视频（idx === 0）刷新页面时不应该静音
              if (!video.muted && !isMutedRef.current && idx !== 0) {
                video.muted = true;
                const p2 = video.play && video.play();
                if (p2 && typeof p2.then === 'function') { p2.catch(() => {}); }
                // 🔧 修复：统一使用200ms延迟恢复声音，避免与playAt冲突
                // 🔧 修复：但只在视频确实需要恢复声音时才设置定时器（避免快进时覆盖）
                if (idx !== null) {
                  const timer = setTimeout(() => {
                    try {
                      // 🔧 修复：再次检查视频状态，如果已经设置了 muted = false，就不需要恢复
                      const currentVideo = videoRefs.current[idx];
                      if (currentVideo && !isMutedRef.current && idx === activeIndexRef.current) {
                        // 只有在视频仍然是静音状态时才恢复声音
                        if (currentVideo.muted) {
                          currentVideo.muted = false;
                          currentVideo.volume = 1.0;
                        }
                      }
                    } catch (_) {}
                    unmuteTimersRef.current.delete(idx);
                  }, 200);
                  unmuteTimersRef.current.set(idx, timer);
                } else if (!isMutedRef.current) {
                  setTimeout(() => {
                    try {
                      // 🔧 修复：检查视频是否仍然是静音状态
                      if (video.muted) {
                        video.muted = false;
                        video.volume = 1.0;
                      }
                    } catch (_) {}
                  }, 200);
                }
              }
            } catch (_) {}
          });
        }
      };
      attempt();
    } catch (_) {}
  }, [activeIndexRef]);

  /**
   * 🔧 辅助函数：清理隐藏的 video 元素
   * @param {number} idx - 视频索引
   */
  const cleanupHiddenVideo = useCallback((idx) => {
    const hiddenVideo = hiddenVideoRefs.current.get(idx);
    if (hiddenVideo) {
      try {
        hiddenVideo.pause();
        hiddenVideo.remove();
      } catch (_) {}
      hiddenVideoRefs.current.delete(idx);
    }
  }, []);

  /**
   * 🔧 智能预加载与预渲染：预加载指定索引的视频
   * @param {number} targetIdx - 要预加载的视频索引
   * @param {boolean} preDecodeFirstFrame - 是否预解码首帧（默认 true）
   */
  const preloadSingleVideo = useCallback((targetIdx, preDecodeFirstFrame = true) => {
    const currentItems = itemsRef.current;
    
    if (targetIdx < 0 || targetIdx >= currentItems.length || preloadedVideosRef.current.has(targetIdx)) {
      return;
    }

    const targetItem = currentItems[targetIdx];
    const src = (targetItem && (targetItem.data || targetItem)?.firstVideoUrl) || '';

    if (!src) return;

    const existing = preloadTimersRef.current.get(targetIdx);
    if (existing) clearTimeout(existing);

    const timer = setTimeout(() => {
      try {
        // 所有视频均为 m3u8，直接走 hls.js 预加载路径
        if (Hls.isSupported()) {
          const old = hlsPrefetchRef.current.get(targetIdx);
          if (old) {
            try { old.destroy(); } catch (_) {}
            hlsPrefetchRef.current.delete(targetIdx);
          }
          
          // 创建独立的 HLS 实例进行预加载（保守 buffer，节省内存）
          const h = new Hls({ ...HLS_PREFETCH_CONFIG(isSafariRef.current), xhrSetup: hlsXhrSetup });
          
          hlsPrefetchRef.current.set(targetIdx, h);
          
          // 🔧 步骤1：等待 manifest 解析完成
          h.once(Hls.Events.MANIFEST_PARSED, () => {
            preloadedReadyRef.current.add(targetIdx);
            updateVideoCache(targetIdx, true);

            // ⚠️ 关键守卫：若用户已滑到本视频（成为 active），完全不再做预加载相关
            // 的 attach / destroy / play / 注册事件 listener 等操作，让 playAt + 
            // attachHlsIfNeeded 接管。否则会出现：
            //   1) destroy 已被 attachHlsIfNeeded 提升到 hlsMapRef 的同一实例（destroy 自己）
            //   2) FRAG_LOADED 回调里注册的 markFirstFrameDecoded 在视频已播 1-2 秒时触发
            //      → currentTime=0 拉回开头 → 表现为"播 1-2 秒循环回 0"。
            // 这个守卫专治"滑到第二个视频就卡 1-2 秒循环"的 bug。
            if (targetIdx === activeIndexRef.current) {
              // 仅做幂等的就绪状态同步（attachHlsIfNeeded 也会读 preloadedReadyRef）
              return;
            }

            // 检查实际的 video 元素是否已存在
            const actualVideo = videoRefs.current[targetIdx];
            // Safari 下不直接 attach 到实际 video 元素，改用隐藏 video，原因：
            // Safari 中 play() 会为 video 元素创建 GPU 合成层，
            // 该层忽略 CSS z-index，会渲染在黑色封面遮罩上方，导致闪烁。
            // 非 Safari 下直接 attach 实际元素，切换时无需重新加载数据。
            let shouldUseActualVideo = actualVideo && actualVideo.parentElement && !isSafariRef.current;
            
            if (shouldUseActualVideo) {
              // 🔧 关键修复：如果实际的 video 元素已存在，直接 attach 到实际的 video 元素
              // 这样切换时就不需要重新加载数据，可以立即显示第一帧
              try {
                // 检查是否已经有 HLS 实例 attach 到这个 video 元素
                const existingHls = hlsMapRef.current.get(targetIdx);
                // ⚠️ 关键守卫：如果 existingHls 就是当前实例 h（可能是 attachHlsIfNeeded
                // 的 Case B 已把本 prefetch 实例迁移到 hlsMapRef），绝不能 destroy 自己，
                // 否则会断流；此时直接复用，跳过 attach。
                if (existingHls && existingHls !== h) {
                  try { existingHls.destroy(); } catch (_) {}
                }

                // 仅在实例尚未 attach 到目标 video 时才 attach（避免重复 attach
                // 触发 buffer 清空 → 视频回退/卡顿）
                if (h.media !== actualVideo) {
                  h.attachMedia(actualVideo);
                }
                hlsMapRef.current.set(targetIdx, h);
                hlsPrefetchRef.current.delete(targetIdx);
                
                // 步骤3：开始加载第一个 TS 片段（预解码首帧）
                if (preDecodeFirstFrame) {
                  h.startLoad();
                  
                  // 监听第一个片段加载完成
                  const onFirstFragLoaded = (event, data) => {
                    // ⚠️ 触发瞬间再次检查 active：用户可能在 manifest→FRAG 之间滑过来了。
                    // active 已变 → 完全跳过：不 play、不注册 listener，让 playAt 接管。
                    if (targetIdx === activeIndexRef.current) {
                      h.off(Hls.Events.FRAG_LOADED, onFirstFragLoaded);
                      return;
                    }

                    // 仅对非活跃视频触发预解码 play（Safari 不 play，避免 GPU 合成层闪烁）
                    if (!isSafariRef.current) {
                      try {
                        const playPromise = actualVideo.play();
                        if (playPromise && typeof playPromise.then === 'function') {
                          playPromise.catch(() => {});
                        }
                      } catch (_) {}
                    }

                    // 监听 loadedmetadata 和 canplay 事件，确认首帧已解码
                    const markFirstFrameDecoded = () => {
                      // ⚠️ 事件触发时再做一次 active 检查；若用户已滑过来，
                      // 任何 pause/reset/setVideoStateAt 都不能动 active 视频的状态。
                      if (targetIdx === activeIndexRef.current) return;
                      preloadedFirstFrameRef.current.add(targetIdx);
                      try {
                        actualVideo.pause();
                        actualVideo.currentTime = 0;
                        // 预加载阶段不应设置 ready=true（会污染非活动视频状态）
                        // 仅标记加载态，ready 交由活动视频的 canplay/playing 时机设置
                        setVideoStateAt(targetIdx, { delayed: false, loading: false });
                      } catch (_) {}
                    };
                    
                    actualVideo.addEventListener('loadedmetadata', markFirstFrameDecoded, { once: true });
                    actualVideo.addEventListener('canplay', markFirstFrameDecoded, { once: true });
                    
                    // 只监听第一个片段
                    h.off(Hls.Events.FRAG_LOADED, onFirstFragLoaded);
                  };
                  
                  h.on(Hls.Events.FRAG_LOADED, onFirstFragLoaded);
                } else {
                  // 不预解码首帧，只加载 manifest
                  h.startLoad();
                }
              } catch (_) {
                // 如果 attach 失败，回退到使用隐藏 video 的方式
                shouldUseActualVideo = false;
              }
            }
            
            // 🔧 如果实际的 video 元素不存在，使用隐藏的 video 元素进行预解码首帧
            if (!shouldUseActualVideo && preDecodeFirstFrame) {
              try {
                const hiddenVideo = document.createElement('video');
                hiddenVideo.style.cssText = 'position: absolute; width: 1px; height: 1px; opacity: 0; pointer-events: none; z-index: -9999;';
                hiddenVideo.muted = true;
                hiddenVideo.playsInline = true;
                hiddenVideo.preload = 'auto';
                document.body.appendChild(hiddenVideo);
                hiddenVideoRefs.current.set(targetIdx, hiddenVideo);
                
                // 将 HLS 实例 attach 到隐藏的 video 元素
                h.attachMedia(hiddenVideo);
                
                // 🔧 步骤3：开始加载第一个 TS 片段（预解码首帧）
                h.startLoad();
                
                // 监听第一个片段加载完成
                const onFirstFragLoaded = (event, data) => {
                  // 尝试播放以触发解码
                  try {
                    const playPromise = hiddenVideo.play();
                    if (playPromise && typeof playPromise.then === 'function') {
                      playPromise.catch(() => {});
                    }
                  } catch (_) {}
                  
                  // 监听 loadedmetadata 和 canplay 事件，确认首帧已解码
                  const markFirstFrameDecoded = () => {
                    preloadedFirstFrameRef.current.add(targetIdx);
                    try {
                      hiddenVideo.pause();
                      hiddenVideo.currentTime = 0;
                    } catch (_) {}
                  };
                  
                  hiddenVideo.addEventListener('loadedmetadata', markFirstFrameDecoded, { once: true });
                  hiddenVideo.addEventListener('canplay', markFirstFrameDecoded, { once: true });
                  
                  // 只监听第一个片段
                  h.off(Hls.Events.FRAG_LOADED, onFirstFragLoaded);
                };
                
                h.on(Hls.Events.FRAG_LOADED, onFirstFragLoaded);
              } catch (_) {
                // 预解码失败不影响预加载，仍然标记为已预加载
                h.startLoad();
              }
            } else if (!shouldUseActualVideo) {
              // 不预解码首帧，只加载 manifest
              h.startLoad();
            }
          });
          
          h.once(Hls.Events.ERROR, (event, data) => {
            // 如果预加载失败，从预加载列表中移除
            if (data.fatal) {
              preloadedVideosRef.current.delete(targetIdx);
              preloadedReadyRef.current.delete(targetIdx);
              preloadedFirstFrameRef.current.delete(targetIdx);
              hlsPrefetchRef.current.delete(targetIdx);
              cleanupHiddenVideo(targetIdx);
              try { h.destroy(); } catch (_) {}
            }
          });
          
          h.loadSource(src);
        }
        preloadedVideosRef.current.add(targetIdx);
      } catch (_) {
      } finally {
        preloadTimersRef.current.delete(targetIdx);
      }
    }, Math.max(0, prefetchNextDelayMs));
    
    preloadTimersRef.current.set(targetIdx, timer);
  }, [prefetchNextDelayMs, updateVideoCache, cleanupHiddenVideo]);

  /**
   * 🔧 智能预加载：预加载下一个（甚至下下个）视频
   * @param {number} currentIdx - 当前视频索引
   */
  const preloadVideos = useCallback((currentIdx) => {
    const currentItems = itemsRef.current;
    const nextIdx = currentIdx + 1;
    const nextNextIdx = currentIdx + 2;

    // 收敛为短视频平台常见策略：优先下一条，网络好时非 Safari 再预热下下条。
    // Safari 下不再预解码下一条首帧，避免隐藏 video/HLS 额外占用内存导致标签页被系统回收。
    if (nextIdx >= 0 && nextIdx < currentItems.length) {
      preloadSingleVideo(nextIdx, !isSafariRef.current);
    }

    if (!isSafariRef.current) {
      try {
        const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection;
        if (connection) {
          const effectiveType = connection.effectiveType;
          const downlink = connection.downlink;
          if ((effectiveType === '4g' || downlink > 2) && nextNextIdx >= 0 && nextNextIdx < currentItems.length) {
            preloadSingleVideo(nextNextIdx, false);
          }
        } else if (nextNextIdx >= 0 && nextNextIdx < currentItems.length) {
          preloadSingleVideo(nextNextIdx, false);
        }
      } catch (_) {
        if (nextNextIdx >= 0 && nextNextIdx < currentItems.length) {
          preloadSingleVideo(nextNextIdx, false);
        }
      }
    }

    const maxPreloadDistance = isSafariRef.current ? 1 : 2;
    hlsPrefetchRef.current.forEach((hls, idx) => {
      if (Math.abs(idx - currentIdx) > maxPreloadDistance) {
        try {
          const hiddenVideo = hiddenVideoRefs.current.get(idx);
          if (hiddenVideo && hls.media === hiddenVideo) {
            hls.detachMedia();
          }
          hls.destroy();
        } catch (_) {}
        hlsPrefetchRef.current.delete(idx);
        cleanupHiddenVideo(idx);
        preloadedVideosRef.current.delete(idx);
        preloadedReadyRef.current.delete(idx);
        preloadedFirstFrameRef.current.delete(idx);
      }
    });
  }, [preloadSingleVideo, cleanupHiddenVideo]);

  /**
   * 🔧 辅助函数：创建视频播放就绪事件处理器
   * @param {number} idx - 视频索引
   * @param {HTMLVideoElement} video - video 元素
   * @returns {Function} canplay 事件处理器
   */

  const attachHlsIfNeeded = useCallback((idx) => {
    const v = videoRefs.current[idx];
    if (!v) return;
    const item = (itemsRef.current[idx]?.data || itemsRef.current[idx]) || {};
    const src = item.firstVideoUrl || '';
    if (!src) return;

    if (Hls.isSupported()) {
      const pref = hlsPrefetchRef.current.get(idx);
      if (pref) {
        try {
          // ── Case A: pref 已经 attach 到当前 video 元素 ─────────────────
          if (pref.media === v) {
            if (v.src?.startsWith('blob:')) {
              // MSE 有效 → 将 pref 提升到 hlsMapRef
              hlsMapRef.current.set(idx, pref);
              hlsPrefetchRef.current.delete(idx);
              if (preloadedFirstFrameRef.current.has(idx)) {
                const quick = Date.now() - lastSwitchTimeRef.current < quickSwitchThresholdMs;
                // 同 requestFirstFrame：仅在视频未起播且未 reset 过时才拉回 0，
                // 避免视频已经开始播放后被二次 attach 的副作用拉回开头
                if (
                  !quick &&
                  v.paused &&
                  v.currentTime !== 0 &&
                  !firstFrameSetRef.current.has(idx)
                ) {
                  try { v.currentTime = 0; } catch (_) {}
                  firstFrameSetRef.current.add(idx);
                }
                setVideoStateAt(idx, { delayed: false, loading: false, ready: true });
              }
              return;
            }
            // MSE 已断开 → 销毁后回退到重建流程
            try { pref.destroy(); } catch (_) {}
            hlsPrefetchRef.current.delete(idx);
            [preloadedVideosRef, preloadedReadyRef, preloadedFirstFrameRef].forEach(r => r.current.delete(idx));
          }

          // ── Case B: pref 存在但未 attach 到当前 video → 迁移 attach ──
          if (!hlsPrefetchRef.current.has(idx)) throw new Error('destroyed'); // 已被上面删掉

          // 清理：从隐藏 video 上 detach
          const hiddenVideo = hiddenVideoRefs.current.get(idx);
          if (hiddenVideo && pref.media === hiddenVideo) {
            try { pref.detachMedia(); cleanupHiddenVideo(idx); } catch (_) {}
          }
          // 清理：移除其他错误 attach 到当前 video 的 HLS 实例
          hlsMapRef.current.forEach((h, k) => {
            if (k !== idx && h?.media === v) {
              try { h.detachMedia(); h.destroy(); hlsMapRef.current.delete(k); } catch (_) {}
            }
          });
          hlsPrefetchRef.current.forEach((h, k) => {
            if (k !== idx && h?.media === v) {
              try { h.detachMedia(); h.destroy(); hlsPrefetchRef.current.delete(k); } catch (_) {}
            }
          });
          // 清理旧的 map 条目
          const old = hlsMapRef.current.get(idx);
          if (old) {
            try { if (old.media === v) old.detachMedia(); old.destroy(); } catch (_) {}
            hlsMapRef.current.delete(idx);
          }

          // 正式 attach 到当前 video
          pref.attachMedia(v);
          hlsMapRef.current.set(idx, pref);
          hlsPrefetchRef.current.delete(idx);

          const isManifest    = preloadedReadyRef.current.has(idx) || (pref.levels?.length > 0);
          const isFirstFrame  = preloadedFirstFrameRef.current.has(idx);

          // 同步 preloadedReadyRef
          if (pref.levels?.length > 0 && !preloadedReadyRef.current.has(idx)) {
            preloadedReadyRef.current.add(idx);
            updateVideoCache(idx, true);
          }
          // 清除加载 timer
          const lt = loadingTimersRef.current.get(idx);
          if (isManifest && lt) { clearTimeout(lt); loadingTimersRef.current.delete(idx); }

          // 启动加载
          try { pref.stopLoad(); } catch (_) {}
          pref.startLoad();

          // 首帧已解码：设置 currentTime=0（非快速切换、首次播放时）并标 ready
          if (isFirstFrame) {
            const quick = Date.now() - lastSwitchTimeRef.current < quickSwitchThresholdMs;
            if (!quick && !firstFrameSetRef.current.has(idx)) {
              const st = videoStateRef.current[idx];
              if (!(st?.ready || v.currentTime > 0) && v.currentTime !== 0) {
                try { v.currentTime = 0; } catch (_) {}
              }
              firstFrameSetRef.current.add(idx);
            }
            setVideoStateAt(idx, { delayed: false, loading: false, ready: true });
          }

          // 注册 ready 事件（幂等：handled 标志防止重复触发）
          let handled = false;
          const onReady = () => {
            if (handled) return; handled = true;
            if (idx === activeIndexRef.current) {
              setVideoStateAt(idx, { delayed: false, loading: false, ready: true });
            }
          };
          v.addEventListener('loadedmetadata', onReady, { once: true });
          v.addEventListener('canplay',        onReady, { once: true });
          if (!isFirstFrame) pref.once(Hls.Events.FRAG_LOADED, onReady);

          // 若 manifest 尚未解析，仅等待加载完成
          if (!isManifest) try { pref.startLoad(); } catch (_) {}

          return;
        } catch (_) {}
      }

      // ── No pref: 检查 hlsMapRef 或新建实例 ─────────────────────────────
      let old = hlsMapRef.current.get(idx);
      if (old?.media === v) {
        if (v.src?.startsWith('blob:')) {
          // MSE 有效，只需确保在加载中
          if (old.levels?.length > 0) try { old.startLoad(); } catch (_) {}
          return;
        }
        // MSE 断开 → 销毁重建
        try { old.destroy(); } catch (_) {}
        hlsMapRef.current.delete(idx);
        old = null;
      }
      if (old) { try { old.destroy(); } catch (_) {} }

      const hls = new Hls({ ...HLS_PLAYBACK_CONFIG, xhrSetup: hlsXhrSetup });
      hls.loadSource(src);
      hls.attachMedia(v);
      try { v.load?.(); } catch (_) {}
      hlsMapRef.current.set(idx, hls);

    } else if (v.canPlayType?.('application/vnd.apple.mpegurl')) {
      // 旧版 iOS Safari：原生 HLS，需重写 m3u8 中 key URI 绕过鉴权
      const idxCopy = idx, vCopy = v;
      patchNativeHlsM3u8(src).then(({ url: patchedUrl, isBlob }) => {
        if (videoRefs.current[idxCopy] !== vCopy) {
          if (isBlob) URL.revokeObjectURL(patchedUrl);
          return;
        }
        const oldBlob = nativeBlobUrlsRef.current.get(idxCopy);
        if (oldBlob) try { URL.revokeObjectURL(oldBlob); } catch (_) {}
        if (isBlob) nativeBlobUrlsRef.current.set(idxCopy, patchedUrl);
        else nativeBlobUrlsRef.current.delete(idxCopy);
        vCopy.src = patchedUrl;
        try { vCopy.load?.(); } catch (_) {}
      });
    }
  }, [quickSwitchThresholdMs, setVideoStateAt, activeIndexRef, updateVideoCache, cleanupHiddenVideo]);


  const markReady = useCallback((idx) => {
    // 只允许当前活动视频进入 ready，避免预加载/后台事件提前改写状态
    if (idx !== activeIndexRef.current) return;
    try {
      requestAnimationFrame(() => {
        setVideoStateAt(idx, { ready: true });
      });
    } catch (_) {
      setVideoStateAt(idx, { ready: true });
    }
  }, [activeIndexRef, setVideoStateAt]);

  const requestFirstFrame = useCallback((idx) => {
    const v = videoRefs.current[idx];
    if (!v) return;
    
    // 🔧 优化：检查是否是快速切换，如果是则禁用第一帧设置
    const timeSinceLastSwitch = Date.now() - lastSwitchTimeRef.current;
    const isQuickSwitch = timeSinceLastSwitch < quickSwitchThresholdMs;
    
    try {
      // 🔧 关键修复：如果首帧已预解码，立即显示第一帧
      const isPreloadedFirstFrame = preloadedFirstFrameRef.current.has(idx);
      if (isPreloadedFirstFrame) {
        try {
          // ⚠️ 关键守卫：reset 到 0 仅在「视频还未真正开始播放」时才执行。
          // canplay 事件在 buffer 空 / HLS level 切换 / seek 后会被二次触发；
          // 若此时 video 已经播了几秒，盲目 reset 会把 currentTime 拉回 0 →
          // 表现为「滑动后视频播 1-2 秒又循环回开头」。
          // 用 firstFrameSetRef 保证每个 idx 只 reset 一次，video.paused
          // 保证仅在尚未起播的窗口内执行。
          if (
            !isQuickSwitch &&
            v.paused &&
            v.currentTime !== 0 &&
            !firstFrameSetRef.current.has(idx)
          ) {
            v.currentTime = 0;
            firstFrameSetRef.current.add(idx);
          }
        } catch (_) {}
        markReady(idx);
        return;
      }
      
      // 🔧 优化：快速切换时，不调用 requestVideoFrameCallback，避免闪烁
      if (!isQuickSwitch && typeof v.requestVideoFrameCallback === 'function') {
        v.requestVideoFrameCallback(() => markReady(idx));
      } else {
        markReady(idx);
      }
    } catch (_) {
      markReady(idx);
    }
  }, [markReady, quickSwitchThresholdMs]);

  const manageLoadingIndicator = useCallback((idx, isLoading) => {
    const currentTime = Date.now();
    const timeSinceLastSwitch = currentTime - lastSwitchTimeRef.current;
    const isQuickSwitch = timeSinceLastSwitch < quickSwitchThresholdMs;

    const existingTimer = loadingTimersRef.current.get(idx);
    if (existingTimer) {
      clearTimeout(existingTimer);
      loadingTimersRef.current.delete(idx);
    }

    if (isLoading) {
      const cached = videoCacheRef.current.get(idx);
      if (cached && cached.loaded) {
        return;
      }
      
      // 🔧 修复：检查预加载是否真正完成（manifest已解析）
      const isPreloadedReady = preloadedReadyRef.current.has(idx);
      const hasHlsPrefetch = hlsPrefetchRef.current.has(idx);
      // 🔧 关键修复：检查已 attach 的 HLS 实例是否已解析 manifest
      const hlsInstance = hlsMapRef.current.get(idx);
      const hasHlsWithLevels = hlsInstance && hlsInstance.levels && hlsInstance.levels.length > 0;
      
      // 🔧 修复：如果有预加载的 HLS 实例或已 attach 的 HLS 实例已解析 manifest，检查视频元素是否已有数据
      if (hasHlsPrefetch || isPreloadedReady || hasHlsWithLevels) {
        const v = videoRefs.current[idx];
        if (v) {
          try {
            const readyState = Number(v.readyState || 0);
            // 如果视频已有当前数据或更高，说明预加载已生效，不显示加载指示器
            if (Number.isFinite(readyState) && readyState >= HAVE_CURRENT_DATA) {
              return;
            }
            // 🔧 修复：如果预加载已完成（manifest已解析），即使视频还没有数据，也不显示加载指示器（避免闪烁）
            // 但只在快速切换时不显示，正常切换时如果视频确实没有数据，应该显示加载指示器
            if ((isPreloadedReady || hasHlsWithLevels) && !isQuickSwitch) {
              // 预加载已完成但视频还没有数据，等待一下再检查
              // 如果500ms后还没有数据，再显示加载指示器
              const checkTimer = setTimeout(() => {
                const v2 = videoRefs.current[idx];
                if (v2) {
                  const readyState2 = Number(v2.readyState || 0);
                  if (Number.isFinite(readyState2) && readyState2 < HAVE_CURRENT_DATA && idx === activeIndexRef.current) {
                    setVideoStateAt(idx, { delayed: true, loading: true });
                  }
                }
              }, 500);
              loadingTimersRef.current.set(idx, checkTimer);
              return;
            }
            // 快速切换时，如果预加载已完成，不显示加载指示器
            if (isPreloadedReady || hasHlsWithLevels) {
              return;
            }
          } catch (_) {}
        } else if (isPreloadedReady || hasHlsWithLevels) {
          // 预加载已完成但 video 元素还未 attach，也不显示加载指示器
          return;
        }
      }
      
      // 🔧 修复：快速切换时，如果视频确实在加载，仍然显示加载指示器（但延迟更短）
      if (isQuickSwitch) {
        // 快速切换时，延迟更短（400ms）显示加载指示器
        const timer = setTimeout(() => {
          // 再次检查视频状态
          const v = videoRefs.current[idx];
          if (v && idx === activeIndexRef.current) {
            const readyState = Number(v.readyState || 0);
            if (Number.isFinite(readyState) && readyState < HAVE_CURRENT_DATA) {
              setVideoStateAt(idx, { delayed: true, loading: true });
            }
          }
          loadingTimersRef.current.delete(idx);
        }, Math.max(0, loadingIndicatorDelayMs - 400));
        loadingTimersRef.current.set(idx, timer);
        return;
      }
      
      const timer = setTimeout(() => {
        setVideoStateAt(idx, { delayed: true, loading: true });
        loadingTimersRef.current.delete(idx);
      }, Math.max(0, loadingIndicatorDelayMs));
      loadingTimersRef.current.set(idx, timer);
    } else {
      setVideoStateAt(idx, { delayed: false, loading: false });
      updateVideoCache(idx, true);
    }
  }, [loadingIndicatorDelayMs, quickSwitchThresholdMs, setVideoStateAt, updateVideoCache]);

  const playAt = useCallback((idx) => {
    lastSwitchTimeRef.current = Date.now();

    // ── 1. 清理所有非当前视频的待处理定时器 ──────────────────────────────
    [loadingTimersRef, unmuteTimersRef].forEach(mapRef => {
      mapRef.current.forEach((t, k) => {
        if (k !== idx) { clearTimeout(t); mapRef.current.delete(k); }
      });
    });
    playTimeoutsRef.current.forEach((t, k) => {
      const kIdx = typeof k === 'string' ? parseInt(k.replace('check_', ''), 10) : k;
      if (kIdx !== idx) { clearTimeout(t); playTimeoutsRef.current.delete(k); }
    });
    // 清理所有播放卡住检测定时器（新视频会按需重启）
    playCheckTimersRef.current.forEach(t => clearInterval(t));
    playCheckTimersRef.current.clear();

    // ── 2. 清理非当前视频的事件监听器 ────────────────────────────────────
    playEventListenersRef.current.forEach((ls, k) => {
      if (k !== idx && ls?.video) {
        const { video, onCanPlayThrough, onCanPlay, onLoadedMetadata, onPlaying } = ls;
        try {
          if (onCanPlayThrough) video.removeEventListener('canplaythrough', onCanPlayThrough);
          if (onCanPlay)        video.removeEventListener('canplay', onCanPlay);
          if (onLoadedMetadata) video.removeEventListener('loadedmetadata', onLoadedMetadata);
          if (onPlaying)        video.removeEventListener('playing', onPlaying);
        } catch (_) {}
        playEventListenersRef.current.delete(k);
      }
    });

    // ── 3. 解决当前视频可能存在的 HLS 实例冲突 ───────────────────────────
    const currentVideo = videoRefs.current[idx];
    if (currentVideo) {
      try {
        const existingHls = hlsMapRef.current.get(idx);
        const prefetchHls  = hlsPrefetchRef.current.get(idx);

        // 若 prefetch 与 map 中是不同实例：以已 attach 到当前 video 的为准
        if (existingHls && prefetchHls && existingHls !== prefetchHls) {
          if (prefetchHls.media === currentVideo) {
            try { existingHls.destroy(); } catch (_) {}
            hlsMapRef.current.set(idx, prefetchHls);
            hlsPrefetchRef.current.delete(idx);
          } else {
            try { prefetchHls.destroy(); } catch (_) {}
            hlsPrefetchRef.current.delete(idx);
          }
        }
        // map 中的实例 attach 到了错误的 video 元素，清理掉
        if (existingHls && existingHls.media !== currentVideo) {
          try { existingHls.detachMedia(); existingHls.destroy(); } catch (_) {}
          hlsMapRef.current.delete(idx);
        }

        // 若视频已在播放且 HLS 已正确 attach，跳过 pause()，避免中断播放
        const hlsOk = (() => {
          const eh = hlsMapRef.current.get(idx);
          const ph = hlsPrefetchRef.current.get(idx);
          return (eh && eh.media === currentVideo) || (ph && ph.media === currentVideo);
        })();
        const isAlreadyPlaying = !currentVideo.paused && currentVideo.readyState >= HAVE_CURRENT_DATA;
        if (!isAlreadyPlaying || !hlsOk) currentVideo.pause();

        // 确保已 attach 的 HLS 实例正在加载
        const attachedHls = hlsMapRef.current.get(idx) || hlsPrefetchRef.current.get(idx);
        if (attachedHls && attachedHls.media === currentVideo && attachedHls.levels?.length > 0) {
          try { attachedHls.startLoad(); } catch (_) {}
        }
      } catch (_) {}
    }

    // ── 4. 暂停其他视频，远距离视频回收（批量更新 ref，一次性同步到 React state）
    // 用累积对象收集所有 ref 变更，最后一次性 assign，避免 forEach 内反复 spread（O(N²)→O(N)）
    const refUpdates = {};
    const statePatches = {}; // 仅收集 ±1 可见范围需同步到 React state 的变更

    // 处理当前视频：清除 paused 标志
    const curActive = videoStateRef.current[idx] || DEFAULT_VIDEO_STATE;
    if (curActive.paused) {
      const next = { ...curActive, paused: false };
      refUpdates[idx] = next;
      statePatches[idx] = next; // 当前 slide 始终在可见范围
    }

    videoRefs.current.forEach((el, i) => {
      if (!el || i === idx) return;
      try {
        el.pause();
        const cur = videoStateRef.current[i] || DEFAULT_VIDEO_STATE;
        if (!cur.paused) {
          const next = { ...cur, paused: true };
          refUpdates[i] = next;
          if (Math.abs(i - idx) <= 1) statePatches[i] = next;
        }
        // 超出保留范围：销毁 HLS、清空 src 释放内存
        if (Math.abs(i - idx) > farRecycleDistance + 1) {
          const inst = hlsMapRef.current.get(i);
          if (inst) { try { inst.destroy(); } catch (_) {} hlsMapRef.current.delete(i); }
          const pref = hlsPrefetchRef.current.get(i);
          if (pref) { try { pref.destroy(); } catch (_) {} hlsPrefetchRef.current.delete(i); }
          cleanupHiddenVideo(i);
          preloadedVideosRef.current.delete(i);
          preloadedReadyRef.current.delete(i);
          preloadedFirstFrameRef.current.delete(i);
          firstFrameSetRef.current.delete(i);
          try { el.src = ''; el.load(); } catch (_) {}
          refUpdates[i] = { ready: false, paused: true, loading: false, delayed: false };
          // 非可见 slide，不加入 statePatches（不触发 React re-render）
        }
      } catch (_) {}
    });

    // 一次性更新 ref（O(N)），再一次 setVideoState 覆盖所有可见变更
    if (Object.keys(refUpdates).length > 0) {
      videoStateRef.current = { ...videoStateRef.current, ...refUpdates };
    }
    if (Object.keys(statePatches).length > 0) {
      setVideoState(prev => ({ ...(prev || {}), ...statePatches }));
    }

    // ── 5. 跳过广告条目 ───────────────────────────────────────────────────
    const currentItem = itemsRef.current[idx];
    if (!currentItem || currentItem.type === 'ad') return;
    if (!videoRefs.current[idx]) return;

    // ── 6. 触发预加载 + HLS attach ────────────────────────────────────────
    preloadVideos(idx);
    attachHlsIfNeeded(idx);

    const v = videoRefs.current[idx];
    if (!v) return;

    // ── 7. 判断就绪状态 ───────────────────────────────────────────────────
    const hls             = hlsMapRef.current.get(idx);
    const isManifestReady = preloadedReadyRef.current.has(idx) || (hls?.levels?.length > 0);
    const isFirstFrameReady = preloadedFirstFrameRef.current.has(idx);
    const hasData = v.readyState >= HAVE_CURRENT_DATA ||
                    v.currentTime > 0 ||
                    (videoStateRef.current[idx] || {}).ready ||
                    isFirstFrameReady;

    // 若 HLS manifest 已解析但 preloadedReadyRef 未同步，补充标记
    if (hls?.levels?.length > 0 && !preloadedReadyRef.current.has(idx)) {
      preloadedReadyRef.current.add(idx);
      updateVideoCache(idx, true);
    }
    // 清除加载 timer（manifest 已就绪时 loading 指示器不需要延迟）
    if (isManifestReady) {
      const lt = loadingTimersRef.current.get(idx);
      if (lt) { clearTimeout(lt); loadingTimersRef.current.delete(idx); }
    }

    // ── 8. 更新 UI 就绪状态 ───────────────────────────────────────────────
    if (hasData) {
      setVideoStateAt(idx, { delayed: false, loading: false, ready: true });
    } else if (isManifestReady) {
      setVideoStateAt(idx, { delayed: true, loading: true });
      manageLoadingIndicator(idx, true);
    } else {
      manageLoadingIndicator(idx, true);
    }

    // ── 9. 设置静音和播放属性 ─────────────────────────────────────────────
    const muted = isMutedRef.current;
    const shouldMute = muted && idx !== 0; // 第一个视频刷新页面时不静音
    try {
      v.muted = shouldMute;
      if (!shouldMute) {
        try { v.removeAttribute('muted'); } catch (_) {}
        try { v.volume = 1.0; } catch (_) {}
      } else {
        try { v.setAttribute('muted', 'true'); } catch (_) {}
      }
      v.playsInline = true;
      v.setAttribute('playsinline', 'true');
      v.setAttribute('webkit-playsinline', 'true');
    } catch (_) {}

    // ── 10. 启动播放 ──────────────────────────────────────────────────────
    if (isManifestReady) {
      // manifest 已就绪：确保 HLS 正在加载
      if (hls?.levels?.length > 0) { try { hls.startLoad(); } catch (_) {} }

      // 首帧已解码 → 立即标记 ready
      if (isFirstFrameReady) {
        setVideoStateAt(idx, { delayed: false, loading: false, ready: true });
      }

      // 尝试立即播放；未能播放则注册事件监听等待数据就绪
      const tryPlay = (forcePlay = false) => {
        if (idx !== activeIndexRef.current) return false;
        const rs = v.readyState || 0;
        if (rs >= HAVE_CURRENT_DATA && (rs >= HAVE_ENOUGH_DATA || forcePlay)) {
          if (v.paused) playWithCatch(v, idx);
          const t = playTimeoutsRef.current.get(idx);
          if (t) { clearTimeout(t); playTimeoutsRef.current.delete(idx); }
          return true;
        }
        return false;
      };

      if (!tryPlay()) {
        // 数据不足 → 显示 loading spinner，500ms 后强制播放
        if (v.readyState < HAVE_CURRENT_DATA) {
          setVideoStateAt(idx, { loading: true, delayed: true });
          manageLoadingIndicator(idx, true);
        }
        const t = setTimeout(() => {
          if (idx === activeIndexRef.current) tryPlay(true);
          playTimeoutsRef.current.delete(idx);
        }, 500);
        playTimeoutsRef.current.set(idx, t);
      }

      // rAF 再尝试一次（确保 attachMedia 后的首帧渲染）
      requestAnimationFrame(() => { if (idx === activeIndexRef.current) tryPlay(); });

      // ── 事件监听：canplay/canplaythrough → play；playing → 清理定时器 ──
      const cleanup = (key) => {
        const ls = playEventListenersRef.current.get(idx);
        if (ls) ls[key] = null;
      };
      const onCanPlayThrough = () => {
        const t = playTimeoutsRef.current.get(idx);
        if (t) { clearTimeout(t); playTimeoutsRef.current.delete(idx); }
        if (idx === activeIndexRef.current) tryPlay();
        cleanup('onCanPlayThrough');
      };
      const onCanPlay = () => {
        if (idx === activeIndexRef.current) {
          if (!tryPlay()) setTimeout(() => { if (idx === activeIndexRef.current) tryPlay(true); }, 200);
        }
        cleanup('onCanPlay');
      };
      const onLoadedMetadata = () => {
        if (idx === activeIndexRef.current) {
          setVideoStateAt(idx, { delayed: false, loading: false, ready: true });
          if (!tryPlay()) setTimeout(() => { if (idx === activeIndexRef.current) tryPlay(true); }, 200);
        }
        cleanup('onLoadedMetadata');
      };
      const onPlaying = () => {
        if (idx === activeIndexRef.current) {
          setVideoStateAt(idx, { paused: false });
          // 视频已开始播放，清理卡住检测
          const ct = playCheckTimersRef.current.get(idx);
          if (ct) { clearInterval(ct); playCheckTimersRef.current.delete(idx); }
          const cto = playTimeoutsRef.current.get(`check_${idx}`);
          if (cto) { clearTimeout(cto); playTimeoutsRef.current.delete(`check_${idx}`); }
        }
        cleanup('onPlaying');
      };

      // 清理旧监听器后注册新的
      const old = playEventListenersRef.current.get(idx);
      if (old?.video) {
        try {
          if (old.onCanPlayThrough) old.video.removeEventListener('canplaythrough', old.onCanPlayThrough);
          if (old.onCanPlay)        old.video.removeEventListener('canplay', old.onCanPlay);
          if (old.onLoadedMetadata) old.video.removeEventListener('loadedmetadata', old.onLoadedMetadata);
          if (old.onPlaying)        old.video.removeEventListener('playing', old.onPlaying);
        } catch (_) {}
      }
      v.addEventListener('canplaythrough', onCanPlayThrough, { once: true });
      v.addEventListener('canplay',        onCanPlay,        { once: true });
      v.addEventListener('loadedmetadata', onLoadedMetadata, { once: true });
      v.addEventListener('playing',        onPlaying,        { once: true });
      playEventListenersRef.current.set(idx, { video: v, onCanPlayThrough, onCanPlay, onLoadedMetadata, onPlaying });

      // ── Safari 卡住检测：有数据但播放不推进 → 重试 play ──────────────────
      // timeSinceSwitch ≈ 0（lastSwitchTimeRef 刚被更新），isQuickSwitch = true，
      // 所以 !isQuickSwitch = false；仅 isSafariRef 时才启动检测。
      if (isSafariRef.current) {
        const checkDelay = 800;
        const cto = setTimeout(() => {
          if (idx !== activeIndexRef.current) { playTimeoutsRef.current.delete(`check_${idx}`); return; }
          const cv = videoRefs.current[idx];
          if (!cv || cv.readyState < HAVE_CURRENT_DATA) { playTimeoutsRef.current.delete(`check_${idx}`); return; }

          let lastTime = cv.currentTime || 0;
          let checkCount = 0;
          let stuckCount = 0;
          const timer = setInterval(() => {
            if (idx !== activeIndexRef.current || !videoRefs.current[idx]) {
              clearInterval(timer); playCheckTimersRef.current.delete(idx); return;
            }
            const cv2 = videoRefs.current[idx];
            checkCount++;
            const ct2  = cv2.currentTime || 0;
            const rs2  = cv2.readyState  || 0;
            const hasD = rs2 >= HAVE_CURRENT_DATA;

            if (!cv2.paused && hasD) {
              stuckCount = ct2 === lastTime ? stuckCount + 1 : 0;
              if (stuckCount >= 3 && checkCount > 3) {
                try { cv2.play().catch(() => setVideoStateAt(idx, { loading: true, delayed: true })); } catch (_) {}
                stuckCount = 0;
              }
            } else if (cv2.paused && hasD && checkCount >= 2) {
              // Safari 有数据但暂停 = 卡住，重试 play
              try { cv2.play().catch(() => {}); } catch (_) {}
            }
            lastTime = ct2;
            if ((!cv2.paused && rs2 >= HAVE_ENOUGH_DATA) || checkCount >= 8) {
              clearInterval(timer); playCheckTimersRef.current.delete(idx);
            }
          }, 500);
          playCheckTimersRef.current.set(idx, timer);
          playTimeoutsRef.current.delete(`check_${idx}`);
        }, checkDelay);
        playTimeoutsRef.current.set(`check_${idx}`, cto);
      }
    } else if (hasData) {
      // 有现成数据（如切回已播放过的视频），直接 play
      if (v.paused) playWithCatch(v, idx);
    } else {
      // 无数据也无 manifest：走正常延迟启动
      setTimeout(() => {
        if (idx === activeIndexRef.current && v.paused) playWithCatch(v, idx);
      }, Math.max(0, autoPlayDelayMs));
    }

    // ── 11. 视频已就绪但被暂停（如切回）→ 补一次 play ────────────────────
    if ((videoStateRef.current[idx] || {}).ready && v.paused) {
      setTimeout(() => {
        if (idx === activeIndexRef.current && v.paused) playWithCatch(v, idx);
      }, 50);
    }

    // ── 12. 进度循环 ──────────────────────────────────────────────────────
    stopProgressLoop();
    startProgressLoop(idx);

    // 确保音量不被意外静音
    if (!muted || idx === 0) {
      try { v.muted = false; v.volume = 1.0; } catch (_) {}
    }
  }, [activeIndexRef, attachHlsIfNeeded, autoPlayDelayMs, farRecycleDistance, cleanupHiddenVideo, playWithCatch, preloadVideos, setVideoStateAt, startProgressLoop, stopProgressLoop, manageLoadingIndicator, updateVideoCache]);


  const registerVideoRef = useCallback((idx, node) => {
    videoRefs.current[idx] = node || null;
  }, []);

  const handleLoadStart = useCallback((idx) => {
    // 🔧 修复：如果预加载已完成，不触发加载指示器
    const isPreloadedReady = preloadedReadyRef.current.has(idx);
    const hasHlsPrefetch = hlsPrefetchRef.current.has(idx);
    
    if (isPreloadedReady) {
      // 预加载已完成，不显示加载指示器，也不设置加载状态
      const currentState = videoStateRef.current[idx] || {};
      if (currentState.loading || currentState.delayed) {
        setVideoStateAt(idx, { delayed: false, loading: false });
      }
      return;
    }
    
    // 如果有预加载的 HLS 实例，检查视频是否已有数据
    if (hasHlsPrefetch) {
      const v = videoRefs.current[idx];
      if (v) {
        try {
          const readyState = Number(v.readyState || 0);
          // 如果视频已有数据，说明预加载已生效，不显示加载指示器
          if (Number.isFinite(readyState) && readyState >= HAVE_CURRENT_DATA) {
            return;
          }
        } catch (_) {}
      }
    }
    
    manageLoadingIndicator(idx, true);
  }, [manageLoadingIndicator, setVideoStateAt]);

  const handleLoadedMetadata = useCallback((idx) => {
    const video = videoRefs.current[idx];
    if (video) {
      requestFirstFrame(idx);
    }
  }, [requestFirstFrame]);

  const handleCanPlay = useCallback((idx) => {
    manageLoadingIndicator(idx, false);
    const v = videoRefs.current[idx];
    if (v && idx === activeIndexRef.current) {
      const readyState = Number(v.readyState || 0);
      if (Number.isFinite(readyState) && readyState >= HAVE_CURRENT_DATA) {
        setVideoStateAt(idx, { ready: true, loading: false, delayed: false });
      }
    }
    errorRecoverTriedRef.current.delete(idx);
    requestFirstFrame(idx);
  }, [activeIndexRef, manageLoadingIndicator, requestFirstFrame, setVideoStateAt]);

  const handlePlaying = useCallback((idx) => {
    // 🔧 优化：视频真正开始播放时才清除加载状态
    manageLoadingIndicator(idx, false);
    markReady(idx);
    // 🔧 关键修复：使用 requestAnimationFrame 确保状态同步更新，避免闪烁
    requestAnimationFrame(() => {
      setVideoStateAt(idx, { loading: false, delayed: false, paused: false, ready: true });
    });
  }, [manageLoadingIndicator, markReady, setVideoStateAt]);

  const handlePause = useCallback((idx) => {
    setVideoStateAt(idx, { paused: true });
  }, [setVideoStateAt]);

  const handlePlay = useCallback((idx) => {
    setVideoStateAt(idx, { paused: false });
  }, [setVideoStateAt]);

  const handleWaiting = useCallback((idx) => {
    const v = videoRefs.current[idx];
    if (!v || idx !== activeIndexRef.current) return;
    
    const readyState = Number(v.readyState || 0);
    const networkState = Number(v.networkState || 0);
    
    // 只在真正缓冲不足时触发 loading 指示器（走 loadingIndicatorDelayMs 延迟，避免短暂卡顿闪烁）
    if (networkState === 2 || (readyState < HAVE_ENOUGH_DATA && networkState !== 3)) {
      manageLoadingIndicator(idx, true);
      // 不直接 setVideoStateAt delayed:true，由 manageLoadingIndicator 延迟后统一设置
      // 避免频繁 waiting/playing 切换导致 spinner 快速闪现
    }
  }, [activeIndexRef, manageLoadingIndicator]);

  const handleError = useCallback((idx) => {
    manageLoadingIndicator(idx, false);
    setVideoStateAt(idx, { ready: false, loading: false, delayed: false });
    const v = videoRefs.current[idx];
    if (!v) return;
    try {
      const src = v.currentSrc || v.src || v.getAttribute('src');
      const isHls = !!(src && /\.m3u8(\?|$)/i.test(src));
      if (isHls && !errorRecoverTriedRef.current.has(idx)) {
        errorRecoverTriedRef.current.add(idx);
        const old = src;
        try { v.removeAttribute('src'); } catch (_) {}
        try { v.load(); } catch (_) {}
        setTimeout(() => {
          try { v.setAttribute('src', old); v.load(); } catch (_) {}
        }, 400);
      }
    } catch (_) {}
  }, [manageLoadingIndicator, setVideoStateAt]);

  const handleClick = useCallback((idx) => {
    const v = videoRefs.current[idx];
    if (!v) return;
    try {
      if (v.paused) {
        // 🔧 修复：传递索引，确保静音恢复逻辑正确
        playWithCatch(v, idx);
        // 保持状态，等待实际播放事件
        setVideoStateAt(idx, { paused: false });
      } else {
        v.pause();
        setVideoStateAt(idx, { paused: true });
      }
    } catch (_) {}
  }, [playWithCatch, setVideoStateAt]);

  const syncMutedStateForIndex = useCallback((idx) => {
    const v = videoRefs.current[idx];
    if (!v) return;
    const muted = isMutedRef.current;
    // 🔧 修复：第一个视频（idx === 0）刷新页面时不应该静音
    const shouldMute = muted && idx !== 0;
    try {
      v.muted = shouldMute;
      if (!shouldMute) {
        try { v.removeAttribute('muted'); } catch (_) {}
        try { v.volume = 1.0; } catch (_) {}
      } else {
        try { v.setAttribute('muted', 'true'); } catch (_) {}
      }
    } catch (_) {}
  }, []);

  const cleanupPlayer = useCallback(() => {
    hlsMapRef.current.forEach(inst => { try { inst.destroy(); } catch (_) {} });
    hlsMapRef.current.clear();
    hlsPrefetchRef.current.forEach((inst, idx) => {
      try {
        const hiddenVideo = hiddenVideoRefs.current.get(idx);
        if (hiddenVideo && inst.media === hiddenVideo) {
          inst.detachMedia();
        }
        inst.destroy();
      } catch (_) {}
    });
    hlsPrefetchRef.current.clear();
    // 清理所有隐藏的 video 元素
    hiddenVideoRefs.current.forEach((hiddenVideo, idx) => {
      cleanupHiddenVideo(idx);
    });
    hiddenVideoRefs.current.clear();
    loadingTimersRef.current.forEach(timer => clearTimeout(timer));
    loadingTimersRef.current.clear();
    preloadTimersRef.current.forEach(timer => clearTimeout(timer));
    preloadTimersRef.current.clear();
    // 🔧 修复：清理静音恢复定时器
    unmuteTimersRef.current.forEach(timer => clearTimeout(timer));
    unmuteTimersRef.current.clear();
    // 🔧 关键修复：清理播放超时
    playTimeoutsRef.current.forEach(timeout => clearTimeout(timeout));
    playTimeoutsRef.current.clear();
    // 🔧 优化：清理播放检查定时器
    playCheckTimersRef.current.forEach(timer => clearInterval(timer));
    playCheckTimersRef.current.clear();
    // 🔧 关键修复：清理事件监听器
    playEventListenersRef.current.forEach((listeners) => {
      if (listeners && listeners.video) {
        try {
          const { video, onCanPlayThrough, onCanPlay, onLoadedMetadata, onPlaying } = listeners;
          if (onCanPlayThrough) video.removeEventListener('canplaythrough', onCanPlayThrough);
          if (onCanPlay) video.removeEventListener('canplay', onCanPlay);
          if (onLoadedMetadata) video.removeEventListener('loadedmetadata', onLoadedMetadata);
          if (onPlaying) video.removeEventListener('playing', onPlaying);
        } catch (_) {}
      }
    });
    playEventListenersRef.current.clear();
    stopProgressLoop();
    progressBarRefs.current = {};
    preloadedVideosRef.current.clear();
    preloadedReadyRef.current.clear();
    preloadedFirstFrameRef.current.clear();
    videoCacheRef.current.clear();
    videoStateRef.current = {};
    setVideoState({});
    progressRef.current = 0;
    // 🔧 关键修复：清理第一帧设置标记
    firstFrameSetRef.current.clear();
    // 清理原生 HLS 重写 m3u8 产生的 Blob URL
    nativeBlobUrlsRef.current.forEach(blobUrl => {
      try { URL.revokeObjectURL(blobUrl); } catch (_) {}
    });
    nativeBlobUrlsRef.current.clear();
  }, [stopProgressLoop, cleanupHiddenVideo]);

  return {
    registerProgressBarRef,
    getVideoState,
    ensureVideoState,
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
  };
};

export default useShortVideoPlayerManager;
