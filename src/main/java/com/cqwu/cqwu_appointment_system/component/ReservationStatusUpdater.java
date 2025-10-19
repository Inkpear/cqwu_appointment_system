package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.entity.*;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.TimeBlockMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
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

@Component
@Slf4j
public class ReservationStatusUpdater {

    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private VenueMapper venueMapper;
    @Autowired
    private TimeBlockMapper timeBlockMapper;
    @Autowired
    private TimeBlockService timeBlockService;

    /**
     * 每分钟执行一次，管理预约的整个生命周期。
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void manageReservationLifecycle() {
        log.info("【定时任务】开始管理预约生命周期...");
        
        processAutoApprovals();
        processActiveLifecycle();
        processExpired();
        
        log.info("【定时任务】预约生命周期管理完成。");
    }

    /**
     * 处理所有 PENDING 的申请，包含自动审批的竞争逻辑。
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
     * 【新增】独立的事务方法，用于处理单个自动审批，确保竞争逻辑的实时性。
     */
    @Transactional
    public void processSingleAutoApproval(ReservationWithBLOBs reservation) {
        if (timeBlockService.isTimeSlotAvailable(reservation.getVenueId(), reservation.getStartDatetime(), reservation.getEndDatetime())) {
            approveBySystem(reservation);
        } else {
            rejectBySystem(reservation, "时间段已被占用，系统自动拒绝");
        }
    }

    @Transactional
    void approveBySystem(ReservationWithBLOBs reservation) {
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setVenueId(reservation.getVenueId());
        timeBlock.setStartDatetime(reservation.getStartDatetime());
        timeBlock.setEndDatetime(reservation.getEndDatetime());
        timeBlock.setTitle(reservation.getTitle());
        timeBlock.setBlockType(Constants.TimeBlockType.APPOINTMENT);
        timeBlock.setReservationId(reservation.getId());
        timeBlockMapper.insertSelective(timeBlock);

        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setId(reservation.getId());
        update.setTimeBlockId(timeBlock.getId());
        update.setStatus(Constants.ReservationStatus.CONFIRMED);
        update.setApproverUserId("SYSTEM");
        update.setApprovalTime(LocalDateTime.now());
        update.setAppointmentCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        reservationMapper.updateByPrimaryKeySelective(update);

        log.info("【定时任务】已自动批准预约 ID: {}", reservation.getId());
    }

    @Transactional
    void rejectBySystem(ReservationWithBLOBs reservation, String reason) {
        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setId(reservation.getId());
        update.setStatus(Constants.ReservationStatus.REJECTED);
        update.setApproverUserId("SYSTEM");
        update.setApprovalTime(LocalDateTime.now());
        update.setRejectionReason(reason);
        reservationMapper.updateByPrimaryKeySelective(update);
        log.warn("【定时任务】因 '{}'，已自动拒绝预约 ID: {}", reason, reservation.getId());
    }

    /**
     * 处理所有活跃的生命周期状态转换 (CONFIRMED -> IN_PROGRESS -> COMPLETED)
     */
    private void processActiveLifecycle() {
        LocalDateTime now = LocalDateTime.now();
        
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
        }

        ReservationExample toCompletedExample = new ReservationExample();
        toCompletedExample.createCriteria()
            .andStatusIn(Arrays.asList(Constants.ReservationStatus.CONFIRMED, Constants.ReservationStatus.IN_PROGRESS))
            .andEndDatetimeLessThan(now);
        
        ReservationWithBLOBs completedUpdate = new ReservationWithBLOBs();
        completedUpdate.setStatus(Constants.ReservationStatus.COMPLETED);
        int completedCount = reservationMapper.updateByExampleSelective(completedUpdate, toCompletedExample);
        if (completedCount > 0) {
            log.info("【定时任务】{} 个预约已变更为 COMPLETED", completedCount);
        }
    }

    /**
     * 处理已过期的 PENDING 申请
     */
    private void processExpired() {
        LocalDateTime now = LocalDateTime.now();
        
        ReservationExample example = new ReservationExample();
        example.createCriteria()
            .andStatusEqualTo(Constants.ReservationStatus.PENDING)
            .andStartDatetimeLessThan(now);
            
        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setStatus(Constants.ReservationStatus.EXPIRED);
        int count = reservationMapper.updateByExampleSelective(update, example);
        if (count > 0) {
            log.info("【定时任务】{} 个待审批申请已变更为 EXPIRED", count);
        }
    }
}