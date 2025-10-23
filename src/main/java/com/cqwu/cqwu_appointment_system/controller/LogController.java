package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.LoginUser;
import com.cqwu.cqwu_appointment_system.domain.dto.FrontendLogRequestDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.query.FrontendLogQuery;
import com.cqwu.cqwu_appointment_system.entity.FrontendLogWithBLOBs;
import com.cqwu.cqwu_appointment_system.service.LogService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 负责处理日志相关的API请求。
 *              仅限 ROOT 权限用户访问。
 */
@RestController
@RequestMapping("/api/logs")
@Slf4j
public class LogController {

    @Autowired
    private LogService logService;

    /**
     * [ROOT] 查询后端指定级别的日志文件。
     *
     * @param level           日志级别 (debug, info, warn, error)。
     * @param lines           要获取的行数，默认为 100。
     * @param beforeTimestamp 可选参数。如果提供，则查询该时间点之前的日志。
     * @return 成功时，返回包含日志列表的 ResponseEntity；失败时，由全局异常处理器统一响应。
     * @throws IOException              如果在读取日志文件时发生 I/O 错误。
     * @throws IllegalArgumentException 如果提供的日志级别无效。
     */
    @GetMapping("/backend")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<AjaxResult> getBackendLogs(
            @RequestParam("level") String level,
            @RequestParam(value = "lines", defaultValue = "100") int lines,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTimestamp)
            throws IOException {

        List<String> logs = logService.getBackendLogs(level, lines, beforeTimestamp);
        return ResponseEntity.ok(AjaxResult.success(logs));
    }

    /**
     * 接收并存储前端上报的日志。
     *
     * @param logRequestDTO 包含日志信息的请求体。
     * @param loginUser     当前登录的用户信息，由 Spring Security 注入。
     * @param userAgent     请求头中的 User-Agent。
     * @return 成功时，返回操作成功的响应；失败时，由全局异常处理器统一响应。
     */
    @PostMapping("/frontend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> reportFrontendLog(
            @RequestBody @Valid FrontendLogRequestDTO logRequestDTO,
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestHeader("User-Agent") String userAgent) {

        String userId = loginUser.getUser().getId();
        logService.saveFrontendLog(logRequestDTO, userId, userAgent);

        return ResponseEntity.ok(AjaxResult.success("前端日志上报成功。"));
    }

    /**
     * 批量接收并存储前端上报的日志。
     * 会严格遵守“单用户最大存储上限”，按需淘汰最旧日志或裁剪本次批量。
     * 
     * @param logRequests 包含多个日志记录的请求体列表。
     * @param loginUser   当前登录的用户信息，由 Spring Security 注入。
     * @param userAgent   请求头中的 User-Agent。
     * @return 成功时，返回操作成功的响应；失败时，由全局异常处理器统一响应。
     */
    @PostMapping("/frontend/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> reportFrontendLogs(
            @RequestBody @Valid List<FrontendLogRequestDTO> logRequests,
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestHeader("User-Agent") String userAgent) {

        String userId = loginUser.getUser().getId();
        logService.saveFrontendLogs(logRequests, userId, userAgent);

        return ResponseEntity.ok(AjaxResult.success("前端日志批量上报成功。"));
    }

    /**
     * [ROOT] 分页和筛选查询前端上报的日志。
     *
     * @param query 包含所有筛选和分页参数的查询对象
     * @return 封装了日志列表和分页信息的成功响应。
     */
    @GetMapping("/frontend/query")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<AjaxResult> queryFrontendLogs(@Valid FrontendLogQuery query) {
        Page<FrontendLogWithBLOBs> logPage = logService.queryFrontendLogs(query);

        PageDTO<FrontendLogWithBLOBs> logPageDTO = PageDTO.fromPage(logPage);

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, logPageDTO));
    }
}