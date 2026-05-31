<template>
  <el-dialog
    title="视频预览"
    :visible.sync="visible"
    width="1000px"
    append-to-body
    @close="handleClose"
  >
    <div class="preview-container" v-if="currentVideo.id">
      <!-- 基本信息 -->
      <el-card class="info-card" shadow="never">
        <div slot="header" class="card-header">
          <span>基本信息</span>
        </div>
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="cover-container">
              <img :src="currentVideo.coverImage" :alt="currentVideo.title" v-if="currentVideo.coverImage" />
              <div class="no-cover" v-else>
                <i class="el-icon-picture-outline"></i>
                <p>暂无封面</p>
              </div>
            </div>
          </el-col>
          <el-col :span="16">
            <div class="info-content">
              <h2 class="video-title">{{ currentVideo.title }}</h2>
              <p class="video-subtitle" v-if="currentVideo.subtitle">{{ currentVideo.subtitle }}</p>
              
              <div class="info-row">
                <span class="label">作者：</span>
                <span class="value">{{ currentVideo.author }}</span>
              </div>
              
              <div class="info-row">
                <span class="label">时长：</span>
                <span class="value">{{ formatDuration(currentVideo.duration) }}</span>
              </div>
              
              <div class="info-row">
                <span class="label">状态：</span>
                <dict-tag :options="dict.type.video_status" :value="currentVideo.status"/>
              </div>
              
              <div class="info-row">
                <span class="label">标记：</span>
                <el-tag v-if="currentVideo.isRecommended == 1" type="success" size="small">推荐</el-tag>
                <el-tag v-if="currentVideo.isHot == 1" type="danger" size="small">热门</el-tag>
                <span v-if="currentVideo.isRecommended != 1 && currentVideo.isHot != 1">无</span>
              </div>
              
              <div class="info-row">
                <span class="label">排序：</span>
                <span class="value">{{ currentVideo.sortOrder }}</span>
              </div>
              
              <div class="info-row">
                <span class="label">创建时间：</span>
                <span class="value">{{ parseTime(currentVideo.createdAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
              </div>
              
              <div class="info-row">
                <span class="label">更新时间：</span>
                <span class="value">{{ parseTime(currentVideo.updatedAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
              </div>
            </div>
          </el-col>
        </el-row>
        
        <div class="description-section" v-if="currentVideo.description">
          <h4>视频描述</h4>
          <p class="description-text">{{ currentVideo.description }}</p>
        </div>
      </el-card>

      <!-- 统计数据 -->
      <el-card class="stats-card" shadow="never">
        <div slot="header" class="card-header">
          <span>统计数据</span>
        </div>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number">{{ currentVideo.viewCount || 0 }}</div>
              <div class="stat-label">观看次数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number">{{ currentVideo.likeCount || 0 }}</div>
              <div class="stat-label">点赞次数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number">{{ currentVideo.shareCount || 0 }}</div>
              <div class="stat-label">分享次数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-number">{{ currentVideo.commentCount || 0 }}</div>
              <div class="stat-label">评论次数</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 关联信息 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="relation-card" shadow="never">
            <div slot="header" class="card-header">
              <span>分类信息</span>
              <span class="count-badge">{{ categories.length }}</span>
            </div>
            <div class="relation-content">
              <el-tag
                v-for="category in categories"
                :key="category.id"
                type="primary"
                size="small"
                class="relation-tag"
              >
                {{ category.name }}
              </el-tag>
              <div v-if="categories.length === 0" class="empty-relation">
                <i class="el-icon-folder-opened"></i>
                <p>暂无分类</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="relation-card" shadow="never">
            <div slot="header" class="card-header">
              <span>标签信息</span>
              <span class="count-badge">{{ tags.length }}</span>
            </div>
            <div class="relation-content">
              <el-tag
                v-for="tag in tags"
                :key="tag.id"
                type="success"
                size="small"
                class="relation-tag"
              >
                {{ tag.name }}
              </el-tag>
              <div v-if="tags.length === 0" class="empty-relation">
                <i class="el-icon-price-tag"></i>
                <p>暂无标签</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图片和地址统计 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="media-card" shadow="never">
            <div slot="header" class="card-header">
              <span>图片信息</span>
              <span class="count-badge">{{ currentVideo.imageCount || 0 }}</span>
            </div>
            <div class="media-content">
              <div class="media-stat">
                <i class="el-icon-picture-outline"></i>
                <div class="media-info">
                  <div class="media-count">{{ currentVideo.imageCount || 0 }} 张图片</div>
                  <div class="media-desc">包含封面图和相关图片</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="media-card" shadow="never">
            <div slot="header" class="card-header">
              <span>地址信息</span>
              <span class="count-badge">{{ currentVideo.urlCount || 0 }}</span>
            </div>
            <div class="media-content">
              <div class="media-stat">
                <i class="el-icon-link"></i>
                <div class="media-info">
                  <div class="media-count">{{ currentVideo.urlCount || 0 }} 个地址</div>
                  <div class="media-desc">包含不同清晰度的播放地址</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleEdit" type="primary" icon="el-icon-edit">编辑视频</el-button>
      <el-button @click="handleImages" type="success" icon="el-icon-picture">管理图片</el-button>
      <el-button @click="handleUrls" type="warning" icon="el-icon-link">管理地址</el-button>
      <el-button @click="handleClose">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getVideoCategories, getVideoTags } from "@/api/chigua/video";

export default {
  name: "VideoPreviewDialog",
  dicts: ['video_status'],
  data() {
    return {
      visible: false,
      currentVideo: {},
      categories: [],
      tags: []
    };
  },
  methods: {
    /** 显示弹窗 */
    show(video) {
      this.currentVideo = video;
      this.visible = true;
      this.getRelationData();
    },
    
    /** 获取关联数据 */
    getRelationData() {
      // 获取分类信息
      getVideoCategories(this.currentVideo.id).then(response => {
        this.categories = response.data || [];
      }).catch(() => {
        this.categories = [];
      });
      
      // 获取标签信息
      getVideoTags(this.currentVideo.id).then(response => {
        this.tags = response.data || [];
      }).catch(() => {
        this.tags = [];
      });
    },
    
    /** 编辑视频 */
    handleEdit() {
      this.$emit('edit', this.currentVideo);
      this.handleClose();
    },
    
    /** 管理图片 */
    handleImages() {
      this.$emit('images', this.currentVideo);
      this.handleClose();
    },
    
    /** 管理地址 */
    handleUrls() {
      this.$emit('urls', this.currentVideo);
      this.handleClose();
    },
    
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
    
    /** 关闭弹窗 */
    handleClose() {
      this.visible = false;
      this.currentVideo = {};
      this.categories = [];
      this.tags = [];
    }
  }
};
</script>

<style scoped>
.preview-container {
  max-height: 600px;
  overflow-y: auto;
}

.info-card, .stats-card, .relation-card, .media-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.count-badge {
  background: #409eff;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.cover-container {
  width: 100%;
  height: 200px;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-cover {
  text-align: center;
  color: #999;
}

.no-cover i {
  font-size: 48px;
  margin-bottom: 10px;
  display: block;
}

.info-content {
  padding-left: 20px;
}

.video-title {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 20px;
  font-weight: bold;
}

.video-subtitle {
  margin: 0 0 20px 0;
  color: #666;
  font-size: 14px;
}

.info-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.label {
  width: 80px;
  color: #666;
  font-size: 14px;
}

.value {
  color: #333;
  font-size: 14px;
}

.description-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.description-section h4 {
  margin: 0 0 10px 0;
  color: #333;
}

.description-text {
  margin: 0;
  color: #666;
  line-height: 1.6;
  white-space: pre-wrap;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.relation-content {
  min-height: 60px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  align-content: flex-start;
}

.relation-tag {
  margin: 0 8px 8px 0;
}

.empty-relation {
  width: 100%;
  text-align: center;
  color: #999;
  padding: 20px 0;
}

.empty-relation i {
  font-size: 32px;
  margin-bottom: 8px;
  display: block;
}

.media-content {
  padding: 20px 0;
}

.media-stat {
  display: flex;
  align-items: center;
}

.media-stat i {
  font-size: 32px;
  color: #409eff;
  margin-right: 15px;
}

.media-info {
  flex: 1;
}

.media-count {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.media-desc {
  font-size: 14px;
  color: #666;
}
</style> 