package com.cqwu.cqwu_appointment_system.entity;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationWithBLOBs extends Reservation {
    /**
     * 【大字段】预约的详细事由/目的
     * SQL: TEXT, NULL
     */
    @Size(max = 65535, message = "预约事由内容过长")
    private String purpose;

    /**
     * 【大字段】拒绝原因
     * SQL: TEXT, NULL
     */
    @Size(max = 65535, message = "审批意见内容过长")
    private String approvalComment;
}