<template>
  <div class="rich-text-video-selector">
    <!-- 触发按钮 -->
    <el-button 
      v-if="showButton"
      type="primary" 
      icon="el-icon-video-camera" 
      @click="showDialog = true"
    >
      插入视频
    </el-button>

    <!-- 视频选择对话框 -->
    <el-dialog
      title="选择视频"
      :visible.sync="showDialog"
      width="80%"
      :before-close="handleClose"
    >
      <!-- 搜索表单 -->
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
        <el-form-item label="视频文件" prop="orgfile">
          <el-input
            v-model="queryParams.orgfile"
            placeholder="请输入视频文件名"
            clearable
            @keyup.enter.native="getVideoList"
          />
        </el-form-item>
        <el-form-item label="分辨率" prop="resolution">
          <el-input
            v-model="queryParams.resolution"
            placeholder="请输入分辨率"
            clearable
            @keyup.enter.native="getVideoList"
          />
        </el-form-item>
        <el-form-item label="显示范围" prop="showUsed">
          <el-select v-model="queryParams.showUsed" placeholder="请选择显示范围" clearable style="width: 150px;">
            <el-option
              label="仅未使用"
              :value="false"
            />
            <el-option
              label="显示所有"
              :value="true"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="getVideoList">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 视频列表 -->
      <el-table v-loading="loading" :data="videoList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="视频文件" prop="orgfile" :show-overflow-tooltip="true" />
        <el-table-column label="分辨率" prop="resolution" width="120" />
        <el-table-column label="时长" prop="duration" width="100">
          <template slot-scope="scope">
            {{ formatDuration(scope.row.duration) }}
          </template>
        </el-table-column>
        <el-table-column label="质量" prop="quality" width="80">
          <template slot-scope="scope">
            <el-tag :type="getQualityTagType(scope.row.quality)">
              {{ formatQuality(scope.row.quality) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" prop="fileSize" width="100">
          <template slot-scope="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="使用状态" prop="isUsed" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isUsed === 1 ? 'warning' : 'success'" size="mini">
              {{ scope.row.isUsed === 1 ? '已使用' : '未使用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="180">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createdAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预览" width="100" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-video-play"
              @click="previewVideo(scope.row)"
            >
              预览
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getVideoList"
      />

      <!-- 对话框按钮 -->
      <div slot="footer" class="dialog-footer">
        <el-button @click="showDialog = false">取 消</el-button>
        <el-button type="primary" @click="insertSelectedVideos" :disabled="selectedVideos.length === 0">
          插入选中视频（{{ selectedVideos.length }}）
        </el-button>
      </div>
    </el-dialog>

    <!-- 视频预览对话框 -->
    <el-dialog
      title="视频预览"
      :visible.sync="showPreview"
      width="60%"
      :before-close="handlePreviewClose"
    >
      <div v-if="previewVideoData" class="video-preview">
        <div v-if="previewLoading" class="loading-container" style="text-align: center; padding: 50px;">
          <i class="el-icon-loading" style="font-size: 30px; color: #409EFF;"></i>
          <p style="margin-top: 10px; color: #666;">正在加载视频预览...</p>
        </div>
        <div v-else-if="previewVideoUrl" class="video-container">
          <video
            ref="videoPlayer"
            :poster="previewPosterUrl"
            controls
            width="100%"
            style="max-height: 400px;"
            @error="handleVideoError"
          >
            您的浏览器不支持视频播放。
          </video>
          <div v-if="videoError" class="video-error" style="text-align: center; padding: 20px; color: #f56c6c;">
            <i class="el-icon-warning" style="font-size: 20px;"></i>
            <p>视频加载失败：{{ videoError }}</p>
            <p style="font-size: 12px; color: #999; margin-top: 10px;">URL: {{ previewVideoUrl }}</p>
          </div>
        </div>
        <div v-else-if="!previewLoading" style="text-align: center; padding: 50px; color: #999;">
          视频加载失败
        </div>
        <div class="video-info" style="margin-top: 15px;">
          <p><strong>文件名：</strong>{{ previewVideoData.orgfile }}</p>
          <p><strong>分辨率：</strong>{{ previewVideoData.resolution }}</p>
          <p><strong>时长：</strong>{{ formatDuration(previewVideoData.duration) }}</p>
          <p><strong>质量：</strong>{{ formatQuality(previewVideoData.quality) }}</p>
          <p v-if="previewVideoUrl"><strong>播放URL：</strong>{{ previewVideoUrl }}</p>
          <p v-if="hlsSupported !== null"><strong>HLS支持：</strong>{{ hlsSupported ? '是' : '否' }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listRichTextVideos, getVideoUrl, getVideoPreview, getResourceUrl } from "@/api/chigua/transcode";
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '@/utils/hlsUtils';

export default {
  name: "RichTextVideoSelector",
  props: {
    // 是否显示触发按钮（用于Editor集成时设为false）
    showButton: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      // 显示控制
      showDialog: false,
      showPreview: false,
      loading: false,
      previewLoading: false,
      
      // 数据
      videoList: [],
      selectedVideos: [],
      previewVideoData: null,
      previewVideoUrl: null,
      previewPosterUrl: null,
      total: 0,
      
      // 视频播放相关
      hls: null,
      hlsSupported: null,
      videoError: null,
      
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orgfile: null,
        resolution: null,
        showUsed: false  // 默认只显示未使用的视频
      }
    };
  },
  created() {
    this.getVideoList();
    this.hlsSupported = Hls.isSupported();
  },
  beforeDestroy() {
    this.destroyHls();
  },
  methods: {
    /** 查询视频列表 */
    getVideoList() {
      this.loading = true;
      console.log('🔍 查询参数:', this.queryParams);
      
      listRichTextVideos(this.queryParams).then(response => {
        this.videoList = response.rows;
        this.total = response.total;
        
        console.log('🔍 查询结果:', {
          total: this.total,
          count: this.videoList.length,
          showUsed: this.queryParams.showUsed,
          firstVideo: this.videoList.length > 0 ? {
            transcodeId: this.videoList[0].transcodeId,
            orgfile: this.videoList[0].orgfile,
            isUsed: this.videoList[0].isUsed
          } : null
        });
        
        this.loading = false;
      }).catch(error => {
        console.error('❌ 查询失败:', error);
        this.loading = false;
      });
    },
    
    /** 重置查询 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.pageNum = 1;
      this.queryParams.showUsed = false; // 重置为默认值：只显示未使用的视频
      this.getVideoList();
    },
    
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.selectedVideos = selection;
    },
    
    /** 预览视频 */
    async previewVideo(row) {
      try {
        this.previewVideoData = row;
        this.previewVideoUrl = null;
        this.previewPosterUrl = null;
        this.previewLoading = true;
        this.videoError = null;
        this.showPreview = true;
        
        // 销毁之前的HLS实例
        this.destroyHls();
        
        // 调用后端API获取带签名的动态URL
        const response = await getVideoUrl(row.id); // 使用id而不是transcodeId
        
        // 适配新的API响应格式
        const urlData = response.data;
        console.log('预览功能 - 获取到的URL数据：', urlData);
        
        this.previewVideoUrl = urlData.preferredPlayUrl || urlData.m3u8Url || urlData.videoUrl;
        console.log('预览功能 - 最终选择的URL：', this.previewVideoUrl);
        console.log('预览功能 - 是否为M3U8：', this.previewVideoUrl && this.previewVideoUrl.includes('.m3u8'));
        
        // 处理封面图URL，确保有签名
        if (urlData.coverUrl) {
          // 检查coverUrl是否已经有签名参数
          if (urlData.coverUrl.includes('signature=') && urlData.coverUrl.includes('expires=')) {
            console.log('预览功能 - 封面图URL已有签名，直接使用');
            this.previewPosterUrl = urlData.coverUrl;
          } else {
            console.log('预览功能 - 封面图URL无签名，尝试生成签名URL');
            try {
              // 按照pornhub项目的方式，提取资源键名
              const resourceKey = this.extractResourceKey(urlData.coverUrl);
              console.log('预览功能 - 提取的资源键名:', resourceKey);
              
              // 明确指定资源类型为cover，参考pornhub项目的做法
              const resourceResponse = await getResourceUrl(resourceKey, 'cover');
              console.log('预览功能 - 资源签名API响应:', resourceResponse);
              
              // 处理API响应，提取签名URL
              if (resourceResponse && resourceResponse.data) {
                // 支持多种可能的响应字段名
                this.previewPosterUrl = resourceResponse.data.signedUrl || 
                                       resourceResponse.data.url || 
                                       resourceResponse.data.signed_url ||
                                       urlData.coverUrl;
                console.log('预览功能 - 封面图签名URL生成成功:', this.previewPosterUrl);
              } else {
                console.warn('预览功能 - API响应格式不正确，使用原始URL');
                this.previewPosterUrl = urlData.coverUrl;
              }
            } catch (error) {
              console.error('预览功能 - 生成封面图签名URL失败:', error);
              // 如果签名失败，仍然使用原始URL
              this.previewPosterUrl = urlData.coverUrl;
            }
          }
        }
        
        this.previewLoading = false;
        
        // 等待DOM更新
        this.$nextTick(() => {
          this.initializeVideoPlayer();
        });
        
      } catch (error) {
        this.previewLoading = false;
        this.videoError = error.message;
        this.$modal.msgError("获取视频预览失败：" + error.message);
      }
    },
    
    /** 插入选中的视频 */
    async insertSelectedVideos() {
      if (this.selectedVideos.length === 0) {
        this.$modal.msgWarning("请选择要插入的视频");
        return;
      }

      try {
        console.log('开始插入视频，选中的视频：', this.selectedVideos);
        
        // 使用与预览相同的API获取带签名的URL
        const urlPromises = this.selectedVideos.map(video => {
          console.log(`为视频 ${video.id} 获取URL`);
          return getVideoUrl(video.id); // 使用与预览相同的API
        });
        
        const responses = await Promise.all(urlPromises);
        console.log('插入功能 - 获取到的API响应：', responses);
        
        // 在前端生成HTML内容
        const htmlContents = await Promise.all(this.selectedVideos.map(async (video, index) => {
          const urlData = responses[index].data;
          console.log(`插入功能 - 视频 ${video.id} 的URL数据：`, urlData);
          console.log(`插入功能 - preferredPlayUrl：`, urlData.preferredPlayUrl);
          console.log(`插入功能 - m3u8Url：`, urlData.m3u8Url);
          console.log(`插入功能 - videoUrl：`, urlData.videoUrl);
          
          return this.generateVideoHtml(video, urlData);
        }));
        
        console.log('插入功能 - 生成的HTML内容：', htmlContents);
        
        // 额外检查：模拟预览逻辑来验证URL选择
        this.selectedVideos.forEach((video, index) => {
          const urlData = responses[index].data;
          const previewUrl = urlData.preferredPlayUrl || urlData.m3u8Url || urlData.videoUrl;
          console.log(`对比测试 - 视频${video.id}：`);
          console.log(`  预览逻辑会选择的URL：${previewUrl}`);
          console.log(`  是否为M3U8：${previewUrl && previewUrl.includes('.m3u8')}`);
          console.log(`  preferredPlayUrl：${urlData.preferredPlayUrl}`);
          console.log(`  原始m3u8Url：${urlData.m3u8Url}`);
          console.log(`  原始videoUrl：${urlData.videoUrl}`);
        });
        
        // 触发插入事件，传递HTML内容给父组件
        this.$emit('insert-videos', htmlContents);
        
        // 关闭对话框
        this.showDialog = false;
        this.selectedVideos = [];
        this.$modal.msgSuccess("视频插入成功");
        
      } catch (error) {
        console.error('插入视频失败：', error);
        this.$modal.msgError("生成视频HTML失败：" + error.message);
      }
    },

    /** 在前端生成视频HTML */
    async generateVideoHtml(video, urlData) {
      console.log('生成视频HTML - 原始URL数据：', urlData);
      console.log('M3U8 URL:', urlData.m3u8Url);
      console.log('Video URL:', urlData.videoUrl);
      console.log('Preferred URL:', urlData.preferredPlayUrl);
      console.log('Cover URL:', urlData.coverUrl);
      
      // 处理封面图URL，确保有签名
      let posterUrl = '';
      if (urlData.coverUrl) {
        // 检查coverUrl是否已经有签名参数
        if (urlData.coverUrl.includes('signature=') && urlData.coverUrl.includes('expires=')) {
          console.log('封面图URL已有签名，直接使用');
          posterUrl = urlData.coverUrl;
        } else {
          console.log('封面图URL无签名，尝试生成签名URL');
          try {
            // 按照pornhub项目的方式，提取资源键名
            const resourceKey = this.extractResourceKey(urlData.coverUrl);
            console.log('提取的资源键名:', resourceKey);
            
            // 明确指定资源类型为cover，参考pornhub项目的做法
            const resourceResponse = await getResourceUrl(resourceKey, 'cover');
            console.log('资源签名API响应:', resourceResponse);
            
            // 处理API响应，提取签名URL
            if (resourceResponse && resourceResponse.data) {
              // 支持多种可能的响应字段名
              posterUrl = resourceResponse.data.signedUrl || 
                         resourceResponse.data.url || 
                         resourceResponse.data.signed_url ||
                         urlData.coverUrl;
              console.log('封面图签名URL生成成功:', posterUrl);
            } else {
              console.warn('API响应格式不正确，使用原始URL');
              posterUrl = urlData.coverUrl;
            }
          } catch (error) {
            console.error('生成封面图签名URL失败:', error);
            // 如果签名失败，仍然使用原始URL
            posterUrl = urlData.coverUrl;
          }
        }
      }
      
      // 确保M3U8优先，与预览逻辑完全一致
      const primaryUrl = urlData.preferredPlayUrl || urlData.m3u8Url || urlData.videoUrl;
      const hasM3u8 = urlData.m3u8Url && urlData.m3u8Url.trim() !== '';
      const hasMp4 = urlData.videoUrl && urlData.videoUrl.trim() !== '';
      
      console.log('URL优先级检查：', {
        primaryUrl,
        hasM3u8,
        hasMp4,
        willUseM3u8First: hasM3u8,
        finalPosterUrl: posterUrl
      });
      
      // 只使用M3U8源，不添加MP4备用
      let sourceTags = '';
      if (hasM3u8) {
        sourceTags += `<source src="${urlData.m3u8Url}" type="application/x-mpegURL">`;
        console.log('添加M3U8 source标签');
      } else if (hasMp4) {
        // 只有在没有M3U8时才使用MP4作为最后的备选
        sourceTags += `<source src="${urlData.videoUrl}" type="video/mp4">`;
        console.log('没有M3U8，使用MP4 source标签');
      }
      
      // 生成与后端兼容的HTML结构，但直接使用签名URL
      const html = `
<div class="rich-text-video" data-video-id="${video.id}" data-transcode-id="${video.transcodeId || ''}" data-orgfile="${this.escapeHtml(video.orgfile)}" data-resolution="${this.escapeHtml(video.resolution)}" data-duration="${video.duration || 0}">
  <video controls width="100%" style="max-width: 600px;" ${posterUrl ? `poster="${posterUrl}"` : ''}>
    ${sourceTags}
    您的浏览器不支持视频播放。
  </video>
</div>
      `.trim();
      
      console.log('生成的HTML：', html);
      console.log('包含M3U8：', html.includes('application/x-mpegURL'));
      console.log('包含MP4：', html.includes('video/mp4'));
      console.log('包含封面图：', html.includes('poster='));
      
      // 验证生成的HTML结构
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = html;
      const videoElement = tempDiv.querySelector('video');
      const richTextVideoDiv = tempDiv.querySelector('.rich-text-video');
      
      console.log('HTML验证结果：', {
        hasRichTextVideoDiv: !!richTextVideoDiv,
        hasVideoElement: !!videoElement,
        videoHasControls: videoElement ? videoElement.hasAttribute('controls') : false,
        videoPoster: videoElement ? videoElement.getAttribute('poster') : null,
        videoSources: videoElement ? Array.from(videoElement.querySelectorAll('source')).map(s => ({
          src: s.src,
          type: s.type
        })) : [],
        divAttributes: richTextVideoDiv ? {
          'data-video-id': richTextVideoDiv.getAttribute('data-video-id'),
          'data-transcode-id': richTextVideoDiv.getAttribute('data-transcode-id'),
          'data-orgfile': richTextVideoDiv.getAttribute('data-orgfile')
        } : {}
      });
      
      return html;
    },

    /** HTML转义 */
    escapeHtml(text) {
      if (!text) return '';
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    },
    
    /** 提取资源键名 - 参考pornhub项目的实现 */
    extractResourceKey(url) {
      if (!url) return '';
      
      console.log('开始提取资源键名, 原始URL:', url);
      
      let resourceKey = '';
      
      // 如果URL包含域名，提取路径部分
      const fullUrlMatch = url.match(/^https?:\/\/[^\/]+\/(.+)$/);
      if (fullUrlMatch) {
        resourceKey = fullUrlMatch[1]; // 返回 files/20250629/byMe35wK/byMe35wK/1.jpg 或 20250629/byMe35wK/byMe35wK/1.jpg
        console.log('从完整URL提取初始路径:', resourceKey);
      } else {
        // 如果是以/开头的相对路径，移除开头的斜杠
        const relativeUrlMatch = url.match(/^\/(.+)$/);
        if (relativeUrlMatch) {
          resourceKey = relativeUrlMatch[1]; // 返回 files/20250629/byMe35wK/byMe35wK/1.jpg 或 20250629/byMe35wK/byMe35wK/1.jpg
          console.log('从相对路径提取初始路径:', resourceKey);
        } else {
          // 其他格式，直接返回（假设已经是资源键名），但确保没有前导斜杠
          resourceKey = url.replace(/^\/+/, '');
          console.log('使用原始值作为初始路径:', resourceKey);
        }
      }
      
      // 移除可能存在的 files/ 前缀，避免重复
      if (resourceKey.startsWith('files/')) {
        resourceKey = resourceKey.substring(6); // 移除 "files/" 前缀
        console.log('移除files/前缀后的资源键名:', resourceKey);
      }
      
      console.log('最终资源键名:', resourceKey);
      return resourceKey;
    },
    
    /** 初始化视频播放器 */
    initializeVideoPlayer() {
      const video = this.$refs.videoPlayer;
      if (!video || !this.previewVideoUrl) return;
      
      console.log('Initializing video player for URL:', this.previewVideoUrl);
      
      // 判断是否为M3U8格式
      if (this.previewVideoUrl.includes('.m3u8')) {
        this.initializeHlsPlayer(video);
      } else {
        // 普通视频格式，直接设置src
        video.src = this.previewVideoUrl;
      }
    },
    
    /** 初始化HLS播放器 */
    initializeHlsPlayer(video) {
      if (Hls.isSupported()) {
        console.log('HLS is supported, initializing HLS player');
        this.hls = new Hls({
          debug: false,
          enableWorker: false,
          xhrSetup: hlsXhrSetup,
        });
        
        this.hls.loadSource(this.previewVideoUrl);
        this.hls.attachMedia(video);
        
        this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
          console.log('HLS manifest parsed, video is ready to play');
        });
        
        this.hls.on(Hls.Events.ERROR, (event, data) => {
          console.error('HLS error:', data);
          if (data.fatal) {
            switch (data.type) {
              case Hls.ErrorTypes.NETWORK_ERROR:
                this.videoError = 'HLS网络错误，尝试恢复...';
                this.hls.startLoad();
                break;
              case Hls.ErrorTypes.MEDIA_ERROR:
                this.videoError = 'HLS媒体错误，尝试恢复...';
                this.hls.recoverMediaError();
                break;
              default:
                this.videoError = `HLS播放器错误: ${data.details}`;
                this.destroyHls();
                break;
            }
          }
        });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // iOS Safari 原生 HLS：重写 key URI 为绝对后端地址
        console.log('Using native HLS support');
        const prevBlobUrl = this._nativeBlobUrl;
        patchNativeHlsM3u8(this.previewVideoUrl).then(({ url: patchedUrl, isBlob }) => {
          if (prevBlobUrl) { try { URL.revokeObjectURL(prevBlobUrl); } catch (_) {} }
          this._nativeBlobUrl = isBlob ? patchedUrl : null;
          video.src = patchedUrl;
        });
      } else {
        this.videoError = '浏览器不支持HLS播放，请使用支持的浏览器或URL格式';
      }
    },
    
    /** 销毁HLS实例 */
    destroyHls() {
      if (this.hls) {
        console.log('Destroying HLS instance');
        this.hls.destroy();
        this.hls = null;
      }
    },
    
    /** 处理视频错误 */
    handleVideoError(event) {
      console.error('Video error:', event);
      const video = event.target;
      const error = video.error;
      
      let errorMessage = '未知错误';
      if (error) {
        switch (error.code) {
          case error.MEDIA_ERR_ABORTED:
            errorMessage = '播放被中止';
            break;
          case error.MEDIA_ERR_NETWORK:
            errorMessage = '网络错误';
            break;
          case error.MEDIA_ERR_DECODE:
            errorMessage = '解码错误';
            break;
          case error.MEDIA_ERR_SRC_NOT_SUPPORTED:
            errorMessage = '不支持的视频格式';
            break;
        }
      }
      
      this.videoError = errorMessage;
    },
    
    /** 格式化时长 */
    formatDuration(duration) {
      if (!duration || duration <= 0) return "未知";
      const minutes = Math.floor(duration / 60);
      const seconds = duration % 60;
      return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    },
    
    /** 格式化文件大小 */
    formatFileSize(size) {
      if (!size || size <= 0) return "未知";
      const units = ['B', 'KB', 'MB', 'GB'];
      let index = 0;
      let fileSize = size;
      
      while (fileSize >= 1024 && index < units.length - 1) {
        fileSize /= 1024;
        index++;
      }
      
      return `${fileSize.toFixed(1)} ${units[index]}`;
    },
    
    /** 格式化质量 */
    formatQuality(quality) {
      const qualityMap = {
        'sd': '标清',
        'hd': '高清',
        'fhd': '全高清',
        '4k': '超高清',
        '8k': '8K'
      };
      return qualityMap[quality] || quality;
    },
    
    /** 获取质量标签类型 */
    getQualityTagType(quality) {
      const typeMap = {
        'sd': 'info',
        'hd': 'success',
        'fhd': 'warning',
        '4k': 'danger',
        '8k': 'danger'
      };
      return typeMap[quality] || 'info';
    },
    
    /** 关闭对话框 */
    handleClose(done) {
      this.selectedVideos = [];
      done();
    },
    
    /** 关闭预览对话框 */
    handlePreviewClose() {
      this.destroyHls();
      this.previewVideoData = null;
      this.previewVideoUrl = null;
      this.previewPosterUrl = null;
      this.previewLoading = false;
      this.videoError = null;
      this.showPreview = false;
    }
  }
};
</script>

<style scoped>
.rich-text-video-selector {
  display: inline-block;
}

.video-preview {
  text-align: center;
}

.video-container {
  position: relative;
}

.video-error {
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  margin-top: 10px;
}

.video-info {
  text-align: left;
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
}

.video-info p {
  margin: 8px 0;
  line-height: 1.5;
  word-break: break-all;
}
</style> 