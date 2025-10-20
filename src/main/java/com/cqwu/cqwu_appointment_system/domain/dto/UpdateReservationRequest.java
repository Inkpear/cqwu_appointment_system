package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @description 用于更新预约信息的请求体 DTO，目前主要用于管理员填写审批意见。
 */
@Data
public class UpdateReservationRequest {
    @NotBlank(message = "审批意见不能为空")
    private String approvalComment;
}