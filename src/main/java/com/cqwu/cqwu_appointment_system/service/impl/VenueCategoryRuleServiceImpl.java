package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRuleExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryRuleMapper;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class VenueCategoryRuleServiceImpl implements VenueCategoryRuleService {

    @Autowired
    private VenueCategoryRuleMapper ruleMapper;

    @Override
    public List<VenueCategoryRule> findRulesByCategoryId(Long categoryId) {
        VenueCategoryRuleExample example = new VenueCategoryRuleExample();
        example.createCriteria().andCategoryIdEqualTo(categoryId);
        return ruleMapper.selectByExample(example);
    }

    @Override
    public int addRule(VenueCategoryRule rule) {
        log.info("正在为分类 ID: {} 添加新的准入规则: {}", rule.getCategoryId(), rule);
        rule.setId(null);
        return ruleMapper.insertSelective(rule);
    }

    @Override
    public int deleteRule(Long ruleId) {
        log.info("正在删除准入规则, ID: {}", ruleId);
        return ruleMapper.deleteByPrimaryKey(ruleId);
    }
}