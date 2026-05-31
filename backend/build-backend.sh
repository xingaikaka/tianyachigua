#!/bin/bash
# ===========================================
# Java后台 - 构建脚本
# ===========================================

echo "📦 开始构建Java后台..."

# 设置错误时退出
set -e

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven未安装，请先安装Maven"
    exit 1
fi

# 清理并构建
echo "🧹 清理旧的构建产物..."
mvn clean

echo "📋 编译和打包..."
mvn package -DskipTests

# 检查构建结果
JAR_FILE="ruoyi-admin/target/ruoyi-admin.jar"
if [ -f "$JAR_FILE" ]; then
    echo "✅ Java后台构建成功"
    echo "📁 JAR文件位置: backend/$JAR_FILE"
    echo "📊 文件大小: $(du -h $JAR_FILE | cut -f1)"
else
    echo "❌ Java后台构建失败，JAR文件未找到"
    exit 1
fi

echo ""
echo "🚀 部署命令："
echo "scp $JAR_FILE root@27.124.10.130:/opt/chigua/"
echo "ssh root@27.124.10.130 'cd /opt/chigua && ./restart-backend.sh'"
