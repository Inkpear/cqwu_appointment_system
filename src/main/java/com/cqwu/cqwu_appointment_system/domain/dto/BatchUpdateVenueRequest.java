package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description 用于批量更新场所信息的请求体 DTO。
 *              它包含一个ID列表和一份要应用于这些ID的更新数据。
 */
@Data
public class BatchUpdateVenueRequest {

    @NotEmpty(message = "要更新的ID列表不能为空")
    private Long[] ids;

    @NotNull(message = "更新数据不能为空")
    @Valid // 关键：启用对 UpdateVenueRequest 内部字段的级联校验
    private UpdateVenueRequest venueData;
}