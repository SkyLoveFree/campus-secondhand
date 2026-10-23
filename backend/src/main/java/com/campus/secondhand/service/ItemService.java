package com.campus.secondhand.service;

import com.campus.secondhand.dto.ItemQueryRequest;
import com.campus.secondhand.dto.ItemSaveRequest;
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

    /**
     * 发布商品（F-08，需登录）：归属 userId 只取 Session 当前用户，请求中的 userId 一律忽略。
     *
     * @param request       已校验的发布请求
     * @param currentUserId Session 当前用户 ID（拦截器保证已登录）
     * @return 创建后的商品视图对象
     */
    ItemVO createItem(ItemSaveRequest request, Long currentUserId);

    /**
     * 修改商品（F-09，需登录）：仅本人或管理员可改；不改归属与创建时间。
     * 判定顺序：商品不存在 404 → 非本人且非管理员 403。
     *
     * @throws com.campus.secondhand.common.BusinessException 404/10005、403/10004
     */
    ItemVO updateItem(Long id, ItemSaveRequest request, Long currentUserId, String currentRole);

    /**
     * 删除商品（F-10，需登录）：仅本人或管理员可删，物理删除。
     * 判定顺序：商品不存在 404 → 非本人且非管理员 403。
     *
     * @throws com.campus.secondhand.common.BusinessException 404/10005、403/10004
     */
    void deleteItem(Long id, Long currentUserId, String currentRole);
}
