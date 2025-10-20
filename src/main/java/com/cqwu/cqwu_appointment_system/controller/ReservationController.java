package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.domain.dto.CreateReservationRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.RejectReservationRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateReservationStatusRequest;
import com.cqwu.cqwu_appointment_system.domain.query.ReservationQuery;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.exception.PermissionDeniedException;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/reservations")
@Slf4j
public class ReservationController {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private VenueService venueService;

    /**
     * 用户创建一个新的预约申请
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> createReservation(@RequestBody @Valid CreateReservationRequest request) {
        User currentUser = getCurrentUser();
        log.info("用户 '{}' 正在发起新的预约申请: {}", currentUser.getId(), request);

        Venue venue = venueService.selectVenueById(request.getVenueId());
        if (venue == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, "指定的场所不存在"));
        }

        try {
            ReservationWithBLOBs reservationToCreate = new ReservationWithBLOBs();
            reservationToCreate.setVenueId(request.getVenueId());
            reservationToCreate.setCreatorUserId(currentUser.getId());
            reservationToCreate.setTitle(request.getTitle());
            reservationToCreate.setPurpose(request.getPurpose());
            reservationToCreate.setStartDatetime(request.getStartDatetime());
            reservationToCreate.setEndDatetime(request.getEndDatetime());

            ReservationWithBLOBs createdReservation = reservationService.createReservation(reservationToCreate,
                    currentUser, venue);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AjaxResult(HttpStatus.CREATED, "预约申请已提交，等待审批", createdReservation));
        } catch (PermissionDeniedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * [管理员] 批准一个预约申请
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> approveReservation(@PathVariable Long id) {
        User adminUser = getCurrentUser();
        log.info("管理员 '{}' 正在批准预约, ID: {}", adminUser.getId(), id);
        try {
            UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forApproval(id, adminUser);
            ReservationWithBLOBs approvedReservation = reservationService.approveReservation(request);
            return ResponseEntity.ok(AjaxResult.success("申请已批准", approvedReservation));
        } catch (ReservationConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(AjaxResult.error(HttpStatus.CONFLICT, e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * [管理员] 拒绝一个预约申请
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> rejectReservation(@PathVariable Long id,
            @RequestBody @Valid RejectReservationRequest request) {
        User adminUser = getCurrentUser();
        log.info("管理员 '{}' 正在拒绝预约, ID: {}", adminUser.getId(), id);
        try {
            UpdateReservationStatusRequest updateRequest = UpdateReservationStatusRequest.forRejection(id, adminUser,
                    request.getReason());
            reservationService.rejectReservation(updateRequest);
            return ResponseEntity.ok(AjaxResult.success("申请已拒绝"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * 用户或管理员取消一个预约
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> cancelReservation(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        log.info("用户 '{}' 正在尝试取消预约, ID: {}", currentUser.getId(), id);
        try {
            UpdateReservationStatusRequest request = UpdateReservationStatusRequest.forCancellation(id, currentUser);
            reservationService.cancelReservation(request);
            return ResponseEntity.ok(AjaxResult.success("预约已取消"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, e.getMessage()));
        } catch (PermissionDeniedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }

    /**
     * 根据预约码查询预约详情
     */
    @GetMapping("/by-code/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getReservationByCode(@PathVariable String code) {
        log.info("正在根据预约码 '{}' 查询详情...", code);
        ReservationWithBLOBs reservation = reservationService.findReservationByCode(code);
        if (reservation != null) {
            return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, reservation));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.QUERY_NOT_FOUND));
        }
    }

    /**
     * 动态分页查询预约列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> searchReservations(ReservationQuery query) {
        User currentUser = getCurrentUser();
        log.info("用户 '{}' 正在查询预约列表, 查询参数: {}", currentUser.getId(), query);

        if (!Arrays.asList(Constants.UserRoleType.ROLE_ADMIN, Constants.UserRoleType.ROLE_ROOT)
                .contains(currentUser.getRole())) {
            query.setCreatorUserId(currentUser.getId());
        }

        Page<ReservationWithBLOBs> pageOfEntities = reservationService.searchReservations(query);
        PageDTO<ReservationWithBLOBs> pageOfDtos = PageDTO.fromPage(pageOfEntities);

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, pageOfDtos));
    }

    private User getCurrentUser() {
        return ((LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
    }
}