package com.ruoyi.chigua.domain.vo.web;

import java.util.List;

/**
 * Telegram 帖子分页结果
 */
public class WebTgPageResult
{
    /** 当前页数据 */
    private List<WebTgPostVO> list;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;

    /** 是否还有下一页 */
    private boolean hasMore;

    public WebTgPageResult() {}

    public WebTgPageResult(List<WebTgPostVO> list, long total, int page, int size) {
        this.list    = list;
        this.total   = total;
        this.page    = page;
        this.size    = size;
        this.hasMore = (long) page * size < total;
    }

    public List<WebTgPostVO> getList() { return list; }
    public void setList(List<WebTgPostVO> list) { this.list = list; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}
