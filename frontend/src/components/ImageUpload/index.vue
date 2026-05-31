<template>
  <div class="component-upload-image">
    <el-upload
      multiple
      :disabled="disabled"
      :action="finalUploadUrl"
      :http-request="customRequest"
      list-type="picture-card"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :data="data"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      ref="imageUpload"
      :on-remove="handleDelete"
      :show-file-list="true"
      :headers="uploadHeaders"
      :file-list="fileList"
      :on-preview="handlePictureCardPreview"
      :class="{hide: this.fileList.length >= this.limit}"
    >
      <i class="el-icon-plus"></i>
    </el-upload>

    <!-- 上传提示 -->
    <div class="el-upload__tip" slot="tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize"> 大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b> </template>
      <template v-if="fileType"> 格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b> </template>
      的文件
    </div>

    <el-dialog
      :visible.sync="dialogVisible"
      title="预览"
      width="800"
      append-to-body
    >
      <img
        :src="dialogImageUrl"
        style="display: block; max-width: 100%; margin: 0 auto"
      />
    </el-dialog>
  </div>
</template>

<script>
import { getToken } from "@/utils/auth"
import { isExternal } from "@/utils/validate"
import Sortable from 'sortablejs'

export default {
  props: {
    value: [String, Object, Array],
    // 上传接口地址
    action: {
      type: String,
      default: "/common/upload"
    },
    // 是否对上传图片叠加水印
    watermark: {
      type: Boolean,
      default: false
    },
    // 水印图片地址（建议放在同域 public 下）
    watermarkSrc: {
      type: String,
      default: '/shuiyin.png'
    },
    // 上传携带的参数
    data: {
      type: Object
    },
    // 图片数量限制
    limit: {
      type: Number,
      default: 5
    },
    // 大小限制(MB)
    fileSize: {
       type: Number,
      default: 5
    },
    // 文件类型, 例如['png', 'jpg', 'jpeg']
    fileType: {
      type: Array,
      default: () => ["png", "jpg", "jpeg"]
    },
    // 是否显示提示
    isShowTip: {
      type: Boolean,
      default: true
    },
    // 禁用组件（仅查看图片）
    disabled: {
      type: Boolean,
      default: false
    },
    // 拖动排序
    drag: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      number: 0,
      uploadList: [],
      dialogImageUrl: "",
      dialogVisible: false,
      hideUpload: false,
      baseUrl: process.env.VUE_APP_BASE_API,
      fileList: []
    }
  },
  beforeDestroy() {
    // 清理所有本地 Blob URL，释放内存
    this.fileList.forEach(file => {
      if (file && file.url && file.url.startsWith('blob:')) {
        URL.revokeObjectURL(file.url);
      }
    });
  },
  mounted() {
    if (this.drag && !this.disabled) {
      this.$nextTick(() => {
        const element = this.$refs.imageUpload?.$el?.querySelector('.el-upload-list')
        Sortable.create(element, {
          onEnd: (evt) => {
            const movedItem = this.fileList.splice(evt.oldIndex, 1)[0]
            this.fileList.splice(evt.newIndex, 0, movedItem)
            this.$emit("input", this.listToString(this.fileList))
          }
        })
      })
    }
  },
  watch: {
    value: {
      handler(val) {
        console.log('👁️ value watch 触发，新值:', val);
        if (val) {
          // 首先将值转为数组
          const list = Array.isArray(val) ? val : this.value.split(',')
          console.log('👁️ 解析为列表:', list);
          // 然后将数组转为对象数组
          this.fileList = list.map(item => {
            if (typeof item === "string") {
              // 🔧 检查是否已经在fileList中存在完整URL项（避免覆盖正确的URL）
              const existingItem = this.fileList.find(file => 
                file.resourceKey === item || 
                (file.url && file.url.includes(item))
              );
              if (existingItem && existingItem.url &&
                  (existingItem.url.startsWith('http') || existingItem.url.startsWith('blob:'))) {
                // 如果已存在且有完整URL（含本地Blob URL），保留原有设置
                console.log('🔄 保留已存在项目的完整URL:', existingItem);
                return existingItem;
              }
              
              // 新的字符串项，按原逻辑处理
              if (item.indexOf(this.baseUrl) === -1 && !isExternal(item)) {
                  item = { name: this.baseUrl + item, url: this.baseUrl + item }
              } else {
                  // 外部URL（Worker 签名URL）：Worker 服务端解密，直接使用 URL
                  item = { name: item, url: item, status: 'success' }
              }
            }
            return item
          })
        } else {
          this.fileList = []
          return []
        }
      },
      deep: true,
      immediate: true
    }
  },
  computed: {
    // 是否显示提示
    showTip() {
      return this.isShowTip && (this.fileType || this.fileSize)
    },
    // 最终上传URL
    finalUploadUrl() {
      // 如果action是完整URL则直接使用，否则加上baseUrl
      if (this.action.startsWith('http://') || this.action.startsWith('https://')) {
        return this.action;
      }
      return this.baseUrl + this.action
    },
    // 请求头 - Worker API不需要Authorization
    uploadHeaders() {
      // 如果是Worker API（包含workers.dev），不发送Authorization
      if (this.finalUploadUrl.includes('workers.dev')) {
        return {};
      }
      // 其他API需要Authorization
      return {
        Authorization: "Bearer " + getToken()
      };
    },
  },
  methods: {
    async customRequest(options) {
      try {
        const { file, action, onSuccess, onError } = options
        let uploadFile = file
        if (this.watermark && file && file.type && file.type.startsWith('image/')) {
          try {
            uploadFile = await this.applyWatermark(file)
          } catch (e) {
            console.warn('水印处理失败，使用原图上传', e)
            uploadFile = file
          }
        }

        const formData = new FormData()
        formData.append('file', uploadFile, uploadFile.name || file.name)
        // 追加自定义表单数据
        if (this.data) {
          Object.keys(this.data).forEach(k => formData.append(k, this.data[k]))
        }

        const headers = this.uploadHeaders || {}
        const res = await fetch(this.finalUploadUrl, {
          method: 'POST',
          headers,
          body: formData
        })
        const json = await res.json()
        onSuccess && onSuccess(json, uploadFile)
      } catch (err) {
        console.error('自定义上传失败', err)
        options.onError && options.onError(err)
        this.handleUploadError()
      }
    },
    async applyWatermark(file) {
      const baseImg = await this.fileToImage(file)
      const wmImg = await this.loadWatermark(this.watermarkSrc)
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
      const type = file.type && file.type.startsWith('image/') ? file.type : 'image/jpeg'
      const blob = await new Promise(resolve => canvas.toBlob(resolve, type, 1.0))
      const outName = this.appendSuffixToName(file.name || 'image', '_wm')
      return new File([blob], outName, { type })
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
    // 上传前loading加载
    handleBeforeUpload(file) {
      let isImg = false
      if (this.fileType.length) {
        let fileExtension = ""
        if (file.name.lastIndexOf(".") > -1) {
          fileExtension = file.name.slice(file.name.lastIndexOf(".") + 1)
        }
        isImg = this.fileType.some(type => {
          if (file.type.indexOf(type) > -1) return true
          if (fileExtension && fileExtension.indexOf(type) > -1) return true
          return false
        })
      } else {
        isImg = file.type.indexOf("image") > -1
      }

      if (!isImg) {
        this.$modal.msgError(`文件格式不正确，请上传${this.fileType.join("/")}图片格式文件!`)
        return false
      }
      if (file.name.includes(',')) {
        this.$modal.msgError('文件名不正确，不能包含英文逗号!')
        return false
      }
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize
        if (!isLt) {
          this.$modal.msgError(`上传头像图片大小不能超过 ${this.fileSize} MB!`)
          return false
        }
      }
      this.$modal.loading("正在上传图片，请稍候...")
      this.number++
    },
    // 文件个数超出
    handleExceed() {
      this.$modal.msgError(`上传文件数量不能超过 ${this.limit} 个!`)
    },
    // 上传成功回调
    handleUploadSuccess(res, file) {
      console.log('🔍 ImageUpload 收到上传响应:', res);
      
      // 🔧 适配多种API响应格式
      if (res.success) {
        let resourceKey, fileName, previewUrl;
        
        // 支持多种响应格式：
        // 1. Worker richtext格式：{ success: true, data: { resourceKey, url, ... } }（与富文本一致）
        // 2. Java后端格式：{ success: true, data: { resourceKey, fileName, url, ... } }
        // 3. Worker通用格式：{ success: true, key, filename, url, ... }
        if (res.data && res.data.resourceKey) {
          // Worker richtext格式或Java后端格式
          resourceKey = res.data.resourceKey;
          fileName = res.data.fileName || res.data.originalName || file.name;
          previewUrl = res.data.url; // 获取完整的预览URL
          console.log('🎯 使用data格式:', { resourceKey, fileName, previewUrl });
        } else if (res.key) {
          // Worker通用格式
          resourceKey = res.key;
          fileName = res.filename;
          previewUrl = res.url; // 获取完整的预览URL
          console.log('🎯 使用key格式:', { resourceKey, fileName, previewUrl });
        } else {
          console.error('❌ 未知的响应格式:', res);
          this.$modal.msgError("响应格式错误");
          this.handleUploadError();
          return;
        }
        
        console.log('📄 ImageUpload 最终处理结果:', { resourceKey, fileName, previewUrl });
        
        // 🔧 检查previewUrl是否是完整的URL
        if (previewUrl && !previewUrl.startsWith('http')) {
          console.warn('⚠️ previewUrl不是完整URL，尝试修复:', previewUrl);
          // 如果不是完整URL，尝试拼接Worker域名
          if (previewUrl.startsWith('/')) {
            previewUrl = 'https://chigua-r2-worker.xingaikaka.workers.dev' + previewUrl;
            console.log('🔧 修复后的previewUrl:', previewUrl);
          }
        }
        
        // 🔧 确保显示URL是完整的，避免被baseUrl处理影响
        let displayUrl = previewUrl || resourceKey;
        if (displayUrl && !displayUrl.startsWith('http') && !displayUrl.startsWith('blob:')) {
          // 如果不是完整URL，检查是否来自Worker
          if (this.finalUploadUrl.includes('workers.dev') && displayUrl.startsWith('/')) {
            // Worker返回的相对路径，转换为完整URL
            displayUrl = 'https://chigua-r2-worker.xingaikaka.workers.dev' + displayUrl;
          } else if (!displayUrl.startsWith('/')) {
            // 相对路径，添加Worker域名
            displayUrl = 'https://chigua-r2-worker.xingaikaka.workers.dev/files/' + encodeURIComponent(displayUrl);
          }
        }
        
        // 优先使用本地文件 Blob URL 作预览（无需二次请求，且不受加密影响）
        let localBlobUrl = null;
        try {
          if (file && (file instanceof File || file instanceof Blob)) {
            localBlobUrl = URL.createObjectURL(file);
          }
        } catch (_) {}
        const finalDisplayUrl = localBlobUrl || displayUrl;

        console.log('🎯 最终显示URL:', finalDisplayUrl, '(本地blob:', !!localBlobUrl, ')');
        
        this.uploadList.push({ 
          name: fileName, 
          url: finalDisplayUrl,            // 用于显示的 URL（优先本地 Blob）
          resourceKey: resourceKey,        // 用于存储的资源键
          uid: file.uid,
          status: 'success'
        });
        this.uploadedSuccessfully();
      } else {
        console.error('❌ 上传失败:', res);
        this.$modal.msgError(res.message || "上传失败");
        this.handleUploadError();
      }
    },
    // 删除图片
    handleDelete(file) {
      const findex = this.fileList.map(f => f.name).indexOf(file.name)
      if (findex > -1) {
        // 清理本地 Blob URL
        const removedItem = this.fileList[findex];
        if (removedItem && removedItem.url && removedItem.url.startsWith('blob:')) {
          URL.revokeObjectURL(removedItem.url);
        }
        this.fileList.splice(findex, 1)
        this.$emit("input", this.listToString(this.fileList))
      }
    },
    // 上传失败
    handleUploadError() {
      this.$modal.msgError("上传图片失败，请重试")
      this.$modal.closeLoading()
    },
    // 上传结束处理
    uploadedSuccessfully() {
      if (this.number > 0 && this.uploadList.length === this.number) {
        console.log('📤 uploadedSuccessfully 开始，uploadList:', this.uploadList);
        this.fileList = this.fileList.concat(this.uploadList)
        console.log('📤 合并后的 fileList:', this.fileList);
        
        const outputString = this.listToString(this.fileList);
        console.log('📤 输出给父组件的字符串:', outputString);
        
        this.uploadList = []
        this.number = 0
        this.$emit("input", outputString)
        this.$modal.closeLoading()
      }
    },
    // 预览
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url
      this.dialogVisible = true
    },
    // 对象转成指定字符串分隔
    listToString(list, separator) {
      let strs = ""
      separator = separator || ","
      for (let i in list) {
        // 🔧 优先使用resourceKey（用于数据库存储），如果没有则使用url并去掉baseUrl
        let path = list[i].resourceKey || (list[i].url ? list[i].url.replace(this.baseUrl, "") : "");
        if (path) {
          strs += path + separator
        }
      }
      return strs != '' ? strs.substr(0, strs.length - 1) : ''
    }
  }
}
</script>
<style scoped lang="scss">
// .el-upload--picture-card 控制加号部分
::v-deep.hide .el-upload--picture-card {
  display: none;
}

::v-deep .el-upload-list--picture-card.is-disabled + .el-upload--picture-card {
  display: none !important;
} 

// 去掉动画效果
::v-deep .el-list-enter-active,
::v-deep .el-list-leave-active {
  transition: all 0s;
}

::v-deep .el-list-enter, .el-list-leave-active {
  opacity: 0;
  transform: translateY(0);
}
</style>

