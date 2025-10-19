package com.cqwu.cqwu_appointment_system.entity;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.validation.ValidStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @description 预约业务表 (reservations) 的实体类，包含所有字段和数据校验约束。
 *              此实体用于 Service 层进行业务逻辑处理。
 */
@Data
public class Reservation {

    private Long id;

    /**
     * 场所ID，冗余字段用于快速查询
     * SQL: BIGINT, NOT NULL
     */
    @NotNull(message = "场所ID不能为空")
    private Long venueId;

    /**
     * 申请的开始时间
     * SQL: DATETIME, NOT NULL
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startDatetime;

    /**
     * 申请的结束时间
     * SQL: DATETIME, NOT NULL
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endDatetime;

    /**
     * 申请的标题
     * SQL: VARCHAR(255), NOT NULL
     */
    @NotBlank(message = "申请标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 关联到 time_blocks 表的主键ID (CONFIRMED 状态下有效)
     * SQL: BIGINT, NULL
     */
    private Long timeBlockId; // 允许为空

    /**
     * 创建者用户ID (学号/工号)
     * SQL: VARCHAR(20), NOT NULL
     */
    @NotBlank(message = "创建者用户ID不能为空")
    @Size(max = 20, message = "创建者ID长度不能超过20个字符")
    private String creatorUserId;

    /**
     * 状态 (PENDING, CONFIRMED, REJECTED, CANCELLED, EXPIRED)
     * SQL: VARCHAR(20), NOT NULL
     */
    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过20个字符")
    @ValidStatus(constantsClass = Constants.ReservationStatus.class, message = "无效的预约状态")
    private String status;

    /**
     * 预约确认码
     * SQL: VARCHAR(50), NULL
     */
    @Size(max = 50, message = "预约码长度不能超过50个字符")
    private String appointmentCode;

    /**
     * 审批人用户ID
     * SQL: VARCHAR(20), NULL
     */
    @Size(max = 20, message = "审批人ID长度不能超过20个字符")
    private String approverUserId;

    /**
     * 审批操作时间
     * SQL: DATETIME, NULL
     */
    private LocalDateTime approvalTime;

    /**
     * 申请创建时间
     * SQL: DATETIME, NOT NULL
     */
    private LocalDateTime createTime;
}