<template>
  <div class="app-container">
    <el-row :gutter="12">
      <el-col :span="4" v-for="card in cards" :key="card.key">
        <el-card shadow="hover" class="metric-card" :body-style="{ padding: '12px' }">
          <div class="metric-header" :style="{ backgroundColor: card.color }">
            <i :class="card.icon" class="metric-icon"></i>
            <span class="metric-title">{{ card.title }}</span>
          </div>
          <div class="metric-value">{{ overview[card.key] || 0 }}</div>
          <div class="metric-date">{{ overview.date || '' }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="12" class="mt16">
      <el-col :span="24">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>日活用户 / 独立访客 / 页面浏览量 趋势</span>
          </div>
          <div class="center-controls">
            <el-radio-group v-model="tsMode1" size="mini" @change="refreshSeries1">
              <el-radio-button label="day">日</el-radio-button>
              <el-radio-button label="month">月</el-radio-button>
            </el-radio-group>
          </div>
          <multi-line-chart :chart-data="series1" :chart-type="'bar'" height="340px" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="mt16">
      <el-col :span="14">
        <el-card shadow="never" class="panel-fixed">
          <div slot="header" class="clearfix">
            <span>Top 100 视频</span>
            <el-select v-model="range" size="small" class="fr" style="width:140px" @change="loadTop">
              <el-option label="今日" value="today"/>
              <el-option label="上升最快" value="7d"/>
            </el-select>
          </div>
          <el-table :data="topVideosView" size="small" height="520">
            <el-table-column type="index" label="#" width="60"/>
            <el-table-column prop="title" label="标题" min-width="300"/>
            <el-table-column prop="categoryName" label="分类" width="140"/>
            <el-table-column :label="range === '7d' ? '今日播放量' : '播放次数'" width="140">
              <template slot-scope="scope">
                <span>{{ scope.row.playCount }}</span>
                <span v-if="range === '7d' && scope.row.growthCount != null" style="color:#67c23a;font-size:11px;margin-left:4px">
                  +{{ scope.row.growthCount }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="panel-fixed">
          <div slot="header" class="clearfix">
            <span>分类点击排行</span>
            <span style="font-size:11px;color:#909399;margin-left:8px">按用户点击分类标签统计</span>
            <el-select v-model="catPeriod" size="small" class="fr" style="width:160px" @change="loadCategoryRanking">
              <el-option label="今日" value="day"/>
              <el-option label="近7天" value="week"/>
              <el-option label="近30天" value="month"/>
            </el-select>
          </div>
          <el-table :data="categoryRanking" size="small" height="520">
            <el-table-column type="index" label="#" width="60"/>
            <el-table-column prop="categoryName" label="分类" min-width="180"/>
            <el-table-column prop="clicks" label="点击量" width="100"/>
            <el-table-column prop="categoryId" label="分类ID" width="100"/>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索关键词排行 -->
    <el-row :gutter="12" class="mt16">
      <el-col :span="24">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>搜索关键词排行</span>
            <span style="font-size:11px;color:#909399;margin-left:8px">共 {{ searchKeywords.length }} 个关键词</span>
            <div class="fr" style="display:flex;align-items:center;gap:8px;">
              <el-date-picker
                v-model="kwDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="small"
                value-format="yyyy-MM-dd"
                :picker-options="kwPickerOptions"
                style="width:240px"
                @change="loadSearchKeywords"
              />
              <el-button size="small" icon="el-icon-refresh" @click="loadSearchKeywords">刷新</el-button>
            </div>
          </div>
          <el-table :data="searchKeywords" size="small" max-height="500" v-loading="kwLoading">
            <el-table-column type="index" label="#" width="60"/>
            <el-table-column prop="keyword" label="关键词" min-width="200"/>
            <el-table-column prop="count" label="搜索次数" width="120"/>
            <el-table-column label="占比" min-width="200">
              <template slot-scope="scope">
                <el-progress
                  :percentage="kwMaxCount > 0 ? Math.round(scope.row.count / kwMaxCount * 100) : 0"
                  :show-text="false"
                  stroke-width="8"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
  
</template>

<script>
import { getOverview, getTopVideos, getCategoryRanking, getTimeSeries, getSearchKeywords } from '@/api/chigua/stats'
import { listCategory } from '@/api/chigua/category'
import MultiLineChart from './MultiLineChart.vue'

export default {
  name: 'StatsDashboard',
  components: { MultiLineChart },
  data() {
    return {
      overview: {},
      topVideos: [],
      topVideosView: [],
      categoryDict: {},
      range: 'today',
      categoryRanking: [],
      catPeriod: 'day',
      // 时序
      tsMode1: 'day',
      series1: { labels: [], series: [] },
      cards: [
        { key: 'dau', title: '日活用户（DAU）', color: '#F0F9EB', icon: 'el-icon-user-solid' },
        { key: 'uv', title: '独立访客（UV）', color: '#F5F7FA', icon: 'el-icon-user' },
        { key: 'view_count', title: '页面浏览量', color: '#ECF5FF', icon: 'el-icon-view' },
        { key: 'play_starts', title: '播放次数', color: '#F0F9FF', icon: 'el-icon-video-camera' },
        { key: 'searches', title: '搜索次数', color: '#FFF4E6', icon: 'el-icon-search' },
        { key: 'shares', title: '分享次数', color: '#FDF6EC', icon: 'el-icon-share' }
      ],
      // 搜索关键词
      searchKeywords: [],
      kwLoading: false,
      kwDateRange: (() => { const t = new Date().toISOString().split('T')[0]; return [t, t] })(),
      kwPickerOptions: {
        disabledDate(time) {
          return time.getTime() > Date.now()
        }
      }
    }
  },
  computed: {
    kwMaxCount() {
      if (!this.searchKeywords || this.searchKeywords.length === 0) return 0
      return this.searchKeywords[0].count || 0
    }
  },
  created() {
    this.loadOverview()
    this.loadTop()
    this.loadCategoryRanking()
    this.loadCategories()
    this.refreshSeries1()
    this.loadSearchKeywords()
  },
  methods: {
    async loadCategories() {
      try {
        const { rows } = await listCategory({ status: 1 })
        const dict = {}
        ;(rows || []).forEach(c => { dict[c.id] = c.name })
        this.categoryDict = dict
        this.mapTopVideos()
      } catch (e) {}
    },
    async refreshSeries1() {
      // DAU/UV/PV 组合
      const period = this.tsMode1
      const [dau, uv, pv] = await Promise.all([
        getTimeSeries('dau', period),
        getTimeSeries('uv', period),
        getTimeSeries('pv', period)
      ])
      const labels = (dau.data?.labels) || []
      this.series1 = {
        labels,
        series: [
          { name: '日活用户(DAU)', data: dau.data?.values || [] },
          { name: '独立访客(UV)', data: uv.data?.values || [] },
          { name: '页面浏览量(PV)', data: pv.data?.values || [] }
        ]
      }
    },
    async loadOverview() {
      try {
        const { data } = await getOverview('today')
        this.overview = data || {}
      } catch (e) {
        this.$message.error('概览数据加载失败')
      }
    },
    async loadTop() {
      try {
        const { data } = await getTopVideos({ date: this.overview.date, range: this.range, limit: 100 })
        this.topVideos = data || []
        this.mapTopVideos()
      } catch (e) {
        this.$message.error('Top视频加载失败')
      }
    },
    mapTopVideos() {
      this.topVideosView = (this.topVideos || []).map(v => ({
        ...v,
        categoryName: this.categoryDict[v.categoryId] || v.categoryId
      }))
    },
    async loadCategoryRanking() {
      try {
        const { data } = await getCategoryRanking({ period: this.catPeriod, limit: 20 })
        this.categoryRanking = data || []
      } catch (e) {
        this.$message.error('分类排行加载失败')
      }
    },
    async loadSearchKeywords() {
      this.kwLoading = true
      try {
        let params = { limit: 200 }
        if (this.kwDateRange && this.kwDateRange.length === 2 && this.kwDateRange[0]) {
          const [start, end] = this.kwDateRange
          if (start === end) {
            params.date = start
          } else {
            params.startDate = start
            params.endDate = end
          }
        } else {
          // 默认查今日
          const today = new Date().toISOString().split('T')[0]
          params.date = today
        }
        const { data } = await getSearchKeywords(params)
        this.searchKeywords = data || []
      } catch (e) {
        this.$message.error('搜索关键词加载失败')
      } finally {
        this.kwLoading = false
      }
    }
  }
}
</script>

<style scoped>
.app-container { padding: 10px; }
.mt16 { margin-top: 16px; }
.fr { float: right; }
.center-controls { display:flex; justify-content:center; margin: 4px 0 8px; }
.stat-title { font-size: 12px; color: #909399; }
.stat-value { font-size: 22px; font-weight: 600; margin-top: 6px; }
.stat-sub { font-size: 12px; color: #C0C4CC; margin-top: 4px; }
.panel-fixed { height: 600px; display: flex; flex-direction: column; }
.panel-fixed >>> .el-card__body { flex: 1; overflow: hidden; display: flex; }
.panel-fixed >>> .el-table { flex: 1; overflow: auto; }

/* 统计卡片样式 */
/* 还原到metric卡片方案所需样式 */
.metric-card { border-radius: 8px; overflow: hidden; }
.metric-header { display: flex; align-items: center; gap: 6px; padding: 8px 10px; border-radius: 6px; }
.metric-icon { color: #606266; font-size: 16px; }
.metric-title { color: #606266; font-size: 12px; }
.metric-value { font-size: 24px; font-weight: 700; color: #303133; padding: 8px 10px 0 10px; }
.metric-date { font-size: 12px; color: #909399; padding: 4px 10px 8px 10px; }
</style>


