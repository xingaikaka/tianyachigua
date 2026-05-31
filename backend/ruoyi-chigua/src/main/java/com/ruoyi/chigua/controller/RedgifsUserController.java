package com.ruoyi.chigua.controller;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.annotation.EncryptResponse;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.chigua.domain.RedgifsUser;
import com.ruoyi.chigua.domain.RedgifsVideo;
import com.ruoyi.chigua.mapper.RedgifsVideoMapper;
import com.ruoyi.chigua.service.IRedgifsUserService;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * RedGifs用户信息Controller
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
@RestController
@RequestMapping("/web/api/redgifs/users")
public class RedgifsUserController extends BaseController
{
    @Autowired
    private IRedgifsUserService redgifsUserService;
    
    @Autowired
    private ChiguaUrlService chiguaUrlService;

    @Autowired
    private RedgifsVideoMapper redgifsVideoMapper;

    /**
     * 查询RedGifs用户信息列表（带缓存，防止并发打穿数据库）
     */
    @RateLimiter(count = 30, time = 60, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/list")
    public TableDataInfo list(RedgifsUser redgifsUser,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize)
    {
        List<RedgifsUser> list = redgifsUserService.getCachedUserList(
                redgifsUser.getStatus(), redgifsUser.getUsername(), pageNum, pageSize);
        long total = redgifsUserService.getCachedUserCount(
                redgifsUser.getStatus(), redgifsUser.getUsername());

        // URL签名在缓存之外单独处理（签名URL有自身的缓存，TTL独立）
        for (RedgifsUser user : list) {
            processUserUrls(user);
        }

        TableDataInfo result = new TableDataInfo();
        result.setCode(com.ruoyi.common.constant.HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        result.setRows(list);
        result.setTotal(total);
        return result;
    }

    /**
     * 导出RedGifs用户信息列表
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:export')")
    @Log(title = "RedGifs用户信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, RedgifsUser redgifsUser)
    {
        List<RedgifsUser> list = redgifsUserService.selectRedGifsUserList(redgifsUser);
        ExcelUtil<RedgifsUser> util = new ExcelUtil<RedgifsUser>(RedgifsUser.class);
        util.exportExcel(response, list, "RedGifs用户数据");
    }

    /**
     * 获取RedGifs用户信息详细信息（管理端按DB主键查询）
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        RedgifsUser user = redgifsUserService.selectRedGifsUserById(id);
        if (user != null) {
            processUserUrls(user);
        }
        return success(user);
    }

    /**
     * 根据用户名获取RedGifs用户信息
     */
    @RateLimiter(count = 60, time = 60, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping(value = "/username/{username}")
    public AjaxResult getInfoByUsername(@PathVariable("username") String username)
    {
        RedgifsUser user = redgifsUserService.selectRedGifsUserByUsername(username);
        if (user != null) {
            processUserUrls(user);
        }
        return success(user);
    }

    /**
     * 新增RedGifs用户信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:add')")
    @Log(title = "RedGifs用户信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody RedgifsUser redgifsUser)
    {
        return toAjax(redgifsUserService.insertRedGifsUser(redgifsUser));
    }

    /**
     * 修改RedGifs用户信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:edit')")
    @Log(title = "RedGifs用户信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody RedgifsUser redgifsUser)
    {
        return toAjax(redgifsUserService.updateRedGifsUser(redgifsUser));
    }

    /**
     * 删除RedGifs用户信息
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:remove')")
    @Log(title = "RedGifs用户信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(redgifsUserService.deleteRedGifsUserByIds(ids));
    }

    /**
     * 批量检查用户是否存在
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:query')")
    @PostMapping("/check")
    public AjaxResult checkUsersExist(@RequestBody Map<String, List<String>> request)
    {
        List<String> usernames = request.get("usernames");
        if (usernames == null || usernames.isEmpty()) {
            return error("用户名列表不能为空");
        }
        Map<String, Map<String, Object>> result = redgifsUserService.checkRedgifsUsersExist(usernames);
        return success(result);
    }

    /**
     * 入库RedGifs用户
     */
    @PostMapping("/ingest")
    public AjaxResult ingestUser(@RequestBody RedgifsUser redgifsUser)
    {
        if (redgifsUser.getUsername() == null || redgifsUser.getUsername().isEmpty()) {
            return error("用户名不能为空");
        }
        Map<String, Object> result = redgifsUserService.ingestRedgifsUser(redgifsUser);
        return success(result);
    }

    /**
     * 更新用户同步状态
     */
    @PreAuthorize("@ss.hasPermi('chigua:redgifs:user:edit')")
    @PostMapping("/update-sync-status")
    public AjaxResult updateSyncStatus(@RequestBody Map<String, Object> request)
    {
        Long userId = null;
        if (request.get("userId") instanceof Integer) {
            userId = ((Integer) request.get("userId")).longValue();
        } else if (request.get("userId") instanceof Long) {
            userId = (Long) request.get("userId");
        }
        
        if (userId == null) {
            return error("用户ID不能为空");
        }
        
        Integer syncStatus = (Integer) request.get("syncStatus");
        if (syncStatus == null) {
            return error("同步状态不能为空");
        }
        
        boolean result = redgifsUserService.updateSyncStatus(userId, syncStatus);
        return result ? success("同步状态更新成功") : error("同步状态更新失败");
    }

    /**
     * 获取指定用户的视频列表（分页）
     * 用于chigua-web前端展示用户详情页的视频列表
     */
    @RateLimiter(count = 30, time = 60, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/{userId}/videos")
    public TableDataInfo getUserVideos(@PathVariable("userId") Long userId)
    {
        startPage();
        List<RedgifsVideo> list = redgifsUserService.selectVideosByUserId(userId);
        for (RedgifsVideo video : list) {
            processVideoUrls(video);
        }
        
        return getDataTable(list);
    }
    
    /**
     * 按视频ID获取单个视频（与用户视频列表格式一致，包含签名URL）
     * 用于分享链接打开时直接定位到指定视频
     */
    @RateLimiter(count = 60, time = 60, limitType = LimitType.IP)
    @EncryptResponse
    @GetMapping("/video/{videoId}")
    public AjaxResult getVideoById(@PathVariable("videoId") Long videoId) {
        RedgifsVideo video = redgifsVideoMapper.selectRedGifsVideoById(videoId);
        if (video == null) {
            return error("视频不存在");
        }
        processVideoUrls(video);
        return success(video);
    }

    /**
     * 为用户的图片URL生成签名
     */
    private void processUserUrls(RedgifsUser user) {
        try {
            // 处理头像URL
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty() 
                && !user.getProfileImageUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    user.getProfileImageUrl(), 
                    ChiguaUrlService.ResourceType.COVER, 
                    false
                );
                user.setProfileImageUrl(signedUrl);
            }
        } catch (Exception e) {
            // 签名失败不影响数据返回，只记录日志
            logger.warn("为用户{}生成URL签名失败: {}", user.getUsername(), e.getMessage());
        }
    }
    
    /**
     * 为视频的URL生成签名
     */
    private void processVideoUrls(RedgifsVideo video) {
        try {
            // 处理高清视频URL
            if (video.getHdUrl() != null && !video.getHdUrl().isEmpty() 
                && !video.getHdUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getHdUrl(), 
                    ChiguaUrlService.ResourceType.VIDEO, 
                    false
                );
                video.setHdUrl(signedUrl);
            }
            
            // 处理标清视频URL
            if (video.getSdUrl() != null && !video.getSdUrl().isEmpty() 
                && !video.getSdUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getSdUrl(), 
                    ChiguaUrlService.ResourceType.VIDEO, 
                    false
                );
                video.setSdUrl(signedUrl);
            }
            
            // 处理封面图URL
            if (video.getPosterUrl() != null && !video.getPosterUrl().isEmpty() 
                && !video.getPosterUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getPosterUrl(), 
                    ChiguaUrlService.ResourceType.COVER, 
                    false
                );
                video.setPosterUrl(signedUrl);
            }
            
            // 处理缩略图URL
            if (video.getThumbnailUrl() != null && !video.getThumbnailUrl().isEmpty() 
                && !video.getThumbnailUrl().startsWith("http")) {
                String signedUrl = chiguaUrlService.generateUrl(
                    video.getThumbnailUrl(), 
                    ChiguaUrlService.ResourceType.THUMBNAIL, 
                    false
                );
                video.setThumbnailUrl(signedUrl);
            }
        } catch (Exception e) {
            // 签名失败不影响数据返回，只记录日志
            logger.warn("为视频{}生成URL签名失败: {}", video.getGifId(), e.getMessage());
        }
    }
}
