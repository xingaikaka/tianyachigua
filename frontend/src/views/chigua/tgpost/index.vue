<template>
  <div class="app-container">

    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="帖子ID" prop="id">
        <el-input v-model.number="queryParams.id" placeholder="精确匹配ID" clearable style="width:120px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="全部分类" clearable style="width:150px">
          <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width:110px">
          <el-option label="显示" :value="1" />
          <el-option label="隐藏" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="源ID" prop="sourceId">
        <el-input v-model="queryParams.sourceId" placeholder="源ID模糊搜索" clearable style="width:150px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="正文" prop="caption">
        <el-input v-model="queryParams.caption" placeholder="Caption模糊搜索" clearable style="width:160px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="发帖时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width:240px"
        />
      </el-form-item>
      <el-form-item label="入库时间">
        <el-date-picker
          v-model="createdDateRange"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width:240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini"
          :disabled="selectedIds.length === 0"
          v-hasPermi="['chigua:tgpost:remove']"
          @click="handleBatchDelete">批量删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-arrow-down" size="mini" @click="expandAll">展开全部</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-arrow-up" size="mini" @click="collapseAll">隐藏全部</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 主表格，默认不展开 -->
    <el-table
      v-loading="loading"
      :data="postList"
      row-key="id"
      :expand-row-keys="expandRowKeys"
      @expand-change="handleExpandChange"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column type="expand">
        <template slot-scope="{ row }">
          <div class="media-expand" v-loading="row._mediaLoading">
            <div v-if="row._mediaLoading" style="padding:12px;color:#999">媒体加载中...</div>
            <div v-else-if="!row._media || row._media.length === 0" style="color:#999;padding:12px">暂无媒体</div>
            <div v-else class="media-list">
              <div v-for="m in row._media" :key="m.id" class="media-item" :class="{ 'is-recommend': m.isRecommend === 1 }">
                <!-- 缩略图（点击查看大图/播放视频） -->
                <div class="media-thumb" @click="previewMedia(m)" style="position:relative">
                  <span v-if="m.isRecommend === 1" class="recommend-badge">
                    <i class="el-icon-star-on" /> 推荐
                  </span>
                  <el-image
                    v-if="m.mediaType === 'photo'"
                    :src="m.localUrl"
                    fit="cover"
                    class="thumb-img"
                  >
                    <div slot="placeholder" class="thumb-error"><i class="el-icon-loading" /></div>
                    <div slot="error" class="thumb-error"><i class="el-icon-picture-outline" /></div>
                  </el-image>
                  <div v-else class="thumb-video">
                    <el-image
                      v-if="m.thumbUrl"
                      :src="m.thumbUrl"
                      fit="cover"
                      class="thumb-img"
                    >
                      <div slot="placeholder" class="thumb-error"><i class="el-icon-loading" /></div>
                      <div slot="error" class="thumb-error"><i class="el-icon-video-camera" /></div>
                    </el-image>
                    <div v-else class="thumb-placeholder"><i class="el-icon-video-camera" /></div>
                    <div class="thumb-play-overlay">
                      <i class="el-icon-video-play" />
                    </div>
                    <span v-if="m.duration" class="thumb-badge">{{ formatDuration(m.duration) }}</span>
                  </div>
                </div>
                <!-- 媒体信息 -->
                <div class="media-info">
                  <div>
                    <el-tag size="mini" :type="m.mediaType === 'photo' ? 'success' : 'warning'">
                      {{ m.mediaType === 'photo' ? '图片' : '视频' }}
                    </el-tag>
                    <span v-if="m.width && m.height" class="media-meta">{{ m.width }}×{{ m.height }}</span>
                    <span v-if="m.fileSize" class="media-meta">{{ formatSize(m.fileSize) }}</span>
                    <span v-if="m.duration" class="media-meta">{{ formatDuration(m.duration) }}</span>
                  </div>
                  <div class="media-path" :title="m.localUrl">{{ m.localUrl }}</div>
                </div>
                <!-- 操作按钮 -->
                <div class="media-actions">
                  <el-tooltip :content="m.isRecommend === 1 ? '取消推荐' : '设为推荐'" placement="top">
                    <el-button size="mini"
                      :type="m.isRecommend === 1 ? 'warning' : 'default'"
                      :icon="m.isRecommend === 1 ? 'el-icon-star-on' : 'el-icon-star-off'"
                      v-hasPermi="['chigua:tgpost:edit']"
                      @click.stop="handleRecommendChange(m)">{{ m.isRecommend === 1 ? '推荐' : '推荐' }}</el-button>
                  </el-tooltip>
                  <el-button size="mini" type="danger" icon="el-icon-delete"
                    v-hasPermi="['chigua:tgpost:edit']"
                    @click.stop="handleDeleteMedia(m, row)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="ID" prop="id" width="70" align="center" />
      <el-table-column label="分类" prop="categoryId" width="120" align="center">
        <template slot-scope="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="源ID" prop="sourceId" width="160" show-overflow-tooltip />
      <el-table-column label="正文" prop="caption" min-width="200" show-overflow-tooltip />
      <el-table-column label="图片数" prop="photoCount" width="65" align="center" />
      <el-table-column label="视频数" prop="videoCount" width="65" align="center" />
      <el-table-column label="浏览" prop="views" width="65" align="center" />
      <el-table-column label="状态" prop="status" width="80" align="center">
        <template slot-scope="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0"
            @change="handleStatusChange(row)" />
        </template>
      </el-table-column>
      <el-table-column label="置顶" prop="isTop" width="80" align="center">
        <template slot-scope="{ row }">
          <el-tooltip :content="row.isTop === 1 ? '已置顶，点击取消' : '点击置顶'" placement="top">
            <span class="top-btn" :class="{ 'is-topped': row.isTop === 1 }"
              @click="handleTopChange(row)">
              <i class="el-icon-top" />
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="发帖时间" prop="postDate" width="175" align="center">
        <template slot-scope="{ row }">
          <el-date-picker
            v-if="row._editingDate"
            v-model="row._dateVal"
            type="datetime"
            size="mini"
            style="width:155px"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm:ss"
            :clearable="false"
            @change="handleDateChange(row)"
            @blur="row._editingDate = false"
          />
          <span v-else class="editable-date" @click="startEditDate(row)">
            {{ formatTime(row.postDate) }}
            <i class="el-icon-edit edit-icon" />
          </span>
        </template>
      </el-table-column>
      <el-table-column label="入库时间" prop="createdAt" width="155" align="center">
        <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button size="mini" type="primary" icon="el-icon-edit"
            v-hasPermi="['chigua:tgpost:edit']"
            @click="handleEdit(row)">修改</el-button>
          <el-button size="mini" type="danger" icon="el-icon-delete"
            v-hasPermi="['chigua:tgpost:remove']"
            @click="handleDeletePost(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total"
      :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 图片预览弹窗 -->
    <el-dialog :visible.sync="imgDialog.visible" title="图片预览"
      width="80%" append-to-body center>
      <div style="text-align:center">
        <el-image :src="imgDialog.src" fit="contain"
          style="max-width:100%;max-height:75vh">
          <div slot="placeholder" style="display:flex;align-items:center;justify-content:center;height:200px;color:#909399;font-size:30px">
            <i class="el-icon-loading" />
          </div>
        </el-image>
      </div>
    </el-dialog>

    <!-- 视频播放弹窗 -->
    <el-dialog :visible.sync="videoDialog.visible" title="视频预览"
      width="900px" append-to-body :close-on-click-modal="false"
      @close="closeVideo">
      <video ref="videoPlayer" controls autoplay playsinline
        style="width:100%;max-height:600px;background:#000;display:block"
        @error="onVideoError" />
      <p v-if="videoError" style="color:#f56c6c;margin-top:8px;font-size:13px">
        视频加载失败，请检查 URL 是否正确
      </p>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog :visible.sync="editDialog.visible" title="修改帖子" width="600px" append-to-body>
      <el-form :model="editForm" ref="editForm" label-width="90px" size="small">
        <el-form-item label="帖子ID">
          <span>{{ editForm.id }}</span>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发帖时间" prop="postDate">
          <el-date-picker
            v-model="editForm.postDate"
            type="datetime"
            placeholder="选择发帖时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">显示</el-radio>
            <el-radio :label="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文" prop="caption">
          <el-input v-model="editForm.caption" type="textarea" :rows="4" placeholder="帖子正文" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="editDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="submitEdit">确 定</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '@/utils/hlsUtils';
