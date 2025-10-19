package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venue-categories")
@Slf4j
public class VenueCategoryController {

    @Autowired
    private VenueCategoryService venueCategoryService;

    /**
     * 获取所有场所分类的列表
     *
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> listAllCategories() {
        log.info("查询所有分类列表");
        List<VenueCategory> categories = venueCategoryService.findAllCategories();
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, categories));
    }

    /**
     * 根据ID获取单个场所分类
     * (对所有登录用户开放)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getCategoryById(@PathVariable Long id) {
        log.info("查询分类详情, ID: {}", id);
        VenueCategory category = venueCategoryService.findVenueCategoryById(id);
        if (category != null) {
            return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, category));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.QUERY_NOT_FOUND));
        }
    }

    /**
     * 创建一个新的场所分类
     *
     */
    @PostMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> createCategory(@RequestBody @Valid VenueCategory venueCategory) {
        log.info("管理员正在创建新分类: {}", venueCategory.getName());
        int result = venueCategoryService.createVenueCategory(venueCategory);
        if (result > 0) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AjaxResult.success(Constants.CREATE_SUCCESS, venueCategory));
        } else {
            return ResponseEntity.status(HttpStatus.ERROR)
                    .body(AjaxResult.error(HttpStatus.ERROR, Constants.CREATE_FAIL));
        }
    }

    /**
     * 更新一个场所分类
     *
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> updateCategory(@PathVariable Long id,
            @RequestBody @Valid VenueCategory venueCategory) {
        log.info("管理员正在更新分类, ID: {}", id);

        venueCategory.setId(id);

        int result = venueCategoryService.updateVenueCategory(venueCategory);
        if (result > 0) {
            return ResponseEntity.ok(AjaxResult.success(Constants.UPDATE_SUCCESS));
        } else {
            if (venueCategoryService.findVenueCategoryById(id) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.UPDATE_FAIL_NOT_FOUND));
            } else {
                return ResponseEntity.ok(AjaxResult.success(Constants.DATA_NOT_CHANGED));
            }
        }
    }

    /**
     * 删除一个场所分类
     * 
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteCategory(@PathVariable Long id) {
        log.info("管理员正在删除分类, ID: {}", id);
        try {
            int result = venueCategoryService.deleteVenueCategory(id);
            if (result > 0) {
                return ResponseEntity.ok(AjaxResult.success(Constants.DELETE_SUCCESS));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.DELETE_FAIL_NOT_FOUND));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }
}