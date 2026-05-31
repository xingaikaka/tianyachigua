<template>
  <el-dialog
    title="图片管理"
    :visible.sync="visible"
    width="1000px"
    append-to-body
    @close="handleClose"
  >
    <div class="image-dialog-container">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" size="small" @click="handleAdd" icon="el-icon-plus">添加图片</el-button>
        <el-button type="success" size="small" @click="handleBatchUpload" icon="el-icon-upload">批量上传</el-button>
        <el-button type="danger" size="small" @click="handleBatchDelete" :disabled="selectedImages.length === 0" icon="el-icon-delete">批量删除</el-button>
        <div class="video-info">
          <span>视频：{{ currentVideo.title }}</span>
          <span class="ml-2">共 {{ imageList.length }} 张图片</span>
        </div>
      </div>

      <!-- 图片网格 -->
      <div class="image-grid" v-loading="loading">
        <div
          v-for="image in imageList"
          :key="image.id"
          class="image-item"
          :class="{ 'selected': selectedImages.includes(image.id), 'primary': image.isPrimary }"
          @click="toggleSelect(image.id)"
        >
          <div class="image-wrapper">
            <img :src="image.imageUrl" :alt="image.description" @error="handleImageError" />
            <div class="image-overlay">
              <div class="image-actions">
                <el-button type="primary" size="mini" icon="el-icon-view" @click.stop="handlePreview(image)">预览</el-button>
                <el-button type="success" size="mini" icon="el-icon-star-on" @click.stop="handleSetPrimary(image)" v-if="!image.isPrimary">设为主图</el-button>
                <el-button type="warning" size="mini" icon="el-icon-edit" @click.stop="handleEdit(image)">编辑</el-button>
                <el-button type="danger" size="mini" icon="el-icon-delete" @click.stop="handleDelete(image)">删除</el-button>
              </div>
            </div>
          </div>
          <div class="image-info">
            <div class="image-title">{{ image.description || '无描述' }}</div>
            <div class="image-meta">
              <span class="sort-order">排序: {{ image.sortOrder }}</span>
              <el-tag v-if="image.isPrimary" type="success" size="mini">主图</el-tag>
            </div>
          </div>
          <el-checkbox v-model="selectedImages" :label="image.id" class="image-checkbox"></el-checkbox>
        </div>
        
        <!-- 空状态 -->
        <div v-if="imageList.length === 0 && !loading" class="empty-state">
          <i class="el-icon-picture-outline"></i>
          <p>暂无图片</p>
          <el-button type="primary" @click="handleAdd">添加第一张图片</el-button>
        </div>
      </div>
    </div>

    <!-- 添加/编辑图片弹窗 -->
    <el-dialog
      :title="imageForm.id ? '编辑图片' : '添加图片'"
      :visible.sync="imageDialogVisible"
      width="600px"
      append-to-body
    >
      <el-form ref="imageForm" :model="imageForm" :rules="imageRules" label-width="100px">
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="imageForm.imageUrl" placeholder="请输入图片地址">
            <template slot="append">
              <el-button @click="handleUploadImage">上传</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="图片预览" v-if="imageForm.imageUrl">
          <img :src="imageForm.imageUrl" style="max-width: 200px; max-height: 200px;" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="imageForm.description" type="textarea" :rows="3" placeholder="请输入图片描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="imageForm.sortOrder" :min="0" placeholder="请输入排序" />
        </el-form-item>
        <el-form-item label="是否主图" prop="isPrimary">
          <el-switch v-model="imageForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="imageDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitImageForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 批量上传弹窗 -->
    <el-dialog
      title="批量上传图片"
      :visible.sync="batchUploadVisible"
      width="600px"
      append-to-body
    >
      <el-form label-width="100px">
        <el-form-item label="图片地址">
          <el-input
            v-model="batchUrls"
            type="textarea"
            :rows="8"
            placeholder="请输入图片地址，每行一个"
          />
          <div class="form-tip">每行输入一个图片地址，支持批量添加</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="batchUploadVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitBatchUpload">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 图片预览弹窗 -->
    <el-dialog
      title="图片预览"
      :visible.sync="previewVisible"
      width="800px"
      append-to-body
    >
      <div class="preview-container" v-if="previewImage">
        <img :src="previewImage.imageUrl" style="max-width: 100%; max-height: 500px;" />
        <div class="preview-info">
          <p><strong>描述：</strong>{{ previewImage.description || '无' }}</p>
          <p><strong>排序：</strong>{{ previewImage.sortOrder }}</p>
          <p><strong>是否主图：</strong>{{ previewImage.isPrimary ? '是' : '否' }}</p>
          <p><strong>创建时间：</strong>{{ previewImage.createdAt }}</p>
        </div>
      </div>
    </el-dialog>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getImagesByVideoId, addVideoImage, updateVideoImage, delVideoImage, setPrimaryImage, batchUploadImages } from "@/api/chigua/videoImage";

