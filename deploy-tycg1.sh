#!/usr/bin/env bash
# =============================================================================
# tycg1.com 一键部署脚本 - 47.76.76.210
# 用法:
#   ./deploy-tycg1.sh              # 全量部署（服务器初始化 + 后端 + 前端）
#   ./deploy-tycg1.sh init         # 只初始化服务器环境（Java/MySQL/Nginx/Redis）
#   ./deploy-tycg1.sh db           # 只迁移数据库
#   ./deploy-tycg1.sh backend      # 只部署后端
#   ./deploy-tycg1.sh frontend     # 只部署前端
#   ./deploy-tycg1.sh nginx        # 只配置Nginx
#   ./deploy-tycg1.sh check        # 健康检查
# =============================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step()    { echo -e "\n${CYAN}${BOLD}▶ $*${NC}"; }

# ── 目标服务器配置 ──────────────────────────────────────────────────────────
TARGET_IP="47.76.76.210"
TARGET_USER="root"
TARGET_PASS="AdSait##11@"
DEPLOY_PATH="/opt/chigua"
REACT_PATH="/var/www/chigua-web"
ADMIN_PATH="/var/www/admin"
SPRING_PROFILES="prod,chigua-prod,encryption-prod,tycg1"
DOMAIN="tycg1.com"

# ── 源数据库配置（47.83.18.67）─────────────────────────────────────────────
SRC_DB_HOST="47.83.18.67"
SRC_DB_USER="root"
SRC_DB_PASS="bVR3H7j2czUZ9wV"
SRC_DB_NAME="chigua7"

# ── 新服务器MySQL配置 ────────────────────────────────────────────────────────
NEW_MYSQL_PASS="AdSait##Mysql11@"
NEW_MYSQL_DB="chigua7"
NEW_REDIS_PASS="AdSait##Redis11@"

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
MODE="${1:-all}"

SSH_OPTS="-o StrictHostKeyChecking=no -o PreferredAuthentications=password -o ConnectTimeout=30"
SSH="sshpass -p '${TARGET_PASS}' ssh ${SSH_OPTS} ${TARGET_USER}@${TARGET_IP}"
SCP="sshpass -p '${TARGET_PASS}' scp ${SSH_OPTS}"

# 工具检测
check_deps() {
  for cmd in sshpass mvn npm; do
    command -v "$cmd" &>/dev/null || error "缺少依赖：$cmd，请先安装"
  done
}

