package com.cqwu.cqwu_appointment_system.domain.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class ReservationQuery {
    private Integer page;
    private Integer size;
    private String creatorUserId;
    private String status;
    private Long venueId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
}