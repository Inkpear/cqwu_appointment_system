package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.config.properties.CasProperties;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import com.cqwu.cqwu_appointment_system.service.CasAuthenticationService;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @description 负责处理用户认证相关的所有API请求，包括登录、刷新令牌和退出登录。
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CasAuthenticationService casAuthenticationService;

    @Autowired
    private CasProperties casProperties;

    /**
     * @description 登录请求的数据传输对象。
     */
    @Data
    public static class LoginRequest {

        private String userId;
        private boolean authenticated;
    }

    /**
     * @description 刷新令牌请求的数据传输对象。
     */
    @Data
    public static class RefreshTokenRequest {

        private String refreshToken;
    }

    /**
     * @description CAS code 交换令牌请求体。
     */
    @Data
    public static class CasCodeExchangeRequest {

        private String code;
    }

    /**
     * 用户登录接口。
     * 该接口保留用于测试环境手工指定用户ID登录。
     * 生产环境请优先使用 CAS 登录流程。
     */
    @PostMapping("/login")
    public ResponseEntity<AjaxResult> login(
            @RequestBody LoginRequest loginRequest) {
        log.info("开始处理用户 '{}' 的登录请求...", loginRequest.getUserId());

        if (loginRequest == null || !loginRequest.isAuthenticated()) {
            log.warn(
                    "CAS 认证失败或请求参数不正确, UserId: {}",
                    loginRequest.getUserId());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    AjaxResult.error(
                            HttpStatus.BAD_REQUEST,
                            Constants.CAS_AUTH_FAIL));
        }

        Map<String, String> tokens = authenticationService.login(
                loginRequest.getUserId());
        return ResponseEntity.ok(
                AjaxResult.success(Constants.LOGIN_SUCCESS, tokens));
    }

    /**
     * 触发 CAS 登录重定向。
     */
    @GetMapping("/cas/login")
    public ResponseEntity<Void> casLogin(
            @RequestParam(value = "redirect", required = false) String redirect) {
        String target = casAuthenticationService.buildCasLoginRedirectUrl(redirect);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, Objects.requireNonNull(target))
                .build();
    }

    /**
     * 接收 CAS 回调，校验 ticket 后签发一次性 code，再跳转回前端。
     */
    @GetMapping("/cas/callback")
    public ResponseEntity<Void> casCallback(
            @RequestParam("ticket") String ticket,
            @RequestParam(value = "state", required = false) String state) {
        String redirectUrl;
        try {
            redirectUrl = casAuthenticationService.handleCasCallbackAndIssueCode(ticket, state);
        } catch (Exception ex) {
            log.error("CAS 回调处理失败", ex);
            redirectUrl = buildCasErrorRedirect(resolveCasCallbackErrorMessage(ex));
        }

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, Objects.requireNonNull(redirectUrl))
                .build();
    }

    private String buildCasErrorRedirect(String message) {
        String frontendRedirect = casProperties.getFrontendRedirectUrl();
        if (frontendRedirect == null || frontendRedirect.isBlank()) {
            frontendRedirect = "/login/callback";
        }

        return UriComponentsBuilder
                .fromUriString(frontendRedirect)
                .queryParam("error", "cas_callback_failed")
                .queryParam("message", message)
                .build()
                .encode()
                .toUriString();
    }

    private String resolveCasCallbackErrorMessage(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof UnknownHostException) {
                return "统一认证成功，但系统数据库主机不可达，请联系管理员检查数据库配置";
            }

            String message = current.getMessage();
            if (message != null &&
                    (message.contains("Communications link failure") ||
                            message.contains("CannotGetJdbcConnectionException") ||
                            message.contains("Unable to acquire JDBC Connection"))) {
                return "统一认证成功，但系统数据库连接失败，请稍后重试或联系管理员";
            }

            current = current.getCause();
        }

        return "统一认证回调失败，请重试或联系管理员";
    }

    /**
     * 前端使用一次性 code 换取系统 accessToken/refreshToken。
     */
    @PostMapping("/cas/exchange")
    public ResponseEntity<AjaxResult> casExchange(
            @RequestBody CasCodeExchangeRequest request) {
        Map<String, String> tokens = casAuthenticationService.exchangeCodeForTokens(request.getCode());
        return ResponseEntity.ok(AjaxResult.success(Constants.LOGIN_SUCCESS, tokens));
    }

    /**
     * 使用 Refresh Token 刷新 Access Token。
     *
     * @param request 包含 refreshToken 的请求体。
     * @return 包含新的 accessToken 的 ResponseEntity。
     */
    @PostMapping("/refresh")
    public ResponseEntity<AjaxResult> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        log.info("开始处理 Access Token 刷新请求...");

        Map<String, String> newTokens = authenticationService.refreshToken(
                request.getRefreshToken());
        return ResponseEntity.ok(
                AjaxResult.success(Constants.REFRESH_SUCCESS, newTokens));
    }

    /**
     * 用户退出登录。
     *
     * @return 操作成功的 ResponseEntity。
     */
    @DeleteMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> logout(
            @AuthenticationPrincipal LoginUser loginUser) {
        String username = loginUser.getUsername();
        log.info("开始处理用户 '{}' 的退出登录请求...", username);

        authenticationService.logout(username);

        return ResponseEntity.ok(AjaxResult.success(Constants.LOGOUT_SUCCESS));
    }

    /**
     * 获取 CAS 退出地址，用于前端执行统一退出。
     */
    @GetMapping("/cas/logout-url")
    public ResponseEntity<AjaxResult> casLogoutUrl(
            @RequestParam(value = "redirect", required = false) String redirect) {
        String url = casAuthenticationService.buildCasLogoutRedirectUrl(redirect);
        return ResponseEntity.ok(AjaxResult.success("获取 CAS 退出地址成功", Map.of("url", url)));
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
            @AuthenticationPrincipal LoginUser loginUser) {
        log.debug("开始获取当前登录用户信息...");
        User user = loginUser.getUser();
        log.debug("成功获取到用户 '{}' 的信息", user.getId());

        return ResponseEntity.ok(AjaxResult.success("获取用户信息成功", user));
    }
}
