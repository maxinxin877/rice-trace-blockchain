package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页响应结果封装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> list;        // 当前页数据
    private long total;          // 总记录数
    private int pageNum;         // 当前页码
    private int pageSize;        // 每页条数
    private int pages;           // 总页数
    private boolean isFirstPage; // 是否为第一页
    private boolean isLastPage;  // 是否为最后一页
    private boolean hasPrevious; // 是否有上一页
    private boolean hasNext;     // 是否有下一页
}
