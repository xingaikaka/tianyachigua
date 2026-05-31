# RedGifs用户列表响应式设计

## 需求描述
用户列表需要在PC端和移动端显示不同的卡片样式：
- **PC端**: 显示完整的用户信息卡片，类似用户详情页的样式，包含大头像、描述、统计信息等
- **移动端**: 保持简洁的卡片样式，只显示头像、用户名和视频数量

## 设计方案
使用Tailwind CSS的响应式工具类（`md:` 前缀）实现两套完全独立的布局：
- 移动端使用 `md:hidden` 类（在中等屏幕及以上隐藏）
- PC端使用 `hidden md:flex` 类（默认隐藏，在中等屏幕及以上显示）

## 样式对比

### 移动端样式（< 768px）
```
┌─────────────────┐
│   ┌────────┐    │
│   │ 头像   │    │  ← 小头像 80x80px
│   │  [数量] │    │  ← 右下角视频数量徽章
│   └────────┘    │
│   用户名        │  ← 较小字体
│   @username     │  ← handle
└─────────────────┘
```

特点：
- 紧凑布局，适合小屏幕
- 2列网格显示
- 较小间距（gap-3）
- 关键信息一目了然

### PC端样式（≥ 768px）
```
┌────────────────────────┐
│     ┌──────────┐       │
│     │          │       │
│     │  大头像   │       │  ← 大头像 176x176px
│     │    ✓     │       │  ← 认证标识
│     └──────────┘       │
│                        │
│      用户名            │  ← 大标题
│      @username         │  ← handle
│                        │
│  这是用户的描述信息...  │  ← 描述/简介
│                        │
│  ──────────────────   │  ← 分割线
│                        │
│  403      179   472.9K │  ← 统计信息
│  粉丝    视频    播放   │
└────────────────────────┘
```

特点：
- 完整信息展示
- 3列网格显示
- 较大间距（gap-6）
- 类似用户详情页的设计
- 悬停效果增强

## 实现细节

### 1. 响应式网格布局
```jsx
<div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-3 gap-3 md:gap-6">
```
- 移动端：2列，间距12px
- PC端：3列，间距24px

### 2. 移动端卡片
```jsx
<div className="p-4 flex flex-col items-center text-center md:hidden">
  {/* 小头像 80x80px */}
  <div className="w-20 h-20 rounded-full ...">
    {/* 视频数量徽章 */}
    <div className="absolute -bottom-1 -right-1 ...">
      <span>{formatNumber(user.publishedGifsCount)}</span>
    </div>
  </div>
  
  {/* 用户名 */}
  <h3 className="text-lg font-bold ...">
    {user.name || user.username}
  </h3>
  
  {/* Handle */}
  <p className="text-gray-400 text-xs">@{user.username}</p>
</div>
```

关键类名：
- `md:hidden` - 在中等屏幕及以上隐藏
- `w-20 h-20` - 80x80px头像
- `text-lg` - 18px用户名
- `text-xs` - 12px handle

### 3. PC端卡片
```jsx
<div className="hidden md:flex md:flex-col md:items-center md:text-center md:p-8">
  {/* 大头像 176x176px */}
  <div className="w-44 h-44 rounded-full ...">
    {/* 认证徽章 */}
    {user.verified === 1 && (
      <div className="absolute bottom-2 right-2 w-12 h-12 ...">
        <span>✓</span>
      </div>
    )}
  </div>
  
  {/* 用户名 */}
  <h3 className="text-2xl font-bold ...">
    {user.name || user.username}
  </h3>
  
  {/* Handle */}
  <p className="text-gray-400 text-base ...">@{user.username}</p>
  
  {/* 描述 */}
  <p className="text-gray-300 text-sm ...">
    {user.description || '暂无简介'}
  </p>
  
  {/* 分割线 */}
  <div className="w-full border-t border-gray-700 ..."></div>
  
  {/* 统计信息 3列网格 */}
  <div className="grid grid-cols-3 gap-6 w-full">
    <div className="flex flex-col items-center">
      <span className="text-3xl font-bold text-[#a78bfa]">
        {formatNumber(user.followers || 0)}
      </span>
      <span className="text-xs text-gray-500">粉丝</span>
    </div>
    <div className="flex flex-col items-center">
      <span className="text-3xl font-bold text-[#a78bfa]">
        {formatNumber(user.publishedGifsCount || user.gifsCount || 0)}
      </span>
      <span className="text-xs text-gray-500">视频</span>
    </div>
    <div className="flex flex-col items-center">
      <span className="text-3xl font-bold text-[#a78bfa]">
        {formatNumber(user.views || 0)}
      </span>
      <span className="text-xs text-gray-500">播放</span>
    </div>
  </div>
</div>
```

