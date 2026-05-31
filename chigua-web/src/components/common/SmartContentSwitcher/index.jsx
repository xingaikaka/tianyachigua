import React, { useState, useEffect } from 'react';
import { useCategoryContent } from '../../../context/CategoryContentContext';
import VideoList from '../../VideoList';
import CollectionList from '../../CollectionList';
import categoryService from '../../../services/categoryService';
import apiCacheService from '../../../services/apiCacheService';
import './SmartContentSwitcher.css';

const SmartContentSwitcher = () => {
  const { currentCategoryId, isTransitioning } = useCategoryContent();
  const [category, setCategory] = useState(null);
  const [loading, setLoading] = useState(false);
  
  // 获取分类信息
  useEffect(() => {
    const fetchCategoryInfo = async () => {
      if (!currentCategoryId) {
        setCategory(null);
        return;
      }
      
      try {
        setLoading(true);
        // 使用缓存服务获取分类详情
        const response = await apiCacheService.getApiData(
          'CATEGORY_DETAIL',
          { categoryId: currentCategoryId },
          (params) => categoryService.getCategoryById(params.categoryId),
          { cacheDuration: 10 * 60 * 1000 } // 10分钟缓存
        );
        if (response.code === 200) {
          setCategory(response.data);
        } else {
          setCategory(null);
        }
      } catch (error) {
        setCategory(null);
      } finally {
        setLoading(false);
      }
    };
    
    fetchCategoryInfo();
  }, [currentCategoryId]);
  
  // 渲染内容组件
  const renderContent = () => {
    if (loading) {
      return (
        <div className="container mx-auto px-4 py-8 max-w-5xl">
          <div className="flex justify-center items-center h-64">
            <div className="text-white text-lg">加载中...</div>
          </div>
        </div>
      );
    }
    
    // 首页显示所有视频
    if (!currentCategoryId) {
      return <VideoList categoryId={null} />;
    }
    
    // 根据分类类型显示不同组件
    if (category && category.isCollection === 1) {
      return <CollectionList categoryId={currentCategoryId} />;
    } else {
      return <VideoList categoryId={currentCategoryId} />;
    }
  };
  
  return (
    <div className={`smart-content-switcher ${isTransitioning ? 'transitioning' : ''}`}>
      <div className="content-container">
        {renderContent()}
      </div>
    </div>
  );
};

export default SmartContentSwitcher;
