package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
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
    public AjaxResult login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest != null && loginRequest.isAuthenticated()) {
            try {
                Map<String, String> tokens = authenticationService.login(loginRequest.getUserId());
                return AjaxResult.success(Constants.LOGIN_SUCCESS, tokens);
            } catch (Exception e) {
                return AjaxResult.error(HttpStatus.UNAUTHORIZED, Constants.LOGIN_FAIL + ": " + e.getMessage());
            }
        }
        return AjaxResult.error(HttpStatus.BAD_REQUEST, Constants.CAS_AUTH_FAIL);
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public AjaxResult refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            Map<String, String> newTokens = authenticationService.refreshToken(request.getRefreshToken());
            return AjaxResult.success(Constants.REFRESH_SUCCESS, newTokens);
        } catch (Exception e) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, Constants.TOKEN_INVALID);
        }
    }

    /**
     * 退出登录
     */
    @DeleteMapping("/logout")
    public AjaxResult logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            authenticationService.logout(loginUser.getUsername());
            return AjaxResult.success(Constants.LOGOUT_SUCCESS);
        }
        
        return AjaxResult.error(HttpStatus.UNAUTHORIZED, Constants.USER_NOT_LOGIN);
    }
}