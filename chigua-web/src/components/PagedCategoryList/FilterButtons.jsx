import React from 'react';
import './FilterButtons.css';

/**
 * 筛选按钮组件
 */
const FilterButtons = ({ onTagClick, onAuthorClick, selectedTagId, selectedAuthor }) => {
  return (
    <div className="filter-buttons-container">
      <button
        className={`filter-button ${selectedTagId ? 'active' : ''}`}
        onClick={onTagClick}
      >
        标签查询
        {selectedTagId && <span className="filter-button-badge">●</span>}
      </button>
      <button
        className={`filter-button ${selectedAuthor ? 'active' : ''}`}
        onClick={onAuthorClick}
      >
        女优查询
        {selectedAuthor && <span className="filter-button-badge">●</span>}
      </button>
    </div>
  );
};

export default FilterButtons;

