package com.cqwu.cqwu_appointment_system.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @description 场所分类实体类，包含了数据校验约束
 */
@Data
public class VenueCategory {

    /**
     * 主键ID
     * SQL: BIGINT, NOT NULL, AUTO_INCREMENT
     */
    private Long id;

    /**
     * 分类名称 (e.g., 普通教室, 会议室, 实验室)
     * SQL: VARCHAR(50), NOT NULL
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String name;

    /**
     * 使用规则或策略描述
     * SQL: TEXT, NULL
     */
    @Size(max = 65535, message = "使用规则内容过长")
    private String usageRules;
}