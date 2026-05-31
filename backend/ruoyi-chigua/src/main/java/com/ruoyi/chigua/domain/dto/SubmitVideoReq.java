package com.ruoyi.chigua.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class SubmitVideoReq {
    private String title;            // 必填
    private Long categoryId;         // 必填：分类ID，由插件配置传入
    private String coverImage;       // 可选：封面完整URL或相对路径
    private List<String> tags;       // 可选：标签名列表
    private String author;           // 可选：作者
}
