package com.ruoyi.chigua.service;

import com.ruoyi.chigua.domain.VideoKey;

/**
 * 视频加密密钥 Service 接口
 */
public interface IVideoKeyService {

    /**
     * 保存密钥（hex 字符串 → 二进制字节存库）
     *
     * @param videoId  视频ID
     * @param sourceId 爬虫来源ID
     * @param keyHex   16字节 AES key 的十六进制字符串（32位hex）
     * @param keyIv    IV 十六进制字符串（从 m3u8 EXT-X-KEY 提取）
     * @return 是否保存成功
     */
    boolean saveKey(Long videoId, String sourceId, String keyHex, String keyIv);

    /**
     * 按 videoId 获取密钥原始字节
     *
     * @param videoId 视频ID
     * @return 16字节 key；不存在或出错返回 null
     */
    byte[] getKeyData(Long videoId);

    /**
     * 按 videoId 获取完整密钥记录
     */
    VideoKey getVideoKey(Long videoId);

    /**
     * 按 sourceId 获取密钥原始字节
     * 用于 hls.js 请求 /open/key-s/{sourceId} 时查找密钥
     *
     * @param sourceId 爬虫写入时的来源ID（与 m3u8 key URI 中的 sourceId 对应）
     * @return 16字节 key；不存在或出错返回 null
     */
    byte[] getKeyDataBySourceId(String sourceId);
}
