import React, { memo, useMemo } from 'react';
import Header from '../Header';
import { useCategories } from '../../../context/CategoriesContext';

// 包装Header，只有在分类列表变化时才重新渲染
const HeaderContainer = memo(({ categoriesVersion }) => {
  return <Header />;
}, (prev, next) => prev.categoriesVersion === next.categoriesVersion);

const StaticHeader = () => {
  const { categories } = useCategories();
  const categoriesVersion = useMemo(() => {
    if (!Array.isArray(categories) || categories.length === 0) {
      return 'empty';
    }
    return categories
      .map(category => `${category.id}-${category.name || ''}-${category.isShort || 0}-${category.isRecommended || 0}`)
      .join('|');
  }, [categories]);

  return <HeaderContainer categoriesVersion={categoriesVersion} />;
};

export default StaticHeader;
