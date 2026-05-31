import React, { useMemo, useRef, useState, useEffect, useCallback } from 'react';
import ReactDOM from 'react-dom';
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '../../utils/hlsUtils';
import videoStatsService from '../../services/videoStatsService';
import statsTracker from '../../utils/statsTracker';
import './PagedVideoModal.css';

// 固定显示区宽度
const REGION_WIDTH = 850;

// 查询所有播放器中的视频元素
const queryLightboxVideos = () => {
  try {
    return Array.from(document.querySelectorAll('.paged-lightbox-video'));
  } catch (_) {
    return [];
  }
};

// 暂停其他视频
const pauseOthers = (keepEl) => {
  try {
    queryLightboxVideos().forEach(v => {
      if (!keepEl || v !== keepEl) {
        try { 
          if (v instanceof HTMLVideoElement) {
            v.pause();
            v.currentTime = 0;
          }
        } catch (_) {}
      }
    });
  } catch (_) {}
};

// 计算有效时长
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

// 视频播放器组件
const VideoPlayer = ({ 
  url, 
  poster, 
  title, 
  tags = [], 
  videoId, 
  isActive = false, 
  onTrackPlay = null,
  onClose,
  onTagClick = null
}) => {
  const videoRef = useRef(null);
  const hlsRef = useRef(null);
  const nativeBlobUrlRef = useRef(null); // iOS 原生 HLS：blob URL 引用，卸载时释放
  const [isVideoReady, setIsVideoReady] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [showLoadingIndicator, setShowLoadingIndicator] = useState(false);
  const [loadingError, setLoadingError] = useState(false);
  const [loadingErrorMessage, setLoadingErrorMessage] = useState('');
  const durationRef = useRef(0);
  const videoIdRef = useRef(videoId);
  const trackPlayRef = useRef(onTrackPlay);
  const loadingTimeoutRef = useRef(null);
  const seekingRef = useRef(false);
  const [activeSeekButton, setActiveSeekButton] = useState(null);

  // 更新引用
  useEffect(() => {
    videoIdRef.current = videoId;
    trackPlayRef.current = onTrackPlay;
  }, [videoId, onTrackPlay]);

  // 更新时长
  const updateDuration = useCallback((newDuration) => {
    if (newDuration > 0 && newDuration !== durationRef.current) {
      durationRef.current = newDuration;
    }
  }, []);

  // 获取有效时长
  const getActiveDuration = useCallback(() => {
    const video = videoRef.current;
    if (!video) return durationRef.current || 0;
    return computeEffectiveDuration(video) || durationRef.current || 0;
  }, []);

  // 快进/倒退函数
  const seekVideo = useCallback((seconds, buttonId) => {
    const video = videoRef.current;
    if (!video || !isVideoReady) return;
    
    if (buttonId) {
      setActiveSeekButton(buttonId);
      setTimeout(() => {
        setActiveSeekButton(null);
      }, 2000);
    }
    
    try {
      const currentTime = video.currentTime || 0;
      const duration = getActiveDuration();
      const newTime = Math.max(0, Math.min(duration, currentTime + seconds));
      video.currentTime = newTime;
    } catch (error) {
      // 忽略错误
    }
  }, [isVideoReady, getActiveDuration]);

  // 初始化HLS和视频事件监听
  useEffect(() => {
    if (!isActive || !url) {
      return;
    }
    
    const video = videoRef.current;
    if (!video) {
      return;
    }

    // 暂停其他视频
    pauseOthers(video);

    // 设置初始加载状态
    setIsLoading(true);
    setIsVideoReady(false);
    setLoadingError(false);
    setShowLoadingIndicator(true);

    // 设置加载超时
    loadingTimeoutRef.current = setTimeout(() => {
      if (!isVideoReady) {
        setLoadingError(true);
        setLoadingErrorMessage('视频加载超时，请重试');
        setShowLoadingIndicator(false);
      }
    }, 30000);

    // 初始化HLS
    if (url.includes('.m3u8')) {
      if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: false,
        backBufferLength: 90,
        xhrSetup: hlsXhrSetup,
      });
      
      hls.loadSource(url);
      hls.attachMedia(video);
      
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
          setIsLoading(false);
      });
      
      hls.on(Hls.Events.ERROR, (event, data) => {
        if (data.fatal) {
            setLoadingError(true);
            setLoadingErrorMessage('视频加载失败');
            setShowLoadingIndicator(false);
        }
      });

        hlsRef.current = hls;
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // iOS Safari 原生 HLS：重写 m3u8 中的 key URI 为绝对后端地址
        patchNativeHlsM3u8(url).then(({ url: patchedUrl, isBlob }) => {
          if (nativeBlobUrlRef.current) {
            try { URL.revokeObjectURL(nativeBlobUrlRef.current); } catch (_) {}
          }
          nativeBlobUrlRef.current = isBlob ? patchedUrl : null;
          video.src = patchedUrl;
        });
      }
    } else {
      video.src = url;
    }

    // 事件监听器
    const onLoadedMetadata = () => {
      const duration = computeEffectiveDuration(video);
      updateDuration(duration);
    };
    
    const onCanPlay = () => {
      setIsVideoReady(true);
      setIsLoading(false);
      setShowLoadingIndicator(false);
      if (loadingTimeoutRef.current) {
        clearTimeout(loadingTimeoutRef.current);
      }
    };
    
    const onWaiting = () => {
      if (!seekingRef.current) {
          setShowLoadingIndicator(true);
        }
    };
    
    const onPlaying = () => {
      setShowLoadingIndicator(false);
      if (!isVideoReady) {
        setIsVideoReady(true);
        setIsLoading(false);
      }
      // 统计播放次数
      if (trackPlayRef.current && videoIdRef.current) {
        trackPlayRef.current(videoIdRef.current);
      }
    };
    
    const onSeeking = () => {
      seekingRef.current = true;
      setShowLoadingIndicator(false);
    };

    const onSeeked = () => {
      seekingRef.current = false;
    };

    try { video.addEventListener('loadedmetadata', onLoadedMetadata, { passive: true }); } catch (_) {}
    try { video.addEventListener('canplay', onCanPlay, { passive: true }); } catch (_) {}
    try { video.addEventListener('waiting', onWaiting, { passive: true }); } catch (_) {}
    try { video.addEventListener('playing', onPlaying, { passive: true }); } catch (_) {}
    try { video.addEventListener('seeking', onSeeking, { passive: true }); } catch (_) {}
    try { video.addEventListener('seeked', onSeeked, { passive: true }); } catch (_) {}

    return () => {
      try { video.removeEventListener('loadedmetadata', onLoadedMetadata); } catch (_) {}
      try { video.removeEventListener('canplay', onCanPlay); } catch (_) {}
      try { video.removeEventListener('waiting', onWaiting); } catch (_) {}
      try { video.removeEventListener('playing', onPlaying); } catch (_) {}
      try { video.removeEventListener('seeking', onSeeking); } catch (_) {}
      try { video.removeEventListener('seeked', onSeeked); } catch (_) {}
      
      if (hlsRef.current) {
        try {
          hlsRef.current.destroy();
          hlsRef.current = null;
        } catch (_) {}
      }
      if (nativeBlobUrlRef.current) {
        try { URL.revokeObjectURL(nativeBlobUrlRef.current); } catch (_) {}
        nativeBlobUrlRef.current = null;
      }
      
      if (loadingTimeoutRef.current) {
        clearTimeout(loadingTimeoutRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActive, url]);

  return (
    <div className="paged-video-content">
      {/* 标题 */}
      {title && (
        <div className="paged-video-info-title" style={{ marginBottom: '8px', padding: '16px 16px 0 16px' }}>
          {title}
        </div>
      )}
      
      {/* 标签 */}
      {tags && Array.isArray(tags) && tags.length > 0 && (
        <div className="paged-video-info-tags" style={{ padding: '0 16px 12px 16px' }}>
          {tags.map((tag, idx) => {
            let tagName = '';
            if (typeof tag === 'string') {
              tagName = tag;
            } else if (tag && typeof tag === 'object') {
              tagName = tag.name || tag.tag || tag.title || String(tag);
            } else {
              tagName = String(tag || '');
            }

            if (!tagName || tagName.trim() === '') {
              return null;
            }

            // 只有对象标签（含 id）才支持点击筛选
            const isClickable = onTagClick && tag && typeof tag === 'object' && tag.id;

            return (
              <span
                key={idx}
                className={`paged-video-info-tag${isClickable ? ' paged-video-info-tag-clickable' : ''}`}
                onClick={isClickable ? () => onTagClick(tag) : undefined}
                title={isClickable ? `按"${tagName}"筛选` : undefined}
              >
                {tagName}
              </span>
            );
          }).filter(Boolean)}
        </div>
      )}
      
      {/* 视频区域 */}
      <div className="paged-video-wrapper">
        <div className="paged-video-inner-wrapper">
            <video
              ref={videoRef}
              controls={true}
              data-ready={isVideoReady}
              poster={poster || undefined}
              playsInline
              muted={false}
              autoPlay={false}
              preload="metadata"
              controlsList="nodownload"
              disablePictureInPicture
              className="paged-lightbox-video"
              onDoubleClick={(e) => {
                if (isVideoReady) {
                  e.preventDefault();
                  e.stopPropagation();
                  const video = e.currentTarget;
                  try {
                    if (!document.fullscreenElement) {
                      video.requestFullscreen().catch(() => {});
                    } else {
                      document.exitFullscreen().catch(() => {});
                    }
                  } catch (_) {}
                }
                  }}
                />
              </div>

            {/* 加载指示器 */}
            {showLoadingIndicator && (
          <div className="paged-video-loading-overlay">
                <div className="paged-video-loading-spinner" />
              </div>
            )}

            {/* 错误提示 */}
            {loadingError && (
          <div className="paged-video-error-overlay">
            <div className="paged-video-error-title">{loadingErrorMessage || '视频加载失败'}</div>
                <button
              className="paged-video-error-retry"
                  onClick={() => {
                    setLoadingError(false);
                    setLoadingErrorMessage('');
                    setShowLoadingIndicator(true);
                    const video = videoRef.current;
                    if (video) {
                      video.load();
                    }
                  }}
                >
                  重试
                </button>
              </div>
            )}
      </div>

      {/* 信息区域 */}
      {isActive && (
        <div className="paged-video-info">
          {/* 快进/倒退控制 */}
          {isVideoReady && (
            <div className="paged-video-seek-controls">
              {/* 倒退控制 */}
              <div className="paged-seek-control-group">
                <button
                  className={`paged-seek-button ${activeSeekButton === 'rewind-10m' ? 'paged-seek-button-active' : ''}`}
                  onClick={() => seekVideo(-600, 'rewind-10m')}
                  aria-label="倒退10分钟"
                >
                  &lt;&lt; 10m
                </button>
                <button
                  className={`paged-seek-button ${activeSeekButton === 'rewind-1m' ? 'paged-seek-button-active' : ''}`}
                  onClick={() => seekVideo(-60, 'rewind-1m')}
                  aria-label="倒退1分钟"
                >
                  &lt; 1m
                </button>
              <button
                  className={`paged-seek-button ${activeSeekButton === 'rewind-10s' ? 'paged-seek-button-active' : ''}`}
                  onClick={() => seekVideo(-10, 'rewind-10s')}
                  aria-label="倒退10秒"
                >
                  &lt; 10s
              </button>
          </div>

              {/* 快进控制 */}
              <div className="paged-seek-control-group">
                <button
                  className={`paged-seek-button ${activeSeekButton === 'forward-10s' ? 'paged-seek-button-active' : ''}`}
                  onClick={() => seekVideo(10, 'forward-10s')}
                  aria-label="快进10秒"
                >
                  10s &gt;
                </button>
                <button
                  className={`paged-seek-button ${activeSeekButton === 'forward-1m' ? 'paged-seek-button-active' : ''}`}
                  onClick={() => seekVideo(60, 'forward-1m')}
                  aria-label="快进1分钟"
                >
                  1m &gt;
                </button>
                <button
                  className={`paged-seek-button ${activeSeekButton === 'forward-10m' ? 'paged-seek-button-active' : ''}`}
                  onClick={() => seekVideo(600, 'forward-10m')}
                  aria-label="快进10分钟"
                >
                  10m &gt;&gt;
                </button>
                </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// 主组件
const PagedVideoModal = ({ open, onClose, videos = [], videoData = [], index = 0, onTagClick }) => {
  const [currentIndex, setCurrentIndex] = useState(index);
  const trackedVideosRef = useRef(new Set());
  
  const trackViewCount = useCallback(async (videoId) => {
    if (!videoId) return;
    
    if (trackedVideosRef.current.has(videoId)) {
      return;
    }
    
    if (!statsTracker.startTracking(videoId, 'view')) {
      return;
    }
    
    trackedVideosRef.current.add(videoId);
    
    try {
      const response = await videoStatsService.incrementViewCount(videoId);
      statsTracker.finishTracking(videoId, 'view', response?.code === 200);
    } catch (_) {
      statsTracker.finishTracking(videoId, 'view', false);
    }
  }, []);

  const trackPlayCount = useCallback(async (videoId) => {
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
  
  useEffect(() => {
    setCurrentIndex(index);
  }, [index]);

  // 统计视频观看次数
  useEffect(() => {
    if (!open) return;
    if (!Array.isArray(videoData) || videoData.length === 0) return;
    if (currentIndex < 0 || currentIndex >= videoData.length) return;
    
    const currentVideo = videoData[currentIndex];
    const videoId = currentVideo?.id;
    if (!videoId) return;
    
    if (!trackedVideosRef.current.has(videoId)) {
      trackViewCount(videoId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, currentIndex]);

  useEffect(() => {
    if (!open) {
      trackedVideosRef.current.clear();
    }
  }, [open]);

  // ESC键关闭
  useEffect(() => {
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

  if (!open) return null;

  // 当前视频数据
  const currentVideo = videoData[currentIndex] || {};
  const videoUrl = videos[currentIndex] || '';
            let tags = [];
  if (Array.isArray(currentVideo.tagList) && currentVideo.tagList.length > 0) {
    tags = currentVideo.tagList;
  } else if (Array.isArray(currentVideo.tags) && currentVideo.tags.length > 0) {
    tags = currentVideo.tags;
  }

  // 使用 Portal 渲染到 body，确保不被任何父元素遮挡
  return ReactDOM.createPortal(
    <div className="paged-video-modal-overlay" onClick={onClose}>
      <div 
        className="paged-video-modal-container" 
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: `${REGION_WIDTH}px` }}
      >
        {/* 关闭按钮 */}
        <button
          onClick={onClose}
          className="paged-video-close-button"
          aria-label="关闭"
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 20 20"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M15 5L5 15M5 5L15 15"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>

        {/* 视频播放器 */}
        <VideoPlayer
          url={videoUrl}
          poster={currentVideo.coverImageUrl || null}
          title={currentVideo.title || ''}
          tags={tags}
          videoId={currentVideo.id || null}
          isActive={true}
          onTrackPlay={trackPlayCount}
          onClose={onClose}
          onTagClick={onTagClick}
        />
      </div>
    </div>,
    document.body
  );
};

export default PagedVideoModal;
