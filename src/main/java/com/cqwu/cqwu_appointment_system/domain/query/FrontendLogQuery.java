package com.cqwu.cqwu_appointment_system.domain.query;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

/**
 * @description 用于查询前端上报日志的数据传输对象 (DTO)
 */
@Data
public class FrontendLogQuery {

    private Integer pageNum = 0;
    private Integer pageSize = Constants.DEFAULT_PAGE_SIZE;

    private String logLevel;
    private String userId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}