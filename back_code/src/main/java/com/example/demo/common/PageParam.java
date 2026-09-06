package com.example.demo.common;

import lombok.Data;

/**
 * 分页请求参数
 */
@Data
public class PageParam {

    /** 当前页码，默认 1 */
    private int pageNum = 1;

    /** 每页条数，默认 10 */
    private int pageSize = 10;
}
