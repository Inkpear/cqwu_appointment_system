package com.cqwu.cqwu_appointment_system.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约扩展信息实体
 * 存储参与人员名单和人数等扩展信息
 */
@Data
public class ReservationDetail {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 预约ID，关联 reservations 表
     */
    private Long reservationId;

    /**
     * 参与人员名单（前端传入的字符串，信任机制）
     * 例如："张三,李四,王五" 或 JSON 格式等
     */
    private String participants;

    /**
     * 参与人数（前端传入，信任机制）
     */
    private Integer participantCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
