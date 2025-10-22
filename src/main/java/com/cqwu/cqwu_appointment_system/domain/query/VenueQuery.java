package com.cqwu.cqwu_appointment_system.domain.query;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 场所查询参数类
 */
@Data
public class VenueQuery {
    @PositiveOrZero(message = "页码不可以传入负数")
    private Integer page;
    @Positive(message = "分页大小至少为1")
    private Integer size;

    private String name;
    private String location;
    private Integer floor;
    private Boolean requiresApproval;
    private String baseStatus;
    private String realtimeStatus;
    private Long categoryId;
}