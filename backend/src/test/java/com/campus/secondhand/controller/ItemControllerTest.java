package com.campus.secondhand.controller;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.common.GlobalExceptionHandler;
import com.campus.secondhand.dto.ItemQueryRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.dto.PageResult;
import com.campus.secondhand.interceptor.AuthInterceptor;
import com.campus.secondhand.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ItemController 接口契约测试：standalone MockMvc + mock ItemService，不启动 Spring 容器、不连接数据库。
 *
 * <p>覆盖：公开访问（无 Session）、列表默认值/参数透传、参数非法 400/10001 + fieldErrors、
 * 详情成功（时间格式 yyyy-MM-dd HH:mm:ss、价格 JSON number）、不存在 404/10005、id 非数字/非正 400/10001。</p>
 */
@DisplayName("ItemController 接口契约测试（不依赖数据库）")
class ItemControllerTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 22, 9, 12, 35);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ItemService itemService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        itemService = mock(ItemService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ItemController(itemService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addInterceptors(new AuthInterceptor(objectMapper))
                .build();
    }

    @Test
    @DisplayName("列表默认值：无参数时 page=1/size=10/keyword=null/sort=null，响应含 list/total/page/size")
    void should_useDefaultQuery_when_listWithoutParams() throws Exception {
        when(itemService.listItems(any(ItemQueryRequest.class))).thenReturn(samplePageResult());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.list[0].id").value(2))
                .andExpect(jsonPath("$.data.list[0].item").value("高等数学教材（同济第七版）"))
                .andExpect(jsonPath("$.data.list[0].userId").value(2))
                .andExpect(jsonPath("$.data.list[0].contactInfo").value("QQ：100000001"))
                // 时间必须是 yyyy-MM-dd HH:mm:ss 格式，价格必须是 JSON number（保留两位小数）
                .andExpect(content().string(containsString("\"createdAt\":\"2026-10-22 09:12:35\"")))
                .andExpect(content().string(containsString("\"updatedAt\":\"2026-10-22 09:12:35\"")))
                .andExpect(content().string(containsString("\"itemPrice\":25.50")));

        ArgumentCaptor<ItemQueryRequest> captor = ArgumentCaptor.forClass(ItemQueryRequest.class);
        verify(itemService).listItems(captor.capture());
        ItemQueryRequest captured = captor.getValue();
        assertThat(captured.getPage()).isEqualTo(1);
        assertThat(captured.getSize()).isEqualTo(10);
        assertThat(captured.getKeyword()).isNull();
        assertThat(captured.getSort()).isNull();
    }

    @Test
    @DisplayName("列表参数透传：page/size/keyword/sort 正确绑定并传给 Service")
    void should_passQueryParams_when_listHasParams() throws Exception {
        when(itemService.listItems(any(ItemQueryRequest.class))).thenReturn(samplePageResult());

        mockMvc.perform(get("/api/items")
                        .param("page", "2")
                        .param("size", "5")
                        .param("keyword", "数学")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<ItemQueryRequest> captor = ArgumentCaptor.forClass(ItemQueryRequest.class);
        verify(itemService).listItems(captor.capture());
        ItemQueryRequest captured = captor.getValue();
        assertThat(captured.getPage()).isEqualTo(2);
        assertThat(captured.getSize()).isEqualTo(5);
        assertThat(captured.getKeyword()).isEqualTo("数学");
        assertThat(captured.getSort()).isEqualTo("price_asc");
    }

    @Test
    @DisplayName("列表参数非法：page=0 → 400/10001 + data.fieldErrors.page")
    void should_returnFieldErrors_when_pageInvalid() throws Exception {
        mockMvc.perform(get("/api/items").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.page").exists());

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("列表参数非法：size=100 → 400/10001 + data.fieldErrors.size")
    void should_returnFieldErrors_when_sizeOutOfRange() throws Exception {
        mockMvc.perform(get("/api/items").param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.size").exists());

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("列表参数非法：sort=abc → 400/10001 + data.fieldErrors.sort")
    void should_returnFieldErrors_when_sortInvalid() throws Exception {
        mockMvc.perform(get("/api/items").param("sort", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.sort").exists());

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("列表参数非法：keyword 超过 50 字符 → 400/10001 + data.fieldErrors.keyword")
    void should_returnFieldErrors_when_keywordTooLong() throws Exception {
        mockMvc.perform(get("/api/items").param("keyword", "x".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.keyword").exists());

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("列表参数类型错误：page=abc → 400/10001 + 中文类型提示")
    void should_returnChineseBindingError_when_pageNotNumber() throws Exception {
        mockMvc.perform(get("/api/items").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.page").value("参数类型不正确"));

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("详情成功：返回 ItemVO，时间格式与价格格式符合契约")
    void should_returnItemDetail_when_idExists() throws Exception {
        when(itemService.getItem(1L)).thenReturn(sampleItemVO(1L, "二手自行车（九成新）", "280.00", "微信：student_demo"));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.item").value("二手自行车（九成新）"))
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.contactInfo").value("微信：student_demo"))
                .andExpect(content().string(containsString("\"itemPrice\":280.00")))
                .andExpect(content().string(containsString("\"createdAt\":\"2026-10-22 09:12:35\"")));

        verify(itemService).getItem(1L);
    }

    @Test
    @DisplayName("详情不存在：404/10005「商品不存在或已删除」")
    void should_returnNotFound_when_itemMissing() throws Exception {
        when(itemService.getItem(999999L))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/items/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(10005))
                .andExpect(jsonPath("$.message").value("商品不存在或已删除"))
                .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    @DisplayName("详情 id 非数字：400/10001 + data.fieldErrors.id")
    void should_returnParamInvalid_when_idNotNumber() throws Exception {
        mockMvc.perform(get("/api/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.id").value("参数类型不正确"));

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("详情 id 非正整数：400/10001 + data.fieldErrors.id")
    void should_returnParamInvalid_when_idNotPositive() throws Exception {
        when(itemService.getItem(0L))
                .thenThrow(new BusinessException(ErrorCode.PARAM_INVALID, "参数校验失败",
                        fieldErrorsData("id", "商品ID必须为正整数")));

        mockMvc.perform(get("/api/items/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.id").value("商品ID必须为正整数"));
    }

    private PageResult<ItemVO> samplePageResult() {
        ItemVO itemVO = sampleItemVO(2L, "高等数学教材（同济第七版）", "25.50", "QQ：100000001");
        return new PageResult<>(List.of(itemVO), 1L, 1, 10);
    }

    private ItemVO sampleItemVO(long id, String name, String price, String contactInfo) {
        return new ItemVO(id, name, new BigDecimal(price), contactInfo, 2L, CREATED_AT, CREATED_AT);
    }

    private Map<String, Object> fieldErrorsData(String field, String message) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, message);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fieldErrors", fieldErrors);
        return data;
    }
}
