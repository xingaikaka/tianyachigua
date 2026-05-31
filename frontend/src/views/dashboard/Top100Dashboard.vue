<template>
  <div class="top100-dashboard">
    <!-- 页面标题 -->
    <div class="dashboard-header">
      <h2>📊 Top详细统计</h2>
      <div class="action-buttons">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="yyyy-MM-dd"
          value-format="yyyy-MM-dd"
          :picker-options="dateRangePickerOptions"
          @change="handleDateRangeChange"
        />
        <el-button @click="refreshData" :loading="loading" type="primary" icon="el-icon-refresh">
          刷新数据
        </el-button>
        <el-button @click="cleanupOldData" :loading="cleanupLoading" type="danger" icon="el-icon-delete">
          清理旧数据
        </el-button>
      </div>
    </div>

    <!-- 基础统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ basicStats.activeVideoCount || 0 }}</div>
            <div class="stat-label">活跃视频</div>
            <div class="stat-change" :class="getChangeClass(basicStats.activeVideoCountChange)">
              {{ formatChange(basicStats.activeVideoCountChange) }}
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ formatNumber(basicStats.totalPlays) }}</div>
            <div class="stat-label">总播放量</div>
            <div class="stat-change" :class="getChangeClass(basicStats.totalPlaysChange)">
              {{ formatChange(basicStats.totalPlaysChange) }}
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ basicStats.avgPlays || 0 }}</div>
            <div class="stat-label">平均播放量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-value">{{ categoryStats.length }}</div>
            <div class="stat-label">活跃分类数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 排行榜区域 -->
    <el-row :gutter="20" class="rankings-section">
      <!-- 今日Top10 -->
      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span>🔥 热门Top</span>
          </div>
          <div class="ranking-list">
            <div
              v-for="(video, index) in rankings.todayTop10"
              :key="video.videoId"
              class="ranking-item"
            >
              <div class="video-info">
                <div class="video-title" :title="video.title">{{ video.title }}</div>
              </div>
              <div class="play-count">{{ formatNumber(video.plays) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 快速上升Top10 -->
      <el-col :span="12">
        <el-card>
          <div slot="header">
            <span>🚀 上升Top</span>
            <span style="font-size:11px;color:#909399;margin-left:8px">按增长率排序</span>
          </div>
          <div class="ranking-list">
            <div
              v-for="(video, index) in rankings.risingTop10"
              :key="video.videoId"
              class="ranking-item"
            >
              <div class="video-info">
                <div class="video-title" :title="video.title">{{ video.title }}</div>
              </div>
              <div class="play-count">
                <span>{{ formatNumber(video.plays) }}</span>
                <span v-if="video.growthCount != null" style="color:#67c23a;font-size:11px;margin-left:4px">
                  +{{ formatNumber(video.growthCount) }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>



  </div>
</template>

<script>
import { getTop100Dashboard, cleanupPlayRecords } from "@/api/chigua/stats";

export default {
  name: "Top100Dashboard",
  components: {},
  data() {
    return {
      loading: false,
      cleanupLoading: false,
      // 日期范围选择
      dateRange: [
        new Date().toISOString().split('T')[0], // 当日
        new Date().toISOString().split('T')[0]  // 当日
      ],
      
      // 日期范围选择器配置
      dateRangePickerOptions: {
        disabledDate(date) {
          // 只允许选择当日之前的7天（包括当日）
          const today = new Date();
          const sevenDaysAgo = new Date();
          sevenDaysAgo.setDate(today.getDate() - 7);
          return date < sevenDaysAgo || date > today;
        }
      },
      
      // 统计数据
      basicStats: {},
      rankings: {
        todayTop10: [], // 实际显示30条数据
        risingTop10: [] // 实际显示30条数据
      },
      categoryStats: [],
      trends: {
        details: []
      },
      
    };
  },
  mounted() {
    if (!this.dateRange || this.dateRange.length !== 2 || !this.dateRange[0] || !this.dateRange[1]) {
      const today = new Date().toISOString().split('T')[0];
      this.dateRange = [today, today];
    }
    console.log('组件挂载，初始日期范围:', this.dateRange);
    this.loadDashboardData();
  },
  methods: {
    async loadDashboardData() {
      if (!this.dateRange || this.dateRange.length !== 2 || !this.dateRange[0] || !this.dateRange[1]) {
        this.$message.error("请选择有效的日期范围");
        return;
      }
      
      this.loading = true;
      try {
        const [startDate, endDate] = this.dateRange;
        console.log('加载数据，日期范围:', startDate, '至', endDate);
        const response = await getTop100Dashboard(startDate, endDate);
        if (response.code === 200) {
          const data = response.data;
          this.basicStats = data.basicStats || {};
          this.rankings = data.rankings || {};
          this.categoryStats = data.categoryStats || [];
          this.trends = data.trends || { details: [] };
          
          // 调试信息：检查返回的数据
          console.log('API返回的完整数据:', data);
          console.log('rankings数据:', this.rankings);
          console.log('todayTop10数据条数:', this.rankings.todayTop10?.length);
          console.log('risingTop10数据条数:', this.rankings.risingTop10?.length);
        }
      } catch (error) {
        console.error("加载Top100面板数据失败:", error);
        this.$message.error("加载数据失败");
      } finally {
        this.loading = false;
      }
    },
    
    async cleanupOldData() {
      this.cleanupLoading = true;
      try {
        const response = await cleanupPlayRecords();
        if (response.code === 200) {
          this.$message.success(response.msg || "清理完成");
          // 清理后刷新数据
          this.loadDashboardData();
        }
      } catch (error) {
        console.error("清理旧数据失败:", error);
        this.$message.error("清理失败");
      } finally {
        this.cleanupLoading = false;
      }
    },
    
    handleDateRangeChange() {
      this.loadDashboardData();
    },
    
    refreshData() {
      this.loadDashboardData();
    },
    
    getDateRangeText() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        return "请选择日期范围";
      }
      return `${this.dateRange[0]} 至 ${this.dateRange[1]}`;
    },
    
    formatNumber(num) {
      if (!num) return "0";
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + "万";
      }
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + "k";
      }
      return num.toString();
    },
    
    formatChange(change) {
      if (!change) return "";
      const prefix = change > 0 ? "+" : "";
      return prefix + this.formatNumber(change);
    },
    
    getChangeClass(change) {
      if (change > 0) return "positive";
      if (change < 0) return "negative";
      return "";
    },
    

    
    getTotalPlays() {
      return this.trends.details.reduce((sum, item) => sum + (item.totalPlays || 0), 0);
    },
    
    getAvgDailyPlays() {
      const total = this.getTotalPlays();
      return this.trends.details.length > 0 ? Math.round(total / this.trends.details.length) : 0;
    },
    
    getMaxDailyPlays() {
      return Math.max(...this.trends.details.map(item => item.totalPlays || 0), 0);
    },
    
    getChartBarHeight(plays) {
      const max = this.getMaxDailyPlays();
      return max > 0 ? (plays / max) * 100 : 0;
    },
    

    

    
    getDateRangeText() {
      if (!this.selectedDate) return "";
      const endDate = new Date(this.selectedDate);
      const startDate = new Date(endDate);
      startDate.setDate(endDate.getDate() - 6);
      
      const formatDate = (date) => {
        return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
      };
      
      return `${formatDate(startDate)} - ${formatDate(endDate)}`;
    }
  }
};
</script>

