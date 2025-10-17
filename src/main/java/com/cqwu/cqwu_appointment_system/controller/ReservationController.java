package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.exception.PermissionDeniedException;
import com.cqwu.cqwu_appointment_system.exception.ReservationConflictException;
import com.cqwu.cqwu_appointment_system.exception.VenueNotAvailableException;
import com.cqwu.cqwu_appointment_system.service.IReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Lyuih
 * @description 预约服务的控制器，提供RESTful API接口
 */
@RestController
@RequestMapping("/api/reservations")
@Slf4j
public class ReservationController {

    @Autowired
    private IReservationService reservationService;

    /**
     * 创建一个新的预约
     * 
     * @param reservation 预约信息，从请求体中获取
     * @return 操作结果
     */
    @PostMapping
    public ResponseEntity<AjaxResult> createReservation(@RequestBody Reservation reservation) {
        Optional<String> userIdOpt = getCurrentUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AjaxResult.error(HttpStatus.UNAUTHORIZED, "用户未登录"));
        }
        reservation.setUserId(userIdOpt.get());
        log.info("用户 '{}' 正在创建新预约: {}", reservation.getUserId(), reservation);

        try {
            Reservation created = reservationService.createReservation(reservation);
            return ResponseEntity.status(HttpStatus.CREATED).body(AjaxResult.success("预约创建成功", created));
        } catch (IllegalArgumentException | VenueNotAvailableException e) {
            log.warn("创建预约失败 (参数错误或场所不可用): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AjaxResult.error(e.getMessage()));
        } catch (ReservationConflictException e) {
            log.warn("创建预约失败 (时间冲突): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 根据ID查询预约详情
     * 
     * @param id 预约ID
     * @return 预约详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<AjaxResult> getReservationById(@PathVariable Long id) {
        log.info("查询预约详情, ID: {}", id);
        Reservation reservation = reservationService.findReservationById(id);
        if (reservation != null) {
            return ResponseEntity.ok(AjaxResult.success(reservation));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AjaxResult.error("预约不存在"));
        }
    }

    /**
     * 动态条件搜索预约
     * 
     * @param venueId  场地ID (可选)
     * @param userId   用户ID (可选)
     * @param status   状态 (可选)
     * @param date     日期 (可选)
     * @param pageable 分页参数
     * @return 预约分页列表
     */
    @GetMapping
    public ResponseEntity<AjaxResult> searchReservations(
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Pageable pageable) {
        log.info("动态搜索预约, venueId={}, userId={}, status={}, date={}", venueId, userId, status, date);
        Page<Reservation> reservations = reservationService.searchReservations(venueId, userId, status, date, pageable);
        return ResponseEntity.ok(AjaxResult.success(PageDTO.fromPage(reservations)));
    }

    /**
     * 用户取消自己的预约
     * 
     * @param id 预约ID
     * @return 操作结果
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<AjaxResult> cancelReservationByUser(@PathVariable Long id) {
        Optional<String> userIdOpt = getCurrentUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AjaxResult.error(HttpStatus.UNAUTHORIZED, "用户未登录"));
        }
        String currentUserId = userIdOpt.get();
        log.info("用户 '{}' 正在取消预约, ID: {}", currentUserId, id);

        try {
            reservationService.cancelReservationByUser(id, currentUserId);
            return ResponseEntity.ok(AjaxResult.success("预约已取消"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AjaxResult.error(e.getMessage()));
        } catch (PermissionDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AjaxResult.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 管理员批准预约
     * 
     * @param id 预约ID
     * @return 操作结果
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ROOT')")
    public ResponseEntity<AjaxResult> approveReservation(@PathVariable Long id) {
        log.info("管理员正在批准预约, ID: {}", id);
        try {
            reservationService.approveReservation(id);
            return ResponseEntity.ok(AjaxResult.success("预约已批准"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AjaxResult.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 管理员拒绝预约
     * 
     * @param id     预约ID
     * @param reason 拒绝原因
     * @return 操作结果
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ROOT')")
    public ResponseEntity<AjaxResult> rejectReservation(@PathVariable Long id,
            @RequestParam(required = false) String reason) {
        log.info("管理员正在拒绝预约, ID: {}, Reason: {}", id, reason);
        try {
            reservationService.rejectReservation(id, reason);
            return ResponseEntity.ok(AjaxResult.success("预约已拒绝"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AjaxResult.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 管理员取消预约
     * 
     * @param id     预约ID
     * @param reason 取消原因
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ROOT')")
    public ResponseEntity<AjaxResult> cancelReservationByAdmin(@PathVariable Long id, @RequestParam String reason) {
        log.info("管理员正在取消预约, ID: {}, Reason: {}", id, reason);
        try {
            reservationService.cancelReservationByAdmin(id, reason);
            return ResponseEntity.ok(AjaxResult.success("预约已被管理员取消"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AjaxResult.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 从 Spring Security 上下文中获取当前登录用户的ID
     * 
     * @return 用户ID Optional
     */
    private Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            return Optional.of(loginUser.getUser().getId());
        }
        return Optional.empty();
    }
}