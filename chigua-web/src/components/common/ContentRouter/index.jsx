import React, { Suspense, lazy, useState, useEffect, useRef, useCallback } from 'react';
import { Routes, Route, useLocation, useParams, useNavigationType, useNavigate } from 'react-router-dom';
import VideoList from '../../VideoList';
import categoryService from '../../../services/categoryService';
import apiCacheService from '../../../services/apiCacheService';
import { usePageConfig } from '../../../hooks/usePageConfig';
import './ContentRouter.css';

// 路由级 lazy 分包：按访问需求下载，减小首屏 main.js
const ShortMasonryGrid     = lazy(() => import(/* webpackChunkName: "p-short-pc"   */ '../../ShortVideo/MasonryGrid'));
const ShortVideoMobileFeed = lazy(() => import(/* webpackChunkName: "p-short-m"    */ '../../ShortVideo/ShortVideoMobileFeed'));
const CollectionList       = lazy(() => import(/* webpackChunkName: "p-collection" */ '../../CollectionList'));
const PagedCategoryList    = lazy(() => import(/* webpackChunkName: "p-paged"      */ '../../PagedCategoryList'));
const TelegramCategoryList = lazy(() => import(/* webpackChunkName: "p-tg-list"    */ '../../TelegramCategoryList'));
const UserList             = lazy(() => import(/* webpackChunkName: "p-userlist"   */ '../../UserList'));
const VideoDetail          = lazy(() => import(/* webpackChunkName: "p-video"      */ '../../../pages/VideoDetail'));
const CollectionDetail     = lazy(() => import(/* webpackChunkName: "p-coll-det"   */ '../../../pages/CollectionDetail'));
const UserDetail           = lazy(() => import(/* webpackChunkName: "p-user"       */ '../../../pages/UserDetail'));
const UserVideoPlayer      = lazy(() => import(/* webpackChunkName: "p-user-vp"    */ '../../../pages/UserVideoPlayer'));
const Submission           = lazy(() => import(/* webpackChunkName: "p-submit"     */ '../../../pages/Submission'));
const HomeWay              = lazy(() => import(/* webpackChunkName: "p-homeway"    */ '../../../pages/HomeWay'));
const SearchResults        = lazy(() => import(/* webpackChunkName: "p-search"     */ '../../../pages/Search'));
const Archives             = lazy(() => import(/* webpackChunkName: "p-archives"   */ '../../../pages/Archives'));
const Tags                 = lazy(() => import(/* webpackChunkName: "p-tags"       */ '../../../pages/Tags'));
const TagDetail            = lazy(() => import(/* webpackChunkName: "p-tag-det"    */ '../../../pages/TagDetail'));
const SeoKeywordDetail     = lazy(() => import(/* webpackChunkName: "p-seo-kw"     */ '../../../pages/SeoKeywordDetail'));
const TgPostDetail         = lazy(() => import(/* webpackChunkName: "p-tg-post"    */ '../../../pages/TgPostDetail'));
const TikTokLoadingTest    = lazy(() => import(/* webpackChunkName: "p-tt-test"    */ '../../../pages/TikTokLoadingTest'));

const RouteFallback = () => (
  <div style={{ minHeight: '50vh' }} aria-hidden="true" />
);

