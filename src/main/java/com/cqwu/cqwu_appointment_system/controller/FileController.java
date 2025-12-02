package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.EntityType;
import com.cqwu.cqwu_appointment_system.common.constant.FileCategory;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.domain.dto.GeneratePresignedUrlRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.PresignedUrlDto;
import com.cqwu.cqwu_appointment_system.entity.AppFile;
import com.cqwu.cqwu_appointment_system.entity.ReservationWithBLOBs;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.service.FileService;
import com.cqwu.cqwu_appointment_system.service.ReservationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 文件管理 Controller
 * 负责文件上传预签名、确认、查询、删除等操作
 */
@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private ReservationService reservationService;

    /**
     * 通用：创建文件上传会话
     * 支持预约附件、场所附件、场所轮播图等所有文件上传场景
     * 
     * @param entityType   实体类型：VENUE 或 RESERVATION
     * @param fileCategory 文件分类：ATTACHMENT 或 CAROUSEL
     * @return 会话ID（10分钟有效期）
     */
    @GetMapping("/upload-session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> createUploadSession(
            @RequestParam EntityType entityType,
            @RequestParam FileCategory fileCategory) {
        String sessionId = fileService.createUploadSession(entityType, fileCategory);
        log.info("创建文件上传会话，sessionId: {}, 实体类型: {}, 文件分类: {}", sessionId, entityType, fileCategory);

        return ResponseEntity.ok(
                AjaxResult.success("上传会话已创建", sessionId));
    }

    /**
     * 通用：生成文件上传的预签名 URL
     * 前端先调用 upload-session 获取 sessionId，再用此接口获取每个文件的上传URL
     *
     * @param request 上传请求（包含 sessionId、文件名、实体类型、文件分类）
     * @return 预签名上传 URL 和 objectName
     */
    @PostMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> generateUploadUrl(
            @RequestBody @Valid GeneratePresignedUrlRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        log.info("生成文件上传URL，sessionId: {}, 文件名: {}, 实体类型: {}, 文件分类: {}",
                request.getSessionId(), request.getOriginalFilename(), 
                request.getEntityType(), request.getFileCategory());

        if (EntityType.VENUE.equals(request.getEntityType())
                && !Constants.UserRoleType.ROLE_ADMIN.equals(loginUser.getUser().getRole())
                && !Constants.UserRoleType.ROLE_ROOT.equals(loginUser.getUser().getRole())) {
            throw new AccessDeniedException("权限不足");
        }

        PresignedUrlDto dto = fileService.generatePresignedUploadUrl(
                request.getOriginalFilename(),
                request.getSessionId(),
                request.getEntityType(),
                request.getFileCategory());

        return ResponseEntity.ok(
                AjaxResult.success(Constants.FileMessage.PRESIGNED_URL_GENERATED, dto));
    }

    /**
     * 删除附件（预约附件或场所文件）
     * 权限：预约创建者 或 管理员
     *
     * @param fileId    文件 ID
     * @param loginUser 当前登录用户
     * @return 操作结果
     */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal LoginUser loginUser) {
        log.info("用户 {} 尝试删除文件，fileId: {}", loginUser.getUser().getId(), fileId);

        User currentUser = loginUser.getUser();

        AppFile appFile = fileService.getFileById(fileId);
        if (appFile == null) {
            throw new IllegalArgumentException("文件不存在");
        }

        boolean isAdmin = Constants.UserRoleType.ROLE_ADMIN.equals(currentUser.getRole())
                || Constants.UserRoleType.ROLE_ROOT.equals(currentUser.getRole());

        if (EntityType.RESERVATION.name().equals(appFile.getEntityType())) {
            if (appFile.getRelatedEntityId() != null) {
                ReservationWithBLOBs reservation = reservationService.findReservationById(
                        appFile.getRelatedEntityId());
                if (reservation == null) {
                    throw new IllegalArgumentException("关联的预约不存在");
                }

                boolean isOwner = currentUser.getId().equals(reservation.getCreatorUserId());
                if (!isOwner && !isAdmin) {
                    throw new IllegalArgumentException("您没有权限删除此文件");
                }
            } else if (!isAdmin) {
                throw new IllegalArgumentException("您没有权限删除此文件");
            }
        } else if (EntityType.VENUE.name().equals(appFile.getEntityType())) {
            if (!isAdmin) {
                throw new IllegalArgumentException("只有管理员可以删除场所文件");
            }
        }

        fileService.deleteFile(fileId);
        log.info("文件删除成功，fileId: {}", fileId);

        return ResponseEntity.ok(AjaxResult.success(Constants.FileMessage.DELETE_SUCCESS));
    }
}
