package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.domain.dto.BatchDeleteRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.BatchUpdateVenueRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.CalendarEventDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateVenueRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueDetailDto;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueName;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueStatisticsDto;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.FileService;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description 负责处理所有与场所资源相关的API请求。
 */
@RestController
@RequestMapping("/api/venues")
@Slf4j
public class VenueController {

    @Autowired
    private VenueService venueService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FileService fileService;

    /**
     * 分页搜索场所信息。
     *
     * @param query 查询参数
     * @return 分页的场所列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> searchVenues(@Valid VenueQuery query) {
        log.info("开始搜索场所, 查询参数: {}", query);
        Page<Venue> venuePage = venueService.searchVenues(query);
        PageDTO<Venue> venuePageDTO = PageDTO.fromPage(venuePage);
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, venuePageDTO));
    }

    /**
     * 根据ID获取单个场所的详细信息。
     *
     * @param id 场所ID
     * @return 场所详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getVenueById(@PathVariable Long id) {
        log.info("查询场所详情, ID: {}", id);
        VenueDetailDto venueDetail = venueService.getVenueDetailById(id);
        if (venueDetail == null) {
            throw new IllegalArgumentException(Constants.QUERY_NOT_FOUND);
        }
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, venueDetail));
    }

    /**
     * 根据ID列表获取场所的ID和名称
     *
     * @param ids 场所ID列表
     * @return 包含ID和名称的场所列表
     */
    @PostMapping("/query-by-ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getVenuesByIds(@RequestBody List<Long> ids) {
        log.info("根据ID列表获取场所, IDs: {}", ids);
        List<Venue> venues = venueService.getVenuesByIds(ids);

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, venues));
    }

    /**
     * 获取所有场所的ID和名称
     *
     * @return 包含ID和名称的场所列表
     */
    @GetMapping("/all-names")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getAllVenues() {
        log.info("查询所有场所名称");
        List<VenueName> venues = venueService.getAllVenueNames();
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, venues));
    }

    /**
     * [管理员] 批量创建场所。
     *
     * @param venues 场所信息列表
     * @return 操作结果
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> createVenues(@RequestBody List<@Valid Venue> venues) {
        if (venues == null || venues.isEmpty()) {
            throw new IllegalArgumentException(Constants.PARAM_IS_EMPTY);
        }
        log.info("开始批量创建场所, 数量: {}", venues.size());
        int successCount = venueService.insertVenues(venues);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.created(String.format("成功添加 %d 个场所", successCount), venues));
    }

    /**
     * [管理员] 创建单个场所。
     *
     * @param venue 场所信息
     * @return 操作结果
     */
    @PostMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> createVenue(@RequestBody @Valid Venue venue) {
        log.info("开始创建场所: {}", venue.getName());

        int result = venueService.insertVenues(Arrays.asList(venue));
        if (result == 0) {
            throw new RuntimeException(Constants.CREATE_FAIL);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.created(Constants.CREATE_SUCCESS, venue));
    }

    /**
     * [管理员] 更新指定ID的场所信息。
     *
     * @param id            场所ID
     * @param updateRequest 包含更新字段的请求体
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> updateVenue(@PathVariable Long id,
            @RequestBody @Valid UpdateVenueRequest updateRequest) {
        log.info("开始更新场所信息, ID: {}", id);

        Venue venueToUpdate = new Venue();
        BeanUtils.copyProperties(updateRequest, venueToUpdate);
        venueToUpdate.setId(id);

        int result = venueService.updateVenueWithDescription(venueToUpdate, updateRequest.getDescription());
        if (result == 0) {
            throw new IllegalArgumentException(Constants.UPDATE_FAIL_NOT_FOUND);
        }

        VenueDetailDto updatedVenue = venueService.getVenueDetailById(id);

        notificationService.sendNotification(
                "all_users",
                Constants.SseEvent.VENUE_UPDATED,
                updatedVenue);

        return ResponseEntity.ok(AjaxResult.success(Constants.UPDATE_SUCCESS, updatedVenue));
    }

    /**
     * [管理员] 为场所关联附件
     * 
     * @param id        场所ID
     * @param sessionId 文件上传会话ID
     * @return 操作结果
     */
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> attachVenueFiles(
            @PathVariable Long id,
            @RequestParam String sessionId) {
        log.info("为场所 {} 关联附件，sessionId: {}", id, sessionId);

        Venue venue = venueService.selectVenueById(id);
        if (venue == null) {
            throw new IllegalArgumentException("场所不存在");
        }

        venueService.attachFilesToVenue(id, sessionId);

        return ResponseEntity.ok(AjaxResult.success("附件关联成功"));
    }

    /**
     * [管理员] 为场所关联轮播图
     * 
     * @param id        场所ID
     * @param sessionId 文件上传会话ID
     * @return 操作结果
     */
    @PostMapping("/{id}/carousel")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> attachVenueCarousel(
            @PathVariable Long id,
            @RequestParam String sessionId) {
        log.info("为场所 {} 关联轮播图，sessionId: {}", id, sessionId);

        Venue venue = venueService.selectVenueById(id);
        if (venue == null) {
            throw new IllegalArgumentException("场所不存在");
        }

        fileService.linkFilesToEntity(
                id,
                sessionId,
                EntityType.VENUE,
                FileCategory.CAROUSEL);

        return ResponseEntity.ok(AjaxResult.success("轮播图关联成功"));
    }

    /**
     * 查询场所的文件列表（轮播图或附件）
     * 
     * @param request 查询请求（包含场所ID和文件分类）
     * @return 文件列表
     */
    @GetMapping("/{id}/files")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getVenueFiles(
            @PathVariable Long id,
            @RequestParam FileCategory fileCategory) {
        log.info("查询场所文件列表，场所ID: {}, 文件分类: {}", id, fileCategory);

        List<com.cqwu.cqwu_appointment_system.domain.dto.FileDetailDto> files = fileService
                .getFilesByEntityAndCategorySorted(
                        id,
                        EntityType.VENUE,
                        fileCategory);

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, files));
    }

    /**
     * [管理员] 根据ID批量删除场所。
     * 
     * @param request 包含要删除的ID列表的请求体
     * @return 操作结果
     */
    @DeleteMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteVenues(@RequestBody @Valid BatchDeleteRequest request) {
        Long[] ids = request.getIds();
        log.info("开始批量删除场所, IDs: {}", Arrays.toString(ids));

        int result = venueService.deleteVenueByIds(ids);

        if (result == 0) {
            throw new IllegalArgumentException(Constants.DELETE_FAIL_NOT_FOUND);
        }

        notificationService.sendNotification(
                Constants.SseBroadcast.ALL_USERS,
                Constants.SseEvent.VENUE_DELETED,
                ids);
        return ResponseEntity.ok(AjaxResult.success(String.format("成功删除 %d 条记录", result)));
    }

    /**
     * [管理员] 批量更新多个场所的通用信息。
     *
     * @param request 包含ID列表和更新数据的请求体
     * @return 操作结果
     */
    @PutMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> updateVenuesBatch(@RequestBody @Valid BatchUpdateVenueRequest request) {
        log.info("开始批量更新 {} 个场所的信息...", request.getIds().length);
        Venue venueToUpdate = new Venue();
        BeanUtils.copyProperties(request.getVenueData(), venueToUpdate);

        int result = venueService.updateVenuesBatch(request.getIds(), venueToUpdate);
        if (result == 0) {
            throw new IllegalArgumentException(Constants.BATCH_UPDATE_FAIL_NOT_FOUND);
        }

        notificationService.sendNotification(
                Constants.SseBroadcast.ALL_USERS,
                Constants.SseEvent.VENUE_UPDATED,
                "list_updated");

        return ResponseEntity.ok(AjaxResult.success(String.format("成功更新 %d 条记录", result)));
    }

    /**
     * 获取指定场所在特定时间范围内的所有日程安排（用于日历视图）。
     *
     * @param venueId 场所ID
     * @param start   日历视图的开始时间 (ISO 8601 格式)
     * @param end     日历视图的结束时间
     * @param auth    Spring Security 注入的认证信息
     * @return 适用于前端日历的事件列表
     */
    @GetMapping("/{venueId}/time-blocks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getVenueSchedule(
            @PathVariable Long venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication auth) {

        User currentUser = ((LoginUser) auth.getPrincipal()).getUser();
        log.info("用户 '{}' 正在查询场所 '{}' 从 '{}' 到 '{}' 的日程安排", currentUser.getId(), venueId, start, end);

        List<CalendarEventDTO> calendarEvents = venueService.getVenueScheduleForUser(venueId, start, end, currentUser);

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, calendarEvents));
    }

    /**
     * 获取指定场所的统计信息（预约数量、预约人次、热门程度等）。
     *
     * @param id 场所ID
     * @return 场所统计信息
     */
    @GetMapping("/{id}/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getVenueStatistics(@PathVariable Long id) {
        log.info("查询场所统计信息, 场所ID: {}", id);

        Venue venue = venueService.selectVenueById(id);
        if (venue == null) {
            throw new IllegalArgumentException(Constants.QUERY_NOT_FOUND);
        }

        VenueStatisticsDto statistics = venueService.getVenueStatistics(id);
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, statistics));
    }

    /**
     * 导出所有场所的统计信息（仅管理员）
     * 返回JSON数据，由前端打包为Excel文件
     *
     * @return 所有场所的统计信息列表
     */
    @GetMapping("/export/statistics")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> exportVenuesStatistics() {
        log.info("管理员导出所有场所统计信息");
        List<VenueStatisticsDto> statistics = venueService.getAllVenuesStatistics();
        return ResponseEntity.ok(AjaxResult.success("导出成功", statistics));
    }
}