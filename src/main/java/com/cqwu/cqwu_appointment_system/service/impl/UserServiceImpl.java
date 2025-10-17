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

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User selectUserById(String userId) {
        log.info("开始根据ID查询用户信息, User ID: {}", userId);
        Assert.hasText(userId, "用户ID不能为空");
        UserExample example = new UserExample();
        example.createCriteria().andIdEqualTo(userId);

        User user = userMapper.selectByExample(example).stream().findFirst().orElse(null);

        if (user != null) {
            log.info("成功查询到用户信息, User ID: {}", userId);
        } else {
            log.warn("未查询到ID为 {} 的用户信息", userId);
        }
        return user;
    }

    @Override
    @Transactional
    public User loginOrSyncUser(String userId) {
        // 等待后续补充CAS对接代码
        log.info("用户登录或同步, User ID: {}", userId);
        Assert.hasText(userId, "用户ID不能为空");
        User user = this.selectUserById(userId);
        LocalDateTime loginTime = LocalDateTime.now();

        if (user == null) {
            log.info("用户 {} 不存在，将创建新用户", userId);
            user = new User();
            user.setId(userId);
            user.setName("新用户-" + userId);
            user.setUserType((byte) 1);
            user.setRole("user");
            user.setLastLoginTime(loginTime);
            userMapper.insert(user);
            log.info("新用户 {} 创建成功", userId);
        } else {
            log.info("用户 {} 已存在，更新最后登录时间", userId);
            this.updateLoginTimeById(userId, loginTime);
            user.setLastLoginTime(loginTime);
        }
        return user;
    }

    @Override
    @Transactional
    public void updateLoginTimeById(String userId) {
        log.info("准备更新用户最后登录时间, User ID: {}", userId);
        Assert.hasText(userId, "用户ID不能为空");
        this.updateLoginTimeById(userId, LocalDateTime.now());
    }

    /**
     * 内部私有方法，用于更新登录时间
     */
    private void updateLoginTimeById(String userId, LocalDateTime loginTime) {
        log.info("执行更新用户登录时间操作, User ID: {}, Login Time: {}", userId, loginTime);
        User userToUpdate = new User();
        userToUpdate.setLastLoginTime(loginTime);

        UserExample example = new UserExample();
        example.createCriteria().andIdEqualTo(userId);

        int result = userMapper.updateByExampleSelective(userToUpdate, example);
        if (result > 0) {
            log.info("成功更新用户 {} 的登录时间", userId);
        } else {
            log.error("更新用户 {} 的登录时间失败", userId);
        }
    }

    @Override
    @Transactional
    public int insertUser(User user) {
        log.info("开始插入新用户, User ID: {}", user.getId());
        int result = userMapper.insert(user);
        if (result > 0) {
            log.info("成功插入新用户, User ID: {}", user.getId());
        } else {
            log.error("插入新用户失败, User ID: {}", user.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public int updateUser(User user) {
        log.info("开始更新用户信息, User ID: {}", user.getId());
        Assert.hasText(user.getId(), "更新用户时，用户ID不能为空");

        UserExample example = new UserExample();
        example.createCriteria().andIdEqualTo(user.getId());

        String userId = user.getId();
        user.setId(null);

        int result = userMapper.updateByExampleSelective(user, example);

        if (result > 0) {
            log.info("成功更新用户信息, User ID: {}", userId);
        } else {
            log.warn("更新用户信息未产生影响, 可能没有找到匹配的用户或数据无变化, User ID: {}", userId);
        }
        return result;
    }

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