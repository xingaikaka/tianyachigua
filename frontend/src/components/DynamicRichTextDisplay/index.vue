<template>
  <div 
    class="dynamic-rich-text-display" 
    v-html="processedContent"
    v-loading="loading"
    element-loading-text="加载视频中..."
  ></div>
</template>

<script>
import { getVideoUrls } from "@/api/chigua/transcode";
import Hls from 'hls.js';
import { hlsXhrSetup, patchNativeHlsM3u8 } from '@/utils/hlsUtils';

export default {
  name: "DynamicRichTextDisplay",
  props: {
    content: {
      type: String,
      default: ''
    },
    // 是否立即加载视频URL（默认true，如果是编辑模式可设为false）
    autoLoadUrls: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      processedContent: '',
      loading: false,
      videoUrls: new Map(), // 缓存已加载的URL
      transcodeIds: [], // 当前内容中的转码ID列表
      hlsInstances: new Map(), // HLS实例管理
      videoObserver: null // DOM观察器
    }
  },
  watch: {
    content: {
      handler(newContent, oldContent) {
        console.log('DynamicRichTextDisplay - 内容变化:', {
          newContent: newContent ? newContent.substring(0, 200) + '...' : null,
          oldContent: oldContent ? oldContent.substring(0, 200) + '...' : null,
          hasNewVideos: newContent && newContent.includes('rich-text-video'),
          hasOldVideos: oldContent && oldContent.includes('rich-text-video')
        });
        this.processContent(newContent);
      },
      immediate: true
    }
  },
  mounted() {
    this.initVideoObserver();
  },
  beforeDestroy() {
    this.destroyAllHls();
    if (this.videoObserver) {
      this.videoObserver.disconnect();
    }
  },
  methods: {
    /**
     * 处理富文本内容
     */
    async processContent(content) {
      if (!content) {
        this.processedContent = '';
        return;
      }

      console.log('DynamicRichTextDisplay - 开始处理内容:', content.substring(0, 200) + '...');

      // 🔧 修复：检查内容是否已经包含完整的URL
      const hasCompleteUrls = content.includes('src="http') || content.includes('src="https');
      const hasPlaceholders = content.includes('data-src-placeholder') || content.includes('data-video-placeholder');
      
      if (hasCompleteUrls && !hasPlaceholders) {
        // 内容已经包含完整的URL，直接显示
        console.log('DynamicRichTextDisplay - 内容已包含完整URL，直接显示');
        this.processedContent = content;
        this.$nextTick(() => {
          this.initializeVideoPlayers();
        });
        return;
      }

      // 1. 先显示内容
      this.processedContent = content;

      if (!this.autoLoadUrls) {
        // 如果不自动加载URLs，直接初始化已有URL的视频播放器
        this.$nextTick(() => {
          this.initializeVideoPlayers();
        });
        return;
      }

      // 2. 检查是否有需要动态加载URL的占位符
      if (!hasPlaceholders) {
        // 3. 如果没有占位符，说明URL已经是最终的，直接初始化播放器
        console.log('DynamicRichTextDisplay - 内容中没有占位符，直接初始化视频播放器');
        this.$nextTick(() => {
          this.initializeVideoPlayers();
        });
        return;
      }

      // 4. 有占位符的情况，需要动态替换URL
      const transcodeIds = this.extractTranscodeIds(content);
      
      if (transcodeIds.length === 0) {
        this.$nextTick(() => {
          this.initializeVideoPlayers();
        });
        return;
      }

      // 5. 过滤出需要加载的转码ID（未缓存的）
      const needLoadIds = transcodeIds.filter(id => !this.videoUrls.has(id));
      
      if (needLoadIds.length === 0) {
        // 6. 如果都已缓存，直接替换URL
        this.replaceVideoUrls(content);
        return;
      }

      // 7. 加载新的URL
      await this.loadVideoUrls(needLoadIds);
      
      // 8. 替换所有URL
      this.replaceVideoUrls(content);
    },

    /**
     * 提取富文本内容中的所有视频ID
     */
    extractTranscodeIds(content) {
      const videoIds = [];
      
      // 支持多种ID字段格式
      const patterns = [
        /<div[^>]*class="[^"]*rich-text-video[^"]*"[^>]*data-video-id="([^"]+)"[^>]*>/g,
        /<div[^>]*class="[^"]*rich-text-video[^"]*"[^>]*data-transcode-id="([^"]+)"[^>]*>/g,
        /<video[^>]*data-video-id="([^"]+)"[^>]*>/g,
        /<video[^>]*data-transcode-id="([^"]+)"[^>]*>/g
      ];
      
      patterns.forEach(pattern => {
        let match;
        while ((match = pattern.exec(content)) !== null) {
          const videoId = match[1];
          if (videoId && !videoIds.includes(videoId)) {
            videoIds.push(videoId);
          }
        }
      });
      
      return videoIds;
    },

    /**
     * 批量加载视频URL
     */
    async loadVideoUrls(transcodeIds) {
      if (transcodeIds.length === 0) return;

      this.loading = true;
      try {
        const response = await getVideoUrls(transcodeIds);
        const urlsMap = response.data.urls; // 适配新的API响应格式
        
        // 缓存URL
        for (const [transcodeId, urls] of Object.entries(urlsMap)) {
          this.videoUrls.set(transcodeId, urls);
        }
        
      } catch (error) {
        console.error('加载视频URL失败:', error);
        this.$message.error('加载视频失败');
      } finally {
        this.loading = false;
      }
    },

    /**
     * 替换富文本内容中的视频URL
     */
    replaceVideoUrls(content) {
      console.log('开始替换视频URL');
      
      let processedContent = content;
      
      // 替换视频poster属性
              processedContent = processedContent.replace(
        /<video([^>]*data-video-placeholder="true"[^>]*)>/g,
        (match, attributes) => {
          // 提取视频ID（支持多种字段名）
          let videoId = null;
          const idMatches = [
            attributes.match(/data-video-id="([^"]+)"/),
            attributes.match(/data-transcode-id="([^"]+)"/)
          ];
          
          for (const idMatch of idMatches) {
            if (idMatch) {
              videoId = idMatch[1];
              break;
            }
          }
          
          if (videoId) {
            const urls = this.videoUrls.get(videoId);
            
            if (urls && urls.coverUrl) {
              // 添加poster属性
              const videoTag = match.replace('data-video-placeholder="true"', `poster="${urls.coverUrl}"`);
              return videoTag;
            }
          }
          // 移除placeholder属性
          return match.replace(' data-video-placeholder="true"', '');
        }
      );

      // 替换视频源地址 - 更新方法以处理两种source类型
      processedContent = processedContent.replace(
        /<source[^>]*data-src-placeholder="([^"]+)"[^>]*>/g,
        (match, videoId) => {
          const urls = this.videoUrls.get(videoId);
          if (!urls) {
            return match;
          }
          
          const type = match.match(/type="([^"]+)"/);
          const sourceType = type ? type[1] : '';
          
          let targetUrl = null;
          if (sourceType === 'application/x-mpegURL' && urls.m3u8Url) {
            targetUrl = urls.m3u8Url;
          } else if (sourceType === 'video/mp4' && urls.videoUrl) {
            targetUrl = urls.videoUrl;
          } else {
            // 如果类型不匹配，使用优先级：M3U8 > MP4
            targetUrl = urls.m3u8Url || urls.videoUrl;
          }
          
          if (targetUrl) {
            return match.replace('data-src-placeholder=', 'src=').replace(`"${videoId}"`, `"${targetUrl}"`);
          }
          
          return match;
        }
      );

      // 🔧 新增：修复缺少源的video标签
      processedContent = this.repairBrokenVideoTags(processedContent);

      this.processedContent = processedContent;
      console.log('视频URL替换完成');
      
      // DOM更新后初始化视频播放器
      this.$nextTick(() => {
        this.initializeVideoPlayers();
      });
    },

    /**
     * 修复缺少源的video标签
     */
    repairBrokenVideoTags(content) {
      console.log('🔧 开始修复缺少源的video标签');
      
      // 查找所有在rich-text-video容器中的video标签
      const videoContainerPattern = /<div[^>]*class="[^"]*rich-text-video[^"]*"[^>]*data-video-id="([^"]+)"[^>]*>(.*?)<\/div>/gs;
      
      return content.replace(videoContainerPattern, (match, videoId, containerContent) => {
        // 检查容器内的video标签是否缺少有效源
        const videoTagPattern = /<video([^>]*)>(.*?)<\/video>/s;
        const videoMatch = containerContent.match(videoTagPattern);
        
        if (!videoMatch) {
          console.log(`🔧 容器${videoId}中没有找到video标签`);
          return match; // 没有video标签，保持原样
        }
        
        const videoAttributes = videoMatch[1];
        const videoInnerHTML = videoMatch[2];
        
        // 检查是否有有效的src或source标签
        const hasSrc = videoAttributes.includes('src="') && !videoAttributes.includes('src=""');
        const hasValidSource = videoInnerHTML.includes('<source') && 
                              videoInnerHTML.includes('src="') && 
                              !videoInnerHTML.includes('src=""');
        
        // 🔧 新增：检查是否有data-resource-key（后端处理中）
        const hasDataResourceKey = videoAttributes.includes('data-resource-key="') || 
                                  videoInnerHTML.includes('data-resource-key="');
        
        if (hasSrc || hasValidSource) {
          console.log(`🔧 视频${videoId}已有有效源，跳过修复`);
          return match; // 已有有效源，保持原样
        }
        
        if (hasDataResourceKey) {
          console.log(`🔧 视频${videoId}有data-resource-key，等待后端处理，跳过前端修复`);
          return match; // 有data-resource-key，让后端处理
        }
        
        console.log(`🔧 发现缺少源的视频${videoId}，尝试修复`);
        
        // 尝试从缓存中获取URL
        const urls = this.videoUrls.get(videoId);
        if (!urls) {
          console.warn(`🔧 视频${videoId}没有缓存的URL数据，无法修复`);
          return match;
        }
        
        // 生成新的source标签
        let newSources = '';
        if (urls.m3u8Url) {
          newSources += `<source src="${urls.m3u8Url}" type="application/x-mpegURL">`;
          console.log(`🔧 为视频${videoId}添加M3U8源: ${urls.m3u8Url}`);
        }
        if (urls.videoUrl) {
          newSources += `<source src="${urls.videoUrl}" type="video/mp4">`;
          console.log(`🔧 为视频${videoId}添加MP4源: ${urls.videoUrl}`);
        }
        
        if (!newSources) {
          console.warn(`🔧 视频${videoId}没有可用的URL，无法修复`);
          return match;
        }
        
        // 添加poster属性（如果有）
        let newVideoAttributes = videoAttributes;
        if (urls.coverUrl && !videoAttributes.includes('poster=')) {
          newVideoAttributes += ` poster="${urls.coverUrl}"`;
          console.log(`🔧 为视频${videoId}添加封面: ${urls.coverUrl}`);
        }
        
        // 构建新的video标签
        const fallbackText = videoInnerHTML.includes('您的浏览器不支持视频播放') ? 
                            videoInnerHTML : 
                            videoInnerHTML + '\n您的浏览器不支持视频播放。';
        
        const newVideoTag = `<video${newVideoAttributes}>${newSources}${fallbackText}</video>`;
        const newContainer = match.replace(videoMatch[0], newVideoTag);
        
        console.log(`🔧 视频${videoId}修复完成`);
        return newContainer;
      });
    },

    /**
     * 手动刷新视频URL（用于签名过期时）
     */
    async refreshVideoUrls() {
      const transcodeIds = this.extractTranscodeIds(this.content);
      if (transcodeIds.length === 0) return;

      // 销毁所有HLS实例
      this.destroyAllHls();
      
      // 清除缓存
      transcodeIds.forEach(id => this.videoUrls.delete(id));
      
      // 重新加载
      await this.processContent(this.content);
      
      this.$message.success('视频URL已刷新');
    },

    /**
     * 获取当前缓存的URL信息
     */
    getCachedUrls() {
      return Object.fromEntries(this.videoUrls);
    },

    /**
     * 清除URL缓存
     */
    clearCache() {
      this.videoUrls.clear();
    },

    /**
     * 初始化DOM观察器
     */
    initVideoObserver() {
      if (typeof MutationObserver !== 'undefined') {
        this.videoObserver = new MutationObserver((mutations) => {
          mutations.forEach((mutation) => {
            if (mutation.type === 'childList') {
              // 检查是否有新的video元素添加
              mutation.addedNodes.forEach((node) => {
                if (node.nodeType === 1) { // Element node
                  const videos = node.querySelectorAll ? node.querySelectorAll('video') : [];
                  if (videos.length > 0) {
                    this.$nextTick(() => {
                      this.initializeVideoPlayers();
                    });
                  }
                }
              });
            }
          });
        });
        
        this.videoObserver.observe(this.$el, {
          childList: true,
          subtree: true
        });
      }
    },

    /**
     * 初始化所有视频播放器
     */
    initializeVideoPlayers() {
      if (!this.$el) {
        console.log('$el不存在，跳过视频播放器初始化');
        return;
      }
      
      console.log('开始初始化视频播放器');
      
      // 查找所有包含视频的富文本容器
      const videoContainers = this.$el.querySelectorAll('.rich-text-video');
      console.log('找到视频容器数量：', videoContainers.length);
      
      // 查找所有video标签
      const allVideos = this.$el.querySelectorAll('video');
      console.log('找到video标签数量：', allVideos.length);
      
      // 详细打印每个视频容器的信息
      videoContainers.forEach((container, index) => {
        console.log(`视频容器${index}:`, {
          html: container.outerHTML,
          videoId: container.getAttribute('data-video-id'),
          transcodeId: container.getAttribute('data-transcode-id'),
          hasVideo: !!container.querySelector('video')
        });
        
        const video = container.querySelector('video');
        if (video) {
          console.log(`视频元素${index}:`, {
            src: video.src,
            sources: Array.from(video.querySelectorAll('source')).map(s => ({
              src: s.src,
              type: s.type
            })),
            controls: video.controls,
            width: video.width,
            style: video.style.cssText
          });
        }
      });
      
      // 如果没有找到rich-text-video容器，直接处理所有video标签
      if (videoContainers.length === 0 && allVideos.length > 0) {
        console.log('没有找到rich-text-video容器，直接处理所有video标签');
        allVideos.forEach((video, index) => {
          console.log(`直接处理视频${index}:`, {
            src: video.src,
            sources: Array.from(video.querySelectorAll('source')).map(s => ({
              src: s.src,
              type: s.type
            })),
            controls: video.controls,
            width: video.width,
            style: video.style.cssText
          });
        });
      }
      
      videoContainers.forEach((container, index) => {
        const videoId = container.getAttribute('data-video-id') || container.getAttribute('data-transcode-id');
        
        const video = container.querySelector('video');
        if (!video) {
          return;
        }
        
        // 为每个video元素生成唯一标识符
        const uniqueId = `${videoId || 'unknown'}_${index}`;
        video.setAttribute('data-unique-id', uniqueId);
        
        // 清理旧的HLS实例
        if (this.hlsInstances.has(uniqueId)) {
          this.hlsInstances.get(uniqueId).destroy();
          this.hlsInstances.delete(uniqueId);
        }
        
        // 检查是否有占位符，如果有则从缓存获取URL
        const hasPlaceholders = video.querySelector('source[data-src-placeholder]') || video.hasAttribute('data-video-placeholder');
        
        if (hasPlaceholders && videoId) {
          console.log(`视频${index} - 占位符模式，从缓存获取URL`);
          const urls = this.videoUrls.get(videoId);
          
          if (!urls) {
            return;
          }
          
          // 检查是否有M3U8 URL
          const m3u8Url = urls.m3u8Url || urls.preferredPlayUrl;
          
          if (m3u8Url && m3u8Url.includes('.m3u8')) {
            this.initializeHlsPlayer(video, m3u8Url, uniqueId);
          } else if (urls.videoUrl) {
            // 更新source标签
            const placeholderSources = video.querySelectorAll('source[data-src-placeholder]');
            placeholderSources.forEach(source => {
              const type = source.getAttribute('type');
              if (type === 'video/mp4' && urls.videoUrl) {
                source.src = urls.videoUrl;
                source.removeAttribute('data-src-placeholder');
              } else if (type === 'application/x-mpegURL' && urls.m3u8Url) {
                source.src = urls.m3u8Url;
                source.removeAttribute('data-src-placeholder');
              }
            });
            video.load();
          }
        } else {
          // 直接模式：URL已经在HTML中
          console.log(`视频${index} - 直接模式，检查已有的URL`);
          
          // 查找M3U8格式的source
          const m3u8Source = video.querySelector('source[type="application/x-mpegURL"]');
          const mp4Source = video.querySelector('source[type="video/mp4"]');
          
          console.log(`视频${index} - 发现source标签:`, {
            m3u8Src: m3u8Source ? m3u8Source.src : null,
            mp4Src: mp4Source ? mp4Source.src : null,
            hasM3u8: !!(m3u8Source && m3u8Source.src),
            hasMp4: !!(mp4Source && mp4Source.src)
          });
          
          if (m3u8Source && m3u8Source.src) {
            console.log(`视频${index} - 初始化HLS播放器（M3U8）:`, m3u8Source.src);
            this.initializeHlsPlayerDirect(video, m3u8Source.src, uniqueId);
          } else if (mp4Source && mp4Source.src) {
            console.log(`视频${index} - 直接播放MP4:`, mp4Source.src);
            video.load();
          } else {
            console.log(`视频${index} - 没有可用的视频源`);
          }
        }
      });
      
      console.log('视频播放器初始化完成');
    },

    /**
     * 初始化HLS播放器
     */
    initializeHlsPlayer(video, m3u8Url, uniqueId) {
      if (Hls.isSupported()) {
        console.log(`Initializing HLS player for ${uniqueId}:`, m3u8Url);
        
        const hls = new Hls({
          debug: false,
          enableWorker: false,
          autoStartLoad: true,
          startLevel: -1,
          defaultAudioCodec: undefined,
          defaultVideoCodec: undefined,
          maxBufferLength: 30,
          maxMaxBufferLength: 600,
          xhrSetup: hlsXhrSetup,
        });
        
        hls.loadSource(m3u8Url);
        hls.attachMedia(video);
        
        // 更新source标签
        const sources = video.querySelectorAll('source[data-src-placeholder]');
        sources.forEach(source => {
          const type = source.getAttribute('type');
          if (type === 'application/x-mpegURL') {
            source.src = m3u8Url;
            source.removeAttribute('data-src-placeholder');
          }
        });
        
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          console.log(`HLS manifest parsed for ${uniqueId}`);
        });
        
        hls.on(Hls.Events.ERROR, (event, data) => {
          console.error(`HLS error for ${uniqueId}:`, data);
          if (data.fatal) {
            switch (data.type) {
              case Hls.ErrorTypes.NETWORK_ERROR:
                console.log(`HLS network error for ${uniqueId}, trying to recover...`);
                hls.startLoad();
                break;
              case Hls.ErrorTypes.MEDIA_ERROR:
                console.log(`HLS media error for ${uniqueId}, trying to recover...`);
                hls.recoverMediaError();
                break;
              default:
                console.error(`HLS fatal error for ${uniqueId}, destroying player`);
                hls.destroy();
                this.hlsInstances.delete(uniqueId);
                this.showVideoError(video, uniqueId, 'HLS播放失败');
                break;
            }
          }
        });
        
        this.hlsInstances.set(uniqueId, hls);
        
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // iOS Safari 原生 HLS：重写 key URI 为绝对后端地址
        console.log(`Using native HLS support for ${uniqueId}`);
        const prevBlobUrl = video._nativeBlobUrl;
        patchNativeHlsM3u8(m3u8Url).then(({ url: patchedUrl, isBlob }) => {
          if (prevBlobUrl) { try { URL.revokeObjectURL(prevBlobUrl); } catch (_) {} }
          video._nativeBlobUrl = isBlob ? patchedUrl : null;
          const hlsSource = video.querySelector('source[type="application/x-mpegURL"]');
          if (hlsSource) {
            hlsSource.src = patchedUrl;
            hlsSource.removeAttribute('data-src-placeholder');
          }
          video.load();
        });
      } else {
        console.warn(`HLS not supported for ${uniqueId}, showing error`);
        this.showVideoError(video, uniqueId, 'HLS不支持');
      }
    },

    /**
     * 初始化HLS播放器（直接模式）
     */
    initializeHlsPlayerDirect(video, m3u8Url, uniqueId) {
      console.log(`DynamicRichTextDisplay - 开始初始化HLS播放器 ${uniqueId}:`, m3u8Url);
      
      video.pause();
      video.currentTime = 0;
      
      if (Hls.isSupported()) {
        console.log(`HLS supported - 创建HLS实例 ${uniqueId}`);
        
        const hls = new Hls({
          debug: false,
          enableWorker: false,
          autoStartLoad: true,
          startLevel: -1,
          defaultAudioCodec: undefined,
          defaultVideoCodec: undefined,
          maxBufferLength: 30,
          maxMaxBufferLength: 600,
          xhrSetup: hlsXhrSetup,
        });
        
        const sources = video.querySelectorAll('source');
        sources.forEach(source => {
          if (source.type !== 'application/x-mpegURL') {
            source.remove();
          }
        });
        
        hls.loadSource(m3u8Url);
        hls.attachMedia(video);
        
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          console.log(`HLS manifest parsed for ${uniqueId} - 可以播放`);
        });
        
        hls.on(Hls.Events.ERROR, (event, data) => {
          console.error(`HLS error for ${uniqueId}:`, data);
          if (data.fatal) {
            switch (data.type) {
              case Hls.ErrorTypes.NETWORK_ERROR:
                console.log(`HLS network error for ${uniqueId}, trying to recover...`);
                hls.startLoad();
                break;
              case Hls.ErrorTypes.MEDIA_ERROR:
                console.log(`HLS media error for ${uniqueId}, trying to recover...`);
                hls.recoverMediaError();
                break;
              default:
                console.error(`HLS fatal error for ${uniqueId}, destroying player`);
                hls.destroy();
                this.hlsInstances.delete(uniqueId);
                this.showVideoError(video, uniqueId, 'HLS播放失败');
                break;
            }
          }
        });
        
        this.hlsInstances.set(uniqueId, hls);
        
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // iOS Safari 原生 HLS：重写 key URI 为绝对后端地址
        console.log(`Using native HLS support for ${uniqueId}`);
        const prevBlobUrl = video._nativeBlobUrl;
        patchNativeHlsM3u8(m3u8Url).then(({ url: patchedUrl, isBlob }) => {
          if (prevBlobUrl) { try { URL.revokeObjectURL(prevBlobUrl); } catch (_) {} }
          video._nativeBlobUrl = isBlob ? patchedUrl : null;
          const sources = video.querySelectorAll('source');
          sources.forEach(source => {
            if (source.type !== 'application/x-mpegURL') {
              source.remove();
            }
          });
          video.src = patchedUrl;
          video.load();
        });
      } else {
        console.warn(`HLS not supported for ${uniqueId}, showing error`);
        this.showVideoError(video, uniqueId, 'HLS不支持');
      }
    },

    /**
     * 显示视频播放错误
     */
    showVideoError(video, uniqueId, errorMessage) {
      console.log(`Showing video error for ${uniqueId}: ${errorMessage}`);
      // 清空视频内容，显示错误信息
      video.innerHTML = `视频播放失败：${errorMessage}`;
      // 可选：添加样式来美化错误显示
      video.style.display = 'flex';
      video.style.alignItems = 'center';
      video.style.justifyContent = 'center';
      video.style.backgroundColor = '#f5f5f5';
      video.style.color = '#666';
      video.style.fontSize = '14px';
      video.style.border = '1px solid #ddd';
      video.style.borderRadius = '4px';
      video.style.minHeight = '100px';
    },



    /**
     * 销毁所有HLS实例
     */
    destroyAllHls() {
      console.log('Destroying all HLS instances');
      this.hlsInstances.forEach((hls, id) => {
        try {
          hls.destroy();
          console.log(`Destroyed HLS instance: ${id}`);
        } catch (error) {
          console.error(`Error destroying HLS instance ${id}:`, error);
        }
      });
      this.hlsInstances.clear();
    }
  }
};
</script>

