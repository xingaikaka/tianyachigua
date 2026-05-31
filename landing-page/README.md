# 天涯吃瓜网 - 落地页

基于React + TypeScript + Tailwind CSS构建的现代化落地页。

## 🎨 设计特色

- **主题色**: #7AD8F1 (青色)
- **响应式设计**: 支持桌面端和移动端
- **现代化UI**: 使用Tailwind CSS构建
- **动画效果**: 平滑的悬停和过渡动画
- **背景装饰**: 渐变光晕效果

## 📱 功能特性

- 🔗 **多线路导航**: 三个主要访问线路
- 📱 **APP下载**: 移动应用下载入口
- 🧭 **福利导航**: 资源导航页面
- 📧 **邮箱订阅**: 自动获取最新信息
- 🌐 **社交媒体**: 多平台社交链接
- 📱 **响应式**: 完美适配各种设备

## 🚀 快速开始

### 开发环境

```bash
npm start
```

在 [http://localhost:3000](http://localhost:3000) 查看页面

### 生产构建

```bash
npm run build
```

构建文件将生成在 `build` 文件夹中。

## 🛠 技术栈

- **React 18** - 用户界面库
- **TypeScript** - 类型安全
- **Tailwind CSS** - 实用优先的CSS框架
- **PostCSS** - CSS后处理器

## 📁 项目结构

```
landing-page/
├── public/          # 静态资源
├── src/
│   ├── App.tsx      # 主应用组件
│   ├── App.css      # 自定义样式
│   ├── index.tsx    # 应用入口
│   └── index.css    # Tailwind CSS导入
├── tailwind.config.js  # Tailwind配置
├── postcss.config.js   # PostCSS配置
└── package.json
```

## 🎯 部署

项目构建后可以部署到任何静态文件服务器：

- Nginx
- Apache
- Cloudflare Pages
- Vercel
- Netlify

## 📝 自定义

### 修改主题色

在 `tailwind.config.js` 中修改 `primary` 颜色：

```javascript
colors: {
  primary: '#7AD8F1',  // 修改这里
  // ...
}
```

### 修改链接地址

在 `src/App.tsx` 中修改相应的链接地址。

## 📄 许可证

MIT License