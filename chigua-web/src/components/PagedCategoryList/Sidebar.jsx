import React, { useState, useEffect } from 'react';
import categoryService from '../../services/categoryService';
import './Sidebar.css';

/**
 * 标签侧边栏组件
 */
export const TagsSidebar = ({ categoryId, selectedTagId, onTagClick }) => {
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!categoryId) {
      setTags([]);
      setLoading(false);
      return;
    }

    const fetchTags = async () => {
      try {
        setLoading(true);
        const response = await categoryService.getCategoryTags(categoryId);
        if (response && response.code === 200 && Array.isArray(response.data)) {
          setTags(response.data);
        } else {
          setTags([]);
        }
      } catch (error) {
        setTags([]);
      } finally {
        setLoading(false);
      }
    };

    fetchTags();
  }, [categoryId]);

  if (loading) {
    return (
      <div className="paged-sidebar paged-sidebar-left">
        <div className="paged-sidebar-title">标签</div>
        <div className="paged-sidebar-loading">加载中...</div>
      </div>
    );
  }

  if (tags.length === 0) {
    return (
      <div className="paged-sidebar paged-sidebar-left">
        <div className="paged-sidebar-title">标签</div>
        <div className="paged-sidebar-empty">暂无标签</div>
      </div>
    );
  }

  return (
    <div className="paged-sidebar paged-sidebar-left">
      <div className="paged-sidebar-title">标签</div>
      <div className="paged-sidebar-content">
        {tags.map((tag) => {
          const tagId = typeof tag === 'object' ? tag.id : tag;
          const tagName = typeof tag === 'object' ? tag.name : tag;
          const isSelected = selectedTagId === tagId;
          
          return (
            <div
              key={tagId}
              className={`paged-sidebar-item ${isSelected ? 'active' : ''}`}
              onClick={() => onTagClick && onTagClick(tagId)}
            >
              {tagName}
            </div>
          );
        })}
      </div>
    </div>
  );
};

/**
 * 作者侧边栏组件
 */
export const AuthorsSidebar = ({ categoryId, selectedAuthor, onAuthorClick }) => {
  const [authors, setAuthors] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!categoryId) {
      setAuthors([]);
      setLoading(false);
      return;
    }

    const fetchAuthors = async () => {
      try {
        setLoading(true);
        const response = await categoryService.getCategoryAuthors(categoryId);
        if (response && response.code === 200 && Array.isArray(response.data)) {
          setAuthors(response.data);
        } else {
          setAuthors([]);
        }
      } catch (error) {
        setAuthors([]);
      } finally {
        setLoading(false);
      }
    };

    fetchAuthors();
  }, [categoryId]);

  if (loading) {
    return (
      <div className="paged-sidebar paged-sidebar-right">
        <div className="paged-sidebar-title">作者</div>
        <div className="paged-sidebar-loading">加载中...</div>
      </div>
    );
  }

  if (authors.length === 0) {
    return (
      <div className="paged-sidebar paged-sidebar-right">
        <div className="paged-sidebar-title">作者</div>
        <div className="paged-sidebar-empty">暂无作者</div>
      </div>
    );
  }

  return (
    <div className="paged-sidebar paged-sidebar-right">
      <div className="paged-sidebar-title">作者</div>
      <div className="paged-sidebar-content">
        {authors.map((author, index) => {
          const authorName = typeof author === 'string' ? author : String(author);
          const isSelected = selectedAuthor === authorName;
          
          return (
            <div
              key={`${authorName}-${index}`}
              className={`paged-sidebar-item ${isSelected ? 'active' : ''}`}
              onClick={() => onAuthorClick && onAuthorClick(authorName)}
            >
              {authorName}
            </div>
          );
        })}
      </div>
    </div>
  );
};

