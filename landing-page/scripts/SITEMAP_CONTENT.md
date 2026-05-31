# 安全版 Sitemap 内容说明

## 📋 当前 Sitemap 内容

### 1. 主页面
```
https://tycg.com/
- Priority: 1.0（最高优先级）
- Changefreq: daily（每天更新）
```

### 2. 关键词页面（15个）
```
https://tycg.com/keyword/吃瓜
https://tycg.com/keyword/吃瓜网
https://tycg.com/keyword/吃瓜网站
https://tycg.com/keyword/51吃瓜
https://tycg.com/keyword/91吃瓜
https://tycg.com/keyword/天涯吃瓜
https://tycg.com/keyword/每日吃瓜
https://tycg.com/keyword/天天吃瓜
https://tycg.com/keyword/网红吃瓜
https://tycg.com/keyword/热门吃瓜
https://tycg.com/keyword/福利导航
https://tycg.com/keyword/免费资源
https://tycg.com/keyword/天涯吃瓜网
https://tycg.com/keyword/吃瓜视频
https://tycg.com/keyword/吃瓜平台
- Priority: 0.8
- Changefreq: weekly（每周更新）
```

## 🔍 如何搜索定位到落地页

### 搜索场景示例

1. **用户搜索"吃瓜"**
   - 搜索引擎匹配到：`https://tycg.com/keyword/吃瓜`
   - 用户点击后访问关键词页面
   - 关键词页面显示主页面内容

2. **用户搜索"天涯吃瓜"**
   - 搜索引擎匹配到：`https://tycg.com/keyword/天涯吃瓜`
   - 用户点击后访问关键词页面

3. **用户搜索"51吃瓜"**
   - 搜索引擎匹配到：`https://tycg.com/keyword/51吃瓜`
   - 用户点击后访问关键词页面

### 工作原理

1. **Sitemap 提交**
   - 将 sitemap.xml 提交到搜索引擎
   - 搜索引擎爬虫会索引所有 URL

2. **关键词匹配**
   - 搜索引擎分析 URL 中的关键词
   - 当用户搜索相关关键词时，匹配到对应的 URL

3. **页面访问**
   - 用户点击搜索结果
   - 访问关键词页面（如 `/keyword/吃瓜`）
   - 页面显示主页面内容（通过路由实现）

## ⚠️ 重要：需要实现关键词页面路由

### 当前状态
- ✅ Sitemap 已包含关键词页面 URL
- ❌ 落地页还没有实现 `/keyword/{关键词}` 路由

### 需要实现的功能

在落地页中添加路由处理，当访问 `/keyword/{关键词}` 时：
1. 解析 URL 中的关键词参数
2. 显示主页面内容（与首页相同）
3. 可选：在页面中高亮显示关键词

### 实现方案（React Router）

```jsx
// 在 App.jsx 中添加路由
import { BrowserRouter, Routes, Route, useParams } from 'react-router-dom';

function KeywordPage() {
  const { keyword } = useParams();
  // 解码关键词
  const decodedKeyword = decodeURIComponent(keyword || '');
  
  // 显示主页面内容（与首页相同）
  return <App />; // 或者复用主页面组件
}

function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/keyword/:keyword" element={<KeywordPage />} />
        <Route path="/" element={<App />} />
      </Routes>
    </BrowserRouter>
  );
}
```

### 或者使用 Nginx 重写规则

如果不想修改 React 代码，可以在 Nginx 配置中添加：

```nginx
# 关键词页面重定向到首页
location ~ ^/keyword/(.+)$ {
    rewrite ^/keyword/(.+)$ / permanent;
    # 或者返回首页内容
    # try_files $uri /index.html;
}
```

## 📊 Sitemap 统计

- **总 URL 数量**: 16个（1个主页面 + 15个关键词页面）
- **文件大小**: 约 2-3 KB
- **更新频率**: 
  - 主页面：daily
  - 关键词页面：weekly

## ✅ 优势

1. **安全合规**
   - 不包含视频页面
   - 符合国内搜索引擎政策

2. **提升搜索可见性**
   - 多个关键词页面增加被搜索到的概率
   - URL 中包含关键词，有利于 SEO

3. **灵活扩展**
   - 可以随时添加更多关键词
   - 不需要修改后端代码

## 🔧 如何添加更多关键词

编辑 `scripts/generate-sitemap.js` 中的 `KEYWORDS` 数组：

```javascript
KEYWORDS: [
  '吃瓜',
  '吃瓜网',
  // ... 添加更多关键词
  '新关键词1',
  '新关键词2',
],
```

然后重新生成 sitemap：
```bash
npm run generate-sitemap
```

## 📝 注意事项

1. **关键词页面必须可访问**
   - 如果 URL 返回 404，搜索引擎会降低排名
   - 必须实现路由或重定向

2. **避免关键词堆砌**
   - 不要添加过多不相关的关键词
   - 保持关键词与网站内容相关

3. **定期更新**
   - 建议每周更新一次 sitemap
   - 保持 lastmod 日期最新

4. **监控收录情况**
   - 在搜索引擎站长平台查看收录情况
   - 检查关键词页面是否被索引

