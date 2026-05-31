#!/bin/bash

# 前端打包脚本 - 解决macOS扩展属性问题

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================="
echo -e "      前端项目打包脚本"
echo -e "==========================================${NC}"

# 清理macOS扩展属性函数
clean_macos_attrs() {
    local dir=$1
    echo -e "${YELLOW}🧹 清理macOS扩展属性: $dir${NC}"
    
    # 删除 .DS_Store 文件
    find "$dir" -name ".DS_Store" -delete 2>/dev/null || true
    
    # 删除 ._* 文件（资源分叉文件）
    find "$dir" -name "._*" -delete 2>/dev/null || true
    
    # 清理扩展属性
    if command -v xattr >/dev/null 2>&1; then
        find "$dir" -type f -exec xattr -c {} \; 2>/dev/null || true
    fi
}

# 1. 构建Vue管理后台
echo -e "${BLUE}📦 1. 构建Vue管理后台...${NC}"
cd frontend

# 清理之前的构建
rm -rf dist

# 构建
npm run build:prod

# 清理macOS属性
clean_macos_attrs "dist"

# 验证构建结果
if [ ! -f "dist/index.html" ]; then
    echo -e "${RED}❌ Vue管理后台构建失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Vue管理后台构建完成 ($(du -sh dist | cut -f1))${NC}"

# 2. 构建React用户前台
echo -e "${BLUE}📦 2. 构建React用户前台...${NC}"
cd ../chigua-web

# 清理之前的构建
rm -rf build

# 构建
npm run build:prod

# 清理macOS属性
clean_macos_attrs "build"

# 验证构建结果
if [ ! -f "build/index.html" ]; then
    echo -e "${RED}❌ React用户前台构建失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ React用户前台构建完成 ($(du -sh build | cut -f1))${NC}"

# 3. 创建部署包
echo -e "${BLUE}📦 3. 创建部署包...${NC}"
cd ..

# 创建部署目录
DEPLOY_DIR="deploy-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DEPLOY_DIR/frontend"

# 复制Vue管理后台到admin目录
echo -e "${YELLOW}📁 打包Vue管理后台...${NC}"
cp -r frontend/dist "$DEPLOY_DIR/frontend/admin"

# 复制React用户前台到根目录
echo -e "${YELLOW}📁 打包React用户前台...${NC}"
cp -r chigua-web/build/* "$DEPLOY_DIR/frontend/"

# 再次清理属性
clean_macos_attrs "$DEPLOY_DIR"

# 创建压缩包（不包含macOS属性）
echo -e "${YELLOW}🗜️  创建压缩包...${NC}"
cd "$DEPLOY_DIR"

# 使用tar创建压缩包，排除macOS特殊文件
tar --exclude='._*' --exclude='.DS_Store' -czf "../frontend-deploy.tar.gz" frontend/

cd ..

echo -e "${GREEN}✅ 前端部署包创建完成${NC}"
echo -e "${BLUE}📁 部署目录: $DEPLOY_DIR${NC}"
echo -e "${BLUE}📦 压缩包: frontend-deploy.tar.gz ($(du -sh frontend-deploy.tar.gz | cut -f1))${NC}"

# 显示文件结构
echo -e "${YELLOW}📋 文件结构:${NC}"
tree "$DEPLOY_DIR" -L 3 2>/dev/null || find "$DEPLOY_DIR" -type d | head -10

echo -e "${GREEN}🎉 前端打包完成！${NC}"
