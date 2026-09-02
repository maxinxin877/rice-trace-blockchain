package com.itheima.qukuailian.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页响应（接口文档 v1.1 分页响应规范）
 * <pre>
 * {
 *   "pageNo": 1, "pageSize": 20, "total": 126, "records": []
 * }
 * </pre>
 */
@Data
public class PageResult<T> {

    private Long pageNo;
    private Long pageSize;
    private Long total;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPageNo(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords());
        return result;
    }

    public static <T> PageResult<T> of(Long pageNo, Long pageSize, Long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setRecords(records);
        return result;
    }
}
