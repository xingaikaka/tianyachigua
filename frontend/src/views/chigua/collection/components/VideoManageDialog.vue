<template>
  <el-dialog
    title="管理合集视频"
    :visible.sync="dialogVisible"
    width="1200px"
    append-to-body
    @close="handleClose"
    :close-on-click-modal="false"
  >
    <div v-if="collection.id" class="video-manage">
      <!-- 合集信息头部 -->
      <div class="collection-header">
        <h3>{{ collection.title }}</h3>
        <div class="collection-stats">
          <el-tag type="success">当前包含 {{ collectionVideos.length }} 个视频</el-tag>
        </div>
      </div>

      <!-- 双面板布局 -->
      <div class="panels-container">
        <!-- 左侧：可用视频列表 -->
        <div class="panel available-panel">
          <div class="panel-header">
            <h4>可用视频</h4>
            <div class="search-controls">
              <el-input
                v-model="searchQuery.title"
                placeholder="搜索视频标题"
                size="small"
                clearable
                @input="searchAvailableVideos"
                style="width: 200px; margin-right: 10px;"
              />
              <el-select
                v-model="searchQuery.categoryId"
                placeholder="选择分类"
                size="small"
                clearable
                @change="searchAvailableVideos"
                style="width: 120px;"
              >
                <el-option
                  v-for="category in categoryOptions"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </div>
          </div>
          
          <div class="panel-content">
            <div class="video-list available-videos">
              <div
                v-for="video in availableVideos"
                :key="'available-' + video.id"
                class="video-item"
                @click="selectVideo(video, 'available')"
                :class="{ selected: selectedAvailable.includes(video.id) }"
              >
                <div class="video-cover">
                  <image-preview 
                    v-if="video.coverImage"
                    :src="video.coverImage" 
                    :width="60" 
                    :height="45"
                  />
                  <div v-else class="no-cover">
                    <i class="el-icon-video-camera"></i>
                  </div>
                </div>
                <div class="video-info">
                  <el-tooltip :content="video.title" placement="top" effect="dark">
                    <div class="video-title ellipsis">{{ video.title }}</div>
                  </el-tooltip>
                  <div class="video-meta">
                    <span v-if="video.author">{{ video.author }}</span>
                    <span>{{ video.viewCount || 0 }} 次观看</span>
                  </div>
                </div>
                <div class="video-actions">
                  <el-button
                    type="primary"
                    size="mini"
                    icon="el-icon-right"
                    @click.stop="addToCollection([video.id])"
                  >添加</el-button>
                </div>
              </div>
            </div>
            
            <!-- 分页 -->
            <div class="pagination-wrapper">
              <el-pagination
                v-if="availableTotal > 0"
                :current-page="availablePageNum"
                :page-size="availablePageSize"
                :total="availableTotal"
                layout="prev, pager, next"
                @current-change="handleAvailablePageChange"
                small
              />
            </div>
          </div>

          <!-- 批量操作 -->
          <div class="panel-footer">
            <el-button
              type="primary"
              size="small"
              :disabled="selectedAvailable.length === 0"
              @click="addToCollection(selectedAvailable)"
            >
              批量添加 ({{ selectedAvailable.length }})
            </el-button>
          </div>
        </div>

        <!-- 右侧：合集视频列表 -->
        <div class="panel collection-panel">
          <div class="panel-header">
            <h4>合集视频</h4>
            <div class="sort-controls">
              <el-button
                type="text"
                size="small"
                @click="enableSortMode"
                v-if="!sortMode"
              >
                <i class="el-icon-sort"></i> 排序模式
              </el-button>
              <div v-else>
                <el-button type="success" size="small" @click="saveSortOrder">保存排序</el-button>
                <el-button type="info" size="small" @click="cancelSortMode">取消</el-button>
              </div>
            </div>
          </div>

          <div class="panel-content">
            <div class="video-list collection-videos" :class="{ 'sort-mode': sortMode }">
              <div
                v-for="(video, index) in collectionVideos"
                :key="'collection-' + video.id"
                class="video-item"
                @click="selectVideo(video, 'collection')"
                :class="{ 
                  selected: selectedCollection.includes(video.id),
                  sortable: sortMode
                }"
              >
                <div class="sort-handle" v-if="sortMode">
                  <i class="el-icon-rank"></i>
                </div>
                <div class="video-index">{{ index + 1 }}</div>
                <div class="video-cover">
                  <image-preview 
                    v-if="video.coverImage"
                    :src="video.coverImage" 
                    :width="60" 
                    :height="45"
                  />
                  <div v-else class="no-cover">
                    <i class="el-icon-video-camera"></i>
                  </div>
                </div>
                <div class="video-info">
                  <el-tooltip :content="video.title" placement="top" effect="dark">
                    <div class="video-title ellipsis">{{ video.title }}</div>
                  </el-tooltip>
                  <div class="video-meta">
                    <span v-if="video.author">{{ video.author }}</span>
                    <span>{{ video.viewCount || 0 }} 次观看</span>
                  </div>
                </div>
                <div class="video-actions" v-if="!sortMode">
                  <el-button
                    type="danger"
                    size="mini"
                    icon="el-icon-left"
                    @click.stop="removeFromCollection([video.id])"
                  >移除</el-button>
                </div>
                <div class="sort-actions" v-if="sortMode">
                  <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-top"
                    @click.stop="moveVideo(index, 'up')"
                    :disabled="index === 0"
                  />
                  <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-bottom"
                    @click.stop="moveVideo(index, 'down')"
                    :disabled="index === collectionVideos.length - 1"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 批量操作 -->
          <div class="panel-footer" v-if="!sortMode">
            <el-button
              type="danger"
              size="small"
              :disabled="selectedCollection.length === 0"
              @click="removeFromCollection(selectedCollection)"
            >
              批量移除 ({{ selectedCollection.length }})
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { 
  getCollectionVideos, 
  getAvailableVideos, 
  addVideosToCollection, 
  removeVideosFromCollection,
  updateVideoSort,
  getAllCategories
} from "@/api/chigua/collection";
import ImagePreview from '@/components/ImagePreview';

