package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.dto.SystemInfoDTO;
import com.cqwu.cqwu_appointment_system.service.SystemMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 负责处理系统监控相关的API请求。
 * 提供查询服务器状态、性能指标等信息的接口。
 */
@RestController
@RequestMapping("/api/monitor")
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('ROOT') or hasRole('MONITOR')")
public class SystemMonitorController {

    @Autowired
    private SystemMonitorService systemMonitorService;

    /**
     * [ADMIN] or [ROOT] or [MONITOR] 获取系统综合监控信息。
     * 此端点返回服务器、CPU、内存和磁盘的详细状态。
     *
     * @return 成功时，返回包含 {@link SystemInfoDTO} 的 ResponseEntity 响应体 (HTTP 200)；
     *         失败时，由全局异常处理器统一返回标准的错误响应。
     * @throws InterruptedException 如果在等待采集 CPU 负载数据的过程中线程被中断。
     */
    @GetMapping("/system-info")
    public ResponseEntity<AjaxResult> getSystemInfo() throws InterruptedException {
        SystemInfoDTO systemInfo = systemMonitorService.getSystemInfo();
        return ResponseEntity.ok(AjaxResult.success(systemInfo));
    }
}