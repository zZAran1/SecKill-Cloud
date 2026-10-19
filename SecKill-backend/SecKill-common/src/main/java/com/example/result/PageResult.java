package com.example.result;

import lombok.Data;

import java.util.List;

/**
 * 统一分页返回结构：列表 + 总数 + 当前页 + 每页大小
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.total = total;
        result.page = page;
        result.size = size;
        return result;
    }
}
