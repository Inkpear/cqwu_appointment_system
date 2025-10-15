package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.VenueService;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

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
@Service // 标记这是一个 Spring Service 组件，将其注册到 IoC 容器中
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
     * 根据场所名称查询场所信息
     *
     * @param name 场所名字
     * @return 场所列表
     */
    @Override
    public List<Venue> selectVenueListByName(String name) {
        VenueExample example = new VenueExample();

        if (StringUtils.hasText(name)) {
            // 使用Like模糊匹配
            example.createCriteria().andNameLike("%" + name + "%");
        }

        return venueMapper.selectByExample(example);
    }

    /**
     * 根据场所状态查询场所列表
     *
     * @param status 场所状态
     * @return 场所列表
     */
    @Override
    public List<Venue> selectVenueListByStatus(String status) {
        VenueExample example = new VenueExample();
        // 如果传入了有效的 status，则添加查询条件
        if (StringUtils.hasText(status)) {
            example.createCriteria().andStatusEqualTo(status);
        }
        // 如果 status 为空或 null，则查询所有场所
        return venueMapper.selectByExample(example);
    }

    /**
     * 根据场所位置模糊查询场所列表
     *
     * @param location 位置关键字
     * @return 场所列表
     */
    @Override
    public List<Venue> selectBenueListByLocation(String location) {
        VenueExample example = new VenueExample();
        if (StringUtils.hasText(location)) {
            // 使用 LIKE 进行模糊查询
            example.createCriteria().andLocationLike("%" + location + "%");
        }
        return venueMapper.selectByExample(example);
    }

    /**
     * 更新场所信息
     *
     * @param venue 场所信息
     * @return 影响的行数
     */
    @Override
    public int updateVenue(Venue venue) {
        // 关键校验：必须传入包含主键ID的 venue 对象
        Assert.notNull(venue, "场所信息不能为空");
        Assert.notNull(venue.getId(), "更新场所时，场所ID不能为空");

        // 使用 updateByPrimaryKeySelective 只更新非 null 字段，更安全
        return venueMapper.updateByPrimaryKeySelective(venue);
    }

    /**
     * 插入新的场所信息
     *
     * @param venue 场所信息
     * @return 影响的行数
     */
    @Override
    public int insertVenue(Venue venue) {
        Assert.notNull(venue, "场所信息不能为空");
        // 插入前，将ID设为null，以确保数据库会使用自增主键
        venue.setId(null);
        return venueMapper.insertSelective(venue);
    }

    /**
     * 根据ID列表批量删除场所
     *
     * @param ids id列表
     * @return 删除的行数
     */
    @Override
    public int deleteVenueByIds(Long[] ids) {
        // 校验传入的数组是否为空或长度为0
        Assert.notEmpty(ids, "要删除的ID列表不能为空");

        VenueExample example = new VenueExample();
        // andIdIn() 方法需要一个 List<Long> 类型的参数
        example.createCriteria().andIdIn(Arrays.asList(ids));

        return venueMapper.deleteByExample(example);
    }

    @Override
    public Page<Venue> searchVenues(String name, String status, String location, int page, int size) {
        // 1. 设置分页 (使用 PageHelper 插件，这是 MyBatis 最流行的分页方案)
        PageHelper.startPage(page + 1, size); // PageHelper 的页码是从 1 开始的

        // 2. 动态构建查询条件
        VenueExample example = new VenueExample();
        VenueExample.Criteria criteria = example.createCriteria();

        if (StringUtils.hasText(name)) {
            criteria.andNameLike("%" + name + "%");
        }
        if (StringUtils.hasText(status)) {
            criteria.andStatusEqualTo(status);
        }
        if (StringUtils.hasText(location)) {
            criteria.andLocationLike("%" + location + "%");
        }

        // 3. 执行查询
        List<Venue> venues = venueMapper.selectByExample(example);

        // 4. 封装分页结果
        PageInfo<Venue> pageInfo = new PageInfo<>(venues);

        return new PageImpl<>(pageInfo.getList(), PageRequest.of(page, size), pageInfo.getTotal());
    }
}