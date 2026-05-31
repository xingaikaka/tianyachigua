import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import userService from '../../services/userService';
import SecureDecryptedImage from '../common/SecureDecryptedImage';
import videoStatsService from '../../services/videoStatsService';

// sessionStorage 缓存：key = 完整 URL，TTL = 5 分钟
const CACHE_TTL = 5 * 60 * 1000;
const getCacheKey = (categoryId, page, keyword) =>
  `userlist:${categoryId}:${page}:${keyword || ''}`;

const readCache = (key) => {
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    const { data, ts } = JSON.parse(raw);
    if (Date.now() - ts > CACHE_TTL) { sessionStorage.removeItem(key); return null; }
    return data;
  } catch { return null; }
};

const writeCache = (key, data) => {
  try { sessionStorage.setItem(key, JSON.stringify({ data, ts: Date.now() })); } catch {}
};

const UserList = ({ categoryId }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0
  });
  const searchTimer = useRef(null);

  const fetchUsers = useCallback(async (pageNum = 1, keyword = '') => {
    // 优先读缓存，命中则跳过请求
    const cacheKey = getCacheKey(categoryId, pageNum, keyword);
    const cached = readCache(cacheKey);
    if (cached) {
      setUsers(cached.rows);
      setPagination(prev => ({ ...prev, current: pageNum, total: cached.total }));
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const response = await userService.getUsers(pageNum, pagination.pageSize, keyword);
      if (response && response.code === 200) {
        const rows = response.rows || [];
        const total = response.total || 0;
        setUsers(rows);
        setPagination(prev => ({ ...prev, current: pageNum, total }));
        writeCache(cacheKey, { rows, total });
      }
    } catch (error) {
      console.error('Failed to fetch users:', error);
    } finally {
      setLoading(false);
    }
  }, [categoryId, pagination.pageSize]);

  useEffect(() => {
    const pageParam = searchParams.get('page');
    const keywordParam = searchParams.get('keyword') || '';
    const initialPage = pageParam ? parseInt(pageParam, 10) : 1;
    setInputValue(keywordParam);
    setSearchKeyword(keywordParam);
    fetchUsers(initialPage, keywordParam);
  }, []);

  const handlePageChange = (pageNum) => {
    const params = { page: pageNum };
    if (searchKeyword) params.keyword = searchKeyword;
    setSearchParams(params);
    fetchUsers(pageNum, searchKeyword);
    window.scrollTo(0, 0);
  };

  const handleSearch = (keyword) => {
    const trimmed = keyword.trim();
    setSearchKeyword(trimmed);
    const params = { page: 1 };
    if (trimmed) params.keyword = trimmed;
    setSearchParams(params);
    // 搜索新关键词时清除当前分类的旧缓存
    try {
      Object.keys(sessionStorage)
        .filter(k => k.startsWith(`userlist:${categoryId}:`))
        .forEach(k => sessionStorage.removeItem(k));
    } catch {}
    fetchUsers(1, trimmed);
  };

  const handleInputChange = (e) => {
    const value = e.target.value;
    setInputValue(value);
    // 防抖搜索：500ms 内无输入则触发
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      handleSearch(value);
    }, 500);
  };

  const handleInputKeyDown = (e) => {
    if (e.key === 'Enter') {
      if (searchTimer.current) clearTimeout(searchTimer.current);
      handleSearch(inputValue);
    }
  };

  const handleClearSearch = () => {
    setInputValue('');
    handleSearch('');
  };

  const handleUserClick = (username) => {
    try { videoStatsService.trackUserClick(username, { categoryId, from: 'userlist_mobile' }); } catch (_) {}
    const params = { from: 'userlist', page: pagination.current };
    if (categoryId) params.categoryId = categoryId;
    if (searchKeyword) params.keyword = searchKeyword;
    navigate(`/user/${username}?${new URLSearchParams(params).toString()}`);
  };

  const handleUserClickPC = (username) => {
    try { videoStatsService.trackUserClick(username, { categoryId, from: 'userlist_pc' }); } catch (_) {}
    // 把当前页面地址存入 state，关闭播放页时精准返回
    navigate(`/user/${username}/video`, {
      state: { from: location.pathname + location.search }
    });
  };

  if (loading && users.length === 0) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-gray-400">加载中...</div>
      </div>
    );
  }

  return (
    <div className="mx-auto px-4 pb-8 max-w-[1200px]">
      {/* 搜索框 */}
      <div className="mb-6">
        <div className="flex items-center gap-2 max-w-md mx-auto">
          <div className="relative flex-1">
            <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              value={inputValue}
              onChange={handleInputChange}
              onKeyDown={handleInputKeyDown}
              placeholder="搜索用户名..."
              className="w-full pl-10 pr-9 py-2.5 bg-[#1f1f1f] border border-[#333] rounded-xl text-white text-sm placeholder-gray-500 focus:outline-none focus:border-[#7c3aed] focus:ring-1 focus:ring-[#7c3aed]/50 transition-all"
            />
            {inputValue && (
              <button
                onClick={handleClearSearch}
                className="absolute inset-y-0 right-3 flex items-center text-gray-400 hover:text-white transition-colors"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>
          <button
            onClick={() => { if (searchTimer.current) clearTimeout(searchTimer.current); handleSearch(inputValue); }}
            className="px-4 py-2.5 bg-[#7c3aed] hover:bg-[#6d28d9] text-white text-sm rounded-xl transition-colors whitespace-nowrap"
          >
            搜索
          </button>
        </div>
        {searchKeyword && (
          <p className="mt-2 text-sm text-gray-400 md:ml-0">
            搜索 "<span className="text-[#a78bfa]">{searchKeyword}</span>" 共 {pagination.total} 个结果
          </p>
        )}
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-4 gap-3 md:gap-4">
        {users.map((user) => (
          <div
            key={user.id}
            className={`bg-[#1f1f1f] rounded-xl overflow-hidden shadow-lg cursor-pointer transform transition-all duration-300 hover:-translate-y-1 border border-[#333] ${
              user.recommended === 1
                ? 'hover:shadow-[0_6px_20px_rgba(230,162,60,0.35)] hover:border-[#E6A23C]'
                : 'hover:shadow-[0_6px_20px_rgba(139,92,246,0.3)] hover:border-[#8b5cf6]'
            }`}
            onClick={() => handleUserClick(user.username)}
          >
            {/* 移动端样式 - 与 PC 端推荐样式一致 */}
            <div className="p-4 flex flex-col items-center text-center md:hidden">
              <div className="relative mb-3">
                <div className={`w-28 h-28 rounded-full overflow-hidden ${
                  user.recommended === 1
                    ? 'border-2 border-[#E6A23C] ring-2 ring-[#E6A23C]/30'
                    : 'border-2 border-[#7c3aed] ring-2 ring-[#7c3aed]/20'
                }`}>
                  <SecureDecryptedImage
                    src={user.profileImageUrl}
                    alt={user.name}
                    className="w-full h-full object-cover"
                    fallbackSrc="/default-avatar.png"
                  />
                </div>
                {user.recommended === 1 && (
                  <div className="absolute bottom-0 right-0 w-7 h-7 rounded-full flex items-center justify-center border-2 border-[#1f1f1f] shadow-lg bg-[#E6A23C]">
                    <span className="text-white text-sm">★</span>
                  </div>
                )}
              </div>
              <h3 className="text-lg font-bold text-white mb-1 truncate max-w-full px-1">
                {user.name || user.username}
              </h3>
              <p className="text-gray-400 text-xs">@{user.username}</p>
            </div>

            {/* PC端样式 - 完整版本，点击直接进入分屏播放页 */}
            <div
              className="hidden md:flex md:flex-col md:items-center md:text-center md:p-4 group/card"
              onClick={(e) => { e.stopPropagation(); handleUserClickPC(user.username); }}
            >
              <div className="relative mb-3">
                <div className={`w-32 h-32 rounded-full overflow-hidden transition-all duration-200 group-hover/card:scale-105 ${
                  user.recommended === 1
                    ? 'border-2 border-[#E6A23C] ring-2 ring-[#E6A23C]/30 group-hover/card:ring-4 group-hover/card:ring-[#E6A23C]/50'
                    : 'border-2 border-[#7c3aed] ring-2 ring-[#7c3aed]/20 group-hover/card:ring-4 group-hover/card:ring-[#7c3aed]/40'
                }`}>
                  <SecureDecryptedImage
                    src={user.profileImageUrl}
                    alt={user.name}
                    className="w-full h-full object-cover"
                    fallbackSrc="/default-avatar.png"
                  />
                </div>
                {user.recommended === 1 && (
                  <div className="absolute bottom-0 right-0 w-7 h-7 rounded-full flex items-center justify-center border-2 border-[#1f1f1f] shadow-lg bg-[#E6A23C]">
                    <span className="text-white text-sm">★</span>
                  </div>
                )}
                {/* hover 播放提示 */}
                <div className="absolute inset-0 rounded-full flex items-center justify-center bg-black/40 opacity-0 group-hover/card:opacity-100 transition-opacity duration-200">
                  <svg className="w-8 h-8 text-white drop-shadow" fill="currentColor" viewBox="0 0 24 24">
                    <polygon points="5 3 19 12 5 21 5 3"/>
                  </svg>
                </div>
              </div>

              <h3 className="text-base font-bold text-white mb-1 truncate w-full px-2">
                {user.name || user.username}
              </h3>
              <p className="text-gray-400 text-xs mb-3 truncate w-full px-2">@{user.username}</p>
              <p className="text-gray-300 text-xs mb-4 min-h-[28px] px-2 line-clamp-2">
                {user.description || '暂无简介'}
              </p>
              <div className="w-full border-t border-gray-700 mb-3"></div>
              <div className="grid grid-cols-3 gap-2 w-full px-1">
                <div className="flex flex-col items-center">
                  <span className="text-lg font-bold text-[#a78bfa] mb-0.5">
                    {formatNumber(user.followers || 0)}
                  </span>
                  <span className="text-[10px] text-gray-500">粉丝</span>
                </div>
                <div className="flex flex-col items-center">
                  <span className="text-lg font-bold text-[#a78bfa] mb-0.5">
                    {formatNumber(user.publishedGifsCount || user.gifsCount || 0)}
                  </span>
                  <span className="text-[10px] text-gray-500">视频</span>
                </div>
                <div className="flex flex-col items-center">
                  <span className="text-lg font-bold text-[#a78bfa] mb-0.5">
                    {formatNumber(user.views || 0)}
                  </span>
                  <span className="text-[10px] text-gray-500">播放</span>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 无结果提示 */}
      {!loading && users.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-gray-500">
          <svg className="w-12 h-12 mb-4 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <p className="text-sm">{searchKeyword ? `未找到与 "${searchKeyword}" 相关的用户` : '暂无用户'}</p>
          {searchKeyword && (
            <button onClick={handleClearSearch} className="mt-3 text-[#7c3aed] text-sm hover:underline">
              清除搜索
            </button>
          )}
        </div>
      )}

      {/* 分页 */}
      {users.length > 0 && (
        <PaginationComponent 
          current={pagination.current}
          total={pagination.total}
          pageSize={pagination.pageSize}
          onChange={handlePageChange}
        />
      )}
    </div>
  );
};

const formatNumber = (num) => {
  if (!num) return '0';
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M';
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K';
  }
  return num.toString();
};

const PaginationComponent = ({ current, total, pageSize, onChange }) => {
  const [jumpPage, setJumpPage] = React.useState('');
  const totalPages = Math.ceil(total / pageSize);
  if (totalPages <= 1) return null;

  const handleJump = () => {
    const page = parseInt(jumpPage, 10);
    if (page >= 1 && page <= totalPages) {
      onChange(page);
      setJumpPage('');
    }
  };

  return (
    <div className="flex flex-col items-center mt-12 mb-8 gap-4">
      <div className="flex flex-wrap justify-center items-center gap-2">
        <button 
          className="px-3 py-1.5 bg-[#333] text-white rounded hover:bg-[#444] disabled:opacity-50 text-sm"
          onClick={() => onChange(1)}
          disabled={current <= 1}
        >
          首页
        </button>
        <button 
          className="px-3 py-1.5 bg-[#333] text-white rounded hover:bg-[#444] disabled:opacity-50 text-sm"
          onClick={() => onChange(current - 1)}
          disabled={current <= 1}
        >
          上一页
        </button>
        <span className="text-gray-300 mx-2 text-sm">{current} / {totalPages}</span>
        <button 
          className="px-3 py-1.5 bg-[#333] text-white rounded hover:bg-[#444] disabled:opacity-50 text-sm"
          onClick={() => onChange(current + 1)}
          disabled={current >= totalPages}
        >
          下一页
        </button>
        <button 
          className="px-3 py-1.5 bg-[#333] text-white rounded hover:bg-[#444] disabled:opacity-50 text-sm"
          onClick={() => onChange(totalPages)}
          disabled={current >= totalPages}
        >
          末页
        </button>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-gray-400 text-sm">跳至</span>
        <input
          type="number"
          min={1}
          max={totalPages}
          value={jumpPage}
          onChange={(e) => setJumpPage(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleJump()}
          placeholder={current.toString()}
          className="w-14 px-2 py-1.5 bg-[#333] border border-[#444] rounded text-white text-sm text-center focus:outline-none focus:border-[#7c3aed] [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
        />
        <span className="text-gray-400 text-sm">页</span>
        <button
          className="px-3 py-1.5 bg-[#7c3aed] hover:bg-[#6d28d9] text-white rounded text-sm"
          onClick={handleJump}
        >
          跳转
        </button>
      </div>
    </div>
  );
};

export default UserList;
