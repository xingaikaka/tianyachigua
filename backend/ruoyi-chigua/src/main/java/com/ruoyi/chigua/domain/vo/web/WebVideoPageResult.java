package com.ruoyi.chigua.domain.vo.web;

import java.io.Serializable;
import java.util.List;
import com.ruoyi.chigua.domain.Video;

/**
 * Web 视频分页结果（用于缓存，保留 total 元信息）
 */
public class WebVideoPageResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Video> list;
    private long total;

    public WebVideoPageResult() {}

    public WebVideoPageResult(List<Video> list, long total) {
        this.list = list;
        this.total = total;
    }

    public List<Video> getList() {
        return list;
    }

    public void setList(List<Video> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}

