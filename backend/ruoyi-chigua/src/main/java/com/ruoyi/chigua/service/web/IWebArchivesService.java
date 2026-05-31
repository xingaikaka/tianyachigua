package com.ruoyi.chigua.service.web;

import com.ruoyi.common.core.domain.AjaxResult;

/**
 * Web端往期内容服务接口
 */
public interface IWebArchivesService {

    /**
     * 根据日期获取往期内容
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 按日期分组的视频数据
     */
    AjaxResult getArchivesByDate(Integer pageNum, Integer pageSize);

    /**
     * 获取标签列表（支持限制数量）
     * @param limit 限制返回的标签数量
     * @return 标签列表
     */
    AjaxResult getAllTags(Integer limit);
}