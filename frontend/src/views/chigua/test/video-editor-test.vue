<template>
  <div class="video-editor-test">
    <el-card>
      <div slot="header">
        <span>富文本编辑器视频插入功能测试</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="clearEditor">清空</el-button>
      </div>
      
      <div class="test-controls">
        <el-button type="primary" @click="insertTestVideo">插入测试视频</el-button>
        <el-button type="success" @click="insertMultipleVideos">插入多个视频</el-button>
        <el-button type="info" @click="showEditorContent">显示编辑器内容</el-button>
      </div>
      
      <div class="editor-container">
        <Editor
          ref="editor"
          v-model="content"
          :min-height="400"
          :file-size="10"
        />
      </div>
      
      <div class="content-display" v-if="showContent">
        <h3>编辑器HTML内容：</h3>
        <pre>{{ content }}</pre>
      </div>
    </el-card>
  </div>
</template>

<script>
import Editor from '@/components/Editor'

export default {
  name: "VideoEditorTest",
  components: {
    Editor
  },
  data() {
    return {
      content: '',
      showContent: false
    }
  },
  methods: {
    // 插入单个测试视频
    insertTestVideo() {
      const testVideoHtml = `
<div class="rich-text-video" data-video-id="test-1" data-transcode-id="test-transcode-1" data-orgfile="test-video.mp4" data-resolution="1920x1080" data-duration="300">
  <video controls width="100%" style="max-width: 600px;">
    <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>test-video.mp4</p>
    <p><strong>分辨率：</strong>1920x1080</p>
    <p><strong>时长：</strong>05:00</p>
    <p><strong>格式：</strong>MP4</p>
  </div>
</div>
      `.trim();
      
      if (this.$refs.editor) {
        this.$refs.editor.handleInsertVideos([testVideoHtml]);
      }
    },
    
    // 插入多个测试视频
    insertMultipleVideos() {
      const videos = [
        `
<div class="rich-text-video" data-video-id="test-2" data-transcode-id="test-transcode-2" data-orgfile="video-1.mp4" data-resolution="1280x720" data-duration="180">
  <video controls width="100%" style="max-width: 600px;">
    <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>video-1.mp4</p>
    <p><strong>分辨率：</strong>1280x720</p>
    <p><strong>时长：</strong>03:00</p>
    <p><strong>格式：</strong>MP4</p>
  </div>
</div>
        `.trim(),
        `
<div class="rich-text-video" data-video-id="test-3" data-transcode-id="test-transcode-3" data-orgfile="video-2.mp4" data-resolution="1920x1080" data-duration="240">
  <video controls width="100%" style="max-width: 600px;">
    <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>video-2.mp4</p>
    <p><strong>分辨率：</strong>1920x1080</p>
    <p><strong>时长：</strong>04:00</p>
    <p><strong>格式：</strong>MP4</p>
  </div>
</div>
        `.trim()
      ];
      
      if (this.$refs.editor) {
        this.$refs.editor.handleInsertVideos(videos);
      }
    },
    
    // 清空编辑器
    clearEditor() {
      this.content = '';
      this.showContent = false;
    },
    
    // 显示编辑器内容
    showEditorContent() {
      this.showContent = !this.showContent;
    }
  }
}
</script>

<style scoped>
.video-editor-test {
  padding: 20px;
}

.test-controls {
  margin-bottom: 20px;
}

.test-controls .el-button {
  margin-right: 10px;
}

.editor-container {
  margin-bottom: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.content-display {
  margin-top: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 4px;
}

.content-display pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 300px;
  overflow-y: auto;
}
</style> 