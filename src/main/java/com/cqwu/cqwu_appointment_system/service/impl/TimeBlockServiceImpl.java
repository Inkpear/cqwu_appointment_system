package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.utils.ValidationUtils;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.TimeBlockExample;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.mapper.TimeBlockMapper;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class TimeBlockServiceImpl implements TimeBlockService {

    @Autowired
    private TimeBlockMapper timeBlockMapper;

    @Override
    @Transactional
    public TimeBlock createTimeBlock(TimeBlock timeBlock) {
        log.info("正在创建新的时间占用块: venueId={}, time={} - {}",
                timeBlock.getVenueId(), timeBlock.getStartDatetime(), timeBlock.getEndDatetime());

        Assert.notNull(timeBlock, "TimeBlock 对象不能为空");
        Assert.notNull(timeBlock.getVenueId(), "场所ID不能为空");
        Assert.notNull(timeBlock.getStartDatetime(), "开始时间不能为空");
        Assert.notNull(timeBlock.getEndDatetime(), "结束时间不能为空");

        ValidationUtils.requireStartBeforeEnd(
                timeBlock.getStartDatetime(),
                timeBlock.getEndDatetime(),
                "占用结束时间必须晚于开始时间");

        if (!isTimeSlotAvailable(timeBlock.getVenueId(), timeBlock.getStartDatetime(), timeBlock.getEndDatetime())) {
            throw new ReservationConflictException("该时间段已被占用，无法创建");
        }

        timeBlockMapper.insertSelective(timeBlock);
        log.info("时间占用块创建成功, ID: {}", timeBlock.getId());
        return timeBlock;
    }

    @Override
    @Transactional
    public void deleteTimeBlockById(Long id) {
        log.info("正在删除时间占用块, ID: {}", id);
        Assert.notNull(id, "要删除的 TimeBlock ID 不能为空");

        int affectedRows = timeBlockMapper.deleteByPrimaryKey(id);

        if (affectedRows > 0) {
            log.info("时间占用块 ID: {} 已成功删除", id);
        } else {
            log.warn("尝试删除一个不存在的时间占用块, ID: {}", id);
        }
    }

    @Override
    public List<TimeBlock> findTimeBlocksForVenue(Long venueId, LocalDateTime start, LocalDateTime end) {
        log.debug("查询场所 {} 在 {} 到 {} 之间的占用块", venueId, start, end);
        TimeBlockExample example = new TimeBlockExample();
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                .andEndDatetimeGreaterThan(start)
                .andStartDatetimeLessThan(end);
        return timeBlockMapper.selectByExample(example);
    }

    @Override
    public boolean isTimeSlotAvailable(Long venueId, LocalDateTime start, LocalDateTime end) {
        TimeBlockExample example = new TimeBlockExample();
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                .andEndDatetimeGreaterThan(start)
                .andStartDatetimeLessThan(end);

        long count = timeBlockMapper.countByExample(example);
        return count == 0;
    }

    @Override
    public TimeBlock findTimeBlockById(Long id) {
        log.debug("根据ID查找时间占用块, ID: {}", id);
        return timeBlockMapper.selectByPrimaryKey(id);
    }
}