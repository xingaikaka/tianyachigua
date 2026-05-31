import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import Pagination from '../../components/ui/Pagination';
import Footer from '../../components/common/Footer';
import tagsService from '../../services/tagsService';
import videoStatsService from '../../services/videoStatsService';

const Tags = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [paginatedTags, setPaginatedTags] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const pageSize = 150; // 每页显示150个标签

  // 获取指定页码的标签（服务端分页）
  const fetchTagsPage = async (page = 1) => {
    try {
      setLoading(true);
      const response = await tagsService.getTagsPaginated(page, pageSize);
      if (response.code === 200) {
        const data = response.data || {};
        const list = data.list || [];
        const total = data.total || list.length || 0;
        setPaginatedTags(list);
        setTotalCount(total);
        setCurrentPage(page);
      } else {
        setPaginatedTags([]);
        setTotalCount(0);
      }
    } catch (error) {
      setPaginatedTags([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  };

  // 页面初始化
  useEffect(() => {
    
    // 立即定位到页面顶部
    window.scrollTo(0, 0);
    fetchTagsPage(1);
  }, []);

  // 处理分页
  const handlePageChange = (page) => {
    fetchTagsPage(page);
    // 滚动到顶部
    window.scrollTo(0, 0);
  };

  // 处理标签点击
  const handleTagClick = (tagId, tagName) => {
    try { videoStatsService.trackTagClick(tagId, { tagName: tagName || '', from: 'tags_page' }); } catch (_) {}
    // 跳转到标签详情页面
    navigate(`/tag/${tagId}`);
  };

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      
      {/* 主要内容区域 */}
      <div className="pt-28 md:pt-24 pb-8">
        
        {/* 页面标题 */}
        <div className="text-center mb-12">
          <h1 
            className="text-4xl md:text-5xl font-bold text-white mb-8"
            style={{ 
              textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
              letterSpacing: '2px'
            }}
          >
            所有标签
          </h1>
          
          {/* 标签统计信息 */}
          <div className="text-gray-400 text-lg">
            共 <span className="text-white font-medium">{totalCount}</span> 个标签
            {totalCount > pageSize && (
              <span className="ml-4">
                每页显示 <span className="text-white font-medium">{pageSize}</span> 个
              </span>
            )}
          </div>
        </div>

        {/* 标签展示区域 */}
        <div className="flex justify-center">
          <div className="w-full px-4" style={{ maxWidth: '760px' }}>
            
            {loading ? (
              <div className="text-center py-12">
                <div className="text-white text-xl">加载标签中...</div>
              </div>
            ) : (
              <>
                {/* 标签网格 - 参考图片样式 */}
                {paginatedTags.length > 0 ? (
                  <div className="flex flex-wrap gap-3 mb-12">
                    {paginatedTags.map((tag) => (
                      <button
                        key={tag.id}
                        onClick={() => handleTagClick(tag.id, tag.name)}
                        className="px-4 py-2 text-gray-300 hover:text-white text-base rounded-full transition-colors duration-200 cursor-pointer"
                        style={{ 
                          backgroundColor: 'transparent',
                          border: 'none',
                          whiteSpace: 'nowrap'
                        }}
                        onMouseEnter={(e) => {
                          e.target.style.backgroundColor = 'rgba(255,255,255,0.1)';
                        }}
                        onMouseLeave={(e) => {
                          e.target.style.backgroundColor = 'transparent';
                        }}
                      >
                        {tag.name}
                      </button>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-12">
                    <div className="text-gray-400 text-lg">暂无标签数据</div>
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
  );
};

export default Tags;