关键类名：
- `hidden md:flex` - 默认隐藏，中等屏幕及以上显示
- `w-44 h-44` - 176x176px大头像
- `text-2xl` - 24px用户名
- `text-base` - 16px handle
- `text-3xl` - 30px统计数字
- `text-[#a78bfa]` - 紫色强调色

### 4. 认证标识
```jsx
{user.verified === 1 && (
  <div className="absolute bottom-2 right-2 w-12 h-12 bg-[#7c3aed] rounded-full flex items-center justify-center border-3 border-[#1f1f1f] shadow-lg">
    <span className="text-white text-2xl font-bold">✓</span>
  </div>
)}
```
- 仅在PC端显示
- 仅当 `user.verified === 1` 时显示
- 位于头像右下角
- 紫色背景，白色对勾

### 5. 描述处理
```jsx
<p className="text-gray-300 text-sm mb-8 min-h-[40px] px-4">
  {user.description || '暂无简介'}
</p>
```
- 如果用户有描述则显示
- 如果没有描述则显示"暂无简介"
- 最小高度40px，保持卡片高度一致

### 6. 统计信息格式化
使用 `formatNumber` 函数格式化大数字：
```javascript
const formatNumber = (num) => {
  if (!num) return '0';
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M';  // 1.5M
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K';     // 1.5K
  }
  return num.toString();
};
```

示例：
- 403 → "403"
- 1500 → "1.5K"
- 472900 → "472.9K"
- 1500000 → "1.5M"

## 断点说明
Tailwind CSS 默认断点：
- `sm`: 640px
- `md`: 768px (本项目主要使用)
- `lg`: 1024px
- `xl`: 1280px
- `2xl`: 1536px

本项目使用 `md` 断点（768px）作为移动端和PC端的分界点。

## 悬停效果
```jsx
className="... hover:-translate-y-1 hover:shadow-[0_6px_20px_rgba(139,92,246,0.3)] hover:border-[#8b5cf6] ..."
```
- 卡片向上平移4px
- 阴影增强（紫色光晕）
- 边框变为紫色

## 颜色方案
- 主背景：`#1f1f1f`
- 强调色（紫色）：`#7c3aed`, `#8b5cf6`, `#a78bfa`
- 文字：白色、灰色渐变
- 边框：`#333`, `#707070`

## 修改文件
- `chigua-web/src/components/UserList/index.jsx`

## 优势
1. ✅ **响应式设计**：移动端和PC端体验各自优化
2. ✅ **代码复用**：单一组件处理两种布局
3. ✅ **性能优化**：使用CSS隐藏而非JavaScript条件渲染
4. ✅ **维护性好**：两套样式独立，易于修改
5. ✅ **用户体验**：
   - 移动端：快速浏览，信息简洁
   - PC端：详细信息，便于选择

## 测试建议
1. 在不同屏幕尺寸下测试（375px, 768px, 1024px, 1920px）
2. 测试有/无描述的用户卡片
3. 测试已认证/未认证用户的显示
4. 测试数字格式化（小数字、K级别、M级别）
5. 测试悬停效果
6. 测试响应式断点切换时的表现
