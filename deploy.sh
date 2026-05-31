#!/usr/bin/env bash
# =============================================================================
# 一键同步部署脚本 - 47.83.18.67
# 用法:
#   ./deploy.sh              # 全量部署（后端 + 两个前端）
#   ./deploy.sh backend      # 只部署后端
#   ./deploy.sh vue          # 只部署 Vue 管理后台
#   ./deploy.sh react        # 只部署 React 用户前端
#   ./deploy.sh frontend     # 只部署两个前端
#   ./deploy.sh db           # 只同步 TOTP 等数据库变更
# =============================================================================

set -euo pipefail

# ── 颜色输出 ─────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step()    { echo -e "\n${CYAN}${BOLD}▶ $*${NC}"; }

# ── 配置 ─────────────────────────────────────────────────────────────────────
SERVER_IP="47.83.18.67"
SERVER_USER="root"
SERVER_PASS="bVR3H7j2czUZ9wV"
DEPLOY_PATH="/opt/chigua"
REACT_PATH="/var/www/chigua-web"
ADMIN_PATH="/var/www/admin"
SPRING_PROFILES="dev,chigua-dev,encryption-dev"

MYSQL_USER="root"
MYSQL_PASS="kakamysql!"
MYSQL_DB="chigua7"

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
MODE="${1:-all}"

# SSH/SCP 公共参数
SSH_OPTS="-o StrictHostKeyChecking=no -o PreferredAuthentications=password"
SSH="sshpass -p ${SERVER_PASS} ssh ${SSH_OPTS} ${SERVER_USER}@${SERVER_IP}"
SCP="sshpass -p ${SERVER_PASS} scp ${SSH_OPTS}"

# ── 工具检测 ──────────────────────────────────────────────────────────────────
check_deps() {
  for cmd in sshpass mvn npm git; do
    command -v "$cmd" &>/dev/null || error "缺少依赖：$cmd，请先安装"
  done
}

# ── Git 提交 & 推送 ───────────────────────────────────────────────────────────
git_push() {
  step "Git：检查并推送到 GitHub"
  cd "$REPO_ROOT"

  if git diff --quiet && git diff --cached --quiet; then
    info "无本地改动，跳过 commit"
  else
    git add -A
    echo -e "${YELLOW}请输入 commit 信息（直接回车使用默认）：${NC}"
    read -r MSG
    MSG="${MSG:-deploy: $(date '+%Y-%m-%d %H:%M:%S')}"
    git commit -m "$MSG"
    success "已提交：$MSG"
  fi

  git push origin master
  success "已推送到 GitHub"
}

# ── 后端：Maven 构建 ──────────────────────────────────────────────────────────
build_backend() {
  step "后端：Maven 构建"
  cd "$REPO_ROOT/backend"
  mvn clean package -Dmaven.test.skip=true -q
  JAR="$REPO_ROOT/backend/ruoyi-admin/target/ruoyi-admin.jar"
  [ -f "$JAR" ] || error "JAR 构建失败，文件不存在"
  SIZE=$(du -sh "$JAR" | cut -f1)
  success "JAR 构建成功（$SIZE）：$JAR"
}

