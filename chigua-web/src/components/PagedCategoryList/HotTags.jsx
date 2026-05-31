import React from 'react';
import videoStatsService from '../../services/videoStatsService';
import './HotTags.css';

/**
 * 热点标签组件
 * 显示从视频列表API返回的热点标签，点击后执行查询
 */
const HotTags = ({ hotTags = [], onTagClick, selectedTagIds = [] }) => {
  // 处理标签点击
  const handleTagClick = (tagId, tagName) => {
    if (onTagClick && tagId) {
      const isSelected = selectedTagIds.includes(tagId);
      if (!isSelected) {
        try { videoStatsService.trackTagClick(tagId, { tagName: tagName || '', from: 'hot_tags' }); } catch (_) {}
      }
      // 如果标签已选中，则取消选择；否则选中该标签
      if (isSelected) {
        onTagClick([]);
      } else {
        onTagClick([tagId]);
      }
    }
  };

  if (!hotTags || hotTags.length === 0) {
    return null; // 无热点标签时不显示
  }

  return (
    <div className="hot-tags-container">
      <div className="hot-tags-list">
        {hotTags.map((tag) => {
          const tagId = tag.id;
          const isSelected = selectedTagIds.includes(tagId);
          
          return (
            <button
              key={tagId}
              className={`hot-tag-item ${isSelected ? 'selected' : ''}`}
              onClick={() => handleTagClick(tagId, tag.name)}
            >
              {tag.name}
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default HotTags;

