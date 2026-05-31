<template>
  <div>
    <el-upload
      :action="uploadUrl"
      :http-request="customUpload"
      :before-upload="handleBeforeUpload"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      name="file"
      :show-file-list="false"
      :headers="headers"
      style="display: none"
      ref="upload"
      v-if="this.type == 'url'"
    >
    </el-upload>
    
    <!-- 批量图片上传Dialog -->
    <el-dialog
      title="批量上传图片"
      :visible.sync="batchImageUploadVisible"
      width="600px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="batch-upload-container">
        <el-tabs v-model="batchUploadTab" type="border-card">
          <!-- 文件选择上传 -->
          <el-tab-pane label="选择文件" name="file">
            <div class="upload-area">
              <el-upload
                :action="uploadUrl"
                :http-request="customUpload"
                :before-upload="handleBatchBeforeUpload"
                :on-success="handleBatchUploadSuccess"
                :on-error="handleBatchUploadError"
                :on-progress="handleBatchUploadProgress"
                name="file"
                multiple
                :file-list="batchFileList"
                :auto-upload="true"
                drag
                accept="image/*"
                :headers="headers"
              >
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">将图片拖到此处，或<em>点击选择图片</em></div>
                <div class="el-upload__tip" slot="tip">
                  支持 jpg、jpeg、png、gif、bmp、webp 格式，单个文件不超过 {{ fileSize }}MB，可同时选择多张图片
                </div>
              </el-upload>
              
              <!-- 图片预览和排序区域 -->
              <div v-if="imagesSortList.length > 0" class="images-sort-area">
                <h4 class="sort-title">
                  <i class="el-icon-picture-outline"></i>
                  图片排序预览 ({{ imagesSortList.length }}张)
                  <span class="sort-tip">拖拽调整顺序</span>
                </h4>
                <draggable 
                  v-model="imagesSortList" 
                  class="images-sort-list"
                  :options="{
                    animation: 200,
                    group: 'images',
                    ghostClass: 'ghost',
                    dragClass: 'drag'
                  }"
                  @change="onImagesSortChanged"
                >
                  <transition-group name="flip-list" tag="div" class="sort-grid">
                    <div 
                      v-for="(image, index) in imagesSortList" 
                      :key="image.id"
                      class="sort-item"
                      :class="{ 'sort-item-error': image.status === 'error' }"
                    >
                      <div class="sort-item-header">
                        <span class="sort-index">{{ index + 1 }}</span>
                        <span class="sort-status" :class="image.status">
                          <i v-if="image.status === 'uploading'" class="el-icon-loading"></i>
                          <i v-else-if="image.status === 'success'" class="el-icon-check"></i>
                          <i v-else-if="image.status === 'error'" class="el-icon-close"></i>
                        </span>
                        <el-button 
                          type="text" 
                          size="mini" 
                          class="remove-btn"
                          @click="removeImageFromSort(index)"
                          title="删除图片"
                        >
                          <i class="el-icon-delete"></i>
                        </el-button>
                      </div>
                      <div class="sort-item-preview">
                        <img 
                          v-if="image.preview" 
                          :src="image.preview" 
                          :alt="image.name"
                          class="preview-image"
                        />
                        <div v-else class="preview-placeholder">
                          <i class="el-icon-picture-outline"></i>
                        </div>
                      </div>
                      <div class="sort-item-info">
                        <div class="image-name" :title="image.name">{{ image.name }}</div>
                        <div class="image-size">{{ formatFileSize(image.size) }}</div>
                        <el-progress 
                          v-if="image.status === 'uploading'"
                          :percentage="image.percentage || 0"
                          :stroke-width="3"
                          class="upload-progress-mini"
                        ></el-progress>
                      </div>
                    </div>
                  </transition-group>
                </draggable>
                
                <!-- 排序操作按钮 -->
                <div class="sort-actions">
                  <el-button size="mini" @click="sortImagesByName">
                    <i class="el-icon-sort"></i>
                    按名称排序
                  </el-button>
                  <el-button size="mini" @click="sortImagesBySize">
                    <i class="el-icon-rank"></i>
                    按大小排序
                  </el-button>
                  <el-button size="mini" @click="reverseImagesOrder">
                    <i class="el-icon-refresh"></i>
                    反转顺序
                  </el-button>
                  <el-button size="mini" type="danger" @click="clearAllImages">
                    <i class="el-icon-delete"></i>
                    清空全部
                  </el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>
          
          <!-- URL批量输入 -->
          <el-tab-pane label="图片URL" name="url">
            <div class="url-input-area">
              <el-input
                type="textarea"
                v-model="batchImageUrls"
                :rows="8"
                placeholder="请输入图片URL地址，每行一个URL&#10;例如：&#10;https://example.com/image1.jpg&#10;https://example.com/image2.png&#10;https://example.com/image3.gif"
              ></el-input>
              <div class="url-tip">
                <i class="el-icon-info"></i>
                每行输入一个图片URL地址，支持 jpg、jpeg、png、gif、bmp、webp 格式
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
        
        <!-- 上传进度 -->
        <div v-if="batchUploadProgress.length > 0" class="upload-progress">
          <h4>上传进度</h4>
          <div v-for="(item, index) in batchUploadProgress" :key="index" class="progress-item">
            <div class="progress-info">
              <span class="filename">{{ item.name }}</span>
              <span class="status" :class="item.status">{{ getStatusText(item.status) }}</span>
            </div>
            <el-progress
              :percentage="item.percentage"
              :status="item.status === 'success' ? 'success' : item.status === 'error' ? 'exception' : ''"
            ></el-progress>
          </div>
        </div>
      </div>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeBatchImageUploadDialog">取消</el-button>
        <el-button type="primary" @click="insertBatchImages" :disabled="!canInsertImages">
          插入图片 ({{ getSelectedImagesCount() }})
        </el-button>
      </div>
    </el-dialog>
    
    <!-- 视频选择器组件 -->
    <RichTextVideoSelector
      ref="videoSelector"
      :show-button="false"
      @insert-videos="handleInsertVideos"
    />
    
    <div class="editor" ref="editor" :style="styles"></div>
  </div>
</template>

<script>
import axios from "axios"
import Quill from "quill"
import "quill/dist/quill.core.css"
import "quill/dist/quill.snow.css"
import "quill/dist/quill.bubble.css"
import { getToken } from "@/utils/auth"
import RichTextVideoSelector from '@/components/RichTextVideoSelector'
import { getVideoProcessedContent } from "@/api/chigua/video"
import Hls from 'hls.js'
import { hlsXhrSetup, patchNativeHlsM3u8 } from '@/utils/hlsUtils'
import draggable from 'vuedraggable'

// 注册自定义Video Blot
const BlockEmbed = Quill.import('blots/block/embed');

class VideoBlot extends BlockEmbed {
  static create(value) {
    console.log('🎬 VideoBlot.create 被调用，参数:', value);
    let node = super.create();
    
    // 如果传入的是字符串，解析为对象
    if (typeof value === 'string') {
      try {
        value = JSON.parse(value);
      } catch (e) {
        // 如果不是JSON，可能是URL
        value = { src: value };
      }
    }
    
    // 设置video属性
    node.setAttribute('controls', 'controls');
    node.setAttribute('width', '100%');
    node.style.maxWidth = '600px';
    node.style.display = 'block';
    node.style.margin = '10px 0';
    
    // 🔧 新增：保存关联的转码记录ID
    if (value.videoId) {
      node.setAttribute('data-video-id', value.videoId);
      console.log('🔧 设置 data-video-id:', value.videoId);
    }
    if (value.transcodeId) {
      node.setAttribute('data-transcode-id', value.transcodeId);
      console.log('🔧 设置 data-transcode-id:', value.transcodeId);
    }
    if (value.orgfile) {
      node.setAttribute('data-orgfile', value.orgfile);
    }
    if (value.resolution) {
      node.setAttribute('data-resolution', value.resolution);
    }
    if (value.duration) {
      node.setAttribute('data-duration', value.duration);
    }
    
    if (value.src) {
      // 如果有source信息，创建source标签
      if (value.type) {
        const source = document.createElement('source');
        source.setAttribute('src', value.src);
        source.setAttribute('type', value.type);
        node.appendChild(source);
      } else {
        node.setAttribute('src', value.src);
      }
    }
    
    if (value.poster) {
      node.setAttribute('poster', value.poster);
    }
    
    // 添加fallback文本
    if (node.children.length === 0) {
      node.textContent = '您的浏览器不支持视频播放。';
    }
    
    console.log('🎬 VideoBlot创建的video元素:', node);
    return node;
  }
  
  static value(node) {
    const src = node.getAttribute('src') || 
                (node.querySelector('source') && node.querySelector('source').getAttribute('src'));
    const type = node.querySelector('source') && node.querySelector('source').getAttribute('type');
    const poster = node.getAttribute('poster');
    
    // 🔧 新增：读取自定义属性
    const videoId = node.getAttribute('data-video-id');
    const transcodeId = node.getAttribute('data-transcode-id');
    const orgfile = node.getAttribute('data-orgfile');
    const resolution = node.getAttribute('data-resolution');
    const duration = node.getAttribute('data-duration');
    
    return {
      src: src,
      type: type,
      poster: poster,
      videoId: videoId,
      transcodeId: transcodeId,
      orgfile: orgfile,
      resolution: resolution,
      duration: duration
    };
  }
  
  static formats(node) {
    return {
      src: node.getAttribute('src'),
      poster: node.getAttribute('poster'),
      // 🔧 新增：包含自定义属性
      'data-video-id': node.getAttribute('data-video-id'),
      'data-transcode-id': node.getAttribute('data-transcode-id'),
      'data-orgfile': node.getAttribute('data-orgfile'),
      'data-resolution': node.getAttribute('data-resolution'),
      'data-duration': node.getAttribute('data-duration')
    };
  }
}

VideoBlot.blotName = 'video';
VideoBlot.tagName = 'video';

// 注册Video Blot
Quill.register(VideoBlot);

console.log('🎬 Video Blot 已注册到 Quill');