<style scoped>
.dynamic-rich-text-display {
  line-height: 1.6;
  color: #333;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  overflow-x: hidden;
}

/* 富文本内容对齐 - 与图片视频保持一致 */
.dynamic-rich-text-display >>> p,
.dynamic-rich-text-display >>> div,
.dynamic-rich-text-display >>> h1,
.dynamic-rich-text-display >>> h2,
.dynamic-rich-text-display >>> h3,
.dynamic-rich-text-display >>> h4,
.dynamic-rich-text-display >>> h5,
.dynamic-rich-text-display >>> h6,
.dynamic-rich-text-display >>> ul,
.dynamic-rich-text-display >>> ol,
.dynamic-rich-text-display >>> blockquote {
  max-width: 800px !important;
  margin-left: auto !important;
  margin-right: auto !important;
  word-wrap: break-word !important;
  text-align: left !important;
}

/* 段落间距优化 */
.dynamic-rich-text-display >>> p {
  margin: 12px auto !important;
  line-height: 1.6 !important;
}

/* 标题样式优化 */
.dynamic-rich-text-display >>> h1,
.dynamic-rich-text-display >>> h2,
.dynamic-rich-text-display >>> h3,
.dynamic-rich-text-display >>> h4,
.dynamic-rich-text-display >>> h5,
.dynamic-rich-text-display >>> h6 {
  margin: 20px auto 12px auto !important;
  line-height: 1.4 !important;
  font-weight: 600 !important;
}

