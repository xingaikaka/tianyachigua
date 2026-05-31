#!/usr/bin/env python3
"""
IndexNow 批量提交工具
=====================

从 sitemap-index (默认 tycg7.com 的 Telegram sitemap) 拉取全部 URL，
按 host 分组后批量提交到 Bing / IndexNow 协议端点。

用法：
  ./run-indexnow                           # 默认 tycg7.com + Bing
  ./run-indexnow --dry-run                 # 只解析 URL，不真正提交
  ./run-indexnow --engine indexnow         # 提交到 https://api.indexnow.org (会广播给所有支持的引擎)
  ./run-indexnow --sitemap https://tycg7.com/prod-api/web/api/sitemap/tg-sitemap-index.xml
  ./run-indexnow --limit 200               # 只提交前 200 个 URL（调试用）

约束：
  - IndexNow 单次最多 10000 URL；本脚本默认 500/批，便于失败重试。
  - 同一 host 一天提交量不要超过该 host 历史 URL 总量的 10x（否则可能被忽略）。
  - 提交前会先校验 https://<host>/<key>.txt 是否可访问，否则直接报错退出。
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.parse
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List

import certifi
import requests

CA_BUNDLE = certifi.where()

# ---- 默认配置（按需在命令行覆盖） ----
DEFAULT_SITEMAP = "https://tycg7.com/prod-api/web/api/sitemap/tg-sitemap-index.xml"
DEFAULT_KEY = "9d12cabc9a16414fb2a3a62bd5a57957"
DEFAULT_HOST = "tycg7.com"

# 端点说明：
#   bing       —— Bing 直收，最快
#   indexnow   —— 通用 IndexNow，广播给所有支持的搜索引擎（Bing、Yandex、Seznam、Naver 等）
#   yandex     —— 仅 Yandex
ENGINES = {
    "bing": "https://www.bing.com/indexnow",
    "indexnow": "https://api.indexnow.org/IndexNow",
    "yandex": "https://yandex.com/indexnow",
}

SITEMAP_NS = {
    "sm": "http://www.sitemaps.org/schemas/sitemap/0.9",
}

UA = "Mozilla/5.0 (compatible; tycg-indexnow-bot/1.0; +https://tycg7.com)"


def fetch(url: str, timeout: int = 30, verify: bool | str = True) -> str:
    r = requests.get(url, timeout=timeout, headers={"User-Agent": UA}, verify=verify)
    r.raise_for_status()
    return r.text


# 拉 sitemap 时使用的 verify 模式；可被 main() 中的 --insecure 覆盖
_SITEMAP_VERIFY: bool | str = CA_BUNDLE


def parse_sitemap(url: str) -> List[str]:
    """递归解析 sitemap 或 sitemap-index，返回所有 <loc>。"""
    text = fetch(url, verify=_SITEMAP_VERIFY)
    root = ET.fromstring(text)
    tag = root.tag.split("}", 1)[-1] if "}" in root.tag else root.tag

    if tag == "sitemapindex":
        children = root.findall("sm:sitemap/sm:loc", SITEMAP_NS)
        urls: List[str] = []
        for child in children:
            loc = (child.text or "").strip()
            if not loc:
                continue
            print(f"  ↳ 子 sitemap: {loc}", file=sys.stderr)
            urls.extend(parse_sitemap(loc))
        return urls

    if tag == "urlset":
        locs = root.findall("sm:url/sm:loc", SITEMAP_NS)
        return [(loc.text or "").strip() for loc in locs if loc.text]

    raise RuntimeError(f"未知 sitemap 根标签: {tag} ({url})")


def verify_key(host: str, key: str, key_location: str | None) -> str:
    """确认 key 文件可访问；返回最终用于提交的 keyLocation。"""
    candidate = key_location or f"https://{host}/{key}.txt"
    try:
        body = fetch(candidate, timeout=15, verify=_SITEMAP_VERIFY).strip()
    except Exception as e:
        print(f"❌ 无法访问 keyLocation: {candidate}\n   {e}", file=sys.stderr)
        sys.exit(2)
    if body != key:
        print(
            f"❌ {candidate} 内容与 key 不一致：\n"
            f"   期望: {key}\n"
            f"   实际: {body!r}\n"
            f"   请先把内容为 {key} 的 UTF-8 文本文件部署到该 URL。",
            file=sys.stderr,
        )
        sys.exit(2)
    print(f"✅ keyLocation 校验通过: {candidate}", file=sys.stderr)
    return candidate


def chunked(seq: List[str], size: int) -> Iterable[List[str]]:
    for i in range(0, len(seq), size):
        yield seq[i : i + size]


def submit_batch(
    endpoint: str,
    host: str,
    key: str,
    key_location: str,
    urls: List[str],
    dry_run: bool,
) -> int:
    payload = {
        "host": host,
        "key": key,
        "keyLocation": key_location,
        "urlList": urls,
    }
    if dry_run:
        print(f"  [dry-run] 将提交 {len(urls)} 条到 {endpoint}")
        return 200
    r = requests.post(
        endpoint,
        data=json.dumps(payload),
        headers={
            "Content-Type": "application/json; charset=utf-8",
            "User-Agent": UA,
        },
        timeout=60,
        verify=CA_BUNDLE,
    )
    return r.status_code


def main() -> int:
    parser = argparse.ArgumentParser(description="批量提交 sitemap URL 到 IndexNow")
    parser.add_argument("--sitemap", default=DEFAULT_SITEMAP, help="sitemap 或 sitemap-index URL")
    parser.add_argument("--key", default=DEFAULT_KEY, help="IndexNow API key")
    parser.add_argument(
        "--key-location",
        default=None,
        help="key 文件的公开 URL；不传则自动用 https://<host>/<key>.txt",
    )
    parser.add_argument(
        "--host",
        default=None,
        help="要提交的目标 host；不传则按每条 URL 的 host 自动分组",
    )
    parser.add_argument(
        "--engine",
        choices=list(ENGINES.keys()),
        default="bing",
        help="提交端点（默认 bing；indexnow=广播到所有引擎）",
    )
    parser.add_argument("--batch", type=int, default=500, help="每批 URL 数量（≤10000，默认 500）")
    parser.add_argument("--limit", type=int, default=0, help="最多提交多少 URL（0=不限）")
    parser.add_argument("--sleep", type=float, default=1.0, help="批次间隔秒数")
    parser.add_argument("--dry-run", action="store_true", help="只解析不提交")
    parser.add_argument(
        "--insecure",
        action="store_true",
        help="拉取 sitemap / key 文件时跳过 SSL 校验（提交端点仍严格校验，仅用于自家 Nginx 缺中间证书的临时绕过）",
    )
    args = parser.parse_args()

    if args.insecure:
        global _SITEMAP_VERIFY
        _SITEMAP_VERIFY = False
        import urllib3
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        print("⚠️  --insecure 开启，sitemap/key 拉取不校验证书", file=sys.stderr)

    endpoint = ENGINES[args.engine]
    print(f"→ 解析 sitemap: {args.sitemap}", file=sys.stderr)
    all_urls = parse_sitemap(args.sitemap)
    # 去重 + 保持顺序
    seen: Dict[str, None] = {}
    for u in all_urls:
        if u and u not in seen:
            seen[u] = None
    urls = list(seen.keys())
    print(f"→ 共提取 {len(urls)} 条唯一 URL", file=sys.stderr)

    if args.limit:
        urls = urls[: args.limit]
        print(f"→ 已截取前 {len(urls)} 条（--limit）", file=sys.stderr)

    if not urls:
        print("⚠️  没有可提交的 URL，退出", file=sys.stderr)
        return 0

    # 按 host 分组（IndexNow 协议要求一次请求只能提交同一个 host）
    if args.host:
        groups = {args.host: [u for u in urls if urllib.parse.urlparse(u).netloc == args.host]}
    else:
        groups = defaultdict(list)
        for u in urls:
            host = urllib.parse.urlparse(u).netloc
            if host:
                groups[host].append(u)

    total_ok = 0
    total_fail = 0
    for host, host_urls in groups.items():
        if not host_urls:
            continue
        print(f"\n=== Host: {host}  共 {len(host_urls)} 条 ===", file=sys.stderr)
        key_location = verify_key(host, args.key, args.key_location)

        for idx, batch in enumerate(chunked(host_urls, args.batch), 1):
            print(f"  → 批次 {idx}: {len(batch)} 条 …", end=" ", flush=True)
            status = None
            for attempt in range(1, 4):
                try:
                    status = submit_batch(
                        endpoint, host, args.key, key_location, batch, args.dry_run
                    )
                except Exception as e:
                    print(f"异常({attempt}): {e}", end=" ", flush=True)
                    status = -1
                if status in (200, 202):
                    break
                # 403 / 422 一般是 key 文件还没被抓 → 多等几秒
                wait = 5 * attempt
                print(f"重试 {attempt}/3 (HTTP {status}, sleep {wait}s)…", end=" ", flush=True)
                time.sleep(wait)
            if status in (200, 202):
                print(f"✅ {status}")
                total_ok += len(batch)
            else:
                print(f"❌ HTTP {status}")
                total_fail += len(batch)
            if args.sleep > 0:
                time.sleep(args.sleep)

    print("\n----- 汇总 -----")
    print(f"成功: {total_ok} 条")
    print(f"失败: {total_fail} 条")
    print(f"端点: {endpoint}")
    return 0 if total_fail == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
