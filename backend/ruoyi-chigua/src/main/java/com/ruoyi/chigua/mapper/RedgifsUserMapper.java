package com.ruoyi.chigua.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.chigua.domain.RedgifsUser;
import com.ruoyi.chigua.domain.RedgifsVideo;
import org.apache.ibatis.annotations.Param;

/**
 * RedGifs用户信息Mapper接口
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public interface RedgifsUserMapper 
{
    /**
     * 查询RedGifs用户信息
     * 
     * @param id RedGifs用户信息主键
     * @return RedGifs用户信息
     */
    public RedgifsUser selectRedGifsUserById(Long id);

    /**
     * 查询RedGifs用户信息列表
     * 
     * @param redgifsUser RedGifs用户信息
     * @return RedGifs用户信息集合
     */
    public List<RedgifsUser> selectRedGifsUserList(RedgifsUser redgifsUser);

    /**
     * 统计RedGifs用户数量（用于缓存分页）
     *
     * @param redgifsUser 查询条件
     * @return 用户总数
     */
    public long countRedGifsUserList(RedgifsUser redgifsUser);

    /**
     * 新增RedGifs用户信息
     * 
     * @param redgifsUser RedGifs用户信息
     * @return 结果
     */
    public int insertRedGifsUser(RedgifsUser redgifsUser);

    /**
     * 修改RedGifs用户信息
     * 
     * @param redgifsUser RedGifs用户信息
     * @return 结果
     */
    public int updateRedGifsUser(RedgifsUser redgifsUser);

    /**
     * 删除RedGifs用户信息
     * 
     * @param id RedGifs用户信息主键
     * @return 结果
     */
    public int deleteRedGifsUserById(Long id);

    /**
     * 批量删除RedGifs用户信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRedGifsUserByIds(Long[] ids);

    /**
     * 根据用户名查询RedGifs用户信息
     * @param username 用户名
     * @return RedGifs用户信息
     */
    public RedgifsUser selectRedGifsUserByUsername(String username);


    /**
     * 批量查询用户是否存在
     * @param usernames 用户名列表
     * @return 存在的用户列表
     */
    public List<RedgifsUser> selectRedGifsUsersByUsernames(@Param("usernames") List<String> usernames);

    /**
     * 更新用户同步状态
     * @param username 用户名
     * @param syncStatus 同步状态
     * @param lastSyncAt 最后同步时间
     * @return 结果
     */
    int updateRedGifsUserSyncStatus(@Param("username") String username, 
                                    @Param("syncStatus") Integer syncStatus, 
                                    @Param("lastSyncAt") Date lastSyncAt);

    /**
     * 查询指定用户的视频列表
     * @param userId 用户ID
     * @return 视频列表
     */
    List<RedgifsVideo> selectVideosByUserId(@Param("userId") Long userId);

    /**
     * 直接更新用户头像URL（用于视频封面同步到用户头像）
     * @param userId 用户主键ID
     * @param profileImageUrl 新头像地址（R2 相对路径）
     * @return 影响行数
     */
    int updateProfileImageUrl(@Param("userId") Long userId, @Param("profileImageUrl") String profileImageUrl);
}
