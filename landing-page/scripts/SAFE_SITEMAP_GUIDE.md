# Sitemap 生成指南 - 国内搜索引擎安全版

## ⚠️ 重要提示

**国内搜索引擎（百度、360、神马、搜狗）对成人内容有严格审核政策！**

提交包含成人内容的 sitemap 可能导致：
- ❌ 提交被拒绝
- ❌ 网站被降权或封禁
- ❌ 影响其他正常内容的收录

## 解决方案

我们提供了两种版本的 sitemap：

### 1. 完整版 sitemap.xml（包含所有视频）
- **用途**：提交给 Google Search Console
- **文件**：`public/sitemap.xml`
- **内容**：主页面 + 所有视频页面

### 2. 安全版 sitemap-safe.xml（仅主页面）
- **用途**：提交给国内搜索引擎（百度、360、神马、搜狗）
- **文件**：`public/sitemap-safe.xml`
- **内容**：仅主页面，不包含视频页面

## 使用方法

### 生成完整版 sitemap（默认）

```bash
npm run generate-sitemap
```

生成文件：`public/sitemap.xml`

### 生成安全版 sitemap

```bash
npm run generate-sitemap:safe
```

或：

```bash
INCLUDE_VIDEOS=false npm run generate-sitemap
```

生成文件：`public/sitemap-safe.xml`

### 同时生成两个版本

```bash
npm run build
```

构建时会自动生成两个版本的 sitemap。

## 搜索引擎提交建议

### ✅ Google Search Console（推荐提交完整版）

1. 访问 https://search.google.com/search-console
2. 添加属性（网站）
3. 在"站点地图"中添加：
   ```
   https://tycg.com/sitemap.xml
   ```

### ✅ 国内搜索引擎（必须使用安全版）

#### 百度站长平台
1. 访问 https://ziyuan.baidu.com
2. 添加网站
3. 在"数据引入" > "链接提交"中添加：
   ```
   https://tycg.com/sitemap-safe.xml
   ```

#### 360 站长平台
1. 访问 https://zhanzhang.so.com
2. 添加网站
3. 提交站点地图：
   ```
   https://tycg.com/sitemap-safe.xml
   ```

#### 神马站长平台（UC）
1. 访问 https://zhanzhang.sm.cn
2. 添加网站
3. 提交站点地图：
   ```
   https://tycg.com/sitemap-safe.xml
   ```

#### 搜狗站长平台
1. 访问 https://zhanzhang.sogou.com
2. 添加网站
3. 提交站点地图：
   ```
   https://tycg.com/sitemap-safe.xml
   ```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `API_BASE_URL` | 后端 API 基础地址 | `https://search.cqqvl.cc/prod-api` |
| `SITE_URL` | 落地页域名 | `https://tycg.com` |
| `INCLUDE_VIDEOS` | 是否包含视频页面 | `true`（完整版）<br>`false`（安全版） |

### 示例

```bash
# 生成完整版
export INCLUDE_VIDEOS=true
npm run generate-sitemap

# 生成安全版
export INCLUDE_VIDEOS=false
npm run generate-sitemap
```

## 文件说明

### sitemap.xml（完整版）
- 包含主页面和所有视频页面
- 适用于 Google Search Console
- 文件较大，包含所有视频 URL

### sitemap-safe.xml（安全版）
- 仅包含主页面
- 适用于国内搜索引擎
- 文件很小，只有主页面 URL

## 注意事项

1. ⚠️ **不要将完整版 sitemap 提交给国内搜索引擎**
   - 可能导致网站被封禁
   - 影响 SEO 排名

2. ✅ **安全版 sitemap 只包含主页面**
   - 不会暴露视频内容
   - 符合国内搜索引擎政策

3. 💡 **建议策略**
   - Google：提交完整版 `sitemap.xml`
   - 国内搜索引擎：提交安全版 `sitemap-safe.xml`
   - 定期更新两个版本的 sitemap

4. 🔄 **更新频率**
   - 完整版：每天或每次部署时更新
   - 安全版：可以更频繁更新（因为只有主页面）

## 自动化部署

在构建脚本中已经集成了自动生成：

```bash
npm run build
```

这会自动生成两个版本的 sitemap。

## 故障排除

### 问题：安全版 sitemap 为空

**原因**：安全版只包含主页面，这是正常的。

**验证**：检查文件内容，应该只有主页面 URL。

### 问题：完整版 sitemap 太大

**解决方案**：
- 如果视频数量超过 50,000，考虑：
  1. 只包含最近更新的视频
  2. 使用 sitemap index 分割多个文件
  3. 修改脚本添加过滤逻辑

## 示例输出

### 完整版 sitemap.xml

```
🚀 开始生成 sitemap.xml (完整版（包含所有视频）)...

📋 配置信息:
   - 后端API: https://search.cqqvl.cc/prod-api
   - 站点URL: https://tycg.com
   - 模式: 完整版（包含所有视频）
   - 输出文件: /path/to/public/sitemap.xml

📡 从后端 sitemap 服务获取视频列表...
✅ 从 sitemap 获取到 50000 个视频

📝 生成 sitemap.xml (完整版（包含所有视频）)...
   📹 添加 50000 个视频页面...

✅ sitemap.xml 生成成功！
   📁 文件位置: /path/to/public/sitemap.xml
   📊 包含 50001 个URL（1个主页面 + 50000个视频页面）
   📏 文件大小: 2.45 MB

💡 提示：此 sitemap 适用于提交给 Google Search Console
   如需生成安全版，请运行: INCLUDE_VIDEOS=false npm run generate-sitemap
```

### 安全版 sitemap-safe.xml

```
🚀 开始生成 sitemap.xml (安全版（仅主页面）)...

📋 配置信息:
   - 后端API: https://search.cqqvl.cc/prod-api
   - 站点URL: https://tycg.com
   - 模式: 安全版（仅主页面）
   - 输出文件: /path/to/public/sitemap-safe.xml

⚠️  注意：安全模式仅包含主页面，适用于提交给国内搜索引擎
   完整版 sitemap 请使用: INCLUDE_VIDEOS=true npm run generate-sitemap

✅ 安全模式：跳过视频列表获取

📝 生成 sitemap.xml (安全版（仅主页面）)...
   ✅ 安全模式：仅包含主页面，不包含视频页面

✅ sitemap.xml 生成成功！
   📁 文件位置: /path/to/public/sitemap-safe.xml
   📊 包含 1 个URL（1个主页面）
   📏 文件大小: 0.35 KB

💡 提示：此 sitemap 适用于提交给国内搜索引擎（百度、360、神马、搜狗）
```

