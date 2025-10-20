package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @description 用于批量删除操作的请求体 DTO。
 */
@Data
public class BatchDeleteRequest {

    @NotEmpty(message = "要删除的ID列表不能为空")
    private Long[] ids;
}