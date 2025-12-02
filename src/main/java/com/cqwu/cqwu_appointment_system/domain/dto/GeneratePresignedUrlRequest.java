package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成预签名上传 URL 请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePresignedUrlRequest {
    
    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    
    /**
     * 文件原始名称
     */
    @NotBlank(message = "文件名不能为空")
    private String originalFilename;
    
    /**
     * 实体类型
     */
    @NotNull(message = "实体类型不能为空")
    private EntityType entityType;
    
    /**
     * 文件分类
     */
    @NotNull(message = "文件分类不能为空")
    private FileCategory fileCategory;
}
