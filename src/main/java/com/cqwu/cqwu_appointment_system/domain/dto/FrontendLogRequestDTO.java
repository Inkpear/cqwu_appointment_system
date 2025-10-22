package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description 用于接收前端上报日志请求的数据传输对象 (DTO)。
 */
@Data
public class FrontendLogRequestDTO {

    @NotBlank(message = "日志级别不能为空")
    private String logLevel;

    @NotBlank(message = "日志内容不能为空")
    private String logMessage;

    @NotNull(message = "日志时间戳不能为空")
    private LocalDateTime logTimestamp;
}