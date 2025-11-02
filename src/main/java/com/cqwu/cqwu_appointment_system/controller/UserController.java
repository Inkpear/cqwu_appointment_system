package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.dto.CreateUserDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateUserRequest;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateUserRoleRequest;
import com.cqwu.cqwu_appointment_system.domain.query.UserQuery;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @description 负责处理用户管理相关的API请求。
 *              所有接口默认需要 ROOT 权限。
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * [ROOT] 分页查询用户列表，支持多条件筛选。
     *
     * @param query 查询参数
     * @return 分页的用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> listUsers(@Valid UserQuery query) {
        log.info("管理员正在查询用户列表, 查询参数: {}", query);
        Page<User> userPage = userService.searchUsers(query);
        PageDTO<User> userPageDTO = PageDTO.fromPage(userPage);
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, userPageDTO));
    }

    /**
     * [AUTHENTICATED] 根据用户ID获取用户姓名。
     *
     * @param id 用户ID
     * @return 用户姓名
     */
    @GetMapping("/names/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AjaxResult> getUserName(@PathVariable String id) {
        log.info("正在查询用户姓名, ID: {}", id);
        User user = userService.selectUserById(id);

        if (user == null) {
            throw new IllegalArgumentException(Constants.USER_NOT_FOUND);
        }

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, user.getName()));
    }

    /**
     * [ROOT] 创建新用户。
     *
     * @param createUserDTO 包含新用户信息的请求体
     * @return 操作成功的 ResponseEntity
     */
    @PostMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> createUser(@RequestBody @Valid CreateUserDTO createUserDTO) {
        log.info("管理员正在创建新用户, 用户ID: {}", createUserDTO.getId());

        User newUser = new User();
        BeanUtils.copyProperties(createUserDTO, newUser);

        userService.insertUser(newUser);

        return ResponseEntity
                .ok(AjaxResult.created(Constants.CREATE_SUCCESS, userService.selectUserById(createUserDTO.getId())));

    }

    /**
     * [ROOT] 根据用户ID获取单个用户详细信息。
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> getUserById(@PathVariable String id) {
        log.info("管理员正在查询用户详情, ID: {}", id);
        User user = userService.selectUserById(id);
        if (user == null) {
            throw new IllegalArgumentException(Constants.USER_NOT_FOUND);
        }
        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, user));
    }

    /**
     * [ROOT] 更新用户信息 (不允许通过此接口修改角色)。
     *
     * @param id                用户ID
     * @param updateUserRequest 包含待更新字段的请求体
     * @return 操作成功的 ResponseEntity
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> updateUser(@PathVariable String id,
            @RequestBody @Valid UpdateUserRequest updateUserRequest) {
        log.info("管理员正在更新用户信息, ID: {}", id);
        User userToUpdate = new User();
        BeanUtils.copyProperties(updateUserRequest, userToUpdate);
        userToUpdate.setId(id);

        int result = userService.updateUser(userToUpdate);

        if (result == 0) {
            return ResponseEntity.ok(AjaxResult.success(Constants.DATA_NOT_CHANGED));
        }
        return ResponseEntity.ok(AjaxResult.success(Constants.UPDATE_SUCCESS));
    }

    @DeleteMapping
    @PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> deleteUsers(@RequestBody String[] userIds) {
        log.info("管理员正在批量删除用户, IDs: {}", (Object) userIds);

        int deletedCount = userService.deleteUserByIds(userIds);

        return ResponseEntity.ok(AjaxResult.success(
                String.format("成功删除 %d 个用户", deletedCount)));
    }

    /**
     * [ROOT] 修改指定用户的角色。
     *
     * @param id           用户ID
     * @param userRoleInfo 包含新角色的请求体
     * @return 操作成功的 ResponseEntity
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole(@securityConstants.ROLE_ROOT)")
    public ResponseEntity<AjaxResult> updateUserRole(@PathVariable String id,
            @RequestBody @Valid UpdateUserRoleRequest userRoleInfo) {
        String newRole = userRoleInfo.getRole();
        log.info("管理员正在修改用户 '{}' 的角色为 '{}'", id, newRole);

        User userToUpdate = new User();
        userToUpdate.setId(id);
        userToUpdate.setRole(newRole);

        int result = userService.updateUser(userToUpdate);

        if (result == 0) {
            return ResponseEntity.ok(AjaxResult.success(Constants.DATA_NOT_CHANGED));
        }
        return ResponseEntity.ok(AjaxResult.success("角色更新成功"));
    }
}