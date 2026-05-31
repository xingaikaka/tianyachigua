<template>
  <div class="video-rich-text-editor">
    <!-- 工具栏 -->
    <div class="editor-toolbar">
      <el-button-group>
        <!-- 常用格式化按钮 -->
        <el-button size="small" icon="el-icon-bold" @click="formatText('bold')" title="粗体"></el-button>
        <el-button size="small" icon="el-icon-italic" @click="formatText('italic')" title="斜体"></el-button>
        <el-button size="small" icon="el-icon-underline" @click="formatText('underline')" title="下划线"></el-button>
      </el-button-group>
      
      <el-button-group style="margin-left: 10px;">
        <!-- 对齐方式 -->
        <el-button size="small" icon="el-icon-align-left" @click="formatText('justifyLeft')" title="左对齐"></el-button>
        <el-button size="small" icon="el-icon-align-center" @click="formatText('justifyCenter')" title="居中"></el-button>
        <el-button size="small" icon="el-icon-align-right" @click="formatText('justifyRight')" title="右对齐"></el-button>
      </el-button-group>

      <el-button-group style="margin-left: 10px;">
        <!-- 列表 -->
        <el-button size="small" icon="el-icon-date" @click="formatText('insertOrderedList')" title="有序列表"></el-button>
        <el-button size="small" icon="el-icon-menu" @click="formatText('insertUnorderedList')" title="无序列表"></el-button>
      </el-button-group>

      <!-- 视频插入按钮 -->
      <div style="display: inline-block; margin-left: 10px;">
        <RichTextVideoSelector @insert-videos="insertVideos" />
      </div>
    </div>

    <!-- 编辑器内容区域 -->
    <div
      ref="editor"
      class="editor-content"
      :style="editorStyles"
      contenteditable="true"
      @input="handleInput"
      @blur="handleBlur"
      @focus="handleFocus"
      v-html="currentContent"
    ></div>

    <!-- 字数统计 -->
    <div class="editor-footer" v-if="showWordCount">
      <span class="word-count">字数：{{ wordCount }}</span>
    </div>
  </div>
</template>

<script>
import RichTextVideoSelector from '@/components/RichTextVideoSelector'

