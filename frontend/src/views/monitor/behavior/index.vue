<template>
  <div class="app-container">
    <!-- 顶部概览 -->
    <el-row :gutter="12" class="mb12">
      <el-col :span="4">
        <el-card shadow="hover" class="ovw-card">
          <div class="ovw-label">总事件数</div>
          <div class="ovw-num">{{ overview.totalEvents || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="ovw-card">
          <div class="ovw-label">独立 IP</div>
          <div class="ovw-num">{{ overview.uniqueIps || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="ovw-card">
          <div class="ovw-label">独立指纹</div>
          <div class="ovw-num">{{ overview.uniqueFingerprints || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="ovw-card">
          <div class="ovw-label">独立 anonId</div>
          <div class="ovw-num">{{ overview.uniqueAnonIds || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="ovw-card">
          <div class="ovw-label">国内 / 海外</div>
          <div class="ovw-num">
            <span style="color:#67c23a">{{ overview.chinaEvents || 0 }}</span>
            <span style="color:#909399"> / </span>
            <span style="color:#e6a23c">{{ overview.overseasEvents || 0 }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="ovw-card">
          <div class="ovw-label">PV / 搜索 / 播放</div>
          <div class="ovw-num">
            {{ overview.pvCount || 0 }}
            <span style="color:#909399">/</span>
            {{ overview.searchCount || 0 }}
            <span style="color:#909399">/</span>
            {{ overview.playCount || 0 }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mb12">
      <div slot="header">
        <span>事件类型分布</span>
        <span style="float:right;color:#909399;font-size:12px">
          内存队列堆积：{{ overview.queueSize || 0 }} 条 ｜ {{ overviewRange }}
        </span>
      </div>
      <el-table :data="breakdown" size="mini" border>
        <el-table-column label="事件类型">
          <template slot-scope="s">
            <el-tag size="mini" :type="tagType(s.row.eventType)">{{ eventLabel(s.row.eventType) }}</el-tag>
            <span style="margin-left:6px;color:#909399">{{ s.row.eventType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cnt" label="次数" align="right" width="180" />
      </el-table>
    </el-card>

    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="78px">
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item label="IP">
        <el-input v-model="queryParams.ip" placeholder="精确 IP" clearable style="width:170px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="归属地">
        <el-input v-model="queryParams.ipRegion" placeholder="如 中国 / 美国" clearable style="width:140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="国家">
        <el-select v-model="queryParams.isChina" placeholder="全部" clearable style="width:100px">
          <el-option label="国内" :value="1" />
          <el-option label="海外" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备">
        <el-select v-model="queryParams.deviceType" placeholder="全部" clearable style="width:110px">
          <el-option label="桌面"   value="desktop" />
          <el-option label="移动"   value="mobile" />
          <el-option label="平板"   value="tablet" />
        </el-select>
      </el-form-item>
      <el-form-item label="事件类型">
        <el-select v-model="queryParams.eventType" placeholder="全部" clearable style="width:160px">
          <el-option v-for="t in eventTypes" :key="t" :label="eventLabel(t)+' ('+t+')'" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab" @tab-click="onTabClick">
      <!-- Tab 1：按 IP 聚合 -->
      <el-tab-pane label="按 IP 聚合" name="ip">
        <el-table v-loading="loading" :data="ipList" border size="small" style="width:100%">
          <el-table-column prop="ip" label="IP" min-width="160">
            <template slot-scope="s">
              <el-link type="primary" @click="openTimeline(s.row.ip)">{{ s.row.ip }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="ipRegion" label="归属地" min-width="200" show-overflow-tooltip />
          <el-table-column label="国家" width="70" align="center">
            <template slot-scope="s">
              <el-tag size="mini" :type="isCN(s.row) ? 'success' : 'warning'">
                {{ isCN(s.row) ? '国内' : '海外' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deviceType" label="设备" width="80" align="center" />
          <el-table-column prop="browser" label="浏览器" width="90" />
          <el-table-column prop="os" label="系统" width="90" />
          <el-table-column prop="eventCount" label="事件数" width="80" align="right" sortable />
          <el-table-column prop="pvCount" label="PV" width="60" align="right" />
          <el-table-column prop="searchCount" label="搜索" width="60" align="right" />
          <el-table-column prop="playCount" label="播放" width="60" align="right" />
          <el-table-column prop="catClickCount" label="分类" width="60" align="right" />
          <el-table-column prop="pageCount" label="页面数" width="70" align="right" />
          <el-table-column prop="activeDays" label="活跃天数" width="80" align="right" />
          <el-table-column prop="anonCount" label="anonId" width="70" align="right" />
          <el-table-column prop="firstSeen" label="首次" width="150" :formatter="dateFmt" />
          <el-table-column prop="lastSeen" label="最近" width="150" :formatter="dateFmt" />
        </el-table>
        <pagination
          v-show="ipTotal > 0"
          :total="ipTotal"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="loadIpList"
        />
      </el-tab-pane>

      <!-- Tab 2：原始事件流 -->
      <el-tab-pane label="原始事件流" name="event">
        <el-form :inline="true" size="mini" style="margin-bottom:8px">
          <el-form-item label="关键词">
            <el-input v-model="queryParams.keyword" placeholder="搜索词模糊" clearable style="width:150px" @keyup.enter.native="loadEventList" />
          </el-form-item>
          <el-form-item label="页面路径前缀">
            <el-input v-model="queryParams.pagePath" placeholder="如 /tg/post" clearable style="width:180px" @keyup.enter.native="loadEventList" />
          </el-form-item>
          <el-form-item label="目标 ID">
            <el-input v-model="queryParams.eventTarget" placeholder="videoId/categoryId" clearable style="width:150px" @keyup.enter.native="loadEventList" />
          </el-form-item>
          <el-button type="primary" size="mini" icon="el-icon-search" @click="loadEventList">查询</el-button>
        </el-form>
        <el-table v-loading="loading" :data="eventList" border size="small">
          <el-table-column prop="eventTime" label="时间" width="155" :formatter="dateFmt" />
          <el-table-column label="事件" width="130">
            <template slot-scope="s">
              <el-tag size="mini" :type="tagType(s.row.eventType)">{{ eventLabel(s.row.eventType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="IP / 归属地" width="240">
            <template slot-scope="s">
              <el-link type="primary" @click="openTimeline(s.row.ip)" style="font-size:12px">{{ s.row.ip }}</el-link>
              <el-tag size="mini" :type="isCN(s.row) ? 'success' : 'warning'" style="margin-left:4px">
                {{ isCN(s.row) ? '国内' : '海外' }}
              </el-tag>
              <div style="font-size:11px;color:#909399;margin-top:2px">{{ s.row.ipRegion || '—' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="对象（点击跳转 C 端）" min-width="280">
            <template slot-scope="s">
              <div v-if="targetUrl(s.row)">
                <el-link :href="targetUrl(s.row)" target="_blank" type="primary" style="font-size:12px">
                  <i class="el-icon-position"></i>
                  {{ targetLabel(s.row) }}
                </el-link>
                <div v-if="s.row.eventTargetTitle" style="font-size:11px;color:#606266;margin-top:2px" :title="s.row.eventTargetTitle">
                  {{ s.row.eventTargetTitle }}
                </div>
              </div>
              <span v-else style="color:#c0c4cc">—</span>
            </template>
          </el-table-column>
          <el-table-column label="关键词 / 路径" min-width="260">
            <template slot-scope="s">
              <span v-if="s.row.keyword" style="color:#e6a23c;font-weight:600">
                关键词：{{ decodeStr(s.row.keyword) }}
              </span>
              <div v-if="s.row.pagePath" style="font-size:11px;color:#909399" :title="decodeStr(s.row.pagePath)">
                {{ decodeStr(s.row.pagePath) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="deviceType" label="设备" width="65" />
          <el-table-column prop="browser" label="浏览器" width="80" />
          <el-table-column label="anonId" width="120" show-overflow-tooltip>
            <template slot-scope="s">
              <span style="font-size:11px;color:#909399">{{ s.row.anonymousId || '—' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="eventTotal > 0"
          :total="eventTotal"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="loadEventList"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 时间线对话框 -->
    <el-dialog :visible.sync="timelineVisible" :title="`IP ${timelineIp} 行为时间线`" width="1100px" append-to-body>
      <el-table :data="timelineEvents" border size="mini" max-height="600" v-loading="timelineLoading">
        <el-table-column prop="eventTime" label="时间" width="155" :formatter="dateFmt" />
        <el-table-column label="事件" width="130">
          <template slot-scope="s">
            <el-tag size="mini" :type="tagType(s.row.eventType)">{{ eventLabel(s.row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="280">
          <template slot-scope="s">
            <div v-if="targetUrl(s.row)">
              <el-link :href="targetUrl(s.row)" target="_blank" type="primary" style="font-size:12px">
                <i class="el-icon-position"></i> {{ targetLabel(s.row) }}
              </el-link>
              <div v-if="s.row.eventTargetTitle" style="font-size:11px;color:#606266;margin-top:2px">
                {{ s.row.eventTargetTitle }}
              </div>
            </div>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="关键词 / 路径" min-width="260">
          <template slot-scope="s">
            <span v-if="s.row.keyword" style="color:#e6a23c;font-weight:600">
              {{ decodeStr(s.row.keyword) }}
            </span>
            <div v-if="s.row.pagePath" style="font-size:11px;color:#909399">{{ decodeStr(s.row.pagePath) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="anonId" width="120" show-overflow-tooltip>
          <template slot-scope="s">
            <span style="font-size:11px;color:#909399">{{ s.row.anonymousId || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div style="text-align:right;color:#909399;margin-top:6px;font-size:12px">共 {{ timelineEvents.length }} 条（最大 500）</div>
    </el-dialog>
  </div>
</template>

<script>
import { getOverview, listIp, ipTimeline, listEvent } from '@/api/monitor/behavior'

const C_SITE = 'https://tycg7.com'

// 事件类型中文标签
const EVENT_LABELS = {
  page_view:         '页面浏览',
  search_submit:     '搜索',
  search_no_result:  '搜索-无结果',
  category_click:    '分类点击',
  video_view:        '视频浏览',
  video_play:        '视频播放',
  video_like:        '视频点赞',
  video_unlike:      '取消点赞',
  video_share:       '视频分享',
  ad_click:          '广告点击',
  comment_submit:    '提交评论',
  collection_view:   '合集查看',
  tag_click:         '标签点击',
  // Redgifs 独立 ID 体系
  redgifs_view:      'Redgifs 浏览',
  redgifs_play:      'Redgifs 播放',
  redgifs_share:     'Redgifs 分享',
  redgifs_user_view: 'Redgifs 用户页',
  // Telegram
  tg_post_view:      'TG 帖子浏览',
  tg_media_play:     'TG 媒体播放'
}

export default {
  name: 'MonitorBehavior',
  data() {
    return {
      loading: false,
      activeTab: 'ip',
      dateRange: [],
      overview: {},
      breakdown: [],
      overviewRange: '',
      eventTypes: Object.keys(EVENT_LABELS),
      queryParams: {
        ip: '', ipRegion: '', isChina: null, deviceType: '', eventType: '',
        keyword: '', pagePath: '', eventTarget: '',
        pageNum: 1, pageSize: 20
      },
      ipList: [], ipTotal: 0,
      eventList: [], eventTotal: 0,
      timelineVisible: false, timelineLoading: false,
      timelineIp: '', timelineEvents: []
    }
  },
  created() {
    const end = new Date()
    const start = new Date(); start.setDate(start.getDate() - 6)
    const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    this.dateRange = [fmt(start), fmt(end)]
    this.loadOverview()
    this.loadIpList()
  },
  methods: {
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadOverview()
      if (this.activeTab === 'ip') this.loadIpList()
      else this.loadEventList()
    },
    resetQuery() {
      this.queryParams = {
        ip: '', ipRegion: '', isChina: null, deviceType: '', eventType: '',
        keyword: '', pagePath: '', eventTarget: '', pageNum: 1, pageSize: 20
      }
      const end = new Date(); const start = new Date(); start.setDate(start.getDate() - 6)
      const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
      this.dateRange = [fmt(start), fmt(end)]
      this.handleQuery()
    },
    buildParams() {
      const p = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        p.startDate = this.dateRange[0]; p.endDate = this.dateRange[1]
      }
      Object.keys(p).forEach(k => { if (p[k] === '' || p[k] === null) delete p[k] })
      return p
    },
    loadOverview() {
      getOverview(this.buildParams()).then(res => {
        if (res.code === 200 && res.data) {
          this.overview = res.data.overview || {}
          this.breakdown = res.data.breakdown || []
          this.overview.queueSize = res.data.queueSize
          this.overviewRange = `${res.data.startDate} ~ ${res.data.endDate}`
        }
      })
    },
    loadIpList() {
      this.loading = true
      listIp(this.buildParams()).then(res => {
        this.ipList = res.rows || []
        this.ipTotal = res.total || 0
      }).finally(() => { this.loading = false })
    },
    loadEventList() {
      this.loading = true
      listEvent(this.buildParams()).then(res => {
        this.eventList = res.rows || []
        this.eventTotal = res.total || 0
      }).finally(() => { this.loading = false })
    },
    onTabClick() {
      this.queryParams.pageNum = 1
      if (this.activeTab === 'ip') this.loadIpList()
      else this.loadEventList()
    },
    openTimeline(ip) {
      this.timelineIp = ip
      this.timelineVisible = true
      this.timelineLoading = true
      const p = { limit: 500 }
      if (this.dateRange && this.dateRange.length === 2) {
        p.startDate = this.dateRange[0]; p.endDate = this.dateRange[1]
      }
      ipTimeline(ip, p).then(res => {
        this.timelineEvents = (res.data && res.data.events) || []
      }).finally(() => { this.timelineLoading = false })
    },
    dateFmt(row, col, val) {
      if (!val) return ''
      try {
        const d = new Date(val)
        return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`
      } catch (e) { return val }
    },
    eventLabel(t) { return EVENT_LABELS[t] || t || '—' },
    tagType(t) {
      switch (t) {
        case 'page_view': return ''
        case 'search_submit': return 'success'
        case 'search_no_result': return 'warning'
        case 'category_click': return 'info'
        case 'video_view': case 'video_play':
        case 'redgifs_view': case 'redgifs_play':
        case 'tg_media_play':
          return 'success'
        case 'video_like': case 'video_share':
        case 'redgifs_share':
          return 'warning'
        case 'video_unlike': return 'danger'
        case 'ad_click': return 'danger'
        case 'comment_submit': return 'success'
        case 'collection_view': return ''
        case 'tag_click': return 'info'
        case 'tg_post_view': case 'redgifs_user_view': return 'info'
        default: return 'info'
      }
    },
    // 健壮 isChina 判断：兼容 1/0、'1'/'0'、true/false、null；并用 ipRegion 兜底
    isCN(row) {
      const v = row.isChina
      if (v === 1 || v === '1' || v === true) return true
      if (v === 0 || v === '0' || v === false) return false
      const r = row.ipRegion || ''
      return typeof r === 'string' && (r.startsWith('中国') || r.indexOf('|CN') >= 0)
    },
    decodeStr(s) {
      if (!s) return ''
      try { return decodeURIComponent(s) } catch (e) { return s }
    },
    // 计算事件对应的 C 端 URL
    targetUrl(row) {
      const t = row.eventType
      const id = row.eventTarget
      const path = row.pagePath
      // 优先 page_path（最直接）
      if (path) {
        return C_SITE + (path.startsWith('/') ? path : '/' + path)
      }
      if (!id) return ''
      if (t === 'comment_submit')          return id ? `${C_SITE}/video/${id}` : ''
      if (t === 'collection_view')         return `${C_SITE}/collection/${id}`
      if (t === 'tag_click')               return `${C_SITE}/tag/${id}`
      if (t && t.startsWith('video_'))     return `${C_SITE}/video/${id}`
      if (t === 'category_click')          return `${C_SITE}/category/${id}`
      if (t === 'tg_post_view' || t === 'tg_media_play') return `${C_SITE}/tg/post/${id}`
      // Redgifs：只能定位到视频 ID（没有 username 上下文时打开 ID 形式的直链）
      if (t === 'redgifs_view' || t === 'redgifs_play' || t === 'redgifs_share') {
        return `${C_SITE}/user/_/video/${id}`
      }
      if (t === 'redgifs_user_view')       return `${C_SITE}/user/${id}`
      return ''
    },
    targetLabel(row) {
      const t = row.eventType
      const id = row.eventTarget
      if (row.pagePath) {
        const decoded = this.decodeStr(row.pagePath)
        return decoded.length > 60 ? decoded.slice(0, 60) + '…' : decoded
      }
      if (!id) {
        if (t === 'comment_submit') return '投稿评论'
        return '—'
      }
      if (t === 'comment_submit')          return `视频 #${id} 的评论`
      if (t === 'collection_view')         return `合集 #${id}`
      if (t === 'tag_click')               return `标签 #${id}`
      if (t && t.startsWith('video_'))     return `视频 #${id}`
      if (t === 'category_click')          return `分类 #${id}`
      if (t === 'ad_click')                return `广告 #${id}`
      if (t === 'tg_post_view' || t === 'tg_media_play') return `TG 帖子 #${id}`
      if (t === 'redgifs_view' || t === 'redgifs_play' || t === 'redgifs_share') return `Redgifs #${id}`
      if (t === 'redgifs_user_view')       return `Redgifs 用户 #${id}`
      return `#${id}`
    }
  }
}
</script>

<style scoped>
.mb8  { margin-bottom: 8px; }
.mb12 { margin-bottom: 12px; }
.ovw-card .ovw-label { color:#909399; font-size: 13px; margin-bottom: 6px; }
.ovw-card .ovw-num   { font-size: 22px; font-weight: 600; color:#303133; }
</style>
