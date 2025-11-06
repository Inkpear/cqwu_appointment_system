package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.domain.dto.CreateReservationRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationStatusRequest;
import com.cqwu.cqwu_appointment_system.domain.query.ReservationQuery;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @description 负责处理所有与预约申请相关的API请求。
 *              所有业务异常均由 GlobalExceptionHandler 统一处理。
 */
@RestController
@RequestMapping("/api/reservations")
@Slf4j
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private VenueService venueService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 用户创建一个新的预约申请。
     *
     * @param request 包含预约详情的请求体
     * @param auth    Spring Security 自动注入的认证信息
     * @return 包含创建成功的预约信息的 ResponseEntity
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> createReservation(
            @RequestBody @Valid CreateReservationRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        User currentUser = loginUser.getUser();
        log.info(
                "用户 '{}' 正在发起新的预约申请: {}",
                currentUser.getId(),
                request);

        Venue venue = venueService.selectVenueById(request.getVenueId());
        if (venue == null) {
            throw new IllegalArgumentException("指定的场所不存在");
        }

        ReservationWithBLOBs reservationToCreate = mapToEntity(
                request,
                currentUser);
        ReservationWithBLOBs createdReservation = reservationService.createReservation(
                reservationToCreate,
                currentUser,
                venue);
        List<String> sendTarget = new ArrayList<>();
        sendTarget.add(Constants.UserRoleType.ROLE_MONITOR);

        if (venue.getRequiresApproval()) {
            sendTarget.add(Constants.UserRoleType.ROLE_ADMIN);
            sendTarget.add(Constants.UserRoleType.ROLE_ROOT);
        }

        notificationService.sendToRoles(
                sendTarget,
                Constants.SseEvent.NEW_RESERVATION,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AjaxResult.created("预约申请已提交，等待审批", createdReservation));
    }

    /**
     * [管理员] 批准一个预约申请。
     *
     * @param id      预约ID
     * @param comment 包含审批意见的请求体 (可选)
     * @param auth    Spring Security 自动注入的认证信息
     * @return 包含批准后的预约信息的 ResponseEntity
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> approveReservation(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid UpdateReservationRequest comment,
            @AuthenticationPrincipal LoginUser loginUser) {
        User adminUser = loginUser.getUser();
        log.info("管理员 '{}' 正在批准预约, ID: {}", adminUser.getId(), id);

        String approvalComment = (comment != null)
                ? comment.getApprovalComment()
                : "批准";
        UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forApproval(
                id,
                adminUser,
                approvalComment);
        ReservationWithBLOBs approvedReservation = reservationService.approveReservation(request);

        // 向用户发送通知
        notificationService.sendNotification(
                approvedReservation.getCreatorUserId(),
                Constants.SseEvent.RESERVATION_APPROVED,
                String.format("您的预约 '%s' 已被批准", approvedReservation.getTitle()));

        return ResponseEntity.ok(
                AjaxResult.success("申请已批准", approvedReservation));
    }

    /**
     * [管理员] 拒绝一个预约申请。
     *
     * @param id      预约ID
     * @param request 包含拒绝理由的请求体
     * @param auth    Spring Security 自动注入的认证信息
     * @return 操作成功的 ResponseEntity
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> rejectReservation(
            @PathVariable Long id,
            @RequestBody @Valid UpdateReservationRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        User adminUser = loginUser.getUser();
        log.info("管理员 '{}' 正在拒绝预约, ID: {}", adminUser.getId(), id);

        UpdateReservationStatusRequest updateRequest = UpdateReservationStatusRequest.forApproval(
                id,
                adminUser,
                request.getApprovalComment());
        ReservationWithBLOBs rejectedReservation = reservationService.rejectReservation(updateRequest);

        // 向用户发送通知
        notificationService.sendNotification(
                rejectedReservation.getCreatorUserId(),
                Constants.SseEvent.RESERVATION_REJECTED,
                String.format("您的预约 '%s' 已被拒绝，原因: %s", rejectedReservation.getTitle(),
                        rejectedReservation.getApprovalComment()));

        return ResponseEntity.ok(AjaxResult.success("申请已拒绝"));
    }

    /**
     * 用户或管理员取消一个预约。
     *
     * @param id   预约ID
     * @param auth Spring Security 自动注入的认证信息
     * @return 操作成功的 ResponseEntity
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        User currentUser = loginUser.getUser();
        log.info("用户 '{}' 正在尝试取消预约, ID: {}", currentUser.getId(), id);

        UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forCancellation(id, currentUser);
        ReservationWithBLOBs cancelledReservation = reservationService.cancelReservation(request);

        if (currentUser.getId().equals(cancelledReservation.getCreatorUserId())) {
            notificationService.sendToRole(
                    Constants.UserRoleType.ROLE_MONITOR,
                    Constants.SseEvent.RESERVATION_CANCELLED_BY_USER,
                    String.format("用户 '%s' 取消了预约 '%s'", currentUser.getName(), cancelledReservation.getTitle()));
        } else {
            notificationService.sendNotification(
                    cancelledReservation.getCreatorUserId(),
                    Constants.SseEvent.RESERVATION_CANCELLED_BY_ADMIN,
                    String.format("您的预约 '%s' 已被管理员取消", cancelledReservation.getTitle()));
        }

        return ResponseEntity.ok(AjaxResult.success("预约已取消"));
    }

    /**
     * 根据预约码查询预约详情。
     *
     * @param code 预约确认码
     * @return 包含预约详情的 ResponseEntity
     */
    @GetMapping("/by-code/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getReservationByCode(
            @PathVariable String code) {
        log.info("正在根据预约码 '{}' 查询详情...", code);
        ReservationWithBLOBs reservation = reservationService.findReservationByCode(code);
        if (reservation == null) {
            throw new IllegalArgumentException("未查询到该预约码对应的记录");
        }
        return ResponseEntity.ok(AjaxResult.success("查询成功", reservation));
    }

    /**
     * 获取指定预约的核验二维码。
     * 只有预约的创建者或管理员才能获取。
     *
     * @param id        预约的ID
     * @param loginUser Spring Security 自动注入的当前用户信息
     * @return 包含二维码 Base64 Data URI 的 ResponseEntity
     */
    @GetMapping("/{id}/qrcode")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getReservationQrCode(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        
        User user = loginUser.getUser();

        log.info("用户 '{}' 正在请求预约 ID '{}' 的二维码", user.getId(), id);

        String qrCodeBase64 = reservationService.generateQrCodeForReservation(id, user.getId());

        
        AjaxResult ajaxResult = AjaxResult.success("二维码获取成功");
        ajaxResult.put("qrCodeBase64", qrCodeBase64);

        return ResponseEntity.ok(ajaxResult);
    }

    /**
     * 动态分页查询预约列表。
     * 普通用户只能查询自己的预约，管理员可以查询所有。
     *
     * @param query 查询参数
     * @param auth  Spring Security 自动注入的认证信息
     * @return 包含分页预约列表的 ResponseEntity
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> searchReservations(
            @Valid ReservationQuery query,
            @AuthenticationPrincipal LoginUser loginUser) {
        User currentUser = loginUser.getUser();
        log.info(
                "用户 '{}' 正在查询预约列表, 查询参数: {}",
                currentUser.getId(),
                query);

        // 如果不是管理员，则强制查询条件为当前用户
        if (!Arrays.asList(
                Constants.UserRoleType.ROLE_ADMIN,
                Constants.UserRoleType.ROLE_ROOT).contains(currentUser.getRole())) {
            query.setCreatorUserId(currentUser.getId());
        }

        Page<ReservationWithBLOBs> pageOfEntities = reservationService.searchReservations(query);
        PageDTO<ReservationWithBLOBs> pageOfDtos = PageDTO.fromPage(
                pageOfEntities);

        return ResponseEntity.ok(AjaxResult.success("查询成功", pageOfDtos));
    }

    /**
     * 将 CreateReservationRequest DTO 转换为 ReservationWithBLOBs 实体。
     * 
     * @param request     DTO
     * @param currentUser 当前用户
     * @return 实体
     */
    private ReservationWithBLOBs mapToEntity(
            CreateReservationRequest request,
            User currentUser) {
        ReservationWithBLOBs entity = new ReservationWithBLOBs();
        entity.setVenueId(request.getVenueId());
        entity.setCreatorUserId(currentUser.getId());
        entity.setTitle(request.getTitle());
        entity.setPurpose(request.getPurpose());
        entity.setStartDatetime(request.getStartDatetime());
        entity.setEndDatetime(request.getEndDatetime());
        return entity;
    }
}