# ── 1. 服务器环境初始化 ───────────────────────────────────────────────────────
init_server() {
  step "初始化服务器环境（Java 17 + MySQL 8 + Redis + Nginx）"

  eval "$SSH" << 'REMOTE_INIT'
set -e
export DEBIAN_FRONTEND=noninteractive

echo "=== 系统基础更新 ==="
# 检测包管理器
if command -v dnf &>/dev/null; then
  PKG="dnf"
elif command -v yum &>/dev/null; then
  PKG="yum"
else
  PKG="apt-get"
fi

echo "包管理器: $PKG"

# 安装基础工具
if [ "$PKG" = "apt-get" ]; then
  apt-get update -qq
  apt-get install -y -qq curl wget unzip tar gzip sshpass net-tools
else
  $PKG install -y curl wget unzip tar gzip net-tools epel-release 2>/dev/null || true
fi

# ── 安装 Java 17 ──
echo "=== 安装 Java 17 ==="
if ! java -version 2>/dev/null | grep -q '17\|21'; then
  if [ "$PKG" = "apt-get" ]; then
    apt-get install -y -qq openjdk-17-jdk
  else
    $PKG install -y java-17-openjdk java-17-openjdk-devel 2>/dev/null || \
    $PKG install -y java-11-openjdk java-11-openjdk-devel 2>/dev/null || true
  fi
fi
java -version 2>&1 | head -1
echo "Java 安装完成"

# ── 安装 MySQL 8 ──
echo "=== 安装 MySQL 8 ==="
if ! command -v mysql &>/dev/null; then
  if [ "$PKG" = "apt-get" ]; then
    apt-get install -y -qq mysql-server
  else
    # Alibaba Linux / CentOS
    if ! rpm -qa | grep -q mysql-community; then
      rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2023 2>/dev/null || true
      $PKG install -y mysql-server 2>/dev/null || \
      (wget -q https://dev.mysql.com/get/mysql80-community-release-el7-11.noarch.rpm -O /tmp/mysql-repo.rpm && \
       rpm -ivh /tmp/mysql-repo.rpm && \
       $PKG install -y mysql-community-server) || true
    fi
  fi
fi
REMOTE_INIT

  # 分开执行MySQL配置（因为密码包含特殊字符）
  eval "$SSH" bash -s << REMOTE_MYSQL
set -e
# 启动 MySQL
if systemctl list-units --type=service 2>/dev/null | grep -q mysqld; then
  systemctl enable mysqld && systemctl start mysqld || true
elif systemctl list-units --type=service 2>/dev/null | grep -q mysql; then
  systemctl enable mysql && systemctl start mysql || true
fi
sleep 3

# 获取临时密码（若是第一次安装）
MYSQL_TEMP_PASS=\$(grep 'temporary password' /var/log/mysqld.log 2>/dev/null | awk '{print \$NF}' | tail -1 || echo "")

# 设置root密码并创建数据库
if [ -n "\$MYSQL_TEMP_PASS" ]; then
  mysql --connect-expired-password -uroot -p"\$MYSQL_TEMP_PASS" -e "
    ALTER USER 'root'@'localhost' IDENTIFIED BY '${NEW_MYSQL_PASS}';
    FLUSH PRIVILEGES;
  " 2>/dev/null || true
fi

# 用新密码创建数据库
mysql -uroot -p'${NEW_MYSQL_PASS}' -e "
  CREATE DATABASE IF NOT EXISTS ${NEW_MYSQL_DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  GRANT ALL PRIVILEGES ON ${NEW_MYSQL_DB}.* TO 'root'@'%' IDENTIFIED BY '${NEW_MYSQL_PASS}' WITH GRANT OPTION;
  GRANT ALL PRIVILEGES ON ${NEW_MYSQL_DB}.* TO 'root'@'localhost' IDENTIFIED BY '${NEW_MYSQL_PASS}' WITH GRANT OPTION;
  FLUSH PRIVILEGES;
" 2>/dev/null || \
mysql -uroot -p'${NEW_MYSQL_PASS}' -e "
  CREATE DATABASE IF NOT EXISTS ${NEW_MYSQL_DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '${NEW_MYSQL_PASS}';
  GRANT ALL PRIVILEGES ON ${NEW_MYSQL_DB}.* TO 'root'@'%';
  FLUSH PRIVILEGES;
" 2>/dev/null || true

echo "MySQL 配置完成"
REMOTE_MYSQL

  eval "$SSH" << 'REMOTE_REDIS_NGINX'
set -e
if command -v dnf &>/dev/null; then PKG="dnf"; elif command -v yum &>/dev/null; then PKG="yum"; else PKG="apt-get"; fi

# ── 安装 Redis ──
echo "=== 安装 Redis ==="
if ! command -v redis-server &>/dev/null; then
  if [ "$PKG" = "apt-get" ]; then
    apt-get install -y -qq redis-server
  else
    $PKG install -y redis 2>/dev/null || true
  fi
fi
systemctl enable redis 2>/dev/null || systemctl enable redis-server 2>/dev/null || true
systemctl start redis 2>/dev/null || systemctl start redis-server 2>/dev/null || true
echo "Redis 启动完成"

# ── 安装 Nginx ──
echo "=== 安装 Nginx ==="
if ! command -v nginx &>/dev/null; then
  if [ "$PKG" = "apt-get" ]; then
    apt-get install -y -qq nginx
  else
    $PKG install -y nginx 2>/dev/null || true
  fi
fi
systemctl enable nginx
systemctl start nginx || true

# ── 创建目录结构 ──
mkdir -p /opt/chigua/logs /opt/chigua/backups /opt/chigua/uploadPath
mkdir -p /var/www/chigua-web /var/www/admin /var/www/backups

echo "环境初始化完成！"
REMOTE_REDIS_NGINX

  success "服务器环境初始化完成"
}

# ── 2. 数据库迁移 ─────────────────────────────────────────────────────────────
migrate_db() {
  step "数据库迁移：从 ${SRC_DB_HOST} 迁移到新服务器"

  # 先尝试在新服务器上直接从源库dump
  info "尝试从新服务器连接源数据库 ${SRC_DB_HOST}..."

  eval "$SSH" bash -s << REMOTE_DB
set -e

echo "=== 测试源数据库连接 ==="
if mysql -h ${SRC_DB_HOST} -u${SRC_DB_USER} -p'${SRC_DB_PASS}' --connect-timeout=10 -e "USE ${SRC_DB_NAME}; SELECT 1;" 2>/dev/null; then
  echo "源数据库连接成功，开始导出..."
  mysqldump -h ${SRC_DB_HOST} -u${SRC_DB_USER} -p'${SRC_DB_PASS}' \
    --single-transaction --routines --triggers --events \
    --set-gtid-purged=OFF \
    ${SRC_DB_NAME} > /tmp/chigua_backup.sql 2>/dev/null
  SIZE=\$(du -sh /tmp/chigua_backup.sql | cut -f1)
  echo "数据库导出成功（\${SIZE}）"

  echo "=== 导入到本地数据库 ==="
  mysql -uroot -p'${NEW_MYSQL_PASS}' ${NEW_MYSQL_DB} < /tmp/chigua_backup.sql 2>/dev/null
  rm -f /tmp/chigua_backup.sql
  echo "数据库迁移完成！"
  ROWS=\$(mysql -uroot -p'${NEW_MYSQL_PASS}' ${NEW_MYSQL_DB} -se "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${NEW_MYSQL_DB}';" 2>/dev/null || echo 0)
  echo "已导入表数量: \${ROWS}"
else
  echo "WARN: 无法直接连接源数据库，将使用项目SQL文件初始化..."
  echo "FALLBACK_NEEDED"
fi
REMOTE_DB

  # 检查是否需要回退方案（上传本地SQL文件）
  RESULT=$(eval "$SSH" "mysql -uroot -p'${NEW_MYSQL_PASS}' ${NEW_MYSQL_DB} -se \"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${NEW_MYSQL_DB}';\" 2>/dev/null || echo 0")
  if [ "${RESULT:-0}" -lt 10 ] 2>/dev/null; then
    warn "数据库表数量不足（${RESULT}），上传SQL初始化文件..."
    
    # 上传主SQL文件
    SQL_DIR="$REPO_ROOT/backend/sql"
    if [ -f "$SQL_DIR/chigua.sql" ]; then
      $SCP "$SQL_DIR/chigua.sql" "${TARGET_USER}@${TARGET_IP}:/tmp/chigua_init.sql"
      eval "$SSH" "mysql -uroot -p'${NEW_MYSQL_PASS}' ${NEW_MYSQL_DB} < /tmp/chigua_init.sql 2>/dev/null && rm -f /tmp/chigua_init.sql"
      success "主数据库Schema初始化完成"
    fi

    # 上传其他SQL文件（按顺序）
    for sql_file in quartz.sql tg_tables.sql user_behavior_log.sql video_keys.sql; do
      if [ -f "$SQL_DIR/$sql_file" ]; then
        $SCP "$SQL_DIR/$sql_file" "${TARGET_USER}@${TARGET_IP}:/tmp/${sql_file}"
        eval "$SSH" "mysql -uroot -p'${NEW_MYSQL_PASS}' ${NEW_MYSQL_DB} < /tmp/${sql_file} 2>/dev/null && rm -f /tmp/${sql_file}" || true
        info "  已执行: ${sql_file}"
      fi
    done
  fi

  success "数据库迁移完成"
}

# ── 3. 构建后端 ───────────────────────────────────────────────────────────────
build_backend() {
  step "后端：Maven 构建（profiles: ${SPRING_PROFILES}）"
  cd "$REPO_ROOT/backend"
  mvn clean package -Dmaven.test.skip=true -q
  JAR="$REPO_ROOT/backend/ruoyi-admin/target/ruoyi-admin.jar"
  [ -f "$JAR" ] || error "JAR 构建失败"
  SIZE=$(du -sh "$JAR" | cut -f1)
  success "JAR 构建成功（$SIZE）"
}

# ── 4. 部署后端 ───────────────────────────────────────────────────────────────
deploy_backend() {
  step "后端：上传 JAR 到 ${TARGET_IP}"
  JAR="$REPO_ROOT/backend/ruoyi-admin/target/ruoyi-admin.jar"
  [ -f "$JAR" ] || error "JAR 不存在，请先构建"

  $SCP "$JAR" "${TARGET_USER}@${TARGET_IP}:${DEPLOY_PATH}/ruoyi-admin-new.jar"
  success "JAR 上传完成"

  step "后端：配置并启动服务"
  eval "$SSH" bash -s << REMOTE_BACKEND
set -e
cd ${DEPLOY_PATH}

# 备份旧版本
if [ -f ruoyi-admin.jar ]; then
  cp ruoyi-admin.jar backups/ruoyi-admin.jar.\$(date +%Y%m%d_%H%M%S)
  ls -t backups/ruoyi-admin.jar.* 2>/dev/null | tail -n +6 | xargs -r rm
fi

# 停止旧服务
if pkill -f "java.*ruoyi-admin" 2>/dev/null; then
  echo "旧服务已停止"
  sleep 5
fi

mv ruoyi-admin-new.jar ruoyi-admin.jar

# 写入应用配置（覆盖数据库和Redis连接）
cat > ${DEPLOY_PATH}/application-server.yml << 'APPCONF'
spring:
  datasource:
    druid:
      master:
        url: jdbc:mysql://127.0.0.1:3306/${NEW_MYSQL_DB}?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: ${NEW_MYSQL_PASS}
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
    password: ""
    timeout: 10s
    lettuce:
      pool:
        min-idle: 5
        max-idle: 20
        max-active: 50
        max-wait: -1ms
APPCONF

# 启动服务
nohup java -server -Xms512m -Xmx1024m \
  -Dspring.profiles.active=${SPRING_PROFILES} \
  -Dspring.config.additional-location=file:${DEPLOY_PATH}/application-server.yml \
  -Duser.timezone=Asia/Shanghai \
  -Dfile.encoding=UTF-8 \
  -jar ruoyi-admin.jar > logs/application.log 2>&1 &

echo "服务启动中 (PID: \$!)"

# 等待启动
for i in \$(seq 1 90); do
  sleep 2
  if ss -tlnp 2>/dev/null | grep -q ':8080' || netstat -tlnp 2>/dev/null | grep -q ':8080'; then
    echo "✓ 后端服务就绪（\${i}*2秒）"
    break
  fi
  if [ \$i -eq 45 ]; then
    echo "启动超时，查看日志:"
    tail -30 logs/application.log
    exit 1
  fi
done
REMOTE_BACKEND

  success "后端部署完成"
}

# ── 5. 构建前端 ───────────────────────────────────────────────────────────────
build_react() {
  step "React chigua-web：npm 构建"
  cd "$REPO_ROOT/chigua-web"
  npm install --legacy-peer-deps --silent
  npm run build
  [ -d build ] || error "React build 目录不存在"
  COUNT=$(find build -type f | wc -l | tr -d ' ')
  success "React 构建成功（$COUNT 个文件）"
}

build_vue() {
  step "Vue Admin：npm 构建"
  cd "$REPO_ROOT/frontend"
  npm install --legacy-peer-deps --silent
  npm run build:prod
  [ -d dist ] || error "Vue Admin dist 目录不存在"
  COUNT=$(find dist -type f | wc -l | tr -d ' ')
  success "Vue Admin 构建成功（$COUNT 个文件）"
}

# ── 6. 部署前端 ───────────────────────────────────────────────────────────────
deploy_react() {
  step "React：部署到 ${TARGET_IP}"
  eval "$SSH" "rm -rf ${REACT_PATH}/* && mkdir -p ${REACT_PATH}"
  $SCP -r "$REPO_ROOT/chigua-web/build/"* "${TARGET_USER}@${TARGET_IP}:${REACT_PATH}/"
  success "React 前端部署完成"
}

deploy_vue() {
  step "Vue Admin：部署到 ${TARGET_IP}"
  eval "$SSH" "rm -rf ${ADMIN_PATH}/* && mkdir -p ${ADMIN_PATH}"
  $SCP -r "$REPO_ROOT/frontend/dist/"* "${TARGET_USER}@${TARGET_IP}:${ADMIN_PATH}/"
  success "Vue Admin 部署完成"
}

# ── 7. 配置 Nginx ─────────────────────────────────────────────────────────────
setup_nginx() {
  step "配置 Nginx（域名: ${DOMAIN}）"

  eval "$SSH" bash -s << REMOTE_NGINX
set -e

# 查找Nginx配置目录
if [ -d /etc/nginx/conf.d ]; then
  NGINX_CONF_DIR=/etc/nginx/conf.d
elif [ -d /etc/nginx/sites-enabled ]; then
  NGINX_CONF_DIR=/etc/nginx/sites-enabled
else
  mkdir -p /etc/nginx/conf.d
  NGINX_CONF_DIR=/etc/nginx/conf.d
fi

# 生成 Nginx 配置
cat > \${NGINX_CONF_DIR}/tycg1.conf << 'NGINXCONF'
# tycg1.com - 主站 (React 前端 + API代理)
server {
    listen 80;
    server_name tycg1.com www.tycg1.com ${TARGET_IP};
    
    charset utf-8;
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # React 前端
    location / {
        root /var/www/chigua-web;
        index index.html;
        try_files \$uri \$uri/ /index.html;
        
        # 静态资源缓存
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2|woff|ttf)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    # 后端 API 代理
    location /prod-api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host \$http_host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        proxy_buffering off;
        client_max_body_size 100m;
    }

    # WebSocket 代理
    location /websocket/ {
        proxy_pass http://127.0.0.1:8080/websocket/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_read_timeout 3600s;
    }

    # 文件上传路径
    location /uploadPath/ {
        alias /opt/chigua/uploadPath/;
        expires 7d;
    }
    
    # 站点地图和SEO
    location /sitemap.xml {
        proxy_pass http://127.0.0.1:8080/sitemap.xml;
        proxy_set_header Host \$http_host;
    }
    
    location /robots.txt {
        proxy_pass http://127.0.0.1:8080/robots.txt;
        proxy_set_header Host \$http_host;
    }
}

# Vue Admin 管理后台 (HTTPS 444端口)
server {
    listen 444 ssl;
    server_name tycg1.com www.tycg1.com ${TARGET_IP};

    # SSL证书（先用自签名，后续替换为正式证书）
    ssl_certificate     /etc/nginx/ssl/tycg1.crt;
    ssl_certificate_key /etc/nginx/ssl/tycg1.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    charset utf-8;
    
    location / {
        root /var/www/admin;
        index index.html;
        try_files \$uri \$uri/ /index.html;
        
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2)$ {
            expires 30d;
        }
    }

    location /prod-api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host \$http_host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        client_max_body_size 100m;
    }
}
NGINXCONF