/* 列表样式优化 */
.dynamic-rich-text-display >>> ul,
.dynamic-rich-text-display >>> ol {
  margin: 12px auto !important;
  padding-left: 20px !important;
}

/* 引用样式优化 */
.dynamic-rich-text-display >>> blockquote {
  margin: 20px auto !important;
  padding: 15px 20px !important;
  border-left: 4px solid #409eff !important;
  background: #f0f9ff !important;
  font-style: italic !important;
}

/* 富文本图片样式 - 与编辑器保持一致 */
.dynamic-rich-text-display >>> img {
  max-width: 100% !important;
  width: auto !important;
  height: auto !important;
  display: block !important;
  margin: 20px auto !important;
  border-radius: 8px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease !important;
}

.dynamic-rich-text-display >>> img:hover {
  transform: scale(1.02) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
  cursor: pointer !important;
}

.dynamic-rich-text-display >>> p img,
.dynamic-rich-text-display >>> div img {
  max-width: 800px !important;
  margin: 20px auto !important;
  display: block !important;
}

/* 富文本视频容器样式 - 与编辑器保持一致 */
.dynamic-rich-text-display >>> .rich-text-video {
  margin: 20px auto !important;
  padding: 15px !important;
  max-width: 800px !important;
  border: 1px solid #e4e7ed !important;
  border-radius: 12px !important;
  background: linear-gradient(145deg, #f9f9f9, #ffffff) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  transition: box-shadow 0.3s ease !important;
}

.dynamic-rich-text-display >>> .rich-text-video:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
}