// 首页组件
function HomePage() {
  const { getConfig } = usePageConfig();
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);

    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  useEffect(() => {
    // 首页：仅当路由 state.forceTop 为 true 且不是 POP 导航时置顶
    const shouldForceTop = window.history.state && window.history.state.usr && window.history.state.usr.forceTop === true;
    // 无法在此准确获取 navigationType，这里不主动置顶，交给子页面处理
    if (shouldForceTop) {
      // 由首页子组件 VideoList 的逻辑处理置顶
    }
  }, []);

  // SEO优化：首页确保"天涯"关键词突出显示
  useEffect(() => {
    document.title = '福利导航_91福利导航_老司机福利导航_136福利导航_深夜福利导航_精品福利导航_免费福利视频导航大全';

    const updateMetaTag = (name, content, attribute = 'name') => {
      if (!content) return;
      let tag = document.querySelector(`meta[${attribute}="${name}"]`);
      if (!tag) {
        tag = document.createElement('meta');
        tag.setAttribute(attribute, name);
        document.head.appendChild(tag);
      }
      tag.setAttribute('content', content);
    };

    updateMetaTag('description', '福利导航大全，汇聚91福利导航、136福利导航、老司机福利导航、深夜福利导航等精品导航站，免费获取海量福利视频资源，实时更新，福利导航在线一站直达。');
    updateMetaTag('keywords', '福利导航,136福利导航,第一福利导航,91福利导航,福利导航大全,老司机福利导航,爱你啪福利导航,久久福利导航,深夜福利导航,福利导航在线,精品福利导航,500福利导航,夜趣福利导航,蓝色福利导航,欧美福利导航');

    updateMetaTag('og:title', '福利导航_91福利导航_老司机福利导航_136福利导航_深夜福利导航_精品福利导航_免费福利视频导航大全', 'property');
    updateMetaTag('og:description', '福利导航大全，汇聚91福利导航、136福利导航、老司机福利导航、深夜福利导航等精品导航站，免费获取海量福利视频资源，实时更新，福利导航在线一站直达。', 'property');
    updateMetaTag('og:site_name', '福利导航', 'property');

    updateMetaTag('twitter:title', '福利导航_91福利导航_老司机福利导航_136福利导航_深夜福利导航_精品福利导航_免费福利视频导航大全');
    updateMetaTag('twitter:description', '福利导航大全，汇聚91福利导航、136福利导航、老司机福利导航、深夜福利导航等精品导航站，免费获取海量福利视频资源，实时更新，福利导航在线一站直达。');

    return () => {
      document.title = '福利导航_91福利导航_老司机福利导航_136福利导航_深夜福利导航_精品福利导航_免费福利视频导航大全';
    };
  }, []);

  return (
    <div>
      {/* 响应式间距样式 */}
      <style>{`
        .category-title-section {
          margin-bottom: 12px;  /* 移动端12px，与顶部间距一致 */
        }
        @media (min-width: 768px) {
          .category-title-section {
            margin-bottom: 48px;  /* 桌面端48px，与列表项间距一致 */
          }
        }
      `}</style>

      {/* 首页标题 */}
      <div
        className="flex justify-center category-title-section mt-3 md:mt-12"
        style={{
          backgroundColor: 'rgba(49, 48, 48, 0.9)',
          height: '130px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          zIndex: 10
        }}
      >
        <div className="w-full text-center" style={{ maxWidth: '770px' }}>
          <h1
            className="font-normal px-1 md:px-4"
            style={{
              fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
              fontStyle: 'normal',
              fontWeight: '300',
              color: 'rgb(255, 255, 255)',
              fontSize: isMobile ? '32px' : '45px',
              lineHeight: isMobile ? '48px' : '68px',
              textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
              wordBreak: 'break-word',
              overflowWrap: 'break-word',
              hyphens: 'auto'
            }}
          >
            {getConfig('site_name') || '天涯吃瓜'}
          </h1>
          {/* 首页副标题：取项目名称的备注 */}
          <HomeSubtitle />
        </div>
      </div>

      {/* 内容区域 */}
      <VideoList />
    </div>
  );
}

// 单独组件以避免循环依赖，内部直接使用usePageConfig读取remark
function HomeSubtitle() {
  const [subtitle, setSubtitle] = React.useState('');
  const { getRemark } = usePageConfig();
  React.useEffect(() => {
    (async () => {
      try {
        const text = await getRemark('site_name');
        setSubtitle(text || '');
      } catch (_) { }
    })();
  }, [getRemark]);
  if (!subtitle) return null;
  return (
    <p
      className="px-6"
      style={{
        fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
        fontStyle: 'normal',
        fontWeight: '400',
        color: 'rgb(255, 255, 255)',
        fontSize: '17px',
        lineHeight: '19px',
        textShadow: '1px 1px 2px rgba(0,0,0,0.6)',
        maxWidth: '600px',
        margin: '0 auto',
        marginBottom: '20px'
      }}
    >
      {subtitle}
    </p>
  );
}

