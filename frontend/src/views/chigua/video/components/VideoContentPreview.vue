<template>
  <div class="video-content-preview">
    <!-- 视频信息头部 -->
    <div class="content-header">
      <div class="video-info">
        <h2 class="video-title">{{ videoData.title }}</h2>
        <div class="video-meta">
          <span class="author">作者：{{ videoData.author }}</span>
          <span class="duration" v-if="videoData.duration">时长：{{ formatDuration(videoData.duration) }}</span>
          <span class="created-time">创建时间：{{ formatTime(videoData.createdAt) }}</span>
        </div>
      </div>
      <div class="action-buttons">
        <el-button type="primary" size="small" @click="handleEdit" v-if="showEditButton">
          <i class="el-icon-edit"></i> 编辑内容
        </el-button>
        <el-button type="default" size="small" @click="handleClose" v-if="showCloseButton">
          <i class="el-icon-close"></i> 关闭
        </el-button>
      </div>
    </div>

    <!-- 副文本内容显示区域 -->
    <div class="content-body">
      <div class="content-wrapper">
        <div 
          class="rich-content" 
          v-html="videoData.videoContent"
          v-if="videoData.videoContent"
        ></div>
        <div class="empty-content" v-else>
          <i class="el-icon-document"></i>
          <p>暂无副文本内容</p>
        </div>
      </div>
    </div>

    <!-- 内容统计信息 -->
    <div class="content-footer" v-if="videoData.videoContent">
      <div class="content-stats">
        <span>字符数：{{ getContentLength() }}</span>
        <span>图片数：{{ getImageCount() }}</span>
        <span>视频数：{{ getVideoCount() }}</span>
      </div>
      <div class="last-edited" v-if="videoData.lastEditedAt">
        最后编辑：{{ formatTime(videoData.lastEditedAt) }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VideoContentPreview',
  props: {
    videoData: {
      type: Object,
      default: () => ({})
    },
    showEditButton: {
      type: Boolean,
      default: true
    },
    showCloseButton: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    /** 格式化时长 */
    formatDuration(seconds) {
      if (!seconds) return '-';
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;
      
      if (hours > 0) {
        return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
      } else {
        return `${minutes}:${secs.toString().padStart(2, '0')}`;
      }
    },
    
    /** 格式化时间 */
    formatTime(dateTime) {
      if (!dateTime) return '-';
      const date = new Date(dateTime);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    },
    
    /** 获取内容长度 */
    getContentLength() {
      if (!this.videoData.videoContent) return 0;
      // 去除HTML标签，计算纯文本长度
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = this.videoData.videoContent;
      const textContent = tempDiv.textContent || tempDiv.innerText || '';
      return textContent.length;
    },
    
    /** 获取图片数量 */
    getImageCount() {
      if (!this.videoData.videoContent) return 0;
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = this.videoData.videoContent;
      const images = tempDiv.querySelectorAll('img');
      return images.length;
    },
    
    /** 获取视频数量 */
    getVideoCount() {
      if (!this.videoData.videoContent) return 0;
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = this.videoData.videoContent;
      const videos = tempDiv.querySelectorAll('iframe, video');
      return videos.length;
    },
    
    /** 编辑按钮点击 */
    handleEdit() {
      this.$emit('edit', this.videoData);
    },
    
    /** 关闭按钮点击 */
    handleClose() {
      this.$emit('close');
    }
  }
}
</script>

<style scoped>
.video-content-preview {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.content-header {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #f8f9fa;
}

.video-info {
  flex: 1;
}

.video-title {
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1.4;
}

.video-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 14px;
  color: #606266;
}

