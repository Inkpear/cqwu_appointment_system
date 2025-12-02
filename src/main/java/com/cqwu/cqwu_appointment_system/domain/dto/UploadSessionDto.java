package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 上传会话信息 DTO
 * 存储在 Redis 中，用于跟踪文件上传会话的状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadSessionDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 实体类型
     */
    private EntityType entityType;
    
    /**
     * 文件分类
     */
    private FileCategory fileCategory;
    
    /**
     * 已上传的文件数量
     */
    private Integer uploadedCount = 0;
    
    /**
     * 创建时间戳
     */
    private Long createTime;
}
