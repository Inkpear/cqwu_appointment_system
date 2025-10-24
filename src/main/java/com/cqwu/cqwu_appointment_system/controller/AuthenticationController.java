package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @description 负责处理用户认证相关的所有API请求，包括登录、刷新令牌和退出登录。
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * @description 登录请求的数据传输对象。
     */
    @Data
    public static class LoginRequest {

        private String userId;
        private boolean authenticated; // 模拟CAS认证结果
    }

    /**
     * @description 刷新令牌请求的数据传输对象。
     */
    @Data
    public static class RefreshTokenRequest {

        private String refreshToken;
    }

    /**
     * 用户登录接口。
     * 在生产环境中，此接口应与学校的统一认证中心（CAS）集成。
     * 当前实现模拟了CAS认证成功后的流程。
     *
     * @param loginRequest 包含用户ID和模拟认证结果的请求体。
     * @return 包含 accessToken 和 refreshToken 的 ResponseEntity。
     */
    @PostMapping("/login")
    public ResponseEntity<AjaxResult> login(
        @RequestBody LoginRequest loginRequest
    ) {
        log.info("开始处理用户 '{}' 的登录请求...", loginRequest.getUserId());

        if (loginRequest == null || !loginRequest.isAuthenticated()) {
            log.warn(
                "CAS 认证失败或请求参数不正确, UserId: {}",
                loginRequest.getUserId()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                AjaxResult.error(
                    HttpStatus.BAD_REQUEST,
                    Constants.CAS_AUTH_FAIL
                )
            );
        }

        Map<String, String> tokens = authenticationService.login(
            loginRequest.getUserId()
        );
        return ResponseEntity.ok(
            AjaxResult.success(Constants.LOGIN_SUCCESS, tokens)
        );
    }

    /**
     * 使用 Refresh Token 刷新 Access Token。
     *
     * @param request 包含 refreshToken 的请求体。
     * @return 包含新的 accessToken 的 ResponseEntity。
     */
    @PostMapping("/refresh")
    public ResponseEntity<AjaxResult> refreshToken(
        @RequestBody RefreshTokenRequest request
    ) {
        log.info("开始处理 Access Token 刷新请求...");

        Map<String, String> newTokens = authenticationService.refreshToken(
            request.getRefreshToken()
        );
        return ResponseEntity.ok(
            AjaxResult.success(Constants.REFRESH_SUCCESS, newTokens)
        );
    }

    /**
     * 用户退出登录。
     *
     * @param auth Spring Security 自动注入的认证信息。
     * @return 操作成功的 ResponseEntity。
     */
    @DeleteMapping("/logout")
    @PreAuthorize("isAuthenticated()") // 确保只有已认证的用户才能调用登出
    public ResponseEntity<AjaxResult> logout(
        @AuthenticationPrincipal LoginUser loginUser
    ) {
        String username = loginUser.getUsername();
        log.info("开始处理用户 '{}' 的退出登录请求...", username);

        authenticationService.logout(username);

        return ResponseEntity.ok(AjaxResult.success(Constants.LOGOUT_SUCCESS));
    }

    /**
     * 获取当前登录用户的详细信息。
     *
     * @param auth Spring Security 自动注入的认证信息。
     * @return 包含当前用户信息的 ResponseEntity。
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getCurrentUserInfo(
        @AuthenticationPrincipal LoginUser loginUser
    ) {
        log.debug("开始获取当前登录用户信息...");
        User user = loginUser.getUser();
        log.debug("成功获取到用户 '{}' 的信息", user.getId());

        return ResponseEntity.ok(AjaxResult.success("获取用户信息成功", user));
    }
}
