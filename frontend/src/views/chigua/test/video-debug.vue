<template>
  <div class="video-debug-page">
    <h2>视频功能调试页面</h2>
    
    <!-- 测试内容 -->
    <div class="test-section">
      <h3>测试用HTML内容</h3>
      <el-button @click="insertTestVideo">插入测试视频HTML</el-button>
      <el-button @click="loadUrls">手动加载URLs</el-button>
      <el-button @click="clearTest">清空</el-button>
    </div>
    
    <!-- 显示区域 -->
    <div class="display-section">
      <h3>渲染结果</h3>
      <DynamicRichTextDisplay 
        ref="display"
        :content="testContent"
        :auto-load-urls="true"
      />
    </div>
    
    <!-- 原始HTML -->
    <div class="raw-section">
      <h3>原始HTML</h3>
      <textarea v-model="testContent" rows="10" style="width: 100%;"></textarea>
    </div>

    <div class="video-debug">
      <h3>视频HTML调试测试</h3>
      
      <el-card header="直接HTML测试">
        <div class="direct-html-test" v-html="testVideoHtml"></div>
      </el-card>
      
      <el-card header="DynamicRichTextDisplay组件测试" style="margin-top: 20px;">
        <DynamicRichTextDisplay :content="testVideoHtml" />
      </el-card>
      
      <el-card header="生成测试HTML" style="margin-top: 20px;">
        <el-button @click="generateTestHtml">生成新的测试HTML</el-button>
        <el-button @click="generateRealUrlTest" type="primary" style="margin-left: 10px;">生成真实URL测试</el-button>
        <pre style="margin-top: 10px; background: #f5f5f5; padding: 10px;">{{ testVideoHtml }}</pre>
      </el-card>
      
      <el-card header="简单Video标签测试" style="margin-top: 20px;">
        <h4>原生video标签测试：</h4>
        <video controls width="400" style="background: #000;">
          <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4">
          您的浏览器不支持视频播放。
        </video>
        
        <h4 style="margin-top: 20px;">富文本video样式测试：</h4>
        <div class="rich-text-video" style="margin: 15px 0; padding: 10px; border: 1px solid #e4e7ed; border-radius: 4px; background: #f9f9f9;">
          <video controls width="100%" style="max-width: 600px; display: block; background: #000;">
            <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4">
            您的浏览器不支持视频播放。
          </video>
          <div class="video-info">
            <p><strong>这是测试视频</strong></p>
          </div>
        </div>
      </el-card>
      
      <el-card header="编辑器测试" style="margin-top: 20px;">
        <VideoRichTextEditor
          ref="editor"
          v-model="editorContent"
          placeholder="测试富文本编辑器中的视频显示..."
          :min-height="200"
        />
        <el-button @click="insertTestVideo" style="margin-top: 10px;">插入测试视频</el-button>
      </el-card>
      
      <el-card header="原始HTML测试 - 验证HTML结构" style="margin-top: 20px;">
        <h4>生成的HTML结构测试：</h4>
        <div class="raw-html-test" v-html="rawVideoHtml"></div>
        
        <h4 style="margin-top: 20px;">当前生成的HTML代码：</h4>
        <el-button @click="generateRawHtml" type="success">重新生成测试HTML</el-button>
        <pre style="background: #f5f5f5; padding: 10px; margin-top: 10px; white-space: pre-wrap;">{{ rawVideoHtml }}</pre>
      </el-card>
    </div>
  </div>
</template>

<script>
  import DynamicRichTextDisplay from '@/components/DynamicRichTextDisplay'
  import VideoRichTextEditor from '@/components/VideoRichTextEditor'