// 分类页面组件
function CategoryPage() {
  const { categoryId } = useParams();
  const location = useLocation();
  const navigationType = useNavigationType();
  const navigate = useNavigate();
  const [category, setCategory] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isMobile, setIsMobile] = useState(false);

  const DETAIL_CACHE_TTL = 10 * 60 * 1000;
  const getDetailCacheKey = useCallback((id) => `category_detail_${id}`, []);

  const readCachedCategoryDetail = useCallback((id) => {
    if (!id) return null;
    try {
      const raw = sessionStorage.getItem(getDetailCacheKey(id));
      if (!raw) return null;
      const parsed = JSON.parse(raw);
      if (!parsed || !parsed.data) return null;
      if (parsed.timestamp && Date.now() - parsed.timestamp > DETAIL_CACHE_TTL) {
        sessionStorage.removeItem(getDetailCacheKey(id));
        return null;
      }
      return parsed.data;
    } catch (_) {
      return null;
    }
  }, [getDetailCacheKey]);

  const writeCachedCategoryDetail = useCallback((id, data) => {
    if (!id || !data) return;
    try {
      sessionStorage.setItem(
        getDetailCacheKey(id),
        JSON.stringify({ data, timestamp: Date.now() })
      );
    } catch (_) { }
  }, [getDetailCacheKey]);

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);

    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  useEffect(() => {
    // 仅在显式动作（带有forceTop且非POP后退）时置顶
    // 从搜索页跳入分类页时允许置顶；但从详情页 POP 返回分类页不置顶
    const shouldForceTop = !!(location && location.state && location.state.forceTop) && navigationType !== 'POP';
    if (shouldForceTop) {
      window.scrollTo(0, 0);
      // 清理state，避免后续POP时再次触发
      try { navigate(`${location.pathname}${location.search}${location.hash}`, { replace: true, state: {} }); } catch (_) { }
    }

    let cancelled = false;

    if (!categoryId) {
      setCategory(null);
      setLoading(false);
      return;
    }

    const numericIdRaw = parseInt(categoryId, 10);
    const numericId = Number.isNaN(numericIdRaw) ? null : numericIdRaw;

    if (!numericId) {
      setCategory(null);
      setLoading(false);
      return;
    }

    const cachedDetail = readCachedCategoryDetail(numericId);

    // POP 返回时优先使用缓存，避免网络请求
    if (navigationType === 'POP' && cachedDetail) {
      setCategory(cachedDetail);
      setLoading(false);
      return;
    }

    // 若有缓存，先显示缓存内容再异步刷新
    if (cachedDetail) {
      setCategory(cachedDetail);
      setLoading(false);
    } else {
      setLoading(true);
    }

    const fetchCategory = async () => {
      try {
        const response = await apiCacheService.getApiData(
          'CATEGORY_DETAIL',
          { categoryId: numericId },
          (params) => categoryService.getCategoryById(params.categoryId),
          { cacheDuration: 10 * 60 * 1000 }
        );

        if (cancelled) return;

        if (response.code === 200) {
          writeCachedCategoryDetail(numericId, response.data);
          setCategory(response.data);
        } else if (!cachedDetail) {
          setCategory(null);
        }
      } catch (_) {
        if (!cachedDetail) {
          setCategory(null);
        }
      } finally {
        if (!cancelled && !cachedDetail) {
          setLoading(false);
        }
      }
    };

    // POP 场景且已有缓存时已返回，无需刷新
    if (navigationType !== 'POP' || !cachedDetail) {
      fetchCategory();
    }

    return () => {
      cancelled = true;
    };
  }, [categoryId, navigationType, readCachedCategoryDetail, writeCachedCategoryDetail]);

  useEffect(() => {
    const isShortVideo = Boolean(category && Number(category.isShort) === 1 && isMobile);
    window.dispatchEvent(new CustomEvent('categoryView:update', { detail: { isMobileShort: isShortVideo } }));
    return () => {
      window.dispatchEvent(new CustomEvent('categoryView:update', { detail: { isMobileShort: false } }));
    };
  }, [category?.id, category?.isShort, isMobile]);

  // 首屏：在未拿到分类详情前不挂载任何列表组件，避免先请求普通列表
  if (loading || !category) {
    return (
      <div>
        {/* 占位容器：等分类详情返回后再根据 isShort 决定渲染哪种列表 */}
      </div>
    );
  }

  // 根据分类类型决定显示什么内容
  // 优先级：Telegram > 用户组 > 分页模式 > 合集 > 短视频 > 普通视频
  let content;
  if (category && category.isTelegram === 1) {
    // Telegram 模式：每行 3 卡片，媒体马赛克布局
    content = <TelegramCategoryList categoryId={categoryId ? parseInt(categoryId) : null} category={category} />;
  } else if (category && category.isUserGroup === 1) {
    // 用户组模式：显示用户列表
    content = <UserList categoryId={categoryId ? parseInt(categoryId) : null} />;
  } else if (category && category.isPagination === 1) {
    // ✅ 分页模式：PC端和移动端都使用分页组件（移动端双列布局）
    content = <PagedCategoryList categoryId={categoryId ? parseInt(categoryId) : null} category={category} />;
  } else if (category && category.isCollection === 1) {
    content = <CollectionList categoryId={categoryId ? parseInt(categoryId) : null} />;
  } else if (category && Number(category.isShort) === 1) {
    // 短视频模式：移动端用新播放器（列表+全屏，参考用户组逻辑），PC 瀑布流
    content = isMobile
      ? <ShortVideoMobileFeed categoryId={categoryId ? parseInt(categoryId) : null} />
      : <ShortMasonryGrid categoryId={categoryId ? parseInt(categoryId) : null} />;
  } else {
    content = <VideoList categoryId={categoryId ? parseInt(categoryId) : null} />;
  }

  return (
    <div>
      {/* 响应式间距样式 */}
      <style>{`
        .category-title-section {
          margin-bottom: 12px;  /* 移动端12px，与顶部间距一致 */
        }
        @media (min-width: 768px) {
          .category-title-section {
            margin-bottom: 48px;  /* 桌面端48px，与列表项间距一致 */
          }
        }
      `}</style>

      {/* 分类标题和副标题（移动端短视频、分页模式、Telegram模式隐藏，组件内部自带标题） */}
      {category && !(isMobile && Number(category.isShort) === 1) && Number(category.isPagination) !== 1 && Number(category.isTelegram) !== 1 && (
        <div
          className="flex justify-center category-title-section mt-3 md:mt-12"
          style={{
            backgroundColor: 'rgba(49, 48, 48, 0.9)',
            height: '130px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
            zIndex: 10 // 确保在Header层级之下，但高于其他内容
          }}
        >
          <div className="w-full text-center" style={{ maxWidth: '770px' }}>
            <h1
              className="font-normal px-1 md:px-4"
              style={{
                fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                fontStyle: 'normal',
                fontWeight: '300',
                color: 'rgb(255, 255, 255)',
                fontSize: isMobile ? '32px' : '45px',
                lineHeight: isMobile ? '48px' : '68px',
                textShadow: '2px 2px 4px rgba(0,0,0,0.8)',
                marginBottom: '8px',
                wordBreak: 'break-word',
                overflowWrap: 'break-word',
                hyphens: 'auto'
              }}
            >
              {category.name}
            </h1>
            {/* 分类描述副标题 - 与视频卡片内文字保持相同的左右间距 */}
            {category.description && (
              <p
                className="px-6"
                style={{
                  fontFamily: '"Mirages Custom", Merriweather, "Open Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft Yahei", "WenQuanYi Micro Hei", "Segoe UI Emoji", "Segoe UI Symbol", Helvetica, Arial, sans-serif',
                  fontStyle: 'normal',
                  fontWeight: '400',
                  color: 'rgb(255, 255, 255)',
                  fontSize: '17px',
                  lineHeight: '19px',
                  textShadow: '1px 1px 2px rgba(0,0,0,0.6)',
                  maxWidth: '600px',
                  margin: '0 auto',
                  marginBottom: '20px'
                }}
              >
                {category.description}
              </p>
            )}
          </div>
        </div>
      )}
      {/* 内容区域 */}
      {content}
    </div>
  );
}

