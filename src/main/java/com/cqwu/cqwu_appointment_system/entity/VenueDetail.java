package com.cqwu.cqwu_appointment_system.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场所扩展信息实体
 * 存储场所描述等扩展信息
 */
@Data
public class VenueDetail {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 场所ID，关联 venue 表
     */
    private Long venueId;

    /**
     * 场所描述（支持Markdown格式）
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
