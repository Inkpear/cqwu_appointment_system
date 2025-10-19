package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description 专为前端日历视图设计的事件数据传输对象。
 */
@Data
public class CalendarEventDTO {

    private Long id;
    private String title;
    private LocalDateTime start; // 适配前端日历的字段名
    private LocalDateTime end;   // 适配前端日历的字段名
    private String blockType;    // 用于前端根据类型设置不同颜色
    
    // 增强信息，方便前端判断
    private boolean isOwnedByCurrentUser; 

    /**
     * 静态工厂方法，用于从 TimeBlock 实体和当前用户创建 DTO。
     */
    public static CalendarEventDTO from(TimeBlock timeBlock) {
        if (timeBlock == null) {
            return null;
        }

        CalendarEventDTO dto = new CalendarEventDTO();
        dto.setId(timeBlock.getId());
        dto.setTitle(timeBlock.getTitle());
        dto.setStart(timeBlock.getStartDatetime());
        dto.setEnd(timeBlock.getEndDatetime());
        dto.setBlockType(timeBlock.getBlockType());

        return dto;
    }
}