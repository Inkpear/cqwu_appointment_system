package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryExample;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryRuleExample;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryRuleMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @description VenueCategoryService 的实现类。
 */
@Service
@Slf4j
public class VenueCategoryServiceImpl implements VenueCategoryService {

    @Autowired
    private VenueCategoryMapper venueCategoryMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private VenueCategoryRuleMapper venueCategoryRuleMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int createVenueCategory(VenueCategory venueCategory) {
        log.info("正在创建新的场所分类: {}", venueCategory.getName());
        Assert.notNull(venueCategory, "场所分类对象不能为空");

        // 确保ID由数据库自动生成
        venueCategory.setId(null);

        return venueCategoryMapper.insertSelective(venueCategory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteVenueCategory(Long id) {
        log.info("正在删除场所分类, ID: {}", id);
        Assert.notNull(id, "要删除的分类ID不能为空");

        // 检查是否有场所仍在使用此分类
        VenueExample example = new VenueExample();
        example.createCriteria().andCategoryIdEqualTo(id);
        if (venueMapper.countByExample(example) > 0) {
            throw new IllegalStateException("无法删除，仍有场所正在使用此分类");
        }

        // 级联删除与此分类关联的所有准入规则
        VenueCategoryRuleExample ruleExample = new VenueCategoryRuleExample();
        ruleExample.createCriteria().andCategoryIdEqualTo(id);
        int deletedRules = venueCategoryRuleMapper.deleteByExample(ruleExample);
        if (deletedRules > 0) {
            log.info("已级联删除了 {} 条关联的准入规则", deletedRules);
        }

        // 执行删除操作
        return venueCategoryMapper.deleteByPrimaryKey(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int updateVenueCategory(VenueCategory venueCategory) {
        log.info("正在更新场所分类, ID: {}", venueCategory.getId());
        Assert.notNull(venueCategory, "场所分类对象不能为空");
        Assert.notNull(venueCategory.getId(), "更新时分类ID不能为空");

        return venueCategoryMapper.updateByPrimaryKeySelective(venueCategory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VenueCategory findVenueCategoryById(Long id) {
        log.debug("正在根据ID查询场所分类, ID: {}", id);
        Assert.notNull(id, "查询的分类ID不能为空");
        return venueCategoryMapper.selectByPrimaryKey(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VenueCategory> findAllCategories() {
        log.debug("正在查询所有场所分类列表");
        // 使用 withBLOBs 是正确的，因为需要加载 TEXT 类型的 usage_rules 字段
        return venueCategoryMapper.selectByExampleWithBLOBs(new VenueCategoryExample());
    }
}