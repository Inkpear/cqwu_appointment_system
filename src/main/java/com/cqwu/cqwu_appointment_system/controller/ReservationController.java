package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.converter.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.service.IReservationService;
//import com.cqwu.cqwu_appointment_system.vo.AjaxResult;
//import com.cqwu.cqwu_appointment_system.common.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Lyuih
 * @description 预约服务的控制器，提供RESTful API接口
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private IReservationService reservationService;

    /**
     * 创建一个新的预约
     * @param reservation 预约信息，从请求体中获取
     * @return 操作结果
     */
    @PostMapping
    public AjaxResult createReservation(@RequestBody Reservation reservation) {
        // 实际项目中，userId 应该从安全上下文（如 Spring Security, Sa-Token）中获取，而不是由前端传递
        // 例如: Long userId = StpUtil.getLoginIdAsLong(); reservation.setUserId(userId);
        Reservation created = reservationService.createReservation(reservation);
        return AjaxResult.success("预约创建成功", created);
    }

    /**
     * 根据ID查询预约详情
     * @param id 预约ID
     * @return 预约详情
     */
    @GetMapping("/{id}")
    public AjaxResult getReservationById(@PathVariable Long id) {
        Reservation reservation = reservationService.findReservationById(id);
        return reservation != null ? AjaxResult.success(reservation) : AjaxResult.error("预约不存在");
    }

    /**
     * 动态条件搜索预约
     * @param venueId 场地ID (可选)
     * @param userId 用户ID (可选)
     * @param status 状态 (可选)
     * @param date 日期 (可选)
     * @param pageable 分页参数
     * @return 预约分页列表
     */
    @GetMapping
    public AjaxResult searchReservations(
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Pageable pageable) {
        Page<Reservation> reservations = reservationService.searchReservations(venueId, userId, status, date, pageable);

        return AjaxResult.success(PageDTO.fromPage(reservations));
    }

    /**
     * 用户取消自己的预约
     * @param id 预约ID
     * @return 操作结果
     */
    @PutMapping("/{id}/cancel")
    public AjaxResult cancelReservationByUser(@PathVariable Long id) {
        // 同样，userId 应该从安全上下文中获取
        Long currentUserId = 1L; // 假设当前登录用户ID为1，实际应替换为真实逻辑
        reservationService.cancelReservationByUser(id, currentUserId);
        return AjaxResult.success("预约已取消");
    }

    /**
     * 管理员批准预约
     * @param id 预约ID
     * @return 操作结果
     */
    @PutMapping("/{id}/approve")
    // @PreAuthorize("hasRole('ADMIN')") // 建议使用Spring Security等进行权限控制
    public AjaxResult approveReservation(@PathVariable Long id) {
        reservationService.approveReservation(id);
        return AjaxResult.success("预约已批准");
    }

    /**
     * 管理员拒绝预约
     * @param id 预约ID
     * @param reason 拒绝原因
     * @return 操作结果
     */
    @PutMapping("/{id}/reject")
    // @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult rejectReservation(@PathVariable Long id, @RequestParam(required = false) String reason) {
        reservationService.rejectReservation(id, reason);
        return AjaxResult.success("预约已拒绝");
    }

    /**
     * 管理员取消预约
     * @param id 预约ID
     * @param reason 取消原因
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult cancelReservationByAdmin(@PathVariable Long id, @RequestParam String reason) {
        reservationService.cancelReservationByAdmin(id, reason);
        return AjaxResult.success("预约已被管理员取消");
    }
}
