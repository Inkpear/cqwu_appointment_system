package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新文件显示顺序请求
 */
@Data
public class UpdateFileOrderRequest {
    
    /**
     * 文件 ID
     */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    
    /**
     * 新的显示顺序（从 0 开始）
     */
    @NotNull(message = "显示顺序不能为空")
    @Min(value = 0, message = "显示顺序不能小于0")
    private Integer displayOrder;
}
