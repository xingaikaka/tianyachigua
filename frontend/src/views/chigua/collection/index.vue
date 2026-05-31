<template>
  <div class="app-container">
    <!-- 查询条件 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="合集标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入合集标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="作者" prop="author">
        <el-input
          v-model="queryParams.author"
          placeholder="请输入作者"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="合集状态" clearable>
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['chigua:collection:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['chigua:collection:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['chigua:collection:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['chigua:collection:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 合集列表 -->
    <el-table v-loading="loading" :data="collectionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="合集ID" align="center" prop="id" />
      <el-table-column label="封面图片" align="center" prop="coverImage" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.coverImage" :width="50" :height="50" v-if="scope.row.coverImage"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="合集标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="作者" align="center" prop="author" />
      <el-table-column label="视频数量" align="center" prop="videoCount" />
      <el-table-column label="观看次数" align="center" prop="viewCount" />
      <el-table-column label="排序权重" align="center" prop="sortOrder" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status == 1 ? 'success' : 'danger'" effect="plain">
            {{ scope.row.status == 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handlePreview(scope.row)"
            v-hasPermi="['chigua:collection:query']"
          >预览</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-s-grid"
            @click="handleManageVideos(scope.row)"
            v-hasPermi="['chigua:collection:edit']"
          >管理视频</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['chigua:collection:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['chigua:collection:remove']"
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

    <!-- 添加或修改合集对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="合集标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入合集标题" />
        </el-form-item>
        <el-form-item label="合集描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入合集描述" />
        </el-form-item>
        <el-form-item label="封面图片" prop="coverImage">
          <image-upload v-model="form.coverImage" :action="uploadCoverUrl"/>
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryIds">
          <el-select v-model="form.categoryIds" multiple placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="category in categoryOptions"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 合集预览对话框 -->
    <collection-preview-dialog
      :visible.sync="previewVisible"
      :collection="currentCollection"
    />

    <!-- 视频管理对话框 -->
    <video-manage-dialog
      :visible.sync="videoManageVisible"
      :collection="currentCollection"
      @refresh="getList"
    />
  </div>
</template>

<script>
import { listCollection, getCollection, delCollection, addCollection, updateCollection, getAllCategories } from "@/api/chigua/collection";
import CollectionPreviewDialog from './components/CollectionPreviewDialog';
import VideoManageDialog from './components/VideoManageDialog';

export default {
  name: "Collection",
  components: {
    CollectionPreviewDialog,
    VideoManageDialog
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
      // 合集表格数据
      collectionList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        author: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        title: [
          { required: true, message: "合集标题不能为空", trigger: "blur" }
        ]
      },
      // 分类选项
      categoryOptions: [],
      // 预览对话框显示状态
      previewVisible: false,
      // 视频管理对话框显示状态
      videoManageVisible: false,
      // 当前操作的合集
      currentCollection: {},
      // 封面图片上传URL - 使用Worker通用上传接口
      uploadCoverUrl: "https://chigua-r2-worker.xingaikaka.workers.dev/upload"
    };
  },
  created() {
    this.getList();
    this.getCategoryOptions();
  },
  methods: {
    /** 查询合集列表 */
    getList() {
      this.loading = true;
      listCollection(this.queryParams).then(response => {
        this.collectionList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 获取分类选项 */
    getCategoryOptions() {
      getAllCategories().then(response => {
        this.categoryOptions = response.data;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        title: null,
        description: null,
        coverImage: null,
        author: null,
        categoryIds: [],
        viewCount: 0,
        videoCount: 0,
        sortOrder: 0,
        status: 1
      };
      this.resetForm("form");
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
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加合集";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getCollection(id).then(response => {
        this.form = response.data;
        // 处理分类ID数组
        if (this.form.categoryId) {
          this.form.categoryIds = this.form.categoryId.split(',').map(id => parseInt(id));
        }
        this.open = true;
        this.title = "修改合集";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 处理分类ID
          if (this.form.categoryIds && this.form.categoryIds.length > 0) {
            this.form.categoryId = this.form.categoryIds.join(',');
          } else {
            this.form.categoryId = null;
          }
          
          // 🔧 处理封面图片URL（如果是Worker签名URL，转换为资源键）
          let coverImage = this.form.coverImage;
          if (coverImage && coverImage.includes('chigua-r2-worker.xingaikaka.workers.dev')) {
            // 从Worker URL中提取资源键
            try {
              const urlObj = new URL(coverImage);
              const key = urlObj.searchParams.get('key');
              if (key) {
                // 确保资源键格式正确（files/xxx）
                coverImage = key.startsWith('files/') ? key : `files/${key}`;
                console.log('🔄 合集封面URL转换:', {
                  original: this.form.coverImage,
                  converted: coverImage
                });
              }
            } catch (e) {
              console.warn('⚠️ 解析封面URL失败，保持原样:', this.form.coverImage);
            }
          }
          
          // 准备提交数据
          const submitData = {
            ...this.form,
            coverImage: coverImage
          };
          
          if (this.form.id != null) {
            updateCollection(submitData).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addCollection(submitData).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除合集编号为"' + ids + '"的数据项？').then(function() {
        return delCollection(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('chigua/collection/export', {
        ...this.queryParams
      }, `collection_${new Date().getTime()}.xlsx`)
    },
    /** 预览按钮操作 */
    handlePreview(row) {
      this.currentCollection = row;
      this.previewVisible = true;
    },
    /** 管理视频按钮操作 */
    handleManageVideos(row) {
      this.currentCollection = row;
      this.videoManageVisible = true;
    }
  }
};
</script> 