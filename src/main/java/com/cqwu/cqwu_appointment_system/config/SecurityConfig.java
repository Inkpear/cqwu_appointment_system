package com.cqwu.cqwu_appointment_system.config;

import com.cqwu.cqwu_appointment_system.security.JwtAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用 @PreAuthorize 等方法级安全注解
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    /**
     * 配置安全过滤链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 禁用 CSRF，因为我们使用 JWT，是无状态的
            .csrf(csrf -> csrf.disable())
            // 2. 设置 Session 管理策略为无状态 (STATELESS)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 3. 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 允许匿名访问登录和刷新接口
                .requestMatchers("/api/auth/**").permitAll()
                // (可选) 允许访问 Swagger 或其他静态资源
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 除了上述路径，所有其他请求都需要认证
                .anyRequest().authenticated()
            )
            // 4. 将我们的 JWT 过滤器添加到 Spring Security 的过滤器链中
            // 在 UsernamePasswordAuthenticationFilter 之前执行
            .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 暴露 AuthenticationManager Bean，用于登录认证
     * Spring Security 6.x 的标准做法
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}