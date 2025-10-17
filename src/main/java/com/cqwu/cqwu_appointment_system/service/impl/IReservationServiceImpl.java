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
import java.util.stream.Collectors;

@Service
public class IReservationServiceImpl implements IReservationService {

    private static final Logger log = LoggerFactory.getLogger(IReservationServiceImpl.class);

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

        List<Long> idsToUpdate = finishedReservations.stream()
                .map(Reservation::getId)
                .collect(Collectors.toList());

        Reservation updateEntity = new Reservation();
        updateEntity.setStatus(STATUS_COMPLETED);

        ReservationExample updateExample = new ReservationExample();
        updateExample.createCriteria().andIdIn(idsToUpdate);

        int updatedCount = reservationMapper.updateByExampleSelective(updateEntity, updateExample);
        log.info("定时任务完成：共更新了 {} 条预约记录为 '{}'。", updatedCount, STATUS_COMPLETED);
    }

    /**
     * 创建一个新的预约,初始状态为“待审批”
     * 
     * @param reservation 包含 venueId, userId, startTime, endTime, purpose 的预约信息对象
     * @return 创建后的预约对象
     */
    @Override
    @Transactional
    public Reservation createReservation(Reservation reservation) {
        log.info("开始创建新预约, VenueId: {}, UserId: {}, Time: {} - {}", reservation.getVenueId(), reservation.getUserId(),
                reservation.getStartTime(), reservation.getEndTime());

        if (reservation.getStartTime() == null || reservation.getEndTime() == null) {
            throw new IllegalArgumentException("预约开始或结束时间不能为空");
        }
        if (reservation.getStartTime().isAfter(reservation.getEndTime())) {
            throw new IllegalArgumentException("预约开始时间不能晚于结束时间");
        }
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("不能预约过去的时间");
        }

        Venue venue = venueMapper.selectByPrimaryKey(reservation.getVenueId());
        if (venue == null) {
            throw new IllegalArgumentException("指定的场所不存在, VenueId: " + reservation.getVenueId());
        }
        if (!"available".equalsIgnoreCase(venue.getStatus())) {
            log.warn("用户尝试预约不可用的场所, VenueId: {}, Status: {}", venue.getId(), venue.getStatus());
            throw new VenueNotAvailableException(
                    "场所 '" + venue.getName() + "' 当前状态为 '" + venue.getStatus() + "', 不可预约。");
        }

        if (!isVenueAvailable(reservation.getVenueId(), reservation.getStartTime(), reservation.getEndTime())) {
            log.warn("预约时间冲突, VenueId: {}, Time: {} - {}", reservation.getVenueId(), reservation.getStartTime(),
                    reservation.getEndTime());
            throw new ReservationConflictException("该时间段已被预约，请选择其他时间。");
        }

        reservation.setReservationCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        reservation.setStatus(STATUS_PENDING);
        reservation.setCreateTime(LocalDateTime.now());
        reservationMapper.insertSelective(reservation);

        log.info("新预约创建成功, ReservationId: {}, Code: {}", reservation.getId(), reservation.getReservationCode());
        return reservation;
    }

    /**
     * 检查指定时间段内场地是否可用
     * 
     * @param venueId   场所ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 是否可用
     */
    @Override
    public boolean isVenueAvailable(Long venueId, LocalDateTime startTime, LocalDateTime endTime) {
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                .andStatusIn(Arrays.asList(STATUS_PENDING, STATUS_CONFIRMED))
                .andStartTimeLessThan(endTime)
                .andEndTimeGreaterThan(startTime);

        long count = reservationMapper.countByExample(example);
        return count == 0;
    }

    /**
     * 用户取消自己的预约
     * 
     * @param reservationId 要取消的预约ID
     * @param userId        操作者用户ID (用于安全校验)
     */
    @Override
    @Transactional
    public void cancelReservationByUser(Long reservationId, String userId) {
        log.info("用户 {} 尝试取消预约, ReservationId: {}", userId, reservationId);
        Reservation reservation = findAndCheckReservation(reservationId);

        if (!Objects.equals(reservation.getUserId(), userId)) {
            log.error("用户权限不足, User {} 尝试取消不属于自己的预约 {}", userId, reservationId);
            throw new PermissionDeniedException("您没有权限取消此预约。");
        }

        if (!Arrays.asList(STATUS_PENDING, STATUS_CONFIRMED).contains(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为 '" + reservation.getStatus() + "', 无法取消。");
        }

        if (LocalDateTime.now().isAfter(reservation.getStartTime())) {
            throw new IllegalStateException("预约已经开始或结束，无法取消。");
        }

        updateReservationStatus(reservationId, STATUS_CANCELLED);
        log.info("用户 {} 成功取消预约 {}", userId, reservationId);
    }

    /**
     * 根据用户ID分页查询预约记录
     * 
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 预约记录分页结果
     */
    @Override
    public Page<Reservation> findReservationsByUserId(String userId, Pageable pageable) {
        log.info("查询用户 {} 的预约记录, Page: {}, Size: {}", userId, pageable.getPageNumber(), pageable.getPageSize());
        PageHelper.startPage(pageable.getPageNumber() + 1, pageable.getPageSize());

        ReservationExample example = new ReservationExample();
        example.createCriteria().andUserIdEqualTo(userId);
        example.setOrderByClause("create_time DESC");

        List<Reservation> list = reservationMapper.selectByExample(example);
        PageInfo<Reservation> pageInfo = new PageInfo<>(list);

        log.info("查询用户 {} 的预约记录完成, 共 {} 条", userId, pageInfo.getTotal());
        return new PageImpl<>(pageInfo.getList(), pageable, pageInfo.getTotal());
    }

    /**
     * 管理员批准一个待处理的预约
     * 
     * @param reservationId 预约ID
     */
    @Override
    @Transactional
    public void approveReservation(Long reservationId) {
        log.info("管理员开始批准预约, ReservationId: {}", reservationId);
        Reservation reservation = findAndCheckReservation(reservationId);
        if (!STATUS_PENDING.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为'" + reservation.getStatus() + "', 无需审批。");
        }
        updateReservationStatus(reservationId, STATUS_CONFIRMED);
        log.info("预约 {} 已被批准", reservationId);
    }

    /**
     * 管理员拒绝一个待处理的预约
     * 
     * @param reservationId 预约ID
     * @param reason        拒绝原因 (可选)
     */
    @Override
    @Transactional
    public void rejectReservation(Long reservationId, String reason) {
        log.info("管理员开始拒绝预约, ReservationId: {}, Reason: {}", reservationId, reason);
        Reservation reservation = findAndCheckReservation(reservationId);
        if (!STATUS_PENDING.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为'" + reservation.getStatus() + "', 无法拒绝。");
        }
        updateReservationStatus(reservationId, STATUS_REJECTED);
        log.info("预约 {} 已被拒绝", reservationId);
    }

    /**
     * 管理员取消预约
     * 
     * @param reservationId 要取消的预约ID
     * @param reason        取消原因
     */
    @Override
    @Transactional
    public void cancelReservationByAdmin(Long reservationId, String reason) {
        log.info("管理员开始取消预约, ReservationId: {}, Reason: {}", reservationId, reason);
        Reservation reservation = findAndCheckReservation(reservationId);

        if (!Arrays.asList(STATUS_PENDING, STATUS_CONFIRMED).contains(reservation.getStatus())) {
            throw new IllegalStateException("该预约当前状态为 '" + reservation.getStatus() + "', 无法取消。");
        }
        updateReservationStatus(reservationId, STATUS_CANCELLED);
        log.info("预约 {} 已被管理员取消", reservationId);
    }

    /**
     * 根据多个条件动态搜索预约记录并分页
     * 
     * @param venueId  场所ID (可选)
     * @param userId   用户ID (可选)
     * @param status   预约状态 (可选)
     * @param date     日期 (可选)
     * @param pageable 分页参数
     * @return 预约记录分页结果
     */
    @Override
    public Page<Reservation> searchReservations(Long venueId, String userId, String status, LocalDate date,
            Pageable pageable) {
        log.info("开始搜索预约记录, VenueId: {}, UserId: {}, Status: {}, Date: {}, Page: {}, Size: {}",
                venueId, userId, status, date, pageable.getPageNumber(), pageable.getPageSize());
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

        log.info("搜索预约记录完成, 共找到 {} 条", pageInfo.getTotal());
        return new PageImpl<>(pageInfo.getList(), pageable, pageInfo.getTotal());
    }

    /**
     * 根据ID查找预约记录
     * 
     * @param reservationId 预约ID
     * @return 预约对象
     */
    @Override
    public Reservation findReservationById(Long reservationId) {
        log.debug("根据ID查找预约, ReservationId: {}", reservationId);
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    /**
     * 根据预约码查找预约记录
     * 
     * @param reservationCode 预约码
     * @return 预约对象
     */
    @Override
    public Reservation findReservationByCode(String reservationCode) {
        log.debug("根据预约码查找预约, Code: {}", reservationCode);
        ReservationExample example = new ReservationExample();
        example.createCriteria().andReservationCodeEqualTo(reservationCode);
        List<Reservation> reservations = reservationMapper.selectByExample(example);
        return CollectionUtils.isEmpty(reservations) ? null : reservations.get(0);
    }

    /**
     * 获取指定场地的即将到来的预约列表
     * 
     * @param venueId 场所ID
     * @param limit   返回记录的数量
     * @return 预约列表
     */
    @Override
    public List<Reservation> getUpcomingReservations(Long venueId, int limit) {
        log.info("获取场地 {} 即将到来的 {} 条预约", venueId, limit);
        PageHelper.startPage(1, limit);

        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andVenueIdEqualTo(venueId)
                .andStatusEqualTo(STATUS_CONFIRMED)
                .andStartTimeGreaterThanOrEqualTo(LocalDateTime.now());
        example.setOrderByClause("start_time ASC");

        return reservationMapper.selectByExample(example);
    }

    private Reservation findAndCheckReservation(Long reservationId) {
        Reservation reservation = reservationMapper.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            log.error("尝试操作不存在的预约记录, ReservationId: {}", reservationId);
            throw new IllegalArgumentException("ID为 " + reservationId + " 的预约记录不存在。");
        }
        return reservation;
    }

    private void updateReservationStatus(Long reservationId, String newStatus) {
        log.info("更新预约 {} 的状态为 '{}'", reservationId, newStatus);
        Reservation updateEntity = new Reservation();
        updateEntity.setId(reservationId);
        updateEntity.setStatus(newStatus);
        int updatedRows = reservationMapper.updateByPrimaryKeySelective(updateEntity);
        if (updatedRows == 0) {
            log.warn("更新预约 {} 状态失败，可能记录已被删除", reservationId);
        }
    }
}