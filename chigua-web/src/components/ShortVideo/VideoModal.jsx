import React, { useMemo } from 'react';
import Lightbox from 'yet-another-react-lightbox';
import Hls from 'hls.js';
import 'yet-another-react-lightbox/styles.css';
import videoStatsService from '../../services/videoStatsService';
import statsTracker from '../../utils/statsTracker';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import TikTokIcon from '../common/TikTokIcon';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../../utils/hlsUtils';

// 自定义播放器，用于 m3u8 + hls.js 播放
const REGION_WIDTH = 850; // 固定显示区宽度
const queryLightboxVideos = () => {
  try {
    return Array.from(document.querySelectorAll('.lightbox-video'));
  } catch (_) {
    return [];
  }
};

const pauseOthers = (keepEl) => {
  try {
    queryLightboxVideos().forEach(v => {
      if (!keepEl || v !== keepEl) {
        try { 
          if (v instanceof HTMLVideoElement) {
            v.pause();
            v.currentTime = 0; // 重置播放位置，避免音频残留
          }
        } catch (_) {}
      }
    });
  } catch (_) {}
};

// 全局暂停所有视频并静音（但不影响即将播放的新视频）
const pauseAndMuteAll = (exceptVideo = null) => {
  try {
    queryLightboxVideos().forEach(v => {
      if (v !== exceptVideo) { // 不影响当前正在切换到的视频
        try { 
          if (v instanceof HTMLVideoElement) {
            v.pause();
            v.currentTime = 0;
            v.muted = true;
          }
        } catch (_) {}
      }
    });
  } catch (_) {}
};

const computeEffectiveDuration = (video) => {
  if (!video) return 0;

  const rawDuration = Number(video.duration);
  if (Number.isFinite(rawDuration) && rawDuration > 0) {
    return rawDuration;
  }

  try {
    const seekable = video.seekable;
    if (seekable && seekable.length > 0) {
      const end = seekable.end(seekable.length - 1);
      if (Number.isFinite(end) && end > 0) {
        return end;
      }
    }
  } catch (_) {}

  try {
    const buffered = video.buffered;
    if (buffered && buffered.length > 0) {
      const end = buffered.end(buffered.length - 1);
      if (Number.isFinite(end) && end > 0) {
        return end;
      }
    }
  } catch (_) {}

  return 0;
};

