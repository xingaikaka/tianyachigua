package com.ruoyi.chigua.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * EFV回调包装器DTO
 * 处理EFV发送的包装在"movie"对象中的JSON数据
 * 
 * @author ruoyi
 * @date 2025-01-22
 */
public class EfvCallbackWrapperDto
{
    /** 包装的电影数据 */
    @JsonProperty("movie")
    private EfvCallbackDto movie;

    // Getters and Setters
    public EfvCallbackDto getMovie() {
        return movie;
    }

    public void setMovie(EfvCallbackDto movie) {
        this.movie = movie;
    }
    
    /**
     * 获取实际的回调数据
     * @return EfvCallbackDto对象，如果没有movie包装则返回null
     */
    public EfvCallbackDto getCallbackData() {
        return this.movie;
    }
}