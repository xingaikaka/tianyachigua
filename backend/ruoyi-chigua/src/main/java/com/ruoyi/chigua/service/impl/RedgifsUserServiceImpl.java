package com.ruoyi.chigua.service.impl;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import com.github.pagehelper.PageHelper;
import com.ruoyi.chigua.mapper.RedgifsUserMapper;
import com.ruoyi.chigua.domain.RedgifsUser;
import com.ruoyi.chigua.service.IRedgifsUserService;

/**
 * RedGifs用户信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-11
 */
@Service
public class RedgifsUserServiceImpl implements IRedgifsUserService 
{
    @Autowired
    private RedgifsUserMapper redgifsUserMapper;

    /**
     * 查询RedGifs用户信息
     * 
     * @param id RedGifs用户信息主键
     * @return RedGifs用户信息
     */
    @Override
    public RedgifsUser selectRedGifsUserById(Long id)
    {
        return redgifsUserMapper.selectRedGifsUserById(id);
    }

    /**
     * 查询RedGifs用户信息列表
     * 
     * @param redgifsUser RedGifs用户信息
     * @return RedGifs用户信息
     */
    @Override
    public List<RedgifsUser> selectRedGifsUserList(RedgifsUser redgifsUser)
    {
        return redgifsUserMapper.selectRedGifsUserList(redgifsUser);
    }

    /**
     * 带缓存的分页用户列表 —— 防止并发流量直接穿透到数据库
     * key 包含全部过滤维度，写操作触发 allEntries 清除
     */
    @Override
    @Cacheable(value = "redgifsUserList",
            key = "'list_' + (#status != null ? #status : 'all') + '_' + (#username != null ? #username : 'all') + '_' + #pageNum + '_' + #pageSize + '_sort_desc_ca'",
            unless = "#result == null || #result.isEmpty()")
    public List<RedgifsUser> getCachedUserList(Integer status, String username, int pageNum, int pageSize)
    {
        RedgifsUser query = buildQuery(status, username);
        // chigua-web /web/api/redgifs/users/list：按 sort_order 倒序
        query.setSortColumn("sortOrder");
        query.setSortDirection("desc");
        PageHelper.startPage(pageNum, pageSize, false);
        return redgifsUserMapper.selectRedGifsUserList(query);
    }

    /**
     * 带缓存的用户总数 —— 与分页列表缓存独立，变动时一并清除
     */
    @Override
    @Cacheable(value = "redgifsUserList",
            key = "'count_' + (#status != null ? #status : 'all') + '_' + (#username != null ? #username : 'all')")
    public long getCachedUserCount(Integer status, String username)
    {
        RedgifsUser query = buildQuery(status, username);
        return redgifsUserMapper.countRedGifsUserList(query);
    }

    private RedgifsUser buildQuery(Integer status, String username)
    {
        RedgifsUser query = new RedgifsUser();
        query.setStatus(status);
        if (username != null && !username.trim().isEmpty()) {
            query.setUsername(username.trim());
        }
        return query;
    }

    /**
     * 新增RedGifs用户信息
     * 
     * @param redgifsUser RedGifs用户信息
     * @return 结果
     */
    @Override
    @CacheEvict(value = "redgifsUserList", allEntries = true)
    public int insertRedGifsUser(RedgifsUser redgifsUser)
    {
        return redgifsUserMapper.insertRedGifsUser(redgifsUser);
    }

    /**
     * 修改RedGifs用户信息
     * 
     * @param redgifsUser RedGifs用户信息
     * @return 结果
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = "redgifsUserDetail", key = "'user_' + #redgifsUser.username", condition = "#redgifsUser.username != null"),
        @CacheEvict(value = "redgifsUserList", allEntries = true)
    })
    public int updateRedGifsUser(RedgifsUser redgifsUser)
    {
        return redgifsUserMapper.updateRedGifsUser(redgifsUser);
    }

    /**
     * 批量删除RedGifs用户信息
     * 
     * @param ids 需要删除的RedGifs用户信息主键
     * @return 结果
     */
    @Override
    @CacheEvict(value = "redgifsUserList", allEntries = true)
    public int deleteRedGifsUserByIds(Long[] ids)
    {
        return redgifsUserMapper.deleteRedGifsUserByIds(ids);
    }

