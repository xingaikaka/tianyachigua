<template>
  <el-dialog
    title="合集预览"
    :visible.sync="dialogVisible"
    width="800px"
    append-to-body
    @close="handleClose"
  >
    <div v-if="collection.id" class="collection-preview">
      <!-- 合集基本信息 -->
      <div class="collection-header">
        <div class="cover-image">
          <el-image
            v-if="collection.coverImage"
            :src="collection.coverImage"
            fit="cover"
            style="width: 200px; height: 120px; border-radius: 8px;"
          />
          <div v-else class="no-image">
            <i class="el-icon-picture-outline"></i>
            <span>暂无封面</span>
          </div>
        </div>
        <div class="collection-info">
          <h2 class="title">{{ collection.title }}</h2>
          <div class="meta-info">
            <el-tag v-if="collection.author" type="info" size="small">
              <i class="el-icon-user"></i> {{ collection.author }}
            </el-tag>
            <el-tag type="success" size="small">
              <i class="el-icon-video-camera"></i> {{ collection.videoCount || 0 }} 个视频
            </el-tag>
            <el-tag type="warning" size="small">
              <i class="el-icon-view"></i> {{ collection.viewCount || 0 }} 次观看
            </el-tag>
            <el-tag :type="collection.status === 1 ? 'success' : 'danger'" size="small">
              {{ collection.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </div>
          <div v-if="collection.description" class="description">
            <p>{{ collection.description }}</p>
          </div>
          <div class="timestamps">
            <span class="create-time">创建时间：{{ parseTime(collection.createdAt) }}</span>
            <span class="update-time">更新时间：{{ parseTime(collection.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 分类信息 -->
      <div v-if="categories.length > 0" class="categories-section">
        <h3>所属分类</h3>
        <div class="categories">
          <el-tag
            v-for="category in categories"
            :key="category.id"
            type="primary"
            size="medium"
            class="category-tag"
          >
            {{ category.name }}
          </el-tag>
        </div>
      </div>

      <!-- 视频列表 -->
      <div class="videos-section">
        <h3>包含视频 ({{ videos.length }})</h3>
        <div v-if="videos.length > 0" class="videos-list">
          <div
            v-for="(video, index) in videos"
            :key="video.id"
            class="video-item"
          >
            <div class="video-index">{{ index + 1 }}</div>
            <div class="video-cover">
              <el-image
                v-if="video.coverImage"
                :src="video.coverImage"
                fit="cover"
                style="width: 80px; height: 60px; border-radius: 4px;"
              />
              <div v-else class="no-cover">
                <i class="el-icon-video-camera"></i>
              </div>
            </div>
            <div class="video-info">
              <div class="video-title">{{ video.title }}</div>
              <div class="video-meta">
                <span v-if="video.author" class="author">{{ video.author }}</span>
                <span class="view-count">{{ video.viewCount || 0 }} 次观看</span>
                <span class="duration" v-if="video.duration">{{ formatDuration(video.duration) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-videos">
          <el-empty description="暂无视频" :image-size="100" />
        </div>
      </div>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">关 闭</el-button>
      <el-button type="primary" @click="handleEdit">编 辑</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getCollection, getCollectionVideos } from "@/api/chigua/collection";

export default {
  name: "CollectionPreviewDialog",
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    collection: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      dialogVisible: false,
      categories: [],
      videos: [],
      loading: false
    };
  },
  watch: {
    visible(val) {
      this.dialogVisible = val;
      if (val && this.collection.id) {
        this.loadCollectionDetails();
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val);
    }
  },
  methods: {
    /** 加载合集详细信息 */
    loadCollectionDetails() {
      this.loading = true;
      
      // 获取合集详细信息（包含分类）
      getCollection(this.collection.id).then(response => {
        this.categories = response.data.categories || [];
      });

      // 获取合集中的视频列表
      getCollectionVideos(this.collection.id).then(response => {
        this.videos = response.rows || [];
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    /** 格式化时长 */
    formatDuration(seconds) {
      if (!seconds) return '';
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;
      
      if (hours > 0) {
        return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
      } else {
        return `${minutes}:${secs.toString().padStart(2, '0')}`;
      }
    },
    /** 关闭对话框 */
    handleClose() {
      this.dialogVisible = false;
      this.categories = [];
      this.videos = [];
    },
    /** 编辑按钮 */
    handleEdit() {
      this.$emit('edit', this.collection);
      this.handleClose();
    }
  }
};
</script>

<style scoped>
.collection-preview {
  padding: 10px 0;
}

.collection-header {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.cover-image {
  flex-shrink: 0;
}

.no-image {
  width: 200px;
  height: 120px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.no-image i {
  font-size: 32px;
  margin-bottom: 8px;
}

.collection-info {
  flex: 1;
}

.title {
  margin: 0 0 15px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.meta-info {
  margin-bottom: 15px;
}

.meta-info .el-tag {
  margin-right: 10px;
  margin-bottom: 5px;
}

.meta-info .el-tag i {
  margin-right: 4px;
}

.description {
  margin-bottom: 15px;
  color: #606266;
  line-height: 1.6;
}

.timestamps {
  font-size: 12px;
  color: #909399;
}

.timestamps span {
  margin-right: 20px;
}

.categories-section,
.videos-section {
  margin-bottom: 30px;
}

.categories-section h3,
.videos-section h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-tag {
  margin: 0;
}

.videos-list {
  max-height: 400px;
  overflow-y: auto;
}

.video-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  transition: all 0.3s;
}

.video-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.video-index {
  width: 24px;
  height: 24px;
  background: #409eff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.video-cover {
  flex-shrink: 0;
}

.no-cover {
  width: 80px;
  height: 60px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.no-cover i {
  font-size: 24px;
}

.video-info {
  flex: 1;
  min-width: 0;
}

.video-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-meta {
  font-size: 12px;
  color: #909399;
}

.video-meta span {
  margin-right: 15px;
}

.empty-videos {
  text-align: center;
  padding: 40px 0;
}
</style> 