/* 视频播放器样式 - 统一尺寸和外观 */
.dynamic-rich-text-display >>> .rich-text-video video,
.dynamic-rich-text-display >>> video {
  max-width: 100% !important;
  width: 100% !important;
  height: auto !important;
  aspect-ratio: 16/9 !important;
  min-height: 300px !important;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  background: #000 !important;
  border-radius: 8px !important;
  margin: 0 0 12px 0 !important;
  position: relative !important;
  z-index: 1 !important;
  outline: none !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2) !important;
}

/* 直接插入的video标签样式 */
.dynamic-rich-text-display >>> video {
  margin: 20px auto !important;
  max-width: 800px !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}

/* iframe视频样式 - 统一外观和尺寸 */
.dynamic-rich-text-display >>> iframe {
  max-width: 800px !important;
  width: 100% !important;
  aspect-ratio: 16/9 !important;
  min-height: 300px !important;
  margin: 20px auto !important;
  display: block !important;
  border: none !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  background: #000 !important;
  outline: none !important;
}

.dynamic-rich-text-display >>> .rich-text-video video:error {
  border: 2px dashed #f56c6c;
  background: #fef0f0;
}

.dynamic-rich-text-display >>> .video-error {
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 8px;
  padding: 15px;
  margin: 20px auto;
  max-width: 800px;
  color: #f56c6c;
  text-align: center;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.1);
}

