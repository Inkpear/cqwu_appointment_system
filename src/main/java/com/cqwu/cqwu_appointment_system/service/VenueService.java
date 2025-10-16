package com.cqwu.cqwu_appointment_system.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;

public interface VenueService {

    /**
     *
     * @param name 场所Id
     * @return 场所信息
     */
    public Venue selectVenueById(Long id); 

    /**
     * 
     * @param venue 场所信息
     * @return 更新成功标识
     */
    public int updateVenue(Venue venue);

   /**
     * 批量新增场所
     * @param venues 场所信息列表
     * @return 成功插入的行数
     */
    int insertVenues(List<Venue> venues);

    /**
     * 
     * @param ids id列表
     * @return 删除状态
     */
    public int deleteVenueByIds(Long[] ids);


     /**
     * 分页并根据多个可选条件搜索场所
     * @return 分页后的场所数据
     */
    Page<Venue> searchVenues(VenueQuery query);
}
