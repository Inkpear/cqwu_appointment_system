package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import java.util.List;

/**
 * @description 场所分类 (VenueCategory) 管理服务接口。
 *              负责场所分类的增删改查操作。
 */
public interface VenueCategoryService {

    /**
     * 创建一个新的场所分类。
     *
     * @param venueCategory 要创建的分类对象，ID应为null。
     * @return 成功插入的记录数 (通常为 1)。
     */
    int createVenueCategory(VenueCategory venueCategory);

    /**
     * 根据ID删除一个场所分类。
     * 实现时应确保该分类未被任何场所使用，并级联删除关联的准入规则。
     *
     * @param id 要删除的分类ID。
     * @return 成功删除的记录数 (通常为 1 或 0)。
     * @throws IllegalStateException 如果仍有场所正在使用此分类。
     */
    int deleteVenueCategory(Long id);

    /**
     * 更新一个已存在的场所分类信息。
     * 这是一个选择性更新，只会更新对象中非null的字段。
     *
     * @param venueCategory 包含更新信息的分类对象，ID 不能为空。
     * @return 成功更新的记录数 (通常为 1 或 0)。
     */
    int updateVenueCategory(VenueCategory venueCategory);

    /**
     * 根据ID查询单个场所分类的详细信息。
     *
     * @param id 分类ID。
     * @return VenueCategory 实体，如果不存在则返回 null。
     */
    VenueCategory findVenueCategoryById(Long id);

    /**
     * 查询所有的场所分类列表。
     *
     * @return 包含所有 VenueCategory 的列表，包含 TEXT 类型的 usage_rules 字段。
     */
    List<VenueCategory> findAllCategories();
}