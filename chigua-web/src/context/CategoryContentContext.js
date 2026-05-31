import React, { createContext, useContext, useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const CategoryContentContext = createContext();

export const useCategoryContent = () => {
  const context = useContext(CategoryContentContext);
  if (!context) {
    throw new Error('useCategoryContent must be used within a CategoryContentProvider');
  }
  return context;
};

export const CategoryContentProvider = ({ children }) => {
  const location = useLocation();
  const [currentCategoryId, setCurrentCategoryId] = useState(null);
  const [isTransitioning, setIsTransitioning] = useState(false);
  
  // 从URL解析当前分类ID
  useEffect(() => {
    // 只有在首页或分类页面时才更新分类状态
    if (location.pathname === '/' || location.pathname.match(/^\/category\/\d+$/)) {
      const match = location.pathname.match(/\/category\/(\d+)/);
      const categoryId = match ? parseInt(match[1]) : null;
      setCurrentCategoryId(categoryId);
    }
  }, [location.pathname]);
  
  // 平滑切换分类内容
  const switchCategory = async (categoryId) => {
    if (categoryId === currentCategoryId) return;
    
    setIsTransitioning(true);
    
    // 短暂延迟以显示过渡效果
    setTimeout(() => {
      setCurrentCategoryId(categoryId);
      setIsTransitioning(false);
      
      // 更新URL但不导航（避免页面重新渲染）
      const newUrl = categoryId ? `/category/${categoryId}` : '/';
      window.history.pushState({}, '', newUrl);
    }, 150);
  };
  
  // 切换到首页
  const switchToHome = () => {
    if (currentCategoryId === null) return;
    
    setIsTransitioning(true);
    
    setTimeout(() => {
      setCurrentCategoryId(null);
      setIsTransitioning(false);
      window.history.pushState({}, '', '/');
    }, 150);
  };
  
  const value = {
    currentCategoryId,
    isTransitioning,
    switchCategory,
    switchToHome
  };
  
  return (
    <CategoryContentContext.Provider value={value}>
      {children}
    </CategoryContentContext.Provider>
  );
};
