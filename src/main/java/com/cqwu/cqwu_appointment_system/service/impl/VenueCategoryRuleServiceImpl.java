package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRule;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRuleExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryRuleMapper;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryRuleService;

import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class VenueCategoryRuleServiceImpl implements VenueCategoryRuleService {

    @Autowired
    private VenueCategoryRuleMapper ruleMapper;

    @Autowired
    private VenueCategoryMapper venueCategoryMapper;

    @Override
    public List<VenueCategoryRule> findRulesByCategoryId(Long categoryId) {
        VenueCategoryRuleExample example = new VenueCategoryRuleExample();
        example.createCriteria().andCategoryIdEqualTo(categoryId);
        return ruleMapper.selectByExample(example);
    }

    @Override
    public int addRule(VenueCategoryRule rule) {
        log.info("正在为分类 ID: {} 添加新的准入规则: {}", rule.getCategoryId(), rule);

        Assert.notNull(rule, "规则对象不能为空");
        Assert.notNull(rule.getCategoryId(), "关联的分类ID不能为空");
        Assert.notNull(rule.getAllowedUserType(), "允许的用户类型不能为空");

        VenueCategory category = venueCategoryMapper.selectByPrimaryKey(rule.getCategoryId());
        if (category == null) {
            throw new IllegalArgumentException("无法添加规则，ID为 " + rule.getCategoryId() + " 的场所分类不存在");
        }

        VenueCategoryRuleExample example = new VenueCategoryRuleExample();
        example.createCriteria()
                .andCategoryIdEqualTo(rule.getCategoryId())
                .andAllowedUserTypeEqualTo(rule.getAllowedUserType());

        long existingCount = ruleMapper.countByExample(example);
        if (existingCount > 0) {
            throw new IllegalArgumentException("已存在相同的准入规则，请勿重复添加");
        }

        rule.setId(null);
        return ruleMapper.insertSelective(rule);
    }

    @Override
    public int deleteRule(Long ruleId) {
        log.info("正在删除准入规则, ID: {}", ruleId);
        return ruleMapper.deleteByPrimaryKey(ruleId);
    }
}