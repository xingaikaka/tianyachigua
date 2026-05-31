import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaTelegram, FaTwitter, FaGithub, FaEnvelope, FaHome, FaCheckCircle, FaGift, FaQq } from 'react-icons/fa';
import { useFooterInfo, useSiteConfig } from '../../../hooks/usePageConfig';

const Footer = () => {
  const navigate = useNavigate();
  
  // 使用新的页面配置hooks
  const { footerInfo, loading: footerLoading } = useFooterInfo();
  const { siteConfigs, getSiteConfig, loading: siteLoading } = useSiteConfig();
  
  const loading = footerLoading || siteLoading;

  // 处理社交媒体链接跳转
  const handleSocialLink = (platform) => {
    const url = getSiteConfig(`${platform}_url`);
    if (url && url.trim()) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  };

  // 处理邮件链接
  const handleEmailLink = () => {
    const email = getSiteConfig('contact_email');
    if (email && email.trim()) {
      window.location.href = `mailto:${email}`;
    }
  };

  // 格式化内容，处理HTML和换行符
  const formatContent = (content) => {
    if (!content) return '';
    
    // 如果内容包含HTML标签，直接返回HTML字符串用于dangerouslySetInnerHTML
    if (content.includes('<') && content.includes('>')) {
      return content;
    }
    
    // 否则处理普通文本的换行符
    return content.split('\n').map((line, index) => (
      <span key={index}>
        {line}
        {index < content.split('\n').length - 1 && <br />}
      </span>
    ));
  };

  return (
    <footer className="mt-16 pb-8 bg-transparent">
      <style dangerouslySetInnerHTML={{
        __html: `
          .footer-html-content p {
            margin-bottom: 1rem;
          }
          .footer-html-content p:last-child {
            margin-bottom: 0;
          }
          .footer-html-content strong {
            font-weight: bold;
            color: #ffffff;
          }
          .footer-html-content a {
            color: #1ABC9C;
            text-decoration: none;
            transition: color 0.2s ease;
          }
          .footer-html-content a:hover {
            color: #16A085;
          }
        `
      }} />
      <div className="flex justify-center px-4 sm:px-0">
        <div className="w-full" style={{ maxWidth: '860px' }}>
          {/* 导航图标区域 */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-8 sm:gap-12 lg:gap-24 justify-items-center mb-12">
            {/* 吃瓜首页 */}
            <button 
              className="flex flex-col items-center space-y-2 transition-all duration-200"
              onClick={() => {
                const homeUrl = getSiteConfig('site_home_url');
                if (homeUrl && homeUrl.trim()) {
                  window.open(homeUrl, '_blank', 'noopener,noreferrer');
                } else {
                  // 如果没有配置，默认跳转到当前站点首页
                  navigate('/');
                }
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.05)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <div className="w-12 h-12 flex items-center justify-center">
                <FaHome className="w-8 h-8 text-white" />
              </div>
              <span className="text-white text-xs sm:text-sm">吃瓜首页</span>
            </button>

            {/* 吃瓜App */}
            <button 
              className="flex flex-col items-center space-y-2 transition-all duration-200"
              onClick={() => {
                const appUrl = getSiteConfig('app_download_url');
                if (appUrl && appUrl.trim()) {
                  window.open(appUrl, '_blank', 'noopener,noreferrer');
                }
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.05)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <div className="w-12 h-12 flex items-center justify-center">
                <FaCheckCircle className="w-8 h-8 text-white" />
              </div>
              <span className="text-white text-xs sm:text-sm">吃瓜App</span>
            </button>

            {/* 福利应用 */}
            <button 
              className="flex flex-col items-center space-y-2 transition-all duration-200"
              onClick={() => {
                const welfareUrl = getSiteConfig('welfare_app_url');
                if (welfareUrl && welfareUrl.trim()) {
                  window.open(welfareUrl, '_blank', 'noopener,noreferrer');
                }
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.05)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <div className="w-12 h-12 flex items-center justify-center">
                <FaGift className="w-8 h-8 text-white" />
              </div>
              <span className="text-white text-xs sm:text-sm">福利应用</span>
            </button>
            
            {/* 吃瓜QQ群 */}
            <button 
              className="flex flex-col items-center space-y-2 transition-all duration-200"
              onClick={() => {
                const qqNumber = getSiteConfig('qq_number');
                if (qqNumber && qqNumber.trim()) {
                  window.open(`http://wpa.qq.com/msgrd?v=3&uin=${qqNumber}&site=qq&menu=yes`, '_blank');
                }
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.05)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              <div className="w-12 h-12 flex items-center justify-center">
                <FaQq className="w-8 h-8 text-white" />
              </div>
              <span className="text-white text-xs sm:text-sm">吃瓜QQ群</span>
            </button>
          </div>

          {/* 网站描述 - 动态获取 */}
          <div className="text-center mb-8 px-4 sm:px-0">
            {loading ? (
              // 加载状态
              <div className="text-white text-base">
                <div className="animate-pulse">
                  <div className="h-4 bg-gray-600 rounded mb-4 mx-auto" style={{ width: '80%' }}></div>
                  <div className="h-4 bg-gray-600 rounded mb-4 mx-auto" style={{ width: '40%' }}></div>
                  <div className="h-4 bg-gray-600 rounded mx-auto" style={{ width: '30%' }}></div>
                </div>
              </div>
            ) : footerInfo ? (
              // 动态内容
              <>
                <div className="text-gray-300 text-base leading-relaxed">
                  {footerInfo.noticeContent && footerInfo.noticeContent.includes('<') && footerInfo.noticeContent.includes('>') ? (
                    // 渲染HTML内容
                    <div 
                      dangerouslySetInnerHTML={{ __html: formatContent(footerInfo.noticeContent) }}
                      style={{
                        // 为HTML内容添加样式
                        lineHeight: '1.6'
                      }}
                      className="footer-html-content"
                    />
                  ) : (
                    // 渲染普通文本内容
                    formatContent(footerInfo.noticeContent)
                  )}
                </div>
              </>
            ) : (
              // 默认内容（当API无数据时的fallback）
              <>
                <p className="text-gray-300 text-base mb-6">
                  天涯吃瓜-最懂男人的吃瓜网，每日更新最新最全的热瓜资讯！
                </p>
                <div className="text-gray-400 text-sm mb-4">
                  <span>天涯吃瓜永久地址（需翻墙访问）</span>
                </div>
                <a 
                  href="https://91cg1.com" 
                  className="text-base transition-colors duration-200"
                  style={{ color: '#1ABC9C' }}
                  onMouseEnter={(e) => e.target.style.color = '#16A085'}
                  onMouseLeave={(e) => e.target.style.color = '#1ABC9C'}
                >
                  https://91cg1.com
                </a>
              </>
            )}
          </div>

          {/* 链接区域 - 横线分割布局 */}
          <div className="flex justify-center mb-8">
            <div className="w-full sm:w-auto" style={{ width: '100%', maxWidth: '760px' }}>
              <button 
                className="w-full text-left py-4 transition-colors duration-200 text-sm sm:text-base"
                style={{ 
                  color: '#BCBCBC',
                  backgroundColor: 'transparent',
                  borderTop: '1px solid #444444'
                }}
                onClick={() => handleSocialLink('telegram')}
              >
                商务合作
              </button>
              
              <button 
                className="w-full text-left py-4 transition-colors duration-200 text-sm sm:text-base"
                style={{ 
                  color: '#BCBCBC',
                  backgroundColor: 'transparent',
                  borderTop: '1px solid #444444'
                }}
                onClick={() => navigate('/archives')}
              >
                往期内容
              </button>
              
              <button 
                className="w-full text-left py-4 transition-colors duration-200 text-sm sm:text-base"
                style={{ 
                  color: '#BCBCBC',
                  backgroundColor: 'transparent',
                  borderTop: '1px solid #444444'
                }}
                onClick={() => navigate('/submission')}
              >
                投稿方式
              </button>
              
              <button 
                className="w-full text-left py-4 transition-colors duration-200 text-sm sm:text-base"
                style={{ 
                  color: '#BCBCBC',
                  backgroundColor: 'transparent',
                  borderTop: '1px solid #444444'
                }}
                onClick={() => navigate('/homeway')}
              >
                回家的路
              </button>
              
              <button 
                className="w-full text-left py-4 transition-colors duration-200 text-sm sm:text-base"
                style={{ 
                  color: '#BCBCBC',
                  backgroundColor: 'transparent',
                  borderTop: '1px solid #444444'
                }}
                onClick={() => handleSocialLink('telegram')}
              >
                加入我们
              </button>
            </div>
          </div>

          {/* 社交媒体图标 */}
          <div className="flex justify-center items-center space-x-4 sm:space-x-8 mt-12 mb-8">
            {/* Telegram */}
            <button 
              className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onClick={() => handleSocialLink('telegram')}
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
              <FaTelegram size={16} />
            </button>

            {/* Twitter/X */}
            <button 
              className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onClick={() => handleSocialLink('twitter')}
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
              <FaTwitter size={16} />
            </button>

            {/* GitHub */}
            <button 
              className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onClick={() => handleSocialLink('github')}
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
              <FaGithub size={16} />
            </button>

            {/* 邮件 */}
            <button 
              className="w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200"
              style={{ 
                color: 'rgba(255, 255, 255, 0.9)',
                backgroundColor: 'rgba(0, 0, 0, 0.3)'
              }}
              onClick={handleEmailLink}
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
              <FaEnvelope size={16} />
            </button>
          </div>

          {/* 版权信息 */}
          <div className="text-center text-gray-400 text-sm">
            <p>
              Copyright © 2025{' '}
              <span style={{ color: '#1ABC9C' }}>
                {getSiteConfig('site_name') || '天涯吃瓜'}
              </span>
              {' '}• Powered by{' '}
              <span style={{ color: '#1ABC9C' }}>
                {getSiteConfig('site_name') || '天涯吃瓜'}
              </span>
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
