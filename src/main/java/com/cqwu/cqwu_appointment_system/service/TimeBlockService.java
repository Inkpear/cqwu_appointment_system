package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.TimeBlock;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 场所时间占用 (TimeBlock) 的核心调度服务接口。
 *              它的唯一职责是管理时间的占用与释放，不关心具体的业务逻辑。
 */
public interface TimeBlockService {

    /**
     * 【内部使用】创建一个新的时间占用块。
     * 这是一个底层的、纯粹的调度操作，不包含业务逻辑。
     * 它会进行时间冲突检测和时间范围校验。
     *
     * @param timeBlock 要创建的 TimeBlock 对象
     * @return 创建成功后的 TimeBlock 对象 (包含数据库生成的 ID)
     * @throws com.cqwu.cqwu_appointment_system.exception.ReservationConflictException 如果时间段已被占用
     * @throws IllegalArgumentException 如果时间范围不合法 (start >= end)
     */
    TimeBlock createTimeBlock(TimeBlock timeBlock);

    /**
     * 根据 ID 删除一个时间占用块。
     * 这是一个底层的调度操作，不进行任何业务权限校验。
     *
     * @param id 要删除的 TimeBlock 的 ID
     */
    void deleteTimeBlockById(Long id);

    /**
     * 查询指定场所在特定时间范围内的所有占用块。
     * 这是渲染前端日历视图和支持定时任务的核心方法。
     *
     * @param venueId 场所ID
     * @param start   查询范围的开始时间
     * @param end     查询范围的结束时间
     * @return TimeBlock 列表
     */
    List<TimeBlock> findTimeBlocksForVenue(Long venueId, LocalDateTime start, LocalDateTime end);

    /**
     * 检查一个具体的时间段对于某个场所是否完全可用 (没有被任何 TimeBlock 占用)。
     * 这是所有上层业务（如创建预约）进行冲突检测的唯一入口。
     *
     * @param venueId 场所ID
     * @param start   要检查的开始时间
     * @param end     要检查的结束时间
     * @return 如果可用则返回 true, 否则返回 false
     */
    boolean isTimeSlotAvailable(Long venueId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 根据ID查找单个时间占用块。
     *
     * @param id TimeBlock ID
     * @return TimeBlock 对象，如果不存在则返回 null
     */
    TimeBlock findTimeBlockById(Long id);
}