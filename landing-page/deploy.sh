#!/bin/bash

# 51吃瓜网落地页部署脚本

echo "🚀 开始构建天涯吃瓜网落地页..."

# 构建项目
npm run build

if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    
    # 创建部署压缩包
    echo "📦 创建部署包..."
    cd build
    tar -czf ../landing-page-dist.tar.gz .
    cd ..
    
    echo "✅ 部署包已创建: landing-page-dist.tar.gz"
    echo ""
    echo "📋 部署说明："
    echo "1. 将 landing-page-dist.tar.gz 上传到服务器"
    echo "2. 解压到网站根目录"
    echo "3. 配置Nginx或Apache指向解压后的文件"
    echo ""
    echo "🌐 本地预览："
    echo "npx serve -s build"
    
else
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi
