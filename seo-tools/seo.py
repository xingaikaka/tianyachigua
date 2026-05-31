#!/usr/bin/env python3
"""
SEO 自动化工具 - Google Search Console
========================================

子命令:
  list-sites             列出所有可访问的 Property
  list-sitemaps          列出 Property 下的全部 sitemap 及抓取状态
  submit-sitemap         提交 sitemap
  delete-sitemap         删除 sitemap
  inspect                查询单个 URL 的索引状态
  inspect-batch          批量查询 URL 索引状态（带速率限制）
  analytics              拉取最近 7 天搜索表现（query/clicks/impressions）
  index-tg               一键流程：拉所有 TG 帖子 URL → 批量 inspect → 输出 CSV 报告

环境变量:
  GSC_KEY  服务账号 JSON 路径，默认 secrets/gsc-bot.json
  SITE     默认操作 site，如 https://search.cqqvl.cc/

依赖:
  pip install google-api-python-client google-auth requests
"""

import argparse
import csv
import json
import os
import sys
import time
import urllib.parse
from datetime import date, timedelta
from pathlib import Path

import requests
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_KEY = SCRIPT_DIR / "secrets" / "gsc-bot.json"
DEFAULT_SITE = "https://search.rklzu.cc/"
SCOPES = [
    "https://www.googleapis.com/auth/webmasters",
    "https://www.googleapis.com/auth/siteverification",
]

# --- 接入服务 --------------------------------------------------------------


def gsc_service(key_path: Path):
    creds = service_account.Credentials.from_service_account_file(
        str(key_path), scopes=SCOPES
    )
    return build("searchconsole", "v1", credentials=creds, cache_discovery=False)


def verify_service(key_path: Path):
    creds = service_account.Credentials.from_service_account_file(
        str(key_path), scopes=SCOPES
    )
    return build("siteVerification", "v1", credentials=creds, cache_discovery=False)


# --- 业务命令 --------------------------------------------------------------


def cmd_list_sites(args, svc):
    sites = svc.sites().list().execute().get("siteEntry", [])
    if not sites:
        print("(空) 该 service account 还没被任何 Property 添加为用户")
        print(f"请把  {service_account_email(args.key)}  加到 GSC Property → 用户和权限")
        return
    print(f"{'权限':<14}{'URL'}")
    print("-" * 60)
    for s in sites:
        print(f"{s.get('permissionLevel', ''):<14}{s.get('siteUrl', '')}")


def cmd_list_sitemaps(args, svc):
    rsp = svc.sitemaps().list(siteUrl=args.site).execute()
    items = rsp.get("sitemap", [])
    if not items:
        print(f"该 site 下没有任何 sitemap: {args.site}")
        return
    for s in items:
        print(f"- {s.get('path')}")
        print(f"    type:        {s.get('type')}")
        print(f"    isPending:   {s.get('isPending')}")
        print(f"    isSitemapsIndex: {s.get('isSitemapsIndex')}")
        print(f"    lastSubmitted:   {s.get('lastSubmitted')}")
        print(f"    lastDownloaded:  {s.get('lastDownloaded')}")
        warnings = s.get("warnings", 0)
        errors = s.get("errors", 0)
        print(f"    warnings/errors: {warnings} / {errors}")
        for c in s.get("contents", []):
            print(f"    contents: type={c.get('type')} submitted={c.get('submitted')} indexed={c.get('indexed')}")


def cmd_submit_sitemap(args, svc):
    feed = args.feedpath
    svc.sitemaps().submit(siteUrl=args.site, feedpath=feed).execute()
    print(f"✅ submitted: {feed}")


def cmd_delete_sitemap(args, svc):
    feed = args.feedpath
    svc.sitemaps().delete(siteUrl=args.site, feedpath=feed).execute()
    print(f"🗑️  deleted: {feed}")


def cmd_inspect(args, svc):
    body = {"inspectionUrl": args.url, "siteUrl": args.site}
    rsp = svc.urlInspection().index().inspect(body=body).execute()
    print(json.dumps(rsp, indent=2, ensure_ascii=False))


