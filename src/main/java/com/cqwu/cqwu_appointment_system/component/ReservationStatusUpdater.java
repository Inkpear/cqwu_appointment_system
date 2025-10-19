package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.uuid.UUID;
import com.cqwu.cqwu_appointment_system.entity.ReservationExample;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    @Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
    @Transactional
    public void manageReservationLifecycle() {
        log.info("【定时任务】开始管理预约生命周期...");

        processAutoApprovals();
        processInProgress();
        processCompleted();
        processExpired();

        log.info("【定时任务】预约生命周期管理完成。");
    }

    /**
     * 【核心重构】处理所有 PENDING 的申请，包含自动审批的竞争逻辑
     */
    private void processAutoApprovals() {
        // 1. 查找所有尚未开始的 PENDING 状态的预约
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andStatusEqualTo(Constants.ReservationStatus.PENDING)
                .andStartDatetimeGreaterThan(LocalDateTime.now());
        List<ReservationWithBLOBs> pendingList = reservationMapper.selectByExampleWithBLOBs(example);

        if (pendingList.isEmpty()) {
            return;
        }

        // 2. 【分组】按 venueId 将所有申请分组
        Map<Long, List<ReservationWithBLOBs>> pendingByVenue = pendingList.stream()
                .collect(Collectors.groupingBy(ReservationWithBLOBs::getVenueId));

        // 3. 提前获取所有相关场所的信息，避免在循环中 N+1 查询
        List<Long> venueIds = new ArrayList<>(pendingByVenue.keySet());
        VenueExample venueExample = new VenueExample();
        venueExample.createCriteria().andIdIn(venueIds);
        Map<Long, Venue> venueMap = venueMapper.selectByExample(venueExample).stream()
                .collect(Collectors.toMap(Venue::getId, Function.identity()));

        // 4. 【排序与顺序处理】遍历每一个场所的申请列表
        for (Map.Entry<Long, List<ReservationWithBLOBs>> entry : pendingByVenue.entrySet()) {
            Long venueId = entry.getKey();
            List<ReservationWithBLOBs> reservationsForVenue = entry.getValue();
            Venue venue = venueMap.get(venueId);

            // 如果场所不存在或不需要自动审批，则跳过
            if (venue == null || venue.getRequiresApproval()) {
                continue;
            }

            // 【关键】按创建时间升序排序，确保公平性
            reservationsForVenue.sort(Comparator.comparing(ReservationWithBLOBs::getCreateTime));

            // 5. 顺序处理排序后的申请
            for (ReservationWithBLOBs reservation : reservationsForVenue) {
                try {
                    // 在这一刻，进行最终的时间冲突检查
                    if (timeBlockService.isTimeSlotAvailable(reservation.getVenueId(), reservation.getStartDatetime(),
                            reservation.getEndDatetime())) {
                        // 【批准】
                        approveBySystem(reservation);
                    } else {
                        // 【拒绝】
                        rejectBySystem(reservation, "时间段已被占用，系统自动拒绝");
                    }
                } catch (Exception e) {
                    log.error("【定时任务】自动处理预约 ID: {} 时发生未知异常", reservation.getId(), e);
                }
            }
        }
    }

    // --- 定时任务的私有辅助方法 ---

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

    private void processInProgress() {
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andStatusEqualTo(Constants.ReservationStatus.CONFIRMED)
                .andStartDatetimeLessThanOrEqualTo(LocalDateTime.now());

        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setStatus(Constants.ReservationStatus.IN_PROGRESS);
        int count = reservationMapper.updateByExampleSelective(update, example);
        if (count > 0)
            log.info("【定时任务】{} 个预约已变更为 IN_PROGRESS", count);
    }

    private void processCompleted() {
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andStatusEqualTo(Constants.ReservationStatus.IN_PROGRESS)
                .andEndDatetimeLessThan(LocalDateTime.now());

        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setStatus(Constants.ReservationStatus.COMPLETED);
        int count = reservationMapper.updateByExampleSelective(update, example);
        if (count > 0)
            log.info("【定时任务】{} 个预约已变更为 COMPLETED", count);
    }

    private void processExpired() {
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andStatusEqualTo(Constants.ReservationStatus.PENDING)
                .andStartDatetimeLessThan(LocalDateTime.now());

        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setStatus(Constants.ReservationStatus.EXPIRED);
        int count = reservationMapper.updateByExampleSelective(update, example);
        if (count > 0)
            log.info("【定时任务】{} 个待审批申请已变更为 EXPIRED", count);
    }
}