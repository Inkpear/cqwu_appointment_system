package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Data
    public static class LoginRequest {
        private String userId;
        private boolean authenticated;
    }

    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    /**
     * 模拟登录接口 (未来替换为 CAS ticket 验证)
     */
    @PostMapping("/login")
    public ResponseEntity<AjaxResult> login(@RequestBody LoginRequest loginRequest) {
        log.info("开始处理用户 '{}' 的登录请求...", loginRequest.getUserId());

        if (loginRequest == null || !loginRequest.isAuthenticated()) {
            log.warn("CAS 认证失败或请求参数不正确, UserId: {}", loginRequest.getUserId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, Constants.CAS_AUTH_FAIL));
        }

        try {
            Map<String, String> tokens = authenticationService.login(loginRequest.getUserId());
            return ResponseEntity.ok(AjaxResult.success(Constants.LOGIN_SUCCESS, tokens));
        } catch (AuthenticationException e) {
            log.warn("用户 '{}' 登录认证失败: {}", loginRequest.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AjaxResult.error(HttpStatus.UNAUTHORIZED, Constants.LOGIN_FAIL));
        }
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AjaxResult> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("开始处理 Access Token 刷新请求...");
        try {
            Map<String, String> newTokens = authenticationService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(AjaxResult.success(Constants.REFRESH_SUCCESS, newTokens));
        } catch (RuntimeException e) {
            log.warn("Access Token 刷新失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AjaxResult.error(HttpStatus.UNAUTHORIZED, Constants.TOKEN_INVALID));
        }
    }

    /**
     * 退出登录
     */
    @DeleteMapping("/logout")
    public ResponseEntity<AjaxResult> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String username = loginUser.getUsername();
            log.info("开始处理用户 '{}' 的退出登录请求...", username);
            authenticationService.logout(username);
            return ResponseEntity.ok(AjaxResult.success(Constants.LOGOUT_SUCCESS));
        }

        log.warn("退出登录失败：无法从 SecurityContext 获取用户信息。");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AjaxResult.error(HttpStatus.UNAUTHORIZED, Constants.USER_NOT_LOGIN));
    }
}