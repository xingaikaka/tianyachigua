package com.ruoyi.chigua.service.web;

import java.util.Map;

import com.ruoyi.common.core.domain.AjaxResult;

/**
 * Web端标签服务接口
 */
public interface IWebTagsService {

    /**
     * 获取所有标签
     * @return 标签列表
     */
    AjaxResult getAllTags();

    /**
     * 分页获取标签
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页标签数据
     */
    AjaxResult getTagsPaginated(Integer pageNum, Integer pageSize);

    /**
     * 根据标签ID获取相关视频
     * @param tagId 标签ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 标签相关视频数据
     */
    AjaxResult getVideosByTag(Long tagId, Integer pageNum, Integer pageSize);

    /**
     * 根据标签ID获取标签信息
     * @param tagId 标签ID
     * @return 标签信息
     */
    AjaxResult getTagInfo(Long tagId);

    /**
     * 标签视频基础数据缓存（不含签名URL；list + total 同一 key，避免不一致）
     * 返回 Map：key="list" 为 List&lt;Video&gt;，key="total" 为 Long 总数
     */
    Map<String, Object> getCachedTagVideoBaseData(Long tagId, Integer pageNum, Integer pageSize);
}