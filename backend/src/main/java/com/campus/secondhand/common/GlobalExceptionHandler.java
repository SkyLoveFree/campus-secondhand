package com.campus.secondhand.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器：把 Controller/Service 抛出的异常统一转换为 {@code {code, message, data}}。
 *
 * <p>约束：响应不返回堆栈、SQL、密码；未预期异常只记日志并返回 500/10500。
 * HTTP 状态码与业务码的对应关系见 docs/api-spec.md 第 1 节。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：按错误码返回对应 HTTP 状态与业务码。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Result<Object> body = Result.fail(errorCode.getCode(), ex.getMessage(), ex.getData());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    /**
     * 参数校验失败：HTTP 400 + 10001，data.fieldErrors 给出字段错误。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, Map<String, String>>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // 同一字段可能有多个校验注解，只保留第一条提示，避免前端展示混乱
            fieldErrors.putIfAbsent(fieldError.getField(), resolveFieldErrorMessage(fieldError));
        }
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        data.put("fieldErrors", fieldErrors);
        Result<Map<String, Map<String, String>>> body = Result.fail(errorCode.getCode(), errorCode.getMessage(), data);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    /**
     * 字段错误提示：查询参数类型转换失败（如 page=abc）时 Spring 默认提示为英文，统一改为中文。
     */
    private String resolveFieldErrorMessage(FieldError fieldError) {
        if (fieldError.isBindingFailure()) {
            return "参数类型不正确";
        }
        return fieldError.getDefaultMessage();
    }

    /**
     * 请求体 JSON 解析失败或字段类型错误：HTTP 400 + 10007。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.REQUEST_FORMAT_ERROR;
        return buildErrorResponse(errorCode);
    }

    /**
     * 请求方法不匹配：HTTP 405 + 10008。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("请求方法不支持: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        return buildErrorResponse(errorCode);
    }

    /**
     * 参数类型不匹配（如路径变量 id 传入非数字）：HTTP 400 + 10001 + fieldErrors。
     * 商品接口在 M3/M4 实现后使用。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Map<String, Map<String, String>>>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getName(), "参数类型不正确");
        ErrorCode errorCode = ErrorCode.PARAM_INVALID;
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        data.put("fieldErrors", fieldErrors);
        Result<Map<String, Map<String, String>>> body = Result.fail(errorCode.getCode(), errorCode.getMessage(), data);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    /**
     * 路径不存在（Spring Boot 3.2+ 未映射静态资源/接口）：HTTP 404 + 10005。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("请求资源不存在: {}", ex.getResourcePath());
        ErrorCode errorCode = ErrorCode.NOT_FOUND;
        return buildErrorResponse(errorCode);
    }

    /**
     * 兜底异常：记录完整堆栈便于排查，但响应不暴露任何内部细节。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception ex) {
        log.error("未预期异常: {}", ex.getMessage(), ex);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return buildErrorResponse(errorCode);
    }

    private ResponseEntity<Result<Object>> buildErrorResponse(ErrorCode errorCode) {
        Result<Object> body = Result.fail(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }
}
