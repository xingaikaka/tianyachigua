import React from 'react';

// 抖音logo的SVG路径
const LOGO_PATH = "M937.4 423.9c-84 0-165.7-27.3-232.9-77.8v352.3c0 179.9-138.6 325.6-309.6 325.6S85.3 878.3 85.3 698.4c0-179.9 138.6-325.6 309.6-325.6 17.1 0 33.7 1.5 49.9 4.3v186.6c-15.5-6.1-32-9.2-48.6-9.2-76.3 0-138.2 65-138.2 145.3 0 80.2 61.9 145.3 138.2 145.3 76.2 0 138.1-65.1 138.1-145.3V0H707c0 134.5 103.7 243.5 231.6 243.5v180.3l-1.2 0.1";

/**
 * 抖音图标组件（静态，无动画）
 * @param {Object} props - 组件属性
 * @param {number} [props.size=24] - 图标尺寸，默认24px
 * @param {string} [props.color='#fff'] - 图标颜色，默认白色
 * @param {number} [props.opacity=1] - 透明度，默认1
 */
const TikTokIcon = ({ size = 24, color = '#fff', opacity = 1 }) => {
  return (
    <svg
      viewBox="0 0 1024 1024"
      xmlns="http://www.w3.org/2000/svg"
      style={{
        width: size,
        height: size,
        display: 'block',
        background: 'transparent'
      }}
      preserveAspectRatio="xMidYMid meet"
      aria-hidden="true"
    >
      <path
        d={LOGO_PATH}
        fill={color}
        opacity={opacity}
      />
    </svg>
  );
};

export default TikTokIcon;

