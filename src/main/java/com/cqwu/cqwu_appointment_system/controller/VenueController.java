package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.common.converter.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    @Autowired
    private VenueService venueService;

    @GetMapping("/search")
    public AjaxResult searchVenues(VenueQuery query) {
        if (query.getPage() == null) {
            query.setPage(Constants.DEFAULT_PAGE);
        }
        if (query.getSize() == null) {
            query.setSize(Constants.DEFAULT_PAGE_SIZE);
        }
        PageDTO<Venue> venuePage = PageDTO.fromPage(venueService.searchVenues(query));

        return AjaxResult.success(Constants.QUERY_SUCCESS, venuePage);
    }

    @GetMapping("/{id}")
    public AjaxResult getVenueById(@PathVariable Long id) {
        Venue venue = venueService.selectVenueById(id);
        return venue != null ? AjaxResult.success(Constants.QUERY_SUCCESS, venue)
                : AjaxResult.error(HttpStatus.NOT_FOUND, Constants.QUERY_NOT_FOUND);
    }

    @PostMapping
    public AjaxResult createVenues(@RequestBody List<@Valid Venue> venues) {
        if (venues == null || venues.isEmpty()) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, Constants.PARAM_IS_EMPTY);
        }
        int successCount = venueService.insertVenues(venues);
        return successCount > 0 ? new AjaxResult(HttpStatus.CREATED, String.format("成功添加 %d 个场所", successCount), venues)
                : AjaxResult.error(HttpStatus.ERROR, Constants.BATCH_CREATE_FAIL);
    }

    @PutMapping("/{id}")
    public AjaxResult updateVenue(@PathVariable Long id, @RequestBody Venue venue) {
        if (venueService.selectVenueById(id) == null) {
            return AjaxResult.error(HttpStatus.NOT_FOUND, Constants.UPDATE_FAIL);
        }
        venue.setId(id);
        int result = venueService.updateVenue(venue);
        return result > 0 ? AjaxResult.success(Constants.UPDATE_SUCCESS)
                : AjaxResult.success(Constants.DATA_NOT_CHANGED);
    }

    @DeleteMapping("/{ids}")
    public AjaxResult deleteVenues(@PathVariable Long[] ids) {
        if (ids == null || ids.length == 0) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, Constants.PARAM_ID_IS_EMPTY);
        }
        int result = venueService.deleteVenueByIds(ids);
        return result > 0 ? AjaxResult.success(String.format("成功删除 %d 条记录", result))
                : AjaxResult.error(HttpStatus.NOT_FOUND, "没有找到与提供ID匹配的记录进行删除");
    }
}