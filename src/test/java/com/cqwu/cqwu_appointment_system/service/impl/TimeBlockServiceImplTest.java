package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
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
class TimeBlockServiceImplTest {

    @Autowired
    private TimeBlockService timeBlockService;

    @Autowired
    private VenueService venueService;

    private Venue testVenue;
    private TimeBlock existingTimeBlock;

    // 为了方便测试，定义一些可复用的时间点
    private final LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0);

    @BeforeEach
    void setUp() {
        // 1. 创建一个用于测试的场所
        testVenue = new Venue();
        testVenue.setName("Test Venue for TimeBlock");
        testVenue.setBaseStatus("AVAILABLE");
        testVenue.setRequiresApproval(false);
        venueService.insertVenues(List.of(testVenue));
        assertNotNull(testVenue.getId());

        // 2. 预先创建一个已存在的占用块，用于测试冲突
        existingTimeBlock = new TimeBlock();
        existingTimeBlock.setVenueId(testVenue.getId());
        existingTimeBlock.setTitle("Existing Event");
        existingTimeBlock.setBlockType("MAINTENANCE");
        existingTimeBlock.setStartDatetime(tomorrow.withHour(10));
        existingTimeBlock.setEndDatetime(tomorrow.withHour(11));
        timeBlockService.createTimeBlock(existingTimeBlock);
        assertNotNull(existingTimeBlock.getId());
    }

    @Test
    @DisplayName("测试成功创建一个不冲突的时间占用块")
    void testCreateTimeBlock_WhenSlotIsFree_ShouldSucceed() {
        // Arrange
        TimeBlock newBlock = new TimeBlock();
        newBlock.setVenueId(testVenue.getId());
        newBlock.setTitle("New Event");
        newBlock.setBlockType("APPOINTMENT");
        newBlock.setStartDatetime(tomorrow.withHour(14)); // 明天 14:00
        newBlock.setEndDatetime(tomorrow.withHour(15)); // 明天 15:00

        // Act
        TimeBlock createdBlock = timeBlockService.createTimeBlock(newBlock);

        // Assert
        assertNotNull(createdBlock);
        assertNotNull(createdBlock.getId());
        assertEquals("New Event", createdBlock.getTitle());
    }

    @Test
    @DisplayName("测试创建一个时间冲突的时间占用块时应抛出异常")
    void testCreateTimeBlock_WhenSlotIsOccupied_ShouldThrowConflictException() {
        // Arrange: 创建一个与 existingTimeBlock 重叠的占用块
        TimeBlock conflictingBlock = new TimeBlock();
        conflictingBlock.setVenueId(testVenue.getId());
        conflictingBlock.setTitle("Conflicting Event");
        conflictingBlock.setBlockType("APPOINTMENT");
        conflictingBlock.setStartDatetime(tomorrow.withHour(10).withMinute(30)); // 10:30
        conflictingBlock.setEndDatetime(tomorrow.withHour(11).withMinute(30)); // 11:30

        // Act & Assert
        assertThrows(ReservationConflictException.class, () -> {
            timeBlockService.createTimeBlock(conflictingBlock);
        }, "创建时间冲突的占用块应该抛出 ReservationConflictException");
    }

    @Test
    @DisplayName("测试创建一个开始时间晚于结束时间的占用块时应抛出异常")
    void testCreateTimeBlock_WhenStartTimeIsAfterEndTime_ShouldThrowIllegalArgumentException() {
        // Arrange
        TimeBlock invalidBlock = new TimeBlock();
        invalidBlock.setVenueId(testVenue.getId());
        invalidBlock.setTitle("Invalid Event");
        invalidBlock.setBlockType("APPOINTMENT");
        invalidBlock.setStartDatetime(tomorrow.withHour(15)); // 15:00
        invalidBlock.setEndDatetime(tomorrow.withHour(14)); // 14:00 (错误!)

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            timeBlockService.createTimeBlock(invalidBlock);
        }, "开始时间晚于结束时间应该抛出 IllegalArgumentException");
    }

    @Test
    @DisplayName("测试根据ID删除一个时间占用块")
    void testDeleteTimeBlockById_ShouldRemoveTheBlock() {
        // Arrange
        Long idToDelete = existingTimeBlock.getId();
        assertNotNull(timeBlockService.findTimeBlockById(idToDelete), "删除前，记录应存在");

        // Act
        timeBlockService.deleteTimeBlockById(idToDelete);

        // Assert
        assertNull(timeBlockService.findTimeBlockById(idToDelete), "删除后，记录应不存在");
    }

    @Test
    @DisplayName("测试 isTimeSlotAvailable 方法的各种场景")
    void testIsTimeSlotAvailable() {
        // 场景1: 完全空闲的时间段
        assertTrue(
                timeBlockService.isTimeSlotAvailable(testVenue.getId(), tomorrow.withHour(14), tomorrow.withHour(15)),
                "一个完全空闲的时间段应该返回 true");

        // 场景2: 与已占用时间完全重合
        assertFalse(
                timeBlockService.isTimeSlotAvailable(testVenue.getId(), tomorrow.withHour(10), tomorrow.withHour(11)),
                "完全重合的时间段应该返回 false");

        // 场景3: 内部重叠
        assertFalse(
                timeBlockService.isTimeSlotAvailable(testVenue.getId(), tomorrow.withHour(10).withMinute(15),
                        tomorrow.withHour(10).withMinute(45)),
                "内部重叠的时间段应该返回 false");

        // 场景4: 跨越开始时间
        assertFalse(
                timeBlockService.isTimeSlotAvailable(testVenue.getId(), tomorrow.withHour(9).withMinute(30),
                        tomorrow.withHour(10).withMinute(30)),
                "跨越开始时间的时间段应该返回 false");

        // 场景5: 跨越结束时间
        assertFalse(
                timeBlockService.isTimeSlotAvailable(testVenue.getId(), tomorrow.withHour(10).withMinute(30),
                        tomorrow.withHour(11).withMinute(30)),
                "跨越结束时间的时间段应该返回 false");
    }

    @Test
    @DisplayName("测试 findTimeBlocksForVenue 方法")
    void testFindTimeBlocksForVenue_ShouldReturnCorrectBlocks() {
        // Arrange: 再额外创建一个占用块
        TimeBlock anotherBlock = new TimeBlock();
        anotherBlock.setVenueId(testVenue.getId());
        anotherBlock.setTitle("Another Event");
        anotherBlock.setBlockType("APPOINTMENT");
        anotherBlock.setStartDatetime(tomorrow.withHour(17)); // 17:00
        anotherBlock.setEndDatetime(tomorrow.withHour(18)); // 18:00
        timeBlockService.createTimeBlock(anotherBlock);

        // Act: 查询明天一整天的占用情况
        List<TimeBlock> blocks = timeBlockService.findTimeBlocksForVenue(testVenue.getId(), tomorrow,
                tomorrow.plusDays(1));

        // Assert
        assertEquals(2, blocks.size(), "应该查询到2个占用块");

        List<String> titles = blocks.stream().map(TimeBlock::getTitle).toList();
        assertTrue(titles.contains("Existing Event"), "结果应包含 'Existing Event'");
        assertTrue(titles.contains("Another Event"), "结果应包含 'Another Event'");
    }
}