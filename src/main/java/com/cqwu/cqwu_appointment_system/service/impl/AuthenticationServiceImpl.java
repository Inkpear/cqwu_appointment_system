package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.JwtUtils;
import com.cqwu.cqwu_appointment_system.config.properties.JwtProperties;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.framework.redis.RedisCache;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    // 直接注入 UserDetailsService，绕过 AuthenticationManager 的密码校验
    @Autowired
    private UserDetailsService userDetailsService;

    // @Autowired
    // private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedisCache redisCache;

    @Override
    public Map<String, String> login(String userId) {
        // 1. 直接通过 UserDetailsService 加载用户，这会触发 loginOrSyncUser 逻辑
        final UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
        LoginUser loginUser = (LoginUser) userDetails;
        User user = loginUser.getUser();

        // 2. 准备 Access Token 的 claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());

        // 3. 生成双令牌
        String accessToken = JwtUtils.createAccessToken(claims, jwtProperties);
        String refreshToken = JwtUtils.createRefreshToken(user.getId(), jwtProperties);

        // 4. 存储 Refresh Token 到 Redis
        String redisKey = Constants.REFRESH_TOKEN_KEY_PREFIX + user.getId();
        redisCache.setCacheObject(redisKey, refreshToken,
                jwtProperties.getRefreshTokenExpireTime().toMillis(), TimeUnit.MILLISECONDS);
        
        log.info("用户 '{}' 登录成功，已生成并存储 Refresh Token。", userId);

        // 5. 准备返回给前端的数据
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        Assert.hasText(refreshToken, "Refresh Token 不能为空");

        String userId = null;
        try {
            // 1. 解析 Refresh Token，验证其签名和基础有效期
            Claims claims = JwtUtils.parseJwt(refreshToken, jwtProperties.getSecret());
            userId = claims.get("userId", String.class);

            // 2. 从 Redis 中获取存储的 Refresh Token
            String redisKey = Constants.REFRESH_TOKEN_KEY_PREFIX + userId;
            String storedToken = redisCache.getCacheObject(redisKey);

            // 3. 对比两个 Token 是否完全一致
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.warn("无效的 Refresh Token: Redis 中不存在或不匹配. UserId: {}", userId);
                throw new JwtException("Refresh Token 无效或已过期");
            }

            // 4. 验证通过，重新加载用户信息以获取最新的角色
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            User user = ((LoginUser) userDetails).getUser();
            
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("userId", user.getId());
            newClaims.put("role", user.getRole());

            String newAccessToken = JwtUtils.createAccessToken(newClaims, jwtProperties);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);

            log.info("用户 '{}' 的 Access Token 刷新成功。", userId);
            return tokens;

        } catch (JwtException e) {
            log.warn("Refresh Token 解析失败: {}, UserId (if available): {}", e.getMessage(), userId);
            if (userId != null) {
                redisCache.deleteObject(Constants.REFRESH_TOKEN_KEY_PREFIX + userId);
            }
            throw new RuntimeException("无效的 Refresh Token，请重新登录", e);
        }
    }

    @Override
    public void logout(String userId) {
        String redisKey = Constants.REFRESH_TOKEN_KEY_PREFIX + userId;
        boolean deleted = redisCache.deleteObject(redisKey);
        if (deleted) {
            log.info("用户 '{}' 退出登录成功，已删除 Refresh Token。", userId);
        } else {
            log.warn("用户 '{}' 退出登录时，未在 Redis 中找到对应的 Refresh Token。", userId);
        }
    }
}