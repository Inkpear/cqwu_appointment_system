package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.dto.BatchDeleteRequest;
import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule;
import com.cqwu.cqwu_appointment_system.service.NotificationService;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryRuleService;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @description 负责处理场所分类 (VenueCategory) 相关的API请求。
 */
@RestController
@RequestMapping("/api/venue-categories")
@Slf4j
public class VenueCategoryController {
    @Autowired
    private VenueCategoryService venueCategoryService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VenueCategoryRuleService venueCategoryRuleService;

    /**
     * 获取所有场所分类的列表。
     *
     * @return 包含所有场所分类的列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> listAllCategories() {
        log.info("查询所有分类列表");
        List<VenueCategory> categories = venueCategoryService.findAllCategories();
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, categories));
    }

    /**
     * 根据ID获取单个场所分类的详细信息。
     *
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getCategoryById(@PathVariable Long id) {
        log.info("查询分类详情, ID: {}", id);
        VenueCategory category = venueCategoryService.findVenueCategoryById(id);
        if (category == null) {
            throw new IllegalArgumentException(Constants.QUERY_NOT_FOUND);
        }
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, category));
    }

    /**
     * [管理员] 创建一个新的场所分类。
     *
     * @param venueCategory 待创建的分类实体
     * @return 包含新创建的分类的 ResponseEntity
     */
    @PostMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> createVenueCategory(@RequestBody @Valid VenueCategory venueCategory) {
        log.info("管理员正在创建新分类: {}", venueCategory.getName());

        venueCategoryService.createVenueCategory(venueCategory);
        notificationService.sendNotification(
                "all_users",
                Constants.SseEvent.VENUE_CATEGORY_CREATED,
                venueCategory);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AjaxResult.created(Constants.CREATE_SUCCESS, venueCategory));
    }

    /**
     * [管理员] 更新一个已存在的场所分类。
     *
     * @param id            分类ID
     * @param venueCategory 包含更新信息的分类实体
     * @return 操作成功的 ResponseEntity
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> updateVenueCategory(@PathVariable("id") Long id,
            @RequestBody @Valid VenueCategory venueCategory) {
        log.info("管理员正在更新分类, ID: {}", id);
        venueCategory.setId(id);

        venueCategoryService.updateVenueCategory(venueCategory);
        notificationService.sendNotification(
                "all_users",
                Constants.SseEvent.VENUE_CATEGORY_UPDATED,
                venueCategory);

        return ResponseEntity.ok(AjaxResult.success(Constants.UPDATE_SUCCESS));
    }

    /**
     * [管理员] 删除一个或多个场所分类。
     * Service层会检查该分类是否仍被使用。
     *
     * @param request 包含ID列表的请求体
     * @return 操作成功的 ResponseEntity
     */
    @DeleteMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteVenueCategories(@RequestBody BatchDeleteRequest request) {
        Long[] ids = request.getIds();
        log.info("管理员正在批量删除分类, IDs: {}", java.util.Arrays.toString(ids));

        for (Long id : ids) {
            venueCategoryService.deleteVenueCategory(id);
        }

        notificationService.sendNotification(
                "all_users",
                Constants.SseEvent.VENUE_CATEGORY_DELETED,
                ids);
        return ResponseEntity.ok(AjaxResult.success(String.format("成功删除 %d 个分类", ids.length)));
    }

    /**
     * 根据分类ID，获取其下所有的准入规则。
     *
     * @param categoryId 场所分类的ID
     * @return 该分类下的所有准入规则列表
     */
    @GetMapping("/{categoryId}/rules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getRulesForCategory(@PathVariable Long categoryId) {
        log.info("查询分类 ID: {} 的所有准入规则", categoryId);
        
        List<VenueCategoryRule> rules = venueCategoryRuleService.findRulesByCategoryId(categoryId);

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, rules));
    }
}