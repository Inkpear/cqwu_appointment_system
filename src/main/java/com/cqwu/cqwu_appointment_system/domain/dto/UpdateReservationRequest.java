package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateReservationRequest {
    @NotBlank(message = "审批意见不能为空")
    private String approvalComment;
}