package com.ruoyi.chigua.service.web;

import java.util.List;
import com.ruoyi.chigua.domain.Video;
import com.ruoyi.chigua.domain.vo.web.WebCategoryVO;
import com.ruoyi.chigua.domain.vo.web.WebVideoVO;
import com.ruoyi.chigua.domain.vo.web.WebVideoListVO;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Web分类Service接口
 * 
 * @author chigua
 * @date 2024-12-20
 */
public interface IWebCategoryService 
{
    /**
     * 查询Web分类列表
     * 
     * @return Web分类列表
     */
    public com.ruoyi.chigua.domain.vo.web.WebCategoryListResponse selectWebCategoryList();

    /**
     * 获取分类列表版本号
     */
    public String getCategoryListVersion();

    /**
     * 根据分类ID查询Web分类
     * 
     * @param categoryId 分类ID
     * @return Web分类
     */
    public WebCategoryVO selectWebCategoryById(Long categoryId);

    /**
     * 根据分类获取视频列表
     * 
     * @param categoryId 分类ID，为null时查询所有分类
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 视频列表
     */
    public TableDataInfo selectWebVideoList(Long categoryId, Integer pageNum, Integer pageSize);

    /**
     * 根据分类获取视频列表（优化版，仅必需字段）
     * 
     * @param categoryId 分类ID，为null时查询所有分类
     * @param title 标题（可选，用于模糊搜索）
     * @param tagId 标签ID（可选，用于筛选，兼容旧接口）
     * @param tagIds 标签ID列表（可选，用于多标签筛选，AND关系）
     * @param author 作者（可选，用于筛选，兼容旧接口）
     * @param authors 作者列表（可选，用于多作者筛选，OR关系）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 优化版视频列表
     */
    public TableDataInfo selectWebVideoListOptimized(Long categoryId, String title, Long tagId, List<Long> tagIds, String author, List<String> authors, Integer pageNum, Integer pageSize);

    /**
     * 查询视频详情
     * 
     * @param videoId 视频ID
     * @return 视频详情
     */
    public WebVideoVO selectWebVideoDetail(Long videoId);

    /**
     * 查询热门推荐视频列表
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 热门推荐视频列表
     */
    public TableDataInfo selectWebHotRecommendedVideoList(Integer pageNum, Integer pageSize);

    /**
     * 短视频模式：按分类或标签查询短视频列表（仅短视频展示需要）
     * @param categoryId 分类ID（可选）
     * @param title 标题（可选，用于模糊搜索）
     * @param tagId 标签ID（可选，兼容旧接口）
     * @param tagIds 标签ID列表（可选，用于多标签筛选，AND关系）
     * @param author 作者（可选，用于筛选，兼容旧接口）
     * @param authors 作者列表（可选，用于多作者筛选，OR关系）
     * @param pageNum 页码
     * @param pageSize 每页条数
     */
    public TableDataInfo selectWebShortVideoList(Long categoryId, String title, Long tagId, List<Long> tagIds, String author, List<String> authors, Integer pageNum, Integer pageSize);

    /**
     * 获取上一篇视频
     * 
     * @param currentVideoId 当前视频ID
     * @return 上一篇视频信息
     */
    public WebVideoVO.AdjacentVideoInfo selectPreviousVideo(Long currentVideoId);

    /**
     * 获取下一篇视频
     * 
     * @param currentVideoId 当前视频ID
     * @return 下一篇视频信息
     */
    public WebVideoVO.AdjacentVideoInfo selectNextVideo(Long currentVideoId);

    /**
     * 获取上一篇视频（支持查询上下文）
     * 
     * @param currentVideoId 当前视频ID
     * @param categoryId 分类ID（可选）
     * @param searchKeyword 搜索关键词（可选）
     * @param sortType 排序类型（可选）
     * @return 上一篇视频信息
     */
    public WebVideoVO.AdjacentVideoInfo selectPreviousVideo(Long currentVideoId, Long categoryId, String searchKeyword, String sortType);

    /**
     * 获取下一篇视频（支持查询上下文）
     * 
     * @param currentVideoId 当前视频ID
     * @param categoryId 分类ID（可选）
     * @param searchKeyword 搜索关键词（可选）
     * @param sortType 排序类型（可选）
     * @return 下一篇视频信息
     */
    public WebVideoVO.AdjacentVideoInfo selectNextVideo(Long currentVideoId, Long categoryId, String searchKeyword, String sortType);

    /**
     * 获取相邻视频（上一篇和下一篇）
     * 
     * @param currentVideoId 当前视频ID
     * @return 相邻视频信息
     */
    public WebVideoVO.AdjacentVideosVO selectAdjacentVideos(Long currentVideoId);

    /**
     * 获取分类下所有去重的标签
     * 
     * @param categoryId 分类ID
     * @return 标签列表
     */
    public List<com.ruoyi.chigua.domain.Tag> selectDistinctTagsByCategory(Long categoryId);

    /**
     * 获取分类下所有去重的作者
     * 
     * @param categoryId 分类ID
     * @return 作者列表（字符串列表）
     */
    public List<String> selectDistinctAuthorsByCategory(Long categoryId);

    /**
     * 获取缓存的视频基础数据（不含签名URL）
     * ✅ 带缓存：避免重复查询数据库
     * 
     * @param categoryId 分类ID
     * @param title 标题
     * @param tagId 标签ID
     * @param tagIds 标签ID列表
     * @param author 作者
     * @param authors 作者列表
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 视频列表
     */
    public List<Video> getCachedBaseVideos(Long categoryId, String title, Long tagId, List<Long> tagIds, 
                                           String author, List<String> authors, int offset, int pageSize);

    /**
     * 获取缓存的视频总数
     * ✅ 带缓存：避免重复统计
     * 
     * @param categoryId 分类ID
     * @param title 标题
     * @param tagId 标签ID
     * @param tagIds 标签ID列表
     * @param author 作者
     * @param authors 作者列表
     * @return 总数
     */
    public long getCachedVideoTotal(Long categoryId, String title, Long tagId, List<Long> tagIds, 
                                    String author, List<String> authors);

    /**
     * 短视频基础列表缓存（不含签名URL，仅DB实体）
     * 用于支持"基础数据缓存 + 出口实时签名"模式，避免跨region缓存污染
     */
    public List<Video> getCachedShortVideoBaseList(Long categoryId, String title, Long tagId, List<Long> tagIds,
                                                    String author, List<String> authors, Integer pageNum, Integer pageSize);

    /**
     * 短视频总数缓存（独立 key，不依赖分页参数）
     */
    public long getCachedShortVideoTotal(Long categoryId, String title, Long tagId, List<Long> tagIds,
                                          String author, List<String> authors);
} 
