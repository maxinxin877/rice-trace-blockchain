package com.itheima.qukuailian.common;

import lombok.Data;

/**
 * 分页查询参数（同时兼容文档规范的 pageNo 与前端常用的 page 写法）
 * <p>接口文档 §2.3 规定分页参数为 pageNo/pageSize；为兼容部分前端直接传 page，
 * 此处同时接收 page，优先使用 pageNo。</p>
 */
@Data
public class PageQuery {

    /** 页码（文档规范） */
    private Integer pageNo;

    /** 页码（前端兼容写法） */
    private Integer page;

    /** 每页条数，默认 20，最大 100 */
    private Integer pageSize;

    /** 解析页码：pageNo 优先，其次 page，默认 1 */
    public long resolvePageNo() {
        Integer value = pageNo != null ? pageNo : page;
        return value == null || value < 1 ? 1L : value;
    }

    /** 解析每页条数：默认 20，最大 100 */
    public long resolvePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 100);
    }
}