const HlsSlide = ({ url, poster, title, vidKey, videoId, isActive = false, isPreloaded = false, isInitialLoad = false, onRequestClose, onTrackPlay = null }) => {
  const videoRef = React.useRef(null);
  const hlsRef = React.useRef(null);
  const nativeBlobUrlRef = React.useRef(null); // iOS 原生 HLS blob URL，卸载时释放
  const videoWrapperRef = React.useRef(null);
  const [isVideoReady, setIsVideoReady] = React.useState(false);
  const [isLoading, setIsLoading] = React.useState(true);
  const [isPlaying, setIsPlaying] = React.useState(false);
  const [progress, setProgress] = React.useState(0);
  const [duration, setDuration] = React.useState(0);
  const durationRef = React.useRef(0);
  const [videoSize, setVideoSize] = React.useState({ width: 0, height: 0 });
  const [showPlayButton, setShowPlayButton] = React.useState(false);
  const [showLoadingIndicator, setShowLoadingIndicator] = React.useState(false);
  const [loadingError, setLoadingError] = React.useState(false);
  const [loadingErrorMessage, setLoadingErrorMessage] = React.useState('');
  const progressPointerActiveRef = React.useRef(false);
  const videoIdRef = React.useRef(videoId);
  const trackPlayRef = React.useRef(onTrackPlay);
  const showLoadingIndicatorRef = React.useRef(showLoadingIndicator);

  const updateDuration = React.useCallback((value) => {
    const next = Number.isFinite(value) && value > 0 ? value : 0;
    if (Math.abs(durationRef.current - next) < 0.05) {
      durationRef.current = next;
      return;
    }
    durationRef.current = next;
    setDuration(next);
  }, []);

  React.useEffect(() => {
    videoIdRef.current = videoId;
  }, [videoId]);

  React.useEffect(() => {
    trackPlayRef.current = onTrackPlay;
  }, [onTrackPlay]);

  React.useEffect(() => {
    showLoadingIndicatorRef.current = showLoadingIndicator;
  }, [showLoadingIndicator]);

  const getActiveDuration = React.useCallback(() => {
    const refDuration = durationRef.current;
    if (Number.isFinite(refDuration) && refDuration > 0) {
      return refDuration;
    }

    const video = videoRef.current;
    if (!video) return 0;

    const computed = computeEffectiveDuration(video);
    if (Number.isFinite(computed) && computed > 0) {
      updateDuration(computed);
      return computed;
    }

    return 0;
  }, [updateDuration]);

  const applySeekFromClientX = React.useCallback((element, clientX) => {
    if (!element || typeof clientX !== 'number') return;
    if (!isVideoReady) return;
    const rect = element.getBoundingClientRect();
    if (!rect || rect.width <= 0) return;
    const video = videoRef.current;
    if (!video) return;
    const baseDuration = getActiveDuration();
    if (!baseDuration || baseDuration <= 0) return;
    const fraction = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    const wasPaused = video.paused;
    const targetTime = baseDuration * fraction;
    try {
      video.currentTime = targetTime;
    } catch (_) {
      return;
    }
    setProgress(fraction * 100);
    updateDuration(baseDuration);
    if (!wasPaused) {
      try {
        video.play().then(() => {
          setIsPlaying(true);
          setShowPlayButton(false);
        }).catch(() => {});
      } catch (_) {}
    } else {
      setIsPlaying(false);
      setShowPlayButton(true);
    }
  }, [isVideoReady, updateDuration]);

  const handleProgressPointerDown = React.useCallback((event) => {
    if (!isVideoReady) return;
    const video = videoRef.current;
    const baseDuration = getActiveDuration();
    if (!baseDuration || baseDuration <= 0) return;
    progressPointerActiveRef.current = true;
    try { event.currentTarget.setPointerCapture(event.pointerId); } catch (_) {}
    applySeekFromClientX(event.currentTarget, event.clientX);
    event.preventDefault();
    event.stopPropagation();
  }, [applySeekFromClientX, getActiveDuration, isVideoReady]);

  const handleProgressPointerMove = React.useCallback((event) => {
    if (!progressPointerActiveRef.current) return;
    if (event.pointerType === 'mouse' && (event.buttons & 1) === 0) return;
    applySeekFromClientX(event.currentTarget, event.clientX);
    event.preventDefault();
    event.stopPropagation();
  }, [applySeekFromClientX]);

  const handleProgressPointerUp = React.useCallback((event) => {
    if (!progressPointerActiveRef.current) return;
    progressPointerActiveRef.current = false;
    applySeekFromClientX(event.currentTarget, event.clientX);
    try { event.currentTarget.releasePointerCapture(event.pointerId); } catch (_) {}
    event.preventDefault();
    event.stopPropagation();
  }, [applySeekFromClientX]);

  const handleProgressPointerCancel = React.useCallback((event) => {
    if (!progressPointerActiveRef.current) return;
    progressPointerActiveRef.current = false;
    try { event.currentTarget.releasePointerCapture(event.pointerId); } catch (_) {}
    event.preventDefault();
    event.stopPropagation();
  }, []);

  // 🔧 调试：监控isVideoReady状态变化
  React.useEffect(() => {
    if (isVideoReady) {
    } else {
    }
  }, [isVideoReady, url]);

  // 🔧 辅助函数定义
  const updateVideoSize = React.useCallback(() => {
    const wrapper = videoWrapperRef.current;
    if (wrapper) {
      const rect = wrapper.getBoundingClientRect();
      setVideoSize({
        width: rect.width,
        height: rect.height
      });
      return;
    }

    const video = videoRef.current;
    if (video) {
      setVideoSize({
        width: video.offsetWidth,
        height: video.offsetHeight
      });
    }
  }, []);

  const ensureUnmuted = React.useCallback(() => {
    try {
      const video = videoRef.current;
      if (video) {
        video.muted = false;
        video.volume = 1.0; // 确保音量最大
      }
    } catch (_) {}
  }, []);

  // 🔶 首帧呈现判定：仅在真实首帧渲染或开始播放后再隐藏加载
  const scheduleFirstFrameReady = React.useCallback((reason = 'unknown') => {
    const video = videoRef.current;
    if (!video || isVideoReady) return;
    try {
      if (typeof video.requestVideoFrameCallback === 'function') {
        video.requestVideoFrameCallback(() => {
          if (isVideoReady) return;
          setIsVideoReady(true);
          setIsLoading(false);
          setShowLoadingIndicator(false);
          setShowPlayButton(video.paused || !isActive);
          setTimeout(() => updateVideoSize(), 50);
        });
      } else {
        const onFirstFrame = () => {
          if (isVideoReady) return;
          setIsVideoReady(true);
          setIsLoading(false);
          setShowLoadingIndicator(false);
          setShowPlayButton(video.paused || !isActive);
          setTimeout(() => updateVideoSize(), 50);
          try {
            video.removeEventListener('playing', onFirstFrame);
            video.removeEventListener('seeked', onFirstFrame);
            video.removeEventListener('timeupdate', onFirstFrame);
          } catch (_) {}
        };
        try {
          video.addEventListener('playing', onFirstFrame, { once: true });
          video.addEventListener('seeked', onFirstFrame, { once: true });
          video.addEventListener('timeupdate', onFirstFrame, { once: true });
        } catch (_) {}
      }
    } catch (_) {}
  }, [isVideoReady, isActive, updateVideoSize]);

  // 🔧 统一的视频加载成功处理函数
  const handleVideoLoadSuccess = React.useCallback((videoType = 'unknown') => {
    
    setLoadingError(false);
    setLoadingErrorMessage('');
    
    // 不再提前就绪，等待首帧呈现/playing
    scheduleFirstFrameReady(videoType);
    
    if (isActive) {
      // 活跃状态下立即设置为播放状态，避免播放按钮闪现
      setIsPlaying(true);
      setTimeout(() => {
        try {
          ensureUnmuted(); // 确保不是静音状态
          const video = videoRef.current;
          if (video) {
            video.play().then(() => {
              setIsPlaying(true);
            }).catch((error) => {
              setIsPlaying(false);
              // 自动播放受阻：显示播放按钮，继续等待首帧（不提前隐藏加载/就绪）
              setShowPlayButton(true);
            });
          }
        } catch (_) {}
      }, 50); // 短暂延迟确保其他视频已停止
    } else {
      setIsPlaying(false);
      setShowPlayButton(true);
    }
  }, [isActive, url, ensureUnmuted, updateVideoSize, scheduleFirstFrameReady]);

  // 🔧 统一的视频加载失败处理函数
  const handleVideoLoadError = React.useCallback((error, videoType = 'unknown') => {
    setIsVideoReady(false);
    setIsLoading(false);
    setShowLoadingIndicator(false);
    setLoadingError(true);
    setLoadingErrorMessage(`${videoType}视频加载失败，请重试`);
    setShowPlayButton(false);
  }, []);

  React.useEffect(() => {
    const video = videoRef.current;
    if (!video || !url) return;
    
    // 🔧 重置所有状态
    setIsVideoReady(false);
    setIsLoading(true);
    setIsPlaying(false);
    setShowPlayButton(false);
    setLoadingError(false);
    setLoadingErrorMessage('');
    // 🔧 优化：立即显示加载提示，简化逻辑
    setShowLoadingIndicator(true);
    
    // 🔧 增加超时保护：如果10秒后仍未加载完成，显示错误提示
    const loadingTimeout = setTimeout(() => {
      if (!isVideoReady) {
        handleVideoLoadError(new Error('加载超时'), '超时');
      }
    }, 10000);
    setProgress(0);
    updateDuration(0);
    setVideoSize({ width: 0, height: 0 });
    
    // 在加载新视频前，先暂停并静音所有其他视频（但不影响当前视频）
    pauseAndMuteAll(video);
    
    const isHls = /\.m3u8(\?|$)/i.test(url);
    
    // 初始化视频状态 - 新视频自动播放，声音开启
    video.muted = false; // 声音开启

    // 确保当前视频不会被意外静音（使用已定义的函数）
    
    try {
      if (isHls) {
        if (Hls.isSupported()) {
          const hls = new Hls({ xhrSetup: hlsXhrSetup });
          hls.loadSource(url);
          hls.attachMedia(video);
          hlsRef.current = hls;
          
          // 🔧 HLS加载完成后，使用统一处理函数
          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            clearTimeout(loadingTimeout);
            handleVideoLoadSuccess('HLS');
          });
          
          // 🔧 HLS加载错误处理
          hls.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal) {
              clearTimeout(loadingTimeout);
              handleVideoLoadError(data, 'HLS');
            }
          });
        } else if (video.canPlayType && video.canPlayType('application/vnd.apple.mpegurl')) {
          // iOS Safari 原生 HLS：重写 key URI 为绝对后端地址
          patchNativeHlsM3u8(url).then(({ url: patchedUrl, isBlob }) => {
            if (nativeBlobUrlRef.current) {
              try { URL.revokeObjectURL(nativeBlobUrlRef.current); } catch (_) {}
            }
            nativeBlobUrlRef.current = isBlob ? patchedUrl : null;
            video.src = patchedUrl;
          });
          video.addEventListener('loadedmetadata', () => {
            clearTimeout(loadingTimeout);
            handleVideoLoadSuccess('Safari HLS');
          }, { once: true });
          
          video.addEventListener('error', (error) => {
            clearTimeout(loadingTimeout);
            handleVideoLoadError(error, 'Safari HLS');
          }, { once: true });
        } else {
          video.src = url; // 让浏览器自行处理
        // 🔧 普通视频使用统一处理函数
        video.addEventListener('loadedmetadata', () => {
          clearTimeout(loadingTimeout);
          handleVideoLoadSuccess('普通视频');
        }, { once: true });
        
        video.addEventListener('error', (error) => {
          clearTimeout(loadingTimeout);
          handleVideoLoadError(error, '普通视频');
        }, { once: true });
        }
      } else {
        video.src = url;
        // 🔧 MP4视频使用统一处理函数
        video.addEventListener('loadedmetadata', () => {
          clearTimeout(loadingTimeout);
          handleVideoLoadSuccess('MP4视频');
        }, { once: true });
        
        video.addEventListener('error', (error) => {
          clearTimeout(loadingTimeout);
          handleVideoLoadError(error, 'MP4视频');
        }, { once: true });
      }
    } catch (_) {}

    // 事件：当当前视频开始播放，暂停其它视频
    const onPlay = (e) => {
      pauseOthers(e.currentTarget);
      setIsPlaying(true);
      setShowPlayButton(false);
      const currentVideoId = videoIdRef.current;
      if (currentVideoId && typeof trackPlayRef.current === 'function') {
        try {
          trackPlayRef.current(currentVideoId);
        } catch (_) {}
      }
    };
    
    const onPause = () => {
      setIsPlaying(false);
      // 延迟显示播放按钮，避免在快速切换时闪现
      setTimeout(() => {
        if (videoRef.current && videoRef.current.paused) {
          setShowPlayButton(true);
        }
      }, 300);
      setShowLoadingIndicator(false);
    };
    
    const onTimeUpdate = (e) => {
      const video = e.currentTarget;
      const effectiveDuration = computeEffectiveDuration(video);
      if (effectiveDuration > 0) {
        updateDuration(effectiveDuration);
        const ratio = Math.max(0, Math.min(1, video.currentTime / effectiveDuration));
        setProgress(ratio * 100);
      }
    };
    
    const onLoadedMetadata = (e) => {
      const video = e.currentTarget;
      updateDuration(computeEffectiveDuration(video));
      // 获取视频的实际显示尺寸
      updateVideoSize();
      // 元数据可用后即安排首帧检测
      scheduleFirstFrameReady('loadedmetadata');
    };
    
    // updateVideoSize函数已在上方定义
    
    // 当视频开始加载时，确保其他视频停止
    const onLoadStart = () => {
      pauseOthers(video);
    };
    
    // 当视频可以播放时，安排首帧检测（不提前隐藏加载）
    const onCanPlay = () => {
      scheduleFirstFrameReady('canplay');
    };
    
    const onWaiting = () => {
      try {
        if (!video.paused && !video.ended) {
          setShowLoadingIndicator(true);
        }
      } catch (_) {}
    };

    const onSeeking = () => {
      try {
        if (!video.paused) {
          setShowLoadingIndicator(true);
        }
      } catch (_) {}
    };

    const onSeekedBufferClear = () => {
      if (!video.paused && showLoadingIndicatorRef.current) {
        setShowLoadingIndicator(false);
      }
    };

    // 当视频正在播放时，确保隐藏加载提示并显示视频
    const onPlaying = () => {
      setShowLoadingIndicator(false);
      setIsPlaying(true);
      // 确保视频已准备好并可见
      if (!isVideoReady) {
        setIsVideoReady(true);
        setIsLoading(false);
        // 更新视频尺寸
        setTimeout(() => updateVideoSize(), 50);
      }
    };
    
    // 当有足够数据播放时，安排首帧检测（不提前隐藏加载）
    const onCanPlayThrough = () => {
      scheduleFirstFrameReady('canplaythrough');
    };

    try { video.addEventListener('play', onPlay, { passive: true }); } catch (_) {}
    try { video.addEventListener('pause', onPause, { passive: true }); } catch (_) {}
    try { video.addEventListener('timeupdate', onTimeUpdate, { passive: true }); } catch (_) {}
    try { video.addEventListener('loadedmetadata', onLoadedMetadata, { passive: true }); } catch (_) {}
    try { video.addEventListener('loadstart', onLoadStart, { passive: true }); } catch (_) {}
    try { video.addEventListener('canplay', onCanPlay, { passive: true }); } catch (_) {}
    try { video.addEventListener('canplaythrough', onCanPlayThrough, { passive: true }); } catch (_) {}
    try { video.addEventListener('playing', onPlaying, { passive: true }); } catch (_) {}
    try { video.addEventListener('waiting', onWaiting, { passive: true }); } catch (_) {}
    try { video.addEventListener('seeking', onSeeking, { passive: true }); } catch (_) {}
    try { video.addEventListener('seeked', onSeekedBufferClear, { passive: true }); } catch (_) {}
    try { video.addEventListener('resize', updateVideoSize, { passive: true }); } catch (_) {}
    
    // 监听窗口大小变化
    const handleResize = () => updateVideoSize();
    try { window.addEventListener('resize', handleResize); } catch (_) {}
    
    return () => {
      // 重置状态
      setIsVideoReady(false);
      setIsLoading(true);
      setIsPlaying(false);
      setProgress(0);
      updateDuration(0);
      setVideoSize({ width: 0, height: 0 });
      setShowPlayButton(false);
      setShowLoadingIndicator(false);
      
      try { if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; } } catch (_) {}
      if (nativeBlobUrlRef.current) {
        try { URL.revokeObjectURL(nativeBlobUrlRef.current); } catch (_) {}
        nativeBlobUrlRef.current = null;
      }
      if (video) {
        try { video.removeEventListener('play', onPlay); } catch (_) {}
        try { video.removeEventListener('pause', onPause); } catch (_) {}
        try { video.removeEventListener('timeupdate', onTimeUpdate); } catch (_) {}
        try { video.removeEventListener('loadedmetadata', onLoadedMetadata); } catch (_) {}
        try { video.removeEventListener('loadstart', onLoadStart); } catch (_) {}
        try { video.removeEventListener('canplay', onCanPlay); } catch (_) {}
        try { video.removeEventListener('canplaythrough', onCanPlayThrough); } catch (_) {}
        try { video.removeEventListener('playing', onPlaying); } catch (_) {}
        try { video.removeEventListener('waiting', onWaiting); } catch (_) {}
        try { video.removeEventListener('seeking', onSeeking); } catch (_) {}
        try { video.removeEventListener('seeked', onSeekedBufferClear); } catch (_) {}
        try { video.removeEventListener('resize', updateVideoSize); } catch (_) {}
        try { 
          video.pause(); 
          video.currentTime = 0;
        } catch (_) {}
        video.removeAttribute('src');
        try { video.load(); } catch(_){}
      }
      try { window.removeEventListener('resize', handleResize); } catch (_) {}
    // 🔧 清理加载超时定时器
      clearTimeout(loadingTimeout);
    };
  }, [getActiveDuration, url, updateDuration]);
  
  // 监听活动状态变化，当变为活动状态时播放，非活动时暂停
  React.useEffect(() => {
    const video = videoRef.current;
    if (!video || !isVideoReady) return; // 只有视频准备就绪后才处理状态变化
    
    // ensureUnmuted函数已在上方定义
    
    if (isActive) {
      setIsPlaying(true); // 立即设置播放状态，避免播放按钮闪现
      setShowPlayButton(false);
      setTimeout(() => {
        try {
          ensureUnmuted();
          video.play().then(() => {
            setIsPlaying(true);
          }).catch((error) => {
            setIsPlaying(false);
            setShowPlayButton(true);
          });
        } catch (_) {}
      }, 50);
    } else {
      setIsPlaying(false);
      setShowPlayButton(false); // 非活动状态不显示播放按钮
      try {
        video.pause();
      } catch (_) {}
    }
  }, [isActive, url, isVideoReady]);

  return (
    <div
      className="flex items-center justify-center"
      style={{ width: '100%', height: '100%', position: 'relative' }}
    >
      {onRequestClose && (
        <div
          className="pc-video-modal-backdrop"
          style={{
            position: 'absolute',
            inset: 0,
            background: 'rgba(0,0,0,0.65)',
            zIndex: 0
          }}
        />
      )}
      {/* 固定宽度的背景模糊条，不随视频变化 */}
      <div
        aria-hidden
        className="pointer-events-none"
        style={{
          position: 'absolute', inset: 0,
          display: 'flex', alignItems: 'stretch', justifyContent: 'center',
          zIndex: 1
        }}
      >
        <div
          style={{
            width: `${REGION_WIDTH}px`, height: '100%',
            backdropFilter: 'blur(24px) brightness(0.9)', WebkitBackdropFilter: 'blur(24px) brightness(0.9)',
            background: 'rgba(0,0,0,0.06)'
          }}
        />
      </div>
      {/* 居中主视频，宽度不超过固定显示区 */}
      <div className="pc-video-modal-interactive" style={{ width: `${REGION_WIDTH}px`, display: 'flex', justifyContent: 'center', position: 'relative', zIndex: 2 }}>
        
        {/* 封面图片 - 只在视频未准备好时显示（使用解密组件） */}
        {poster && !isVideoReady && (
          <SecureDecryptedImage
            src={poster}
            alt={title || '视频封面'}
            lazyLoad={false}
            priority="high"
            showLoadingIndicator={false}
            objectFit="contain"
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              maxWidth: '100%',
              maxHeight: '80vh',
              zIndex: 3,
              borderRadius: '8px',
              opacity: 1,
              transition: 'opacity 0.3s ease'
            }}
          />
        )}
        
        <div
          ref={videoWrapperRef}
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: isVideoReady && isPlaying ? 6 : 0,
            position: 'relative'
          }}
        >
          <video
            ref={(el) => { 
              videoRef.current = el; 
            }}
            controls={false} // 像移动端一样，不显示默认控件
            data-ready={isVideoReady} // 用于CSS选择器
            playsInline
            muted={false}
            autoPlay={false}
            preload="auto"
            controlsList="nofullscreen noplaybackrate nodownload"
            disablePictureInPicture
            onContextMenu={(e) => e.preventDefault()}
            className="lightbox-video"
            onClick={(e) => {
              // 点击视频切换播放/暂停
              if (isVideoReady) {
                const video = e.currentTarget;
                if (video.paused) {
                  video.play();
                } else {
                  video.pause();
                }
              }
            }}
            style={{ 
              maxWidth: '100%', 
              maxHeight: '80vh', 
              background: 'transparent',
              visibility: isVideoReady ? 'visible' : 'hidden',
              opacity: isVideoReady ? 1 : 0,
              transition: 'opacity 0.3s ease',
              cursor: isVideoReady ? 'pointer' : 'default',
              position: 'relative',
              zIndex: 2
            }}
          />

          {isVideoReady && isPlaying && videoSize.width > 0 && (
            <div 
              style={{
                width: '100%',
                height: '6px',
                background: 'rgba(255,255,255,0.18)',
                borderRadius: '999px',
                cursor: 'pointer',
                touchAction: 'none',
                boxShadow: '0 0 8px rgba(0, 0, 0, 0.25)'
              }}
              onPointerDown={handleProgressPointerDown}
              onPointerMove={handleProgressPointerMove}
              onPointerUp={handleProgressPointerUp}
              onPointerCancel={handleProgressPointerCancel}
              role="slider"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={Math.round(Math.max(0, Math.min(100, progress)))}
            >
              <div 
                style={{
                  height: '100%',
                  background: 'linear-gradient(90deg, rgba(255,255,255,0.7) 0%, rgba(255,255,255,0.95) 100%)',
                  borderRadius: '999px',
                  width: `${Math.max(0, Math.min(100, progress))}%`,
                  transition: progressPointerActiveRef.current ? 'none' : 'width 0.12s ease'
                }}
              />
            </div>
          )}
        </div>
        
        {/* 加载指示器 - 视频加载时显示 */}
        {showLoadingIndicator && (
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '16px',
              zIndex: 10
            }}
          >
            <div
              style={{
                width: '48px',
                height: '48px',
                border: '4px solid rgba(255, 255, 255, 0.3)',
                borderTopColor: '#fff',
                borderRadius: '50%',
                animation: 'pc-video-spin 1s linear infinite'
              }}
            />
            <div
              style={{
                color: '#fff',
                fontSize: '16px',
                fontWeight: '500',
                textShadow: '0 2px 4px rgba(0, 0, 0, 0.5)'
              }}
            >
              视频加载中...
            </div>
          </div>
        )}
        
        {/* 🔧 错误提示 - 视频加载失败时显示 */}
        {loadingError && (
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '16px',
              zIndex: 10,
              background: 'rgba(0, 0, 0, 0.8)',
              padding: '20px',
              borderRadius: '12px'
            }}
          >
            <div
              style={{
                color: '#ff6b6b',
                fontSize: '18px',
                fontWeight: '500',
                textAlign: 'center'
              }}
            >
              {loadingErrorMessage || '视频加载失败'}
            </div>
            <button
              onClick={() => {
                // 🔧 重试机制：重新加载当前视频
                setLoadingError(false);
                setLoadingErrorMessage('');
                setShowLoadingIndicator(true);
                // 触发重新加载
                const video = videoRef.current;
                if (video) {
                  video.load();
                }
              }}
              style={{
                background: '#4CAF50',
                color: 'white',
                border: 'none',
                padding: '10px 20px',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              重试
            </button>
          </div>
        )}
        
        {/* 播放控制按钮 - 悬浮在视频中央，只在活动状态且暂停时显示 */}
        {isVideoReady && isActive && !isPlaying && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              const video = videoRef.current;
              if (video) {
                if (video.paused) {
                  video.play();
                } else {
                  video.pause();
                }
              }
            }}
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              background: 'rgba(255, 255, 255, 0.9)',
              border: 'none',
              color: '#333',
              cursor: 'pointer',
              padding: '20px',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '80px',
              height: '80px',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
              zIndex: 5
            }}
          >
            <TikTokIcon size={32} color="#333" opacity={0.9} />
          </button>
        )}
      </div>
    </div>
  );
};

