package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "用户类型不能为空")
    private Byte userType;

    private String college;

    private String major;
}