package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueExample;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;
import com.cqwu.cqwu_appointment_system.service.VenueService;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class VenueServiceImpl implements VenueService {

    @Autowired
    private VenueMapper venueMapper;

    /**
     * 根据主键ID查询场所信息
     *
     * @param id 场所ID
     * @return 场所信息
     */
    @Override
    public Venue selectVenueById(Long id) {
        log.info("开始根据ID查询场所信息, ID: {}", id);
        // 参数校验
        Assert.notNull(id, "场所ID不能为空");
        Venue venue = venueMapper.selectByPrimaryKey(id);
        if (venue != null) {
            log.info("成功查询到场所信息, ID: {}", id);
        } else {
            log.warn("未查询到ID为 {} 的场所信息", id);
        }
        return venue;
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
        log.info("开始更新场所信息, Venue ID: {}", venue.getId());
        Assert.notNull(venue, "场所信息不能为空");
        Assert.notNull(venue.getId(), "更新场所时，场所ID不能为空");

        int result = venueMapper.updateByPrimaryKeySelective(venue);
        if (result > 0) {
            log.info("成功更新场所信息, Venue ID: {}", venue.getId());
        } else {
            log.error("更新场所信息失败, Venue ID: {}", venue.getId());
        }
        return result;
    }

    /**
     * 批量插入场所
     *
     * @param venues 场所列表
     * @return 插入的行数
     */
    @Override
    @Transactional
    public int insertVenues(List<Venue> venues) {
        log.info("开始批量插入场所, 数量: {}", venues.size());
        Assert.notEmpty(venues, "要批量添加的场所列表不能为空");

        for (Venue venue : venues) {
            venue.setId(null);
        }

        int result = venueMapper.insertBatch(venues);
        log.info("成功批量插入 {} 条场所信息", result);
        return result;
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
        log.info("开始根据ID列表批量删除场所, IDs: {}", Arrays.toString(ids));
        Assert.notEmpty(ids, "要删除的ID列表不能为空");

        VenueExample example = new VenueExample();
        example.createCriteria().andIdIn(Arrays.asList(ids));

        int result = venueMapper.deleteByExample(example);
        if (result > 0) {
            log.info("成功删除 {} 条场所信息, IDs: {}", result, Arrays.toString(ids));
        } else {
            log.warn("没有删除任何场所信息, 可能ID列表匹配不到数据, IDs: {}", Arrays.toString(ids));
        }
        return result;
    }

    /**
     * 根据条件分页查询场所
     *
     * @param query 查询条件
     * @return 分页后的场所列表
     */
    @Override
    public Page<Venue> searchVenues(VenueQuery query) {
        log.info("开始根据条件分页查询场所, 查询参数: {}", query);

        Integer page = query.getPage() == null ? Constants.DEFAULT_PAGE : query.getPage();
        Integer size = query.getSize() == null ? Constants.DEFAULT_PAGE_SIZE : query.getSize();

        PageHelper.startPage(page + 1, size);

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

        List<Venue> venues = venueMapper.selectByExample(example);
        PageInfo<Venue> pageInfo = new PageInfo<>(venues);

        log.info("查询完成, 共找到 {} 条数据", pageInfo.getTotal());
        return new PageImpl<>(pageInfo.getList(), PageRequest.of(page, size), pageInfo.getTotal());
    }
}