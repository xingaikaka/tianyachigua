<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="视频ID" prop="videoId">
        <el-input
          v-model="queryParams.videoId"
          placeholder="请输入视频ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="评论内容" prop="content">
        <el-input
          v-model="queryParams.content"
          placeholder="请输入评论内容"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="评论类型" prop="commentType">
        <el-select v-model="queryParams.commentType" placeholder="请选择评论类型" clearable>
          <el-option label="视频评论" value="video" />
          <el-option label="投稿评论" value="submission" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否置顶" prop="isSticky">
        <el-select v-model="queryParams.isSticky" placeholder="是否置顶" clearable>
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="mb8">
      <el-col :span="6">
        <el-card class="statistics-card">
          <div class="statistics-item">
            <div class="statistics-value">{{ statistics.total || 0 }}</div>
            <div class="statistics-label">总评论数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="statistics-card">
          <div class="statistics-item">
            <div class="statistics-value pending">{{ statistics.pending || 0 }}</div>
            <div class="statistics-label">待审核</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="statistics-card">
          <div class="statistics-item">
            <div class="statistics-value approved">{{ statistics.approved || 0 }}</div>
            <div class="statistics-label">已通过</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="statistics-card">
          <div class="statistics-item">
            <div class="statistics-value rejected">{{ statistics.rejected || 0 }}</div>
            <div class="statistics-label">已拒绝</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-check"
          size="mini"
          :disabled="multiple"
          @click="handleBatchAudit(1)"
          v-hasPermi="['chigua:comment:audit','system:comment:audit']"
        >批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-close"
          size="mini"
          :disabled="multiple"
          @click="handleBatchAudit(2)"
          v-hasPermi="['chigua:comment:audit','system:comment:audit']"
        >批量拒绝</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['chigua:comment:remove','system:comment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleExport"
          v-hasPermi="['chigua:comment:export','system:comment:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" :fixed="useFixed ? 'left' : false" />
      <el-table-column label="ID" align="center" prop="id" width="80" :fixed="useFixed ? 'left' : false" />
      <el-table-column label="视频ID" align="center" prop="videoId" width="100" />
      <el-table-column label="评论类型" align="center" prop="commentType" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.commentType === 'video' ? 'primary' : 'success'" size="mini">
            {{ scope.row.commentType === 'video' ? '视频评论' : '投稿评论' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="用户名" align="center" prop="username" width="120" />
      <el-table-column label="评论内容" align="center" prop="content" width="300" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag
            :type="scope.row.status === 1 ? 'success' : (scope.row.status === 2 ? 'danger' : 'warning')"
            disable-transitions
          >
            {{ scope.row.status === 0 ? '待审核' : (scope.row.status === 1 ? '已通过' : '已拒绝') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" align="center" prop="isSticky" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isSticky === 1" type="primary" size="mini">置顶</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="回复数" align="center" prop="replyCount" width="80" />
      <el-table-column label="点赞数" align="center" prop="likeCount" width="80" />
      <el-table-column label="IP地址" align="center" prop="ipAddress" width="120" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核时间" align="center" prop="auditTime" width="160">
        <template slot-scope="scope">
          <span v-if="scope.row.auditTime">{{ parseTime(scope.row.auditTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200" :fixed="useFixed ? 'right' : false">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['chigua:comment:query','system:comment:query']"
          >详情</el-button>
          <el-button
            v-if="scope.row.status === 0"
            size="mini"
            type="text"
            icon="el-icon-check"
            @click="handleAudit(scope.row, 1)"
            v-hasPermi="['chigua:comment:audit','system:comment:audit']"
          >通过</el-button>
          <el-button
            v-if="scope.row.status === 0"
            size="mini"
            type="text"
            icon="el-icon-close"
            @click="handleAudit(scope.row, 2)"
            v-hasPermi="['chigua:comment:audit','system:comment:audit']"
          >拒绝</el-button>
          <el-button
            v-if="scope.row.status === 1"
            size="mini"
            type="text"
            icon="el-icon-top"
            @click="handleSticky(scope.row)"
            v-hasPermi="['chigua:comment:edit','system:comment:edit']"
          >{{ scope.row.isSticky === 1 ? '取消置顶' : '置顶' }}</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['chigua:comment:remove','system:comment:remove']"
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

    <!-- 评论详情对话框 -->
    <el-dialog title="评论详情" :visible.sync="viewOpen" width="800px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="评论ID">{{ viewForm.id }}</el-descriptions-item>
        <el-descriptions-item label="视频ID">{{ viewForm.videoId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ viewForm.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ viewForm.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            :type="viewForm.status === 1 ? 'success' : (viewForm.status === 2 ? 'danger' : 'warning')"
          >
            {{ viewForm.status === 0 ? '待审核' : (viewForm.status === 1 ? '已通过' : '已拒绝') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="置顶状态">
          <el-tag v-if="viewForm.isSticky === 1" type="primary">置顶</el-tag>
          <span v-else>普通</span>
        </el-descriptions-item>
        <el-descriptions-item label="回复数">{{ viewForm.replyCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="点赞数">{{ viewForm.likeCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ viewForm.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ parseTime(viewForm.auditTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核人ID">{{ viewForm.auditUserId || '-' }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider>评论内容</el-divider>
      <div class="comment-content">{{ viewForm.content }}</div>
      
      <el-divider v-if="viewForm.auditRemark">审核备注</el-divider>
      <div v-if="viewForm.auditRemark" class="audit-remark">{{ viewForm.auditRemark }}</div>
      
      <el-divider v-if="viewForm.userAgent">用户代理</el-divider>
      <div v-if="viewForm.userAgent" class="user-agent">{{ viewForm.userAgent }}</div>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog title="审核评论" :visible.sync="auditOpen" width="500px" append-to-body>
      <el-form ref="auditForm" :model="auditForm" label-width="80px">
        <el-form-item label="审核状态">
          <el-radio-group v-model="auditForm.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入审核备注（可选）"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="auditOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitAudit">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 批量审核对话框 -->
    <el-dialog title="批量审核评论" :visible.sync="batchAuditOpen" width="500px" append-to-body>
      <el-form ref="batchAuditForm" :model="batchAuditForm" label-width="80px">
        <el-form-item label="选中评论">
          <span>已选中 {{ ids.length }} 条评论</span>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-radio-group v-model="batchAuditForm.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="batchAuditForm.auditRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入审核备注（可选）"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="batchAuditOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitBatchAudit">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listComment, getComment, delComment, auditComment, batchAuditComment, stickyComment, getCommentStatistics } from "@/api/chigua/comment";

export default {
  name: "Comment",
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
      // 评论表格数据
      commentList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查看对话框
      viewOpen: false,
      // 审核对话框
      auditOpen: false,
      // 批量审核对话框
      batchAuditOpen: false,
      // 日期范围
      dateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        videoId: null,
        username: null,
        content: null,
        status: null,
        commentType: null,
        isSticky: null,
      },
      // 查看表单参数
      viewForm: {},
      // 审核表单参数
      auditForm: {
        id: null,
        status: null,
        auditRemark: null
      },
      // 批量审核表单参数
      batchAuditForm: {
        ids: [],
        status: null,
        auditRemark: null
      },
      // 统计信息
      statistics: {
        total: 0,
        pending: 0,
        approved: 0,
        rejected: 0
      },
      // 是否启用固定列（根据屏幕宽度自适应）
      useFixed: true
    };
  },
  created() {
    this.getList();
    // 根据屏幕宽度决定是否使用固定列，避免大屏缩放导致样式问题
    const decideFixed = () => {
      const width = window.innerWidth || document.documentElement.clientWidth;
      // 当可视宽度过小或系统缩放导致可用宽度紧张时，关闭固定列以避免错位
      this.useFixed = width >= 1440; // 1440px以上启用固定列
    };
    decideFixed();
    window.addEventListener('resize', decideFixed);
    this.$once('hook:beforeDestroy', () => window.removeEventListener('resize', decideFixed));
    this.getStatistics();
  },
  methods: {
    /** 查询评论列表 */
    getList() {
      this.loading = true;
      listComment(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.commentList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 获取统计信息 */
    getStatistics() {
      // 使用与列表查询相同的条件
      const params = { ...this.queryParams };
      delete params.pageNum;
      delete params.pageSize;
      
      getCommentStatistics(params).then(response => {
        this.statistics = response.data;
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
        videoId: null,
        parentId: null,
        username: null,
        email: null,
        content: null,
        ipAddress: null,
        userAgent: null,
        status: 0,
        replyCount: null,
        likeCount: null,
        isSticky: 0,
        auditUserId: null,
        auditTime: null,
        auditRemark: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
      this.getStatistics();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 查看详情 */
    handleView(row) {
      const id = row.id || this.ids
      getComment(id).then(response => {
        this.viewForm = response.data;
        this.viewOpen = true;
      });
    },
    /** 审核评论 */
    handleAudit(row, status) {
      this.auditForm = {
        id: row.id,
        status: status,
        auditRemark: null
      };
      this.auditOpen = true;
    },
    /** 提交审核 */
    submitAudit() {
      auditComment(this.auditForm.id, this.auditForm).then(response => {
        this.$modal.msgSuccess("审核成功");
        this.auditOpen = false;
        this.getList();
        this.getStatistics();
      });
    },
    /** 批量审核 */
    handleBatchAudit(status) {
      if (this.ids.length === 0) {
        this.$modal.msgError("请选择要审核的评论");
        return;
      }
      this.batchAuditForm = {
        ids: this.ids,
        status: status,
        auditRemark: null
      };
      this.batchAuditOpen = true;
    },
    /** 提交批量审核 */
    submitBatchAudit() {
      batchAuditComment(this.batchAuditForm).then(response => {
        this.$modal.msgSuccess("批量审核成功");
        this.batchAuditOpen = false;
        this.getList();
        this.getStatistics();
      });
    },
    /** 置顶/取消置顶 */
    handleSticky(row) {
      const isSticky = row.isSticky === 1 ? 0 : 1;
      const action = isSticky === 1 ? '置顶' : '取消置顶';
      
      this.$modal.confirm(`确认要${action}该评论吗？`).then(function() {
        return stickyComment(row.id, { isSticky: isSticky });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(`${action}成功`);
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除评论编号为"' + ids + '"的数据项？').then(function() {
        return delComment(ids);
      }).then(() => {
        this.getList();
        this.getStatistics();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/comment/export', {
        ...this.queryParams
      }, `comment_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

<style scoped>
.statistics-card {
  margin-bottom: 20px;
}

.statistics-item {
  text-align: center;
  padding: 10px;
}

.statistics-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.statistics-value.pending {
  color: #e6a23c;
}

.statistics-value.approved {
  color: #67c23a;
}

.statistics-value.rejected {
  color: #f56c6c;
}

.statistics-label {
  font-size: 14px;
  color: #606266;
}

.comment-content {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.audit-remark {
  padding: 10px;
  background-color: #fff6f6;
  border-left: 4px solid #f56c6c;
  border-radius: 4px;
}

.user-agent {
  padding: 10px;
  background-color: #f0f9ff;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}
</style> 