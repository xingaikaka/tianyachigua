<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons')
import resize from './mixins/resize'

export default {
  name: 'MultiLineChart',
  mixins: [resize],
  props: {
    className: { type: String, default: 'chart' },
    width: { type: String, default: '100%' },
    height: { type: String, default: '360px' },
    chartType: { // 'line' | 'bar'
      type: String,
      default: 'line'
    },
    chartData: { // { labels: string[], series: [{ name, data:number[] }] }
      type: Object,
      required: true
    }
  },
  data() {
    return { chart: null }
  },
  watch: {
    chartData: {
      deep: true,
      handler(val) { this.setOptions(val) }
    }
  },
  mounted() {
    this.$nextTick(() => { this.initChart() })
  },
  beforeDestroy() {
    if (this.chart) { this.chart.dispose(); this.chart = null }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')
      this.setOptions(this.chartData)
    },
    setOptions({ labels = [], series = [] } = {}) {
      if (!this.chart) return
      const isBar = this.chartType === 'bar'
      this.chart.setOption({
        xAxis: {
          type: 'category',
          data: labels,
          boundaryGap: isBar,
          axisTick: { show: false },
          axisLabel: { hideOverlap: true, margin: 12 }
        },
        grid: { left: 30, right: 30, bottom: 35, top: 30, containLabel: true },
        tooltip: { trigger: 'axis', axisPointer: { type: isBar ? 'shadow' : 'cross' }, padding: [5, 10] },
        yAxis: { type: 'value', minInterval: 1, axisTick: { show: false } },
        legend: { data: series.map(s => s.name) },
        series: series.map(s => ({
          name: s.name,
          type: isBar ? 'bar' : 'line',
          smooth: !isBar,
          showSymbol: !isBar && false,
          barMaxWidth: isBar ? 18 : undefined,
          barGap: isBar ? '20%' : undefined,
          data: s.data || [],
          animationDuration: 1200,
          animationEasing: 'cubicInOut'
        }))
      })
    }
  }
}
</script>


