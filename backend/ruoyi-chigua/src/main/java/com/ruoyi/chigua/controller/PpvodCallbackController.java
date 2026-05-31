package com.ruoyi.chigua.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;

import com.ruoyi.chigua.service.IVideoTranscodeService;
import com.ruoyi.chigua.service.IVideoService;
import com.ruoyi.chigua.dto.PpvodCallbackDto;
import java.util.Map;
import java.util.HashMap;

/**
 * ppvod回调处理Controller
 * 
 * @author ruoyi
 * @date 2025-01-15
 */
@RestController
@RequestMapping("/chigua/video/ppvod")
public class PpvodCallbackController extends BaseController
{
    @Autowired
    private IVideoTranscodeService videoTranscodeService;
    
    @Autowired
    private IVideoService videoService;

    /**
     * 处理ppvod转码回调 - 只保存转码记录到video_transcodes表
     */
    @Log(title = "ppvod转码回调", businessType = BusinessType.INSERT)
    @PostMapping("/callback")
    public AjaxResult handleCallback(@RequestBody PpvodCallbackDto callbackData)
    {
        try {
            logger.info("收到ppvod回调数据: transcodeId={}", callbackData.getTranscodeId());
            
            // 验证必要参数
            if (callbackData.getTranscodeId() == null || callbackData.getTranscodeId().trim().isEmpty()) {
                return error("转码ID不能为空");
            }
            
            // 保存转码记录到video_transcodes表
            int transcodeResult = videoTranscodeService.savePpvodCallback(callbackData);
            
            if (transcodeResult > 0) {
                logger.info("ppvod回调处理成功: transcodeId={}, 转码记录已保存", callbackData.getTranscodeId());
                
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("message", "回调处理成功");
                resultMap.put("transcodeId", callbackData.getTranscodeId());
                resultMap.put("transcodeSaved", true);
                resultMap.put("status", callbackData.getTranscodeResult());
                resultMap.put("resolution", callbackData.getResolution());
                resultMap.put("duration", callbackData.getDuration());
                resultMap.put("fileSize", callbackData.getFileSize());
                
                return success(resultMap);
            } else {
                logger.error("保存转码记录失败: transcodeId={}", callbackData.getTranscodeId());
                return error("保存转码记录失败");
            }
            
        } catch (Exception e) {
            logger.error("ppvod回调处理异常: transcodeId={}, 错误: {}", 
                callbackData.getTranscodeId(), e.getMessage(), e);
            return error("回调处理异常: " + e.getMessage());
        }
    }

    /**
     * 查看转码记录状态
     */
    @GetMapping("/transcode-status/{transcodeId}")
    public AjaxResult getTranscodeStatus(@PathVariable String transcodeId)
    {
        try {
            // 查询转码记录
            com.ruoyi.chigua.domain.VideoTranscode transcodeRecord = 
                videoTranscodeService.selectVideoTranscodeByTranscodeId(transcodeId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("transcodeRecord", transcodeRecord);
            result.put("hasTranscodeRecord", transcodeRecord != null);
            // isUsed字段已从表结构中移除（表结构已调整为与pronhub一致）
            result.put("isUsed", false); // 默认为未使用
            
            if (transcodeRecord != null) {
                result.put("status", transcodeRecord.getStatus());
                result.put("transcodeResult", transcodeRecord.getTranscodeResult());
                result.put("resolution", transcodeRecord.getResolution());
                result.put("duration", transcodeRecord.getDuration());
                result.put("fileSize", transcodeRecord.getFileSize());
                result.put("createdAt", transcodeRecord.getCreatedAt());
                result.put("updatedAt", transcodeRecord.getUpdatedAt());
            }
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("查询转码状态异常: {}", e.getMessage(), e);
            return error("查询转码状态异常: " + e.getMessage());
        }
    }

    /**
     * 处理ppvod转码回调并自动插入视频到富文本 - 新增功能
     * 保持现有业务不变，在保存转码记录的基础上，根据视频名称查询videos表并更新富文本内容
     */
    @PostMapping("/callback-with-content-update")
    public AjaxResult handleCallbackWithContentUpdate(@RequestBody PpvodCallbackDto callbackData)
    {
        try {
            logger.info("收到ppvod回调数据（带富文本更新）: transcodeId={}, orgfile={}", 
                callbackData.getTranscodeId(), callbackData.getOrgfile());
            
            // 验证必要参数
            if (callbackData.getTranscodeId() == null || callbackData.getTranscodeId().trim().isEmpty()) {
                return error("转码ID不能为空");
            }
            
            // 1. 先执行原有业务：保存转码记录到video_transcodes表
            int transcodeResult = videoTranscodeService.savePpvodCallback(callbackData);
            
            if (transcodeResult <= 0) {
                logger.error("保存转码记录失败: transcodeId={}", callbackData.getTranscodeId());
                return error("保存转码记录失败");
            }
            
            logger.info("✅ 转码记录保存成功: transcodeId={}", callbackData.getTranscodeId());
            
            // 2. 新增业务：根据视频名称查询并更新富文本内容
            String videoUpdateResult = null;
            try {
                videoUpdateResult = videoService.updateVideoContentByTranscodeCallback(callbackData);
                logger.info("✅ 富文本更新处理完成: transcodeId={}, 结果={}", 
                    callbackData.getTranscodeId(), videoUpdateResult);
            } catch (Exception e) {
                logger.error("❌ 富文本更新处理异常: transcodeId={}, 错误: {}", 
                    callbackData.getTranscodeId(), e.getMessage(), e);
                // 富文本更新失败不影响主要业务，仅记录日志
                videoUpdateResult = "富文本更新失败: " + e.getMessage();
            }
            
            // 3. 返回结果（包含原有业务和新增业务的结果）
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("message", "回调处理成功");
            resultMap.put("transcodeId", callbackData.getTranscodeId());
            resultMap.put("transcodeSaved", true);
            resultMap.put("status", callbackData.getTranscodeResult());
            resultMap.put("resolution", callbackData.getResolution());
            resultMap.put("duration", callbackData.getDuration());
            resultMap.put("fileSize", callbackData.getFileSize());
            resultMap.put("videoContentUpdateResult", videoUpdateResult);
            
            return success(resultMap);
            
        } catch (Exception e) {
            logger.error("ppvod回调处理异常（带富文本更新）: transcodeId={}, 错误: {}", 
                callbackData.getTranscodeId(), e.getMessage(), e);
            return error("回调处理异常: " + e.getMessage());
        }
    }

}