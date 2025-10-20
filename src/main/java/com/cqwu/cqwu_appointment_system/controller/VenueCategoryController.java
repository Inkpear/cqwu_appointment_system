package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule; // 新增引入
import com.cqwu.cqwu_appointment_system.service.VenueCategoryRuleService; // 新增注入
import com.cqwu.cqwu_appointment_system.service.VenueCategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description 负责处理场所分类 (VenueCategory) 相关的API请求。
 */
@RestController
@RequestMapping("/api/venue-categories")
@Slf4j
public class VenueCategoryController {

    @Autowired
    private VenueCategoryService venueCategoryService;

    // --- 新增注入 ---
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
    public ResponseEntity<AjaxResult> createCategory(@RequestBody @Valid VenueCategory venueCategory) {
        log.info("管理员正在创建新分类: {}", venueCategory.getName());

        venueCategoryService.createVenueCategory(venueCategory);

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
    public ResponseEntity<AjaxResult> updateCategory(@PathVariable Long id,
            @RequestBody @Valid VenueCategory venueCategory) {
        log.info("管理员正在更新分类, ID: {}", id);
        venueCategory.setId(id);

        int result = venueCategoryService.updateVenueCategory(venueCategory);
        if (result == 0) {
            throw new IllegalArgumentException(Constants.UPDATE_FAIL_NOT_FOUND);
        }

        return ResponseEntity.ok(AjaxResult.success(Constants.UPDATE_SUCCESS));
    }

    /**
     * [管理员] 删除一个场所分类。
     * Service层会检查该分类是否仍被使用。
     *
     * @param id 分类ID
     * @return 操作成功的 ResponseEntity
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteCategory(@PathVariable Long id) {
        log.info("管理员正在删除分类, ID: {}", id);

        // Service层会在删除失败（如分类被占用）时抛出 IllegalStateException，由 GlobalExceptionHandler 捕获
        int result = venueCategoryService.deleteVenueCategory(id);
        if (result == 0) {
            throw new IllegalArgumentException(Constants.DELETE_FAIL_NOT_FOUND);
        }

        return ResponseEntity.ok(AjaxResult.success(Constants.DELETE_SUCCESS));
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