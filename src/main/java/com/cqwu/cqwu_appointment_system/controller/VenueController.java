package com.cqwu.cqwu_appointment_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cqwu.cqwu_appointment_system.common.converter.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.VenueService;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    @Autowired
    private VenueService venueService;

    @GetMapping("/search")
    public AjaxResult searchVenues(
            // @RequestParam(required = false) 表示这个参数是可选的
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page, // 默认请求第0页
            @RequestParam(defaultValue = "10") int size  // 默认每页10条
    ) {
        // 调用 service 获取分页数据
        PageDTO<Venue> venuePage = PageDTO.fromPage(venueService.searchVenues(name, status, location, page, size));

        return AjaxResult.success(venuePage);
    }
}