package com.ruoyi.chigua.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.chigua.domain.PageConfig;
import com.ruoyi.chigua.domain.PageConfigVersion;
import com.ruoyi.chigua.mapper.PageConfigMapper;
import com.ruoyi.chigua.mapper.PageConfigVersionMapper;
import com.ruoyi.chigua.service.IPageConfigService;
import com.ruoyi.chigua.service.CacheRefreshService;
import com.ruoyi.chigua.service.WebRichTextProcessorService;
import org.springframework.context.annotation.Lazy;
import com.ruoyi.chigua.config.ChiguaProperties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 页面配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-01-01
 */
@Service
public class PageConfigServiceImpl implements IPageConfigService 
{
    private static final Logger logger = LoggerFactory.getLogger(PageConfigServiceImpl.class);
    
    @Autowired
    private PageConfigMapper pageConfigMapper;

    @Autowired
    private PageConfigVersionMapper pageConfigVersionMapper;

    @Autowired
    private CacheRefreshService cacheRefreshService;

    @Autowired
    private WebRichTextProcessorService webRichTextProcessorService;

    @Autowired
    private ChiguaProperties chiguaProperties;

    /**
     * 自我注入：用于在内部方法调用时仍然走 Spring AOP 代理，
     * 这样 self.getAllActivePageConfigs() 才会触发 @Cacheable 基础数据缓存。
     */
    @Autowired
    @Lazy
    private IPageConfigService self;

    /**
     * 查询页面配置
     * 
     * @param configId 页面配置主键
     * @return 页面配置
     */
    @Override
    public PageConfig selectPageConfigById(Long configId)
    {
        return pageConfigMapper.selectPageConfigById(configId);
    }

    /**
     * 根据配置键值查询页面配置
     * 
     * @param configKey 配置键值
     * @return 页面配置
     */
    @Override
    public PageConfig selectPageConfigByKey(String configKey)
    {
        return pageConfigMapper.selectPageConfigByKey(configKey);
    }

    /**
     * 查询页面配置列表
     * 
     * @param pageConfig 页面配置
     * @return 页面配置
     */
    @Override
    public List<PageConfig> selectPageConfigList(PageConfig pageConfig)
    {
        return pageConfigMapper.selectPageConfigList(pageConfig);
    }

    /**
     * 根据配置类型查询页面配置列表
     * 
     * @param configType 配置类型
     * @return 页面配置集合
     */
    @Override
    @Cacheable(value = "pageConfigList", key = "#configType", unless = "#result == null")
    public List<PageConfig> selectPageConfigListByType(String configType)
    {
        return pageConfigMapper.selectPageConfigListByType(configType);
    }

    /**
     * 根据配置分类查询页面配置列表
     * 
     * @param configCategory 配置分类
     * @return 页面配置集合
     */
    @Override
    public List<PageConfig> selectPageConfigListByCategory(String configCategory)
    {
        return pageConfigMapper.selectPageConfigListByCategory(configCategory);
    }

    /**
     * 获取所有有效的页面配置（用于前端缓存）
     * 使用Spring Cache + 版本控制
     * 
     * @return 页面配置Map，key为configKey，value为PageConfig
     */
    @Override
    @Cacheable(value = "pageConfig", key = "'all_active_configs'", unless = "#result == null")
    public Map<String, PageConfig> getAllActivePageConfigs()
    {
        logger.info("🎯 执行数据库查询 getAllActivePageConfigs");
        
        // 从数据库获取
        List<PageConfig> configList = pageConfigMapper.selectAllActivePageConfigs();
        Map<String, PageConfig> configMap = configList.stream()
                .collect(Collectors.toMap(PageConfig::getConfigKey, config -> config));

        logger.info("✅ 页面配置查询完成: {}个配置项", configMap.size());
        return configMap;
    }

