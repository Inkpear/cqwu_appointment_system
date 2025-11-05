package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.entity.*;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description 定时任务组件，负责驱动预约申请的整个生命周期状态自动流转。
 */
@Component
@Slf4j
public class ReservationStatusUpdater {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private TimeBlockService timeBlockService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 主调度方法，每分钟执行一次，按顺序管理预约的各个生命周期阶段。
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
    @Transactional
    public void manageReservationLifecycle() {
        log.info("【定时任务】开始管理预约生命周期...");
        processAutoApprovals();
        processActiveLifecycle();
        processExpired();
        log.info("【定时任务】预约生命周期管理完成。");
    }

    /**
     * 筛选出所有符合自动审批条件的 PENDING。
     */
    private void processAutoApprovals() {
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andStatusEqualTo(Constants.ReservationStatus.PENDING)
                .andStartDatetimeGreaterThan(LocalDateTime.now());
        List<ReservationWithBLOBs> pendingList = reservationMapper.selectByExampleWithBLOBs(example);

        if (pendingList.isEmpty()) {
            return;
        }

        Map<Long, List<ReservationWithBLOBs>> pendingByVenue = pendingList.stream()
                .collect(Collectors.groupingBy(ReservationWithBLOBs::getVenueId));

        List<Long> venueIds = new ArrayList<>(pendingByVenue.keySet());
        VenueExample venueExample = new VenueExample();
        venueExample.createCriteria().andIdIn(venueIds);
        Map<Long, Venue> venueMap = venueMapper.selectByExample(venueExample).stream()
                .collect(Collectors.toMap(Venue::getId, Function.identity()));

        for (Map.Entry<Long, List<ReservationWithBLOBs>> entry : pendingByVenue.entrySet()) {
            Long venueId = entry.getKey();
            List<ReservationWithBLOBs> reservationsForVenue = entry.getValue();
            Venue venue = venueMap.get(venueId);

            if (venue == null || venue.getRequiresApproval()) {
                continue;
            }

            reservationsForVenue.sort(Comparator.comparing(ReservationWithBLOBs::getCreateTime));

            for (ReservationWithBLOBs reservation : reservationsForVenue) {
                try {
                    processSingleAutoApproval(reservation);
                } catch (Exception e) {
                    log.error("【定时任务】自动处理预约 ID: {} 时发生未知异常", reservation.getId(), e);
                }
            }
        }
    }

    /**
     * 尝试对单个预约进行自动审批或拒绝。
     * 
     * @param reservation 待处理的预约实体
     */
    public void processSingleAutoApproval(ReservationWithBLOBs reservation) {
        Venue venue = venueMapper.selectByPrimaryKey(reservation.getVenueId());

        if (venue == null || !Constants.VenueBaseStatus.AVAILABLE.equalsIgnoreCase(venue.getBaseStatus())) {
            String reason = (venue == null) ? "关联的场所不存在" : "场所当前状态为'" + venue.getBaseStatus() + "'，不可预约";
            reservationService.rejectBySystem(reservation, reason);
            return;
        }

        if (timeBlockService.isTimeSlotAvailable(reservation.getVenueId(), reservation.getStartDatetime(),
                reservation.getEndDatetime())) {
            reservationService.approveBySystem(reservation);
        } else {
            reservationService.rejectBySystem(reservation, "时间段已被占用");
        }
    }

    /**
     * 批量处理活跃预约的状态推进。
     * (CONFIRMED -> IN_PROGRESS, IN_PROGRESS -> COMPLETED)
     */
    private void processActiveLifecycle() {
        LocalDateTime now = LocalDateTime.now();

        // 将已开始的 CONFIRMED 预约更新为 IN_PROGRESS
        ReservationExample toInProgressExample = new ReservationExample();
        toInProgressExample.createCriteria()
                .andStatusEqualTo(Constants.ReservationStatus.CONFIRMED)
                .andStartDatetimeLessThanOrEqualTo(now)
                .andEndDatetimeGreaterThan(now);

        ReservationWithBLOBs inProgressUpdate = new ReservationWithBLOBs();
        inProgressUpdate.setStatus(Constants.ReservationStatus.IN_PROGRESS);
        int inProgressCount = reservationMapper.updateByExampleSelective(inProgressUpdate, toInProgressExample);
        if (inProgressCount > 0) {
            log.info("【定时任务】{} 个预约已变更为 IN_PROGRESS", inProgressCount);
            notificationService.sendNotification(Constants.SseBroadcast.ALL_USERS,
                    Constants.SseEvent.RESERVATION_AUTO_UPDATED,
                    "有预约状态已自动更新为进行中");
        }

        ReservationExample toCompletedExample = new ReservationExample();
        toCompletedExample.createCriteria()
                .andStatusIn(
                        Arrays.asList(Constants.ReservationStatus.CONFIRMED, Constants.ReservationStatus.IN_PROGRESS))
                .andEndDatetimeLessThan(now);

        ReservationWithBLOBs completedUpdate = new ReservationWithBLOBs();
        completedUpdate.setStatus(Constants.ReservationStatus.COMPLETED);
        int completedCount = reservationMapper.updateByExampleSelective(completedUpdate, toCompletedExample);
        if (completedCount > 0) {
            log.info("【定时任务】{} 个预约已变更为 COMPLETED", completedCount);
            notificationService.sendNotification(Constants.SseBroadcast.ALL_USERS,
                    Constants.SseEvent.RESERVATION_AUTO_UPDATED,
                    "有预约状态已自动更新为已完成");
        }
    }

    /**
     * 批量处理已过期的 PENDING 申请，将其状态更新为 EXPIRED，并逐一通知申请人。
     */
    private void processExpired() {
        LocalDateTime now = LocalDateTime.now();

        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andStatusEqualTo(Constants.ReservationStatus.PENDING)
                .andStartDatetimeLessThan(now);

        List<ReservationWithBLOBs> expiredList = reservationMapper.selectByExampleWithBLOBs(example);
        if (expiredList.isEmpty()) {
            return;
        }

        int updatedCount = 0;
        for (ReservationWithBLOBs reservation : expiredList) {
            ReservationWithBLOBs update = new ReservationWithBLOBs();
            update.setId(reservation.getId());
            update.setStatus(Constants.ReservationStatus.EXPIRED);
            if (reservationMapper.updateByPrimaryKeySelective(update) > 0) {
                updatedCount++;
                Map<String, Object> payload = new HashMap<>();
                payload.put("reservationId", reservation.getId());
                payload.put("venueId", reservation.getVenueId());
                payload.put("status", Constants.ReservationStatus.EXPIRED);
                payload.put("message", "您的预约已自动过期");
                notificationService.sendNotification(reservation.getCreatorUserId(),
                        Constants.SseEvent.RESERVATION_EXPIRED, payload);
            }
        }

        if (updatedCount > 0) {
            log.info("【定时任务】{} 个待审批申请已变更为 EXPIRED", updatedCount);
        }
    }
}