#!/bin/bash

# 视频列表查询优化测试脚本

echo "🎯 视频列表查询优化测试"
echo "================================"

BASE_URL="http://localhost:8080"
CATEGORY_ID=1
PAGE_SIZE=20

echo ""
echo "📊 测试配置:"
echo "- 服务地址: $BASE_URL"
echo "- 分类ID: $CATEGORY_ID"
echo "- 页面大小: $PAGE_SIZE"
echo ""

# 检查服务是否启动
echo "🔍 检查服务状态..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "❌ 服务未启动，请先启动后端服务"
    echo "   cd backend && mvn spring-boot:run"
    exit 1
fi
echo "✅ 服务正常运行"
echo ""

# 测试原始接口
echo "🔄 测试原始接口..."
echo "GET $BASE_URL/web/api/category/videos?categoryId=$CATEGORY_ID&pageSize=$PAGE_SIZE"
ORIGINAL_START=$(date +%s%N)
ORIGINAL_RESPONSE=$(curl -s -w "%{http_code}|%{size_download}|%{time_total}" \
    "$BASE_URL/web/api/category/videos?categoryId=$CATEGORY_ID&pageSize=$PAGE_SIZE")
ORIGINAL_END=$(date +%s%N)

ORIGINAL_HTTP_CODE=$(echo $ORIGINAL_RESPONSE | cut -d'|' -f1)
ORIGINAL_SIZE=$(echo $ORIGINAL_RESPONSE | cut -d'|' -f2)
ORIGINAL_TIME=$(echo $ORIGINAL_RESPONSE | cut -d'|' -f3)

if [ "$ORIGINAL_HTTP_CODE" = "200" ]; then
    echo "✅ 原始接口响应成功"
    echo "   - 响应大小: ${ORIGINAL_SIZE} bytes"
    echo "   - 响应时间: ${ORIGINAL_TIME}s"
else
    echo "❌ 原始接口响应失败 (HTTP $ORIGINAL_HTTP_CODE)"
fi
echo ""

# 测试优化接口
echo "🚀 测试优化接口..."
echo "GET $BASE_URL/web/api/category/videos/optimized?categoryId=$CATEGORY_ID&pageSize=$PAGE_SIZE"
OPTIMIZED_START=$(date +%s%N)
OPTIMIZED_RESPONSE=$(curl -s -w "%{http_code}|%{size_download}|%{time_total}" \
    "$BASE_URL/web/api/category/videos/optimized?categoryId=$CATEGORY_ID&pageSize=$PAGE_SIZE")
OPTIMIZED_END=$(date +%s%N)

OPTIMIZED_HTTP_CODE=$(echo $OPTIMIZED_RESPONSE | cut -d'|' -f1)
OPTIMIZED_SIZE=$(echo $OPTIMIZED_RESPONSE | cut -d'|' -f2)
OPTIMIZED_TIME=$(echo $OPTIMIZED_RESPONSE | cut -d'|' -f3)

if [ "$OPTIMIZED_HTTP_CODE" = "200" ]; then
    echo "✅ 优化接口响应成功"
    echo "   - 响应大小: ${OPTIMIZED_SIZE} bytes"
    echo "   - 响应时间: ${OPTIMIZED_TIME}s"
else
    echo "❌ 优化接口响应失败 (HTTP $OPTIMIZED_HTTP_CODE)"
fi
echo ""

# 性能对比
if [ "$ORIGINAL_HTTP_CODE" = "200" ] && [ "$OPTIMIZED_HTTP_CODE" = "200" ]; then
    echo "📈 性能对比结果:"
    echo "================================"
    
    # 计算大小优化比例
    if [ "$ORIGINAL_SIZE" -gt 0 ]; then
        SIZE_REDUCTION=$(echo "scale=1; (1 - $OPTIMIZED_SIZE / $ORIGINAL_SIZE) * 100" | bc -l)
        echo "📦 数据传输优化: ${SIZE_REDUCTION}% 减少"
        echo "   - 原始大小: ${ORIGINAL_SIZE} bytes"
        echo "   - 优化大小: ${OPTIMIZED_SIZE} bytes"
    fi
    
    # 计算时间优化比例
    TIME_REDUCTION=$(echo "scale=1; (1 - $OPTIMIZED_TIME / $ORIGINAL_TIME) * 100" | bc -l 2>/dev/null || echo "N/A")
    if [ "$TIME_REDUCTION" != "N/A" ]; then
        echo "⚡ 响应时间优化: ${TIME_REDUCTION}% 提升"
        echo "   - 原始时间: ${ORIGINAL_TIME}s"
        echo "   - 优化时间: ${OPTIMIZED_TIME}s"
    fi
    
    echo ""
    echo "🎯 优化效果评估:"
    if (( $(echo "$SIZE_REDUCTION > 80" | bc -l) )); then
        echo "✅ 数据传输优化: 优秀 (>80%)"
    elif (( $(echo "$SIZE_REDUCTION > 50" | bc -l) )); then
        echo "✅ 数据传输优化: 良好 (>50%)"
    else
        echo "⚠️  数据传输优化: 一般 (<50%)"
    fi
    
    if [ "$TIME_REDUCTION" != "N/A" ] && (( $(echo "$TIME_REDUCTION > 30" | bc -l) )); then
        echo "✅ 响应时间优化: 显著提升"
    elif [ "$TIME_REDUCTION" != "N/A" ] && (( $(echo "$TIME_REDUCTION > 0" | bc -l) )); then
        echo "✅ 响应时间优化: 有所提升"
    else
        echo "⚠️  响应时间优化: 需要进一步分析"
    fi
fi

echo ""
echo "🔧 进一步测试建议:"
echo "1. 使用 ab 工具进行压力测试"
echo "2. 监控数据库查询时间"
echo "3. 检查缓存命中率"
echo "4. 验证前端功能完整性"
echo ""
echo "测试完成！"
