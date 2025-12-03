package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.entity.ReservationDetail;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约详情DTO（包含扩展信息）
 */
@Data
public class ReservationDetailDto {
    private Long id;
    private Long venueId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String title;
    private String purpose;
    private Long timeBlockId;
    private String creatorUserId;
    private String status;
    private String appointmentCode;
    private String approverUserId;
    private LocalDateTime approvalTime;
    private String approvalComment;
    private LocalDateTime createTime;
    private String participants;
    private Integer participantCount;

    /**
     * 从Reservation和ReservationDetail构建DTO
     */
    public static ReservationDetailDto from(ReservationWithBLOBs reservation, ReservationDetail detail) {
        ReservationDetailDto dto = new ReservationDetailDto();
        dto.setId(reservation.getId());
        dto.setVenueId(reservation.getVenueId());
        dto.setStartDatetime(reservation.getStartDatetime());
        dto.setEndDatetime(reservation.getEndDatetime());
        dto.setTitle(reservation.getTitle());
        dto.setPurpose(reservation.getPurpose());
        dto.setTimeBlockId(reservation.getTimeBlockId());
        dto.setCreatorUserId(reservation.getCreatorUserId());
        dto.setStatus(reservation.getStatus());
        dto.setAppointmentCode(reservation.getAppointmentCode());
        dto.setApproverUserId(reservation.getApproverUserId());
        dto.setApprovalTime(reservation.getApprovalTime());
        dto.setApprovalComment(reservation.getApprovalComment());
        dto.setCreateTime(reservation.getCreateTime());

        // 设置扩展信息
        if (detail != null) {
            dto.setParticipants(detail.getParticipants());
            dto.setParticipantCount(detail.getParticipantCount());
        }

        return dto;
    }

    /**
     * 从Reservation构建DTO（无扩展信息）
     */
    public static ReservationDetailDto from(ReservationWithBLOBs reservation) {
        return from(reservation, null);
    }
}
