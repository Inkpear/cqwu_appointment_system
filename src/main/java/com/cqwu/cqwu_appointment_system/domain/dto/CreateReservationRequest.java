package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @description 用于创建新预约申请的请求体 DTO。
 */
@Data
public class CreateReservationRequest {

    @NotNull(message = "场所ID不能为空")
    private Long venueId;

    @NotNull(message = "开始时间不能为空")
    @Future(message = "预约开始时间必须是未来的时间")
    private LocalDateTime startDatetime;

    @NotNull(message = "结束时间不能为空")
    @Future(message = "预约结束时间必须是未来的时间")
    private LocalDateTime endDatetime;

    @NotBlank(message = "预约标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "预约理由不能为空")
    @Size(max = 65535, message = "预约事由内容过长")
    private String purpose;

}