package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.domain.dto.FrontendLogRequestDTO;
import com.cqwu.cqwu_appointment_system.domain.query.FrontendLogQuery;
import com.cqwu.cqwu_appointment_system.entity.FrontendLogWithBLOBs;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

/**
 * @description 负责提供日志相关服务的接口。
 *              包括查询后端日志文件和存储前端上报日志。
 */

public interface LogService {

    /**
     * 从指定的后端日志文件中读取日志。
     *
     * @param level           要查询的日志级别 (debug, info, warn, error)。
     * @param lines           要读取的行数。
     * @param beforeTimestamp 如果为 null, 则读取最新的 N 行；否则读取该时间点之前的 N 行。
     * @return 包含日志内容的字符串列表。
     * @throws IOException              如果文件读取失败。
     * @throws IllegalArgumentException 如果日志级别无效。
     */
    List<String> getBackendLogs(String level, int lines, LocalDateTime beforeTimestamp) throws IOException;

    /**
     * 保存一条从前端上报的日志。
     * 包含滚动删除旧日志的逻辑。
     * 
     * @param dto       包含日志信息的 DTO。
     * @param userId    操作用户的ID (可为 null)。
     * @param userAgent 用户的 User-Agent (可为 null)。
     */
    void saveFrontendLog(FrontendLogRequestDTO dto, String userId, String userAgent);

    /**
     * 批量保存前端上报的日志。需要保证单用户的日志存储量不超过上限：
     * - 若现有数量 + 本次批量 > 上限，则优先删除该用户最旧的历史日志；
     * - 若仍超过（即本次批量条数本身就超过上限），仅保留本次批量中最新的上限条。
     *
     * @param list      前端日志请求列表
     * @param userId    用户ID（可为匿名）
     * @param userAgent User-Agent
     */
    void saveFrontendLogs(List<FrontendLogRequestDTO> list, String userId, String userAgent);

    /**
     * 根据条件分页查询前端上报的日志。
     *
     * @param query 包含所有查询条件和分页信息的对象 (pageNum 为 0-based)。
     * @return Spring Data Page 格式的日志列表。
     */
    Page<FrontendLogWithBLOBs> queryFrontendLogs(FrontendLogQuery query);
}