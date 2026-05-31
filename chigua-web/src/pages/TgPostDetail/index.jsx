import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { request } from '../../services/api';
import videoStatsService from '../../services/videoStatsService';
import TelegramCard from '../../components/TelegramCategoryList/TelegramCard';
import Footer from '../../components/common/Footer';
import '../../components/TelegramCategoryList/index.css';

/**
 * Telegram 帖子详情页（SEO 落地页）
 *
 * 路由：/tg/post/:id
 * SEO 要点：
 *   1. canonical 固定锁到主域 tycg7.com（避免泛域名重复内容）
 *   2. 失败 / 不存在 → robots noindex
 *   3. title / description / keywords 含 #id 与媒体数量，避免重复
 *   4. BreadcrumbList + Article + VideoObject 结构化数据
 *   5. 可见面包屑 + h2 副标题 + 静态 SEO 文案，丰富可索引文本量
 */

const SITE_NAME       = '每日吃瓜';
const CANONICAL_HOST  = 'https://tycg7.com';
const DEFAULT_KEYWORDS = '每日吃瓜,吃瓜网,网红黑料,明星黑料,主播黑料,娱乐黑料,热点爆料,独家爆料,网曝吃瓜';

const truncate = (text, max) => {
  if (!text) return '';
  const t = String(text).replace(/\s+/g, ' ').trim();
  return t.length > max ? t.slice(0, max - 1) + '…' : t;
};

