package com.campus.secondhand.dto;

import java.util.List;

/**
 * 分页结果：响应字段 {@code {list, total, page, size}}（M3 商品列表使用）。
 *
 * @param <T> 列表元素类型
 */
public class PageResult<T> {

    private final List<T> list;
    private final long total;
    private final int page;
    private final int size;

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