# 生成自签名SSL证书（后续可替换为Let's Encrypt正式证书）
mkdir -p /etc/nginx/ssl
if [ ! -f /etc/nginx/ssl/tycg1.crt ]; then
  openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
    -keyout /etc/nginx/ssl/tycg1.key \
    -out /etc/nginx/ssl/tycg1.crt \
    -subj "/C=CN/ST=HK/L=HongKong/O=tycg1/CN=tycg1.com" 2>/dev/null
  echo "自签名SSL证书已生成"
fi

# 删除默认配置
rm -f /etc/nginx/conf.d/default.conf /etc/nginx/sites-enabled/default 2>/dev/null || true

# 测试配置
nginx -t && systemctl reload nginx
echo "Nginx 配置完成并已重载"
REMOTE_NGINX

  success "Nginx 配置完成"
}

# ── 8. 设置自动备份 ───────────────────────────────────────────────────────────
setup_backup() {
  step "配置自动备份"

  eval "$SSH" bash -s << REMOTE_BACKUP
set -e
mkdir -p /opt/backups/db /opt/backups/app

# 创建数据库备份脚本
cat > /opt/backups/backup-db.sh << 'BACKUPSCRIPT'
#!/bin/bash
DATE=\$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/opt/backups/db

mkdir -p \$BACKUP_DIR
mysqldump -uroot -p'${NEW_MYSQL_PASS}' \
  --single-transaction --routines --triggers \
  ${NEW_MYSQL_DB} | gzip > \${BACKUP_DIR}/chigua7_\${DATE}.sql.gz

# 只保留最近14天的备份
find \$BACKUP_DIR -name "*.sql.gz" -mtime +14 -delete
echo "[\$(date)] 数据库备份完成: chigua7_\${DATE}.sql.gz" >> /opt/backups/backup.log
BACKUPSCRIPT

chmod +x /opt/backups/backup-db.sh

# 添加 crontab 每天凌晨2点执行备份
(crontab -l 2>/dev/null | grep -v 'backup-db.sh'; echo "0 2 * * * /opt/backups/backup-db.sh") | crontab -

echo "自动备份已配置（每天凌晨2点执行）"

# 立即执行一次备份
/opt/backups/backup-db.sh && echo "首次备份完成"
REMOTE_BACKUP

  success "自动备份配置完成"
}

