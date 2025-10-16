package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.entity.User;

public interface UserService {
    /**
     * 根据用户ID (学号/工号) 查询用户
     * @param userId 用户ID
     * @return User 对象，如果不存在则返回 null
     */
    User selectUserById(String userId);

    /**
     * (未来对接 CAS 的核心方法)
     * 根据用户ID查找用户，如果不存在则创建一个新用户
     * @param userId 学号/工号
     * @return User 对象
     */
    User loginOrSyncUser(String userId);
}