export default {
  name: "VideoDebug",
      components: {
      DynamicRichTextDisplay,
      VideoRichTextEditor
    },
  data() {
          return {
                testContent: '',
        editorContent: '',
        testVideoHtml: `
<div class="rich-text-video" data-video-id="19" data-transcode-id="test-id" data-orgfile="test.mp4" data-resolution="1920x1080" data-duration="300">
  <video controls width="100%" style="max-width: 600px;" data-video-placeholder="true">
    <source data-src-placeholder="19" type="application/x-mpegURL">
    <source data-src-placeholder="19" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>这是广告.mp4</p>
    <p><strong>分辨率：</strong>1920x1080</p>
    <p><strong>时长：</strong>05:00</p>
    <p><strong>质量：</strong>HD</p>
  </div>
</div>
        `.trim(),
        rawVideoHtml: `
<div class="rich-text-video" data-video-id="19" data-transcode-id="test-id" data-orgfile="test.mp4" data-resolution="1920x1080" data-duration="300">
  <video controls width="100%" style="max-width: 600px;">
    <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>测试视频.mp4</p>
    <p><strong>分辨率：</strong>1920x1080</p>
    <p><strong>时长：</strong>05:00</p>
    <p><strong>质量：</strong>全高清</p>
    <p><strong>格式：</strong>MP4</p>
  </div>
</div>
        `.trim()
    }
  },
  methods: {
    insertTestVideo() {
      // 模拟后端生成的HTML结构
      this.testContent = `
<div class="rich-text-video" data-video-id="19" data-transcode-id="test-id" data-orgfile="test.mp4" data-resolution="1920x1080" data-duration="300">
  <video controls width="100%" style="max-width: 600px;" data-video-placeholder="true">
    <source data-src-placeholder="19" type="application/x-mpegURL">
    <source data-src-placeholder="19" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>这是广告.mp4</p>
    <p><strong>分辨率：</strong>1920x1080</p>
    <p><strong>时长：</strong>05:00</p>
    <p><strong>质量：</strong>HD</p>
  </div>
</div>
      `.trim();
    },
    
    loadUrls() {
      if (this.$refs.display) {
        this.$refs.display.refreshVideoUrls();
      }
    },
    
    clearTest() {
      this.testContent = '';
    },

    generateTestHtml() {
      const videoId = Math.floor(Math.random() * 100) + 1; // 生成一个随机视频ID
      const transcodeId = `test-id-${videoId}`;
      const orgFile = `test-${videoId}.mp4`;
      const resolution = "1920x1080";
      const duration = "05:00";

      this.testVideoHtml = `
<div class="rich-text-video" data-video-id="${videoId}" data-transcode-id="${transcodeId}" data-orgfile="${orgFile}" data-resolution="${resolution}" data-duration="${duration}">
  <video controls width="100%" style="max-width: 600px;" data-video-placeholder="true">
    <source data-src-placeholder="${videoId}" type="application/x-mpegURL">
    <source data-src-placeholder="${videoId}" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>这是广告.mp4</p>
    <p><strong>分辨率：</strong>${resolution}</p>
    <p><strong>时长：</strong>${duration}</p>
    <p><strong>质量：</strong>HD</p>
  </div>
</div>
      `.trim();
    },

         generateRealUrlTest() {
       const videoId = Math.floor(Math.random() * 100) + 1; // 生成一个随机视频ID
       const transcodeId = `test-id-${videoId}`;
       const orgFile = `test-${videoId}.mp4`;
       const resolution = "1920x1080";
       const duration = "300";

       // 模拟真实的签名URL，类似从RichTextVideoSelector插入的HTML
       const m3u8Url = `https://khjghjghjjh.xyz/20250629/byMe35wK/index.m3u8?key=/20250629/byMe35wK/index.m3u8&expires=${Date.now() + 3600000}&downloads=&signature=test123`;
       
       this.testVideoHtml = `
<div class="rich-text-video" data-video-id="${videoId}" data-transcode-id="${transcodeId}" data-orgfile="${orgFile}" data-resolution="${resolution}" data-duration="${duration}">
  <video controls width="100%" style="max-width: 600px;">
    <source src="${m3u8Url}" type="application/x-mpegURL">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>${orgFile}</p>
    <p><strong>分辨率：</strong>${resolution}</p>
    <p><strong>时长：</strong>05:00</p>
    <p><strong>质量：</strong>全高清</p>
    <p><strong>格式：</strong>HLS (M3U8)</p>
  </div>
  </div>
        `.trim();
      },

             generateRawHtml() {
         const videoId = Math.floor(Math.random() * 100) + 1; // 生成一个随机视频ID
         const transcodeId = `test-id-${videoId}`;
         const orgFile = `test-${videoId}.mp4`;
         const resolution = "1920x1080";
         const duration = "300";

         this.rawVideoHtml = `
<div class="rich-text-video" data-video-id="${videoId}" data-transcode-id="${transcodeId}" data-orgfile="${orgFile}" data-resolution="${resolution}" data-duration="${duration}">
  <video controls width="100%" style="max-width: 600px;">
    <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>${orgFile}</p>
    <p><strong>分辨率：</strong>${resolution}</p>
    <p><strong>时长：</strong>05:00</p>
    <p><strong>质量：</strong>全高清</p>
    <p><strong>格式：</strong>MP4</p>
  </div>
</div>
         `.trim();
      },

      insertTestVideo() {
        const testVideoHtml = `
<div class="rich-text-video" data-video-id="999" data-transcode-id="test-999" data-orgfile="test-editor.mp4" data-resolution="1920x1080" data-duration="300">
  <video controls width="100%" style="max-width: 600px;">
    <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4">
    您的浏览器不支持视频播放。
  </video>
  <div class="video-info">
    <p><strong>视频：</strong>test-editor.mp4</p>
    <p><strong>分辨率：</strong>1920x1080</p>
    <p><strong>时长：</strong>05:00</p>
    <p><strong>质量：</strong>全高清</p>
    <p><strong>格式：</strong>MP4</p>
  </div>
</div>
        `.trim();
        
        // 模拟插入视频
        this.$refs.editor?.insertVideos([testVideoHtml]);
      }
    }
  }
</script>

<style scoped>
.video-debug-page {
  padding: 20px;
}

.test-section, .display-section, .raw-section {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

h2, h3 {
  margin-bottom: 15px;
}
</style> 