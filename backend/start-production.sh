#!/bin/bash

# 生产环境启动脚本 - 增强版
# 用于确保JAR包在生产环境稳定启动

# 设置脚本错误时退出
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo -e "      天涯吃瓜 - 生产环境启动脚本"
echo -e "==========================================${NC}"

# 检查JAR包是否存在
JAR_FILE="ruoyi-admin.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}❌ 错误: 找不到JAR包文件 $JAR_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 找到JAR包: $(ls -lh $JAR_FILE | awk '{print $9, $5}')${NC}"

# 检查Java版本
echo -e "${BLUE}🔍 检查Java环境...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ 错误: 未找到Java环境${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo -e "${GREEN}✅ Java版本: $JAVA_VERSION${NC}"

# 设置默认环境变量（如果未设置）
export SERVER_PORT=${SERVER_PORT:-8080}
export CONTEXT_PATH=${CONTEXT_PATH:-/}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
export SPRING_PROFILES_INCLUDE=${SPRING_PROFILES_INCLUDE:-encryption-prod,chigua-prod}

echo -e "${BLUE}📋 环境配置:${NC}"
echo -e "  端口: $SERVER_PORT"
echo -e "  路径: $CONTEXT_PATH"
echo -e "  Profile: $SPRING_PROFILES_ACTIVE"
echo -e "  Include: $SPRING_PROFILES_INCLUDE"

# 检查端口是否被占用
if lsof -i :$SERVER_PORT >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  警告: 端口 $SERVER_PORT 已被占用${NC}"
    echo -e "${YELLOW}正在查找占用进程...${NC}"
    lsof -i :$SERVER_PORT
    read -p "是否终止占用进程并继续? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        PID=$(lsof -t -i :$SERVER_PORT)
        kill -9 $PID
        echo -e "${GREEN}✅ 已终止进程 $PID${NC}"
        sleep 2
    else
        echo -e "${RED}❌ 启动取消${NC}"
        exit 1
    fi
fi

# 创建日志目录
mkdir -p logs
echo -e "${GREEN}✅ 日志目录已准备${NC}"

# 生产环境JVM参数（保守配置）
JVM_OPTS=(
    "-server"
    "-Xms512m"
    "-Xmx1024m"
    "-XX:MetaspaceSize=128m"
    "-XX:MaxMetaspaceSize=256m"
    "-XX:+UseG1GC"
    "-XX:MaxGCPauseMillis=200"
    "-XX:+HeapDumpOnOutOfMemoryError"
    "-XX:HeapDumpPath=./logs/"
    "-Djava.awt.headless=true"
    "-Dfile.encoding=UTF-8"
    "-Duser.timezone=Asia/Shanghai"
    "-Dspring.profiles.active=$SPRING_PROFILES_ACTIVE"
)

# 如果设置了include profiles
if [ -n "$SPRING_PROFILES_INCLUDE" ]; then
    JVM_OPTS+=("-Dspring.profiles.include=$SPRING_PROFILES_INCLUDE")
fi

echo -e "${BLUE}🚀 启动应用...${NC}"
echo -e "${YELLOW}JVM参数: ${JVM_OPTS[*]}${NC}"

# 启动应用（前台模式，便于调试）
echo -e "${GREEN}正在启动，请稍候...${NC}"

# 使用nohup后台启动
nohup java "${JVM_OPTS[@]}" -jar "$JAR_FILE" > logs/application.log 2>&1 &
PID=$!

echo -e "${GREEN}✅ 应用已启动，进程ID: $PID${NC}"
echo -e "${BLUE}📄 日志文件: logs/application.log${NC}"

# 等待应用启动
echo -e "${YELLOW}等待应用完全启动...${NC}"
for i in {1..30}; do
    if curl -s -f http://localhost:$SERVER_PORT/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✅ 应用启动成功！${NC}"
        echo -e "${GREEN}🌐 访问地址: http://localhost:$SERVER_PORT${NC}"
        break
    fi
    
    if ! ps -p $PID > /dev/null; then
        echo -e "${RED}❌ 应用启动失败，进程已退出${NC}"
        echo -e "${RED}查看日志: tail -f logs/application.log${NC}"
        exit 1
    fi
    
    echo -n "."
    sleep 2
done

echo -e "\n${BLUE}=========================================="
echo -e "  启动完成"
echo -e "  进程ID: $PID"
echo -e "  日志文件: logs/application.log"
echo -e "  停止命令: kill $PID"
echo -e "  查看日志: tail -f logs/application.log"
echo -e "==========================================${NC}"
