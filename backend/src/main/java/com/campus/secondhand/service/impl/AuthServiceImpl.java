package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.dto.LoginRequest;
import com.campus.secondhand.dto.RegisterRequest;
import com.campus.secondhand.dto.UserVO;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 认证业务实现。
 *
 * <p>要点：注册先查重（409）再 BCrypt 加密（cost 10）入库，并发唯一键冲突兜底为 409；
 * 登录失败统一提示「用户名或密码错误」，不区分账号不存在与密码错误。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 用户名允许的字符：字母、数字、下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    /** 密码禁止包含空白字符 */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 20;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 20;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        // @Valid 已做第一层校验；Service 再做二次校验，避免绕过 Controller 直接调用时写入非法数据
        String username = validateUsername(request.getUsername());
        String password = validatePassword(request.getPassword());

        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Constants.ROLE_USER);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 先查后插在并发下仍可能违反唯一键；按契约统一返回 409/10006
            log.warn("注册用户名唯一键冲突: username={}", username);
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        log.info("用户注册成功: userId={}", user.getId());
        return new UserVO(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public UserVO login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        // 防御性校验：正常链路由 @Valid 拦截，此处避免 Mapper 收到空用户名
        if (username == null || username.isEmpty() || password == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            // 账号不存在与密码错误返回同一提示，避免暴露账号是否已注册
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        log.info("用户登录成功: userId={}", user.getId());
        return new UserVO(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public UserVO getCurrentUser(Long userId, String username, String role) {
        return new UserVO(userId, username, role);
    }

    /**
     * 校验并返回 trim 后的用户名。
     */
    private String validateUsername(String rawUsername) {
        String username = rawUsername == null ? null : rawUsername.trim();
        boolean invalidLength = username == null
                || username.length() < USERNAME_MIN_LENGTH
                || username.length() > USERNAME_MAX_LENGTH;
        if (invalidLength || !USERNAME_PATTERN.matcher(username).matches()) {
            throw paramInvalid("username", "用户名需为 3-20 位字母、数字或下划线");
        }
        return username;
    }

    /**
     * 校验密码（不 trim，密码中出现空白字符即不合法）。
     */
    private String validatePassword(String password) {
        boolean invalidLength = password == null
                || password.length() < PASSWORD_MIN_LENGTH
                || password.length() > PASSWORD_MAX_LENGTH;
        if (invalidLength || WHITESPACE_PATTERN.matcher(password).find()) {
            throw paramInvalid("password", "密码需为 6-20 位且不能包含空白字符");
        }
        return password;
    }

    /**
     * 构造参数校验异常，data.fieldErrors 与全局异常处理的格式保持一致。
     */
    private BusinessException paramInvalid(String field, String message) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, message);
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        data.put("fieldErrors", fieldErrors);
        return new BusinessException(ErrorCode.PARAM_INVALID, ErrorCode.PARAM_INVALID.getMessage(), data);
    }
}
