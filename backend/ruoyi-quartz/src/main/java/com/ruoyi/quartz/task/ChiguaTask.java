package com.ruoyi.quartz.task;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.chigua.service.StatsService;

/**
 * Chigua定时任务调度
 * 
 * @author chigua
 */
@Component("chiguaTask")
public class ChiguaTask
{
    private static final Logger logger = LoggerFactory.getLogger(ChiguaTask.class);

    @Autowired
    private StatsService statsService;

    // ==================== 统计数据聚合任务 ====================

    /**
     * 聚合统计数据到数据库
     * 每半小时执行一次：0 0,30 * * * ?
     */
    public void aggregateStatsData()
    {
        logger.info("=== 开始执行统计数据聚合任务 ===");
        
        try {
            LocalDate today = LocalDate.now();
            java.time.LocalTime now = java.time.LocalTime.now();
            
            // 🔥 修改逻辑：只在00:00聚合昨天的数据（并清理Redis）
            // 00:30不再聚合昨天，避免Redis被清空后读到0值覆盖数据
            if (now.getHour() == 0 && now.getMinute() == 0) {
                // 凌晨00:00：聚合昨天的数据（会清理昨天的Redis数据）
                LocalDate yesterday = today.minusDays(1);
                logger.info("🌅 凌晨00:00，聚合昨日数据并清理Redis: {}", yesterday);
                aggregateStatsForDate(yesterday);
            }
            
            // 聚合当天的数据到数据库（但不清理当天的Redis数据）
            logger.info("📊 聚合当天数据到数据库（保留Redis实时数据）: {}", today);
            aggregateStatsForDate(today);
            
            logger.info("=== 统计数据聚合任务执行完成 ===");
        } catch (Exception e) {
            logger.error("统计数据聚合任务执行失败", e);
        }
    }

    /**
     * 聚合指定日期的统计数据
     */
    private void aggregateStatsForDate(LocalDate date) {
        try {
            logger.info("📊 开始聚合日期 {} 的统计数据", date);
            
            // 调用统计服务进行数据聚合
            statsService.aggregateDailyToMySQL(date);
            
            logger.info("✅ 日期 {} 的统计数据聚合完成", date);
            
        } catch (Exception e) {
            logger.error("❌ 聚合日期 {} 的统计数据失败: {}", date, e.getMessage(), e);
        }
    }

    /**
     * 每日凌晨清理过期的Redis统计数据
     * 每天凌晨3点执行：0 0 3 * * ?
     */
    public void cleanupExpiredStatsData()
    {
        logger.info("=== 开始执行Redis统计数据清理任务 ===");
        
        try {
            // 清理7天前的Redis数据（保留最近7天用于实时查询）
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            cleanupRedisStatsForDate(sevenDaysAgo);
            
            logger.info("=== Redis统计数据清理任务执行完成 ===");
        } catch (Exception e) {
            logger.error("Redis统计数据清理任务执行失败", e);
        }
    }

    /**
     * 清理指定日期的Redis统计数据
     */
    private void cleanupRedisStatsForDate(LocalDate date) {
        try {
            // 通过StatsService清理Redis数据
            int cleanedCount = statsService.cleanupRedisStatsForDate(date);
            logger.info("✅ 日期 {} 的Redis数据清理完成，共清理 {} 个key", date, cleanedCount);
            
        } catch (Exception e) {
            logger.error("❌ 清理日期 {} 的Redis数据失败: {}", date, e.getMessage(), e);
        }
    }

    /**
     * 手动测试统计数据聚合
     */
    public void testStatsAggregation() {
        logger.info("手动执行统计数据聚合测试");
        aggregateStatsData();
    }

    /**
     * 手动测试Redis数据清理
     */
    public void testStatsCleanup() {
        logger.info("手动执行Redis数据清理测试");
        cleanupExpiredStatsData();
    }

    // ==================== 播放记录清理任务 ====================

    /**
     * 清理7天前的播放记录数据
     * 每天凌晨4点执行：0 0 4 * * ?
     */
    public void cleanupOldPlayRecords()
    {
        logger.info("=== 开始执行播放记录清理任务 ===");
        
        try {
            // 清理7天前的播放记录，只保留最近7天的数据
            int deletedCount = statsService.cleanupOldPlayRecords();
            
            if (deletedCount > 0) {
                logger.info("✅ 播放记录清理完成，删除了 {} 条7天前的记录", deletedCount);
            } else {
                logger.info("✅ 播放记录清理完成，没有需要删除的记录");
            }
            
            logger.info("=== 播放记录清理任务执行完成 ===");
        } catch (Exception e) {
            logger.error("播放记录清理任务执行失败", e);
        }
    }

    /**
     * 手动测试播放记录清理
     */
    public void testPlayRecordsCleanup() {
        logger.info("手动执行播放记录清理测试");
        cleanupOldPlayRecords();
    }
}