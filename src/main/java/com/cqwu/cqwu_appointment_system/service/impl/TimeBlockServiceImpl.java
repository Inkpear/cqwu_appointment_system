package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.ValidationUtils;
import com.cqwu.cqwu_appointment_system.entity.ReservationExample;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.TimeBlockExample;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.TimeBlockMapper;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description TimeBlockService 的实现类。
 */
@Service
@Slf4j
public class TimeBlockServiceImpl implements TimeBlockService {

    @Autowired
    private TimeBlockMapper timeBlockMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public TimeBlock createTimeBlock(TimeBlock timeBlock) {
        log.debug("正在创建新的时间占用块: venueId={}, time={} - {}",
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
        log.debug("时间占用块创建成功, ID: {}", timeBlock.getId());
        return timeBlock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteTimeBlockById(Long id) {
        log.debug("正在删除时间占用块, ID: {}", id);
        Assert.notNull(id, "要删除的 TimeBlock ID 不能为空");

        int affectedRows = timeBlockMapper.deleteByPrimaryKey(id);

        if (affectedRows > 0) {
            log.debug("时间占用块 ID: {} 已成功删除", id);
        } else {
            log.warn("尝试删除一个不存在的时间占用块, ID: {}", id);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TimeBlock> findTimeBlocksForVenue(Long venueId, LocalDateTime start, LocalDateTime end) {
        log.debug("查询场所 {} 在 {} 到 {} 之间的占用块", venueId, start, end);
        TimeBlockExample example = new TimeBlockExample();
        // 查询条件：结束时间 > 查询开始时间 AND 开始时间 < 查询结束时间
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                .andEndDatetimeGreaterThan(start)
                .andStartDatetimeLessThan(end);
        return timeBlockMapper.selectByExample(example);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeBlock findTimeBlockById(Long id) {
        log.debug("根据ID查找时间占用块, ID: {}", id);
        return timeBlockMapper.selectByPrimaryKey(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int forceInsertCourseTimeBlocks(List<TimeBlock> courseTimeBlocks) {
        if (courseTimeBlocks == null || courseTimeBlocks.isEmpty()) {
            return 0;
        }
        log.warn("开始强制增量更新 {} 个课程 TimeBlock...", courseTimeBlocks.size());

        validateNoSelfConflicts(courseTimeBlocks);

        int appointmentConflictCount = 0;
        int courseConflictCount = 0;

        for (TimeBlock newCourseBlock : courseTimeBlocks) {
            // 查找与新课程时间冲突的、所有类型的 TimeBlock
            TimeBlockExample conflictExample = new TimeBlockExample();
            conflictExample.createCriteria()
                    .andVenueIdEqualTo(newCourseBlock.getVenueId())
                    .andEndDatetimeGreaterThan(newCourseBlock.getStartDatetime())
                    .andStartDatetimeLessThan(newCourseBlock.getEndDatetime());

            List<TimeBlock> conflictingBlocks = timeBlockMapper.selectByExample(conflictExample);

            if (!conflictingBlocks.isEmpty()) {
                // 分类处理冲突
                // 提取与 APPOINTMENT 冲突的预约ID
                List<Long> conflictingReservationIds = conflictingBlocks.stream()
                        .filter(block -> Constants.TimeBlockType.APPOINTMENT.equals(block.getBlockType()))
                        .map(TimeBlock::getReservationId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                courseConflictCount += conflictingBlocks.stream()
                        .filter(block -> Constants.TimeBlockType.COURSE.equals(block.getBlockType()))
                        .count();

                // 一次性删除所有冲突的 TimeBlock (无论类型)
                List<Long> conflictingBlockIds = conflictingBlocks.stream().map(TimeBlock::getId)
                        .collect(Collectors.toList());
                TimeBlockExample deleteExample = new TimeBlockExample();
                deleteExample.createCriteria().andIdIn(conflictingBlockIds);
                timeBlockMapper.deleteByExample(deleteExample);

                // 如果存在预约冲突，则更新关联的 Reservation 状态
                if (!conflictingReservationIds.isEmpty()) {
                    ReservationWithBLOBs updateEntity = new ReservationWithBLOBs();
                    updateEntity.setStatus(Constants.ReservationStatus.REJECTED);
                    updateEntity.setApproverUserId("SYSTEM");
                    updateEntity.setApprovalTime(LocalDateTime.now());
                    updateEntity.setApprovalComment("因课程安排调整，该时段已被占用，系统自动拒绝此预约。");

                    ReservationExample resUpdateExample = new ReservationExample();
                    resUpdateExample.createCriteria().andIdIn(conflictingReservationIds);
                    reservationMapper.updateByExampleSelective(updateEntity, resUpdateExample);
                    appointmentConflictCount += conflictingReservationIds.size();
                }
            }
        }

        if (appointmentConflictCount > 0) {
            log.warn("已自动拒绝并移除了 {} 个冲突的用户预约。", appointmentConflictCount);
        }
        if (courseConflictCount > 0) {
            log.warn("已覆盖了 {} 个冲突的旧课程记录。", courseConflictCount);
        }

        // 批量插入所有新课程
        int insertedCount = timeBlockMapper.insertBatch(courseTimeBlocks);
        log.debug("成功插入 {} 个新课程 TimeBlock。", insertedCount);

        return insertedCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteAllCourseTimeBlocksByVenueIds(List<Long> venueIds) {
        if (venueIds == null || venueIds.isEmpty()) {
            return 0;
        }
        log.warn("准备删除 {} 个场所的所有课程类时间占用...", venueIds.size());

        TimeBlockExample example = new TimeBlockExample();
        example.createCriteria()
                .andVenueIdIn(venueIds)
                .andBlockTypeEqualTo(Constants.TimeBlockType.COURSE);

        int deletedCount = timeBlockMapper.deleteByExample(example);
        log.debug("成功删除了 {} 条课程类 TimeBlock 记录。", deletedCount);
        return deletedCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteCourseTimeBlocksByRange(Long venueId, LocalDateTime start, LocalDateTime end) {
        log.warn("准备清空场所 ID: {} 在时间范围 [{} -> {}] 内的所有课程...", venueId, start, end);
        Assert.notNull(venueId, "场所ID不能为空");
        Assert.notNull(start, "开始时间不能为空");
        Assert.notNull(end, "结束时间不能为空");

        TimeBlockExample example = new TimeBlockExample();
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                .andBlockTypeEqualTo(Constants.TimeBlockType.COURSE)
                .andEndDatetimeGreaterThan(start) // 任何与范围重叠的
                .andStartDatetimeLessThan(end); // 都会被删除

        int deletedCount = timeBlockMapper.deleteByExample(example);
        log.debug("成功清空了 {} 条范围内的课程记录。", deletedCount);
        return deletedCount;
    }

    /**
     * 校验 TimeBlock 列表，确保内部不存在针对同一场地的重叠时间段。
     * 
     * @param blocks 要校验的 TimeBlock 列表
     * @throws ReservationConflictException 如果发现内部存在时间冲突
     */
    private void validateNoSelfConflicts(List<TimeBlock> blocks) {

        blocks.stream()
                .collect(Collectors.groupingBy(TimeBlock::getVenueId))
                .forEach((venueId, blocksForVenue) -> {
                    blocksForVenue.sort(Comparator.comparing(TimeBlock::getStartDatetime));

                    for (int i = 0; i < blocksForVenue.size() - 1; i++) {
                        TimeBlock current = blocksForVenue.get(i);
                        TimeBlock next = blocksForVenue.get(i + 1);

                        if (current.getEndDatetime().isAfter(next.getStartDatetime())) {
                            log.error("待插入的课程列表中存在内部时间冲突！Venue ID: {}, Conflict: [{} - {}] and [{} - {}]",
                                    venueId, current.getStartDatetime(), current.getEndDatetime(),
                                    next.getStartDatetime(), next.getEndDatetime());
                            throw new ReservationConflictException(
                                    String.format("提交的课程数据中存在内部时间冲突，场地ID: %d", venueId));
                        }
                    }
                });
    }

    /** {@inheritDoc} */
    @Override
    public int cleanTimeBlocksOlderThan(LocalDateTime cutoffDateTime) {
        log.debug("开始清理所有早于 {} 的过期 TimeBlock 记录...", cutoffDateTime);
        Assert.notNull(cutoffDateTime, "截止时间不能为空");

        TimeBlockExample example = new TimeBlockExample();
        example.createCriteria()
                .andEndDatetimeLessThan(cutoffDateTime);

        int deletedCount = timeBlockMapper.deleteByExample(example);
        log.debug("成功清理了 {} 条过期的 TimeBlock 记录。", deletedCount);
        return deletedCount;
    }
}