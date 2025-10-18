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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class VenueStatusUpdater {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private TimeBlockMapper timeBlockMapper;

    /**
     * 每分钟执行一次，更新所有场所的实时状态。
     *
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateVenueRealtimeStatus() {
        log.info("【定时任务】开始更新场所实时状态...");

        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();

        TimeBlockExample timeBlockExample = new TimeBlockExample();
        timeBlockExample.createCriteria()
                .andStartDatetimeLessThanOrEqualTo(now)
                .andEndDatetimeGreaterThan(now);
        Set<Long> occupiedVenueIds = timeBlockMapper.selectByExample(timeBlockExample)
                .stream()
                .map(TimeBlock::getVenueId)
                .collect(Collectors.toSet());

        List<Venue> allVenues = venueMapper.selectByExample(new VenueExample());
        if (allVenues.isEmpty()) {
            log.info("【定时任务】没有找到任何场所，任务结束。");
            return;
        }

        List<Venue> venuesToUpdate = new ArrayList<>();
        for (Venue venue : allVenues) {
            String newRealtimeStatus;

            if (!Constants.VenueBaseStatus.AVAILABLE.equalsIgnoreCase(venue.getBaseStatus())) {
                newRealtimeStatus = venue.getBaseStatus();
            } 
            else if (venue.getOpenTime() == null || venue.getCloseTime() == null ||
                     nowTime.isBefore(venue.getOpenTime()) || nowTime.isAfter(venue.getCloseTime())) {
                newRealtimeStatus = Constants.VenueRealtimeStatus.CLOSED;
            }
            else if (occupiedVenueIds.contains(venue.getId())) {
                newRealtimeStatus = Constants.VenueRealtimeStatus.OCCUPIED;
            }
            else {
                newRealtimeStatus = Constants.VenueRealtimeStatus.AVAILABLE;
            }

            if (!newRealtimeStatus.equals(venue.getRealtimeStatus())) {
                Venue updateEntity = new Venue();
                updateEntity.setId(venue.getId());
                updateEntity.setRealtimeStatus(newRealtimeStatus);
                venuesToUpdate.add(updateEntity);
            }
        }

        if (!venuesToUpdate.isEmpty()) {
            venueMapper.updateRealtimeStatusBatch(venuesToUpdate);
            log.info("【定时任务】任务完成：共批量更新了 {} 个场所的实时状态。", venuesToUpdate.size());
        } else {
            log.debug("【定时任务】任务完成：没有场所状态需要更新。");
        }
    }
}