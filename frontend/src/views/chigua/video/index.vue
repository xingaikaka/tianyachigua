<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="视频标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入视频标题"
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
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable>
          <el-option
            v-for="category in categoryOptions"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已下架" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="推荐" prop="isRecommended">
        <el-select v-model="queryParams.isRecommended" placeholder="是否推荐" clearable>
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="热门" prop="isHot">
        <el-select v-model="queryParams.isHot" placeholder="是否热门" clearable>
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
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
      <el-form-item label="发布时间">
        <el-date-picker
          v-model="publishDateRange"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="发布开始日期"
          end-placeholder="发布结束日期"
        ></el-date-picker>
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
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['chigua:video:add']"
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
          v-hasPermi="['chigua:video:edit']"
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
          v-hasPermi="['chigua:video:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-dropdown @command="handleBatchCommand" v-hasPermi="['chigua:video:edit']">
          <el-button type="info" plain size="mini">
            批量操作<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="publish">批量发布</el-dropdown-item>
            <el-dropdown-item command="draft">批量草稿</el-dropdown-item>
            <el-dropdown-item command="offline">批量下架</el-dropdown-item>
            <el-dropdown-item command="recommend">批量推荐</el-dropdown-item>
            <el-dropdown-item command="unrecommend">取消推荐</el-dropdown-item>
            <el-dropdown-item command="hot">批量热门</el-dropdown-item>
            <el-dropdown-item command="unhot">取消热门</el-dropdown-item>
            <el-dropdown-item divided command="changeCategory">批量修改分类</el-dropdown-item>
            <el-dropdown-item command="replaceTags">批量添加标签</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['chigua:video:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 视频列表 -->
    <el-table v-loading="loading" :data="videoList" @selection-change="handleSelectionChange" @sort-change="onSortChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="视频ID" align="center" prop="id" width="80" />
      <el-table-column label="封面" align="center" prop="coverImage" width="620">
        <template slot-scope="scope">
          <image-preview :src="scope.row.coverImage" :width="600" :height="300" v-if="scope.row.coverImage"/>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" min-width="200">
        <template slot-scope="scope">
          <el-link type="primary" @click="handlePreview(scope.row)">{{ scope.row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="作者" align="center" prop="author" width="100" />
      <el-table-column label="所属分类" align="center" prop="categoryName" width="150">
        <template slot-scope="scope">
          <el-tooltip v-if="scope.row.categoryName" 
                      :content="scope.row.categoryName" 
                      placement="top" 
                      :disabled="scope.row.categoryName.split(', ').length <= 2"
                      effect="dark">
            <div style="max-height: 40px; overflow: hidden; line-height: 20px;">
              <el-tag 
                v-for="(category, index) in scope.row.categoryName.split(', ')" 
                :key="index"
                type="info" 
                size="mini"
                style="margin-right: 4px; margin-bottom: 2px;"
              >
                {{ category }}
              </el-tag>
            </div>
          </el-tooltip>
          <span v-else style="color: #909399;">未分类</span>
        </template>
      </el-table-column>
      <el-table-column label="视频类型" align="center" prop="videoType" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.videoType === 'short'" type="success" size="small">
            <i class="el-icon-mobile-phone"></i> 短视频
          </el-tag>
          <el-tag v-else-if="scope.row.videoType === 'long'" type="primary" size="small">
            <i class="el-icon-film"></i> 长视频
          </el-tag>
          <el-tag v-else type="info" size="small">
            <i class="el-icon-film"></i> 长视频
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="浏览量" align="center" prop="viewCount" width="80" sortable="custom">
        <template slot-scope="scope">
          {{ scope.row.viewCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="播放量" align="center" prop="playCount" width="80" sortable="custom">
        <template slot-scope="scope">
          {{ scope.row.playCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="分享量" align="center" prop="shareCount" width="80" sortable="custom">
        <template slot-scope="scope">
          {{ scope.row.shareCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="发布时间" align="center" prop="publishedAt" width="160" sortable="custom">
        <template slot-scope="scope">
          <span v-if="scope.row.publishedAt">{{ parseTime(scope.row.publishedAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          <span v-else style="color: #909399;">未发布</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.video_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="标记" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isRecommended == 1" type="success" size="mini">推荐</el-tag>
          <el-tag v-if="scope.row.isHot == 1" type="danger" size="mini">热门</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80">
        <template slot-scope="scope">
          <span>{{ scope.row.sortOrder || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['chigua:video:edit']"
          >修改</el-button>

          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handlePreviewContent(scope.row)"
                    >预览副文本</el-button>
          <br/>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['chigua:video:remove']"
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

    <!-- 添加或修改视频对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="视频标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入视频标题" />
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="form.subtitle" placeholder="请输入副标题" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="封面图片" prop="coverImage">
          <image-upload v-model="form.coverImage" :limit="1" :action="uploadCoverUrl"/>
        </el-form-item>
        <el-form-item label="视频描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入视频描述" />
        </el-form-item>
        <el-form-item label="视频内容" prop="videoContent">
          <Editor
            ref="mainEditor"
            v-model="form.videoContent"
            :video-id="form.id"
            :min-height="200"
            :file-size="10"
          />
        </el-form-item>
        <el-row>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态">
                <el-option label="草稿" :value="0" />
                <el-option label="已发布" :value="1" />
                <el-option label="已下架" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否推荐" prop="isRecommended">
              <el-switch v-model="form.isRecommended" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否热门" prop="isHot">
              <el-switch v-model="form.isHot" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" placeholder="请输入排序" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryIds">
              <el-select v-model="form.categoryIds" multiple placeholder="请选择分类">
                <el-option
                  v-for="category in categoryOptions"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签" prop="tagIds">
          <div class="tag-selector">
            <!-- 已选标签显示 -->
            <div class="selected-tags" v-if="selectedTags.length > 0">
              <el-tag
                v-for="tag in selectedTags"
                :key="tag.id"
                closable
                @close="removeTag(tag.id)"
                style="margin-right: 8px; margin-bottom: 8px;"
              >
                {{ tag.name }}
              </el-tag>
            </div>
            
            <!-- 标签选择按钮 -->
            <el-button type="primary" plain @click="openTagDialog" icon="el-icon-plus">
              选择标签 ({{ selectedTags.length }})
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 标签选择弹出框 -->
    <el-dialog
      title="选择标签"
      :visible.sync="showTagDialog"
      width="900px"
      custom-class="video-tag-selector-dialog"
      append-to-body
      @open="loadAllTags"
    >
      <div class="tag-dialog-content">
        <!-- 搜索框 -->
        <div class="tag-search-bar">
          <el-input
            v-model="tagSearchKeyword"
            placeholder="搜索标签..."
            prefix-icon="el-icon-search"
            clearable
            @input="filterTags"
            style="width: 400px; margin-right: 10px;"
          />
          <el-button type="success" @click="showCreateTagDialog = true" icon="el-icon-plus">
            新建标签
          </el-button>
        </div>
        
        <!-- 标签列表 -->
        <div class="tag-list-container">
          <div class="tag-grid">
                          <div
                v-for="tag in filteredTags"
                :key="tag.id"
                class="tag-item"
                :class="{ 'selected': isTagSelected(tag.id) }"
                @click="toggleTag(tag)"
              >
              <el-checkbox
                :value="isTagSelected(tag.id)"
                @change="toggleTag(tag)"
                @click.stop
              >
                {{ tag.name }}
              </el-checkbox>
            </div>
          </div>
          
          <!-- 空状态 -->
          <div v-if="filteredTags.length === 0" class="empty-state">
            <i class="el-icon-search"></i>
            <p>没有找到匹配的标签</p>
          </div>
        </div>
        
        <!-- 已选标签统计 -->
        <div class="selected-count">
          已选择 {{ selectedTags.length }} 个标签
        </div>
      </div>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="showTagDialog = false">取 消</el-button>
        <el-button type="primary" @click="confirmTagSelection">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 新建标签弹出框 -->
    <el-dialog
      title="新建标签"
      :visible.sync="showCreateTagDialog"
      width="400px"
      append-to-body
    >
      <el-form ref="createTagForm" :model="newTagForm" :rules="newTagRules" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="newTagForm.name" placeholder="请输入标签名称" />
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancelCreateTag">取 消</el-button>
        <el-button type="primary" @click="confirmCreateTag">确 定</el-button>
      </div>
    </el-dialog>



    <!-- 图片管理弹窗 -->
    <VideoImageDialog
      ref="imageDialog"
      @refresh="getList"
    />

    <!-- 地址管理弹窗 -->
    <VideoUrlDialog
      ref="urlDialog"
      @refresh="getList"
    />

    <!-- 视频预览弹窗 -->
    <VideoPreviewDialog
      ref="previewDialog"
    />

    <!-- 副文本预览弹窗 -->
    <el-dialog
      title="预览副文本"
      :visible.sync="contentPreviewVisible"
      width="1000px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div class="video-content-preview">
        <!-- 视频信息头部 -->
        <div class="content-header">
          <div class="video-info">
            <h2 class="video-title">{{ currentPreviewVideo.title }}</h2>
            <div class="video-meta">
              <span class="author">作者：{{ currentPreviewVideo.author }}</span>
              <span class="stats">浏览：{{ currentPreviewVideo.viewCount || 0 }}</span>
              <span class="stats">播放：{{ currentPreviewVideo.playCount || 0 }}</span>
              <span class="stats">分享：{{ currentPreviewVideo.shareCount || 0 }}</span>
              <span class="created-time">创建时间：{{ parseTime(currentPreviewVideo.createdAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            </div>
          </div>
          <div class="action-buttons">
            <el-button type="success" size="small" @click="refreshVideoUrls" title="刷新视频URL">
              <i class="el-icon-refresh"></i> 刷新视频
            </el-button>
          </div>
        </div>

        <!-- 动态富文本内容显示 -->
        <div class="content-body">
          <DynamicRichTextDisplay
            :content="currentPreviewVideo.videoContent"
            :autoLoadUrls="true"
            ref="richTextDisplay"
          />
          <div class="empty-content" v-if="!currentPreviewVideo.videoContent">
            <i class="el-icon-document"></i>
            <p>暂无副文本内容</p>
          </div>
        </div>

        <!-- 内容统计信息 -->
        <div class="content-footer" v-if="currentPreviewVideo.videoContent">
          <div class="content-stats">
            <span>字符数：{{ getPreviewContentLength() }}</span>
            <span>视频数：{{ getVideoCountFromContent(currentPreviewVideo.videoContent) }}</span>
          </div>
        </div>
      </div>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="contentPreviewVisible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 批量修改分类对话框 -->
    <el-dialog title="批量修改分类" :visible.sync="batchCategoryDialogVisible" width="500px" append-to-body>
      <el-form>
        <el-form-item label="目标分类" label-width="100px">
          <el-select v-model="batchCategoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="category in categoryOptions"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="batch-info">
            <i class="el-icon-info"></i>
            <span>将对选中的 {{ ids.length }} 个视频进行主分类修改</span>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="batchCategoryDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmBatchChangeCategory">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 批量添加标签对话框 -->
    <el-dialog title="批量添加标签" :visible.sync="batchTagDialogVisible" width="600px" append-to-body>
      <el-form>
        <el-form-item label="搜索标签" label-width="100px">
          <el-input
            v-model="tagSearchKeyword"
            placeholder="请输入标签名称进行搜索"
            clearable
            @input="handleTagSearch"
            @clear="handleTagSearchClear"
            style="width: 100%"
          >
            <i slot="prefix" class="el-input__icon el-icon-search"></i>
          </el-input>
        </el-form-item>
        <el-form-item label="搜索结果" label-width="100px" v-if="tagSearchKeyword.trim()">
          <div class="tag-search-results" v-if="tagSearchResults.length > 0">
            <el-tag
              v-for="tag in tagSearchResults"
              :key="tag.id"
              :type="batchSelectedTags.includes(tag.id) ? 'success' : 'info'"
              :effect="batchSelectedTags.includes(tag.id) ? 'dark' : 'plain'"
              @click="toggleTagSelection(tag)"
              style="margin: 4px; cursor: pointer;"
              :color="tag.color"
            >
              {{ tag.name }}
              <i v-if="batchSelectedTags.includes(tag.id)" class="el-icon-check" style="margin-left: 4px;"></i>
            </el-tag>
            <div class="search-info" style="margin-top: 8px; color: #909399; font-size: 12px;">
              <i class="el-icon-info"></i>
              <span>显示前20条搜索结果，点击标签进行选择</span>
            </div>
          </div>
          <div v-else-if="tagSearchKeyword.trim() && !tagSearchLoading" class="no-results">
            <i class="el-icon-warning-outline" style="color: #909399;"></i>
            <span style="color: #909399; margin-left: 4px;">未找到匹配的标签</span>
          </div>
          <div v-else-if="tagSearchLoading" class="searching">
            <i class="el-icon-loading" style="color: #409EFF;"></i>
            <span style="color: #409EFF; margin-left: 4px;">搜索中...</span>
          </div>
        </el-form-item>
        <el-form-item label="已选标签" label-width="100px" v-if="selectedTagsDisplay.length > 0">
          <div class="selected-tags">
            <el-tag
              v-for="tag in selectedTagsDisplay"
              :key="tag.id"
              type="success"
              closable
              @close="removeTagSelection(tag.id)"
              style="margin: 4px;"
              :color="tag.color"
            >
              {{ tag.name }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item>
          <div class="batch-info">
            <i class="el-icon-info"></i>
            <span>将为选中的 {{ ids.length }} 个视频添加所选标签（已存在的标签将被跳过）</span>
          </div>
          <div class="batch-info" style="margin-top: 8px; color: #909399;">
            <i class="el-icon-warning-outline"></i>
            <span>请先搜索标签，然后点击选择需要添加的标签</span>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="batchTagDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmBatchReplaceTags" :disabled="batchSelectedTags.length === 0">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listVideo, getVideo, getVideoRaw, delVideo, addVideo, updateVideo, getCategories, searchTags, updateVideoStatus, updateVideoRecommended, updateVideoHot, getVideoProcessedContent, updateVideoPrimaryCategory, replaceVideoTags, getTags } from "@/api/chigua/video";
import { listTag, addTag as createTagApi } from "@/api/chigua/tag";
import VideoImageDialog from './components/VideoImageDialog';
import VideoUrlDialog from './components/VideoUrlDialog';
import VideoPreviewDialog from './components/VideoPreviewDialog';
import VideoContentPreview from './components/VideoContentPreview';
import DynamicRichTextDisplay from '@/components/DynamicRichTextDisplay';

export default {
  name: "Video",
  dicts: ['video_status'],
  components: {
    VideoImageDialog,
    VideoUrlDialog,
    VideoPreviewDialog,
    VideoContentPreview,
    DynamicRichTextDisplay
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
      // 视频表格数据
      videoList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 日期范围
      dateRange: [],
      // 发布时间范围
      publishDateRange: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'published_at',
        isAsc: 'desc',
        title: null,
        author: null,
        categoryId: null,
        status: null,
        isRecommended: null,
        isHot: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        title: [
          { required: true, message: "视频标题不能为空", trigger: "blur" }
        ],
        author: [
          { required: true, message: "作者不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "change" }
        ],
        publishedAt: [
          { required: true, message: "发布时间不能为空", trigger: "change" },
          { 
            validator: (rule, value, callback) => {
              if (!value || value === '' || value === null || value === undefined) {
                callback(new Error('发布时间不能为空'))
              } else {
                callback()
              }
            }, 
            trigger: "change" 
          }
        ]
      },
      // 分类选项
      categoryOptions: [],
      // 标签选项
      tagOptions: [],
      // 标签加载状态
      tagLoading: false,
      // 已选标签
      selectedTags: [],
      // 标签搜索值
      tagSearchValue: '',
      // 常用标签
      popularTags: [],
      // 是否显示标签选择对话框
      showTagDialog: false,
      // 标签搜索关键词
      tagSearchKeyword: '',
      // 过滤后的标签列表
      filteredTags: [],
      // 是否显示新建标签对话框
      showCreateTagDialog: false,
      // 新建标签表单
      newTagForm: {
        name: ''
      },
      // 新建标签校验规则
      newTagRules: {
        name: [
          { required: true, message: "标签名称不能为空", trigger: "blur" }
        ]
      },

      // 副文本预览弹出框
      contentPreviewVisible: false,
      // 当前预览的视频
      currentPreviewVideo: {},
      // 封面图片上传方式
      coverImageType: 'upload',
      // 封面图片上传URL - 使用Worker richtext API与富文本保持完全一致
      uploadCoverUrl: "https://chigua-r2-worker.xingaikaka.workers.dev/api/upload/richtext",

      // 批量修改分类对话框
      batchCategoryDialogVisible: false,
      batchCategoryId: null,
      // 批量添加标签对话框
      batchTagDialogVisible: false,
      batchSelectedTags: [],
      // 标签搜索相关
      tagSearchKeyword: '',
      tagSearchResults: [],
      tagSearchLoading: false,
      allTagsMap: new Map() // 用于存储所有标签的映射
    };
  },
  // 🔧 添加路由监听，处理从表单页面返回时的刷新
  watch: {
    '$route'(to, from) {
      // 当路由变化且包含refresh参数时，刷新列表
      if (to.path === '/chigua/video' && to.query.refresh) {
        console.log('🔄 检测到从表单页面返回，刷新视频列表')
        this.getList()
        
        // 清除URL中的refresh参数，避免重复刷新
        this.$nextTick(() => {
          this.$router.replace({ path: '/chigua/video' })
        })
      }
    }
  },
  computed: {
    // 已选择标签的显示数据
    selectedTagsDisplay() {
      return this.batchSelectedTags.map(tagId => {
        return this.allTagsMap.get(tagId) || { id: tagId, name: `标签${tagId}` };
      });
    }
  },
  created() {
    this.getList();
    this.getCategories();
    this.getTags();
  },
  methods: {
    // 表头排序→远程全量排序
    onSortChange({ prop, order }) {
      const map = {
        publishedAt: 'published_at',
        viewCount:   'stats_view_count',
        playCount:   'stats_play_count',
        shareCount:  'stats_share_count'
      };
      const col = map[prop] || undefined;
      const asc = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : undefined;
      // 取消排序时回落到默认（发布时间倒序）
      this.queryParams.orderByColumn = col || 'published_at';
      this.queryParams.isAsc = asc || 'desc';
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 查询视频列表 */
    getList() {
      this.loading = true;
      // 先添加创建时间范围
      let params = this.addDateRange(this.queryParams, this.dateRange);
      // 再添加发布时间范围
      if (this.publishDateRange && this.publishDateRange.length === 2) {
        params.publishBeginTime = this.publishDateRange[0];
        params.publishEndTime = this.publishDateRange[1];
      }
      listVideo(params).then(response => {
        this.videoList = response.rows;
        this.total = response.total;
        this.loading = false;
        
        // 🔍 添加调试日志，查看封面图片URL
        console.log('📋 视频列表数据:', response.rows);
        response.rows.forEach((video, index) => {
          console.log(`🎬 视频${index + 1}: ID=${video.id}, title=${video.title}, sortOrder=${video.sortOrder}`);
          console.log(`📷 封面图片: ${video.coverImage || '无封面'}`);
          if (video.coverImage) {
            console.log(`🔗 封面URL长度: ${video.coverImage.length}`);
            console.log(`🌐 是否包含域名: ${video.coverImage.includes('http')}`);
            console.log(`🔐 是否包含签名: ${video.coverImage.includes('signature=')}`);
            console.log(`🔓 是否包含解密: ${video.coverImage.includes('decrypt=true')}`);
            
            // 测试第一个有封面的视频URL
            if (index === 0) {
              this.testCoverUrl(video.coverImage);
            }
          }
        });
      }).catch(() => {
        this.loading = false;
      });
    },
    /** 获取分类列表 */
    getCategories() {
      getCategories().then(response => {
        this.categoryOptions = response.data;
      });
    },
    /** 获取标签列表 */
    getTags() {
      listTag({ status: 1, orderBy: 'usageCount', isAsc: 'desc' }).then(response => {
        // 尝试多种数据结构
        let tags = [];
        if (response.rows && Array.isArray(response.rows)) {
          tags = response.rows;
        } else if (response.data && Array.isArray(response.data)) {
          tags = response.data;
        } else if (Array.isArray(response)) {
          tags = response;
        }
        
        this.tagOptions = tags;
        console.log('📋 获取标签列表:', this.tagOptions.length, '个标签');
      }).catch(error => {
        console.error('❌ 获取标签列表失败:', error);
        this.tagOptions = [];
      });
    },
    /** 获取常用标签 */
    getPopularTags() {
      listTag({ status: 1, pageSize: 10, orderBy: 'usageCount', isAsc: 'desc' }).then(response => {
        this.popularTags = (response.rows || []).slice(0, 10); // 取前10个作为常用标签
      }).catch(error => {
        console.error('❌ 获取常用标签失败:', error);
        this.popularTags = [];
      });
    },
    /** 搜索标签 */
    searchTagsRemote(keyword) {
      if (keyword !== '') {
        this.tagLoading = true;
        searchTags(keyword).then(response => {
          this.tagOptions = response.data;
          this.tagLoading = false;
        });
      } else {
        // 如果没有关键词，显示常用标签
        this.getPopularTags();
      }
    },
    /** 处理标签选择 */
    handleTagSelect(value) {
      if (!value) return;
      
      this.tagSearchValue = ''; // 清空搜索框
      
      if (typeof value === 'string' && value.startsWith('new:')) {
        // 创建新标签
        const newTagName = value.slice(4);
        this.createNewTag(newTagName);
      } else {
        // 选择已有标签
        this.addExistingTag(value);
      }
    },
    /** 创建新标签 */
    createNewTag(tagName) {
      const tagData = {
        name: tagName,
        status: 0 // 0=正常，1=停用（根据字典数据）
      };
      
      createTagApi(tagData).then(response => {
        const newTag = response.data;
        
        if (!this.form.tagIds) {
          this.form.tagIds = [];
        }
        this.form.tagIds.push(newTag.id);
        this.selectedTags.push(newTag);
        this.tagOptions.push(newTag);
        this.filteredTags.push(newTag);
        
        this.$message.success('标签创建成功');
      }).catch(error => {
        this.$message.error('标签创建失败：' + (error.message || '未知错误'));
      });
    },
    /** 添加已有标签 */
    addExistingTag(tagId) {
      if (this.form.tagIds && this.form.tagIds.includes(tagId)) {
        this.$message.warning('该标签已选择');
        return;
      }
      
      const tag = this.tagOptions.find(t => t.id === tagId);
      if (tag) {
        if (!this.form.tagIds) {
          this.form.tagIds = [];
        }
        this.form.tagIds.push(tagId);
        this.selectedTags.push(tag);
      }
    },
    /** 移除标签 */
    removeTag(tagId) {
      if (this.form.tagIds) {
        this.form.tagIds = this.form.tagIds.filter(id => id !== tagId);
      }
      this.selectedTags = this.selectedTags.filter(tag => tag.id !== tagId);
    },
    /** 检查标签是否存在 */
    tagExists(tagName) {
      return this.tagOptions.some(tag => tag.name.toLowerCase() === tagName.toLowerCase());
    },
    /** 快速选择常用标签 */
    quickSelectTag(tag) {
      if (this.form.tagIds && this.form.tagIds.includes(tag.id)) {
        this.$message.warning('该标签已选择');
        return;
      }
      
      if (!this.form.tagIds) {
        this.form.tagIds = [];
      }
      this.form.tagIds.push(tag.id);
      this.selectedTags.push(tag);
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
        subtitle: null,
        author: null,
        description: null,
        coverImage: null,
        status: 0,
        isRecommended: 0,
        isHot: 0,
        sortOrder: 0,
        categoryIds: [],
        tagIds: [],
        videoContent: ''
      };
      this.selectedTags = [];
      this.tagSearchValue = '';
      this.coverImageType = 'upload'; // 重置封面图片上传方式
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = [];
      this.publishDateRange = [];
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
      // 打开新的标签页来新增视频
      this.$tab.openPage("新增视频", "/chigua/video/form/add");
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      const id = row.id || this.ids;
      // 打开新的标签页来编辑视频
      this.$tab.openPage(`编辑视频 - ${row.title}`, `/chigua/video/form/${id}`);
    },
    /** 加载已选标签的详细信息 */
    loadSelectedTags(tagIds) {
      // 从已有的tagOptions中查找
      this.selectedTags = [];
      tagIds.forEach(tagId => {
        const tag = this.tagOptions.find(t => t.id === tagId);
        if (tag) {
          this.selectedTags.push(tag);
        }
      });
      
      // 如果有标签没找到，需要从后端加载
      const missingTagIds = tagIds.filter(tagId => 
        !this.selectedTags.some(tag => tag.id === tagId)
      );
      
      if (missingTagIds.length > 0) {
        // 这里可以调用API获取标签详情
        listTag({ status: 0, pageSize: 1000 }).then(response => {
          const allTags = response.rows || [];
          missingTagIds.forEach(tagId => {
            const tag = allTags.find(t => t.id === tagId);
            if (tag) {
              this.selectedTags.push(tag);
              // 同时添加到tagOptions中
              if (!this.tagOptions.some(t => t.id === tagId)) {
                this.tagOptions.push(tag);
              }
            }
          });
        }).catch(error => {
          console.error('❌ 获取缺失标签失败:', error);
        });
      }
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 🔧 关键修复：使用Editor组件的getStorageContent方法获取转换后的内容
          let formData = { ...this.form };
          let videoContent = this.form.videoContent;
          
          if (this.$refs.mainEditor && this.$refs.mainEditor.getStorageContent) {
            const storageContent = this.$refs.mainEditor.getStorageContent();
            formData.videoContent = storageContent;
            videoContent = storageContent;
            
            // 🔧 详细的调试信息
            console.log('🔄 保存主表单视频内容转换详情:', {
              原始长度: this.form.videoContent.length,
              转换后长度: storageContent.length,
              包含WorkerURL: this.form.videoContent.includes('chigua-r2-worker.xingaikaka.workers.dev'),
              包含tycgimage1: this.form.videoContent.includes('tycgimage1.org'),
              包含签名参数: this.form.videoContent.includes('signature='),
              包含data_resource_key: storageContent.includes('data-resource-key='),
              包含相对路径: storageContent.includes('src="files/'),
              原始内容预览: this.form.videoContent.substring(0, 500),
              转换后内容预览: storageContent.substring(0, 500)
            });
            
            // 🔧 特别检查视频标签转换
            const videoTagsOriginal = this.form.videoContent.match(/<video[^>]*>.*?<\/video>/gis) || [];
            const videoTagsConverted = storageContent.match(/<video[^>]*>.*?<\/video>/gis) || [];
            console.log('🎬 视频标签转换对比:', {
              原始视频标签数: videoTagsOriginal.length,
              转换后视频标签数: videoTagsConverted.length,
              原始视频标签: videoTagsOriginal.map(tag => tag.substring(0, 300)),
              转换后视频标签: videoTagsConverted.map(tag => tag.substring(0, 300))
            });
            
            // 🔧 特别检查source标签转换
            const sourceTagsOriginal = this.form.videoContent.match(/<source[^>]*>/gi) || [];
            const sourceTagsConverted = storageContent.match(/<source[^>]*>/gi) || [];
            console.log('🎬 source标签转换对比:', {
              原始source标签数: sourceTagsOriginal.length,
              转换后source标签数: sourceTagsConverted.length,
              原始source标签: sourceTagsOriginal,
              转换后source标签: sourceTagsConverted
            });
          }
          
          // 🔧 从富文本中提取第一个视频的video_transcodes.id并设置到transcodeId字段
          let extractedTranscodeId = null;
          
          // 先从转换后的内容中提取
          if (videoContent) {
            const videoIdMatch = videoContent.match(/data-video-id="(\d+)"/);
            if (videoIdMatch) {
              extractedTranscodeId = videoIdMatch[1];
              console.log('🔄 从转换后内容中提取到video-id:', extractedTranscodeId);
            }
          }
          
          // 如果转换后的内容中没有找到，从原始内容中提取
          if (!extractedTranscodeId && this.form.videoContent) {
            const originalVideoIdMatch = this.form.videoContent.match(/data-video-id="(\d+)"/);
            if (originalVideoIdMatch) {
              extractedTranscodeId = originalVideoIdMatch[1];
              console.log('🔄 从原始内容中提取到video-id:', extractedTranscodeId);
            }
          }
          
          if (!formData.transcodeId && extractedTranscodeId) {
            formData.transcodeId = extractedTranscodeId;
            console.log('🔄 设置transcodeId:', formData.transcodeId);
          }
          
          if (formData.id != null) {
            updateVideo(formData).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addVideo(formData).then(response => {
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
      this.$modal.confirm('是否确认删除视频编号为"' + ids + '"的数据项？').then(function() {
        return delVideo(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('chigua/video/export', {
        ...this.queryParams
      }, `video_${new Date().getTime()}.xlsx`)
    },
    // /** 图片管理 */
    // handleImages(row) {
    //   this.$refs.imageDialog.show(row);
    // },
    // /** 地址管理 */
    // handleUrls(row) {
    //   this.$refs.urlDialog.show(row);
    // },
    /** 预览视频 */
    handlePreview(row) {
      this.$refs.previewDialog.show(row);
    },
    /** 批量操作 */
    handleBatchCommand(command) {
      if (this.ids.length === 0) {
        this.$modal.msgError("请选择要操作的数据");
        return;
      }
      
      let data = { ids: this.ids };
      let message = "";
      let apiCall = null;
      
      switch (command) {
        case 'publish':
          data.status = 1;
          message = "批量发布";
          apiCall = updateVideoStatus;
          break;
        case 'draft':
          data.status = 0;
          message = "批量设为草稿";
          apiCall = updateVideoStatus;
          break;
        case 'offline':
          data.status = 2;
          message = "批量下架";
          apiCall = updateVideoStatus;
          break;
        case 'recommend':
          data.isRecommended = 1;
          message = "批量推荐";
          apiCall = updateVideoRecommended;
          break;
        case 'unrecommend':
          data.isRecommended = 0;
          message = "取消推荐";
          apiCall = updateVideoRecommended;
          break;
        case 'hot':
          data.isHot = 1;
          message = "批量热门";
          apiCall = updateVideoHot;
          break;
        case 'unhot':
          data.isHot = 0;
          message = "取消热门";
          apiCall = updateVideoHot;
          break;
        case 'changeCategory':
          this.showBatchCategoryDialog();
          return;
        case 'replaceTags':
          this.showBatchTagDialog();
          return;
      }
      
      this.$modal.confirm(`确认${message}选中的${this.ids.length}条数据？`).then(() => {
        return apiCall(data);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(`${message}成功`);
      }).catch(() => {});
    },
    /** 刷新预览中的视频URL */
    refreshVideoUrls() {
      if (this.$refs.richTextDisplay) {
        this.$refs.richTextDisplay.refreshVideoUrls();
      }
    },
    /** 显示批量修改分类对话框 */
    showBatchCategoryDialog() {
      this.batchCategoryId = null;
      this.batchCategoryDialogVisible = true;
    },
    /** 确认批量修改分类 */
    confirmBatchChangeCategory() {
      if (!this.batchCategoryId) {
        this.$modal.msgError("请选择目标分类");
        return;
      }
      
      const data = {
        ids: this.ids,
        categoryId: this.batchCategoryId
      };
      
      this.$modal.confirm(`确认将选中的${this.ids.length}个视频的主分类修改为所选分类？`).then(() => {
        return updateVideoPrimaryCategory(data);
      }).then(() => {
        this.getList();
        this.batchCategoryDialogVisible = false;
        this.$modal.msgSuccess("批量修改分类成功");
      }).catch(() => {});
    },
    /** 显示批量添加标签对话框 */
    async showBatchTagDialog() {
      this.batchSelectedTags = [];
      this.tagSearchKeyword = '';
      this.tagSearchResults = [];
      this.allTagsMap.clear();
      
      // 预加载所有标签到Map中（用于搜索和显示）
      try {
        const response = await getTags();
        const allTags = response.data || [];
        allTags.forEach(tag => {
          this.allTagsMap.set(tag.id, tag);
        });
      } catch (error) {
        console.error('加载标签列表失败:', error);
      }
      
      this.batchTagDialogVisible = true;
    },
    /** 确认批量添加标签 */
    confirmBatchReplaceTags() {
      if (this.batchSelectedTags.length === 0) {
        this.$modal.msgWarning("请先选择要添加的标签");
        return;
      }
      
      const data = {
        ids: this.ids,
        tagIds: this.batchSelectedTags
      };
      
      const message = `确认为选中的${this.ids.length}个视频添加所选标签？（已存在的标签将被跳过）`;
      
      this.$modal.confirm(message).then(() => {
        return replaceVideoTags(data);
      }).then(() => {
        this.getList();
        this.batchTagDialogVisible = false;
        this.$modal.msgSuccess("批量添加标签成功");
      }).catch(() => {});
    },
    /** 处理标签搜索 */
    async handleTagSearch() {
      console.log('🔍 开始搜索标签:', this.tagSearchKeyword);
      
      if (!this.tagSearchKeyword.trim()) {
        this.tagSearchResults = [];
        this.tagSearchLoading = false;
        return;
      }
      
      this.tagSearchLoading = true;
      
      try {
        // 使用导入的searchTags函数
        console.log('📡 调用搜索API:', { keyword: this.tagSearchKeyword.trim() });
        
        const response = await searchTags(this.tagSearchKeyword.trim());
        
        console.log('✅ 搜索API响应:', response);
        
        this.tagSearchResults = response.data || response || [];
        
        console.log('📊 搜索结果:', this.tagSearchResults.length, '个标签');
        
        // 将搜索结果也添加到Map中
        this.tagSearchResults.forEach(tag => {
          this.allTagsMap.set(tag.id, tag);
        });
      } catch (error) {
        console.error('❌ 搜索标签失败:', error);
        this.tagSearchResults = [];
        this.$message.error('搜索标签失败: ' + (error.message || '未知错误'));
      } finally {
        this.tagSearchLoading = false;
      }
    },
    /** 清空标签搜索 */
    handleTagSearchClear() {
      this.tagSearchKeyword = '';
      this.tagSearchResults = [];
    },
    /** 切换标签选择状态 */
    toggleTagSelection(tag) {
      const index = this.batchSelectedTags.indexOf(tag.id);
      if (index > -1) {
        // 已选择，则取消选择
        this.batchSelectedTags.splice(index, 1);
      } else {
        // 未选择，则添加选择
        this.batchSelectedTags.push(tag.id);
      }
    },
    /** 移除标签选择 */
    removeTagSelection(tagId) {
      const index = this.batchSelectedTags.indexOf(tagId);
      if (index > -1) {
        this.batchSelectedTags.splice(index, 1);
      }
    },
    /** 获取预览内容长度 */
    getPreviewContentLength() {
      if (!this.currentPreviewVideo.videoContent) return 0;
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = this.currentPreviewVideo.videoContent;
      const textContent = tempDiv.textContent || tempDiv.innerText || '';
      return textContent.length;
    },
    /** 从富文本内容中统计视频数量 */
    getVideoCountFromContent(content) {
      if (!content) return 0;
      const regex = /<div[^>]*class="[^"]*rich-text-video[^"]*"/g;
      const matches = content.match(regex);
      return matches ? matches.length : 0;
    },
    /** 格式化时长 */
    formatDuration(seconds) {
      if (!seconds) return '-';
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const secs = seconds % 60;
      
      if (hours > 0) {
        return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
      } else {
        return `${minutes}:${secs.toString().padStart(2, '0')}`;
      }
    },
    /** 加载所有标签 */
    loadAllTags() {
      // 重新从服务器获取最新的启用标签 (1-启用，0-禁用)
      listTag({ status: 1, pageSize: 1000 }).then(response => {
        // 尝试多种数据结构
        let tags = [];
        if (response.rows && Array.isArray(response.rows)) {
          tags = response.rows;
        } else if (response.data && Array.isArray(response.data)) {
          tags = response.data;
        } else if (Array.isArray(response)) {
          tags = response;
        }
        
        this.tagOptions = tags;
        this.filteredTags = tags;
        this.tagSearchKeyword = '';
        console.log('📋 加载所有标签:', tags.length, '个标签');
        

      }).catch((error) => {
        console.error('❌ 获取标签列表失败:', error);
        this.$message.error('获取标签列表失败');
        this.tagOptions = [];
        this.filteredTags = [];
      });
    },
    /** 过滤标签 */
    filterTags() {
      if (this.tagSearchKeyword) {
        this.filteredTags = this.tagOptions.filter(tag => tag.name.toLowerCase().includes(this.tagSearchKeyword.toLowerCase()));
      } else {
        this.filteredTags = this.tagOptions;
      }
    },
    /** 检查标签是否被选中 */
    isTagSelected(tagId) {
      return this.form.tagIds && this.form.tagIds.includes(tagId);
    },
    /** 切换标签选择状态 */
    toggleTag(tag) {
      if (this.isTagSelected(tag.id)) {
        this.removeTag(tag.id);
      } else {
        this.addExistingTag(tag.id);
      }
    },
    /** 确认标签选择 */
    confirmTagSelection() {
      this.showTagDialog = false;
      this.form.tagIds = this.selectedTags.map(tag => tag.id);
    },
    /** 取消标签选择 */
    cancelCreateTag() {
      this.showCreateTagDialog = false;
      this.newTagForm.name = '';
      this.$nextTick(() => {
        if (this.$refs.createTagForm) {
          this.$refs.createTagForm.clearValidate();
        }
      });
    },
    /** 创建新标签 */
    confirmCreateTag() {
      this.$refs.createTagForm.validate(valid => {
        if (valid) {
          const newTag = {
            name: this.newTagForm.name,
            status: 1 // 1=启用，0=禁用（修正状态值）
          };
          
          createTagApi(newTag).then(response => {
            const newTagData = response.data;
            this.tagOptions.push(newTagData);
            this.selectedTags.push(newTagData);
            this.filteredTags.push(newTagData);
            if (!this.form.tagIds) {
              this.form.tagIds = [];
            }
            this.form.tagIds.push(newTagData.id);
            this.showCreateTagDialog = false;
            this.newTagForm.name = '';
            this.$message.success('标签创建成功');

          }).catch(error => {
            this.$message.error('标签创建失败：' + (error.message || '未知错误'));
          });
        }
      });
    },

    /** 打开标签选择弹窗 */
    openTagDialog() {
      console.log('🎯 打开标签选择弹窗');
      this.showTagDialog = true;
    },

    /** 预览副文本 */
    handlePreviewContent(row) {
      console.log('🔄 开始预览副文本, 视频ID:', row.id);
      
      // 🔧 修复：获取处理后的富文本内容（包含签名URL）
      getVideoProcessedContent(row.id).then(response => {
        console.log('📄 API响应:', response);
        
        const videoContent = response.msg || response.data || '';
        console.log('📝 处理后的内容长度:', videoContent.length);
        console.log('🔍 内容预览:', videoContent.substring(0, 500));
        
        // 检查是否包含data-resource-key
        if (videoContent.includes('data-resource-key')) {
          console.warn('⚠️ 内容仍包含data-resource-key，可能转换失败');
          console.log('🔗 data-resource-key实例:', videoContent.match(/data-resource-key="[^"]+"/g));
        }
        
        // 检查是否包含img标签
        const imgMatches = videoContent.match(/<img[^>]*>/g);
        if (imgMatches) {
          console.log('🖼️ 找到图片标签:', imgMatches);
        } else {
          console.log('❌ 未找到图片标签');
        }
        
        this.currentPreviewVideo = {
          ...row,
          videoContent: videoContent
        };
        
        this.contentPreviewVisible = true;
      }).catch((error) => {
        console.error('❌ 获取视频内容失败:', error);
        this.$message.error('获取视频内容失败');
      });
    },

    /** 处理图片加载错误 */
    handleImageError(event) {
      // 显示一个简单的错误提示，而不是替换为默认图片
      event.target.style.display = 'none';
      const errorDiv = document.createElement('div');
      errorDiv.innerHTML = '<i class="el-icon-picture" style="font-size: 48px; color: #ccc;"></i><br><span style="color: #999; font-size: 12px;">图片加载失败</span>';
      errorDiv.style.cssText = 'text-align: center; padding: 20px; border: 1px dashed #ddd; border-radius: 4px; background-color: #f9f9f9;';
      event.target.parentNode.appendChild(errorDiv);
    },
    /** 处理图片加载成功 */
    handleImageLoad() {
      // 处理图片加载成功后的逻辑
    },
    /** 检查URL是否有效 */
    isValidUrl(url) {
      if (!url) return false;
      
      try {
        const urlObj = new URL(url);
        // 检查是否是http或https协议
        if (!['http:', 'https:'].includes(urlObj.protocol)) {
          return false;
        }
        
        // 检查是否是图片格式
        const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'];
        const pathname = urlObj.pathname.toLowerCase();
        const hasImageExtension = imageExtensions.some(ext => pathname.endsWith(ext));
        
        // 如果没有明确的图片扩展名，但URL看起来合理，也认为是有效的
        // 这样可以支持一些动态生成的图片URL
        return hasImageExtension || pathname.includes('image') || pathname.includes('img') || url.includes('image') || url.includes('img');
      } catch (_) {
        return false;
      }
    },
    /** 处理URL粘贴事件 */
    handleUrlPaste(event) {
      // 延迟一点时间让粘贴内容生效
      this.$nextTick(() => {
        if (this.form.coverImage && this.isValidUrl(this.form.coverImage)) {
          this.$message.success('检测到有效的图片URL');
        }
      });
    },
    /** 处理URL输入事件 */
    handleUrlInput(value) {
      // 实时验证URL，但不显示过多提示
      if (value && value.length > 10 && this.isValidUrl(value)) {
        // URL有效时的处理
      }
    },
    /** 测试封面URL是否可访问 */
    testCoverUrl(url) {
      if (!url) return;
      
      console.log('🧪 测试封面URL:', url);
      
      // 测试方法1: 创建img元素测试
      const testImg = new Image();
      testImg.onload = () => {
        console.log('✅ 封面URL可以正常加载:', url);
      };
      testImg.onerror = (error) => {
        console.error('❌ 封面URL加载失败:', url, error);
        
        // 进一步测试：直接fetch请求
        fetch(url)
          .then(response => {
            console.log('🌐 Fetch响应状态:', response.status, response.statusText);
            console.log('📄 Fetch响应头:', Array.from(response.headers.entries()));
          })
          .catch(fetchError => {
            console.error('💥 Fetch请求失败:', fetchError);
          });
      };
      testImg.src = url;
    }
  }
};
</script>

<style scoped>
.tag-selector {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  background-color: #fafafa;
}

.selected-tags {
  margin-bottom: 10px;
  min-height: 32px;
}

.popular-tags .el-tag {
  transition: all 0.3s;
}

.popular-tags .el-tag:hover:not(.is-disabled) {
  transform: scale(1.05);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.popular-tags .el-tag.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.el-select-dropdown__item {
  padding: 8px 20px;
}





.cover-image-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 15px;
  background-color: #fafafa;
}

.url-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px;
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.url-preview img {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.url-error {
  text-align: center;
}



/* 预览对话框样式 */
.video-content-preview .content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0 0 15px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.video-content-preview .video-info .video-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.video-content-preview .video-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #666;
}

.video-content-preview .action-buttons {
  flex-shrink: 0;
}

.video-content-preview .content-body {
  min-height: 200px;
  margin-bottom: 20px;
}

.video-content-preview .empty-content {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.video-content-preview .empty-content i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
}

.video-content-preview .content-footer {
  padding: 15px 0 0 0;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #666;
}

.video-content-preview .content-stats {
  display: flex;
  gap: 15px;
}

/* 批量标签搜索样式 */
.tag-search-results {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  background-color: #fafafa;
}

.selected-tags {
  max-height: 120px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  background-color: #f0f9ff;
}

.search-info {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-results, .searching {
  padding: 20px;
  text-align: center;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed #e4e7ed;
  border-radius: 4px;
  background-color: #fafafa;
}

/* scoped样式块中不再包含标签弹窗样式，避免冲突 */
</style>

<!-- 专门为标签选择弹窗的全局样式 - 不受scoped限制 -->
<style>
/* 标签选择弹窗 - 使用唯一类名避免冲突 */
.video-tag-selector-dialog {
  width: 900px !important;
  max-width: 90vw !important;
}

.video-tag-selector-dialog .el-dialog__body {
  padding: 20px !important;
}

.video-tag-selector-dialog .tag-dialog-content {
  padding: 0 !important;
}

.video-tag-selector-dialog .tag-search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.video-tag-selector-dialog .tag-list-container {
  margin-bottom: 20px;
  max-height: 500px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 15px;
}

.video-tag-selector-dialog .tag-grid {
  display: grid !important;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)) !important;
  gap: 10px !important;
}

.video-tag-selector-dialog .tag-item {
  margin: 0 !important;
  padding: 8px 12px !important;
  border: 1px solid #dcdfe6 !important;
  border-radius: 4px !important;
  background: #f5f7fa !important;
  transition: all 0.3s !important;
  cursor: pointer !important;
}

.video-tag-selector-dialog .tag-item:hover {
  border-color: #409eff !important;
  background: #ecf5ff !important;
}

.video-tag-selector-dialog .tag-item.selected {
  border-color: #409eff !important;
  background: #ecf5ff !important;
}

.video-tag-selector-dialog .tag-item.selected .el-checkbox__label {
  color: #409eff !important;
  font-weight: 500 !important;
}

.video-tag-selector-dialog .tag-item .el-checkbox {
  pointer-events: none;
}

.video-tag-selector-dialog .tag-item .el-checkbox__label {
  font-size: 14px !important;
  color: #606266 !important;
}

.video-tag-selector-dialog .selected-count {
  text-align: right;
  margin-top: 10px;
  color: #606266;
  font-size: 14px;
}

.video-tag-selector-dialog .empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.video-tag-selector-dialog .empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  display: block;
}

/* 批量操作对话框样式 */
.batch-info {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background-color: #f4f4f5;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
}

.batch-info i {
  margin-right: 8px;
  font-size: 16px;
}

.batch-info .el-icon-info {
  color: #409eff;
}

.batch-info .el-icon-warning {
  color: #e6a23c;
}
</style>

 