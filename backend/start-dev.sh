#!/bin/bash

# 开发环境启动脚本

echo "=========================================="
echo "启动 ChiguaVideo 后台服务 - 开发环境"
echo "=========================================="

# 设置环境变量
export SPRING_PROFILES_ACTIVE=dev
export SPRING_PROFILES_INCLUDE=encryption-dev,chigua-dev

# 检查是否存在环境配置文件
if [ -f ".env.development" ]; then
    echo "正在加载开发环境配置文件..."
    export $(cat .env.development | grep -v '^#' | grep -v '^$' | xargs)
else
    echo "警告: 未找到 .env.development 文件"
    echo "请复制 env.development.template 为 .env.development 并配置相应参数"
    echo ""
    echo "执行以下命令创建配置文件:"
    echo "cp env.development.template .env.development"
    echo ""
    read -p "是否继续使用默认配置启动? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "当前环境: 开发环境 (dev)"
echo "激活的配置文件: encryption-dev, chigua-dev"
echo "服务端口: ${SERVER_PORT:-8080}"
echo "数据库: ${DB_URL:-本地MySQL}"
echo "Redis: ${REDIS_HOST:-localhost}:${REDIS_PORT:-6379}"
echo "Swagger文档: ${SWAGGER_ENABLED:-true}"
echo ""

# 检查Java版本
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java运行环境"
    echo "请安装Java 8或更高版本"
    exit 1
fi

java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Java版本: $java_version"
echo ""

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven"
    echo "请安装Maven 3.6或更高版本"
    exit 1
fi

echo "正在启动应用..."
echo "=========================================="

# 使用Maven启动Spring Boot应用
mvn spring-boot:run -pl ruoyi-admin \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev -Dspring.profiles.include=encryption-dev,chigua-dev"
