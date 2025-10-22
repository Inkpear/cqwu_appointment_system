package com.cqwu.cqwu_appointment_system.domain.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

/**
 * @description 用于查询前端上报日志的数据传输对象 (DTO)
 */
@Data
public class FrontendLogQuery {

    @PositiveOrZero(message = "页码不可以传入负数")
    private Integer page;
    @Positive(message = "分页大小至少为1")
    private Integer size;

    private String logLevel;
    private String userId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}