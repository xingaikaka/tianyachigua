//package com.ruoyi.chigua.service;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * 分类版本控制缓存测试
// * 验证分类级版本控制功能是否正常工作
// */
//@SpringBootTest
//@ActiveProfiles("test")
//public class CategoryVersionCacheTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(CategoryVersionCacheTest.class);
//
//    @Autowired
//    private CacheVersionService cacheVersionService;
//
//    @Autowired
//    private CacheRefreshService cacheRefreshService;
//
//    /**
//     * 测试分类版本控制基本功能
//     */
//    @Test
//    public void testCategoryVersionControl() {
//        logger.info("🧪 开始测试分类版本控制功能");
//
//        // 测试获取初始版本号
//        Long category1Version = cacheVersionService.getCategoryVersion("videoList", 1L);
//        Long category2Version = cacheVersionService.getCategoryVersion("videoList", 2L);
//        Long globalVersion = cacheVersionService.getGlobalVersion("videoList");
//
//        logger.info("📊 初始版本号 - 分类1: {}, 分类2: {}, 全局: {}",
//                   category1Version, category2Version, globalVersion);
//
//        // 测试组合版本号生成
//        String combinedVersion1 = cacheVersionService.getCombinedVersion("videoList", 1L);
//        String combinedVersion2 = cacheVersionService.getCombinedVersion("videoList", 2L);
//
//        logger.info("🔑 组合版本号 - 分类1: {}, 分类2: {}", combinedVersion1, combinedVersion2);
//
//        // 测试分类版本递增
//        logger.info("⬆️ 递增分类1版本号");
//        cacheVersionService.incrementCategoryVersion("videoList", 1L);
//
//        Long newCategory1Version = cacheVersionService.getCategoryVersion("videoList", 1L);
//        Long unchangedCategory2Version = cacheVersionService.getCategoryVersion("videoList", 2L);
//
//        logger.info("📈 递增后版本号 - 分类1: {} (应该+1), 分类2: {} (应该不变)",
//                   newCategory1Version, unchangedCategory2Version);
//
//        // 验证分类1版本递增，分类2版本不变
//        assert newCategory1Version.equals(category1Version + 1) : "分类1版本号应该递增1";
//        assert unchangedCategory2Version.equals(category2Version) : "分类2版本号应该保持不变";
//
//        // 测试全局版本递增
//        logger.info("🌐 递增全局版本号");
//        cacheVersionService.incrementGlobalVersion("videoList");
//
//        Long newGlobalVersion = cacheVersionService.getGlobalVersion("videoList");
//        logger.info("🌍 递增后全局版本号: {} (应该+1)", newGlobalVersion);
//
//        assert newGlobalVersion.equals(globalVersion + 1) : "全局版本号应该递增1";
//
//        logger.info("✅ 分类版本控制功能测试通过");
//    }
//
//    /**
//     * 测试智能缓存失效功能
//     */
//    @Test
//    public void testSmartCacheInvalidation() {
//        logger.info("🧪 开始测试智能缓存失效功能");
//
//        // 获取初始版本号
//        Long initialCategory1 = cacheVersionService.getCategoryVersion("videoList", 1L);
//        Long initialCategory2 = cacheVersionService.getCategoryVersion("videoList", 2L);
//        Long initialGlobal = cacheVersionService.getGlobalVersion("videoList");
//
//        logger.info("📊 初始版本号 - 分类1: {}, 分类2: {}, 全局: {}",
//                   initialCategory1, initialCategory2, initialGlobal);
//
//        // 测试单分类影响
//        logger.info("📂 测试单分类影响 - 分类1");
//        cacheRefreshService.smartRefreshVideoCache(123L, 1L, false);
//
//        Long afterCategory1 = cacheVersionService.getCategoryVersion("videoList", 1L);
//        Long afterCategory2 = cacheVersionService.getCategoryVersion("videoList", 2L);
//        Long afterGlobal = cacheVersionService.getGlobalVersion("videoList");
//
//        logger.info("📈 单分类影响后 - 分类1: {} (应该+1), 分类2: {} (应该不变), 全局: {} (应该不变)",
//                   afterCategory1, afterCategory2, afterGlobal);
//
//        assert afterCategory1 > initialCategory1 : "分类1版本号应该递增";
//        assert afterCategory2.equals(initialCategory2) : "分类2版本号应该保持不变";
//        assert afterGlobal.equals(initialGlobal) : "全局版本号应该保持不变";
//
//        // 测试全局影响
//        logger.info("🌐 测试全局影响");
//        cacheRefreshService.smartRefreshVideoCache(456L, null, true);
//
//        Long finalGlobal = cacheVersionService.getGlobalVersion("videoList");
//        logger.info("🌍 全局影响后 - 全局: {} (应该+1)", finalGlobal);
//
//        assert finalGlobal > afterGlobal : "全局版本号应该递增";
//
//        logger.info("✅ 智能缓存失效功能测试通过");
//    }
//
//    /**
//     * 测试缓存键生成差异
//     */
//    @Test
//    public void testCacheKeyGeneration() {
//        logger.info("🧪 开始测试缓存键生成差异");
//
//        // 模拟不同分类的缓存键应该不同
//        String version1 = cacheVersionService.getCombinedVersion("videoList", 1L);
//        String version2 = cacheVersionService.getCombinedVersion("videoList", 2L);
//
//        logger.info("🔑 不同分类的版本号 - 分类1: {}, 分类2: {}", version1, version2);
//
//        // 递增分类1版本
//        cacheVersionService.incrementCategoryVersion("videoList", 1L);
//
//        String newVersion1 = cacheVersionService.getCombinedVersion("videoList", 1L);
//        String unchangedVersion2 = cacheVersionService.getCombinedVersion("videoList", 2L);
//
//        logger.info("📈 递增分类1后 - 分类1: {} (应该变化), 分类2: {} (应该不变)",
//                   newVersion1, unchangedVersion2);
//
//        assert !newVersion1.equals(version1) : "分类1版本号应该变化";
//        assert unchangedVersion2.equals(version2) : "分类2版本号应该保持不变";
//
//        logger.info("✅ 缓存键生成差异测试通过");
//    }
//}
