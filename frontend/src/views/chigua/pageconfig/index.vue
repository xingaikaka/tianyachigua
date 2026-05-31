<template>
  <div class="app-container">
    <!-- 搜索条件 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="配置名称" prop="configName">
        <el-input
          v-model="queryParams.configName"
          placeholder="请输入配置名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="配置类型" prop="configType">
        <el-select v-model="queryParams.configType" placeholder="配置类型" clearable>
          <el-option
            v-for="dict in dict.type.page_config_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="配置分类" prop="configCategory">
        <el-select v-model="queryParams.configCategory" placeholder="配置分类" clearable>
          <el-option
            v-for="dict in dict.type.page_config_category"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="配置状态" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
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
          v-hasPermi="['chigua:pageconfig:add']"
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
          v-hasPermi="['chigua:pageconfig:edit']"
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
          v-hasPermi="['chigua:pageconfig:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['chigua:pageconfig:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="handleRefreshCache"
          v-hasPermi="['chigua:pageconfig:edit']"
        >刷新缓存</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="pageConfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="配置ID" align="center" prop="configId" width="80" />
      <el-table-column label="配置键值" align="center" prop="configKey" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="配置名称" align="center" prop="configName" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="配置类型" align="center" prop="configType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.page_config_type" :value="scope.row.configType"/>
        </template>
      </el-table-column>
      <el-table-column label="配置分类" align="center" prop="configCategory" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.page_config_category" :value="scope.row.configCategory"/>
        </template>
      </el-table-column>
      <el-table-column label="内容预览" align="center" prop="content" width="200" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span v-if="scope.row.configType === 'rich_text'">{{ getPlainText(scope.row.richContent) }}</span>
          <span v-else-if="scope.row.configType === 'category_more'">
            {{ scope.row.basicContent }}
            <br/>
            <span style="color: #909399; font-size: 12px;">{{ scope.row.jumpUrl }}</span>
          </span>
          <span v-else>{{ scope.row.basicContent }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['chigua:pageconfig:edit']"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['chigua:pageconfig:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['chigua:pageconfig:remove']"
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

    <!-- 添加或修改页面配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="配置键值" prop="configKey">
              <el-input v-model="form.configKey" placeholder="请输入配置键值" :disabled="!!form.configId" />
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">
                配置键值用于前端调用，创建后不可修改
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="配置名称" prop="configName">
              <el-input v-model="form.configName" placeholder="请输入配置名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="配置类型" prop="configType">
              <el-select v-model="form.configType" placeholder="请选择配置类型" @change="handleTypeChange">
                <el-option
                  v-for="dict in dict.type.page_config_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
                <el-option label="图片配置" value="image" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.configType !== 'category_more'">
            <el-form-item label="配置分类" prop="configCategory">
              <el-select v-model="form.configCategory" placeholder="请选择配置分类">
                <el-option
                  v-for="dict in dict.type.page_config_category"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 富文本内容 -->
        <el-form-item 
          v-if="form.configType === 'rich_text'" 
          label="富文本内容" 
          prop="richContent"
        >
          <editor v-model="form.richContent" :min-height="192"/>
        </el-form-item>
        
        <!-- 基础文本内容 -->
        <el-form-item 
          v-if="form.configType === 'basic_config'" 
          label="基础内容" 
          prop="basicContent"
        >
          <el-input 
            v-model="form.basicContent" 
            type="textarea" 
            placeholder="请输入基础文本内容" 
            :rows="4"
          />
        </el-form-item>
        
        <!-- 图片配置 -->
        <el-form-item
          v-if="form.configType === 'image'"
          label="图片"
          prop="basicContent"
        >
          <image-upload
            v-model="form.basicContent"
            :file-type="['png', 'jpg', 'jpeg', 'gif', 'webp']"
            :file-size="10"
            :limit="1"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            图片将加密上传，自动按CDN配置生成访问地址
          </div>
        </el-form-item>
        
        <!-- 分类更多类型的内容 -->
        <el-row v-if="form.configType === 'category_more'">
          <el-col :span="12">
            <el-form-item label="显示文字" prop="basicContent">
              <el-input 
                v-model="form.basicContent" 
                placeholder="请输入显示文字" 
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="跳转地址" prop="jumpUrl">
              <el-input 
                v-model="form.jumpUrl" 
                placeholder="请输入跳转地址，如：/page 或 https://example.com" 
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
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
import { 
  listPageConfig, 
  getPageConfig, 
  delPageConfig, 
  addPageConfig, 
  updatePageConfig,
  refreshCache,
  clearCache
} from "@/api/chigua/pageconfig";

export default {
  name: "PageConfig",
  dicts: ['page_config_type', 'page_config_category'],
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
      // 页面配置表格数据
      pageConfigList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: null,
        configType: null,
        configCategory: null,
        status: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        configKey: [
          { required: true, message: "配置键值不能为空", trigger: "blur" },
          { min: 1, max: 100, message: "配置键值长度必须介于1和100之间", trigger: "blur" },
          { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: "配置键值只能包含字母、数字和下划线，且必须以字母开头", trigger: "blur" }
        ],
        configName: [
          { required: true, message: "配置名称不能为空", trigger: "blur" },
          { min: 1, max: 200, message: "配置名称长度必须介于1和200之间", trigger: "blur" }
        ],
        configType: [
          { required: true, message: "配置类型不能为空", trigger: "change" }
        ],
        configCategory: [
          { required: true, message: "配置分类不能为空", trigger: "change" }
        ],
        richContent: [
          { required: true, message: "富文本内容不能为空", trigger: "blur" }
        ],
        basicContent: [
          { required: true, message: "基础内容不能为空", trigger: "blur" }
        ],
        jumpUrl: [
          { max: 500, message: "跳转地址长度不能超过500个字符", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询页面配置列表 */
    getList() {
      this.loading = true;
      listPageConfig(this.queryParams).then(response => {
        this.pageConfigList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
    },
    
    /** 表单重置 */
    reset() {
      this.form = {
        configId: null,
        configKey: null,
        configName: null,
        configType: null,
        richContent: null,
        basicContent: null,
        jumpUrl: null,
        configCategory: null,
        status: "0",
        sortOrder: 0,
        remark: null
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
    
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.configId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加页面配置";
    },
    
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const configId = row.configId || this.ids[0];
      getPageConfig(configId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改页面配置";
      });
    },
    
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // image类型：提交前提取R2 key，不保存完整签名URL
          const submitForm = { ...this.form };
          if (submitForm.configType === 'image' && submitForm.basicContent) {
            submitForm.basicContent = this.extractR2Key(submitForm.basicContent);
          }
          if (submitForm.configId != null) {
            updatePageConfig(submitForm).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addPageConfig(submitForm).then(response => {
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
      const configIds = row.configId ? [row.configId] : this.ids;
      this.$modal.confirm('是否确认删除页面配置编号为"' + configIds + '"的数据项？').then(function() {
        return delPageConfig(configIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    
    /** 导出按钮操作 */
    handleExport() {
      this.download('chigua/pageconfig/export', {
        ...this.queryParams
      }, `pageconfig_${new Date().getTime()}.xlsx`)
    },
    
    /** 配置类型变化处理 */
    handleTypeChange(value) {
      // 清除另一种类型的内容
      if (value === 'rich_text') {
        this.form.basicContent = null;
        this.form.jumpUrl = null;
      } else if (value === 'basic_config') {
        this.form.richContent = null;
        this.form.jumpUrl = null;
      } else if (value === 'category_more') {
        this.form.richContent = null;
        this.form.configCategory = 'nav_menu'; // 自动设置为导航菜单分类
      } else if (value === 'image') {
        this.form.richContent = null;
        this.form.jumpUrl = null;
      }
    },
    
    /** 从签名URL中提取R2资源key */
    extractR2Key(url) {
      if (!url || !url.startsWith('http')) return url;
      try {
        const urlObj = new URL(url);
        const key = urlObj.searchParams.get('key');
        if (key) return key;
        // 没有key参数时，取路径部分（去掉开头/files/）
        const pathname = urlObj.pathname;
        return pathname.startsWith('/files/') ? pathname.substring(7) : pathname.substring(1);
      } catch (e) {
        return url;
      }
    },
    
    /** 状态变化处理 */
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + row.configName + '"配置吗？').then(function() {
        return updatePageConfig(row);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(function() {
        row.status = row.status === "0" ? "1" : "0";
      });
    },
    
    /** 刷新缓存 */
    handleRefreshCache() {
      this.$modal.confirm('确认要刷新页面配置缓存吗？刷新后前端将获取最新配置数据。').then(function() {
        return refreshCache();
      }).then(() => {
        this.$modal.msgSuccess("缓存刷新成功");
      }).catch(() => {});
    },
    
    /** 获取纯文本内容 */
    getPlainText(html) {
      if (!html) return '';
      const div = document.createElement('div');
      div.innerHTML = html;
      const text = div.textContent || div.innerText || '';
      return text.length > 50 ? text.substring(0, 50) + '...' : text;
    }
  }
};
</script>