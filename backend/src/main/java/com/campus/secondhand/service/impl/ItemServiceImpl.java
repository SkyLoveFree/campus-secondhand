package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.dto.ItemQueryRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.dto.PageResult;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品查询业务实现（F-05/F-06/F-07）。
 *
 * <p>安全要点：
 * <ol>
 *   <li>keyword 在 Java 层转义 LIKE 通配符 {@code \}、{@code %}、{@code _}，
 *       再以 #{} 参数化传给 SQL 的 {@code LIKE ... ESCAPE '\\'}，避免用户输入被当成通配符；</li>
 *   <li>sort 只允许 price_asc / price_desc 两个白名单值，SQL 端用 choose 映射固定 ORDER BY，
 *       绝不把用户输入拼接进 SQL；</li>
 *   <li>Service 做二次参数校验，防止绕过 Controller 直接调用。</li>
 * </ol></p>
 */
@Service
public class ItemServiceImpl implements ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemServiceImpl.class);

    /** 价格升序排序值（与 api-spec 一致） */
    public static final String SORT_PRICE_ASC = "price_asc";

    /** 价格降序排序值（与 api-spec 一致） */
    public static final String SORT_PRICE_DESC = "price_desc";

    /** 排序白名单 */
    private static final Set<String> ALLOWED_SORTS = Set.of(SORT_PRICE_ASC, SORT_PRICE_DESC);

    private static final int MIN_PAGE = 1;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = ItemQueryRequest.MAX_SIZE;
    private static final int MAX_KEYWORD_LENGTH = ItemQueryRequest.MAX_KEYWORD_LENGTH;

    private final ItemMapper itemMapper;

    public ItemServiceImpl(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    @Override
    public PageResult<ItemVO> listItems(ItemQueryRequest query) {
        ItemQueryRequest actualQuery = query == null ? new ItemQueryRequest() : query;
        int page = actualQuery.getPage();
        int size = actualQuery.getSize();
        String keyword = trimToNull(actualQuery.getKeyword());
        String sort = trimToNull(actualQuery.getSort());
        validateQuery(page, size, keyword, sort);

        String escapedKeyword = escapeLikeKeyword(keyword);
        // 用 long 计算偏移量，避免超大 page 时 int 溢出
        long offset = (long) (page - 1) * size;
        long total = itemMapper.countList(escapedKeyword);
        List<Item> items = itemMapper.selectList(escapedKeyword, sort, offset, size);
        List<ItemVO> list = new ArrayList<>(items.size());
        for (Item item : items) {
            list.add(toItemVO(item));
        }
        log.debug("商品列表查询: page={}, size={}, keyword={}, sort={}, total={}", page, size, keyword, sort, total);
        return new PageResult<>(list, total, page, size);
    }

    @Override
    public ItemVO getItem(Long id) {
        if (id == null || id <= 0) {
            throw paramInvalid("id", "商品ID必须为正整数");
        }
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return toItemVO(item);
    }

    /**
     * Service 二次参数校验：与 Controller 的 Bean Validation 保持同一口径。
     */
    private void validateQuery(int page, int size, String keyword, String sort) {
        if (page < MIN_PAGE) {
            throw paramInvalid("page", "页码必须大于等于 1");
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw paramInvalid("size", "每页条数必须在 1-50 之间");
        }
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw paramInvalid("keyword", "搜索关键词长度不能超过 50 个字符");
        }
        if (sort != null && !ALLOWED_SORTS.contains(sort)) {
            throw paramInvalid("sort", "排序方式只支持 price_asc 或 price_desc");
        }
    }

    /**
     * 转义 LIKE 通配符：先转义反斜杠本身，再转义 % 与 _；SQL 端配合 {@code ESCAPE '\\'}。
     */
    private String escapeLikeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(keyword.length() + 8);
        for (int i = 0; i < keyword.length(); i++) {
            char current = keyword.charAt(i);
            if (current == '\\' || current == '%' || current == '_') {
                escaped.append('\\');
            }
            escaped.append(current);
        }
        return escaped.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ItemVO toItemVO(Item item) {
        return new ItemVO(item.getId(), item.getItem(), item.getItemPrice(), item.getContactInfo(),
                item.getUserId(), item.getCreatedAt(), item.getUpdatedAt());
    }

    /**
     * 构造参数校验异常，data.fieldErrors 与全局异常处理格式保持一致。
     */
    private BusinessException paramInvalid(String field, String message) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, message);
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        data.put("fieldErrors", fieldErrors);
        return new BusinessException(ErrorCode.PARAM_INVALID, ErrorCode.PARAM_INVALID.getMessage(), data);
    }
}
