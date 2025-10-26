package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.validation.ValidStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDTO {
    @NotBlank(message = "用户ID不能为空")
    @Size(max = 20, message = "用户ID长度不能超过20个字符")
    private String id;

    @NotBlank(message = "角色不能为空")
    @ValidStatus(constantsClass = Constants.UserRoleType.class, message = "无效的权限类型。有效值为: ROOT, ADMIN, USER")
    private String role;

    @NotBlank(message = "用户姓名不能为空")
    @Size(max = 50, message = "用户姓名长度不能超过50个字符")
    private String name;

    @NotNull(message = "用户类型不能为空")
    private Byte userType;

    private Byte sex;

    private String major;

    private String college;
}
