package com.cqwu.cqwu_appointment_system.domain.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 预签名URL响应DTO
 * 用于向前端返回MinIO上传所需信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlDto {
    /**
     * 前端应该向这个URL发起PUT请求来上传文件
     */
    private String uploadUrl;

    /**
     * 文件在MinIO中的完整对象名，上传成功后前端需将此值传回后端进行确认
     */
    private String objectName;
}
