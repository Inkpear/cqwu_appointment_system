package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.entity.AppFile;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件详情DTO
 * 向前端返回文件列表时使用，包含了可直接访问的完整URL
 */
@Data
@NoArgsConstructor
public class FileDetailDto {
    private Long id;
    private String originalFilename;
    private String fileCategory;
    private Integer displayOrder;
    private String url;

    /**
     * 从AppFile实体转换为DTO
     * 
     * @param appFile 文件实体
     * @param presignedUrl 预签名的可访问URL
     * @return 文件详情DTO
     */
    public static FileDetailDto fromAppFile(AppFile appFile, String presignedUrl) {
        FileDetailDto dto = new FileDetailDto();
        dto.setId(appFile.getId());
        dto.setOriginalFilename(appFile.getOriginalFilename());
        dto.setFileCategory(appFile.getFileCategory());
        dto.setDisplayOrder(appFile.getDisplayOrder());
        dto.setUrl(presignedUrl);
        return dto;
    }
}