    /**
     * 删除RedGifs用户信息信息
     * 
     * @param id RedGifs用户信息主键
     * @return 结果
     */
    @Override
    @CacheEvict(value = "redgifsUserList", allEntries = true)
    public int deleteRedGifsUserById(Long id)
    {
        return redgifsUserMapper.deleteRedGifsUserById(id);
    }

    /**
     * 根据用户名查询RedGifs用户信息
     * @param username 用户名
     * @return RedGifs用户信息
     */
    @Override
    @Cacheable(value = "redgifsUserDetail", key = "'user_' + #username", unless = "#result == null")
    public RedgifsUser selectRedGifsUserByUsername(String username)
    {
        return redgifsUserMapper.selectRedGifsUserByUsername(username);
    }

    /**
     * 批量检查用户是否存在
     * @param usernames 用户名列表
     * @return Map<String, Map<String, Object>> 包含username, exists, userId
     */
    @Override
    public Map<String, Map<String, Object>> checkRedgifsUsersExist(List<String> usernames)
    {
        Map<String, Map<String, Object>> result = new HashMap<>();
        
        if (usernames == null || usernames.isEmpty()) {
            return result;
        }
        
        List<RedgifsUser> existingUsers = redgifsUserMapper.selectRedGifsUsersByUsernames(usernames);
        Map<String, RedgifsUser> userMap = new HashMap<>();
        
        for (RedgifsUser user : existingUsers) {
            userMap.put(user.getUsername(), user);
        }
        
        for (String username : usernames) {
            Map<String, Object> userInfo = new HashMap<>();
            RedgifsUser user = userMap.get(username);
            boolean exists = user != null;
            
            userInfo.put("exists", exists);
            if (exists) {
                userInfo.put("userId", user.getId());
                userInfo.put("syncStatus", user.getSyncStatus());
                userInfo.put("lastSyncAt", user.getLastSyncAt());
                // 只要存在就认为已同步
                userInfo.put("synced", true);
            }
            result.put(username, userInfo);
        }
        
        return result;
    }

    /**
     * 入库RedGifs用户
     * @param redgifsUser 用户信息
     * @return Map<String, Object> 包含success, userId, exists, message
     */
    @Override
    public Map<String, Object> ingestRedgifsUser(RedgifsUser redgifsUser)
    {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // username 重复时追加随机数后缀直到唯一，然后作为新用户插入
            String originalUsername = redgifsUser.getUsername();
            while (redgifsUserMapper.selectRedGifsUserByUsername(redgifsUser.getUsername()) != null) {
                int suffix = (int)(Math.random() * 9000) + 1000;
                redgifsUser.setUsername(originalUsername + suffix);
            }

            // 插入数据库
            int insertResult = redgifsUserMapper.insertRedGifsUser(redgifsUser);

            result.put("success", insertResult > 0);
            result.put("userId", redgifsUser.getId());
            result.put("exists", !redgifsUser.getUsername().equals(originalUsername));
            result.put("finalUsername", redgifsUser.getUsername());
            result.put("message", insertResult > 0 ? "用户信息已入库" : "用户信息入库失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("exists", false);
            result.put("message", "入库失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 更新用户同步状态
     */
    @Override
    public boolean updateSyncStatus(Long userId, Integer syncStatus)
    {
        try {
            RedgifsUser user = new RedgifsUser();
            user.setId(userId);
            user.setSyncStatus(syncStatus);
            user.setLastSyncAt(new Date());
            int result = redgifsUserMapper.updateRedGifsUser(user);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 查询指定用户的视频列表
     * @param userId 用户ID
     * @return 视频列表
     */
    @Override
    public List<com.ruoyi.chigua.domain.RedgifsVideo> selectVideosByUserId(Long userId)
    {
        return redgifsUserMapper.selectVideosByUserId(userId);
    }
}
