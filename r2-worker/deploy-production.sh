#!/bin/bash

# Chigua R2 Worker 生产环境部署脚本
# 包含CDN缓存功能

set -e  # 遇到错误立即退出

echo "=========================================="
echo "Chigua R2 Worker 生产环境部署"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查是否在正确的目录
if [ ! -f "wrangler.toml" ]; then
    echo -e "${RED}错误: 请在 r2-worker 目录下运行此脚本${NC}"
    exit 1
fi

# 步骤1: 检查依赖
echo -e "${YELLOW}步骤 1/5: 检查依赖...${NC}"
if ! command -v wrangler &> /dev/null; then
    echo -e "${RED}错误: wrangler 未安装${NC}"
    echo "请运行: npm install -g wrangler"
    exit 1
fi
echo -e "${GREEN}✓ Wrangler 已安装: $(wrangler --version)${NC}"
echo ""

# 步骤2: 安装项目依赖
echo -e "${YELLOW}步骤 2/5: 安装项目依赖...${NC}"
npm install
echo -e "${GREEN}✓ 依赖安装完成${NC}"
echo ""

# 步骤3: 类型检查（可选）
echo -e "${YELLOW}步骤 3/5: TypeScript 类型检查...${NC}"
if npx tsc --noEmit 2>/dev/null; then
    echo -e "${GREEN}✓ 类型检查通过${NC}"
else
    echo -e "${YELLOW}⚠ 类型检查有警告，继续部署...${NC}"
fi
echo ""

# 步骤4: 显示将要部署的配置
echo -e "${YELLOW}步骤 4/5: 部署配置检查...${NC}"
echo "Worker 名称: chigua-r2-worker"
echo "环境变量:"
echo "  - ENABLE_EDGE_CACHE: true"
echo "  - CACHE_TTL: 3600"
echo "  - MAX_CACHE_SIZE: 10485760"
echo "  - CACHE_EXCLUDE_PATTERNS: temp/*,draft/*,preview/*"
echo ""

# 确认部署
read -p "确认部署到生产环境? (y/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}部署已取消${NC}"
    exit 0
fi

# 步骤5: 部署到 Cloudflare
echo -e "${YELLOW}步骤 5/5: 部署到 Cloudflare...${NC}"
wrangler deploy

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ 部署成功！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "部署信息:"
echo "  - Worker URL: https://chigua-r2-worker.lee888.workers.dev"
echo "  - 缓存功能: 已启用"
echo ""
echo "测试端点:"
echo "  1. 缓存统计: curl https://chigua-r2-worker.lee888.workers.dev/api/cache/stats"
echo "  2. 上传文件: curl -X POST https://chigua-r2-worker.lee888.workers.dev/api/upload -F 'file=@test.txt'"
echo "  3. 健康检查: curl https://chigua-r2-worker.lee888.workers.dev/"
echo ""
echo -e "${YELLOW}提示: 首次部署后，请测试所有关键功能确保正常工作${NC}"

