import React, { useState, useRef, useEffect } from 'react';

const EmojiPicker = ({ onEmojiSelect, isVisible, onClose }) => {
  const [activeCategory, setActiveCategory] = useState('bubbles');
  const pickerRef = useRef(null);

  // 表情数据
  const emojiCategories = {
    bubbles: {
      name: '泡泡',
      emojis: [
        '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃',
        '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙',
        '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔'
      ]
    },
    alu: {
      name: '阿鲁',
      emojis: [
        '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥',
        '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮',
        '🤧', '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '🥸', '😎'
      ]
    },
    text: {
      name: '颜文字',
      emojis: [
        '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔',
        '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️',
        '✨', '⭐', '🌟', '💫', '⚡', '💥', '💢', '💤', '💦', '💨'
      ]
    }
  };

  const categories = Object.keys(emojiCategories);

  // 点击外部关闭
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (pickerRef.current && !pickerRef.current.contains(event.target)) {
        onClose();
      }
    };

    if (isVisible) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isVisible, onClose]);

  if (!isVisible) return null;

  const handleEmojiClick = (emoji) => {
    onEmojiSelect(emoji);
    onClose();
  };

  return (
    <div 
      ref={pickerRef}
      style={{
        position: 'absolute',
        bottom: '100%',
        left: '0',
        marginBottom: '8px',
        width: '320px',
        height: '260px',
        backgroundColor: '#2a2a2a',
        border: '1px solid #666',
        borderRadius: '6px',
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.3)',
        zIndex: 50
      }}
    >
      {/* 表情网格 */}
      <div style={{ padding: '12px', height: '200px', overflowY: 'auto' }}>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(8, 1fr)', 
          gap: '4px' 
        }}>
          {emojiCategories[activeCategory].emojis.map((emoji, index) => (
            <button
              key={index}
              onClick={() => handleEmojiClick(emoji)}
              style={{
                width: '32px',
                height: '32px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '18px',
                backgroundColor: 'transparent',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                outline: 'none'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#444'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
              title={emoji}
            >
              {emoji}
            </button>
          ))}
        </div>
      </div>

      {/* 底部分类标签 */}
      <div style={{
        position: 'absolute',
        bottom: '0',
        left: '0',
        right: '0',
        height: '40px',
        backgroundColor: '#333',
        borderTop: '1px solid #555',
        borderBottomLeftRadius: '6px',
        borderBottomRightRadius: '6px',
        display: 'flex'
      }}>
        {categories.map((category) => (
          <button
            key={category}
            onClick={() => setActiveCategory(category)}
            style={{
              flex: 1,
              height: '100%',
              backgroundColor: activeCategory === category ? '#1ABC9C' : 'transparent',
              border: 'none',
              color: activeCategory === category ? '#ffffff' : '#cccccc',
              fontSize: '12px',
              cursor: 'pointer',
              outline: 'none'
            }}
            className="hover:opacity-80"
          >
            {emojiCategories[category].name}
          </button>
        ))}
      </div>
    </div>
  );
};

export default EmojiPicker; 