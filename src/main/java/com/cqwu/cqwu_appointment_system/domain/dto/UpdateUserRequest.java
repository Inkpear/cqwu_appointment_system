package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String name;

    private Byte userType;
    
    @Size(max = 100, message = "所属学院长度不能超过100个字符")
    private String college;

    @Size(max = 100, message = "所属专业长度不能超过100个字符")
    private String major;
}