export default {
  name: "VideoManageDialog",
  components: {
    ImagePreview
  },
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
      loading: false,
      
      // 合集视频列表
      collectionVideos: [],
      originalCollectionVideos: [], // 用于排序取消时恢复
      
      // 可用视频列表
      availableVideos: [],
      availableTotal: 0,
      availablePageNum: 1,
      availablePageSize: 10,
      
      // 搜索条件
      searchQuery: {
        title: '',
        categoryId: null,
        status: 1 // 默认只显示已发布的视频
      },
      
      // 选中的视频
      selectedAvailable: [],
      selectedCollection: [],
      
      // 分类选项
      categoryOptions: [],
      
      // 排序模式
      sortMode: false,
      
      // 防抖定时器
      searchTimer: null
    };
  },
  watch: {
    visible(val) {
      this.dialogVisible = val;
      if (val && this.collection.id) {
        this.loadData();
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val);
    }
  },
  methods: {
    /** 加载数据 */
    loadData() {
      this.loadCollectionVideos();
      this.loadAvailableVideos();
      this.loadCategories();
    },
    
    /** 加载合集视频 */
    loadCollectionVideos() {
      getCollectionVideos(this.collection.id).then(response => {
        this.collectionVideos = response.rows || [];
        this.originalCollectionVideos = [...this.collectionVideos];
      });
    },
    
    /** 加载可用视频 */
    loadAvailableVideos() {
      this.loading = true;
      const params = {
        pageNum: this.availablePageNum,
        pageSize: this.availablePageSize,
        ...this.searchQuery
      };
      
      getAvailableVideos(this.collection.id, params).then(response => {
        this.availableVideos = response.rows || [];
        this.availableTotal = response.total || 0;
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    
    /** 加载分类选项 */
    loadCategories() {
      getAllCategories().then(response => {
        this.categoryOptions = response.data || [];
      });
    },
    
    /** 搜索可用视频（防抖） */
    searchAvailableVideos() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }
      this.searchTimer = setTimeout(() => {
        this.availablePageNum = 1;
        this.loadAvailableVideos();
      }, 300);
    },
    
    /** 可用视频分页变化 */
    handleAvailablePageChange(page) {
      this.availablePageNum = page;
      this.loadAvailableVideos();
    },
    
    /** 选择视频 */
    selectVideo(video, type) {
      if (this.sortMode) return; // 排序模式下不允许选择
      
      if (type === 'available') {
        const index = this.selectedAvailable.indexOf(video.id);
        if (index > -1) {
          this.selectedAvailable.splice(index, 1);
        } else {
          this.selectedAvailable.push(video.id);
        }
      } else {
        const index = this.selectedCollection.indexOf(video.id);
        if (index > -1) {
          this.selectedCollection.splice(index, 1);
        } else {
          this.selectedCollection.push(video.id);
        }
      }
    },
    
    /** 添加视频到合集 */
    addToCollection(videoIds) {
      if (!videoIds || videoIds.length === 0) return;
      
      this.loading = true;
      addVideosToCollection(this.collection.id, videoIds).then(() => {
        this.$message.success(`成功添加 ${videoIds.length} 个视频到合集`);
        this.selectedAvailable = [];
        this.loadCollectionVideos();
        this.loadAvailableVideos();
        this.$emit('refresh');
      }).catch(() => {
        this.loading = false;
      });
    },
    
    /** 从合集移除视频 */
    removeFromCollection(videoIds) {
      if (!videoIds || videoIds.length === 0) return;
      
      this.$confirm(`确定要从合集中移除 ${videoIds.length} 个视频吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        this.loading = true;
        removeVideosFromCollection(this.collection.id, videoIds).then(() => {
          this.$message.success(`成功移除 ${videoIds.length} 个视频`);
          this.selectedCollection = [];
          this.loadCollectionVideos();
          this.loadAvailableVideos();
          this.$emit('refresh');
        }).catch(() => {
          this.loading = false;
        });
      });
    },
    
    /** 启用排序模式 */
    enableSortMode() {
      this.sortMode = true;
      this.selectedCollection = [];
      this.originalCollectionVideos = [...this.collectionVideos];
    },
    
    /** 取消排序模式 */
    cancelSortMode() {
      this.sortMode = false;
      this.collectionVideos = [...this.originalCollectionVideos];
    },
    
    /** 移动视频位置 */
    moveVideo(index, direction) {
      const videos = [...this.collectionVideos];
      if (direction === 'up' && index > 0) {
        [videos[index], videos[index - 1]] = [videos[index - 1], videos[index]];
      } else if (direction === 'down' && index < videos.length - 1) {
        [videos[index], videos[index + 1]] = [videos[index + 1], videos[index]];
      }
      this.collectionVideos = videos;
    },
    
    /** 保存排序 */
    saveSortOrder() {
      const sortData = this.collectionVideos.map((video, index) => ({
        id: video.id,
        sortOrder: index + 1
      }));
      
      this.loading = true;
      updateVideoSort(this.collection.id, sortData).then(() => {
        this.$message.success('排序保存成功');
        this.sortMode = false;
        this.loadCollectionVideos();
        this.$emit('refresh');
      }).catch(() => {
        this.loading = false;
      });
    },
    
    /** 关闭对话框 */
    handleClose() {
      this.dialogVisible = false;
      this.resetData();
    },
    
    /** 重置数据 */
    resetData() {
      this.collectionVideos = [];
      this.availableVideos = [];
      this.selectedAvailable = [];
      this.selectedCollection = [];
      this.searchQuery = { title: '', categoryId: null, status: 1 };
      this.availablePageNum = 1;
      this.sortMode = false;
      
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
        this.searchTimer = null;
      }
    }
  }
};
</script>

<style scoped>
.video-manage {
  padding: 10px 0;
}

.collection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.collection-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.panels-container {
  display: flex;
  gap: 20px;
  height: 600px;
}

.panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.panel-header {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
}

.panel-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.search-controls {
  display: flex;
  align-items: center;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.video-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.video-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.video-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.video-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.video-item.sortable {
  cursor: move;
}

.sort-handle {
  cursor: move;
  color: #909399;
  font-size: 16px;
  margin-right: 5px;
}

.video-index {
  width: 20px;
  height: 20px;
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
  width: 60px;
  height: 45px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.no-cover i {
  font-size: 20px;
}

.video-info {
  flex: 1;
  min-width: 0;
}

.video-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px; /* 防止长标题撑开布局 */
}

.video-meta {
  font-size: 11px;
  color: #909399;
}

.video-meta span {
  margin-right: 10px;
}

.video-actions,
.sort-actions {
  flex-shrink: 0;
}

.sort-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pagination-wrapper {
  padding: 10px;
  text-align: center;
  border-top: 1px solid #ebeef5;
}

.panel-footer {
  padding: 15px;
  border-top: 1px solid #ebeef5;
  background: #f8f9fa;
}

.sort-mode .video-item {
  border-style: dashed;
}

/* 响应式 */
@media (max-width: 1400px) {
  .panels-container {
    height: 500px;
  }
}
</style> 