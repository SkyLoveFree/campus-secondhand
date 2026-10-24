package com.campus.secondhand.controller;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.common.GlobalExceptionHandler;
import com.campus.secondhand.dto.ItemSaveRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.interceptor.AuthInterceptor;
import com.campus.secondhand.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ItemController 写接口契约测试（F-08/F-09/F-10）：standalone MockMvc + mock ItemService。
 *
 * <p>覆盖：未登录 401/10002（拦截器）、发布成功与归属取 Session、Bean Validation 400/10001、
 * JSON 格式错误 400/10007、无权限 403/10004、不存在 404/10005、删除成功 data=null。</p>
 */
@DisplayName("ItemController 写接口契约测试（不依赖数据库）")
class ItemWriteControllerTest {

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
    @DisplayName("发布：未登录返回 401/10002，不进入 Service")
    void should_return401_when_createWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("发布：登录后 200 + code=0，归属取 Session 当前用户")
    void should_create_when_loggedIn() throws Exception {
        ItemVO vo = new ItemVO(10L, "九成新机械键盘", new BigDecimal("199.00"), "微信:kbd2026", 2L,
                LocalDateTime.of(2026, 10, 23, 12, 0, 0), LocalDateTime.of(2026, 10, 23, 12, 0, 0));
        when(itemService.createItem(any(ItemSaveRequest.class), eq(2L))).thenReturn(vo);

        mockMvc.perform(post("/api/items")
                        .session(loginSession(2L, Constants.ROLE_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.userId").value(2));

        verify(itemService).createItem(any(ItemSaveRequest.class), eq(2L));
    }

    @Test
    @DisplayName("发布：缺少名称与价格返回 400/10001 + fieldErrors")
    void should_return400_when_createBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/items")
                        .session(loginSession(2L, Constants.ROLE_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"\",\"itemPrice\":null,\"contactInfo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.item").exists())
                .andExpect(jsonPath("$.data.fieldErrors.itemPrice").exists())
                .andExpect(jsonPath("$.data.fieldErrors.contactInfo").exists());
    }

    @Test
    @DisplayName("发布：JSON 格式错误返回 400/10007")
    void should_return400when_jsonMalformed() throws Exception {
        mockMvc.perform(post("/api/items")
                        .session(loginSession(2L, Constants.ROLE_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10007));
    }

    @Test
    @DisplayName("修改：无权限返回 403/10004")
    void should_return403_when_updateForbidden() throws Exception {
        when(itemService.updateItem(eq(10L), any(ItemSaveRequest.class), eq(3L), eq(Constants.ROLE_USER)))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(put("/api/items/10")
                        .session(loginSession(3L, Constants.ROLE_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(10004))
                .andExpect(jsonPath("$.message").value("无权限执行该操作"));
    }

    @Test
    @DisplayName("修改：商品不存在返回 404/10005")
    void should_return404_when_updateMissing() throws Exception {
        when(itemService.updateItem(eq(999L), any(ItemSaveRequest.class), eq(2L), eq(Constants.ROLE_USER)))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(put("/api/items/999")
                        .session(loginSession(2L, Constants.ROLE_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(10005));
    }

    @Test
    @DisplayName("删除：未登录返回 401/10002")
    void should_return401_when_deleteWithoutLogin() throws Exception {
        mockMvc.perform(delete("/api/items/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));

        verifyNoInteractions(itemService);
    }

    @Test
    @DisplayName("删除：成功返回 200 + code=0 + data=null，角色从 Session 传入 Service")
    void should_delete_when_ownerLoggedIn() throws Exception {
        mockMvc.perform(delete("/api/items/10")
                        .session(loginSession(2L, Constants.ROLE_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verify(itemService).deleteItem(10L, 2L, Constants.ROLE_USER);
    }

    @Test
    @DisplayName("删除：管理员身份正确传递 ROLE_ADMIN")
    void should_passAdminRole_when_adminDeletes() throws Exception {
        mockMvc.perform(delete("/api/items/10")
                        .session(loginSession(1L, Constants.ROLE_ADMIN)))
                .andExpect(status().isOk());

        verify(itemService).deleteItem(10L, 1L, Constants.ROLE_ADMIN);
    }

    private MockHttpSession loginSession(Long userId, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.SESSION_USER_ID, userId);
        session.setAttribute(Constants.SESSION_USERNAME, "user" + userId);
        session.setAttribute(Constants.SESSION_ROLE, role);
        return session;
    }

    private String validBody() {
        return "{\"item\":\"九成新机械键盘\",\"itemPrice\":199.00,\"contactInfo\":\"微信:kbd2026\"}";
    }
}