import { getCategories } from "@/api/chigua/video";
import { listTgPost, getTgMedia, updateTgPost, delTgPost, delTgMedia, toggleTgPostTop, toggleTgMediaRecommend } from "@/api/chigua/tgpost";
import { parseTime } from "@/utils/ruoyi";
export default {
  name: "TgPost",
  data() {
    return {
      loading: false,
      showSearch: true,
      postList: [],
      total: 0,
      selectedIds: [],
      categoryOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        id: undefined,
        categoryId: undefined,
        status: undefined,
        sourceId: undefined,
        caption: undefined,
      },
      dateRange: [],          // 发帖时间范围 [beginDate, endDate]
      createdDateRange: [],   // 入库时间范围 [beginCreatedAt, endCreatedAt]
      expandRowKeys: [],   // 当前展开的行 id 列表（字符串）
      imgDialog: { visible: false, src: "" },
      videoDialog: { visible: false, src: "" },
      videoError: false,
      hlsInstance: null,
      editDialog: { visible: false, saving: false },
      editForm: { id: null, categoryId: null, postDate: null, status: 1, caption: "" },
    };
  },
  created() {
    this.loadCategories();
    this.getList();
  },
  beforeDestroy() {
    this.destroyHls();
  },
  methods: {
    formatTime(val) {
      if (!val) return "-";
      return parseTime(val, "{y}-{m}-{d} {h}:{i}");
    },
    categoryName(id) {
      if (!id) return "-";
      const c = this.categoryOptions.find(c => c.id === id);
      return c ? c.name : String(id);
    },
    formatDuration(secs) {
      if (!secs) return "";
      const s = Math.floor(Number(secs));
      const m = Math.floor(s / 60);
      return `${String(m).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
    },
    formatSize(bytes) {
      if (!bytes) return "";
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + "KB";
      return (bytes / 1024 / 1024).toFixed(1) + "MB";
    },
    loadCategories() {
      getCategories().then(res => { this.categoryOptions = res.data || []; });
    },
    getList() {
      this.loading = true;
      this.expandRowKeys = [];
      const params = { ...this.queryParams };
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginDate = this.dateRange[0];
        params.endDate   = this.dateRange[1];
      }
      if (this.createdDateRange && this.createdDateRange.length === 2) {
        params.beginCreatedAt = this.createdDateRange[0];
        params.endCreatedAt   = this.createdDateRange[1];
      }
      listTgPost(params).then(res => {
        this.postList = (res.rows || []).map(p => ({
          ...p,
          _media: null,
          _mediaLoading: false,
        }));
        this.total = res.total || 0;
      }).finally(() => { this.loading = false; });
    },
    loadMedia(post) {
      if (post._media !== null || post._mediaLoading) return;
      this.$set(post, "_mediaLoading", true);
      getTgMedia(post.id).then(res => {
        this.$set(post, "_media", res.data || []);
      }).finally(() => {
        this.$set(post, "_mediaLoading", false);
      });
    },
    handleExpandChange(row, expandedRows) {
      this.expandRowKeys = expandedRows.map(r => String(r.id));
      // 展开时懒加载媒体
      const isExpanded = expandedRows.some(r => r.id === row.id);
      if (isExpanded) this.loadMedia(row);
    },
    expandAll() {
      this.expandRowKeys = this.postList.map(p => String(p.id));
      this.postList.forEach(post => this.loadMedia(post));
    },
    collapseAll() {
      this.expandRowKeys = [];
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.$refs.queryForm && this.$refs.queryForm.resetFields();
      this.queryParams = { pageNum: 1, pageSize: 20, id: undefined, categoryId: undefined, status: undefined, sourceId: undefined, caption: undefined };
      this.dateRange = [];
      this.createdDateRange = [];
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selectedIds = rows.map(r => r.id);
    },
    handleStatusChange(row) {
      const text = row.status === 1 ? "显示" : "隐藏";
      this.$modal.confirm(`确认将帖子 #${row.id} 改为"${text}"吗？`).then(() => {
        return updateTgPost({ id: row.id, status: row.status });
      }).then(() => {
        this.$modal.msgSuccess("状态已更新");
      }).catch(() => { row.status = row.status === 1 ? 0 : 1; });
    },
    handleTopChange(row) {
      const newVal = row.isTop === 1 ? 0 : 1;
      const text = newVal === 1 ? "置顶" : "取消置顶";
      toggleTgPostTop(row.id, newVal).then(() => {
        this.$set(row, "isTop", newVal);
        this.$modal.msgSuccess(`帖子 #${row.id} 已${text}`);
      }).catch(() => {});
    },
    handleRecommendChange(media) {
      const newVal = media.isRecommend === 1 ? 0 : 1;
      const text = newVal === 1 ? "设为推荐" : "取消推荐";
      toggleTgMediaRecommend(media.id, newVal).then(() => {
        this.$set(media, "isRecommend", newVal);
        this.$modal.msgSuccess(`媒体 #${media.id} 已${text}`);
      }).catch(() => {});
    },
    handleDeletePost(row) {
      this.$modal.confirm(`确认删除帖子 #${row.id} 及其所有媒体吗？`).then(() => {
        return delTgPost(row.id);
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.getList();
      }).catch(() => {});
    },
    handleBatchDelete() {
      if (!this.selectedIds.length) return;
      this.$modal.confirm(`确认删除选中的 ${this.selectedIds.length} 条帖子（含所有媒体）吗？`).then(() => {
        return delTgPost(this.selectedIds.join(","));
      }).then(() => {
        this.$modal.msgSuccess("批量删除成功");
        this.selectedIds = [];
        this.getList();
      }).catch(() => {});
    },
    handleDeleteMedia(media, row) {
      const typeName = media.mediaType === "photo" ? "图片" : "视频";
      this.$modal.confirm(`确认删除该${typeName}记录（ID: ${media.id}）吗？`).then(() => {
        return delTgMedia(media.id);
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.$set(row, "_media", row._media.filter(m => m.id !== media.id));
      }).catch(() => {});
    },
    previewMedia(m) {
      if (m.mediaType === "photo") {
        this.imgDialog = { visible: true, src: m.localUrl };
      } else {
        this.videoDialog.visible = true;
        this.videoError = false;
        this.$nextTick(() => { this.playVideo(m.localUrl); });
      }
    },
    playVideo(url) {
      this.destroyHls();
      const video = this.$refs.videoPlayer;
      if (!video) return;
      const isM3u8 = url && url.includes(".m3u8");
      if (isM3u8 && Hls.isSupported()) {
        const hls = new Hls({ enableWorker: false, xhrSetup: hlsXhrSetup });
        hls.loadSource(url);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => { video.play().catch(() => {}); });
        this.hlsInstance = hls;
      } else if (isM3u8 && video.canPlayType("application/vnd.apple.mpegurl")) {
        // iOS Safari 原生 HLS：重写 key URI 为绝对后端地址
        if (this._nativeBlobUrl) { try { URL.revokeObjectURL(this._nativeBlobUrl); } catch (_) {} }
        patchNativeHlsM3u8(url).then(({ url: patchedUrl, isBlob }) => {
          this._nativeBlobUrl = isBlob ? patchedUrl : null;
          video.src = patchedUrl;
          video.play().catch(() => {});
        });
      } else {
        video.src = url;
        video.play().catch(() => {});
      }
    },
    closeVideo() {
      this.videoDialog.visible = false;
      this.destroyHls();
      const video = this.$refs.videoPlayer;
      if (video) {
        video.pause();
        video.removeAttribute('src');
        video.load();
      }
    },
    destroyHls() {
      if (this.hlsInstance) {
        this.hlsInstance.destroy();
        this.hlsInstance = null;
      }
      if (this._nativeBlobUrl) {
        try { URL.revokeObjectURL(this._nativeBlobUrl); } catch (_) {}
        this._nativeBlobUrl = null;
      }
    },
    onVideoError(e) {
      // 对话框关闭时 src 被清空会误触发 error，忽略此类噪音
      if (!this.videoDialog.visible) return;
      this.videoError = true;
      const mediaErr = e.target && e.target.error;
      console.error('视频加载失败:', mediaErr ? `code=${mediaErr.code} ${mediaErr.message}` : e);
    },
    // 行内编辑发帖时间
    startEditDate(row) {
      this.$set(row, "_dateVal", row.postDate || null);
      this.$set(row, "_editingDate", true);
    },
    handleDateChange(row) {
      if (!row._dateVal) {
        row._editingDate = false;
        return;
      }
      updateTgPost({ id: row.id, postDate: row._dateVal }).then(() => {
        row.postDate = row._dateVal;
        row._editingDate = false;
        this.$modal.msgSuccess("发帖时间已更新");
      }).catch(() => {
        row._editingDate = false;
      });
    },
    // 打开编辑弹窗
    handleEdit(row) {
      this.editForm = {
        id: row.id,
        categoryId: row.categoryId,
        postDate: row.postDate || null,
        status: row.status,
        caption: row.caption || "",
      };
      this.editDialog.visible = true;
    },
    // 提交编辑
    submitEdit() {
      this.editDialog.saving = true;
      updateTgPost({ ...this.editForm }).then(() => {
        this.$modal.msgSuccess("修改成功");
        this.editDialog.visible = false;
        this.getList();
      }).finally(() => {
        this.editDialog.saving = false;
      });
    },
  },
};
</script>

