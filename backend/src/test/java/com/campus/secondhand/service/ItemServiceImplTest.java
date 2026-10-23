package com.campus.secondhand.service;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.dto.ItemQueryRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.dto.PageResult;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ItemServiceImpl 单元测试：mock ItemMapper，不启动 Spring、不连接数据库。
 *
 * <p>覆盖：默认分页/排序、分页偏移量、关键词 trim 与 LIKE 转义、排序白名单透传、
 * 参数非法 400/10001 + fieldErrors、详情 404/10005、ID 非正整数校验。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemServiceImpl 单元测试（不依赖数据库）")
class ItemServiceImplTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 22, 9, 12, 35);

    @Mock
    private ItemMapper itemMapper;

    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemServiceImpl(itemMapper);
    }

    @Test
    @DisplayName("默认查询：page=1、size=10、无关键词、默认排序，实体正确映射为 ItemVO")
    void listItems_should_useDefaultPagingAndDefaultSort_when_queryEmpty() {
        when(itemMapper.countList(isNull())).thenReturn(5L);
        when(itemMapper.selectList(isNull(), isNull(), eq(0L), eq(10)))
                .thenReturn(List.of(sampleItem(2L, "高等数学教材（同济第七版）", "25.50")));

        PageResult<ItemVO> result = itemService.listItems(new ItemQueryRequest());

        assertThat(result.getTotal()).isEqualTo(5L);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getList()).hasSize(1);
        ItemVO itemVO = result.getList().get(0);
        assertThat(itemVO.getId()).isEqualTo(2L);
        assertThat(itemVO.getItem()).isEqualTo("高等数学教材（同济第七版）");
        assertThat(itemVO.getItemPrice()).isEqualByComparingTo("25.50");
        assertThat(itemVO.getContactInfo()).isEqualTo("QQ：100000001");
        assertThat(itemVO.getUserId()).isEqualTo(2L);
        assertThat(itemVO.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(itemVO.getUpdatedAt()).isEqualTo(CREATED_AT);
        verify(itemMapper).countList(isNull());
        verify(itemMapper).selectList(isNull(), isNull(), eq(0L), eq(10));
    }

    @Test
    @DisplayName("分页：page=2、size=2 时 offset=(2-1)*2=2，响应回显 page/size")
    void listItems_should_calculateOffset_when_pageAndSizeProvided() {
        ItemQueryRequest query = new ItemQueryRequest();
        query.setPage(2);
        query.setSize(2);
        when(itemMapper.countList(isNull())).thenReturn(5L);
        when(itemMapper.selectList(isNull(), isNull(), eq(2L), eq(2)))
                .thenReturn(List.of(sampleItem(3L, "护眼台灯", "39.90")));

        PageResult<ItemVO> result = itemService.listItems(query);

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(5L);
        assertThat(result.getList()).hasSize(1);
        verify(itemMapper).selectList(isNull(), isNull(), eq(2L), eq(2));
    }

    @Test
    @DisplayName("搜索：keyword 的 \\、%、_ 在 Java 层转义后再参数化传给 SQL")
    void listItems_should_escapeLikeWildcards_when_keywordContainsWildcards() {
        ItemQueryRequest query = new ItemQueryRequest();
        // 原始关键词：50%_off\
        query.setKeyword("50%_off\\");
        // 期望转义结果：50\%\_off\\
        String expectedEscapedKeyword = "50\\%\\_off\\\\";
        when(itemMapper.countList(expectedEscapedKeyword)).thenReturn(0L);
        when(itemMapper.selectList(eq(expectedEscapedKeyword), isNull(), eq(0L), eq(10))).thenReturn(List.of());

        PageResult<ItemVO> result = itemService.listItems(query);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
        verify(itemMapper).countList(expectedEscapedKeyword);
        verify(itemMapper).selectList(eq(expectedEscapedKeyword), isNull(), eq(0L), eq(10));
    }

    @Test
    @DisplayName("搜索：keyword 首尾空格被 trim 后传入 Mapper")
    void listItems_should_trimKeyword_when_keywordHasSpaces() {
        ItemQueryRequest query = new ItemQueryRequest();
        query.setKeyword("  数学  ");
        when(itemMapper.countList("数学")).thenReturn(1L);
        when(itemMapper.selectList(eq("数学"), isNull(), eq(0L), eq(10)))
                .thenReturn(List.of(sampleItem(2L, "高等数学教材（同济第七版）", "25.50")));

        PageResult<ItemVO> result = itemService.listItems(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        verify(itemMapper).countList("数学");
    }

    @Test
    @DisplayName("排序：price_asc / price_desc 白名单值原样透传给 Mapper")
    void listItems_should_passSortValue_when_sortIsWhitelisted() {
        ItemQueryRequest ascending = new ItemQueryRequest();
        ascending.setSort("price_asc");
        when(itemMapper.countList(isNull())).thenReturn(5L);
        when(itemMapper.selectList(isNull(), eq("price_asc"), eq(0L), eq(10))).thenReturn(List.of());
        itemService.listItems(ascending);
        verify(itemMapper).selectList(isNull(), eq("price_asc"), eq(0L), eq(10));

        ItemQueryRequest descending = new ItemQueryRequest();
        descending.setSort("price_desc");
        when(itemMapper.selectList(isNull(), eq("price_desc"), eq(0L), eq(10))).thenReturn(List.of());
        itemService.listItems(descending);
        verify(itemMapper).selectList(isNull(), eq("price_desc"), eq(0L), eq(10));
    }

    @Test
    @DisplayName("参数非法：page=0 → 400/10001 + fieldErrors.page，且不查询数据库")
    void listItems_should_throwParamInvalid_when_pageLessThanOne() {
        ItemQueryRequest query = new ItemQueryRequest();
        query.setPage(0);

        BusinessException ex = catchThrowableOfType(() -> itemService.listItems(query), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        assertThat(ex.getErrorCode().getCode()).isEqualTo(10001);
        assertThat(fieldErrors(ex)).containsKey("page");
        verifyNoInteractions(itemMapper);
    }

    @Test
    @DisplayName("参数非法：size=51 → 400/10001 + fieldErrors.size")
    void listItems_should_throwParamInvalid_when_sizeOutOfRange() {
        ItemQueryRequest query = new ItemQueryRequest();
        query.setSize(51);

        BusinessException ex = catchThrowableOfType(() -> itemService.listItems(query), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        assertThat(fieldErrors(ex)).containsKey("size");
        verifyNoInteractions(itemMapper);
    }

    @Test
    @DisplayName("参数非法：sort 非白名单值 → 400/10001 + fieldErrors.sort")
    void listItems_should_throwParamInvalid_when_sortNotWhitelisted() {
        ItemQueryRequest query = new ItemQueryRequest();
        query.setSort("price_asc;drop table item");

        BusinessException ex = catchThrowableOfType(() -> itemService.listItems(query), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        assertThat(fieldErrors(ex)).containsKey("sort");
        verifyNoInteractions(itemMapper);
    }

    @Test
    @DisplayName("参数非法：keyword 超过 50 字符 → 400/10001 + fieldErrors.keyword")
    void listItems_should_throwParamInvalid_when_keywordTooLong() {
        ItemQueryRequest query = new ItemQueryRequest();
        query.setKeyword("x".repeat(51));

        BusinessException ex = catchThrowableOfType(() -> itemService.listItems(query), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        assertThat(fieldErrors(ex)).containsKey("keyword");
        verifyNoInteractions(itemMapper);
    }

    @Test
    @DisplayName("详情：商品存在时映射为 ItemVO")
    void getItem_should_returnItemVO_when_itemExists() {
        when(itemMapper.selectById(1L)).thenReturn(sampleItem(1L, "二手自行车（九成新）", "280.00"));

        ItemVO itemVO = itemService.getItem(1L);

        assertThat(itemVO.getId()).isEqualTo(1L);
        assertThat(itemVO.getItem()).isEqualTo("二手自行车（九成新）");
        assertThat(itemVO.getItemPrice()).isEqualByComparingTo("280.00");
        assertThat(itemVO.getUserId()).isEqualTo(2L);
        assertThat(itemVO.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("详情：商品不存在 → 404/10005「商品不存在」")
    void getItem_should_throwNotFound_when_itemMissing() {
        when(itemMapper.selectById(999999L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(() -> itemService.getItem(999999L), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getErrorCode().getCode()).isEqualTo(10005);
        assertThat(ex.getMessage()).isEqualTo("商品不存在");
    }

    @Test
    @DisplayName("详情：id 非正整数（null/0/-1）→ 400/10001 + fieldErrors.id，不查库")
    void getItem_should_throwParamInvalid_when_idNotPositive() {
        BusinessException nullId = catchThrowableOfType(() -> itemService.getItem(null), BusinessException.class);
        BusinessException zeroId = catchThrowableOfType(() -> itemService.getItem(0L), BusinessException.class);
        BusinessException negativeId = catchThrowableOfType(() -> itemService.getItem(-1L), BusinessException.class);

        assertThat(nullId.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        assertThat(fieldErrors(zeroId)).containsKey("id");
        assertThat(fieldErrors(negativeId)).containsKey("id");
        verifyNoInteractions(itemMapper);
    }

    private Item sampleItem(long id, String name, String price) {
        Item item = new Item();
        item.setId(id);
        item.setItem(name);
        item.setItemPrice(new BigDecimal(price));
        item.setContactInfo("QQ：100000001");
        item.setUserId(2L);
        item.setCreatedAt(CREATED_AT);
        item.setUpdatedAt(CREATED_AT);
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fieldErrors(BusinessException ex) {
        Map<?, ?> data = (Map<?, ?>) ex.getData();
        return (Map<String, String>) data.get("fieldErrors");
    }
}
