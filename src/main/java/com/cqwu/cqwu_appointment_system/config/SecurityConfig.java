package com.cqwu.cqwu_appointment_system.config;

import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.security.JwtAuthenticationTokenFilter;
import com.cqwu.cqwu_appointment_system.security.RestAuthenticationEntryPoint;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * @description Spring Security 的核心配置类。
 *              负责定义安全策略、过滤器链、认证和授权规则。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Autowired
    private RestAuthenticationEntryPoint unauthorizedHandler;

    /**
     * 配置安全过滤器链，定义所有HTTP请求的安全处理规则。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 配置CORS（跨域资源共享）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 禁用CSRF保护，因为我们使用无状态的JWT，不依赖Cookie
                .csrf(csrf -> csrf.disable())

                // 设置Session管理策略为无状态（STATELESS），不创建HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置HTTP请求的授权规则
                .authorizeHttpRequests(auth -> auth

                        // 放行容器的异步与错误分派，避免 SSE 断开后异步再分派触发二次鉴权
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()

                        // 允许所有OPTIONS预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 公开访问认证相关的所有API端点
                        .requestMatchers("/api/auth/**").permitAll()

                        // 除上述之外的所有其他请求都需要进行身份认证
                        .anyRequest().authenticated())

                // 异常处理：若响应已提交（如 SSE 流），不要再尝试写 403，避免无意义报错
                .exceptionHandling(handler -> handler
                    // 配置认证入口点，处理 401 Unauthorized (未认证或认证失败)
                    .authenticationEntryPoint(unauthorizedHandler)
                    // 配置访问拒绝处理器，处理 403 Forbidden (已认证但无权限)
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        if (response.isCommitted()) {
                            return;
                        }
                        response.setStatus(HttpStatus.FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，无法访问此资源\"}");
                    })
                )

                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 定义CORS配置源。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 【注意】生产环境应替换为具体的前端域名
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 从AuthenticationConfiguration中获取AuthenticationManager的Bean实例。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}