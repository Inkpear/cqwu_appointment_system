package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.config.properties.CasProperties;
import com.cqwu.cqwu_appointment_system.domain.CasUserPrincipal;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.framework.redis.RedisCache;
import com.cqwu.cqwu_appointment_system.service.AuthenticationService;
import com.cqwu.cqwu_appointment_system.service.CasAuthenticationService;
import com.cqwu.cqwu_appointment_system.service.CasClientService;
import com.cqwu.cqwu_appointment_system.service.UserService;
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

        if (frontendRedirect != null && !frontendRedirect.isBlank()) {
            log.info("当前 CAS 登录流程使用固定前端回跳地址，已忽略前端自定义 redirect: {}", frontendRedirect);
        }

        String callbackServiceUrl = buildCallbackServiceUrl();

        return UriComponentsBuilder
                .fromUriString(Objects.requireNonNull(casProperties.getServerLoginUrl()))
                .queryParam("service", callbackServiceUrl)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public String handleCasCallbackAndIssueCode(String ticket, String state) {
        ensureCasEnabled();
        Assert.hasText(ticket, "CAS ticket 不能为空");

        String frontendRedirect = casProperties.getFrontendRedirectUrl();
        if (frontendRedirect == null || frontendRedirect.isBlank()) {
            frontendRedirect = "/login/callback";
        }

        String callbackServiceUrl = buildCallbackServiceUrl();
        CasUserPrincipal principal = casClientService.validateServiceTicket(ticket, callbackServiceUrl);

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

        String casName = firstNonBlank(attributes.get("cn"), attributes.get("user_name"), attributes.get("name"));
        if (casName != null && !casName.equals(currentUser.getName())) {
            update.setName(casName);
        }

        String college = firstNonBlank(attributes.get("college"), attributes.get("department"));
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
