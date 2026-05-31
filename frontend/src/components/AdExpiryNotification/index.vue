<template>
  <transition name="notification-fade">
    <div v-if="showNotification" class="ad-expiry-notification">
      <div class="notification-header">
        <i class="el-icon-warning-outline"></i>
        <span class="notification-title">广告到期提醒</span>
        <i class="el-icon-close" @click="closeNotification"></i>
      </div>
      
      <div class="notification-content">
        <div class="notification-message">
          您有 <span class="highlight">{{ expiringAds.length }}</span> 个广告即将到期，请及时处理：
        </div>
        
        <div class="ad-list">
          <div 
            v-for="ad in expiringAds" 
            :key="ad.id" 
            class="ad-item"
            @click="viewAdDetail(ad)"
          >
            <div class="ad-info">
              <div class="ad-title">{{ ad.title }}</div>
              <div class="ad-meta">
                <span class="ad-position">{{ formatPosition(ad.position) }}</span>
                <span class="ad-expiry">
                  {{ ad.daysLeft }}天后到期 ({{ formatDate(ad.endDate) }})
                </span>
              </div>
            </div>
            <div class="ad-status" :class="getStatusClass(ad.daysLeft)">
              {{ getStatusText(ad.daysLeft) }}
            </div>
          </div>
        </div>
        
        <div class="notification-actions">
          <el-button size="small" @click="viewAllAds">查看所有广告</el-button>
          <el-button type="primary" size="small" @click="closeNotification">知道了</el-button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: 'AdExpiryNotification',
  props: {
    expiringAds: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      showNotification: false
    }
  },
  mounted() {
    console.log('AdExpiryNotification mounted, expiringAds:', this.expiringAds);
    if (this.expiringAds && this.expiringAds.length > 0) {
      console.log('有广告数据，延迟1秒显示通知面板');
      // 延迟显示，确保页面加载完成
      setTimeout(() => {
        console.log('开始显示广告通知面板');
        this.showNotification = true
      }, 1000)
    } else {
      console.log('没有广告数据，不显示通知面板');
    }
  },
  watch: {
    expiringAds: {
      handler(newAds) {
        console.log('AdExpiryNotification watch triggered, newAds:', newAds);
        if (newAds && newAds.length > 0) {
          console.log('Watch检测到广告数据，显示通知面板');
          this.showNotification = true
        }
      },
      immediate: true
    }
  },
  methods: {
    closeNotification() {
      this.showNotification = false
      this.$emit('close')
    },
    
    formatDate(dateStr) {
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN')
    },
    
    formatPosition(position) {
      const positionMap = {
        'home_banner': '首页横幅',
        'home_sidebar': '首页侧边栏',
        'video_detail': '视频详情页',
        'category_top': '分类页顶部',
        'search_result': '搜索结果页',
        'footer': '页脚广告'
      }
      return positionMap[position] || position
    },
    
    getStatusClass(daysLeft) {
      if (daysLeft <= 1) return 'urgent'
      if (daysLeft <= 2) return 'warning'
      return 'notice'
    },
    
    getStatusText(daysLeft) {
      if (daysLeft <= 0) return '已过期'
      if (daysLeft <= 1) return '紧急'
      if (daysLeft <= 2) return '警告'
      return '提醒'
    },
    
    viewAdDetail(ad) {
      // 跳转到广告详情页 - 编辑指定广告
      this.$router.push({
        path: '/chigua/advertisement',
        query: { id: ad.id }
      })
      this.closeNotification()
    },
    
    viewAllAds() {
      // 跳转到广告管理页
      this.$router.push('/chigua/advertisement')
      this.closeNotification()
    }
  }
}
</script>

<style lang="scss" scoped>
.ad-expiry-notification {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 380px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border-left: 4px solid #e6a23c;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.notification-header {
  display: flex;
  align-items: center;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f0f0f0;
  background: #fdf6ec;
  border-radius: 8px 8px 0 0;
  
  .el-icon-warning-outline {
    color: #e6a23c;
    font-size: 18px;
    margin-right: 8px;
  }
  
  .notification-title {
    flex: 1;
    font-weight: 600;
    font-size: 14px;
    color: #303133;
  }
  
  .el-icon-close {
    cursor: pointer;
    color: #909399;
    font-size: 16px;
    padding: 4px;
    
    &:hover {
      color: #606266;
    }
  }
}

.notification-content {
  padding: 16px 20px 20px;
}

.notification-message {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.5;
  
  .highlight {
    color: #e6a23c;
    font-weight: 600;
  }
}

.ad-list {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.ad-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #f8f9fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background: #e8f4f8;
    transform: translateX(2px);
  }
  
  &:last-child {
    margin-bottom: 0;
  }
}

.ad-info {
  flex: 1;
  min-width: 0;
}

.ad-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ad-meta {
  font-size: 11px;
  color: #909399;
  
  .ad-position {
    margin-right: 8px;
    padding: 1px 6px;
    background: #e1f3f8;
    color: #409eff;
    border-radius: 3px;
  }
}

.ad-status {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  
  &.urgent {
    background: #fef0f0;
    color: #f56c6c;
    border: 1px solid #fbc4c4;
  }
  
  &.warning {
    background: #fdf6ec;
    color: #e6a23c;
    border: 1px solid #f5dab1;
  }
  
  &.notice {
    background: #f0f9ff;
    color: #409eff;
    border: 1px solid #b3d8ff;
  }
}

.notification-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

// 动画效果
.notification-fade-enter-active,
.notification-fade-leave-active {
  transition: all 0.3s ease;
}

.notification-fade-enter {
  transform: translateX(100%);
  opacity: 0;
}

.notification-fade-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

// 滚动条样式
.ad-list::-webkit-scrollbar {
  width: 4px;
}

.ad-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.ad-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.ad-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>