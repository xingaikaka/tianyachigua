package com.ruoyi.chigua.utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 分词工具类
 * 提供简单的中文分词功能用于标签推荐
 * 
 * @author ruoyi
 * @date 2024-01-01
 */
public class WordSegmentationUtil {
    
    /** 中文字符正则表达式 */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]+");
    
    /** 英文单词正则表达式 */
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");
    
    /** 数字正则表达式 */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    
    /** 常见停用词（针对成人内容标签优化） */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "和", "与", "或", "及", "等", "也", "都", "很", "更", "最",
        "一个", "这个", "那个", "什么", "怎么", "为什么", "可以", "能够", "应该",
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"
    ));
    
    /** 最小词长度 */
    private static final int MIN_WORD_LENGTH = 2;
    
    /** 常见地名标签（优先完整匹配） */
    private static final Set<String> LOCATION_TAGS = new HashSet<>(Arrays.asList(
        "上海", "北京", "广州", "深圳", "杭州", "南京", "武汉", "成都", "重庆", "天津",
        "河南", "山东", "江苏", "浙江", "广东", "四川", "湖北", "湖南", "河北", "山西",
        "台湾", "香港", "澳门", "台北", "高雄"
    ));
    
    /** 常见平台标签（优先完整匹配） */
    private static final Set<String> PLATFORM_TAGS = new HashSet<>(Arrays.asList(
        "抖音", "快手", "微博", "小红书", "B站", "知乎", "贴吧",
        "onlyfans", "pornhub", "youtube", "tiktok", "instagram",
        "秀人网", "黑料网"
    ));
    
    /** 常见角色标签（优先完整匹配） */
    private static final Set<String> ROLE_TAGS = new HashSet<>(Arrays.asList(
        "学生妹", "女老师", "空姐", "护士", "秘书", "女神", "嫩模", "主播", "女主播",
        "少妇", "人妻", "御姐", "萝莉", "女大学生", "体育生", "舞蹈生", "幼师",
        "女技师", "女同事", "反差学生妹", "精神小妹","母狗","反差","sm","3p","乱伦",
        "淫荡","淫妻","宿舍","高中","裸聊","自慰","福利","约炮","御姐","极品",
        "酒店","清纯","探花","猎奇","重口","调教","网调","丝袜","学生","口交","口爆",
        "巨乳","萝莉"
    ));
    
    /**
     * 对标签名称进行分词（针对成人内容标签优化）
     * 
     * @param tagName 标签名称
     * @return 分词结果列表
     */
    public static List<String> segmentTagName(String tagName) {
        if (!StringUtils.hasText(tagName)) {
            return new ArrayList<>();
        }
        
        List<String> keywords = new ArrayList<>();
        String cleanName = tagName.trim();
        
        // 1. 优先匹配完整的特殊标签（地名、平台、角色）
        extractSpecialTags(cleanName, keywords);
        
        // 2. 提取中文词组（针对成人内容优化）
        extractChineseWordsOptimized(cleanName, keywords);
        
        // 3. 提取英文单词（平台名、专有名词）
        extractEnglishWords(cleanName, keywords);
        
        // 4. 提取数字
        extractNumbers(cleanName, keywords);
        
        // 5. 按特殊分隔符分割
        extractByDelimiters(cleanName, keywords);
        
        // 6. 去重、去停用词、过滤长度
        return keywords.stream()
            .distinct()
            .filter(word -> word.length() >= MIN_WORD_LENGTH)
            .filter(word -> !STOP_WORDS.contains(word.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * 从多个标签名称中提取所有关键词
     * 
     * @param tagNames 标签名称列表
     * @return 合并后的关键词列表
     */
    public static List<String> segmentMultipleTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        return tagNames.stream()
            .filter(StringUtils::hasText)
            .flatMap(tagName -> segmentTagName(tagName).stream())
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * 优先匹配特殊标签（地名、平台、角色）
     */
    private static void extractSpecialTags(String text, List<String> keywords) {
        // 匹配地名标签
        for (String location : LOCATION_TAGS) {
            if (text.contains(location)) {
                keywords.add(location);
            }
        }
        
        // 匹配平台标签
        for (String platform : PLATFORM_TAGS) {
            if (text.toLowerCase().contains(platform.toLowerCase())) {
                keywords.add(platform);
            }
        }
        
        // 匹配角色标签
        for (String role : ROLE_TAGS) {
            if (text.contains(role)) {
                keywords.add(role);
            }
        }
    }
    
    /**
     * 优化的中文词组提取（针对成人内容标签）
     */
    private static void extractChineseWordsOptimized(String text, List<String> keywords) {
        java.util.regex.Matcher matcher = CHINESE_PATTERN.matcher(text);
        while (matcher.find()) {
            String chineseText = matcher.group();
            
            // 添加完整的中文字符串（如果长度合适）
            if (chineseText.length() >= MIN_WORD_LENGTH && chineseText.length() <= 8) {
                keywords.add(chineseText);
            }
            
            // 对于较长的文本，提取有意义的子串
            if (chineseText.length() > 2) {
                // 提取2-3字符的核心词汇
                for (int i = 0; i < chineseText.length(); i++) {
                    for (int len = 2; len <= Math.min(3, chineseText.length() - i); len++) {
                        if (i + len <= chineseText.length()) {
                            String subStr = chineseText.substring(i, i + len);
                            // 避免生成过于碎片化的词汇
                            if (isValidChineseSegment(subStr)) {
                                keywords.add(subStr);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 判断中文片段是否有意义
     */
    private static boolean isValidChineseSegment(String segment) {
        // 避免单独的助词、语气词等
        Set<String> invalidSegments = new HashSet<>(Arrays.asList(
            "的话", "之类", "什么", "怎么", "这样", "那样", "一些", "如果", "虽然", "但是", "因为", "所以"
        ));
        return !invalidSegments.contains(segment);
    }
    
    /**
     * 提取中文词组（原方法保留）
     */
    private static void extractChineseWords(String text, List<String> keywords) {
        java.util.regex.Matcher matcher = CHINESE_PATTERN.matcher(text);
        while (matcher.find()) {
            String chineseText = matcher.group();
            
            // 添加完整的中文字符串
            if (chineseText.length() >= MIN_WORD_LENGTH) {
                keywords.add(chineseText);
            }
            
            // 提取中文字符的2-4字符子串
            for (int i = 0; i < chineseText.length(); i++) {
                for (int len = 2; len <= Math.min(4, chineseText.length() - i); len++) {
                    if (i + len <= chineseText.length()) {
                        String subStr = chineseText.substring(i, i + len);
                        keywords.add(subStr);
                    }
                }
            }
        }
    }
    
    /**
     * 提取英文单词
     */
    private static void extractEnglishWords(String text, List<String> keywords) {
        java.util.regex.Matcher matcher = ENGLISH_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            if (word.length() >= MIN_WORD_LENGTH) {
                keywords.add(word);
                
                // 提取英文单词的前缀（用于模糊匹配）
                if (word.length() > 3) {
                    keywords.add(word.substring(0, 3));
                }
            }
        }
    }
    
    /**
     * 提取数字
     */
    private static void extractNumbers(String text, List<String> keywords) {
        java.util.regex.Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            String number = matcher.group();
            if (number.length() >= MIN_WORD_LENGTH) {
                keywords.add(number);
            }
        }
    }
    
    /**
     * 按分隔符分割
     */
    private static void extractByDelimiters(String text, List<String> keywords) {
        // 常见分隔符
        String[] delimiters = {" ", "-", "_", ".", ",", "，", "、", "/", "\\", "|", ":", "：", ";", "；"};
        
        for (String delimiter : delimiters) {
            if (text.contains(delimiter)) {
                String[] parts = text.split(Pattern.quote(delimiter));
                for (String part : parts) {
                    part = part.trim();
                    if (StringUtils.hasText(part) && part.length() >= MIN_WORD_LENGTH) {
                        keywords.add(part);
                    }
                }
            }
        }
    }
}
