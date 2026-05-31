#!/usr/bin/env node

/**
 * 生成 sitemap.xml 脚本（安全版）
 * 仅生成包含主页面的 sitemap.xml，符合国内搜索引擎政策
 * 适用于提交给：百度、360、神马、搜狗
 */

const fs = require('fs');
const path = require('path');

// 配置
const CONFIG = {
  // 落地页域名（用于生成 sitemap 中的 URL）
  SITE_URL: process.env.SITE_URL || 'https://tycg8.com',
  // 输出文件路径（仅生成安全版，只包含主页面和关键词页面）
  OUTPUT_FILE: path.join(__dirname, '../public/sitemap.xml'),
  // 关键词列表（用于生成关键词页面，提升搜索引擎可搜索性）
  KEYWORDS: [
    '吃瓜',
    '吃瓜网',
    '吃瓜网站',
    '51吃瓜',
    '91吃瓜',
    '天涯吃瓜',
    '每日吃瓜',
    '天天吃瓜',
    '网红吃瓜',
    '热门吃瓜',
    '福利导航',
    '免费资源',
    '天涯吃瓜网',
    '吃瓜视频',
    '吃瓜平台',
  ],
};

// XML 转义函数
function escapeXml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

// 格式化日期
function formatDate(date) {
  if (!date) return new Date().toISOString().split('T')[0];
  if (date instanceof Date) {
    return date.toISOString().split('T')[0];
  }
  if (typeof date === 'string') {
    return date.split('T')[0];
  }
  return new Date().toISOString().split('T')[0];
}

// 生成 sitemap.xml（安全版：主页面 + 关键词页面）
function generateSitemap() {
  console.log('📝 生成 sitemap.xml（安全版：主页面 + 关键词页面）...');
  
  const today = formatDate(new Date());
  const siteUrl = CONFIG.SITE_URL.replace(/\/$/, '');
  
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
  xml += '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"\n';
  xml += '        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n';
  xml += '        xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9\n';
  xml += '        http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">\n';
  
  // 主页面（最高优先级）
  xml += '  <url>\n';
  xml += `    <loc>${escapeXml(siteUrl)}/</loc>\n`;
  xml += `    <lastmod>${today}</lastmod>\n`;
  xml += '    <changefreq>daily</changefreq>\n';
  xml += '    <priority>1.0</priority>\n';
  xml += '  </url>\n';
  
  // 关键词页面（提升搜索引擎可搜索性）
  // 这些页面指向主页面，但 URL 不同，便于搜索引擎通过关键词索引
  console.log(`   📝 添加 ${CONFIG.KEYWORDS.length} 个关键词页面...`);
  
  CONFIG.KEYWORDS.forEach((keyword, index) => {
    // URL 编码关键词，确保 URL 安全
    const encodedKeyword = encodeURIComponent(keyword);
    const keywordUrl = `${siteUrl}/keyword/${encodedKeyword}`;
    
    xml += '  <url>\n';
    xml += `    <loc>${escapeXml(keywordUrl)}</loc>\n`;
    xml += `    <lastmod>${today}</lastmod>\n`;
    xml += '    <changefreq>weekly</changefreq>\n';
    xml += '    <priority>0.8</priority>\n';
    xml += '  </url>\n';
    
    if ((index + 1) % 5 === 0) {
      console.log(`   📝 已添加 ${index + 1}/${CONFIG.KEYWORDS.length} 个关键词页面...`);
    }
  });
  
  xml += '</urlset>\n';
  
  console.log(`   ✅ 安全版：包含 1 个主页面 + ${CONFIG.KEYWORDS.length} 个关键词页面`);
  console.log('   ✅ 不包含视频页面（符合国内搜索引擎政策）');
  
  return xml;
}

// 主函数
function main() {
  console.log('🚀 开始生成 sitemap.xml（安全版：主页面 + 关键词页面）...\n');
  console.log('📋 配置信息:');
  console.log(`   - 站点URL: ${CONFIG.SITE_URL}`);
  console.log(`   - 输出文件: ${CONFIG.OUTPUT_FILE}`);
  console.log(`   - 关键词数量: ${CONFIG.KEYWORDS.length}`);
  console.log(`   - 模式: 安全版（主页面 + 关键词页面，符合国内搜索引擎政策）\n`);
  
  console.log('📝 关键词列表:');
  CONFIG.KEYWORDS.forEach((keyword, index) => {
    if (index < 5) {
      console.log(`   - ${keyword}`);
    } else if (index === 5) {
      console.log(`   ... 还有 ${CONFIG.KEYWORDS.length - 5} 个关键词`);
    }
  });
  console.log('');
  
  console.log('⚠️  注意：此 sitemap 包含主页面和关键词页面，不包含视频页面');
  console.log('   适用于提交给国内搜索引擎：百度、360、神马、搜狗');
  console.log('   关键词页面可提升搜索引擎通过关键词搜索到落地页的概率\n');
  
  try {
    // 生成 sitemap.xml（安全版，不需要获取视频列表）
    const sitemapXml = generateSitemap();
    
    // 确保输出目录存在
    const outputDir = path.dirname(CONFIG.OUTPUT_FILE);
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
    }
    
    // 写入文件
    fs.writeFileSync(CONFIG.OUTPUT_FILE, sitemapXml, 'utf8');
    
    const totalUrls = 1 + CONFIG.KEYWORDS.length;
    console.log(`\n✅ sitemap.xml 生成成功！`);
    console.log(`   📁 文件位置: ${CONFIG.OUTPUT_FILE}`);
    console.log(`   📊 包含 ${totalUrls} 个URL（1个主页面 + ${CONFIG.KEYWORDS.length}个关键词页面）`);
    console.log(`   📏 文件大小: ${(sitemapXml.length / 1024).toFixed(2)} KB`);
    console.log(`\n💡 提示：此 sitemap 适用于提交给国内搜索引擎（百度、360、神马、搜狗）`);
    console.log(`   提交地址: ${CONFIG.SITE_URL}/sitemap.xml`);
    console.log(`\n🔍 搜索优化说明：`);
    console.log(`   - 用户搜索"吃瓜"、"天涯吃瓜"等关键词时，搜索引擎可以匹配到关键词页面`);
    console.log(`   - 关键词页面 URL: ${CONFIG.SITE_URL}/keyword/{关键词}`);
    console.log(`   - 建议在落地页中实现关键词页面路由，显示主页面内容`);
    
  } catch (error) {
    console.error('\n❌ 生成 sitemap.xml 失败:', error.message);
    console.error(error.stack);
    process.exit(1);
  }
}

// 运行主函数
if (require.main === module) {
  main();
}

module.exports = { generateSitemap };

