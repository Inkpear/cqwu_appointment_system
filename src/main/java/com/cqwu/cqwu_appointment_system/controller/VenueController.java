package com.cqwu.cqwu_appointment_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.common.converter.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.VenueService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/venues")
public class VenueController {

    @Autowired
    private VenueService venueService;

    @GetMapping("/search")
    public AjaxResult searchVenues(VenueQuery query) {
        // 如果前端没有传递 page 参数，query.getPage() 会是 null
        if (query.getPage() == null) {
            query.setPage(Constants.DEFAULT_PAGE);
        }
        // 如果前端没有传递 size 参数，query.getSize() 会是 null
        if (query.getSize() == null) {
            query.setSize(Constants.DEFAULT_PAGE_SIZE);
        }
        // 调用 service 获取分页数据
        PageDTO<Venue> venuePage = PageDTO.fromPage(venueService.searchVenues(query));

        return AjaxResult.success("查询成功", venuePage);
    }

    /**
     * 根据ID获取单个场所的详细信息
     * GET /api/venues/1
     */
    @GetMapping("/{id}")
    public AjaxResult getVenueById(@PathVariable Long id) {
        Venue venue = venueService.selectVenueById(id);

        if (venue != null) {
            return AjaxResult.success(Constants.QUERY_SUCCESS, venue);
        }
        return AjaxResult.success(Constants.QUERY_FAIL);
    }

    /**
     * 新增一个场所
     * POST /api/venues
     * 请求体 (Request Body) 中应包含场所的 JSON 数据
     */
    @PostMapping
    public AjaxResult createVenue(@Valid @RequestBody Venue venue) {
        // @RequestBody 注解告诉 Spring MVC 从请求体中解析 JSON 数据并绑定到 Venue 对象
        int result = venueService.insertVenue(venue);
        if (result > 0) {
            return AjaxResult.success(Constants.CREATE_SUCCESS, venue);
        }
        return AjaxResult.success(Constants.CREATE_FAIL);
    }

    /**
     * 更新一个已存在的场所信息
     * PUT /api/venues/{id}
     * 请求体中应包含要更新的字段的 JSON 数据
     */
    @PutMapping("/{id}")
    public AjaxResult updateVenue(@PathVariable Long id, @RequestBody Venue venue) {
        // 检查要更新的资源是否存在
        if (venueService.selectVenueById(id) == null) {
            return AjaxResult.error(HttpStatus.NOT_FOUND, "要更新的场所不存在");
        }

        // 将路径变量中的 id 设置到 venue 对象中，防止请求体中的 id 与路径不一致
        venue.setId(id);
        int result = venueService.updateVenue(venue);

        if (result > 0) {
            return AjaxResult.success("更新成功");
        } else {
            // 可能是因为数据没有实际变化，或者并发问题导致更新了0行
            return AjaxResult.warn("数据未发生变化或更新失败");
        }
    }

    /**
     * 根据ID批量删除场所
     * DELETE /api/venues/1,2,3
     */
    @DeleteMapping("/{ids}")
    public AjaxResult deleteVenues(@PathVariable Long[] ids) {
        if (ids == null || ids.length == 0) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST,"至少需要提供一个要删除的ID");
        }

        int result = venueService.deleteVenueByIds(ids);

        String message = result > 0? ("成功删除 " + result + " 条记录"): "没有找到与提供ID匹配的记录进行删除";
        
        return AjaxResult.success(message);
    }
}