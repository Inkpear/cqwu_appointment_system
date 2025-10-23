package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule;
import java.util.List;

/**
 * @description 场所分类准入规则 (VenueCategoryRule) 管理服务接口。
 *              负责定义和管理哪些用户类型可以预约哪些场所分类。
 */
public interface VenueCategoryRuleService {

    /**
     * 根据场所分类ID查询其下所有的准入规则。
     *
     * @param categoryId 场所分类的ID。
     * @return 包含该分类下所有准入规则的列表 (List<VenueCategoryRule>)。
     */
    List<VenueCategoryRule> findRulesByCategoryId(Long categoryId);

    /**
     * 获取系统中所有的场所分类准入规则。
     *
     * @return 包含所有准入规则的列表 (List<VenueCategoryRule>)。
     */
    List<VenueCategoryRule> getAllVenueCategoryRules();

    /**
     * 为指定的场所分类添加一条新的准入规则。
     *
     * @param rule 包含 categoryId 和 allowedUserType 的规则实体。
     * @return 成功插入的记录数 (通常为 1)。
     * @throws IllegalArgumentException 如果分类不存在或规则已重复。
     */
    int addRule(VenueCategoryRule rule);

    /**
     * 根据规则的ID删除一条准入规则。
     *
     * @param ruleId 要删除的规则的唯一ID。
     * @return 成功删除的记录数 (通常为 1 或 0)。
     */
    int deleteRule(Long ruleId);
}