package com.ruoyi.chigua.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.RedgifsUser;

/**
 * RedGifs用户信息Service接口
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
public interface IRedgifsUserService 
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
     * 带缓存的分页用户列表（防止大量并发直接打到数据库）
     */
    List<RedgifsUser> getCachedUserList(Integer status, String username, int pageNum, int pageSize);

    /**
     * 带缓存的用户总数统计
     */
    long getCachedUserCount(Integer status, String username);

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
     * 批量删除RedGifs用户信息
     * 
     * @param ids 需要删除的RedGifs用户信息主键集合
     * @return 结果
     */
    public int deleteRedGifsUserByIds(Long[] ids);

    /**
     * 删除RedGifs用户信息信息
     * 
     * @param id RedGifs用户信息主键
     * @return 结果
     */
    public int deleteRedGifsUserById(Long id);

    /**
     * 根据用户名查询RedGifs用户信息
     * @param username 用户名
     * @return RedGifs用户信息
     */
    RedgifsUser selectRedGifsUserByUsername(String username);

    /**
     * 批量检查用户是否存在
     * @param usernames 用户名列表
     * @return Map<String, Map<String, Object>> 包含username, exists, userId
     */
    Map<String, Map<String, Object>> checkRedgifsUsersExist(List<String> usernames);

    /**
     * 入库RedGifs用户
     * @param redgifsUser 用户信息
     * @return Map<String, Object> 包含success, userId, exists, message
     */
    Map<String, Object> ingestRedgifsUser(RedgifsUser redgifsUser);

    /**
     * 更新用户同步状态
     * @param userId 用户ID
     * @param syncStatus 同步状态：0-未同步，1-同步中，2-已同步
     * @return 是否成功
     */
    boolean updateSyncStatus(Long userId, Integer syncStatus);

    /**
     * 查询指定用户的视频列表
     * @param userId 用户ID
     * @return 视频列表
     */
    List<com.ruoyi.chigua.domain.RedgifsVideo> selectVideosByUserId(Long userId);
}
