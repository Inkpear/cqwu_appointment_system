package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.domain.dto.CalendarEventDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueDetailDto;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueName;
import com.cqwu.cqwu_appointment_system.domain.dto.VenueStatisticsDto;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description 场所信息管理服务接口，定义了对场所资源的增删改查等核心操作。
 */
public interface VenueService {

    /**
     * 根据主键ID查询单个场所的详细信息。
     *
     * @param id 场所的唯一ID。
     * @return 匹配的 Venue 实体，如果不存在则返回 null。
     */
    Venue selectVenueById(Long id);

    /**
     * 更新单个场所的信息。
     * 这是一个选择性更新，只会更新 Venue 对象中非null的字段。
     *
     * @param venue 包含待更新字段的 Venue 实体，ID 不能为空。
     * @return 成功更新的记录数 (通常为 1 或 0)。
     */
    int updateVenue(Venue venue);

    /**
     * 批量新增场所信息。
     *
     * @param venues 包含待新增场所信息的列表。
     * @return 成功插入的记录数。
     */
    int insertVenues(List<Venue> venues);

    /**
     * 根据ID列表批量删除场所。
     *
     * @param ids 包含一个或多个场所ID的数组。
     * @return 成功删除的记录数。
     */
    int deleteVenueByIds(Long[] ids);

    /**
     * 根据动态查询条件分页搜索场所列表。
     *
     * @param query 封装了过滤条件和分页参数的查询对象。
     * @return 封装了查询结果和分页信息的 Spring Data Page 对象。
     */
    Page<Venue> searchVenues(VenueQuery query);

    /**
     * [内部调用] 批量更新场所的实时状态。
     * 此方法专为 VenueStatusUpdater 定时任务设计。
     *
     * @param venuesToUpdate 包含ID和新realtimeStatus的Venue对象列表。
     * @return 成功更新的记录数。
     */
    int updateRealtimeStatusBatch(List<Venue> venuesToUpdate);

    /**
     * 将上传的附件关联到场所
     *
     * @param venueId   场所ID
     * @param sessionId 上传会话ID
     */
    void attachFilesToVenue(Long venueId, String sessionId);

    /**
     * 批量更新多个场所的通用信息。
     * 使用一份更新数据，应用到所有指定的ID上。
     *
     * @param ids             要更新的场所ID数组。
     * @param venueUpdateData 包含待更新字段的 Venue 实体。
     * @return 成功更新的记录数。
     */
    int updateVenuesBatch(Long[] ids, Venue venueUpdateData);

    /**
     * 获取指定场所在特定时间范围内的所有日程安排，并标记是否为当前用户所有。
     *
     * @param venueId     场所ID
     * @param start       查询开始时间
     * @param end         查询结束时间
     * @param currentUser 当前登录的用户
     * @return 适用于前端日历的事件 DTO 列表
     */
    List<CalendarEventDTO> getVenueScheduleForUser(Long venueId, LocalDateTime start, LocalDateTime end,
            User currentUser);

    /**
     * 获取场所统计信息
     * 用于展示场所的热门程度和使用情况
     *
     * @param venueId 场所ID
     * @return 场所统计信息
     */
    VenueStatisticsDto getVenueStatistics(Long venueId);

    /**
     * 获取所有场所的统计信息（用于数据导出）
     *
     * @return 所有场所的统计信息列表
     */
    List<VenueStatisticsDto> getAllVenuesStatistics();

    /**
     * 根据ID查询场所详情（包含扩展信息）
     *
     * @param id 场所ID
     * @return 场所详情DTO（包含描述等扩展信息）
     */
    VenueDetailDto getVenueDetailById(Long id);

    /**
     * 更新场所扩展信息（描述）
     *
     * @param venueId 场所ID
     * @param description 场所描述（支持Markdown）
     */
    void updateVenueDescription(Long venueId, String description);

    /**
     * 根据ID列表获取场所的ID和名称
     *
     * @param ids 场所ID列表
     * @return 包含ID和名称的场所列表
     */
    List<Venue> getVenuesByIds(List<Long> ids);

    /**
     * 获取所有场所的ID和名称
     *
     * @return 包含ID和名称的场所列表
     */
    List<VenueName> getAllVenueNames();
}
