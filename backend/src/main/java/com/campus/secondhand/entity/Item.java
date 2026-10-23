package com.campus.secondhand.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体：只映射数据库 {@code item} 表。
 *
 * <p>本期（M2）仅提供骨架供 M3/M4 使用；对外统一用 {@code ItemVO}。</p>
 */
public class Item {

    private Long id;
    private String item;
    private BigDecimal itemPrice;
    private String contactInfo;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Item{id=" + id + ", item='" + item + "', itemPrice=" + itemPrice + ", userId=" + userId + "}";
    }
}
