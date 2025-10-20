package com.cqwu.cqwu_appointment_system.config;

import com.cqwu.cqwu_appointment_system.security.JwtAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
@EnableMethodSecurity // 启用方法级别的安全注解，如 @PreAuthorize
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

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
                // 允许所有OPTIONS预检请求
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 公开访问认证相关的所有API端点
                .requestMatchers("/api/auth/**").permitAll()
                
                // 除上述之外的所有其他请求都需要进行身份认证
                .anyRequest().authenticated()
            )
            
            // 将自定义的JWT认证过滤器添加到Spring Security的过滤器链中
            // 它会在处理用户名密码认证的过滤器之前执行
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}