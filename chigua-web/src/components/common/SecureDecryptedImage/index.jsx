import React, { useState, useEffect, useRef, forwardRef } from 'react';
import blobImageDecryption from '../../../utils/blobImageDecryption';
import imageBlobCache from '../../../utils/imageBlobCache';

/**
 * 安全的防盗版图片组件（基于Blob URL）
 * 
 * 新的实现策略：
 * 1. 前端解密：保持现有的解密逻辑
 * 2. Blob URL：解密后创建Blob URL，使用原生img标签显示
 * 3. 内存管理：自动管理Blob URL的生命周期
 * 4. 性能优化：缓存解密结果，避免重复解密
 */
/**
 * @typedef {Object} SecureDecryptedImageProps
 * @property {string} src
 * @property {string} [alt]
 * @property {string} [className]
 * @property {Object} [style]
 * @property {string} [imageClassName]
 * @property {Object} [imageStyle]
 * @property {'contain'|'cover'|'fill'|'none'|'scale-down'} [objectFit]
 * @property {string} [fallbackSrc]
 * @property {boolean} [asBackground]
 * @property {React.ReactNode} [children]
 * @property {(e: any) => void} [onLoad]
 * @property {(e: any) => void} [onError]
 * @property {boolean} [lazyLoad]
 * @property {'low'|'normal'|'high'} [priority]
 * @property {boolean} [showLoadingIndicator]
 */

/**
 * @param {SecureDecryptedImageProps} props
 */
