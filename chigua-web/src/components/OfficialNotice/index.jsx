import React from 'react';
import { usePageConfig } from '../../hooks/usePageConfig';

const OfficialNotice = ({ 
  content = null,              // 可以传入具体内容
  configKey = null,            // 直接指定配置键
  categoryType = null,         // 兼容旧的 categoryType 参数
  className = '',              // 额外的CSS类名
  style = {},                 // 额外的样式
  showTitle = true,           // 是否显示"官方公告"标题
  title = '官方公告'           // 标题文字，可自定义
}) => {
  const { getConfig } = usePageConfig();
  
  // 将旧的 categoryType 映射到新的 configKey
  const getCategoryConfigKey = (categoryType) => {
    const mapping = {
      '公告': 'notice_info',
      '投稿': 'submission_info', 
      '回家地址': 'homeway_info'
    };
    return mapping[categoryType] || 'notice_info';
  };
  
  // 确定最终使用的配置键
  const finalConfigKey = configKey || (categoryType ? getCategoryConfigKey(categoryType) : 'notice_info');
  
  // 优先使用传入的content，否则从配置中获取
  const noticeContent = content || getConfig(finalConfigKey);
  
  // 根据类型自动设置标题
  const getAutoTitle = (categoryType, configKey) => {
    if (categoryType) {
      const titleMapping = {
        '公告': '官方公告',
        '投稿': '投稿信息',
        '回家地址': '回家地址'
      };
      return titleMapping[categoryType] || '官方公告';
    }
    
    if (configKey) {
      const keyTitleMapping = {
        'notice_info': '官方公告',
        'submission_info': '投稿信息', 
        'homeway_info': '回家地址'
      };
      return keyTitleMapping[configKey] || '公告信息';
    }
    
    return '官方公告';
  };
  
  // 如果没有自定义标题，使用自动标题
  const displayTitle = title === '官方公告' ? getAutoTitle(categoryType, finalConfigKey) : title;
  
  if (!noticeContent) {
    return null;
  }

  return (
    <>
      {/* CSS样式 */}
      <style dangerouslySetInnerHTML={{
        __html: `
          .official-notice-content a {
            color: #1ABC9C !important;
            text-decoration: none !important;
            border-bottom: 1px solid rgba(26, 188, 156, 0.3) !important;
            padding-bottom: 1px !important;
            transition: opacity 0.2s ease !important;
          }
          .official-notice-content a:hover {
            opacity: 0.8 !important;
          }
          .official-notice-content strong {
            color: #BCBCBC !important;
            font-size: 16px !important;
          }
          .official-notice-content p {
            margin-bottom: 12px !important;
            text-align: left !important;
            font-size: 16px !important;
            line-height: 1.6 !important;
          }
          .official-notice-content div {
            text-align: left !important;
            font-size: 16px !important;
            line-height: 1.6 !important;
          }
          .official-notice-content * {
            text-align: left !important;
            font-size: 16px !important;
            line-height: 1.6 !important;
          }
        `
      }} />
      
      <div 
        className={`w-full relative ${className}`} 
        style={{ textAlign: 'left', ...style }}
      >
        {showTitle && (
          <>
            {/* 官方公告选项卡标签 */}
            <div 
              className="inline-block px-4 py-2 relative z-10" 
              style={{ 
                backgroundColor: 'transparent',
                border: '1px solid #403E3E',
                borderBottom: 'none',
                borderTopLeftRadius: '8px',
                borderTopRightRadius: '8px',
                marginLeft: '0',
                textAlign: 'left'
              }}
            >
              <span className="text-sm font-medium" style={{ color: '#BCBCBC' }}>
                {displayTitle}
              </span>
              {/* 遮挡下方边框线的元素 */}
              <div 
                style={{
                  position: 'absolute',
                  bottom: '-1px',
                  left: '0',
                  right: '0',
                  height: '2px',
                  backgroundColor: '#2C2A2A',
                  zIndex: 15
                }}
              />
            </div>
          </>
        )}
        
        {/* 公告内容区域 */}
        <div 
          className={`p-6 text-left ${showTitle ? '-mt-px' : ''}`}
          style={{ 
            backgroundColor: 'transparent',
            border: '1px solid #403E3E',
            borderRadius: '8px',
            borderTopLeftRadius: showTitle ? '0' : '8px',
            textAlign: 'left'
          }}
        >
          <div 
            className="official-notice-content" 
            style={{ 
              color: '#BCBCBC',
              textAlign: 'left',
              fontSize: '16px',
              lineHeight: '1.6'
            }}
            dangerouslySetInnerHTML={{ __html: noticeContent }}
          />
        </div>
      </div>
    </>
  );
};

export default OfficialNotice;