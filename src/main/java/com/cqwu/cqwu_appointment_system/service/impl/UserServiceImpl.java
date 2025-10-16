package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.entity.UserExample;
import com.cqwu.cqwu_appointment_system.mapper.UserMapper;
import com.cqwu.cqwu_appointment_system.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User selectUserById(String userId) {
        UserExample example = new UserExample();

        example.createCriteria().andIdEqualTo(userId);

        return userMapper.selectByExample(example).stream().findFirst().orElse(null);
    }

    @Override
    public User loginOrSyncUser(String userId) {
        User user = selectUserById(userId);
        if (user == null) {
            // 如果用户不存在，我们创建一个临时的、最基础的用户信息
            // 真实场景下，这里会从 CAS 返回的信息中填充 name, user_type 等
            user = new User();
            user.setId(userId);
            user.setName("新用户-" + userId); // 临时名称
            user.setUserType((byte) 1); // 默认为学生
            user.setRole("user"); // 默认角色
            userMapper.insert(user);
        }
        // (可选) 更新最后登录时间
        // User userToUpdate = new User();
        // userToUpdate.setId(userId);
        // userToUpdate.setLastLoginTime(LocalDateTime.now());
        // userMapper.updateByPrimaryKeySelective(userToUpdate);
        
        return user;
    }
}