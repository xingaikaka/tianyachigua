<template>
  <div class="app-container">
    <!-- 统计信息 -->
    <el-row :gutter="20" class="mb8">
      <el-col :span="8">
        <el-card class="statistics-card">
          <div class="statistics-item">
            <div class="statistics-value pending">{{ pendingCount || 0 }}</div>
            <div class="statistics-label">待审核评论</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="statistics-card">
          <div class="statistics-item">
            <div class="statistics-value">{{ todayCount || 0 }}</div>
            <div class="statistics-label">今日新增</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="statistics-card">
          <div class="statistics-item">
            <div class="statistics-value">{{ auditedCount || 0 }}</div>
            <div class="statistics-label">今日已审核</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
      <el-form-item label="评论类型" prop="commentType">
        <el-select v-model="queryParams.commentType" placeholder="请选择评论类型" clearable>
          <el-option label="视频评论" value="video" />
          <el-option label="投稿评论" value="submission" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 待审核评论列表 -->
    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange" class="audit-table">
      <el-table-column type="selection" width="55" align="center" fixed="left" />
      <el-table-column label="ID" align="center" prop="id" width="80" fixed="left" />
      <el-table-column label="视频ID" align="center" prop="videoId" width="100" />
      <el-table-column label="评论类型" align="center" prop="commentType" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.commentType === 'video' ? 'primary' : 'success'" size="mini">
            {{ scope.row.commentType === 'video' ? '视频评论' : '投稿评论' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="用户信息" align="center" width="140">
        <template slot-scope="scope">
          <div class="user-info">
            <div class="username">{{ scope.row.username }}</div>
            <div class="email" v-if="scope.row.email">{{ scope.row.email }}</div>
            <div class="ip">{{ scope.row.ipAddress }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="评论内容" align="left" prop="content" min-width="400">
        <template slot-scope="scope">
          <div class="comment-preview">
            <div class="content-text">{{ scope.row.content }}</div>
            <div class="content-meta">
              <el-tag size="mini" type="info">{{ scope.row.content.length }} 字</el-tag>
              <span class="create-time">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template slot-scope="scope">
          <el-tag type="warning" disable-transitions>待审核</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="success"
            icon="el-icon-check"
            @click="handleQuickAudit(scope.row, 1)"
            v-hasPermi="['chigua:comment:audit','system:comment:audit']"
          >通过</el-button>
          <el-button
            size="mini"
            type="warning"
            icon="el-icon-close"
            @click="handleQuickAudit(scope.row, 2)"
            v-hasPermi="['chigua:comment:audit','system:comment:audit']"
          >拒绝</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['chigua:comment:query','system:comment:query']"
          >详情</el-button>
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
        <el-descriptions-item label="IP地址">{{ viewForm.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="父评论ID">{{ viewForm.parentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="warning">待审核</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      
      <el-divider>评论内容</el-divider>
      <div class="comment-content">{{ viewForm.content }}</div>
      
      <el-divider v-if="viewForm.userAgent">用户代理</el-divider>
      <div v-if="viewForm.userAgent" class="user-agent">{{ viewForm.userAgent }}</div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关闭</el-button>
        <el-button type="success" @click="handleAuditFromDetail(1)">通过</el-button>
        <el-button type="warning" @click="handleAuditFromDetail(2)">拒绝</el-button>
      </div>
    </el-dialog>

    <!-- 快速审核对话框 -->
    <el-dialog title="审核评论" :visible.sync="auditOpen" width="500px" append-to-body>
      <div class="audit-preview">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ auditForm.username }}</el-descriptions-item>
          <el-descriptions-item label="评论内容">
            <div class="preview-content">{{ auditForm.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <el-form ref="auditFormRef" :model="auditForm" label-width="80px" style="margin-top: 20px;">
        <el-form-item label="审核状态">
          <el-radio-group v-model="auditForm.status">
            <el-radio :label="1" :style="{ color: '#67c23a' }">
              <i class="el-icon-check"></i> 通过
            </el-radio>
            <el-radio :label="2" :style="{ color: '#f56c6c' }">
              <i class="el-icon-close"></i> 拒绝
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="3"
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
      <el-alert
        :title="`您选择了 ${ids.length} 条待审核评论`"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;">
      </el-alert>
      
      <el-form ref="batchAuditForm" :model="batchAuditForm" label-width="80px">
        <el-form-item label="审核状态">
          <el-radio-group v-model="batchAuditForm.status">
            <el-radio :label="1" :style="{ color: '#67c23a' }">
              <i class="el-icon-check"></i> 通过
            </el-radio>
            <el-radio :label="2" :style="{ color: '#f56c6c' }">
              <i class="el-icon-close"></i> 拒绝
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="batchAuditForm.auditRemark"
            type="textarea"
            :rows="3"
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
import { listPendingComment, getComment, delComment, auditComment, batchAuditComment, getCommentStatistics } from "@/api/chigua/comment";

export default {
  name: "CommentAudit",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 评论表格数据
      commentList: [],
      // 查看对话框
      viewOpen: false,
      // 审核对话框
      auditOpen: false,
      // 批量审核对话框
      batchAuditOpen: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        videoId: null,
        username: null,
        content: null,
        commentType: null,
      },
      // 查看表单参数
      viewForm: {},
      // 审核表单参数
      auditForm: {
        id: null,
        username: null,
        content: null,
        status: null,
        auditRemark: null
      },
      // 批量审核表单参数
      batchAuditForm: {
        ids: [],
        status: null,
        auditRemark: null
      },
      // 统计数据
      pendingCount: 0,
      todayCount: 0,
      auditedCount: 0
    };
  },
  created() {
    this.getList();
    this.getStatistics();
  },
  methods: {
    /** 查询待审核评论列表 */
    getList() {
      this.loading = true;
      listPendingComment(this.queryParams).then(response => {
        this.commentList = response.rows;
        this.total = response.total;
        this.pendingCount = response.total;
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
        const data = response.data;
        this.pendingCount = data.pending || 0;
        // 这里可以扩展为获取今日统计
        this.todayCount = 0;
        this.auditedCount = 0;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
      this.getStatistics();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
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
    /** 快速审核 */
    handleQuickAudit(row, status) {
      this.auditForm = {
        id: row.id,
        username: row.username,
        content: row.content,
        status: status,
        auditRemark: null
      };
      this.auditOpen = true;
    },
    /** 从详情页面审核 */
    handleAuditFromDetail(status) {
      this.auditForm = {
        id: this.viewForm.id,
        username: this.viewForm.username,
        content: this.viewForm.content,
        status: status,
        auditRemark: null
      };
      this.viewOpen = false;
      this.auditOpen = true;
    },
    /** 提交审核 */
    submitAudit() {
      auditComment(this.auditForm.id, {
        status: this.auditForm.status,
        auditRemark: this.auditForm.auditRemark
      }).then(response => {
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
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row ? row.id : this.ids;
      this.$modal.confirm('是否确认删除选中的评论？').then(function() {
        return delComment(ids);
      }).then(() => {
        this.getList();
        this.getStatistics();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
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
  padding: 15px;
}

.statistics-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}

.statistics-value.pending {
  color: #e6a23c;
}

.statistics-label {
  font-size: 14px;
  color: #606266;
}

.audit-table {
  margin-top: 20px;
}

.user-info {
  text-align: left;
}

.user-info .username {
  font-weight: bold;
  color: #303133;
  margin-bottom: 2px;
}

.user-info .email {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.user-info .ip {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-preview {
  text-align: left;
}

.content-text {
  line-height: 1.6;
  margin-bottom: 8px;
  max-height: 60px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.content-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.create-time {
  font-size: 12px;
  color: #909399;
}

.comment-content {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
}

.user-agent {
  padding: 10px;
  background-color: #f0f9ff;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.audit-preview {
  margin-bottom: 20px;
}

.preview-content {
  max-height: 100px;
  overflow-y: auto;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.el-radio {
  margin-right: 30px;
}

.el-radio__label {
  font-weight: bold;
}
</style> 