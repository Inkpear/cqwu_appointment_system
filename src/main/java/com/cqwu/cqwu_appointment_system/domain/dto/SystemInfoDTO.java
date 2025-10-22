package com.cqwu.cqwu_appointment_system.domain.dto;

import lombok.Data;
import java.util.List;

/**
 * @description 用于封装完整的系统监控信息的数据传输对象 (DTO)。
 * 包含了服务器、CPU、内存和磁盘的详细信息。
 */
@Data
public class SystemInfoDTO {

    private ServerInfo serverInfo;
    private CpuInfo cpuInfo;
    private MemoryInfo memoryInfo;
    private List<DiskStoreInfo> diskInfo;

    /**
     * 用于封装服务器和 JVM 基础信息。
     */
    @Data
    public static class ServerInfo {
        private String serverName;
        private String osName;
        private String osArch;
        private String osVersion;
        private String jvmName;
        private String jvmVersion;
        private String javaHome;
        private String projectDir;
    }

    /**
     * 用于封装 CPU 信息和使用率统计。
     */
    @Data
    public static class CpuInfo {
        private String cpuModel;
        private int physicalCoreCount;
        private int logicalCoreCount;
        private double systemUsage;
        private double userUsage;
        private double ioWait;
        private double idle;
        private double totalUsage;
    }

    /**
     * 用于封装物理内存使用情况统计。
     */
    @Data
    public static class MemoryInfo {
        private double total;
        private double used;
        private double available;
        private double usageRate;
    }

    /**
     * 用于封装单个磁盘分区信息。
     */
    @Data
    public static class DiskStoreInfo {
        private String mountPoint;
        private String fileSystemType;
        private double totalSpace;
        private double usedSpace;
        private double freeSpace;
        private double usageRate;
    }
}