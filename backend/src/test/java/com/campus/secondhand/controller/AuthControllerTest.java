package com.campus.secondhand.controller;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.common.GlobalExceptionHandler;
import com.campus.secondhand.dto.LoginRequest;
import com.campus.secondhand.dto.RegisterRequest;
import com.campus.secondhand.dto.UserVO;
import com.campus.secondhand.interceptor.AuthInterceptor;
import com.campus.secondhand.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 接口契约测试：standalone MockMvc + mock AuthService，不启动 Spring 容器、不连接数据库。
 *
 * <p>覆盖：成功响应结构、参数校验 400/10001 + fieldErrors、JSON 格式错误 400/10007、
 * 登录失败 401/10003、未登录 401/10002、登录换新 Session、登出失效 Session、/me 读取 Session。</p>
 */
@DisplayName("AuthController 接口契约测试（不依赖数据库）")
class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addInterceptors(new AuthInterceptor(objectMapper))
                .build();
    }

    @Test
    @DisplayName("注册成功：200 + code=0，data 仅含 id/username/role，未知字段 role 被忽略")
    void should_returnUserVO_when_registerRequestValid() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(new UserVO(2L, "buyer_2024", "USER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"buyer_2024\",\"password\":\"abc12345\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.username").value("buyer_2024"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("注册参数非法：400 + 10001 + data.fieldErrors.username")
    void should_returnFieldErrors_when_registerRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"password\":\"abc12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.username").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("注册密码含空白字符：400 + 10001 + data.fieldErrors.password")
    void should_returnFieldErrors_when_registerPasswordHasWhitespace() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"buyer_2024\",\"password\":\"abc 12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("请求体 JSON 格式错误：400 + 10007")
    void should_returnRequestFormatError_when_jsonMalformed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"buyer_2024\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10007));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("登录成功：200 + 返回 UserVO，旧 Session 失效并写入新 Session 用户信息")
    void should_createNewSession_when_loginSuccess() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new UserVO(2L, "student", "USER"));
        MockHttpSession oldSession = new MockHttpSession();
        oldSession.setAttribute(Constants.SESSION_USER_ID, 1L);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(oldSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("student"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();

        assertThat(oldSession.isInvalid()).isTrue();
        MockHttpSession newSession = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(newSession).isNotNull();
        assertThat(newSession).isNotSameAs(oldSession);
        assertThat(newSession.getAttribute(Constants.SESSION_USER_ID)).isEqualTo(2L);
        assertThat(newSession.getAttribute(Constants.SESSION_USERNAME)).isEqualTo("student");
        assertThat(newSession.getAttribute(Constants.SESSION_ROLE)).isEqualTo("USER");
    }

    @Test
    @DisplayName("登录凭据错误：401 + 10003，统一提示「用户名或密码错误」")
    void should_returnLoginFailed_when_credentialsWrong() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.LOGIN_FAILED));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\",\"password\":\"wrong123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10003))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("登录缺少密码：400 + 10001 + data.fieldErrors.password")
    void should_returnFieldErrors_when_loginPasswordMissing() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001))
                .andExpect(jsonPath("$.data.fieldErrors.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("未登录调用登出：401 + 10002（拦截器返回统一 JSON）")
    void should_returnUnauthorized_when_logoutWithoutSession() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("登出成功：200 + code=0 + data=null，Session 失效")
    void should_invalidateSession_when_logoutSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.SESSION_USER_ID, 2L);
        session.setAttribute(Constants.SESSION_USERNAME, "student");
        session.setAttribute(Constants.SESSION_ROLE, "USER");

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("未登录获取当前用户：401 + 10002")
    void should_returnUnauthorized_when_meWithoutSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("已登录获取当前用户：200 + 从 Session 组装 UserVO（不查库）")
    void should_returnCurrentUser_when_meWithSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.SESSION_USER_ID, 2L);
        session.setAttribute(Constants.SESSION_USERNAME, "student");
        session.setAttribute(Constants.SESSION_ROLE, "USER");
        when(authService.getCurrentUser(2L, "student", "USER")).thenReturn(new UserVO(2L, "student", "USER"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.username").value("student"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        verify(authService).getCurrentUser(2L, "student", "USER");
    }

    @Test
    @DisplayName("已登录调用注册接口不影响当前登录态（注册不自动登录）")
    void should_keepCurrentSession_when_registerWhileLoggedIn() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(new UserVO(3L, "new_user", "USER"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(Constants.SESSION_USER_ID, 1L);

        mockMvc.perform(post("/api/auth/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_user\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3));

        assertThat(session.isInvalid()).isFalse();
        assertThat(session.getAttribute(Constants.SESSION_USER_ID)).isEqualTo(1L);
    }

    @Test
    @DisplayName("请求方法不支持：405 + 10008")
    void should_returnMethodNotAllowed_when_methodMismatch() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(10008));
    }
}
