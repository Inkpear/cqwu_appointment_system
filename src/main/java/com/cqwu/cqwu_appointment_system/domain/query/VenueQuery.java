package com.cqwu.cqwu_appointment_system.domain.query;

import lombok.Data;

/**
 * 场所查询参数类
 */
@Data
public class VenueQuery {
    private String name;
    private String location;
    private Integer floor;
    private Integer page;
    private Integer size;
    private Boolean requiresApproval;
    private String baseStatus;   
    private String realtimeStatus; 
    private Long categoryId;
}