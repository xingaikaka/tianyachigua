package com.ruoyi.chigua.config;

import com.ruoyi.chigua.service.StatsService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;

@Component
// @EnableScheduling  // 🔥 已禁用：使用Quartz的ChiguaTask统一管理，避免重复聚合导致数据被0覆盖
public class StatsAggregationScheduler {

    @Resource
    private StatsService statsService;

    // 每天 00:10 聚合前一日
    // @Scheduled(cron = "0 10 0 * * ?")  // 🔥 已禁用：使用Quartz的ChiguaTask统一管理
    public void aggregateYesterday() {
        statsService.aggregateDailyToMySQL(LocalDate.now().minusDays(1));
    }
}


