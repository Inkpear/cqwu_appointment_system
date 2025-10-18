package com.cqwu.cqwu_appointment_system.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @description 校验相关的工具类
 */
public class ValidationUtils {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtils.class);

    /**
     * 检查一个对象的所有字段是否都为 null。
     * 
     * @param object 要检查的对象
     * @return 如果对象为 null 或其所有字段都为 null，则返回 true；否则返回 false。
     */
    public static boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true;
        }

        // 遍历对象的所有声明字段（包括 private）
        for (Field f : object.getClass().getDeclaredFields()) {
            f.setAccessible(true); // 允许访问私有字段
            try {
                if (f.get(object) != null) {
                    return false; // 只要找到一个非 null 字段，就立即返回 false
                }
            } catch (IllegalAccessException e) {
                // 这是一个不应该发生的内部错误，但为了健壮性我们还是处理它
                log.error("通过反射访问字段时出错: {}", e.getMessage(), e);
                // 在发生异常时，我们采取保守策略，假定它不为空，以避免错误地阻止一个有效的请求
                return false;
            }
        }

        return true; // 所有字段都检查完毕，且都为 null
    }

    /**
     * 【新增】校验一个范围的开始值是否严格小于结束值。
     * 适用于任何实现了 Comparable 接口的类型 (如 LocalTime, LocalDateTime, Integer)。
     *
     * @param start   范围的开始值
     * @param end     范围的结束值
     * @param message 如果校验失败，要抛出的异常信息
     * @param <T>     必须是可比较的类型
     * @throws IllegalArgumentException 如果 start 不小于 end
     */
    public static <T extends Comparable<T>> void requireStartBeforeEnd(T start, T end, String message) {
        // 只有当两个值都非 null 时，才进行比较
        if (start != null && end != null) {
            // compareTo < 0 => start < end
            // compareTo == 0 => start == end
            // compareTo > 0 => start > end
            if (start.compareTo(end) >= 0) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}