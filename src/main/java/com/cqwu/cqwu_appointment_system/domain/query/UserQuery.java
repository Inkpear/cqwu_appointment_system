package com.cqwu.cqwu_appointment_system.domain.query;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class UserQuery {
    @PositiveOrZero(message = "页码不可以传入负数")
    private Integer page;
    @Positive(message = "分页大小至少为1")
    private Integer size;

    private String id;
    private String name;
    private String role;
    private Byte userType;
}