package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.VenueService;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * 场所服务实现类
 */
@Service
public class VenueServiceImpl implements VenueService {

    @Autowired
    private VenueMapper venueMapper; // 注入由 MyBatis 生成的 Mapper

    /**
     * 根据主键ID查询场所信息
     *
     * @param id 场所ID
     * @return 场所信息
     */
    @Override
    public Venue selectVenueById(Long id) {
        // 参数校验
        Assert.notNull(id, "场所ID不能为空");
        return venueMapper.selectByPrimaryKey(id);
    }

    /**
     * 更新场所信息
     *
     * @param venue 场所信息
     * @return 影响的行数
     */
    @Override
    @Transactional
    public int updateVenue(Venue venue) {
        // 关键校验：必须传入包含主键ID的 venue 对象
        Assert.notNull(venue, "场所信息不能为空");
        Assert.notNull(venue.getId(), "更新场所时，场所ID不能为空");

        return venueMapper.updateByPrimaryKeySelective(venue);
    }

    @Override
    @Transactional
    public int insertVenues(List<Venue> venues) {
        // 参数校验
        Assert.notEmpty(venues, "要批量添加的场所列表不能为空");

        for (Venue venue : venues) {
            venue.setId(null);
        }

        return venueMapper.insertBatch(venues);
    }

    /**
     * 根据ID列表批量删除场所
     *
     * @param ids id列表
     * @return 删除的行数
     */
    @Override
    @Transactional
    public int deleteVenueByIds(Long[] ids) {
        // 校验传入的数组是否为空或长度为0
        Assert.notEmpty(ids, "要删除的ID列表不能为空");

        VenueExample example = new VenueExample();
        // andIdIn() 方法需要一个 List<Long> 类型的参数
        example.createCriteria().andIdIn(Arrays.asList(ids));

        return venueMapper.deleteByExample(example);
    }

    @Override
    public Page<Venue> searchVenues(VenueQuery query) {
        // 1. 设置分页 (使用 PageHelper 插件，这是 MyBatis 最流行的分页方案)
        PageHelper.startPage(query.getPage() + 1, query.getSize()); // PageHelper 的页码是从 1 开始的

        // 2. 动态构建查询条件
        VenueExample example = new VenueExample();
        VenueExample.Criteria criteria = example.createCriteria();

        if (StringUtils.hasText(query.getName())) {
            criteria.andNameLike("%" + query.getName() + "%");
        }
        if (StringUtils.hasText(query.getStatus())) {
            criteria.andStatusEqualTo(query.getStatus());
        }
        if (StringUtils.hasText(query.getLocation())) {
            criteria.andLocationLike("%" + query.getLocation() + "%");
        }
        if (query.getFloor() != null) {
            criteria.andFloorEqualTo(query.getFloor());
        }

        // 3. 执行查询
        List<Venue> venues = venueMapper.selectByExample(example);

        // 4. 封装分页结果
        PageInfo<Venue> pageInfo = new PageInfo<>(venues);

        return new PageImpl<>(pageInfo.getList(), PageRequest.of(query.getPage(), query.getSize()),
                pageInfo.getTotal());
    }
}