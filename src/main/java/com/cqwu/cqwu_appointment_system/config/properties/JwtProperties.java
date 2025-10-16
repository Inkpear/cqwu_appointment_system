package com.cqwu.cqwu_appointment_system.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 配置属性类
 * 通过 @ConfigurationProperties 注解与 application.properties 中的 jwt 前缀绑定
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Base64 编码的密钥
     */
    private String secret;

    /**
     * Access Token 过期时间
     */
    private Duration accessTokenExpireTime;

    /**
     * Refresh Token 过期时间
     */
    private Duration refreshTokenExpireTime;

    /**
     * Token 在 HTTP Header 中的名称
     */
    private String header;
}