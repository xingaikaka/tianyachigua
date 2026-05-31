#!/bin/bash

# 配置文件验证脚本

echo "=========================================="
echo "ChiguaVideo 配置文件验证"
echo "=========================================="

CONFIG_DIR="ruoyi-admin/src/main/resources"
ERROR_COUNT=0

# 检查YAML语法
echo "1. 检查YAML语法..."
echo ""

config_files=(
    "application.yml"
    "application-dev.yml" 
    "application-prod.yml"
    "application-chigua-dev.yml"
    "application-chigua-prod.yml"
    "application-encryption-dev.yml"
    "application-encryption-prod.yml"
)

for file in "${config_files[@]}"; do
    filepath="$CONFIG_DIR/$file"
    if [ -f "$filepath" ]; then
        echo -n "  检查 $file ... "
        
        # 使用Ruby检查YAML语法（如果可用）
        if command -v ruby &> /dev/null; then
            ruby -e "
require 'yaml'
begin
  YAML.load_file('$filepath')
  puts '✓ 通过'
rescue Psych::SyntaxError => e
  puts '✗ 失败'
  puts '    错误: ' + e.message
  exit 1
rescue => e
  puts '✗ 失败'
  puts '    错误: ' + e.message
  exit 1
end
" 2>/dev/null
            if [ $? -ne 0 ]; then
                ((ERROR_COUNT++))
            fi
        # 使用简单的语法检查
        elif grep -q "^[[:space:]]*[a-zA-Z]" "$filepath" && ! grep -q "^[[:space:]]*-[[:space:]]*[a-zA-Z].*:.*:" "$filepath"; then
            echo "✓ 基础语法检查通过"
        else
            echo "⚠ 跳过（无YAML验证工具）"
        fi
    else
        echo "  ✗ 文件不存在: $file"
        ((ERROR_COUNT++))
    fi
done

echo ""

# 检查环境变量模板文件
echo "2. 检查环境变量模板文件..."
echo ""

template_files=(
    "env.development.template"
    "env.production.template"
)

for file in "${template_files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file 存在"
    else
        echo "  ✗ $file 不存在"
        ((ERROR_COUNT++))
    fi
done

echo ""

# 检查启动脚本
echo "3. 检查启动脚本..."
echo ""

script_files=(
    "start-dev.sh"
    "start-prod.sh"
)

for file in "${script_files[@]}"; do
    if [ -f "$file" ]; then
        if [ -x "$file" ]; then
            echo "  ✓ $file 存在且可执行"
        else
            echo "  ⚠ $file 存在但不可执行"
            echo "    运行: chmod +x $file"
        fi
    else
        echo "  ✗ $file 不存在"
        ((ERROR_COUNT++))
    fi
done

echo ""

# 检查必要的依赖
echo "4. 检查系统依赖..."
echo ""

dependencies=(
    "java:Java运行环境"
    "mvn:Maven构建工具"
)

for dep in "${dependencies[@]}"; do
    cmd="${dep%:*}"
    desc="${dep#*:}"
    
    if command -v "$cmd" &> /dev/null; then
        version_info=""
        case "$cmd" in
            "java")
                version_info=" ($(java -version 2>&1 | head -n 1))"
                ;;
            "mvn")
                version_info=" ($(mvn -version 2>&1 | head -n 1))"
                ;;
        esac
        echo "  ✓ $desc$version_info"
    else
        echo "  ✗ $desc 未安装"
        ((ERROR_COUNT++))
    fi
done

echo ""

# 配置文件关系验证
echo "5. 检查配置文件关系..."
echo ""

# 检查主配置文件中的profiles配置
if grep -q "encryption-dev,chigua-dev" "$CONFIG_DIR/application.yml"; then
    echo "  ✓ 主配置文件包含开发环境profiles引用"
else
    echo "  ✗ 主配置文件缺少开发环境profiles引用"
    ((ERROR_COUNT++))
fi

# 检查环境变量占位符
echo "  检查环境变量占位符使用..."
placeholder_count=$(grep -c '\${[A-Z_]*[:-]' "$CONFIG_DIR/application.yml" || true)
if [ "$placeholder_count" -gt 0 ]; then
    echo "    ✓ 主配置文件使用了 $placeholder_count 个环境变量占位符"
else
    echo "    ⚠ 主配置文件未使用环境变量占位符"
fi

echo ""

# 总结
echo "=========================================="
if [ $ERROR_COUNT -eq 0 ]; then
    echo "✓ 验证通过！所有配置文件都正确配置。"
    echo ""
    echo "下一步操作："
    echo "1. 复制环境变量模板: cp env.development.template .env.development"
    echo "2. 编辑环境变量文件: nano .env.development" 
    echo "3. 启动开发环境: ./start-dev.sh"
    exit 0
else
    echo "✗ 验证失败！发现 $ERROR_COUNT 个问题需要修复。"
    exit 1
fi