const SecureDecryptedImage = forwardRef(({
  src,
  alt = '',
  className = '',
  style = {},
  imageClassName = '',
  imageStyle = {},
  objectFit = 'cover',
  fallbackSrc = '/800x700.png',
  asBackground = false,
  children,
  onLoad = () => {},
  onError = () => {},
  lazyLoad = true,
  priority = 'normal',
  showLoadingIndicator = true
}, ref) => {
  const [blobUrl, setBlobUrl] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [shouldLoad, setShouldLoad] = useState(!lazyLoad); // 如果禁用懒加载，立即开始加载
  const [isVisible, setIsVisible] = useState(false); // 是否在可见区域
  
  const containerRef = useRef(null);
  const imgRef = useRef(null);
  const observerRef = useRef(null);
  const currentBlobUrlRef = useRef(null);

  // 合并外部ref和内部containerRef
  useEffect(() => {
    if (ref) {
      if (typeof ref === 'function') {
        ref(containerRef.current);
      } else if (ref.current !== undefined) {
        ref.current = containerRef.current;
      }
    }
  }, [ref]);

  // 懒加载Intersection Observer
  useEffect(() => {
    if (!lazyLoad || shouldLoad) return;

    const setupIntersectionObserver = () => {
      const container = containerRef.current;
      if (!container) return;

      observerRef.current = new IntersectionObserver(
        (entries) => {
          const entry = entries[0];
          if (entry.isIntersecting) {
            setIsVisible(true);
            setShouldLoad(true);
            observerRef.current?.disconnect();
          }
        },
        {
          rootMargin: '200px', // 提前200px开始加载
          threshold: [0, 0.1]
        }
      );

      observerRef.current.observe(container);
    };

    setupIntersectionObserver();

    return () => {
      observerRef.current?.disconnect();
    };
  }, [lazyLoad, shouldLoad]);

  // 主要的图片加载和解密逻辑
  useEffect(() => {
    const loadAndDecryptImage = async () => {
      if (!src || !shouldLoad) {
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        setError(false);
        setErrorMessage('');

        // 先查缓存，命中则直接使用，最小改动命中路径
        const cached = imageBlobCache.get(src);
        if (cached) {
          const isBlobUrl = typeof cached === 'string' && cached.startsWith('blob:');
          const isValidCachedUrl = !isBlobUrl || blobImageDecryption.isValidBlobUrl(cached);

          if (!isValidCachedUrl) {
            imageBlobCache.del(src);
          } else if (containerRef.current) {
            setBlobUrl(cached);
            currentBlobUrlRef.current = cached;
            setIsLoading(false);
            return;
          }
        }

        // 动态调整优先级：可见区域的图片优先级更高
        const dynamicPriority = isVisible ? 'high' : (priority === 'high' ? 'normal' : priority);
        
        // 解密图片并获取Blob URL
        const decryptedBlobUrl = await blobImageDecryption.decryptImageToBlob(src, {
          priority: dynamicPriority
        });

        // 检查组件是否仍然挂载
        if (containerRef.current) {
          setBlobUrl(decryptedBlobUrl);
          currentBlobUrlRef.current = decryptedBlobUrl;
          // 写入缓存，供列表页一次性加载后的命中
          try { imageBlobCache.set(src, decryptedBlobUrl); } catch (_) {}
          setIsLoading(false);
        }

      } catch (err) {
        if (containerRef.current) {
          setError(true);
          setErrorMessage(err.message);
          setIsLoading(false);
          
          // 尝试加载fallback图片
          if (fallbackSrc && fallbackSrc !== src) {
            try {
              setBlobUrl(fallbackSrc);
              setError(false);
              setErrorMessage('');
            } catch (fallbackError) {
            }
          }
        }
      }
    };

    loadAndDecryptImage();

    // 清理函数：不主动 revoke 缓存的 blobUrl，避免返回列表后图片失效
    return () => {
      currentBlobUrlRef.current = null;
    };
  }, [src, fallbackSrc, shouldLoad, priority]);

  // 图片加载成功处理
  const handleImageLoad = (e) => {
    setIsLoading(false);
    setError(false);
    onLoad(e);
  };

  // 图片加载失败处理
  const handleImageError = (e) => {
    setError(true);
    setIsLoading(false);
    
    // 如果当前不是fallback图片，尝试加载fallback
    if (blobUrl !== fallbackSrc && fallbackSrc) {
      setBlobUrl(fallbackSrc);
      setError(false);
    } else {
      onError(e);
    }
  };

  // 如果用作背景图片
  const resolvedObjectFit = imageStyle?.objectFit ?? objectFit ?? 'cover';
  const resolvedObjectPosition = imageStyle?.objectPosition ?? 'center';
  const mergedImageClassName = ['w-full h-full', imageClassName].filter(Boolean).join(' ');

  if (asBackground) {
    return (
      <div 
        ref={containerRef}
        className={`relative ${className}`} 
        style={{
          ...style,
          overflow: 'hidden'
        }}
      >
        {/* 背景图片 */}
        {blobUrl && !error && (
          <img
            ref={imgRef}
            src={blobUrl}
            alt={alt}
            className={mergedImageClassName}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              objectFit: resolvedObjectFit,
              objectPosition: resolvedObjectPosition,
              zIndex: 0,
              ...imageStyle
            }}
            onLoad={handleImageLoad}
            onError={handleImageError}
            draggable={false}
          />
        )}
        
        {/* 加载状态 */}
        {isLoading && showLoadingIndicator && (
          <div className="absolute inset-0 animate-pulse flex items-center justify-center z-10" style={{ backgroundColor: '#1F1D1D' }}>
            <div className="text-gray-500">
              {!shouldLoad ? '准备加载...' : '加载中...'}
            </div>
          </div>
        )}
        
        {/* 错误状态 */}
        {error && (
          <div className="absolute inset-0 bg-gray-600 flex items-center justify-center z-10">
            <div className="text-red-400 text-center">
              <svg className="w-8 h-8 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>图片加载失败</div>
              {errorMessage && <div className="text-xs mt-1">{errorMessage}</div>}
            </div>
          </div>
        )}
        
        {/* 内容 */}
        <div className="relative z-5">
          {children}
        </div>
      </div>
    );
  }

  // 普通图片模式
  return (
    <div ref={containerRef} className={`relative ${className}`} style={style}>
      {/* 主图片 */}
      {blobUrl && !error && (
        <img
          ref={imgRef}
          src={blobUrl}
          alt={alt}
          className={mergedImageClassName}
          style={{
            width: '100%',
            height: '100%',
            objectFit: resolvedObjectFit,
            objectPosition: resolvedObjectPosition,
            display: isLoading ? 'none' : 'block',
            ...imageStyle
          }}
          onLoad={handleImageLoad}
          onError={handleImageError}
          draggable={false}
        />
      )}
      
      {/* 加载状态 */}
      {isLoading && showLoadingIndicator && (
        <div className="absolute inset-0 animate-pulse flex items-center justify-center" style={{ backgroundColor: '#1F1D1D' }}>
          <div className="text-gray-500">
            {!shouldLoad ? '准备加载...' : '加载中...'}
          </div>
        </div>
      )}
      
      {/* 错误状态 */}
      {error && (
        <div className="absolute inset-0 bg-gray-600 flex items-center justify-center">
          <div className="text-red-400 text-center">
            <svg className="w-8 h-8 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>图片加载失败</div>
            {errorMessage && <div className="text-xs mt-1">{errorMessage}</div>}
          </div>
        </div>
      )}
    </div>
  );
});

// 为forwardRef组件设置displayName，便于调试
SecureDecryptedImage.displayName = 'SecureDecryptedImage';

export default SecureDecryptedImage;
