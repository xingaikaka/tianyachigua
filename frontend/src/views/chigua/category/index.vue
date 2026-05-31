<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="分类名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入分类名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="分类状态" clearable>
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="视频合集" prop="isCollection">
        <el-select v-model="queryParams.isCollection" placeholder="是否视频合集" clearable>
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="短视频" prop="isShort">
        <el-select v-model="queryParams.isShort" placeholder="是否短视频" clearable>
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否推荐" prop="isRecommended">
        <el-select v-model="queryParams.isRecommended" placeholder="是否推荐" clearable>
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户组" prop="isUserGroup">
        <el-select v-model="queryParams.isUserGroup" placeholder="是否用户组" clearable>
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="Telegram" prop="isTelegram">
        <el-select v-model="queryParams.isTelegram" placeholder="是否Telegram模式" clearable>
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
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
          v-hasPermi="['chigua:category:add']"
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
          v-hasPermi="['chigua:category:edit']"
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
          v-hasPermi="['chigua:category:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['chigua:category:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="categoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分类ID" align="center" prop="id" />
      <el-table-column label="分类名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="分类描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="排序权重" align="center" prop="sortOrder" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="视频合集" align="center" prop="isCollection">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isCollection === 1 ? 'warning' : 'info'" size="small">
            {{ scope.row.isCollection === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="短视频" align="center" prop="isShort">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isShort === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.isShort === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="推荐" align="center" prop="isRecommended">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isRecommended === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.isRecommended === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="用户组" align="center" prop="isUserGroup">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isUserGroup === 1 ? 'primary' : 'info'" size="small">
            {{ scope.row.isUserGroup === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Telegram" align="center" prop="isTelegram">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isTelegram === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.isTelegram === 1 ? '是' : '否' }}
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
            v-hasPermi="['chigua:category:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['chigua:category:remove']"
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

    <!-- 添加或修改分类对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="分类状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="视频合集">
          <el-radio-group v-model="form.isCollection">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="短视频">
          <el-radio-group v-model="form.isShort">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否推荐">
          <el-radio-group v-model="form.isRecommended">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户组">
          <el-radio-group v-model="form.isUserGroup">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Telegram模式">
          <el-radio-group v-model="form.isTelegram">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否分页模式">
          <el-radio-group v-model="form.isPagination">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="广告显示模式" prop="adDisplayMode">
          <el-select v-model="form.adDisplayMode" placeholder="请选择广告显示模式" @change="handleAdModeChange">
            <el-option
              v-for="dict in dict.type.ad_display_mode"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="广告间隔" prop="adInterval" v-if="form.adDisplayMode === 2">
          <el-input-number 
            v-model="form.adInterval" 
            :min="1" 
            :max="20" 
            controls-position="right" 
            placeholder="每几个内容项插入一个广告"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            交替显示模式下，每 {{ form.adInterval || 3 }} 个内容项之间插入一个广告
          </div>
        </el-form-item>
        <el-form-item label="分类描述">
          <el-input v-model="form.description" type="textarea" placeholder="请输入分类描述"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCategory, getCategory, delCategory, addCategory, updateCategory } from "@/api/chigua/category";

export default {
  name: "Category",
  dicts: ['ad_display_mode'],
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
      // 分类表格数据
      categoryList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
        status: null,
        isCollection: null,
        isRecommended: null,
        isShort: null,
        isUserGroup: null,
        isTelegram: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "分类名称不能为空", trigger: "blur" }
        ],
        sortOrder: [
          { required: true, message: "排序权重不能为空", trigger: "blur" }
        ],
        adDisplayMode: [
          { required: true, message: "请选择广告显示模式", trigger: "change" }
        ],
        adInterval: [
          { 
            required: true, 
            message: "广告间隔不能为空", 
            trigger: "blur",
            validator: (rule, value, callback) => {
              if (this.form.adDisplayMode === 2) {
                if (!value || value < 1 || value > 20) {
                  callback(new Error('交替显示模式下，广告间隔必须在1-20之间'));
                } else {
                  callback();
                }
              } else {
                callback();
              }
            }
          }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询分类列表 */
    getList() {
      this.loading = true;
      listCategory(this.queryParams).then(response => {
        this.categoryList = response.rows;
        this.total = response.total;
        this.loading = false;
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
        name: null,
        description: null,
        sortOrder: 0,
        status: 1,
        adDisplayMode: 1,
        adInterval: 3,
        isCollection: 0,
        isRecommended: 0,
        isShort: 0,
        isPagination: 0,
        isUserGroup: 0,
        isTelegram: 0
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
      this.title = "添加分类";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getCategory(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改分类";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateCategory(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addCategory(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除分类编号为"' + ids + '"的数据项？').then(function() {
        return delCategory(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 广告显示模式变化处理 */
    handleAdModeChange(value) {
      // 当选择"交替显示"时，设置默认间隔
      if (value === 2) {
        if (!this.form.adInterval || this.form.adInterval < 1) {
          this.form.adInterval = 3;
        }
      }
      // 当选择"不显示广告"时，间隔值不重要，但保持一个合理的默认值
      else if (value === 3) {
        this.form.adInterval = 1;
      }
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('chigua/category/export', {
        ...this.queryParams
      }, `category_${new Date().getTime()}.xlsx`)
    }
  }
};
</script> 