export default {
  name: "Editor",
  components: {
    RichTextVideoSelector,
    draggable
  },
  props: {
    /* 编辑器的内容 */
    value: {
      type: String,
      default: "",
    },
    /* 视频ID（用于转换data-resource-key为实际URL） */
    videoId: {
      type: [Number, String],
      default: null,
    },
    /* 高度 */
    height: {
      type: Number,
      default: null,
    },
    /* 最小高度 */
    minHeight: {
      type: Number,
      default: null,
    },
    /* 只读 */
    readOnly: {
      type: Boolean,
      default: false,
    },
    /* 上传文件大小限制(MB) */
    fileSize: {
      type: Number,
      default: 5,
    },
    /* 类型（base64格式、url格式） */
    type: {
      type: String,
      default: "url",
    }
  },
  data() {
    return {
      uploadUrl: "https://chigua-r2-worker.xingaikaka.workers.dev/api/upload/richtext", // 🔧 改为Worker API
      headers: {
        // 🔧 Worker不需要Authorization，去掉认证头
      },
      watermarkSrc: '/shuiyin.png',
      Quill: null,
      currentValue: "",
      savedSelection: null, // 保存的光标位置
      // 防抖处理相关
      textChangeDebounceTimer: null,
      lastContentLength: 0,
      // 批量上传相关
      batchImageUploadVisible: false,
      batchUploadTab: 'file',
      batchFileList: [],
      batchImageUrls: '',
      batchUploadProgress: [],
      batchUploadedImages: [], // 存储成功上传的图片信息
      imagesSortList: [], // 图片排序列表（用于拖拽排序）
      options: {
        theme: "snow",
        bounds: document.body,
        debug: "warn",
        modules: {
          // 工具栏配置
          toolbar: [
            ["bold", "italic", "underline", "strike"],       // 加粗 斜体 下划线 删除线
            ["blockquote", "code-block"],                    // 引用  代码块
            [{ list: "ordered" }, { list: "bullet" }],       // 有序、无序列表
            [{ indent: "-1" }, { indent: "+1" }],            // 缩进
            [{ size: ["small", false, "large", "huge"] }],   // 字体大小
            [{ header: [1, 2, 3, 4, 5, 6, false] }],         // 标题
            [{ color: [] }, { background: [] }],             // 字体颜色、字体背景颜色
            [{ align: [] }],                                 // 对齐方式
            ["clean"],                                       // 清除文本格式
            ["link", "image", "video"]                       // 链接、图片、视频
          ],
        },
        placeholder: "请输入内容",
        readOnly: this.readOnly,
          }
  }
},
  computed: {
    styles() {
      let style = {}
      if (this.minHeight) {
        style.minHeight = `${this.minHeight}px`
      }
      if (this.height) {
        style.height = `${this.height}px`
      }
      return style
    },
    // 计算属性：是否可以插入图片
    canInsertImages() {
      return this.getSelectedImagesCount() > 0
    }
  },
  watch: {
    value: {
      async handler(val) {
        if (val !== this.currentValue) {
          const newValue = val === null ? "" : val
          
          // 🔧 后端方案：如果内容包含data-resource-key或相对路径files/，调用后端API转换为签名URL
          const needsBackendProcess = newValue && this.videoId && (
            newValue.includes('data-resource-key') ||
            /<(source|img|video)[^>]*\s(src|poster)="files\//i.test(newValue)
          );
          if (needsBackendProcess) {
            try {
              const response = await getVideoProcessedContent(this.videoId)
              // 🔧 修复：使用response.msg而不是response.data
              let processed = response.msg || response.data || newValue
              // 🔧 回显前清理video上的blob/src属性，强制使用<source>的m3u8
              processed = this.sanitizeVideoTags(processed)
              this.currentValue = processed
            } catch (error) {
              console.warn('⚠️ 后端URL转换失败，使用原始内容:', error)
              this.currentValue = this.sanitizeVideoTags(newValue)
            }
          } else {
            this.currentValue = this.sanitizeVideoTags(newValue)
          }
          
          if (this.Quill) {
            this.Quill.clipboard.dangerouslyPasteHTML(this.currentValue)
            // 🔧 关键修复：在内容加载后延迟初始化视频播放器
            this.$nextTick(() => {
              setTimeout(() => {
                const videos = this.Quill.root.querySelectorAll('video')
                if (videos.length > 0) {
                  console.log('🎬 内容加载后发现视频，初始化播放器')
                  this.initializeAllVideoPlayers()
                }
              }, 500)
            })
          }
        }
      },
      immediate: true,
    },
  },
  mounted() {
    this.init()
  },
  beforeDestroy() {
    // 清理防抖定时器
    if (this.textChangeDebounceTimer) {
      clearTimeout(this.textChangeDebounceTimer);
      this.textChangeDebounceTimer = null;
    }
    // 清理所有HLS实例
    this.cleanupAllHlsInstances();
    // 清理图片事件处理器
    this.removeImageClickHandlers();
    this.Quill = null;
  },
  methods: {
    // 自定义上传：在发往 R2 前叠加水印
    async customUpload(options) {
      try {
        const { file, onSuccess, onError } = options
        const processedFile = await this.applyWatermarkToFile(file, this.watermarkSrc)
        const formData = new FormData()
        formData.append('file', processedFile, processedFile.name || file.name)
        const res = await fetch(this.uploadUrl, {
          method: 'POST',
          headers: this.headers,
          body: formData
        })
        const json = await res.json()
        onSuccess && onSuccess(json, processedFile)
      } catch (e) {
        console.error('富文本图片上传（加水印）失败', e)
        options.onError && options.onError(e)
        this.handleUploadError()
      }
    },
    async applyWatermarkToFile(file, wmSrc) {
      if (!file || !file.type || !file.type.startsWith('image/')) return file
      const baseImg = await this.fileToImage(file)
      const wmImg = await this.loadWatermark(wmSrc)
      const canvas = document.createElement('canvas')
      canvas.width = baseImg.naturalWidth || baseImg.width
      canvas.height = baseImg.naturalHeight || baseImg.height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(baseImg, 0, 0, canvas.width, canvas.height)
      // 🎯 智能自适应水印尺寸
      const watermarkSize = this.calculateAdaptiveWatermarkSize(canvas.width, canvas.height)
      const targetW = watermarkSize.width
      const targetH = watermarkSize.height
      const dx = canvas.width - targetW - 10
      const dy = canvas.height - targetH - 10
      
      // 🔧 确保水印完全不透明且清晰
      ctx.globalAlpha = 1.0
      ctx.imageSmoothingEnabled = false  // 禁用图像平滑，保持清晰
      ctx.drawImage(wmImg, dx, dy, targetW, targetH)
      ctx.imageSmoothingEnabled = true   // 恢复图像平滑
      const type = file.type || 'image/jpeg'
      const blob = await new Promise(resolve => canvas.toBlob(resolve, type, 1.0))
      const name = this.appendSuffixToName(file.name || 'image', '_wm')
      return new File([blob], name, { type })
    },
    fileToImage(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => {
          const img = new Image()
          img.onload = () => resolve(img)
          img.onerror = reject
          img.src = reader.result
        }
        reader.onerror = reject
        reader.readAsDataURL(file)
      })
    },
    loadWatermark(src) {
      return new Promise((resolve, reject) => {
        const img = new Image()
        img.crossOrigin = 'anonymous'
        img.onload = () => resolve(img)
        img.onerror = reject
        img.src = src
      })
    },
    appendSuffixToName(name, suffix) {
      const idx = name.lastIndexOf('.')
      if (idx === -1) return name + suffix
      return name.slice(0, idx) + suffix + name.slice(idx)
    },
    
    /**
     * 🎯 计算水印尺寸 - 固定260x90
     */
    calculateAdaptiveWatermarkSize(imageWidth, imageHeight) {
      // 固定水印尺寸
      let targetWidth = 260
      let targetHeight = 90
      
      // 🔧 确保水印不会超过图片尺寸的一半
      const maxAllowedWidth = imageWidth / 2
      const maxAllowedHeight = imageHeight / 2
      
      if (targetWidth > maxAllowedWidth || targetHeight > maxAllowedHeight) {
        // 如果固定尺寸太大，按比例缩小
        const scaleX = maxAllowedWidth / targetWidth
        const scaleY = maxAllowedHeight / targetHeight
        const scale = Math.min(scaleX, scaleY)
        
        targetWidth = Math.round(targetWidth * scale)
        targetHeight = Math.round(targetHeight * scale)
      }
      
      console.log(`🎯 水印尺寸: 图片${imageWidth}x${imageHeight} -> 水印${targetWidth}x${targetHeight}`)
      
      return {
        width: targetWidth,
        height: targetHeight
      }
    },
    // 🔧 清理video标签的src（去除blob或任何残留src），优先让<source>生效
    sanitizeVideoTags(html) {
      if (!html) return html
      try {
        // 去掉 video 上的 src 属性（双引号/单引号都处理）
        let out = html.replace(/<video([^>]*?)\s+src=\"[^\"]*\"([^>]*)>/gi, '<video$1$2>')
        out = out.replace(/<video([^>]*?)\s+src=\'[^\']*\'([^>]*)>/gi, '<video$1$2>')
        return out
      } catch (e) {
        return html
      }
    },
    // 清理所有HLS实例
    cleanupAllHlsInstances() {
      if (this.Quill && this.Quill.root) {
        const allVideos = this.Quill.root.querySelectorAll('video');
        allVideos.forEach((video, index) => {
          if (video._hlsInstance) {
            console.log(`🎬 清理HLS实例${index}`);
            video._hlsInstance.destroy();
            video._hlsInstance = null;
          }
        });
      }
    },
    init() {
      const editor = this.$refs.editor
      this.Quill = new Quill(editor, this.options)
      // 如果设置了上传地址则自定义图片上传事件
      if (this.type == 'url') {
        let toolbar = this.Quill.getModule("toolbar")
        
        // 自定义图片处理
        toolbar.addHandler("image", (value) => {
          if (value) {
            // 先保存当前光标位置
            const savedRange = this.Quill.getSelection()
            
            // 弹出选择框：输入URL、单张上传或批量上传
            this.$confirm('请选择图片插入方式', '插入图片', {
              confirmButtonText: '输入图片URL',
              cancelButtonText: '文件上传',
              distinguishCancelAndClose: true,
              customClass: 'image-insert-dialog',
              message: '您可以输入图片URL地址，或选择文件上传（支持批量上传）'
            }).then(() => {
              // 用户选择输入URL
              this.$prompt('请输入图片URL地址', '插入图片URL', {
                confirmButtonText: '插入图片',
                cancelButtonText: '取消',
                inputPattern: /^https?:\/\/.+\.(jpg|jpeg|png|gif|bmp|webp)(\?.*)?$/i,
                inputErrorMessage: '请输入有效的图片URL地址'
              }).then(({ value: imageUrl }) => {
                this.insertImageFromUrl(imageUrl, savedRange)
              }).catch(() => {
                console.log('用户取消输入URL')
              })
            }).catch((action) => {
              if (action === 'cancel') {
                // 用户选择文件上传，显示批量上传选项
                this.showBatchImageUploadDialog(savedRange)
              }
            })
          } else {
            this.Quill.format("image", false)
          }
        })

        // 自定义视频处理
        toolbar.addHandler("video", (value) => {
          if (value) {
            // 保存当前光标位置
            const savedRange = this.Quill.getSelection()
            this.savedSelection = savedRange
            
            // 显示视频选择对话框
            this.showVideoSelector()
          } else {
            this.Quill.format("video", false)
          }
        })

        this.Quill.root.addEventListener('paste', this.handlePasteCapture, true)
      }
      this.Quill.clipboard.dangerouslyPasteHTML(this.currentValue)
      
      // 🔧 关键修复：初始化完成后检查并初始化视频播放器
      this.$nextTick(() => {
        setTimeout(() => {
          const videos = this.Quill.root.querySelectorAll('video')
          if (videos.length > 0) {
            console.log('🎬 编辑器初始化后发现视频，初始化播放器')
            this.initializeAllVideoPlayers()
          }
          
          // 初始化图片点击事件处理
          this.initImageClickHandlers()
        }, 500)
      })
      
      // 添加键盘事件处理 - Enter键
      this.Quill.keyboard.addBinding({
        key: 'Enter'
      }, (range, context) => {
        if (range) {
          // 检查光标是否在媒体元素上或附近
          const [leaf] = this.Quill.getLeaf(range.index)
          const [prevLeaf] = range.index > 0 ? this.Quill.getLeaf(range.index - 1) : [null]
          
          // 如果当前位置或前一个位置是媒体元素
          if ((leaf && this.isMediaElement(leaf.domNode)) || 
              (prevLeaf && this.isMediaElement(prevLeaf.domNode))) {
            
            // 在当前位置插入新行，并确保有可编辑内容
            this.Quill.insertText(range.index, '\n', 'user')
            this.ensureEditableParagraphAt(range.index + 1)
            this.Quill.setSelection(range.index + 1)
            return false // 阻止默认行为
          }
        }
        return true // 允许默认行为
      })
      
      // 添加Shift+Enter的处理
      this.Quill.keyboard.addBinding({
        key: 'Enter',
        shiftKey: true
      }, (range, context) => {
        if (range) {
          // 检查光标是否在媒体元素上或附近
          const [leaf] = this.Quill.getLeaf(range.index)
          const [prevLeaf] = range.index > 0 ? this.Quill.getLeaf(range.index - 1) : [null]
          
          // 如果当前位置或前一个位置是媒体元素
          if ((leaf && this.isMediaElement(leaf.domNode)) || 
              (prevLeaf && this.isMediaElement(prevLeaf.domNode))) {
            
            // 在当前位置插入新行，并确保有可编辑内容
            this.Quill.insertText(range.index, '\n', 'user')
            this.ensureEditableParagraphAt(range.index + 1)
            this.Quill.setSelection(range.index + 1)
            return false // 阻止默认行为
          }
        }
        return true // 允许默认行为
      })
      
      // 添加上下箭头键导航优化
      this.Quill.keyboard.addBinding({
        key: 'ArrowUp'
      }, (range, context) => {
        if (range && range.length === 0) {
          const [leaf] = this.Quill.getLeaf(range.index)
          if (leaf && this.isMediaElement(leaf.domNode)) {
            // 在图片上按上箭头，移动到图片前
            const newPos = Math.max(0, range.index - 1)
            this.ensureEditableParagraphAt(newPos)
            this.Quill.setSelection(newPos)
            return false
          }
        }
        return true
      })
      
      this.Quill.keyboard.addBinding({
        key: 'ArrowDown'
      }, (range, context) => {
        if (range && range.length === 0) {
          const [leaf] = this.Quill.getLeaf(range.index)
          if (leaf && this.isMediaElement(leaf.domNode)) {
            // 在图片上按下箭头，移动到图片后
            const newPos = range.index + 1
            this.ensureEditableParagraphAt(newPos)
            this.Quill.setSelection(newPos)
            return false
          }
        }
        return true
      })
      
      // 添加点击事件处理（保留原有逻辑作为备用）
      this.Quill.root.addEventListener('click', (e) => {
        // 如果点击的不是图片，则使用原有逻辑
        if (e.target.tagName !== 'IMG') {
          setTimeout(() => {
            const selection = this.Quill.getSelection()
            if (selection) {
              // 检查光标是否在媒体元素上
              const [leaf] = this.Quill.getLeaf(selection.index)
              if (leaf && this.isMediaElement(leaf.domNode)) {
                // 如果光标在媒体元素上，移动到元素后
                const index = selection.index
                // 确保媒体元素后有换行符
                this.ensureEditableParagraphAt(index + 1)
                // 将光标移动到媒体元素后的新行
                this.Quill.setSelection(index + 1)
              }
            }
          }, 10) // 短暂延迟确保选择已更新
        }
      })
      
      this.Quill.on("text-change", (delta, oldDelta, source) => {
        const html = this.$refs.editor.children[0].innerHTML
        const text = this.Quill.getText()
        const quill = this.Quill
        this.currentValue = html
        
        // 🔧 防抖处理：只有内容长度真正变化时才打印日志
        const currentLength = html.length
        if (Math.abs(currentLength - this.lastContentLength) > 5) { // 长度变化超过5个字符才记录
          console.log('📝 编辑器内容变化:', { length: currentLength, delta: currentLength - this.lastContentLength });
          this.lastContentLength = currentLength
        }
        
        this.$emit("input", html)
        this.$emit("on-change", { html: html, text, quill })
      })
      this.Quill.on("text-change", (delta, oldDelta, source) => {
        this.$emit("on-text-change", delta, oldDelta, source)
        
        // 防抖处理图片事件初始化
        this.debouncedInitImageClickHandlers()
      })
      this.Quill.on("selection-change", (range, oldRange, source) => {
        this.$emit("on-selection-change", range, oldRange, source)
      })
      this.Quill.on("editor-change", (eventName, ...args) => {
        this.$emit("on-editor-change", eventName, ...args)
      })
    },
    // 上传前校检格式和大小
    handleBeforeUpload(file) {
      const type = ["image/jpeg", "image/jpg", "image/png", "image/svg"]
      const isJPG = type.includes(file.type)
      // 检验文件格式
      if (!isJPG) {
        this.$message.error(`图片格式错误!`)
        return false
      }
      // 校检文件大小
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize
        if (!isLt) {
          this.$message.error(`上传文件大小不能超过 ${this.fileSize} MB!`)
          return false
        }
      }
      return true
    },
    handleUploadSuccess(res, file) {
      // 🔧 适配Worker API响应格式
      console.log('🔄 图片上传完整响应:', JSON.stringify(res, null, 2));
      
      if (res.success) {
        console.log('🔄 图片上传成功，Worker响应:', res);
        
        // 🔧 验证响应数据结构
        if (!res.data) {
          console.error('❌ Worker响应缺少data字段:', res);
          this.$message.error("上传响应格式错误：缺少data字段");
          return;
        }
        
        if (!res.data.resourceKey || !res.data.url) {
          console.error('❌ Worker响应缺少必要字段:', {
            hasResourceKey: !!res.data.resourceKey,
            hasUrl: !!res.data.url,
            data: res.data
          });
          this.$message.error("上传响应格式错误：缺少resourceKey或url字段");
          return;
        }
        
        // 获取富文本组件实例
        let quill = this.Quill
        
        // 优先使用保存的光标位置
        let insertPosition = null
        if (this.savedSelection) {
          insertPosition = this.savedSelection.index
          this.savedSelection = null // 清除保存的位置
        } else {
          // 尝试获取当前光标位置
          const currentRange = quill.getSelection()
          if (currentRange) {
            insertPosition = currentRange.index
          }
        }
        
        if (insertPosition !== null) {
          // 🔧 Worker API返回的数据
          const resourceKey = res.data.resourceKey;  // 用于存储的资源键
          const previewUrl = res.data.url; // 立即显示用的预览URL
          
          console.log('📌 资源信息:', { resourceKey, previewUrl });
          
          // 确保插入位置有可编辑的段落
          this.ensureEditableParagraphAt(insertPosition)
          
          // 使用更好的DOM结构，确保图片前后有可编辑区域
          const imgHtml = `<p><br></p><p><img src="${previewUrl}" data-resource-key="${resourceKey}" alt="图片" style="max-width: 100%; display: block; margin: 10px auto;"></p><p><br></p>`;
          
          console.log('🖼️ 插入的HTML:', imgHtml);
          
          // 使用完整的段落结构插入编辑器
          quill.clipboard.dangerouslyPasteHTML(insertPosition, imgHtml);
          
          // 设置光标到图片后的空段落
          const newPosition = insertPosition + 3; // 三个段落：空段落 + 图片段落 + 空段落
          quill.setSelection(newPosition);
          
          console.log('✅ 图片已插入到编辑器');
          
          // 延迟初始化图片点击事件（防抖处理）
          this.debouncedInitImageClickHandlers()
        } else {
          // 如果没有光标位置，插入到文档末尾
          const length = quill.getLength()
          const resourceKey = res.data.resourceKey;
          const previewUrl = res.data.url;
          
          // 确保末尾有可编辑区域
          const insertPos = Math.max(0, length - 1)
          this.ensureEditableParagraphAt(insertPos)
          
          // 使用更好的DOM结构
          const imgHtml = `<p><br></p><p><img src="${previewUrl}" data-resource-key="${resourceKey}" alt="图片" style="max-width: 100%; display: block; margin: 10px auto;"></p><p><br></p>`;
          quill.clipboard.dangerouslyPasteHTML(insertPos, imgHtml);
          
          // 设置光标到最后的空段落
          const newLength = quill.getLength()
          quill.setSelection(newLength - 1);
          
          console.log('✅ 图片已插入到编辑器末尾');
          
          // 延迟初始化图片点击事件（防抖处理）
          this.debouncedInitImageClickHandlers()
        }
      } else {
        console.error('❌ 图片上传失败:', res);
        this.$message.error(res.message || "上传失败");
      }
    },
    handleUploadError() {
      this.$message.error("图片插入失败")
    },
    // 复制粘贴图片处理
    handlePasteCapture(e) {
      const clipboard = e.clipboardData || window.clipboardData
      if (clipboard && clipboard.items) {
        for (let i = 0; i < clipboard.items.length; i++) {
          const item = clipboard.items[i]
          if (item.type.indexOf('image') !== -1) {
            e.preventDefault()
            // 保存当前光标位置
            this.savedSelection = this.Quill.getSelection()
            const file = item.getAsFile()
            this.insertImage(file)
          }
        }
      }
      
      // 处理粘贴的文本，检查是否为图片URL
      if (clipboard && clipboard.getData) {
        const pastedText = clipboard.getData('text/plain')
        if (pastedText && this.isImageUrl(pastedText)) {
          e.preventDefault()
          const range = this.Quill.getSelection()
          
          // 检查是否为本服务器的图片URL
          const isLocalImage = pastedText.includes(process.env.VUE_APP_BASE_API)
          
          if (range) {
            this.ensureEditableParagraphAt(range.index)
            
            if (isLocalImage) {
              // 本服务器图片：转换为资源键格式，但保留原URL用于显示
              const resourceKey = this.extractResourceKeyFromUrl(pastedText)
              const imgHtml = `<p><br></p><p><img src="${pastedText}" data-resource-key="${resourceKey}" alt="图片" style="max-width: 100%; display: block; margin: 10px auto;"></p><p><br></p>`
              this.Quill.clipboard.dangerouslyPasteHTML(range.index, imgHtml)
              this.Quill.setSelection(range.index + 3)
            } else {
              // 外部图片：直接使用URL但确保前后有空行
              this.Quill.insertText(range.index, '\n', 'user')
              this.Quill.insertEmbed(range.index + 1, 'image', pastedText)
              this.Quill.insertText(range.index + 2, '\n\n', 'user')
              this.Quill.setSelection(range.index + 4)
            }
            
            // 延迟初始化图片点击事件（防抖处理）
            this.debouncedInitImageClickHandlers()
          } else {
            // 如果没有选区，插入到文档末尾
            const length = this.Quill.getLength()
            const insertPos = Math.max(0, length - 1)
            this.ensureEditableParagraphAt(insertPos)
            
            if (isLocalImage) {
              const resourceKey = this.extractResourceKeyFromUrl(pastedText)
              const imgHtml = `<p><br></p><p><img src="${pastedText}" data-resource-key="${resourceKey}" alt="图片" style="max-width: 100%; display: block; margin: 10px auto;"></p><p><br></p>`
              this.Quill.clipboard.dangerouslyPasteHTML(insertPos, imgHtml)
              const newLength = this.Quill.getLength()
              this.Quill.setSelection(newLength - 1)
            } else {
              this.Quill.insertText(insertPos, '\n', 'user')
              this.Quill.insertEmbed(insertPos + 1, 'image', pastedText)
              this.Quill.insertText(insertPos + 2, '\n\n', 'user')
              const newLength = this.Quill.getLength()
              this.Quill.setSelection(newLength - 1)
            }
            
            // 延迟初始化图片点击事件（防抖处理）
            this.debouncedInitImageClickHandlers()
          }
        }
      }
    },
    insertImage(file) {
      const formData = new FormData()
      formData.append("file", file)
      axios.post(this.uploadUrl, formData, { headers: { "Content-Type": "multipart/form-data", Authorization: this.headers.Authorization } }).then(res => {
        this.handleUploadSuccess(res.data)
      })
    },
    // 检查是否为图片URL
    isImageUrl(url) {
      return /^https?:\/\/.+\.(jpg|jpeg|png|gif|bmp|webp|svg)(\?.*)?$/i.test(url)
    },
    // 从URL中提取资源键
    extractResourceKeyFromUrl(url) {
      if (url.includes(process.env.VUE_APP_BASE_API)) {
        const path = url.substring(process.env.VUE_APP_BASE_API.length);
        let resourceKey = path.startsWith('/') ? path.substring(1) : path;
        
        // 移除可能存在的 files/ 前缀，避免重复
        if (resourceKey.startsWith('files/')) {
          resourceKey = resourceKey.substring(6); // 移除 "files/" 前缀
        }
        
        return resourceKey;
      }
      return url; // 如果不是本服务器URL，则保持原样
    },

    // 检查是否为媒体元素
    isMediaElement(node) {
      if (!node || !node.tagName) return false
      const mediaElements = ["IMG", "VIDEO", "IFRAME"]
      return mediaElements.includes(node.tagName.toUpperCase())
    },
    // 显示视频选择器
    showVideoSelector() {
      if (this.$refs.videoSelector) {
        this.$refs.videoSelector.showDialog = true
      }
    },
    // 处理插入视频
    handleInsertVideos(htmlContents) {
      console.log('🎬 Quill Editor - handleInsertVideos被调用！');
      console.log('🎬 htmlContents:', htmlContents);
      
      if (!this.Quill || !htmlContents || htmlContents.length === 0) {
        console.log('🎬 缺少必要条件，退出插入');
        return
      }

      try {
        // 获取当前光标位置
        let insertIndex = 0;
        const savedRange = this.savedSelection;
        
        if (savedRange) {
          insertIndex = savedRange.index;
          console.log('🎬 使用保存的光标位置:', insertIndex);
        } else {
          const currentSelection = this.Quill.getSelection();
          if (currentSelection) {
            insertIndex = currentSelection.index;
          } else {
            insertIndex = Math.max(0, this.Quill.getLength() - 1);
          }
          console.log('🎬 使用当前光标位置:', insertIndex);
        }
        
        htmlContents.forEach((htmlContent, index) => {
          console.log(`🎬 处理第${index + 1}个视频HTML:`, htmlContent);
          
          try {
            // 🔧 新方案：从HTML中提取视频数据，使用VideoBlot插入
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = htmlContent;
            
            // 提取视频数据
            const videoDiv = tempDiv.querySelector('.rich-text-video');
            const videoElement = tempDiv.querySelector('video');
            const sourceElement = tempDiv.querySelector('source');
            
            if (videoDiv && videoElement) {
              const videoData = {
                videoId: videoDiv.getAttribute('data-video-id'),
                transcodeId: videoDiv.getAttribute('data-transcode-id'),
                orgfile: videoDiv.getAttribute('data-orgfile'),
                resolution: videoDiv.getAttribute('data-resolution'),
                duration: videoDiv.getAttribute('data-duration'),
                poster: videoElement.getAttribute('poster'),
                src: sourceElement ? sourceElement.getAttribute('src') : videoElement.getAttribute('src'),
                type: sourceElement ? sourceElement.getAttribute('type') : null
              };
              
              console.log(`🎬 提取的视频数据:`, videoData);
              
              // 使用VideoBlot插入
              this.Quill.insertEmbed(insertIndex, 'video', videoData, 'user');
              console.log(`🎬 视频${index + 1} Blot插入成功`);
              
              // 在视频后插入换行
              insertIndex += 1;
              this.Quill.insertText(insertIndex, '\n', 'user');
              insertIndex += 1;
              
            } else {
              // 降级方案：直接插入HTML
              console.log(`🎬 降级方案：直接插入HTML`);
              this.Quill.clipboard.dangerouslyPasteHTML(insertIndex, htmlContent);
              insertIndex += 1;
              this.Quill.insertText(insertIndex, '\n', 'user');
              insertIndex += 1;
            }
            
          } catch (insertError) {
            console.error(`🚨 插入视频${index + 1}失败:`, insertError);
            
            // 如果所有方法都失败，插入纯文本描述
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = htmlContent;
            const orgfile = tempDiv.querySelector('[data-orgfile]')?.getAttribute('data-orgfile') || 'unknown';
            const fallbackText = `[视频: ${orgfile}]\n`;
            this.Quill.insertText(insertIndex, fallbackText, 'user');
            insertIndex += fallbackText.length;
          }
        });
        
        // 确保有换行分隔
        if (insertIndex > 0) {
          const prevChar = this.Quill.getText(insertIndex - 1, 1);
          if (prevChar !== '\n') {
            this.Quill.insertText(insertIndex, '\n', 'user');
            insertIndex += 1;
          }
        }
        
        // 设置光标到最后插入位置
        this.Quill.setSelection(insertIndex);
        
        // 清除保存的选择
        this.savedSelection = null;
        
        // 触发内容变化事件
        this.$emit("blur");
        
        console.log('🎬 视频插入完成，检查结果');
        
        // 延迟检查插入结果和初始化播放器
        this.$nextTick(() => {
          setTimeout(() => {
            const editor = this.Quill.root;
            const videos = editor.querySelectorAll('video');
            console.log(`🎬 最终检查：编辑器中有${videos.length}个video元素`);
            
            if (videos.length > 0) {
              console.log('🎬 开始初始化视频播放器');
              videos.forEach((video, i) => {
                console.log(`🎬 找到video${i}:`, {
                  src: video.src,
                  sources: Array.from(video.querySelectorAll('source')).map(s => ({
                    src: s.src,
                    type: s.type
                  })),
                  controls: video.hasAttribute('controls'),
                  poster: video.getAttribute('poster')
                });
              });
              this.initializeAllVideoPlayers();
            } else {
              console.error('🚨 没有找到video元素');
              console.log('🎬 编辑器HTML内容:', editor.innerHTML);
            }
          }, 500);
        });
        
      } catch (error) {
        console.error('🚨 Quill Editor - 插入视频失败:', error);
        this.$message.error('插入视频失败: ' + error.message);
      }
    },
    
    // 初始化所有视频播放器
    initializeAllVideoPlayers() {
      console.log('🎬 ========== 开始初始化所有视频播放器 ==========');
      
      const editor = this.Quill.root;
      const allVideos = editor.querySelectorAll('video');
      const richTextVideos = editor.querySelectorAll('.rich-text-video');
      
      console.log('🎬 发现的元素:', {
        '所有video标签': allVideos.length,
        'rich-text-video容器': richTextVideos.length
      });
      
      // 初始化所有视频播放器
      allVideos.forEach((video, index) => {
        console.log(`🎬 初始化video播放器${index}:`, {
          tagName: video.tagName,
          src: video.src,
          controls: video.hasAttribute('controls'),
          sources: Array.from(video.querySelectorAll('source')).map(s => ({
            src: s.src,
            type: s.type
          })),
          computedStyle: {
            display: getComputedStyle(video).display,
            visibility: getComputedStyle(video).visibility,
            opacity: getComputedStyle(video).opacity
          }
        });
        
        this.initializeVideoPlayer(video, `all_${index}`);
      });
      
      console.log('🎬 ========== 视频播放器初始化完成 ==========');
    },
    
    // 初始化视频播放器
    initializeVideoPlayer(video, index) {
      console.log(`🎬 初始化video播放器 - 标识: ${index}`);
      
      if (!video || !video.isConnected) {
        console.error(`🚨 video播放器${index} - video元素不存在或未连接到DOM`);
        return;
      }
      
      try {
        // 确保video元素有基本属性
        if (!video.hasAttribute('controls')) {
          video.setAttribute('controls', 'controls');
        }
        
        // 确保样式正确
        video.style.display = 'block';
        video.style.width = '100%';
        video.style.maxWidth = '600px';
        
        // 清理可能存在的旧HLS实例
        if (video._hlsInstance) {
          console.log(`🎬 video播放器${index} - 清理旧HLS实例`);
          video._hlsInstance.destroy();
          video._hlsInstance = null;
        }
        
        // 查找视频源
        const m3u8Source = video.querySelector('source[type="application/x-mpegURL"]');
        const mp4Source = video.querySelector('source[type="video/mp4"]');
        
        console.log(`🎬 video播放器${index} - 源分析:`, {
          hasM3u8: !!m3u8Source,
          hasMP4: !!mp4Source,
          m3u8Url: m3u8Source ? m3u8Source.src : null,
          mp4Url: mp4Source ? mp4Source.src : null,
          allSources: Array.from(video.querySelectorAll('source')).map(s => ({
            src: s.src,
            type: s.type
          }))
        });
        
        // 处理M3U8视频
        if (m3u8Source && m3u8Source.src) {
          const m3u8Url = m3u8Source.src;
          console.log(`🎬 video播放器${index} - 处理M3U8视频:`, m3u8Url);
          
          // 检查是否支持HLS.js
          if (typeof Hls !== 'undefined' && Hls.isSupported()) {
            console.log(`🎬 video播放器${index} - 使用HLS.js`);
            
            const hls = new Hls({
              debug: false,
              enableWorker: false,
              autoStartLoad: true,
              startLevel: -1,
              maxLoadingDelay: 4,
              maxBufferLength: 30,
              maxMaxBufferLength: 600,
              xhrSetup: hlsXhrSetup,
            });
            
            hls.loadSource(m3u8Url);
            hls.attachMedia(video);
            
            hls.on(Hls.Events.MANIFEST_PARSED, () => {
              console.log(`🎬 video播放器${index} - HLS manifest解析成功`);
            });
            
            hls.on(Hls.Events.ERROR, (event, data) => {
              console.error(`🎬 video播放器${index} - HLS错误:`, data);
              if (data.fatal) {
                switch (data.type) {
                  case Hls.ErrorTypes.NETWORK_ERROR:
                    console.log(`🎬 video播放器${index} - 网络错误，尝试重启`);
                    hls.startLoad();
                    break;
                  case Hls.ErrorTypes.MEDIA_ERROR:
                    console.log(`🎬 video播放器${index} - 媒体错误，尝试恢复`);
                    hls.recoverMediaError();
                    break;
                  default:
                    console.error(`🎬 video播放器${index} - 致命错误，销毁HLS`);
                    hls.destroy();
                    video._hlsInstance = null;
                    this.fallbackToMp4(video, mp4Source, index);
                    break;
                }
              }
            });
            
            video._hlsInstance = hls;
            console.log(`🎬 video播放器${index} - HLS实例已保存`);
            
          } else if (video.canPlayType && video.canPlayType('application/vnd.apple.mpegurl')) {
            // iOS Safari 原生 HLS：重写 key URI 为绝对后端地址
            console.log(`🎬 video播放器${index} - 使用Safari原生HLS`);
            const prevBlobUrl = video._nativeBlobUrl;
            patchNativeHlsM3u8(m3u8Url).then(({ url: patchedUrl, isBlob }) => {
              if (prevBlobUrl) { try { URL.revokeObjectURL(prevBlobUrl); } catch (_) {} }
              video._nativeBlobUrl = isBlob ? patchedUrl : null;
              video.src = patchedUrl;
              video.load();
            });
          } else {
            console.warn(`🎬 video播放器${index} - 不支持HLS，尝试MP4备选`);
            this.fallbackToMp4(video, mp4Source, index);
          }
        } 
        // 处理MP4视频
        else if (mp4Source && mp4Source.src) {
          console.log(`🎬 video播放器${index} - 直接使用MP4:`, mp4Source.src);
          video.src = mp4Source.src;
          video.load();
        } 
        // 没有找到有效源
        else {
          console.warn(`🎬 video播放器${index} - 未找到有效视频源`);
          // 尝试从video.src获取
          if (video.src) {
            console.log(`🎬 video播放器${index} - 尝试使用video.src:`, video.src);
            video.load();
          } else {
            console.error(`🎬 video播放器${index} - 完全没有视频源`);
          }
        }
        
        // 添加错误处理
        video.addEventListener('error', (e) => {
          console.error(`🚨 video播放器${index} - 视频加载错误:`, e);
          console.error(`🚨 video播放器${index} - 错误详情:`, {
            error: video.error,
            networkState: video.networkState,
            readyState: video.readyState,
            src: video.src,
            currentSrc: video.currentSrc
          });
        });
        
        // 添加加载成功监听
        video.addEventListener('loadedmetadata', () => {
          console.log(`🎬 video播放器${index} - 视频元数据加载成功`);
        });
        
        console.log(`🎬 video播放器${index} - 初始化完成`);
        
      } catch (error) {
        console.error(`🚨 video播放器${index} - 初始化失败:`, error);
      }
    },
    
    // MP4备选方案
    fallbackToMp4(video, mp4Source, index) {
      if (mp4Source && mp4Source.src) {
        console.log(`🎬 video播放器${index} - 使用MP4备选:`, mp4Source.src);
        video.src = mp4Source.src;
        video.load();
      } else {
        console.error(`🎬 video播放器${index} - 没有MP4备选源`);
      }
    },
    
    
    
    // 🔧 强制转换所有签名URL为相对路径
    forceConvertSignedUrls(html) {
      if (!html) return html;
      
      console.log('🔧 强制转换签名URL开始');
      let processed = html;
      
      // 强制转换所有包含签名参数的URL
      processed = processed.replace(
        /src="([^"]*\?[^"]*(?:signature=|key=|expires=)[^"]*)"/gi,
        (match, url) => {
          console.log('🔧 发现签名URL:', url);
          
          // 提取/files/后面的路径
          const filesMatch = url.match(/\/files\/([^?]*)/);
          if (filesMatch) {
            const resourceKey = `files/${filesMatch[1]}`;
            console.log('🔧 转换为相对路径:', resourceKey);
            return `src="${resourceKey}"`;
          }
          
          return match;
        }
      );
      
      // 强制转换poster属性
      processed = processed.replace(
        /poster="([^"]*\?[^"]*(?:signature=|key=|expires=)[^"]*)"/gi,
        (match, url) => {
          console.log('🔧 发现签名poster URL:', url);
          
          // 提取/files/后面的路径
          const filesMatch = url.match(/\/files\/([^?]*)/);
          if (filesMatch) {
            const resourceKey = `files/${filesMatch[1]}`;
            console.log('🔧 转换poster为相对路径:', resourceKey);
            return `poster="${resourceKey}"`;
          }
          
          return match;
        }
      );
      
      console.log('🔧 强制转换完成');
      return processed;
    },
    
    // 🔧 获取用于存储的HTML内容（转换预览URL为存储格式）
    getStorageContent() {
      try {
        const html = this.$refs.editor.children[0].innerHTML;
        
        // 🔧 简化逻辑：如果内容已经包含存储格式特征，直接返回
        const hasStorageFormat = html.includes('data-resource-key=') || 
                                html.includes('src="files/') || 
                                html.includes('poster="files/');
        const hasSignedUrls = html.includes('signature=') ||
                             html.includes('expires=') ||
                             html.includes('?key=') ||
                             html.includes('&key=') ||
                             // 检查是否包含完整的HTTP(S) URL或blob URL
                             /(?:src|poster)="(?:https?:\/\/[^"]+|blob:[^"]+)"/i.test(html);
        
        if (hasStorageFormat && !hasSignedUrls) {
          console.log('✅ 内容已经是存储格式，直接返回');
          return html;
        }
        
        let converted = this.convertPreviewUrlsToStorage(html);
        
        // 🔧 如果常规转换没有效果，使用强制转换
        if (converted === html && (html.includes('signature=') || html.includes('key=') || html.includes('expires='))) {
          console.log('🔧 常规转换无效果，使用强制转换');
          converted = this.forceConvertSignedUrls(html);
        }
        
        // 🔧 添加内容完整性检查
        if (html.length > 100 && converted.length < html.length * 0.5) {
          console.warn('⚠️ 转换后内容长度显著减少，使用原始内容:', {
            原始长度: html.length,
            转换后长度: converted.length,
            减少比例: ((html.length - converted.length) / html.length * 100).toFixed(1) + '%'
          });
          return html; // 如果转换导致内容大幅减少，返回原始内容
        }
        
        return converted;
      } catch (error) {
        console.error('❌ 获取存储内容失败:', error);
        // 降级处理：返回原始HTML
        return this.$refs.editor.children[0].innerHTML;
      }
    },
    
    // 🔧 将预览URL转换为存储格式
    convertPreviewUrlsToStorage(html) {
      if (!html) return html;
      
      console.log('🔍 开始转换HTML，原始长度:', html.length);
      
      let processed = html;
      let conversionStats = {
        图片转换数: 0,
        视频转换数: 0,
        错误数: 0
      };
      
      // 1. 处理图片：将Worker签名URL转换为资源路径
      try {
        processed = processed.replace(
          /<img([^>]*?)>/gi,
          (match) => {
            try {
              // 🔧 改进：检查是否需要转换（包含签名URL特征或blob URL）
              const needsConversion = match.includes('signature=') ||
                                    match.includes('expires=') ||
                                    match.includes('?key=') ||
                                    match.includes('&key=') ||
                                    // 检查是否包含完整的HTTP(S) URL或blob URL（而不是相对路径）
                                    /src="(?:https?:\/\/[^"]+|blob:[^"]+)"/i.test(match);
              
              if (needsConversion) {
                // 🔧 优先使用data-resource-key，如果没有则从URL提取
                let resourceKey = null;
                
                const resourceKeyMatch = match.match(/data-resource-key="([^"]*?)"/);
                if (resourceKeyMatch) {
                  resourceKey = resourceKeyMatch[1];
                  try {
                    resourceKey = decodeURIComponent(resourceKey);
                  } catch (e) {
                    console.warn('⚠️ 解码resourceKey失败:', resourceKey);
                    conversionStats.错误数++;
                  }
                } else {
                  // 🔧 通用URL解析：从任何域名的/files/路径中提取resourceKey
                  let srcMatch = match.match(/src="https?:\/\/[^\/]+\/files\/([^"?]*)/);
                  if (srcMatch) {
                    resourceKey = `files/${decodeURIComponent(srcMatch[1])}`;
                    console.log('🔧 从/files/路径提取resourceKey:', resourceKey);
                  } else {
                    // 尝试从其他格式的签名URL中提取路径
                    srcMatch = match.match(/src="https?:\/\/[^\/]+\/([^"?]+)(\?[^"]*)?"/);
                    if (srcMatch && srcMatch[2] && (srcMatch[2].includes('signature=') || srcMatch[2].includes('key='))) {
                      resourceKey = decodeURIComponent(srcMatch[1]);
                      console.log('🔧 从签名URL提取resourceKey:', resourceKey);
                    }
                  }
                }
                
                if (resourceKey) {
                  // 重构img标签，使用resourceKey作为src，并确保有data-resource-key
                  let newTag = match.replace(
                    /src="[^"]*?"/gi,
                    `src="${resourceKey}"`
                  );
                  
                  // 如果原来没有data-resource-key，添加它
                  if (!match.includes('data-resource-key=')) {
                    newTag = newTag.replace('<img', `<img data-resource-key="${resourceKey}"`);
                  }
                  
                  conversionStats.图片转换数++;
                  console.log('🔄 图片URL转换成功:', { 
                    resourceKey,
                    hadDataResourceKey: match.includes('data-resource-key=')
                  });
                  
                  return newTag;
                } else {
                  console.warn('⚠️ 无法提取图片resourceKey:', match.substring(0, 100));
                  conversionStats.错误数++;
                }
              }
              return match;
            } catch (error) {
              console.error('❌ 图片转换失败:', error, match.substring(0, 100));
              conversionStats.错误数++;
              return match;
            }
          }
        );
      } catch (error) {
        console.error('❌ 图片批量转换失败:', error);
        conversionStats.错误数++;
      }
      
      // 2. 处理视频：将各种签名URL转换为资源路径
      // 首先处理完整的rich-text-video容器
      try {
        processed = processed.replace(
          /<div([^>]*class="[^"]*rich-text-video[^"]*"[^>]*)>(.*?)<\/div>/gis,
          (match, divAttrs, divContent) => {
            console.log('🔄 处理rich-text-video容器:', { divAttrs, divContent: divContent.substring(0, 200) });
            // 处理div容器内的video标签
            const updatedDivContent = divContent.replace(
              /<video([^>]*?)>(.*?)<\/video>/gis,
              (videoMatch, videoAttrs, videoContent) => {
                console.log('🔄 处理容器内的video标签:', { videoAttrs, videoContent: videoContent.substring(0, 100) });
                return this.processVideoTag(videoMatch, videoAttrs, videoContent);
              }
            );
            const result = `<div${divAttrs}>${updatedDivContent}</div>`;
            console.log('🔄 rich-text-video容器处理完成:', result.substring(0, 200));
            return result;
          }
        );
        conversionStats.视频转换数++;
      } catch (error) {
        console.error('❌ rich-text-video容器转换失败:', error);
        conversionStats.错误数++;
      }
      
      // 然后处理单独的video标签（没有rich-text-video容器的）
      try {
        processed = processed.replace(
          /<video([^>]*?)>(.*?)<\/video>/gis,
          (match, videoAttrs, videoContent) => {
            console.log('🔄 处理独立video标签:', { videoAttrs, videoContent: videoContent.substring(0, 100) });
            return this.processVideoTag(match, videoAttrs, videoContent);
          }
        );
      } catch (error) {
        console.error('❌ 独立video标签转换失败:', error);
        conversionStats.错误数++;
      }
      
      // 输出转换统计
      console.log('🔄 HTML转换完成:', { 
        hasWorkerUrls: html.includes('chigua-r2-worker.xingaikaka.workers.dev'),
        converted: processed !== html,
        originalLength: html.length,
        processedLength: processed.length,
        stats: conversionStats
      });
      
      // 如果有错误，给出警告
      if (conversionStats.错误数 > 0) {
        console.warn(`⚠️ 转换过程中发生 ${conversionStats.错误数} 个错误，请检查日志`);
      }
      
      // 如果转换后内容长度显著减少，给出警告
      if (html.length > 500 && processed.length < html.length * 0.7) {
        console.warn('⚠️ 转换后内容长度显著减少，可能存在问题:', {
          原始长度: html.length,
          转换后长度: processed.length,
          减少比例: ((html.length - processed.length) / html.length * 100).toFixed(1) + '%'
        });
      }
      
      return processed;
    },
    
    // 处理video标签的URL转换
    processVideoTag(match, videoAttrs, videoContent) {
      let hasChanges = false;
      
      try {
      
      // 🔧 转换video标签中的poster属性 - 直接转换为相对路径，让后端处理
      let newVideoAttrs = videoAttrs;
      
      // 通用poster URL转换 - 支持任何域名的/files/路径
      newVideoAttrs = newVideoAttrs.replace(
        /poster="https?:\/\/[^\/]+\/files\/[^"?]*(\?[^"]*)?"/gi,
        (posterMatch) => {
          hasChanges = true;
          const pathMatch = posterMatch.match(/\/files\/([^"?]*)/);
          if (pathMatch) {
            const resourceKey = `files/${decodeURIComponent(pathMatch[1])}`;
            console.log('🔄 转换poster URL:', { original: posterMatch, resourceKey });
            return `poster="${resourceKey}"`;
          }
          return posterMatch;
        }
      );
      
      // 其他域名的签名URL poster（如原来的khjghjghjjh.xyz等，现在统一为Worker域名）
      newVideoAttrs = newVideoAttrs.replace(
        /poster="https?:\/\/[^\/]+\/([^"?]+)(\?[^"]*)?"/gi,
        (posterMatch, path, query) => {
          if (query && (query.includes('signature=') || query.includes('key='))) {
            hasChanges = true;
            const resourceKey = decodeURIComponent(path);
            console.log('🔄 转换非Worker poster URL:', { original: posterMatch, resourceKey });
            return `poster="${resourceKey}"`;
          }
          return posterMatch;
        }
      );
      
      // 🔧 转换source标签中的src属性 - 直接转换为相对路径，让后端处理
      let newVideoContent = videoContent.replace(
        /<source([^>]*?)>/gi,
        (sourceMatch, sourceAttrs) => {
          let newSourceAttrs = sourceAttrs;
          
          // 通用source URL转换 - 支持任何域名的/files/路径
          newSourceAttrs = newSourceAttrs.replace(
            /src="https?:\/\/[^\/]+\/files\/[^"?]*(\?[^"]*)?"/gi,
            (srcMatch) => {
              hasChanges = true;
              console.log('🔍 检测到需要转换的source URL:', srcMatch);
              const pathMatch = srcMatch.match(/\/files\/([^"?]*)/);
              if (pathMatch) {
                const resourceKey = `files/${decodeURIComponent(pathMatch[1])}`;
                console.log('🔄 转换source URL:', { original: srcMatch, resourceKey });
                return `src="${resourceKey}"`;
              } else {
                console.warn('⚠️ 无法从URL中提取files路径:', srcMatch);
              }
              return srcMatch;
            }
          );
          
          // 其他域名的签名URL source（如原来的khjghjghjjh.xyz等，现在统一为Worker域名）
          newSourceAttrs = newSourceAttrs.replace(
            /src="https?:\/\/[^\/]+\/([^"?]+)(\?[^"]*)?"/gi,
            (srcMatch, path, query) => {
              if (query && (query.includes('signature=') || query.includes('key='))) {
                hasChanges = true;
                const resourceKey = decodeURIComponent(path);
                console.log('🔄 转换非Worker source URL:', { original: srcMatch, resourceKey });
                return `src="${resourceKey}"`;
              }
              return srcMatch;
            }
          );
          
          return `<source${newSourceAttrs}>`;
        }
      );
      
        if (hasChanges) {
          const newTag = `<video${newVideoAttrs}>${newVideoContent}</video>`;
          console.log('🔄 视频URL转换成功');
          return newTag;
        }
        
        return match;
        
      } catch (error) {
        console.error('❌ 视频标签转换失败:', error, match.substring(0, 100));
        return match; // 转换失败时返回原始标签
      }
    },
    
    // ========== 批量图片上传相关方法 ==========
    
    // 显示批量图片上传对话框
    showBatchImageUploadDialog(savedRange) {
      this.savedSelection = savedRange
      this.batchImageUploadVisible = true
      this.batchUploadTab = 'file'
      this.batchFileList = []
      this.batchImageUrls = ''
      this.batchUploadProgress = []
      this.batchUploadedImages = []
    },
    
    // 关闭批量图片上传对话框
    closeBatchImageUploadDialog() {
      this.batchImageUploadVisible = false
      this.batchFileList = []
      this.batchImageUrls = ''
      this.batchUploadProgress = []
      this.batchUploadedImages = []
      this.imagesSortList = []
      this.savedSelection = null
    },
    
    // 批量上传前验证
    handleBatchBeforeUpload(file) {
      const type = ["image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp", "image/svg+xml"]
      const isValidType = type.includes(file.type)
      
      if (!isValidType) {
        this.$message.error(`文件 ${file.name} 格式错误！只支持 jpg、jpeg、png、gif、bmp、webp、svg 格式`)
        return false
      }
      
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize
        if (!isLt) {
          this.$message.error(`文件 ${file.name} 大小超过限制！不能超过 ${this.fileSize} MB`)
          return false
        }
      }
      
      // 生成唯一ID
      const imageId = 'img_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
      
      // 创建图片预览
      const reader = new FileReader()
      reader.onload = (e) => {
        // 添加到排序列表
        this.imagesSortList.push({
          id: imageId,
          name: file.name,
          size: file.size,
          preview: e.target.result,
          status: 'uploading',
          percentage: 0,
          file: file
        })
      }
      reader.readAsDataURL(file)
      
      // 添加到进度跟踪
      this.batchUploadProgress.push({
        id: imageId,
        name: file.name,
        percentage: 0,
        status: 'uploading'
      })
      
      return true
    },
    
    // 批量上传进度
    handleBatchUploadProgress(event, file, fileList) {
      const progressIndex = this.batchUploadProgress.findIndex(item => item.name === file.name)
      if (progressIndex !== -1) {
        this.batchUploadProgress[progressIndex].percentage = Math.round(event.percent)
        
        // 同步更新排序列表中的进度
        const sortIndex = this.imagesSortList.findIndex(item => item.name === file.name)
        if (sortIndex !== -1) {
          this.imagesSortList[sortIndex].percentage = Math.round(event.percent)
        }
      }
    },
    
    // 批量上传成功
    handleBatchUploadSuccess(res, file) {
      const progressIndex = this.batchUploadProgress.findIndex(item => item.name === file.name)
      const sortIndex = this.imagesSortList.findIndex(item => item.name === file.name)
      
      if (res.success) {
        console.log('🖼️ 批量上传成功:', file.name, res)
        
        // 更新进度状态
        if (progressIndex !== -1) {
          this.batchUploadProgress[progressIndex].status = 'success'
          this.batchUploadProgress[progressIndex].percentage = 100
        }
        
        // 更新排序列表状态
        if (sortIndex !== -1) {
          this.imagesSortList[sortIndex].status = 'success'
          this.imagesSortList[sortIndex].percentage = 100
          this.imagesSortList[sortIndex].resourceKey = res.data.resourceKey
          this.imagesSortList[sortIndex].url = res.data.url
        }
        
        // 添加到成功列表（已移除，使用排序列表）
        // this.batchUploadedImages.push({
        //   name: file.name,
        //   resourceKey: res.data.resourceKey,
        //   url: res.data.url
        // })
        
        this.$message.success(`图片 ${file.name} 上传成功`)
      } else {
        console.error('❌ 批量上传失败:', file.name, res)
        
        // 更新进度状态
        if (progressIndex !== -1) {
          this.batchUploadProgress[progressIndex].status = 'error'
        }
        
        // 更新排序列表状态
        if (sortIndex !== -1) {
          this.imagesSortList[sortIndex].status = 'error'
        }
        
        this.$message.error(`图片 ${file.name} 上传失败: ${res.message || '未知错误'}`)
      }
    },
    
    // 批量上传失败
    handleBatchUploadError(err, file) {
      console.error('❌ 批量上传错误:', file.name, err)
      
      const progressIndex = this.batchUploadProgress.findIndex(item => item.name === file.name)
      if (progressIndex !== -1) {
        this.batchUploadProgress[progressIndex].status = 'error'
      }
      
      const sortIndex = this.imagesSortList.findIndex(item => item.name === file.name)
      if (sortIndex !== -1) {
        this.imagesSortList[sortIndex].status = 'error'
      }
      
      this.$message.error(`图片 ${file.name} 上传失败`)
    },
    
    // 插入URL图片
    insertImageFromUrl(imageUrl, savedRange) {
      const isLocalImage = imageUrl.includes(process.env.VUE_APP_BASE_API)
      
      if (savedRange) {
        // 使用保存的光标位置
        this.ensureEditableParagraphAt(savedRange.index)
        
        if (isLocalImage) {
          // 本服务器图片：转换为资源键格式，但保留原URL用于显示
          const resourceKey = this.extractResourceKeyFromUrl(imageUrl)
          const imgHtml = `<p><br></p><p><img src="${imageUrl}" data-resource-key="${resourceKey}" alt="图片" style="max-width: 100%; display: block; margin: 10px auto;"></p><p><br></p>`
          this.Quill.clipboard.dangerouslyPasteHTML(savedRange.index, imgHtml)
          this.Quill.setSelection(savedRange.index + 3)
        } else {
          // 外部图片：直接使用URL但确保前后有空行
          this.Quill.insertText(savedRange.index, '\n', 'user')
          this.Quill.insertEmbed(savedRange.index + 1, 'image', imageUrl)
          this.Quill.insertText(savedRange.index + 2, '\n\n', 'user')
          this.Quill.setSelection(savedRange.index + 4)
        }
      } else {
        // 如果没有保存的位置，插入到文档末尾
        const length = this.Quill.getLength()
        const insertPos = Math.max(0, length - 1)
        this.ensureEditableParagraphAt(insertPos)
        
        if (isLocalImage) {
          const resourceKey = this.extractResourceKeyFromUrl(imageUrl)
          const imgHtml = `<p><br></p><p><img src="${imageUrl}" data-resource-key="${resourceKey}" alt="图片" style="max-width: 100%; display: block; margin: 10px auto;"></p><p><br></p>`
          this.Quill.clipboard.dangerouslyPasteHTML(insertPos, imgHtml)
          const newLength = this.Quill.getLength()
          this.Quill.setSelection(newLength - 1)
        } else {
          this.Quill.insertText(insertPos, '\n', 'user')
          this.Quill.insertEmbed(insertPos + 1, 'image', imageUrl)
          this.Quill.insertText(insertPos + 2, '\n\n', 'user')
          const newLength = this.Quill.getLength()
          this.Quill.setSelection(newLength - 1)
        }
      }
      
      // 延迟初始化图片点击事件（防抖处理）
      this.debouncedInitImageClickHandlers()
    },
    
    // 插入批量图片
    insertBatchImages() {
      let imagesToInsert = []
      
      if (this.batchUploadTab === 'file') {
        // 文件上传模式：使用排序列表中成功上传的图片
        imagesToInsert = this.imagesSortList
          .filter(img => img.status === 'success' && img.url && img.resourceKey)
          .map(img => ({
            url: img.url,
            resourceKey: img.resourceKey,
            name: img.name
          }))
      } else {
        // URL模式：解析输入的URL
        const urls = this.batchImageUrls.split('\n')
          .map(url => url.trim())
          .filter(url => url && this.isImageUrl(url))
        
        imagesToInsert = urls.map((url, index) => ({
          url: url,
          resourceKey: url.includes(process.env.VUE_APP_BASE_API) ? this.extractResourceKeyFromUrl(url) : null,
          name: `图片${index + 1}`
        }))
      }
      
      if (imagesToInsert.length === 0) {
        this.$message.warning('没有可插入的图片')
        return
      }
      
      // 获取插入位置
      let insertIndex = 0
      if (this.savedSelection) {
        insertIndex = this.savedSelection.index
      } else {
        const currentSelection = this.Quill.getSelection()
        if (currentSelection) {
          insertIndex = currentSelection.index
        } else {
          insertIndex = Math.max(0, this.Quill.getLength() - 1)
        }
      }
      
      // 在开始插入前，确保当前位置有可编辑的段落
      this.ensureEditableParagraphAt(insertIndex)
      
      // 按排序后的顺序逐个插入图片
      imagesToInsert.forEach((img, index) => {
        const isLocalImage = img.resourceKey !== null
        
        // 在插入图片前，先插入一个空段落（如果需要）
        if (index === 0 && insertIndex === 0) {
          // 如果是第一张图片且在文档开头，先插入一个空段落
          this.Quill.insertText(insertIndex, '\n', 'user')
          insertIndex += 1
        }
        
        if (isLocalImage) {
          // 本服务器图片 - 使用更好的DOM结构
          const imgHtml = `<p><br></p><p><img src="${img.url}" data-resource-key="${img.resourceKey}" alt="${img.name}" style="max-width: 100%; display: block; margin: 10px auto;"></p><p><br></p>`
          this.Quill.clipboard.dangerouslyPasteHTML(insertIndex, imgHtml)
          insertIndex += 3 // 三个段落：空段落 + 图片段落 + 空段落
        } else {
          // 外部图片 - 确保前后有空行
          this.Quill.insertText(insertIndex, '\n', 'user')
          insertIndex += 1
          this.Quill.insertEmbed(insertIndex, 'image', img.url, 'user')
          insertIndex += 1
          this.Quill.insertText(insertIndex, '\n\n', 'user')
          insertIndex += 2
        }
      })
      
      // 设置光标到最后位置，并确保有可编辑区域
      this.ensureEditableParagraphAt(insertIndex)
      this.Quill.setSelection(insertIndex)
      
      // 延迟初始化点击事件处理（防抖处理）
      this.debouncedInitImageClickHandlers()
      
      this.$message.success(`成功按排序顺序插入 ${imagesToInsert.length} 张图片`)
      this.closeBatchImageUploadDialog()
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        uploading: '上传中',
        success: '成功',
        error: '失败'
      }
      return statusMap[status] || '未知'
    },
    
    // 获取选择的图片数量
    getSelectedImagesCount() {
      if (this.batchUploadTab === 'file') {
        return this.imagesSortList.filter(img => img.status === 'success').length
      } else {
        const urls = this.batchImageUrls.split('\n')
          .map(url => url.trim())
          .filter(url => url && this.isImageUrl(url))
        return urls.length
      }
    },
    
    // ========== 图片拖拽排序相关方法 ==========
    
    // 排序变化事件
    onImagesSortChanged(evt) {
      console.log('📋 图片排序已变化:', evt)
    },
    
    // 从排序列表中移除图片
    removeImageFromSort(index) {
      const image = this.imagesSortList[index]
      this.$confirm(`确定要移除图片 "${image.name}" 吗？`, '确认移除', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(() => {
        this.imagesSortList.splice(index, 1)
        this.$message.success('图片已移除')
      }).catch(() => {})
    },
    
    // 按名称排序
    sortImagesByName() {
      this.imagesSortList.sort((a, b) => a.name.localeCompare(b.name))
      this.$message.success('已按名称排序')
    },
    
    // 按大小排序
    sortImagesBySize() {
      this.imagesSortList.sort((a, b) => b.size - a.size)
      this.$message.success('已按大小排序（从大到小）')
    },
    
    // 反转顺序
    reverseImagesOrder() {
      this.imagesSortList.reverse()
      this.$message.success('顺序已反转')
    },
    
    // 清空所有图片
    clearAllImages() {
      this.$confirm('确定要清空所有图片吗？', '确认清空', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(() => {
        this.imagesSortList = []
        this.batchUploadProgress = []
        this.batchUploadedImages = []
        this.$message.success('已清空所有图片')
      }).catch(() => {})
    },
    
    // 格式化文件大小
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    // ========== 光标定位优化相关方法 ==========
    
    // 确保指定位置有可编辑的段落
    ensureEditableParagraphAt(index) {
      if (!this.Quill) return
      
      const length = this.Quill.getLength()
      if (index >= length) return
      
      // 检查当前位置的内容
      const currentChar = this.Quill.getText(index, 1)
      const prevChar = index > 0 ? this.Quill.getText(index - 1, 1) : ''
      const nextChar = index < length - 1 ? this.Quill.getText(index + 1, 1) : ''
      
      // 如果当前位置不是换行符且前后也没有换行符，则插入换行符
      if (currentChar !== '\n' && prevChar !== '\n') {
        this.Quill.insertText(index, '\n', 'user')
      }
    },
    
    // 防抖的图片初始化方法
    debouncedInitImageClickHandlers() {
      if (this.textChangeDebounceTimer) {
        clearTimeout(this.textChangeDebounceTimer)
      }
      this.textChangeDebounceTimer = setTimeout(() => {
        this.$nextTick(() => {
          this.initImageClickHandlers()
        })
      }, 200)
    },
    
    // 初始化图片点击事件处理
    initImageClickHandlers() {
      if (!this.Quill || !this.Quill.root) return
      
      // 移除旧的事件监听器
      this.removeImageClickHandlers()
      
      const images = this.Quill.root.querySelectorAll('img')
      
      // 防止重复初始化 - 检查图片是否已经有处理器
      const newImages = Array.from(images).filter(img => !img._clickHandler)
      
      if (newImages.length === 0) {
        return // 所有图片都已经初始化过了
      }
      
      newImages.forEach((img, index) => {
        // 为每个图片添加点击事件
        const clickHandler = (e) => {
          e.preventDefault()
          e.stopPropagation()
          
          // 获取图片在编辑器中的位置
          const imgPosition = this.getImagePosition(img)
          if (imgPosition !== -1) {
            // 根据点击位置决定光标放置
            const rect = img.getBoundingClientRect()
            const clickY = e.clientY
            const imgTop = rect.top
            const imgHeight = rect.height
            const clickRatio = (clickY - imgTop) / imgHeight
            
            let targetPosition
            if (clickRatio < 0.3) {
              // 点击图片上部，光标放在图片前
              targetPosition = Math.max(0, imgPosition - 1)
            } else {
              // 点击图片下部，光标放在图片后
              targetPosition = imgPosition + 1
            }
            
            // 确保目标位置有可编辑内容
            this.ensureEditableParagraphAt(targetPosition)
            
            // 设置光标位置
            this.Quill.setSelection(targetPosition)
            
            console.log(`🖼️ 图片${index + 1}点击，光标设置到位置: ${targetPosition}`)
          }
        }
        
        // 添加悬停效果提示
        const mouseenterHandler = () => {
          img.style.cursor = 'text'
          img.title = '点击图片上部在前面编辑，点击下部在后面编辑'
        }
        
        img.addEventListener('click', clickHandler)
        img.addEventListener('mouseenter', mouseenterHandler)
        
        // 保存事件处理器引用供清理使用
        img._clickHandler = clickHandler
        img._mouseenterHandler = mouseenterHandler
      })
      
      if (newImages.length > 0 && process.env.NODE_ENV === 'development') {
        console.log(`🖼️ 已为 ${newImages.length} 张图片初始化点击事件`)
      }
    },
    
    // 移除图片点击事件处理
    removeImageClickHandlers() {
      if (!this.Quill || !this.Quill.root) return
      
      const images = this.Quill.root.querySelectorAll('img')
      images.forEach(img => {
        if (img._clickHandler) {
          img.removeEventListener('click', img._clickHandler)
          delete img._clickHandler
        }
        if (img._mouseenterHandler) {
          img.removeEventListener('mouseenter', img._mouseenterHandler)
          delete img._mouseenterHandler
        }
      })
    },
    
    // 获取图片在编辑器中的位置
    getImagePosition(imgElement) {
      if (!this.Quill || !imgElement) return -1
      
      try {
        const blot = this.Quill.constructor.find(imgElement)
        if (blot) {
          return this.Quill.getIndex(blot)
        }
      } catch (error) {
        console.warn('获取图片位置失败:', error)
      }
      
      return -1
    }
  }
}
</script>

<style>
.editor, .ql-toolbar {
  white-space: pre-wrap !important;
  line-height: normal !important;
}
.quill-img {
  display: none;
}

/* 编辑器中的图片样式 */
.ql-editor img {
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

/* 编辑器中的视频样式 - 关键修改 */
.ql-editor video,
.ql-editor iframe {
  max-width: 100%;
  width: calc(100% - 20px); /* 减少更多宽度，确保两边有空间 */
  height: auto;
  min-height: 300px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin: 15px 10px; /* 左右留出更多空间 */
  display: block;
  background: #f5f5f5;
  position: relative;
}

/* 为iframe设置16:9的宽高比 */
.ql-editor iframe {
  aspect-ratio: 16/9;
}

/* 为video标签设置16:9的宽高比 */
.ql-editor video {
  aspect-ratio: 16/9;
}

/* 移除之前的伪元素方案 */
.ql-editor iframe::after,
.ql-editor video::after {
  display: none;
}

/* 确保编辑器有足够的底部空间 */
.ql-editor {
  padding-bottom: 50px !important;
}

/* 编辑器中的段落确保有最小高度 */
.ql-editor p {
  min-height: 1.5em;
  line-height: 1.5;
}

/* 确保空段落也能被点击 */
.ql-editor p:empty::before {
  content: '\200B'; /* 零宽空格 */
  display: inline-block;
}

/* 强制在媒体元素周围创建可点击区域 */
.ql-editor img,
.ql-editor video,
.ql-editor iframe {
  border: 1px solid transparent; /* 创建边界 */
}

/* 媒体元素容器 - 确保周围有可编辑空间 */
.ql-editor p:has(img),
.ql-editor p:has(video), 
.ql-editor p:has(iframe) {
  padding: 10px 0;
  position: relative;
}

/* 为不支持:has的浏览器提供备选方案 */
.ql-editor img,
.ql-editor video,
.ql-editor iframe {
  margin: 20px auto;
  display: block;
  position: relative;
}

/* 为媒体元素添加下方可点击区域 */
.ql-editor img::after,
.ql-editor video::after,
.ql-editor iframe::after {
  content: '';
  display: block;
  width: 100%;
  height: 30px;
  position: absolute;
  bottom: -30px;
  left: 0;
  cursor: text;
  background: transparent;
  pointer-events: auto;
}

/* 视频和图片的hover效果 */
.ql-editor iframe:hover,
.ql-editor video:hover,
.ql-editor img:hover {
  opacity: 0.9;
  border-color: #409eff;
}

/* 在编辑模式下禁用视频播放但允许选择 */
.ql-editor iframe {
  pointer-events: none;
}

/* 富文本编辑器整体布局 */
.ql-editor {
  max-width: 800px !important;
  margin: 0 auto !important;
  padding: 20px !important;
}

/* 富文本段落样式 - 与图片视频对齐 */
.ql-editor p,
.ql-editor div,
.ql-editor h1,
.ql-editor h2,
.ql-editor h3,
.ql-editor h4,
.ql-editor h5,
.ql-editor h6,
.ql-editor ul,
.ql-editor ol,
.ql-editor blockquote {
  max-width: 800px !important;
  margin-left: auto !important;
  margin-right: auto !important;
  word-wrap: break-word !important;
  text-align: left !important;
}

/* 段落间距优化 */
.ql-editor p {
  margin: 12px auto !important;
  line-height: 1.6 !important;
}

/* 标题样式优化 */
.ql-editor h1,
.ql-editor h2,
.ql-editor h3,
.ql-editor h4,
.ql-editor h5,
.ql-editor h6 {
  margin: 20px auto 12px auto !important;
  line-height: 1.4 !important;
  font-weight: 600 !important;
}

/* 列表样式优化 */
.ql-editor ul,
.ql-editor ol {
  margin: 12px auto !important;
  padding-left: 20px !important;
}

/* 引用样式优化 */
.ql-editor blockquote {
  margin: 20px auto !important;
  padding: 15px 20px !important;
  border-left: 4px solid #409eff !important;
  background: #f0f9ff !important;
  font-style: italic !important;
}

/* 富文本图片样式 - 统一大小和布局 */
.ql-editor img {
  pointer-events: auto;
  max-width: 100% !important;
  width: auto !important;
  height: auto !important;
  display: block !important;
  margin: 20px auto !important;
  border-radius: 8px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease !important;
}

/* 图片悬停效果 */
.ql-editor img:hover {
  transform: scale(1.02) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
  cursor: pointer !important;
}

/* 图片容器 - 确保图片在合适的容器中显示 */
.ql-editor p img,
.ql-editor div img {
  max-width: 800px !important;
  margin: 20px auto !important;
  display: block !important;
}

/* iframe视频样式 - 统一外观和尺寸 */
.ql-editor iframe {
  max-width: 800px !important;
  width: 100% !important;
  aspect-ratio: 16/9 !important;
  min-height: 300px !important;
  margin: 20px auto !important;
  display: block !important;
  border: none !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  background: #000 !important;
  outline: none !important;
  pointer-events: none !important;
}

/* 兼容不支持aspect-ratio的浏览器 */
@supports not (aspect-ratio: 16/9) {
  .ql-editor iframe,
  .ql-editor .rich-text-video video,
  .ql-editor video {
    height: 56.25vw; /* 16:9 比例 */
    max-height: 450px;
  }
}

/* 特定视频平台的iframe样式优化 */
.ql-editor iframe[src*="youtube"],
.ql-editor iframe[src*="youtu.be"],
.ql-editor iframe[src*="vimeo"],
.ql-editor iframe[src*="bilibili"] {
  border: none !important;
  outline: none !important;
  border-radius: 12px !important;
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .ql-editor {
    max-width: 100% !important;
    padding: 15px !important;
  }
  
  .ql-editor p,
  .ql-editor div,
  .ql-editor h1,
  .ql-editor h2,
  .ql-editor h3,
  .ql-editor h4,
  .ql-editor h5,
  .ql-editor h6,
  .ql-editor ul,
  .ql-editor ol,
  .ql-editor blockquote {
    max-width: 100% !important;
    padding: 0 10px !important;
  }
  
  .ql-editor img {
    max-width: 100% !important;
    margin: 15px auto !important;
    border-radius: 6px !important;
  }
  
  .ql-editor iframe,
  .ql-editor video,
  .ql-editor .rich-text-video video {
    min-height: 220px !important;
    border-radius: 8px !important;
    margin: 15px auto !important;
  }
  
  .ql-editor .rich-text-video {
    margin: 15px auto !important;
    padding: 12px !important;
    border-radius: 8px !important;
  }
  
  @supports not (aspect-ratio: 16/9) {
    .ql-editor iframe,
    .ql-editor video,
    .ql-editor .rich-text-video video {
      height: 56.25vw !important;
      max-height: 280px !important;
    }
  }
}

/* 小屏设备进一步优化 */
@media (max-width: 480px) {
  .ql-editor {
    padding: 12px !important;
  }
  
  .ql-editor p,
  .ql-editor div,
  .ql-editor h1,
  .ql-editor h2,
  .ql-editor h3,
  .ql-editor h4,
  .ql-editor h5,
  .ql-editor h6,
  .ql-editor ul,
  .ql-editor ol,
  .ql-editor blockquote {
    padding: 0 8px !important;
  }
  
  .ql-editor img {
    margin: 12px auto !important;
    border-radius: 4px !important;
  }
  
  .ql-editor iframe,
  .ql-editor video,
  .ql-editor .rich-text-video video {
    min-height: 180px !important;
    margin: 12px auto !important;
    border-radius: 6px !important;
  }
  
  .ql-editor .rich-text-video {
    margin: 12px auto !important;
    padding: 10px !important;
    border-radius: 6px !important;
  }
}

.ql-snow .ql-tooltip[data-mode="link"]::before {
  content: "请输入链接地址:";
}
.ql-snow .ql-tooltip.ql-editing a.ql-action::after {
  border-right: 0px;
  content: "保存";
  padding-right: 0px;
}
.ql-snow .ql-tooltip[data-mode="video"]::before {
  content: "请输入视频地址:";
}
.ql-snow .ql-picker.ql-size .ql-picker-label::before,
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  content: "14px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="small"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="small"]::before {
  content: "10px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="large"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="large"]::before {
  content: "18px";
}

/* 富文本视频容器样式 - 与图片保持一致 */
.ql-editor .rich-text-video {
  margin: 20px auto !important;
  padding: 15px !important;
  max-width: 800px !important;
  border: 1px solid #e4e7ed !important;
  border-radius: 12px !important;
  background: linear-gradient(145deg, #f9f9f9, #ffffff) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  display: block !important;
  clear: both !important;
  position: relative !important;
  transition: box-shadow 0.3s ease !important;
}

.ql-editor .rich-text-video:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
}

/* 视频播放器样式 - 统一尺寸和外观 */
.ql-editor .rich-text-video video,
.ql-editor video {
  max-width: 100% !important;
  width: 100% !important;
  height: auto !important;
  aspect-ratio: 16/9 !important;
  min-height: 300px !important;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  background: #000 !important;
  border-radius: 8px !important;
  margin: 0 0 12px 0 !important;
  position: relative !important;
  z-index: 1 !important;
  outline: none !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2) !important;
}

/* 直接插入的video标签样式 */
.ql-editor > video,
.ql-editor p video,
.ql-editor div video {
  margin: 20px auto !important;
  max-width: 800px !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}

/* 确保video标签的controls属性生效 */
.ql-editor .rich-text-video video[controls],
.ql-editor video[controls] {
  display: block !important;
}

/* 强制显示video元素，覆盖任何可能的隐藏样式 */
.ql-editor video {
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  position: relative !important;
}

/* 视频信息样式优化 */
.ql-editor .rich-text-video .video-info {
  margin-top: 12px !important;
  padding: 12px 16px !important;
  background: linear-gradient(145deg, #f5f7fa, #e8ecf0) !important;
  border-radius: 8px !important;
  border: 1px solid #e1e4e8 !important;
  font-size: 13px !important;
  color: #586069 !important;
  display: block !important;
  line-height: 1.5 !important;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05) !important;
}

.ql-editor .rich-text-video .video-info p {
  margin: 4px 0 !important;
  line-height: 1.6 !important;
}

.ql-editor .rich-text-video .video-info strong {
  font-weight: 600 !important;
  color: #24292e !important;
}

.ql-editor .rich-text-video .video-info:first-child {
  margin-top: 8px !important;
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="huge"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="huge"]::before {
  content: "32px";
}
.ql-snow .ql-picker.ql-header .ql-picker-label::before,
.ql-snow .ql-picker.ql-header .ql-picker-item::before {
  content: "文本";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="1"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="1"]::before {
  content: "标题1";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="2"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="2"]::before {
  content: "标题2";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="3"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="3"]::before {
  content: "标题3";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="4"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="4"]::before {
  content: "标题4";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="5"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="5"]::before {
  content: "标题5";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="6"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="6"]::before {
  content: "标题6";
}
.ql-snow .ql-picker.ql-font .ql-picker-label::before,
.ql-snow .ql-picker.ql-font .ql-picker-item::before {
  content: "标准字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="serif"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="serif"]::before {
  content: "衬线字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="monospace"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="monospace"]::before {
  content: "等宽字体";
}

/* 批量图片上传对话框样式 */
.batch-upload-container {
  min-height: 400px;
}

.upload-area {
  padding: 20px;
}

.upload-area .el-upload-dragger {
  width: 100%;
  height: 180px;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  text-align: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.upload-area .el-upload-dragger:hover {
  border-color: #409eff;
}

.upload-area .el-upload-dragger .el-icon-upload {
  font-size: 67px;
  color: #c0c4cc;
  margin: 40px 0 16px;
  line-height: 50px;
}

.url-input-area {
  padding: 20px;
}

.url-tip {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
}

.url-tip .el-icon-info {
  margin-right: 5px;
}

.upload-progress {
  margin-top: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.upload-progress h4 {
  margin: 0 0 15px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.progress-item {
  margin-bottom: 15px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.filename {
  font-size: 13px;
  color: #606266;
  flex: 1;
  margin-right: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.status.uploading {
  background-color: #e6f7ff;
  color: #1890ff;
}

.status.success {
  background-color: #f6ffed;
  color: #52c41a;
}

.status.error {
  background-color: #fff2f0;
  color: #f5222d;
}

/* 自定义对话框样式 */
.image-insert-dialog .el-message-box__message {
  color: #606266;
  margin: 0;
}

.image-insert-dialog .el-message-box__content {
  padding: 20px 24px;
}

.image-insert-dialog .el-message-box__btns {
  padding: 10px 24px 20px;
}

/* 图片拖拽排序样式 */
.images-sort-area {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(145deg, #f8f9fa, #e9ecef);
  border-radius: 12px;
  border: 1px solid #dee2e6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.sort-title {
  margin: 0 0 15px 0;
  color: #495057;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-title .el-icon-picture-outline {
  color: #007bff;
  font-size: 18px;
}

.sort-tip {
  margin-left: auto;
  font-size: 12px;
  color: #6c757d;
  font-weight: normal;
  background: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.images-sort-list {
  min-height: 100px;
}

.sort-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
  padding: 10px 0;
}

.sort-item {
  background: white;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.3s ease;
  cursor: move;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.sort-item:hover {
  border-color: #007bff;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.15);
  transform: translateY(-2px);
}

.sort-item-error {
  border-color: #dc3545;
  background: #fff5f5;
}

.sort-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.sort-index {
  background: #007bff;
  color: white;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 12px;
  min-width: 24px;
  text-align: center;
}

.sort-status {
  font-size: 16px;
  margin-left: auto;
  margin-right: 8px;
}

.sort-status.uploading {
  color: #007bff;
}

.sort-status.success {
  color: #28a745;
}

.sort-status.error {
  color: #dc3545;
}

.remove-btn {
  padding: 4px !important;
  min-height: auto !important;
  color: #dc3545 !important;
}

.remove-btn:hover {
  background: #f8d7da !important;
}

.sort-item-preview {
  width: 100%;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  border: 1px solid #e9ecef;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.preview-image:hover {
  transform: scale(1.05);
}

.preview-placeholder {
  color: #6c757d;
  font-size: 24px;
}

.sort-item-info {
  text-align: center;
}

.image-name {
  font-size: 13px;
  color: #495057;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-size {
  font-size: 11px;
  color: #6c757d;
  margin-bottom: 8px;
}

.upload-progress-mini {
  margin: 0;
}

.sort-actions {
  margin-top: 15px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.sort-actions .el-button {
  font-size: 12px;
  padding: 8px 12px;
}

/* 拖拽状态样式 */
.ghost {
  opacity: 0.5;
  background: #e3f2fd;
  border: 2px dashed #2196f3;
}

.drag {
  transform: rotate(5deg);
  z-index: 1000;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

/* 动画效果 */
.flip-list-move {
  transition: transform 0.3s;
}

.flip-list-enter-active,
.flip-list-leave-active {
  transition: all 0.3s;
}

.flip-list-enter,
.flip-list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .batch-upload-container {
    min-height: auto;
  }
  
  .upload-area {
    padding: 15px;
  }
  
  .upload-area .el-upload-dragger {
    height: 120px;
  }
  
  .upload-area .el-upload-dragger .el-icon-upload {
    font-size: 45px;
    margin: 20px 0 10px;
  }
  
  .url-input-area {
    padding: 15px;
  }
  
  .upload-progress {
    padding: 15px;
    max-height: 200px;
  }
  
  .images-sort-area {
    padding: 15px;
    margin-top: 15px;
  }
  
  .sort-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 10px;
  }
  
  .sort-item {
    padding: 10px;
  }
  
  .sort-item-preview {
    height: 100px;
  }
  
  .sort-actions {
    gap: 6px;
  }
  
  .sort-actions .el-button {
    font-size: 11px;
    padding: 6px 10px;
  }
}

@media (max-width: 480px) {
  .sort-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }
  
  .sort-item {
    padding: 8px;
  }
  
  .sort-item-preview {
    height: 80px;
  }
  
  .sort-title {
    font-size: 14px;
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
  
  .sort-tip {
    margin-left: 0;
    align-self: flex-end;
  }
}
</style>