# ── 9. 健康检查 ───────────────────────────────────────────────────────────────
health_check() {
  step "健康检查"
  sleep 5
  
  B=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 "http://${TARGET_IP}:8080/login" 2>/dev/null || echo "ERR")
  R=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 "http://${TARGET_IP}" 2>/dev/null || echo "ERR")
  A=$(curl -sk -o /dev/null -w "%{http_code}" --connect-timeout 10 "https://${TARGET_IP}:444" 2>/dev/null || echo "ERR")

  echo ""
  echo -e "  后端 API  :8080  → HTTP ${B}"
  echo -e "  React 前端  :80   → HTTP ${R}"
  echo -e "  Vue Admin :444  → HTTP ${A}"
  echo ""

  [[ "$B" =~ ^(200|302)$ ]] && success "后端正常" || warn "后端异常（${B}），请检查日志：ssh root@${TARGET_IP} 'tail -50 /opt/chigua/logs/application.log'"
  [[ "$R" =~ ^(200|301|302)$ ]] && success "React 前端正常" || warn "React 前端异常（${R}）"
  [[ "$A" =~ ^(200|301|302)$ ]] && success "Vue Admin 正常" || warn "Vue Admin 异常（${A}）"
  
  echo ""
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BOLD}部署地址汇总：${NC}"
  echo -e "  主站:   http://${TARGET_IP}  (域名: http://${DOMAIN})"
  echo -e "  后台:   https://${TARGET_IP}:444"
  echo -e "  API:    http://${TARGET_IP}:8080"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
  echo -e "${YELLOW}[提示] 域名解析：将 ${DOMAIN} 的 A 记录指向 ${TARGET_IP}${NC}"
  echo -e "${YELLOW}[提示] 申请SSL证书后替换 /etc/nginx/ssl/ 目录下的证书文件${NC}"
}

