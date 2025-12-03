package com.cqwu.cqwu_appointment_system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 场所统计信息 DTO
 * 用于展示场所的热门程度和使用情况
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueStatisticsDto {
    
    /**
     * 场所 ID
     */
    private Long venueId;
    
    /**
     * 场所名称
     */
    private String venueName;
    
    /**
     * 总预约次数（所有状态）
     */
    private Long totalReservations;
    
    /**
     * 成功预约次数（已批准 + 已完成）
     */
    private Long successfulReservations;
    
    /**
     * 待审批预约次数
     */
    private Long pendingReservations;
    
    /**
     * 进行中的预约次数
     */
    private Long ongoingReservations;
    
    /**
     * 已完成的预约次数
     */
    private Long completedReservations;
    
    /**
     * 预约总人次（所有预约的参与人数总和）
     */
    private Long totalParticipants;
    
    /**
     * 独立预约用户数（去重）
     */
    private Long uniqueUsers;
    
    /**
     * 最近 30 天预约次数
     */
    private Long recentReservations;
    
    /**
     * 预约通过率（批准/申请）
     */
    private Double approvalRate;
    
    /**
     * 平均每次预约时长（分钟）
     */
    private Double averageDurationMinutes;
    
    /**
     * 最后一次预约时间
     */
    private LocalDateTime lastReservationTime;
    
    /**
     * 热门度评分（0-100，综合计算）
     */
    private Integer popularityScore;
    
    /**
     * 场所使用率（已预约时长 / 总可用时长，百分比）
     * 仅计算最近 30 天
     */
    private Double utilizationRate;
}
