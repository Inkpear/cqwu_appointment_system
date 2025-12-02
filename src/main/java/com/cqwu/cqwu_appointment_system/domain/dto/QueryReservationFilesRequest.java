package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询预约文件请求 DTO
 */
@Data
public class QueryReservationFilesRequest {
    
    @NotNull(message = "预约ID不能为空")
    private Long reservationId;
    
    @NotNull(message = "文件分类不能为空")
    private FileCategory fileCategory;
}
