<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card" class="stats-tabs">

      <!-- ===== Tab 1: 概览 ===== -->
      <el-tab-pane label="概览" name="overview">
        <!-- 6 指标卡 -->
        <el-row :gutter="12" class="mb16">
          <el-col :span="4" v-for="card in cards" :key="card.key">
            <el-card shadow="hover" class="metric-card" :body-style="{ padding: '12px' }">
              <div class="metric-header" :style="{ backgroundColor: card.color }">
                <i :class="card.icon" class="metric-icon"></i>
                <span class="metric-title">{{ card.title }}</span>
              </div>
              <div class="metric-value">{{ overview[card.key] || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- DAU/UV/PV 趋势图 -->
        <el-card shadow="never" class="mb16">
          <div slot="header" class="clearfix">
            <span>日活用户 / 独立访客 / 页面浏览量 趋势</span>
            <div class="header-controls">
              <el-radio-group v-model="tsMode1" size="mini" @change="refreshSeries1">
                <el-radio-button label="day">日</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <multi-line-chart :chart-data="series1" :chart-type="'bar'" height="300px" />
        </el-card>

        <!-- 新用户增长趋势图 -->
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>新用户增长趋势</span>
            <div class="header-controls">
              <el-radio-group v-model="newUserMode" size="mini" @change="refreshNewUserSeries">
                <el-radio-button label="day">日</el-radio-button>
                <el-radio-button label="month">月</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <multi-line-chart :chart-data="newUserSeries" :chart-type="'bar'" height="260px" />
        </el-card>
      </el-tab-pane>

      <!-- ===== Tab 2: 用户 ===== -->
      <el-tab-pane label="用户" name="user">
        <!-- 留存汇总 + 每日留存明细 -->
        <el-row :gutter="12" class="mb16">
          <el-col :span="10">
            <el-card shadow="never">
              <div slot="header" class="clearfix">
                <span>留存汇总</span>
                <el-date-picker
                  v-model="retentionRange"
                  type="daterange"
                  size="small"
                  value-format="yyyy-MM-dd"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  class="fr retention-picker"
                  :picker-options="retentionPickerOptions"
                  @change="loadRetention"
                />
              </div>
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="统计区间">
                  {{ retentionSummary.startDate || '--' }} ~ {{ retentionSummary.endDate || '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="新增用户">{{ retentionSummary.totalNewUsers || 0 }}</el-descriptions-item>
                <el-descriptions-item label="D1 留存">
                  {{ formatPercent(retentionSummary.d1Rate) }} ({{ retentionSummary.totalD1Retained || 0 }})
                </el-descriptions-item>
                <el-descriptions-item label="D7 留存">
                  {{ formatPercent(retentionSummary.d7Rate) }} ({{ retentionSummary.totalD7Retained || 0 }})
                </el-descriptions-item>
                <el-descriptions-item label="D30 留存">
                  {{ formatPercent(retentionSummary.d30Rate) }} ({{ retentionSummary.totalD30Retained || 0 }})
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never">
              <div slot="header" class="clearfix"><span>每日留存明细</span></div>
              <el-table :data="retentionSeries" size="small" border height="320" v-loading="retentionLoading">
                <el-table-column prop="date" label="日期" width="120"/>
                <el-table-column prop="newUsers" label="新增" width="80"/>
                <el-table-column label="D1 留存" min-width="120">
                  <template slot-scope="scope">{{ formatPercent(scope.row.d1Rate) }} ({{ scope.row.d1Retained }})</template>
                </el-table-column>
                <el-table-column label="D7 留存" min-width="120">
                  <template slot-scope="scope">{{ formatPercent(scope.row.d7Rate) }} ({{ scope.row.d7Retained }})</template>
                </el-table-column>
                <el-table-column label="D30 留存" min-width="120">
                  <template slot-scope="scope">{{ formatPercent(scope.row.d30Rate) }} ({{ scope.row.d30Retained }})</template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <!-- 24小时活跃时段图 -->
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>24小时活跃时段分布</span>
            <span style="font-size:11px;color:#909399;margin-left:8px">按事件上报量统计</span>
            <div class="fr" style="display:flex;align-items:center;gap:8px;">
              <el-date-picker
                v-model="hourlyDate"
                type="date"
                size="small"
                value-format="yyyy-MM-dd"
                placeholder="选择日期"
                style="width:140px"
                :picker-options="pastOnlyOptions"
                @change="loadHourlyActivity"
              />
              <el-button size="small" icon="el-icon-refresh" @click="loadHourlyActivity">刷新</el-button>
            </div>
          </div>
          <div v-loading="hourlyLoading">
            <div ref="hourlyChart" style="height:240px;width:100%"></div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ===== Tab 3: 内容 ===== -->
      <el-tab-pane label="内容" name="content">
        <el-card shadow="never" style="margin-bottom:16px">
          <div slot="header" class="clearfix"><span>Top详细统计</span></div>
          <top100-dashboard />
        </el-card>

        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>分类统计排行</span>
            <span style="font-size:11px;color:#909399;margin-left:8px">分类点击量 · 视频播放数</span>
            <el-select v-model="catPeriod" size="small" class="fr" style="width:160px" @change="loadCategoryRanking">
              <el-option label="今日" value="day"/>
              <el-option label="近7天" value="week"/>
              <el-option label="近30天" value="month"/>
            </el-select>
          </div>
          <el-table :data="categoryRanking" size="small" height="400" v-loading="catLoading">
            <el-table-column type="index" label="#" width="60"/>
            <el-table-column prop="categoryName" label="分类" min-width="180"/>
            <el-table-column prop="clicks" label="点击量" width="120" sortable/>
            <el-table-column prop="plays" label="播放数" width="120" sortable/>
            <el-table-column prop="categoryId" label="分类ID" width="100"/>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ===== Tab 4: 搜索 ===== -->
      <el-tab-pane label="搜索" name="search">
        <el-row :gutter="12">
          <!-- 搜索关键词排行 -->
          <el-col :span="14">
            <el-card shadow="never">
              <div slot="header" class="clearfix">
                <span>搜索关键词排行</span>
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
              <el-table :data="searchKeywords" size="small" height="400" v-loading="kwLoading">
                <el-table-column type="index" label="#" width="50"/>
                <el-table-column prop="keyword" label="关键词" min-width="160"/>
                <el-table-column prop="count" label="搜索次数" width="100"/>
                <el-table-column label="占比" min-width="160">
                  <template slot-scope="scope">
                    <el-progress
                      :percentage="kwMaxCount > 0 ? Math.round(scope.row.count / kwMaxCount * 100) : 0"
                      :show-text="false" stroke-width="8"
                    />
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <!-- 零结果关键词 -->
          <el-col :span="10">
            <el-card shadow="never">
              <div slot="header" class="clearfix">
                <span>零结果关键词</span>
                <span style="font-size:11px;color:#909399;margin-left:8px">内容缺口分析</span>
                <div class="fr" style="display:flex;align-items:center;gap:8px;">
                  <el-date-picker
                    v-model="nrDate"
                    type="date"
                    size="small"
                    value-format="yyyy-MM-dd"
                    placeholder="选择日期"
                    style="width:140px"
                    :picker-options="pastOnlyOptions"
                    @change="loadNoResultKeywords"
                  />
                  <el-button size="small" icon="el-icon-refresh" @click="loadNoResultKeywords">刷新</el-button>
                </div>
              </div>
              <el-table :data="noResultKeywords" size="small" height="400" v-loading="nrLoading">
                <el-table-column prop="rank" label="#" width="50"/>
                <el-table-column prop="keyword" label="关键词" min-width="160"/>
                <el-table-column prop="count" label="零结果次数" width="110"/>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ===== Tab 5: 内容健康度 ===== -->
      <el-tab-pane label="内容健康度" name="health">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span>内容库健康度分布</span>
            <span style="font-size:11px;color:#909399;margin-left:8px">按视频播放量分级统计</span>
            <div class="fr" style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:13px;color:#606266">统计近</span>
              <el-select v-model="healthDays" size="small" style="width:90px" @change="loadContentHealth">
                <el-option label="7 天" :value="7"/>
                <el-option label="30 天" :value="30"/>
                <el-option label="90 天" :value="90"/>
              </el-select>
              <el-button size="small" icon="el-icon-refresh" @click="loadContentHealth">刷新</el-button>
            </div>
          </div>
          <el-row :gutter="16" class="mb16" v-loading="healthLoading">
            <el-col :span="6">
              <el-card shadow="hover" :body-style="{ padding: '20px', textAlign: 'center' }">
                <div style="font-size:28px;font-weight:bold;color:#F56C6C">{{ contentHealth.zeroPlays || 0 }}</div>
                <div style="font-size:13px;color:#909399;margin-top:8px">零播放视频</div>
                <div style="font-size:11px;color:#C0C4CC;margin-top:4px">近{{ healthDays }}天 0 次播放</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" :body-style="{ padding: '20px', textAlign: 'center' }">
                <div style="font-size:28px;font-weight:bold;color:#E6A23C">{{ contentHealth.lowPlays || 0 }}</div>
                <div style="font-size:13px;color:#909399;margin-top:8px">低播放视频</div>
                <div style="font-size:11px;color:#C0C4CC;margin-top:4px">1 ~ 10 次播放</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" :body-style="{ padding: '20px', textAlign: 'center' }">
                <div style="font-size:28px;font-weight:bold;color:#409EFF">{{ contentHealth.midPlays || 0 }}</div>
                <div style="font-size:13px;color:#909399;margin-top:8px">中等播放视频</div>
                <div style="font-size:11px;color:#C0C4CC;margin-top:4px">11 ~ 100 次播放</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" :body-style="{ padding: '20px', textAlign: 'center' }">
                <div style="font-size:28px;font-weight:bold;color:#67C23A">{{ contentHealth.highPlays || 0 }}</div>
                <div style="font-size:13px;color:#909399;margin-top:8px">高播放视频</div>
                <div style="font-size:11px;color:#C0C4CC;margin-top:4px">100+ 次播放</div>
              </el-card>
            </el-col>
          </el-row>
          <div v-if="contentHealth.totalActive" style="color:#606266;font-size:13px;padding-top:8px">
            近 {{ healthDays }} 天内共有 <strong>{{ contentHealth.totalActive }}</strong> 个视频有播放记录
          </div>
        </el-card>
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getOverview, getTimeSeries, getRetentionData, getSearchKeywords, getCategoryRanking, getHourlyActivity, getNoResultKeywords, getContentHealth } from '@/api/chigua/stats'
import MultiLineChart from './dashboard/MultiLineChart.vue'
import Top100Dashboard from './dashboard/Top100Dashboard.vue'

export default {
  name: 'Index',
  components: { MultiLineChart, Top100Dashboard },
  data() {
    const today = new Date().toISOString().split('T')[0]
    return {
      activeTab: 'overview',

      // 概览
      overview: {},
      series1: { labels: [], series: [] },
      tsMode1: 'day',
      newUserSeries: { labels: [], series: [] },
      newUserMode: 'day',

      // 用户留存
      retentionRange: [],
      retentionSeries: [],
      retentionSummary: {},
      retentionLoading: false,
      retentionPickerOptions: {
        disabledDate(time) { return time.getTime() > Date.now() }
      },

      // 24小时时段
      hourlyDate: today,
      hourlyData: [],
      hourlyLoading: false,
      hourlyChartInstance: null,

      // 搜索关键词
      searchKeywords: [],
      kwLoading: false,
      kwDateRange: null,
      kwPickerOptions: {
        disabledDate(time) { return time.getTime() > Date.now() }
      },

      // 零结果关键词
      noResultKeywords: [],
      nrLoading: false,
      nrDate: today,

      // 内容健康度
      contentHealth: {},
      healthLoading: false,
      healthDays: 30,

      // 分类点击排行
      categoryRanking: [],
      catPeriod: 'day',
      catLoading: false,

      // 通用：只能选过去
      pastOnlyOptions: {
        disabledDate(time) { return time.getTime() > Date.now() }
      },

      // 指标卡配置
      cards: [
        { key: 'dau',        title: '日活用户（DAU）', color: '#E8F4FD', icon: 'el-icon-user-solid' },
        { key: 'uv',         title: '独立访客（UV）',  color: '#FDF6EC', icon: 'el-icon-view' },
        { key: 'view_count', title: '页面浏览量',      color: '#FEF0F0', icon: 'el-icon-document' },
        { key: 'searches',   title: '搜索次数',        color: '#F0F9FF', icon: 'el-icon-search' },
        { key: 'play_starts',title: '播放次数',        color: '#FFF7E6', icon: 'el-icon-video-play' },
        { key: 'shares',     title: '分享次数',        color: '#FDF6EC', icon: 'el-icon-share' }
      ]
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
    this.refreshSeries1()
    this.refreshNewUserSeries()
    this.initRetentionRange()
    this.loadRetention()
    this.loadCategoryRanking()
    this.loadSearchKeywords()
    this.loadNoResultKeywords()
    this.loadContentHealth()
  },

  mounted() {
    this.loadHourlyActivity()
  },

  watch: {
    activeTab(tab) {
      if (tab === 'user') {
        this.$nextTick(() => { this.renderHourlyChart() })
      }
    }
  },

  methods: {
    formatDate(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },

    // ── 概览 ──
    async loadOverview() {
      try {
        const { data } = await getOverview('today')
        this.overview = data || {}
      } catch (e) {
        this.$message.error('概览数据加载失败')
      }
    },

    async refreshSeries1() {
      try {
        const period = this.tsMode1
        const [dau, uv, pv, plays] = await Promise.all([
          getTimeSeries('dau', period),
          getTimeSeries('uv', period),
          getTimeSeries('pv', period),
          getTimeSeries('play_starts', period)
        ])
        const labels = dau.data?.labels || []
        this.series1 = {
          labels,
          series: [
            { name: '日活用户(DAU)', data: dau.data?.values || [] },
            { name: '独立访客(UV)',  data: uv.data?.values  || [] },
            { name: '页面浏览量(PV)', data: pv.data?.values || [] },
            { name: '播放量', data: plays.data?.values || [] }
          ]
        }
      } catch (e) {
        console.error('加载DAU/UV/PV趋势失败:', e)
      }
    },

    async refreshNewUserSeries() {
      try {
        const { data } = await getTimeSeries('new_users', this.newUserMode)
        this.newUserSeries = {
          labels: data?.labels || [],
          series: [{ name: '新增用户', data: data?.values || [] }]
        }
      } catch (e) {
        console.error('加载新用户趋势失败:', e)
      }
    },

    // ── 用户留存 ──
    initRetentionRange() {
      const end = new Date()
      const start = new Date()
      start.setDate(end.getDate() - 29)
      this.retentionRange = [this.formatDate(start), this.formatDate(end)]
    },

    async loadRetention() {
      if (!this.retentionRange || this.retentionRange.length !== 2) this.initRetentionRange()
      const [start, end] = this.retentionRange || []
      if (!start || !end) return
      this.retentionLoading = true
      try {
        const { data } = await getRetentionData({ startDate: start, endDate: end })
        this.retentionSeries = data?.series || []
        this.retentionSummary = data?.summary || {}
      } catch (e) {
        this.$message.error('留存数据加载失败')
      } finally {
        this.retentionLoading = false
      }
    },

    formatPercent(val) {
      if (val === null || val === undefined) return '--'
      const num = Number(val)
      if (Number.isNaN(num)) return '--'
      return (num * 100).toFixed(1) + '%'
    },

    // ── 分类点击排行 ──
    async loadCategoryRanking() {
      this.catLoading = true
      try {
        const { data } = await getCategoryRanking({ period: this.catPeriod, limit: 20 })
        this.categoryRanking = data || []
      } catch (e) {
        console.error('加载分类点击排行失败:', e)
      } finally {
        this.catLoading = false
      }
    },

    // ── 24小时活跃时段 ──
    async loadHourlyActivity() {
      this.hourlyLoading = true
      try {
        const { data } = await getHourlyActivity(this.hourlyDate)
        this.hourlyData = data || []
        this.$nextTick(() => { this.renderHourlyChart() })
      } catch (e) {
        console.error('加载时段数据失败:', e)
      } finally {
        this.hourlyLoading = false
      }
    },

    renderHourlyChart() {
      const el = this.$refs.hourlyChart
      if (!el || !this.hourlyData.length) return
      if (this.hourlyChartInstance) {
        this.hourlyChartInstance.dispose()
      }
      const chart = echarts.init(el)
      this.hourlyChartInstance = chart
      const hours = this.hourlyData.map(d => `${d.hour}时`)
      const counts = this.hourlyData.map(d => d.count)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: hours },
        yAxis: { type: 'value', name: '事件量' },
        series: [{ type: 'bar', data: counts, itemStyle: { color: '#409EFF' }, name: '事件量' }],
        grid: { left: '3%', right: '3%', bottom: '3%', top: '8%', containLabel: true }
      })
    },

    // ── 搜索关键词 ──
    async loadSearchKeywords() {
      this.kwLoading = true
      try {
        let params = { limit: 50 }
        if (this.kwDateRange && this.kwDateRange.length === 2 && this.kwDateRange[0]) {
          const [start, end] = this.kwDateRange
          if (start === end) { params.date = start } else { params.startDate = start; params.endDate = end }
        } else {
          params.date = new Date().toISOString().split('T')[0]
        }
        const { data } = await getSearchKeywords(params)
        this.searchKeywords = data || []
      } catch (e) {
        this.$message.error('搜索关键词加载失败')
      } finally {
        this.kwLoading = false
      }
    },

    // ── 零结果关键词 ──
    async loadNoResultKeywords() {
      this.nrLoading = true
      try {
        const { data } = await getNoResultKeywords({ date: this.nrDate, limit: 20 })
        this.noResultKeywords = data || []
      } catch (e) {
        console.error('加载零结果关键词失败:', e)
      } finally {
        this.nrLoading = false
      }
    },

    // ── 内容健康度 ──
    async loadContentHealth() {
      this.healthLoading = true
      try {
        const { data } = await getContentHealth(this.healthDays)
        this.contentHealth = data || {}
      } catch (e) {
        console.error('加载内容健康度失败:', e)
      } finally {
        this.healthLoading = false
      }
    }
  },

  beforeDestroy() {
    if (this.hourlyChartInstance) {
      this.hourlyChartInstance.dispose()
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
}

.stats-tabs {
  border-radius: 4px;
}

.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }

.retention-picker { width: 240px; }

.metric-card {
  height: 120px;

  .metric-header {
    display: flex;
    align-items: center;
    padding: 8px 12px;
    border-radius: 4px;
    margin-bottom: 8px;

    .metric-icon {
      font-size: 16px;
      margin-right: 8px;
      color: #666;
    }

    .metric-title {
      font-size: 12px;
      color: #666;
      font-weight: 500;
    }
  }

  .metric-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
    text-align: center;
    margin-bottom: 4px;
  }
}

.header-controls {
  float: right;
}

.clearfix::after {
  content: "";
  display: table;
  clear: both;
}

.fr { float: right; }
</style>
