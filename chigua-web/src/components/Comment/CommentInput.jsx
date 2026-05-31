import React, { useState, useRef, useEffect } from 'react';
import EmojiPicker from './EmojiPicker';

const CommentInput = ({ 
  onSubmit, 
  isLoading = false, 
  placeholder = "写下你的评论...",
  replyTo = null,
  onCancelReply = null,
  position = 'top' // 'top' | 'moved'
}) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    content: ''
  });
  const [errors, setErrors] = useState([]);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submitMessage, setSubmitMessage] = useState('');
  const textareaRef = useRef(null);
  const containerRef = useRef(null);

  // 从本地存储加载用户信息
  useEffect(() => {
    const savedUserInfo = localStorage.getItem('commentUserInfo');
    if (savedUserInfo) {
      try {
        const userInfo = JSON.parse(savedUserInfo);
        setFormData(prev => ({
          ...prev,
          username: userInfo.username || '',
          email: userInfo.email || ''
        }));
      } catch (error) {
        
      }
    }
  }, []);

  // 当移动到回复位置时，滚动到该位置
  useEffect(() => {
    if (position === 'moved' && containerRef.current) {
      setTimeout(() => {
        containerRef.current.scrollIntoView({
          block: 'center'
        });
        // 聚焦到输入框
        if (textareaRef.current) {
          textareaRef.current.focus();
        }
      }, 100);
    }
  }, [position]);

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    
    // 清除相关错误
    if (errors.length > 0) {
      setErrors([]);
    }
  };

  const handleEmojiSelect = (emoji) => {
    const textarea = textareaRef.current;
    if (textarea) {
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const newContent = formData.content.slice(0, start) + emoji + formData.content.slice(end);
      
      setFormData(prev => ({
        ...prev,
        content: newContent
      }));

      // 恢复光标位置
      setTimeout(() => {
        const newPosition = start + emoji.length;
        textarea.setSelectionRange(newPosition, newPosition);
        textarea.focus();
      }, 0);
    }
  };

  const validateForm = () => {
    const errors = [];
    
    if (!formData.username.trim()) {
      errors.push('请输入用户名');
    } else if (formData.username.length > 50) {
      errors.push('用户名不能超过50个字符');
    }

    if (!formData.content.trim()) {
      errors.push('请输入评论内容');
    } else if (formData.content.length > 1000) {
      errors.push('评论内容不能超过1000个字符');
    }

    if (formData.email && formData.email.trim()) {
      const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
      if (!emailRegex.test(formData.email)) {
        errors.push('邮箱格式不正确');
      }
    }

    return errors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const validationErrors = validateForm();
    if (validationErrors.length > 0) {
      setErrors(validationErrors);
      return;
    }

    setErrors([]);

    try {
      // 保存用户信息到本地存储
      localStorage.setItem('commentUserInfo', JSON.stringify({
        username: formData.username,
        email: formData.email
      }));

      // 提交评论
      await onSubmit({
        username: formData.username.trim(),
        email: formData.email.trim(),
        content: formData.content.trim(),
        parentId: replyTo ? replyTo.id : null
      });

      // 显示成功提示
      setSubmitSuccess(true);
      setSubmitMessage('评论提交成功，正在审核中');
      
      // 清空内容
      setFormData(prev => ({
        ...prev,
        content: ''
      }));

      // 如果是回复，取消回复状态
      if (replyTo && onCancelReply) {
        onCancelReply();
      }

      // 3秒后隐藏成功提示
      setTimeout(() => {
        setSubmitSuccess(false);
        setSubmitMessage('');
      }, 3000);

    } catch (error) {

      // 隐藏成功提示，显示错误提示
      setSubmitSuccess(false);
      setSubmitMessage('');
      
      // 判断错误类型并显示相应提示
      const errorMessage = error.message || '提交失败，请稍后重试';
      
      if (errorMessage.includes('评论过于频繁')) {
        setErrors(['评论过于频繁，请5分钟后再试']);
      } else if (errorMessage.includes('IP评论限制') || errorMessage.includes('次数过多')) {
        setErrors(['评论过于频繁，请稍后再试']);
      } else if (errorMessage.includes('网络')) {
        setErrors(['网络连接异常，请检查网络后重试']);
      } else {
        setErrors([errorMessage]);
      }
      
      // 5秒后自动清除错误提示
      setTimeout(() => {
        setErrors([]);
      }, 5000);
    }
  };

  const handleKeyDown = (e) => {
    // Ctrl/Cmd + Enter 提交
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <div ref={containerRef} style={{ marginBottom: '20px' }}>
      {/* 标题和取消回复 */}
      <div className="flex items-center justify-between mb-4">
        <h3 style={{ color: '#1ABC9C', fontSize: '16px', fontWeight: 'normal', margin: 0 }}>
          {replyTo ? `回复 ${replyTo.username}` : '添加新评论'}
        </h3>
        {replyTo && onCancelReply && (
          <button
            type="button"
            onClick={onCancelReply}
            style={{ 
              color: '#1ABC9C', 
              fontSize: '14px',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              padding: '0'
            }}
            className="hover:opacity-80"
          >
            取消回复
          </button>
        )}
      </div>

      {/* 错误提示 */}
      {errors.length > 0 && (
        <div style={{ 
          backgroundColor: '#4a2c2a', 
          border: '1px solid #ff4444', 
          borderRadius: '4px', 
          padding: '12px', 
          marginBottom: '16px' 
        }}>
          <ul style={{ margin: 0, paddingLeft: '16px', color: '#ff6666' }}>
            {errors.map((error, index) => (
              <li key={index} style={{ fontSize: '14px' }}>{error}</li>
            ))}
          </ul>
        </div>
      )}

      {/* 成功提示 */}
      {submitSuccess && (
        <div style={{ 
          backgroundColor: '#1a4d3a', 
          border: '1px solid #1ABC9C', 
          borderRadius: '4px', 
          padding: '12px', 
          marginBottom: '16px',
          display: 'flex',
          alignItems: 'center'
        }}>
          <svg style={{ width: '16px', height: '16px', marginRight: '8px', color: '#1ABC9C' }} fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
          <span style={{ color: '#1ABC9C', fontSize: '14px' }}>{submitMessage}</span>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* 评论内容输入框 */}
        <div className="mb-4">
          <textarea
            ref={textareaRef}
            placeholder="在这里输入你的评论..."
            value={formData.content}
            onChange={(e) => handleInputChange('content', e.target.value)}
            onKeyDown={handleKeyDown}
            style={{
              width: '100%',
              height: '100px',
              backgroundColor: 'transparent',
              border: '1px solid #666',
              borderRadius: '4px',
              padding: '12px',
              color: '#ffffff',
              fontSize: '14px',
              resize: 'none',
              outline: 'none'
            }}
            maxLength={1000}
            required
          />
        </div>

        {/* 昵称输入框 */}
        <div className="mb-4">
          <input
            type="text"
            placeholder="请输入昵称 *"
            value={formData.username}
            onChange={(e) => handleInputChange('username', e.target.value)}
            style={{
              width: '100%',
              backgroundColor: 'transparent',
              border: '1px solid #666',
              borderRadius: '4px',
              padding: '8px 12px',
              color: '#ffffff',
              fontSize: '14px',
              outline: 'none'
            }}
            maxLength={50}
            required
          />
        </div>

        {/* 表情按钮和提交按钮 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            {/* 表情按钮 */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                style={{
                  backgroundColor: 'transparent',
                  border: '1px solid #666',
                  borderRadius: '4px',
                  padding: '6px 12px',
                  color: '#ffffff',
                  fontSize: '14px',
                  cursor: 'pointer',
                  outline: 'none'
                }}
                className="hover:opacity-80"
              >
                OuO
              </button>

              <EmojiPicker
                isVisible={showEmojiPicker}
                onEmojiSelect={handleEmojiSelect}
                onClose={() => setShowEmojiPicker(false)}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading || !formData.username.trim() || !formData.content.trim()}
            style={{
              backgroundColor: '#1ABC9C',
              border: 'none',
              borderRadius: '4px',
              padding: '10px 24px',
              color: '#ffffff',
              fontSize: '14px',
              cursor: 'pointer',
              outline: 'none',
              opacity: (isLoading || !formData.username.trim() || !formData.content.trim()) ? 0.5 : 1
            }}
            className="hover:opacity-80 transition-opacity"
          >
            {isLoading ? '发送中...' : '提交评论'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CommentInput; 