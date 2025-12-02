package com.cqwu.cqwu_appointment_system.component;

import com.cqwu.cqwu_appointment_system.entity.AppFile;
import com.cqwu.cqwu_appointment_system.entity.AppFileExample;
import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.mapper.AppFileMapper;
import com.cqwu.cqwu_appointment_system.mapper.ReservationMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 临时文件清理定时任务
 * 
 * 功能：
 * 1. 清理超过 30 分钟未关联业务实体的临时文件（防止攻击者上传后不确认）
 * 2. 清理关联的业务实体已被删除的孤儿文件
 * 3. 同时删除 MinIO 物理文件和数据库记录
 * 
 * 执行频率：每小时一次
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

    /**
     * 每小时执行一次，清理临时文件和孤儿文件
     */
    @Scheduled(cron = "0 0 * * * ?")  // 每小时整点执行
    @Transactional
    public void cleanupTempFiles() {
        log.info("【定时任务】开始清理临时文件和孤儿文件...");
        
        int cleanedTempFiles = cleanupUnconfirmedTempFiles();
        int cleanedOrphanFiles = cleanupOrphanFiles();
        
        log.info("【定时任务】清理完成，删除临时文件: {} 个，删除孤儿文件: {} 个", 
                cleanedTempFiles, cleanedOrphanFiles);
    }

    /**
     * 清理超过 30 分钟未关联实体的临时文件
     * 场景：用户获取预签名 URL 并上传到 MinIO，但没有调用确认接口，或确认后没有关联到实体
     */
    private int cleanupUnconfirmedTempFiles() {
        // 查找超过 30 分钟且未关联实体的临时文件
        AppFileExample example = new AppFileExample();
        example.createCriteria()
                .andRelatedEntityIdIsNull()  // 未关联业务实体
                .andCreateTimeLessThan(LocalDateTime.now().minusMinutes(30));  // 超过30分钟
        
        List<AppFile> tempFiles = appFileMapper.selectByExample(example);
        
        if (tempFiles.isEmpty()) {
            log.debug("【临时文件清理】未找到需要清理的临时文件");
            return 0;
        }
        
        log.info("【临时文件清理】发现 {} 个超过30分钟未关联的临时文件", tempFiles.size());
        
        int deletedCount = 0;
        for (AppFile file : tempFiles) {
            try {
                // 1. 从 MinIO 删除物理文件
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(file.getObjectName())
                                .build()
                );
                
                // 2. 从数据库删除记录
                appFileMapper.deleteByPrimaryKey(file.getId());
                
                deletedCount++;
                log.debug("【临时文件清理】成功删除临时文件，fileId: {}, objectName: {}", 
                        file.getId(), file.getObjectName());
                
            } catch (Exception e) {
                log.error("【临时文件清理】删除临时文件失败，fileId: {}, objectName: {}", 
                        file.getId(), file.getObjectName(), e);
            }
        }
        
        return deletedCount;
    }

    /**
     * 清理关联的业务实体已被删除的孤儿文件
     * 场景：预约或场所被删除后，其关联的附件成为孤儿文件
     */
    private int cleanupOrphanFiles() {
        // 查找所有已关联实体的文件
        AppFileExample example = new AppFileExample();
        example.createCriteria()
                .andRelatedEntityIdIsNotNull();  // 已关联业务实体
        
        List<AppFile> attachedFiles = appFileMapper.selectByExample(example);
        
        if (attachedFiles.isEmpty()) {
            log.debug("【孤儿文件清理】未找到已关联实体的文件");
            return 0;
        }
        
        log.debug("【孤儿文件清理】检查 {} 个已关联文件的实体状态", attachedFiles.size());
        
        List<AppFile> orphanFiles = new ArrayList<>();
        
        // 检查每个文件关联的实体是否还存在
        for (AppFile file : attachedFiles) {
            boolean isOrphan = false;
            
            if ("RESERVATION".equals(file.getEntityType())) {
                // 检查预约是否存在
                Reservation reservation = reservationMapper.selectByPrimaryKey(file.getRelatedEntityId());
                if (reservation == null) {
                    isOrphan = true;
                    log.debug("【孤儿文件清理】发现孤儿文件，预约已删除，fileId: {}, reservationId: {}", 
                            file.getId(), file.getRelatedEntityId());
                }
            } else if ("VENUE".equals(file.getEntityType())) {
                // 检查场所是否存在
                Venue venue = venueMapper.selectByPrimaryKey(file.getRelatedEntityId());
                if (venue == null) {
                    isOrphan = true;
                    log.debug("【孤儿文件清理】发现孤儿文件，场所已删除，fileId: {}, venueId: {}", 
                            file.getId(), file.getRelatedEntityId());
                }
            }
            
            if (isOrphan) {
                orphanFiles.add(file);
            }
        }
        
        if (orphanFiles.isEmpty()) {
            log.debug("【孤儿文件清理】未发现孤儿文件");
            return 0;
        }
        
        log.info("【孤儿文件清理】发现 {} 个孤儿文件", orphanFiles.size());
        
        int deletedCount = 0;
        for (AppFile file : orphanFiles) {
            try {
                // 1. 从 MinIO 删除物理文件
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(file.getObjectName())
                                .build()
                );
                
                // 2. 从数据库删除记录
                appFileMapper.deleteByPrimaryKey(file.getId());
                
                deletedCount++;
                log.debug("【孤儿文件清理】成功删除孤儿文件，fileId: {}, objectName: {}, entityType: {}, entityId: {}", 
                        file.getId(), file.getObjectName(), file.getEntityType(), file.getRelatedEntityId());
                
            } catch (Exception e) {
                log.error("【孤儿文件清理】删除孤儿文件失败，fileId: {}, objectName: {}", 
                        file.getId(), file.getObjectName(), e);
            }
        }
        
        return deletedCount;
    }
}
