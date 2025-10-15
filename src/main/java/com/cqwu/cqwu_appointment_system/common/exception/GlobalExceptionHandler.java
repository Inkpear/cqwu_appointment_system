package com.cqwu.cqwu_appointment_system.common.exception;

import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice // 声明这是一个用于 @RestController 的全局增强组件
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常 (IllegalArgumentException)
     * 当 Service 层的 Assert.notNull() 等断言失败时，会抛出此异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 设置 HTTP 响应状态码为 400
    public AjaxResult handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("参数校验失败: {}", e.getMessage());
        // 返回一个包含具体错误信息的 AjaxResult 对象
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    /**
     * 处理请求体丢失或格式错误的异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("请求体为空或JSON格式错误: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), "请求体不能为空且必须为有效的JSON格式");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE) // 设置 HTTP 状态码为 415
    public AjaxResult handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        // log.error("不支持的媒体类型: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "不支持的内容类型，请使用 application/json");
    }

    /**
     * 处理 @Valid 注解校验失败的异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 从异常中获取第一个校验失败的错误信息
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("数据校验失败: {}", message);
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理请求方法不支持的异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED) // 设置 HTTP 状态码为 405
    public AjaxResult handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("请求方法不支持: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "不支持的请求方法 '" + e.getMethod() + "'");
    }

    /**
     * 处理请求路径未找到的异常 (HTTP 404)
     * 当 Spring MVC 找不到任何可以处理请求的 Controller 方法时触发
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<AjaxResult> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("请求的资源不存在: {}", e.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(AjaxResult.error(HttpStatus.NOT_FOUND.value(), "您访问的路径不存在"));
    }

    /**
     * 兜底处理所有其他未被捕获的异常
     * 这是防止系统内部错误（如 NullPointerException）直接暴露给前端的最后一道防线
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 设置 HTTP 响应状态码为 500
    public AjaxResult handleAllExceptions(Exception e) {
        log.error("服务器内部发生未知错误: ", e); // 记录完整的异常堆栈信息
        return AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误，请联系管理员");
    }
}