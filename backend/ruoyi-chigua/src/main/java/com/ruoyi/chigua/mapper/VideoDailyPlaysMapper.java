package com.ruoyi.chigua.mapper;

import com.ruoyi.chigua.domain.DailyTopVideo;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日视频播放记录Mapper接口
 * 
 * @author ruoyi
 * @date 2025-08-25
 */
@Mapper
public interface VideoDailyPlaysMapper {
    
    /**
     * 记录当日播放（如果已存在则增加计数）
     */
    @Insert("INSERT INTO video_daily_plays (video_id, play_date, play_count) " +
            "VALUES (#{videoId}, #{playDate}, 1) " +
            "ON DUPLICATE KEY UPDATE " +
            "play_count = play_count + 1, " +
            "last_play_at = CURRENT_TIMESTAMP")
    int recordDailyPlay(@Param("videoId") Long videoId, @Param("playDate") LocalDate playDate);
    
    /**
     * 获取某日播放量Top100
     */
    @Select("SELECT " +
            "vdp.video_id, " +
            "v.title, " +
            "v.author, " +
            "v.category_id, " +
            "c.name as category_name, " +
            "vdp.play_count as daily_play_count, " +
            "COALESCE(vs.play_count, 0) as total_play_count, " +
            "v.published_at, " +
            "vdp.play_date, " +
            "vdp.first_play_at, " +
            "vdp.last_play_at " +
            "FROM video_daily_plays vdp " +
            "INNER JOIN videos v ON vdp.video_id = v.id " +
            "LEFT JOIN video_statistics vs ON v.id = vs.video_id " +
            "LEFT JOIN categories c ON v.category_id = c.id " +
            "WHERE vdp.play_date = #{date} AND v.status = 1 " +
            "ORDER BY vdp.play_count DESC " +
            "LIMIT #{limit}")
    List<DailyTopVideo> selectDailyTop100(@Param("date") LocalDate date, @Param("limit") int limit);
    
    /**
     * 获取某日活跃视频数量
     */
    @Select("SELECT COUNT(*) FROM video_daily_plays WHERE play_date = #{date}")
    int countActiveVideos(@Param("date") LocalDate date);
    
    /**
     * 获取某日总播放量
     */
    @Select("SELECT COALESCE(SUM(play_count), 0) FROM video_daily_plays WHERE play_date = #{date}")
    Long sumDailyPlays(@Param("date") LocalDate date);
    
