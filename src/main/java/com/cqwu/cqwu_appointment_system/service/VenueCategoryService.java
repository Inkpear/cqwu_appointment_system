package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import java.util.List;

/**
 * @description 场所分类 (VenueCategory)
 */
public interface VenueCategoryService {

    /**
     * 创建一个新的场所分类。
     * 
     * @param venueCategory 要创建的分类对象
     * @return 影响的行数
     */
    int createVenueCategory(VenueCategory venueCategory);

    /**
     * 根据ID删除一个场所分类。
     * 
     * @param id 要删除的分类ID
     * @return 影响的行数
     */
    int deleteVenueCategory(Long id);

    /**
     * 更新一个场所分类的信息。
     * 
     * @param venueCategory 包含更新信息的分类对象
     * @return 影响的行数
     */
    int updateVenueCategory(VenueCategory venueCategory);

    /**
     * 根据ID查询单个场所分类。
     * 
     * @param id 分类ID
     * @return VenueCategory 对象，如果不存在则返回 null
     */
    VenueCategory findVenueCategoryById(Long id);

    /**
     * 查询所有的场所分类列表。
     * 
     * @return VenueCategory 的列表
     */
    List<VenueCategory> findAllCategories();
}