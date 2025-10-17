package com.cqwu.cqwu_appointment_system.domain.query;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class UserQuery {
    private Integer page;
    private Integer size;

    private String id; // 学号或工号
    private String name;
    private String role;
    private Byte userType;
}