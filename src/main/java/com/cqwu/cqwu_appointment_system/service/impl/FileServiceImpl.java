package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import com.cqwu.cqwu_appointment_system.common.utils.uuid.UUID;
import com.cqwu.cqwu_appointment_system.config.FileUploadProperties;
import com.cqwu.cqwu_appointment_system.config.MinioProperties;
import com.cqwu.cqwu_appointment_system.domain.dto.FileDetailDto;
import com.cqwu.cqwu_appointment_system.domain.dto.PresignedUrlDto;
import com.cqwu.cqwu_appointment_system.domain.dto.UploadSessionDto;
import com.cqwu.cqwu_appointment_system.entity.AppFile;
import com.cqwu.cqwu_appointment_system.entity.AppFileExample;
import com.cqwu.cqwu_appointment_system.mapper.AppFileMapper;
import com.cqwu.cqwu_appointment_system.service.FileService;
import com.cqwu.cqwu_appointment_system.framework.redis.RedisCache;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

/**
 * 文件服务实现
 * 提供MinIO文件操作的完整流程
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final String SESSION_KEY_PREFIX = "file:upload:session:";
    private static final long SESSION_EXPIRY_MINUTES = 10L;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private AppFileMapper appFileMapper;

    @Autowired
    private FileUploadProperties fileUploadProperties;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private RedisCache redisCache;

    @Override
    public String createUploadSession(EntityType entityType, FileCategory fileCategory) {
        if (EntityType.RESERVATION.equals(entityType) && FileCategory.CAROUSEL.equals(fileCategory)) {
            throw new IllegalArgumentException("预约不支持轮播图类型的文件");
        }

        String sessionId = UUID.randomUUID().toString(true);
        String redisKey = SESSION_KEY_PREFIX + sessionId;

        UploadSessionDto sessionDto = new UploadSessionDto(
                sessionId,
                entityType,
                fileCategory,
                0,
                System.currentTimeMillis());

        redisCache.setCacheObject(redisKey, sessionDto, SESSION_EXPIRY_MINUTES, TimeUnit.MINUTES);
        log.info("创建文件上传会话，sessionId: {}, 实体类型: {}, 文件分类: {}, 过期时间: {}分钟",
                sessionId, entityType, fileCategory, SESSION_EXPIRY_MINUTES);

        return sessionId;
    }

    /**
     * 校验 sessionId 是否有效，并返回会话信息
     */
    private UploadSessionDto validateSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("会话 ID 不能为空");
        }

        String redisKey = SESSION_KEY_PREFIX + sessionId;
        UploadSessionDto sessionDto = redisCache.getCacheObject(redisKey);

        if (sessionDto == null) {
            log.warn("无效或过期的会话 ID: {}", sessionId);
            throw new IllegalArgumentException("会话已过期或不存在，请重新获取上传会话");
        }

        return sessionDto;
    }

    @Override
    public PresignedUrlDto generatePresignedUploadUrl(
            String originalFilename,
            String sessionId,
            EntityType entityType,
            FileCategory fileCategory) {

        if (originalFilename.contains("-")) {
            throw new IllegalArgumentException("文件名不能包含特殊字符 '-'");
        }

        UploadSessionDto sessionDto = validateSession(sessionId);

        if (!sessionDto.getEntityType().equals(entityType)) {
            throw new IllegalArgumentException(
                    String.format("实体类型不匹配：会话类型为 %s，请求类型为 %s",
                            sessionDto.getEntityType(), entityType));
        }
        if (!sessionDto.getFileCategory().equals(fileCategory)) {
            throw new IllegalArgumentException(
                    String.format("文件分类不匹配：会话分类为 %s，请求分类为 %s",
                            sessionDto.getFileCategory(), fileCategory));
        }

        if (EntityType.RESERVATION.equals(entityType) && FileCategory.ATTACHMENT.equals(fileCategory)) {
            if (sessionDto.getUploadedCount() >= fileUploadProperties.getMaxAttachmentsPerReservation()) {
                throw new IllegalArgumentException(
                        String.format("预约附件数量超出限制，最多允许 %d 个文件",
                                fileUploadProperties.getMaxAttachmentsPerReservation()));
            }
        }

        validateFileType(originalFilename);

        String objectName = buildObjectName(originalFilename, sessionId, entityType, fileCategory);

        try {
            String url = getPresignClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .expiry(fileUploadProperties.getPresignedUrlExpiryMinutes(), TimeUnit.MINUTES)
                            .build());
            log.info("为会话 {} 生成预签名上传URL，实体类型: {}, 对象名: {}",
                    sessionId, entityType, objectName);
            return new PresignedUrlDto(url, objectName);
        } catch (Exception e) {
            log.error("生成MinIO预签名上传URL失败", e);
            throw new RuntimeErrorException(new Error("获取文件上传许可失败"));
        }
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId) {
        AppFile appFile = appFileMapper.selectByPrimaryKey(fileId);
        if (appFile == null) {
            log.warn("尝试删除不存在的文件记录，fileId: {}", fileId);
            throw new IllegalArgumentException("文件不存在");
        }

        int deleted = appFileMapper.deleteByPrimaryKey(fileId);
        if (deleted <= 0) {
            throw new RuntimeException("文件记录删除失败");
        }

        scheduleMinioDeleteAfterCommit(List.of(appFile.getObjectName()));

        log.info("文件记录已删除，fileId: {}", fileId);
    }

    @Override
    @Transactional
    public int deleteFileRecordsByEntityIds(EntityType entityType, List<Long> entityIds) {
        if (entityType == null || entityIds == null || entityIds.isEmpty()) {
            return 0;
        }

        AppFileExample selectExample = new AppFileExample();
        selectExample.createCriteria()
                .andEntityTypeEqualTo(entityType.name())
                .andRelatedEntityIdIn(entityIds);

        List<AppFile> files = appFileMapper.selectByExample(selectExample);
        if (files.isEmpty()) {
            return 0;
        }

        List<String> objectNames = files.stream()
                .map(AppFile::getObjectName)
                .collect(Collectors.toList());

        AppFileExample deleteExample = new AppFileExample();
        deleteExample.createCriteria()
                .andEntityTypeEqualTo(entityType.name())
                .andRelatedEntityIdIn(entityIds);

        int deleted = appFileMapper.deleteByExample(deleteExample);
        scheduleMinioDeleteAfterCommit(objectNames);

        log.info("按实体批量删除文件记录，entityType: {}, entityCount: {}, deleted: {}",
                entityType, entityIds.size(), deleted);

        return deleted;
    }

    private void scheduleMinioDeleteAfterCommit(List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return;
        }

        Runnable deleteTask = () -> {
            for (String objectName : objectNames) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(minioProperties.getBucketName())
                                    .object(objectName)
                                    .build());
                    log.info("成功从MinIO删除文件: {}", objectName);
                } catch (Exception e) {
                    log.warn("MinIO 文件删除失败，等待定时任务清理，objectName: {}", objectName, e);
                }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteTask.run();
                }
            });
        } else {
            deleteTask.run();
        }
    }

    /**
     * 将会话中已上传到 MinIO 的文件关联到业务实体
     * 此方法会：
     * 1. 验证 session 有效性
     * 2. 查询 MinIO 中该 session 对应路径的所有文件
     * 3. 为每个文件创建数据库记录并关联到实体
     * 
     * 这样就不需要前端先调用 confirm 接口了，避免产生孤儿文件
     */
    @Override
    @Transactional
    public void linkFilesToEntity(Long entityId, String sessionId, EntityType entityType, FileCategory fileCategory) {
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("尝试关联文件但 sessionId 为空");
            return;
        }

        UploadSessionDto sessionDto = validateSession(sessionId);

        if (!sessionDto.getEntityType().equals(entityType)) {
            throw new IllegalArgumentException(
                    String.format("实体类型不匹配：会话类型为 %s，请求类型为 %s",
                            sessionDto.getEntityType(), entityType));
        }
        if (!sessionDto.getFileCategory().equals(fileCategory)) {
            throw new IllegalArgumentException(
                    String.format("文件分类不匹配：会话分类为 %s，请求分类为 %s",
                            sessionDto.getFileCategory(), fileCategory));
        }

        log.info("关联文件到实体，实体ID: {}, 会话ID: {}, 实体类型: {}, 文件分类: {}",
                entityId, sessionId, entityType, fileCategory);

        String prefix = buildTempPrefix(sessionId, entityType, fileCategory);
        List<String> uploadedObjects = listObjectsWithPrefix(prefix);

        if (uploadedObjects.isEmpty()) {
            log.warn("会话 {} 未找到任何已上传的文件（prefix: {}）", sessionId, prefix);
            return;
        }

        int successCount = 0;
        for (String objectName : uploadedObjects) {
            String originalFilename = extractOriginalFilename(objectName);

            AppFile appFile = new AppFile();
            appFile.setRelatedEntityId(entityId);
            appFile.setEntityType(entityType.name());
            appFile.setFileCategory(fileCategory.name());
            appFile.setObjectName(objectName);
            appFile.setOriginalFilename(originalFilename);
            appFile.setDisplayOrder(successCount);

            int inserted = appFileMapper.insertSelective(appFile);
            if (inserted <= 0) {
                throw new RuntimeException("文件关联失败，objectName: " + objectName);
            }
            successCount++;

            log.debug("已创建文件记录并关联实体，fileId: {}, objectName: {}", appFile.getId(), objectName);
        }

        log.info("成功关联 {} 个文件到实体 {}（类型: {}）", successCount, entityId, entityType);

        // 5. 清理 Redis session（已完成关联，不再需要）
        String redisKey = SESSION_KEY_PREFIX + sessionId;
        redisCache.deleteObject(redisKey);
    }

    /**
     * 构建临时文件前缀路径
     */
    private String buildTempPrefix(String sessionId, EntityType entityType, FileCategory fileCategory) {
        String entityFolder = entityType == EntityType.VENUE ? "venue" : "reservation";
        String categoryFolder = fileCategory == FileCategory.CAROUSEL ? "carousel" : "attachment";
        return String.format("%s/temp_%s/%s/", entityFolder, sessionId, categoryFolder);
    }

    /**
     * 列出指定前缀的所有对象
     */
    private List<String> listObjectsWithPrefix(String prefix) {
        List<String> objectNames = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .prefix(prefix)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                objectNames.add(item.objectName());
            }
        } catch (Exception e) {
            log.error("列出 MinIO 对象失败，prefix: {}", prefix, e);
            throw new RuntimeException("查询已上传文件失败", e);
        }
        return objectNames;
    }

    /**
     * 从对象名中提取原始文件名
     * 例如：venue/temp_abc123/attachment/uuid_document.pdf -> document.pdf
     */
    private String extractOriginalFilename(String objectName) {
        String filename = objectName.substring(objectName.lastIndexOf('/') + 1);
        // 移除 UUID 前缀（格式：uuid-originalname）
        int underscoreIndex = filename.indexOf('-');
        if (underscoreIndex > 0 && underscoreIndex < filename.length() - 1) {
            return filename.substring(underscoreIndex + 1);
        }
        return filename;
    }

    @Override
    public AppFile getFileById(Long fileId) {
        return appFileMapper.selectByPrimaryKey(fileId);
    }

    @Override
    public List<FileDetailDto> getFilesByEntity(Long relatedEntityId, EntityType entityType) {
        AppFileExample example = new AppFileExample();
        example.createCriteria()
                .andRelatedEntityIdEqualTo(relatedEntityId)
                .andEntityTypeEqualTo(entityType.name());

        List<AppFile> appFiles = appFileMapper.selectByExample(example);
        return convertToDetailDtoList(appFiles);
    }

    @Override
    public List<FileDetailDto> getFilesByEntityAndCategorySorted(
            Long relatedEntityId,
            EntityType entityType,
            FileCategory fileCategory) {
        AppFileExample example = new AppFileExample();
        example.setOrderByClause("display_order ASC");
        example.createCriteria()
                .andRelatedEntityIdEqualTo(relatedEntityId)
                .andEntityTypeEqualTo(entityType.name())
                .andFileCategoryEqualTo(fileCategory.name());

        List<AppFile> appFiles = appFileMapper.selectByExample(example);
        return convertToDetailDtoList(appFiles);
    }

    /**
     * 将AppFile实体列表转换为DTO列表
     * 为每个文件生成预签名的下载URL（7天有效期）
     */
    private List<FileDetailDto> convertToDetailDtoList(List<AppFile> appFiles) {
        return appFiles.stream()
                .map(file -> {
                    String presignedUrl = generatePresignedDownloadUrl(file.getObjectName());
                    return FileDetailDto.fromAppFile(file, presignedUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * 生成预签名的下载URL
     * 
     * @param objectName MinIO对象名
     * @return 预签名URL（7天有效期）
     */
    private String generatePresignedDownloadUrl(String objectName) {
        try {
            return getPresignClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build());
        } catch (Exception e) {
            log.error("生成下载URL失败, objectName: {}", objectName, e);
            String fallbackBase = StringUtils.hasText(minioProperties.getPublicEndpoint())
                    ? minioProperties.getPublicEndpoint()
                    : minioProperties.getEndpoint();
            return fallbackBase + "/" + minioProperties.getBucketName() + "/" + objectName;
        }
    }

    private MinioClient getPresignClient() {
        if (!StringUtils.hasText(minioProperties.getPublicEndpoint())) {
            return minioClient;
        }

        return MinioClient.builder()
                .endpoint(minioProperties.getPublicEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    /**
     * 构建MinIO对象名（使用 sessionId 而非 relatedEntityId）
     * 路径结构: {entity_type}/temp_{sessionId}/{category}/{uuid}_{originalFilename}
     * 
     * 例如:
     * venue/temp_abc123/attachment/550e8400-e29b-41d4-a716-446655440000_document.pdf
     */
    private String buildObjectName(
            String originalFilename,
            String sessionId,
            EntityType entityType,
            FileCategory fileCategory) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 生成唯一的 UUID
        String uuid = UUID.randomUUID().toString(true);

        // 构建对象名：uuid_originalFilename 格式
        // 这样可以保留原始文件名，同时确保唯一性
        return String.format("%s/temp_%s/%s/%s-%s",
                entityType.name().toLowerCase(),
                sessionId,
                fileCategory.name().toLowerCase(),
                uuid,
                originalFilename);
    }

    /**
     * 验证文件类型是否允许
     */
    private void validateFileType(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        List<String> allowedExtensions = fileUploadProperties.getAllowedExtensions();

        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            log.warn("文件扩展名白名单未配置，将拒绝所有上传");
            throw new IllegalArgumentException("文件上传功能未正确配置，请联系管理员");
        }

        if (!allowedExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException(
                    String.format("不支持的文件类型: %s。允许的类型: %s",
                            fileExtension,
                            String.join(", ", allowedExtensions)));
        }
    }

    @Override
    @Transactional
    public void updateFileDisplayOrder(Long fileId, Integer displayOrder) {
        if (fileId == null) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        if (displayOrder == null || displayOrder < 0) {
            throw new IllegalArgumentException("显示顺序必须大于等于0");
        }

        AppFile file = appFileMapper.selectByPrimaryKey(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }

        AppFile updateEntity = new AppFile();
        updateEntity.setId(fileId);
        updateEntity.setDisplayOrder(displayOrder);

        appFileMapper.updateByPrimaryKeySelective(updateEntity);

        log.info("更新文件显示顺序，fileId: {}, 原顺序: {}, 新顺序: {}",
                fileId, file.getDisplayOrder(), displayOrder);
    }
}
