package com.ruoyi.chigua.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.chigua.domain.Tag;

/**
 * 标签Mapper接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface TagMapper 
{
    /**
     * 查询标签
     * 
     * @param id 标签主键
     * @return 标签
     */
    public Tag selectTagById(Long id);

    /**
     * 查询标签列表
     * 
     * @param tag 标签
     * @return 标签集合
     */
    public List<Tag> selectTagList(Tag tag);

    /**
     * 新增标签
     * 
     * @param tag 标签
     * @return 结果
     */
    public int insertTag(Tag tag);

    /**
     * 修改标签
     * 
     * @param tag 标签
     * @return 结果
     */
    public int updateTag(Tag tag);

    /**
     * 删除标签
     * 
     * @param id 标签主键
     * @return 结果
     */
    public int deleteTagById(Long id);

    /**
     * 批量删除标签
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTagByIds(Long[] ids);

    /**
     * 校验标签名称是否唯一
     * 
     * @param name 标签名称
     * @return 标签信息
     */
    public Tag checkTagNameUnique(String name);

    /**
     * 检查标签是否被使用
     * 
     * @param id 标签ID
     * @return 使用次数
     */
    public int checkTagExistUsage(Long id);

    /**
     * 增加标签使用次数
     * 
     * @param id 标签ID
     * @param increment 增加的数量
     * @return 结果
     */
    public int incrementTagUsageCount(@Param("id") Long id, @Param("increment") int increment);

    /**
     * 批量更新标签使用次数
     * 
     * @param tagIds 标签ID列表
     * @param increment 增加的数量（可以为负数表示减少）
     * @return 结果
     */
    public int batchUpdateTagUsageCount(@Param("tagIds") List<Long> tagIds, @Param("increment") int increment);
} 