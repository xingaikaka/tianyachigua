import React from 'react';
import TikTokLoading from '../../components/common/TikTokLoading';
import './TikTokLoadingTest.css';

const TikTokLoadingTest = () => {
  return (
    <div className="tiktok-loading-test">
      <h1>抖音加载图标测试页面</h1>
      
      <div className="test-section">
        <h2>不同尺寸（浅色背景版本 - 视频加载使用）</h2>
        <div className="test-row">
          <div className="test-item">
            <p>32px</p>
            <TikTokLoading size={32} dark={false} />
          </div>
          <div className="test-item">
            <p>48px</p>
            <TikTokLoading size={48} dark={false} />
          </div>
          <div className="test-item">
            <p>64px</p>
            <TikTokLoading size={64} dark={false} />
          </div>
          <div className="test-item">
            <p>96px</p>
            <TikTokLoading size={96} dark={false} />
          </div>
        </div>
      </div>

      <div className="test-section">
        <h2>不同背景对比</h2>
        <div className="test-row">
          <div className="test-item dark-bg">
            <p>深色背景（dark={true}）</p>
            <TikTokLoading size={64} dark={true} />
          </div>
          <div className="test-item light-bg">
            <p>浅色背景（dark={false}）</p>
            <TikTokLoading size={64} dark={false} />
          </div>
        </div>
      </div>

      <div className="test-section">
        <h2>实际使用场景</h2>
        <div className="test-item video-bg">
          <p>视频加载场景（使用浅色背景版本）</p>
          <TikTokLoading size={64} dark={false} />
        </div>
        <div className="test-item video-bg-small">
          <p>视频加载场景 - 小尺寸（48px）</p>
          <TikTokLoading size={48} dark={false} />
        </div>
      </div>

      <div className="test-section">
        <h2>无背景验证</h2>
        <p className="test-note">以下测试用于验证图标没有方形背景，渐变只显示在logo形状内。SVG元素本身设置为透明背景，使用双层path叠加实现渐变效果。</p>
        <div className="test-row">
          <div className="test-item transparent-bg">
            <p>透明背景测试</p>
            <TikTokLoading size={64} dark={false} />
          </div>
          <div className="test-item colored-bg">
            <p>彩色背景测试</p>
            <TikTokLoading size={64} dark={false} />
          </div>
          <div className="test-item striped-bg">
            <p>条纹背景测试</p>
            <TikTokLoading size={64} dark={false} />
          </div>
        </div>
      </div>

      <div className="test-section">
        <h2>动画效果说明</h2>
        <div className="test-info">
          <p>✓ 所有版本统一使用从上到下的动画方向</p>
          <p>✓ <strong>渐变效果只显示在logo形状内，不会出现方形背景</strong></p>
          <p>✓ 视频加载场景统一使用浅色背景版本（dark={false}）</p>
          <p>✓ <strong>实现方式：使用SVG path直接应用渐变，SVG元素设置为透明背景</strong></p>
          <p>✓ <strong>双层path叠加：灰色底层 + 渐变上层（screen混合模式）</strong></p>
          <p>✓ <strong>已移除所有CSS伪元素，确保无背景干扰</strong></p>
        </div>
      </div>
    </div>
  );
};

export default TikTokLoadingTest;

