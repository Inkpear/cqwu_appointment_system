package com.cqwu.cqwu_appointment_system.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalTime;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.validation.ValidStatus;

/**
 * @description 场所信息实体类，包含了数据校验约束
 */
@Data
public class Venue {

    /**
     * 主键ID
     * SQL: BIGINT, NOT NULL, AUTO_INCREMENT
     */
    private Long id;

    /**
     * 场所名称
     * SQL: VARCHAR(100), NOT NULL
     */
    @NotBlank(message = "场所名称不能为空")
    @Size(max = 100, message = "场所名称长度不能超过100个字符")
    private String name;

    /**
     * 位置
     * SQL: VARCHAR(200), NULL
     */
    @Size(max = 200, message = "位置信息长度不能超过200个字符")
    private String location;

    /**
     * 楼层
     * SQL: INT, NULL
     */
    private Integer floor;

    /**
     * 容量
     * SQL: INT, NULL, DEFAULT 0
     */
    @PositiveOrZero(message = "容量必须是大于或等于0的整数")
    private Integer capacity;

    /**
     * 分类ID
     * SQL: BIGINT, NULL
     */
    private Long categoryId;

    /**
     * 设备配置
     * SQL: TEXT, NULL
     */
    @Size(max = 65535, message = "设备配置信息过长")
    private String equipment;

    /**
     * 基础状态 (由管理员设置)
     * SQL: VARCHAR(20), NOT NULL
     */
    @NotBlank(message = "场所基础状态不能为空")
    @Size(max = 20, message = "基础状态长度不能超过20个字符")
    @ValidStatus(constantsClass = Constants.VenueBaseStatus.class, message = "无效的基础状态值。有效值为: AVAILABLE, MAINTENANCE, DISABLED")
    private String baseStatus;

    /**
     * 实时状态 (由定时任务计算)
     * 注意：此字段不应由客户端传入，因此不添加任何校验注解
     */
    private String realtimeStatus;

    /**
     * 每日开放时间
     * SQL: TIME, NULL
     */
    private LocalTime openTime;

    /**
     * 每日关闭时间
     * SQL: TIME, NULL
     */
    private LocalTime closeTime;

    /**
     * 预约是否需要人工审批
     * SQL: BOOLEAN, NOT NULL
     */
    @NotNull(message = "必须明确是否需要审批")
    private Boolean requiresApproval;

    /**
     * 单次预约最长时长（分钟）
     * SQL: INT, NULL
     */
    @PositiveOrZero(message = "最长预约时长必须为非负整数")
    private Integer maxBookingDurationMinutes;
}