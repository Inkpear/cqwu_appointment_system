package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询场所文件请求 DTO
 */
@Data
public class QueryVenueFilesRequest {
    
    @NotNull(message = "场所ID不能为空")
    private Long venueId;
    
    @NotNull(message = "文件分类不能为空")
    private FileCategory fileCategory;
}
