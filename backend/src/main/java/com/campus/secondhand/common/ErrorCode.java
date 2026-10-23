package com.campus.secondhand.common;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码与对应的 HTTP 状态码。
 *
 * <p>与 docs/api-spec.md 第 1 节错误码表保持一致；新增或修改错误码必须先同步契约文档。</p>
 */
public enum ErrorCode {

    /** 参数校验失败：字段缺失、格式、长度、范围、枚举不合法 */
    PARAM_INVALID(10001, HttpStatus.BAD_REQUEST, "参数校验失败"),

    /** 未登录或 Session 已过期 */
    UNAUTHORIZED(10002, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),

    /** 登录失败：用户名不存在与密码错误统一提示，避免暴露账号是否注册 */
    LOGIN_FAILED(10003, HttpStatus.UNAUTHORIZED, "用户名或密码错误"),

    /** 已登录但无操作权限 */
    FORBIDDEN(10004, HttpStatus.FORBIDDEN, "无权限"),

    /** 资源不存在或已删除 */
    NOT_FOUND(10005, HttpStatus.NOT_FOUND, "资源不存在"),

    /** 用户名已存在（含并发唯一键冲突） */
    USERNAME_EXISTS(10006, HttpStatus.CONFLICT, "用户名已存在"),

    /** 请求体 JSON 解析失败或字段类型错误 */
    REQUEST_FORMAT_ERROR(10007, HttpStatus.BAD_REQUEST, "请求格式错误"),

    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(10008, HttpStatus.METHOD_NOT_ALLOWED, "方法不支持"),

    /** 未预期异常：只记录日志，响应不返回堆栈 */
    INTERNAL_ERROR(10500, HttpStatus.INTERNAL_SERVER_ERROR, "服务器错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
