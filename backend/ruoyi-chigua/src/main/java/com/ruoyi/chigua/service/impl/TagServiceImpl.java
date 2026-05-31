package com.ruoyi.chigua.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.chigua.mapper.TagMapper;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.service.ITagService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.constant.UserConstants;
import org.springframework.cache.annotation.Cacheable;
import com.ruoyi.chigua.service.CacheRefreshService;

/**
 * 标签Service业务层处理
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
@Service
public class TagServiceImpl implements ITagService 
{
    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    /**
     * 查询标签
     * 
     * @param id 标签主键
     * @return 标签
     */
    @Override
    public Tag selectTagById(Long id)
    {
        return tagMapper.selectTagById(id);
    }

    /**
     * 查询标签列表
     * 
     * @param tag 标签
     * @return 标签
     */
    @Override
    public List<Tag> selectTagList(Tag tag)
    {
        return tagMapper.selectTagList(tag);
    }

    /**
     * 新增标签
     * 
     * @param tag 标签
     * @return 结果
     */
    @Override
    public int insertTag(Tag tag)
    {
        int result = tagMapper.insertTag(tag);
        if (result > 0) {
            cacheRefreshService.refreshTagCache();
        }
        return result;
    }

    /**
     * 修改标签
     * 
     * @param tag 标签
     * @return 结果
     */
    @Override
    public int updateTag(Tag tag)
    {
        int result = tagMapper.updateTag(tag);
        if (result > 0) {
            cacheRefreshService.refreshTagCache();
        }
        return result;
    }

    /**
     * 批量删除标签
     * 
     * @param ids 需要删除的标签主键
     * @return 结果
     */
    @Override
    public int deleteTagByIds(Long[] ids)
    {
        int result = tagMapper.deleteTagByIds(ids);
        if (result > 0) {
            cacheRefreshService.refreshTagCache();
        }
        return result;
    }

    /**
     * 删除标签信息
     * 
     * @param id 标签主键
     * @return 结果
     */
    @Override
    public int deleteTagById(Long id)
    {
        int result = tagMapper.deleteTagById(id);
        if (result > 0) {
            cacheRefreshService.refreshTagCache();
        }
        return result;
    }

    /**
     * 校验标签名称是否唯一
     * 
     * @param tag 标签信息
     * @return 结果
     */
    @Override
    public boolean checkTagNameUnique(Tag tag)
    {
        Long tagId = StringUtils.isNull(tag.getId()) ? -1L : tag.getId();
        Tag info = tagMapper.checkTagNameUnique(tag.getName());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != tagId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
} 