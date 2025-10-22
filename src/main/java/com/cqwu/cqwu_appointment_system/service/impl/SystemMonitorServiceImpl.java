package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.domain.dto.SystemInfoDTO;
import com.cqwu.cqwu_appointment_system.service.SystemMonitorService;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OperatingSystem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SystemMonitorService 的实现类，使用 Oshi 库来获取系统底层硬件和软件信息。
 */
@Service
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private static final int OSHI_WAIT_SECOND = 1;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final double GIB_DIVISOR = 1024 * 1024 * 1024.0;

    /**
     * {@inheritDoc}
     */
    @Override
    public SystemInfoDTO getSystemInfo() throws InterruptedException {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        OperatingSystem os = systemInfo.getOperatingSystem();

        SystemInfoDTO systemInfoDTO = new SystemInfoDTO();
        systemInfoDTO.setServerInfo(createServerInfo(os));
        systemInfoDTO.setCpuInfo(createCpuInfo(hardware.getProcessor()));
        systemInfoDTO.setMemoryInfo(createMemoryInfo(hardware.getMemory()));
        systemInfoDTO.setDiskInfo(createDiskInfo(os.getFileSystem()));

        return systemInfoDTO;
    }

    /**
     * 采集服务器和 JVM 的基本信息。
     * 
     * @param os Oshi 的操作系统对象
     * @return 封装好的服务器信息 DTO
     */
    private SystemInfoDTO.ServerInfo createServerInfo(OperatingSystem os) {
        SystemInfoDTO.ServerInfo serverInfo = new SystemInfoDTO.ServerInfo();
        Properties props = System.getProperties();
        serverInfo.setOsName(os.toString());
        serverInfo.setOsArch(props.getProperty("os.arch"));
        serverInfo.setOsVersion(props.getProperty("os.version"));
        serverInfo.setJvmName(props.getProperty("java.vm.name"));
        serverInfo.setJvmVersion(props.getProperty("java.version"));
        serverInfo.setJavaHome(props.getProperty("java.home"));
        serverInfo.setProjectDir(props.getProperty("user.dir"));

        try {
            serverInfo.setServerName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            serverInfo.setServerName("Unknown");
        }
        return serverInfo;
    }

    /**
     * 采集 CPU 的详细信息和使用率。
     * 
     * @param processor Oshi 的中央处理器对象
     * @return 封装好的 CPU 信息 DTO
     * @throws InterruptedException 如果线程在等待时被中断
     */
    private SystemInfoDTO.CpuInfo createCpuInfo(CentralProcessor processor) throws InterruptedException {
        SystemInfoDTO.CpuInfo cpuInfo = new SystemInfoDTO.CpuInfo();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        TimeUnit.SECONDS.sleep(OSHI_WAIT_SECOND);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
                - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
                - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()]
                - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;

        cpuInfo.setCpuModel(processor.getProcessorIdentifier().getName());
        cpuInfo.setPhysicalCoreCount(processor.getPhysicalProcessorCount());
        cpuInfo.setLogicalCoreCount(processor.getLogicalProcessorCount());
        cpuInfo.setSystemUsage(formatDouble(100d * cSys / totalCpu));
        cpuInfo.setUserUsage(formatDouble(100d * user / totalCpu));
        cpuInfo.setIoWait(formatDouble(100d * iowait / totalCpu));
        cpuInfo.setIdle(formatDouble(100d * idle / totalCpu));
        cpuInfo.setTotalUsage(formatDouble(100d * (totalCpu - idle) / totalCpu));

        return cpuInfo;
    }

    /**
     * 采集物理内存的使用情况。
     * 
     * @param memory Oshi 的全局内存对象
     * @return 封装好的内存信息 DTO
     */
    private SystemInfoDTO.MemoryInfo createMemoryInfo(GlobalMemory memory) {
        SystemInfoDTO.MemoryInfo memoryInfo = new SystemInfoDTO.MemoryInfo();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        memoryInfo.setTotal(convertBytesToGib(total));
        memoryInfo.setAvailable(convertBytesToGib(available));
        memoryInfo.setUsed(convertBytesToGib(used));
        memoryInfo.setUsageRate((total > 0) ? formatDouble(100d * used / total) : 0);

        return memoryInfo;
    }

    /**
     * 采集所有磁盘分区的使用情况。
     * 
     * @param fileSystem Oshi 的文件系统对象
     * @return 包含所有磁盘分区信息的 DTO 列表
     */
    private List<SystemInfoDTO.DiskStoreInfo> createDiskInfo(FileSystem fileSystem) {
        return fileSystem.getFileStores().stream().map(fs -> {
            long total = fs.getTotalSpace();
            long free = fs.getUsableSpace();
            long used = total - free;

            SystemInfoDTO.DiskStoreInfo diskStoreInfo = new SystemInfoDTO.DiskStoreInfo();
            diskStoreInfo.setMountPoint(fs.getMount());
            diskStoreInfo.setFileSystemType(fs.getType());
            diskStoreInfo.setTotalSpace(convertBytesToGib(total));
            diskStoreInfo.setFreeSpace(convertBytesToGib(free));
            diskStoreInfo.setUsedSpace(convertBytesToGib(used));
            diskStoreInfo.setUsageRate((total > 0) ? formatDouble(100d * used / total) : 0);

            return diskStoreInfo;
        }).collect(Collectors.toList());
    }

    /**
     * 将 double 值格式化为两位小数。
     * 
     * @param value 待格式化的值
     * @return 格式化后的值
     */
    private double formatDouble(double value) {
        return Double.parseDouble(DECIMAL_FORMAT.format(value));
    }

    /**
     * 将字节数转换为 GiB。
     * 
     * @param bytes 字节大小
     * @return GiB 大小
     */
    private double convertBytesToGib(long bytes) {
        return formatDouble(bytes / GIB_DIVISOR);
    }
}