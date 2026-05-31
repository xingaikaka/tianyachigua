<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card" class="ad-stats-tabs">

      <!-- ===== Tab 1: 数据报表 ===== -->
      <el-tab-pane label="数据报表" name="table">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="queryParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="yyyy-MM-dd"
              value-format="yyyy-MM-dd"
              @change="handleDateChange">
            </el-date-picker>
          </el-form-item>

          <el-form-item label="按广告筛选">
            <el-select v-model="queryParams.adTitle" placeholder="全部广告" clearable style="width:180px" @change="handleAdNameChange">
              <el-option label="全部广告" value="" />
              <el-option
                v-for="name in adNameList"
                :key="name"
                :label="name"
                :value="name" />
            </el-select>
          </el-form-item>

          <el-form-item label="广告类型">
            <el-select v-model="queryParams.adType" placeholder="请选择广告类型" clearable>
              <el-option
                v-for="dict in dict.type.ad_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="广告位置">
            <el-select v-model="queryParams.position" placeholder="请选择广告位置" clearable>
              <el-option
                v-for="dict in dict.type.ad_position"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
              v-hasPermi="['chigua:advertisement:statistics:export']">导出</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="statisticsList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="广告ID" align="center" prop="id" width="80" />
          <el-table-column label="广告标题" align="center" prop="title" :show-overflow-tooltip="true" />
          <el-table-column label="广告类型" align="center" prop="adType">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.ad_type" :value="scope.row.adType"/>
            </template>
          </el-table-column>
          <el-table-column label="应用类型" align="center" prop="appType">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.app_type" :value="scope.row.appType" v-if="scope.row.appType"/>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="广告位置" align="center" prop="position">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.ad_position" :value="scope.row.position" v-if="scope.row.position"/>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="投放范围" align="center" prop="isGlobal">
            <template slot-scope="scope">
              <el-tag :type="scope.row.isGlobal === 1 ? 'success' : 'info'">
                {{ scope.row.isGlobal === 1 ? '全站' : '分类' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="点击量" align="center" prop="clickCount" sortable width="100">
            <template slot-scope="scope">
              <span style="color:#409EFF;font-weight:bold;font-size:16px">{{ scope.row.clickCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template slot-scope="scope">
              <el-switch v-model="scope.row.status" :active-value="1" :inactive-value="0" disabled />
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="80">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)"
                v-hasPermi="['chigua:advertisement:statistics:detail']">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <!-- ===== Tab 2: 图表分析 ===== -->
      <el-tab-pane label="图表分析" name="chart">
        <!-- 图表时间范围选择器 -->
        <div style="margin-bottom:16px;display:flex;align-items:center;gap:12px">
          <span style="font-size:13px;color:#606266">时间范围：</span>
          <el-date-picker
            v-model="chartDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            size="small"
            style="width:260px"
            @change="loadAllCharts" />
          <el-button type="primary" size="small" icon="el-icon-refresh" @click="loadAllCharts">刷新</el-button>
        </div>

        <el-row :gutter="16">
          <!-- 图表1：每日点击/曝光趋势 -->
          <el-col :span="24" style="margin-bottom:16px">
            <el-card shadow="never">
              <div slot="header" class="clearfix">
                <span>每日点击 / 曝光趋势</span>
                <span style="font-size:11px;color:#909399;margin-left:8px">按日期汇总所有广告</span>
              </div>
              <div ref="trendChart" style="height:300px" v-loading="chartLoading.trend"></div>
            </el-card>
          </el-col>

          <!-- 图表2：广告类型分布 + 图表3：广告位置分布 -->
          <el-col :span="12" style="margin-bottom:16px">
            <el-card shadow="never">
              <div slot="header" class="clearfix">
                <span>广告类型点击分布</span>
                <span style="font-size:11px;color:#909399;margin-left:8px">环形图</span>
              </div>
              <div ref="typeChart" style="height:280px" v-loading="chartLoading.type"></div>
            </el-card>
          </el-col>
          <el-col :span="12" style="margin-bottom:16px">
            <el-card shadow="never">
              <div slot="header" class="clearfix">
                <span>广告位置点击分布</span>
                <span style="font-size:11px;color:#909399;margin-left:8px">按位置汇总</span>
              </div>
              <div ref="positionChart" style="height:280px" v-loading="chartLoading.position"></div>
            </el-card>
          </el-col>

          <!-- 图表4：Top10广告点击排行 -->
          <el-col :span="24">
            <el-card shadow="never">
              <div slot="header" class="clearfix">
                <span>Top 10 广告点击排行</span>
                <span style="font-size:11px;color:#909399;margin-left:8px">按点击量降序</span>
              </div>
              <div ref="topAdsChart" style="height:320px" v-loading="chartLoading.topAds"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

    </el-tabs>

    <!-- 点击详情对话框 -->
    <el-dialog :title="detailTitle" :visible.sync="detailOpen" width="500px" append-to-body>
      <el-table :data="detailList" v-loading="detailLoading" size="small">
        <el-table-column label="统计日期" align="center" prop="statDate" width="130">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.statDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当日点击次数" align="center" prop="clickCount">
          <template slot-scope="scope">
            <el-tag type="primary">{{ scope.row.clickCount }} 次</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listAdStatistics, getAdNames, getClickDetails, getChartTrend, getChartByType, getChartByPosition, getChartTopAds } from '@/api/chigua/adStatistics'

export default {
  name: 'AdStatistics',
  dicts: ['ad_type', 'app_type', 'ad_position'],
  data() {
    const today = this.formatDate(new Date())
    const weekAgo = this.formatDate(new Date(Date.now() - 7 * 86400000))
    return {
      activeTab: 'table',
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      statisticsList: [],
      adNameList: [],
      detailOpen: false,
      detailTitle: '',
      detailList: [],
      detailLoading: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        dateRange: [weekAgo, today],
        startDate: weekAgo,
        endDate: today,
        adTitle: null,
        adType: null,
        position: null
      },
      // 图表
      chartDateRange: [weekAgo, today],
      chartInstances: {},
      chartLoading: { trend: false, type: false, position: false, topAds: false }
    }
  },
  created() {
    this.getAdNameList()
    this.getList()
  },
  watch: {
    activeTab(tab) {
      if (tab === 'chart') {
        this.$nextTick(() => { this.loadAllCharts() })
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

    // ── 数据报表 Tab ──

    getList() {
      if (!this.queryParams.startDate || !this.queryParams.endDate) {
        this.$modal.msgWarning('请先选择时间范围')
        this.statisticsList = []
        this.total = 0
        return
      }
      this.loading = true
      listAdStatistics(this.queryParams).then(response => {
        this.statisticsList = response.rows || []
        this.total = response.total || 0
        this.loading = false
      }).catch(() => {
        this.statisticsList = []
        this.total = 0
        this.loading = false
        this.$modal.msgError('查询失败，请稍后重试')
      })
    },

    getAdNameList() {
      if (this.queryParams.startDate && this.queryParams.endDate) {
        getAdNames(this.queryParams.startDate, this.queryParams.endDate).then(res => {
          this.adNameList = res.data || []
        })
      }
    },

    handleDateChange(value) {
      if (value && value.length === 2) {
        this.queryParams.startDate = value[0]
        this.queryParams.endDate = value[1]
        this.getAdNameList()
        this.queryParams.pageNum = 1
        this.getList()
      } else {
        this.queryParams.startDate = null
        this.queryParams.endDate = null
        this.adNameList = []
        this.queryParams.pageNum = 1
        this.statisticsList = []
        this.total = 0
      }
    },

    handleAdNameChange() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    resetQuery() {
      const today = this.formatDate(new Date())
      const weekAgo = this.formatDate(new Date(Date.now() - 7 * 86400000))
      this.queryParams = {
        ...this.queryParams,
        pageNum: 1,
        dateRange: [weekAgo, today],
        startDate: weekAgo,
        endDate: today,
        adTitle: null,
        adType: null,
        position: null
      }
      this.getAdNameList()
      this.getList()
    },

    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },

    handleDetail(row) {
      if (!this.queryParams.startDate || !this.queryParams.endDate) {
        this.$modal.msgError('请先选择时间范围')
        return
      }
      this.detailLoading = true
      this.detailTitle = `每日点击趋势 — ${row.title}`
      this.detailOpen = true
      getClickDetails(row.id, this.queryParams.startDate, this.queryParams.endDate).then(res => {
        this.detailList = res.data || []
        this.detailLoading = false
      }).catch(() => { this.detailLoading = false })
    },

    handleExport() {
      this.download('chigua/advertisement/statistics/export', { ...this.queryParams }, `广告统计_${Date.now()}.xlsx`)
    },

    // ── 图表分析 Tab ──

    loadAllCharts() {
      const [startDate, endDate] = this.chartDateRange || []
      if (!startDate || !endDate) return
      this.loadTrendChart(startDate, endDate)
      this.loadTypeChart(startDate, endDate)
      this.loadPositionChart(startDate, endDate)
      this.loadTopAdsChart(startDate, endDate)
    },

    disposeChart(key) {
      if (this.chartInstances[key]) {
        this.chartInstances[key].dispose()
        this.chartInstances[key] = null
      }
    },

    initChart(refKey) {
      this.disposeChart(refKey)
      const el = this.$refs[refKey]
      if (!el) return null
      const chart = echarts.init(el)
      this.chartInstances[refKey] = chart
      return chart
    },

    async loadTrendChart(startDate, endDate) {
      this.chartLoading.trend = true
      try {
        const { data } = await getChartTrend(startDate, endDate)
        const list = data || []
        const chart = this.initChart('trendChart')
        if (!chart) return
        chart.setOption({
          tooltip: { trigger: 'axis' },
          legend: { data: ['点击量', '曝光量'] },
          xAxis: { type: 'category', data: list.map(d => d.statDate), axisLabel: { rotate: 30 } },
          yAxis: { type: 'value', name: '次数' },
          series: [
            { name: '点击量', type: 'line', smooth: true, data: list.map(d => d.clickCount || 0), itemStyle: { color: '#409EFF' } },
            { name: '曝光量', type: 'line', smooth: true, data: list.map(d => d.impressionCount || 0), itemStyle: { color: '#67C23A' } }
          ],
          grid: { left: '3%', right: '4%', bottom: '10%', top: '12%', containLabel: true }
        })
      } catch (e) { console.error('趋势图加载失败', e) }
      finally { this.chartLoading.trend = false }
    },

    async loadTypeChart(startDate, endDate) {
      this.chartLoading.type = true
      try {
        const { data } = await getChartByType(startDate, endDate)
        const list = data || []
        const typeLabels = {
          '1': '横幅广告', '2': 'Logo广告', '3': '文字链接',
          '4': '弹窗广告', '5': '短视频广告', '6': '分页广告'
        }
        const chart = this.initChart('typeChart')
        if (!chart) return
        chart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
          legend: { orient: 'vertical', right: 10, top: 'center' },
          series: [{
            name: '广告类型',
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['40%', '50%'],
            label: { show: false },
            data: list.map(d => ({ name: typeLabels[d.adType] || d.adType || '未知', value: d.clickCount || 0 }))
          }]
        })
      } catch (e) { console.error('类型图加载失败', e) }
      finally { this.chartLoading.type = false }
    },

    async loadPositionChart(startDate, endDate) {
      this.chartLoading.position = true
      try {
        const { data } = await getChartByPosition(startDate, endDate)
        const list = data || []
        const chart = this.initChart('positionChart')
        if (!chart) return
        const positionDict = (this.dict && this.dict.type && this.dict.type.ad_position) || []
        const posLabel = (val) => {
          const found = positionDict.find(d => String(d.value) === String(val))
          return found ? found.label : (val || '未知')
        }
        chart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          xAxis: { type: 'value', name: '点击量' },
          yAxis: { type: 'category', data: list.map(d => posLabel(d.position)), inverse: true },
          series: [{
            type: 'bar',
            data: list.map(d => d.clickCount || 0),
            itemStyle: { color: '#E6A23C' },
            label: { show: true, position: 'right' }
          }],
          grid: { left: '3%', right: '10%', bottom: '3%', top: '3%', containLabel: true }
        })
      } catch (e) { console.error('位置图加载失败', e) }
      finally { this.chartLoading.position = false }
    },

    async loadTopAdsChart(startDate, endDate) {
      this.chartLoading.topAds = true
      try {
        const { data } = await getChartTopAds(startDate, endDate, 10)
        const list = (data || []).reverse()
        const chart = this.initChart('topAdsChart')
        if (!chart) return
        chart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          xAxis: { type: 'value', name: '点击量' },
          yAxis: {
            type: 'category',
            data: list.map(d => d.adTitle || `广告${d.adId}`),
            axisLabel: { width: 150, overflow: 'truncate' }
          },
          series: [{
            type: 'bar',
            data: list.map(d => d.clickCount || 0),
            itemStyle: { color: '#F56C6C' },
            label: { show: true, position: 'right', formatter: '{c} 次' }
          }],
          grid: { left: '2%', right: '12%', bottom: '3%', top: '3%', containLabel: true }
        })
      } catch (e) { console.error('Top广告图加载失败', e) }
      finally { this.chartLoading.topAds = false }
    }
  },

  beforeDestroy() {
    Object.values(this.chartInstances).forEach(c => c && c.dispose())
  }
}
</script>

<style scoped>
.ad-stats-tabs {
  border: none;
}
</style>
