package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.entity.User;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateReservationStatusRequest {
    
    @NotNull(message = "预约ID不能为空")
    private final Long reservationId;
    
    private final User currentUser;
    
    private String reason;
    

    public static UpdateReservationStatusRequest forCancellation(Long id, User user) {
        return new UpdateReservationStatusRequest(id, user, null);
    }
    
    public static UpdateReservationStatusRequest forApproval(Long id, User user) {
        return new UpdateReservationStatusRequest(id, user, null);
    }
    
    public static UpdateReservationStatusRequest forRejection(Long id, User user, String reason) {
        return new UpdateReservationStatusRequest(id, user, reason);
    }
}