package com.cqwu.cqwu_appointment_system.exception;

/**
 * 场所不可用异常
 * <p>
 * 当尝试预约一个状态并非“可用”的场所时抛出（例如，场所正在维护中）。
 * 通常应导致 HTTP 400 Bad Request 或 409 Conflict 响应。
 * </p>
 */
public class VenueNotAvailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 默认构造函数
     */
    public VenueNotAvailableException() {
        super("The venue is not available for reservation at this moment.");
    }

    /**
     * 带有指定详细消息的构造函数
     *
     * @param message 详细消息
     */
    public VenueNotAvailableException(String message) {
        super(message);
    }

    /**
     * 带有指定详细消息和原因的构造函数
     *
     * @param message 详细消息
     * @param cause   原因 (通常是另一个异常)
     */
    public VenueNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}