// PC 端短视频弹层：透明背景 + 上一部/下一部（左右箭头）
const VideoModal = ({ open, onClose, videos = [], videoData = [], index = 0 }) => {
  const [currentIndex, setCurrentIndex] = React.useState(index);
  const [preloadedVideos, setPreloadedVideos] = React.useState(new Set());
  const [isInitialLoad, setIsInitialLoad] = React.useState(true); // 标记是否为初始加载（点击列表）

  const trackViewCount = React.useCallback(async (videoId) => {
    if (!videoId) return;
    if (!statsTracker.startTracking(videoId, 'view')) {
      return;
    }

    try {
      const response = await videoStatsService.incrementViewCount(videoId);
      statsTracker.finishTracking(videoId, 'view', response?.code === 200);
    } catch (_) {
      statsTracker.finishTracking(videoId, 'view', false);
    }
  }, []);

  const trackPlayCount = React.useCallback(async (videoId) => {
    if (!videoId) return;
    if (!statsTracker.startTracking(videoId, 'play')) {
      return;
    }

    try {
      const response = await videoStatsService.incrementPlayCount(videoId);
      statsTracker.finishTracking(videoId, 'play', response?.code === 200);
    } catch (_) {
      statsTracker.finishTracking(videoId, 'play', false);
    }
  }, []);
  
  const slides = useMemo(() => {
    const result = Array.isArray(videos) ? videos.filter((v) => !!v).map((src, idx) => ({ 
      src,
      poster: videoData[idx]?.coverImageUrl || null,
      title: videoData[idx]?.title || ''
    })) : [];
    return result;
  }, [videos, videoData, index]);
  
  // 当外部index变化时，更新内部状态
  React.useEffect(() => {
    setCurrentIndex(index);
    // 外部index变化通常意味着用户点击了列表中的视频（初始加载）
    setIsInitialLoad(true);
  }, [index]);

  React.useEffect(() => {
    if (!open) {
      return;
    }
    if (!Array.isArray(videoData) || videoData.length === 0) {
      return;
    }
    if (currentIndex < 0 || currentIndex >= videoData.length) {
      return;
    }
    const currentVideo = videoData[currentIndex];
    const videoId = currentVideo?.id;
    if (!videoId) {
      return;
    }
    trackViewCount(videoId);
  }, [open, currentIndex, videoData, trackViewCount]);

  // 当弹窗打开状态变化时，重置初始加载标记
  React.useEffect(() => {
    if (open) {
      setIsInitialLoad(true); // 弹窗打开时标记为初始加载
    } else {
      setPreloadedVideos(new Set()); // 弹窗关闭时清理预加载状态
    }
  }, [open]);

  // 添加键盘ESC键关闭功能
  React.useEffect(() => {
    if (!open) return;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape' || event.keyCode === 27) {
        event.preventDefault();
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [open, onClose]);

  // PC端预加载逻辑：当前视频±1个视频
  const preloadVideos = React.useCallback((currentIdx) => {
    if (!open || !slides || slides.length === 0) return;
    
    const toPreload = [];
    
    // 当前视频
    if (currentIdx >= 0 && currentIdx < slides.length) {
      toPreload.push(currentIdx);
    }
    
    // 前一个视频
    if (currentIdx > 0) {
      toPreload.push(currentIdx - 1);
    }
    
    // 后一个视频
    if (currentIdx < slides.length - 1) {
      toPreload.push(currentIdx + 1);
    }


    toPreload.forEach(idx => {
      if (preloadedVideos.has(idx)) return; // 已预加载
      
      const videoUrl = slides[idx]?.src;
      if (!videoUrl) return;

      // 创建隐藏的video元素进行预加载
      const preloadVideo = document.createElement('video');
      preloadVideo.preload = 'auto';
      preloadVideo.muted = true;
      preloadVideo.style.display = 'none';
      
      const isHls = /\.m3u8(\?|$)/i.test(videoUrl);
      
      if (isHls && Hls.isSupported()) {
        const hls = new Hls({ xhrSetup: hlsXhrSetup });
        hls.loadSource(videoUrl);
        hls.attachMedia(preloadVideo);
        
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          setPreloadedVideos(prev => {
            const newSet = new Set(prev);
            newSet.add(idx);
            return newSet;
          });
          // 清理预加载资源
          setTimeout(() => {
            try {
              hls.destroy();
              preloadVideo.remove();
            } catch (_) {}
          }, 1000);
        });
      } else {
        preloadVideo.src = videoUrl;
        preloadVideo.addEventListener('loadedmetadata', () => {
          setPreloadedVideos(prev => {
            const newSet = new Set(prev);
            newSet.add(idx);
            return newSet;
          });
          // 清理预加载资源
          setTimeout(() => {
            try {
              preloadVideo.remove();
            } catch (_) {}
          }, 1000);
        }, { once: true });
      }
      
      // 添加到DOM以开始加载
      document.body.appendChild(preloadVideo);
    });
  }, [open, slides, preloadedVideos]);

  // 当弹窗打开或当前索引变化时触发预加载
  React.useEffect(() => {
    if (open && slides.length > 0) {
      // 延迟预加载，避免阻塞当前视频
      setTimeout(() => {
        preloadVideos(currentIndex);
      }, 200);
    }
  }, [open, currentIndex, slides.length, preloadVideos]);

  const vw = typeof window !== 'undefined' ? window.innerWidth : 1200;
  const regionLeft = Math.max(16, Math.round((vw - REGION_WIDTH) / 2));
  const BUTTON_SIZE = 44; // 与样式保持一致
  const OUTER_GAP = 12; // 与背景层外侧的距离

  // 在最后一个视频时，提示用户滚动主页面或自动滚动
  React.useEffect(() => {
    if (!open) return;
    
    const total = slides.length;
    const isLastSlide = currentIndex >= total - 1 && total > 0;
    

    let btn = null;
    
    const clickHandler = (e) => {
      if (currentIndex >= slides.length - 1 && slides.length > 0) {
        e.preventDefault();
        e.stopPropagation();
        
        // 显示提示信息
        const showScrollHint = () => {
          const hint = document.createElement('div');
          hint.innerHTML = '已到达最后一个视频<br/>请滚动主页面查看更多内容';
          hint.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0, 0, 0, 0.85);
            color: white;
            padding: 20px 30px;
            border-radius: 12px;
            font-size: 16px;
            text-align: center;
            line-height: 1.5;
            z-index: 10001;
            pointer-events: none;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
          `;
          document.body.appendChild(hint);
          
          // 4秒后移除提示
          setTimeout(() => {
            if (hint.parentNode) {
              hint.parentNode.removeChild(hint);
            }
          }, 4000);
        };
        
        showScrollHint();
      }
    };

    const keyHandler = (e) => {
      if (currentIndex >= slides.length - 1 && slides.length > 0 && (e.key === 'ArrowRight' || e.keyCode === 39)) {
        e.preventDefault();
        
        // 调用相同的处理逻辑
        clickHandler({ preventDefault: () => {}, stopPropagation: () => {} });
      }
    };

    // 使用定时器确保按钮已渲染，并尝试多种选择器
    const timer = setTimeout(() => {
      // 尝试多种可能的选择器
      btn = document.querySelector('.yarl__navigation_next') || 
            document.querySelector('[aria-label="Next slide"]') ||
            document.querySelector('.yarl__button_next') ||
            document.querySelector('.yarl__navigation button:last-child');
      
      
      if (btn) {
        // 移除可能存在的旧监听器
        btn.removeEventListener('click', clickHandler, true);
        // 添加新监听器
        btn.addEventListener('click', clickHandler, true);
      } else {
        // 如果没找到，再延迟一点时间重试
        setTimeout(() => {
          btn = document.querySelector('.yarl__navigation_next') || 
                document.querySelector('[aria-label="Next slide"]') ||
                document.querySelector('.yarl__button_next') ||
                document.querySelector('.yarl__navigation button:last-child');
          if (btn) {
            btn.addEventListener('click', clickHandler, true);
          }
        }, 200);
      }
      
      try { window.addEventListener('keydown', keyHandler, true); } catch (_) {}
    }, 100);

    return () => {
      clearTimeout(timer);
      try { if (btn) btn.removeEventListener('click', clickHandler, true); } catch (_) {}
      try { window.removeEventListener('keydown', keyHandler, true); } catch (_) {}
    };
  }, [open, currentIndex, slides.length, onClose]);

  return (
    <>
      <style>{`
        /* 调整容器的指针样式 */
        .yarl__container {
          cursor: default !important;
        }
        .yarl__slide {
          cursor: default !important;
        }
        .yarl__slide > div {
          cursor: default !important;
        }
        
        /* 保持按钮白底圆形，但使用库内置的通用图标（上一部/下一部/关闭） */
        .yarl__button { 
          background: #ffffff !important; 
          color: #111111 !important; 
          border-radius: 9999px !important; 
          box-shadow: 0 6px 18px rgba(0,0,0,0.35) !important;
          width: 44px !important; height: 44px !important;
          display: flex !important; align-items: center !important; justify-content: center !important; padding: 0 !important;
        }
        .yarl__button svg { display: block !important; width: 20px !important; height: 20px !important; margin: 0 !important; }
        /* 垂直居中放置于背景层外侧 */
        .yarl__navigation_prev, .yarl__navigation_next { top: 50% !important; transform: translateY(-50%) !important; }
        
        
        /* 强制隐藏加载中的视频控件 */
        .lightbox-video:not([data-ready="true"]) {
          pointer-events: none !important;
        }
        .lightbox-video:not([data-ready="true"])::-webkit-media-controls-panel {
          display: none !important;
        }
        .lightbox-video:not([data-ready="true"])::-webkit-media-controls {
          display: none !important;
        }
        
        /* PC端视频加载动画 */
        @keyframes pc-video-spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
      <Lightbox
        open={open}
        close={onClose}
        slides={slides}
        index={index}
        animation={{ fade: 0, swipe: 0 }}
        carousel={{ finite: false }}
        controller={{ 
          closeOnPullDown: false, 
          closeOnBackdropClick: false,
          closeOnPullUp: false,
          touchAction: 'none'
        }}
        on={{
          click: () => {
            // 由于Lightbox的click事件参数结构不确定，我们主要依赖自定义的背景覆盖层
            // 这里可以作为备用的关闭方法
          },
          view: (params) => {
            // 更新当前活动的slide索引
            const viewIndex = params?.index ?? 0;
            
            // 检查是否是从最后一个视频尝试进入下一个（循环到第一个）
            if (currentIndex === slides.length - 1 && viewIndex === 0 && slides.length > 1) {
              
              // 显示提示信息
              const hint = document.createElement('div');
              hint.innerHTML = '已到达最后一个视频<br/>请滚动主页面查看更多内容';
              hint.style.cssText = `
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                background: rgba(0, 0, 0, 0.85);
                color: white;
                padding: 20px 30px;
                border-radius: 12px;
                font-size: 16px;
                text-align: center;
                line-height: 1.5;
                z-index: 10001;
                pointer-events: none;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
              `;
              document.body.appendChild(hint);
              
              // 4秒后移除提示
              setTimeout(() => {
                if (hint.parentNode) {
                  hint.parentNode.removeChild(hint);
                }
              }, 4000);
              
              // 保持在最后一个视频，不要循环
              setTimeout(() => {
                setCurrentIndex(slides.length - 1);
              }, 50);
              return;
            }
            
            setCurrentIndex(viewIndex);
            // 在弹窗内导航时，标记为非初始加载
            setIsInitialLoad(false);
          }
        }}
        styles={{
          container: { backgroundColor: 'transparent' },
          navigationPrev: { left: `${Math.max(8, regionLeft - (BUTTON_SIZE + OUTER_GAP))}px`, zIndex: 1000000, transform: 'translateY(-50%)' },
          navigationNext: { right: `${Math.max(8, regionLeft - (BUTTON_SIZE + OUTER_GAP))}px`, zIndex: 1000000, transform: 'translateY(-50%)' }
        }}
        render={{
          slide: ({ slide }) => {
            const slideIndex = slides.findIndex(s => s.src === slide.src);
            const slideData = slide;
            const slideInfo = Array.isArray(videoData) && slideIndex >= 0 ? videoData[slideIndex] : null;
            const videoId = slideInfo?.id;
            return (
              <HlsSlide 
                url={slideData.src}
                poster={slideData.poster || null}
                title={slideData.title || ''}
                vidKey={slideIndex} 
                videoId={videoId}
                isActive={slideIndex === currentIndex}
                isPreloaded={preloadedVideos.has(slideIndex)}
                isInitialLoad={isInitialLoad && slideIndex === currentIndex}
                onRequestClose={onClose}
                onTrackPlay={trackPlayCount}
                key={slide.src} 
              />
            );
          }
        }}
      />
    </>
  );
};

export default VideoModal;
