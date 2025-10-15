package com.cqwu.cqwu_appointment_system.common.constant;

import java.util.Map;

public final class StaticMapping {

    private static final Map<Long, String> CATEGORY_MAP = Map.of(
        1L, "科研场所",
        2L, "专用教室",
        3L, "会议室"
    );

    /**
     * 根据分类ID获取分类名称
     * @param key 分类ID
     * @return 对应的分类名称，如果不存在则返回 null
     */
    public String getCategoryName(Long key) {
        return CATEGORY_MAP.get(key);
    }
    

    private StaticMapping() {}
}