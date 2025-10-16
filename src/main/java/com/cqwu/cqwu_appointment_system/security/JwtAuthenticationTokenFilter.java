package com.cqwu.cqwu_appointment_system.security;

import com.cqwu.cqwu_appointment_system.common.utils.JwtUtils;
import com.cqwu.cqwu_appointment_system.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {     

        String authHeader = request.getHeader(jwtProperties.getHeader());
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = JwtUtils.parseJwt(token, jwtProperties.getSecret());
                String userId = claims.get("userId", String.class);

                // 如果 token 有效，且当前安全上下文中没有认证信息
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                    // 创建一个认证对象，封装用户详情和权限
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 将认证信息放入 Spring Security 的上下文中
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("Authenticated user: {}, setting security context", userId);
                }
            } catch (Exception e) {
                log.warn("JWT Token validation failed: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}