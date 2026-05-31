import React, { useState, useEffect, useRef } from 'react';
import imageDecryption from '../../utils/imageDecryption';

/**
 * 安全图片组件
 * 特点：
 * 1. 获取加密的图片数据，在前端解密
 * 2. 使用Canvas渲染，不创建可下载的blob URL
 * 3. 防止用户通过开发者工具获取解密后的图片
 */
const SecureImage = ({ 
  src, 
  alt = '', 
  style = {}, 
  className = '', 
  asBackground = false, 
  children,
  onLoad = () => {},
  onError = () => {},
  ...props 
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const canvasRef = useRef(null);
  const containerRef = useRef(null);

  useEffect(() => {
    const loadAndDecryptImage = async () => {
      if (!src) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(false);
        setErrorMessage('');

        // 如果已经是blob URL，直接使用（向后兼容）
        if (src.startsWith('blob:')) {
          await loadImageToCanvas(src);
          setLoading(false);
          return;
        }

        // 如果不包含签名参数，可能不是加密图片，直接使用
        if (!src.includes('signature=') && !src.includes('key=')) {
          await loadImageToCanvas(src);
          setLoading(false);
          return;
        }

        // 检查是否支持图片解密
        if (!imageDecryption.isSupported()) {
          throw new Error(imageDecryption.getErrorMessage());
        }

        // 获取加密的图片数据（不使用 decrypt=true）
        
        const response = await fetch(src);
        
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const contentType = response.headers.get('content-type') || '';
        const isEncrypted = response.headers.get('x-encrypted') === 'true';
        const originalContentType = response.headers.get('x-original-content-type') || 'image/jpeg';

        const imageBuffer = await response.arrayBuffer();

        let finalBuffer = imageBuffer;
        let finalContentType = contentType;

        // 如果是加密图片，进行解密
        if (isEncrypted || imageDecryption.isEncryptedImage(contentType)) {
          const filePath = imageDecryption.extractFilePathFromUrl(src);

          finalBuffer = await imageDecryption.decryptImage(
            imageBuffer, 
            filePath, 
            { encrypted: true }
          );
          finalContentType = originalContentType;

        }

        // 将解密后的数据渲染到Canvas
        await renderBufferToCanvas(finalBuffer, finalContentType);
        
        // 调用成功回调
        onLoad();
        
      } catch (err) {
        
        setError(true);
        setErrorMessage(err.message);
        onError(err);
        
        // 降级处理：尝试直接加载原始URL
        try {
          await loadImageToCanvas(src);
          setError(false);
          setErrorMessage('');
        } catch (fallbackError) {
          
        }
      } finally {
        setLoading(false);
      }
    };

    loadAndDecryptImage();
  }, [src]);

  /**
   * 将buffer数据渲染到Canvas
   */
  const renderBufferToCanvas = async (buffer, contentType) => {
    return new Promise((resolve, reject) => {
      const blob = new Blob([buffer], { type: contentType });
      const tempUrl = URL.createObjectURL(blob);
      
      const img = new Image();
      img.onload = () => {
        drawImageToCanvas(img);
        URL.revokeObjectURL(tempUrl); // 立即释放临时URL
        resolve();
      };
      img.onerror = () => {
        URL.revokeObjectURL(tempUrl);
        reject(new Error('图片数据格式错误'));
      };
      img.src = tempUrl;
    });
  };

  /**
   * 直接加载图片到Canvas
   */
  const loadImageToCanvas = async (imageUrl) => {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        drawImageToCanvas(img);
        resolve();
      };
      img.onerror = () => {
        reject(new Error('图片加载失败'));
      };
      
      // 如果是跨域图片，设置crossOrigin
      if (!imageUrl.startsWith('blob:') && !imageUrl.startsWith('data:')) {
        img.crossOrigin = 'anonymous';
      }
      
      img.src = imageUrl;
    });
  };

  /**
   * 将图片绘制到Canvas
   */
  const drawImageToCanvas = (img) => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    
    // 设置Canvas尺寸
    canvas.width = img.naturalWidth;
    canvas.height = img.naturalHeight;
    
    // 绘制图片
    ctx.drawImage(img, 0, 0);
    
    // 应用CSS样式到Canvas
    updateCanvasStyle();
  };

  /**
   * 更新Canvas样式
   */
  const updateCanvasStyle = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    // 默认样式
    const defaultStyle = {
      width: '770px',
      height: 'auto',
      display: 'block',
      margin: '15px auto',
      borderRadius: '4px',
      objectFit: 'cover',
      ...style
    };

    // 应用样式
    Object.assign(canvas.style, defaultStyle);
  };

  const defaultStyle = {
    width: '770px',
    height: 'auto',
    display: 'block',
    margin: '15px auto',
    borderRadius: '4px',
    objectFit: 'cover',
    ...style
  };

  if (loading) {
    // 背景模式加载状态
    if (asBackground) {
      return (
        <div
          ref={containerRef}
          className={className}
          style={{
            ...style,
            background: 'linear-gradient(135deg, #4a5568 0%, #2d3748 100%)',
            position: 'relative'
          }}
          {...props}
        >
          <div className="absolute inset-0 bg-black bg-opacity-40"></div>
          <div className="absolute inset-0 flex flex-col items-center justify-center text-white text-center p-6 z-10">
            <div className="flex items-center justify-center mb-4">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white opacity-75"></div>
            </div>
            <div className="text-xl md:text-2xl font-normal text-white text-opacity-80">
              安全加载中...
            </div>
          </div>
          {children}
        </div>
      );
    }
    
    // 普通模式加载状态
    return (
      <div 
        className={`flex items-center justify-center bg-gray-600 text-gray-300 ${className}`}
        style={{
          ...defaultStyle,
          height: '200px',
        }}
      >
        <div className="flex items-center space-x-2">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-gray-300"></div>
          <span>安全加载中...</span>
        </div>
      </div>
    );
  }

  if (error) {
    const errorDisplayMessage = errorMessage || '图片加载失败';
    
    // 背景模式错误状态
    if (asBackground) {
      return (
        <div
          ref={containerRef}
          className={className}
          style={{
            ...style,
            background: 'linear-gradient(135deg, #4a5568 0%, #2d3748 100%)',
            position: 'relative'
          }}
          {...props}
        >
          <div className="absolute inset-0 bg-black bg-opacity-40"></div>
          <div className="absolute inset-0 flex flex-col items-center justify-center text-white text-center p-6 z-10">
            <svg className="w-8 h-8 mb-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div className="text-xl md:text-2xl font-normal text-red-400">
              {errorDisplayMessage}
            </div>
          </div>
          {children}
        </div>
      );
    }
    
    // 普通模式错误状态
    return (
      <div 
        className={`flex items-center justify-center bg-gray-600 text-red-400 ${className}`}
        style={{
          ...defaultStyle,
          height: '200px',
        }}
      >
        <div className="text-center">
          <svg className="w-8 h-8 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{errorDisplayMessage}</span>
        </div>
      </div>
    );
  }

  // 背景模式：返回带Canvas背景的div
  if (asBackground) {
    return (
      <div
        ref={containerRef}
        className={className}
        style={{
          ...style,
          position: 'relative',
          overflow: 'hidden'
        }}
        {...props}
      >
        <canvas
          ref={canvasRef}
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            objectFit: style.backgroundSize || 'cover',
            objectPosition: style.backgroundPosition || 'center',
          }}
          aria-label={alt}
        />
        {children}
      </div>
    );
  }

  // 普通模式：返回Canvas元素
  return (
    <canvas
      ref={canvasRef}
      className={className}
      style={defaultStyle}
      aria-label={alt}
      {...props}
    />
  );
};

export default SecureImage;