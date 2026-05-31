#!/usr/bin/env node

/**
 * 创建环境配置文件
 * 确保前后端密钥一致性
 */

const fs = require('fs');
const path = require('path');

// 开发环境配置
const developmentConfig = `# ===========================================
# 天涯吃瓜 - 本地开发环境配置
# ===========================================

# ===========================================
# 🚀 API 服务配置
# ===========================================
# 后端API基础地址（本地开发环境）
REACT_APP_API_BASE_URL=http://localhost:8080

# API请求超时时间（毫秒）
REACT_APP_API_TIMEOUT=30000

# ===========================================
# 🔐 数据解密配置
# ===========================================
# 数据解密密钥（统一密钥，必须与后端保持一致）
REACT_APP_DECRYPT_KEY=chigua-web-secret-dev-2024

# 时间戳容差（毫秒，10分钟）
REACT_APP_TIMESTAMP_TOLERANCE=600000

# 开发环境允许解密失败时降级
REACT_APP_ALLOW_DECRYPT_FALLBACK=true

# 开发环境启用解密调试日志
REACT_APP_DECRYPT_DEBUG=true

# 解密重试次数
REACT_APP_DECRYPT_MAX_RETRIES=5

# 重试间隔（毫秒）
REACT_APP_DECRYPT_RETRY_INTERVAL=1000

# ===========================================
# 🖼️ 图片解密配置（与后端application-chigua.yml保持一致）
# ===========================================
# 图片解密密钥（必须与后端chigua.r2.image-encryption.key保持一致）
REACT_APP_IMAGE_ENCRYPTION_KEY=DR4jiK+U+Z6QwyuVxp//Cl3Zl543bl1xiQrfH6hnLc4=

# 开发环境启用图片解密调试
REACT_APP_IMAGE_DECRYPT_DEBUG=true

# 开发环境允许图片解密失败降级
REACT_APP_ALLOW_IMAGE_DECRYPT_FALLBACK=true

# 图片最大渲染尺寸
REACT_APP_IMAGE_MAX_WIDTH=4096
REACT_APP_IMAGE_MAX_HEIGHT=4096

# 图片质量
REACT_APP_IMAGE_QUALITY=0.9

# 启用图片平滑缩放
REACT_APP_IMAGE_SMOOTHING=true

# ===========================================
# 🛠️ 开发环境配置
# ===========================================
# 开发环境显示调试信息
REACT_APP_SHOW_DEBUG=true

# 开发环境启用控制台日志
REACT_APP_ENABLE_CONSOLE_LOG=true

# 不使用模拟API
REACT_APP_MOCK_API=false

# ===========================================
# 🎛️ 功能开关
# ===========================================
# 启用安全图片功能
REACT_APP_ENABLE_SECURE_IMAGE=true

# 启用数据加密功能
REACT_APP_ENABLE_DATA_ENCRYPTION=true

# 启用图片解密功能
REACT_APP_ENABLE_IMAGE_DECRYPTION=true
`;

// 生产环境配置
const productionConfig = `# ===========================================
# 天涯吃瓜 - 生产环境配置
# ===========================================

# ===========================================
# 🚀 API 服务配置
# ===========================================
# 后端API基础地址（生产环境）
# 使用同域反代前缀，线上通过 Nginx 转发至后端
REACT_APP_API_BASE_URL=/prod-api

# API请求超时时间（毫秒）
REACT_APP_API_TIMEOUT=30000

# ===========================================
# 🔐 数据解密配置
# ===========================================
# 数据解密密钥（统一密钥，必须与后端保持一致）
REACT_APP_DECRYPT_KEY=chigua-web-secret-dev-2024

# 时间戳容差（毫秒，5分钟）
REACT_APP_TIMESTAMP_TOLERANCE=300000

# 解密失败时不降级（生产环境）
REACT_APP_ALLOW_DECRYPT_FALLBACK=false

# 不启用解密调试日志（生产环境）
REACT_APP_DECRYPT_DEBUG=false

# 解密重试次数
REACT_APP_DECRYPT_MAX_RETRIES=3

# 重试间隔（毫秒）
REACT_APP_DECRYPT_RETRY_INTERVAL=1000

# ===========================================
# 🖼️ 图片解密配置（与后端application-chigua.yml保持一致）
# ===========================================
# 图片解密密钥（必须与后端chigua.r2.image-encryption.key保持一致）
REACT_APP_IMAGE_ENCRYPTION_KEY=DR4jiK+U+Z6QwyuVxp//Cl3Zl543bl1xiQrfH6hnLc4=

# 生产环境不启用图片解密调试
REACT_APP_IMAGE_DECRYPT_DEBUG=false

# 生产环境不允许图片解密失败降级
REACT_APP_ALLOW_IMAGE_DECRYPT_FALLBACK=false

# 图片最大渲染尺寸
REACT_APP_IMAGE_MAX_WIDTH=4096
REACT_APP_IMAGE_MAX_HEIGHT=4096

# 图片质量
REACT_APP_IMAGE_QUALITY=0.9

# 启用图片平滑缩放
REACT_APP_IMAGE_SMOOTHING=true

# ===========================================
# 🛠️ 生产环境配置
# ===========================================
# 不显示调试信息（生产环境）
REACT_APP_SHOW_DEBUG=false

# 不启用控制台日志（生产环境）
REACT_APP_ENABLE_CONSOLE_LOG=false

# 不使用模拟API
REACT_APP_MOCK_API=false

# ===========================================
# 🎛️ 功能开关
# ===========================================
# 启用安全图片功能
REACT_APP_ENABLE_SECURE_IMAGE=true

# 启用数据加密功能
REACT_APP_ENABLE_DATA_ENCRYPTION=true

# 启用图片解密功能
REACT_APP_ENABLE_IMAGE_DECRYPTION=true
`;

const log = (message = '') => {
  process.stdout.write(`${message}\n`);
};

const logError = (message = '', detail = '') => {
  process.stderr.write(`${message}${detail ? ` ${detail}` : ''}\n`);
};

try {
  // 写入 .env.development 文件
  fs.writeFileSync('.env.development', developmentConfig, 'utf8');
  log('✅ 已创建 .env.development 文件（开发环境配置）');
  
  // 写入 .env.production 文件
  fs.writeFileSync('.env.production', productionConfig, 'utf8');
  log('✅ 已创建 .env.production 文件（生产环境配置）');
  
  log('\n🔑 密钥配置总览：');
  log('开发环境:');
  log('  - 数据解密密钥: chigua-web-secret-dev-2024');
  log('  - 图片解密密钥: DR4jiK+U+Z6QwyuVxp//Cl3Zl543bl1xiQrfH6hnLc4=');
  log('  - API地址: http://localhost:8080');
  
  log('\n生产环境:');
  log('  - 数据解密密钥: chigua-web-secret-dev-2024 (统一密钥)');
  log('  - 图片解密密钥: DR4jiK+U+Z6QwyuVxp//Cl3Zl543bl1xiQrfH6hnLc4=');
  log('  - API地址: /prod-api');
  
  log('\n📋 配置与后端保持一致！');
  log('\n🚀 使用方法：');
  log('开发环境: NODE_ENV=development npm start');
  log('生产环境: NODE_ENV=production npm run build');
  
} catch (error) {
  logError('❌ 创建环境文件失败：', error.message);
  process.exit(1);
}
