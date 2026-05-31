<template>
  <div class="app-container">
    <!-- 面包屑导航 -->
    <el-page-header @back="goBack" :content="'用户: ' + (username || '未知用户') + ' 的视频列表'">
    </el-page-header>

    <el-divider></el-divider>

    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="视频标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入视频标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="GIF ID" prop="gifId">
        <el-input
          v-model="queryParams.gifId"
          placeholder="请输入 gif_id（精准匹配）"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="视频状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
        >批量删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 视频列表 -->
    <el-table v-loading="loading" :data="videoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="视频封面" align="center" width="150">
        <template slot-scope="scope">
          <div class="thumbnail-wrapper" @click="handlePlayVideo(scope.row)">
            <el-image 
              class="video-thumbnail"
              :src="scope.row.posterUrl || scope.row.thumbnailUrl" 
              fit="cover"
            >
              <div slot="placeholder" class="image-slot">
                <i class="el-icon-loading"></i>
              </div>
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline"></i>
              </div>
            </el-image>
            <div class="play-overlay">
              <i class="el-icon-video-play"></i>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" min-width="250" />
      <el-table-column label="时长" align="center" prop="duration" width="80">
        <template slot-scope="scope">
          {{ formatDuration(scope.row.duration) }}
        </template>
      </el-table-column>
      <el-table-column label="尺寸" align="center" width="100">
        <template slot-scope="scope">
          {{ scope.row.width }} × {{ scope.row.height }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createDate" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createDate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="180">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-user"
            @click="handleSyncPosterToUser(scope.row)"
          >同步头像</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 视频播放对话框 -->
    <el-dialog 
      title="视频预览" 
      :visible.sync="videoDialogVisible" 
      width="900px"
      :close-on-click-modal="false"
      @opened="handleDialogOpened"
      @close="handleCloseVideo"
    >
      <div v-if="currentVideo" class="video-player-container">
        <video 
          ref="videoPlayer"
          controls
          autoplay
          preload="auto"
          style="width: 100%; max-height: 600px; background: #000;"
          @error="handleVideoError"
        ></video>
      </div>
      <div v-if="currentVideo" class="video-info">
        <h3>{{ currentVideo.title || currentVideo.videoId }}</h3>
        <p>
          时长: {{ formatDuration(currentVideo.duration) }} | 
          尺寸: {{ currentVideo.width }} × {{ currentVideo.height }}
        </p>
        <p v-if="videoError" class="video-error">
          <el-tag type="danger">视频加载失败，请检查视频URL是否正确</el-tag>
        </p>
        <p class="video-url">
          <el-tag size="mini" type="info">高清: {{ currentVideo.hdUrl || '无' }}</el-tag>
          <el-tag size="mini" type="info">标清: {{ currentVideo.sdUrl || '无' }}</el-tag>
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '@/utils/hlsUtils';
import { listRedGifsVideoByUserId, delRedGifsVideo, delRedGifsVideos, updateRedGifsVideoStatus, syncPosterToUserAvatar } from "@/api/chigua/redgifsVideo";
export default {
  name: "RedGifsVideo",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 视频表格数据
      videoList: [],
      // 用户ID
      userId: null,
      // 用户名
      username: null,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        gifId: null,
        status: null
      },
      // 视频播放
      videoDialogVisible: false,
      currentVideo: null,
      videoError: false,
      hlsInstance: null
    };
  },
  created() {
    this.userId = this.$route.query.userId;
    this.username = this.$route.query.username;
    if (this.userId) {
      this.getList();
    } else {
      this.$modal.msgError("缺少用户ID参数");
      this.goBack();
    }
  },
  methods: {
    /** 查询视频列表 */
    getList() {
      this.loading = true;
      listRedGifsVideoByUserId(this.userId, this.queryParams).then(response => {
        this.videoList = response.rows;
        this.total = response.total;
        this.loading = false;
        
        // 调试信息：查看视频数据
        if (this.videoList.length > 0) {
          console.log('视频列表数据示例:', this.videoList[0]);
          console.log('视频封面URL:', this.videoList[0].posterUrl);
          console.log('视频缩略图URL:', this.videoList[0].thumbnailUrl);
          console.log('高清视频URL:', this.videoList[0].hdUrl);
          console.log('标清视频URL:', this.videoList[0].sdUrl);
        }
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.multiple = !selection.length;
    },
    /** 状态修改 */
    handleStatusChange(row) {
      let text = row.status === 1 ? "启用" : "禁用";
      this.$modal.confirm('确认要"' + text + '"该视频吗？').then(() => {
        return updateRedGifsVideoStatus(row.id, row.status);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.status = row.status === 0 ? 1 : 0;
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const videoIds = row.id ? [row.id] : this.ids;
      
      this.$modal.confirm('是否确认删除选中的视频数据项？').then(() => {
        return row.id ? delRedGifsVideo(row.id) : delRedGifsVideos(videoIds.join(','));
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 播放视频 */
    handlePlayVideo(video) {
      this.currentVideo = video;
      this.videoError = false;
      this.videoDialogVisible = true;
    },
    /** Dialog 完全打开后初始化播放器（确保 video DOM 已挂载） */
    handleDialogOpened() {
      const url = this.currentVideo?.hdUrl || this.currentVideo?.sdUrl;
      if (!url) return;
      const video = this.$refs.videoPlayer;
      if (!video) return;
      this.destroyHls();

      if (url.includes('.m3u8')) {
        if (Hls.isSupported()) {
          const hls = new Hls({ enableWorker: false, xhrSetup: hlsXhrSetup });
          hls.loadSource(url);
          hls.attachMedia(video);
          hls.on(Hls.Events.MANIFEST_PARSED, () => { video.play().catch(() => {}); });
          hls.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal) { this.videoError = true; hls.destroy(); this.hlsInstance = null; }
          });
          this.hlsInstance = hls;
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
          // iOS Safari 原生 HLS
          if (this._nativeBlobUrl) { try { URL.revokeObjectURL(this._nativeBlobUrl); } catch (_) {} }
          patchNativeHlsM3u8(url).then(({ url: patchedUrl, isBlob }) => {
            this._nativeBlobUrl = isBlob ? patchedUrl : null;
            video.src = patchedUrl;
            video.play().catch(() => {});
          });
        } else {
          this.videoError = true;
        }
      } else {
        video.src = url;
        video.play().catch(() => {});
      }
    },
    /** 销毁 HLS 实例 */
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
    /** 视频加载错误 */
    handleVideoError(e) {
      // 对话框关闭时 video.src 被清空会误触发 error 事件，忽略此类噪音
      if (!this.videoDialogVisible || !this.currentVideo) return;
      this.videoError = true;
      const mediaErr = e.target && e.target.error;
      console.error('视频加载失败:', mediaErr ? `code=${mediaErr.code} ${mediaErr.message}` : e);
    },
    /** 关闭视频 */
    handleCloseVideo() {
      this.videoDialogVisible = false;
      this.currentVideo = null;
      this.videoError = false;
      this.destroyHls();
      const video = this.$refs.videoPlayer;
      if (video) {
        video.pause();
        video.removeAttribute('src');
        video.load(); // 清空缓冲区，避免触发 error 事件
      }
    },
    /** 将视频封面同步为用户头像 */
    handleSyncPosterToUser(row) {
      this.$confirm('确定要将该视频封面设置为用户头像吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        syncPosterToUserAvatar(row.id).then(res => {
          this.$modal.msgSuccess(res.msg || '头像已更新');
        }).catch(() => {
          this.$modal.msgError('操作失败');
        });
      }).catch(() => {});
    },
    /** 返回上一页 */
    goBack() {
      this.$router.back();
    },
    /** 格式化时长 */
    formatDuration(seconds) {
      if (!seconds) return '0:00';
      const mins = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60);
      return `${mins}:${secs.toString().padStart(2, '0')}`;
    },
    /** 格式化数字 */
    formatNumber(num) {
      if (!num) return '0';
      if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
      }
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
      }
      return num.toString();
    }
  }
};
</script>

<style scoped lang="scss">
.thumbnail-wrapper {
  position: relative;
  width: 120px;
  height: 80px;
  cursor: pointer;
  margin: 0 auto;
  
  &:hover .play-overlay {
    opacity: 1;
  }
}

.video-thumbnail {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  display: block;
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;

  i {
    color: #fff;
    font-size: 24px;
  }
}

.image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  font-size: 30px;
  color: #909399;
  background: #f5f7fa;
}

.video-player-container {
  background: #000;
  border-radius: 4px;
  overflow: hidden;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-info {
  margin-top: 16px;
  
  h3 {
    font-size: 16px;
    margin-bottom: 8px;
    color: #303133;
  }
  
  p {
    font-size: 14px;
    color: #606266;
    margin-bottom: 8px;
  }
  
  .video-error {
    color: #f56c6c;
  }
  
  .video-url {
    margin-top: 12px;
    
    .el-tag {
      margin-right: 8px;
      max-width: 400px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>
