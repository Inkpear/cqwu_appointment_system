package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.entity.AppFile;
import com.cqwu.cqwu_appointment_system.entity.AppFileExample;
import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.mapper.AppFileMapper;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.config.MinioProperties;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 临时文件清理调度器
 * 定时扫描 MinIO 和数据库，清理孤儿文件和记录
 */
@Component
@Slf4j
public class TempFileCleanupScheduler {

    @Autowired
    private AppFileMapper appFileMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    private static final int FILE_EXPIRY_MINUTES = 30;

    /**
     * 每小时执行一次，清理孤儿文件
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupOrphanFiles() {
        log.info("【定时任务】开始清理孤儿文件...");

        int cleanedTempRecords = cleanupUnlinkedRecordsInDatabase();
        int cleanedMinioFiles = cleanupOrphanFilesInMinio();
        int cleanedDbRecords = cleanupOrphanRecordsInDatabase();

        log.info("【定时任务】清理完成，数据库未关联记录: {} 条，MinIO 孤儿文件: {} 个，数据库孤儿记录: {} 条",
                cleanedTempRecords, cleanedMinioFiles, cleanedDbRecords);
    }

    /**
     * 清理数据库中 related_entity_id 为 NULL 的记录（未关联实体的临时记录）
     * 场景：用户上传文件后未调用业务接口关联，导致数据库中存在孤儿记录
     */
    private int cleanupUnlinkedRecordsInDatabase() {
        AppFileExample example = new AppFileExample();
        example.createCriteria()
                .andRelatedEntityIdIsNull()
                .andCreateTimeLessThan(LocalDateTime.now().minusMinutes(FILE_EXPIRY_MINUTES));

        List<AppFile> unlinkedFiles = appFileMapper.selectByExample(example);

        if (unlinkedFiles.isEmpty()) {
            log.debug("【数据库清理】未找到超时的未关联记录");
            return 0;
        }

        log.info("【数据库清理】发现 {} 条超过 {} 分钟未关联的记录",
                unlinkedFiles.size(), FILE_EXPIRY_MINUTES);

        int deletedCount = 0;
        for (AppFile file : unlinkedFiles) {
            try {
                appFileMapper.deleteByPrimaryKey(file.getId());
                deletedCount++;

                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(minioProperties.getBucketName())
                                    .object(file.getObjectName())
                                    .build());
                    log.debug("【数据库清理】同步删除 MinIO 文件: {}", file.getObjectName());
                } catch (Exception e) {
                    log.warn("【数据库清理】MinIO 文件可能已不存在: {}", file.getObjectName());
                }

                log.debug("【数据库清理】删除未关联记录，fileId: {}, objectName: {}, createTime: {}",
                        file.getId(), file.getObjectName(), file.getCreateTime());

            } catch (Exception e) {
                log.error("【数据库清理】删除未关联记录失败，fileId: {}", file.getId(), e);
            }
        }

        log.info("【数据库清理】成功清理 {} 条未关联记录", deletedCount);
        return deletedCount;
    }

    /**
     * 清理 MinIO 中的孤儿文件
     * 策略：获取数据库白名单，删除 MinIO 中不在白名单且超时的文件
     */
    private int cleanupOrphanFilesInMinio() {
        try {
            Set<String> validObjectNames = getValidObjectNamesFromDatabase();
            log.debug("【MinIO 清理】数据库中有效文件数量: {}", validObjectNames.size());

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .recursive(true)
                            .build());

            int deletedCount = 0;
            int checkedCount = 0;
            LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(FILE_EXPIRY_MINUTES);

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                checkedCount++;

                if (validObjectNames.contains(objectName)) {
                    continue;
                }

                LocalDateTime lastModified = Instant.ofEpochMilli(item.lastModified().toInstant().toEpochMilli())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                if (lastModified.isBefore(expiryThreshold)) {
                    try {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(minioProperties.getBucketName())
                                        .object(objectName)
                                        .build());
                        deletedCount++;
                        log.debug("【MinIO 清理】删除孤儿文件: {}, 上传时间: {}", objectName, lastModified);
                    } catch (Exception e) {
                        log.error("【MinIO 清理】删除文件失败: {}", objectName, e);
                    }
                }
            }

            log.info("【MinIO 清理】检查了 {} 个文件，删除了 {} 个孤儿文件", checkedCount, deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("【MinIO 清理】遍历 MinIO 文件失败", e);
            return 0;
        }
    }

    /**
     * 从数据库获取所有有效文件的 objectName 集合
     */
    private Set<String> getValidObjectNamesFromDatabase() {
        List<AppFile> allFiles = appFileMapper.selectByExample(new AppFileExample());
        Set<String> validNames = new HashSet<>();

        for (AppFile file : allFiles) {
            validNames.add(file.getObjectName());
        }

        return validNames;
    }

    /**
     * 清理数据库中关联实体已被删除的记录
     * 场景：预约或场所被删除后，其关联的附件记录成为孤儿记录
     */
    private int cleanupOrphanRecordsInDatabase() {
        AppFileExample example = new AppFileExample();
        example.createCriteria()
                .andRelatedEntityIdIsNotNull();

        List<AppFile> attachedFiles = appFileMapper.selectByExample(example);

        if (attachedFiles.isEmpty()) {
            log.debug("【数据库清理】未找到已关联实体的文件记录");
            return 0;
        }

        log.debug("【数据库清理】检查 {} 条文件记录的实体状态", attachedFiles.size());

        List<AppFile> orphanRecords = new ArrayList<>();

        for (AppFile file : attachedFiles) {
            boolean isOrphan = false;

            if ("RESERVATION".equals(file.getEntityType())) {
                Reservation reservation = reservationMapper.selectByPrimaryKey(file.getRelatedEntityId());
                if (reservation == null) {
                    isOrphan = true;
                    log.debug("【数据库清理】发现孤儿记录，预约已删除，fileId: {}, reservationId: {}",
                            file.getId(), file.getRelatedEntityId());
                }
            } else if ("VENUE".equals(file.getEntityType())) {
                Venue venue = venueMapper.selectByPrimaryKey(file.getRelatedEntityId());
                if (venue == null) {
                    isOrphan = true;
                    log.debug("【数据库清理】发现孤儿记录，场所已删除，fileId: {}, venueId: {}",
                            file.getId(), file.getRelatedEntityId());
                }
            }

            if (isOrphan) {
                orphanRecords.add(file);
            }
        }

        if (orphanRecords.isEmpty()) {
            log.debug("【数据库清理】未发现孤儿记录");
            return 0;
        }

        log.info("【数据库清理】发现 {} 条孤儿记录", orphanRecords.size());

        int deletedCount = 0;
        for (AppFile file : orphanRecords) {
            try {
                appFileMapper.deleteByPrimaryKey(file.getId());
                deletedCount++;

                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(minioProperties.getBucketName())
                                    .object(file.getObjectName())
                                    .build());
                    log.debug("【数据库清理】同步删除 MinIO 文件: {}", file.getObjectName());
                } catch (Exception e) {
                    log.warn("【数据库清理】MinIO 文件可能已不存在: {}", file.getObjectName());
                }

                log.debug("【数据库清理】成功删除孤儿记录，fileId: {}, objectName: {}, entityType: {}, entityId: {}",
                        file.getId(), file.getObjectName(), file.getEntityType(), file.getRelatedEntityId());

            } catch (Exception e) {
                log.error("【数据库清理】删除孤儿记录失败，fileId: {}, objectName: {}",
                        file.getId(), file.getObjectName(), e);
            }
        }

        log.info("【数据库清理】成功清理 {} 条孤儿记录", deletedCount);
        return deletedCount;
    }
}
