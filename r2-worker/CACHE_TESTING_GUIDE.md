# CDN缓存测试指南 - 使用F12开发者工具

## 🎯 测试目标

验证CDN缓存功能是否正常工作，并学会如何通过浏览器开发者工具查看缓存状态。

---

## 📋 准备工作

1. 打开Chrome/Edge/Firefox浏览器
2. 准备一个测试文件（如 `test.txt`）
3. Worker URL: `https://chigua-r2-worker.xingaikaka.workers.dev`

---

## 🔬 测试步骤

### 步骤1: 上传测试文件

```bash
# 创建测试文件
echo "CDN Cache Test - $(date)" > test.txt

# 上传到Worker
curl -X POST https://chigua-r2-worker.xingaikaka.workers.dev/api/upload \
  -F "file=@test.txt" \
  -F "type=document"

# 复制返回的 previewUrl
```

响应示例：
```json
{
  "success": true,
  "data": {
    "previewUrl": "https://chigua-r2-worker.xingaikaka.workers.dev/files/documents%2F2025%2F10%2F26%2Fxxx.txt?key=...&signature=...&expires=..."
  }
}
```

### 步骤2: 使用F12查看缓存

#### 2.1 打开开发者工具

1. 按 **F12** 打开开发者工具
2. 切换到 **Network（网络）** 标签

#### 2.2 第一次访问（预期：CACHE MISS）

1. 点击 🚫 清空网络记录
2. 在浏览器地址栏粘贴 `previewUrl`
3. 按 **Enter** 访问

**观察要点**：

在Network列表中，点击该请求，查看 **Headers** 标签：

```
General:
  Request URL: https://chigua-r2-worker.xingaikaka.workers.dev/files/...
  Request Method: GET
  Status Code: 200 OK

Response Headers:
  ✅ 查找这个！
  X-Cache-Status: MISS          ← 第一次访问，缓存未命中
  
  Cache-Control: public, max-age=3600
  Content-Type: text/plain
  Content-Length: 42
```

**截图示例**：
```
┌─────────────────────────────────────┐
│ Name        Status  Type  Size Time │
├─────────────────────────────────────┤
│ xxx.txt     200     txt   42B  250ms│ ← 注意时间较长
└─────────────────────────────────────┘

Response Headers:
X-Cache-Status: MISS ← 🔴 未命中
Age: 0
```

#### 2.3 第二次访问（预期：CACHE HIT）

1. **不要清空** Network记录
2. 在地址栏再次按 **Enter** 或 **Ctrl+R** 刷新
3. 观察新的请求

**观察要点**：

```
Response Headers:
  ✅ 关键变化！
  X-Cache-Status: HIT           ← 缓存命中！
  Age: 15                       ← 缓存已存在15秒
  
  Cache-Control: public, max-age=3600
```

**截图示例**：
```
┌─────────────────────────────────────┐
│ Name        Status  Type  Size Time │
├─────────────────────────────────────┤
│ xxx.txt     200     txt   42B   50ms│ ← 时间变短！
└─────────────────────────────────────┘

Response Headers:
X-Cache-Status: HIT ← 🟢 命中！
Age: 15             ← 缓存年龄
```

#### 2.4 查看Timing（性能对比）

点击请求 → **Timing** 标签：

**第一次访问（MISS）**：
```
Queueing:             0.5ms
DNS Lookup:          15.0ms
Initial connection:  50.0ms
SSL:                 80.0ms
Waiting (TTFB):     200.0ms ← 从R2读取
Content Download:    50.0ms
─────────────────────────────
Total:              395.5ms
```

**第二次访问（HIT）**：
```
Queueing:             0.3ms
DNS Lookup:           0.0ms (cached)
Initial connection:   0.0ms (reused)
SSL:                  0.0ms (reused)
Waiting (TTFB):      30.0ms ← 从边缘缓存！
Content Download:    15.0ms
─────────────────────────────
Total:               45.3ms ← 快了8倍！
```

---

## 📊 判断标准表格

| 指标 | CACHE MISS | CACHE HIT | 说明 |
|------|------------|-----------|------|
| **X-Cache-Status** | MISS | HIT | 最直接的标志 |
| **响应时间** | 200-500ms | 30-100ms | 快了3-5倍 |
| **Age头** | 无或0 | >0 | 缓存存在时间 |
| **Size显示** | 实际大小 | 小/from cache | 浏览器缓存 |
| **Waiting(TTFB)** | >150ms | <50ms | 服务器响应时间 |

---

## 🎨 Chrome DevTools 截图指南

### 查看位置图示

