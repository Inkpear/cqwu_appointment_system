package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import com.cqwu.cqwu_appointment_system.domain.dto.FileDetailDto;
import com.cqwu.cqwu_appointment_system.domain.dto.PresignedUrlDto;
import com.cqwu.cqwu_appointment_system.entity.AppFile;

import java.util.List;

/**
 * 文件服务接口
 * 负责文件的上传、删除、查询等操作
 */
public interface FileService {

        /**
         * 创建文件上传会话
         * 生成唯一的 sessionId 并存入 Redis（10分钟有效期）
         *
         * @param entityType   实体类型（VENUE 或 RESERVATION）
         * @param fileCategory 文件分类（ATTACHMENT 或 CAROUSEL）
         * @return sessionId
         */
        String createUploadSession(EntityType entityType, FileCategory fileCategory);

        /**
         * 为文件上传生成一个预签名的URL。
         *
         * @param originalFilename 文件的原始名称，用于提取后缀
         * @param sessionId        会话标识（用于临时文件标记，避免并发冲突）
         * @param entityType       业务实体类型 (VENUE 或 RESERVATION)
         * @param fileCategory     文件分类 (CAROUSEL 或 ATTACHMENT)
         * @return 包含上传URL和objectName的DTO
         */
        PresignedUrlDto generatePresignedUploadUrl(
                        String originalFilename,
                        String sessionId,
                        EntityType entityType,
                        FileCategory fileCategory);

        /**
         * 根据文件ID删除文件（包括MinIO实体和数据库记录）。
         *
         * @param fileId 文件的数据库主键ID
         */
        void deleteFile(Long fileId);

        /**
         * 按业务实体批量删除文件记录。
         * 数据库记录会优先删除，MinIO 物理文件在事务提交后尽力清理。
         *
         * @param entityType 业务实体类型
         * @param entityIds  业务实体ID列表
         * @return 成功删除的数据库记录数
         */
        int deleteFileRecordsByEntityIds(EntityType entityType, List<Long> entityIds);

        /**
         * 将会话中的临时文件关联到实体
         *
         * @param entityId     实体ID（场所ID或预约ID）
         * @param sessionId    上传会话ID
         * @param entityType   实体类型
         * @param fileCategory 文件分类
         */
        void linkFilesToEntity(Long entityId, String sessionId, EntityType entityType, FileCategory fileCategory);

        /**
         * 根据文件ID查询文件信息
         *
         * @param fileId 文件ID
         * @return 文件实体
         */
        AppFile getFileById(Long fileId);

        /**
         * 根据业务实体查询其所有关联的文件。
         *
         * @param relatedEntityId 关联的业务实体ID
         * @param entityType      业务实体类型
         * @return 包含完整访问URL的文件详情DTO列表
         */
        List<FileDetailDto> getFilesByEntity(Long relatedEntityId, EntityType entityType);

        /**
         * 根据业务实体和文件分类查询文件，并按display_order排序。
         *
         * @param relatedEntityId 关联的业务实体ID
         * @param entityType      业务实体类型
         * @param fileCategory    文件分类
         * @return 排序后的文件详情DTO列表
         */
        List<FileDetailDto> getFilesByEntityAndCategorySorted(
                        Long relatedEntityId,
                        EntityType entityType,
                        FileCategory fileCategory);

        /**
         * 更新文件的显示顺序
         *
         * @param fileId       文件ID
         * @param displayOrder 新的显示顺序
         */
        void updateFileDisplayOrder(Long fileId, Integer displayOrder);
}
