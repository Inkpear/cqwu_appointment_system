package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.TimeBlockExample;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.TimeBlockMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @description 定时任务组件，负责周期性地计算并更新所有场所的实时状态 (realtime_status)。
 *              这个状态用于前端向用户展示场地是“可用”、“占用中”、“闭馆”还是“维护中”等。
 */
@Component
@Slf4j
public class VenueStatusUpdater {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private VenueService venueService;

    @Autowired
    private TimeBlockMapper timeBlockMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * 主调度方法，每分钟执行一次，编排整个场所实时状态的更新流程。
     * 流程：获取当前占用信息 -> 遍历所有场所 -> 计算新状态 -> 批量更新有变化的场所。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateVenueRealtimeStatus() {
        log.info("开始执行场所实时状态更新任务...");

        List<Venue> venuesToUpdate = new ArrayList<>();
        List<Venue> allVenues = venueMapper.selectByExample(new VenueExample());
        if (allVenues.isEmpty()) {
            log.info("没有找到任何场所，任务结束。");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> occupiedVenueIds = getCurrentlyOccupiedVenueIds(now);

        for (Venue venue : allVenues) {
            String newRealtimeStatus = calculateNewStatus(venue, occupiedVenueIds, now.toLocalTime());

            if (!newRealtimeStatus.equals(venue.getRealtimeStatus())) {
                Venue updateEntity = new Venue();
                updateEntity.setId(venue.getId());
                updateEntity.setRealtimeStatus(newRealtimeStatus);
                venuesToUpdate.add(updateEntity);
            }
        }

        if (!venuesToUpdate.isEmpty()) {
            int updatedCount = venueService.updateRealtimeStatusBatch(venuesToUpdate);
            log.info("成功更新了 {} 个场所的实时状态。", updatedCount);
            notificationService.sendNotification(
                    Constants.SseBroadcast.ALL_USERS,
                    Constants.SseEvent.VENUE_UPDATED,
                    "list_updated");
        } else {
            log.info("所有场所的实时状态均无需更新。");
        }

        log.info("场所实时状态更新任务执行完毕。");
    }

    /**
     * 计算单个场所的最新实时状态。
     * 计算优先级: 基础状态 > 开放时间 > 是否被占用 > 空闲。
     *
     * @param venue            需要计算的场所实体
     * @param occupiedVenueIds 当前所有被占用的场所ID集合
     * @param nowTime          当前时间 (LocalTime)
     * @return 计算出的最新实时状态字符串
     */
    private String calculateNewStatus(Venue venue, Set<Long> occupiedVenueIds, LocalTime nowTime) {
        if (!Constants.VenueBaseStatus.AVAILABLE.equalsIgnoreCase(venue.getBaseStatus())) {
            return venue.getBaseStatus();
        }

        if (venue.getOpenTime() == null || venue.getCloseTime() == null ||
                nowTime.isBefore(venue.getOpenTime()) || nowTime.isAfter(venue.getCloseTime())) {
            return Constants.VenueRealtimeStatus.CLOSED;
        }

        if (occupiedVenueIds.contains(venue.getId())) {
            return Constants.VenueRealtimeStatus.OCCUPIED;
        }

        return Constants.VenueRealtimeStatus.AVAILABLE;
    }

    /**
     * 从数据库中查询在指定时刻正被占用的所有场所的ID。
     *
     * @param timestamp 查询的时刻
     * @return 一个包含所有被占用场所ID的Set集合
     */
    private Set<Long> getCurrentlyOccupiedVenueIds(LocalDateTime timestamp) {
        TimeBlockExample timeBlockExample = new TimeBlockExample();
        timeBlockExample.createCriteria()
                .andStartDatetimeLessThanOrEqualTo(timestamp)
                .andEndDatetimeGreaterThan(timestamp);

        return timeBlockMapper.selectByExample(timeBlockExample)
                .stream()
                .map(TimeBlock::getVenueId)
                .collect(Collectors.toSet());
    }
}