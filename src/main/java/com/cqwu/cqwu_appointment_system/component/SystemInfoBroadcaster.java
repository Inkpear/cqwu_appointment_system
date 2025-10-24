package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.dto.SystemInfoDTO;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import com.cqwu.cqwu_appointment_system.service.SystemMonitorService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SystemInfoBroadcaster {

    @Autowired
    private SystemMonitorService systemMonitorService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 每隔 5 秒获取一次系统信息，并向所有监控角色广播。
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastSystemInfo() {
        try {
            SystemInfoDTO systemInfo = systemMonitorService.getSystemInfo();
            notificationService.sendToRole(
                Constants.UserRoleType.ROLE_MONITOR, 
                "system-update", 
                systemInfo
            );
            
        } catch (Exception e) {
            log.error("【广播任务】获取或广播系统信息时发生错误", e);
        }
    }
}