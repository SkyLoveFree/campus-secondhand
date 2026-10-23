package com.campus.secondhand.common;

/**
 * 统一响应结构 {@code {code, message, data}}。
 *
 * <p>成功一律 HTTP 200 + {@code code=0}；失败由全局异常处理按错误码设置 HTTP 状态。
 * {@code data} 为 null 时也保留字段（如登出成功返回 {@code "data":null}）。</p>
 *
 * @param <T> 业务数据类型
 */
public class Result<T> {

    /** 成功业务码 */
    public static final int SUCCESS_CODE = 0;

    /** 成功提示文案 */
    public static final String SUCCESS_MESSAGE = "ok";

    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（包含业务数据）。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 成功响应（无业务数据，序列化为 {@code "data":null}）。
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败响应（无附加数据）。
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（附带数据，例如参数校验的 {@code fieldErrors}）。
     */
    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, message, data);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
