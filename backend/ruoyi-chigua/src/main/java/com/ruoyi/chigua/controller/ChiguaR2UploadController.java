package com.ruoyi.chigua.controller;

import com.ruoyi.chigua.service.ChiguaR2UploadService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Chigua R2 上传控制器
 * 
 * 🚨 @Deprecated 此控制器已废弃，文件上传功能已迁移到Cloudflare Worker
 * 新的文件上传请使用：https://chigua-r2-worker.xingaikaka.workers.dev/api/upload
 * 
 * 提供文件上传到R2存储的API接口
 */
@Deprecated
@RestController
@RequestMapping("/chigua/upload")
public class ChiguaR2UploadController extends BaseController {
    
    @Autowired
    private ChiguaR2UploadService chiguaR2UploadService;
    
    /**
     * 上传图片到R2
     */
    @PostMapping("/image")
    @PreAuthorize("@ss.hasPermi('chigua:upload:image')")
    @Log(title = "R2图片上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile file) {
        return chiguaR2UploadService.uploadFile(file, "image");
    }
    
    /**
     * 上传视频到R2
     */
    @PostMapping("/video")
    @PreAuthorize("@ss.hasPermi('chigua:upload:video')")
    @Log(title = "R2视频上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadVideo(@RequestParam("file") MultipartFile file) {
        return chiguaR2UploadService.uploadFile(file, "video");
    }
    
    /**
     * 上传文档到R2
     */
    @PostMapping("/document")
    @PreAuthorize("@ss.hasPermi('chigua:upload:document')")
    @Log(title = "R2文档上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadDocument(@RequestParam("file") MultipartFile file) {
        return chiguaR2UploadService.uploadFile(file, "document");
    }
    
    /**
     * 上传音频到R2
     */
    @PostMapping("/audio")
    @PreAuthorize("@ss.hasPermi('chigua:upload:audio')")
    @Log(title = "R2音频上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadAudio(@RequestParam("file") MultipartFile file) {
        return chiguaR2UploadService.uploadFile(file, "audio");
    }
    
    /**
     * 通用文件上传（根据文件类型自动判断）
     */
    @PostMapping("/file")
    @PreAuthorize("@ss.hasPermi('chigua:upload:file')")
    @Log(title = "R2文件上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam(value = "type", required = false) String fileType) {
        // 如果没有指定类型，根据文件扩展名自动判断
        if (fileType == null || fileType.isEmpty()) {
            fileType = detectFileType(file);
        }
        return chiguaR2UploadService.uploadFile(file, fileType);
    }
    
    /**
     * 批量上传文件
     */
    @PostMapping("/files")
    @PreAuthorize("@ss.hasPermi('chigua:upload:files')")
    @Log(title = "R2批量文件上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadFiles(@RequestParam("files") MultipartFile[] files,
                                 @RequestParam(value = "type", required = false) String fileType) {
        // 如果没有指定类型，根据第一个文件的扩展名自动判断
        if (fileType == null || fileType.isEmpty()) {
            fileType = detectFileType(files[0]);
        }
        return chiguaR2UploadService.uploadFiles(files, fileType);
    }
    
    /**
     * 富文本编辑器专用上传接口
     */
    @PostMapping("/rich-text")
    @PreAuthorize("@ss.hasPermi('chigua:upload:richtext')")
    @Log(title = "富文本R2上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadForRichText(@RequestParam("file") MultipartFile file) {
        // 富文本编辑器主要上传图片
        String fileType = detectFileType(file);
        AjaxResult result = chiguaR2UploadService.uploadFile(file, fileType);
        
        if (result.isSuccess()) {
            // 返回富文本编辑器期望的格式 - 使用data包装
            return AjaxResult.success("富文本文件上传成功")
                .put("data", AjaxResult.success()
                    .put("resourceKey", result.get("resourceKey"))
                    .put("url", result.get("url"))
                    .put("fileName", result.get("fileName"))
                    .put("originalName", result.get("originalName"))
                    .put("size", result.get("size"))
                    .put("type", detectFileType(file)));
        } else {
            return AjaxResult.error()
                .put("code", 500)
                .put("msg", result.get("msg"));
        }
    }

    /**
     * 视频封面上传接口
     */
    @PostMapping("/cover")
    @PreAuthorize("@ss.hasPermi('chigua:upload:cover')")
    @Log(title = "视频封面R2上传", businessType = BusinessType.INSERT)
    public AjaxResult uploadCoverImage(@RequestParam("file") MultipartFile file) {
        // 封面上传，强制使用image类型
        AjaxResult result = chiguaR2UploadService.uploadFile(file, "image");
        
        if (result.isSuccess()) {
            // 返回与富文本上传一致的格式 - 使用data包装
            return AjaxResult.success("视频封面上传成功")
                .put("data", AjaxResult.success()
                    .put("resourceKey", result.get("resourceKey"))
                    .put("url", result.get("url"))
                    .put("fileName", result.get("fileName"))
                    .put("originalName", result.get("originalName"))
                    .put("size", result.get("size"))
                    .put("type", "image"));
        } else {
            return AjaxResult.error()
                .put("code", 500)
                .put("msg", result.get("msg"));
        }
    }
    
    /**
     * 根据文件扩展名检测文件类型
     */
    private String detectFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "document";
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        
        // 图片类型
        if (extension.matches("jpg|jpeg|png|gif|webp|bmp|svg")) {
            return "image";
        }
        // 视频类型
        else if (extension.matches("mp4|avi|mov|wmv|flv|webm|mkv|m3u8|ts")) {
            return "video";
        }
        // 音频类型
        else if (extension.matches("mp3|wav|flac|aac|ogg")) {
            return "audio";
        }
        // 文档类型
        else if (extension.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt")) {
            return "document";
        }
        else {
            return "document"; // 默认为文档类型
        }
    }
} 