def cmd_inspect_batch(args, svc):
    urls = [u.strip() for u in Path(args.input).read_text().splitlines() if u.strip()]
    out_path = Path(args.output)
    rps = 0.5  # ~1 次/2 秒，避开 600/min quota
    last = 0.0
    with out_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["url", "coverageState", "verdict", "lastCrawlTime", "googleCanonical", "userCanonical", "indexingState", "fetchState", "pageFetchState"])
        for i, url in enumerate(urls, 1):
            wait = max(0, 1 / rps - (time.time() - last))
            if wait:
                time.sleep(wait)
            last = time.time()
            try:
                rsp = svc.urlInspection().index().inspect(
                    body={"inspectionUrl": url, "siteUrl": args.site}
                ).execute()
                idx = rsp.get("inspectionResult", {}).get("indexStatusResult", {})
                w.writerow([
                    url,
                    idx.get("coverageState", ""),
                    idx.get("verdict", ""),
                    idx.get("lastCrawlTime", ""),
                    idx.get("googleCanonical", ""),
                    idx.get("userCanonical", ""),
                    idx.get("indexingState", ""),
                    "",
                    idx.get("pageFetchState", ""),
                ])
                print(f"[{i}/{len(urls)}] {idx.get('verdict', '?'):<10} {url}")
            except HttpError as e:
                print(f"[{i}/{len(urls)}] ERROR  {url}  {e}")
                w.writerow([url, "", f"ERROR: {e.status_code}", "", "", "", "", "", ""])
    print(f"\n✅ 报告写入: {out_path}")


def cmd_analytics(args, svc):
    end = date.today() - timedelta(days=2)  # GSC 数据 ~2 天延迟
    start = end - timedelta(days=int(args.days) - 1)
    body = {
        "startDate": start.isoformat(),
        "endDate": end.isoformat(),
        "dimensions": ["query"],
        "rowLimit": int(args.limit),
    }
    rsp = svc.searchanalytics().query(siteUrl=args.site, body=body).execute()
    rows = rsp.get("rows", [])
    if not rows:
        print(f"({start} ~ {end}) 无数据；新站通常需要被索引后才会有数据")
        return
    print(f"日期范围: {start} ~ {end}    共 {len(rows)} 行（按点击数倒序）")
    print(f"{'clicks':>7}  {'imps':>7}  {'ctr':>6}  {'pos':>5}  query")
    print("-" * 70)
    for r in sorted(rows, key=lambda x: -x.get("clicks", 0)):
        q = r.get("keys", ["?"])[0]
        print(f"{r['clicks']:>7}  {r['impressions']:>7}  {r['ctr']*100:>5.2f}%  {r['position']:>5.1f}  {q}")


def cmd_verify_token(args, svc):
    """步骤 1: 申请 HTML 文件验证 token"""
    vsvc = verify_service(Path(args.key))
    rsp = vsvc.webResource().getToken(body={
        "site": {"identifier": args.site, "type": "SITE"},
        "verificationMethod": "FILE",
    }).execute()
    token = rsp["token"]
    print(f"Token文件名: {token}")
    print(f"Token文件内容: google-site-verification: {token}")
    print()
    print("接下来把这个文件放到网站根目录, 浏览器访问需返回完全相同的内容:")
    print(f"  https://{urllib.parse.urlparse(args.site).hostname}/{token}")
    print()
    print(f"放好后再跑:  ./run verify-confirm")


def cmd_verify_confirm(args, svc):
    """步骤 2: 已部署文件后, 调用 verify 让 service account 成为所有者"""
    vsvc = verify_service(Path(args.key))
    try:
        rsp = vsvc.webResource().insert(
            verificationMethod="FILE",
            body={"site": {"identifier": args.site, "type": "SITE"}},
        ).execute()
        print(f"✅ Site Verification 成功! 所有者: {rsp.get('owners', [])}")
    except HttpError as e:
        if e.resp.status == 400 and b"already verified" in e.content:
            print("ℹ️  已经验证过, 跳过.")
        else:
            print(f"❌ 验证失败: {e}")
            return
    # 第 2 步: 在 Search Console 里把它加成 Property
    try:
        svc.sites().add(siteUrl=args.site).execute()
        print(f"✅ Search Console 已添加 Property: {args.site}")
    except HttpError as e:
        if e.resp.status == 409 or b"already" in e.content.lower():
            print("ℹ️  Property 已存在, 跳过.")
        else:
            print(f"❌ add Property 失败: {e}")
            return
    print(f"\n现在跑 ./run list-sites 应该能看到 {args.site}")


