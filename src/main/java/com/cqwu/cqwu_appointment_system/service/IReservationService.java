package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author YourName
 * @description 预约记录服务接口
 * 定义了预约相关的核心业务逻辑
 */
public interface IReservationService {

    // --- 核心预约功能 ---

    /**
     * 创建一个新的预约
     * 这是最核心的功能。在实现时，必须包含以下逻辑：
     * 1. **权限校验**: 确认当前操作用户有预约权限。
     * 2. **冲突检测**: 检查该场所在指定时间段内是否已被预约。如果冲突，则抛出异常。
     * 3. **数据保存**: 如果没有冲突，则生成预约码(reservation_code)，设置初始状态(status)，并保存到数据库。
     *
     * @param reservation 包含 venueId, userId, startTime, endTime, purpose 的预约信息对象
     * @return 创建成功并带有完整信息的预约对象 (包括ID, create_time等)
     * @throws ReservationConflictException 如果时间段冲突
     * @throws VenueNotAvailableException 如果场所当前状态不可用
     */
    Reservation createReservation(Reservation reservation);

    /**
     * 检查指定场所在特定时间段内是否可用
     *
     * @param venueId 场所ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return true 如果可用, false 如果已被预约
     */
    boolean isVenueAvailable(Long venueId, LocalDateTime startTime, LocalDateTime endTime);


    // --- 用户侧功能 ---

    /**
     * 用户取消自己的预约
     * 实现时需要校验：
     * 1. 预约是否存在。
     * 2. 操作者是否是该预约的创建者。
     * 3. 预约的状态是否允许取消（例如，已开始或已完成的预约可能无法取消）。
     *
     * @param reservationId 要取消的预约ID
     * @param userId 操作者用户ID (用于安全校验)
     * @throws PermissionDeniedException 如果用户试图取消不属于自己的预约
     * @throws IllegalStateException 如果预约状态不允许取消
     */
    void cancelReservationByUser(Long reservationId, Long userId);

    /**
     * 根据用户ID分页查询其所有预约记录
     *
     * @param userId 用户ID
     * @param pageable 分页参数 (包含页码, 每页大小, 排序规则)
     * @return 预约记录的分页结果
     */
    Page<Reservation> findReservationsByUserId(Long userId, Pageable pageable);


    // --- 管理员侧功能 ---

    /**
     * 管理员审批预约 (通过)
     * 将预约状态从 'pending' (待审核) 更新为 'confirmed' (已确认)
     *
     * @param reservationId 预约ID
     */
    void approveReservation(Long reservationId);

    /**
     * 管理员审批预约 (拒绝)
     * 将预约状态更新为 'rejected' (已拒绝)，可能需要记录拒绝原因。
     *
     * @param reservationId 预约ID
     * @param reason 拒绝原因 (可选)
     */
    void rejectReservation(Long reservationId, String reason);

    /**
     * 管理员取消任意预约
     * (例如，因场馆临时维护需要取消)
     *
     * @param reservationId 要取消的预约ID
     * @param reason 取消原因
     */
    void cancelReservationByAdmin(Long reservationId, String reason);

    /**
     * 多条件动态分页查询所有预约记录 (管理员使用)
     *
     * @param venueId 场所ID (可选, 为null则不作为查询条件)
     * @param userId 用户ID (可选)
     * @param status 预约状态 (可选)
     * @param date 日期 (可选, 查询指定某一天的预约)
     * @param pageable 分页参数
     * @return 符合条件的预约记录分页结果
     */
//    Page<Reservation> searchReservations(Long venueId, Long userId, String status, LocalDateTime date, Pageable pageable);


    // ---通用查询功能 ---

    Page<Reservation> searchReservations(Long venueId, Long userId, String status, LocalDate date, Pageable pageable);

    /**
     * 根据ID查询预约详情
     *
     * @param reservationId 预约ID
     * @return 预约详情，如果不存在则返回 Optional.empty() 或 null
     */
    Reservation findReservationById(Long reservationId);

    /**
     * 根据预约码查询预约详情
     * 用于现场扫码核销等场景。
     *
     * @param reservationCode 预约码
     * @return 预约详情
     */
    Reservation findReservationByCode(String reservationCode);

    /**
     * 获取指定场所未来即将发生的预约
     * 可用于前台大屏展示等场景。
     *
     * @param venueId 场所ID
     * @param limit 返回记录的数量
     * @return 即将发生的预约列表
     */
    List<Reservation> getUpcomingReservations(Long venueId, int limit);
}