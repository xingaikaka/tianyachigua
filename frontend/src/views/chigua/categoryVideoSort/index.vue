<template>
  <div class="app-container">
    <!-- 分类列表视图 -->
    <div v-if="!currentCategory" class="category-list-view">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input
            v-model="queryParams.categoryName"
            placeholder="请输入分类名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="info"
            plain
            icon="el-icon-info"
            size="mini"
          >分类视频排序管理</el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getCategoryList"></right-toolbar>
      </el-row>

      <!-- 分类列表表格 -->
      <el-table v-loading="loading" :data="filteredCategoryList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="分类ID" align="center" prop="id" width="80" />
        <el-table-column label="分类名称" align="center" prop="name" :show-overflow-tooltip="true" />
        <el-table-column label="排序权重" align="center" prop="sortOrder" width="100" />
        <el-table-column label="状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已排序视频" align="center" width="120">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.sortedVideoCount > 0" type="primary">
              {{ scope.row.sortedVideoCount }} 个
            </el-tag>
            <el-tag v-else type="info">未设置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="primary"
              icon="el-icon-sort"
              @click="handleManageSort(scope.row)"
              v-hasPermi="['chigua:categoryVideoSort:edit']"
            >管理排序</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleViewSort(scope.row)"
              v-hasPermi="['chigua:categoryVideoSort:list']"
            >查看排序</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total>0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getCategoryList"
      />
    </div>

    <!-- 分类视频排序管理视图 -->
    <div v-else class="sort-management-view">
      <!-- 返回按钮和分类信息 -->
      <el-row :gutter="10" class="mb8">
        <el-col :span="12">
          <el-button
            type="primary"
            plain
            icon="el-icon-back"
            size="mini"
            @click="handleBackToList"
          >返回分类列表</el-button>
          <el-tag type="primary" class="ml10">
            当前分类：{{ currentCategory.name }}
          </el-tag>
        </el-col>
        <el-col :span="12" class="text-right">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['chigua:categoryVideoSort:add']"
          >添加视频</el-button>
          <el-button
            type="success"
            plain
            icon="el-icon-check"
            size="mini"
            @click="handleSaveSort"
            v-hasPermi="['chigua:categoryVideoSort:edit']"
            :disabled="sortedVideoList.length === 0"
          >保存排序</el-button>
        </el-col>
      </el-row>

      <el-alert
        title="拖拽视频卡片可以调整排序，排序越靠前的视频在前端显示时优先级越高"
        type="warning"
        :closable="false"
        show-icon
        class="mb20">
      </el-alert>

      <div class="mb20">
        <el-tag type="success">已选视频：{{ sortedVideoList.length }} 个</el-tag>
      </div>

      <!-- 视频排序列表 -->
      <!-- 列表排序模式（可拖动、可编辑排序值） -->
      <div class="video-sort-list" v-if="sortedVideoList.length > 0">
        <draggable :list="sortedVideoList" handle=".drag-handle" item-key="id">
          <transition-group type="transition" name="flip-list" tag="div">
            <div class="sort-row" v-for="(item, index) in sortedVideoList" :key="item.id">
              <span class="drag-handle el-icon-rank" title="拖动排序"></span>
              <span class="row-index">{{ index + 1 }}</span>
              <image-preview 
                class="row-cover" 
                v-if="item.video && (item.video.coverImage || item.video.coverUrl)"
                :src="buildImageSrc(item.video.coverImage || item.video.coverUrl)"
                :width="80" 
                :height="45" 
              />
              <img v-else class="row-cover" src="/static/default-cover.jpg" />
              <span class="row-title">{{ item.video ? item.video.title : ('#' + item.videoId) }}</span>
              <el-input-number
                v-model="item.sortOrder"
                :min="0"
                :max="999999"
                :step="1"
                size="small"
              />
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleRemoveVideo(item)">移除</el-button>
            </div>
          </transition-group>
        </draggable>
      </div>

      <div v-else class="empty-state">
        <el-empty description="该分类暂无排序视频">
          <el-button type="primary" @click="handleAdd">添加视频</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 添加视频对话框 -->
    <el-dialog title="添加视频到排序" :visible.sync="addDialogVisible" width="800px" append-to-body>
      <el-form :model="addForm" ref="addForm" size="small" label-width="80px">
        <el-form-item label="搜索视频">
          <el-input
            v-model="videoSearchKeyword"
            placeholder="输入视频标题搜索"
            @input="handleVideoSearch"
            clearable>
            <i slot="prefix" class="el-input__icon el-icon-search"></i>
          </el-input>
        </el-form-item>
      </el-form>

      <div class="available-videos" v-loading="videoLoading">
        <div v-if="availableVideoList.length > 0" class="video-grid">
          <div 
            v-for="video in availableVideoList" 
            :key="video.id"
            class="video-item"
            :class="{ 'selected': selectedVideos.includes(video.id) }"
            @click="toggleVideoSelection(video)">
            <div class="video-cover-small">
              <image-preview 
                v-if="video.coverImage || video.coverUrl"
                :src="buildImageSrc(video.coverImage || video.coverUrl)" 
                :width="60" 
                :height="45"/>
              <img v-else src="/static/default-cover.jpg" alt="封面" />
            </div>
            <div class="video-info-small">
              <h5>{{ video.title }}</h5>
              <p>{{ parseTime(video.publishedAt) }}</p>
            </div>
            <div class="selection-indicator">
              <i class="el-icon-check" v-if="selectedVideos.includes(video.id)"></i>
            </div>
          </div>
        </div>
        <el-empty v-else description="没有找到可添加的视频"></el-empty>
      </div>

      <!-- 视频分页 -->
      <div class="video-pagination" v-if="availableVideoList.length > 0">
        <el-pagination
          background
          layout="prev, pager, next, sizes"
          :current-page="videoQueryParams.pageNum"
          :page-size="videoQueryParams.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="videoTotal"
          @current-change="handleVideoPageChange"
          @size-change="handleVideoSizeChange"
        />
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="addDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleConfirmAdd" :disabled="selectedVideos.length === 0">
          添加选中视频 ({{ selectedVideos.length }})
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
  listCategoryVideoSort, 
  getCategoryVideoSort, 
  delCategoryVideoSort, 
  addCategoryVideoSort, 
  updateCategoryVideoSort,
  listByCategory,
  getAvailableVideos,
  getCategories,
  batchSetCategoryVideoSort,
  removeCategoryVideoSort
} from "@/api/chigua/categoryVideoSort";
import draggable from 'vuedraggable';
import ImagePreview from '@/components/ImagePreview';

