import React, { useState, useRef, useEffect, useCallback } from 'react';
import ReactDOM from 'react-dom';
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../../utils/hlsUtils';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import TikTokIcon from '../common/TikTokIcon';
import videoStatsService from '../../services/videoStatsService';

/* ─────────────────────────────────────────────────────
   工具
───────────────────────────────────────────────────── */
function fmtDuration(secs) {
  if (secs == null || isNaN(Number(secs))) return null;
  const s = Math.floor(Number(secs));
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
}

function Portal({ children }) {
  return ReactDOM.createPortal(children, document.body);
}

// 记录最近已经拿到首帧/可播放状态的视频，帮助移动端上下切换时复用“已就绪”状态。
const videoWarmCache = new Set();

/* ─────────────────────────────────────────────────────
   视频播放器（内部用，接受 src 变化）
   thumbSrc：视频就绪前显示的缩略图，防止黑屏闪烁
───────────────────────────────────────────────────── */
/* 所有面板均加载 HLS manifest（几乎无流量），仅激活面板下载分片并播放。
   切换时相邻面板已有 manifest，startLoad() 后可立即开始缓冲，减少等待。
   overlay 在 !isActive || !videoReady 时始终显示缩略图，确保全程无黑屏。 */
function VideoPlayer({ src, thumbSrc = null, isMobile, isActive = true, onFirstPlay = null }) {
  const playedRef = useRef(false);
  const videoRef         = useRef(null);
  const hlsRef           = useRef(null);
  const nativeBlobUrlRef = useRef(null); // iOS 原生 HLS：重写 m3u8 后的 Blob URL，卸载时释放
  // iOS Safari 专用：记录待加载的 src，等面板激活时再真正设置 vid.src
  // 避免 preload='none' 被 Safari 忽略导致非激活面板也触发 ts/key 请求
  const nativeHlsPendingSrcRef = useRef(null);
  const progressTimer    = useRef(null);
  const progressDragging = useRef(false);
  const isActiveRef      = useRef(isActive);
  // 跟踪上一次 isActive 值：用于区分"初始挂载 isActive=false"和"从 true 变为 false"
  // 仅在 active→inactive 时才调 stopLoad()，避免在 inactive 初挂载时中断 manifest 请求
  const prevIsActiveRef  = useRef(isActive);
  const mountTimeRef     = useRef(Date.now());
  const isSafari = useRef(
    typeof navigator !== 'undefined' &&
    /Safari/i.test(navigator.userAgent) &&
    !/Chrome|CriOS|Edg|EdgiOS|Android/i.test(navigator.userAgent)
  );

  const [videoReady, setVideoReady]   = useState(() => (src ? videoWarmCache.has(src) : false));
  const [paused, setPaused]           = useState(false);
  const [progress, setProgress]       = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration]       = useState(0);

  useEffect(() => { isActiveRef.current = isActive; }, [isActive]);

  const markVideoReady = useCallback(() => {
    if (src) videoWarmCache.add(src);
    setVideoReady(true);
    // 确认仍是激活面板再播放（避免非激活面板的 canplay 事件意外触发播放）
    if (isActiveRef.current && videoRef.current && videoRef.current.paused) {
      videoRef.current.play().catch(() => {});
    }
  }, [src]);

  // ── src 变化：重建 HLS（所有面板都加载 manifest，非激活面板暂不加载分片） ──
  useEffect(() => {
    const vid = videoRef.current;
    if (!vid || !src) return;

    // 销毁旧实例（保留 video 最后一帧，不清 src，防黑屏）
    if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; }
    // 释放上一次原生 HLS 的 Blob URL（如有）
    if (nativeBlobUrlRef.current) {
      URL.revokeObjectURL(nativeBlobUrlRef.current);
      nativeBlobUrlRef.current = null;
    }
    nativeHlsPendingSrcRef.current = null;
    clearInterval(progressTimer.current);

    setVideoReady(videoWarmCache.has(src));
    setPaused(false);
    setProgress(0); setCurrentTime(0); setDuration(0);
    mountTimeRef.current = Date.now();
    // src 重建时重置 prevIsActiveRef，确保新 HLS 实例走正确的 stopLoad 逻辑
    prevIsActiveRef.current = isActiveRef.current;

    const isM3u8 = src.includes('.m3u8');

    if (isM3u8 && Hls.isSupported()) {
      // 统一 autoStartLoad: false，避免以下双触发问题：
      //   ① autoStartLoad:true 在 MANIFEST_PARSED 后自动拉段
      //   ② isActive effect 又调 startLoad() → 从头重拉，导致重复 ts/key 请求
      // 所有面板均只加载 manifest；由 isActive effect 统一调 startLoad() 控制时机。
      const hls = new Hls({
        enableWorker: true,
        autoStartLoad: false,
        maxBufferLength: isSafari.current ? 8 : 15,
        maxMaxBufferLength: isSafari.current ? 15 : 30,
        startLevel: 0,
        xhrSetup: hlsXhrSetup,
      });
      hls.loadSource(src);
      hls.attachMedia(vid);
      // fatal 错误时销毁实例，防止 hls.js 内部无限重试死循环
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          try { hls.destroy(); } catch (_e) {}
          if (hlsRef.current === hls) hlsRef.current = null;
        }
      });
      hlsRef.current = hls;
    } else if (isM3u8 && vid.canPlayType('application/vnd.apple.mpegurl')) {
      // iOS Safari 原生 HLS：preload='none' 不被 Safari 可靠遵守，
      // 非激活面板一旦设置 vid.src 就会触发实际的 ts/key 请求（多面板重复加载同一视频）。
      // 修复：非激活面板先只记录待加载 src，等面板激活时（isActive effect）再真正加载。
      if (isActiveRef.current) {
        patchNativeHlsM3u8(src).then(({ url, isBlob }) => {
          if (isBlob) nativeBlobUrlRef.current = url;
          // 检查组件未在异步等待期间卸载（src 可能已经变化）
          if (videoRef.current === vid && nativeHlsPendingSrcRef.current === null) {
            vid.src = url;
            vid.play().catch(() => {});
          } else if (isBlob) {
            URL.revokeObjectURL(url);
            nativeBlobUrlRef.current = null;
          }
        });
      } else {
        // 非激活面板：仅记录待加载 src，不触发网络请求
        nativeHlsPendingSrcRef.current = src;
      }
    } else {
      // 普通 mp4 等：同上
      if (isActiveRef.current) {
        vid.src = src;
        vid.play().catch(() => {});
      }
    }

    if (isActiveRef.current) {
      progressTimer.current = setInterval(() => {
        if (vid.duration > 0) {
          setProgress(vid.currentTime / vid.duration);
          setCurrentTime(vid.currentTime);
          setDuration(vid.duration);
        }
      }, 500);
    }

    return () => {
      clearInterval(progressTimer.current);
      if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; }
      if (nativeBlobUrlRef.current) {
        URL.revokeObjectURL(nativeBlobUrlRef.current);
        nativeBlobUrlRef.current = null;
      }
      // 不清 src，保留最后一帧作为"天然缩略图"
      try { vid.pause(); } catch (_) {}
    };
  }, [src]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── isActive 变化：开始/停止分片加载和播放 ──────────────────────────────────
  useEffect(() => {
    const vid = videoRef.current;
    const hls = hlsRef.current;
    if (!vid) return;

    const wasActive = prevIsActiveRef.current;
    prevIsActiveRef.current = isActive;

    if (isActive) {
      // 启动分片下载（非激活时 autoStartLoad=false，此处补充触发）
      if (hls) {
        hls.startLoad();
      } else if (nativeHlsPendingSrcRef.current) {
        // iOS Safari 延迟加载：面板现在激活，执行之前跳过的 patchNativeHlsM3u8
        const pendingSrc = nativeHlsPendingSrcRef.current;
        nativeHlsPendingSrcRef.current = null; // 清除标记，防止重复触发
        patchNativeHlsM3u8(pendingSrc).then(({ url, isBlob }) => {
          if (isBlob) {
            if (nativeBlobUrlRef.current) URL.revokeObjectURL(nativeBlobUrlRef.current);
            nativeBlobUrlRef.current = url;
          }
          if (videoRef.current && isActiveRef.current) {
            videoRef.current.src = url;
            videoRef.current.play().catch(() => {});
          } else if (isBlob) {
            URL.revokeObjectURL(url);
            nativeBlobUrlRef.current = null;
          }
        });
      }

      // 进度轮询
      clearInterval(progressTimer.current);
      progressTimer.current = setInterval(() => {
        if (vid.duration > 0) {
          setProgress(vid.currentTime / vid.duration);
          setCurrentTime(vid.currentTime);
          setDuration(vid.duration);
        }
      }, 500);

      // 直接调用 play()，无论 readyState 是多少。
      // 浏览器会将 play 请求入队，等数据就绪后自动执行；
      // iOS Safari（无 HLS.js）同样依赖此路径触发播放。
      vid.play().catch(() => {});
    } else {
      clearInterval(progressTimer.current);
      vid.pause();
      // 仅在 active→inactive 时才停止加载分片（防止已激活面板在切走后继续缓冲）。
      // 初始挂载时 isActive=false 不调 stopLoad()，否则会中断 manifest 的 XHR 请求，
      // 导致切换到该面板时 hls.startLoad() 无法恢复（MANIFEST_PARSED 永不触发，视频卡住）。
      if (wasActive && hls) hls.stopLoad();
    }
  }, [isActive]); // eslint-disable-line react-hooks/exhaustive-deps

  // 格式化秒数为 mm:ss
  const fmt = (s) => {
    const t = Math.floor(s || 0);
    const m = Math.floor(t / 60);
    return `${String(m).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`;
  };

  // 点击切换播放/暂停（进度条拖动或冷却期内不触发）
  const handleTap = useCallback((e) => {
    if (progressDragging.current) return;
    if (!isActive) return;
    if (Date.now() - mountTimeRef.current < 600) return; // 防滑动后游离 click
    e.stopPropagation();
    const vid = videoRef.current;
    if (!vid || !videoReady) return;
    if (vid.paused) { vid.play().catch(() => {}); } else { vid.pause(); }
  }, [isActive, videoReady]);

  // 根据点击/拖拽位置 seek
  const seekFromClientX = useCallback((el, clientX) => {
    const vid = videoRef.current;
    if (!el || !vid || !vid.duration) return;
    const rect = el.getBoundingClientRect();
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    vid.currentTime = ratio * vid.duration;
    setProgress(ratio);
  }, []);

  const handleProgressPointerDown = useCallback((e) => {
    e.stopPropagation();
    e.preventDefault();
    progressDragging.current = true;
    try { e.currentTarget.setPointerCapture(e.pointerId); } catch (_) {}
    seekFromClientX(e.currentTarget, e.clientX);
  }, [seekFromClientX]);

  const handleProgressPointerMove = useCallback((e) => {
    if (!progressDragging.current) return;
    e.stopPropagation();
    seekFromClientX(e.currentTarget, e.clientX);
  }, [seekFromClientX]);

  const handleProgressPointerUp = useCallback((e) => {
    if (!progressDragging.current) return;
    progressDragging.current = false;
    e.stopPropagation();
    seekFromClientX(e.currentTarget, e.clientX);
    try { e.currentTarget.releasePointerCapture(e.pointerId); } catch (_) {}
  }, [seekFromClientX]);

  if (isMobile) {
    return (
      <div
        style={{
          position: 'relative',
          width: '100vw',
          // iOS Safari: 100vh 包含工具栏导致溢出，用 dvh 适配，不支持时回退 100vh
          height: 'calc(var(--tg-vh, 1vh) * 100)',
          overflow: 'hidden',
          // 提前建立 GPU 合成层，防止 iOS 播放时出现"从小到大"缩放闪烁
          transform: 'translateZ(0)',
          WebkitTransform: 'translateZ(0)',
        }}
        onClick={handleTap}
      >
        <style>{`@keyframes tg-spin { to { transform: rotate(360deg); } }`}</style>

        {/* 封面覆盖层（zIndex:2）：
            - !isActive：始终显示缩略图（非激活面板全程用缩略图，无黑屏、无视频渲染开销）
            - isActive && !videoReady：激活但视频未就绪，显示缩略图 + spinner 过渡
            - isActive && videoReady：立即隐藏，露出正在播放的视频
            不使用 opacity transition（避免 iOS 创建新合成层引发缩放闪烁） */}
        <div style={{
          position: 'absolute', top: 0, right: 0, bottom: 0, left: 0,
          background: '#000',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          pointerEvents: 'none',
          zIndex: 2,
          visibility: (!isActive || !videoReady) ? 'visible' : 'hidden',
        }}>
          {thumbSrc ? (
            <SecureDecryptedImage
              src={thumbSrc} alt=""
              lazyLoad={false}
              imageStyle={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
            />
          ) : (
            <div style={{ width: '100%', height: '100%', background: '#111' }} />
          )}
        </div>

        {/* 视频层（zIndex:1），低于覆盖层，就绪后覆盖层隐藏即可露出 */}
        <video
          ref={videoRef}
          playsInline
          muted={false}
          poster={thumbSrc || undefined}
          preload={isActive ? 'auto' : 'none'}
          onLoadedData={markVideoReady}
          onCanPlay={markVideoReady}
          onPlay={() => {
            setPaused(false);
            if (!playedRef.current) {
              playedRef.current = true;
              try { onFirstPlay && onFirstPlay(); } catch (_) {}
            }
          }}
          onPause={() => setPaused(true)}
          style={{
            position: 'absolute', top: 0, left: 0,
            width: '100%', height: '100%',
            display: 'block',
            background: '#000',
            objectFit: 'contain',
            zIndex: 1,
            // 强制 GPU 合成，避免 iOS 播放时触发隐式缩放动画
            transform: 'translateZ(0)',
            WebkitTransform: 'translateZ(0)',
          }}
        />

        {/* 加载中 spinner：仅在激活且视频未就绪时显示（覆盖层之上 zIndex:3） */}
        {isActive && !videoReady && (
          <div style={{
            position: 'absolute', top: 0, right: 0, bottom: 0, left: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            pointerEvents: 'none', zIndex: 3,
          }}>
            <div style={{
              width: 44, height: 44,
              borderRadius: '50%',
              border: '3px solid rgba(255,255,255,0.15)',
              borderTopColor: 'rgba(255,255,255,0.85)',
              animation: 'tg-spin 0.8s linear infinite',
            }} />
          </div>
        )}

        {/* 暂停图标（zIndex:10） */}
        {isActive && paused && videoReady && (
          <div style={{
            position: 'absolute', top: 0, right: 0, bottom: 0, left: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            pointerEvents: 'none', zIndex: 10,
          }}>
            <div style={{
              width: 60, height: 60,
              borderRadius: '50%',
              backgroundColor: 'rgba(0,0,0,0.55)',
              border: '2px solid rgba(255,255,255,0.25)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <TikTokIcon size={28} color="#fff" opacity={0.9} />
            </div>
          </div>
        )}

        {/* 底部进度条（zIndex:20，明确高于覆盖层和视频层） */}
        {isActive && videoReady && (
          <div
            style={{
              position: 'absolute',
              bottom: 'calc(env(safe-area-inset-bottom, 0px) + 20px)',
              left: 0, right: 0,
              height: 54,
              display: 'flex', alignItems: 'flex-end',
              padding: '0 16px 4px',
              boxSizing: 'border-box',
              zIndex: 20,
              touchAction: 'none',
              cursor: 'pointer',
            }}
            onPointerDown={handleProgressPointerDown}
            onPointerMove={handleProgressPointerMove}
            onPointerUp={handleProgressPointerUp}
            onPointerCancel={handleProgressPointerUp}
            onTouchStart={e => e.stopPropagation()}
            onTouchMove={e => e.stopPropagation()}
            onTouchEnd={e => e.stopPropagation()}
          >
            <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 5 }}>
              {duration > 0 && (
                <div style={{
                  display: 'flex', justifyContent: 'space-between',
                  color: 'rgba(255,255,255,0.75)', fontSize: 11,
                  fontVariantNumeric: 'tabular-nums',
                  pointerEvents: 'none', userSelect: 'none',
                }}>
                  <span>{fmt(currentTime)}</span>
                  <span>{fmt(duration)}</span>
                </div>
              )}
              <div style={{
                width: '100%', height: 4,
                background: 'rgba(255,255,255,0.25)',
                borderRadius: 2, overflow: 'hidden',
              }}>
                <div style={{
                  height: '100%',
                  width: `${progress * 100}%`,
                  background: '#fff',
                  borderRadius: 2,
                  transition: progressDragging.current ? 'none' : 'width 0.5s linear',
                }} />
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  return (
    <div
      onClick={e => e.stopPropagation()}
      style={{
        width: '100%', maxWidth: 'min(92vw, 880px)',
        background: '#000', borderRadius: 10, overflow: 'hidden',
        lineHeight: 0, boxShadow: '0 0 60px rgba(0,0,0,0.9)',
      }}
    >
      <video
        ref={videoRef}
        controls
        playsInline
        style={{ width: '100%', maxHeight: '82vh', display: 'block', background: '#000' }}
      />
    </div>
  );
}

/* ─────────────────────────────────────────────────────
   统一媒体查看器（移动端上下滑动翻看，PC 端左右按钮）
───────────────────────────────────────────────────── */
function MediaViewer({ mediaList, initialIndex, onClose, postId = null }) {
  const tgPlayedSet = useRef(new Set());
  const fireTgMediaPlay = useCallback((m) => {
    if (!postId || !m) return;
    const mid = m.id || m.mediaId || m.localUrl;
    const key = `${postId}:${mid}`;
    if (tgPlayedSet.current.has(key)) return;
    tgPlayedSet.current.add(key);
    try { videoStatsService.trackTgMediaPlay(postId, mid); } catch (_) {}
  }, [postId]);
  const [index, setIndex] = useState(initialIndex);
  const [showNav, setShowNav] = useState(true);   // 移动端箭头可见性
  const total = mediaList.length;
  const item  = mediaList[index] || mediaList[0];
  const isMobile = typeof window !== 'undefined' && window.innerWidth <= 767;
  const navTimerRef = useRef(null);
  // 用 ref 保持 onClose 最新引用，避免 effect 依赖数组需要 onClose
  const onCloseRef = useRef(onClose);
  useEffect(() => { onCloseRef.current = onClose; }, [onClose]);

  // ── 下滑关闭动画状态 ──
  const [drag, setDrag]       = useState({ x: 0, y: 0 });
  const [closing, setClosing] = useState(false);
  const dragDir = useRef(null); // 'vertical' | 'horizontal' | null

  // ── 上下滑动切换动画状态（移动端） ──
  // 用 ref 直接操控 DOM，避免每次 touchmove 触发 React re-render，提升滑动流畅性
  const trackRef                        = useRef(null);   // 三面板轨道 DOM 引用
  const slideAnimatingRef               = useRef(false);  // 防重入

  // 直接设置轨道 transform，不触发 React 渲染
  // 使用 --tg-vh 适配 iOS Safari 真实可视高度（window.innerHeight 而非 100vh）
  const setTrackY = useCallback((dy, withTransition = false) => {
    const el = trackRef.current;
    if (!el) return;
    el.style.transition = withTransition
      ? 'transform 0.22s cubic-bezier(0.25,0.46,0.45,0.94)'
      : 'none';
    el.style.transform = `translateY(calc(var(--tg-vh, 1vh) * -100 + ${dy}px))`;
  }, []);

  // 移动端：显示提示图标后 1.5 秒自动隐藏
  const flashNav = useCallback(() => {
    if (!isMobile) return;
    setShowNav(true);
    clearTimeout(navTimerRef.current);
    navTimerRef.current = setTimeout(() => setShowNav(false), 1500);
  }, [isMobile]);

  // 打开时触发一次
  useEffect(() => {
    if (isMobile) flashNav();
    return () => clearTimeout(navTimerRef.current);
  }, [isMobile, flashNav]);

  // 每次 MediaViewer 新打开时清除 warm cache，确保封面层正常显示
  // （warm cache 会在 VideoPlayer 首次播放后写入，若不清除，第二次打开时
  //  videoReady 会初始化为 true，封面被立即隐藏，视觉上无封面过渡）
  useEffect(() => {
    mediaList.forEach(item => {
      if (item.localUrl) videoWarmCache.delete(item.localUrl);
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const goPrev = useCallback((e) => {
    e && e.stopPropagation();
    setIndex(i => Math.max(0, i - 1));
  }, []);

  const goNext = useCallback((e) => {
    e && e.stopPropagation();
    setIndex(i => Math.min(total - 1, i + 1));
  }, [total]);

  const getMediaIdentity = useCallback((media, mediaIndex) => {
    return String(media?.id ?? media?.localUrl ?? `${media?.mediaType || 'media'}-${mediaIndex}`);
  }, []);

  // 键盘（PC 左右，移动端上下）+ iOS 视口高度修正
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape')     onClose();
      if (isMobile) {
        if (e.key === 'ArrowUp')   goPrev();
        if (e.key === 'ArrowDown') goNext();
      } else {
        if (e.key === 'ArrowLeft')  goPrev();
        if (e.key === 'ArrowRight') goNext();
      }
    };
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';

    // iOS Safari: window.innerHeight 是真实可视高度（不含工具栏），
    // 用 CSS 变量 --tg-vh 替代 1vh，避免 100vh 包含工具栏导致视频超出屏幕
    if (isMobile) {
      const setVh = () => {
        document.documentElement.style.setProperty('--tg-vh', `${window.innerHeight * 0.01}px`);
      };
      setVh();
      window.addEventListener('resize', setVh);
      return () => {
        document.removeEventListener('keydown', onKey);
        document.body.style.overflow = '';
        window.removeEventListener('resize', setVh);
      };
    }

    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [onClose, goPrev, goNext, isMobile]);

  // ── 移动端：拦截浏览器原生右滑返回手势，改为关闭播放页 ──
  useEffect(() => {
    if (!isMobile) return;
    // 推入虚拟历史条目，使浏览器原生返回手势触发 popstate 而不是真正跳页
    const stateKey = `tgViewer-${Date.now()}`;
    window.history.pushState({ tgViewer: stateKey }, '');

    // React StrictMode 会执行两次 effect：第一次 cleanup 的 history.back()
    // 会异步触发 popstate，若立即监听会误关刚打开的播放页。
    // 用 100ms 延迟激活，避免捕获到上一次 cleanup 产生的杂散 popstate。
    let canHandle = false;
    const enableTimer = setTimeout(() => { canHandle = true; }, 100);

    const handlePopState = () => {
      if (!canHandle) return;
      // 浏览器已自动弹出虚拟条目，直接关闭播放层即可
      onCloseRef.current();
    };
    window.addEventListener('popstate', handlePopState);

    return () => {
      clearTimeout(enableTimer);
      window.removeEventListener('popstate', handlePopState);
      // 若通过 UI 关闭（点按钮/左右滑），虚拟条目仍在栈顶，需手动清除
      if (window.history.state?.tgViewer === stateKey) {
        window.history.back();
      }
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── 触摸处理：跟手拖动 + 松手动画 ──
  const touchStartX = useRef(null);
  const touchStartY = useRef(null);

  const triggerClose = useCallback((direction = 'down') => {
    setClosing(true);
    setTrackY(0, false);
    const target = direction === 'left'
      ? { x: -window.innerWidth, y: 0 }
      : direction === 'right'
        ? { x: window.innerWidth, y: 0 }
        : { x: 0, y: window.innerHeight };
    setDrag(target);
    setTimeout(onClose, 290);
  }, [onClose]);

  const onTouchStart = (e) => {
    if (closing) return;
    touchStartX.current = e.touches[0].clientX;
    touchStartY.current = e.touches[0].clientY;
    dragDir.current = null;
  };

  const onTouchMove = (e) => {
    if (closing || touchStartX.current === null) return;
    const dx = e.touches[0].clientX - touchStartX.current;
    const dy = e.touches[0].clientY - touchStartY.current;

    // 首次移动确定方向：垂直=上下切换，水平=左滑关闭
    if (!dragDir.current) {
      if (Math.abs(dy) > Math.abs(dx)) dragDir.current = 'vertical';
      else if (Math.abs(dx) > Math.abs(dy)) dragDir.current = 'horizontal';
      else return;
    }

    if (dragDir.current === 'vertical') {
      if (!slideAnimatingRef.current) {
        // 上下跟手切换（上滑=下一张，下滑=上一张）：直接操控 DOM，无 re-render
        setTrackY(dy, false);
      }
    } else if (dragDir.current === 'horizontal') {
      // 左右滑跟手（用于关闭）
      setDrag({ x: dx, y: 0 });
    }
  };

  const onTouchEnd = (e) => {
    if (closing || touchStartX.current === null) return;
    const dx = e.changedTouches[0].clientX - touchStartX.current;
    const dy = e.changedTouches[0].clientY - touchStartY.current;

    if (dragDir.current === 'vertical' && !slideAnimatingRef.current) {
      const canGoPrev = index > 0;
      const canGoNext = index < total - 1;
      const shouldSwitch = (dy < 0 && canGoNext) || (dy > 0 && canGoPrev);
      if (Math.abs(dy) > 50 && shouldSwitch) {
        slideAnimatingRef.current = true;
        // 上滑(dy<0)→下一张，下滑(dy>0)→上一张（首尾不循环）
        const target = dy < 0 ? -window.innerHeight : window.innerHeight;
        setTrackY(target, true);
        setTimeout(() => {
          // 切换 index 后重置轨道位置（无过渡，防止视觉跳跃）
          dy < 0 ? goNext() : goPrev();
          // 下一帧再重置，确保 React 已渲染新面板
          requestAnimationFrame(() => {
            setTrackY(0, false);
            slideAnimatingRef.current = false;
          });
        }, 220);
      } else {
        // 未达阈值：弹回
        setTrackY(0, true);
        setTimeout(() => setTrackY(0, false), 220);
      }
    } else if (dragDir.current === 'horizontal') {
      if (dx < -80) {
        // 左滑超过阈值：关闭（向左滑出）
        triggerClose('left');
      } else if (dx > 80) {
        // 右滑超过阈值：关闭（向右滑出）
        triggerClose('right');
      } else {
        // 未达阈值：弹回
        setDrag({ x: 0, y: 0 });
      }
    }

    touchStartX.current = null;
    touchStartY.current = null;
    dragDir.current = null;
  };

  const isPhoto = item.mediaType === 'photo';

  /* ── 公共按钮样式 ── */
  const navBtnBase = {
    position: /** @type {'fixed'} */ ('fixed'),
    zIndex: 25,
    background: 'rgba(0,0,0,0.45)',
    border: '1px solid rgba(255,255,255,0.2)',
    borderRadius: '50%',
    width: isMobile ? 36 : 44,
    height: isMobile ? 36 : 44,
    color: '#fff',
    cursor: 'pointer',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    backdropFilter: 'blur(4px)',
    transition: 'background 0.15s',
  };
  // 视频面板：进度条从 bottom: safe-area+20px 起，高 54px，加上 iPhone 安全区约 34px
  // 合计约 108px，下方箭头设为 130px 留出余量；图片面板无进度条可贴底
  const bottomNavOffset = isMobile && !isPhoto ? 130 : 12;
  const navBtnStyle = (side) => isMobile
    ? { ...navBtnBase, left: '50%', transform: 'translateX(-50%)', [side]: side === 'bottom' ? bottomNavOffset : 12 }
    : { ...navBtnBase, top: '50%', transform: 'translateY(-50%)', [side]: 20 };

  // 拖拽进度（0~1）用于透明度
  const dragProgress = isMobile
    ? Math.min(1, (Math.abs(drag.x) + Math.abs(drag.y)) / (window.innerHeight * 0.55))
    : 0;
  const overlayOpacity = 1 - dragProgress * 0.65;

  return (
    <Portal>
      <div
        style={{
          position: 'fixed', top: 0, right: 0, bottom: 0, left: 0,
          zIndex: 2147483647,
          background: `rgba(0,0,0,${overlayOpacity})`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          transform: `translate(${drag.x}px, ${drag.y}px)`,
          transition: closing
            ? 'transform 0.28s cubic-bezier(0.25,0.46,0.45,0.94), background 0.28s'
            : drag.x === 0 && drag.y === 0
              ? 'transform 0.22s cubic-bezier(0.25,0.46,0.45,0.94)'
              : 'none',
          willChange: 'transform',
          touchAction: 'none',
        }}
        onClick={isMobile ? undefined : onClose}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
      >
        {/* ── 顶部工具栏 ── */}
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0,
          height: 64,
          background: 'linear-gradient(to bottom, rgba(0,0,0,0.65) 0%, transparent 100%)',
          display: 'flex', alignItems: 'center',
          justifyContent: isMobile ? 'space-between' : 'flex-end',
          padding: '0 16px',
          zIndex: 3, pointerEvents: 'none',
        }}>
          {/* 移动端返回 */}
          {isMobile && (
            <button
              onClick={onClose}
              style={{
                pointerEvents: 'all',
                background: 'transparent', border: 'none',
                color: '#fff', fontSize: 14, cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '6px 10px',
              }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6" />
              </svg>
              返回
            </button>
          )}

          {/* 计数：PC 端显示在顶栏，移动端已移至底部控制区 */}
          {!isMobile && total > 1 && (
            <div style={{
              pointerEvents: 'none',
              color: 'rgba(255,255,255,0.85)', fontSize: 14,
              background: 'rgba(0,0,0,0.4)', padding: '3px 10px',
              borderRadius: 20, backdropFilter: 'blur(4px)',
            }}>
              {index + 1} / {total}
            </div>
          )}

          {/* PC 关闭 */}
          {!isMobile && (
            <button
              onClick={onClose}
              style={{
                pointerEvents: 'all',
                marginLeft: 12,
                background: 'rgba(0,0,0,0.5)', border: '1px solid rgba(255,255,255,0.3)',
                borderRadius: '50%', width: 36, height: 36,
                color: '#fff', fontSize: 20, cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}
            >×</button>
          )}
        </div>

        {/* ── 移动端：开场提示（上下箭头 + 下滑关闭），1.5s 后淡出 ── */}
        {isMobile && (
          <>
            {/* 上下切换按钮（首尾不循环，到边界时隐藏对应按钮） */}
            {total > 1 && (
              <>
                {index > 0 && (
                  <button style={{ ...navBtnStyle('top'), opacity: showNav ? 1 : 0, transition: 'opacity 0.4s ease', pointerEvents: showNav ? 'all' : 'none' }} onClick={goPrev}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="18 15 12 9 6 15" />
                    </svg>
                  </button>
                )}
                {index < total - 1 && (
                  <button style={{ ...navBtnStyle('bottom'), opacity: showNav ? 1 : 0, transition: 'opacity 0.4s ease', pointerEvents: showNav ? 'all' : 'none' }} onClick={goNext}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="6 9 12 15 18 9" />
                    </svg>
                  </button>
                )}
              </>
            )}
            {/* 上下切换提示（居中底部，多媒体时显示） */}
            {total > 1 && (
              <div style={{
                position: 'fixed',
                bottom: isPhoto ? 80 : 155,
                left: '50%',
                transform: 'translateX(-50%)',
                zIndex: 26,
                pointerEvents: 'none',
                opacity: showNav ? 1 : 0,
                transition: 'opacity 0.4s ease',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 2,
              }}>
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.75)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                  style={{ animation: showNav ? 'swipeDownHint 0.8s ease-in-out infinite alternate' : 'none' }}>
                  <polyline points="6 9 12 15 18 9" />
                </svg>
                <span style={{ color: 'rgba(255,255,255,0.55)', fontSize: 11, letterSpacing: '0.5px' }}>上下切换 · 左右滑关闭</span>
              </div>
            )}
          </>
        )}

        {/* ── 媒体内容（PC端含左右按钮紧贴两侧） ── */}
        {isMobile ? (
          /* 移动端：三面板轮播轨道（上下滑动），拖动时可预览前后内容 */
          <div style={{ position: 'fixed', top: 0, right: 0, bottom: 0, left: 0, overflow: 'hidden' }}>
            {(() => {
              // 首尾不循环：第一张的上一张、最后一张的下一张均显示当前
              const panelMediaIndexes = [
                index > 0 ? index - 1 : index,
                index,
                index < total - 1 ? index + 1 : index,
              ];

              // 使用 --tg-vh 适配 iOS Safari 真实可视高度
              const vhUnit = 'calc(var(--tg-vh, 1vh) * 100)';

              return (
            <div
              ref={trackRef}
              style={{
                display: 'flex',
                flexDirection: 'column',
                width: '100vw',
                height: `calc(var(--tg-vh, 1vh) * 300)`,
                transform: `translateY(calc(var(--tg-vh, 1vh) * -100))`,
                transition: 'none',
                willChange: 'transform',
              }}
            >
              {panelMediaIndexes.map((mediaIndex, panelIdx) => {
                const m = mediaList[mediaIndex];
                const mediaKey = getMediaIdentity(m, mediaIndex);
                // 关键：正常情况用 mediaKey 做 key，让 React 在面板间复用同一组件实例，
                // 避免切换时 unmount/remount 引发的黑屏和合成层重建。
                // 首尾边界时相邻面板 mediaIndex 相同（会产生重复 key），加方向前缀区分。
                const isAtStart = index === 0;
                const isAtEnd   = index === total - 1;
                const panelKey  =
                  (panelIdx === 0 && isAtStart) ? `prev-boundary-${mediaKey}` :
                  (panelIdx === 2 && isAtEnd)   ? `next-boundary-${mediaKey}` :
                  mediaKey;

                return (
                <div key={panelKey} style={{
                  width: '100vw', height: vhUnit, flexShrink: 0,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  {m.mediaType === 'photo' ? (
                    <div onClick={e => e.stopPropagation()} style={{ lineHeight: 0 }}>
                      <SecureDecryptedImage
                        src={m.localUrl} alt=""
                        lazyLoad={false} objectFit="contain" style={{ lineHeight: 0 }}
                        imageStyle={{ width: '100vw', height: '100vh', objectFit: 'contain', display: 'block' }}
                      />
                    </div>
                  ) : (
                    /* 使用媒体稳定 key，尽量让相邻视频实例在切换到中间时被复用，
                       旁边面板只做轻量预热，但拿到首帧后也允许展示，减少“闪一下再播放” */
                    <VideoPlayer
                      src={m.localUrl}
                      thumbSrc={m.firstFrameUrl || m.thumbUrl}
                      isMobile={true}
                      isActive={panelIdx === 1}
                      onFirstPlay={panelIdx === 1 ? () => fireTgMediaPlay(m) : null}
                    />
                  )}
                </div>
                );
              })}
            </div>
              );
            })()}
          </div>
        ) : null}

        {/* PC 端主内容 */}
        {!isMobile && (
          /* PC端：按钮 + 内容水平排列，整体居中，点内容区不关闭 */
          <div
            onClick={e => e.stopPropagation()}
            style={{ display: 'flex', alignItems: 'center', gap: 16 }}
          >
            {/* 上一张（首张时隐藏） */}
            {total > 1 && index > 0 && (
              <button
                onClick={goPrev}
                style={{
                  flexShrink: 0,
                  background: 'rgba(0,0,0,0.45)',
                  border: '1px solid rgba(255,255,255,0.2)',
                  borderRadius: '50%', width: 44, height: 44,
                  color: '#fff', cursor: 'pointer',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  backdropFilter: 'blur(4px)', transition: 'background 0.15s',
                }}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="15 18 9 12 15 6" />
                </svg>
              </button>
            )}

            {/* 媒体 */}
            {isPhoto ? (
              <div style={{ lineHeight: 0, flexShrink: 0 }}>
                <SecureDecryptedImage
                  key={item.localUrl} src={item.localUrl} alt=""
                  lazyLoad={false} objectFit="contain" style={{ lineHeight: 0 }}
                  imageStyle={{
                    width: 'auto', height: 'auto',
                    maxWidth: 'min(700px, 72vw)', maxHeight: '85vh',
                    borderRadius: 8,
                    boxShadow: '0 4px 40px rgba(0,0,0,0.8)',
                    display: 'block',
                  }}
                />
              </div>
            ) : (
              <VideoPlayer
                key={item.localUrl}
                src={item.localUrl}
                isMobile={false}
                onFirstPlay={() => fireTgMediaPlay(item)}
              />
            )}

            {/* 下一张（末张时隐藏） */}
            {total > 1 && index < total - 1 && (
              <button
                onClick={goNext}
                style={{
                  flexShrink: 0,
                  background: 'rgba(0,0,0,0.45)',
                  border: '1px solid rgba(255,255,255,0.2)',
                  borderRadius: '50%', width: 44, height: 44,
                  color: '#fff', cursor: 'pointer',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  backdropFilter: 'blur(4px)', transition: 'background 0.15s',
                }}
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </button>
            )}
          </div>
        )}

      </div>
    </Portal>
  );
}

/* ─────────────────────────────────────────────────────
   马赛克布局
───────────────────────────────────────────────────── */
function MediaMosaic({ mediaList, onOpen, captionAlt = '' }) {
  const show  = mediaList;
  const count = show.length;
  const isPC  = typeof window !== 'undefined' && window.innerWidth > 767;
  if (count === 0) return null;

  // 单图/单视频时直接用 caption；多媒体追加序号，确保 alt 各不相同
  const altFor = (item, i) => {
    const base = captionAlt || '每日吃瓜';
    if (count === 1) return base;
    const suffix = item.mediaType === 'photo' ? `图${i + 1}` : `视频${i + 1}`;
    return `${base} ${suffix}`;
  };

  const renderItem = (item, i) => {
    const recommendBadge = item.isRecommend === 1 ? (
      <div className="tg-recommend-badge">
        <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
        </svg>
      </div>
    ) : null;

    if (item.mediaType === 'photo') {
      return (
        <div className="tg-cell tg-image-cell" key={item.id || i}
          onClick={(e) => { e.stopPropagation(); onOpen(i); }}
          style={{ cursor: 'zoom-in', position: 'relative' }}>
          <SecureDecryptedImage
            src={item.localUrl} alt={altFor(item, i)}
            className="tg-cell-media" loading="lazy"
          />
          {recommendBadge}
        </div>
      );
    }
    return (
      <div className="tg-cell tg-video-cell" key={item.id || i}
        onClick={(e) => { e.stopPropagation(); onOpen(i); }}
        style={{ cursor: 'pointer', position: 'relative' }}>
        {(item.firstFrameUrl || item.thumbUrl) ? (
          <SecureDecryptedImage src={item.firstFrameUrl || item.thumbUrl} alt={`${altFor(item, i)} 封面`} className="tg-cell-media" />
        ) : (
          <div className="tg-cell-media tg-cell-placeholder" />
        )}
        {fmtDuration(item.duration) && (
          <div className="tg-duration-badge">{fmtDuration(item.duration)}</div>
        )}
        <div className="tg-play-btn">
          <svg width="26" height="26" viewBox="0 0 40 40" fill="none">
            <circle cx="20" cy="20" r="20" fill="rgba(0,0,0,0.45)" />
            <polygon points="15,11 32,20 15,29" fill="white" />
          </svg>
        </div>
        {recommendBadge}
      </div>
    );
  };

  // ── PC 端：每行 2 个，固定 150×260 ──
  if (isPC) {
    return (
      <div className="tg-uniform-grid">
        {show.map((item, i) => (
          <div key={item.id || i} className="tg-uniform-cell">
            {renderItem(item, i)}
          </div>
        ))}
      </div>
    );
  }

  // ── 移动端：原有马赛克布局 ──
  if (count === 1) return <div className="tg-mosaic tg-mosaic-1">{renderItem(show[0], 0)}</div>;

  if (count === 2) return (
    <div className="tg-mosaic tg-mosaic-2">{show.map((item, i) => renderItem(item, i))}</div>
  );

  if (count === 3) return (
    <div className="tg-mosaic tg-mosaic-3">
      <div className="tg-mosaic-3-left">{renderItem(show[0], 0)}</div>
      <div className="tg-mosaic-3-right">{renderItem(show[1], 1)}{renderItem(show[2], 2)}</div>
    </div>
  );

  if (count === 4) return (
    <div className="tg-mosaic tg-mosaic-4">{show.map((item, i) => renderItem(item, i))}</div>
  );

  if (count === 5) return (
    <div className="tg-mosaic tg-mosaic-5">
      <div className="tg-mosaic-5-top">{show.slice(0, 2).map((item, i) => renderItem(item, i))}</div>
      <div className="tg-mosaic-5-bottom">{show.slice(2, 5).map((item, i) => renderItem(item, i + 2))}</div>
    </div>
  );

  return (
    <div className="tg-mosaic tg-mosaic-grid">
      {show.map((item, i) => (
        <div key={item.id || i} className="tg-mosaic-grid-cell">{renderItem(item, i)}</div>
      ))}
    </div>
  );
}

/* ─────────────────────────────────────────────────────
   Telegram 风格卡片
───────────────────────────────────────────────────── */
const TelegramCard = ({ post, captionAlt = '' }) => {
  // viewer: null | { index: number }
  const [viewer, setViewer] = useState(null);
  const mediaList = post?.media || [];
  const postViewedRef = useRef(false);

  const handleOpen = useCallback((index) => {
    if (post && post.id && !postViewedRef.current) {
      postViewedRef.current = true;
      try {
        videoStatsService.trackTgPostView(post.id, {
          mediaCount: mediaList.length,
          mediaIndex: index
        });
      } catch (_) {}
    }
    setViewer({ index });
  }, [post, mediaList.length]);
  const handleClose = useCallback(() => setViewer(null), []);

  const altText = captionAlt || (post?.caption ? String(post.caption).slice(0, 60) : '每日吃瓜');

  return (
    <>
      <div className="tg-card">
        {post?.caption && (
          <div className="tg-card-caption">{post.caption}</div>
        )}
        {mediaList.length > 0 && (
          <div className="tg-card-media">
            <MediaMosaic mediaList={mediaList} onOpen={handleOpen} captionAlt={altText} />
          </div>
        )}
      </div>

      {viewer !== null && (
        <MediaViewer
          mediaList={mediaList}
          initialIndex={viewer.index}
          postId={post?.id || null}
          onClose={handleClose}
        />
      )}
    </>
  );
};

export { MediaViewer };
export default TelegramCard;
