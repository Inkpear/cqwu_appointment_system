package com.cqwu.cqwu_appointment_system.domain.dto;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class SseNotificationEvent extends ApplicationEvent {

    private final String eventName;
    private final Object data;
    // 原始目标的标识，可能是单个用户ID，也可能是"ALL_USERS"
    private final String originalTargetId;
    // 原始目标的角色列表
    private final List<String> originalTargetRoles;

    /**
     * 构造函数用于单用户或广播
     */
    public SseNotificationEvent(Object source, String eventName, Object data, String originalTargetId) {
        super(source);
        this.eventName = eventName;
        this.data = data;
        this.originalTargetId = originalTargetId;
        this.originalTargetRoles = null;
    }

    /**
     * 构造函数用于按角色发送
     */
    public SseNotificationEvent(Object source, String eventName, Object data, List<String> originalTargetRoles) {
        super(source);
        this.eventName = eventName;
        this.data = data;
        this.originalTargetId = null;
        this.originalTargetRoles = originalTargetRoles;
    }
}