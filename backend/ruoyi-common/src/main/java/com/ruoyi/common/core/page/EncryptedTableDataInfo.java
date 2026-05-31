package com.ruoyi.common.core.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 支持加密的表格分页数据对象
 * 
 * @author ruoyi
 */
public class EncryptedTableDataInfo extends TableDataInfo
{
    private static final long serialVersionUID = 1L;

    /** 是否加密 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean encrypted;

    /** 时间戳 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long timestamp;

    /** 签名 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String signature;

    /** 加密的数据（当数据被加密时使用） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;

    /**
     * 默认构造函数
     */
    public EncryptedTableDataInfo()
    {
        super();
    }

    /**
     * 从TableDataInfo创建加密版本
     * 
     * @param tableDataInfo 原始TableDataInfo
     */
    public EncryptedTableDataInfo(TableDataInfo tableDataInfo)
    {
        super();
        this.setCode(tableDataInfo.getCode());
        this.setMsg(tableDataInfo.getMsg());
        this.setTotal(tableDataInfo.getTotal());
        this.setRows(tableDataInfo.getRows());
    }

    /**
     * 分页
     * 
     * @param list 列表数据
     * @param total 总记录数
     */
    public EncryptedTableDataInfo(List<?> list, long total)
    {
        super(list, total);
    }

    public Boolean getEncrypted()
    {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted)
    {
        this.encrypted = encrypted;
    }

    public Long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Long timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getSignature()
    {
        return signature;
    }

    public void setSignature(String signature)
    {
        this.signature = signature;
    }

    public Object getData()
    {
        return data;
    }

    public void setData(Object data)
    {
        this.data = data;
    }
}