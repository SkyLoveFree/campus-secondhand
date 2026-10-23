package com.campus.secondhand.interceptor;

import com.campus.secondhand.common.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthInterceptor 单元测试：校验白名单按「方法 + 路径」放行，未登录返回统一 JSON 401/10002。
 */
@DisplayName("AuthInterceptor 单元测试（不依赖数据库）")
class AuthInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setUp() {
        authInterceptor = new AuthInterceptor(objectMapper);
    }

    @Test
    @DisplayName("白名单：POST /api/auth/register 未登录可访问")
    void should_allowPostRegister_when_notLoggedIn() throws Exception {
        assertThat(preHandle("POST", "/api/auth/register")).isTrue();
    }

    @Test
    @DisplayName("白名单：POST /api/auth/login 未登录可访问")
    void should_allowPostLogin_when_notLoggedIn() throws Exception {
        assertThat(preHandle("POST", "/api/auth/login")).isTrue();
    }

    @Test
    @DisplayName("白名单：GET /api/items 与 GET /api/items/12 未登录可访问")
    void should_allowGetItemsAndDetail_when_notLoggedIn() throws Exception {
        assertThat(preHandle("GET", "/api/items")).isTrue();
        assertThat(preHandle("GET", "/api/items/12")).isTrue();
    }

    @Test
    @DisplayName("白名单按方法区分：POST /api/items 未登录返回 401/10002")
    void should_rejectPostItems_when_notLoggedIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/items");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(10002);
        assertThat(body.get("message").asText()).isNotBlank();
        assertThat(body.get("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("未登录访问 GET /api/auth/me：拦截并返回 JSON 401/10002（不重定向）")
    void should_rejectMe_when_notLoggedIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asInt()).isEqualTo(10002);
    }

    @Test
    @DisplayName("未登录访问 POST /api/auth/logout：返回 401/10002")
    void should_rejectLogout_when_notLoggedIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(authInterceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("已登录（Session 有 currentUserId）访问 /api/auth/me 放行")
    void should_allowMe_when_sessionHasCurrentUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.getSession(true).setAttribute(Constants.SESSION_USER_ID, 2L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(authInterceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("非 /api 请求（静态资源、错误页）不参与认证")
    void should_allowNonApiPath_when_notLoggedIn() throws Exception {
        assertThat(preHandle("GET", "/index.html")).isTrue();
        assertThat(preHandle("GET", "/error")).isTrue();
    }

    private boolean preHandle(String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        return authInterceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }
}