INDEXNOW_KEY = "3e7f0153a745ef73254ca18e763816ad"
INDEXNOW_ENDPOINTS = [
    "https://api.indexnow.org/IndexNow",
    "https://www.bing.com/indexnow",
    "https://yandex.com/indexnow",
]


def cmd_indexnow(args, svc):
    """提交URL列表到IndexNow协议 (Bing/Yandex/Naver, 实时收录)"""
    host = urllib.parse.urlparse(args.site).hostname
    key_url = f"{args.site.rstrip('/')}/{INDEXNOW_KEY}.txt"
    if args.input:
        urls = [u.strip() for u in Path(args.input).read_text().splitlines() if u.strip()]
    elif args.tg:
        sm = f"{args.site.rstrip('/')}/prod-api/web/api/sitemap/tg-sitemap-index.xml"
        urls = collect_urls_from_sitemap(sm)
    elif args.all:
        sm = f"{args.site.rstrip('/')}/prod-api/web/api/sitemap/sitemap-index.xml"
        urls = collect_urls_from_sitemap(sm)
        tg = f"{args.site.rstrip('/')}/prod-api/web/api/sitemap/tg-sitemap-index.xml"
        urls += collect_urls_from_sitemap(tg)
        urls = list(dict.fromkeys(urls))
    else:
        sys.exit("必须指定 --tg / --all / --input <file>")

    print(f"📊 待推送 {len(urls)} 个 URL → IndexNow ({host})")
    print(f"🔑 keyLocation: {key_url}")

    # IndexNow 单次提交最多 10000 个 URL
    batch_size = 10000
    total_ok = 0
    for endpoint in INDEXNOW_ENDPOINTS:
        print(f"\n→ {endpoint}")
        ok = 0
        for i in range(0, len(urls), batch_size):
            batch = urls[i:i + batch_size]
            try:
                r = requests.post(endpoint, json={
                    "host": host,
                    "key": INDEXNOW_KEY,
                    "keyLocation": key_url,
                    "urlList": batch,
                }, headers={"Content-Type": "application/json"}, timeout=30)
                # IndexNow: 200=ok, 202=accepted, 422=invalid url(s), 403=key invalid
                tag = {200: "✅ OK", 202: "✅ accepted", 400: "❌ 400 bad", 403: "❌ 403 key", 422: "⚠️ 422 部分URL无效", 429: "⚠️ 429 限流"}.get(r.status_code, f"? {r.status_code}")
                print(f"   batch {i // batch_size + 1}: {len(batch)} urls → {tag}  body={r.text[:200]}")
                if r.status_code in (200, 202):
                    ok += len(batch)
            except Exception as e:
                print(f"   batch {i // batch_size + 1}: ERROR {e}")
        if ok:
            total_ok = max(total_ok, ok)
    print(f"\n📈 至少 {total_ok}/{len(urls)} 个 URL 被至少 1 个搜索引擎接收")


def cmd_index_tg(args, svc):
    """一键流程: 抓后端 tg sitemap → 巡检所有 URL → 输出报告"""
    sitemap_url = f"{args.site.rstrip('/')}/prod-api/web/api/sitemap/tg-sitemap-index.xml"
    print(f"📥 拉取 sitemap 索引: {sitemap_url}")
    urls = collect_urls_from_sitemap(sitemap_url)
    print(f"📊 总 URL 数: {len(urls)}")

    # 优先抓的子集 (按 GSC 单 site 200/天的 URL Inspection 限额, 我们采样)
    sample_n = int(args.sample) if args.sample else min(150, len(urls))
    sample = urls[:sample_n]
    print(f"🔎 抽样巡检前 {sample_n} 条")

    tmp_in = SCRIPT_DIR / "_tg_urls.txt"
    tmp_in.write_text("\n".join(sample))
    args.input = str(tmp_in)
    args.output = str(SCRIPT_DIR / f"tg-coverage-{date.today().isoformat()}.csv")
    cmd_inspect_batch(args, svc)
    tmp_in.unlink(missing_ok=True)


