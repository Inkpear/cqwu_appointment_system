package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.JwtUtils;
import com.cqwu.cqwu_appointment_system.config.properties.JwtProperties;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.framework.redis.RedisCache;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import com.cqwu.cqwu_appointment_system.service.UserService;

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

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private RedisCache redisCache;

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> login(String userId) {
        log.info("开始处理用户 '{}' 的登录请求...", userId);
        // TODO: 此处应为与学校统一认证系统（如CAS）的集成点。
        // 当前为开发模式，直接加载用户信息。
        final UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
        LoginUser loginUser = (LoginUser) userDetails;
        User user = loginUser.getUser();

        // 创建JWT载荷
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        claims.put("name", user.getName());
        claims.put("userType", user.getUserType());

        // 生成双令牌
        String accessToken = JwtUtils.createAccessToken(claims, jwtProperties);
        String refreshToken = JwtUtils.createRefreshToken(user.getId(), jwtProperties);

        // 将Refresh Token存入Redis，并设置与Token本身相同的过期时间
        String redisKey = Constants.REFRESH_TOKEN_KEY_PREFIX + user.getId();
        redisCache.setCacheObject(redisKey, refreshToken,
                jwtProperties.getRefreshTokenExpireTime().toMillis(), TimeUnit.MILLISECONDS);

        log.info("用户 '{}' 登录成功，已生成并存储 Refresh Token。", userId);

        // 更新最后登录时间
        userService.updateLoginTimeById(userId);

        // 返回双令牌
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        log.info("开始处理 Access Token 刷新请求...");
        Assert.hasText(refreshToken, "Refresh Token 不能为空");

        String userId = null;
        try {
            // 解析 Refresh Token
            Claims claims = JwtUtils.parseJwt(refreshToken, jwtProperties.getSecret());
            userId = claims.get("userId", String.class);
            log.info("从 Refresh Token 中解析出 UserId: '{}'。", userId);

            // 校验 Redis 中存储的 Token 是否与传入的一致
            String redisKey = Constants.REFRESH_TOKEN_KEY_PREFIX + userId;
            String storedToken = redisCache.getCacheObject(redisKey);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.warn("无效的 Refresh Token: Redis 中不存在或不匹配。UserId: {}", userId);
                throw new JwtException("Refresh Token 无效或已过期");
            }

            // 校验通过，加载用户信息，生成新的 Access Token
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
            log.warn("Refresh Token 解析或验证失败: {}, UserId (if available): {}", e.getMessage(), userId);
            // 如果能解析出 userId，则主动删除可能存在的脏数据，防止被盗用的Token反复尝试
            if (userId != null) {
                redisCache.deleteObject(Constants.REFRESH_TOKEN_KEY_PREFIX + userId);
            }
            throw new RuntimeException("无效的 Refresh Token，请重新登录", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(String userId) {
        log.info("开始处理用户 '{}' 的退出登录请求...", userId);
        String redisKey = Constants.REFRESH_TOKEN_KEY_PREFIX + userId;
        boolean deleted = redisCache.deleteObject(redisKey);
        if (deleted) {
            log.info("用户 '{}' 退出登录成功，已删除 Redis 中的 Refresh Token。", userId);
        } else {
            log.warn("用户 '{}' 退出登录时，未在 Redis 中找到对应的 Refresh Token (可能已过期或重复退出)。", userId);
        }
    }
}