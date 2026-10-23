package com.campus.secondhand.controller;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.LoginRequest;
import com.campus.secondhand.dto.RegisterRequest;
import com.campus.secondhand.dto.UserVO;
import com.campus.secondhand.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、登出、获取当前用户（对应 docs/api-spec.md 3.1-3.4）。
 *
 * <p>Controller 只负责路由、参数校验与 Session 读写；业务规则在 {@link AuthService}。
 * Session 操作集中在本层，保证 Service 不依赖 Servlet API。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册（公开）：成功 HTTP 200 + code=0，返回 {@code {id, username, role}}，不自动登录。
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 用户登录（公开）：校验成功后失效旧 Session 再创建新 Session，防止会话固定。
     */
    @PostMapping("/login")
    public Result<UserVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        UserVO user = authService.login(request);
        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(Constants.SESSION_USER_ID, user.getId());
        session.setAttribute(Constants.SESSION_USERNAME, user.getUsername());
        session.setAttribute(Constants.SESSION_ROLE, user.getRole());
        return Result.success(user);
    }

    /**
     * 用户登出（需登录）：Session 失效，成功返回 data=null；未登录由拦截器返回 401/10002。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        session.invalidate();
        return Result.success();
    }

    /**
     * 获取当前用户（需登录）：从 Session 读取，不查库，用于前端刷新后恢复登录态。
     */
    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = (Long) session.getAttribute(Constants.SESSION_USER_ID);
        String username = (String) session.getAttribute(Constants.SESSION_USERNAME);
        String role = (String) session.getAttribute(Constants.SESSION_ROLE);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Result.success(authService.getCurrentUser(userId, username, role));
    }
}
