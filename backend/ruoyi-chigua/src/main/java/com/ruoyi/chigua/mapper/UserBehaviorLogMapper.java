package com.ruoyi.chigua.mapper;

import com.ruoyi.chigua.domain.UserBehaviorLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 用户行为日志 数据层
 *
 * @author chigua
 * @date 2026-05-24
 */
public interface UserBehaviorLogMapper {

    /** 单条插入（不常用，主要走批量） */
    int insert(UserBehaviorLog log);

    /** 批量插入 */
    int batchInsert(@Param("list") List<UserBehaviorLog> list);

    /** 分页查询事件流（按筛选条件） */
    List<UserBehaviorLog> selectEventList(@Param("q") Map<String, Object> q);

    /** 计数（与 selectEventList 同条件） */
    long countEventList(@Param("q") Map<String, Object> q);

    /** IP 维度的聚合列表（首次/最近/事件数/页面数等） */
    List<Map<String, Object>> selectIpSummary(@Param("q") Map<String, Object> q);

    long countIpSummary(@Param("q") Map<String, Object> q);

    /** 单 IP 的事件时间线 */
    List<UserBehaviorLog> selectByIp(@Param("ip") String ip,
                                    @Param("startTime") Date startTime,
                                    @Param("endTime") Date endTime,
                                    @Param("limit") int limit);

    /** 概览：当天 / 时段 总览数据 */
    Map<String, Object> selectOverview(@Param("startDate") Date startDate,
                                       @Param("endDate") Date endDate);

    /** 概览：按事件类型分布 */
    List<Map<String, Object>> selectEventTypeBreakdown(@Param("startDate") Date startDate,
                                                       @Param("endDate") Date endDate);

    /** 清理过期数据（按天） */
    int deleteBeforeDate(@Param("beforeDate") Date beforeDate);

    /** 批量查询视频标题：返回 [{id, title}, ...] */
    List<Map<String, Object>> selectVideoTitles(@Param("ids") List<Long> ids);

    /** 批量查询 TG 帖子标题：返回 [{id, title}, ...]（来自 tg_posts.caption 截断） */
    List<Map<String, Object>> selectTgPostTitles(@Param("ids") List<Long> ids);

    /** 批量查询分类名：返回 [{id, name}, ...] */
    List<Map<String, Object>> selectCategoryNames(@Param("ids") List<Long> ids);

    /** 批量查询 Redgifs 视频标题：返回 [{id, title}, ...] */
    List<Map<String, Object>> selectRedgifsTitles(@Param("ids") List<Long> ids);

    /** 批量查询合集标题：返回 [{id, title}, ...] */
    List<Map<String, Object>> selectCollectionTitles(@Param("ids") List<Long> ids);

    /** 批量查询广告名：返回 [{id, name}, ...] */
    List<Map<String, Object>> selectAdvertisementNames(@Param("ids") List<Long> ids);

    /** 批量查询标签名：返回 [{id, name}, ...] */
    List<Map<String, Object>> selectTagNames(@Param("ids") List<Long> ids);
}
