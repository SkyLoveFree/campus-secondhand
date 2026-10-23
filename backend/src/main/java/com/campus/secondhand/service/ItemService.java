package com.campus.secondhand.service;

import com.campus.secondhand.dto.ItemQueryRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.dto.PageResult;

/**
 * 商品查询业务接口（F-05/F-06/F-07）。
 *
 * <p>只读接口，不依赖 Servlet API；参数校验、关键词转义、排序白名单映射在本层完成。</p>
 */
public interface ItemService {

    /**
     * 商品列表：分页 + 关键词模糊搜索 + 价格排序（默认创建时间倒序）。
     *
     * @param query 已绑定并校验过的查询参数
     * @return 分页结果 {@code {list, total, page, size}}
     * @throws com.campus.secondhand.common.BusinessException 参数非法（400/10001）
     */
    PageResult<ItemVO> listItems(ItemQueryRequest query);

    /**
     * 商品详情。
     *
     * @param id 商品 ID，必须为正整数
     * @return 商品视图对象
     * @throws com.campus.secondhand.common.BusinessException ID 非法（400/10001）或商品不存在（404/10005）
     */
    ItemVO getItem(Long id);
}
