import React, { useState, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import categoryService from '../../services/categoryService';
import './FilterModal.css';

/**
 * 筛选模态框组件
 */
const FilterModal = ({ 
  open, 
  onClose, 
  categoryId, 
  type, // 'tag' 或 'author'
  selectedTagIds = [], // 改为数组，支持多选
  selectedAuthors = [], // 改为数组，支持多选
  onConfirm // 确定按钮回调，传递选中的标签/作者数组
}) => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  // 临时选择的标签/作者（在点击确定前不会生效）
  const [tempSelectedTagIds, setTempSelectedTagIds] = useState([]);
  const [tempSelectedAuthors, setTempSelectedAuthors] = useState([]);

  // 当模态框打开时，初始化临时选择状态
  useEffect(() => {
    if (open) {
      if (type === 'tag') {
        setTempSelectedTagIds(Array.isArray(selectedTagIds) ? [...selectedTagIds] : (selectedTagIds ? [selectedTagIds] : []));
      } else {
        setTempSelectedAuthors(Array.isArray(selectedAuthors) ? [...selectedAuthors] : (selectedAuthors ? [selectedAuthors] : []));
      }
      setSearchKeyword('');
    }
  }, [open, type, selectedTagIds, selectedAuthors]);

  useEffect(() => {
    if (!open || !categoryId) {
      setItems([]);
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        let response;
        
        if (type === 'tag') {
          response = await categoryService.getCategoryTags(categoryId);
        } else {
          response = await categoryService.getCategoryAuthors(categoryId);
        }
        
        if (response && response.code === 200 && Array.isArray(response.data)) {
          setItems(response.data);
        } else {
          setItems([]);
        }
      } catch (error) {
        setItems([]);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [open, categoryId, type]);

  // 过滤数据
  const filteredItems = useMemo(() => {
    if (!searchKeyword.trim()) {
      return items;
    }
    
    const keyword = searchKeyword.toLowerCase().trim();
    
    if (type === 'tag') {
      return items.filter(item => {
        const tagName = typeof item === 'object' ? item.name : String(item);
        return tagName.toLowerCase().includes(keyword);
      });
    } else {
      return items.filter(item => {
        const authorName = typeof item === 'string' ? item : String(item);
        return authorName.toLowerCase().includes(keyword);
      });
    }
  }, [items, searchKeyword, type]);

  // 处理选择（多选，最多5个）
  const handleSelect = (item) => {
    if (type === 'tag') {
      const tagId = typeof item === 'object' ? item.id : item;
      setTempSelectedTagIds(prev => {
        if (prev.includes(tagId)) {
          // 如果已选中，则移除
          return prev.filter(id => id !== tagId);
        } else {
          // 如果未选中，检查是否已达到上限
          if (prev.length >= 5) {
            // 已达到上限，不添加
            return prev;
          }
          // 如果未选中，则添加
          return [...prev, tagId];
        }
      });
    } else {
      const authorName = typeof item === 'string' ? item : String(item);
      setTempSelectedAuthors(prev => {
        if (prev.includes(authorName)) {
          // 如果已选中，则移除
          return prev.filter(name => name !== authorName);
        } else {
          // 如果未选中，检查是否已达到上限
          if (prev.length >= 5) {
            // 已达到上限，不添加
            return prev;
          }
          // 如果未选中，则添加
          return [...prev, authorName];
        }
      });
    }
  };

  // 删除已选择的标签/作者
  const handleRemoveSelected = (item) => {
    if (type === 'tag') {
      const tagId = typeof item === 'object' ? item.id : item;
      setTempSelectedTagIds(prev => prev.filter(id => id !== tagId));
    } else {
      const authorName = typeof item === 'string' ? item : String(item);
      setTempSelectedAuthors(prev => prev.filter(name => name !== authorName));
    }
  };

  // 判断是否选中（基于临时选择状态）
  const isSelected = (item) => {
    if (type === 'tag') {
      const tagId = typeof item === 'object' ? item.id : item;
      return tempSelectedTagIds.includes(tagId);
    } else {
      const authorName = typeof item === 'string' ? item : String(item);
      return tempSelectedAuthors.includes(authorName);
    }
  };

  // 获取已选择的标签/作者列表
  const getSelectedItems = () => {
    if (type === 'tag') {
      return items.filter(item => {
        const tagId = typeof item === 'object' ? item.id : item;
        return tempSelectedTagIds.includes(tagId);
      });
    } else {
      return tempSelectedAuthors.map(authorName => authorName);
    }
  };

  // 处理确定按钮
  const handleConfirm = () => {
    if (type === 'tag') {
      onConfirm && onConfirm(tempSelectedTagIds);
    } else {
      onConfirm && onConfirm(tempSelectedAuthors);
    }
    onClose();
  };

  // 获取显示名称
  const getDisplayName = (item) => {
    if (type === 'tag') {
      return typeof item === 'object' ? item.name : String(item);
    } else {
      return typeof item === 'string' ? item : String(item);
    }
  };

  if (!open) {
    return null;
  }

  const title = type === 'tag' ? '标签查询' : '女优查询';

  const modalContent = (
    <div className="filter-modal-overlay" onClick={onClose}>
      <div className="filter-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="filter-modal-header">
          <h2 className="filter-modal-title">{title}</h2>
          <button className="filter-modal-close" onClick={onClose}>×</button>
        </div>
        
        <div className="filter-modal-search">
          <input
            type="text"
            placeholder={`搜索${type === 'tag' ? '标签' : '女优'}...`}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="filter-modal-search-input"
            autoFocus
          />
        </div>

        {/* 已选择的标签/作者显示区域 */}
        {getSelectedItems().length > 0 && (
          <div className="filter-modal-selected">
            <div className="filter-modal-selected-label">
              已选择：<span className="filter-modal-selected-count">
                ({type === 'tag' ? tempSelectedTagIds.length : tempSelectedAuthors.length}/5)
              </span>
            </div>
            <div className="filter-modal-selected-list">
              {getSelectedItems().map((item, index) => {
                const displayName = getDisplayName(item);
                const key = type === 'tag' 
                  ? (typeof item === 'object' ? item.id : item)
                  : `${displayName}-${index}`;
                
                return (
                  <div key={key} className="filter-modal-selected-item">
                    <span>{displayName}</span>
                    <button
                      className="filter-modal-selected-remove"
                      onClick={() => handleRemoveSelected(item)}
                      title="删除"
                    >
                      ×
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        <div className="filter-modal-body">
          {loading ? (
            <div className="filter-modal-loading">加载中...</div>
          ) : filteredItems.length === 0 ? (
            <div className="filter-modal-empty">
              {searchKeyword.trim() ? '未找到匹配项' : '暂无数据'}
            </div>
          ) : (
            <div className="filter-modal-list">
              {filteredItems.map((item, index) => {
                const displayName = getDisplayName(item);
                const selected = isSelected(item);
                const key = type === 'tag' 
                  ? (typeof item === 'object' ? item.id : item)
                  : `${displayName}-${index}`;
                
                // 检查是否已达到上限且未选中
                const isMaxReached = type === 'tag' 
                  ? (tempSelectedTagIds.length >= 5 && !selected)
                  : (tempSelectedAuthors.length >= 5 && !selected);
                
                return (
                  <div
                    key={key}
                    className={`filter-modal-item ${selected ? 'selected' : ''} ${isMaxReached ? 'disabled' : ''}`}
                    onClick={() => {
                      if (!isMaxReached) {
                        handleSelect(item);
                      }
                    }}
                    title={isMaxReached ? '最多只能选择5个' : ''}
                  >
                    {displayName}
                    {selected && <span className="filter-modal-check">✓</span>}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 底部按钮区域 */}
        <div className="filter-modal-footer">
          <button className="filter-modal-button filter-modal-button-cancel" onClick={onClose}>
            取消
          </button>
          <button className="filter-modal-button filter-modal-button-confirm" onClick={handleConfirm}>
            确定
          </button>
        </div>
      </div>
    </div>
  );

  // 使用 Portal 渲染到 body，确保不受父元素层级限制
  if (!open) {
    return null;
  }

  return createPortal(modalContent, document.body);
};

export default FilterModal;

