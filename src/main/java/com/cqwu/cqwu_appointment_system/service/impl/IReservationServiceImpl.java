package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.entity.ReservationExample;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.exception.PermissionDeniedException;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.exception.VenueNotAvailableException;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.IReservationService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class IReservationServiceImpl implements IReservationService {

    private static final Logger log = LoggerFactory.getLogger(IReservationServiceImpl.class);

    //将状态定义为常量，避免硬编码字符串
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_COMPLETED = "completed";

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private VenueMapper venueMapper;



    /**
     * 定时任务：每小时执行一次，用于更新已过期的预约状态。
     * cron表达式 "0 0 * * * ?" 表示每个整点执行。
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void updateFinishedReservations() {
        log.info("开始执行定时任务：更新已完成的预约状态...");

        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andStatusEqualTo(STATUS_CONFIRMED)
                .andEndTimeLessThan(LocalDateTime.now());

        List<Reservation> finishedReservations = reservationMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(finishedReservations)) {
            log.info("没有需要更新的已完成预约。");
            return;
        }

        List<Long> idsToUpdate = finishedReservations.stream().map(Reservation::getId).toList();

        Reservation updateEntity = new Reservation();
        updateEntity.setStatus(STATUS_COMPLETED); // 更新为 "completed"

        ReservationExample updateExample = new ReservationExample();
        updateExample.createCriteria().andIdIn(idsToUpdate);

        int updatedCount = reservationMapper.updateByExampleSelective(updateEntity, updateExample);
        log.info("定时任务完成：共更新了 {} 条预约记录为 '{}'。", updatedCount, STATUS_COMPLETED);
    }

    /**
     *创建一个新的预约,初始状态为“待审批”
     * @param reservation 包含 venueId, userId, startTime, endTime, purpose 的预约信息对象
     * @return
     */
    @Override
    @Transactional
    public Reservation createReservation(Reservation reservation) {
        // 1. 基本参数校验
        if (reservation.getStartTime() == null || reservation.getEndTime() == null) {
            throw new IllegalArgumentException("预约开始或结束时间不能为空");
        }
        if (reservation.getStartTime().isAfter(reservation.getEndTime())) {
            throw new IllegalArgumentException("预约开始时间不能晚于结束时间");
        }
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("不能预约过去的时间");
        }

        // 2. 检查场所状态
        Venue venue = venueMapper.selectByPrimaryKey(reservation.getVenueId());
        if (venue == null) {
            throw new IllegalArgumentException("指定的场所不存在");
        }
        if (!"available".equalsIgnoreCase(venue.getStatus())) {
            throw new VenueNotAvailableException("场所 '" + venue.getName() + "' 当前状态为 '" + venue.getStatus() + "', 不可预约。");
        }

        // 3. 冲突检测
        if (!isVenueAvailable(reservation.getVenueId(), reservation.getStartTime(), reservation.getEndTime())) {
            throw new ReservationConflictException("该时间段已被预约，请选择其他时间。");
        }

        // 4. 完善预约信息并保存
        reservation.setReservationCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        reservation.setStatus(STATUS_PENDING); // 新预约状态为 "pending"
        reservation.setCreateTime(LocalDateTime.now());
        reservationMapper.insertSelective(reservation);

        return reservation;
    }


    /**
     *
     * 检查指定时间段内场地是否可用
     * 只有 'pending' 和 'confirmed' 状态的预约才被认为是占用了时间
     * @param venueId 场所ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return
     */
    @Override
    public boolean isVenueAvailable(Long venueId, LocalDateTime startTime, LocalDateTime endTime) {
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                // 【关键修改】只考虑待审批和已确认的预约
                .andStatusIn(Arrays.asList(STATUS_PENDING, STATUS_CONFIRMED))
                // 冲突条件: (现有预约的开始时间 < 新预约的结束时间) AND (现有预约的结束时间 > 新预约的开始时间)
                .andStartTimeLessThan(endTime)
                .andEndTimeGreaterThan(startTime);

        return reservationMapper.countByExample(example) == 0;
    }

    /**
     *
     * 用户取消自己的预约
     * @param reservationId 要取消的预约ID
     * @param userId 操作者用户ID (用于安全校验)
     */
    @Override
    @Transactional
    public void cancelReservationByUser(Long reservationId, Long userId) {
        Reservation reservation = findAndCheckReservation(reservationId);

        if (!Objects.equals(reservation.getUserId(), userId)) {
            throw new PermissionDeniedException("您没有权限取消此预约。");
        }

        // 【建议修改】增加状态检查
        if (!Arrays.asList(STATUS_PENDING, STATUS_CONFIRMED).contains(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为 '" + reservation.getStatus() + "', 无法取消。");
        }

        if (LocalDateTime.now().isAfter(reservation.getStartTime())) {
            throw new IllegalStateException("预约已经开始或结束，无法取消。");
        }

        // 【建议修改】使用常量
        updateReservationStatus(reservationId, STATUS_CANCELLED);
    }

    /**
     *
     * 根据用户ID分页查询预约记录
     * @param userId 用户ID
     * @param pageable 分页参数 (包含页码, 每页大小, 排序规则)
     * @return
     */
    @Override
    public Page<Reservation> findReservationsByUserId(Long userId, Pageable pageable) {
        // 使用PageHelper进行分页
        PageHelper.startPage(pageable.getPageNumber() + 1, pageable.getPageSize());

        ReservationExample example = new ReservationExample();
        example.createCriteria().andUserIdEqualTo(userId);
        example.setOrderByClause("create_time DESC"); // 按创建时间降序

        List<Reservation> list = reservationMapper.selectByExample(example);
        PageInfo<Reservation> pageInfo = new PageInfo<>(list);

        return new PageImpl<>(pageInfo.getList(), pageable, pageInfo.getTotal());
    }

    /**
     *管理员批准一个待处理的预约
     * @param reservationId 预约ID
     */
    @Override
    @Transactional
    public void approveReservation(Long reservationId) {
        Reservation reservation = findAndCheckReservation(reservationId);
        if (!STATUS_PENDING.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为'" + reservation.getStatus() + "', 无需审批。");
        }
        updateReservationStatus(reservationId, STATUS_CONFIRMED);
    }

    /**
     *管理员拒绝一个待处理的预约
     * @param reservationId 预约ID
     * @param reason 拒绝原因 (可选)
     */
    @Override
    @Transactional
    public void rejectReservation(Long reservationId, String reason) {
        Reservation reservation = findAndCheckReservation(reservationId);
        if (!STATUS_PENDING.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为'" + reservation.getStatus() + "', 无法拒绝。");
        }
        updateReservationStatus(reservationId, STATUS_REJECTED);
    }

    /**
     *管理员取消预约
     * @param reservationId 要取消的预约ID
     * @param reason 取消原因
     */

    @Override
    @Transactional
    public void cancelReservationByAdmin(Long reservationId, String reason) {
        Reservation reservation = findAndCheckReservation(reservationId);

        // 【建议修改】增加状态检查
        if (!Arrays.asList(STATUS_PENDING, STATUS_CONFIRMED).contains(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为 '" + reservation.getStatus() + "', 无法取消。");
        }

        // (可选) 管理员可以取消已开始的预约，所以这里可以不检查开始时间

        // 【建议修改】使用常量
        updateReservationStatus(reservationId, STATUS_CANCELLED);
    }

//    @Override
//    public Page<Reservation> searchReservations(Long venueId, Long userId, String status, LocalDateTime date, Pageable pageable) {
//        return null;
//    }

    /**
     *根据多个条件动态搜索预约记录并分页
     * @param venueId 场所ID (可选, 为null则不作为查询条件)
     * @param userId 用户ID (可选)
     * @param status 预约状态 (可选)
     * @param date 日期 (可选, 查询指定某一天的预约)
     * @param pageable 分页参数
     * @return
     */

    @Override
    public Page<Reservation> searchReservations(Long venueId, Long userId, String status, LocalDate date, Pageable pageable) {
        PageHelper.startPage(pageable.getPageNumber() + 1, pageable.getPageSize());

        ReservationExample example = new ReservationExample();
        ReservationExample.Criteria criteria = example.createCriteria();

        if (venueId != null) {
            criteria.andVenueIdEqualTo(venueId);
        }
        if (userId != null) {
            criteria.andUserIdEqualTo(userId);
        }
        if (StringUtils.hasText(status)) {
            criteria.andStatusEqualTo(status);
        }
        if (date != null) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            criteria.andStartTimeLessThan(endOfDay);
            criteria.andEndTimeGreaterThan(startOfDay);
        }

        example.setOrderByClause("create_time DESC");

        List<Reservation> list = reservationMapper.selectByExample(example);
        PageInfo<Reservation> pageInfo = new PageInfo<>(list);

        return new PageImpl<>(pageInfo.getList(), pageable, pageInfo.getTotal());
    }

    /**
     *根据ID查找预约记录
     * @param reservationId 预约ID
     * @return
     */

    @Override
    public Reservation findReservationById(Long reservationId) {
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    /**
     *根据预约码查找预约记录
     * @param reservationCode 预约码
     * @return
     */
    @Override
    public Reservation findReservationByCode(String reservationCode) {
        ReservationExample example = new ReservationExample();
        example.createCriteria().andReservationCodeEqualTo(reservationCode);
        List<Reservation> reservations = reservationMapper.selectByExample(example);
        return CollectionUtils.isEmpty(reservations) ? null : reservations.get(0);
    }

    /**
     *获取指定场地的即将到来的预约列表
     * @param venueId 场所ID
     * @param limit 返回记录的数量
     * @return
     */
    @Override
    public List<Reservation> getUpcomingReservations(Long venueId, int limit) {
        // 使用PageHelper的limit功能
        PageHelper.startPage(1, limit);

        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                .andStatusEqualTo("confirmed")
                .andStartTimeGreaterThanOrEqualTo(LocalDateTime.now());
        example.setOrderByClause("start_time ASC");

        return reservationMapper.selectByExample(example);
    }

    // --- 内部辅助方法 ---

    /**
     *查找并检查预约是否存在
     * @param reservationId
     * @return
     */
    private Reservation findAndCheckReservation(Long reservationId) {
        Reservation reservation = reservationMapper.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("ID为 " + reservationId + " 的预约记录不存在。");
        }
        return reservation;
    }

    /**
     *更新预约的状态
     * @param reservationId
     * @param newStatus
     */
    private void updateReservationStatus(Long reservationId, String newStatus) {
        Reservation updateEntity = new Reservation();
        updateEntity.setId(reservationId);
        updateEntity.setStatus(newStatus);
        // 使用Selective方法，只更新传入的非null字段
        reservationMapper.updateByPrimaryKeySelective(updateEntity);
    }
}
