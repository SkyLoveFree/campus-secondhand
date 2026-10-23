package com.campus.secondhand.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象：API JSON 字段使用 camelCase（itemPrice/contactInfo/userId/createdAt/updatedAt）。
 *
 * <p>M3/M4 商品接口使用；时间格式固定 {@code yyyy-MM-dd HH:mm:ss}（Asia/Shanghai）。</p>
 */
public class ItemVO {

    private final Long id;
    private final String item;
    private final BigDecimal itemPrice;
    private final String contactInfo;
    private final Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime updatedAt;

    public ItemVO(Long id, String item, BigDecimal itemPrice, String contactInfo, Long userId,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.item = item;
        this.itemPrice = itemPrice;
        this.contactInfo = contactInfo;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