.video-meta span {
  display: flex;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.content-body {
  padding: 30px;
  min-height: 400px;
}

.content-wrapper {
  max-width: 100%;
}

.rich-content {
  line-height: 1.8;
  font-size: 16px;
  color: #303133;
  word-wrap: break-word;
}

/* 富文本内容样式 */
.rich-content >>> h1,
.rich-content >>> h2,
.rich-content >>> h3,
.rich-content >>> h4,
.rich-content >>> h5,
.rich-content >>> h6 {
  margin: 20px 0 15px 0;
  font-weight: bold;
  line-height: 1.4;
}

.rich-content >>> h1 { font-size: 28px; }
.rich-content >>> h2 { font-size: 24px; }
.rich-content >>> h3 { font-size: 20px; }
.rich-content >>> h4 { font-size: 18px; }
.rich-content >>> h5 { font-size: 16px; }
.rich-content >>> h6 { font-size: 14px; }

.rich-content >>> p {
  margin: 15px 0;
  line-height: 1.8;
}

.rich-content >>> img {
  max-width: 100%;
  width: 100%;
  height: auto;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin: 15px 0;
  display: block;
  background: #f5f5f5;
  object-fit: cover;
}

.rich-content >>> video,
.rich-content >>> iframe {
  max-width: 100%;
  width: 100%;
  height: auto;
  min-height: 300px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin: 15px 0;
  display: block;
  background: #f5f5f5;
}

/* 为iframe设置16:9的宽高比 */
.rich-content >>> iframe {
  aspect-ratio: 16/9;
}

/* 为video标签设置16:9的宽高比 */
.rich-content >>> video {
  aspect-ratio: 16/9;
}

/* 兼容不支持aspect-ratio的浏览器 */
@supports not (aspect-ratio: 16/9) {
  .rich-content >>> iframe,
  .rich-content >>> video {
    height: 56.25vw; /* 16:9 比例 */
    max-height: 400px;
  }
}

/* 视频容器样式优化 */
.rich-content >>> iframe[src*="youtube"],
.rich-content >>> iframe[src*="youtu.be"],
.rich-content >>> iframe[src*="vimeo"],
.rich-content >>> iframe[src*="bilibili"] {
  border: none;
  outline: none;
}

/* 确保视频播放器在移动端的显示效果 */
@media (max-width: 768px) {
  .rich-content >>> iframe,
  .rich-content >>> video {
    min-height: 200px;
  }
  
  @supports not (aspect-ratio: 16/9) {
    .rich-content >>> iframe,
    .rich-content >>> video {
      height: 56.25vw;
      max-height: 300px;
    }
  }
}

.rich-content >>> blockquote {
  margin: 20px 0;
  padding: 15px 20px;
  background: #f8f9fa;
  border-left: 4px solid #409eff;
  border-radius: 0 4px 4px 0;
  font-style: italic;
  color: #606266;
}

.rich-content >>> pre {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 15px 0;
  font-family: 'Courier New', monospace;
}

.rich-content >>> code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.rich-content >>> ul,
.rich-content >>> ol {
  margin: 15px 0;
  padding-left: 30px;
}

.rich-content >>> li {
  margin: 8px 0;
  line-height: 1.6;
}

.rich-content >>> a {
  color: #409eff;
  text-decoration: none;
}

.rich-content >>> a:hover {
  text-decoration: underline;
}

.rich-content >>> strong {
  font-weight: bold;
}

.rich-content >>> em {
  font-style: italic;
}

.rich-content >>> u {
  text-decoration: underline;
}

.rich-content >>> s {
  text-decoration: line-through;
}

.empty-content {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-content i {
  font-size: 48px;
  margin-bottom: 16px;
  display: block;
  color: #c0c4cc;
}

.empty-content p {
  font-size: 16px;
  margin: 0;
}

.content-footer {
  padding: 15px 30px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.content-stats {
  display: flex;
  gap: 20px;
}

.last-edited {
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .video-meta {
    flex-direction: column;
    gap: 8px;
  }
  
  .content-body {
    padding: 20px;
  }
  
  .content-footer {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .content-stats {
    flex-direction: column;
    gap: 8px;
  }
}
</style> 