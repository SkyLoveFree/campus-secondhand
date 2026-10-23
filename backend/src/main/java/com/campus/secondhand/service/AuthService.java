package com.campus.secondhand.service;

import com.campus.secondhand.dto.LoginRequest;
import com.campus.secondhand.dto.RegisterRequest;
import com.campus.secondhand.dto.UserVO;

/**
 * 认证业务接口：注册、登录校验、组装当前用户。
 *
 * <p>Service 不依赖 {@code HttpServletRequest}/Session：Session 的创建、失效与属性读写
 * 统一由 Controller 负责（本接口只返回校验后的用户信息）。</p>
 */
public interface AuthService {

    /**
     * 注册普通用户（role 固定 USER，不自动登录）。
     *
     * @return 新用户信息 {@code {id, username, role}}
     * @throws com.campus.secondhand.common.BusinessException 用户名已存在（409/10006）、参数非法（400/10001）
     */
    UserVO register(RegisterRequest request);

    /**
     * 校验用户名与密码，成功返回用户信息由 Controller 写入 Session。
     *
     * @throws com.campus.secondhand.common.BusinessException 凭据错误统一 401/10003
     */
    UserVO login(LoginRequest request);

    /**
     * 组装当前登录用户（本期直接使用 Session 中的信息，不查库）。
     */
    UserVO getCurrentUser(Long userId, String username, String role);
}