export default {
  name: "CategoryVideoSort",
  components: {
    draggable,
    ImagePreview
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 分类视频排序表格数据
      categoryVideoSortList: [],
      // 排序后的视频列表
      sortedVideoList: [],
      // 分类列表
      categoryList: [],
      // 过滤后的分类列表
      filteredCategoryList: [],
      // 当前操作的分类
      currentCategory: null,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 添加视频对话框
      addDialogVisible: false,
      // 可用视频列表
      availableVideoList: [],
      // 选中的视频ID列表
      selectedVideos: [],
      // 视频搜索关键词
      videoSearchKeyword: '',
      // 视频加载状态
      videoLoading: false,
      // 视频分页参数
      videoQueryParams: {
        pageNum: 1,
        pageSize: 30,
        title: ''
      },
      // 视频总数
      videoTotal: 0,
      // 拖拽状态
      drag: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        categoryName: null,
        categoryId: null,
        videoId: null,
        sortOrder: null
      },
      // 添加表单
      addForm: {},
      // 表单校验
      rules: {}
    };
  },
  created() {
    this.getCategoryList();
  },
  methods: {
    // 统一封面地址构建：
    // - http/https 直接返回
    // - 相对路径确保以 / 开头，交给 ImagePreview 去拼接 VUE_APP_BASE_API
    buildImageSrc(raw) {
      if (!raw) return '';
      const url = String(raw).trim();
      if (url.startsWith('http://') || url.startsWith('https://')) return url;
      // 若已是/images或/profile/images开头，原样或压回/images
      if (url.startsWith('/images/')) return url;
      if (url.startsWith('/profile/images/')) return url.replace('/profile', '');
      // 将 images/... 规范为 /images/...
      const normalized = url.replace(/^\/+/, '');
      if (normalized.startsWith('images/')) return `/${normalized}`;
      return `/${normalized}`;
    },
    /** 获取分类列表 */
    async getCategoryList() {
      this.loading = true;
      console.log('🔍 开始获取分类列表...');
      try {
        const response = await getCategories();
        console.log('✅ 分类列表获取成功:', response);
        console.log('📊 API返回的数据:', response.data);
        this.categoryList = response.data || [];
        console.log('📋 设置后的categoryList:', this.categoryList);
        console.log('📋 categoryList长度:', this.categoryList.length);
        
        // 等待统计数据加载完成
        await this.loadCategorySortCounts();
        this.filterCategoryList();
        this.loading = false;
      } catch (error) {
        console.error('❌ 分类列表获取失败:', error);
        this.$modal.msgError('获取分类列表失败: ' + (error.message || '未知错误'));
        this.loading = false;
      }
    },
    /** 加载分类排序视频数量 */
    async loadCategorySortCounts() {
      console.log('🔢 开始加载分类排序视频数量，分类数量:', this.categoryList.length);
      
      // 使用Promise.all并行处理所有分类的统计
      const promises = this.categoryList.map(async (category) => {
        try {
          console.log('📊 查询分类', category.id, category.name, '的排序视频数量');
          const response = await listByCategory(category.id);
          category.sortedVideoCount = response.data ? response.data.length : 0;
          console.log('✅ 分类', category.name, '排序视频数量:', category.sortedVideoCount);
        } catch (error) {
          console.error('❌ 获取分类', category.name, '排序视频数量失败:', error);
          category.sortedVideoCount = 0;
        }
      });
      
      await Promise.all(promises);
      console.log('🔢 所有分类排序视频数量加载完成');
      
      // 强制更新视图
      this.$forceUpdate();
      this.filterCategoryList();
    },
    /** 过滤分类列表 */
    filterCategoryList() {
      console.log('🔍 开始过滤分类列表，原始数量:', this.categoryList.length);
      let filtered = [...this.categoryList];
      
      // 按分类名称过滤
      if (this.queryParams.categoryName) {
        filtered = filtered.filter(category => 
          category.name.toLowerCase().includes(this.queryParams.categoryName.toLowerCase())
        );
        console.log('🔍 按名称过滤后数量:', filtered.length);
      }
      
      this.filteredCategoryList = filtered;
      this.total = filtered.length;
      console.log('✅ 过滤完成，最终显示数量:', this.filteredCategoryList.length);
      console.log('📋 过滤后的分类列表:', this.filteredCategoryList);
    },
    /** 查询分类视频排序列表 */
    getList() {
      if (!this.currentCategory) {
        console.log('❌ 没有选择分类，清空排序列表');
        this.sortedVideoList = [];
        return;
      }

      console.log('🔍 开始获取分类排序视频列表，分类ID:', this.currentCategory.id);
      this.loading = true;
      listByCategory(this.currentCategory.id).then(response => {
        console.log('✅ 分类排序视频列表获取成功:', response);
        console.log('📊 API返回的排序视频数据:', response.data);
        this.sortedVideoList = response.data || [];
        console.log('📋 设置后的sortedVideoList:', this.sortedVideoList);
        console.log('📋 sortedVideoList长度:', this.sortedVideoList.length);
        this.loading = false;
      }).catch(error => {
        console.error('❌ 分类排序视频列表获取失败:', error);
        this.loading = false;
      });
    },
    /** 管理排序按钮 */
    handleManageSort(category) {
      console.log('🎯 点击管理排序按钮，分类:', category);
      this.currentCategory = category;
      this.queryParams.categoryId = category.id;
      console.log('📝 设置当前分类:', this.currentCategory);
      this.getList();
      this.selectedVideos = [];
    },
    /** 查看排序按钮 */
    handleViewSort(category) {
      this.handleManageSort(category);
    },
    /** 返回分类列表 */
    async handleBackToList() {
      this.currentCategory = null;
      this.queryParams.categoryId = null;
      this.sortedVideoList = [];
      // 重新获取分类列表并加载统计数据
      await this.getCategoryList();
      // 强制刷新视图
      this.$nextTick(() => {
        this.$forceUpdate();
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      if (this.currentCategory) {
        // 在排序管理视图中，搜索功能暂不实现
        this.getList();
      } else {
        // 在分类列表视图中，过滤分类列表
        this.queryParams.pageNum = 1;
        this.filterCategoryList();
      }
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.categoryName = null;
      if (this.currentCategory) {
        this.getList();
      } else {
        this.filterCategoryList();
      }
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      if (!this.currentCategory) {
        this.$modal.msgError("请先选择分类");
        return;
      }
      this.selectedVideos = [];
      this.videoSearchKeyword = '';
      // 重置视频分页参数
      this.videoQueryParams.pageNum = 1;
      this.videoQueryParams.pageSize = 30;
      this.videoQueryParams.title = '';
      this.loadAvailableVideos();
      this.addDialogVisible = true;
    },
    /** 加载可用视频 */
    loadAvailableVideos() {
      this.videoLoading = true;
      this.videoQueryParams.title = this.videoSearchKeyword;
      console.log('🔍 加载可用视频:', this.videoQueryParams);
      
      getAvailableVideos(this.currentCategory.id, this.videoQueryParams).then(response => {
        this.availableVideoList = response.data || [];
        // 注意：由于后端使用了过滤，实际返回的数量可能少于pageSize
        // 这里暂时不处理总数，因为需要额外的接口来获取总数
        this.videoLoading = false;
        console.log('✅ 可用视频加载完成，共', this.availableVideoList.length, '条');
      }).catch(error => {
        console.error('❌ 加载可用视频失败:', error);
        this.videoLoading = false;
      });
    },
    /** 视频搜索 */
    handleVideoSearch() {
      // 防抖处理
      clearTimeout(this.searchTimer);
      this.searchTimer = setTimeout(() => {
        this.videoQueryParams.pageNum = 1; // 搜索时重置到第一页
        this.loadAvailableVideos();
      }, 500);
    },
    /** 视频分页改变 */
    handleVideoPageChange(page) {
      this.videoQueryParams.pageNum = page;
      this.loadAvailableVideos();
    },
    /** 视频分页大小改变 */
    handleVideoSizeChange(size) {
      this.videoQueryParams.pageSize = size;
      this.videoQueryParams.pageNum = 1;
      this.loadAvailableVideos();
    },
    /** 切换视频选择 */
    toggleVideoSelection(video) {
      const index = this.selectedVideos.indexOf(video.id);
      if (index > -1) {
        this.selectedVideos.splice(index, 1);
      } else {
        this.selectedVideos.push(video.id);
      }
    },
    /** 确认添加视频 */
    handleConfirmAdd() {
      if (this.selectedVideos.length === 0) {
        this.$modal.msgError("请选择要添加的视频");
        return;
      }

      // 构建视频排序数据
      const videoSorts = this.selectedVideos.map(videoId => {
        const video = this.availableVideoList.find(v => v.id === videoId);
        return {
          videoId: videoId,
          title: video ? video.title : ''
        };
      });

      // 添加到现有排序列表的末尾
      const existingSorts = this.sortedVideoList.map(item => ({
        videoId: item.videoId,
        title: item.video ? item.video.title : ''
      }));

      const allSorts = [...existingSorts, ...videoSorts];

      // 批量保存
      const request = {
        categoryId: this.currentCategory.id,
        videoSorts: allSorts
      };

      batchSetCategoryVideoSort(request).then(response => {
        this.$modal.msgSuccess("添加成功");
        this.addDialogVisible = false;
        this.getList();
        // 更新分类列表中的计数
        this.updateCategorySortCount(this.currentCategory.id);
      });
    },
    /** 保存排序 */
    handleSaveSort() {
      if (this.sortedVideoList.length === 0) {
        this.$modal.msgError("没有可保存的排序数据");
        return;
      }

      const videoSorts = this.sortedVideoList.map(item => ({
        videoId: item.videoId,
        title: item.video ? item.video.title : ''
      }));

      const request = {
        categoryId: this.currentCategory.id,
        videoSorts: videoSorts
      };

      batchSetCategoryVideoSort(request).then(response => {
        this.$modal.msgSuccess("排序保存成功");
        this.getList();
        // 更新分类列表中的计数
        this.updateCategorySortCount(this.currentCategory.id);
      });
    },
    /** 更新分类排序视频计数 */
    updateCategorySortCount(categoryId) {
      const category = this.categoryList.find(c => c.id === categoryId);
      if (category) {
        listByCategory(categoryId).then(response => {
          category.sortedVideoCount = response.data ? response.data.length : 0;
        });
      }
    },
    /** 移除视频 */
    handleRemoveVideo(item) {
      const self = this;
      this.$modal.confirm('是否确认移除视频"' + item.video.title + '"的排序？').then(function() {
        return removeCategoryVideoSort(item.categoryId, item.videoId);
      }).then(() => {
        self.getList();
        self.$modal.msgSuccess("移除成功");
        // 更新分类列表中的计数
        self.updateCategorySortCount(item.categoryId);
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除分类视频排序编号为"' + ids + '"的数据项？').then(function() {
        return delCategoryVideoSort(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>

<style scoped>
.category-list-view {
  margin-top: 0;
}

.sort-management-view {
  margin-top: 0;
}

.sort-container {
  margin-top: 20px;
}

.ml10 {
  margin-left: 10px;
}

.text-right {
  text-align: right;
}

.video-sort-list {
  min-height: 200px;
}

.sort-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 10px;
  background: #fff;
}

.drag-handle {
  cursor: move;
  color: #606266;
  font-size: 18px;
  width: 22px;
}

.drag-handle:hover {
  color: #409eff;
}

.row-index {
  width: 24px;
  color: #606266;
  font-weight: 500;
}

.row-cover {
  width: 80px;
  height: 45px;
  border-radius: 4px;
  object-fit: cover;
}

.row-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.draggable-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.video-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  cursor: move;
  transition: all 0.3s;
}

.video-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.video-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.video-cover {
  position: relative;
  width: 120px;
  height: 68px;
  margin-right: 16px;
  border-radius: 4px;
  overflow: hidden;
}

.video-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sort-number {
  position: absolute;
  top: 4px;
  left: 4px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.video-details {
  flex: 1;
}

.video-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
}

.video-meta {
  margin: 0 0 4px 0;
  font-size: 12px;
  color: #909399;
}

.video-meta span {
  margin-right: 16px;
}

.video-sort {
  margin: 0;
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}

.video-actions {
  margin-left: 16px;
}

.select-category-tip {
  margin-top: 40px;
}

.available-videos {
  max-height: 400px;
  overflow-y: auto;
  margin-top: 16px;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.video-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.video-item:hover {
  border-color: #409eff;
}

.video-item.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.video-cover-small {
  width: 60px;
  height: 34px;
  margin-right: 12px;
  border-radius: 4px;
  overflow: hidden;
}

.video-cover-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-info-small {
  flex: 1;
}

.video-info-small h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
}

.video-info-small p {
  margin: 0;
  font-size: 12px;
  color: #606266;
}

.selection-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-item.selected .selection-indicator {
  opacity: 1;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.mb20 {
  margin-bottom: 20px;
}

/* 视频分页样式 */
.video-pagination {
  padding: 20px 0;
  text-align: center;
  border-top: 1px solid #ebeef5;
  margin-top: 20px;
}
</style>
