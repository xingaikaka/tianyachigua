#!/bin/bash

# RedGifs 统一API测试脚本
# 测试chigua-web和51chigua-tycg使用的所有API端点

echo "========================================"
echo "🧪 RedGifs 统一API测试"
echo "========================================"
echo ""

BACKEND_URL="http://localhost:8080"

# 颜色代码
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

test_count=0
success_count=0
fail_count=0

# 测试函数
test_api() {
  local name="$1"
  local method="$2"
  local endpoint="$3"
  local data="$4"
  
  test_count=$((test_count + 1))
  echo -e "${BLUE}测试 $test_count: $name${NC}"
  echo "  方法: $method"
  echo "  端点: $endpoint"
  
  if [ "$method" = "GET" ]; then
    response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL$endpoint")
  else
    response=$(curl -s -w "\n%{http_code}" -X "$method" \
      -H "Content-Type: application/json" \
      -d "$data" \
      "$BACKEND_URL$endpoint")
  fi
  
  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')
  
  if [ "$http_code" = "200" ]; then
    code=$(echo "$body" | grep -o '"code":[0-9]*' | head -1 | cut -d':' -f2)
    if [ "$code" = "200" ] || [ -z "$code" ]; then
      echo -e "  ${GREEN}✅ 成功 (HTTP $http_code)${NC}"
      success_count=$((success_count + 1))
    else
      echo -e "  ${RED}❌ 业务失败 (HTTP $http_code, code: $code)${NC}"
      echo "  响应: $(echo "$body" | head -c 200)"
      fail_count=$((fail_count + 1))
    fi
  else
    echo -e "  ${RED}❌ HTTP失败 (HTTP $http_code)${NC}"
    echo "  响应: $(echo "$body" | head -c 200)"
    fail_count=$((fail_count + 1))
  fi
  echo ""
}

echo "========================================"
echo "📱 chigua-web 使用的API"
echo "========================================"
echo ""

# 1. 获取用户列表（分页）
test_api \
  "获取用户列表（chigua-web用户列表页）" \
  "GET" \
  "/api/redgifs/users/list?pageNum=1&pageSize=20&status=1" \
  ""

# 2. 根据用户名获取用户详情
test_api \
  "根据用户名获取用户详情（chigua-web用户详情页）" \
  "GET" \
  "/api/redgifs/users/username/its_not_gay" \
  ""

# 3. 获取用户的视频列表（新增）
test_api \
  "获取用户视频列表（chigua-web用户详情页）" \
  "GET" \
  "/api/redgifs/users/2/videos?pageNum=1&pageSize=20" \
  ""

echo "========================================"
echo "🖥️  51chigua-tycg 使用的API"
echo "========================================"
echo ""

# 4. 批量检查用户是否存在
test_api \
  "批量检查用户存在（桌面端列表加载）" \
  "POST" \
  "/api/redgifs/users/check" \
  '{"usernames":["its_not_gay","test_user","nonexistent_user"]}'

# 5. 入库用户信息
test_api \
  "入库用户信息（桌面端同步）" \
  "POST" \
  "/api/redgifs/users/ingest" \
  "{
    \"username\": \"test_unified_api_$(date +%s)\",
    \"name\": \"统一API测试用户\",
    \"description\": \"这是统一API测试\",
    \"profileImageUrl\": \"https://example.com/avatar.jpg\",
    \"followers\": 100,
    \"gifsCount\": 10,
    \"verified\": 0
  }"

# 6. 更新用户同步状态
test_api \
  "更新用户同步状态（桌面端同步完成）" \
  "POST" \
  "/api/redgifs/users/update-sync-status" \
  '{"userId": 2, "syncStatus": 2}'

# 7. 批量检查视频是否存在
test_api \
  "批量检查视频存在（桌面端同步前）" \
  "POST" \
  "/api/redgifs/videos/check-batch" \
  '{"gifIds":["testgif123","testgif456"]}'

# 8. 入库视频信息
test_api \
  "入库视频信息（桌面端同步）" \
  "POST" \
  "/api/redgifs/videos/ingest" \
  "{
    \"gifId\": \"test_unified_$(date +%s)\",
    \"userId\": 2,
    \"videoUrl\": \"https://r2.example.com/test.mp4\",
    \"posterUrl\": \"https://r2.example.com/poster.jpg\",
    \"thumbnailUrl\": \"https://r2.example.com/thumb.jpg\",
    \"duration\": 15.5,
    \"width\": 1920,
    \"height\": 1080,
    \"likes\": 100,
    \"views\": 1000
  }"

echo "========================================"
echo "📊 测试结果汇总"
echo "========================================"
echo ""
echo "  总测试数: $test_count"
echo -e "  ${GREEN}成功: $success_count${NC}"
echo -e "  ${RED}失败: $fail_count${NC}"
echo ""

if [ $fail_count -eq 0 ]; then
  echo -e "${GREEN}✅ 所有测试通过！chigua-web和51chigua-tycg都可以正常使用！${NC}"
  exit 0
else
  echo -e "${RED}❌ 有 $fail_count 个测试失败，请检查后端服务！${NC}"
  exit 1
fi
