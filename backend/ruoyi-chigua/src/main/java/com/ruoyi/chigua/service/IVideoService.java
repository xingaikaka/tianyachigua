package com.ruoyi.chigua.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.domain.vo.web.WebVideoListVO;

/**
 * 视频管理Service接口
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface IVideoService 
{
    /**
     * 查询视频
     * 
     * @param id 视频主键
     * @return 视频
     */
    public Video selectVideoById(Long id);

    /**
     * 查询视频列表
     * 
     * @param video 视频
     * @return 视频集合
     */
    public List<Video> selectVideoList(Video video);

    /**
     * 根据source_id列表批量查询视频
     * 
     * @param sourceIds source_id列表
     * @return 视频集合（包含id和sourceId字段）
     */
    public List<Video> selectVideosBySourceIds(List<String> sourceIds);

    /**
     * 根据title列表批量查询视频（精确匹配）
     *
     * @param titles title列表
     * @return 视频集合（包含id、title、sourceId字段）
     */
    public List<Video> selectVideosByTitles(List<String> titles);

    /**
     * 查询视频列表（包含分类和标签信息）
     * 
     * @param video 视频
     * @return 视频集合
     */
    public List<Video> selectVideoListWithRelations(Video video);

    /**
     * 查询视频列表（基础字段，不含 CDN 签名 URL）
     *
     * 历史方法保留，仅用于"相邻视频"等少数仅需 id/title 的场景。
     * 已按"方案 B"重构：缓存里只存原始相对路径，签名 URL 由调用方在出口实时生成。
     *
     * @param video 视频
     * @return 视频集合（不含签名 URL）
     */
    public List<Video> selectVideoListWithSignedUrls(Video video);

    /**
     * 获取缓存的视频列表基础数据（不包含签名URL）
     * 专门为热门推荐等需要缓存但不需要签名URL的场景设计
     * 
     * @param video 视频查询条件
     * @return 视频集合（不包含签名URL）
     */
    public List<Video> getCachedVideoListBasic(Video video);



    /**
     * Web 精简版：查询视频总数 - 可缓存
     */
    public Long countWebVideoListBasic(Video video);

    /**
     * Web 精简版：分页查询视频列表（仅基础字段），并生成封面签名URL
     */
    public List<Video> selectWebVideoListBasicPagedWithSignedUrls(Video video, Integer pageNum, Integer pageSize);
    
    /**
     * 带缓存的视频列表查询（用于解决AOP代理问题）
     */
    public List<Video> getCachedVideoListBasicPaged(Video video, int offset, int pageSize);

    /**
     * 带缓存的分类视频混合排序查询
     */
    public List<Video> getCachedCategoryVideoListWithMixedSort(Video video, int offset, int pageSize);

    /**
     * 列表专用：分页查询视频列表（仅必需字段），并生成封面签名URL
     */
    public List<WebVideoListVO> selectVideoListOnlyPagedWithSignedUrls(Video video, Integer pageNum, Integer pageSize);

    /**
     * 列表专用：带缓存的视频列表查询（仅必需字段）
     */
    public List<WebVideoListVO> getCachedVideoListOnlyPaged(Video video, int offset, int pageSize);

    /**
     * 列表专用：查询视频总数
     */
    public Long countVideoListOnly(Video video);

    /**
     * 获取（或生成）带缓存的封面签名URL
     */
    public String getCachedCoverSignedUrl(Long videoId, String originalPath);

    /**
     * 查询视频的分类列表（带缓存）
     *
     * @param videoId 视频ID
     * @return 分类集合
     */
    public List<Category> selectCategoriesByVideoId(Long videoId);

    /**
     * 查询视频的标签列表（带缓存）
     *
     * @param videoId 视频ID  
     * @return 标签集合
     */
    public List<Tag> selectTagsByVideoId(Long videoId);

    /**
     * 批量查询视频的标签信息（解决N+1问题）
     *
     * @param videoIds 视频ID列表
     * @return 视频ID到标签列表的映射
     */
    public Map<Long, List<Tag>> selectTagsByVideoIdsBatch(List<Long> videoIds);

    /**
     * 批量查询视频的分类信息（解决N+1问题）
     *
     * @param videoIds 视频ID列表
     * @return 视频ID到分类列表的映射
     */
    public Map<Long, List<Category>> selectCategoriesByVideoIdsBatch(List<Long> videoIds);


    /**
     * 新增视频
     * 
     * @param video 视频
     * @return 结果
     */
    public int insertVideo(Video video);

    /**
     * 修改视频
     * 
     * @param video 视频
     * @return 结果
     */
    public int updateVideo(Video video);

    /**
     * 批量删除视频
     * 
     * @param ids 需要删除的视频主键集合
     * @return 结果
     */
    public int deleteVideoByIds(Long[] ids);

    /**
     * 删除视频信息
     * 
     * @param id 视频主键
     * @return 结果
     */
    public int deleteVideoById(Long id);



    /**
     * 保存视频分类关系
     * 
     * @param videoId 视频ID
     * @param categoryIds 分类ID列表
     * @return 结果
     */
    public int saveVideoCategoryRelations(Long videoId, List<Long> categoryIds);

    /**
     * 保存视频标签关系
     * 
     * @param videoId 视频ID
     * @param tagIds 标签ID列表
     * @return 结果
     */
    public int saveVideoTagRelations(Long videoId, List<Long> tagIds);

    /**
     * 更新视频统计数据
     * 
     * @param video 视频对象
     * @return 结果
     */
    public int updateVideoStatistics(Video video);

    /**
     * 查询所有分类列表（用于下拉选择）
     * 
     * @return 分类集合
     */
    public List<Category> selectAllCategories();

    /**
     * 查询所有标签列表（用于下拉选择）
     * 
     * @return 标签集合
     */
    public List<Tag> selectAllTags();

    /**
     * 分页查询标签列表（用于标签选择弹出层）
     * 
     * @param tag 标签查询条件
     * @return 标签集合
     */
    public List<Tag> selectTagsPage(Tag tag);

    /**
     * 根据ID列表查询标签信息（用于编辑时回显已选标签）
     * 
     * @param tagIds 标签ID列表
     * @return 标签集合
     */
    public List<Tag> selectTagsByIds(List<Long> tagIds);

    /**
     * 根据关键词搜索标签
     * 
     * @param keyword 关键词
     * @return 标签集合
     */
    public List<Tag> searchTags(String keyword);

    /**
     * 根据已选标签推荐相关标签
     * 
     * @param selectedTagIds 已选标签ID列表
     * @return 推荐标签集合
     */
    public List<Tag> recommendTags(List<Long> selectedTagIds);

    /**
     * 创建新标签
     * 
     * @param tag 标签对象
     * @return 结果
     */
    public int insertTag(Tag tag);

    /**
     * 更新视频内容
     * 
     * @param videoId 视频ID
     * @param videoContent 视频内容
     * @return 结果
     */
    public int updateVideoContent(Long videoId, String videoContent);

    /**
     * 根据转码ID查找视频
     * 
     * @param transcodeId 转码ID
     * @return 视频对象
     */
    public Video findByTranscodeId(String transcodeId);

    /**
     * 获取视频的富文本内容（包含动态生成的签名URL）
     * 
     * @param videoId 视频ID
     * @return 处理后的富文本内容
     */
    public String getProcessedVideoContent(Long videoId);

    /**
     * 获取包含URL信息的视频详情
     * 
     * @param id 视频ID
     * @return 包含URL信息的视频DTO
     */
    public com.ruoyi.chigua.dto.VideoWithUrlsDto selectVideoWithUrlsById(Long id);

    /**
     * 生成视频的完整富文本内容
     * 
     * @param videoId 视频ID
     * @return 富文本内容
     */
    public String generateEnhancedVideoContent(Long videoId);

    /**
     * 更新视频的富文本内容（包含播放器和技术信息）
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int updateVideoContentWithUrls(Long videoId);

    /**
     * 批量修改视频状态
     * 
     * @param ids 视频ID数组
     * @param status 状态
     * @return 结果
     */
    public int updateVideoStatus(Long[] ids, Integer status);

    /**
     * 批量设置推荐状态
     * 
     * @param ids 视频ID数组
     * @param isRecommended 是否推荐
     * @return 结果
     */
    public int updateVideoRecommended(Long[] ids, Integer isRecommended);

    /**
     * 批量设置热门状态
     * 
     * @param ids 视频ID数组
     * @param isHot 是否热门
     * @return 结果
     */
    public int updateVideoHot(Long[] ids, Integer isHot);

    /**
     * 批量设置多分类
     * 
     * @param ids 视频ID数组
     * @param categoryIds 分类ID列表
     * @return 结果
     */
    public int batchUpdateVideoCategories(Long[] ids, List<Long> categoryIds);

    /**
     * 批量修改视频主分类
     * 
     * @param ids 视频ID数组
     * @param categoryId 目标分类ID
     * @return 结果
     */
    public int batchUpdateVideoPrimaryCategory(Long[] ids, Long categoryId);

    /**
     * 批量添加标签
     * 
     * @param ids 视频ID数组
     * @param tagIds 标签ID列表
     * @return 结果
     */
    public int batchAddVideoTags(Long[] ids, List<Long> tagIds);

    /**
     * 批量移除标签
     * 
     * @param ids 视频ID数组
     * @param tagIds 标签ID列表
     * @return 结果
     */
    public int batchRemoveVideoTags(Long[] ids, List<Long> tagIds);

    /**
     * 批量替换标签（清空原标签后设置新标签）
     * 
     * @param ids 视频ID数组
     * @param tagIds 新标签ID列表
     * @return 结果
     */
    public int batchReplaceVideoTags(Long[] ids, List<Long> tagIds);

    /**
     * 更新视频富文本中的视频URL签名
     * 
     * @param videoId 视频ID
     * @return 更新结果
     */
    public int updateVideoContentSignatures(Long videoId);

    /**
     * 批量更新视频富文本中的视频URL签名
     * 
     * @param videoIds 视频ID列表
     * @return 更新结果
     */
    public int batchUpdateVideoContentSignatures(List<Long> videoIds);

    /**
     * 根据转码回调数据更新视频富文本内容
     * 根据视频名称模糊查询匹配的视频，在其富文本内容末尾添加视频播放器
     * 
     * @param callbackData 转码回调数据
     * @return 更新结果描述
     */
    public String updateVideoContentByTranscodeCallback(com.ruoyi.chigua.dto.PpvodCallbackDto callbackData);
} 
