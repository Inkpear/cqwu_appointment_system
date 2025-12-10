package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import com.cqwu.cqwu_appointment_system.common.utils.ValidationUtils;
import com.cqwu.cqwu_appointment_system.domain.dto.CalendarEventDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueDetailDto;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueStatisticsDto;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueDetail;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueDetailMapper;
import com.cqwu.cqwu_appointment_system.service.FileService;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description VenueService 的实现类。
 */
@Service
@Slf4j
public class VenueServiceImpl implements VenueService {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private VenueCategoryMapper venueCategoryMapper;

    @Autowired
    private VenueDetailMapper venueDetailMapper;

    @Autowired
    private TimeBlockService timeBlockService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private FileService fileService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Venue selectVenueById(Long id) {
        log.debug("根据ID查询场所信息, ID: {}", id);
        Assert.notNull(id, "场所ID不能为空");
        return venueMapper.selectByPrimaryKey(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int updateVenue(Venue venue) {
        log.debug("开始更新场所信息, Venue ID: {}", venue.getId());
        Assert.notNull(venue, "场所信息不能为空");
        Assert.notNull(venue.getId(), "更新场所时，场所ID不能为空");

        Venue existingVenue = venueMapper.selectByPrimaryKey(venue.getId());
        if (existingVenue == null) {
            throw new IllegalArgumentException("更新失败，ID为 " + venue.getId() + " 的场所不存在");
        }

        validateCategoryId(venue.getCategoryId());

        LocalTime openTime = venue.getOpenTime() != null ? venue.getOpenTime() : existingVenue.getOpenTime();
        LocalTime closeTime = venue.getCloseTime() != null ? venue.getCloseTime() : existingVenue.getCloseTime();
        ValidationUtils.requireStartBeforeEnd(openTime, closeTime, "每日关闭时间必须晚于开放时间");

        int result = venueMapper.updateByPrimaryKeySelective(venue);
        log.debug("场所信息更新完成, Venue ID: {}, 影响行数: {}", venue.getId(), result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("null")
    @Override
    @Transactional
    public int insertVenues(List<Venue> venues) {
        log.debug("开始批量插入场所, 数量: {}", venues != null ? venues.size() : 0);
        Assert.notEmpty(venues, "要批量添加的场所列表不能为空");

        for (Venue venue : venues) {
            validateCategoryId(venue.getCategoryId());
            ValidationUtils.requireStartBeforeEnd(venue.getOpenTime(), venue.getCloseTime(), "每日关闭时间必须晚于开放时间");

            venue.setId(null);
            venue.setRealtimeStatus(null);
        }

        int result = venueMapper.insertBatch(venues);
        log.debug("成功批量插入 {} 条场所信息", result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteVenueByIds(Long[] ids) {
        log.debug("开始根据ID列表批量删除场所, IDs: {}", Arrays.toString(ids));
        Assert.notEmpty(ids, "要删除的ID列表不能为空");

        int detailDeleted = venueDetailMapper.deleteByVenueIds(ids);
        log.debug("已删除 {} 条场所扩展信息", detailDeleted);

        VenueExample example = new VenueExample();
        example.createCriteria().andIdIn(Arrays.asList(ids));

        int result = venueMapper.deleteByExample(example);
        log.debug("场所删除操作完成, 影响行数: {}", result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void attachFilesToVenue(Long venueId, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("尝试关联文件但 sessionId 为空");
            return;
        }

        log.info("关联附件到场所，场所ID: {}, sessionId: {}", venueId, sessionId);
        fileService.linkFilesToEntity(
                venueId,
                sessionId,
                EntityType.VENUE,
                FileCategory.ATTACHMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Venue> searchVenues(VenueQuery query) {
        log.debug("开始根据条件分页查询场所, 查询参数: {}", query);

        Integer page = query.getPage() == null ? Constants.DEFAULT_PAGE : query.getPage();
        Integer size = query.getSize() == null ? Constants.DEFAULT_PAGE_SIZE : query.getSize();
        PageHelper.startPage(page + 1, size);

        VenueExample example = buildVenueExample(query);

        List<Venue> venues = venueMapper.selectByExampleWithBLOBs(example);

        PageInfo<Venue> pageInfo = new PageInfo<>(venues);
        log.debug("查询完成, 共找到 {} 条数据", pageInfo.getTotal());
        return new PageImpl<>(pageInfo.getList(), PageRequest.of(page, size), pageInfo.getTotal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int updateRealtimeStatusBatch(List<Venue> venuesToUpdate) {
        if (venuesToUpdate == null || venuesToUpdate.isEmpty()) {
            return 0;
        }
        log.debug("开始批量更新 {} 个场所的实时状态...", venuesToUpdate.size());
        return venueMapper.updateRealtimeStatusBatch(venuesToUpdate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int updateVenuesBatch(Long[] ids, Venue venueUpdateData) {
        log.debug("管理员开始批量更新 {} 个场所的信息", ids.length);
        Assert.notEmpty(ids, "要更新的ID列表不能为空");
        Assert.notNull(venueUpdateData, "更新数据不能为空");

        validateCategoryId(venueUpdateData.getCategoryId());
        ValidationUtils.requireStartBeforeEnd(venueUpdateData.getOpenTime(), venueUpdateData.getCloseTime(),
                "批量更新失败：指定的关闭时间必须晚于开放时间");

        if (venueUpdateData.getOpenTime() != null || venueUpdateData.getCloseTime() != null) {
            List<Long> idList = Arrays.asList(ids);
            VenueExample example = new VenueExample();
            example.createCriteria().andIdIn(idList);

            Map<Long, Venue> existingVenuesMap = venueMapper.selectByExample(example).stream()
                    .collect(Collectors.toMap(Venue::getId, Function.identity()));

            for (Long id : idList) {
                Venue existingVenue = existingVenuesMap.get(id);
                if (existingVenue != null) {
                    LocalTime finalOpenTime = venueUpdateData.getOpenTime() != null ? venueUpdateData.getOpenTime()
                            : existingVenue.getOpenTime();
                    LocalTime finalCloseTime = venueUpdateData.getCloseTime() != null ? venueUpdateData.getCloseTime()
                            : existingVenue.getCloseTime();

                    ValidationUtils.requireStartBeforeEnd(finalOpenTime, finalCloseTime,
                            "批量更新失败：ID为 " + id + " 的场所时间范围不合法");
                }
            }
        }

        venueUpdateData.setId(null);
        venueUpdateData.setRealtimeStatus(null);
        List<Long> idList = Arrays.asList(ids);
        int result = venueMapper.updateVenuesBatch(venueUpdateData, idList);
        log.debug("批量更新操作完成, 影响行数: {}", result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CalendarEventDTO> getVenueScheduleForUser(Long venueId, LocalDateTime start, LocalDateTime end,
            User currentUser) {

        List<TimeBlock> timeBlocks = timeBlockService.findTimeBlocksForVenue(venueId, start, end);
        if (timeBlocks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> reservationIds = timeBlocks.stream()
                .filter(tb -> Constants.TimeBlockType.APPOINTMENT.equals(tb.getBlockType())
                        && tb.getReservationId() != null)
                .map(TimeBlock::getReservationId)
                .collect(Collectors.toList());

        Map<Long, Reservation> reservationMap = reservationService.findReservationsByIds(reservationIds)
                .stream()
                .collect(Collectors.toMap(Reservation::getId, Function.identity()));

        return timeBlocks.stream().map(tb -> {
            CalendarEventDTO dto = CalendarEventDTO.from(tb);

            boolean isOwned = false;
            if (Constants.TimeBlockType.APPOINTMENT.equals(tb.getBlockType()) && tb.getReservationId() != null) {
                Reservation r = reservationMap.get(tb.getReservationId());
                if (r != null && Objects.equals(r.getCreatorUserId(), currentUser.getId())) {
                    isOwned = true;
                }
            }
            dto.setOwnedByCurrentUser(isOwned);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 根据查询对象构建 MyBatis Example 对象。
     * 
     * @param query 查询条件封装对象
     * @return 构建好的 VenueExample 对象
     */
    private VenueExample buildVenueExample(VenueQuery query) {
        VenueExample example = new VenueExample();
        VenueExample.Criteria criteria = example.createCriteria();

        if (StringUtils.hasText(query.getName())) {
            criteria.andNameLike("%" + query.getName() + "%");
            if (!StringUtils.hasText(query.getLocation())) {
                example.or().andLocationLike("%" + query.getName() + "%");
            }
        }
        if (StringUtils.hasText(query.getBaseStatus()))
            criteria.andBaseStatusEqualTo(query.getBaseStatus());
        if (StringUtils.hasText(query.getRealtimeStatus()))
            criteria.andRealtimeStatusEqualTo(query.getRealtimeStatus());
        if (StringUtils.hasText(query.getLocation()))
            criteria.andLocationLike("%" + query.getLocation() + "%");

        if (!Objects.isNull(query.getFloor()))
            criteria.andFloorEqualTo(query.getFloor());
        if (!Objects.isNull(query.getRequiresApproval()))
            criteria.andRequiresApprovalEqualTo(query.getRequiresApproval());
        if (!Objects.isNull(query.getCategoryId()))
            criteria.andCategoryIdEqualTo(query.getCategoryId());

        if (!Objects.isNull(query.getNameOrderAsc())) {
            if (query.getNameOrderAsc()) {
                example.setOrderByClause("name ASC");
            } else {
                example.setOrderByClause("name DESC");
            }
        }

        return example;
    }

    /**
     * 私有辅助方法，用于校验 CategoryID 的存在性。
     * 
     * @param categoryId 待校验的场所分类ID
     * @throws IllegalArgumentException 如果分类ID不存在
     */
    private void validateCategoryId(Long categoryId) {
        if (categoryId != null && venueCategoryMapper.selectByPrimaryKey(categoryId) == null) {
            throw new IllegalArgumentException("操作失败，ID为 " + categoryId + " 的场所分类不存在");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VenueStatisticsDto getVenueStatistics(Long venueId) {
        log.debug("获取场所统计信息, venueId: {}", venueId);

        Venue venue = venueMapper.selectByPrimaryKey(venueId);
        if (venue == null) {
            throw new IllegalArgumentException("场所不存在");
        }

        com.cqwu.cqwu_appointment_system.entity.ReservationExample example = new com.cqwu.cqwu_appointment_system.entity.ReservationExample();
        example.createCriteria().andVenueIdEqualTo(venueId);
        List<Reservation> allReservations = reservationService.findReservationsByExample(example);

        long totalReservations = allReservations.size();

        long successfulReservations = allReservations.stream()
                .filter(r -> Constants.ReservationStatus.CONFIRMED.equals(r.getStatus())
                        || Constants.ReservationStatus.COMPLETED.equals(r.getStatus()))
                .count();

        long pendingReservations = allReservations.stream()
                .filter(r -> Constants.ReservationStatus.PENDING.equals(r.getStatus()))
                .count();

        long ongoingReservations = allReservations.stream()
                .filter(r -> Constants.ReservationStatus.CONFIRMED.equals(r.getStatus()))
                .count();

        long completedReservations = allReservations.stream()
                .filter(r -> Constants.ReservationStatus.COMPLETED.equals(r.getStatus()))
                .count();

        long totalParticipants = totalReservations;

        long uniqueUsers = allReservations.stream()
                .map(Reservation::getCreatorUserId)
                .distinct()
                .count();

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentReservations = allReservations.stream()
                .filter(r -> r.getCreateTime() != null && r.getCreateTime().isAfter(thirtyDaysAgo))
                .count();

        long totalApplied = allReservations.stream()
                .filter(r -> !Constants.ReservationStatus.CANCELLED.equals(r.getStatus()))
                .count();
        double approvalRate = totalApplied > 0
                ? (double) successfulReservations / totalApplied * 100
                : 0.0;

        double averageDurationMinutes = allReservations.stream()
                .filter(r -> r.getStartDatetime() != null && r.getEndDatetime() != null)
                .mapToLong(r -> java.time.Duration.between(r.getStartDatetime(), r.getEndDatetime()).toMinutes())
                .average()
                .orElse(0.0);

        LocalDateTime lastReservationTime = allReservations.stream()
                .map(Reservation::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        int popularityScore = calculatePopularityScore(
                recentReservations,
                uniqueUsers,
                approvalRate,
                totalReservations);

        double utilizationRate = calculateUtilizationRate(
                allReservations,
                thirtyDaysAgo,
                venue);

        return VenueStatisticsDto.builder()
                .venueId(venueId)
                .venueName(venue.getName())
                .totalReservations(totalReservations)
                .successfulReservations(successfulReservations)
                .pendingReservations(pendingReservations)
                .ongoingReservations(ongoingReservations)
                .completedReservations(completedReservations)
                .totalParticipants(totalParticipants)
                .uniqueUsers(uniqueUsers)
                .recentReservations(recentReservations)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .averageDurationMinutes(Math.round(averageDurationMinutes * 100.0) / 100.0)
                .lastReservationTime(lastReservationTime)
                .popularityScore(popularityScore)
                .utilizationRate(Math.round(utilizationRate * 100.0) / 100.0)
                .build();
    }

    /**
     * 计算热门度评分（0-100）
     * 综合考虑最近预约数、独立用户数、通过率、总预约数
     */
    private int calculatePopularityScore(long recentReservations, long uniqueUsers,
            double approvalRate, long totalReservations) {
        // 最近预约数得分（0-40分）
        double recentScore = Math.min(recentReservations / 10.0 * 40, 40);

        // 独立用户数得分（0-30分）
        double userScore = Math.min(uniqueUsers / 20.0 * 30, 30);

        // 通过率得分（0-20分）
        double approvalScore = approvalRate / 100.0 * 20;

        // 总预约数得分（0-10分）
        double totalScore = Math.min(totalReservations / 50.0 * 10, 10);

        return (int) Math.round(recentScore + userScore + approvalScore + totalScore);
    }

    /**
     * 计算使用率（最近30天）
     * 使用率 = 已预约时长 / 理论可用时长
     */
    private double calculateUtilizationRate(List<Reservation> allReservations,
            LocalDateTime thirtyDaysAgo,
            Venue venue) {
        long bookedMinutes = allReservations.stream()
                .filter(r -> r.getCreateTime() != null && r.getCreateTime().isAfter(thirtyDaysAgo))
                .filter(r -> Constants.ReservationStatus.CONFIRMED.equals(r.getStatus())
                        || Constants.ReservationStatus.COMPLETED.equals(r.getStatus()))
                .filter(r -> r.getStartDatetime() != null && r.getEndDatetime() != null)
                .mapToLong(r -> java.time.Duration.between(r.getStartDatetime(), r.getEndDatetime()).toMinutes())
                .sum();

        if (venue.getOpenTime() == null || venue.getCloseTime() == null) {
            return 0.0;
        }

        long dailyMinutes = java.time.Duration.between(
                venue.getOpenTime().atDate(LocalDateTime.now().toLocalDate()),
                venue.getCloseTime().atDate(LocalDateTime.now().toLocalDate())).toMinutes();

        long totalAvailableMinutes = dailyMinutes * 30;

        if (totalAvailableMinutes == 0) {
            return 0.0;
        }

        return (double) bookedMinutes / totalAvailableMinutes * 100;
    }

    @Override
    public List<VenueStatisticsDto> getAllVenuesStatistics() {
        log.info("查询所有场所的统计信息");

        VenueExample example = new VenueExample();
        List<Venue> allVenues = venueMapper.selectByExample(example);

        return allVenues.stream()
                .map(venue -> getVenueStatistics(venue.getId()))
                .toList();
    }

    @Override
    public VenueDetailDto getVenueDetailById(Long id) {
        log.info("查询场所详情（含扩展信息），场所ID: {}", id);

        // 查询基础信息
        Venue venue = venueMapper.selectByPrimaryKey(id);
        if (venue == null) {
            return null;
        }

        // 查询扩展信息
        VenueDetail detail = venueDetailMapper.selectByVenueId(id);

        // 组装DTO
        return VenueDetailDto.from(venue, detail);
    }

    @Override
    @Transactional
    public void updateVenueDescription(Long venueId, String description) {
        log.info("更新场所描述，场所ID: {}, 描述长度: {}", venueId,
                description != null ? description.length() : 0);

        Venue venue = venueMapper.selectByPrimaryKey(venueId);
        if (venue == null) {
            throw new IllegalArgumentException("场所不存在");
        }

        VenueDetail detail = new VenueDetail();
        detail.setVenueId(venueId);
        detail.setDescription(description);

        venueDetailMapper.insertOrUpdate(detail);
        log.info("场所描述更新成功");
    }

    @Override
    public List<Venue> getVenuesByIds(List<Long> ids) {
        log.info("根据ID列表查询场所名称和ID，IDs: {}", ids);

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return venueMapper.selectByIds(ids);
    }
}
