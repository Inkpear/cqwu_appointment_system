package com.cqwu.cqwu_appointment_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件上传配置属性
 * 从 application.properties 读取文件上传相关的配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * 文件大小限制（字节）
     * 默认: 20MB
     */
    private Long maxFileSize = 20 * 1024 * 1024L;

    /**
     * 允许的文件扩展名列表
     */
    private List<String> allowedExtensions;

    /**
     * 预签名URL有效期（分钟）
     * 默认: 5分钟
     */
    private Integer presignedUrlExpiryMinutes = 5;

    /**
     * 单个预约最大附件数量
     * 默认: 10个
     */
    private Integer maxAttachmentsPerReservation = 10;
}
