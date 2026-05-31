# Sitemap 生成配置

## 环境变量配置

可以通过环境变量配置 sitemap 生成脚本：

```bash
# 后端 API 地址（必需）
export API_BASE_URL=https://search.cqqvl.cc/prod-api

# 落地页域名（必需，用于生成 sitemap 中的 URL）
export SITE_URL=https://tycg.com

# 运行生成脚本
npm run generate-sitemap
```

## 默认配置

如果不设置环境变量，脚本使用以下默认值：

- `API_BASE_URL`: `https://search.cqqvl.cc/prod-api`
- `SITE_URL`: `https://tycg.com`

## 使用方法

### 1. 手动生成 sitemap

```bash
# 设置环境变量（可选）
export API_BASE_URL=https://your-api-domain.com/prod-api
export SITE_URL=https://your-site.com

# 生成 sitemap.xml
npm run generate-sitemap
```

### 2. 构建时自动生成

运行 `npm run build` 时会自动生成 sitemap.xml：

```bash
npm run build
```

### 3. 跳过 sitemap 生成

如果后端 API 不可用，可以使用：

```bash
npm run build:no-sitemap
```

## 输出文件

生成的 `sitemap.xml` 文件位于 `public/sitemap.xml`，构建时会自动包含在输出目录中。

## 故障排除

### 问题：API 请求失败

**解决方案：**
1. 检查后端 API 是否可访问
2. 检查网络连接
3. 如果 API 需要认证，可能需要修改脚本添加认证头

### 问题：生成的 sitemap 为空

**解决方案：**
1. 检查后端 API 返回的数据格式
2. 查看脚本输出的错误信息
3. 确认视频数据是否存在

### 问题：sitemap 太大

**解决方案：**
- sitemap.xml 文件大小不应超过 50MB
- 如果视频数量超过 50,000，需要生成多个 sitemap 文件
- 可以修改脚本添加分页逻辑

## 注意事项

1. **API 加密**：如果后端 API 返回加密数据，脚本可能无法正确解析。需要先解密或使用未加密的接口。

2. **视频数量**：如果视频数量很大（>10万），建议：
   - 只包含最近更新的视频
   - 使用 sitemap index 分割多个 sitemap 文件
   - 定期更新 sitemap

3. **更新频率**：建议每天或每次部署时重新生成 sitemap.xml

4. **搜索引擎提交**：生成 sitemap 后，需要提交到：
   - Google Search Console
   - 百度站长平台
   - 360 站长平台
   - 神马站长平台
   - 搜狗站长平台

