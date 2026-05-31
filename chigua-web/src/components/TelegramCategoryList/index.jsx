import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { request } from '../../services/api';
import Pagination from '../ui/Pagination';
import TelegramCard from './TelegramCard';
import './index.css';

const PAGE_SIZE = 20;

const TelegramCategoryList = ({ categoryId, category }) => {
  const [posts, setPosts]             = useState([]);
  const [total, setTotal]             = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState(null);
  const [keyword, setKeyword]         = useState('');
  const [inputVal, setInputVal]       = useState('');
  const [isPC, setIsPC]               = useState(() => window.innerWidth > 767);

  useEffect(() => {
    const onResize = () => setIsPC(window.innerWidth > 767);
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  const requestIdRef = useRef(0);
  const fetchPageRef = useRef(null);

  const totalPages = useMemo(
    () => (total > 0 ? Math.ceil(total / PAGE_SIZE) : 0),
    [total]
  );

  const fetchPage = useCallback(async (page, caption) => {
    if (!categoryId) return;
    const reqId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    try {
      const params = { categoryId, page, size: PAGE_SIZE };
      if (caption && caption.trim()) params.caption = caption.trim();
      const res = await request('/web/api/tg/posts', { params });
      if (reqId !== requestIdRef.current) return;
      if (res && res.code === 200 && res.data) {
        const data = res.data;
        setPosts(data.list || []);
        setTotal(Number(data.total || 0));
        setCurrentPage(page);
      } else {
        setPosts([]);
        setTotal(0);
        setError('内容加载失败，请刷新重试');
      }
    } catch (err) {
      if (reqId === requestIdRef.current) {
        setError('内容加载失败，请刷新重试');
        setPosts([]);
      }
    } finally {
      if (reqId === requestIdRef.current) setLoading(false);
    }
  }, [categoryId]);

  // 始终保持 ref 指向最新的 fetchPage，避免 effect 依赖它导致重复触发
  useEffect(() => {
    fetchPageRef.current = fetchPage;
  });

  // 分类切换时重置：只依赖 categoryId，不依赖 fetchPage 引用
  // 这样可以防止组件重建时 fetchPage 引用变化导致 effect 重跑，
  // 进而将 requestIdRef 递增、把正在进行的翻页请求响应"顶掉"
  useEffect(() => {
    setPosts([]);
    setTotal(0);
    setCurrentPage(1);
    setKeyword('');
    setInputVal('');
    fetchPageRef.current?.(1, '');
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [categoryId]);

  const handleSearch = useCallback(() => {
    const kw = inputVal.trim();
    setKeyword(kw);
    setCurrentPage(1);
    window.scrollTo(0, 0);
    fetchPage(1, kw);
  }, [inputVal, fetchPage]);

  const handleKeyDown = useCallback((e) => {
    if (e.key === 'Enter') handleSearch();
  }, [handleSearch]);

  const handleClear = useCallback(() => {
    setInputVal('');
    setKeyword('');
    setCurrentPage(1);
    fetchPage(1, '');
  }, [fetchPage]);

  const handlePageChange = useCallback((page) => {
    if (page < 1 || page > totalPages || page === currentPage) return;
    window.scrollTo(0, 0);
    fetchPage(page, keyword);
  }, [currentPage, totalPages, fetchPage, keyword]);

  // ─── 渲染 ───────────────────────────────────────────────

  if (loading && posts.length === 0) {
    return (
      <div className="tg-list-loading mt-3 md:mt-12">
        <div className="tg-list-spinner" />
        <span>内容加载中...</span>
      </div>
    );
  }

  return (
    <div className="tg-list">
      {/* 分类标题 + 搜索区（与 PagedCategoryList 风格一致） */}
      <div className="flex justify-center category-title-section mt-3 md:mt-12" style={{
        backgroundColor: 'rgba(49, 48, 48, 0.9)',
        padding: '24px 16px 20px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '14px',
        position: 'relative',
        zIndex: 10,
      }}>
        {/* 标题 */}
        <h1 style={{
          fontWeight: '300',
          color: '#fff',
          fontSize: 'clamp(22px, 4vw, 36px)',
          textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
          margin: 0,
          textAlign: 'center',
        }}>
          {category?.name || ''}
        </h1>

        {/* 搜索框 */}
        <div style={{ width: '100%', maxWidth: '700px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ position: 'relative', flex: '1 1 auto' }}>
            <input
              type="text"
              placeholder="搜索内容..."
              value={inputVal}
              onChange={e => setInputVal(e.target.value)}
              onKeyDown={handleKeyDown}
              style={{
                width: '100%',
                height: '44px',
                padding: '0 80px 0 20px',
                borderRadius: '22px',
                backgroundColor: 'rgba(0,0,0,0.6)',
                border: '1px solid rgba(255,255,255,0.1)',
                color: '#fff',
                fontSize: '15px',
                outline: 'none',
                backdropFilter: 'blur(10px)',
                boxSizing: 'border-box',
              }}
            />
            {/* 清除按钮 */}
            {inputVal && (
              <button
                onClick={handleClear}
                style={{
                  position: 'absolute', right: 48, top: '50%', transform: 'translateY(-50%)',
                  background: 'none', border: 'none', color: 'rgba(255,255,255,0.5)',
                  cursor: 'pointer', fontSize: 14, padding: '0 6px', lineHeight: 1,
                }}
              >✕</button>
            )}
            {/* 搜索图标按钮 */}
            <button
              onClick={handleSearch}
              style={{
                position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
                background: 'none', border: 'none', color: '#fff',
                cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
                padding: '0 6px',
              }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
            </button>
          </div>
        </div>

        {/* 快捷标签 */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', justifyContent: 'center', maxWidth: '700px', width: '100%' }}>
          {['反差','母狗','高中','初中','绿帽','NTR','推特','3p','萝莉','大奶','抖音'].map(tag => (
            <button
              key={tag}
              onClick={() => {
                const next = keyword === tag ? '' : tag;
                setInputVal(next);
                setKeyword(next);
                setCurrentPage(1);
                window.scrollTo(0, 0);
                fetchPage(1, next);
              }}
              style={{
                padding: '4px 12px',
                fontSize: '13px',
                borderRadius: '20px',
                border: keyword === tag ? '1px solid rgba(0,123,255,0.6)' : 'none',
                backgroundColor: keyword === tag ? 'rgba(0,123,255,0.3)' : 'rgba(0,0,0,0.7)',
                color: '#fff',
                cursor: 'pointer',
                fontWeight: '300',
                whiteSpace: 'nowrap',
                transition: 'all 0.2s ease',
              }}
            >{tag}</button>
          ))}
        </div>

        {/* 搜索结果提示 */}
        {keyword && (
          <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.5)' }}>
            搜索"{keyword}"，共 {total} 条结果
          </div>
        )}
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="tg-list-empty" style={{ color: '#e74c3c' }}>{error}</div>
      )}

      {/* 内容区 */}
      {!error && posts.length === 0 && !loading ? (
        <div className="tg-list-empty">暂无内容</div>
      ) : isPC ? (
        /* ── PC 端：CSS Grid 横向顺序排列，每张卡片自然高度 ── */
        <div className="tg-pc-card-grid">
          {posts.map((post, idx) => (
            <TelegramCard key={post.id || idx} post={post} />
          ))}
        </div>
      ) : (
        /* ── 移动端：单列列表 ── */
        <div className="tg-mobile-list">
          {posts.map((post, idx) => (
            <TelegramCard key={post.id || idx} post={post} />
          ))}
        </div>
      )}

      {/* 翻页中加载提示 */}
      {loading && posts.length > 0 && (
        <div className="tg-list-loading" style={{ minHeight: '80px' }}>
          <div className="tg-list-spinner" style={{ width: 32, height: 32 }} />
        </div>
      )}

      {/* 分页：loading 时禁用所有按钮，防止重复点击导致 requestIdRef 被递增顶掉当前请求 */}
      {totalPages > 1 && (
        <div className="tg-list-pagination" style={{ opacity: loading ? 0.5 : 1, pointerEvents: loading ? 'none' : 'auto', transition: 'opacity 0.2s' }}>
          <Pagination
            current={currentPage}
            total={total}
            pageSize={PAGE_SIZE}
            onChange={handlePageChange}
          />
        </div>
      )}
    </div>
  );
};

export default TelegramCategoryList;
