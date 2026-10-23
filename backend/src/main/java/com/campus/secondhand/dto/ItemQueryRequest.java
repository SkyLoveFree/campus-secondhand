package com.campus.secondhand.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 商品列表查询参数（从 query string 绑定）。
 *
 * <p>默认值：page=1、size=10；keyword/sort 在 setter 中 trim，空串归一为 null
 * （keyword 为 null 表示不筛选，sort 为 null 表示默认按创建时间倒序）。
 * 参数校验失败统一由全局异常处理返回 400/10001 + {@code data.fieldErrors}。</p>
 */
public class ItemQueryRequest {

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_SIZE = 10;

    public static final int MAX_SIZE = 50;

    public static final int MAX_KEYWORD_LENGTH = 50;

    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer page = DEFAULT_PAGE;

    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = MAX_SIZE, message = "每页条数不能超过 50")
    private Integer size = DEFAULT_SIZE;

    @Size(max = MAX_KEYWORD_LENGTH, message = "搜索关键词长度不能超过 50 个字符")
    private String keyword;

    @Pattern(regexp = "^(price_asc|price_desc)?$", message = "排序方式只支持 price_asc 或 price_desc")
    private String sort;

    /**
     * 页码；即使绑定为空也回落到默认值 1，避免 Service 收到 null。
     */
    public int getPage() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * 每页条数；即使绑定为空也回落到默认值 10。
     */
    public int getSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = trimToNull(keyword);
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = trimToNull(sort);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