export default {
  name: "VideoRichTextEditor",
  components: {
    RichTextVideoSelector
  },
  props: {
    value: {
      type: String,
      default: ''
    },
    height: {
      type: [Number, String],
      default: 300
    },
    minHeight: {
      type: [Number, String],
      default: 200
    },
    placeholder: {
      type: String,
      default: '请输入内容...'
    },
    readonly: {
      type: Boolean,
      default: false
    },
    showWordCount: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      currentContent: '',
      wordCount: 0,
      focused: false
    }
  },
  computed: {
    editorStyles() {
      return {
        height: typeof this.height === 'number' ? this.height + 'px' : this.height,
        minHeight: typeof this.minHeight === 'number' ? this.minHeight + 'px' : this.minHeight
      }
    }
  },
  watch: {
    value: {
      handler(newVal) {
        if (newVal !== this.currentContent) {
          this.currentContent = newVal || '';
          this.updateWordCount();
        }
      },
      immediate: true
    }
  },
  mounted() {
    this.initEditor();
  },
  methods: {
    initEditor() {
      const editor = this.$refs.editor;
      if (editor) {
        editor.addEventListener('paste', this.handlePaste);
        this.updateWordCount();
      }
    },

    handleInput(event) {
      this.currentContent = event.target.innerHTML;
      this.updateWordCount();
      this.$emit('input', this.currentContent);
    },

    handleBlur() {
      this.focused = false;
      this.$emit('blur');
    },

    handleFocus() {
      this.focused = true;
      this.$emit('focus');
    },

    handlePaste(event) {
      // 处理粘贴内容，清理格式
      event.preventDefault();
      const text = (event.clipboardData || window.clipboardData).getData('text/plain');
      document.execCommand('insertText', false, text);
    },

    formatText(command, value = null) {
      if (this.readonly) return;
      
      this.$refs.editor.focus();
      document.execCommand(command, false, value);
      this.handleInput({ target: this.$refs.editor });
    },

    insertVideos(htmlContents) {
      console.log('🎬 VideoRichTextEditor - insertVideos方法被调用！');
      console.log('🎬 参数htmlContents:', htmlContents);
      console.log('🎬 readonly状态:', this.readonly);
      console.log('🎬 $refs.editor存在:', !!this.$refs.editor);
      
      if (this.readonly) {
        console.log('🎬 编辑器为只读模式，跳过插入');
        return;
      }
      
      try {
        console.log('VideoRichTextEditor - 开始插入视频，数量:', htmlContents.length);
        
        this.$refs.editor.focus();
        htmlContents.forEach((html, i) => {
          console.log(`VideoRichTextEditor - 视频${i} HTML:`, html);
          
          // 验证HTML结构
          const tempDiv = document.createElement('div');
          tempDiv.innerHTML = html;
          const richTextVideoDiv = tempDiv.querySelector('.rich-text-video');
          const videoElement = tempDiv.querySelector('video');
          
          console.log(`VideoRichTextEditor - 视频${i} 结构验证:`, {
            hasRichTextVideoDiv: !!richTextVideoDiv,
            hasVideoElement: !!videoElement,
            videoHTML: videoElement ? videoElement.outerHTML : null,
            videoSources: videoElement ? Array.from(videoElement.querySelectorAll('source')).map(s => ({
              src: s.src,
              type: s.type
            })) : []
          });
        });
        
        const selection = window.getSelection();
        const range = selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
        
        console.log('🎬 selection和range状态:', {
          selectionExists: !!selection,
          rangeCount: selection?.rangeCount || 0,
          rangeExists: !!range
        });
        
        if (range) {
          console.log('🎬 使用range模式插入');
          // 清除选择
          range.deleteContents();
          
          htmlContents.forEach((htmlContent, index) => {
            console.log(`VideoRichTextEditor - 插入视频${index}:`, htmlContent);
            
            // 创建一个临时div来解析HTML
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = htmlContent;
            
            // 获取富文本视频div
            const videoDiv = tempDiv.firstElementChild;
            
            console.log(`VideoRichTextEditor - 创建的videoDiv:`, {
              tagName: videoDiv ? videoDiv.tagName : null,
              className: videoDiv ? videoDiv.className : null,
              innerHTML: videoDiv ? videoDiv.innerHTML : null,
              hasVideo: videoDiv ? !!videoDiv.querySelector('video') : false
            });
            
            if (videoDiv) {
              // 验证video元素
              const video = videoDiv.querySelector('video');
              console.log(`VideoRichTextEditor - video元素验证:`, {
                exists: !!video,
                controls: video ? video.hasAttribute('controls') : false,
                width: video ? video.getAttribute('width') : null,
                style: video ? video.getAttribute('style') : null,
                sources: video ? Array.from(video.querySelectorAll('source')).map(s => ({
                  src: s.src,
                  type: s.type
                })) : []
              });
              
              // 插入视频
              range.insertNode(videoDiv);
              
              // 如果不是最后一个视频，添加分隔符
              if (index < htmlContents.length - 1) {
                const br = document.createElement('br');
                range.insertNode(br);
              }
              
              // 移动光标到插入内容后面
              range.setStartAfter(videoDiv);
              range.collapse(true);
            }
          });
          
          selection.removeAllRanges();
          selection.addRange(range);
        } else {
          // 如果没有选择范围，直接在末尾添加
          console.log('🎬 使用末尾追加模式插入');
          const editor = this.$refs.editor;
          htmlContents.forEach((htmlContent, index) => {
            console.log(`VideoRichTextEditor - 末尾插入视频${index}:`, htmlContent);
            const videoDiv = document.createElement('div');
            videoDiv.innerHTML = htmlContent;
            videoDiv.style.margin = '15px 0';
            
            console.log(`VideoRichTextEditor - 末尾插入videoDiv:`, {
              innerHTML: videoDiv.innerHTML,
              hasVideo: !!videoDiv.querySelector('video')
            });
            
            editor.appendChild(videoDiv);
          });
        }
        
        console.log('VideoRichTextEditor - 插入完成，触发handleInput');
        this.handleInput({ target: this.$refs.editor });
        
        // 确保数据正确传递
        this.$nextTick(() => {
          console.log('VideoRichTextEditor - 最终编辑器内容:', this.currentContent);
          console.log('VideoRichTextEditor - 编辑器DOM:', this.$refs.editor.innerHTML);
          
          // 检查插入后的video元素状态
          const insertedVideos = this.$refs.editor.querySelectorAll('video');
          console.log('VideoRichTextEditor - 插入后的video元素数量:', insertedVideos.length);
          insertedVideos.forEach((video, i) => {
            console.log(`VideoRichTextEditor - 插入后video${i}:`, {
              tagName: video.tagName,
              controls: video.hasAttribute('controls'),
              src: video.src,
              currentSrc: video.currentSrc,
              style: video.style.cssText,
              computedStyle: {
                display: getComputedStyle(video).display,
                visibility: getComputedStyle(video).visibility,
                opacity: getComputedStyle(video).opacity
              },
              sources: Array.from(video.querySelectorAll('source')).map(s => ({
                src: s.src,
                type: s.type
              }))
            });
          });
        });
        
      } catch (error) {
        console.error('🚨 VideoRichTextEditor - insertVideos方法出错:', error);
        console.error('🚨 错误堆栈:', error.stack);
        // 显示错误信息给用户
        this.$message.error('插入视频失败: ' + error.message);
      }
    },

    updateWordCount() {
      const editor = this.$refs.editor;
      if (editor) {
        // 计算纯文本字数（不包括HTML标签）
        const text = editor.innerText || editor.textContent || '';
        this.wordCount = text.replace(/\s/g, '').length;
      }
    },

    insertText(text) {
      if (this.readonly) return;
      
      this.$refs.editor.focus();
      document.execCommand('insertText', false, text);
      this.handleInput({ target: this.$refs.editor });
    },

    insertHtml(html) {
      if (this.readonly) return;
      
      this.$refs.editor.focus();
      document.execCommand('insertHTML', false, html);
      this.handleInput({ target: this.$refs.editor });
    },

    clear() {
      this.currentContent = '';
      this.$refs.editor.innerHTML = '';
      this.updateWordCount();
      this.$emit('input', '');
    },

    getContent() {
      return this.currentContent;
    },

    setContent(content) {
      this.currentContent = content;
      this.$refs.editor.innerHTML = content;
      this.updateWordCount();
      this.$emit('input', content);
    }
  }
};
</script>

