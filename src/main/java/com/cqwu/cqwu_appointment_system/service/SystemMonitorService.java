package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.domain.dto.SystemInfoDTO;

/**
 * @description 负责提供系统监控相关信息服务的接口。
 */
public interface SystemMonitorService {

    /**
     * 获取完整的系统信息。
     * 此方法会采集服务器的基本信息、CPU 实时负载、内存使用情况以及所有磁盘分区的状态。
     *
     * @return 包含所有采集到的系统指标的 {@link SystemInfoDTO} 对象。
     * @throws InterruptedException 如果在等待采集 CPU 负载数据的过程中线程被中断。
     */
    SystemInfoDTO getSystemInfo() throws InterruptedException;
}