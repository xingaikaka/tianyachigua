package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.mapper.SysNoticeMapper;
import com.ruoyi.system.service.ISysNoticeService;

/**
 * 公告 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService
{
    @Autowired
    private SysNoticeMapper noticeMapper;

    /**
     * 查询公告信息（带缓存）
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    @Cacheable(value = "sysNotice", key = "#noticeId", unless = "#result == null")
    public SysNotice selectNoticeById(Long noticeId)
    {
        return noticeMapper.selectNoticeById(noticeId);
    }

    /**
     * 查询公告列表（带缓存）
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    @Cacheable(value = "sysNoticeList", keyGenerator = "parameterKeyGenerator", unless = "#result == null")
    public List<SysNotice> selectNoticeList(SysNotice notice)
    {
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 新增公告（清除缓存）
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = "sysNoticeList", allEntries = true),
        @CacheEvict(value = "sysNotice", allEntries = true)
    })
    public int insertNotice(SysNotice notice)
    {
        return noticeMapper.insertNotice(notice);
    }

    /**
     * 修改公告（清除缓存）
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = "sysNoticeList", allEntries = true),
        @CacheEvict(value = "sysNotice", key = "#notice.noticeId")
    })
    public int updateNotice(SysNotice notice)
    {
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告对象（清除缓存）
     * 
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = "sysNoticeList", allEntries = true),
        @CacheEvict(value = "sysNotice", key = "#noticeId")
    })
    public int deleteNoticeById(Long noticeId)
    {
        return noticeMapper.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息（清除缓存）
     * 
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = "sysNoticeList", allEntries = true),
        @CacheEvict(value = "sysNotice", allEntries = true)
    })
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }
}