/* 视频信息样式优化 - 与编辑器保持一致 */
.dynamic-rich-text-display >>> .video-info {
  margin-top: 12px !important;
  padding: 12px 16px !important;
  background: linear-gradient(145deg, #f5f7fa, #e8ecf0) !important;
  border-radius: 8px !important;
  border: 1px solid #e1e4e8 !important;
  font-size: 13px !important;
  color: #586069 !important;
  line-height: 1.5 !important;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05) !important;
}

.dynamic-rich-text-display >>> .video-info p {
  margin: 4px 0 !important;
  line-height: 1.6 !important;
}

.dynamic-rich-text-display >>> .video-info strong {
  font-weight: 600 !important;
  color: #24292e !important;
}

/* 其他富文本样式 */
.dynamic-rich-text-display >>> p {
  margin: 10px 0;
}

.dynamic-rich-text-display >>> ul, 
.dynamic-rich-text-display >>> ol {
  margin: 10px 0;
  padding-left: 20px;
}

.dynamic-rich-text-display >>> blockquote {
  margin: 15px 0;
  padding: 10px 15px;
  border-left: 4px solid #409eff;
  background: #f0f9ff;
  color: #666;
}

.dynamic-rich-text-display >>> h1, 
.dynamic-rich-text-display >>> h2, 
.dynamic-rich-text-display >>> h3,
.dynamic-rich-text-display >>> h4, 
.dynamic-rich-text-display >>> h5, 
.dynamic-rich-text-display >>> h6 {
  margin: 15px 0 10px 0;
  font-weight: bold;
}