const ContentRouter = () => {
  // 移除动画相关的状态和逻辑，直接渲染内容
  return (
    <div className="content-router">
      <div className="content-container">
        <Suspense fallback={<RouteFallback />}>
        <Routes>
          {/* 首页 - 显示所有视频 */}
          <Route path="/" element={<HomePage />} />
          {/* 分类页面 - 显示特定分类的视频或合集 */}
          <Route path="/category/:categoryId" element={<CategoryPage />} />
          {/* 用户详情页面 */}
          <Route path="/user/:username" element={<UserDetail />} />
          {/* 用户视频播放页（从用户列表直接进入，加载第一个视频） */}
          <Route path="/user/:username/video" element={<UserVideoPlayer />} />
          {/* 用户视频播放页（支持分享直达，加载指定视频） */}
          <Route path="/user/:username/video/:videoId" element={<UserVideoPlayer />} />
          {/* 视频详情页面 */}
          <Route path="/video/:id" element={<VideoDetail />} />
          {/* 合集详情页面 */}
          <Route path="/collection/:id" element={<CollectionDetail />} />
          {/* 投稿求瓜页面 */}
          <Route path="/submission" element={<Submission />} />
          <Route path="/submission/:categoryId" element={<Submission />} />
          {/* 回家的路页面 */}
          <Route path="/homeway" element={<HomeWay />} />
          <Route path="/homeway/:categoryId" element={<HomeWay />} />
          {/* 搜索结果页面 */}
          <Route path="/search" element={<SearchResults />} />
          {/* 往期内容页面 */}
          <Route path="/archives" element={<Archives />} />
          {/* 标签页面 */}
          <Route path="/tags" element={<Tags />} />
          {/* 标签详情页面 */}
          <Route path="/tag/:tagId" element={<TagDetail />} />
          {/* SEO关键词详情页面 */}
          <Route path="/keyword/:id" element={<SeoKeywordDetail />} />
          {/* Telegram 帖子详情页（SEO 落地页） */}
          <Route path="/tg/post/:id" element={<TgPostDetail />} />
          {/* TikTok加载图标测试页面 */}
          <Route path="/test/tiktok-loading" element={<TikTokLoadingTest />} />
        </Routes>
        </Suspense>
      </div>
    </div>
  );
};

export default ContentRouter;
