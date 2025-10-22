package com.cqwu.cqwu_appointment_system.domain.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

@Data
public class ReservationQuery {
    @PositiveOrZero(message = "页码不可以传入负数")
    private Integer page;
    @Positive(message = "分页大小至少为1")
    private Integer size;
    
    private String creatorUserId;
    private String status;
    private Long venueId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
}