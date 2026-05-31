#!/usr/bin/env bash
# chigua-web 增量部署脚本（rsync + nginx 零停机）
# 用法：
#   ./scripts/deploy-web.sh         # 默认：构建 + 部署 + 校验
#   ./scripts/deploy-web.sh nobuild # 不构建，仅同步当前 build/
#
# 优势：
#   - 增量传输：只上传变化的文件（CSS/JS hash 不变就跳过）
#   - --delete-after 部署成功后再清理旧 chunk，避免短暂 404
#   - nginx 直接读 /var/www/chigua-web，无需 reload/restart

set -euo pipefail

SSH_PASS="${SSH_PASS:-bVR3H7j2czUZ9wV}"
SSH_USER="${SSH_USER:-root}"
SSH_HOST="${SSH_HOST:-47.83.18.67}"
SSH_PORT="${SSH_PORT:-22}"
REMOTE_DIR="${REMOTE_DIR:-/var/www/chigua-web/}"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WEB_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
BUILD_DIR="$WEB_DIR/build"

cyan()  { printf "\033[36m%s\033[0m\n" "$1"; }
green() { printf "\033[32m%s\033[0m\n" "$1"; }
red()   { printf "\033[31m%s\033[0m\n" "$1"; }

if [[ "${1:-}" != "nobuild" ]]; then
  cyan ">>> [1/3] 本地构建 chigua-web ..."
  cd "$WEB_DIR"
  npm run build | tail -8
fi

if [[ ! -d "$BUILD_DIR" ]]; then
  red "❌ build 目录不存在：$BUILD_DIR"
  exit 1
fi

cyan ">>> [2/3] rsync 增量同步到 ${SSH_HOST}:${REMOTE_DIR} ..."

# 关键参数：
#   -a 保留属性  -z 压缩传输（CDN 上游加速）
#   --delete-after 等所有新文件就位后再删除服务器上多余的旧文件（避免短暂 404）
#   --info=stats2 输出"传输 N 个文件"汇总
#   ssh ConnectTimeout 防止 SSH 卡住
SSHPASS="$SSH_PASS" sshpass -e rsync \
  -az --delete-after \
  --info=stats2 \
  -e "ssh -p ${SSH_PORT} -o StrictHostKeyChecking=no -o ConnectTimeout=60 -o ServerAliveInterval=30" \
  "${BUILD_DIR}/" \
  "${SSH_USER}@${SSH_HOST}:${REMOTE_DIR}"

cyan ">>> [3/3] 校验远端 main.js 文件名"
REMOTE_MAIN=$(SSHPASS="$SSH_PASS" sshpass -e ssh -p "${SSH_PORT}" \
  -o StrictHostKeyChecking=no -o ConnectTimeout=30 \
  "${SSH_USER}@${SSH_HOST}" \
  "ls /var/www/chigua-web/static/js/main.*.js 2>/dev/null | head -1")
LOCAL_MAIN=$(ls "$BUILD_DIR"/static/js/main.*.js 2>/dev/null | head -1 | xargs basename)

if [[ "$REMOTE_MAIN" == *"$LOCAL_MAIN" ]]; then
  green "✅ 部署成功"
  green "   本地: $LOCAL_MAIN"
  green "   远端: $REMOTE_MAIN"
  green "   nginx 无需重启，刷新页面即可（注意 Cloudflare 边缘缓存可能需 1-2 分钟）"
else
  red "⚠️  远端 main.js 与本地不一致："
  red "   本地: $LOCAL_MAIN"
  red "   远端: $REMOTE_MAIN"
  exit 2
fi
