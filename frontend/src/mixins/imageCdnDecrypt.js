/**
 * CDN 图片解密 Mixin
 * 在模板中用 cdnSrc(url) 替代原始 CDN 签名 URL，
 * 首次调用触发异步解密，解密完成后响应式更新渲染。
 * 命中缓存时同步返回，无闪烁。
 */
import { getDecryptedBlobUrl, getCachedBlobUrl } from '@/utils/imageDecryption'

export default {
  data() {
    return {
      cdnBlobMap: {}
    }
  },
  methods: {
    /**
     * 返回解密后的 Blob URL（或原始 URL）。
     * 若图片正在解密中，返回空字符串（让 el-image 显示 placeholder 插槽）。
     * @param {string} url CDN 签名 URL
     * @returns {string}
     */
    cdnSrc(url) {
      if (!url || !url.includes('signature=')) return url || ''

      // 命中全局缓存，直接同步返回
      const cached = getCachedBlobUrl(url)
      if (cached) return cached

      // 已在本组件发起过请求，返回当前状态（'' 表示进行中，blobUrl 表示完成）
      if (Object.prototype.hasOwnProperty.call(this.cdnBlobMap, url)) {
        return this.cdnBlobMap[url]
      }

      // 标记"解密中"，触发异步请求
      this.$set(this.cdnBlobMap, url, '')
      getDecryptedBlobUrl(url)
        .then(blobUrl => this.$set(this.cdnBlobMap, url, blobUrl))
        .catch(() => this.$set(this.cdnBlobMap, url, url))

      return ''
    }
  }
}
