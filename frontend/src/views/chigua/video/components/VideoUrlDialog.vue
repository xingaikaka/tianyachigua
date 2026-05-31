<template>
  <el-dialog
    title="地址管理"
    :visible.sync="visible"
    width="1200px"
    append-to-body
    @close="handleClose"
  >
    <div class="url-dialog-container">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" size="small" @click="handleAdd" icon="el-icon-plus">添加地址</el-button>
        <el-button type="success" size="small" @click="handleBatchImport" icon="el-icon-upload">批量导入</el-button>
        <el-button type="danger" size="small" @click="handleBatchDelete" :disabled="selectedUrls.length === 0" icon="el-icon-delete">批量删除</el-button>
        <div class="video-info">
          <span>视频：{{ currentVideo.title }}</span>
          <span class="ml-2">共 {{ urlList.length }} 个地址</span>
        </div>
      </div>

      <!-- 地址列表 -->
      <el-table
        v-loading="loading"
        :data="urlList"
        @selection-change="handleSelectionChange"
        height="500"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="地址标题" prop="title" width="150" show-overflow-tooltip />
        <el-table-column label="播放地址" prop="videoUrl" min-width="200" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-link :href="scope.row.videoUrl" target="_blank" type="primary">
              {{ scope.row.videoUrl }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="清晰度" prop="quality" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="getQualityType(scope.row.quality)" size="mini">
              {{ scope.row.quality || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="格式" prop="format" width="80" align="center" />
        <el-table-column label="大小" prop="fileSize" width="100" align="center">
          <template slot-scope="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="播放次数" prop="playCount" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.isPrimary" type="success" size="mini">主要</el-tag>
            <el-tag v-if="scope.row.status === 1" type="success" size="mini">正常</el-tag>
            <el-tag v-else-if="scope.row.status === 0" type="danger" size="mini">失效</el-tag>
            <el-tag v-else type="info" size="mini">未测试</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="text" @click="handleSetPrimary(scope.row)" v-if="!scope.row.isPrimary" icon="el-icon-star-on">设为主要</el-button>
            <el-button size="mini" type="text" @click="handleEdit(scope.row)" icon="el-icon-edit">编辑</el-button>
            <el-button size="mini" type="text" @click="handleDelete(scope.row)" icon="el-icon-delete" style="color: #f56c6c;">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑地址弹窗 -->
    <el-dialog
      :title="urlForm.id ? '编辑地址' : '添加地址'"
      :visible.sync="urlDialogVisible"
      width="600px"
      append-to-body
    >
      <el-form ref="urlForm" :model="urlForm" :rules="urlRules" label-width="100px">
        <el-form-item label="地址标题" prop="title">
          <el-input v-model="urlForm.title" placeholder="请输入地址标题" />
        </el-form-item>
        <el-form-item label="播放地址" prop="videoUrl">
          <el-input v-model="urlForm.videoUrl" placeholder="请输入播放地址" />
        </el-form-item>
        <el-form-item label="清晰度" prop="quality">
          <el-select v-model="urlForm.quality" placeholder="请选择清晰度">
            <el-option label="4K" value="4K" />
            <el-option label="2K" value="2K" />
            <el-option label="1080P" value="1080P" />
            <el-option label="720P" value="720P" />
            <el-option label="480P" value="480P" />
            <el-option label="360P" value="360P" />
            <el-option label="240P" value="240P" />
          </el-select>
        </el-form-item>
        <el-form-item label="格式" prop="format">
          <el-select v-model="urlForm.format" placeholder="请选择格式">
            <el-option label="MP4" value="mp4" />
            <el-option label="AVI" value="avi" />
            <el-option label="MKV" value="mkv" />
            <el-option label="MOV" value="mov" />
            <el-option label="WMV" value="wmv" />
            <el-option label="FLV" value="flv" />
            <el-option label="M3U8" value="m3u8" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件大小(MB)" prop="fileSize">
          <el-input-number v-model="urlForm.fileSize" :min="0" :precision="2" placeholder="请输入文件大小" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="urlForm.sortOrder" :min="0" placeholder="请输入排序" />
        </el-form-item>
        <el-form-item label="是否主要" prop="isPrimary">
          <el-switch v-model="urlForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="urlForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="urlDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitUrlForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog
      title="批量导入地址"
      :visible.sync="batchImportVisible"
      width="800px"
      append-to-body
    >
      <el-form label-width="100px">
        <el-form-item label="导入格式">
          <el-radio-group v-model="importFormat">
            <el-radio label="simple">简单格式（每行一个地址）</el-radio>
            <el-radio label="detailed">详细格式（地址|清晰度|格式|大小）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地址数据">
          <el-input
            v-model="batchUrls"
            type="textarea"
            :rows="10"
            :placeholder="importFormat === 'simple' ? '请输入播放地址，每行一个' : '请输入地址数据，格式：地址|清晰度|格式|大小(MB)，每行一个'"
          />
          <div class="form-tip">
            <div v-if="importFormat === 'simple'">简单格式示例：</div>
            <div v-else>详细格式示例：</div>
            <div class="example-text">
              <div v-if="importFormat === 'simple'">
                https://example.com/video1.mp4<br>
                https://example.com/video2.mp4
              </div>
              <div v-else>
                https://example.com/video1.mp4|1080P|mp4|1024<br>
                https://example.com/video2.mp4|720P|mp4|512
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="batchImportVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitBatchImport">确 定</el-button>
      </div>
    </el-dialog>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getUrlsByVideoId, addVideoUrl, updateVideoUrl, delVideoUrl, setPrimaryUrl, batchImportUrls } from "@/api/chigua/videoUrl";

export default {
  name: "VideoUrlDialog",
  data() {
    return {
      visible: false,
      loading: false,
      currentVideo: {},
      urlList: [],
      selectedUrls: [],
      
      // 地址表单
      urlDialogVisible: false,
      urlForm: {
        id: null,
        videoId: null,
        title: '',
        videoUrl: '',
        quality: '',
        format: '',
        fileSize: null,
        sortOrder: 0,
        isPrimary: 0,
        remark: ''
      },
      urlRules: {
        title: [
          { required: true, message: '地址标题不能为空', trigger: 'blur' },
          { min: 1, max: 100, message: '地址标题长度在 1 到 100 个字符', trigger: 'blur' }
        ],
        videoUrl: [
          { required: true, message: '播放地址不能为空', trigger: 'blur' },
          { type: 'url', message: '请输入正确的URL格式', trigger: 'blur' }
        ],
        quality: [
          { required: true, message: '清晰度不能为空', trigger: 'change' }
        ],
        format: [
          { required: true, message: '格式不能为空', trigger: 'change' }
        ]
      },
      
      // 批量导入
      batchImportVisible: false,
      batchUrls: '',
      importFormat: 'simple'
    };
  },
  methods: {
    /** 显示弹窗 */
    show(video) {
      this.currentVideo = video;
      this.visible = true;
      this.getUrlList();
    },
    
    /** 获取地址列表 */
    getUrlList() {
      this.loading = true;
      getUrlsByVideoId(this.currentVideo.id).then(response => {
        this.urlList = response.data;
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.selectedUrls = selection.map(item => item.id);
    },
    
    /** 添加地址 */
    handleAdd() {
      this.resetUrlForm();
      this.urlForm.videoId = this.currentVideo.id;
      this.urlDialogVisible = true;
    },
    
    /** 编辑地址 */
    handleEdit(url) {
      this.urlForm = { ...url };
      this.urlDialogVisible = true;
    },
    
    /** 删除地址 */
    handleDelete(url) {
      this.$modal.confirm(`确认删除地址"${url.videoUrl}"？`).then(() => {
        return delVideoUrl(url.id);
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.getUrlList();
        this.$emit('refresh');
      }).catch(() => {});
    },
    
    /** 批量删除 */
    handleBatchDelete() {
      if (this.selectedUrls.length === 0) {
        this.$modal.msgError("请选择要删除的地址");
        return;
      }
      
      this.$modal.confirm(`确认删除选中的${this.selectedUrls.length}个地址？`).then(() => {
        const promises = this.selectedUrls.map(id => delVideoUrl(id));
        return Promise.all(promises);
      }).then(() => {
        this.$modal.msgSuccess("批量删除成功");
        this.selectedUrls = [];
        this.getUrlList();
        this.$emit('refresh');
      }).catch(() => {});
    },
    
    /** 设为主要地址 */
    handleSetPrimary(url) {
      setPrimaryUrl(this.currentVideo.id, url.id).then(() => {
        this.$modal.msgSuccess("设置主要地址成功");
        this.getUrlList();
        this.$emit('refresh');
      });
    },
    
    /** 批量导入 */
    handleBatchImport() {
      this.batchImportVisible = true;
      this.batchUrls = '';
      this.importFormat = 'simple';
    },
    
    /** 提交批量导入 */
    submitBatchImport() {
      if (!this.batchUrls.trim()) {
        this.$message.warning('请输入地址数据');
        return;
      }
      
      const lines = this.batchUrls.split('\n').filter(line => line.trim());
      const urls = [];
      
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;
        
        if (this.importFormat === 'simple') {
          // 简单格式：每行一个地址
          urls.push({
            videoId: this.currentVideo.id,
            title: `地址${i + 1}`,
            videoUrl: line,
            quality: '1080P',
            format: 'mp4',
            sortOrder: i,
            isPrimary: 0
          });
        } else {
          // 详细格式：地址|清晰度|格式|大小
          const parts = line.split('|');
          if (parts.length < 2) {
            this.$message.error(`第${i + 1}行格式错误，请按照：地址|清晰度|格式|大小 的格式输入`);
            return;
          }
          
          urls.push({
            videoId: this.currentVideo.id,
            title: `地址${i + 1}`,
            videoUrl: parts[0].trim(),
            quality: parts[1] ? parts[1].trim() : '1080P',
            format: parts[2] ? parts[2].trim() : 'mp4',
            fileSize: parts[3] ? parseFloat(parts[3].trim()) : null,
            sortOrder: i,
            isPrimary: 0
          });
        }
      }
      
      // 批量添加
      batchImportUrls(this.currentVideo.id, urls).then(response => {
        this.$message.success('批量导入成功');
        this.batchImportVisible = false;
        this.getUrlList();
      }).catch(error => {
        this.$message.error('批量导入失败：' + (error.message || '未知错误'));
      });
    },
    
    /** 重置地址表单 */
    resetUrlForm() {
      this.urlForm = {
        id: null,
        videoId: this.currentVideo.id,
        title: '',
        videoUrl: '',
        quality: '',
        format: '',
        fileSize: null,
        sortOrder: 0,
        isPrimary: 0,
        remark: ''
      };
      this.$nextTick(() => {
        if (this.$refs.urlForm) {
          this.$refs.urlForm.clearValidate();
        }
      });
    },
    
    /** 提交地址表单 */
    submitUrlForm() {
      this.$refs.urlForm.validate(valid => {
        if (valid) {
          if (this.urlForm.id) {
            updateVideoUrl(this.urlForm).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.urlDialogVisible = false;
              this.getUrlList();
              this.$emit('refresh');
            });
          } else {
            addVideoUrl(this.urlForm).then(() => {
              this.$modal.msgSuccess("添加成功");
              this.urlDialogVisible = false;
              this.getUrlList();
              this.$emit('refresh');
            });
          }
        }
      });
    },
    
    /** 获取清晰度标签类型 */
    getQualityType(quality) {
      const qualityMap = {
        '4K': 'danger',
        '2K': 'warning',
        '1080P': 'success',
        '720P': 'primary',
        '480P': 'info',
        '360P': '',
        '240P': ''
      };
      return qualityMap[quality] || '';
    },
    
    /** 格式化文件大小 */
    formatFileSize(size) {
      if (!size) return '-';
      if (size < 1024) return size + ' MB';
      return (size / 1024).toFixed(2) + ' GB';
    },
    
    /** 关闭弹窗 */
    handleClose() {
      this.visible = false;
      this.selectedUrls = [];
      this.resetUrlForm();
    }
  }
};
</script>

<style scoped>
.url-dialog-container {
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

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.example-text {
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  margin-top: 5px;
  font-family: monospace;
  font-size: 11px;
  line-height: 1.4;
}

.ml-2 {
  margin-left: 8px;
}
</style> 