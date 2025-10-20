package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description 负责处理与外部日程（如教务系统课程表）同步相关的API请求。
 *              所有接口都需要 ROOT 权限。
 */
@RestController
@RequestMapping("/api/sync/schedule")
@Slf4j
@PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
public class ScheduleSyncController {

    @Autowired
    private TimeBlockService timeBlockService;

    /**
     * [ROOT] 强制批量插入课程安排。
     * 此接口会覆盖所有时间冲突的用户预约。
     *
     * @param courses DTO 列表，包含课程信息
     * @return 操作结果
     */
    @PostMapping("/courses")
    public ResponseEntity<AjaxResult> syncCourses(@RequestBody @Valid List<@Valid SyncCourseRequestDTO> courses) {
        log.warn("接收到课程同步请求，准备强制插入 {} 条课程记录...", courses.size());

        // 将 DTO 列表转换为 Entity 列表
        List<TimeBlock> courseTimeBlocks = courses.stream()
                .map(SyncCourseRequestDTO::toEntity)
                .collect(Collectors.toList());

        int count = timeBlockService.forceInsertCourseTimeBlocks(courseTimeBlocks);
        return ResponseEntity.ok(AjaxResult.success(String.format("成功同步 %d 条课程记录", count)));
    }

    /**
     * [ROOT] 清空指定场所的所有课程安排。
     *
     * @param request 包含 venueIds 列表的请求体
     * @return 操作结果
     */
    @DeleteMapping("/courses")
    public ResponseEntity<AjaxResult> clearCourses(@RequestBody @Valid ClearCoursesRequest request) {
        log.warn("接收到课程清空请求，准备清空 {} 个场所的课程...", request.getVenueIds().size());
        int count = timeBlockService.deleteAllCourseTimeBlocksByVenueIds(request.getVenueIds());
        return ResponseEntity.ok(AjaxResult.success(String.format("成功清空 %d 条课程记录", count)));
    }

    /**
     * @description 用于课程同步接口的请求体 DTO。
     */
    @Data
    public static class SyncCourseRequestDTO {
        @NotNull(message = "场所ID不能为空")
        private Long venueId;

        @NotNull(message = "开始时间不能为空")
        private LocalDateTime startDatetime;

        @NotNull(message = "结束时间不能为空")
        private LocalDateTime endDatetime;

        @NotBlank(message = "标题不能为空")
        private String title;

        public TimeBlock toEntity() {
            TimeBlock tb = new TimeBlock();
            tb.setVenueId(this.venueId);
            tb.setStartDatetime(this.startDatetime);
            tb.setEndDatetime(this.endDatetime);
            tb.setTitle(this.title);
            // 关键：由后端强制设定类型，而不是信任客户端
            tb.setBlockType(Constants.TimeBlockType.COURSE);
            return tb;
        }
    }

    /**
     * @description 用于清空课程接口的请求体 DTO。
     */
    @Data
    static class ClearCoursesRequest {
        @NotEmpty(message = "要清空课程的场所ID列表不能为空")
        private List<Long> venueIds;
    }
}