package com.ruoyi.chigua.mapper;

import java.util.List;
import com.ruoyi.chigua.domain.TgPost;
import com.ruoyi.chigua.domain.TgMedia;
import org.apache.ibatis.annotations.Param;

/**
 * TG 帖子 数据层
 */
public interface TgPostMapper
{
    // ─── 查询（Web 展示用） ─────────────────────────────────────

    TgPost selectTgPostById(Long id);

    List<TgPost> selectTgPostListByCategoryId(@Param("categoryId") Integer categoryId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit,
                                               @Param("caption") String caption);

    int countTgPostsByCategoryId(@Param("categoryId") Integer categoryId,
                                  @Param("caption") String caption);

    List<TgMedia> selectTgMediaByPostIds(@Param("postIds") List<Long> postIds);

    List<TgMedia> selectTgMediaByPostId(@Param("postId") Long postId);

    int incrementViews(@Param("id") Long id);

    // ─── 站点地图（SEO） ─────────────────────────────────────────

    /**
     * 站点地图：统计所有 Telegram 分类下 status=1 的帖子总数
     */
    long countTgPostsForSitemap();

    /**
     * 站点地图：分页查询所有 Telegram 分类下 status=1 的帖子（仅取 SEO 必需字段）
     * 联表过滤 categories.is_telegram=1 AND categories.status=1
     */
    List<TgPost> selectTgPostsForSitemap(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 站点地图：查询所有启用的 Telegram 分类（用于生成分类落地页 URL）
     */
    List<com.ruoyi.chigua.domain.Category> selectTgCategoriesForSitemap();

    // ─── 后台管理 ────────────────────────────────────────────────

    /** 列表查询（支持 categoryId / status 过滤，配合 PageHelper） */
    List<TgPost> selectTgPostList(TgPost tgPost);

    int updateTgPost(TgPost tgPost);

    int updateTgMediaRecommend(@Param("id") Long id, @Param("isRecommend") Integer isRecommend);

    int deleteTgPostById(Long id);
    int deleteTgPostByIds(Long[] ids);

    int deleteTgMediaByPostId(Long postId);
    int deleteTgMediaByPostIds(Long[] postIds);
    int deleteTgMediaById(Long id);

    // ─── 同步工具专用 ────────────────────────────────────────────

    /**
     * 根据 source_id 查询帖子 ID，用于去重判断。
     * 存在返回帖子 id，不存在返回 null。
     */
    Long selectIdBySourceId(@Param("sourceId") String sourceId);

    /**
     * 插入帖子，执行后 post.id 自动回填。
     */
    int insertTgPost(TgPost post);

    /**
     * 批量插入媒体列表。
     */
    int insertTgMediaBatch(@Param("list") List<TgMedia> list);
}
