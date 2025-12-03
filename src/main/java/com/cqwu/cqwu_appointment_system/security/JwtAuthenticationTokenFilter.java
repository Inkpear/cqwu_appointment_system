package com.cqwu.cqwu_appointment_system.security;

import com.cqwu.cqwu_appointment_system.common.utils.JwtUtils;
import com.cqwu.cqwu_appointment_system.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserDetailsService userDetailsService;

    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationTokenFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(jwtProperties.getHeader());

        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(TOKEN_PREFIX.length());

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = JwtUtils.parseJwt(token, jwtProperties.getSecret());
                String userId = claims.get("userId", String.class);

                if (userId != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("成功为用户 '{}' 验证 JWT Token，安全上下文已设置。", userId);
                }
            }

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT Token 验证失败: {}", e.getMessage());
            resolver.resolveException(request, response, null, e);
        }
    }
}