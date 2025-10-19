package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatchUpdateVenueRequest {

    @NotEmpty(message = "要更新的ID列表不能为空")
    private Long[] ids;
    
    @NotNull(message = "更新数据不能为空")
    @Valid
    private UpdateVenueRequest venueData;
}