package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description 用于封装更新预约状态所需的所有信息，在 Service 层内部使用，聚合了操作目标和操作主体。
 */
@Data
@AllArgsConstructor
public class UpdateReservationStatusRequest {

    @NotNull(message = "预约ID不能为空")
    private final Long reservationId;

    private final User currentUser;

    private String approvalComment;

    /**
     * 创建一个用于“取消”操作的请求对象。
     */
    public static UpdateReservationStatusRequest forCancellation(Long id, User user) {
        return new UpdateReservationStatusRequest(id, user, null);
    }

    /**
     * 创建一个用于“批准”或“拒绝”操作的请求对象。
     */
    public static UpdateReservationStatusRequest forApproval(Long id, User user, String approvalComment) {
        return new UpdateReservationStatusRequest(id, user, approvalComment);
    }
}