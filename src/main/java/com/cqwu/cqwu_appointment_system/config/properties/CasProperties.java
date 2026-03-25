package com.cqwu.cqwu_appointment_system.config.properties;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cas")
public class CasProperties {

    private boolean enabled = false;

    /** CAS 服务前缀，如 https://ids.example.edu/authserver */
    private String serverUrlPrefix;

    /** CAS 登录地址，如 https://ids.example.edu/authserver/login */
    private String serverLoginUrl;

    /** CAS 退出地址，如 https://ids.example.edu/authserver/logout */
    private String serverLogoutUrl;

    /** 后端接收 CAS ticket 的回调地址 */
    private String clientServiceUrl;

    /** 认证完成后跳转的前端地址（通常是 /login/callback） */
    private String frontendRedirectUrl;

    /** Redis 中 state 的有效期 */
    private Duration stateTtl = Duration.ofMinutes(5);

    /** Redis 中一次性 code 的有效期 */
    private Duration codeTtl = Duration.ofMinutes(2);

    /** CAS 请求编码 */
    private String encoding = "UTF-8";
}
