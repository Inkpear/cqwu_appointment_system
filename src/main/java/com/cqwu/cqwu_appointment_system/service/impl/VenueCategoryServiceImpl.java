package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.VenueCategory;
import com.cqwu.cqwu_appointment_system.entity.VenueCategoryExample;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueCategoryMapper;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.VenueCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Service
@Slf4j
public class VenueCategoryServiceImpl implements VenueCategoryService {

    @Autowired
    private VenueCategoryMapper venueCategoryMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Override
    @Transactional
    public int createVenueCategory(VenueCategory venueCategory) {
        log.info("正在创建新的分类: {}", venueCategory.getName());
        Assert.notNull(venueCategory, "场所分类对象不能为空");

        venueCategory.setId(null);

        return venueCategoryMapper.insertSelective(venueCategory);
    }

    @Override
    @Transactional
    public int deleteVenueCategory(Long id) {
        log.info("正在删除分类, ID: {}", id);
        Assert.notNull(id, "要删除的分类ID不能为空");

        VenueExample example = new VenueExample();
        example.createCriteria().andCategoryIdEqualTo(id);
        if (venueMapper.countByExample(example) > 0) {
            throw new IllegalStateException("无法删除，仍有场所正在使用此分类");
        }

        return venueCategoryMapper.deleteByPrimaryKey(id);
    }

    @Override
    @Transactional
    public int updateVenueCategory(VenueCategory venueCategory) {
        log.info("正在更新分类, ID: {}", venueCategory.getId());
        Assert.notNull(venueCategory, "场所分类对象不能为空");
        Assert.notNull(venueCategory.getId(), "更新时分类ID不能为空");

        return venueCategoryMapper.updateByPrimaryKeySelective(venueCategory);
    }

    @Override
    public VenueCategory findVenueCategoryById(Long id) {
        log.debug("正在根据ID查询分类, ID: {}", id);
        Assert.notNull(id, "查询的分类ID不能为空");
        return venueCategoryMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<VenueCategory> findAllCategories() {
        log.debug("正在查询所有分类列表");
        return venueCategoryMapper.selectByExampleWithBLOBs(new VenueCategoryExample());
    }
}