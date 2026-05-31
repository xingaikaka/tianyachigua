<template>
  <div class="triple-cover-composer">
    <div class="uploader">
      <el-upload
        action="#"
        :auto-upload="false"
        :multiple="true"
        :limit="3"
        :file-list="[]"
        accept="image/jpeg,image/png,image/webp"
        :on-change="handleFileSelect"
        :on-exceed="onExceed"
        list-type="text">
        <el-button size="small" type="primary" icon="el-icon-upload">选择图片</el-button>
        <span class="hint">（支持1-3张图片，单张图片直接上传，多张图片可拖拽排序后合成）</span>
      </el-upload>
      
      <!-- 现有封面预览（编辑模式，未开始新上传时显示） -->
      <div v-if="!showStage && existingSignedUrl" class="existing-cover-preview">
        <div class="existing-cover-label">
          <i class="el-icon-picture"></i>
          当前封面（重新选择图片将覆盖）
        </div>
        <div class="existing-cover-img-wrap">
          <img :src="existingSignedUrl" class="existing-cover-img" />
        </div>
      </div>

      <!-- 初始状态提示（无现有封面时才显示） -->
      <div v-if="!showStage && !sortDialogVisible && !existingSignedUrl" class="upload-hint">
        <i class="el-icon-info"></i>
        <span>请点击上方按钮选择1-3张图片开始制作封面</span>
      </div>
    </div>

    <!-- 图片排序弹出层 -->
    <el-dialog
      title="图片排序"
      :visible.sync="sortDialogVisible"
      width="600px"
      :close-on-click-modal="false"
      append-to-body>
      <div class="sort-container">
        <div class="sort-hint">
          <i class="el-icon-info"></i>
          <span>拖拽图片调整顺序，确定后将按此顺序合成封面</span>
        </div>
        <div class="image-sort-list">
          <div
            v-for="(image, index) in selectedImages"
            :key="image.id"
            class="sort-item"
            :class="{ 'dragging': draggingIndex === index, 'drag-over': dragOverIndex === index }"
            draggable="true"
            @dragstart="handleSortDragStart($event, index)"
            @dragend="handleSortDragEnd"
            @dragover.prevent="handleSortDragOver($event, index)"
            @drop.prevent="handleSortDrop($event, index)"
            @dragenter.prevent="handleSortDragEnter($event, index)"
            @dragleave.prevent="handleSortDragLeave($event, index)">
            <div class="sort-number">{{ index + 1 }}</div>
            <div class="sort-image">
              <img :src="image.url" alt="图片预览" />
            </div>
            <div class="sort-info">
              <div class="image-name">{{ image.name }}</div>
              <div class="image-size">{{ formatFileSize(image.size) }}</div>
            </div>
            <div class="sort-actions">
              <el-button size="mini" @click="removeImage(index)" icon="el-icon-delete" type="danger">删除</el-button>
            </div>
          </div>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelSort">取消</el-button>
        <el-button type="primary" @click="confirmSort">确定</el-button>
      </div>
    </el-dialog>

    <!-- 合成区域 -->
    <div v-if="showStage" class="stage" :style="{width: targetWidth + 'px', height: targetHeight + 'px'}" @mouseleave="endDrag">
      <div class="stage-header">
        <span class="stage-title">封面合成区域（不可调整顺序）</span>
        <el-button size="mini" @click="resetImages" type="warning">重新选择图片</el-button>
      </div>
      <div
        v-for="(slot, idx) in actualSlotCount"
        :key="'slot-'+idx"
        class="slot"
        :class="{ active: activeIndex === idx }"
        :style="slotStyle(idx)"
        @mousedown="(e)=>startDrag(e, idx)"
        @mousemove="(e)=>onDrag(e, idx)"
        @mouseup="endDrag"
      >
        <template v-if="items[idx] && items[idx].url">
          <img
            :src="items[idx].url"
            class="slot-img"
            :style="imgStyle(idx)"
            draggable="false"
            @load="(e)=>onImgLoad(e, idx)"
          />
        </template>
        <template v-else>
          <div class="slot-placeholder">
            <div class="empty-slot-hint">
              <i class="el-icon-picture-outline"></i>
              <span>第{{ idx+1 }}张图片</span>
            </div>
          </div>
        </template>
        <div class="slot-tools">
          <el-button size="mini" @click.stop="zoom(idx, 0.1)">放大</el-button>
          <el-button size="mini" @click.stop="zoom(idx, -0.1)">缩小</el-button>
          <el-button size="mini" @click.stop="reset(idx)">重置</el-button>
        </div>
      </div>
      <div class="slot-guides">
        <div class="guide" :style="{left: segmentWidths[0] + 'px'}"></div>
        <div class="guide" :style="{left: (segmentWidths[0]+segmentWidths[1]) + 'px'}"></div>
      </div>
    </div>

    <!-- 操作按钮区域 -->
    <div v-if="showStage" class="actions">
      <div class="action-buttons">
        <el-button 
          v-if="actualSlotCount === 1" 
          type="primary" 
          size="medium"
          :disabled="!readyToUploadSingle || composing" 
          :loading="composing" 
          @click="uploadSingleImage"
          icon="el-icon-upload">
          直接上传图片
        </el-button>
        <el-button 
          v-else
          type="primary" 
          size="medium"
          :disabled="!readyToCompose || composing" 
          :loading="composing" 
          @click="composeAndUpload"
          icon="el-icon-picture-outline">
          生成并上传封面
        </el-button>
        
        <!-- 状态提示 -->
        <div v-if="actualSlotCount === 1 && !readyToUploadSingle" class="status-hint">
          <i class="el-icon-warning"></i>
          <span>请等待图片加载完成</span>
        </div>
        <div v-else-if="actualSlotCount > 1 && !readyToCompose" class="status-hint">
          <i class="el-icon-warning"></i>
          <span>请等待所有图片加载完成</span>
        </div>
      </div>
      
      <div v-if="previewUrl" class="preview-section">
        <span class="preview-label">预览：</span>
        <a :href="previewUrl" target="_blank" class="preview-link">查看封面</a>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'TripleCoverComposer',
  components: {},
  props: {
    value: [String, null],
    action: {
      type: String,
      default: 'https://chigua-r2-worker.xingaikaka.workers.dev/upload'
    },
    /** 现有封面的 CDN 签名 URL（编辑时由父级传入，用于解密预览） */
    existingSignedUrl: {
      type: String,
      default: ''
    },
    outputType: {
      type: String,
      default: 'jpeg'
    },
    quality: {
      type: Number,
      default: 0.82
    },
    // 是否叠加水印
    watermark: {
      type: Boolean,
      default: false
    },
    // 水印图片地址（请将 shuiyin.png 放到 admin 前端 public 根目录）
    watermarkSrc: {
      type: String,
      default: '/shuiyin.png'
    }
  },
  data() {
    return {
      items: [null, null, null],
      dragging: false,
      dragStart: { x: 0, y: 0 },
      startOffset: { x: 0, y: 0 },
      activeIndex: -1,
      composing: false,
      previewUrl: '',
      targetWidth: 960,
      targetHeight: 540,
      segmentWidths: [320, 320, 320],
      minScales: [1, 1, 1],
      wmImage: null,
      
      // 图片选择和排序相关
      sortDialogVisible: false,
      selectedImages: [],
      showStage: false,
      actualSlotCount: 3,
      
      // 拖拽排序相关
      draggingIndex: -1,
      dragOverIndex: -1,

    }
  },
  computed: {
    readyToCompose() {
      if (this.actualSlotCount === 1) return false
      const ready = this.items.slice(0, this.actualSlotCount).every(it => it && it.image)
      console.log('readyToCompose:', { actualSlotCount: this.actualSlotCount, ready, items: this.items.slice(0, this.actualSlotCount) })
      return ready
    },
    
    readyToUploadSingle() {
      const ready = this.actualSlotCount === 1 && this.items[0] && this.items[0].image
      console.log('readyToUploadSingle:', { actualSlotCount: this.actualSlotCount, ready, item0: this.items[0] })
      return ready
    }
  },
  methods: {
    onExceed() {
      this.$message.error('最多只能选择3张图片')
    },
    
    handleFileSelect(file, fileList) {
      const files = fileList.slice(0, 3).map(f => f.raw || f)
      
      if (files.length === 0) return
      
      // 如果只有一张图片，直接上传
      if (files.length === 1) {
        this.handleSingleImage(files[0])
        return
      }
      
      // 多张图片，显示排序对话框
      this.selectedImages = files.map((file, index) => ({
        id: Date.now() + index,
        file: file,
        name: file.name,
        size: file.size,
        url: ''
      }))
      
      // 生成预览URL
      const tasks = this.selectedImages.map(img => 
        this.readFileAsDataUrl(img.file).then(url => {
          img.url = url
          return img
        })
      )
      
      Promise.all(tasks).then(() => {
        this.sortDialogVisible = true
      })
    },
    
    // 处理单张图片
    async handleSingleImage(file) {
      try {
        const url = await this.readFileAsDataUrl(file)
        this.actualSlotCount = 1
        this.targetWidth = 960
        this.targetHeight = 540
        this.segmentWidths = [960]
        
        const item = this.buildItem(url)
        item.file = file // 保存原始文件用于上传
        this.$set(this.items, 0, item)
        this.$set(this.items, 1, null)
        this.$set(this.items, 2, null)
        
        this.showStage = true
        this.$nextTick(() => {
          this.initItemScale(0)
        })
      } catch (error) {
        this.$message.error('图片加载失败')
      }
    },
    readFileAsDataUrl(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = reject
        reader.readAsDataURL(file)
      })
    },
    zoom(idx, delta) {
      const item = this.items[idx]
      if (!item) return
      const minScale = this.minScales[idx] || 1
      const newScale = Math.max(minScale, item.scale + delta)
      const cx = this.segmentWidths[idx] / 2
      const cy = this.targetHeight / 2
      // 调整位移以尽量保持中心不变
      const k = newScale / item.scale
      item.tx = cx - (cx - item.tx) * k
      item.ty = cy - (cy - item.ty) * k
      item.scale = newScale
      this.clampPosition(idx)
    },
    reset(idx) {
      const item = this.items[idx]
      if (!item) return
      item.scale = this.minScales[idx]
      // 居中
      item.tx = (this.segmentWidths[idx] - item.nw * item.scale) / 2
      item.ty = (this.targetHeight - item.nh * item.scale) / 2
      this.clampPosition(idx)
    },
    async composeCanvas() {
      const canvas = document.createElement('canvas')
      canvas.width = this.targetWidth
      canvas.height = this.targetHeight
      const ctx = canvas.getContext('2d')

      for (let i = 0; i < 3; i++) {
        const item = this.items[i]
        if (!item || !item.image) continue
        const dx0 = this.segmentWidths.slice(0, i).reduce((a, b) => a + b, 0)
        const panelW = this.segmentWidths[i]
        const panelH = this.targetHeight

        // 计算与源图对应的裁剪区域
        const iw = item.nw, ih = item.nh
        const iwS = iw * item.scale, ihS = ih * item.scale
        const tx = item.tx, ty = item.ty

        const visX0 = Math.max(0, tx)
        const visY0 = Math.max(0, ty)
        const visX1 = Math.min(panelW, tx + iwS)
        const visY1 = Math.min(panelH, ty + ihS)
        const visW = Math.max(0, visX1 - visX0)
        const visH = Math.max(0, visY1 - visY0)
        if (visW <= 0 || visH <= 0) continue

        const sx = Math.max(0, -tx) / item.scale
        const sy = Math.max(0, -ty) / item.scale
        const sw = visW / item.scale
        const sh = visH / item.scale

        ctx.drawImage(
          item.image,
          sx, sy, sw, sh,
          dx0 + visX0, visY0,
          visW, visH
        )
      }

      // 叠加水印（每个分区右下角，使用水印原始尺寸，不透明）
      if (this.watermark) {
        const wm = await this.ensureWatermark()
        if (wm) {
          for (let i = 0; i < 3; i++) {
            if (!this.items[i]) continue
            const dx0 = this.segmentWidths.slice(0, i).reduce((a, b) => a + b, 0)
            const panelW = this.segmentWidths[i]
            const panelH = this.targetHeight
            const dx = dx0 + Math.max(0, panelW - wm.width - 10)
            const dy = Math.max(0, panelH - wm.height - 10)
            ctx.globalAlpha = 1
            ctx.drawImage(wm, dx, dy)
          }
        }
      }

      return canvas
    },
    async composeAndUpload() {
      if (!this.readyToCompose) {
        this.$message.error('请先选择3张图片并调整裁剪')
        return
      }
      this.composing = true
      try {
        const canvas = await this.composeCanvas()
        const mime = this.supportsWebp() ? 'image/webp' : 'image/jpeg'
        const blob = await new Promise(resolve => canvas.toBlob(resolve, mime, this.quality))
        const fileName = `cover_${Date.now()}.${mime.includes('webp') ? 'webp' : 'jpg'}`
        const formData = new FormData()
        formData.append('file', blob, fileName)

        const res = await axios.post(this.action, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

        let resourceKey = ''
        let previewUrl = ''
        if (res.data && res.data.success) {
          if (res.data.data && res.data.data.resourceKey) {
            resourceKey = res.data.data.resourceKey
            previewUrl = res.data.data.url || ''
          } else if (res.data.key) {
            resourceKey = res.data.key
            previewUrl = res.data.url || ''
          }
        }
        if (!resourceKey) {
          this.$message.error('上传失败：返回数据不包含资源键')
          return
        }

        this.previewUrl = previewUrl || this.buildWorkerPreviewUrl(resourceKey)
        this.$emit('input', resourceKey)
        this.$message.success('封面已生成并上传')
      } catch (e) {
        console.error(e)
        this.$message.error('生成或上传失败')
      } finally {
        this.composing = false
      }
    },
    buildItem(url) {
      return { url, image: null, nw: 0, nh: 0, scale: 1, tx: 0, ty: 0 }
    },
    onImgLoad(e, idx) {
      const el = e.target
      const item = this.items[idx]
      if (!item) return
      item.image = el
      item.nw = el.naturalWidth
      item.nh = el.naturalHeight
      this.initItemScale(idx)
    },
    initItemScale(idx) {
      const item = this.items[idx]
      if (!item || !item.nw || !item.nh) return
      const minScale = Math.max(this.segmentWidths[idx] / item.nw, this.targetHeight / item.nh)
      this.$set(this.minScales, idx, minScale)
      item.scale = minScale
      item.tx = (this.segmentWidths[idx] - item.nw * minScale) / 2
      item.ty = (this.targetHeight - item.nh * minScale) / 2
      this.clampPosition(idx)
    },
    clampPosition(idx) {
      const item = this.items[idx]
      if (!item) return
      const panelW = this.segmentWidths[idx]
      const panelH = this.targetHeight
      const iwS = item.nw * item.scale
      const ihS = item.nh * item.scale
      const minX = Math.min(0, panelW - iwS)
      const maxX = Math.max(0, panelW - iwS)
      const minY = Math.min(0, panelH - ihS)
      const maxY = Math.max(0, panelH - ihS)
      item.tx = Math.min(Math.max(item.tx, minX), maxX)
      item.ty = Math.min(Math.max(item.ty, minY), maxY)
    },
    startDrag(e, idx) {
      if (!this.items[idx]) return
      this.activeIndex = idx
      this.dragging = true
      this.dragStart = { x: e.clientX, y: e.clientY }
      this.startOffset = { x: this.items[idx].tx, y: this.items[idx].ty }
    },
    onDrag(e, idx) {
      if (!this.dragging || this.activeIndex !== idx) return
      const dx = e.clientX - this.dragStart.x
      const dy = e.clientY - this.dragStart.y
      const item = this.items[idx]
      item.tx = this.startOffset.x + dx
      item.ty = this.startOffset.y + dy
      this.clampPosition(idx)
    },
    endDrag() {
      this.dragging = false
    },
    supportsWebp() {
      try {
        const canvas = document.createElement('canvas')
        return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0
      } catch (_) {
        return false
      }
    },
    buildWorkerPreviewUrl(key) {
      if (!key) return ''
      if (key.startsWith('http')) return key
      return 'https://chigua-r2-worker.xingaikaka.workers.dev/files/' + encodeURIComponent(key)
    },
    async ensureWatermark() {
      if (this.wmImage) return this.wmImage
      try {
        const img = await this.loadWatermark(this.watermarkSrc)
        this.wmImage = img
        return img
      } catch (e) {
        console.warn('加载水印失败', e)
        return null
      }
    },
    loadWatermark(src) {
      return new Promise((resolve, reject) => {
        const img = new Image()
        img.onload = () => resolve(img)
        img.onerror = reject
        img.src = src
      })
    },
    slotLeft(idx) {
      if (idx === 0) return 0
      if (idx === 1) return this.segmentWidths[0]
      return this.segmentWidths[0] + this.segmentWidths[1]
    },
    slotStyle(idx) {
      return {
        left: this.slotLeft(idx) + 'px',
        width: this.segmentWidths[idx] + 'px',
        height: this.targetHeight + 'px'
      }
    },
    imgStyle(idx) {
      const item = this.items[idx]
      if (!item) return {}
      return {
        transform: `translate(${item.tx}px, ${item.ty}px) scale(${item.scale})`,
        transformOrigin: 'top left'
      }
    },
    
    // ============= 图片排序相关方法 =============
    
    // 格式化文件大小
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    // 开始拖拽排序
    handleSortDragStart(e, index) {
      this.draggingIndex = index
      e.dataTransfer.setData('text/plain', index.toString())
      e.dataTransfer.effectAllowed = 'move'
    },
    
    // 结束拖拽排序
    handleSortDragEnd() {
      this.draggingIndex = -1
      this.dragOverIndex = -1
    },
    
    // 拖拽进入
    handleSortDragEnter(e, index) {
      if (this.draggingIndex === -1) return
      this.dragOverIndex = index
    },
    
    // 拖拽离开
    handleSortDragLeave(e, index) {
      // 检查是否真的离开了目标区域
      const rect = e.currentTarget.getBoundingClientRect()
      const x = e.clientX
      const y = e.clientY
      
      if (x < rect.left || x > rect.right || y < rect.top || y > rect.bottom) {
        if (this.dragOverIndex === index) {
          this.dragOverIndex = -1
        }
      }
    },
    
    // 拖拽悬停
    handleSortDragOver(e, index) {
      if (this.draggingIndex === -1) return
      e.preventDefault()
      e.dataTransfer.dropEffect = 'move'
      this.dragOverIndex = index
    },
    
    // 放置
    handleSortDrop(e, targetIndex) {
      if (this.draggingIndex === -1) return
      
      e.preventDefault()
      const sourceIndex = parseInt(e.dataTransfer.getData('text/plain'))
      
      if (sourceIndex === targetIndex) {
        this.handleSortDragEnd()
        return
      }
      
      // 交换位置
      const sourceItem = this.selectedImages[sourceIndex]
      const targetItem = this.selectedImages[targetIndex]
      
      this.$set(this.selectedImages, sourceIndex, targetItem)
      this.$set(this.selectedImages, targetIndex, sourceItem)
      
      this.handleSortDragEnd()
    },
    
    // 删除图片
    removeImage(index) {
      this.selectedImages.splice(index, 1)
      if (this.selectedImages.length === 0) {
        this.sortDialogVisible = false
      }
    },
    
    // 取消排序
    cancelSort() {
      this.sortDialogVisible = false
      this.selectedImages = []
    },
    
    // 确认排序
    confirmSort() {
      if (this.selectedImages.length === 0) {
        this.$message.warning('请至少选择一张图片')
        return
      }
      
      this.actualSlotCount = this.selectedImages.length
      
      // 根据图片数量调整布局
      if (this.actualSlotCount === 2) {
        this.targetWidth = 960
        this.targetHeight = 540
        this.segmentWidths = [480, 480]
      } else if (this.actualSlotCount === 3) {
        this.targetWidth = 960
        this.targetHeight = 540
        this.segmentWidths = [320, 320, 320]
      }
      
      // 清空items
      this.items = [null, null, null]
      
      // 按排序后的顺序加载图片
      const tasks = this.selectedImages.map((img, index) => 
        this.readFileAsDataUrl(img.file).then(url => {
          const item = this.buildItem(url)
          item.file = img.file // 保存原始文件
          this.$set(this.items, index, item)
          return item
        })
      )
      
      Promise.all(tasks).then(() => {
        this.showStage = true
        this.sortDialogVisible = false
        
        // 初始化缩放
        this.$nextTick(() => {
          for (let i = 0; i < this.actualSlotCount; i++) {
            if (this.items[i]) {
              this.initItemScale(i)
            }
          }
        })
      })
    },
    
    // 重新选择图片
    resetImages() {
      this.showStage = false
      this.items = [null, null, null]
      this.selectedImages = []
      this.actualSlotCount = 3
      this.targetWidth = 960
      this.targetHeight = 540
      this.segmentWidths = [320, 320, 320]
      this.previewUrl = ''
      this.$emit('input', '')
    },
    
    // 单张图片直接上传
    async uploadSingleImage() {
      if (!this.items[0] || !this.items[0].file) {
        this.$message.error('没有可上传的图片')
        return
      }
      
      this.composing = true
      try {
        const file = this.items[0].file
        const formData = new FormData()
        formData.append('file', file)
        
        const res = await axios.post(this.action, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        
        let resourceKey = ''
        let previewUrl = ''
        if (res.data && res.data.success) {
          if (res.data.data && res.data.data.resourceKey) {
            resourceKey = res.data.data.resourceKey
            previewUrl = res.data.data.url || ''
          } else if (res.data.key) {
            resourceKey = res.data.key
            previewUrl = res.data.url || ''
          }
        }
        
        if (!resourceKey) {
          this.$message.error('上传失败：返回数据不包含资源键')
          return
        }
        
        this.previewUrl = previewUrl || this.buildWorkerPreviewUrl(resourceKey)
        this.$emit('input', resourceKey)
        this.$message.success('图片上传成功')
      } catch (e) {
        console.error(e)
        this.$message.error('上传失败')
      } finally {
        this.composing = false
      }
    }
  }
}
</script>

