package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryRuleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @description 负责处理场所分类准入规则 (VenueCategoryRule) 相关的API请求。
 */
@RestController
@RequestMapping("/api/venue-category-rules")
@Slf4j
public class VenueCategoryRuleController {
    @Autowired
    private VenueCategoryRuleService venueCategoryRuleService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取所有场所分类准入规则
     * 
     * @return
     */
    @GetMapping
    public ResponseEntity<AjaxResult> getAllVenueCategoryRules() {
        return ResponseEntity.ok(AjaxResult.success(venueCategoryRuleService.getAllVenueCategoryRules()));
    }

    /**
     * [管理员] 为一个分类添加一条新的准入规则。
     * Service层会校验分类是否存在以及规则是否重复。
     *
     * @param rule 待添加的规则实体
     * @return 包含新创建的规则的 ResponseEntity
     */
    @PostMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> addRule(@RequestBody @Valid VenueCategoryRule rule) {
        log.info("正在添加新的准入规则: {}", rule);

        // Service层会在失败时抛出异常，由GlobalExceptionHandler处理
        venueCategoryRuleService.addRule(rule);

        notificationService.sendNotification(
                Constants.SseBroadcast.ALL_USERS,
                Constants.SseEvent.VENUE_CATEGORY_RULE_UPDATED,
                "list_updated");

        // 返回 201 Created，并使用 created 方法确保响应体内 code 也为 201
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.created(Constants.CREATE_SUCCESS, rule));
    }

    /**
     * [管理员] 根据规则ID，删除一条准入规则。
     *
     * @param ruleId 要删除的规则ID
     * @return 操作成功的 ResponseEntity
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteRule(@PathVariable Long ruleId) {
        log.info("正在删除准入规则, ID: {}", ruleId);

        int result = venueCategoryRuleService.deleteRule(ruleId);

        if (result == 0) {
            // 如果 service 返回0，说明未找到该ID，主动抛出异常
            throw new IllegalArgumentException("未找到要删除的规则");
        }

        notificationService.sendNotification(
                Constants.SseBroadcast.ALL_USERS,
                Constants.SseEvent.VENUE_CATEGORY_RULE_UPDATED,
                "list_updated");

        return ResponseEntity.ok(AjaxResult.success(Constants.DELETE_SUCCESS));
    }
}