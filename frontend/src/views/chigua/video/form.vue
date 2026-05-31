<template>
  <div class="app-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button 
          type="text" 
          icon="el-icon-arrow-left" 
          @click="goBack"
          style="font-size: 16px; color: #409EFF;"
        >
          返回视频列表
        </el-button>
      </div>
      <div class="header-center">
        <h2>{{ pageTitle }}</h2>
      </div>
      <div class="header-right">
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ form.id ? '更新' : '保存' }}
        </el-button>
      </div>
    </div>

    <!-- 表单内容 -->
    <el-card class="form-card" shadow="never">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <!-- 基本信息 -->
        <el-card class="section-card" shadow="never">
          <div slot="header" class="section-header">
            <span>基本信息</span>
          </div>
          
          <!-- 状态放在标题上方 -->
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择状态" style="width: 300px">
              <el-option label="草稿" :value="0"></el-option>
              <el-option label="已发布" :value="1"></el-option>
              <el-option label="已下架" :value="2"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="视频标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入视频标题" maxlength="300" show-word-limit />
          </el-form-item>

          <el-form-item label="副标题" prop="subtitle">
            <el-input v-model="form.subtitle" placeholder="请输入副标题" maxlength="500" show-word-limit />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="作者" prop="author">
                <el-input v-model="form.author" placeholder="请输入作者" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="时长(秒)" prop="duration">
                <el-input-number v-model="form.duration" :min="0" placeholder="请输入时长" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="视频描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入视频描述" maxlength="1000" show-word-limit />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="是否推荐">
                <el-switch v-model="form.isRecommended" :active-value="1" :inactive-value="0"></el-switch>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="是否热门">
                <el-switch v-model="form.isHot" :active-value="1" :inactive-value="0"></el-switch>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="排序权重">
                <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="发布时间" prop="publishedAt">
                <el-date-picker
                  v-model="form.publishedAt"
                  type="datetime"
                  placeholder="请选择发布时间"
                  format="yyyy-MM-dd HH:mm:ss"
                  value-format="yyyy-MM-dd HH:mm:ss"
                  :clearable="false"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 封面设置 -->
        <el-card class="section-card" shadow="never">
          <div slot="header" class="section-header">
            <span>封面设置</span>
          </div>
          
          <el-form-item label="上传方式">
            <el-radio-group v-model="coverImageType">
              <el-radio label="upload">上传文件</el-radio>
              <el-radio label="url">URL地址</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="coverImageType === 'upload'" label="封面图片" prop="coverImage">
            <triple-cover-composer
              v-model="form.coverImage"
              :action="uploadCoverUrl"
              :existing-signed-url="form.coverUrl"
            />
          </el-form-item>

          <el-form-item v-if="coverImageType === 'url'" label="封面URL" prop="coverImage">
            <el-input v-model="form.coverImage" placeholder="请输入封面图片URL" />
          </el-form-item>
        </el-card>

        <!-- 分类标签 -->
        <el-card class="section-card" shadow="never">
          <div slot="header" class="section-header">
            <span>分类标签</span>
          </div>
          
          <!-- 分类和标签放在一行 -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="分类选择">
                <el-input
                  v-model="selectedCategoriesText"
                  placeholder="点击选择分类"
                  readonly
                  @click="showCategoryDialog = true"
                  style="cursor: pointer;">
                  <el-button slot="append" icon="el-icon-more" @click="showCategoryDialog = true"></el-button>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="标签选择">
                <el-input
                  v-model="selectedTagsText"
                  placeholder="点击选择标签"
                  readonly
                  @click="openTagDialog"
                  style="cursor: pointer;">
                  <el-button slot="append" icon="el-icon-more" @click="openTagDialog"></el-button>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 视频内容 -->
        <el-card class="section-card" shadow="never">
          <div slot="header" class="section-header">
            <span>视频内容（富文本）</span>
            <div class="section-tools">
              <el-button size="mini" @click="previewContent">预览内容</el-button>
            </div>
          </div>
          
          <el-form-item label="视频内容" prop="videoContent">
            <Editor
              ref="editor"
              v-model="form.videoContent"
              :video-id="form.id"
              :min-height="500"
              :file-size="10"
            />
          </el-form-item>
        </el-card>


      </el-form>
    </el-card>

    <!-- 内容预览对话框 -->
    <el-dialog
      title="内容预览"
      :visible.sync="previewVisible"
      width="80%"
      append-to-body>
      <div class="content-preview" v-html="form.videoContent"></div>
    </el-dialog>

    <!-- 分类选择对话框 -->
    <el-dialog
      title="选择分类"
      :visible.sync="showCategoryDialog"
      width="600px"
      append-to-body>
      <div class="category-selection">
        <div class="selection-header">
          <el-input
            v-model="categorySearchText"
            placeholder="搜索分类..."
            prefix-icon="el-icon-search"
            style="margin-bottom: 15px;">
          </el-input>
          
          <!-- 分类全选控制 -->
          <div class="select-controls" style="margin-bottom: 15px;">
            <el-button 
              size="small" 
              type="primary" 
              plain
              @click="selectAllCategories">
              全选分类
            </el-button>
            <el-button 
              size="small" 
              plain
              @click="clearAllCategories">
              取消全选
            </el-button>
            <span class="selected-count">
              已选择: {{ selectedCategories.length }} / {{ filteredCategories.length }}
            </span>
          </div>
        </div>
        <div class="selection-content">
          <el-checkbox-group v-model="selectedCategories" @change="updateCategoriesText">
            <div class="category-grid">
              <el-checkbox
                v-for="category in filteredCategories"
                :key="category.id"
                :label="category.id"
                class="category-item">
                {{ category.name }}
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showCategoryDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCategorySelection">确定</el-button>
      </div>
    </el-dialog>

    <!-- 标签选择对话框 -->
    <el-dialog
      title="选择标签"
      :visible.sync="showTagDialog"
      width="600px"
      append-to-body>
      <div class="tag-selection">
        <div class="selection-header">
          <el-row :gutter="10">
            <el-col :span="16">
              <el-input
                v-model="tagSearchText"
                placeholder="搜索标签..."
                prefix-icon="el-icon-search">
              </el-input>
            </el-col>
            <el-col :span="8">
              <el-button type="primary" @click="showAddTagForm = !showAddTagForm">
                {{ showAddTagForm ? '取消' : '新增标签' }}
              </el-button>
            </el-col>
          </el-row>
          
          <!-- 新增标签表单 -->
          <div v-if="showAddTagForm" class="add-tag-form">
            <el-row :gutter="10" style="margin-top: 15px;">
              <el-col :span="16">
                <el-input
                  v-model="newTagName"
                  placeholder="请输入新标签名称"
                  @keyup.enter="addNewTag">
                </el-input>
              </el-col>
              <el-col :span="8">
                <el-button type="success" @click="addNewTag" :loading="addingTag">添加</el-button>
              </el-col>
            </el-row>
          </div>

          <!-- 标签全选控制 -->
          <div class="select-controls" style="margin-top: 15px;">
            <el-button 
              size="small" 
              type="primary" 
              plain
              @click="selectAllTags">
              全选标签
            </el-button>
            <el-button 
              size="small" 
              plain
              @click="clearAllTags">
              取消全选
            </el-button>
            <el-button 
              size="small" 
              type="success"
              plain
              @click="getRecommendedTags"
              :disabled="selectedTags.length === 0"
              :loading="recommendLoading">
              智能推荐
            </el-button>
            <span class="selected-count">
              已选择: {{ selectedTags.length }} / {{ filteredTags.length }}
            </span>
          </div>
        </div>
        
        <!-- 推荐标签区域 -->
        <div v-if="recommendedTags.length > 0" class="recommended-tags" style="margin-top: 15px; padding: 15px; background-color: #f5f7fa; border-radius: 4px;">
          <div style="margin-bottom: 10px; font-weight: 500; color: #409eff;">
            <i class="el-icon-star-on"></i> 智能推荐标签 (基于已选标签语义分析)
          </div>
          <el-checkbox-group v-model="selectedTags" @change="updateTagsText">
            <div class="tag-grid">
              <el-checkbox
                v-for="tag in recommendedTags"
                :key="'rec_' + tag.id"
                :label="tag.id"
                class="tag-item recommended-tag">
                {{ tag.name }} <span class="recommendation-badge">推荐</span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </div>

        <div class="selection-content" style="margin-top: 15px;">
          <div v-if="!recommendedTags.length || tagSearchText" style="margin-bottom: 10px; font-weight: 500; color: #606266;">
            全部标签 ({{ tagPagination.total }} 个)
          </div>
          
          <!-- 加载状态 -->
          <div v-if="tagLoading" style="text-align: center; padding: 20px;">
            <i class="el-icon-loading"></i> 正在加载标签...
          </div>
          
          <el-checkbox-group v-model="selectedTags" @change="updateTagsText">
            <div class="tag-grid">
              <el-checkbox
                v-for="tag in filteredTags"
                :key="tag && tag.id ? tag.id : Math.random()"
                :label="tag.id"
                class="tag-item"
                v-if="tag && tag.id && tag.name">
                {{ tag.name }}
              </el-checkbox>
            </div>
          </el-checkbox-group>
          
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreTags" style="text-align: center; margin-top: 15px;">
            <el-button 
              type="primary" 
              plain 
              @click="loadMoreTags"
              :loading="tagLoading">
              加载更多 ({{ tagOptions.length }}/{{ tagPagination.total }})
            </el-button>
          </div>
          
          <!-- 没有更多数据提示 -->
          <div v-else-if="tagOptions.length > 0 && tagPagination.total > 0" style="text-align: center; margin-top: 15px; color: #909399; font-size: 12px;">
            已加载全部标签
          </div>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showTagDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmTagSelection">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getVideo, addVideo, updateVideo, getCategories, getTags, getTagsPage, getTagsByIds, createTag, recommendTags } from "@/api/chigua/video"