# ── 时间统计 ──────────────────────────────────────────────────────────────────
START_TIME=$(date +%s)
elapsed() {
  local S=$(($(date +%s) - START_TIME))
  printf "%dm%02ds" $((S/60)) $((S%60))
}

# ── 主流程 ────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║   tycg1.com 部署脚本 → ${TARGET_IP}   ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════════════╝${NC}\n"

check_deps

case "$MODE" in
  all)
    init_server
    migrate_db
    build_backend & PID_B=$!
    build_vue     & PID_V=$!
    build_react   & PID_R=$!
    info "三端并行构建中，请稍候..."
    wait $PID_B || error "后端构建失败"
    wait $PID_V || error "Vue Admin 构建失败"
    wait $PID_R || error "React 构建失败"
    deploy_backend
    deploy_vue
    deploy_react
    setup_nginx
    setup_backup
    ;;
  init)
    init_server
    ;;
  db)
    migrate_db
    ;;
  backend)
    build_backend
    deploy_backend
    ;;
  vue)
    build_vue
    deploy_vue
    ;;
  react)
    build_react
    deploy_react
    ;;
  frontend)
    build_vue   & PID_V=$!
    build_react & PID_R=$!
    info "前端并行构建中..."
    wait $PID_V || error "Vue Admin 构建失败"
    wait $PID_R || error "React 构建失败"
    deploy_vue
    deploy_react
    ;;
  nginx)
    setup_nginx
    ;;
  backup)
    setup_backup
    ;;
  check)
    health_check
    exit 0
    ;;
  *)
    error "未知参数：$MODE\n用法: ./deploy-tycg1.sh [all|init|db|backend|vue|react|frontend|nginx|backup|check]"
    ;;
esac

health_check

echo -e "\n${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║  ✓  部署完成！耗时 $(elapsed)                ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}\n"
