package com.cqwu.cqwu_appointment_system.service;

import java.util.Map;

public interface AuthenticationService {

    /**
     * 用户登录，成功后返回双令牌
     * @param userId 用户ID (学号/工号)
     * @return 包含 accessToken 和 refreshToken 的 Map
     */
    Map<String, String> login(String userId);

    /**
     * 使用 Refresh Token 刷新 Access Token
     * @param refreshToken 用户持有的 Refresh Token
     * @return 包含新 accessToken 的 Map
     */
    Map<String, String> refreshToken(String refreshToken);

    /**
     * 用户退出登录
     * @param userId 要退出登录的用户ID
     */
    void logout(String userId);
}