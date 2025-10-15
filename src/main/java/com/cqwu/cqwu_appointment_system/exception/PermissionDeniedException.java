package com.cqwu.cqwu_appointment_system.exception;

/**
 * 权限不足异常
 * <p>
 * 当用户尝试执行其角色或身份不允许的操作时抛出。
 * 通常应导致 HTTP 403 Forbidden 响应。
 * </p>
 */
public class PermissionDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 默认构造函数
     */
    public PermissionDeniedException() {
        super("Permission denied. You do not have the required authorization to perform this action.");
    }

    /**
     * 带有指定详细消息的构造函数
     *
     * @param message 详细消息
     */
    public PermissionDeniedException(String message) {
        super(message);
    }

    /**
     * 带有指定详细消息和原因的构造函数
     *
     * @param message 详细消息
     * @param cause   原因 (通常是另一个异常)
     */
    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}