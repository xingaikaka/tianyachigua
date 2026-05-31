/**
 * 路由配置管理
 */

// 应用路由配置
export const ROUTES = {
  // 主页面
  home: '/',
  
  // 视频相关
  video: {
    detail: (id) => `/video/${id}`,
    category: (categoryId) => `/category/${categoryId}`,
    tag: (tagName) => `/tag/${tagName}`,
    search: '/search'
  },
  
  // 合集相关
  collection: {
    list: '/collections',
    detail: (id) => `/collection/${id}`
  },
  
  // 用户相关
  user: {
    profile: '/profile',
    favorites: '/favorites',
    history: '/history'
  },
  
  // 其他页面
  pages: {
    archives: '/archives',
    tags: '/tags',
    submission: '/submission',
    homeWay: '/homeway',
    about: '/about',
    privacy: '/privacy',
    terms: '/terms'
  },
  
  // 错误页面
  error: {
    notFound: '/404',
    serverError: '/500'
  }
};

// 导航菜单配置
export const NAVIGATION = {
  main: [
    { name: '首页', path: ROUTES.home, icon: 'home' },
    { name: '分类', path: ROUTES.video.category('all'), icon: 'category' },
    { name: '合集', path: ROUTES.collection.list, icon: 'collection' },
    { name: '标签', path: ROUTES.pages.tags, icon: 'tags' },
    { name: '归档', path: ROUTES.pages.archives, icon: 'archive' }
  ],
  
  footer: [
    { name: '投稿求瓜', path: ROUTES.pages.submission },
    { name: '回家的路', path: ROUTES.pages.homeWay },
    { name: '关于我们', path: ROUTES.pages.about },
    { name: '隐私政策', path: ROUTES.pages.privacy },
    { name: '服务条款', path: ROUTES.pages.terms }
  ]
};

// 路由守卫配置
export const ROUTE_GUARDS = {
  // 需要登录的路由
  requireAuth: [
    ROUTES.user.profile,
    ROUTES.user.favorites,
    ROUTES.user.history
  ],
  
  // 访客可访问的路由
  guestOnly: [
    ROUTES.home,
    ROUTES.video.search,
    ROUTES.pages.archives
  ]
};

// 页面标题配置
export const PAGE_TITLES = {
  [ROUTES.home]: '天涯吃瓜 - 首页',
  [ROUTES.video.search]: '搜索 - 天涯吃瓜',
  [ROUTES.collection.list]: '合集 - 天涯吃瓜',
  [ROUTES.pages.archives]: '归档 - 天涯吃瓜',
  [ROUTES.pages.tags]: '标签 - 天涯吃瓜',
  [ROUTES.pages.submission]: '投稿求瓜 - 天涯吃瓜',
  [ROUTES.pages.homeWay]: '回家的路 - 天涯吃瓜'
};

export default {
  routes: ROUTES,
  navigation: NAVIGATION,
  guards: ROUTE_GUARDS,
  titles: PAGE_TITLES
};