    /**
     * 获取所有有效的页面配置（富文本签名后的完整URL）
     *
     * ⚠️ 严格"方案 B"：本方法不再加 @Cacheable，避免把"含 CDN 域名的签名 URL"
     *   缓存到 Redis 后造成跨 region（CN/OS）污染。
     *
     * 数据获取链路：
     *   1) 通过 self.getAllActivePageConfigs() 取上层"原始 PageConfig 基础缓存"
     *      （仅含数据库存储的相对路径，与 region 无关）。
     *   2) 出口处对每条 PageConfig 做深拷贝（不污染基础缓存对象），
     *      调用 webRichTextProcessorService 现场拼接签名 URL。
     *   3) chiguaUrlService.generateUrl 内部按当前请求 IP 选择 CDN 域名，
     *      天然按 region 区分，无需额外 cache key 后缀。
     */
    @Override
    public Map<String, PageConfig> getAllActivePageConfigsSigned() {
        // 1) 走上层基础缓存（pageConfig::all_active_configs，缓存的是无签名 URL 的原始实体）
        Map<String, PageConfig> baseMap = self.getAllActivePageConfigs();
        if (baseMap == null || baseMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, PageConfig> configMap = new HashMap<>(baseMap.size());

        // 2) 深拷贝 + 出口实时签名
        for (PageConfig cfg : baseMap.values()) {
            if (cfg == null) continue;
            PageConfig copy = clonePageConfig(cfg);

            // 富文本签名：将存库路径转换为完整签名 URL（实时按当前请求 IP 决定 CDN 域名）
            String rich = cfg.getRichContent();
            if (org.springframework.util.StringUtils.hasText(rich)) {
                try {
                    String processed = webRichTextProcessorService.processRichTextForAdminEditor(rich);
                    copy.setRichContent(processed);
                } catch (Exception e) {
                    copy.setRichContent(rich);
                }
            }

            configMap.put(copy.getConfigKey(), copy);
        }

        return configMap;
    }

    /**
     * 深拷贝 PageConfig，避免修改"上层基础数据缓存"中的对象（防止缓存污染）
     */
    private PageConfig clonePageConfig(PageConfig cfg) {
        PageConfig copy = new PageConfig();
        copy.setConfigId(cfg.getConfigId());
        copy.setConfigKey(cfg.getConfigKey());
        copy.setConfigName(cfg.getConfigName());
        copy.setConfigType(cfg.getConfigType());
        copy.setConfigCategory(cfg.getConfigCategory());
        copy.setStatus(cfg.getStatus());
        copy.setSortOrder(cfg.getSortOrder());
        copy.setCreateBy(cfg.getCreateBy());
        copy.setCreateTime(cfg.getCreateTime());
        copy.setUpdateBy(cfg.getUpdateBy());
        copy.setUpdateTime(cfg.getUpdateTime());
        copy.setRemark(cfg.getRemark());
        copy.setBasicContent(cfg.getBasicContent());
        copy.setJumpUrl(cfg.getJumpUrl());
        copy.setRichContent(cfg.getRichContent());
        return copy;
    }

    /**
     * 获取当前配置版本号
     * 
     * @return 版本号
     */
    @Override
    @Cacheable(value = "pageConfigVersion", key = "'current_version'", unless = "#result == null")
    public String getCurrentConfigVersion()
    {
        logger.info("🎯 执行数据库查询 getCurrentConfigVersion");
        PageConfigVersion version = pageConfigVersionMapper.selectLatestVersion();
        String versionHash = version != null ? version.getVersionHash() : "default";
        logger.info("✅ 页面配置版本查询完成: {}", versionHash);
        return versionHash;
    }

    /**
     * 新增页面配置
     * 
     * @param pageConfig 页面配置
     * @return 结果
     */
    @Override
    @Transactional
    @CacheEvict(value = {"pageConfig", "pageConfigVersion", "pageConfigList", "pageConfigSigned"}, allEntries = true)
    public int insertPageConfig(PageConfig pageConfig)
    {
        // 入库前：将富文本中的完整URL规范化为路径存储
        if (pageConfig != null && org.springframework.util.StringUtils.hasText(pageConfig.getRichContent())) {
            pageConfig.setRichContent(normalizeRichTextToPath(pageConfig.getRichContent()));
        }

        int result = pageConfigMapper.insertPageConfig(pageConfig);
        if (result > 0) {
            // 刷新Spring Cache缓存
            cacheRefreshService.refreshPageConfigCache();
        }
        return result;
    }

    /**
     * 修改页面配置
     * 
     * @param pageConfig 页面配置
     * @return 结果
     */
    @Override
    @Transactional
    @CacheEvict(value = {"pageConfig", "pageConfigVersion", "pageConfigList", "pageConfigSigned"}, allEntries = true)
    public int updatePageConfig(PageConfig pageConfig)
    {
        // 入库前：将富文本中的完整URL规范化为路径存储
        if (pageConfig != null && org.springframework.util.StringUtils.hasText(pageConfig.getRichContent())) {
            pageConfig.setRichContent(normalizeRichTextToPath(pageConfig.getRichContent()));
        }

        int result = pageConfigMapper.updatePageConfig(pageConfig);
        if (result > 0) {
            // 刷新Spring Cache缓存
            cacheRefreshService.refreshPageConfigCache();
        }
        return result;
    }

    /**
     * 批量删除页面配置
     * 
     * @param configIds 需要删除的页面配置主键
     * @return 结果
     */
    @Override
    @Transactional
    @CacheEvict(value = {"pageConfig", "pageConfigVersion", "pageConfigList", "pageConfigSigned"}, allEntries = true)
    public int deletePageConfigByIds(Long[] configIds)
    {
        int result = pageConfigMapper.deletePageConfigByIds(configIds);
        if (result > 0) {
            // 刷新Spring Cache缓存
            cacheRefreshService.refreshPageConfigCache();
        }
        return result;
    }

    /**
     * 删除页面配置信息
     * 
     * @param configId 页面配置主键
     * @return 结果
     */
    @Override
    @Transactional
    @CacheEvict(value = {"pageConfig", "pageConfigVersion", "pageConfigList", "pageConfigSigned"}, allEntries = true)
    public int deletePageConfigById(Long configId)
    {
        int result = pageConfigMapper.deletePageConfigById(configId);
        if (result > 0) {
            // 刷新Spring Cache缓存
            cacheRefreshService.refreshPageConfigCache();
        }
        return result;
    }

    /**
     * 校验配置键值是否唯一
     * 
     * @param pageConfig 页面配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(PageConfig pageConfig)
    {
        Long configId = StringUtils.isNull(pageConfig.getConfigId()) ? -1L : pageConfig.getConfigId();
        PageConfig config = pageConfigMapper.checkConfigKeyUnique(pageConfig.getConfigKey());
        if (StringUtils.isNotNull(config) && config.getConfigId().longValue() != configId.longValue())
        {
            return false;
        }
        return true;
    }

    /**
     * 强制刷新配置缓存
     */
    @Override
    public void refreshConfigCache()
    {
        // 使用版本控制刷新缓存
        cacheRefreshService.refreshPageConfigCache();
        getAllActivePageConfigs(); // 重新加载缓存
    }

    /**
     * 将富文本中的完整URL归一化为路径存储
     * 规则：
     * - 仅处理<img>、<video poster>、<source src>中的资源
     * - 命中主域名的完整URL，提取其路径作为入库存储
     * - 对相对路径保持不变
     */
    private String normalizeRichTextToPath(String html) {
        try {
            if (!org.springframework.util.StringUtils.hasText(html)) return html;

            String mainDomain = chiguaProperties.getDomains().getMain();
            String fallbackDomain = chiguaProperties.getDomains().getFallback();
            Set<String> allowedHosts = new HashSet<>();
            try { allowedHosts.add(new java.net.URL(mainDomain).getHost()); } catch (Exception ex) { allowedHosts.add(mainDomain.replaceFirst("https?://", "")); }
            try { allowedHosts.add(new java.net.URL(fallbackDomain).getHost()); } catch (Exception ex) { allowedHosts.add(fallbackDomain.replaceFirst("https?://", "")); }

            String processed = html;

            // img src - double quotes
            processed = replaceAttrWithPath(processed, Pattern.compile("<img([^>]*?)src=\\\"(https?://[^\\\"]+)\\\"([^>]*)>", Pattern.CASE_INSENSITIVE), allowedHosts, "img", "src");
            // img src - single quotes
            processed = replaceAttrWithPath(processed, Pattern.compile("<img([^>]*?)src='(https?://[^']+)'([^>]*)>", Pattern.CASE_INSENSITIVE), allowedHosts, "img", "src");

            // video poster
            processed = replaceAttrWithPath(processed, Pattern.compile("<video([^>]*?)poster=\\\"(https?://[^\\\"]+)\\\"([^>]*)>", Pattern.CASE_INSENSITIVE), allowedHosts, "video", "poster");
            processed = replaceAttrWithPath(processed, Pattern.compile("<video([^>]*?)poster='(https?://[^']+)'([^>]*)>", Pattern.CASE_INSENSITIVE), allowedHosts, "video", "poster");

            // source src
            processed = replaceAttrWithPath(processed, Pattern.compile("<source([^>]*?)src=\\\"(https?://[^\\\"]+)\\\"([^>]*)>", Pattern.CASE_INSENSITIVE), allowedHosts, "source", "src");
            processed = replaceAttrWithPath(processed, Pattern.compile("<source([^>]*?)src='(https?://[^']+)'([^>]*)>", Pattern.CASE_INSENSITIVE), allowedHosts, "source", "src");

            return processed;
        } catch (Exception e) {
            return html;
        }
    }

    private String replaceAttrWithPath(String html, Pattern pattern, Set<String> allowedHosts, String tagName, String attrName) {
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String pre = matcher.group(1);
            String url = matcher.group(2);
            String post = matcher.group(3);
            String path = extractPathIfMatch(url, allowedHosts);
            if (path == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                String replacement = "<" + tagName + pre + attrName + "=\"" + path + "\"" + post + ">";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String extractPathIfMatch(String url, Set<String> allowedHosts) {
        try {
            if (url == null || !url.startsWith("http")) return null;
            java.net.URL u = new java.net.URL(url);
            String host = u.getHost();
            boolean hostAllowed = allowedHosts.contains(host) || host.endsWith(".workers.dev");
            if (!hostAllowed) return null;

            // 优先使用查询参数 key 作为资源路径
            String query = u.getQuery();
            if (query != null && !query.isEmpty()) {
                String[] parts = query.split("&");
                for (String p : parts) {
                    int eq = p.indexOf('=');
                    String name = eq > 0 ? p.substring(0, eq) : p;
                    if ("key".equalsIgnoreCase(name)) {
                        String val = eq > 0 ? p.substring(eq + 1) : "";
                        String decoded = java.net.URLDecoder.decode(val, "UTF-8");
                        if (decoded != null && !decoded.isEmpty()) {
                            String normalized = decoded.startsWith("/") ? decoded : "/" + decoded;
                            if (normalized.startsWith("/files/")) {
                                normalized = normalized.substring("/files".length());
                            }
                            return normalized;
                        }
                    }
                }
            }

            // 其次退回到 URL 的路径，并做 URL 解码
            String rawPath = u.getPath();
            if (rawPath == null) return null;
            String decodedPath = java.net.URLDecoder.decode(rawPath, "UTF-8");
            if (decodedPath.startsWith("/files/")) {
                decodedPath = decodedPath.substring("/files".length());
            }
            return decodedPath;
        } catch (Exception e) {
            return null;
        }
    }


}