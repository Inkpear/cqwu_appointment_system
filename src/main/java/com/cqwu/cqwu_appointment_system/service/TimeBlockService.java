package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.TimeBlock;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 场所时间占用 (TimeBlock) 的核心调度服务接口。
 *              它的唯一职责是管理时间的占用与释放，是整个预约系统的物理时间基础。
 *              此服务不关心具体的业务逻辑（如审批流程）。
 */
public interface TimeBlockService {

    /**
     * 【内部服务】创建一个新的时间占用块。
     * 这是一个底层的、纯粹的调度操作，在执行插入前会进行严格的时间冲突检测。
     *
     * @param timeBlock 要创建的 TimeBlock 对象，必须包含 venueId, startDatetime, endDatetime。
     * @return 创建成功并持久化后的 TimeBlock 对象 (包含数据库生成的 ID)。
     * @throws com.cqwu.cqwu_appointment_system.exception.ReservationConflictException 如果指定时间段已被占用。
     * @throws IllegalArgumentException                                                如果时间范围不合法
     *                                                                                 (例如
     *                                                                                 start
     *                                                                                 >=
     *                                                                                 end)。
     */
    TimeBlock createTimeBlock(TimeBlock timeBlock);

    /**
     * 根据 ID 删除一个时间占用块。
     * 这是一个底层的调度操作，不进行任何业务权限校验，调用方需确保权限。
     *
     * @param id 要删除的 TimeBlock 的主键 ID。
     */
    void deleteTimeBlockById(Long id);

    /**
     * 查询指定场所在特定时间范围内的所有时间占用块。
     * 此方法用于检查时间冲突、渲染前端日历视图等场景。
     *
     * @param venueId 场所ID。
     * @param start   查询范围的开始时间。
     * @param end     查询范围的结束时间。
     * @return 一个包含所有重叠时间块的列表 (List<TimeBlock>)。
     */
    List<TimeBlock> findTimeBlocksForVenue(Long venueId, LocalDateTime start, LocalDateTime end);

    /**
     * 检查一个具体的时间段对于某个场所是否完全可用。
     * 这是所有上层业务（如 `ReservationService` 批准预约）进行冲突检测的唯一入口。
     *
     * @param venueId 场所ID。
     * @param start   要检查的开始时间。
     * @param end     要检查的结束时间。
     * @return 如果时间段内没有任何占位，返回 true；否则返回 false。
     */
    boolean isTimeSlotAvailable(Long venueId, LocalDateTime start, LocalDateTime end);

    /**
     * 根据ID查找单个时间占用块。
     *
     * @param id TimeBlock 的主键 ID。
     * @return TimeBlock 实体，如果不存在则返回 null。
     */
    TimeBlock findTimeBlockById(Long id);

    /**
     * [管理员/系统] 强制批量插入课程表 (COURSE) 类型的 TimeBlock。
     * 此方法具有高优先级：
     * 1. 它会检测与已存在的【预约 (APPOINTMENT)】类型的 TimeBlock 的冲突。
     * 2. 如果发生冲突，它会【自动删除】这些冲突的 TimeBlock，并将关联的 Reservation 状态置为 REJECTED。
     * 3. 最后批量插入新的课程 TimeBlock。
     *
     * @param courseTimeBlocks 要插入的、block_type 必须为 'COURSE' 的 TimeBlock 列表。
     * @return 成功插入的记录数量。
     */
    int forceInsertCourseTimeBlocks(List<TimeBlock> courseTimeBlocks);

    /**
     * [管理员/系统] 根据场所ID列表，批量删除所有【课程 (COURSE)】类型的 TimeBlock。
     * 通常在同步新学期课表前调用，用于清空旧的课程数据。
     *
     * @param venueIds 要清空课程安排的场所ID列表。
     * @return 成功删除的记录数量。
     */
    int deleteAllCourseTimeBlocksByVenueIds(List<Long> venueIds);

    /**
     * 根据场所ID和时间范围，删除所有 'COURSE' 类型的 TimeBlock。
     * 用于外部系统进行精确的增量数据清理。
     *
     * @param venueId 场所ID。
     * @param start   时间范围的开始。
     * @param end     时间范围的结束。
     * @return 成功删除的记录数。
     */
    int deleteCourseTimeBlocksByRange(Long venueId, LocalDateTime start, LocalDateTime end);
}