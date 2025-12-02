package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.common.utils.ValidationUtils;
import com.cqwu.cqwu_appointment_system.domain.dto.CalendarEventDTO;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
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

            venue.setId(null); // 确保ID由数据库生成
            // 设置为null，让数据库使用默认值'UNKNOWN'
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
                com.cqwu.cqwu_appointment_system.common.constant.EntityType.VENUE,
                com.cqwu.cqwu_appointment_system.common.constant.FileCategory.ATTACHMENT
        );
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

        if (StringUtils.hasText(query.getName()))
            criteria.andNameLike("%" + query.getName() + "%");
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
}