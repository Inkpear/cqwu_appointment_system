package com.cqwu.cqwu_appointment_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 配置属性
 * 从 application.properties 读取 MinIO 相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO 服务端点地址
     * 例如: http://localhost:9000
     */
    private String endpoint;

    /**
     * MinIO 对外可访问地址（用于返回给前端的预签名URL域名）
     * 例如: http://your-vm-ip:1900
     */
    private String publicEndpoint;

    /**
     * MinIO 访问密钥（Access Key）
     */
    private String accessKey;

    /**
     * MinIO 密钥（Secret Key）
     */
    private String secretKey;

    /**
     * 默认存储桶名称
     */
    private String bucketName;
}
