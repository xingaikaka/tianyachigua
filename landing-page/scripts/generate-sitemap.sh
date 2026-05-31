#!/bin/bash

# Sitemap 生成脚本的快速配置和使用示例

# ============================================
# 配置区域（根据实际情况修改）
# ============================================

# 后端 API 地址
export API_BASE_URL="https://search.cqqvl.cc/prod-api"

# 落地页域名
export SITE_URL="https://tycg8.com"

# ============================================
# 使用方法
# ============================================

echo "📋 Sitemap 生成配置"
echo ""
echo "当前配置："
echo "  - API地址: $API_BASE_URL"
echo "  - 站点URL: $SITE_URL"
echo ""
echo "生成 sitemap..."
npm run generate-sitemap