# ── 后端：上传 & 重启 ─────────────────────────────────────────────────────────
deploy_backend() {
  step "后端：上传 JAR 到服务器"
  JAR="$REPO_ROOT/backend/ruoyi-admin/target/ruoyi-admin.jar"
  [ -f "$JAR" ] || error "JAR 不存在，请先执行构建"

  $SCP "$JAR" "${SERVER_USER}@${SERVER_IP}:${DEPLOY_PATH}/ruoyi-admin-new.jar"
  success "JAR 上传完成"

  step "后端：备份旧版本 & 重启服务"
  $SSH << REMOTE
set -e
cd ${DEPLOY_PATH}

if [ -f ruoyi-admin.jar ]; then
  cp ruoyi-admin.jar backups/ruoyi-admin.jar.\$(date +%Y%m%d_%H%M%S)
  ls -t backups/ruoyi-admin.jar.* 2>/dev/null | tail -n +6 | xargs -r rm
  echo "  已备份，保留最近5个版本"
fi

if pkill -f "java.*ruoyi-admin" 2>/dev/null; then
  echo "  旧服务已停止"
  sleep 5
else
  echo "  无运行中的旧服务"
fi

mv ruoyi-admin-new.jar ruoyi-admin.jar

nohup java -server -Xms512m -Xmx1024m \
  -Dspring.profiles.active=${SPRING_PROFILES} \
  -Duser.timezone=Asia/Shanghai \
  -Dfile.encoding=UTF-8 \
  -jar ruoyi-admin.jar > logs/application.log 2>&1 &

echo "  服务启动中 (PID: \$!)"

for i in \$(seq 1 90); do
  sleep 1
  if ss -tlnp 2>/dev/null | grep -q ':8080'; then
    echo "  后端服务就绪（\${i}秒）"
    break
  fi
  [ \$i -eq 90 ] && { echo "  启动超时！"; tail -30 logs/application.log; exit 1; }
done
REMOTE
  success "后端部署完成"
}

# ── Vue Admin：构建 ───────────────────────────────────────────────────────────
build_vue() {
  step "Vue Admin：npm 构建"
  cd "$REPO_ROOT/frontend"
  npm install --legacy-peer-deps --silent
  npm run build:prod
  [ -d dist ] || error "Vue Admin dist 目录不存在，构建失败"
  COUNT=$(find dist -type f | wc -l | tr -d ' ')
  success "Vue Admin 构建成功（$COUNT 个文件）"
}

# ── Vue Admin：上传 ───────────────────────────────────────────────────────────
deploy_vue() {
  step "Vue Admin：备份旧版本"
  $SSH "mkdir -p /var/www/backups && \
    [ \"\$(ls -A ${ADMIN_PATH} 2>/dev/null)\" ] && \
    tar -czf /var/www/backups/admin-\$(date +%Y%m%d_%H%M%S).tar.gz -C ${ADMIN_PATH} . && \
    ls -t /var/www/backups/admin-*.tar.gz 2>/dev/null | tail -n +4 | xargs -r rm && \
    echo '  已备份，保留最近3个' || echo '  无需备份'" 2>/dev/null || true

  $SSH "rm -rf ${ADMIN_PATH}/* && mkdir -p ${ADMIN_PATH}"

  step "Vue Admin：上传 dist"
  $SCP -r "$REPO_ROOT/frontend/dist/"* "${SERVER_USER}@${SERVER_IP}:${ADMIN_PATH}/"
  success "Vue Admin 部署完成 → https://${SERVER_IP}:444"
}

# ── React chigua-web：构建 ────────────────────────────────────────────────────
build_react() {
  step "React chigua-web：npm 构建"
  cd "$REPO_ROOT/chigua-web"
  npm install --legacy-peer-deps --silent
  npm run build
  [ -d build ] || error "chigua-web build 目录不存在，构建失败"
  COUNT=$(find build -type f | wc -l | tr -d ' ')
  success "React 构建成功（$COUNT 个文件）"
}

# ── React chigua-web：上传 ────────────────────────────────────────────────────
deploy_react() {
  step "React chigua-web：备份旧版本"
  $SSH "mkdir -p /var/www/backups && \
    [ \"\$(ls -A ${REACT_PATH} 2>/dev/null)\" ] && \
    tar -czf /var/www/backups/chigua-web-\$(date +%Y%m%d_%H%M%S).tar.gz -C ${REACT_PATH} . && \
    ls -t /var/www/backups/chigua-web-*.tar.gz 2>/dev/null | tail -n +4 | xargs -r rm && \
    echo '  已备份，保留最近3个' || echo '  无需备份'" 2>/dev/null || true

  $SSH "rm -rf ${REACT_PATH}/* && mkdir -p ${REACT_PATH}"

  step "React chigua-web：上传 build"
  $SCP -r "$REPO_ROOT/chigua-web/build/"* "${SERVER_USER}@${SERVER_IP}:${REACT_PATH}/"
  success "React 部署完成 → http://${SERVER_IP}"
}

