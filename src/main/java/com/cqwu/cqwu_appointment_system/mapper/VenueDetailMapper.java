package com.cqwu.cqwu_appointment_system.mapper;

import com.cqwu.cqwu_appointment_system.entity.VenueDetail;
import org.apache.ibatis.annotations.*;

/**
 * 场所扩展信息 Mapper
 */
@Mapper
public interface VenueDetailMapper {

    /**
     * 插入场所扩展信息
     */
    @Insert("INSERT INTO venue_detail (venue_id, description) " +
            "VALUES (#{venueId}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VenueDetail detail);

    /**
     * 根据场所ID查询扩展信息
     */
    @Select("SELECT * FROM venue_detail WHERE venue_id = #{venueId}")
    VenueDetail selectByVenueId(Long venueId);

    /**
     * 更新场所扩展信息
     */
    @Update("UPDATE venue_detail SET description = #{description} WHERE venue_id = #{venueId}")
    int updateByVenueId(VenueDetail detail);

    /**
     * 根据场所ID删除扩展信息
     */
    @Delete("DELETE FROM venue_detail WHERE venue_id = #{venueId}")
    int deleteByVenueId(Long venueId);

    /**
     * 根据场所ID列表批量删除
     */
    @Delete("<script>" +
            "DELETE FROM venue_detail WHERE venue_id IN " +
            "<foreach collection='array' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByVenueIds(Long[] venueIds);

    /**
     * 插入或更新（如果已存在则更新）
     */
    @Insert("INSERT INTO venue_detail (venue_id, description) " +
            "VALUES (#{venueId}, #{description}) " +
            "ON DUPLICATE KEY UPDATE description = #{description}")
    int insertOrUpdate(VenueDetail detail);
}
