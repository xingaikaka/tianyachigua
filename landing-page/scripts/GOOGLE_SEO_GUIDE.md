# Google Search Console Sitemap 使用指南

## 📋 概述

已创建专门用于 Google Search Console 的完整版 sitemap 生成脚本，包含：
- ✅ 主页面
- ✅ 关键词页面（使用现有的15个关键词）
- ✅ 视频页面（从后端 sitemap 服务获取）

## 🚀 快速开始

### 生成 Google Sitemap

```bash
cd landing-page
npm run generate-sitemap:google
```

### 输出文件

- 文件位置：`public/sitemap-google.xml`
- 访问地址：`https://tycg3.com/sitemap-google.xml`

## 📊 Sitemap 内容

### 1. 主页面（1个）
```
https://tycg3.com/
- Priority: 1.0
- Changefreq: daily
```

### 2. 关键词页面（15个）
使用现有的关键词列表：
- 吃瓜、吃瓜网、吃瓜网站
- 51吃瓜、91吃瓜、天涯吃瓜
- 每日吃瓜、天天吃瓜、网红吃瓜、热门吃瓜
- 福利导航、免费资源
- 天涯吃瓜网、吃瓜视频、吃瓜平台

URL 格式：`https://tycg3.com/keyword/{关键词}`

### 3. 视频页面（N个）
从后端 sitemap 服务获取所有视频

URL 格式：`https://tycg3.com/video/{视频ID}`

## 🔧 配置说明

### 环境变量

可以通过环境变量自定义配置：

```bash
# 后端 API 地址
export API_BASE_URL=https://search.cqqvl.cc/prod-api

# 站点域名
export SITE_URL=https://tycg3.com

# 生成 sitemap
npm run generate-sitemap:google
```

### 默认配置

- `API_BASE_URL`: `https://search.cqqvl.cc/prod-api`
- `SITE_URL`: `https://tycg3.com`
- `MAX_VIDEOS`: `50000`（最大视频数量）

## 📝 Google Search Console 提交步骤

### 1. 访问 Google Search Console

- 网址：https://search.google.com/search-console
- 使用 Google 账号登录

### 2. 添加属性

1. 点击"添加属性"
2. 选择"网址前缀"
3. 输入：`https://tycg3.com`
4. 点击"继续"

### 3. 验证网站所有权

**方法一：HTML标签验证（推荐）**

1. 选择"HTML标签"验证方式
2. 复制提供的 meta 标签代码
3. 添加到 `public/index.html` 的 `<head>` 标签中
4. 重新构建和部署
5. 点击"验证"

**方法二：文件验证**

1. 选择"HTML文件"验证方式
2. 下载验证文件
3. 上传到网站根目录（`public/` 目录）
4. 确保可以通过 `https://tycg3.com/验证文件名.html` 访问
5. 点击"验证"

### 4. 提交 Sitemap

1. 验证成功后，进入"站点地图"页面
2. 在"添加新的站点地图"中输入：
   ```
   https://tycg3.com/sitemap-google.xml
   ```
3. 点击"提交"
4. 等待 Google 处理（通常 24-48 小时）

## 📊 监控和优化

### 1. 查看索引状态

- 进入"索引" → "网页"
- 查看已索引的页面数量
- 查看索引覆盖率

### 2. 查看搜索表现

- 进入"效果"页面
- 查看：
  - 点击次数
  - 展示次数
  - 平均排名
  - 点击率

### 3. 优化建议

- **提升点击率**：优化页面标题和描述
- **提升排名**：优化关键词密度
- **增加索引**：确保所有页面都可以访问
- **修复错误**：处理抓取错误和索引问题

## ⚙️ 构建集成

### 自动生成

运行 `npm run build` 时会自动生成两个版本的 sitemap：

```bash
npm run build
```

这会生成：
- `public/sitemap.xml` - 安全版（国内搜索引擎）
- `public/sitemap-google.xml` - 完整版（Google Search Console）

### 单独生成

```bash
# 只生成 Google sitemap
npm run generate-sitemap:google

# 只生成安全版 sitemap
npm run generate-sitemap
```

## 🔍 关键词说明

### 使用的关键词

脚本使用现有的15个关键词，这些关键词已经过优化：

1. **核心关键词**：吃瓜、吃瓜网、吃瓜网站
2. **品牌关键词**：51吃瓜、91吃瓜、天涯吃瓜
3. **长尾关键词**：每日吃瓜、天天吃瓜、网红吃瓜、热门吃瓜
4. **功能关键词**：福利导航、免费资源
5. **扩展关键词**：天涯吃瓜网、吃瓜视频、吃瓜平台

### 关键词页面 URL

每个关键词都会生成一个独立的 URL：
- `https://tycg3.com/keyword/吃瓜`
- `https://tycg3.com/keyword/天涯吃瓜`
- `https://tycg3.com/keyword/51吃瓜`
- ... 等

### 搜索匹配

当用户在 Google 搜索这些关键词时：
- 搜索引擎会匹配到对应的关键词页面
- 视频页面也会被索引
- 提升整体搜索可见性

## ⚠️ 重要提示

### 1. 关键词页面路由

- ⚠️ **必须实现关键词页面路由**
- ✅ 确保 `/keyword/{关键词}` 可以正常访问
- ✅ 如果返回 404，Google 会降低排名
- ✅ 可以使用 React Router 或 Nginx 重写规则

### 2. 视频页面路由

- ⚠️ **必须实现视频页面路由**
- ✅ 确保 `/video/{视频ID}` 可以正常访问
- ✅ 视频页面应该显示实际的视频内容

### 3. 内容质量

- ✅ 确保视频内容质量高
- ✅ 优化视频标题和描述
- ✅ 添加视频缩略图
- ✅ 使用视频结构化数据（可选）

### 4. 更新频率

- 📅 建议每周更新一次 sitemap
- 📅 保持 lastmod 日期最新
- 📅 Google 会自动抓取更新

## 🎯 预期效果

提交到 Google Search Console 后：

1. **索引时间**：24-48 小时内开始抓取
2. **完全索引**：1-2 周内完成索引
3. **搜索可见**：用户搜索关键词时，可能出现在搜索结果中
4. **流量提升**：随着索引量增加，搜索流量逐步提升

## 📚 相关文档

- [替代 SEO 方案](./ALTERNATIVE_SEO.md)
- [Sitemap 内容说明](./SITEMAP_CONTENT.md)
- [提交指南](./SUBMIT_GUIDE.md)

