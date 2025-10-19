package com.cqwu.cqwu_appointment_system.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description 自定义校验注解，用于验证一个字符串字段的值是否在指定的常量类中定义。
 */
@Target({ElementType.FIELD}) // 这个注解只能用在字段上
@Retention(RetentionPolicy.RUNTIME) // 运行时校验
@Constraint(validatedBy = StatusValidator.class) // 指定由 StatusValidator 类来执行校验逻辑
public @interface ValidStatus {

    // 校验失败时的默认错误消息
    String message() default "无效的状态值";

    // JSR-303/Jakarta Validation 的标准样板代码
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * 【核心】指定包含有效状态常量的类。
     * 例如：Constants.VenueBaseStatus.class
     */
    Class<?> constantsClass();
}