<style scoped>
.top100-dashboard {
  padding: 20px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h2 {
  margin: 0;
  color: #303133;
}

.action-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-item {
  padding: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
}

.stat-change {
  font-size: 12px;
  font-weight: bold;
}

.stat-change.positive {
  color: #67C23A;
}

.stat-change.negative {
  color: #F56C6C;
}

.stat-desc {
  font-size: 12px;
  color: #909399;
}

.rankings-section,
.secondary-rankings,
.analysis-section {
  margin-bottom: 20px;
}

.ranking-list {
  max-height: 600px; /* 调整为适合30条数据的高度 */
  overflow-y: auto;
  padding-right: 5px;
}

/* 滚动条样式优化 */
.ranking-list::-webkit-scrollbar {
  width: 6px;
}

.ranking-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.ranking-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.ranking-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #EBEEF5;
}

.ranking-item:last-child {
  border-bottom: none;
}







.video-info {
  flex: 1;
  margin-right: 10px;
  min-width: 0;
}

.video-title {
  font-weight: bold;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: help;
  line-height: 1.4;
}



.play-count {
  font-weight: bold;
  color: #409EFF;
  min-width: 50px;
  text-align: left;
  flex-shrink: 0;
}



.header-desc {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}



.category-stats {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 10px;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #EBEEF5;
  gap: 15px;
}

.category-item:last-child {
  border-bottom: none;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.category-meta {
  font-size: 12px;
  color: #909399;
}

.category-stats-right {
  text-align: right;
  min-width: 170px;
  flex-shrink: 0;
  margin-right: 50px;
}

.total-plays {
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 5px;
}

.percentage {
  font-size: 12px;
  color: #E6A23C;
}

.trend-tabs {
  float: right;
}




</style>
