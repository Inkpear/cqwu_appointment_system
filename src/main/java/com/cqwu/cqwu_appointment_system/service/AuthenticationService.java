package com.cqwu.cqwu_appointment_system.service;

import java.util.Map;

/**
 * @description 认证服务接口，定义了登录、刷新令牌和登出的核心操作。
 */
public interface AuthenticationService {

    /**
     * 用户登录。成功后，生成并返回包含 Access Token 和 Refresh Token 的Map。
     * 
     * @param userId 用户ID (学号/工号)，通常来自CAS等认证源。
     * @return 包含 "accessToken" 和 "refreshToken" 的 Map。
     */
    Map<String, String> login(String userId);

    /**
     * 使用 Refresh Token 刷新 Access Token。
     * 
     * @param refreshToken 用户持有的、未过期的 Refresh Token。
     * @return 包含新的 "accessToken" 的 Map。
     * @throws RuntimeException 如果 Refresh Token 无效、过期或已被撤销。
     */
    Map<String, String> refreshToken(String refreshToken);

    /**
     * 用户退出登录。
     * 主要操作是使当前用户的 Refresh Token 失效。
     * 
     * @param userId 要退出登录的用户ID。
     */
    void logout(String userId);
}