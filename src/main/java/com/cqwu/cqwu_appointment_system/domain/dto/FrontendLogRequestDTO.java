package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.validation.ValidStatus;

/**
 * @description 用于接收前端上报日志请求的数据传输对象 (DTO)。
 */
@Data
public class FrontendLogRequestDTO {

    @NotBlank(message = "日志级别不能为空")
    @ValidStatus(constantsClass = Constants.LogLevel.class, message = "无效的日志级别值。有效值为: DEBUG, INFO, WARN, ERROR")
    private String logLevel;

    @NotBlank(message = "日志内容不能为空")
    @Size(max = 65535, message = "日志内容过长")
    private String logMessage;

    @NotNull(message = "日志时间戳不能为空")
    private LocalDateTime logTimestamp;
}