export default {
  name: "VideoImageDialog",
  data() {
    return {
      visible: false,
      loading: false,
      currentVideo: {},
      imageList: [],
      selectedImages: [],
      
      // 图片表单
      imageDialogVisible: false,
      imageForm: {
        id: null,
        videoId: null,
        imageUrl: '',
        description: '',
        sortOrder: 0,
        isPrimary: 0
      },
      imageRules: {
        imageUrl: [
          { required: true, message: "图片地址不能为空", trigger: "blur" }
        ]
      },
      
      // 批量上传
      batchUploadVisible: false,
      batchUrls: '',
      
      // 图片预览
      previewVisible: false,
      previewImage: null
    };
  },
  methods: {
    /** 显示弹窗 */
    show(video) {
      this.currentVideo = video;
      this.visible = true;
      this.getImageList();
    },
    
    /** 获取图片列表 */
    getImageList() {
      this.loading = true;
      getImagesByVideoId(this.currentVideo.id).then(response => {
        this.imageList = response.data;
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    
    /** 切换选择 */
    toggleSelect(imageId) {
      const index = this.selectedImages.indexOf(imageId);
      if (index > -1) {
        this.selectedImages.splice(index, 1);
      } else {
        this.selectedImages.push(imageId);
      }
    },
    
    /** 添加图片 */
    handleAdd() {
      this.resetImageForm();
      this.imageForm.videoId = this.currentVideo.id;
      this.imageDialogVisible = true;
    },
    
    /** 编辑图片 */
    handleEdit(image) {
      this.imageForm = { ...image };
      this.imageDialogVisible = true;
    },
    
    /** 删除图片 */
    handleDelete(image) {
      this.$modal.confirm(`确认删除图片"${image.description || '无描述'}"？`).then(() => {
        return delVideoImage(image.id);
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.getImageList();
        this.$emit('refresh');
      }).catch(() => {});
    },
    
    /** 批量删除 */
    handleBatchDelete() {
      if (this.selectedImages.length === 0) {
        this.$modal.msgError("请选择要删除的图片");
        return;
      }
      
      this.$modal.confirm(`确认删除选中的${this.selectedImages.length}张图片？`).then(() => {
        const promises = this.selectedImages.map(id => delVideoImage(id));
        return Promise.all(promises);
      }).then(() => {
        this.$modal.msgSuccess("批量删除成功");
        this.selectedImages = [];
        this.getImageList();
        this.$emit('refresh');
      }).catch(() => {});
    },
    
    /** 设为主图 */
    handleSetPrimary(image) {
      setPrimaryImage(this.currentVideo.id, image.id).then(() => {
        this.$modal.msgSuccess("设置主图成功");
        this.getImageList();
        this.$emit('refresh');
      });
    },
    
    /** 预览图片 */
    handlePreview(image) {
      this.previewImage = image;
      this.previewVisible = true;
    },
    
    /** 批量上传 */
    handleBatchUpload() {
      this.batchUrls = '';
      this.batchUploadVisible = true;
    },
    
    /** 提交批量上传 */
    submitBatchUpload() {
      if (!this.batchUrls.trim()) {
        this.$modal.msgError("请输入图片地址");
        return;
      }
      
      const urls = this.batchUrls.split('\n').filter(url => url.trim());
      if (urls.length === 0) {
        this.$modal.msgError("请输入有效的图片地址");
        return;
      }
      
      batchUploadImages(this.currentVideo.id, urls).then(() => {
        this.$modal.msgSuccess(`批量上传成功，共${urls.length}张图片`);
        this.batchUploadVisible = false;
        this.getImageList();
        this.$emit('refresh');
      });
    },
    
    /** 上传图片 */
    handleUploadImage() {
      // 这里可以集成文件上传组件
      this.$modal.msg("请直接输入图片地址，或集成文件上传功能");
    },
    
    /** 重置图片表单 */
    resetImageForm() {
      this.imageForm = {
        id: null,
        videoId: null,
        imageUrl: '',
        description: '',
        sortOrder: 0,
        isPrimary: 0
      };
      if (this.$refs.imageForm) {
        this.$refs.imageForm.resetFields();
      }
    },
    
    /** 提交图片表单 */
    submitImageForm() {
      this.$refs.imageForm.validate(valid => {
        if (valid) {
          if (this.imageForm.id) {
            updateVideoImage(this.imageForm).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.imageDialogVisible = false;
              this.getImageList();
              this.$emit('refresh');
            });
          } else {
            addVideoImage(this.imageForm).then(() => {
              this.$modal.msgSuccess("添加成功");
              this.imageDialogVisible = false;
              this.getImageList();
              this.$emit('refresh');
            });
          }
        }
      });
    },
    
    /** 图片加载错误 */
    handleImageError(event) {
      event.target.src = '/img/no-image.png'; // 设置默认图片
    },
    
    /** 关闭弹窗 */
    handleClose() {
      this.visible = false;
      this.selectedImages = [];
      this.resetImageForm();
    }
  }
};
</script>

<style scoped>
.image-dialog-container {
  height: 600px;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.video-info {
  color: #666;
  font-size: 14px;
}

.image-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  overflow-y: auto;
  padding: 10px 0;
}

.image-item {
  position: relative;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.3s, box-shadow 0.3s;
  background: #fff;
}

.image-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.image-item.selected {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.image-item.primary {
  border-color: #67c23a;
}

.image-wrapper {
  position: relative;
  height: 150px;
  overflow: hidden;
  background: #f5f5f5;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.image-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.image-info {
  padding: 10px;
  background: #f9f9f9;
  height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.image-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #666;
}

.image-checkbox {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 2px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 20px;
  display: block;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.preview-container {
  text-align: center;
}

.preview-info {
  margin-top: 20px;
  text-align: left;
  background: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
}

.preview-info p {
  margin: 5px 0;
}

.ml-2 {
  margin-left: 8px;
}
</style> 