import ImageUpload from '@/components/ImageUpload'
import TripleCoverComposer from '@/components/TripleCoverComposer'
import Editor from '@/components/Editor'

export default {
  name: "VideoForm",
  components: {
    ImageUpload,
    TripleCoverComposer,
    Editor
  },
  data() {
    return {
      // 页面状态
      submitting: false,
      previewVisible: false,
      
      // 对话框状态
      showCategoryDialog: false,
      showTagDialog: false,
      showAddTagForm: false,
      addingTag: false,
      recommendLoading: false,
      
      // 搜索文本
      categorySearchText: '',
      tagSearchText: '',
      newTagName: '',
      
      // 显示文本
      selectedCategoriesText: '',
      selectedTagsText: '',
      
      // 表单数据
      form: {
        id: null,
        title: '',
        subtitle: '',
        description: '',
        videoContent: '',
        coverImage: '',
        duration: null,
        author: '',
        categoryId: null,
        status: 0,
        isRecommended: 0,
        isHot: 0,
        sortOrder: 0,
        publishedAt: null
      },
      
      // 封面上传方式（默认选中上传文件）
      coverImageType: 'upload',
      
      // 选择的分类和标签
      selectedCategories: [],
      selectedTags: [],
      
      // 选项数据
      categoryOptions: [],
      tagOptions: [],
      recommendedTags: [],
      
      // 标签分页数据
      tagPagination: {
        pageNum: 1,
        pageSize: 50,
        total: 0
      },
      tagLoading: false,
      tagSearchTimer: null,
      
      // 表单验证规则
      rules: {
        title: [
          { required: true, message: "视频标题不能为空", trigger: "blur" },
          { min: 1, max: 300, message: "视频标题长度在 1 到 300 个字符", trigger: "blur" }
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
      
      // 上传URL - 使用Worker上传接口
      uploadCoverUrl: "https://chigua-r2-worker.xingaikaka.workers.dev/upload"
    }
  },
  computed: {
    pageTitle() {
      return this.form.id ? '编辑视频' : '新增视频'
    },
    
    // 过滤后的分类
    filteredCategories() {
      if (!this.categorySearchText) {
        return this.categoryOptions
      }
      return this.categoryOptions.filter(category => 
        category.name.toLowerCase().includes(this.categorySearchText.toLowerCase())
      )
    },
    
    // 过滤后的标签
    filteredTags() {
      // 先过滤掉无效的标签对象
      const validTags = this.tagOptions.filter(tag => tag && tag.id && tag.name)
      
      if (!this.tagSearchText) {
        return validTags
      }
      return validTags.filter(tag => 
        tag.name.toLowerCase().includes(this.tagSearchText.toLowerCase())
      )
    },
    
    // 是否还有更多标签可以加载
    hasMoreTags() {
      return this.tagOptions.length < this.tagPagination.total && !this.tagSearchText
    }
  },
  
  watch: {
    // 监听路由变化，当路由参数改变时重新加载数据
    '$route'(to, from) {
      if (to.params.id !== from.params.id) {
        console.log('🔄 路由参数变化，重新加载数据:', { from: from.params.id, to: to.params.id })
        // 🔧 路由变化时也强制设置封面上传方式为默认值
        this.coverImageType = 'upload'
        this.loadData()
      }
    },
    
    // 监听标签搜索文本变化
    tagSearchText: {
      handler(newVal, oldVal) {
        if (newVal !== oldVal) {
          // 延迟搜索，避免频繁请求
          clearTimeout(this.tagSearchTimer)
          this.tagSearchTimer = setTimeout(() => {
            this.searchTags()
          }, 300)
        }
      }
    }
  },
  
  created() {
    // 设置默认发布时间
    if (!this.form.publishedAt) {
      this.form.publishedAt = this.getCurrentDateTime()
    }
    // 🔧 确保封面上传方式默认选中"上传文件"
    this.coverImageType = 'upload'
    
    // 🔧 修复：先加载分类数据，再加载视频数据，确保回显正常
    this.getCategories().then(() => {
      this.loadData()
    })
    // 不再在初始化时加载所有标签，改为在打开对话框时分页加载
  },
  
  mounted() {
    // 🔧 组件挂载后再次确保封面上传方式为默认值
    this.$nextTick(() => {
      this.coverImageType = 'upload'
      console.log('🖼️ 组件挂载完成，确保封面上传方式为默认值')
    })
  },
  methods: {
    /** 加载数据 */
    loadData() {
      // 🔧 每次加载数据时都强制设置封面上传方式为默认值
      this.coverImageType = 'upload'
      
      const id = this.$route.params.id
      if (id && id !== 'add') {
        this.getVideoInfo(id)
      } else {
        // 新增模式：重置所有表单数据
        this.resetAllFormData()
      }
    },
    
    /** 获取视频信息 */
    getVideoInfo(id) {
      // 🔧 修复：使用getVideo接口获取带签名URL的数据，而不是raw接口
      getVideo(id).then(response => {
        this.form = response.data
        
        // 处理分类和标签
        if (this.form.categoryIds) {
          this.selectedCategories = [...this.form.categoryIds]
          console.log('📁 设置选中分类:', {
            categoryIds: this.form.categoryIds,
            categoryOptionsLoaded: this.categoryOptions.length
          })
          this.updateCategoriesText()
        }
        if (this.form.tagIds) {
          this.selectedTags = [...this.form.tagIds]
          console.log('🏷️ 设置选中标签:', {
            tagIds: this.form.tagIds,
            tagOptionsLoaded: this.tagOptions.length
          })
          // 🔧 修复：立即更新标签显示文本，确保回显正常
          this.updateTagsText()
        }
        
        // 🔧 强制默认选择"上传文件"，不进行智能判断
        // 用户需要手动切换到URL方式（如果需要的话）
        this.coverImageType = 'upload'
        console.log('🖼️ 强制默认选择上传文件方式，封面URL:', this.form.coverImage)
      })
    },
    
    /** 获取分类列表 */
    getCategories() {
      return getCategories().then(response => {
        this.categoryOptions = response.data
        // 🔧 修复：分类数据加载完成后，如果已有选中分类，重新更新显示文本
        if (this.selectedCategories && this.selectedCategories.length > 0) {
          this.updateCategoriesText()
          console.log('📁 分类数据加载完成，重新更新显示文本:', {
            selectedCategories: this.selectedCategories,
            categoryOptions: this.categoryOptions.length,
            selectedCategoriesText: this.selectedCategoriesText
          })
        }
        return response.data
      })
    },
    
    /** 获取标签列表（保留原方法用于兼容性） */
    getTags() {
      return getTags().then(response => {
        // 确保返回的数据是数组格式，并过滤掉无效数据
        const tags = Array.isArray(response.data) ? response.data : []
        this.tagOptions = tags.filter(tag => tag && tag.id && tag.name)
        
        console.log('🏷️ 标签数据加载完成:', {
          原始数据长度: tags.length,
          有效数据长度: this.tagOptions.length,
          无效数据: tags.filter(tag => !tag || !tag.id || !tag.name)
        })
        
        // 🔧 修复：标签数据加载完成后，如果已有选中标签，重新更新显示文本
        if (this.selectedTags && this.selectedTags.length > 0) {
          this.updateTagsText()
          console.log('🏷️ 标签数据加载完成，重新更新显示文本:', {
            selectedTags: this.selectedTags,
            tagOptions: this.tagOptions.length,
            selectedTagsText: this.selectedTagsText
          })
        }
        
        return this.tagOptions
      }).catch(error => {
        console.error('获取标签列表失败:', error)
        this.tagOptions = []
        this.$message.error('获取标签列表失败')
        return []
      })
    },

    /** 分页获取标签列表（用于弹出层） */
    getTagsPage(reset = false) {
      if (reset) {
        this.tagPagination.pageNum = 1
        // 🔧 修复：重置时保留已选择的标签，只清空其他标签
        if (this.selectedTags && this.selectedTags.length > 0) {
          // 保留已选择的标签
          this.tagOptions = this.tagOptions.filter(tag => 
            tag && tag.id && this.selectedTags.includes(tag.id)
          )
          console.log('🏷️ 重置时保留已选择标签:', this.tagOptions.length)
        } else {
          this.tagOptions = []
        }
      }
      
      this.tagLoading = true
      const params = {
        pageNum: this.tagPagination.pageNum,
        pageSize: this.tagPagination.pageSize
      }
      
      // 如果有搜索关键词，添加到查询参数
      if (this.tagSearchText && this.tagSearchText.trim()) {
        params.name = this.tagSearchText.trim()
      }
      
      return getTagsPage(params).then(response => {
        const newTags = Array.isArray(response.rows) ? response.rows : []
        const validTags = newTags.filter(tag => tag && tag.id && tag.name)
        
        if (reset) {
          // 重置时，将新标签与已保留的选中标签合并
          const existingIds = new Set(this.tagOptions.map(tag => tag.id))
          const uniqueTags = validTags.filter(tag => !existingIds.has(tag.id))
          this.tagOptions = [...this.tagOptions, ...uniqueTags]
        } else {
          // 追加新数据，避免重复
          const existingIds = new Set(this.tagOptions.map(tag => tag.id))
          const uniqueTags = validTags.filter(tag => !existingIds.has(tag.id))
          this.tagOptions = [...this.tagOptions, ...uniqueTags]
        }
        
        this.tagPagination.total = response.total || 0
        
        console.log('🏷️ 分页标签数据加载完成:', {
          当前页: this.tagPagination.pageNum,
          页面大小: this.tagPagination.pageSize,
          总数: this.tagPagination.total,
          本次加载: validTags.length,
          累计加载: this.tagOptions.length,
          搜索关键词: this.tagSearchText,
          已选择标签数: this.selectedTags ? this.selectedTags.length : 0
        })
        
        return this.tagOptions
      }).catch(error => {
        console.error('获取标签列表失败:', error)
        this.$message.error('获取标签列表失败')
        return []
      }).finally(() => {
        this.tagLoading = false
      })
    },

    /** 搜索标签 */
    searchTags() {
      console.log('🔍 搜索标签:', this.tagSearchText)
      this.getTagsPage(true) // 重置并搜索
    },

    /** 加载更多标签 */
    loadMoreTags() {
      if (this.tagLoading || !this.hasMoreTags) {
        return
      }
      
      this.tagPagination.pageNum += 1
      console.log('📄 加载更多标签，页码:', this.tagPagination.pageNum)
      this.getTagsPage(false) // 追加加载
    },

    /** 打开标签选择对话框 */
    async openTagDialog() {
      this.showTagDialog = true
      // 重置搜索和分页状态
      this.tagSearchText = ''
      this.tagPagination.pageNum = 1
      
      // 🔧 修复：保留已有的tagOptions，不要清空，避免丢失已选择标签的信息
      // this.tagOptions = [] // 注释掉这行，保留已有标签数据
      
      // 如果有已选择的标签，先加载这些标签以确保能够正确显示
      if (this.selectedTags && this.selectedTags.length > 0) {
        try {
          console.log('🏷️ 加载已选择的标签:', this.selectedTags)
          const selectedTagsResponse = await getTagsByIds(this.selectedTags)
          const selectedTagsData = Array.isArray(selectedTagsResponse.data) ? selectedTagsResponse.data : []
          
          // 🔧 将已选择的标签合并到现有选项中，避免重复
          const validSelectedTags = selectedTagsData.filter(tag => tag && tag.id && tag.name)
          if (validSelectedTags.length > 0) {
            const existingIds = new Set(this.tagOptions.map(tag => tag.id))
            const newTags = validSelectedTags.filter(tag => !existingIds.has(tag.id))
            this.tagOptions = [...this.tagOptions, ...newTags]
            console.log('🏷️ 已选择标签合并完成，新增:', newTags.length, '总计:', this.tagOptions.length)
          }
        } catch (error) {
          console.error('加载已选择标签失败:', error)
        }
      }
      
      // 加载第一页标签，与已有标签合并
      this.getTagsPage(false) // 使用false避免清空已加载的选中标签
    },
    
    /** 更新分类显示文本 */
    updateCategoriesText() {
      if (!this.categoryOptions || this.categoryOptions.length === 0) {
        console.log('⚠️ 分类选项未加载，无法更新显示文本')
        return
      }
      
      if (!this.selectedCategories || this.selectedCategories.length === 0) {
        this.selectedCategoriesText = ''
        return
      }
      
      // 🔧 验证分类选择：不能同时选择长视频和短视频分类
      const selectedCategoryObjects = this.categoryOptions
        .filter(category => this.selectedCategories.includes(category.id))
      
      const hasShortVideo = selectedCategoryObjects.some(c => c.isShort === 1)
      const hasLongVideo = selectedCategoryObjects.some(c => c.isShort === 0 || c.isShort === null)
      
      if (hasShortVideo && hasLongVideo) {
        const shortCategories = selectedCategoryObjects
          .filter(c => c.isShort === 1)
          .map(c => c.name)
          .join(', ')
        const longCategories = selectedCategoryObjects
          .filter(c => c.isShort === 0 || c.isShort === null)
          .map(c => c.name)
          .join(', ')
        
        this.$message.error(`不能同时选择长视频分类和短视频分类！\n短视频分类：[${shortCategories}]\n长视频分类：[${longCategories}]`)
        
        // 恢复到上一次有效的选择（清空当前选择）
        this.selectedCategories = []
        this.selectedCategoriesText = ''
        return
      }
      
      const selectedNames = selectedCategoryObjects.map(category => category.name)
      this.selectedCategoriesText = selectedNames.join(', ')
      
      // 显示视频类型提示
      if (hasShortVideo) {
        console.log('✅ 选择的是短视频分类，视频将被设置为短视频类型')
      } else if (hasLongVideo) {
        console.log('✅ 选择的是长视频分类，视频将被设置为长视频类型')
      }
      
      console.log('📁 更新分类显示文本:', {
        selectedCategories: this.selectedCategories,
        foundNames: selectedNames,
        selectedCategoriesText: this.selectedCategoriesText,
        categoryOptionsCount: this.categoryOptions.length,
        hasShortVideo,
        hasLongVideo
      })
    },
    
    /** 更新标签显示文本 */
    updateTagsText() {
      if (!this.selectedTags || this.selectedTags.length === 0) {
        this.selectedTagsText = ''
        return
      }
      
      // 如果tagOptions为空或不包含所有选中标签，尝试从服务器获取标签信息
      const foundTags = []
      const missingTagIds = []
      
      if (this.tagOptions && this.tagOptions.length > 0) {
        // 从已加载的标签中查找
        this.selectedTags.forEach(tagId => {
          const tag = this.tagOptions.find(t => t && t.id === tagId)
          if (tag && tag.name) {
            foundTags.push(tag.name)
          } else {
            missingTagIds.push(tagId)
          }
        })
      } else {
        // 如果没有加载任何标签，所有选中的标签都需要获取
        missingTagIds.push(...this.selectedTags)
      }
      
      // 如果有未找到的标签，从服务器获取
      if (missingTagIds.length > 0) {
        console.log('🏷️ 需要从服务器获取标签信息:', missingTagIds)
        getTagsByIds(missingTagIds).then(response => {
          const missingTags = Array.isArray(response.data) ? response.data : []
          const missingNames = missingTags
            .filter(tag => tag && tag.id && tag.name)
            .map(tag => tag.name)
          
          // 🔧 将获取到的标签添加到tagOptions中，确保后续能找到
          const validMissingTags = missingTags.filter(tag => tag && tag.id && tag.name)
          if (validMissingTags.length > 0) {
            // 避免重复添加
            const existingIds = new Set(this.tagOptions.map(tag => tag.id))
            const newTags = validMissingTags.filter(tag => !existingIds.has(tag.id))
            this.tagOptions = [...this.tagOptions, ...newTags]
            console.log('🏷️ 已将获取的标签添加到选项中:', newTags.length)
          }
          
          // 合并已找到的和新获取的标签名称
          const allNames = [...foundTags, ...missingNames]
          this.selectedTagsText = allNames.join(', ')
          
          console.log('🏷️ 更新标签显示文本（包含服务器获取）:', {
            selectedTags: this.selectedTags,
            foundInOptions: foundTags,
            fetchedFromServer: missingNames,
            finalText: this.selectedTagsText
          })
        }).catch(error => {
          console.error('获取标签信息失败:', error)
          // 如果获取失败，至少显示已找到的标签
          this.selectedTagsText = foundTags.join(', ')
        })
      } else {
        // 所有标签都已找到
        this.selectedTagsText = foundTags.join(', ')
        console.log('🏷️ 更新标签显示文本:', {
          selectedTags: this.selectedTags,
          foundNames: foundTags,
          selectedTagsText: this.selectedTagsText
        })
      }
    },
    
    /** 确认分类选择 */
    confirmCategorySelection() {
      this.updateCategoriesText()
      this.showCategoryDialog = false
    },
    
    /** 确认标签选择 */
    confirmTagSelection() {
      this.updateTagsText()
      this.showTagDialog = false
      // 清空推荐标签
      this.recommendedTags = []
    },

    /** 获取推荐标签 */
    async getRecommendedTags() {
      if (this.selectedTags.length === 0) {
        this.$message.warning('请先选择一些标签')
        return
      }
      
      this.recommendLoading = true
      try {
        const response = await recommendTags(this.selectedTags)
        if (response.code === 200) {
          this.recommendedTags = response.data || []
          if (this.recommendedTags.length === 0) {
            this.$message.info('暂无相关推荐标签')
          } else {
            this.$message.success(`为您推荐了 ${this.recommendedTags.length} 个相关标签`)
          }
        } else {
          this.$message.error(response.msg || '获取推荐标签失败')
        }
      } catch (error) {
        console.error('获取推荐标签失败:', error)
        this.$message.error('获取推荐标签失败，请重试')
      } finally {
        this.recommendLoading = false
      }
    },
    
    /** 添加新标签 */
    addNewTag() {
      if (!this.newTagName.trim()) {
        this.$message.warning('请输入标签名称')
        return
      }
      
      // 检查是否已存在
      const exists = this.tagOptions.some(tag => tag && tag.name === this.newTagName.trim())
      if (exists) {
        this.$message.warning('标签已存在')
        return
      }
      
      this.addingTag = true
      createTag({ name: this.newTagName.trim(), status: 1 }).then(response => {
        console.log('🏷️ 新增标签API完整响应:', response)
        console.log('🏷️ response.data:', response.data)
        console.log('🏷️ response.data类型:', typeof response.data)
        
        // 验证返回数据格式 - 临时注释掉严格验证
        // if (!response || !response.data || !response.data.id) {
        //   throw new Error('返回数据格式不正确')
        // }
        
        // 重新加载当前页标签以确保数据一致性
        this.getTagsPage(true).then(() => {
          // 查找刚刚添加的标签（按名称匹配）
          const newTagName = this.newTagName.trim()
          const newTag = this.tagOptions.find(tag => tag.name === newTagName)
          if (newTag && newTag.id) {
            this.selectedTags.push(newTag.id)
            this.updateTagsText()
            console.log('🏷️ 成功选中新添加的标签:', newTag)
          } else {
            console.warn('⚠️ 未找到刚添加的标签:', newTagName)
          }
          
          this.newTagName = ''
          this.showAddTagForm = false
          this.$message.success('标签添加成功')
        })
      }).catch((error) => {
        console.error('标签添加失败:', error)
        this.$message.error('标签添加失败: ' + (error.message || '未知错误'))
      }).finally(() => {
        this.addingTag = false
      })
    },
    
    /** 预览内容 */
    previewContent() {
      if (!this.form.videoContent) {
        this.$message.warning('暂无内容可预览')
        return
      }
      this.previewVisible = true
    },
    
    /** 重置表单 */
    resetForm() {
      const isAddMode = !this.form.id || this.$route.params.id === 'add'
      
      if (isAddMode) {
        // 新增模式：完全重置
        this.resetAllFormData()
      } else {
        // 编辑模式：只重置字段
        this.$refs.form.resetFields()
        this.selectedCategories = []
        this.selectedTags = []
        this.selectedCategoriesText = ''
        this.selectedTagsText = ''
        // 🔧 确保封面上传方式重置为默认的"上传文件"
        this.coverImageType = 'upload'
      }
    },

    /** 完全重置表单数据（新增模式专用） */
    resetAllFormData() {
      // 重置主表单数据
      this.form = {
        id: null,
        title: '',
        subtitle: '',
        description: '',
        videoContent: '',
        coverImage: '',
        duration: null,
        author: '',
        categoryId: null,
        status: 0,
        isRecommended: 0,
        isHot: 0,
        sortOrder: 0,
        publishedAt: this.getCurrentDateTime()
      }
      
      // 重置分类和标签选择
      this.selectedCategories = []
      this.selectedTags = []
      this.selectedCategoriesText = ''
      this.selectedTagsText = ''
      
      // 重置UI状态（确保封面上传方式默认选中"上传文件"）
      this.coverImageType = 'upload'
      this.submitting = false
      this.previewVisible = false
      this.showCategoryDialog = false
      this.showTagDialog = false
      this.showAddTagForm = false
      this.addingTag = false
      this.categorySearchText = ''
      this.tagSearchText = ''
      this.newTagName = ''
      
      // 清除缓存数据，重新获取最新的分类
      this.categoryOptions = []
      this.tagOptions = []
      this.getCategories()
      // 标签数据在打开对话框时再加载
      
      // 清除表单验证状态
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
      
      console.log('✅ 新增模式：表单数据已完全重置，缓存已清理')
    },
    
    /** 提交表单 */
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.$message.error('请完善表单信息')
          return
        }
        
        // 🔧 额外的发布时间验证
        if (!this.form.publishedAt || this.form.publishedAt === '' || this.form.publishedAt === null) {
          this.$message.error('发布时间不能为空')
          this.submitting = false
          return
        }
        
        this.submitting = true
        
        // 🔧 获取富文本的存储格式内容（转换预览URL为资源键）
        let videoContent = this.form.videoContent
        if (this.$refs.editor && this.$refs.editor.getStorageContent) {
          videoContent = this.$refs.editor.getStorageContent()
          console.log('🔄 富文本内容转换:', {
            original: this.form.videoContent?.substring(0, 200) + '...',
            converted: videoContent?.substring(0, 200) + '...'
          })
        }
        
        // 🔧 处理封面图片URL（如果是Worker签名URL，转换为资源键）
        let coverImage = this.form.coverImage
        if (coverImage && coverImage.includes('chigua-r2-worker.xingaikaka.workers.dev/files/')) {
          const pathMatch = coverImage.match(/\/files\/([^?]*)/);
          if (pathMatch) {
            coverImage = decodeURIComponent(pathMatch[1]);
            console.log('🔄 封面图片URL转换:', { original: this.form.coverImage, converted: coverImage });
          }
        }
        
        // 准备提交数据
        const submitData = {
          ...this.form,
          videoContent: videoContent, // 使用转换后的内容
          coverImage: coverImage, // 使用转换后的封面图片
          categoryIds: this.selectedCategories,
          tagIds: this.selectedTags
        }
        
        // 🔧 从富文本中提取第一个视频的video_transcodes.id并设置到transcodeId字段
        // 同时从原始HTML中提取（防止转换后丢失）
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
        
        if (!submitData.transcodeId && extractedTranscodeId) {
          submitData.transcodeId = extractedTranscodeId;
          console.log('🔄 设置transcodeId:', submitData.transcodeId);
        }
        
        const submitMethod = this.form.id ? updateVideo : addVideo
        
        submitMethod(submitData).then(response => {
          this.$modal.msgSuccess(this.form.id ? "修改成功" : "新增成功")
          this.goBack()
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    
    /** 返回列表 */
    goBack() {
      // 🔧 修复：返回列表时刷新数据
      // 方案1：通过路由跳转并传递刷新标识
      this.$router.push({ 
        path: "/chigua/video",
        query: { 
          refresh: Date.now() // 添加时间戳确保刷新
        }
      })
      
      // 方案2：如果使用标签页系统，也要关闭当前页
      this.$nextTick(() => {
        this.$tab.closeOpenPage({ path: "/chigua/video" })
      })
    },

    // ============= 全选功能方法 =============
    
    /** 全选分类 */
    selectAllCategories() {
      this.selectedCategories = this.filteredCategories.map(category => category.id)
      this.updateCategoriesText()
      console.log('✅ 已全选所有分类')
    },

    /** 取消全选分类 */
    clearAllCategories() {
      this.selectedCategories = []
      this.updateCategoriesText()
      console.log('✅ 已取消全选分类')
    },

    /** 全选标签 */
    selectAllTags() {
      // 使用过滤后的有效标签
      this.selectedTags = this.filteredTags
        .filter(tag => tag && tag.id)
        .map(tag => tag.id)
      this.updateTagsText()
      console.log('✅ 已全选所有标签:', {
        selectedCount: this.selectedTags.length,
        totalValidTags: this.filteredTags.length
      })
    },

    /** 取消全选标签 */
    clearAllTags() {
      this.selectedTags = []
      this.updateTagsText()
      console.log('✅ 已取消全选标签')
    },
    
    /** 获取当前日期时间（格式化为YYYY-MM-DD HH:mm:ss） */
    getCurrentDateTime() {
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      const day = String(now.getDate()).padStart(2, '0')
      const hours = String(now.getHours()).padStart(2, '0')
      const minutes = String(now.getMinutes()).padStart(2, '0')
      const seconds = String(now.getSeconds()).padStart(2, '0')
      
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 15px 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  
  .header-left {
    flex: 1;
  }
  
  .header-center {
    flex: 2;
    text-align: center;
    
    h2 {
      margin: 0;
      color: #303133;
      font-size: 18px;
      font-weight: 600;
    }
  }
  
  .header-right {
    flex: 1;
    text-align: right;
  }
}

.form-card {
  border: none;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}

.section-card {
  margin-bottom: 20px;
  border: 1px solid #ebeef5;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    color: #303133;
  }
  
  .section-tools {
    .el-button {
      margin-left: 10px;
    }
  }
}

.content-preview {
  max-height: 60vh;
  overflow-y: auto;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  
  ::v-deep img {
    max-width: 100%;
    height: auto;
  }
  
  ::v-deep video {
    max-width: 100%;
    height: auto;
  }
}

::v-deep .el-card__body {
  padding: 20px;
}

::v-deep .el-form-item__label {
  font-weight: 500;
  color: #606266;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
}

// 分类标签选择对话框样式
.category-selection,
.tag-selection {
  .selection-content {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 15px;
  }
}

.category-grid,
.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  
  .category-item,
  .tag-item {
    margin: 0;
    padding: 8px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background: #f5f7fa;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409eff;
      background: #ecf5ff;
    }
    
    ::v-deep .el-checkbox__label {
      font-size: 14px;
      color: #606266;
    }
    
    &.is-checked {
      border-color: #409eff;
      background: #ecf5ff;
      
      ::v-deep .el-checkbox__label {
        color: #409eff;
        font-weight: 500;
      }
    }
  }
}

.add-tag-form {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

// 全选控制样式
.select-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  
  .el-button {
    margin: 0;
  }
  
  .selected-count {
    margin-left: auto;
    font-size: 12px;
    color: #666;
    font-weight: 500;
  }
}

.recommended-tag {
  .recommendation-badge {
    background-color: #67c23a;
    color: white;
    font-size: 10px;
    padding: 1px 4px;
    border-radius: 3px;
    margin-left: 5px;
  }
}

.recommended-tags {
  border-left: 3px solid #409eff;
}
</style> 