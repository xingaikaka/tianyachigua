package com.ruoyi.chigua.service.impl;

import com.ruoyi.chigua.domain.VideoKey;
import com.ruoyi.chigua.mapper.VideoKeyMapper;
import com.ruoyi.chigua.service.IVideoKeyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 视频加密密钥 Service 实现
 */
@Service
@RequiredArgsConstructor
public class VideoKeyServiceImpl implements IVideoKeyService {

    private static final Logger logger = LoggerFactory.getLogger(VideoKeyServiceImpl.class);

    private final VideoKeyMapper videoKeyMapper;

    @Override
    public boolean saveKey(Long videoId, String sourceId, String keyHex, String keyIv) {
        try {
            byte[] keyBytes = hexToBytes(keyHex);
            if (keyBytes == null || keyBytes.length != 16) {
                logger.error("[VideoKey] keyHex 无效，不是16字节: videoId={}, keyHex={}", videoId, keyHex);
                return false;
            }

            VideoKey vk = new VideoKey();
            vk.setVideoId(videoId);
            vk.setSourceId(sourceId);
            vk.setKeyData(keyBytes);
            vk.setKeyIv(keyIv);

            int rows = videoKeyMapper.insertVideoKey(vk);
            if (rows == 1) {
                logger.info("[VideoKey] 密钥新增成功: videoId={}, sourceId={}", videoId, sourceId);
                return true;
            } else if (rows == 2) {
                logger.info("[VideoKey] 密钥已存在，已更新: videoId={}, sourceId={}", videoId, sourceId);
                return true;
            } else {
                logger.warn("[VideoKey] 密钥保存失败，影响行数=0: videoId={}", videoId);
                return false;
            }
        } catch (Exception e) {
            logger.error("[VideoKey] 密钥保存异常: videoId={}, error={}", videoId, e.getMessage());
            return false;
        }
    }

    @Override
    public byte[] getKeyData(Long videoId) {
        VideoKey vk = getVideoKey(videoId);
        return vk != null ? vk.getKeyData() : null;
    }

    @Override
    public VideoKey getVideoKey(Long videoId) {
        try {
            return videoKeyMapper.selectByVideoId(videoId);
        } catch (Exception e) {
            logger.error("[VideoKey] 查询密钥异常: videoId={}, error={}", videoId, e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] getKeyDataBySourceId(String sourceId) {
        if (sourceId == null || sourceId.isEmpty()) return null;
        try {
            VideoKey vk = videoKeyMapper.selectBySourceId(sourceId);
            return vk != null ? vk.getKeyData() : null;
        } catch (Exception e) {
            logger.error("[VideoKey] 按 sourceId 查询密钥异常: sourceId={}, error={}", sourceId, e.getMessage());
            return null;
        }
    }

    /**
     * 十六进制字符串转字节数组
     * 支持含 "0x" 前缀或不含前缀的格式
     */
    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        String h = hex.startsWith("0x") || hex.startsWith("0X") ? hex.substring(2) : hex;
        h = h.trim();
        if (h.length() != 32) return null;  // 16字节 = 32位hex
        try {
            byte[] bytes = new byte[16];
            for (int i = 0; i < 16; i++) {
                bytes[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
