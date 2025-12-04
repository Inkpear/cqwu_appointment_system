package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.validation.ValidStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalTime;

/**
 * @description 用于管理员更新场所详细信息的请求体 DTO。
 *              所有字段都是可选的，允许部分更新。
 */
@Data
public class UpdateVenueRequest {

    @Size(max = 100, message = "场所名称长度不能超过100个字符")
    private String name;

    @Size(max = 200, message = "位置信息长度不能超过200个字符")
    private String location;

    private Integer floor;

    @PositiveOrZero(message = "容量必须是大于或等于0的整数")
    private Integer capacity;

    private Long categoryId;

    @Size(max = 65535, message = "设备配置信息过长")
    private String equipment;

    @Size(max = 20, message = "基础状态长度不能超过20个字符")
    @ValidStatus(constantsClass = Constants.VenueBaseStatus.class, message = "无效的基础状态值。有效值为: AVAILABLE, MAINTENANCE, DISABLED")
    private String baseStatus;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Boolean requiresApproval;
    
    private String description;
}
