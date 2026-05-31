package com.ruoyi.chigua.mapper;

import com.ruoyi.chigua.domain.PageConfigVersion;

/**
 * 页面配置版本 数据层
 * 
 * @author ruoyi
 * @date 2025-01-01
 */
public interface PageConfigVersionMapper
{
    /**
     * 查询最新的配置版本
     * 
     * @return 页面配置版本
     */
    public PageConfigVersion selectLatestVersion();

    /**
     * 新增页面配置版本
     * 
     * @param version 页面配置版本
     * @return 结果
     */
    public int insertPageConfigVersion(PageConfigVersion version);

    /**
     * 更新配置版本（生成新的版本哈希）
     * 
     * @return 结果
     */
    public int updateConfigVersion();
}