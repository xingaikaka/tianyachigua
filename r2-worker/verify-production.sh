#!/bin/bash

# R2 Worker 生产环境验证脚本
# 用于快速检查系统状态和功能

WORKER_URL="https://chigua-r2-worker.xingaikaka.workers.dev"

echo "=========================================="
echo "R2 Worker 生产环境验证"
echo "=========================================="
echo "Worker URL: $WORKER_URL"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 测试计数
PASSED=0
FAILED=0

# 测试函数
test_endpoint() {
    local name=$1
    local url=$2
    local expected=$3
    
    echo -n "测试 $name... "
    
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    
    if [ "$RESPONSE" == "$expected" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $RESPONSE)"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗ 失败${NC} (期望 $expected, 得到 $RESPONSE)"
        ((FAILED++))
        return 1
    fi
}

# 1. 健康检查
echo "=== 基础功能测试 ==="
test_endpoint "健康检查" "$WORKER_URL/" "200"
test_endpoint "缓存统计API" "$WORKER_URL/api/cache/stats" "200"
echo ""

# 2. 功能测试
echo "=== 核心功能测试 ==="

# 文件上传测试
echo -n "测试 文件上传... "
echo "Test file $(date)" > /tmp/verify-test.txt
UPLOAD_RESPONSE=$(curl -s -X POST "$WORKER_URL/api/upload" \
  -F "file=@/tmp/verify-test.txt" \
  -F "type=document" 2>/dev/null)

if echo "$UPLOAD_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✓ 通过${NC}"
    ((PASSED++))
    
    # 提取文件URL
    PREVIEW_URL=$(echo "$UPLOAD_RESPONSE" | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('data', {}).get('previewUrl', ''))" 2>/dev/null)
    
    if [ -n "$PREVIEW_URL" ]; then
        # 测试文件访问
        echo -n "测试 文件访问... "
        FILE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$PREVIEW_URL" 2>/dev/null)
        if [ "$FILE_RESPONSE" == "200" ]; then
            echo -e "${GREEN}✓ 通过${NC} (HTTP $FILE_RESPONSE)"
            ((PASSED++))
        else
            echo -e "${RED}✗ 失败${NC} (HTTP $FILE_RESPONSE)"
            ((FAILED++))
        fi
    fi
else
    echo -e "${RED}✗ 失败${NC}"
    ((FAILED++))
fi
echo ""

# 3. 缓存功能测试
echo "=== CDN缓存测试 ==="
echo -n "测试 缓存统计数据... "
CACHE_STATS=$(curl -s "$WORKER_URL/api/cache/stats" 2>/dev/null)

if echo "$CACHE_STATS" | grep -q '"cacheEnabled":true'; then
    echo -e "${GREEN}✓ 缓存已启用${NC}"
    ((PASSED++))
    
    # 显示缓存统计
    echo "$CACHE_STATS" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    s = d.get('stats', {})
    print('  命中率: {}'.format(s.get('hitRate', 'N/A')))
    print('  命中: {}'.format(s.get('hits', 0)))
    print('  未命中: {}'.format(s.get('misses', 0)))
    print('  节省: {}'.format(s.get('costSavings', 'N/A')))
except:
    pass
" 2>/dev/null
else
    echo -e "${YELLOW}⚠ 缓存未启用或数据异常${NC}"
fi
echo ""

# 4. 总结
echo "=========================================="
echo "测试总结"
echo "=========================================="
echo -e "通过: ${GREEN}$PASSED${NC}"
echo -e "失败: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ 所有测试通过！系统运行正常${NC}"
    exit 0
else
    echo -e "${RED}⚠️ 有 $FAILED 个测试失败，请检查系统${NC}"
    exit 1
fi

