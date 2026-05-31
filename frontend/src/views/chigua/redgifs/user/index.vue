<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="已认证" prop="verified">
        <el-select v-model="queryParams.verified" placeholder="是否认证" clearable>
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否推荐" prop="recommended">
        <el-select v-model="queryParams.recommended" placeholder="是否推荐" clearable>
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
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
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
        >批量删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 用户列表 -->
    <el-table
      ref="userTable"
      v-loading="loading"
      :data="userList"
      @selection-change="handleSelectionChange"
      @sort-change="handleTableSortChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户头像" align="center" width="130">
        <template slot-scope="scope">
          <div class="user-avatar-wrapper" :class="{ 'recommended-avatar': scope.row.recommended === 1 }">
            <el-image
              v-if="scope.row.profileImageUrl"
              :src="scope.row.profileImageUrl"
              class="user-avatar-img"
              fit="cover"
            >
              <div slot="placeholder" class="user-avatar-placeholder">
                <i class="el-icon-loading"></i>
              </div>
              <div slot="error" class="user-avatar-placeholder">
                <i class="el-icon-user-solid"></i>
              </div>
            </el-image>
            <div v-else class="user-avatar-placeholder">
              <i class="el-icon-user-solid"></i>
            </div>
            <div v-if="scope.row.recommended === 1" class="recommended-badge">
              <i class="el-icon-star-on"></i>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用户名" align="center" prop="username" min-width="150" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <div>
            <span style="font-weight: bold;">{{ scope.row.username }}</span>
            <el-tag v-if="scope.row.verified" type="success" size="mini" style="margin-left: 5px;">
              <i class="el-icon-success"></i> 已认证
            </el-tag>
            <el-tag v-if="scope.row.recommended === 1" type="warning" size="mini" style="margin-left: 5px;">
              <i class="el-icon-star-on"></i> 推荐
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="统计信息" align="center" width="280">
        <template slot-scope="scope">
          <div style="display: flex; justify-content: space-around;">
            <el-tag size="mini" type="primary">
              <i class="el-icon-video-camera"></i> {{ scope.row.publishedGifsCount || scope.row.gifsCount || 0 }}
            </el-tag>
            <el-tag size="mini" type="info">
              <i class="el-icon-view"></i> {{ formatNumber(scope.row.views) }}
            </el-tag>
            <el-tag size="mini" type="warning">
              <i class="el-icon-user"></i> {{ formatNumber(scope.row.followers) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdAt" width="160" sortable="custom">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="160">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
            @change="handleStatusChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="推荐" align="center" width="130">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.recommended"
            :active-value="1"
            :inactive-value="0"
            active-text="推荐"
            inactive-text="普通"
            active-color="#E6A23C"
            inactive-color="#DCDFE6"
            @change="handleRecommendedChange(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="130" sortable="custom">
        <template slot-scope="scope">
          <div class="sort-input-wrapper">
            <el-input-number
              v-model="scope.row.sortOrder"
              :min="0"
              :max="9999"
              size="mini"
              controls-position="right"
              style="width: 90px;"
              @change="handleSortOrderChange(scope.row)"
            ></el-input-number>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleViewVideos(scope.row)"
          >查看视频</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
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
  </div>
</template>

<script>
import { listRedGifsUser, delRedGifsUser, delRedGifsUsers, updateRedGifsUserStatus, updateRedGifsUserRecommended, updateRedGifsUserSort } from "@/api/chigua/redgifsUser";
export default {
  name: "RedGifsUser",
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      userList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 12,
        username: null,
        status: null,
        verified: null,
        recommended: null,
        sortColumn: null,
        sortDirection: null
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listRedGifsUser(this.queryParams).then(response => {
        this.userList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.sortColumn = null;
      this.queryParams.sortDirection = null;
      if (this.$refs.userTable) {
        this.$refs.userTable.clearSort();
      }
      this.handleQuery();
    },
    /** 表头排序：按 sort_order / created_at 服务端排序 */
    handleTableSortChange({ prop, order }) {
      if (!order) {
        this.queryParams.sortColumn = null;
        this.queryParams.sortDirection = null;
      } else if (prop === "sortOrder" || prop === "createdAt") {
        this.queryParams.sortColumn = prop;
        this.queryParams.sortDirection = order === "ascending" ? "asc" : "desc";
      } else {
        return;
      }
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.multiple = !selection.length;
    },
    handleStatusChange(user) {
      let text = user.status === 1 ? "启用" : "禁用";
      this.$modal.confirm('确认要"' + text + '""' + user.username + '"用户吗？').then(() => {
        return updateRedGifsUserStatus(user.id, user.status);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        user.status = user.status === 0 ? 1 : 0;
      });
    },
    handleRecommendedChange(user) {
      let text = user.recommended === 1 ? "推荐" : "取消推荐";
      updateRedGifsUserRecommended(user.id, user.recommended).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        user.recommended = user.recommended === 0 ? 1 : 0;
        this.$modal.msgError(text + "失败");
      });
    },
    /** 修改排序数值后提交保存 */
    handleSortOrderChange(user) {
      if (user == null || user.id == null) {
        return;
      }
      const sortVal = user.sortOrder != null ? user.sortOrder : 0;
      updateRedGifsUserSort(user.id, sortVal).then(() => {
        this.$modal.msgSuccess("排序更新成功");
      }).catch(() => {
        this.$modal.msgError("排序更新失败");
      });
    },
    handleDelete(row) {
      const userIds = row.id ? [row.id] : this.ids;
      const usernames = row.id ? row.username : this.userList.filter(u => userIds.includes(u.id)).map(u => u.username).join(', ');
      
      this.$modal.confirm('是否确认删除用户"' + usernames + '"？').then(() => {
        return row.id ? delRedGifsUser(row.id) : delRedGifsUsers(userIds.join(','));
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleViewVideos(user) {
      this.$router.push({
        path: '/chigua/redgifs/video',
        query: { userId: user.id, username: user.username }
      });
    },
    handleImageError(event) {
      event.target.style.display = 'none';
      const placeholder = document.createElement('div');
      placeholder.className = 'user-avatar-placeholder';
      placeholder.innerHTML = '<i class="el-icon-user-solid"></i>';
      event.target.parentNode.appendChild(placeholder);
    },
    formatNumber(num) {
      if (!num) return '0';
      if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
      }
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
      }
      return num.toString();
    },
    parseTime(time) {
      if (!time) return '';
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }
  }
};
</script>

<style scoped lang="scss">
.user-avatar-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  margin: 0 auto;
  border-radius: 50%;
  overflow: visible;
  background-color: #f5f5f5;

  &.recommended-avatar {
    .user-avatar-img,
    .user-avatar-placeholder {
      border: 2px solid #E6A23C;
      box-shadow: 0 0 8px rgba(230, 162, 60, 0.5);
    }
  }
}

.user-avatar-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  object-position: center;
  border: 2px solid #dcdfe6;

  ::v-deep .el-image__inner {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    object-fit: cover;
    object-position: center;
  }
}

.user-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: #f0f0f0;
  color: #909399;
  font-size: 32px;
  border: 2px solid #dcdfe6;
}

.recommended-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 22px;
  height: 22px;
  background: linear-gradient(135deg, #f6d365, #E6A23C);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);

  i {
    color: #fff;
    font-size: 12px;
  }
}

.sort-input-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
