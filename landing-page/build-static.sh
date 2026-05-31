#!/bin/bash

# 天涯吃瓜网落地页静态文件构建脚本

echo "🚀 开始构建天涯吃瓜网落地页静态文件..."

# 生成 sitemap.xml（可选，如果后端API不可用会跳过）
echo "📝 生成 sitemap.xml..."
npm run generate-sitemap || echo "⚠️  sitemap 生成失败，继续构建..."

# 构建项目
npm run build:no-sitemap

if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    
    # 清理旧的out目录
    if [ -d "out" ]; then
        rm -rf out
        echo "🧹 清理旧的out目录"
    fi
    
    # 复制build目录到out目录
    cp -r build out
    echo "📁 创建out目录"
    
    # 创建静态文件部署包
    tar -czf landing-page-static.tar.gz -C out .
    echo "📦 创建静态文件部署包: landing-page-static.tar.gz"
    
    echo ""
    echo "✅ 静态文件构建完成！"
    echo ""
    echo "📋 部署说明："
    echo "1. 使用 landing-page-static.tar.gz 进行静态文件部署"
    echo "2. 解压到Web服务器根目录"
    echo "3. 配置Nginx/Apache指向解压后的文件"
    echo ""
    echo "📁 静态文件位置："
    echo "- out/ 目录包含所有静态文件"
    echo "- index.html 为入口文件"
    echo ""
    echo "🌐 本地预览："
    echo "cd out && python3 -m http.server 8000"
    echo "或者: npx serve -s out"
    
else
    echo "❌ 构建失败！"
    exit 1
fi
