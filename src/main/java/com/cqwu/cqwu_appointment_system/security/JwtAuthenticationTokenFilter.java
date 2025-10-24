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

/**
 * @description JWT认证过滤器。
 *              在每个HTTP请求中，检查是否存在有效的JWT Access Token。
 *              如果有效，则解析Token，加载用户信息，并将其设置到Spring Security的上下文中。
 */
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

        // 如果请求头中没有Authorization信息或格式不正确，则直接放行
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(TOKEN_PREFIX.length());

        // 如果安全上下文中已有认证信息，则直接放行
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 解析JWT Token
            Claims claims = JwtUtils.parseJwt(token, jwtProperties.getSecret());
            String userId = claims.get("userId", String.class);

            if (userId != null) {
                // 加载用户详情并构建认证令牌
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证信息设置到SecurityContext中，表示当前用户已认证
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.debug("成功为用户 '{}' 验证 JWT Token，安全上下文已设置。", userId);
            }
        } catch (ExpiredJwtException e) {
            log.debug("JWT Token 已过期: {}", e.getMessage());
        } catch (SignatureException e) {
            log.debug("无效的 JWT 签名: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("无效的 JWT Token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("不支持的 JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT Claims 字符串为空: {}", e.getMessage());
        }

        // 无论Token是否有效，都继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}