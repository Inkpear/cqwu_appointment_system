package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule;
import java.util.List;

public interface VenueCategoryRuleService {
    List<VenueCategoryRule> findRulesByCategoryId(Long categoryId);

    int addRule(VenueCategoryRule rule);

    int deleteRule(Long ruleId);
}