package com.cqwu.cqwu_appointment_system.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * @description @ValidStatus 注解的具体实现逻辑
 */
public class StatusValidator implements ConstraintValidator<ValidStatus, String> {

    private final Set<String> allowedValues = new HashSet<>();

    @Override
    public void initialize(ValidStatus constraintAnnotation) {
        Class<?> constantsClass = constraintAnnotation.constantsClass();
        Field[] fields = constantsClass.getFields(); // 获取所有 public 字段

        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers()) &&
                Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                field.getType().equals(String.class)) {
                try {
                    allowedValues.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 核心校验逻辑。
     * @param value 待校验的字段值 (例如: "AVAILABLE", "INVALID_STATUS")
     * @return 如果值在允许的列表中，则返回 true，否则返回 false
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return allowedValues.contains(value);
    }
}