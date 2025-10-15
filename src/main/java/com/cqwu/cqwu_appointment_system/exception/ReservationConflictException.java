package com.cqwu.cqwu_appointment_system.exception;

/**
 * 预约冲突异常
 * <p>
 * 当尝试创建一个与现有预约在时间上重叠的预约时抛出。
 * 通常应导致 HTTP 409 Conflict 响应。
 * </p>
 */
public class ReservationConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 默认构造函数
     */
    public ReservationConflictException() {
        super("Reservation time conflicts with an existing reservation.");
    }

    /**
     * 带有指定详细消息的构造函数
     *
     * @param message 详细消息
     */
    public ReservationConflictException(String message) {
        super(message);
    }

    /**
     * 带有指定详细消息和原因的构造函数
     *
     * @param message 详细消息
     * @param cause   原因 (通常是另一个异常)
     */
    public ReservationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}