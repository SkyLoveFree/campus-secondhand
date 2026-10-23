package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.dto.ItemSaveRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.mapper.ItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ItemServiceImpl 写操作单元测试（F-08/F-09/F-10）：mock ItemMapper，不连接数据库。
 *
 * <p>核心覆盖权限矩阵：归属只取 Session；404（不存在）优先于 403（无权限）；
 * 管理员可改删任意商品；普通用户仅本人；影响行数为 0 归一 404。</p>
 */
@DisplayName("ItemServiceImpl 商品写操作与权限测试（不依赖数据库）")
class ItemServiceWriteTest {

    private static final Long OWNER_ID = 2L;
    private static final Long OTHER_ID = 3L;
    private static final Long ADMIN_ID = 1L;

    private ItemMapper itemMapper;
    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemMapper = mock(ItemMapper.class);
        itemService = new ItemServiceImpl(itemMapper);
    }

    @Test
    @DisplayName("发布：归属 userId 只取 Session 当前用户，自增主键回填")
    void should_useSessionUserId_when_createItem() {
        when(itemMapper.insert(any(Item.class))).thenAnswer(invocation -> {
            Item arg = invocation.getArgument(0);
            arg.setId(10L);
            return 1;
        });
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));

        ItemVO vo = itemService.createItem(validRequest(), OWNER_ID);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(OWNER_ID);
        assertThat(captor.getValue().getCreatedAt()).isNull();
        assertThat(vo.getId()).isEqualTo(10L);
        assertThat(vo.getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("修改：本人成功，名称/价格/联系方式被更新，归属不变")
    void should_update_when_ownerEditsOwnItem() {
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));
        when(itemMapper.update(any(Item.class))).thenReturn(1);

        ItemSaveRequest request = validRequest();
        request.setItem("新名称");
        ItemVO vo = itemService.updateItem(10L, request, OWNER_ID, Constants.ROLE_USER);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).update(captor.capture());
        assertThat(captor.getValue().getItem()).isEqualTo("新名称");
        assertThat(captor.getValue().getUserId()).isEqualTo(OWNER_ID);
        assertThat(vo.getItem()).isEqualTo("新名称");
    }

    @Test
    @DisplayName("修改：管理员可改他人商品")
    void should_update_when_adminEditsOthersItem() {
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));
        when(itemMapper.update(any(Item.class))).thenReturn(1);

        ItemVO vo = itemService.updateItem(10L, validRequest(), ADMIN_ID, Constants.ROLE_ADMIN);

        verify(itemMapper).update(any(Item.class));
        assertThat(vo.getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("修改：非本人且非管理员返回 403/10004，不执行 UPDATE")
    void should_forbid_when_nonOwnerEdits() {
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));

        assertThatThrownBy(() -> itemService.updateItem(10L, validRequest(), OTHER_ID, Constants.ROLE_USER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(itemMapper, never()).update(any(Item.class));
    }

    @Test
    @DisplayName("修改：商品不存在返回 404/10005（404 优先于 403）")
    void should_notFound_when_updateMissingItem() {
        when(itemMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> itemService.updateItem(999L, validRequest(), OTHER_ID, Constants.ROLE_USER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("修改：并发下 UPDATE 影响行数为 0 归一 404")
    void should_notFound_when_updateAffectsZeroRows() {
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));
        when(itemMapper.update(any(Item.class))).thenReturn(0);

        assertThatThrownBy(() -> itemService.updateItem(10L, validRequest(), OWNER_ID, Constants.ROLE_USER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("删除：本人成功，执行物理删除")
    void should_delete_when_ownerDeletesOwnItem() {
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));
        when(itemMapper.deleteById(10L)).thenReturn(1);

        itemService.deleteItem(10L, OWNER_ID, Constants.ROLE_USER);

        verify(itemMapper).deleteById(10L);
    }

    @Test
    @DisplayName("删除：管理员可删他人商品")
    void should_delete_when_adminDeletesOthersItem() {
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));
        when(itemMapper.deleteById(10L)).thenReturn(1);

        itemService.deleteItem(10L, ADMIN_ID, Constants.ROLE_ADMIN);

        verify(itemMapper).deleteById(10L);
    }

    @Test
    @DisplayName("删除：非本人且非管理员返回 403/10004，不执行 DELETE")
    void should_forbid_when_nonOwnerDeletes() {
        when(itemMapper.selectById(10L)).thenReturn(persistedItem(10L, OWNER_ID));

        assertThatThrownBy(() -> itemService.deleteItem(10L, OTHER_ID, Constants.ROLE_USER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(itemMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("删除：商品不存在返回 404/10005")
    void should_notFound_when_deleteMissingItem() {
        when(itemMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> itemService.deleteItem(999L, OWNER_ID, Constants.ROLE_USER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("写操作：id 非正整数返回 400/10001")
    void should_reject_when_idNotPositive() {
        assertThatThrownBy(() -> itemService.deleteItem(0L, OWNER_ID, Constants.ROLE_USER))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID));
    }

    private ItemSaveRequest validRequest() {
        ItemSaveRequest request = new ItemSaveRequest();
        request.setItem("九成新机械键盘");
        request.setItemPrice(new BigDecimal("199.00"));
        request.setContactInfo("微信：kbd2026");
        return request;
    }

    private Item persistedItem(Long id, Long userId) {
        Item item = new Item();
        item.setId(id);
        item.setItem("九成新机械键盘");
        item.setItemPrice(new BigDecimal("199.00"));
        item.setContactInfo("微信：kbd2026");
        item.setUserId(userId);
        item.setCreatedAt(LocalDateTime.of(2026, 10, 23, 10, 0, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 10, 23, 10, 0, 0));
        return item;
    }
}
