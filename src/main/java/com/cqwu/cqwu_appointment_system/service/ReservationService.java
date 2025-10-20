package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationStatusRequest;
import com.cqwu.cqwu_appointment_system.domain.query.ReservationQuery;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;

import java.util.List;

import org.springframework.data.domain.Page;

public interface ReservationService {

    /**
     * 创建一个新的预约申请，状态始终为 PENDING。
     * Venue 将在方法内部根据 reservation.venueId 自行加载。
     */
    ReservationWithBLOBs createReservation(ReservationWithBLOBs reservation, User currentUser, Venue venue);

    /**
     * 取消一个预约。
     */
    ReservationWithBLOBs cancelReservation(UpdateReservationStatusRequest request);

    /**
     * [管理员] 批准一个待审批的预约。
     */
    ReservationWithBLOBs approveReservation(UpdateReservationStatusRequest request);

    /**
     * [管理员] 拒绝一个待审批的预约。
     */
    ReservationWithBLOBs rejectReservation(UpdateReservationStatusRequest request);

    /**
     * 根据预约ID获取完整的 Reservation 实体。
     */
    ReservationWithBLOBs findReservationById(Long reservationId);

    /**
     * 根据唯一的预约码获取完整的 Reservation 实体。
     */
    ReservationWithBLOBs findReservationByCode(String code);

    /**
     * 动态分页查询预约列表 (包含大字段)。
     */
    Page<ReservationWithBLOBs> searchReservations(ReservationQuery query);

    /**
     * 批量id查询预约
     */
    List<ReservationWithBLOBs> findReservationsByIds(List<Long> ids);
}