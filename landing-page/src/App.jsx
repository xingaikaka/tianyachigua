import React, { useEffect } from 'react';
import { FaTelegram, FaTwitter, FaEnvelope, FaGithub } from 'react-icons/fa';
import './App.css';

function App() {
  // 统一的多端适配处理
  useEffect(() => {
    const setupLayout = () => {
      // 设置全局背景色
      document.body.style.backgroundColor = '#111827';
      document.documentElement.style.backgroundColor = '#111827';
      
      // 移动端特殊处理
      if (window.innerWidth < 1024) {
        document.body.style.minHeight = '100vh';
        document.documentElement.style.minHeight = '100vh';
        
        // 针对iOS Safari和Android Chrome的特殊处理
        if (/iPhone|iPad|iPod|Android/i.test(navigator.userAgent)) {
          document.body.style.minHeight = '100dvh';
          document.documentElement.style.minHeight = '100dvh';
        }
      }
    };

    setupLayout();
    
    // 监听屏幕变化
    window.addEventListener('resize', setupLayout);
    window.addEventListener('orientationchange', setupLayout);
    
    return () => {
      window.removeEventListener('resize', setupLayout);
      window.removeEventListener('orientationchange', setupLayout);
    };
  }, []);
  // 生成随机单词的函数
  const generateRandomWord = () => {
    const words = [
      'alpha', 'beta', 'gamma', 'delta', 'omega', 'sigma', 'theta', 'kappa',
      'lambda', 'micro', 'nova', 'pixel', 'quantum', 'rapid', 'swift', 'ultra',
      'vortex', 'wave', 'xenon', 'zero', 'azure', 'blaze', 'cyber', 'dream',
      'echo', 'flash', 'glow', 'hyper', 'ionic', 'jade', 'karma', 'lunar',
      'magic', 'neon', 'orbit', 'prime', 'quest', 'royal', 'storm', 'titan'
    ];
    return words[Math.floor(Math.random() * words.length)];
  };

  // 生成随机域名（新三组顶级域名）
  const domains = [
    `https://${generateRandomWord()}.rklzu.cc`,
    `https://${generateRandomWord()}.cqqvl.cc`,
    `https://${generateRandomWord()}.uoatx.cc`
  ];

  // 读取当前页面的 utm_source，并用于续传到真实站链接
  const getUtmSource = () => {
    try {
      const params = new URLSearchParams(window.location.search);
      const utm = params.get('utm_source');
      return utm && utm.trim() ? utm.trim() : '';
    } catch (e) {
      return '';
    }
  };

  const appendUtm = (url, utm) => {
    if (!utm) return url;
    try {
      const u = new URL(url);
      if (!u.searchParams.has('utm_source')) {
        u.searchParams.set('utm_source', utm);
      }
      return u.toString();
    } catch (e) {
      // 对于可能的相对链接或无效URL，降级为字符串拼接
      const joiner = url.includes('?') ? '&' : '?';
      return `${url}${joiner}utm_source=${encodeURIComponent(utm)}`;
    }
  };

  const utmSource = getUtmSource();
  return (
    <div className="min-h-screen bg-gray-900 text-white font-chinese relative" style={{ backgroundColor: '#111827', minHeight: '100vh' }}>
      {/* 多层背景保护 */}
      <div className="fixed inset-0 bg-gray-900 pointer-events-none" style={{ backgroundColor: '#111827', zIndex: -1000 }}></div>
      <div className="fixed inset-0 bg-gray-900 pointer-events-none" style={{ backgroundColor: '#111827', zIndex: -999 }}></div>
      <div className="absolute inset-0 bg-gray-900 pointer-events-none" style={{ backgroundColor: '#111827', zIndex: -998 }}></div>
      
      {/* 背景装饰 */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none" style={{ zIndex: -997 }}>
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-primary opacity-10 rounded-full blur-3xl"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-primary opacity-10 rounded-full blur-3xl"></div>
      </div>
      
      {/* 主容器 - 统一布局 */}
      <div className="main-container mx-auto w-full">
        
        {/* 内容容器 */}
        <div className="content-wrapper">
        
        {/* Logo和标题区域 */}
        <div className="text-center mb-6 sm:mb-8">
          <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-bold mb-3 sm:mb-4">
            <span 
              className="animated-gradient-text"
              style={{
                background: 'linear-gradient(45deg, #4A90E2, #7ED4E6, #50C9C3, #4A90E2, #7ED4E6, #50C9C3)',
                backgroundSize: '200% 200%',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
                animation: 'gradientShift 3s ease-in-out infinite'
              }}
            >
              天涯吃瓜网
            </span>
          </h1>
          <p className="text-lg sm:text-xl md:text-2xl text-gray-300 font-medium mb-1 px-2">正式开启网红吃瓜万象的长征路</p>
          <p className="text-lg sm:text-xl md:text-2xl text-gray-300 font-medium px-2">天涯原团队 免费资源</p>
        </div>

        {/* 导航链接区域 - 响应式宽度 */}
        <div className="mb-6 sm:mb-8 w-full max-w-sm px-4 sm:px-0 mx-auto" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <a 
            href={appendUtm(domains[0], utmSource)} 
            target="_blank" 
            rel="noopener noreferrer"
            className="block w-full px-4 sm:px-6 py-3 sm:py-4 text-center border border-gray-600 bg-gray-800 font-medium flex flex-col justify-center min-h-[60px] sm:min-h-[65px]"
            style={{ borderRadius: '12px' }}
          >
            <div className="text-gray-300 mb-1 font-semibold text-sm sm:text-base">天涯吃瓜—线路一</div>
            <div className="text-primary text-xs sm:text-sm">{domains[0].replace('https://', '')}</div>
          </a>
          
          <a 
            href={appendUtm(domains[1], utmSource)} 
            target="_blank" 
            rel="noopener noreferrer"
            className="block w-full px-4 sm:px-6 py-3 sm:py-4 text-center border border-gray-600 bg-gray-800 font-medium flex flex-col justify-center min-h-[60px] sm:min-h-[65px]"
            style={{ borderRadius: '12px' }}
          >
            <div className="text-gray-300 mb-1 font-semibold text-sm sm:text-base">天涯吃瓜—线路二</div>
            <div className="text-primary text-xs sm:text-sm">{domains[1].replace('https://', '')}</div>
          </a>
          
          <a 
            href={appendUtm(domains[2], utmSource)} 
            target="_blank" 
            rel="noopener noreferrer"
            className="block w-full px-4 sm:px-6 py-3 sm:py-4 text-center border border-gray-600 bg-gray-800 font-medium flex flex-col justify-center min-h-[60px] sm:min-h-[65px]"
            style={{ borderRadius: '12px' }}
          >
            <div className="text-gray-300 mb-1 font-semibold text-sm sm:text-base">天涯吃瓜—线路三</div>
            <div className="text-primary text-xs sm:text-sm">{domains[2].replace('https://', '')}</div>
          </a>
        </div>

        {/* 下载和导航按钮 */}
        <div className="grid grid-cols-2 gap-3 sm:gap-4 mb-4 sm:mb-6 w-full max-w-sm px-4 sm:px-0 mx-auto place-items-stretch">
          <a 
            href="https://github.com/tiantianchigua521"
            target="_blank"
            rel="noopener noreferrer"
            className="bg-gray-700 hover:bg-gray-600 text-white px-4 sm:px-6 rounded-lg transition-colors duration-300 cursor-pointer text-center text-sm sm:text-base h-16 flex items-center justify-center w-full"
          >
            下载天涯吃瓜APP
          </a>
          <a 
            href="https://github.com/tiantianchigua521"
            target="_blank"
            rel="noopener noreferrer"
            className="bg-gray-700 hover:bg-gray-600 text-white px-4 sm:px-6 rounded-lg transition-colors duration-300 cursor-pointer text-center text-sm sm:text-base h-16 flex items-center justify-center w-full"
          >
            天涯福利导航
          </a>
        </div>

        {/* 邮箱订阅区域 */}
        <div className="w-full max-w-sm px-4 sm:px-0 mx-auto">
          <div className="text-center mb-4 sm:mb-6 p-4 sm:p-5 bg-gray-800 rounded-lg w-full">
            <p className="text-xs sm:text-sm mb-2">
              <span 
                className="animated-gradient-text"
                style={{
                  background: 'linear-gradient(45deg, #4A90E2, #7ED4E6, #50C9C3, #4A90E2, #7ED4E6, #50C9C3)',
                  backgroundSize: '200% 200%',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundClip: 'text',
                  animation: 'gradientShift 3s ease-in-out infinite'
                }}
              >
                发送任意消息到邮箱，自动获取天涯吃瓜最新
              </span>
            </p>
            <p className="text-xs sm:text-sm">
              <span className="text-red-400">【免费翻墙】</span>
              <span className="text-gray-300">地址：</span>
              <span className="text-primary break-all">tycgfun@pm.me</span>
            </p>
          </div>
        </div>

        {/* 社交媒体图标和标签 */}
        <div className="w-full max-w-sm mx-auto flex justify-center space-x-4 sm:space-x-6 md:space-x-8 mb-4 sm:mb-6 px-4 sm:px-0">
          {/* X/Twitter */}
          <div className="flex flex-col items-center space-y-1 sm:space-y-2">
            <a 
              href="https://twitter.com/tycg666"
              target="_blank"
              rel="noopener noreferrer"
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.transform = 'scale(1.1)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <FaTwitter size={14} className="sm:w-4 sm:h-4" />
            </a>
            <span className="text-xs text-gray-400 text-center">官方推特</span>
          </div>
          
          {/* Telegram */}
          <div className="flex flex-col items-center space-y-1 sm:space-y-2">
            <a 
              href="https://t.me/tianyachigua"
              target="_blank"
              rel="noopener noreferrer"
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.transform = 'scale(1.1)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <FaTelegram size={14} className="sm:w-4 sm:h-4" />
            </a>
            <span className="text-xs text-gray-400 text-center">官方TG群</span>
          </div>
          
          {/* Email */}
          <div className="flex flex-col items-center space-y-1 sm:space-y-2">
            <a 
              href="mailto:tycg666@gmail.com"
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.transform = 'scale(1.1)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <FaEnvelope size={14} className="sm:w-4 sm:h-4" />
            </a>
            <span className="text-xs text-gray-400 text-center">联系邮箱</span>
          </div>
          
          {/* GitHub */}
          <div className="flex flex-col items-center space-y-1 sm:space-y-2">
            <a 
              href="https://github.com/tiantianchigua521"
              target="_blank"
              rel="noopener noreferrer"
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = 'rgba(0, 0, 0, 0.8)';
                e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.transform = 'scale(1.1)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = 'rgba(255, 255, 255, 0.9)';
                e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.3)';
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <FaGithub size={14} className="sm:w-4 sm:h-4" />
            </a>
            <span className="text-xs text-gray-400 text-center">GitHub</span>
          </div>
        </div>

        {/* 底部版权信息 */}
        <div className="text-center text-xs sm:text-sm text-gray-500 px-4">
          <p className="mb-2 leading-relaxed sm:whitespace-nowrap">本站只为海外华人提供服务，非法活动举报以及19岁以下用户禁止访问</p>
          <p className="leading-relaxed sm:whitespace-nowrap">
            © 2025 天涯吃瓜 - 官方入口 
            <a href="https://tycg3.com" className="text-primary hover:underline ml-1">https://tycg3.com</a>
            <span>{' '}Inc. All Rights Reserved.</span>
          </p>
        </div>
        
        </div> {/* 内容容器结束 */}
      </div> {/* 主容器结束 */}
    </div>
  );
}

export default App;