<style scoped>
.triple-cover-composer { display: flex; flex-direction: column; gap: 12px; }
.uploader .hint { margin-left: 8px; color: #909399; font-size: 12px; }

/* 现有封面预览 */
.existing-cover-preview {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
}
.existing-cover-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.existing-cover-img-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}
.existing-cover-img {
  max-width: 100%;
  max-height: 270px;
  border-radius: 4px;
  object-fit: contain;
  border: 1px solid #ebeef5;
}
.existing-cover-loading {
  font-size: 28px;
  color: #909399;
}
.existing-cover-placeholder {
  font-size: 36px;
  color: #c0c4cc;
}

.upload-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  color: #1e40af;
  font-size: 14px;
}

.upload-hint i {
  font-size: 16px;
}

/* 合成区域样式 */
.stage { 
  position: relative; 
  background: repeating-conic-gradient(#eee 0% 25%, #f8f8f8 0% 50%) 50%/20px 20px; 
  border: 1px solid #ebeef5; 
  border-radius: 4px; 
  user-select: none;
  padding-top: 40px; /* 为header留出空间 */
}

.stage-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  z-index: 5;
}

.stage-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.slot { position: absolute; top: 40px; overflow: hidden; }
.slot.active { outline: 2px solid #409EFF; outline-offset: -2px; }
.slot-img { position: absolute; top: 0; left: 0; will-change: transform; }
.slot-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.7); }

