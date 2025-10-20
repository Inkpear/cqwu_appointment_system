package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryRuleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venue-category-rules")
@Slf4j
public class VenueCategoryRuleController {

    @Autowired
    private VenueCategoryRuleService ruleService;

    /**
     * 根据分类ID，获取其下所有的准入规则
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<AjaxResult> getRulesForCategory(@PathVariable Long categoryId) {
        log.info("查询分类 ID: {} 的所有准入规则", categoryId);
        List<VenueCategoryRule> rules = ruleService.findRulesByCategoryId(categoryId);
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, rules));
    }

    /**
     * 为一个分类添加一条新的准入规则
     */
    @PostMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> addRule(@RequestBody @Valid VenueCategoryRule rule) {
        log.info("正在添加新的准入规则: {}", rule);
        int result = ruleService.addRule(rule);
        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AjaxResult.success(Constants.CREATE_SUCCESS, rule));
        } else {
            return ResponseEntity.status(HttpStatus.ERROR)
                    .body(AjaxResult.error(HttpStatus.ERROR, "添加规则失败"));
        }
    }

    /**
     * 根据规则ID，删除一条准入规则
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteRule(@PathVariable Long ruleId) {
        log.info("正在删除准入规则, ID: {}", ruleId);
        int result = ruleService.deleteRule(ruleId);
        if (result > 0) {
            return ResponseEntity.ok(AjaxResult.success(Constants.DELETE_SUCCESS));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, "未找到要删除的规则"));
        }
    }
}