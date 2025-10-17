package com.cqwu.cqwu_appointment_system.service;

import org.springframework.data.domain.Page;

import com.cqwu.cqwu_appointment_system.domain.query.UserQuery;
import com.cqwu.cqwu_appointment_system.entity.User;


public interface UserService {
    /**
     * 根据用户ID (学号/工号) 查询用户
     * 
     * @param userId 用户ID
     * @return User 对象，如果不存在则返回 null
     */
    public User selectUserById(String userId);

    /**
     * (未来对接 CAS 的核心方法)
     * 根据用户ID查找用户，如果不存在则创建一个新用户
     * 
     * @param userId 学号/工号
     * @return User 对象
     */
    public User loginOrSyncUser(String userId);

    /**
     * 
     * @param user
     * @return 状态
     */
    public int insertUser(User user);

    /**
     * 根据用户id更新登陆时间
     * 
     * @param userId
     */
    public void updateLoginTimeById(String userId);

    /**
     * 
     * @param user
     * @return 状态
     */
    public int updateUser(User user);

    /**
     * 根据条件分页查询用户
     * 
     * @param query 查询条件
     * @return 分页后的用户列表
     */
    Page<User> searchUsers(UserQuery query);
}