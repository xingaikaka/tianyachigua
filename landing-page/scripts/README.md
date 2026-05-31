# Sitemap 生成指南

## 快速开始

### 方法一：使用 npm 脚本（推荐）

```bash
# 设置环境变量（可选，有默认值）
export API_BASE_URL=https://search.cqqvl.cc/prod-api
export SITE_URL=https://tycg.com

# 生成 sitemap.xml
npm run generate-sitemap
```

### 方法二：使用 shell 脚本

```bash
# 编辑 scripts/generate-sitemap.sh 设置配置
# 然后运行
./scripts/generate-sitemap.sh
```

### 方法三：直接运行 Node.js 脚本

```bash
node scripts/generate-sitemap.js
```

## 工作原理

脚本会按以下顺序尝试获取视频数据：

1. **优先方案**：从后端 sitemap 服务获取（`/web/api/sitemap/sitemap-index.xml`）
   - ✅ 无需解密，更可靠
   - ✅ 包含完整的视频元数据
   - ✅ XML 格式，易于解析

2. **备用方案**：从视频列表 API 获取（`/web/api/category/videos`）
   - ⚠️ 可能返回加密数据，无法解析
   - ⚠️ 需要处理分页

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `API_BASE_URL` | 后端 API 基础地址 | `https://search.cqqvl.cc/prod-api` |
| `SITE_URL` | 落地页域名（用于生成 sitemap 中的 URL） | `https://tycg.com` |

### 配置文件

可以在 `scripts/generate-sitemap.sh` 中修改配置：

```bash
export API_BASE_URL="https://your-api-domain.com/prod-api"
export SITE_URL="https://your-site.com"
```

## 输出文件

生成的 `sitemap.xml` 文件位于：
- 源文件：`public/sitemap.xml`
- 构建后：`build/sitemap.xml` 或 `out/sitemap.xml`

## 构建集成

运行 `npm run build` 时会自动生成 sitemap.xml：

```bash
npm run build
```

如果后端 API 不可用，构建会继续，但 sitemap 可能不完整。

## 故障排除

### 问题：无法连接到后端 API

**解决方案：**
1. 检查网络连接
2. 确认 `API_BASE_URL` 配置正确
3. 检查后端服务是否运行
4. 检查防火墙设置

### 问题：sitemap 为空或只有主页面

**可能原因：**
1. 后端 sitemap 服务不可用
2. API 返回加密数据无法解析
3. 视频数据为空

**解决方案：**
1. 检查后端日志
2. 尝试直接访问 sitemap URL：`https://search.cqqvl.cc/prod-api/web/api/sitemap/sitemap-index.xml`
3. 检查后端配置

### 问题：sitemap 文件太大

**说明：**
- sitemap.xml 文件大小不应超过 50MB
- 单个 sitemap 不应超过 50,000 个 URL

**解决方案：**
- 脚本会自动限制最多处理 10 个 sitemap 文件
- 如果视频数量超过 50,000，建议：
  1. 只包含最近更新的视频
  2. 使用 sitemap index 分割多个文件
  3. 修改脚本添加过滤逻辑

## 搜索引擎提交

生成 sitemap 后，需要提交到搜索引擎：

### Google Search Console
1. 访问 https://search.google.com/search-console
2. 添加属性（网站）
3. 在"站点地图"中添加：`https://tycg.com/sitemap.xml`

### 百度站长平台
1. 访问 https://ziyuan.baidu.com
2. 添加网站
3. 在"数据引入" > "链接提交"中添加 sitemap

### 360 站长平台
1. 访问 https://zhanzhang.so.com
2. 添加网站
3. 提交 sitemap

### 神马站长平台（UC）
1. 访问 https://zhanzhang.sm.cn
2. 添加网站
3. 提交 sitemap

### 搜狗站长平台
1. 访问 https://zhanzhang.sogou.com
2. 添加网站
3. 提交 sitemap

## 更新频率建议

- **开发环境**：每次构建时更新
- **生产环境**：每天或每次部署时更新
- **自动化**：可以设置定时任务（cron）自动生成

## 注意事项

1. ⚠️ **API 加密**：如果后端 API 返回加密数据，脚本可能无法解析，建议使用 sitemap 服务
2. ⚠️ **文件大小**：确保生成的 sitemap.xml 不超过 50MB
3. ⚠️ **URL 数量**：单个 sitemap 不应超过 50,000 个 URL
4. ✅ **自动构建**：`npm run build` 会自动生成 sitemap
5. ✅ **错误处理**：如果生成失败，构建仍会继续

## 示例输出

```
🚀 开始生成 sitemap.xml...

📋 配置信息:
   - 后端API: https://search.cqqvl.cc/prod-api
   - 站点URL: https://tycg.com
   - 输出文件: /path/to/public/sitemap.xml

📡 从后端 sitemap 服务获取视频列表（推荐方案）...
   📄 获取 sitemap index: https://search.cqqvl.cc/prod-api/web/api/sitemap/sitemap-index.xml
   📋 找到 5 个 sitemap 文件
   📄 处理 sitemap 1/5: https://search.cqqvl.cc/prod-api/web/api/sitemap/video-sitemap-1.xml
   ✅ sitemap 1 解析完成，当前总计: 10000 个视频
   ...
✅ 从 sitemap 获取到 50000 个视频

📝 生成 sitemap.xml...
   📹 添加 50000 个视频页面...

✅ sitemap.xml 生成成功！
   📁 文件位置: /path/to/public/sitemap.xml
   📊 包含 50001 个URL（1个主页面 + 50000个视频页面）
   📏 文件大小: 2.45 MB
```

