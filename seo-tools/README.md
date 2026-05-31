# SEO 工具

Google Search Console 自动化脚本，用 service account 认证。

## 安装

```bash
cd seo-tools
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## 使用

```bash
# 1. 列出所有可访问 Property（验证 service account 是否被授权）
./run list-sites

# 2. 列出当前 site 下的全部 sitemap 及抓取状态
./run list-sitemaps

# 3. 提交 sitemap
./run submit-sitemap https://search.cqqvl.cc/prod-api/web/api/sitemap/tg-sitemap-index.xml

# 4. 检查单个 URL 是否被索引
./run inspect https://search.cqqvl.cc/tg/post/30

# 5. 批量巡检（每天 ≤200 个，受 GSC quota）
./run inspect-batch --input urls.txt --output report.csv

# 6. 拉最近 7 天搜索数据（新站没数据是正常的）
./run analytics --days 7 --limit 100

# 7. 一键: 拉 TG sitemap → 抽样 150 条巡检 → 输出 CSV
./run index-tg
./run index-tg --sample 200
```

## 自动化（每天跑）

```bash
crontab -e
# 每天 03:00 跑一次抽样巡检
0 3 * * * cd /Users/lee/mierichigua/seo-tools && ./run index-tg >> daily.log 2>&1
```

## 切换 site / key

```bash
# 临时
SITE=https://其他子域名.cc/ ./run list-sitemaps
GSC_KEY=/path/to/other.json ./run list-sites

# 永久（写到 ~/.zshrc）
export GSC_KEY=/Users/lee/mierichigua/seo-tools/secrets/gsc-bot.json
export SITE=https://search.cqqvl.cc/
```

## 注意事项

- **service account JSON 不要 commit 到 git**（已在 `.gitignore`）。
- GSC URL Inspection API 单 Property **配额约 2000/天，单分钟 600**，本脚本默认 0.5 req/s。
- Google **不提供"强制立即收录"API**，本工具的本质是**监控 + 报告**，加快收录靠：好 sitemap + 内链 + 真实流量。
