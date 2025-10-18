package com.cqwu.cqwu_appointment_system.config;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import org.springframework.stereotype.Component;

/**
 * @description 用于在 SpEL 表达式中暴露安全相关的常量
 */
@Component("securityConstants")
public class SecurityConstants {

    public final String ROLE_ADMIN = Constants.ROLE_ADMIN;
    public final String ROLE_USER = Constants.ROLE_USER;
    public final String ROLE_ROOT = Constants.ROLE_ROOT; 
}