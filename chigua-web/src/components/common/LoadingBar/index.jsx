import React, { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { useLoadingProgress } from '../../../hooks/useLoadingProgress';
import './LoadingBar.css';

const LoadingBar = () => {
  const location = useLocation();
  const { isLoading } = useLoadingProgress();
  const [progress, setProgress] = useState(0);
  const [isVisible, setIsVisible] = useState(false);
  const progressTimers = useRef([]);

  const clearTimers = () => {
    progressTimers.current.forEach(timer => clearTimeout(timer));
    progressTimers.current = [];
  };

  useEffect(() => {
    // 路由变化时开始加载进度条
    clearTimers();
    setIsVisible(true);
    setProgress(0);

    // 快速进度到30%
    progressTimers.current.push(setTimeout(() => {
      setProgress(30);
    }, 50));

    return clearTimers;
  }, [location.pathname]);

  useEffect(() => {
    if (isLoading) {
      // 有真实加载任务时，进度到70%
      setProgress(70);
    } else if (isVisible) {
      // 加载完成时，快速完成进度条
      clearTimers();
      setProgress(90);
      
      progressTimers.current.push(setTimeout(() => {
        setProgress(100);
      }, 100));

      progressTimers.current.push(setTimeout(() => {
        setIsVisible(false);
        setProgress(0);
      }, 300));
    }
  }, [isLoading, isVisible]);

  if (!isVisible) return null;

  return (
    <div className="loading-bar-container">
      <div 
        className="loading-bar"
        style={{ width: `${progress}%` }}
      />
    </div>
  );
};

export default LoadingBar;
