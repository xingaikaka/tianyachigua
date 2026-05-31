<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="广告标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入广告标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="广告类型" prop="adType">
        <el-select v-model="queryParams.adType" placeholder="请选择广告类型" clearable>
          <el-option
            v-for="dict in dict.type.ad_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="广告位置" prop="position">
        <el-select v-model="queryParams.position" placeholder="请选择广告位置" clearable>
          <el-option
            v-for="dict in dict.type.ad_position"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="广告状态" clearable>
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['chigua:advertisement:add']"
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
          v-hasPermi="['chigua:advertisement:edit']"
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
          v-hasPermi="['chigua:advertisement:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['chigua:advertisement:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="advertisementList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="广告ID" align="center" prop="id" />
      <el-table-column label="广告标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="广告类型" align="center" prop="adType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ad_type" :value="scope.row.adType"/>
        </template>
      </el-table-column>
      <el-table-column label="应用类型" align="center" prop="appType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.app_type" :value="scope.row.appType" v-if="scope.row.appType"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="广告位置" align="center" prop="position">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ad_position" :value="scope.row.position" v-if="scope.row.position"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="图标名称" align="center" prop="iconName">
        <template slot-scope="scope">
          <span v-if="scope.row.iconName">{{ scope.row.iconName }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="投放范围" align="center" prop="isGlobal">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isGlobal === 1 ? 'success' : 'info'">
            {{ scope.row.isGlobal === 1 ? '全站投放' : '分类投放' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="点击/展示" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.clickCount || 0 }} / {{ scope.row.impressionCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['chigua:advertisement:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['chigua:advertisement:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改广告对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="广告标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入广告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="广告描述">
              <el-input v-model="form.description" type="textarea" placeholder="请输入广告描述"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="广告类型" prop="adType">
              <el-select v-model="form.adType" placeholder="请选择广告类型" @change="handleAdTypeChange">
                <el-option
                  v-for="dict in dict.type.ad_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.adType === '2'">
            <el-form-item label="应用类型" prop="appType">
              <el-select v-model="form.appType" placeholder="请选择应用类型">
                <el-option
                  v-for="dict in dict.type.app_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.adType === '1' && form.adType !== '5' && form.adType !== '6'">
            <el-form-item label="广告位置" prop="position">
              <el-select v-model="form.position" placeholder="请选择广告位置" @change="handlePositionChange">
                <el-option
                  v-for="dict in dict.type.ad_position"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="shouldShowCategorySelection">
            <el-form-item label="选择分类" prop="categoryId">
              <div style="margin-bottom: 10px;">
                <el-checkbox 
                  v-model="checkAll" 
                  @change="handleCheckAllChange"
                  :indeterminate="isIndeterminate"
                >全选</el-checkbox>
              </div>
              <el-checkbox-group v-model="selectedCategories" @change="handleCheckedCategoriesChange">
                <el-checkbox
                  v-for="category in categoryList"
                  :key="category.id"
                  :label="category.id"
                >{{ category.name }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="showImageUpload">
            <el-form-item label="广告图片">
              <image-upload v-model="form.imageUrl" :limit="1" :action="uploadImageUrl" :file-type="['png', 'jpg', 'jpeg', 'gif', 'webp']" :file-size="10" />
              <div class="el-form-item__extra" style="color:#999;font-size:12px;margin-top:4px;">
                请上传广告图片，上传后将自动走加密CDN处理
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.adType === '2'">
            <el-form-item label="图标名称">
              <el-input v-model="form.iconName" placeholder="请输入图标名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="跳转链接">
              <el-input v-model="form.linkUrl" placeholder="请输入跳转链接" />
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.adType === '3'">
            <el-form-item label="链接文字" prop="linkText">
              <el-input v-model="form.linkText" placeholder="请输入链接文字" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="投放开始时间">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                placeholder="选择开始时间"
                format="yyyy-MM-dd"
                value-format="yyyy-MM-dd">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="投放结束时间">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                placeholder="选择结束时间"
                format="yyyy-MM-dd"
                value-format="yyyy-MM-dd">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序权重">
              <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="广告状态">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAdvertisement, getAdvertisement, delAdvertisement, addAdvertisement, updateAdvertisement, getCategoryList } from "@/api/chigua/advertisement";

export default {
  name: "Advertisement",
  dicts: ['ad_type', 'app_type', 'ad_position'],
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
      // 广告表格数据
      advertisementList: [],
      // 分类列表
      categoryList: [],
      // 选中的分类
      selectedCategories: [],
      // 全选状态
      checkAll: false,
      // 半选状态
      isIndeterminate: false,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        adType: null,
        position: null,
        status: null
      },
      // 表单参数
      form: {},
      // 广告图片上传地址（使用 Worker 通用上传接口）
      uploadImageUrl: "https://chigua-r2-worker.xingaikaka.workers.dev/api/upload/richtext",
      // 表单校验
      rules: {
        title: [
          { required: true, message: "广告标题不能为空", trigger: "blur" }
        ],
        adType: [
          { required: true, message: "广告类型不能为空", trigger: "change" }
        ],
        position: [
          { 
            required: true, 
            message: "广告位置不能为空", 
            trigger: "change",
            validator: (rule, value, callback) => {
              // 只有横幅广告(adType=1)需要验证广告位置
              if (this.form.adType === '1') {
                if (!value) {
                  callback(new Error('广告位置不能为空'));
                } else {
                  callback();
                }
              } else {
                callback();
              }
            }
          }
        ],
        appType: [
          { required: true, message: "应用类型不能为空", trigger: "change" }
        ],
        linkText: [
          { required: true, message: "链接文字不能为空", trigger: "blur" }
        ]
      }
    };
  },
  computed: {
    showImageUpload() {
      return this.form.adType === '1' || this.form.adType === '2' || this.form.adType === '4' || this.form.adType === '5' || this.form.adType === '6' || this.form.adType === '7';
    },
    shouldShowCategorySelection() {
      // 短视频广告类型(adType=5)和分页模式广告类型(adType=6)不显示分类选择
      if (this.form.adType === '5' || this.form.adType === '6') {
        return false;
      }
      // 当选择"首页顶部横幅"(5)、"首页底部横幅"(6)或"搜索页面下方横幅"(7)时，隐藏分类选择
      return this.form.position !== '5' && this.form.position !== '6' && this.form.position !== '7';
    }
  },
  created() {
    this.getList();
    this.getCategoryList();
    // 检查URL参数，如果有id则自动打开编辑对话框
    if (this.$route.query.id) {
      this.handleAutoEdit(this.$route.query.id);
    }
  },
  methods: {
    /** 查询广告列表 */
    getList() {
      this.loading = true;
      listAdvertisement(this.queryParams).then(response => {
        this.advertisementList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 获取分类列表 */
    getCategoryList() {
      getCategoryList().then(response => {
        this.categoryList = response.data;
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
        adType: null,
        appType: null,
        position: null,
        categoryId: null,
        imageUrl: null,
        iconName: null,
        linkUrl: null,
        linkText: null,
        sortOrder: 0,
        startDate: null,
        endDate: null,
        status: 1,
        isGlobal: 0
      };
      this.selectedCategories = [];
      this.checkAll = false;
      this.isIndeterminate = false;
      // 安全检查，确保表单引用存在
      if (this.$refs.form) {
        this.resetForm("form");
      }
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      // 安全检查，确保表单引用存在
      if (this.$refs.queryForm) {
        this.resetForm("queryForm");
      }
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
      this.title = "添加广告";
      // 确保表单验证状态重置
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate();
        }
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getAdvertisement(id).then(response => {
        this.form = response.data;
        // 处理分类数据
        if (this.form.categoryId) {
          this.selectedCategories = this.form.categoryId.split(',').map(id => parseInt(id));
        } else {
          this.selectedCategories = [];
        }
        // 更新全选状态
        this.$nextTick(() => {
          this.handleCheckedCategoriesChange(this.selectedCategories);
        });
        this.open = true;
        this.title = "修改广告";
        // 确保表单验证状态重置
        this.$nextTick(() => {
          if (this.$refs.form) {
            this.$refs.form.clearValidate();
          }
        });
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 处理分类数据
          if (this.selectedCategories.length > 0) {
            this.form.categoryId = this.selectedCategories.join(',');
            this.form.isGlobal = 0; // 有选择分类则为指定分类投放
          } else {
            this.form.categoryId = null;
            this.form.isGlobal = 1; // 没有选择分类则为全分类投放
          }

          // 处理图片URL：统一转换为 R2 资源键后存库
          let imageUrl = this.form.imageUrl;
          if (imageUrl && imageUrl.startsWith('http')) {
            try {
              const urlObj = new URL(imageUrl);
              // 情况1: Worker 上传返回的URL（chigua-r2-worker.xingaikaka.workers.dev）
              // 情况2: CDN 签名URL（编辑时由后端返回，含 key= 参数）
              const key = urlObj.searchParams.get('key');
              if (key) {
                imageUrl = key.startsWith('files/') ? key : key;
              }
            } catch (e) {
              console.warn('⚠️ 解析广告图片URL失败，保持原样:', imageUrl);
            }
          }

          const submitData = { ...this.form, imageUrl };

          if (this.form.id != null) {
            updateAdvertisement(submitData).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addAdvertisement(submitData).then(response => {
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
      this.$modal.confirm('是否确认删除广告编号为"' + ids + '"的数据项？').then(function() {
        return delAdvertisement(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('chigua/advertisement/export', {
        ...this.queryParams
      }, `advertisement_${new Date().getTime()}.xlsx`)
    },
    /** 广告类型变化处理 */
    handleAdTypeChange(value) {
      // 清空相关字段
      this.form.appType = null;
      this.form.linkText = null;
      
      // 只有横幅广告需要广告位置，其他类型清空位置字段
      if (value !== '1') {
        this.form.position = null;
      }
      
      // 只有Logo广告需要图标名称，其他类型清空图标名称字段
      if (value !== '2') {
        this.form.iconName = null;
      }
      
      // 短视频广告类型处理
      if (value === '5') {
        // 短视频广告不需要分类和位置，清空相关字段
        this.form.position = null;
        this.selectedCategories = [];
        this.checkAll = false;
        this.isIndeterminate = false;
        this.form.categoryId = null;
        this.form.isGlobal = 1; // 设置为全局投放
      }
      
      // 分页模式广告类型处理
      if (value === '6') {
        // 分页模式广告不需要分类和位置，清空相关字段
        this.form.position = null;
        this.selectedCategories = [];
        this.checkAll = false;
        this.isIndeterminate = false;
        this.form.categoryId = null;
        this.form.isGlobal = 1; // 设置为全局投放
      }

      // 九宫格弹窗广告类型处理
      if (value === '7') {
        // 九宫格弹窗广告不需要分类和位置，清空相关字段
        this.form.position = null;
        this.selectedCategories = [];
        this.checkAll = false;
        this.isIndeterminate = false;
        this.form.categoryId = null;
        this.form.isGlobal = 1; // 设置为全局投放
      }
      
      // 根据广告类型设置默认值
      if (value === '2') {
        this.form.appType = '1'; // logo广告默认选择热门应用
      }
    },
    
    /** 广告位置变化处理 */
    handlePositionChange(value) {
      // 当选择首页横幅时，清空分类选择（因为首页横幅不需要分类）
      if (value === '5' || value === '6') { // 首页顶部横幅或首页底部横幅
        this.selectedCategories = [];
        this.checkAll = false;
        this.isIndeterminate = false;
        this.form.categoryId = null;
        this.form.isGlobal = 1; // 设置为全局投放
      }
    },
    /** 全选变化处理 */
    handleCheckAllChange(val) {
      this.selectedCategories = val ? this.categoryList.map(item => item.id) : [];
      this.isIndeterminate = false;
    },
    
    /** 分类选择变化处理 */
    handleCheckedCategoriesChange(value) {
      let checkedCount = value.length;
      this.checkAll = checkedCount === this.categoryList.length;
      this.isIndeterminate = checkedCount > 0 && checkedCount < this.categoryList.length;
    },
    
    /** 自动编辑广告（通过URL参数） */
    handleAutoEdit(adId) {
      // 延迟执行，确保数据列表已加载完成
      setTimeout(() => {
        this.reset();
        getAdvertisement(adId).then(response => {
          this.form = response.data;
          // 处理分类数据
          if (this.form.categoryId) {
            this.selectedCategories = this.form.categoryId.split(',').map(id => parseInt(id));
          } else {
            this.selectedCategories = [];
          }
          // 更新全选状态
          this.$nextTick(() => {
            this.handleCheckedCategoriesChange(this.selectedCategories);
          });
          this.open = true;
          this.title = "修改广告";
          // 确保表单验证状态重置
          this.$nextTick(() => {
            if (this.$refs.form) {
              this.$refs.form.clearValidate();
            }
          });
        }).catch(error => {
          this.$modal.msgError("加载广告信息失败");
          console.error('Error loading advertisement:', error);
        });
      }, 500);
    }
  }
};
</script> 