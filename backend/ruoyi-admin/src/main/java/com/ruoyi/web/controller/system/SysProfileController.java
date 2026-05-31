package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TotpUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = getLoginUser();
        Long userId = loginUser.getUserId();
        String password = loginUser.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userId, newPassword) > 0)
        {
            // 更新缓存用户密码&密码最后更新时间
            loginUser.getUser().setPwdUpdateDate(DateUtils.getNowDate());
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION, true);
            if (userService.updateUserAvatar(loginUser.getUserId(), avatar))
            {
                String oldAvatar = loginUser.getUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatar))
                {
                    FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(oldAvatar));
                }
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }

    /**
     * 获取TOTP绑定信息（生成新密钥和二维码URL）
     */
    @GetMapping("/totp/qrcode")
    public AjaxResult getTotpQrCode()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        String secret = TotpUtils.generateSecret();
        String otpAuthUrl = TotpUtils.getOtpAuthUrl("后台管理", user.getUserName(), secret);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("secret", secret);
        data.put("otpAuthUrl", otpAuthUrl);
        data.put("totpEnabled", Integer.valueOf(1).equals(user.getTotpEnabled()));
        return success(data);
    }

    /**
     * 绑定TOTP（验证动态码后保存密钥）
     */
    @Log(title = "绑定Google验证器", businessType = BusinessType.UPDATE)
    @PostMapping("/totp/bind")
    public AjaxResult bindTotp(@RequestBody Map<String, String> params)
    {
        String secret = params.get("secret");
        String code = params.get("code");
        if (StringUtils.isEmpty(secret) || StringUtils.isEmpty(code))
        {
            return error("参数不能为空");
        }
        if (!TotpUtils.verifyCode(secret, code))
        {
            return error("动态码不正确，请确认手机时间准确后重试");
        }
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        user.setTotpSecret(secret);
        user.setTotpEnabled(1);
        if (userService.updateUserTotp(user) > 0)
        {
            tokenService.setLoginUser(loginUser);
            return success("Google验证器绑定成功");
        }
        return error("绑定失败，请联系管理员");
    }

    /**
     * 解绑TOTP
     */
    @Log(title = "解绑Google验证器", businessType = BusinessType.UPDATE)
    @DeleteMapping("/totp/unbind")
    public AjaxResult unbindTotp(@RequestBody Map<String, String> params)
    {
        String code = params.get("code");
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        // 解绑时需验证当前动态码，防止误操作
        if (Integer.valueOf(1).equals(user.getTotpEnabled()))
        {
            if (StringUtils.isEmpty(code) || !TotpUtils.verifyCode(user.getTotpSecret(), code))
            {
                return error("动态码不正确，解绑失败");
            }
        }
        user.setTotpSecret(null);
        user.setTotpEnabled(0);
        if (userService.updateUserTotp(user) > 0)
        {
            tokenService.setLoginUser(loginUser);
            return success("Google验证器已解绑");
        }
        return error("解绑失败，请联系管理员");
    }
}