```
┌─────────────────────────────────────────────────────┐
│ Elements  Console  Sources  [Network]  Performance  │ ← 点这里
├─────────────────────────────────────────────────────┤
│ 🚫 ⟳ ⬇️  ⚙️  🔍 Filter                    Preserve log ☑│
├─────────────────────────────────────────────────────┤
│ Name         Status Type   Size     Time   Waterfall│
│ ───────────────────────────────────────────────────│
│ xxx.txt      200    txt    42B      50ms   ▓░░░░░  │ ← 点这一行
│                                                      │
│ ┌──[Headers]─[Preview]─[Response]─[Timing]──────┐ │
│ │ General:                                        │ │
│ │   Request URL: https://...                     │ │
│ │   Status Code: 200 OK                          │ │
│ │                                                 │ │
│ │ Response Headers:                              │ │
│ │   x-cache-status: HIT          ← 🔍 看这里！  │ │
│ │   age: 15                                      │ │
│ │   cache-control: public, max-age=3600         │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## 🧪 高级测试

### 测试1: 连续访问观察Age变化

```bash
# 第1次访问
curl -I https://chigua-r2-worker.xingaikaka.workers.dev/files/xxx.txt?...
# X-Cache-Status: MISS

# 等待5秒
sleep 5

# 第2次访问
curl -I https://chigua-r2-worker.xingaikaka.workers.dev/files/xxx.txt?...
# X-Cache-Status: HIT
# Age: 5

# 等待10秒
sleep 10

# 第3次访问
curl -I https://chigua-r2-worker.xingaikaka.workers.dev/files/xxx.txt?...
# X-Cache-Status: HIT
# Age: 15  ← Age在增长
```

### 测试2: 不同文件类型的TTL

上传不同类型的文件，观察Cache-Control的max-age差异：

```bash
# 文档文件（默认1小时）
Cache-Control: public, max-age=3600

# 图片文件（2小时）
Cache-Control: public, max-age=7200

# HLS文件（30分钟）
Cache-Control: public, max-age=1800
```

### 测试3: 硬刷新（Bypass Cache）

- **普通刷新**: `F5` 或 `Ctrl+R` - 可能使用缓存
- **硬刷新**: `Ctrl+F5` 或 `Ctrl+Shift+R` - 强制重新请求
- **清空缓存刷新**: F12 → 右键刷新按钮 → Empty Cache and Hard Reload

---

## 🐛 常见问题排查

### Q1: 看不到 X-Cache-Status 头？

**可能原因**：
1. 响应头被过滤了
   - 解决：在Headers标签中，确保选择 "Response Headers"（不是"Request Headers"）
   
2. 文件不在缓存范围内
   - 检查：文件是否是 documents/ 或 HLS文件
   
3. CDN缓存未启用
   - 验证：访问 `/api/cache/stats` 检查 `cacheEnabled`

### Q2: 总是显示 MISS？

**可能原因**：
1. 每次都用不同的URL（签名参数变化）
   - 解决：使用同一个previewUrl测试
   
2. 文件太大（>10MB）
   - 解决：上传小文件测试
   
3. 浏览器缓存干扰
   - 解决：在Network标签勾选 "Disable cache"

### Q3: 时间差异不明显？

**可能原因**：
1. 网络条件好，R2响应也很快
2. 测试文件太小
3. 地理位置接近Cloudflare节点

**建议**：
- 上传稍大的文件（1-5MB）
- 多次测试取平均值
- 使用不同地区的网络测试

---

## 📈 性能对比示例

### 真实测试数据

| 请求 | X-Cache-Status | 响应时间 | TTFB | 提升 |
|------|----------------|----------|------|------|
| 1st  | MISS          | 387ms    | 268ms| 基准 |
| 2nd  | HIT           | 52ms     | 35ms | ↓87% |
| 3rd  | HIT           | 48ms     | 32ms | ↓88% |
| 4th  | HIT           | 51ms     | 34ms | ↓87% |

**结论**: CDN缓存使响应速度提升了 **87%**，TTFB降低了 **88%**！

---

## 🎓 学习要点

1. **X-Cache-Status 是最可靠的指标**
   - MISS = 未命中，从源读取
   - HIT = 命中，从缓存读取

2. **Age 头显示缓存年龄**
   - Age越大，说明缓存使用越久
   - 超过max-age会重新验证

3. **响应时间是性能指标**
   - 缓存命中时间显著降低
   - 通常能提升75-90%

4. **合理使用浏览器工具**
   - Network标签查看请求详情
   - Timing标签分析性能瓶颈
   - Preserve log保留历史记录

---

## ✅ 测试检查清单

- [ ] F12开发者工具已打开
- [ ] Network标签已选中
- [ ] 已清空之前的记录
- [ ] 第一次访问看到 X-Cache-Status: MISS
- [ ] 第二次访问看到 X-Cache-Status: HIT
- [ ] 响应时间明显变快（>50%提升）
- [ ] Age头正确显示缓存年龄
- [ ] Timing数据对比记录完整

---

**测试完成后，记得查看缓存统计**：
```bash
curl https://chigua-r2-worker.xingaikaka.workers.dev/api/cache/stats
```

好好享受CDN加速带来的性能提升吧！🚀

