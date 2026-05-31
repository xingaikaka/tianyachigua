package com.ruoyi.chigua.domain.query;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 广告统计查询参数
 * 
 * @author ruoyi
 * @date 2025-01-28
 */
public class AdStatisticsQuery
{
    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 广告标题（用户筛选） */
    private String adTitle;

    /** 广告类型 */
    private String adType;

    /** 广告位置 */
    private String position;

    /** 广告ID */
    private Long adId;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getAdTitle() {
        return adTitle;
    }

    public void setAdTitle(String adTitle) {
        this.adTitle = adTitle;
    }

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
    }
}