# --- 辅助函数 --------------------------------------------------------------


def service_account_email(key_path) -> str:
    return json.loads(Path(key_path).read_text()).get("client_email", "?")


def collect_urls_from_sitemap(url: str) -> list:
    """递归把 sitemap-index → sitemap → url 全部展开"""
    out = []
    rsp = requests.get(url, timeout=30, headers={"User-Agent": "chigua-seo/1.0"})
    rsp.raise_for_status()
    xml = rsp.text
    import re
    # 子 sitemap
    for m in re.findall(r"<sitemap>.*?<loc>(.*?)</loc>", xml, re.S):
        out.extend(collect_urls_from_sitemap(m))
    # 终端 url
    for m in re.findall(r"<url>.*?<loc>(.*?)</loc>", xml, re.S):
        out.append(m.strip())
    return out


# --- argparse --------------------------------------------------------------


def main():
    p = argparse.ArgumentParser(prog="seo", description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--key", default=os.environ.get("GSC_KEY", str(DEFAULT_KEY)),
                   help=f"service account JSON 路径 (默认: {DEFAULT_KEY})")
    p.add_argument("--site", default=os.environ.get("SITE", DEFAULT_SITE),
                   help=f"GSC siteUrl, 末尾必须带 / (默认: {DEFAULT_SITE})")
    sub = p.add_subparsers(dest="cmd", required=True)

    sub.add_parser("list-sites", help="列出全部可访问的 Property")
    sub.add_parser("list-sitemaps", help="列出当前 site 下的 sitemap")
    sub.add_parser("verify-token", help="步骤1: 申请HTML文件验证token (绕过UI添加用户)")
    sub.add_parser("verify-confirm", help="步骤2: 部署文件后调用verify让service account拿到权限")

    sp = sub.add_parser("submit-sitemap", help="提交 sitemap")
    sp.add_argument("feedpath", help="sitemap 完整 URL")

    sp = sub.add_parser("delete-sitemap", help="删除 sitemap")
    sp.add_argument("feedpath")

    sp = sub.add_parser("inspect", help="检查单个 URL 索引状态")
    sp.add_argument("url")

    sp = sub.add_parser("inspect-batch", help="批量 URL 索引检查 → CSV")
    sp.add_argument("--input", required=True, help="一行一个 URL 的文本文件")
    sp.add_argument("--output", required=True, help="输出 CSV 路径")

    sp = sub.add_parser("analytics", help="拉取近 N 天搜索表现")
    sp.add_argument("--days", default=7)
    sp.add_argument("--limit", default=100)

    sp = sub.add_parser("index-tg", help="一键: 抓后端 sitemap + 批量巡检 + CSV")
    sp.add_argument("--sample", default=0, help="只抽样前 N 条; 默认 min(150, total)")

    sp = sub.add_parser("indexnow", help="推送URL到IndexNow(Bing/Yandex/Naver,实时收录)")
    sp.add_argument("--tg", action="store_true", help="推送TG sitemap全部URL")
    sp.add_argument("--all", action="store_true", help="推送全站sitemap+TG sitemap全部URL")
    sp.add_argument("--input", help="自定义URL列表文件(一行一个)")

    args = p.parse_args()

    key_path = Path(args.key)
    if not key_path.exists():
        sys.exit(f"❌ key file not found: {key_path}")

    try:
        svc = gsc_service(key_path)
    except Exception as e:
        sys.exit(f"❌ auth failed: {e}")

    {
        "list-sites":     cmd_list_sites,
        "list-sitemaps":  cmd_list_sitemaps,
        "submit-sitemap": cmd_submit_sitemap,
        "delete-sitemap": cmd_delete_sitemap,
        "inspect":        cmd_inspect,
        "inspect-batch":  cmd_inspect_batch,
        "analytics":      cmd_analytics,
        "index-tg":       cmd_index_tg,
        "indexnow":       cmd_indexnow,
        "verify-token":   cmd_verify_token,
        "verify-confirm": cmd_verify_confirm,
    }[args.cmd](args, svc)


if __name__ == "__main__":
    main()
