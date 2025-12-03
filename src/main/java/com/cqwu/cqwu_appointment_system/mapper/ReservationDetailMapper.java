package com.cqwu.cqwu_appointment_system.mapper;

import com.cqwu.cqwu_appointment_system.entity.ReservationDetail;
import org.apache.ibatis.annotations.*;

/**
 * 预约扩展信息 Mapper
 */
@Mapper
public interface ReservationDetailMapper {

    /**
     * 插入预约扩展信息
     */
    @Insert("INSERT INTO reservation_detail (reservation_id, participants, participant_count) " +
            "VALUES (#{reservationId}, #{participants}, #{participantCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReservationDetail detail);

    /**
     * 根据预约ID查询扩展信息
     */
    @Select("SELECT * FROM reservation_detail WHERE reservation_id = #{reservationId}")
    ReservationDetail selectByReservationId(Long reservationId);

    /**
     * 更新预约扩展信息
     */
    @Update("UPDATE reservation_detail SET " +
            "participants = #{participants}, " +
            "participant_count = #{participantCount} " +
            "WHERE reservation_id = #{reservationId}")
    int updateByReservationId(ReservationDetail detail);

    /**
     * 根据预约ID删除扩展信息
     */
    @Delete("DELETE FROM reservation_detail WHERE reservation_id = #{reservationId}")
    int deleteByReservationId(Long reservationId);

    /**
     * 根据预约ID列表批量删除
     */
    @Delete("<script>" +
            "DELETE FROM reservation_detail WHERE reservation_id IN " +
            "<foreach collection='list' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByReservationIds(@Param("list") java.util.List<Long> reservationIds);
}
