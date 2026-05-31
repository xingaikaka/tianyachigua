import React, { useState, useEffect } from 'react';
import noticeService from '../../services/noticeService';
import apiCacheService from '../../services/apiCacheService';

const OfficialAnnouncement = ({ categoryType = '公告' }) => {
  const [announcement, setAnnouncement] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAnnouncement();
  }, [categoryType]);

  const fetchAnnouncement = async () => {
    try {
      setLoading(true);
      // 使用缓存服务获取公告
      const response = await apiCacheService.getApiData(
        'OFFICIAL_ANNOUNCEMENT',
        { categoryType },
        (params) => noticeService.getLatestAnnouncement(params.categoryType),
        { cacheDuration: 10 * 60 * 1000 } // 10分钟缓存
      );
      
      if (response.code === 200 && response.data) {
        setAnnouncement(response.data);
      } else {
        setAnnouncement(null);
      }
    } catch (error) {
      
      setAnnouncement(null);
    } finally {
      setLoading(false);
    }
  };

  // 如果正在加载，显示加载状态
  if (loading) {
    return (
      <div className="rounded-lg p-6 w-full" style={{ backgroundColor: '#343232' }}>
        <div className="text-left" style={{ color: '#BCBCBC' }}>
          加载中...
        </div>
      </div>
    );
  }

  // 如果没有公告数据，不显示组件
  if (!announcement) {
    return null;
  }

  return (
    <div className="rounded-lg p-6 w-full official-announcement" style={{ backgroundColor: '#343232' }}>
      <style>{`
        /* 与视频详情一致的富文本规则：不覆盖内联样式，支持 Quill 字号类 */
        .rich-text-content { color: #d1d5db; text-align: left !important; }
        .rich-text-content, .rich-text-content * { text-align: left !important; }
        .rich-text-content p { margin: 8px 0; line-height: 1.6; }
        /* 覆盖全局 .rich-text-content { font-size: 14px; }，仅在公告模块内还原为默认，让内联字号可见 */
        .official-announcement .rich-text-content { font-size: initial !important; }
        /* 取消全局标题固定字号，交给内联/类名控制（仅公告作用域） */
        .official-announcement .rich-text-content h1,
        .official-announcement .rich-text-content h2,
        .official-announcement .rich-text-content h3,
        .official-announcement .rich-text-content h4,
        .official-announcement .rich-text-content h5,
        .official-announcement .rich-text-content h6 { font-size: inherit !important; }
        /* Quill 字号类映射 */
        .rich-text-content .ql-size-small { font-size: 0.75em !important; }
        .rich-text-content .ql-size-large { font-size: 1.5em !important; }
        .rich-text-content .ql-size-huge { font-size: 2.5em !important; }
        /* 常见像素字号类（若后端去掉了内联style，只保留 ql-size-14px 这类类名时使用） */
        .rich-text-content .ql-size-12px { font-size: 12px !important; }
        .rich-text-content .ql-size-13px { font-size: 13px !important; }
        .rich-text-content .ql-size-14px { font-size: 14px !important; }
        .rich-text-content .ql-size-15px { font-size: 15px !important; }
        .rich-text-content .ql-size-16px { font-size: 16px !important; }
        .rich-text-content .ql-size-18px { font-size: 18px !important; }
        .rich-text-content .ql-size-20px { font-size: 20px !important; }
        .rich-text-content .ql-size-24px { font-size: 24px !important; }
        .rich-text-content .ql-size-28px { font-size: 28px !important; }
        .rich-text-content .ql-size-32px { font-size: 32px !important; }
      `}</style>
      {/* 公告标题 */}
      {announcement.noticeTitle && (
        <div className="mb-4 text-left">
          <h3 className="font-bold text-lg" style={{ color: '#BCBCBC' }}>
            {announcement.noticeTitle}
          </h3>
        </div>
      )}
      
      {/* 公告内容 */}
      {announcement.noticeContent && (
        <div 
          className="rich-text-content text-left" 
          style={{ color: '#BCBCBC' }}
          dangerouslySetInnerHTML={{ __html: announcement.noticeContent }}
        />
      )}
    </div>
  );
};

export default OfficialAnnouncement; 