    /**
     * 获取7日内活跃视频数量
     */
    @Select("SELECT COUNT(DISTINCT video_id) FROM video_daily_plays WHERE play_date BETWEEN #{startDate} AND #{endDate}")
    int countActiveVideos7Days(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 获取7日内总播放量
     */
    @Select("SELECT COALESCE(SUM(play_count), 0) FROM video_daily_plays WHERE play_date BETWEEN #{startDate} AND #{endDate}")
    Long sumDailyPlays7Days(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 获取视频在指定日期范围内的播放历史
     */
    @Select("SELECT " +
            "vdp.video_id, " +
            "v.title, " +
            "v.author, " +
            "v.category_id, " +
            "c.name as category_name, " +
            "vdp.play_count as daily_play_count, " +
            "COALESCE(vs.play_count, 0) as total_play_count, " +
            "v.published_at, " +
            "vdp.play_date, " +
            "vdp.first_play_at, " +
            "vdp.last_play_at " +
            "FROM video_daily_plays vdp " +
            "INNER JOIN videos v ON vdp.video_id = v.id " +
            "LEFT JOIN video_statistics vs ON v.id = vs.video_id " +
            "LEFT JOIN categories c ON v.category_id = c.id " +
            "WHERE vdp.video_id = #{videoId} " +
            "AND vdp.play_date BETWEEN #{startDate} AND #{endDate} " +
            "AND v.status = 1 " +
            "ORDER BY vdp.play_date DESC")
    List<DailyTopVideo> selectVideoPlayHistory(@Param("videoId") Long videoId, 
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    /**
     * 获取快速上升的视频（今日播放量相比昨日增长率最高的）
     */
    @Select("SELECT " +
            "today.video_id, " +
            "v.title, " +
            "v.author, " +
            "v.category_id, " +
            "c.name as category_name, " +
            "today.play_count as daily_play_count, " +
            "COALESCE(vs.play_count, 0) as total_play_count, " +
            "v.published_at, " +
            "today.play_date, " +
            "today.first_play_at, " +
            "today.last_play_at, " +
            "(today.play_count - COALESCE(yesterday.play_count, 0)) AS growth_count " +
            "FROM video_daily_plays today " +
            "LEFT JOIN video_daily_plays yesterday ON today.video_id = yesterday.video_id " +
            "    AND yesterday.play_date = #{date} - INTERVAL 1 DAY " +
            "INNER JOIN videos v ON today.video_id = v.id " +
            "LEFT JOIN video_statistics vs ON v.id = vs.video_id " +
            "LEFT JOIN categories c ON v.category_id = c.id " +
            "WHERE today.play_date = #{date} " +
            "AND v.status = 1 " +
            "ORDER BY (today.play_count - COALESCE(yesterday.play_count, 0)) " +
            "       / GREATEST(COALESCE(yesterday.play_count, 1), 1.0) DESC " +
            "LIMIT #{limit}")
    List<DailyTopVideo> selectRisingVideos(@Param("date") LocalDate date, @Param("limit") int limit);
    
    /**
     * 获取时间段内每日播放趋势
     */
    @Select("SELECT " +
            "play_date, " +
            "COUNT(*) as active_video_count, " +
            "SUM(play_count) as total_plays " +
            "FROM video_daily_plays " +
            "WHERE play_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY play_date " +
            "ORDER BY play_date")
    @Results({
        @Result(property = "playDate", column = "play_date"),
        @Result(property = "activeVideoCount", column = "active_video_count"),
        @Result(property = "totalPlays", column = "total_plays")
    })
    List<PlayTrendData> selectPlayTrend(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);
    
    /**
     * 清理指定日期之前的记录
     */
    @Delete("DELETE FROM video_daily_plays WHERE play_date < #{cutoffDate}")
    int deleteOldRecords(@Param("cutoffDate") LocalDate cutoffDate);
    
    /**
     * 获取播放量分布统计（7日内数据）
     */
    @Select("SELECT " +
            "CASE " +
            "  WHEN play_count >= 1000 THEN '1000+' " +
            "  WHEN play_count >= 500 THEN '500-999' " +
            "  WHEN play_count >= 100 THEN '100-499' " +
            "  WHEN play_count >= 50 THEN '50-99' " +
            "  WHEN play_count >= 10 THEN '10-49' " +
            "  ELSE '1-9' " +
            "END as play_range, " +
            "COUNT(*) as video_count, " +
            "SUM(play_count) as total_plays " +
            "FROM video_daily_plays " +
            "WHERE play_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY play_range " +
            "ORDER BY MIN(play_count) DESC")
    @Results({
        @Result(property = "playRange", column = "play_range"),
        @Result(property = "videoCount", column = "video_count"),
        @Result(property = "totalPlays", column = "total_plays")
    })
    List<PlayDistribution> selectPlayDistribution7Days(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 获取新视频排行榜（发布7天内）
     */
    @Select("SELECT " +
            "vdp.video_id, " +
            "v.title, " +
            "v.author, " +
            "v.category_id, " +
            "c.name as category_name, " +
            "vdp.play_count as daily_play_count, " +
            "COALESCE(vs.play_count, 0) as total_play_count, " +
            "v.published_at, " +
            "vdp.play_date, " +
            "vdp.first_play_at, " +
            "vdp.last_play_at " +
            "FROM video_daily_plays vdp " +
            "INNER JOIN videos v ON vdp.video_id = v.id " +
            "LEFT JOIN video_statistics vs ON v.id = vs.video_id " +
            "LEFT JOIN categories c ON v.category_id = c.id " +
            "WHERE vdp.play_date = #{date} " +
            "AND v.status = 1 " +
            "AND v.published_at >= #{date} - INTERVAL 7 DAY " +
            "ORDER BY vdp.play_count DESC " +
            "LIMIT #{limit}")
    List<DailyTopVideo> selectNewVideoRanking(@Param("date") LocalDate date, @Param("limit") int limit);
    
    /**
     * 获取分类播放量统计（7日内数据）
     */
    @Select("SELECT " +
            "c.id as category_id, " +
            "c.name as category_name, " +
            "COUNT(DISTINCT vdp.video_id) as video_count, " +
            "SUM(vdp.play_count) as total_plays, " +
            "SUM(vdp.play_count) / NULLIF(COUNT(DISTINCT vdp.video_id), 0) as avg_plays " +
            "FROM video_daily_plays vdp " +
            "INNER JOIN videos v ON vdp.video_id = v.id " +
            "INNER JOIN video_category_tag_relations vctr ON v.id = vctr.video_id " +
            "INNER JOIN categories c ON vctr.category_id = c.id " +
            "WHERE vdp.play_date BETWEEN #{startDate} AND #{endDate} " +
            "AND v.status = 1 AND vctr.relation_type = 1 AND c.status = 1 " +
            "GROUP BY c.id, c.name " +
            "ORDER BY total_plays DESC")
    @Results({
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "categoryName", column = "category_name"),
        @Result(property = "videoCount", column = "video_count"),
        @Result(property = "totalPlays", column = "total_plays"),
        @Result(property = "avgPlays", column = "avg_plays")
    })
    List<CategoryPlayStats> selectCategoryPlayStats7Days(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 获取近N天的播放趋势（每日汇总）
     */
    @Select("SELECT " +
            "play_date, " +
            "COUNT(*) as active_video_count, " +
            "SUM(play_count) as total_plays, " +
            "AVG(play_count) as avg_plays " +
            "FROM video_daily_plays " +
            "WHERE play_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY play_date " +
            "ORDER BY play_date")
    @Results({
        @Result(property = "playDate", column = "play_date"),
        @Result(property = "activeVideoCount", column = "active_video_count"),
        @Result(property = "totalPlays", column = "total_plays"),
        @Result(property = "avgPlays", column = "avg_plays")
    })
    List<DailyPlayTrend> selectDailyPlayTrend(@Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);
    
    /**
     * 播放趋势数据内部类
     */
    class PlayTrendData {
        private LocalDate playDate;
        private Integer activeVideoCount;
        private Long totalPlays;
        
        // Getters and Setters
        public LocalDate getPlayDate() { return playDate; }
        public void setPlayDate(LocalDate playDate) { this.playDate = playDate; }
        
        public Integer getActiveVideoCount() { return activeVideoCount; }
        public void setActiveVideoCount(Integer activeVideoCount) { this.activeVideoCount = activeVideoCount; }
        
        public Long getTotalPlays() { return totalPlays; }
        public void setTotalPlays(Long totalPlays) { this.totalPlays = totalPlays; }
        
        @Override
        public String toString() {
            return "PlayTrendData{" +
                    "playDate=" + playDate +
                    ", activeVideoCount=" + activeVideoCount +
                    ", totalPlays=" + totalPlays +
                    '}';
        }
    }
    
    /**
     * 播放量分布数据类
     */
    class PlayDistribution {
        private String playRange;
        private Integer videoCount;
        private Long totalPlays;
        
        // Getters and Setters
        public String getPlayRange() { return playRange; }
        public void setPlayRange(String playRange) { this.playRange = playRange; }
        
        public Integer getVideoCount() { return videoCount; }
        public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }
        
        public Long getTotalPlays() { return totalPlays; }
        public void setTotalPlays(Long totalPlays) { this.totalPlays = totalPlays; }
        
        @Override
        public String toString() {
            return "PlayDistribution{" +
                    "playRange='" + playRange + '\'' +
                    ", videoCount=" + videoCount +
                    ", totalPlays=" + totalPlays +
                    '}';
        }
    }
    
    /**
     * 分类播放统计数据类
     */
    class CategoryPlayStats {
        private Long categoryId;
        private String categoryName;
        private Integer videoCount;
        private Long totalPlays;
        private Double avgPlays;
        
        // Getters and Setters
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        
        public Integer getVideoCount() { return videoCount; }
        public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }
        
        public Long getTotalPlays() { return totalPlays; }
        public void setTotalPlays(Long totalPlays) { this.totalPlays = totalPlays; }
        
        public Double getAvgPlays() { return avgPlays; }
        public void setAvgPlays(Double avgPlays) { this.avgPlays = avgPlays; }
        
        @Override
        public String toString() {
            return "CategoryPlayStats{" +
                    "categoryId=" + categoryId +
                    ", categoryName='" + categoryName + '\'' +
                    ", videoCount=" + videoCount +
                    ", totalPlays=" + totalPlays +
                    ", avgPlays=" + avgPlays +
                    '}';
        }
    }
    
    /**
     * 每日播放趋势数据类
     */
    class DailyPlayTrend {
        private LocalDate playDate;
        private Integer activeVideoCount;
        private Long totalPlays;
        private Double avgPlays;
        
        // Getters and Setters
        public LocalDate getPlayDate() { return playDate; }
        public void setPlayDate(LocalDate playDate) { this.playDate = playDate; }
        
        public Integer getActiveVideoCount() { return activeVideoCount; }
        public void setActiveVideoCount(Integer activeVideoCount) { this.activeVideoCount = activeVideoCount; }
        
        public Long getTotalPlays() { return totalPlays; }
        public void setTotalPlays(Long totalPlays) { this.totalPlays = totalPlays; }
        
        public Double getAvgPlays() { return avgPlays; }
        public void setAvgPlays(Double avgPlays) { this.avgPlays = avgPlays; }
        
        @Override
        public String toString() {
            return "DailyPlayTrend{" +
                    "playDate=" + playDate +
                    ", activeVideoCount=" + activeVideoCount +
                    ", totalPlays=" + totalPlays +
                    ", avgPlays=" + avgPlays +
                    '}';
        }
    }
}
