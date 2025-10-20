package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.domain.query.UserQuery;
import com.cqwu.cqwu_appointment_system.entity.User;
import org.springframework.data.domain.Page;

/**
 * @description 用户信息管理服务接口。
 *              负责用户的查询、创建、更新等核心业务。
 */
public interface UserService {
    /**
     * 根据用户唯一ID (学号/工号) 查询用户详细信息。
     *
     * @param userId 用户的唯一ID。
     * @return 匹配的 User 实体，如果不存在则返回 null。
     */
    User selectUserById(String userId);

    /**
     * (未来对接 CAS 的核心方法)
     * 根据用户ID查找用户。如果用户在本地数据库中不存在，则根据ID创建一个新用户记录。
     * 这是实现外部认证系统用户自动同步的关键。
     *
     * @param userId 从外部认证系统获取的学号/工号。
     * @return 已存在或新创建的 User 实体。
     */
    User loginOrSyncUser(String userId);

    /**
     * 插入一个全新的用户记录。
     *
     * @param user 待插入的用户实体，ID 不能为空。
     * @return 成功插入的记录数 (通常为 1)。
     */
    int insertUser(User user);

    /**
     * 根据用户ID更新其最后登录时间为当前时间。
     *
     * @param userId 要更新的用户的ID。
     */
    void updateLoginTimeById(String userId);

    /**
     * 更新一个已存在的用户信息。
     * 这是一个选择性更新，只会更新 User 对象中非null的字段。
     *
     * @param user 包含待更新字段的用户实体，ID 不能为空。
     * @return 成功更新的记录数 (通常为 1 或 0)。
     */
    int updateUser(User user);

    /**
     * 根据动态查询条件分页搜索用户列表。
     *
     * @param query 封装了过滤条件和分页参数的查询对象。
     * @return 封装了查询结果和分页信息的 Spring Data Page 对象。
     */
    Page<User> searchUsers(UserQuery query);
}