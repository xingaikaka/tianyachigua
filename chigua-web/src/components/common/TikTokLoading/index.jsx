import React, { useMemo } from 'react';
import './TikTokLoading.css';

// 抖音logo的SVG路径
const LOGO_PATH = "M937.4 423.9c-84 0-165.7-27.3-232.9-77.8v352.3c0 179.9-138.6 325.6-309.6 325.6S85.3 878.3 85.3 698.4c0-179.9 138.6-325.6 309.6-325.6 17.1 0 33.7 1.5 49.9 4.3v186.6c-15.5-6.1-32-9.2-48.6-9.2-76.3 0-138.2 65-138.2 145.3 0 80.2 61.9 145.3 138.2 145.3 76.2 0 138.1-65.1 138.1-145.3V0H707c0 134.5 103.7 243.5 231.6 243.5v180.3l-1.2 0.1";

// 颜色常量
const COLORS = {
  dark: {
    base: "rgba(200, 200, 200, 0.5)",
    gradient: "rgba(255, 255, 255, 0.9)"
  },
  light: {
    base: "rgba(128, 128, 128, 0.5)",
    gradient: "rgba(255, 255, 255, 0.8)"
  }
};

// 动画常量
const ANIMATION_DURATION = "2s";
const GRADIENT_TRANSLATE_DISTANCE = 1024;

/**
 * 抖音风格加载图标组件
 * @param {Object} props - 组件属性
 * @param {number} [props.size=48] - 图标尺寸，默认48px
 * @param {boolean} [props.dark=true] - 是否为深色模式，默认true
 */
const TikTokLoading = ({ size = 48, dark = true }) => {
  // 使用useMemo缓存gradientId，避免重复计算
  const gradientId = useMemo(
    () => `tiktok-gradient-${dark ? 'dark' : 'light'}-${size}`,
    [dark, size]
  );

  // 计算动画值
  const animationValues = useMemo(() => {
    // 统一使用从上到下的动画方向
    const start = -GRADIENT_TRANSLATE_DISTANCE;
    const end = GRADIENT_TRANSLATE_DISTANCE;
    return `0 ${start}; 0 ${end}; 0 ${start}`;
  }, []);

  // 获取当前模式的颜色
  const colors = useMemo(() => COLORS[dark ? 'dark' : 'light'], [dark]);

  // SVG样式
  const svgStyle = useMemo(
    () => ({
      width: size,
      height: size,
      display: 'block',
      background: 'transparent'
    }),
    [size]
  );

  return (
    <div className={`tiktok-loading-container ${dark ? 'dark' : ''}`}>
      <svg
        className="tiktok-icon"
        viewBox="0 0 1024 1024"
        xmlns="http://www.w3.org/2000/svg"
        style={svgStyle}
        preserveAspectRatio="xMidYMid meet"
        aria-label="加载中"
      >
        <defs>
          <linearGradient id={gradientId} x1="0%" y1="0%" x2="0%" y2="100%" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="rgba(255, 255, 255, 0)" />
            <stop offset="20%" stopColor="rgba(255, 255, 255, 0)" />
            <stop offset="50%" stopColor={colors.gradient} />
            <stop offset="80%" stopColor="rgba(255, 255, 255, 0)" />
            <stop offset="100%" stopColor="rgba(255, 255, 255, 0)" />
            <animateTransform
              attributeName="gradientTransform"
              type="translate"
              values={animationValues}
              dur={ANIMATION_DURATION}
              repeatCount="indefinite"
            />
          </linearGradient>
        </defs>
        {/* 基础灰色图标 - 底层 */}
        <path
          d={LOGO_PATH}
          fill={colors.base}
          opacity="0.6"
        />
        {/* 渐变层 - 上层，使用screen混合模式叠加 */}
        <path
          d={LOGO_PATH}
          fill={`url(#${gradientId})`}
          opacity="0.8"
          style={{ mixBlendMode: 'screen' }}
        />
      </svg>
    </div>
  );
};

export default TikTokLoading;

