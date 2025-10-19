package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.TimeBlock;
import com.cqwu.cqwu_appointment_system.service.TimeBlockService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sync/schedule")
@Slf4j
@PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
public class ScheduleSyncController {

    @Autowired
    private TimeBlockService timeBlockService;

    /**
     * 强制批量插入课程安排。
     * 此接口会覆盖所有冲突的用户预约。
     */
    @PostMapping("/courses")
    public ResponseEntity<AjaxResult> syncCourses(@RequestBody List<@Valid TimeBlock> courseTimeBlocks) {
        log.warn("接收到课程同步请求，准备强制插入 {} 条课程记录...", courseTimeBlocks.size());
        
        courseTimeBlocks = courseTimeBlocks.stream()
            .filter(course -> course.getBlockType().equals(Constants.TimeBlockType.COURSE))
            .collect(Collectors.toList());
        
        int count = timeBlockService.forceInsertCourseTimeBlocks(courseTimeBlocks);
        return ResponseEntity.ok(AjaxResult.success(String.format("成功同步 %d 条课程记录", count)));
    }

    /**
     * 清空指定场所的所有课程安排。
     */
    @DeleteMapping("/courses")
    public ResponseEntity<AjaxResult> clearCourses(@RequestBody ClearCoursesRequest request) {
        log.warn("接收到课程清空请求，准备清空 {} 个场所的课程...", request.getVenueIds().size());
        int count = timeBlockService.deleteAllCourseTimeBlocksByVenueIds(request.getVenueIds());
        return ResponseEntity.ok(AjaxResult.success(String.format("成功清空 %d 条课程记录", count)));
    }

    /**
     * 用于 clearCourses 接口的请求体 DTO
     */
    @Data
    static class ClearCoursesRequest {
        private List<Long> venueIds;
    }
}