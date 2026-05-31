import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import categoryCacheService from '../../services/categoryCacheService';
import videoStatsService from '../../services/videoStatsService';

const CategoryNavBar = ({ currentCategoryId = null, onCategoryChange, compact = false }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [showMoreDropdown, setShowMoreDropdown] = useState(false);
  // 初始化时立即检查缓存，避免闪烁
  const initializeData = () => {
    // 尝试从内存缓存立即获取数据
    const cachedCategories = categoryCacheService.memoryCache.categories;
    const cachedMoreMenu = categoryCacheService.memoryCache.moreMenu;
    const cacheTimestamp = categoryCacheService.memoryCache.timestamp;
    
    // 检查内存缓存是否有效
    const isCacheValid = cacheTimestamp && 
      (Date.now() - cacheTimestamp < categoryCacheService.CACHE_CONFIG.CACHE_DURATION);
    
    if (isCacheValid && cachedCategories && cachedMoreMenu) {
      // 有有效缓存，立即使用
      return {
        categories: cachedCategories,
        moreMenuItems: cachedMoreMenu,
        loading: false
      };
    }
    
    // 尝试从本地存储获取
    try {
      const localCategories = categoryCacheService.getCachedData(categoryCacheService.CACHE_KEYS.CATEGORIES);
      const localMoreMenu = categoryCacheService.getCachedData(categoryCacheService.CACHE_KEYS.MORE_MENU);
      const localTimestamp = categoryCacheService.getCachedTimestamp();
      
      const isLocalCacheValid = localTimestamp && 
        (Date.now() - localTimestamp < categoryCacheService.CACHE_CONFIG.CACHE_DURATION);
      
      if (isLocalCacheValid && localCategories && localMoreMenu) {
        // 有有效的本地缓存，立即使用
        return {
          categories: localCategories,
          moreMenuItems: localMoreMenu,
          loading: false
        };
      }
    } catch (error) {
    }
    
    // 没有有效缓存，需要从API获取
    return {
      categories: [],
      moreMenuItems: [],
      loading: true
    };
  };

  // 使用初始化数据
  const initialData = initializeData();
  const [categories, setCategories] = useState(initialData.categories);
  const [loading, setLoading] = useState(initialData.loading);
  const [moreMenuItems, setMoreMenuItems] = useState(initialData.moreMenuItems);

  // 获取分类和更多菜单数据
  useEffect(() => {
    const fetchData = async () => {
      try {
        // 如果已经有缓存数据，就不显示加载状态
        if (categories.length === 0 || moreMenuItems.length === 0) {
          setLoading(true);
        }
        
        // 并行获取分类和更多菜单数据
        const [categoriesData, moreMenuData] = await Promise.all([
          categoryCacheService.getCategories(),
          categoryCacheService.getMoreMenuItems()
        ]);
        
        setCategories(categoriesData);
        setMoreMenuItems(moreMenuData);
        
      } catch (error) {
        // 只有在没有任何数据时才设置为空
        if (categories.length === 0) {
          setCategories([]);
        }
        if (moreMenuItems.length === 0) {
          setMoreMenuItems([]);
        }
      } finally {
        setLoading(false);
      }
    };

    // 如果初始化时没有获取到缓存数据，才需要异步获取
    if (initialData.loading) {
      fetchData();
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // 监听缓存数据更新
  useEffect(() => {
    const handleDataUpdate = (event) => {
      const { type, data } = event.detail;
      if (type === 'categories') {
        setCategories(data);
      } else if (type === 'moreMenu') {
        setMoreMenuItems(data);
      }
    };

    window.addEventListener('categoryDataUpdate', handleDataUpdate);
    
    return () => {
      window.removeEventListener('categoryDataUpdate', handleDataUpdate);
    };
  }, []);


  // 点击外部区域关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (showMoreDropdown && !event.target.closest('.more-dropdown-container')) {
        setShowMoreDropdown(false);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [showMoreDropdown]);

  // 处理分类点击
  const handleCategoryClick = (category) => {
    // 分类点击：置顶行为仅在进入分类页时生效，不影响后退返回
    window.scrollTo(0, 0);

    // 🔄 清除相关缓存，确保分类切换时获取新数据
    try {
      // 触发缓存清理事件，通知相关组件清理缓存
      window.dispatchEvent(new CustomEvent('categoryChange', { 
        detail: { 
          categoryId: category.id,
          timestamp: Date.now()
        } 
      }));
    } catch (_) {}

    if (category.id === null) {
      // 点击"全部" - 跳转到首页，标记为路由导航
      sessionStorage.setItem('route_navigation', 'true');
      try { videoStatsService.trackCategoryClick('all'); } catch (_) {}
      navigate('/', { state: { forceTop: true } });
    } else {
      // 点击具体分类 - 跳转到分类页面
      try { videoStatsService.trackCategoryClick(category.id); } catch (_) {}
      // 显式表明来源为搜索页，以便分类页在 POP 返回时不置顶
      const fromSearch = location.pathname.startsWith('/search');
      navigate(`/category/${category.id}`, { state: { forceTop: true, fromSearch } });
    }

    // 如果有回调函数，也调用它
    if (onCategoryChange) {
      onCategoryChange(category.id);
    }
  };

  // 处理"更多"菜单项点击
  const handleMoreMenuClick = (menuItem) => {
    setShowMoreDropdown(false); // 关闭下拉菜单
    
    const url = menuItem.jumpUrl;
    if (!url) return;
    
    // 判断是外部链接还是内部路由
    if (url.startsWith('http://') || url.startsWith('https://')) {
      // 外部链接，在新窗口打开
      window.open(url, '_blank', 'noopener,noreferrer');
    } else {
      // 内部路由，使用navigate跳转
      navigate(url);
    }
  };

  // 处理"更多"按钮点击
  const handleMoreButtonClick = (e) => {
    e.stopPropagation();
    
    setShowMoreDropdown(!showMoreDropdown);
  };

  // 判断分类是否激活
  const isActive = (categoryId) => {
    if (categoryId === null) {
      // "全部"选项在首页时激活
      return location.pathname === '/';
    } else {
      // 其他分类根据当前分类ID判断
      return currentCategoryId === categoryId;
    }
  };

  if (loading && categories.length === 0) {
    return (
      <div className="category-navbar-loading" style={{
        height: '48px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'transparent', // 改为透明，减少视觉冲击
        margin: '0 16px',
        borderRadius: '8px'
      }}>
        <span style={{ color: '#ffffff80', fontSize: '14px' }}>加载分类中...</span>
      </div>
    );
  }

  return (
    <div className={`category-navbar ${compact ? 'compact' : ''}`} style={{
      width: '100%',
      background: 'transparent',
      padding: compact ? '0' : '12px 0',
      position: 'relative',
      zIndex: compact ? 'auto' : 40 // Header内部时不需要特殊层级
    }}>
      {/* 横向滚动容器 */}
      <div 
        className={`category-scroll-container ${compact ? 'compact' : ''}`}
        style={{
          display: 'flex',
          overflowX: 'auto',
          overflowY: 'hidden',
          gap: compact ? '10px' : '12px',
          padding: compact ? '0 8px' : '0 16px',
          scrollBehavior: 'smooth',
          WebkitOverflowScrolling: 'touch', // iOS平滑滚动
          scrollbarWidth: 'none', // Firefox隐藏滚动条
          msOverflowStyle: 'none', // IE隐藏滚动条
          // 移动端优化
          touchAction: 'pan-x', // 只允许水平滚动
          scrollSnapType: 'x proximity' // 滚动吸附
        }}
      >
        {categories.map((category) => (
          <div
            key={category.id || 'all'}
            className={`category-item ${compact ? 'compact' : ''}`}
            onClick={() => handleCategoryClick(category)}
            style={{
              flex: 'none', // 防止收缩
              minWidth: 'fit-content',
              padding: compact ? '6px 10px' : '8px 12px',
              background: 'transparent', // 完全透明背景
              border: 'none', // 移除边框
              fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
              fontSize: '16px',
              lineHeight: '32px',
              fontWeight: '400',
              fontStyle: 'normal',
              color: isActive(category.id) ? '#1ABC9C' : '#FFFFFFE6', // 激活时青绿色，非激活时使用新默认颜色
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              userSelect: 'none',
              whiteSpace: 'nowrap',
              // 移动端优化
              scrollSnapAlign: 'start', // 滚动吸附对齐
              WebkitTapHighlightColor: 'transparent' // 移除点击高亮
            }}
            onMouseEnter={(e) => {
              if (!isActive(category.id) && e.target instanceof HTMLElement) {
                e.target.style.color = 'rgba(255, 255, 255, 0.75)';
                e.target.style.transform = 'scale(1.05)';
              }
            }}
            onMouseLeave={(e) => {
              if (!isActive(category.id) && e.target instanceof HTMLElement) {
                e.target.style.color = '#FFFFFFE6';
                e.target.style.transform = 'scale(1)';
              }
            }}
          >
            {category.categoryName}
          </div>
        ))}
        
        {/* "更多"按钮和下拉菜单 */}
        {moreMenuItems.length > 0 && (
          <div className="more-dropdown-container" style={{ position: 'relative', flex: 'none' }}>
            <div
              className={`category-item more-button ${compact ? 'compact' : ''}`}
              onClick={handleMoreButtonClick}
              style={{
                flex: 'none',
                minWidth: 'fit-content',
                padding: compact ? '6px 10px' : '8px 12px',
                background: 'transparent',
                border: 'none',
                fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                fontSize: '16px',
                lineHeight: '32px',
                fontWeight: '400',
                fontStyle: 'normal',
                color: showMoreDropdown ? '#1ABC9C' : '#FFFFFFE6',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                userSelect: 'none',
                whiteSpace: 'nowrap',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                scrollSnapAlign: 'start',
                WebkitTapHighlightColor: 'transparent'
              }}
              onMouseEnter={(e) => {
                if (!showMoreDropdown && e.target instanceof HTMLElement) {
                  e.target.style.color = 'rgba(255, 255, 255, 0.75)';
                  e.target.style.transform = 'scale(1.05)';
                }
              }}
              onMouseLeave={(e) => {
                if (!showMoreDropdown && e.target instanceof HTMLElement) {
                  e.target.style.color = '#FFFFFFE6';
                  e.target.style.transform = 'scale(1)';
                }
              }}
            >
              更多
              <span style={{ 
                fontSize: '12px',
                transform: showMoreDropdown ? 'rotate(180deg)' : 'rotate(0deg)',
                transition: 'transform 0.3s ease'
              }}>▼</span>
            </div>
            
            {/* 下拉菜单 */}
            {showMoreDropdown && (
              <div
                className="more-dropdown-menu"
                style={{
                  position: 'absolute',
                  top: '100%',
                  left: '0',
                  background: 'rgba(0, 0, 0, 0.9)',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: '8px',
                  minWidth: '160px',
                  padding: '8px 0',
                  zIndex: 1000,
                  boxShadow: '0 4px 12px rgba(0, 0, 0, 0.3)',
                  animation: 'fadeIn 0.2s ease-out',
                  marginTop: '4px',
                  backdropFilter: 'blur(10px)'
                }}
              >
                {moreMenuItems.map((item) => (
                  <div
                    key={item.configId}
                    className="more-menu-item"
                    onClick={() => handleMoreMenuClick(item)}
                    style={{
                      padding: '8px 16px',
                      color: '#ffffff',
                      fontSize: '14px',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
                      userSelect: 'none'
                    }}
                    onMouseEnter={(e) => {
                      if (e.target instanceof HTMLElement) {
                        e.target.style.backgroundColor = '#403E3F';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (e.target instanceof HTMLElement) {
                        e.target.style.backgroundColor = 'transparent';
                      }
                    }}
                  >
                    {item.basicContent}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 自定义滚动条样式 */}
      <style>{`
        .category-scroll-container::-webkit-scrollbar {
          display: none; /* WebKit浏览器隐藏滚动条 */
        }
        
        /* 移动端触摸优化 */
        .category-item:active {
          transform: scale(0.95);
          color: #1ABC9C !important;
        }
        
        /* 响应式设计 */
        @media (max-width: 768px) {
          .category-navbar:not(.compact) {
            padding: 8px 0;
          }
          .category-scroll-container:not(.compact) {
            gap: 8px;
            padding: 0 12px;
          }
          .category-item:not(.compact) {
            padding: 6px 8px !important;
            font-size: 12px !important;
          }
        }
        
        /* 清除渐变边缘效果，去掉背景 */
        
        /* 下拉菜单动画 */
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(-10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        
        /* 更多按钮响应式优化 */
        @media (max-width: 768px) {
          .more-dropdown-menu {
            min-width: 140px !important;
            right: 0;
            left: auto;
          }
          .more-menu-item {
            padding: 10px 12px !important;
            font-size: 13px !important;
          }
        }
      `}</style>
    </div>
  );
};

export default CategoryNavBar;/* Category More Dropdown Implementation Mon Aug  4 05:54:03 CST 2025 */
