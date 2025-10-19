package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationStatusRequest;
import com.cqwu.cqwu_appointment_system.entity.*;
import com.cqwu.cqwu_appointment_system.exception.PermissionDeniedException;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import com.cqwu.cqwu_appointment_system.service.UserService;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReservationServiceImplTest {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private TimeBlockService timeBlockService;
    @Autowired
    private VenueService venueService;
    @Autowired
    private UserService userService;

    private Venue testVenue;
    private User testUser;
    private User adminUser;
    private User rootUser; // 新增 ROOT 用户
    private final LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("testuser01");
        testUser.setName("普通用户");
        testUser.setRole(Constants.UserRoleType.ROLE_USER);
        testUser.setUserType((byte)1);
        userService.insertUser(testUser);

        adminUser = new User();
        adminUser.setId("adminuser01");
        adminUser.setName("管理员");
        adminUser.setRole(Constants.UserRoleType.ROLE_ADMIN);
        adminUser.setUserType((byte)2);
        userService.insertUser(adminUser);
        
        rootUser = new User();
        rootUser.setId("rootuser01");
        rootUser.setName("超级管理员");
        rootUser.setRole(Constants.UserRoleType.ROLE_ROOT);
        rootUser.setUserType((byte)0);
        userService.insertUser(rootUser);

        testVenue = new Venue();
        testVenue.setName("多功能测试厅");
        testVenue.setBaseStatus(Constants.VenueBaseStatus.AVAILABLE);
        testVenue.setRequiresApproval(true);
        testVenue.setMaxBookingDurationMinutes(120);
        venueService.insertVenues(List.of(testVenue));
    }

    @Test
    @DisplayName("创建预约申请 - 状态应为 PENDING")
    void testCreateReservation() {
        ReservationWithBLOBs reservation = new ReservationWithBLOBs();
        reservation.setVenueId(testVenue.getId());
        reservation.setCreatorUserId(testUser.getId());
        reservation.setTitle("我的申请");
        reservation.setStartDatetime(tomorrow.withHour(10));
        reservation.setEndDatetime(tomorrow.withHour(11));

        ReservationWithBLOBs result = reservationService.createReservation(reservation);

        assertNotNull(result);
        assertEquals(Constants.ReservationStatus.PENDING, result.getStatus());
        assertNull(result.getTimeBlockId());
    }

    @Test
    @DisplayName("批准预约 - 成功创建 TimeBlock 并更新状态")
    void testApproveReservation_Success() {
        ReservationWithBLOBs pending = createPendingReservation(tomorrow.withHour(10), tomorrow.withHour(11));
        UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forApproval(pending.getId(), adminUser);

        ReservationWithBLOBs approved = reservationService.approveReservation(request);

        assertNotNull(approved);
        assertEquals(Constants.ReservationStatus.CONFIRMED, approved.getStatus());
        assertEquals(adminUser.getId(), approved.getApproverUserId());
        assertNotNull(approved.getTimeBlockId());
        assertNotNull(approved.getAppointmentCode());
        assertNotNull(timeBlockService.findTimeBlockById(approved.getTimeBlockId()));
    }

    @Test
    @DisplayName("批准预约 - 因时间冲突而失败")
    void testApproveReservation_FailDueToConflict() {
        timeBlockService.createTimeBlock(createTimeBlock(tomorrow.withHour(14), tomorrow.withHour(16)));
        ReservationWithBLOBs pending = createPendingReservation(tomorrow.withHour(15), tomorrow.withHour(17));
        UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forApproval(pending.getId(), adminUser);

        assertThrows(ReservationConflictException.class, () -> reservationService.approveReservation(request));
    }

    @Test
    @DisplayName("取消预约 - PENDING 和 CONFIRMED 两种状态")
    void testCancelReservation() {
        // 取消 PENDING
        ReservationWithBLOBs pending = createPendingReservation(tomorrow.withHour(10), tomorrow.withHour(11));
        UpdateReservationStatusRequest cancelPending = UpdateReservationStatusRequest.forCancellation(pending.getId(), testUser);
        ReservationWithBLOBs cancelledPending = reservationService.cancelReservation(cancelPending);
        assertEquals(Constants.ReservationStatus.CANCELLED, cancelledPending.getStatus());
        assertNull(cancelledPending.getTimeBlockId());

        // 取消 CONFIRMED
        ReservationWithBLOBs confirmed = createAndApproveReservation(tomorrow.withHour(14), tomorrow.withHour(15));
        Long timeBlockId = confirmed.getTimeBlockId();
        UpdateReservationStatusRequest cancelConfirmed = UpdateReservationStatusRequest.forCancellation(confirmed.getId(), testUser);
        reservationService.cancelReservation(cancelConfirmed);
        
        assertNull(timeBlockService.findTimeBlockById(timeBlockId), "取消 CONFIRMED 预约后，关联的 TimeBlock 必须被删除");
    }

    @Test
    @DisplayName("取消预约 - 权限检查 (Owner, Admin, Root)")
    void testCancelReservation_Permissions() {
        ReservationWithBLOBs reservation = createAndApproveReservation(tomorrow.withHour(18), tomorrow.withHour(19));

        User anotherUser = new User();
        anotherUser.setId("anotheruser02");
        anotherUser.setName("其他用户");
        anotherUser.setRole(Constants.UserRoleType.ROLE_USER);
        anotherUser.setUserType((byte)1);
        userService.insertUser(anotherUser);
        
        // 另一个普通用户无法取消
        assertThrows(PermissionDeniedException.class, () -> reservationService.cancelReservation(UpdateReservationStatusRequest.forCancellation(reservation.getId(), anotherUser)));
        
        // 管理员可以取消
        ReservationWithBLOBs r2 = createAndApproveReservation(tomorrow.withHour(20), tomorrow.withHour(21));
        assertDoesNotThrow(() -> reservationService.cancelReservation(UpdateReservationStatusRequest.forCancellation(r2.getId(), adminUser)));

        // 超级管理员可以取消
        ReservationWithBLOBs r3 = createAndApproveReservation(tomorrow.withHour(22), tomorrow.withHour(23));
        assertDoesNotThrow(() -> reservationService.cancelReservation(UpdateReservationStatusRequest.forCancellation(r3.getId(), rootUser)));
    }

    @Test
    @DisplayName("拒绝预约 - 应更新状态且不删除 TimeBlock")
    void testRejectReservation() {
        ReservationWithBLOBs pending = createPendingReservation(tomorrow.withHour(10), tomorrow.withHour(11));
        assertNull(pending.getTimeBlockId());

        UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forRejection(pending.getId(), adminUser, "原因");
        ReservationWithBLOBs rejected = reservationService.rejectReservation(request);

        assertEquals(Constants.ReservationStatus.REJECTED, rejected.getStatus());
        assertEquals("原因", rejected.getRejectionReason());
        assertNull(rejected.getTimeBlockId(), "被拒绝的预约不应有关联的 TimeBlock");
    }

    // --- 辅助方法 ---

    private ReservationWithBLOBs createPendingReservation(LocalDateTime start, LocalDateTime end) {
        ReservationWithBLOBs reservation = new ReservationWithBLOBs();
        reservation.setVenueId(testVenue.getId());
        reservation.setCreatorUserId(testUser.getId());
        reservation.setTitle("Test Title");
        reservation.setStartDatetime(start);
        reservation.setEndDatetime(end);
        return reservationService.createReservation(reservation);
    }

    private TimeBlock createTimeBlock(LocalDateTime start, LocalDateTime end) {
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setVenueId(testVenue.getId());
        timeBlock.setTitle("Existing Block");
        timeBlock.setStartDatetime(start);
        timeBlock.setEndDatetime(end);
        timeBlock.setBlockType(Constants.TimeBlockType.COURSE);
        return timeBlock;
    }
    
    private ReservationWithBLOBs createAndApproveReservation(LocalDateTime start, LocalDateTime end) {
        ReservationWithBLOBs pending = createPendingReservation(start, end);
        UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forApproval(pending.getId(), adminUser);
        return reservationService.approveReservation(request);
    }
}