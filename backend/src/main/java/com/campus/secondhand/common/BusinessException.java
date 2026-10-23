package com.campus.secondhand.common;

/**
 * 业务异常：由 Service/Controller 主动抛出，交给全局异常处理器转换为统一错误响应。
 *
 * <p>继承 {@link RuntimeException}，不强制调用方捕获；异常中的 message 必须是可直接展示的中文。</p>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 附加数据，例如参数校验的 fieldErrors；默认 null */
    private final Object data;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
