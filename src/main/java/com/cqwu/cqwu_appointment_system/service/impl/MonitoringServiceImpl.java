package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.dto.SseNotificationEvent;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringServiceImpl {

    private final NotificationService notificationService;

    /**
     * 监听SSE通知事件
     * 
     * @param event 事件对象
     */
    @Async
    @EventListener
    public void handleSseNotification(SseNotificationEvent event) {
        if (isMonitoringRequired(event)) {
            log.debug("监控服务已捕获到事件: {}, 目标: {}, 正在转发给监控者...",
                    event.getEventName(),
                    event.getOriginalTargetId() != null ? event.getOriginalTargetId() : event.getOriginalTargetRoles());

            Map<String, Object> monitorData = new HashMap<>();
        monitorData.put("originalTargetId", event.getOriginalTargetId());
        monitorData.put("originalTargetRoles", event.getOriginalTargetRoles());
        monitorData.put("eventName", event.getEventName());
        monitorData.put("payload", event.getData());


            notificationService.sendToRole(Constants.UserRoleType.ROLE_MONITOR, "monitoring-event", monitorData);
        }
    }

    /**
     * 判断一个事件是否需要被监控的逻辑
     * 
     * @param event 事件
     * @return true 如果需要监控
     */
    private boolean isMonitoringRequired(SseNotificationEvent event) {
        if (Constants.SseEvent.PING.equals(event.getEventName())) {
            return false;
        }

        List<String> roles = event.getOriginalTargetRoles();
        if (roles != null && roles.size() == 1 && Constants.UserRoleType.ROLE_MONITOR.equals(roles.get(0))) {
            return false;
        }

        return true;    
    }
}