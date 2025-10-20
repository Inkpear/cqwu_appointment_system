package com.cqwu.cqwu_appointment_system.entity;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VenueCategoryRule {
    private Long id;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotNull(message = "允许的用户类型不能为空")
    private Byte allowedUserType;
}