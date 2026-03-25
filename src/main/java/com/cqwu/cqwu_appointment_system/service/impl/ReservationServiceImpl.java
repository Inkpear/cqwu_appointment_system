package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import com.cqwu.cqwu_appointment_system.common.utils.QrCodeUtils;
import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.common.utils.ValidationUtils;
import com.cqwu.cqwu_appointment_system.common.utils.uuid.UUID;
import com.cqwu.cqwu_appointment_system.domain.dto.ReservationDetailDto;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationStatusRequest;
import com.cqwu.cqwu_appointment_system.domain.query.ReservationQuery;
import com.cqwu.cqwu_appointment_system.entity.*;
import com.cqwu.cqwu_appointment_system.exception.PermissionDeniedException;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.framework.redis.RedisCache;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.ReservationDetailMapper;
import com.cqwu.cqwu_appointment_system.mapper.TimeBlockMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryRuleMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import com.cqwu.cqwu_appointment_system.service.FileService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @description 预约服务实现类，负责处理所有预约相关的业务逻辑。
 */
@Service
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private ReservationDetailMapper reservationDetailMapper;

    @Autowired
    private TimeBlockMapper timeBlockMapper;

    @Autowired
    private TimeBlockService timeBlockService;

    @Autowired
    private VenueCategoryRuleMapper venueCategoryRuleMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    NotificationServiceImpl notificationService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private FileService fileService;

    /**
     * 创建一个新的预约申请。
     * 此方法遵循“竞争”原则，只在数据库中创建一条 PENDING 记录，
     * 不会创建 TimeBlock 或检查物理时间冲突。
     *
     * @param reservation 包含预约信息的实体对象
     * @param currentUser 当前操作的用户实体
     * @param venue       预约的场地实体
     * @return 创建成功并持久化后的预约实体
     * @throws IllegalArgumentException  如果场地不可用、时间不合法或超过最大预约时长
     * @throws PermissionDeniedException 如果用户类型不满足场地分类的预约规则
     */
    @Override
    @Transactional
    public ReservationWithBLOBs createReservation(ReservationWithBLOBs reservation, User currentUser, Venue venue) {
        log.debug("用户 '{}' 正在提交新预约申请...", reservation.getCreatorUserId());

        if (venue == null)
            throw new IllegalArgumentException("指定的场所不存在");

        if (venue == null || !Constants.VenueBaseStatus.AVAILABLE.equalsIgnoreCase(venue.getBaseStatus())) {
            String reason = (venue == null) ? "关联的场所不存在" : "场所当前状态为'" + venue.getBaseStatus() + "'，不可预约";
            throw new IllegalArgumentException(reason);
        }

        checkCategoryPermissions(currentUser, venue);

        ValidationUtils.requireStartBeforeEnd(reservation.getStartDatetime(), reservation.getEndDatetime(),
                "结束时间必须晚于开始时间");

        checkUserHasNoOverlappingReservations(reservation);

        reservation.setStatus(Constants.ReservationStatus.PENDING);
        reservation.setCreateTime(LocalDateTime.now());

        reservationMapper.insertSelective(reservation);
        log.debug("新预约申请已提交 (PENDING), Reservation ID: {}", reservation.getId());
        return reservation;
    }

    @Override
    @Transactional
    public ReservationWithBLOBs createReservationWithDetails(ReservationWithBLOBs reservation, User currentUser,
            Venue venue,
            String participants, Integer participantCount, String attachmentSessionId) {
        ReservationWithBLOBs createdReservation = createReservation(reservation, currentUser, venue);

        if (participants != null || participantCount != null) {
            ReservationDetail detail = new ReservationDetail();
            detail.setReservationId(createdReservation.getId());
            detail.setParticipants(participants);
            detail.setParticipantCount(participantCount);
            reservationDetailMapper.insert(detail);
            log.info("已保存预约 {} 的扩展信息：参与人员={}, 人数={}",
                    createdReservation.getId(), participants, participantCount);
        }

        if (StringUtils.isNotBlank(attachmentSessionId)) {
            fileService.linkFilesToEntity(
                    createdReservation.getId(),
                    attachmentSessionId,
                    EntityType.RESERVATION,
                    FileCategory.ATTACHMENT);
            log.info("已为预约 {} 关联会话 {} 的附件", createdReservation.getId(), attachmentSessionId);
        }

        return createdReservation;
    }

    /**
     * 批量关联附件到指定预约
     * 直接从 MinIO 查询会话文件并创建关联记录
     *
     * @param reservationId 预约 ID
     * @param sessionId     会话标识
     */
    @Override
    @Transactional
    public void attachFilesToReservation(Long reservationId, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }

        log.info("批量关联附件到预约，预约ID: {}, 会话ID: {}", reservationId, sessionId);

        fileService.linkFilesToEntity(
                reservationId,
                sessionId,
                EntityType.RESERVATION,
                FileCategory.ATTACHMENT);

        log.info("成功关联附件到预约 {}", reservationId);
    }

    /**
     * 取消一个预约申请。
     * 允许用户取消自己的 PENDING 或 CONFIRMED 状态的预约。
     * 管理员可以取消任何 PENDING 或 CONFIRMED 状态的预约。
     * 如果预约已批准 (CONFIRMED)，此操作会同步删除关联的 TimeBlock。
     *
     * @param request 包含预约ID和当前用户信息的请求DTO
     * @return 更新为 CANCELLED 状态的预约实体
     * @throws IllegalStateException     如果预约状态不合法，无法取消
     * @throws PermissionDeniedException 如果用户没有权限取消此预约
     */
    @Override
    @Transactional
    public ReservationWithBLOBs cancelReservation(UpdateReservationStatusRequest request) {
        User currentUser = request.getCurrentUser();
        Long reservationId = request.getReservationId();
        log.debug("用户 '{}' 正在尝试取消预约, ID: {}", currentUser.getId(), reservationId);

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

        log.debug("预约 ID: {} 已被成功取消", reservationId);
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    /**
     * 批准一个预约申请。
     * 这是创建 TimeBlock 并占用物理时间的唯一入口。
     * 在批准的瞬间，会进行最终的时间冲突检查。
     *
     * @param request 包含预约ID、批准意见和管理员信息的请求DTO
     * @return 更新为 CONFIRMED 状态的预约实体
     * @throws IllegalStateException        如果预约状态不是PENDING，或关联场所不可用，或预约已过期
     * @throws ReservationConflictException 如果该时间段已被占用
     */
    @Override
    @Transactional
    public ReservationWithBLOBs approveReservation(UpdateReservationStatusRequest request) {
        User adminUser = request.getCurrentUser();
        Long reservationId = request.getReservationId();
        log.debug("管理员 '{}' 正在批准预约, ID: {}", adminUser.getId(), reservationId);

        ReservationWithBLOBs reservation = findAndCheckPendingReservation(reservationId);

        Venue venue = venueMapper.selectByPrimaryKey(reservation.getVenueId());
        if (venue == null) {
            throw new IllegalStateException("关联的场所不存在，无法批准");
        }
        if (!Constants.VenueBaseStatus.AVAILABLE.equalsIgnoreCase(venue.getBaseStatus())) {
            throw new IllegalStateException("场所当前状态为'" + venue.getBaseStatus() + "'，无法批准");
        }
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
        updateEntity.setApprovalComment(request.getApprovalComment());
        reservationMapper.updateByPrimaryKeySelective(updateEntity);

        log.debug("预约 ID: {} 已被批准，并创建了关联的 TimeBlock ID: {}", reservationId, timeBlock.getId());
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    /**
     * 驳回一个预约申请。
     *
     * @param request 包含预约ID、驳回理由和管理员信息的请求DTO
     * @return 更新为 REJECTED 状态的预约实体
     * @throws IllegalStateException 如果预约状态不是PENDING
     */
    @Override
    @Transactional
    public ReservationWithBLOBs rejectReservation(UpdateReservationStatusRequest request) {
        User adminUser = request.getCurrentUser();
        Long reservationId = request.getReservationId();
        String reason = request.getApprovalComment();
        log.debug("管理员 '{}' 正在拒绝预约, ID: {}", adminUser.getId(), reservationId);

        findAndCheckPendingReservation(reservationId);

        ReservationWithBLOBs updateEntity = new ReservationWithBLOBs();
        updateEntity.setId(reservationId);
        updateEntity.setStatus(Constants.ReservationStatus.REJECTED);
        updateEntity.setApproverUserId(adminUser.getId());
        updateEntity.setApprovalTime(LocalDateTime.now());
        updateEntity.setApprovalComment(reason);
        reservationMapper.updateByPrimaryKeySelective(updateEntity);

        log.debug("预约 ID: {} 已被拒绝", reservationId);
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    /**
     * 根据ID查找预约信息。
     * 
     * @param reservationId 预约ID
     * @return 预约实体，如果不存在则返回 null
     */
    @Override
    public ReservationWithBLOBs findReservationById(Long reservationId) {
        return reservationMapper.selectByPrimaryKey(reservationId);
    }

    /**
     * 根据预约码查找预约信息。
     * 
     * @param code 预约码
     * @return 预约实体，如果不存在则返回 null
     */
    @Override
    public ReservationWithBLOBs findReservationByCode(String code) {
        if (StringUtils.isBlank(code))
            return null;
        ReservationExample example = new ReservationExample();
        example.createCriteria().andAppointmentCodeEqualTo(code);
        return reservationMapper.selectByExampleWithBLOBs(example).stream().findFirst().orElse(null);
    }

    /**
     * 根据查询条件分页搜索预约列表。
     * 
     * @param query 包含分页和过滤条件的查询对象
     * @return Spring Data Page 格式的预约列表
     */
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

    /**
     * 根据ID列表批量查找预约信息。
     * 
     * @param ids 预约ID列表
     * @return 预约实体列表
     */
    @Override
    public List<ReservationWithBLOBs> findReservationsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        ReservationExample example = new ReservationExample();
        example.createCriteria().andIdIn(ids);
        return reservationMapper.selectByExampleWithBLOBs(example);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationWithBLOBs approveBySystem(ReservationWithBLOBs reservation) {
        log.debug("【系统】正在自动批准预约 ID: {}", reservation.getId());

        // 创建 TimeBlock 占用物理时间
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.setVenueId(reservation.getVenueId());
        timeBlock.setStartDatetime(reservation.getStartDatetime());
        timeBlock.setEndDatetime(reservation.getEndDatetime());
        timeBlock.setTitle(reservation.getTitle());
        timeBlock.setBlockType(Constants.TimeBlockType.APPOINTMENT);
        timeBlock.setReservationId(reservation.getId());
        timeBlockMapper.insertSelective(timeBlock);

        // 更新预约状态为 CONFIRMED
        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setId(reservation.getId());
        update.setTimeBlockId(timeBlock.getId());
        update.setStatus(Constants.ReservationStatus.CONFIRMED);
        update.setApproverUserId("SYSTEM");
        update.setApprovalTime(LocalDateTime.now());
        update.setAppointmentCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        reservationMapper.updateByPrimaryKeySelective(update);

        log.debug("【系统】已自动批准预约 ID: {}", reservation.getId());

        notificationService.sendNotification(
                reservation.getCreatorUserId(),
                Constants.SseEvent.RESERVATION_AUTO_UPDATED,
                "您的预约状态已自动更新为已批准");
        return reservationMapper.selectByPrimaryKey(reservation.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationWithBLOBs rejectBySystem(ReservationWithBLOBs reservation, String reason) {
        log.warn("【系统】因 '{}'，正在自动拒绝预约 ID: {}", reason, reservation.getId());

        ReservationWithBLOBs update = new ReservationWithBLOBs();
        update.setId(reservation.getId());
        update.setStatus(Constants.ReservationStatus.REJECTED);
        update.setApproverUserId("SYSTEM");
        update.setApprovalTime(LocalDateTime.now());
        update.setApprovalComment(reason);
        reservationMapper.updateByPrimaryKeySelective(update);

        log.warn("【系统】已自动拒绝预约 ID: {}", reservation.getId());

        notificationService.sendNotification(
                reservation.getCreatorUserId(),
                Constants.SseEvent.RESERVATION_AUTO_UPDATED,
                "您的预约已被系统自动拒绝，原因：" + reason);

        return reservationMapper.selectByPrimaryKey(reservation.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateQrCodeForReservation(Long reservationId, User currentUser) {
        final String cacheKey = Constants.BASE64_URI_PREFIX + reservationId;
        String cachedQrCode = redisCache.getCacheObject(cacheKey);
        if (cachedQrCode != null) {
            log.debug("二维码缓存命中, ID: {}", reservationId);
            return cachedQrCode;
        }

        ReservationWithBLOBs reservation = reservationMapper.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            throw new EntityNotFoundException("预约不存在, ID: " + reservationId);
        }

        boolean isAdmin = Arrays.asList(Constants.UserRoleType.ROLE_ADMIN, Constants.UserRoleType.ROLE_ROOT)
                .contains(currentUser.getRole());

        if (!reservation.getCreatorUserId().equals(currentUser.getId()) && !isAdmin) {
            throw new AccessDeniedException("您无权访问此预约的二维码");
        }

        String verificationCode = reservation.getAppointmentCode();
        if (verificationCode == null || verificationCode.isEmpty()) {
            throw new IllegalStateException("该预约缺少核验码，无法生成二维码");
        }

        log.debug("二维码缓存未命中, 正在为预约 ID '{}' 生成新的二维码", reservationId);
        String qrCodeBase64 = QrCodeUtils.generateQrCodeBase64(verificationCode);

        long ttl = Duration.between(LocalDateTime.now(), reservation.getEndDatetime()).toMinutes() + 60;
        if (ttl > 0) {
            redisCache.setCacheObject(cacheKey, qrCodeBase64, ttl, TimeUnit.MINUTES);
        }

        return qrCodeBase64;
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

    /**
     * 检查指定用户在同一场所是否已有时间冲突的、有效的预约申请。
     * “有效”的申请包括 PENDING, CONFIRMED, IN_PROGRESS 状态。
     *
     * @param user    申请人
     * @param venueId 目标场所ID
     * @param start   申请的开始时间
     * @param end     申请的结束时间
     * @throws IllegalArgumentException 如果发现时间冲突的已有申请
     */
    private void checkUserHasNoOverlappingReservations(ReservationWithBLOBs reservation) {
        ReservationExample example = new ReservationExample();
        example.createCriteria()
                .andCreatorUserIdEqualTo(reservation.getCreatorUserId())
                .andVenueIdEqualTo(reservation.getVenueId())
                .andStatusIn(Arrays.asList(
                        Constants.ReservationStatus.PENDING,
                        Constants.ReservationStatus.CONFIRMED,
                        Constants.ReservationStatus.IN_PROGRESS))
                .andEndDatetimeGreaterThan(reservation.getStartDatetime())
                .andStartDatetimeLessThan(reservation.getEndDatetime());

        long conflictingCount = reservationMapper.countByExample(example);

        if (conflictingCount > 0) {
            throw new IllegalArgumentException("您在该场所的相近时间段内已有一个预约申请，请勿重复提交");
        }
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public int cleanReservationsWhereVenueNotExists() {
        log.debug("正在清理关联场所已被删除的预约记录...");

        List<Long> existingVenueIds = venueMapper.selectByExample(new VenueExample())
                .stream()
                .map(Venue::getId)
                .toList();

        if (existingVenueIds.isEmpty()) {
            long totalCount = reservationMapper.countByExample(new ReservationExample());
            if (totalCount > 0) {
                List<Long> allReservationIds = reservationMapper.selectByExample(new ReservationExample())
                        .stream()
                        .map(Reservation::getId)
                        .toList();
                if (!allReservationIds.isEmpty()) {
                    int fileDeleted = fileService.deleteFileRecordsByEntityIds(EntityType.RESERVATION,
                            allReservationIds);
                    log.debug("已清理 {} 条预约文件记录", fileDeleted);

                    int detailDeleted = reservationDetailMapper.deleteByReservationIds(allReservationIds);
                    log.debug("已清理 {} 条预约扩展信息", detailDeleted);
                }

                int deletedCount = reservationMapper.deleteByExample(new ReservationExample());
                log.warn("数据库中没有任何场所，已清理所有 {} 条预约记录", deletedCount);
                return deletedCount;
            }
            log.debug("数据库中没有场所也没有预约，无需清理");
            return 0;
        }

        ReservationExample example = new ReservationExample();
        example.createCriteria().andVenueIdNotIn(existingVenueIds);

        List<Long> reservationIdsToDelete = reservationMapper.selectByExample(example)
                .stream()
                .map(Reservation::getId)
                .toList();

        if (!reservationIdsToDelete.isEmpty()) {
            int fileDeleted = fileService.deleteFileRecordsByEntityIds(EntityType.RESERVATION, reservationIdsToDelete);
            log.debug("已清理 {} 条预约文件记录", fileDeleted);

            int detailDeleted = reservationDetailMapper.deleteByReservationIds(reservationIdsToDelete);
            log.debug("已清理 {} 条预约扩展信息", detailDeleted);
        }

        int deletedCount = reservationMapper.deleteByExample(example);
        log.debug("已清理 {} 条无效预约记录", deletedCount);
        return deletedCount;
    }

    @Override
    public List<Reservation> findReservationsByExample(ReservationExample example) {
        return reservationMapper.selectByExample(example);
    }

    @Override
    public List<ReservationDetailDto> getAllReservationsForExport() {
        log.info("查询所有预约数据（含扩展信息）用于导出");
        ReservationExample example = new ReservationExample();
        example.setOrderByClause("create_time DESC");
        List<ReservationWithBLOBs> reservations = reservationMapper.selectByExampleWithBLOBs(example);

        // 转换为包含扩展信息的 DTO
        return reservations.stream()
                .map(reservation -> {
                    ReservationDetail detail = reservationDetailMapper.selectByReservationId(reservation.getId());
                    return ReservationDetailDto.from(reservation, detail);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public ReservationDetailDto getReservationDetailById(Long reservationId) {
        log.info("查询预约详情（含扩展信息），预约ID: {}", reservationId);

        // 查询基础信息
        ReservationWithBLOBs reservation = reservationMapper.selectByPrimaryKey(reservationId);
        if (reservation == null) {
            return null;
        }

        // 查询扩展信息
        ReservationDetail detail = reservationDetailMapper.selectByReservationId(reservationId);

        // 组装DTO
        return ReservationDetailDto.from(reservation, detail);
    }
}
