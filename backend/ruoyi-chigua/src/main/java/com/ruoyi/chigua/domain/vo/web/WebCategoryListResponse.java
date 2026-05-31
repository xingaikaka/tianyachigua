package com.ruoyi.chigua.domain.vo.web;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Web 分类列表响应，包含分类数据及版本号，便于前端缓存比对。
 */
public class WebCategoryListResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<WebCategoryVO> categories;
    private String version;

    public WebCategoryListResponse() {
        this.categories = Collections.emptyList();
        this.version = "";
    }

    public WebCategoryListResponse(List<WebCategoryVO> categories, String version) {
        this.categories = categories != null ? categories : Collections.emptyList();
        this.version = version;
    }

    public List<WebCategoryVO> getCategories() {
        return categories;
    }

    public void setCategories(List<WebCategoryVO> categories) {
        this.categories = categories;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
