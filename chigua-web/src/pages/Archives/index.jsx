import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import archivesService from '../../services/archivesService';
import Pagination from '../../components/ui/Pagination';
import Footer from '../../components/common/Footer';
import { usePageConfig } from '../../hooks/usePageConfig';

const Archives = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [archives, setArchives] = useState({});
  const [tags, setTags] = useState([]);
  
  // 获取页面配置
  const { getConfig } = usePageConfig();
  const [currentPage, setCurrentPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const pageSize = 100; // 每页显示100个视频

  // 获取标签数据
  const fetchTags = async () => {
    try {
      const response = await archivesService.getAllTags();
      if (response.code === 200) {
        setTags(response.data || []);

      }
    } catch (error) {
      
    }
  };

  // 获取往期内容数据
  const fetchArchives = async (page = 1) => {
    try {
      setLoading(true);
      const response = await archivesService.getArchivesByDate(page, pageSize);
      
      if (response.code === 200) {
        setArchives(response.data?.archives || {});
        setTotalCount(response.data?.total || 0);
        setCurrentPage(page);
        
      } else {
        
      }
    } catch (error) {
      
    } finally {
      setLoading(false);
    }
  };

  // 页面初始化
  useEffect(() => {
    
    // 立即定位到页面顶部
    window.scrollTo(0, 0);

    fetchTags();
    fetchArchives(1);
  }, []);

  // 处理分页
  const handlePageChange = (page) => {
    fetchArchives(page);
    // 滚动到顶部
    window.scrollTo(0, 0);
  };

  // 处理视频点击
  const handleVideoClick = (videoId) => {
    navigate(`/video/${videoId}`);
  };

  // 处理标签点击
  const handleTagClick = (tagId) => {
    navigate(`/tag/${tagId}`);
  };

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      {/* CSS样式 - 网站地址信息样式 */}
      <style dangerouslySetInnerHTML={{
        __html: `
          .detail-site-address a {
            color: #1ABC9C !important;
            text-decoration: none !important;
            transition: opacity 0.2s ease !important;
          }
          .detail-site-address a:hover {
            opacity: 0.8 !important;
          }
          .detail-site-address p, .detail-site-address span, .detail-site-address strong {
            color: #AAAAAA !important;
          }
        `
      }} />
      
      {/* 主要内容区域 */}
      <div className="pt-8 md:pt-20 pb-8">
        <div className="container mx-auto px-4 max-w-6xl">
        
        {/* 页面标题 */}
        <div className="flex justify-center mb-8 px-1 md:px-4">
          <div className="w-full text-center" style={{ maxWidth: '770px' }}>
          <h1 
            className="text-4xl md:text-5xl font-bold mb-8"
            style={{ 
              color: '#BCBCBC',
              textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
              letterSpacing: '2px'
            }}
          >
            往期内容
          </h1>
          </div>
        </div>

        {/* 标签云区域 */}
        <div className="flex justify-center mb-8 sm:mb-12 px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '860px' }}>
            <div className="px-8 py-8">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-medium text-left" style={{ color: '#BCBCBC' }}>标签云</h2>
                <span 
                  className="text-base cursor-pointer transition-colors duration-200"
                  style={{ color: '#BCBCBC' }}
                  onClick={() => navigate('/tags')}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.color = '#FFFFFF';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.color = '#BCBCBC';
                  }}
                >
                  更多&gt;&gt;
                </span>
              </div>
              
              {/* 标签云 */}
              <div className="flex flex-wrap gap-2">
                {tags.length > 0 ? tags.map((tag) => (
                  <button
                    key={tag.id}
                    onClick={() => handleTagClick(tag.id)}
                    className="transition-all duration-200"
                    style={{ 
                      color: '#BCBCBC',
                      backgroundColor: 'transparent',
                      padding: '8px 16px',
                      borderRadius: '0',
                      border: 'none',
                      cursor: 'pointer',
                      whiteSpace: 'nowrap',
                      fontSize: '16px',
                      fontWeight: '400'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.backgroundColor = '#4a4848';
                      e.currentTarget.style.borderRadius = '20px';
                      e.currentTarget.style.color = '#FFFFFF';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.backgroundColor = 'transparent';
                      e.currentTarget.style.borderRadius = '0';
                      e.currentTarget.style.color = '#BCBCBC';
                    }}
                  >
                    {tag.name}
                  </button>
                )) : (
                  <div className="w-full text-center py-4 text-lg" style={{ color: '#BCBCBC' }}>
                    {loading ? '加载标签中...' : '暂无标签数据'}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* 网站站点地址区域（往期内容页不显示） */}

        {/* 按日期分组的视频列表 */}
        <div className="flex justify-center px-1 md:px-4">
          <div className="w-full" style={{ maxWidth: '860px' }}>
            <div className="px-2 sm:px-8">
            
            {loading ? (
              <div className="text-center py-12">
                <div className="text-2xl" style={{ color: '#BCBCBC' }}>加载中...</div>
              </div>
            ) : (
              <>
                {/* 按月份分组显示 */}
                {Object.keys(archives).length > 0 ? (
                  <div className="space-y-12">
                    {Object.entries(archives).map(([monthKey, videos]) => (
                      <div key={monthKey}>
                        {/* 月份标题 */}
                        <h3 
                          className="text-3xl mb-6 font-medium text-left"
                          style={{ 
                            color: '#BCBCBC',
                            borderBottom: '1px solid #555', 
                            paddingBottom: '8px',
                            textAlign: 'left'
                          }}
                        >
                          {monthKey}
                        </h3>
                        
                        {/* 该月的视频列表 */}
                        <div className="space-y-3">
                          {videos.map((video) => (
                            <div 
                              key={video.id}
                              className="flex items-start cursor-pointer py-3 pl-0 pr-3 sm:p-3 transition-all duration-200"
                              onClick={() => handleVideoClick(video.id)}
                            >
                              {/* 日期 */}
                              <div 
                                className="text-base mr-4 sm:mr-6 flex-shrink-0" 
                                style={{ 
                                  color: '#BCBCBC',
                                  minWidth: '90px',
                                  paddingTop: '2px'
                                }}
                              >
                                {video.dateDisplay}
                              </div>
                              
                              {/* 视频标题 */}
                              <div 
                                className="text-lg transition-colors duration-200 flex-1"
                                style={{ 
                                  color: '#BCBCBC',
                                  textAlign: 'left',
                                  lineHeight: '1.4'
                                }}
                                onMouseEnter={(e) => {
                                  e.currentTarget.style.color = '#FFFFFF';
                                }}
                                onMouseLeave={(e) => {
                                  e.currentTarget.style.color = '#BCBCBC';
                                }}
                              >
                                {video.title}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-12">
                    <div className="text-xl" style={{ color: '#BCBCBC' }}>暂无往期内容</div>
                  </div>
                )}
                
                {/* 分页组件 */}
                {totalCount > pageSize && (
                  <div className="flex justify-center mt-12">
                    <Pagination
                      current={currentPage}
                      total={totalCount}
                      pageSize={pageSize}
                      onChange={handlePageChange}
                    />
                  </div>
                )}
              </>
            )}
            </div>
          </div>
        </div>
        
        {/* 底部区域 */}
        <Footer />
        
        </div>
      </div>
    </div>
  );
};

export default Archives;