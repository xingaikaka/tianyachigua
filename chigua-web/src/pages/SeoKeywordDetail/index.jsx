import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import seoKeywordService from '../../services/seoKeywordService';
import Pagination from '../../components/ui/Pagination';
import Footer from '../../components/common/Footer';
import SecureDecryptedImage from '../../components/common/SecureDecryptedImage';

/**
 * SEO 关键词详情页（落地页）
 *
 * SEO 要点：
 *   1. canonical / og:url / JSON-LD 统一锁主域 tycg7.com（与 sitemap 域名一致，否则 GSC 会判"备用网页带规范标记"导致不被索引）
 *   2. 视频卡 + 相关关键词 用真实 <Link to>，让 Googlebot 能跟随内链
 *   3. fuzzy / recommended 命中 → robots noindex（避免低质重复页污染索引）
 *   4. 可见面包屑 + BreadcrumbList JSON-LD + H2 + 文案段落，丰富可索引文本
 */

const SITE_NAME      = '每日吃瓜';
const CANONICAL_HOST = 'https://tycg7.com';

const SeoKeywordDetail = () => {
  const { id } = useParams();

  const [loading, setLoading] = useState(true);
  const [keywordInfo, setKeywordInfo] = useState(null);
  const [videos, setVideos] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [matchType, setMatchType] = useState('exact');
  const [relatedKeywords, setRelatedKeywords] = useState([]);
  const pageSize = 20;

  const fetchKeywordVideos = useCallback(async (page = 1) => {
    try {
      setLoading(true);
      const response = await seoKeywordService.getKeywordDetail(id, page, pageSize);

      if (response.code === 200) {
        const data = response.data;
        setKeywordInfo(data.keywordInfo);
        setVideos(data.list || []);
        setTotalCount(data.total || 0);
        setCurrentPage(page);
        setMatchType(data.matchType || 'exact');

        if (page === 1) {
          seoKeywordService.recordSearch(id);
        }
      }
    } catch (error) {
      console.error('获取关键词视频失败', error);
    } finally {
      setLoading(false);
    }
  }, [id]);

  const fetchRelatedKeywords = useCallback(async () => {
    try {
      const response = await seoKeywordService.getRandomKeywords(20);
      if (response.code === 200) {
        setRelatedKeywords(response.data || []);
      }
    } catch (error) {
      console.error('获取相关关键词失败', error);
    }
  }, []);

  // 切换 id 时重新拉取
  useEffect(() => {
    window.scrollTo(0, 0);
    fetchKeywordVideos(1);
    fetchRelatedKeywords();
  }, [fetchKeywordVideos, fetchRelatedKeywords]);

  // ── SEO head ──
  useEffect(() => {
    if (!keywordInfo) return;

    const kw = keywordInfo.keyword;
    const canonicalUrl = `${CANONICAL_HOST}/keyword/${id}`;
    const seoTitle = keywordInfo.seoTitle
      || (matchType === 'exact'
            ? `${kw} - 共${totalCount}个相关视频 | ${SITE_NAME}`
            : `${kw} - 相关推荐 | ${SITE_NAME}`);

    const seoDesc = keywordInfo.seoDescription
      || `${SITE_NAME}为您整理${kw}相关${totalCount}个精选视频，实时更新网红、明星、主播、社会热点黑料与独家爆料，海量短视频、图文一站直达。`;

    const updateMetaTag = (name, content, attribute = 'name') => {
      let tag = document.querySelector(`meta[${attribute}="${name}"]`);
      if (!tag) {
        tag = document.createElement('meta');
        tag.setAttribute(attribute, name);
        document.head.appendChild(tag);
      }
      tag.setAttribute('content', content);
    };

    const updateLinkTag = (rel, href) => {
      let tag = document.querySelector(`link[rel="${rel}"]`);
      if (!tag) {
        tag = document.createElement('link');
        tag.setAttribute('rel', rel);
        document.head.appendChild(tag);
      }
      tag.setAttribute('href', href);
    };

    const prevTitle = document.title;
    document.title = seoTitle;

    updateMetaTag('description', seoDesc);
    updateMetaTag('keywords', `${kw},每日吃瓜,吃瓜网,网红黑料,明星黑料,主播黑料,娱乐黑料,热点爆料,独家爆料,网曝吃瓜`);

    // fuzzy/recommended 命中 → 不索引，避免低质重复页污染（仍 follow 内链）
    updateMetaTag('robots', matchType === 'exact' ? 'index,follow' : 'noindex,follow');

    updateMetaTag('og:title',       seoTitle,      'property');
    updateMetaTag('og:description', seoDesc,       'property');
    updateMetaTag('og:type',        'website',     'property');
    updateMetaTag('og:url',         canonicalUrl,  'property');
    updateMetaTag('og:site_name',   SITE_NAME,     'property');

    updateMetaTag('twitter:card',        'summary');
    updateMetaTag('twitter:title',       seoTitle);
    updateMetaTag('twitter:description', seoDesc);

    updateLinkTag('canonical', canonicalUrl);

    // 结构化数据：BreadcrumbList + ItemList（仅 exact 命中下输出）
    const ldScripts = [];

    const upsertJsonLd = (markerAttr, data) => {
      let tag = document.querySelector(`script[${markerAttr}]`);
      if (!tag) {
        tag = document.createElement('script');
        tag.setAttribute('type', 'application/ld+json');
        const [k, v] = markerAttr.split('=');
        tag.setAttribute(k, (v || 'true').replace(/"/g, ''));
        document.head.appendChild(tag);
      }
      tag.textContent = JSON.stringify(data);
      ldScripts.push(tag);
    };

    upsertJsonLd('data-seo-keyword-breadcrumb="1"', {
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: [
        { '@type': 'ListItem', position: 1, name: '首页',     item: CANONICAL_HOST + '/' },
        { '@type': 'ListItem', position: 2, name: '热门关键词', item: CANONICAL_HOST + '/' },
        { '@type': 'ListItem', position: 3, name: kw,         item: canonicalUrl },
      ],
    });

    if (matchType === 'exact' && videos.length > 0) {
      upsertJsonLd('data-seo-keyword-structured="1"', {
        '@context': 'https://schema.org',
        '@type': 'ItemList',
        name: `${kw}相关视频`,
        description: seoDesc,
        numberOfItems: totalCount,
        itemListElement: videos.slice(0, 10).map((video, index) => ({
          '@type': 'ListItem',
          position: index + 1,
          url: `${CANONICAL_HOST}/video/${video.id}`,
          name: video.title,
        })),
      });
    }

    return () => {
      document.title = prevTitle;
      document.querySelectorAll('script[data-seo-keyword-structured],script[data-seo-keyword-breadcrumb]')
        .forEach((s) => s.remove());
    };
  }, [keywordInfo, videos, totalCount, id, matchType]);

  const handleVideoClick = () => {
    seoKeywordService.recordClick(id);
  };

  const handlePageChange = (page) => {
    fetchKeywordVideos(page);
    window.scrollTo(0, 0);
  };

  const formatDuration = (seconds) => {
    if (!seconds) return '--:--';
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) return '1天前';
    if (diffDays < 7) return `${diffDays}天前`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)}周前`;
    return date.toLocaleDateString('zh-CN');
  };

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      <div className="pt-28 md:pt-24 pb-8">

        {/* 可见面包屑（SEO + 用户导航双赢） */}
        {keywordInfo && (
          <nav
            aria-label="面包屑"
            className="mx-auto mb-4 px-4"
            style={{
              maxWidth: 860, color: '#9ca3af', fontSize: 12,
              display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 4,
            }}
          >
            <Link to="/" style={{ color: '#9ca3af', textDecoration: 'none' }}>首页</Link>
            <span style={{ opacity: 0.6 }}>/</span>
            <Link to="/" style={{ color: '#9ca3af', textDecoration: 'none' }}>热门关键词</Link>
            <span style={{ opacity: 0.6 }}>/</span>
            <span style={{ color: '#cbd5e1' }}>{keywordInfo.keyword}</span>
          </nav>
        )}

        {/* 关键词标题 */}
        {keywordInfo && (
          <div className="text-center mb-12 px-4">
            <h1
              className="text-3xl md:text-5xl font-bold text-white mb-4"
              style={{
                textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
                letterSpacing: '2px',
              }}
            >
              {keywordInfo.keyword}
            </h1>

            <div className="flex justify-center mb-6">
              <div style={{
                width: '80px',
                height: '3px',
                background: 'linear-gradient(90deg, transparent, #4EA394, transparent)',
              }}></div>
            </div>

            {matchType === 'exact' && (
              <div className="text-gray-400 text-lg">
                共找到 <span className="text-white font-medium">{totalCount}</span> 个相关视频
              </div>
            )}

            {matchType === 'fuzzy' && (
              <div className="text-gray-400 text-lg">
                共找到 <span className="text-white font-medium">{totalCount}</span> 个相关视频
                <div className="text-gray-500 text-sm mt-2">
                  （包含"{keywordInfo.keyword}"相关内容）
                </div>
              </div>
            )}

            {matchType === 'recommended' && (
              <div className="text-gray-500 text-sm mb-2">
                暂无"{keywordInfo.keyword}"精确匹配的视频
              </div>
            )}
            {matchType === 'recommended' && (
              <div className="text-gray-400 text-lg">
                为您推荐以下热门内容（{totalCount}个视频）
              </div>
            )}
          </div>
        )}

        {/* 视频列表 */}
        <div className="flex justify-center">
          <div className="w-full px-4" style={{ maxWidth: '860px' }}>
            {loading ? (
              <div className="text-center py-12">
                <div className="flex items-center justify-center mb-4">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-white"></div>
                </div>
                <div className="text-white text-xl">加载中...</div>
              </div>
            ) : videos.length > 0 ? (
              <>
                <div className="space-y-6">
                  {videos.map((video, index) => (
                    <Link
                      key={video.id}
                      to={`/video/${video.id}`}
                      onClick={handleVideoClick}
                      className="group block cursor-pointer transform transition-all duration-300 hover:scale-[1.03] hover:shadow-2xl"
                      style={{ textDecoration: 'none', color: 'inherit' }}
                      title={video.title}
                    >
                      <div
                        className="relative rounded-lg overflow-hidden shadow-lg"
                        style={{
                          height: '280px',
                          backgroundColor: '#1a1a1a',
                        }}
                      >
                        <SecureDecryptedImage
                          src={video.coverUrl || video.coverImage}
                          alt={video.title}
                          className="w-full h-full object-cover"
                          priority={index < 3 ? 'high' : 'normal'}
                          lazyLoad={index >= 3}
                        />

                        <div className="absolute inset-0 bg-black bg-opacity-30"></div>

                        <div className="absolute inset-0 flex flex-col items-center justify-center text-white text-center px-6">
                          <h3
                            className="text-xl md:text-2xl font-medium leading-relaxed mb-4"
                            style={{ textShadow: '2px 2px 4px rgba(0,0,0,0.9)' }}
                          >
                            {video.title}
                          </h3>

                          <div className="flex items-center gap-4 text-sm text-gray-300">
                            {video.viewCount > 0 && (
                              <span>👁️ {video.viewCount >= 10000 ? `${(video.viewCount / 10000).toFixed(1)}万` : video.viewCount}</span>
                            )}
                            {video.duration > 0 && (
                              <span>⏱️ {formatDuration(video.duration)}</span>
                            )}
                            {video.createdAt && (
                              <span>📅 {formatDate(video.createdAt)}</span>
                            )}
                          </div>
                        </div>

                        <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                          <div className="w-16 h-16 rounded-full bg-white bg-opacity-20 flex items-center justify-center">
                            <svg className="w-8 h-8 text-white" fill="currentColor" viewBox="0 0 20 20">
                              <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                            </svg>
                          </div>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>

                {totalCount > pageSize && (
                  <div className="flex justify-center mt-12">
                    <Pagination
                      current={currentPage}
                      total={totalCount}
                      pageSize={pageSize}
                      onChange={handlePageChange}
                    />
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12">
                <div className="text-gray-400 text-lg">
                  暂无视频内容
                </div>
              </div>
            )}
          </div>
        </div>

        {/* SEO 友好文案段落：增加可索引文本 + 自然嵌入品牌词 */}
        {keywordInfo && (
          <section
            className="mx-auto mt-12 px-4"
            style={{ maxWidth: 860 }}
          >
            <div style={{
              padding: '16px 18px',
              background: 'rgba(255,255,255,0.04)',
              border: '1px solid rgba(255,255,255,0.06)',
              borderRadius: 10,
              color: '#cbd5e1', fontSize: 13, lineHeight: 1.75,
            }}>
              <h2 style={{
                color: '#e5e7eb', fontSize: 15, fontWeight: 600,
                margin: 0, marginBottom: 8,
              }}>
                关于"{keywordInfo.keyword}"
              </h2>
              <p style={{ margin: 0 }}>
                <strong style={{ color: '#fff' }}>{SITE_NAME}</strong>
                为您收录与<strong style={{ color: '#fff' }}>{keywordInfo.keyword}</strong>
                相关的{totalCount}个精选内容，实时更新网红、明星、主播、娱乐、社会等领域的独家黑料与热点资讯。
                如需查看更多热门话题，可访问<Link to="/" style={{ color: '#7dd3fc', textDecoration: 'none' }}>{SITE_NAME}首页</Link>。
              </p>
            </div>
          </section>
        )}

        {/* 相关关键词推荐（真实 <Link>，让爬虫能跟随） */}
        {relatedKeywords.length > 0 && (
          <div className="mt-16 mb-8">
            <div className="flex justify-center">
              <div className="w-full px-4" style={{ maxWidth: '860px' }}>
                <h2 className="text-2xl font-bold text-white mb-6 text-center">
                  相关热门关键词
                </h2>

                <div className="flex flex-wrap gap-3 justify-center">
                  {relatedKeywords.map((kw) => (
                    <Link
                      key={kw.id}
                      to={`/keyword/${kw.id}`}
                      className="px-4 py-2 rounded-full text-gray-300 hover:text-white text-sm transition-all duration-200"
                      style={{ backgroundColor: '#343232', textDecoration: 'none' }}
                      title={kw.keyword}
                      onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#4EA394'; }}
                      onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = '#343232'; }}
                    >
                      {kw.keyword}
                    </Link>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

      </div>

      <Footer />
    </div>
  );
};

export default SeoKeywordDetail;