// 把 caption 中的中文短语提取出来当 keywords，避免和默认通用词重复
const extractKeywordsFromCaption = (caption) => {
  if (!caption) return [];
  const cleaned = String(caption)
    .replace(/[\s\u00a0]+/g, ' ')
    .replace(/[#@“”"'，。！？!?,.:;、（）()【】\[\]{}<>《》|/\\~`*\-_]+/g, ' ')
    .trim();
  if (!cleaned) return [];
  return cleaned
    .split(' ')
    .map((s) => s.trim())
    .filter((s) => s && s.length >= 2 && s.length <= 16)
    .slice(0, 6);
};

const buildMediaSummary = (post) => {
  const photoCount = Number(post?.photoCount) || 0;
  const videoCount = Number(post?.videoCount) || 0;
  const parts = [];
  if (photoCount > 0) parts.push(`${photoCount}张高清图`);
  if (videoCount > 0) parts.push(`${videoCount}个视频`);
  return parts.join(' + ');
};

const formatDate = (raw) => {
  if (!raw) return '';
  // raw 形如 "2026-03-18 19:33:25" → 取日期部分
  return String(raw).slice(0, 10);
};

/**
 * 设置 / 更新 / 删除 head 中的 meta + link + ld+json，
 * 用 data 属性打标，方便组件卸载时统一清理。
 */
function applyHeadTags(tags) {
  const created = [];

  const upsert = (selector, builder, attrs) => {
    let tag = document.querySelector(selector);
    if (!tag) {
      tag = builder();
      tag.setAttribute('data-tg-seo', '1');
      document.head.appendChild(tag);
      created.push(tag);
    }
    Object.entries(attrs).forEach(([k, v]) => {
      if (v == null || v === '') return;
      tag.setAttribute(k, v);
    });
    return tag;
  };

  // meta
  (tags.meta || []).forEach(({ name, property, content }) => {
    if (!content) return;
    if (property) {
      upsert(`meta[property="${property}"]`, () => document.createElement('meta'), {
        property,
        content,
      });
    } else if (name) {
      upsert(`meta[name="${name}"]`, () => document.createElement('meta'), {
        name,
        content,
      });
    }
  });

  // link
  (tags.link || []).forEach(({ rel, href }) => {
    if (!rel || !href) return;
    upsert(`link[rel="${rel}"]`, () => {
      const t = document.createElement('link');
      t.setAttribute('rel', rel);
      return t;
    }, { href });
  });

  // ld+json
  (tags.jsonLd || []).forEach(({ key, data }) => {
    let tag = document.querySelector(`script[data-tg-jsonld="${key}"]`);
    if (!tag) {
      tag = document.createElement('script');
      tag.setAttribute('type', 'application/ld+json');
      tag.setAttribute('data-tg-jsonld', key);
      tag.setAttribute('data-tg-seo', '1');
      document.head.appendChild(tag);
    }
    tag.textContent = JSON.stringify(data);
  });
}

function cleanupSeoTags() {
  document
    .querySelectorAll('[data-tg-seo="1"]')
    .forEach((el) => el.remove());
}

const TgPostDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [post, setPost]       = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  useEffect(() => {
    let cancelled = false;
    window.scrollTo(0, 0);
    setLoading(true);
    setError(null);

    try { videoStatsService.trackTgPostView(id); } catch (_) {}

    request(`/web/api/tg/posts/${id}`)
      .then((res) => {
        if (cancelled) return;
        if (res && res.code === 200 && res.data) {
          setPost(res.data);
        } else {
          setPost(null);
          setError(res?.msg || '内容不存在或已删除');
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPost(null);
          setError('加载失败，请稍后重试');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, [id]);

  // ── SEO head：成功加载 ──
  useEffect(() => {
    if (!post) return;

    const canonicalUrl = `${CANONICAL_HOST}/tg/post/${id}`;
    const rawCaption   = (post.caption || '').trim();
    const captionShort = truncate(rawCaption, 60);
    const mediaSummary = buildMediaSummary(post);
    const dateStr      = formatDate(post.postDate);

    // title：尽量含关键词 + 媒体类型 + 品牌
    let title;
    if (rawCaption) {
      title = mediaSummary
        ? `${captionShort}｜${mediaSummary} - ${SITE_NAME}`
        : `${captionShort} - ${SITE_NAME}`;
    } else {
      title = `${SITE_NAME} 爆料 #${id}${mediaSummary ? `｜${mediaSummary}` : ''}`;
    }

    // description：有 caption 用 caption；否则给一段含 id 的兜底文案
    const descCore = rawCaption
      ? truncate(rawCaption, 140)
      : `${SITE_NAME}独家爆料 #${id}${mediaSummary ? `，包含${mediaSummary}` : ''}${dateStr ? `，发布于 ${dateStr}` : ''}。`;
    const description = `${descCore}${rawCaption ? `（${SITE_NAME}每日更新）` : ` 每日吃瓜，最新最全网红、明星、主播、社会热点黑料与独家爆料一站直达。`}`;

    // keywords：默认词 + caption 关键短语
    const capKws = extractKeywordsFromCaption(rawCaption);
    const keywords = Array.from(new Set([...capKws, ...DEFAULT_KEYWORDS.split(',')]))
      .slice(0, 18)
      .join(',');

    // 媒体相关
    const photos = (post.media || []).filter((m) => m.mediaType === 'photo');
    const videos = (post.media || []).filter((m) => m.mediaType === 'video');
    const firstImage = photos[0];
    const firstVideo = videos[0];
    const coverUrl   = firstImage?.localUrl
      || firstVideo?.firstFrameUrl
      || firstVideo?.thumbUrl
      || '';

    const prevTitle = document.title;
    document.title  = title;

    const isoPublish = post.postDate ? `${post.postDate.replace(' ', 'T')}+08:00` : '';

    applyHeadTags({
      meta: [
        { name: 'description', content: description },
        { name: 'keywords', content: keywords },
        { name: 'robots', content: 'index, follow, max-image-preview:large' },
        { property: 'og:type',        content: 'article' },
        { property: 'og:title',       content: title },
        { property: 'og:description', content: description },
        { property: 'og:url',         content: canonicalUrl },
        { property: 'og:site_name',   content: SITE_NAME },
        { property: 'og:locale',      content: 'zh_CN' },
        ...(coverUrl ? [
          { property: 'og:image',     content: coverUrl },
          { property: 'og:image:alt', content: captionShort || `${SITE_NAME} #${id}` },
        ] : []),
        ...(isoPublish ? [
          { property: 'article:published_time', content: isoPublish },
          { property: 'article:modified_time',  content: isoPublish },
        ] : []),
        { property: 'article:author',  content: SITE_NAME },
        { property: 'article:section', content: '热点爆料' },
        { name: 'twitter:card',        content: coverUrl ? 'summary_large_image' : 'summary' },
        { name: 'twitter:title',       content: title },
        { name: 'twitter:description', content: description },
        ...(coverUrl ? [{ name: 'twitter:image', content: coverUrl }] : []),
      ],
      link: [
        { rel: 'canonical', href: canonicalUrl },
      ],
      jsonLd: [
        {
          key: 'article',
          data: {
            '@context': 'https://schema.org',
            '@type': 'Article',
            headline: truncate(rawCaption || title, 110),
            description,
            url: canonicalUrl,
            mainEntityOfPage: canonicalUrl,
            inLanguage: 'zh-CN',
            datePublished: isoPublish || undefined,
            dateModified:  isoPublish || undefined,
            author: { '@type': 'Organization', name: SITE_NAME, url: CANONICAL_HOST },
            publisher: {
              '@type': 'Organization',
              name: SITE_NAME,
              url: CANONICAL_HOST,
              logo: { '@type': 'ImageObject', url: `${CANONICAL_HOST}/logo.png` },
            },
            ...(coverUrl ? { image: [coverUrl] } : {}),
            ...(Number(post.views) > 0 ? {
              interactionStatistic: {
                '@type': 'InteractionCounter',
                interactionType: { '@type': 'ViewAction' },
                userInteractionCount: Number(post.views),
              },
            } : {}),
          },
        },
        {
          key: 'breadcrumb',
          data: {
            '@context': 'https://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
              { '@type': 'ListItem', position: 1, name: '首页',     item: CANONICAL_HOST + '/' },
              { '@type': 'ListItem', position: 2, name: '热点爆料', item: CANONICAL_HOST + '/' },
              { '@type': 'ListItem', position: 3, name: captionShort || `爆料 #${id}`, item: canonicalUrl },
            ],
          },
        },
        // 视频 JSON-LD（Google 视频搜索富卡片）
        ...(videos.length > 0 ? [{
          key: 'videos',
          data: videos.map((v, idx) => ({
            '@context': 'https://schema.org',
            '@type': 'VideoObject',
            name: `${captionShort || `每日吃瓜 #${id}`} - 视频${videos.length > 1 ? ` ${idx + 1}` : ''}`,
            description,
            uploadDate: isoPublish || undefined,
            thumbnailUrl: v.firstFrameUrl || v.thumbUrl || coverUrl || undefined,
            contentUrl: v.localUrl,
            embedUrl: canonicalUrl,
            ...(v.duration ? { duration: `PT${Math.round(Number(v.duration))}S` } : {}),
            publisher: {
              '@type': 'Organization',
              name: SITE_NAME,
              logo: { '@type': 'ImageObject', url: `${CANONICAL_HOST}/logo.png` },
            },
          })),
        }] : []),
        // 图集 JSON-LD（Google 图片搜索富卡片）
        ...(photos.length > 0 ? [{
          key: 'images',
          data: photos.map((p, idx) => ({
            '@context': 'https://schema.org',
            '@type': 'ImageObject',
            contentUrl: p.localUrl,
            url: p.localUrl,
            name: `${captionShort || `每日吃瓜 #${id}`}${photos.length > 1 ? ` - 图${idx + 1}` : ''}`,
            caption: captionShort || undefined,
            description,
            ...(isoPublish ? { uploadDate: isoPublish } : {}),
            ...(p.width && p.height ? { width: p.width, height: p.height } : {}),
            representativeOfPage: idx === 0 ? true : undefined,
            creditText: SITE_NAME,
            creator: { '@type': 'Organization', name: SITE_NAME, url: CANONICAL_HOST },
          })),
        }] : []),
      ],
    });

    return () => {
      document.title = prevTitle;
      cleanupSeoTags();
    };
  }, [post, id]);

  // ── SEO head：错误 / 不存在 → noindex ──
  useEffect(() => {
    if (loading || post) return;
    if (!error) return;
    const canonicalUrl = `${CANONICAL_HOST}/tg/post/${id}`;
    const prevTitle = document.title;
    document.title = `内容不存在 - ${SITE_NAME}`;
    applyHeadTags({
      meta: [
        { name: 'robots', content: 'noindex, nofollow' },
        { name: 'description', content: '该内容不存在或已被删除，请返回首页查看其他每日吃瓜内容。' },
      ],
      link: [{ rel: 'canonical', href: canonicalUrl }],
      jsonLd: [],
    });
    return () => {
      document.title = prevTitle;
      cleanupSeoTags();
    };
  }, [loading, post, error, id]);

  const captionAlt = useMemo(
    () => truncate((post?.caption || '').trim(), 60) || `${SITE_NAME} #${id}`,
    [post, id]
  );

  const photoCount = Number(post?.photoCount) || 0;
  const videoCount = Number(post?.videoCount) || 0;
  const mediaSummary = buildMediaSummary(post);

  return (
    <div className="min-h-screen" style={{ backgroundColor: '#2C2A2A' }}>
      <div className="pt-28 md:pt-24 pb-8">
        <div className="mx-auto px-4" style={{ maxWidth: 720 }}>
          {/* 可见面包屑：SEO + 用户导航双赢 */}
          <nav
            aria-label="面包屑"
            style={{
              color: '#9ca3af', fontSize: 12, marginBottom: 12,
              display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 4,
            }}
          >
            <Link to="/" style={{ color: '#9ca3af', textDecoration: 'none' }}>首页</Link>
            <span style={{ opacity: 0.6 }}>/</span>
            <Link to="/" style={{ color: '#9ca3af', textDecoration: 'none' }}>热点爆料</Link>
            <span style={{ opacity: 0.6 }}>/</span>
            <span style={{ color: '#cbd5e1' }}>
              {truncate(post?.caption || '', 30) || `爆料 #${id}`}
            </span>
          </nav>

          <button
            onClick={() => navigate(-1)}
            className="text-sm text-gray-300 hover:text-white"
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              background: 'transparent', border: 'none',
              padding: '6px 10px', cursor: 'pointer', marginBottom: 12,
            }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
            返回
          </button>

          {loading && (
            <div className="tg-list-loading" style={{ minHeight: '40vh' }}>
              <div className="tg-list-spinner" />
              <span>加载中...</span>
            </div>
          )}

          {!loading && error && (
            <div className="tg-list-empty" style={{ color: '#e74c3c' }}>{error}</div>
          )}

          {!loading && !error && post && (
            <article itemScope itemType="https://schema.org/Article">
              <header style={{ marginBottom: 16 }}>
                <h1 itemProp="headline" style={{
                  color: '#fff',
                  fontSize: 'clamp(18px, 3.4vw, 26px)',
                  fontWeight: 500,
                  lineHeight: 1.4,
                  margin: 0,
                  wordBreak: 'break-word',
                }}>
                  {truncate(post.caption || `${SITE_NAME} 爆料 #${id}`, 80)}
                </h1>
                <div style={{
                  color: '#888', fontSize: 12, marginTop: 8,
                  display: 'flex', flexWrap: 'wrap', gap: 12,
                }}>
                  {post.postDate && (
                    <span>
                      <time itemProp="datePublished" dateTime={post.postDate.replace(' ', 'T') + '+08:00'}>
                        发布于 {formatDate(post.postDate)}
                      </time>
                    </span>
                  )}
                  {mediaSummary && <span>{mediaSummary}</span>}
                  <span itemProp="author" itemScope itemType="https://schema.org/Organization">
                    作者：<span itemProp="name">{SITE_NAME}</span>
                  </span>
                </div>
              </header>

              {(photoCount > 0 || videoCount > 0) && (
                <h2 style={{
                  color: '#e5e7eb', fontSize: 14, fontWeight: 500,
                  margin: '16px 0 8px', opacity: 0.85,
                }}>
                  {videoCount > 0 && photoCount > 0 ? '图集与视频' :
                    videoCount > 0 ? `视频内容（${videoCount}）` :
                      `高清图集（${photoCount}）`}
                </h2>
              )}

              <TelegramCard post={post} captionAlt={captionAlt} />

              {/* SEO 友好的正文段落：增加可索引文本量、自然嵌入品牌词 */}
              <section style={{
                marginTop: 24, padding: '16px 18px',
                background: 'rgba(255,255,255,0.04)',
                border: '1px solid rgba(255,255,255,0.06)',
                borderRadius: 10,
                color: '#cbd5e1', fontSize: 13, lineHeight: 1.75,
              }}>
                <h2 style={{
                  color: '#e5e7eb', fontSize: 14, fontWeight: 600,
                  margin: 0, marginBottom: 8,
                }}>
                  关于本条内容
                </h2>
                <p style={{ margin: 0 }}>
                  本条爆料 #{id} 由<strong style={{ color: '#fff' }}>{SITE_NAME}</strong>整理收录
                  {post.postDate ? `，发布于 ${formatDate(post.postDate)}` : ''}
                  {mediaSummary ? `，包含 ${mediaSummary}` : ''}，可在本页直接观看。
                  {SITE_NAME}是国内热门<strong style={{ color: '#fff' }}>吃瓜网</strong>，
                  每天更新网红、明星、主播、娱乐、社会等领域的独家黑料与热点资讯。
                </p>
                <p style={{ margin: '8px 0 0' }}>
                  更多内容：
                  <Link to="/" style={{ color: '#7dd3fc', textDecoration: 'none' }}>{SITE_NAME}首页</Link>
                  <span style={{ margin: '0 6px', opacity: 0.6 }}>·</span>
                  <Link to="/archives" style={{ color: '#7dd3fc', textDecoration: 'none' }}>往期爆料归档</Link>
                  <span style={{ margin: '0 6px', opacity: 0.6 }}>·</span>
                  <Link to="/tags" style={{ color: '#7dd3fc', textDecoration: 'none' }}>热门话题标签</Link>
                </p>
              </section>

              <nav style={{
                marginTop: 24, fontSize: 13, color: '#aaa',
                display: 'flex', gap: 16, flexWrap: 'wrap',
              }}>
                <Link to="/" style={{ color: '#7dd3fc', textDecoration: 'none' }}>
                  ← 返回{SITE_NAME}首页
                </Link>
                <Link to="/archives" style={{ color: '#7dd3fc', textDecoration: 'none' }}>
                  浏览往期爆料归档 →
                </Link>
                <Link to="/tags" style={{ color: '#7dd3fc', textDecoration: 'none' }}>
                  热门话题 →
                </Link>
              </nav>
            </article>
          )}
        </div>
      </div>

      <Footer />
    </div>
  );
};

export default TgPostDetail;
