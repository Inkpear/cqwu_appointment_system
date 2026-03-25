package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.config.properties.CasProperties;
import com.cqwu.cqwu_appointment_system.domain.CasUserPrincipal;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.framework.redis.RedisCache;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import com.cqwu.cqwu_appointment_system.service.CasAuthenticationService;
import com.cqwu.cqwu_appointment_system.service.CasClientService;
import com.cqwu.cqwu_appointment_system.service.UserService;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class CasAuthenticationServiceImpl implements CasAuthenticationService {

    private static final String CAS_CODE_KEY_PREFIX = "cas:code:";
    private static final String CAS_STATE_KEY_PREFIX = "cas:state:";

    @Autowired
    private CasProperties casProperties;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private CasClientService casClientService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public String buildCasLoginRedirectUrl(String frontendRedirect) {
        ensureCasEnabled();

        String redirectToUse = resolveAndValidateFrontendRedirect(frontendRedirect);
        String state = UUID.randomUUID().toString().replace("-", "");

        redisCache.setCacheObject(
                CAS_STATE_KEY_PREFIX + state,
                redirectToUse,
                casProperties.getStateTtl().toMillis(),
                TimeUnit.MILLISECONDS);

        String callbackServiceUrl = buildCallbackServiceUrl();

        return UriComponentsBuilder
                .fromUriString(Objects.requireNonNull(casProperties.getServerLoginUrl()))
                .queryParam("service", callbackServiceUrl)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public String handleCasCallbackAndIssueCode(String ticket, String state) {
        ensureCasEnabled();
        Assert.hasText(ticket, "CAS ticket 不能为空");

        String frontendRedirect = consumeFrontendRedirectByState(state);

        String callbackServiceUrl = buildCallbackServiceUrl();
        CasUserPrincipal principal = casClientService.validateServiceTicket(ticket, callbackServiceUrl);
        // log.info("[CAS-DEBUG] validated userId={}, attributes={}", principal.getUserId(), principal.getAttributes());

        User user = userService.loginOrSyncUser(principal.getUserId());
        syncBasicProfile(user, principal.getAttributes());

        String code = UUID.randomUUID().toString().replace("-", "");
        redisCache.setCacheObject(
                CAS_CODE_KEY_PREFIX + code,
                user.getId(),
                casProperties.getCodeTtl().toMillis(),
                TimeUnit.MILLISECONDS);

        return UriComponentsBuilder
                .fromUriString(frontendRedirect)
                .queryParam("code", code)
                .build()
                .encode()
                .toUriString();
    }

    private String consumeFrontendRedirectByState(String state) {
        String fallback = casProperties.getFrontendRedirectUrl();
        if (fallback == null || fallback.isBlank()) {
            fallback = "/login/callback";
        }

        if (state == null || state.isBlank()) {
            return fallback;
        }

        String stateKey = CAS_STATE_KEY_PREFIX + state;
        String redirect = redisCache.getCacheObject(stateKey);
        redisCache.deleteObject(stateKey);

        if (redirect == null || redirect.isBlank()) {
            return fallback;
        }
        return redirect;
    }

    private String resolveAndValidateFrontendRedirect(String requestedRedirect) {
        String fallback = casProperties.getFrontendRedirectUrl();
        if (fallback == null || fallback.isBlank()) {
            fallback = "/login/callback";
        }

        if (requestedRedirect == null || requestedRedirect.isBlank()) {
            return fallback;
        }

        String trimmed = requestedRedirect.trim();
        if (trimmed.startsWith("/")) {
            return trimmed;
        }

        if (isSameOrigin(trimmed, fallback)) {
            return trimmed;
        }

        log.warn("非法 CAS redirect 参数，已回退默认地址: {}", trimmed);
        return fallback;
    }

    private boolean isSameOrigin(String url1, String url2) {
        try {
            URI u1 = URI.create(url1);
            URI u2 = URI.create(url2);
            if (u1.getHost() == null || u2.getHost() == null) {
                return false;
            }

            String s1 = u1.getScheme() == null ? "" : u1.getScheme();
            String s2 = u2.getScheme() == null ? "" : u2.getScheme();
            int p1 = u1.getPort();
            int p2 = u2.getPort();

            return s1.equalsIgnoreCase(s2)
                    && u1.getHost().equalsIgnoreCase(u2.getHost())
                    && p1 == p2;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public Map<String, String> exchangeCodeForTokens(String code) {
        ensureCasEnabled();
        Assert.hasText(code, "CAS code 不能为空");

        String codeKey = CAS_CODE_KEY_PREFIX + code;
        String userId = redisCache.getCacheObject(codeKey);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("CAS code 无效或已过期");
        }

        redisCache.deleteObject(codeKey);
        return authenticationService.login(userId);
    }

    @Override
    public String buildCasLogoutRedirectUrl(String postLogoutRedirect) {
        ensureCasEnabled();

        String target = postLogoutRedirect;
        if (target == null || target.isBlank()) {
            target = "/login";
        }

        return UriComponentsBuilder
                .fromUriString(Objects.requireNonNull(casProperties.getServerLogoutUrl()))
                .queryParam("service", target)
                .build()
                .encode()
                .toUriString();
    }

    private String buildCallbackServiceUrl() {
        return UriComponentsBuilder
                .fromUriString(Objects.requireNonNull(casProperties.getClientServiceUrl()))
                .build()
                .encode()
                .toUriString();
    }

    private void syncBasicProfile(User currentUser, Map<String, String> attributes) {
        if (currentUser == null || attributes == null || attributes.isEmpty()) {
            return;
        }

        User update = new User();
        update.setId(currentUser.getId());

        String casName = attributes.get("cn");
        if (casName != null && !casName.equals(currentUser.getName())) {
            update.setName(casName);
        }

        String college = attributes.get("eduPersonOrgDN");
        if (college != null && !college.equals(currentUser.getCollege())) {
            update.setCollege(college);
        }

        String major = firstNonBlank(attributes.get("major"), attributes.get("profession"));
        if (major != null && !major.equals(currentUser.getMajor())) {
            update.setMajor(major);
        }

        if (update.getName() != null || update.getCollege() != null || update.getMajor() != null) {
            userService.updateUser(update);
            log.debug("CAS 用户属性已同步, userId: {}", currentUser.getId());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void ensureCasEnabled() {
        if (!casProperties.isEnabled()) {
            throw new IllegalStateException("CAS 未启用，请检查配置项 cas.enabled");
        }
        Assert.hasText(casProperties.getServerLoginUrl(), "cas.server-login-url 不能为空");
        Assert.hasText(casProperties.getServerUrlPrefix(), "cas.server-url-prefix 不能为空");
        Assert.hasText(casProperties.getClientServiceUrl(), "cas.client-service-url 不能为空");
        Assert.hasText(casProperties.getServerLogoutUrl(), "cas.server-logout-url 不能为空");
    }
}
