package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.domain.query.UserQuery;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.UserExample;
import com.cqwu.cqwu_appointment_system.mapper.UserMapper;
import com.cqwu.cqwu_appointment_system.service.UserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description UserService 的实现类。
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public User selectUserById(String userId) {
        log.debug("开始根据ID查询用户信息, User ID: {}", userId);
        Assert.hasText(userId, "用户ID不能为空");

        // 【修正】使用 Example 查询是正确的，因为主键是VARCHAR类型
        UserExample example = new UserExample();
        example.createCriteria().andIdEqualTo(userId);
        return userMapper.selectByExample(example).stream().findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public User loginOrSyncUser(String userId) {
        // TODO: 等待后续补充CAS对接代码
        log.info("用户登录或同步, User ID: {}", userId);
        Assert.hasText(userId, "用户ID不能为空");

        User user = this.selectUserById(userId);
        LocalDateTime loginTime = LocalDateTime.now();

        if (user == null) {
            log.info("用户 {} 在本地不存在，将创建新用户", userId);
            user = new User();
            user.setId(userId);
            user.setName("新用户-" + userId); // 默认名称，后续可由CAS信息覆盖
            user.setUserType((byte) 1); // 默认用户类型
            user.setRole(Constants.UserRoleType.ROLE_USER.toLowerCase()); // 默认角色
            user.setLastLoginTime(loginTime);
            this.insertUser(user);
            log.info("新用户 {} 创建成功", userId);
        } else {
            log.info("用户 {} 已存在，更新其最后登录时间", userId);
            this.updateLoginTimeById(userId, loginTime);
            user.setLastLoginTime(loginTime); // 更新内存中对象的状态以返回
        }
        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int insertUser(User user) {
        log.info("开始插入新用户, User ID: {}", user.getId());
        Assert.notNull(user, "User对象不能为空");
        Assert.hasText(user.getId(), "用户ID不能为空");
        return userMapper.insertSelective(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateLoginTimeById(String userId) {
        Assert.hasText(userId, "用户ID不能为空");
        this.updateLoginTimeById(userId, LocalDateTime.now());
    }

    /**
     * 内部私有方法，使用指定的时间更新用户的最后登录时间。
     * 
     * @param userId    用户ID
     * @param loginTime 指定的登录时间
     */
    private void updateLoginTimeById(String userId, LocalDateTime loginTime) {
        log.debug("执行更新用户登录时间操作, User ID: {}, Login Time: {}", userId, loginTime);
        User userToUpdate = new User();
        userToUpdate.setLastLoginTime(loginTime);

        UserExample example = new UserExample();
        example.createCriteria().andIdEqualTo(userId);

        int result = userMapper.updateByExampleSelective(userToUpdate, example);
        if (result == 0) {
            // 在登录场景下，更新失败可能是严重问题，因此使用error级别
            log.error("更新用户 {} 的登录时间失败，该用户可能已被删除", userId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int updateUser(User user) {
        log.info("开始更新用户信息, User ID: {}", user.getId());
        Assert.notNull(user, "User对象不能为空");
        Assert.hasText(user.getId(), "更新用户时，用户ID不能为空");

        if (this.selectUserById(user.getId()) == null) {
            throw new IllegalArgumentException(Constants.UPDATE_FAIL_NOT_FOUND);
        }

        UserExample example = new UserExample();
        example.createCriteria().andIdEqualTo(user.getId());

        String userId = user.getId();
        user.setId(null);

        int result = userMapper.updateByExampleSelective(user, example);
        if (result == 0) {
            log.warn("更新用户信息未产生影响, 数据无变化, User ID: {}", userId);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<User> searchUsers(UserQuery query) {
        log.info("开始根据条件分页查询用户, 查询参数: {}", query);

        Integer page = query.getPage() == null ? Constants.DEFAULT_PAGE : query.getPage();
        Integer size = query.getSize() == null ? Constants.DEFAULT_PAGE_SIZE : query.getSize();

        PageHelper.startPage(page + 1, size);

        UserExample example = new UserExample();
        UserExample.Criteria criteria = example.createCriteria();

        if (StringUtils.hasText(query.getId())) {
            criteria.andIdLike("%" + query.getId() + "%");
        }
        if (StringUtils.hasText(query.getName())) {
            criteria.andNameLike("%" + query.getName() + "%");
        }
        if (StringUtils.hasText(query.getRole())) {
            criteria.andRoleEqualTo(query.getRole());
        }
        if (query.getUserType() != null) {
            criteria.andUserTypeEqualTo(query.getUserType());
        }

        List<User> users = userMapper.selectByExample(example);
        PageInfo<User> pageInfo = new PageInfo<>(users);

        log.info("用户查询完成, 共找到 {} 条数据", pageInfo.getTotal());
        return new PageImpl<>(pageInfo.getList(), PageRequest.of(page, size), pageInfo.getTotal());
    }
}