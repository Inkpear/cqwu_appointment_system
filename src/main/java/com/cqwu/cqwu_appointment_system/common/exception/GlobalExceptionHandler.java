package com.cqwu.cqwu_appointment_system.common.exception;

import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.exception.PermissionDeniedException;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务断言等原因导致的参数错误 (IllegalArgumentException)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    /**
     * 处理对象状态不合法导致的异常 (IllegalStateException)
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleIllegalStateException(IllegalStateException e) {
        log.warn("试图对非法状态的对象进行操作: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    
    /**
     * 处理请求体丢失或格式错误的异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体为空或数据格式错误: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), "请求体不能为空或数据格式错误");
    }

    /**
     * 处理不支持的媒体类型异常 (Content-Type)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public AjaxResult handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的媒体类型: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "不支持的内容类型");
    }

    /**
     * 处理 @Valid 注解校验失败的异常 (RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("数据校验失败: {}", message);
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理请求方法不支持的异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public AjaxResult handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "不支持的请求方法 '" + e.getMethod() + "'");
    }

    /**
     * 处理请求路径未找到的异常 (404 Not Found)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<AjaxResult> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("请求的资源不存在: {}", e.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(AjaxResult.error(HttpStatus.NOT_FOUND.value(), "您访问的路径不存在"));
    }

    /**
     * 处理 Spring 6.1+ 引入的方法参数 @Valid 校验异常
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String message = e.getParameterValidationResults().stream()
                .map(ParameterValidationResult::getResolvableErrors)
                .flatMap(Collection::stream)
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");

        log.warn("方法参数校验失败: {}", message);
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理权限不足异常 (403 Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN.value(), "对不起，您没有权限访问此资源");
    }

    /**
     * 处理数据访问超时异常 (例如 Redis 或数据库连接超时)
     */
    @ExceptionHandler(QueryTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public AjaxResult handleQueryTimeoutException(QueryTimeoutException e) {
        log.error("数据访问超时，可能是数据库或缓存服务（如Redis）响应缓慢: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "服务暂时不可用，请稍后重试");
    }

    /**
     * 处理自定义的预约时间冲突异常 (409 Conflict)
     */
    @ExceptionHandler(ReservationConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AjaxResult handleReservationConflictException(ReservationConflictException e) {
        log.warn("预约时间冲突: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    /**
     * 处理自定义的权限不足异常 (403 Forbidden)
     * 注意：这可以与 Spring Security 的 AccessDeniedException 共存，处理更具体的业务权限场景。
     */
    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public AjaxResult handlePermissionDeniedException(PermissionDeniedException e) {
        log.warn("业务权限不足: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
    }

    /**
     * 处理 Spring Security 认证过程中抛出的异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public AjaxResult handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return AjaxResult.error(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
    }

    /**
     * 处理线程中断异常 (InterruptedException)。
     * 当一个操作（如 I/O 或长时间等待）被中断时抛出。
     * 通常表示服务暂时无法处理请求。
     */
    @ExceptionHandler(InterruptedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public AjaxResult handleInterruptedException(InterruptedException e) {
        log.warn("请求处理过程中线程被中断: {}", e.getMessage());
        Thread.currentThread().interrupt();
        return AjaxResult.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "服务正忙，请稍后重试");
    }

    /**
     * 处理 I/O 异常 (IOException)。
     * 通常发生在文件读写等操作失败时。
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(IOException e, HttpServletRequest request, HttpServletResponse response) {
        // 典型场景：SSE 客户端主动断开连接，容器抛出 IOException（如 Broken pipe）。
        // 当响应 Content-Type 为 text/event-stream 或 Accept 包含该类型时，不要再尝试写 JSON。
        String accept = request.getHeader("Accept");
        String contentType = response.getContentType();
        boolean isSse = (accept != null && accept.contains("text/event-stream"))
                || (contentType != null && contentType.contains("text/event-stream"));

        if (isSse) {
            // 对于 SSE：返回 204 No Content，静默结束，避免 HttpMessageNotWritableException。
            log.warn("SSE 连接 I/O 中断，静默结束: {}", e.toString());
            return ResponseEntity.noContent().build();
        }

        log.error("服务器发生 I/O 错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部发生 I/O 错误"));
    }

    /**
     * 处理请求中缺少必需 @RequestParam 参数的异常 (400 Bad Request)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("请求缺少必需的参数: '%s'", e.getParameterName());
        log.warn(message);
        return AjaxResult.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 兜底处理所有其他未被捕获的服务器内部异常 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public AjaxResult handleAllExceptions(Exception e) {
        log.error("服务器内部发生未知错误: ", e);
        return AjaxResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器繁忙，请稍后重试");
    }
}