package com.campus.secondhand.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 商品发布/修改请求体（M3/M4 使用，本期仅骨架）。
 *
 * <p>校验口径与 docs/api-spec.md 一致：item、contactInfo trim 后 1-100 字符；
 * itemPrice &gt; 0 且 ≤ 999999.99，最多两位小数。归属 userId 只取 Session，请求中忽略该字段。</p>
 */
public class ItemSaveRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(min = 1, max = 100, message = "商品名称长度需为 1-100 个字符")
    private String item;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "商品价格必须大于 0")
    @DecimalMax(value = "999999.99", message = "商品价格不能超过 999999.99")
    @Digits(integer = 6, fraction = 2, message = "商品价格最多两位小数")
    private BigDecimal itemPrice;

    @NotBlank(message = "联系方式不能为空")
    @Size(min = 1, max = 100, message = "联系方式长度需为 1-100 个字符")
    private String contactInfo;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item == null ? null : item.trim();
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
        this.contactInfo = contactInfo == null ? null : contactInfo.trim();
    }
}