# ── 数据库同步 ────────────────────────────────────────────────────────────────
sync_db() {
  step "数据库：同步 TOTP 绑定数据"
  LOCAL_ROWS=$(mysql -u root -pkakamysql! ${MYSQL_DB} \
    -se "SELECT user_id, user_name, totp_secret, totp_enabled FROM sys_user WHERE totp_enabled=1 OR totp_secret IS NOT NULL;" 2>/dev/null)

  if [ -z "$LOCAL_ROWS" ]; then
    warn "本地无 TOTP 绑定数据，跳过同步"
    return
  fi

  info "本地 TOTP 数据：\n$LOCAL_ROWS"

  # 生成 UPDATE SQL
  SQL_FILE=$(mktemp /tmp/sync_totp_XXXX.sql)
  mysql -u root -pkakamysql! ${MYSQL_DB} \
    -se "SELECT CONCAT('UPDATE sys_user SET totp_secret=', QUOTE(totp_secret), ', totp_enabled=', totp_enabled, ' WHERE user_name=', QUOTE(user_name), ';') FROM sys_user WHERE totp_enabled=1 OR totp_secret IS NOT NULL;" \
    2>/dev/null > "$SQL_FILE"

  cat "$SQL_FILE"
  $SCP "$SQL_FILE" "${SERVER_USER}@${SERVER_IP}:/tmp/sync_totp.sql"
  $SSH "mysql -u ${MYSQL_USER} -p${MYSQL_PASS} ${MYSQL_DB} < /tmp/sync_totp.sql 2>/dev/null && rm -f /tmp/sync_totp.sql"
  rm -f "$SQL_FILE"
  success "TOTP 数据同步完成"
}

# ── 健康检查 ──────────────────────────────────────────────────────────────────
health_check() {
  step "健康检查"
  sleep 3
  B=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "http://${SERVER_IP}:8080/login" 2>/dev/null || echo "ERR")
  R=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "http://${SERVER_IP}" 2>/dev/null || echo "ERR")
  A=$(curl -sk -o /dev/null -w "%{http_code}" --connect-timeout 5 "https://${SERVER_IP}:444" 2>/dev/null || echo "ERR")

  echo -e "  后端 API  :8080  → HTTP ${B}"
  echo -e "  React 前端  :80   → HTTP ${R}"
  echo -e "  Vue Admin :444  → HTTP ${A}"

  [[ "$B" =~ ^(200|302)$ ]] && success "后端正常" || warn "后端异常（$B），请检查日志"
  [[ "$R" =~ ^(200|301|302)$ ]] && success "React 前端正常" || warn "React 前端异常（$R）"
  [[ "$A" =~ ^(200|301|302)$ ]] && success "Vue Admin 正常" || warn "Vue Admin 异常（$A）"
}

# ── 时间统计 ──────────────────────────────────────────────────────────────────
START_TIME=$(date +%s)
elapsed() {
  local S=$(($(date +%s) - START_TIME))
  printf "%dm%02ds" $((S/60)) $((S%60))
}

# ── 主流程 ────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║    一键部署脚本  →  ${SERVER_IP}    ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════════╝${NC}\n"

check_deps
git_push

case "$MODE" in
  all)
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
  db)
    sync_db
    ;;
  *)
    error "未知参数：$MODE\n用法: ./deploy.sh [all|backend|vue|react|frontend|db]"
    ;;
esac

health_check

echo -e "\n${BOLD}${GREEN}╔══════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║  ✓  部署完成！耗时 $(elapsed)              ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════╝${NC}\n"
