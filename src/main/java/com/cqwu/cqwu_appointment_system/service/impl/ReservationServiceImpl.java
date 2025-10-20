package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.common.utils.ValidationUtils;
import com.cqwu.cqwu_appointment_system.common.utils.uuid.UUID;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationStatusRequest;
import com.cqwu.cqwu_appointment_system.domain.query.ReservationQuery;
import com.cqwu.cqwu_appointment_system.entity.*;
import com.cqwu.cqwu_appointment_system.exception.PermissionDeniedException;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.TimeBlockMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryRuleMapper;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private TimeBlockMapper timeBlockMapper;

    @Autowired
    private TimeBlockService timeBlockService;

    @Autowired
    private VenueCategoryRuleMapper venueCategoryRuleMapper;

    @Override
    @Transactional
    public ReservationWithBLOBs createReservation(ReservationWithBLOBs reservation, User currentUser, Venue venue) {
        log.info("用户 '{}' 正在提交新预约申请...", reservation.getCreatorUserId());

        if (venue == null)
            throw new IllegalArgumentException("指定的场所不存在");

        checkCategoryPermissions(currentUser, venue);

        ValidationUtils.requireStartBeforeEnd(reservation.getStartDatetime(), reservation.getEndDatetime(),
                "结束时间必须晚于开始时间");

        if (venue.getMaxBookingDurationMinutes() != null) {
            long duration = Duration.between(reservation.getStartDatetime(), reservation.getEndDatetime()).toMinutes();
            if (duration > venue.getMaxBookingDurationMinutes()) {
                throw new IllegalArgumentException("预约时长超过了该场所允许的最大限制");
            }
        }

        reservation.setStatus(Constants.ReservationStatus.PENDING);
        reservation.setCreateTime(LocalDateTime.now());

        reservationMapper.insertSelective(reservation);
        log.info("新预约申请已提交 (PENDING), Reservation ID: {}", reservation.getId());
        return reservation;
    }

    @Override
    @Transactional
    public ReservationWithBLOBs cancelReservation(UpdateReservationStatusRequest request) {
        User currentUser = request.getCurrentUser();
        Long reservationId = request.getReservationId();
        log.info("用户 '{}' 正在尝试取消预约, ID: {}", currentUser.getId(), reservationId);

        ReservationWithBLOBs reservation = findAndCheckReservation(reservationId);

        if (!StringUtils.equals(reservation.getStatus(), Constants.ReservationStatus.PENDING) &&
                !StringUtils.equals(reservation.getStatus(), Constants.ReservationStatus.CONFIRMED)) {
            throw new IllegalStateException("该预约当前状态为 '" + reservation.getStatus() + "', 无法取消");
        }

        boolean isRoot = currentUser.getRole().equals(Constants.UserRoleType.ROLE_ROOT);
        boolean isAdmin = currentUser.getRole().equals(Constants.UserRoleType.ROLE_ADMIN);
        boolean isOwner = currentUser.getId().equals(reservation.getCreatorUserId());
        if (!isRoot && !isAdmin && !isOwner) {
            throw new PermissionDeniedException("您没有权限取消此预约");
        }

        if (reservation.getTimeBlockId() != null) {
            timeBlockMapper.deleteByPrimaryKey(reservation.getTimeBlockId());
        }

        ReservationWithBLOBs updateEntity = new ReservationWithBLOBs();
        updateEntity.setId(reservationId);
        updateEntity.setStatus(Constants.ReservationStatus.CANCELLED);
        reservationMapper.updateByPrimaryKeySelective(updateEntity);

        log.info("预约 ID: {} 已被成功取消", reservationId);
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    @Override
    @Transactional
    public ReservationWithBLOBs approveReservation(UpdateReservationStatusRequest request) {
        User adminUser = request.getCurrentUser();
        Long reservationId = request.getReservationId();
        log.info("管理员 '{}' 正在批准预约, ID: {}", adminUser.getId(), reservationId);

        ReservationWithBLOBs reservation = findAndCheckPendingReservation(reservationId);

        if (reservation.getStartDatetime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("该预约的开始时间已过，无法批准");
        }

        if (!timeBlockService.isTimeSlotAvailable(reservation.getVenueId(), reservation.getStartDatetime(),
                reservation.getEndDatetime())) {
            throw new ReservationConflictException("无法批准，该时间段已被占用");
        }

        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setVenueId(reservation.getVenueId());
        timeBlock.setStartDatetime(reservation.getStartDatetime());
        timeBlock.setEndDatetime(reservation.getEndDatetime());
        timeBlock.setTitle(reservation.getTitle());
        timeBlock.setBlockType(Constants.TimeBlockType.APPOINTMENT);
        timeBlock.setReservationId(reservation.getId());
        timeBlockMapper.insertSelective(timeBlock);

        ReservationWithBLOBs updateEntity = new ReservationWithBLOBs();
        updateEntity.setId(reservationId);
        updateEntity.setTimeBlockId(timeBlock.getId());
        updateEntity.setStatus(Constants.ReservationStatus.CONFIRMED);
        updateEntity.setApproverUserId(adminUser.getId());
        updateEntity.setApprovalTime(LocalDateTime.now());
        updateEntity.setAppointmentCode(generateAppointmentCode());
        reservationMapper.updateByPrimaryKeySelective(updateEntity);

        log.info("预约 ID: {} 已被批准，并创建了关联的 TimeBlock ID: {}", reservationId, timeBlock.getId());
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    @Override
    @Transactional
    public ReservationWithBLOBs rejectReservation(UpdateReservationStatusRequest request) {
        User adminUser = request.getCurrentUser();
        Long reservationId = request.getReservationId();
        String reason = request.getReason();
        log.info("管理员 '{}' 正在拒绝预约, ID: {}", adminUser.getId(), reservationId);

        findAndCheckPendingReservation(reservationId);

        ReservationWithBLOBs updateEntity = new ReservationWithBLOBs();
        updateEntity.setId(reservationId);
        updateEntity.setStatus(Constants.ReservationStatus.REJECTED);
        updateEntity.setApproverUserId(adminUser.getId());
        updateEntity.setApprovalTime(LocalDateTime.now());
        updateEntity.setRejectionReason(reason);
        reservationMapper.updateByPrimaryKeySelective(updateEntity);

        log.info("预约 ID: {} 已被拒绝", reservationId);
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    @Override
    public ReservationWithBLOBs findReservationById(Long reservationId) {
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    @Override
    public ReservationWithBLOBs findReservationByCode(String code) {
        if (StringUtils.isBlank(code))
            return null;
        ReservationExample example = new ReservationExample();
        example.createCriteria().andAppointmentCodeEqualTo(code);
        return reservationMapper.selectByExampleWithBLOBs(example).stream().findFirst().orElse(null);
    }

    @Override
    public Page<ReservationWithBLOBs> searchReservations(ReservationQuery query) {
        Integer page = query.getPage() == null ? Constants.DEFAULT_PAGE : query.getPage();
        Integer size = query.getSize() == null ? Constants.DEFAULT_PAGE_SIZE : query.getSize();

        ReservationExample example = new ReservationExample();
        ReservationExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(query.getCreatorUserId()))
            criteria.andCreatorUserIdEqualTo(query.getCreatorUserId());
        if (StringUtils.isNotBlank(query.getStatus()))
            criteria.andStatusEqualTo(query.getStatus());

        if (query.getVenueId() != null)
            criteria.andVenueIdEqualTo(query.getVenueId());
        if (query.getDate() != null) {
            criteria.andStartDatetimeLessThan(query.getDate().plusDays(1).atStartOfDay());
            criteria.andEndDatetimeGreaterThan(query.getDate().atStartOfDay());
        }

        PageHelper.startPage(page + 1, size);
        List<ReservationWithBLOBs> list = reservationMapper.selectByExampleWithBLOBs(example);
        PageInfo<ReservationWithBLOBs> pageInfo = new PageInfo<>(list);

        return new PageImpl<>(list, PageRequest.of(page, size), pageInfo.getTotal());
    }

    @Override
    public List<ReservationWithBLOBs> findReservationsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        ReservationExample example = new ReservationExample();
        example.createCriteria().andIdIn(ids);
        return reservationMapper.selectByExampleWithBLOBs(example);
    }

    private String generateAppointmentCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private ReservationWithBLOBs findAndCheckReservation(Long reservationId) {
        ReservationWithBLOBs r = reservationMapper.selectByPrimaryKey(reservationId);
        if (r == null)
            throw new IllegalArgumentException("ID为 " + reservationId + " 的预约记录不存在");
        return r;
    }

    private ReservationWithBLOBs findAndCheckPendingReservation(Long reservationId) {
        ReservationWithBLOBs r = findAndCheckReservation(reservationId);
        if (!Constants.ReservationStatus.PENDING.equals(r.getStatus())) {
            throw new IllegalStateException("该申请当前状态为 '" + r.getStatus() + "', 无法进行此操作");
        }
        return r;
    }

    private void checkCategoryPermissions(User user, Venue venue) {
        if (venue.getCategoryId() == null) {
            return;
        }

        VenueCategoryRuleExample ruleExample = new VenueCategoryRuleExample();
        ruleExample.createCriteria().andCategoryIdEqualTo(venue.getCategoryId());

        List<VenueCategoryRule> rules = venueCategoryRuleMapper.selectByExample(ruleExample);
        if (rules.isEmpty()) {
            return;
        }

        boolean isAllowed = rules.stream()
                .anyMatch(rule -> rule.getAllowedUserType().equals(user.getUserType()));

        if (!isAllowed) {
            throw new PermissionDeniedException("您的用户类型不符合该场所分类的预约要求");
        }
    }
}