.dynamic-rich-text-display >>> h1 { font-size: 24px; }
.dynamic-rich-text-display >>> h2 { font-size: 20px; }
.dynamic-rich-text-display >>> h3 { font-size: 18px; }
.dynamic-rich-text-display >>> h4 { font-size: 16px; }
.dynamic-rich-text-display >>> h5 { font-size: 14px; }
.dynamic-rich-text-display >>> h6 { font-size: 12px; }

/* 移动端响应式样式 - 与编辑器保持一致 */
@media (max-width: 768px) {
  .dynamic-rich-text-display {
    max-width: 100% !important;
    padding: 15px !important;
  }
  
  .dynamic-rich-text-display >>> p,
  .dynamic-rich-text-display >>> div,
  .dynamic-rich-text-display >>> h1,
  .dynamic-rich-text-display >>> h2,
  .dynamic-rich-text-display >>> h3,
  .dynamic-rich-text-display >>> h4,
  .dynamic-rich-text-display >>> h5,
  .dynamic-rich-text-display >>> h6,
  .dynamic-rich-text-display >>> ul,
  .dynamic-rich-text-display >>> ol,
  .dynamic-rich-text-display >>> blockquote {
    max-width: 100% !important;
    padding: 0 10px !important;
  }
  
  .dynamic-rich-text-display >>> img {
    max-width: 100% !important;
    margin: 15px auto !important;
    border-radius: 6px !important;
  }
  
  .dynamic-rich-text-display >>> iframe,
  .dynamic-rich-text-display >>> video,
  .dynamic-rich-text-display >>> .rich-text-video video {
    min-height: 220px !important;
    border-radius: 8px !important;
    margin: 15px auto !important;
  }
  
  .dynamic-rich-text-display >>> .rich-text-video {
    margin: 15px auto !important;
    padding: 12px !important;
    border-radius: 8px !important;
  }
  
  .dynamic-rich-text-display >>> .video-error {
    margin: 15px auto !important;
    padding: 12px !important;
    border-radius: 6px !important;
    font-size: 13px !important;
  }
}

