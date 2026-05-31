package com.ruoyi.chigua.controller.open;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.chigua.service.IVideoKeyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 视频 AES-128 密钥分发接口
 *
 * 供 HLS 播放器获取解密 key（AES-128-CBC）
 * m3u8 中 EXT-X-KEY URI 格式：
 *   /open/key/{videoId}    ← 按 videoId 查询（旧流程）
 *   /open/key-s/{sourceId} ← 按 sourceId 查询（新流程，爬虫写入）
 */
@Anonymous
@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class OpenKeyController {

    private static final Logger logger = LoggerFactory.getLogger(OpenKeyController.class);

    private final IVideoKeyService videoKeyService;

    /**
     * 获取视频 AES-128 密钥（按 videoId）
     *
     * GET /open/key/{videoId}
     */
    @GetMapping("/key/{videoId}")
    public ResponseEntity<byte[]> getKey(@PathVariable Long videoId) {
        byte[] keyData = videoKeyService.getKeyData(videoId);
        if (keyData == null || keyData.length != 16) {
            logger.warn("[KeyAPI] 密钥不存在: videoId={}", videoId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return buildKeyResponse(keyData);
    }

    /**
     * 获取视频 AES-128 密钥（按 sourceId）
     *
     * GET /open/key-s/{sourceId}
     *
     * hls.js 播放时，m3u8 中的 EXT-X-KEY URI = /open/key-s/{sourceId}
     * 前端 xhrSetup 拦截后拼接 /prod-api 前缀，发到此接口
     */
    @GetMapping("/key-s/{sourceId}")
    public ResponseEntity<byte[]> getKeyBySourceId(@PathVariable String sourceId) {
        byte[] keyData = videoKeyService.getKeyDataBySourceId(sourceId);
        if (keyData == null || keyData.length != 16) {
            logger.warn("[KeyAPI] 密钥不存在: sourceId={}", sourceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return buildKeyResponse(keyData);
    }

    private ResponseEntity<byte[]> buildKeyResponse(byte[] keyData) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Cache-Control", "no-store")
                .body(keyData);
    }
}
