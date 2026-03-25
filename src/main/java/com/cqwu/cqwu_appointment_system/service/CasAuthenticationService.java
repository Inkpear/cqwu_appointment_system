package com.cqwu.cqwu_appointment_system.service;

import java.util.Map;

public interface CasAuthenticationService {
    String buildCasLoginRedirectUrl(String frontendRedirect);

    String handleCasCallbackAndIssueCode(String ticket, String state);

    Map<String, String> exchangeCodeForTokens(String code);

    String buildCasLogoutRedirectUrl(String postLogoutRedirect);
}
