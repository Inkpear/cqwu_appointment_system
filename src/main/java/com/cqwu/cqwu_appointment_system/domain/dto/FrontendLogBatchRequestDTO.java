package com.cqwu.cqwu_appointment_system.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FrontendLogBatchRequestDTO {
    @NotNull(message = "日志列表不能为空")
    @Size(min = 1, message = "至少包含一条日志")
    private List<FrontendLogRequestDTO> logs;
}