.empty-slot-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.empty-slot-hint i {
  font-size: 32px;
}
.slot-tools { position: absolute; left: 6px; bottom: 6px; display: flex; gap: 6px; }
.slot-guides { position: absolute; top: 40px; left: 0; right: 0; bottom: 0; pointer-events: none; }
.guide { position: absolute; top: 0; bottom: 0; width: 0; border-left: 1px dashed rgba(64,158,255,0.6); }
/* 操作按钮区域 */
.actions { 
  display: flex; 
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-buttons .el-button {
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  min-width: 140px;
}

.status-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  font-size: 13px;
  margin-left: 16px;
}

.status-hint i {
  font-size: 14px;
}

.preview-section {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e9ecef;
}

.preview-label {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.preview-link {
  color: #409EFF;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.preview-link:hover {
  background-color: rgba(64, 158, 255, 0.1);
  text-decoration: underline;
}

/* 图片排序弹出层样式 */
.sort-container {
  max-height: 500px;
  overflow-y: auto;
}

.sort-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  margin-bottom: 16px;
  color: #1e40af;
  font-size: 14px;
}

.image-sort-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sort-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: move;
  transition: all 0.3s ease;
}

.sort-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.sort-item.dragging {
  opacity: 0.5;
  transform: scale(0.98);
  z-index: 10;
}

.sort-item.drag-over {
  border-color: #409EFF;
  background-color: rgba(64, 158, 255, 0.05);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.sort-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
}

.sort-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
}

.sort-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.sort-info {
  flex: 1;
  min-width: 0;
}

.image-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-size {
  font-size: 12px;
  color: #6b7280;
}

.sort-actions {
  flex-shrink: 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .sort-item {
    padding: 8px;
    gap: 8px;
  }
  
  .sort-number {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }
  
  .sort-image {
    width: 50px;
    height: 50px;
  }
  
  .image-name {
    font-size: 13px;
  }
  
  .image-size {
    font-size: 11px;
  }
}
</style>

