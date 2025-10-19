package com.cqwu.cqwu_appointment_system.entity;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.validation.ValidStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @description 场所时间占用表（调度核心）的实体类
 */
@Data
public class TimeBlock {

    /**
     * 主键ID
     * SQL: BIGINT, NOT NULL, AUTO_INCREMENT
     */
    private Long id;

    /**
     * 场所ID, 关联 venue 表
     * SQL: BIGINT, NOT NULL
     */
    @NotNull(message = "场所ID不能为空")
    private Long venueId;

    /**
     * 占用开始时间
     * SQL: DATETIME, NOT NULL
     */
    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是未来的时间")
    private LocalDateTime startDatetime;

    /**
     * 占用结束时间
     * SQL: DATETIME, NOT NULL
     */
    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须是未来的时间")
    private LocalDateTime endDatetime;

    /**
     * 占用事件的标题
     * SQL: VARCHAR(255), NOT NULL
     */
    @NotBlank(message = "事件标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 占用类型 (APPOINTMENT, COURSE, MAINTENANCE)
     * SQL: VARCHAR(50), NOT NULL
     */
    @NotBlank(message = "占用类型不能为空")
    @Size(max = 50, message = "占用类型长度不能超过50个字符")
    @ValidStatus(constantsClass = Constants.TimeBlockType.class, message = "无效的占用类型。有效值为: APPOINTMENT, COURSE, MAINTENANCE")
    private String blockType;

    /**
     * 如果类型是APPOINTMENT, 这里反向关联到 reservations 表
     * SQL: BIGINT, NULL
     * 注意：此字段由后端服务逻辑维护，不应由客户端传入，因此不加校验注解。
     */
    private Long reservationId;
}