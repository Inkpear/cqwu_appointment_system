package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectReservationRequest {
    @NotBlank(message = "拒绝的理由不能为空")
    private String reason;
}