package com.ruoyi.chigua.service.web.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.ruoyi.chigua.domain.TgPost;
import com.ruoyi.chigua.domain.TgMedia;
import com.ruoyi.chigua.domain.vo.web.WebTgPostVO;
import com.ruoyi.chigua.domain.vo.web.WebTgMediaVO;
import com.ruoyi.chigua.domain.vo.web.WebTgPageResult;
import com.ruoyi.chigua.mapper.TgPostMapper;
import com.ruoyi.chigua.service.ChiguaUrlService;
import com.ruoyi.chigua.service.ChiguaUrlService.ResourceType;
import com.ruoyi.chigua.service.web.IWebTgPostService;

/**
 * Telegram 帖子 Web 服务实现
 */
@Service
public class WebTgPostServiceImpl implements IWebTgPostService
{
    @Autowired
    private TgPostMapper tgPostMapper;

    @Autowired
    private ChiguaUrlService chiguaUrlService;

    @Override
    public WebTgPageResult selectTgPostPage(Integer categoryId, int page, int size, String caption)
    {
        int offset = (page - 1) * size;
        List<TgPost> posts = tgPostMapper.selectTgPostListByCategoryId(categoryId, offset, size, caption);
        int total = tgPostMapper.countTgPostsByCategoryId(categoryId, caption);

        if (posts.isEmpty()) {
            return new WebTgPageResult(new ArrayList<>(), total, page, size);
        }

        // 批量查询媒体，避免 N+1
        List<Long> postIds = posts.stream().map(TgPost::getId).collect(Collectors.toList());
        List<TgMedia> allMedia = tgPostMapper.selectTgMediaByPostIds(postIds);

        Map<Long, List<TgMedia>> mediaByPost = allMedia.stream()
                .collect(Collectors.groupingBy(TgMedia::getPostId));

        List<WebTgPostVO> voList = posts.stream()
                .map(post -> toVO(post, mediaByPost.getOrDefault(post.getId(), new ArrayList<>())))
                .collect(Collectors.toList());

        return new WebTgPageResult(voList, total, page, size);
    }

    @Override
    public WebTgPostVO selectTgPostById(Long id)
    {
        TgPost post = tgPostMapper.selectTgPostById(id);
        if (post == null) return null;
        List<TgMedia> media = tgPostMapper.selectTgMediaByPostId(id);
        return toVO(post, media);
    }

    private WebTgPostVO toVO(TgPost post, List<TgMedia> mediaList)
    {
        WebTgPostVO vo = new WebTgPostVO();
        vo.setId(post.getId());
        vo.setCaption(post.getCaption());
        vo.setPostDate(post.getPostDate());
        vo.setMediaCount(post.getMediaCount());
        vo.setPhotoCount(post.getPhotoCount());
        vo.setVideoCount(post.getVideoCount());
        vo.setViews(post.getViews());
        vo.setIsTop(post.getIsTop());

        List<WebTgMediaVO> mediaVOs = mediaList.stream()
                .map(this::toMediaVO)
                .collect(Collectors.toList());
        vo.setMedia(mediaVOs);
        return vo;
    }

    private WebTgMediaVO toMediaVO(TgMedia m)
    {
        WebTgMediaVO vo = new WebTgMediaVO();
        vo.setId(m.getId());
        vo.setMediaType(m.getMediaType());
        vo.setWidth(m.getWidth());
        vo.setHeight(m.getHeight());
        vo.setFileSize(m.getFileSize());
        vo.setMimeType(m.getMimeType());
        vo.setDuration(m.getDuration());
        vo.setSupportsStreaming(m.getSupportsStreaming());
        vo.setThumbWidth(m.getThumbWidth());
        vo.setThumbHeight(m.getThumbHeight());
        vo.setSortOrder(m.getSortOrder());
        vo.setIsRecommend(m.getIsRecommend());

        // 生成带签名的访问 URL（与其他分类保持一致）
        // 图片：IMAGE 类型（不加 decrypt=true，由前端 JS 解密）
        // 视频：STREAM 类型（m3u8）
        if (StringUtils.hasText(m.getLocalUrl())) {
            String rawUrl = m.getLocalUrl();
            ResourceType type = "video".equals(m.getMediaType())
                    ? ResourceType.STREAM
                    : ResourceType.IMAGE;
            // addDecryptParam=false：前端用 JS 解密，与 generateCoverUrlForWeb 保持一致
            vo.setLocalUrl(chiguaUrlService.generateUrl(rawUrl, type, false));
        }

        // 封面图（视频用）
        if (StringUtils.hasText(m.getThumbUrl())) {
            vo.setThumbUrl(chiguaUrlService.generateUrl(m.getThumbUrl(), ResourceType.THUMBNAIL, false));
        }

        // 首帧图片（视频用）
        if (StringUtils.hasText(m.getFirstFrameUrl())) {
            vo.setFirstFrameUrl(chiguaUrlService.generateUrl(m.getFirstFrameUrl(), ResourceType.THUMBNAIL, false));
        }

        return vo;
    }
}
