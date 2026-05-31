import React, { useMemo, useState, useEffect } from 'react';

// 分页组件 - 通用分页样式
const Pagination = ({ current, total, pageSize, onChange }) => {
  const totalPages = Math.ceil(total / pageSize);
  
  // 检测是否为移动端
  const [isMobile, setIsMobile] = useState(false);
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };
    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);
  
  // 计算要显示的页码数组
  const pageNumbers = useMemo(() => {
    const pages = [];
    const maxVisiblePages = isMobile ? 3 : 7; // 移动端最多显示3个，PC端显示7个
    
    if (totalPages <= maxVisiblePages) {
      // 如果总页数少于等于最大显示数，显示所有页码
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // 如果总页数大于最大显示数，使用省略号模式
      if (isMobile) {
        // ✅ 移动端精简模式：只显示 1 ... 当前页 ... 最后一页
        if (current === 1) {
          pages.push(1, 2, 'ellipsis', totalPages);
        } else if (current === totalPages) {
          pages.push(1, 'ellipsis', totalPages - 1, totalPages);
        } else {
          pages.push(1, 'ellipsis', current, 'ellipsis', totalPages);
        }
      } else {
        // PC端：显示更多页码
      if (current <= 4) {
        // 当前页在前4页，显示：1 2 3 4 5 ... 最后一页
        for (let i = 1; i <= 5; i++) {
          pages.push(i);
        }
        pages.push('ellipsis');
        pages.push(totalPages);
      } else if (current >= totalPages - 3) {
        // 当前页在后4页，显示：1 ... 倒数5页
        pages.push(1);
        pages.push('ellipsis');
        for (let i = totalPages - 4; i <= totalPages; i++) {
          pages.push(i);
        }
      } else {
        // 当前页在中间，显示：1 ... 当前页前后各2页 ... 最后一页
        pages.push(1);
        pages.push('ellipsis');
        for (let i = current - 2; i <= current + 2; i++) {
          pages.push(i);
        }
        pages.push('ellipsis');
        pages.push(totalPages);
        }
      }
    }
    
    return pages;
  }, [current, totalPages, isMobile]);
  
  if (totalPages <= 1) {
    return null; // 只有一页或没有数据时不显示分页
  }
  
  return (
    <div className="flex justify-center items-center mt-0 mb-0">
      <div className="flex items-center gap-1 md:gap-2">
        {/* 上一页按钮 */}
        <button 
          className="px-2 md:px-4 h-8 md:h-9 text-xs md:text-sm bg-transparent text-white border border-white rounded-md hover:bg-white/10 transition-colors duration-150 flex items-center justify-center whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={() => {
            if (current > 1) {
              onChange(current - 1);
            }
          }}
          disabled={current <= 1}
        >
          上一页
        </button>
        
        {/* 页码按钮 */}
        {pageNumbers.map((page, index) => {
          if (page === 'ellipsis') {
            return (
              <span key={`ellipsis-${index}`} className="px-1 md:px-2 text-white text-xs md:text-base">
                ...
              </span>
            );
          }
          
          return (
            <button
              key={page}
              className={`min-w-[32px] md:min-w-[36px] h-8 md:h-9 px-2 md:px-3 text-xs md:text-base border rounded-md transition-colors duration-150 flex items-center justify-center ${
                page === current
                  ? 'bg-white text-black border-white font-medium'
                  : 'bg-transparent text-white border-white hover:bg-white/10'
              }`}
              onClick={() => onChange(page)}
            >
              {page}
            </button>
          );
        })}
        
        {/* 下一页按钮 */}
        <button 
          className="px-2 md:px-4 h-8 md:h-9 text-xs md:text-sm bg-transparent text-white border border-white rounded-md hover:bg-white/10 transition-colors duration-150 flex items-center justify-center whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
          onClick={() => {
            if (current < totalPages) {
              onChange(current + 1);
            }
          }}
          disabled={current >= totalPages}
        >
          下一页
        </button>
        
        {/* 页码输入框和跳转按钮 */}
        <div className="flex items-center space-x-2 ml-4">
          <span className="text-white text-sm">跳转至</span>
          <input 
            type="number" 
            min="1"
            max={totalPages}
            placeholder={current.toString()}
            className="w-16 h-9 px-2 text-sm bg-transparent text-white border border-white rounded-md focus:outline-none focus:border-white/90 text-center"
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                const pageNum = parseInt(e.currentTarget.value);
                if (pageNum >= 1 && pageNum <= totalPages) {
                  onChange(pageNum);
                  e.currentTarget.value = '';
                }
              }
            }}
          />
          <span className="text-white text-sm">页</span>
        </div>
      </div>
    </div>
  );
};

export default Pagination;
