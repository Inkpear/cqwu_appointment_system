package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
@Slf4j
public class VenueController {

    @Autowired
    private VenueService venueService;

    /**
     * 分页搜索场所信息
     */
    @GetMapping
    public ResponseEntity<AjaxResult> searchVenues(VenueQuery query) {
        log.info("开始搜索场所, 查询参数: {}", query);

        PageDTO<Venue> venuePage = PageDTO.fromPage(venueService.searchVenues(query));

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, venuePage));
    }

    /**
     * 根据ID获取单个场所信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<AjaxResult> getVenueById(@PathVariable Long id) {
        log.info("开始查询ID为 '{}' 的场所信息...", id);
        Venue venue = venueService.selectVenueById(id);
        if (venue != null) {
            return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, venue));
        } else {
            log.warn("未找到ID为 '{}' 的场所", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.QUERY_NOT_FOUND));
        }
    }

    /**
     * 批量创建场所
     * 
     * @Valid 校验失败由 GlobalExceptionHandler 统一处理。
     */
    @PostMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> createVenues(@RequestBody List<@Valid Venue> venues) {
        log.info("开始批量创建场所, 数量: {}", venues != null ? venues.size() : 0);
        if (venues == null || venues.isEmpty()) {
            log.warn("创建场所失败：请求体为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, Constants.PARAM_IS_EMPTY));
        }

        int successCount = venueService.insertVenues(venues);
        if (successCount > 0) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AjaxResult(HttpStatus.CREATED, String.format("成功添加 %d 个场所", successCount), venues));
        } else {
            log.warn("批量创建场所操作未影响任何行，可能是数据重复或其他业务约束导致");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, Constants.BATCH_CREATE_FAIL));
        }
    }

    /**
     * 更新指定ID的场所信息
     * 优化了“先查后改”模式，现在更高效。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> updateVenue(@PathVariable Long id, @RequestBody @Valid Venue venue) {
        log.info("开始更新ID为 '{}' 的场所信息...", id);
        venue.setId(id);

        int result = venueService.updateVenue(venue);

        if (result > 0) {
            log.info("成功更新ID为 '{}' 的场所", id);
            return ResponseEntity.ok(AjaxResult.success(Constants.UPDATE_SUCCESS));
        } else {
            if (venueService.selectVenueById(id) == null) {
                log.warn("更新失败: 未找到ID为 '{}' 的场所", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.UPDATE_FAIL_NOT_FOUND));
            } else {
                log.info("ID为 '{}' 的场所信息未发生变化", id);
                return ResponseEntity.ok(AjaxResult.success(Constants.DATA_NOT_CHANGED));
            }
        }
    }

    /**
     * 根据ID批量删除场所
     */
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ADMIN) or hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteVenues(@PathVariable Long[] ids) {
        log.info("开始删除场所, IDs: {}", Arrays.toString(ids));
        if (ids == null || ids.length == 0) {
            log.warn("删除场所失败：ID列表为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error(HttpStatus.BAD_REQUEST, Constants.PARAM_ID_IS_EMPTY));
        }

        int result = venueService.deleteVenueByIds(ids);

        if (result > 0) {
            log.info("成功删除 {} 条场所记录", result);
            return ResponseEntity.ok(AjaxResult.success(String.format("成功删除 %d 条记录", result)));
        } else {
            log.warn("删除操作未影响任何记录, 可能ID '{}' 对应的资源不存在", Arrays.toString(ids));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.DELETE_FAIL_NOT_FOUND));
        }
    }
}