package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.validation.ValidStatus;

import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @ValidStatus(constantsClass = Constants.UserRoleType.class, message = "无效的权限类型。有效值为: ROOT, ADMIN, USER")
    private String role;
}
