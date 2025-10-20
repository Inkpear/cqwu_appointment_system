package com.cqwu.cqwu_appointment_system.config;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import org.springframework.stereotype.Component;

/**
 * @description 将核心业务常量暴露为 Spring Bean，以便在 Spring Expression Language (SpEL) 中安全地引用。
 *              主要用于 Spring Security 的注解，如 @PreAuthorize，以避免硬编码角色字符串。
 * @example @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN)")
 */
@Component("securityConstants")
public class SecurityConstants {    
    public final String ROLE_ADMIN = Constants.UserRoleType.ROLE_ADMIN;
    public final String ROLE_USER = Constants.UserRoleType.ROLE_USER;
    public final String ROLE_ROOT = Constants.UserRoleType.ROLE_ROOT; 
}