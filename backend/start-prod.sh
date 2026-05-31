#!/bin/bash

# 生产环境启动脚本

echo "=========================================="
echo "启动 ChiguaVideo 后台服务 - 生产环境"
echo "=========================================="

# 设置环境变量
export SPRING_PROFILES_ACTIVE=prod
export SPRING_PROFILES_INCLUDE=encryption-prod,chigua-prod

# 检查是否存在环境配置文件
if [ -f ".env.production" ]; then
    echo "正在加载生产环境配置文件..."
    export $(cat .env.production | grep -v '^#' | grep -v '^$' | xargs)
else
    echo "错误: 未找到 .env.production 文件"
    echo "生产环境必须配置环境变量文件"
    echo ""
    echo "执行以下命令创建配置文件:"
    echo "cp env.production.template .env.production"
    echo "然后编辑 .env.production 文件，填入正确的生产环境配置"
    exit 1
fi

# 验证必要的环境变量
required_vars=(
    "DB_PASSWORD"
    "R2_ACCESS_KEY_ID"
    "R2_SECRET_KEY"
    "IMAGE_ENCRYPTION_KEY"
    "URL_SIGNATURE_SECRET"
    "DATA_ENCRYPTION_SECRET_KEY"
    "JWT_SECRET"
)

echo "正在验证必要的环境变量..."
missing_vars=()

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        missing_vars+=("$var")
    fi
done

if [ ${#missing_vars[@]} -ne 0 ]; then
    echo "错误: 以下必要的环境变量未配置:"
    for var in "${missing_vars[@]}"; do
        echo "  - $var"
    done
    echo ""
    echo "请在 .env.production 文件中配置这些变量"
    exit 1
fi

echo "环境变量验证通过"
echo ""
echo "当前环境: 生产环境 (prod)"
echo "激活的配置文件: encryption-prod, chigua-prod"
echo "服务端口: ${SERVER_PORT:-8080}"
echo "数据库: ${DB_URL}"
echo "Redis: ${REDIS_HOST}:${REDIS_PORT}"
echo "Swagger文档: ${SWAGGER_ENABLED:-false}"
echo "数据加密: ${DATA_ENCRYPTION_ENABLED:-true}"
echo "图片加密: ${IMAGE_ENCRYPTION_ENABLED:-true}"
echo "URL签名: ${URL_SIGNATURE_ENABLED:-true}"
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

# 生产环境使用jar包启动
jar_file="ruoyi-admin/target/ruoyi-admin.jar"

if [ ! -f "$jar_file" ]; then
    echo "未找到jar文件，正在构建..."
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo "构建失败"
        exit 1
    fi
fi

echo "正在启动应用..."
echo "=========================================="

# 生产环境JVM参数优化
JVM_OPTS="-server \
-Xms1g \
-Xmx2g \
-XX:NewRatio=1 \
-XX:SurvivorRatio=8 \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:+PrintGCDetails \
-XX:+PrintGCTimeStamps \
-XX:+PrintGCApplicationStoppedTime \
-Xloggc:./logs/gc.log \
-XX:+UseGCLogFileRotation \
-XX:NumberOfGCLogFiles=5 \
-XX:GCLogFileSize=10M \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=./logs/ \
-Dspring.profiles.active=prod \
-Dspring.profiles.include=encryption-prod,chigua-prod"

# 启动应用
nohup java $JVM_OPTS -jar $jar_file > ./logs/application.log 2>&1 &

echo "应用正在后台启动..."
echo "进程ID: $!"
echo "日志文件: ./logs/application.log"
echo "GC日志: ./logs/gc.log"
echo ""
echo "查看启动日志: tail -f ./logs/application.log"
echo "查看应用状态: ps aux | grep ruoyi-admin"
echo "停止应用: pkill -f ruoyi-admin"
