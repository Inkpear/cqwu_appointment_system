package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.TimeBlockExample;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.TimeBlockMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // 推荐为定时任务加上事务

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private TimeBlockMapper timeBlockMapper;

    /**
     * 主调度方法，每分钟执行一次，编排整个场所实时状态的更新流程。
     * 流程：获取当前占用信息 -> 遍历所有场所 -> 计算新状态 -> 批量更新有变化的场所。
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional // 为整个更新操作添加事务性，确保数据一致性
    public void updateVenueRealtimeStatus() {
        log.info("【定时任务】开始更新场所实时状态...");

        // 1. 获取基础数据：当前时间和所有被占用的场地ID
        LocalDateTime now = LocalDateTime.now();
        Set<Long> occupiedVenueIds = getCurrentlyOccupiedVenueIds(now);

        // 2. 获取所有需要管理的场所
        List<Venue> allVenues = venueMapper.selectByExample(new VenueExample());
        if (allVenues.isEmpty()) {
            log.info("【定时任务】没有找到任何场所，任务结束。");
            return;
        }

        // 3. 计算并收集状态需要更新的场所
        List<Venue> venuesToUpdate = new ArrayList<>();
        for (Venue venue : allVenues) {
            String newRealtimeStatus = calculateNewStatus(venue, occupiedVenueIds, now.toLocalTime());

            // 仅当状态发生变化时才加入更新列表
            if (!newRealtimeStatus.equals(venue.getRealtimeStatus())) {
                Venue updateEntity = new Venue();
                updateEntity.setId(venue.getId());
                updateEntity.setRealtimeStatus(newRealtimeStatus);
                venuesToUpdate.add(updateEntity);
            }
        }

        // 4. 如果有需要更新的场所，则执行批量更新
        if (!venuesToUpdate.isEmpty()) {
            venueMapper.updateRealtimeStatusBatch(venuesToUpdate);
            log.info("【定时任务】任务完成：共批量更新了 {} 个场所的实时状态。", venuesToUpdate.size());
        } else {
            // 使用 debug 级别，因为状态不变是常态，避免日志刷屏
            log.debug("【定时任务】任务完成：没有场所状态需要更新。");
        }
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
        // 优先级1：如果场所的基础状态不是"可用"（如"维修中"），则实时状态直接等于基础状态
        if (!Constants.VenueBaseStatus.AVAILABLE.equalsIgnoreCase(venue.getBaseStatus())) {
            return venue.getBaseStatus();
        }

        // 优先级2：检查是否在开放时间内，如果不在则为"闭馆"
        if (venue.getOpenTime() == null || venue.getCloseTime() == null ||
                nowTime.isBefore(venue.getOpenTime()) || nowTime.isAfter(venue.getCloseTime())) {
            return Constants.VenueRealtimeStatus.CLOSED;
        }

        // 优先级3：检查是否被占用
        if (occupiedVenueIds.contains(venue.getId())) {
            return Constants.VenueRealtimeStatus.OCCUPIED;
        }

        // 优先级4：以上都不是，则为"可用"
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