<style scoped>
.media-expand {
  padding: 10px 16px 14px 56px;
  background: #f9f9f9;
}
.media-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.media-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 10px 12px;
  width: calc(50% - 5px);
  box-sizing: border-box;
  min-width: 420px;
}
.media-thumb {
  flex-shrink: 0;
  width: 160px;
  height: 120px;
  overflow: hidden;
  border-radius: 4px;
  cursor: pointer;
  background: #111;
  position: relative;
}
.thumb-img {
  width: 100%;
  height: 100%;
  display: block;
}
.thumb-video {
  width: 100%;
  height: 100%;
  position: relative;
}
.thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #222;
  font-size: 24px;
  color: #aaa;
}
.thumb-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  font-size: 20px;
  color: #bbb;
}
.thumb-play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  font-size: 28px;
  color: #fff;
}
.thumb-badge {
  position: absolute;
  bottom: 3px;
  right: 4px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 11px;
  padding: 1px 5px;
  border-radius: 10px;
}
.media-info {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  color: #555;
}
.media-meta {
  margin-left: 8px;
  color: #888;
  font-size: 12px;
}
.media-path {
  margin-top: 5px;
  color: #aaa;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.media-actions {
  flex-shrink: 0;
}
.editable-date {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 4px;
  border-radius: 3px;
  transition: background 0.2s;
}
.editable-date:hover {
  background: #ecf5ff;
  color: #409eff;
}
.edit-icon {
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}
.editable-date:hover .edit-icon {
  opacity: 1;
}
/* 置顶按钮 */
.top-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  color: #c0c4cc;
  background: #f4f4f5;
  transition: all 0.2s;
}
.top-btn:hover {
  color: #e6a23c;
  background: #fdf6ec;
}
.top-btn.is-topped {
  color: #fff;
  background: #e6a23c;
}
/* 媒体推荐角标 */
.recommend-badge {
  position: absolute;
  top: 3px;
  left: 3px;
  z-index: 2;
  background: linear-gradient(135deg, #f7ba2a, #ea5455);
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 8px;
  line-height: 1.6;
  pointer-events: none;
}
/* 推荐状态媒体边框高亮 */
.media-item.is-recommend {
  border-color: #f7ba2a;
  box-shadow: 0 0 0 1px #f7ba2a22;
}
</style>
