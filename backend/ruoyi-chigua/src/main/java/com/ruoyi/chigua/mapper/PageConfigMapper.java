package com.ruoyi.chigua.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.PageConfig;

/**
 * 页面配置 数据层
 * 
 * @author ruoyi
 * @date 2025-01-01
 */
public interface PageConfigMapper
{
    /**
     * 查询页面配置
     * 
     * @param configId 页面配置主键
     * @return 页面配置
     */
    public PageConfig selectPageConfigById(Long configId);

    /**
     * 根据配置键值查询页面配置
     * 
     * @param configKey 配置键值
     * @return 页面配置
     */
    public PageConfig selectPageConfigByKey(String configKey);

    /**
     * 查询页面配置列表
     * 
     * @param pageConfig 页面配置
     * @return 页面配置集合
     */
    public List<PageConfig> selectPageConfigList(PageConfig pageConfig);

    /**
     * 根据配置类型查询页面配置列表
     * 
     * @param configType 配置类型
     * @return 页面配置集合
     */
    public List<PageConfig> selectPageConfigListByType(String configType);

    /**
     * 根据配置分类查询页面配置列表
     * 
     * @param configCategory 配置分类
     * @return 页面配置集合
     */
    public List<PageConfig> selectPageConfigListByCategory(String configCategory);

    /**
     * 查询所有有效的页面配置（用于前端缓存）
     * 
     * @return 页面配置Map，key为configKey，value为PageConfig
     */
    public List<PageConfig> selectAllActivePageConfigs();

    /**
     * 校验配置键值是否唯一
     * 
     * @param configKey 配置键值
     * @return 页面配置信息
     */
    public PageConfig checkConfigKeyUnique(String configKey);

    /**
     * 新增页面配置
     * 
     * @param pageConfig 页面配置
     * @return 结果
     */
    public int insertPageConfig(PageConfig pageConfig);

    /**
     * 修改页面配置
     * 
     * @param pageConfig 页面配置
     * @return 结果
     */
    public int updatePageConfig(PageConfig pageConfig);

    /**
     * 删除页面配置
     * 
     * @param configId 页面配置主键
     * @return 结果
     */
    public int deletePageConfigById(Long configId);

    /**
     * 批量删除页面配置
     * 
     * @param configIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePageConfigByIds(Long[] configIds);
}