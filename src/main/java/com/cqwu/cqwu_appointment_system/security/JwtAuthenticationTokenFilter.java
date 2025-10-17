package com.cqwu.cqwu_appointment_system.security;

import com.cqwu.cqwu_appointment_system.common.utils.JwtUtils;
import com.cqwu.cqwu_appointment_system.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserDetailsService userDetailsService;

    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(jwtProperties.getHeader());

        // 1. 如果没有 Token，直接放行
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(TOKEN_PREFIX.length());

        // 2. 如果上下文中已有认证信息，则不进行处理
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = JwtUtils.parseJwt(token, jwtProperties.getSecret());
            String userId = claims.get("userId", String.class);

            if (userId != null) {
                // 3. 加载用户详情并创建认证令牌
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 4. 将认证信息设置到 SecurityContext 中
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.info("成功为用户 '{}' 验证 JWT Token，安全上下文已设置。", userId);
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("无效的 JWT 签名: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("无效的 JWT Token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT Claims 字符串为空: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}