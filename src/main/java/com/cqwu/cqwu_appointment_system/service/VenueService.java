package com.cqwu.cqwu_appointment_system.service;

import java.util.List;

import org.springframework.data.domain.Page;

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
     * @param name 场所名字
     * @return 场所信息
     */
    public List<Venue> selectVenueListByName(String name); 

    /**
     * 
     * @param status 场所状态
     * @return 场所列表
     */
    public List<Venue> selectVenueListByStatus(String status);

    /**
     * 
     * @param localtion
     * @return 场所列表
     */
    public List<Venue> selectBenueListByLocation(String localtion);

    /**
     * 
     * @param venue 场所信息
     * @return 更新成功标识
     */
    public int updateVenue(Venue venue);

    /**
     * 
     * @param venue 场所信息
     * @return 返回Id
     */
    public int insertVenue(Venue venue);

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
    Page<Venue> searchVenues(String name, String status, String location, int page, int size);
}