/* 小屏设备进一步优化 */
@media (max-width: 480px) {
  .dynamic-rich-text-display {
    padding: 12px !important;
  }
  
  .dynamic-rich-text-display >>> p,
  .dynamic-rich-text-display >>> div,
  .dynamic-rich-text-display >>> h1,
  .dynamic-rich-text-display >>> h2,
  .dynamic-rich-text-display >>> h3,
  .dynamic-rich-text-display >>> h4,
  .dynamic-rich-text-display >>> h5,
  .dynamic-rich-text-display >>> h6,
  .dynamic-rich-text-display >>> ul,
  .dynamic-rich-text-display >>> ol,
  .dynamic-rich-text-display >>> blockquote {
    padding: 0 8px !important;
  }
  
  .dynamic-rich-text-display >>> img {
    margin: 12px auto !important;
    border-radius: 4px !important;
  }
  
  .dynamic-rich-text-display >>> iframe,
  .dynamic-rich-text-display >>> video,
  .dynamic-rich-text-display >>> .rich-text-video video {
    min-height: 180px !important;
    margin: 12px auto !important;
    border-radius: 6px !important;
  }
  
  .dynamic-rich-text-display >>> .rich-text-video {
    margin: 12px auto !important;
    padding: 10px !important;
    border-radius: 6px !important;
  }
  
  .dynamic-rich-text-display >>> .video-error {
    margin: 12px auto !important;
    padding: 10px !important;
    font-size: 12px !important;
  }
  
  .dynamic-rich-text-display >>> .video-info {
    padding: 10px 12px !important;
    font-size: 12px !important;
  }
}
</style> 