package com.cqwu.cqwu_appointment_system.entity;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Venue {
    private Long id;

    /**
     * 场所名称
     * SQL约束: NOT NULL, VARCHAR(100)
     */
    @NotBlank(message = "场所名称不能为空")
    @Size(max = 100, message = "场所名称长度不能超过100个字符")
    private String name;

    /**
     * 位置
     * SQL约束: VARCHAR(200), NULL
     */
    @Size(max = 200, message = "位置信息长度不能超过200个字符")
    private String location;

    /**
     * 楼层
     * SQL约束: INT, NULL
     * 业务约束: 楼层通常是非负数，这是一个合理的业务层校验
     */
    private Integer floor;

    /**
     * 容量
     * SQL约束: INT, NULL, DEFAULT 0
     * 业务约束: 容量不能为负数
     */
    @PositiveOrZero(message = "容量必须是大于或等于0的整数")
    private Integer capacity;

    /**
     * 分类ID
     * SQL约束: BIGINT, NULL
     */
    private Long categoryId;

    /**
     * 状态
     * SQL约束: VARCHAR(20), NULL, DEFAULT 'available'
     */
    @Size(max = 20, message = "状态长度不能超过20个字符")
    private String status;

    /**
     * 开放开始时间
     * SQL约束: TIME, NULL
     */
    private LocalTime openTime;

    /**
     * 开放结束时间
     * SQL约束: TIME, NULL
     */
    private LocalTime closeTime;

    /**
     * 设备配置
     * SQL约束: TEXT, NULL
     */
    @Size(max = 65535, message = "设备配置信息过长")
    private String equipment;
}