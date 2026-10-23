package com.campus.secondhand.service;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.dto.LoginRequest;
import com.campus.secondhand.dto.RegisterRequest;
import com.campus.secondhand.dto.UserVO;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AuthServiceImpl 单元测试：mock UserMapper，使用真实 BCryptPasswordEncoder，不启动 Spring、不连接数据库。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 单元测试（不依赖数据库）")
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "abc12345";

    /** 复用同一编码器生成测试用哈希（与 PasswordConfig 的 cost 10 一致） */
    private static final PasswordEncoder TEST_ENCODER = new BCryptPasswordEncoder(10);
    private static final String PASSWORD_HASH = TEST_ENCODER.encode(RAW_PASSWORD);

    @Mock
    private UserMapper userMapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userMapper, new BCryptPasswordEncoder(10));
    }

    @Test
    @DisplayName("注册成功：角色固定 USER，密码以 BCrypt 哈希入库且不落明文")
    void register_should_createNormalUserWithBcryptHash_when_usernameAvailable() {
        when(userMapper.countByUsername("buyer_2024")).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        });

        UserVO userVO = authService.register(registerRequest("buyer_2024", RAW_PASSWORD));

        assertThat(userVO.getId()).isEqualTo(2L);
        assertThat(userVO.getUsername()).isEqualTo("buyer_2024");
        assertThat(userVO.getRole()).isEqualTo("USER");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(TEST_ENCODER.matches(RAW_PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("注册时用户名已存在：409/10006，且不执行插入")
    void register_should_throwUsernameExists_when_usernameAlreadyTaken() {
        when(userMapper.countByUsername("student")).thenReturn(1L);

        BusinessException ex = catchThrowableOfType(
                () -> authService.register(registerRequest("student", RAW_PASSWORD)), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USERNAME_EXISTS);
        assertThat(ex.getErrorCode().getCode()).isEqualTo(10006);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册并发唯一键冲突（插入抛 DuplicateKeyException）：统一按 409/10006 处理")
    void register_should_throwUsernameExists_when_insertHitsDuplicateKey() {
        when(userMapper.countByUsername("buyer_2024")).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uk_user_username"));

        BusinessException ex = catchThrowableOfType(
                () -> authService.register(registerRequest("buyer_2024", RAW_PASSWORD)), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USERNAME_EXISTS);
    }

    @Test
    @DisplayName("注册用户名非法（绕过 @Valid 直接调 Service）：400/10001 + fieldErrors.username")
    void register_should_rejectInvalidUsername_when_serviceValidatesAgain() {
        BusinessException ex = catchThrowableOfType(
                () -> authService.register(registerRequest("ab", RAW_PASSWORD)), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        assertThat(ex.getErrorCode().getCode()).isEqualTo(10001);
        assertThat(ex.getData()).isInstanceOf(Map.class);
        Map<?, ?> data = (Map<?, ?>) ex.getData();
        assertThat(data.get("fieldErrors")).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) data.get("fieldErrors")).containsKey("username")).isTrue();
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("注册密码含空白字符（绕过 @Valid）：400/10001 + fieldErrors.password")
    void register_should_rejectPasswordWithWhitespace_when_serviceValidatesAgain() {
        BusinessException ex = catchThrowableOfType(
                () -> authService.register(registerRequest("buyer_2024", "abc 12345")), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        Map<?, ?> fieldErrors = (Map<?, ?>) ((Map<?, ?>) ex.getData()).get("fieldErrors");
        assertThat(fieldErrors.containsKey("password")).isTrue();
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("注册用户名首尾空格：trim 后再查重与入库")
    void register_should_trimUsername_beforeQueryAndInsert() {
        when(userMapper.countByUsername("buyer_2024")).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return 1;
        });

        UserVO userVO = authService.register(registerRequest("  buyer_2024  ", RAW_PASSWORD));

        assertThat(userVO.getUsername()).isEqualTo("buyer_2024");
        verify(userMapper).countByUsername("buyer_2024");
    }

    @Test
    @DisplayName("登录成功：BCrypt 校验通过并返回 UserVO")
    void login_should_returnUserVO_when_credentialsValid() {
        when(userMapper.selectByUsername("student")).thenReturn(existingUser(2L, "student", "USER"));

        UserVO userVO = authService.login(loginRequest("student", RAW_PASSWORD));

        assertThat(userVO.getId()).isEqualTo(2L);
        assertThat(userVO.getUsername()).isEqualTo("student");
        assertThat(userVO.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("登录时用户名首尾空格：trim 后查询")
    void login_should_trimUsername_when_requestHasSpaces() {
        when(userMapper.selectByUsername("student")).thenReturn(existingUser(2L, "student", "USER"));

        UserVO userVO = authService.login(loginRequest("  student  ", RAW_PASSWORD));

        assertThat(userVO.getUsername()).isEqualTo("student");
        verify(userMapper).selectByUsername("student");
    }

    @Test
    @DisplayName("登录账号不存在：401/10003，统一提示“用户名或密码错误”")
    void login_should_throwLoginFailed_when_userNotFound() {
        when(userMapper.selectByUsername("nobody")).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> authService.login(loginRequest("nobody", RAW_PASSWORD)), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAILED);
        assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
    }

    @Test
    @DisplayName("登录密码错误：401/10003，与账号不存在提示一致")
    void login_should_throwLoginFailed_when_passwordMismatch() {
        when(userMapper.selectByUsername("student")).thenReturn(existingUser(2L, "student", "USER"));

        BusinessException ex = catchThrowableOfType(
                () -> authService.login(loginRequest("student", "wrong123")), BusinessException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAILED);
        assertThat(ex.getMessage()).isEqualTo("用户名或密码错误");
    }

    @Test
    @DisplayName("获取当前用户：直接用 Session 信息组装 UserVO，不查库")
    void getCurrentUser_should_assembleUserVO_withoutQueryingDatabase() {
        UserVO userVO = authService.getCurrentUser(2L, "student", "USER");

        assertThat(userVO.getId()).isEqualTo(2L);
        assertThat(userVO.getUsername()).isEqualTo("student");
        assertThat(userVO.getRole()).isEqualTo("USER");
        verifyNoInteractions(userMapper);
    }

    private RegisterRequest registerRequest(String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private User existingUser(long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(PASSWORD_HASH);
        user.setRole(role);
        return user;
    }
}
