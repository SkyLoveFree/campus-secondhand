package com.campus.secondhand.interceptor;

import com.campus.secondhand.common.Constants;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 认证拦截器：{@code /api/**} 默认需要登录，白名单按「HTTP 方法 + 路径」放行。
 *
 * <p>白名单与 docs/api-spec.md 一致：注册、登录（POST）与商品列表、详情（GET）。
 * 未登录时直接写回统一 JSON（401/10002），不做重定向，避免前端拿到 HTML 登录页。</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    /** 后端接口统一前缀 */
    private static final String API_PREFIX = "/api";

    /** 白名单：方法 + Ant 风格路径同时匹配才放行 */
    private static final List<WhitelistRule> WHITELIST = List.of(
            new WhitelistRule(HttpMethod.POST, "/api/auth/register"),
            new WhitelistRule(HttpMethod.POST, "/api/auth/login"),
            new WhitelistRule(HttpMethod.GET, "/api/items"),
            new WhitelistRule(HttpMethod.GET, "/api/items/*")
    );

    private final ObjectMapper objectMapper;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = resolvePath(request);
        if (!path.startsWith(API_PREFIX)) {
            // 非接口请求（静态资源、错误页等）不参与认证
            return true;
        }
        if (isWhitelisted(request.getMethod(), path)) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(Constants.SESSION_USER_ID) == null) {
            writeUnauthorized(response);
            return false;
        }
        return true;
    }

    private boolean isWhitelisted(String method, String path) {
        for (WhitelistRule rule : WHITELIST) {
            if (rule.method().matches(method) && pathMatcher.match(rule.pathPattern(), path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取出去掉 contextPath 后的请求路径（本项目未配置 contextPath，保留兼容写法）。
     */
    private String resolvePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return uri;
        }
        return uri.substring(contextPath.length());
    }

    /**
     * 未登录：HTTP 401 + 业务码 10002，响应体与 Controller 返回结构一致。
     */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        log.debug("拦截未登录请求: {}", errorCode.getCode());
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.fail(errorCode.getCode(), errorCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * 白名单规则：HTTP 方法与路径模式需同时匹配。
     */
    private record WhitelistRule(HttpMethod method, String pathPattern) {
    }
}
