package com.ruoyi.chigua.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.Category;
import com.ruoyi.chigua.domain.Tag;
import com.ruoyi.chigua.domain.vo.web.WebVideoListVO;
import org.apache.ibatis.annotations.Param;

/**
 * 视频管理 数据层
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public interface VideoMapper
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
     * 查询视频列表（搜索专用，固定排除短视频）
     *
     * @param video 视频
     * @return 视频集合
     */
    public List<Video> selectVideoListForSearch(Video video);

    /**
     * 搜索专用：计数（固定排除短视频）
     */
    public long countVideoListForSearch(Video video);

    /**
     * 搜索专用：显式分页查询（固定排除短视频）
     */
    public List<Video> selectVideoListForSearchPaged(@Param("video") Video video, @Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 查询视频列表（包含分类和标签信息）
     * 
     * @param video 视频
     * @return 视频集合
     */
    public List<Video> selectVideoListWithRelations(Video video);

    /**
     * 查询分类下可用于排序的视频列表（排除已排序的视频）
     * 
     * @param categoryId 分类ID
     * @param title 视频标题（可选）
     * @param excludeVideoIds 要排除的视频ID列表
     * @return 视频集合
     */
    public List<Video> selectAvailableVideosForSort(@Param("categoryId") Long categoryId, 
                                                   @Param("title") String title,
                                                   @Param("excludeVideoIds") java.util.Set<Long> excludeVideoIds);

    /**
     * 根据标题模糊查询视频列表（用于转码回调匹配）
     * 
     * @param video 视频查询条件（主要使用title和status字段）
     * @return 视频集合
     */
    public List<Video> selectVideoListByTitleLike(Video video);

    /**
     * 根据标题精确查询视频列表（用于回调更新，避免模糊匹配导致的错误）
     * 
     * @param video 视频查询条件（主要使用title和status字段）
     * @return 视频集合
     */
    public List<Video> selectVideoListByTitleExact(Video video);

    /**
     * 根据source_id列表批量查询视频
     * 
     * @param sourceIds source_id列表
     * @return 视频集合（包含id和sourceId字段）
     */
    public List<Video> selectVideosBySourceIds(@Param("sourceIds") List<String> sourceIds);

    /**
     * 根据title列表批量查询视频（精确匹配）
     *
     * @param titles title列表
     * @return 视频集合（包含id、title、sourceId字段）
     */
    public List<Video> selectVideosByTitles(@Param("titles") List<String> titles);

    /**
     * 查询视频列表（包含统计数据）
     * 
     * @param video 视频
     * @return 视频集合
     */
    public List<Video> selectVideoListWithStats(Video video);

    /**
     * Web 精简版视频列表（仅基础字段 + 分类名）
     *
     * @param video 查询条件
     * @return 视频集合（精简字段）
     */
    public List<Video> selectWebVideoListBasic(Video video);

    /**
     * Web 精简版计数（用于分页总数）
     */
    public long countWebVideoListBasic(Video video);

    /**
     * Web 精简版分页（显式 offset/limit，确保第二页正确）
     */
    public List<Video> selectWebVideoListBasicPaged(@Param("video") Video video, @Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 列表专用：分页查询（仅必需字段）
     */
    public List<WebVideoListVO> selectVideoListOnlyPaged(@Param("video") Video video, @Param("offset") int offset, @Param("pageSize") int pageSize, @Param("excludeShort") Boolean excludeShort);

    /**
     * 列表专用：计数查询
     */
    public long countVideoListOnly(@Param("video") Video video, @Param("excludeShort") Boolean excludeShort);

    /**
     * 站点地图专用：分页查询视频（仅普通分类，排除短视频和分页模式分类）
     */
    public List<Video> selectVideoListForSitemap(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 站点地图专用：统计普通分类视频总数
     */
    public long countVideoListForSitemap();

    /**
     * 短视频模式：分页查询基础字段（包含富文本，用于解析首个视频）
     */
    public List<Video> selectShortVideoPage(@Param("categoryId") Long categoryId,
                                            @Param("title") String title,
                                            @Param("tagId") Long tagId,
                                            @Param("tagIds") List<Long> tagIds,
                                            @Param("author") String author,
                                            @Param("authors") List<String> authors,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    /**
     * 短视频模式：计数
     */
    public long countShortVideoPage(@Param("categoryId") Long categoryId,
                                    @Param("title") String title,
                                    @Param("tagId") Long tagId,
                                    @Param("tagIds") List<Long> tagIds,
                                    @Param("author") String author,
                                    @Param("authors") List<String> authors);

    /**
     * 普通分类和首页：分页查询（不过滤 first_video_url）
     */
    public List<Video> selectCategoryVideoPage(@Param("categoryId") Long categoryId,
                                               @Param("offset") int offset,
                                               @Param("pageSize") int pageSize);

    /**
     * 普通分类和首页：计数（不过滤 first_video_url）
     */
    public long countCategoryVideoPage(@Param("categoryId") Long categoryId);

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
     * 删除视频
     * 
     * @param id 视频主键
     * @return 结果
     */
    public int deleteVideoById(Long id);

    /**
     * 批量删除视频
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVideoByIds(Long[] ids);

    /**
     * 查询视频的分类列表
     * 
     * @param videoId 视频ID
     * @return 分类集合
     */
    public List<Category> selectCategoriesByVideoId(Long videoId);

    /**
     * 查询视频的标签列表
     * 
     * @param videoId 视频ID
     * @return 标签集合
     */
    public List<Tag> selectTagsByVideoId(Long videoId);


    /**
     * 插入视频分类关系
     * 
     * @param videoId 视频ID
     * @param categoryId 分类ID
     * @return 结果
     */
    public int insertVideoCategoryRelation(@Param("videoId") Long videoId, @Param("categoryId") Long categoryId);

    /**
     * 插入视频标签关系
     * 
     * @param videoId 视频ID
     * @param tagId 标签ID
     * @return 结果
     */
    public int insertVideoTagRelation(@Param("videoId") Long videoId, @Param("tagId") Long tagId);

    /**
     * 删除视频分类关系
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int deleteVideoCategoryRelations(Long videoId);

    /**
     * 删除视频标签关系
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int deleteVideoTagRelations(Long videoId);

    /**
     * 删除视频的所有标签关系（批量替换标签时使用）
     * 
     * @param videoId 视频ID
     * @return 结果
     */
    public int deleteVideoTagRelationsByVideoId(Long videoId);

    /**
     * 批量更新视频主分类
     * 
     * @param videoIds 视频ID列表
     * @param categoryId 目标分类ID
     * @return 结果
     */
    public int batchUpdateVideoPrimaryCategory(@Param("videoIds") List<Long> videoIds, @Param("categoryId") Long categoryId);

    /**
     * 批量删除视频标签关系
     * 
     * @param videoIds 视频ID列表
     * @return 结果
     */
    public int batchDeleteVideoTagRelations(@Param("videoIds") List<Long> videoIds);

    /**
     * 批量删除视频分类关系
     * 
     * @param videoIds 视频ID列表
     * @return 结果
     */
    public int batchDeleteVideoCategoryRelations(@Param("videoIds") List<Long> videoIds);

    /**
     * 检查视频分类关系是否存在
     * 
     * @param videoId 视频ID
     * @param categoryId 分类ID
     * @return 是否存在
     */
    public boolean checkVideoCategoryRelationExists(@Param("videoId") Long videoId, @Param("categoryId") Long categoryId);

    /**
     * 检查视频标签关系是否存在
     * 
     * @param videoId 视频ID
     * @param tagId 标签ID
     * @return 是否存在
     */
    public boolean checkVideoTagRelationExists(@Param("videoId") Long videoId, @Param("tagId") Long tagId);

    /**
     * 批量插入视频分类关系
     * 
     * @param videoId 视频ID
     * @param categoryIds 分类ID列表
     * @return 结果
     */
    public int batchInsertVideoCategoryRelations(@Param("videoId") Long videoId, @Param("categoryIds") List<Long> categoryIds);

    /**
     * 批量插入视频标签关系
     * 
     * @param videoId 视频ID
     * @param tagIds 标签ID列表
     * @return 结果
     */
    public int batchInsertVideoTagRelations(@Param("videoId") Long videoId, @Param("tagIds") List<Long> tagIds);

    /**
     * 查询视频关联的标签ID列表
     * 
     * @param videoId 视频ID
     * @return 标签ID列表
     */
    public List<Long> selectTagIdsByVideoId(Long videoId);

    /**
     * 更新视频统计数据
     * 
     * @param video 视频对象
     * @return 结果
     */
    public int updateVideoStatistics(Video video);

    /**
     * 根据分类ID查询视频数量
     * 
     * @param categoryId 分类ID
     * @return 视频数量
     */
    public int countVideosByCategoryId(Long categoryId);

    /**
     * 根据标签ID查询视频数量
     * 
     * @param tagId 标签ID
     * @return 视频数量
     */
    public int countVideosByTagId(Long tagId);

    /**
     * 根据标签ID查询视频列表
     * 
     * @param tagId 标签ID
     * @return 视频集合
     */
    public List<Video> selectVideosByTagId(Long tagId);

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
    public List<Tag> selectTagsByIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 搜索标签
     * 
     * @param keyword 关键词
     * @return 标签集合
     */
    public List<Tag> searchTags(@Param("keyword") String keyword);

    /**
     * 根据已选标签推荐相关标签
     * 
     * @param selectedTagIds 已选标签ID列表
     * @return 推荐标签集合
     */
    public List<Tag> selectRecommendedTags(@Param("selectedTagIds") List<Long> selectedTagIds);

    /**
     * 基于分词模糊匹配推荐相关标签
     * 
     * @param selectedTagIds 已选标签ID列表
     * @param keywords 分词关键词列表
     * @return 匹配的标签集合
     */
    public List<Tag> selectTagsByWordSegmentation(@Param("selectedTagIds") List<Long> selectedTagIds, 
                                                  @Param("keywords") List<String> keywords);

    /**
     * 获取热门标签作为备选推荐（排除已选标签）
     * 
     * @param selectedTagIds 已选标签ID列表
     * @return 热门标签集合
     */
    public List<Tag> selectHotTagsExcluding(@Param("selectedTagIds") List<Long> selectedTagIds);

    /**
     * 新增标签
     * 
     * @param tag 标签
     * @return 结果
     */
    public int insertTag(Tag tag);

    /**
     * 根据转码ID查找视频
     * 
     * @param transcodeId 转码ID
     * @return 视频对象
     */
    public Video findByTranscodeId(@Param("transcodeId") String transcodeId);

    /**
     * 优化的相邻视频查询 - 获取上一个视频
     * 
     * @param currentVideoId 当前视频ID
     * @param categoryId 分类ID（可选）
     * @param searchKeyword 搜索关键词（可选）
     * @param sortType 排序类型（可选，暂未使用）
     * @return 上一个视频
     */
    public Video selectPreviousVideoOptimized(@Param("currentVideoId") Long currentVideoId,
                                            @Param("categoryId") Long categoryId,
                                            @Param("searchKeyword") String searchKeyword,
                                            @Param("sortType") String sortType);

    /**
     * 优化的相邻视频查询 - 获取下一个视频
     * 
     * @param currentVideoId 当前视频ID
     * @param categoryId 分类ID（可选）
     * @param searchKeyword 搜索关键词（可选）
     * @param sortType 排序类型（可选，暂未使用）
     * @return 下一个视频
     */
    public Video selectNextVideoOptimized(@Param("currentVideoId") Long currentVideoId,
                                        @Param("categoryId") Long categoryId,
                                        @Param("searchKeyword") String searchKeyword,
                                        @Param("sortType") String sortType);

    /**
     * 批量查询视频的标签信息（解决N+1问题）
     * 
     * @param videoIds 视频ID列表
     * @return 视频ID到标签列表的映射
     */
    public List<Map<String, Object>> selectTagsByVideoIdsBatch(@Param("videoIds") List<Long> videoIds);

    /**
     * 批量查询视频的分类信息（解决N+1问题）
     * 
     * @param videoIds 视频ID列表
     * @return 视频ID到分类列表的映射
     */
    public List<Map<String, Object>> selectCategoriesByVideoIdsBatch(@Param("videoIds") List<Long> videoIds);

    /**
     * 获取分类下所有去重的标签
     * 
     * @param categoryId 分类ID
     * @return 标签列表
     */
    public List<Tag> selectDistinctTagsByCategory(@Param("categoryId") Long categoryId);

    /**
     * 获取分类下所有去重的作者
     * 
     * @param categoryId 分类ID
     * @return 作者列表（字符串列表）
     */
    public List<String> selectDistinctAuthorsByCategory(@Param("categoryId") Long categoryId);

    // ========== SEO关键词模块专用方法 ==========
    
    /**
     * 根据关键词搜索视频（精确匹配）
     * 
     * @param params 参数（keyword, pageNum, pageSize）
     * @return 视频列表
     */
    public List<Video> searchByKeyword(@Param("params") Map<String, Object> params);

    /**
     * 统计关键词匹配的视频数量
     * 
     * @param keyword 关键词
     * @return 数量
     */
    public int countByKeyword(@Param("keyword") String keyword);

    /**
     * 根据多个关键词搜索视频（模糊匹配）
     * 
     * @param params 参数（keywords, pageNum, pageSize）
     * @return 视频列表
     */
    public List<Video> searchByKeywords(@Param("params") Map<String, Object> params);

    /**
     * 统计多关键词匹配的视频数量
     * 
     * @param keywords 关键词列表
     * @return 数量
     */
    public int countByKeywords(@Param("keywords") List<String> keywords);

    /**
     * 查询热门视频（用于兜底推荐）
     * 
     * @param limit 返回数量
     * @return 视频列表
     */
    public List<Video> selectHotVideos(@Param("limit") Integer limit);
} 