<style scoped>
.video-rich-text-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}

.editor-toolbar {
  padding: 10px 15px;
  border-bottom: 1px solid #dcdfe6;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.editor-content {
  padding: 15px;
  border: none;
  outline: none;
  overflow-y: auto;
  line-height: 1.6;
  font-size: 14px;
  color: #606266;
  background: #fff;
  position: relative;
}

.editor-content:empty::before {
  content: attr(placeholder);
  color: #c0c4cc;
  position: absolute;
  pointer-events: none;
}

.editor-content:focus {
  border-color: #409eff;
}

.editor-footer {
  padding: 8px 15px;
  border-top: 1px solid #dcdfe6;
  background: #f5f7fa;
  text-align: right;
}

.word-count {
  font-size: 12px;
  color: #909399;
}

/* 编辑器内部样式 */
.editor-content >>> .rich-text-video {
  margin: 15px 0;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #f9f9f9;
}

.editor-content >>> .rich-text-video video {
  max-width: 100%;
  width: 100%;
  height: auto;
  min-height: 200px;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  background: #000;
  border-radius: 4px;
}

.editor-content >>> .video-info {
  margin-top: 10px;
  padding: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.editor-content >>> p {
  margin: 10px 0;
}

.editor-content >>> ul, .editor-content >>> ol {
  margin: 10px 0;
  padding-left: 20px;
}

.editor-content >>> blockquote {
  margin: 15px 0;
  padding: 10px 15px;
  border-left: 4px solid #409eff;
  background: #f0f9ff;
  color: #666;
}

.editor-content >>> h1, .editor-content >>> h2, .editor-content >>> h3,
.editor-content >>> h4, .editor-content >>> h5, .editor-content >>> h6 {
  margin: 15px 0 10px 0;
  font-weight: bold;
}

.editor-content >>> h1 { font-size: 24px; }
.editor-content >>> h2 { font-size: 20px; }
.editor-content >>> h3 { font-size: 18px; }
.editor-content >>> h4 { font-size: 16px; }
.editor-content >>> h5 { font-size: 14px; }
.editor-content >>> h6 { font-size: 12px; }
</style> 