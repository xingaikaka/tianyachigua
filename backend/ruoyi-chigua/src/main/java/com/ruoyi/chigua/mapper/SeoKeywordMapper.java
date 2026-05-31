package com.ruoyi.chigua.mapper;

import com.ruoyi.chigua.domain.SeoKeyword;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * SEO关键词Mapper接口
 * 
 * @author ruoyi
 * @date 2026-01-14
 */
public interface SeoKeywordMapper {
    
    /**
     * 根据ID查询关键词
     */
    SeoKeyword selectById(@Param("id") Long id);
    
    /**
     * 根据关键词内容查询
     */
    SeoKeyword selectByKeyword(@Param("keyword") String keyword);
    
    /**
     * 查询热门关键词（根据搜索次数排序）
     */
    List<SeoKeyword> selectHotKeywords(@Param("limit") Integer limit);
    
    /**
     * 查询随机关键词
     */
    List<SeoKeyword> selectRandomKeywords(@Param("limit") Integer limit);
    
    /**
     * 查询所有启用的关键词（用于sitemap）
     */
    List<SeoKeyword> selectAllActive();

    /**
     * 分页查询启用的关键词 ID（用于 sitemap 分片，只取 id，避免全表加载）
     */
    List<SeoKeyword> selectActiveForSitemap(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 增加搜索次数
     */
    int incrementSearchCount(@Param("id") Long id);
    
    /**
     * 增加点击次数
     */
    int incrementClickCount(@Param("id") Long id);
    
    /**
     * 插入关键词
     */
    int insertSeoKeyword(SeoKeyword seoKeyword);
    
    /**
     * 批量插入关键词
     */
    int batchInsert(@Param("list") List<SeoKeyword> list);
    
    /**
     * 更新关键词
     */
    int updateSeoKeyword(SeoKeyword seoKeyword);
    
    /**
     * 删除关键词
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 统计关键词总数
     */
    int countAll();
    
    /**
     * 统计启用的关键词数量
     */
    int countActive();
}

