package com.cqwu.cqwu_appointment_system.controller;

import com.cqwu.cqwu_appointment_system.common.constant.Constants;
import com.cqwu.cqwu_appointment_system.common.constant.HttpStatus;
import com.cqwu.cqwu_appointment_system.common.utils.StringUtils;
import com.cqwu.cqwu_appointment_system.domain.dto.PageDTO;
import com.cqwu.cqwu_appointment_system.domain.AjaxResult;
import com.cqwu.cqwu_appointment_system.domain.dto.UpdateUserRequest;
import com.cqwu.cqwu_appointment_system.domain.query.UserQuery;
import com.cqwu.cqwu_appointment_system.entity.User;
import com.cqwu.cqwu_appointment_system.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
@PreAuthorize("hasRole('ROOT')")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表，支持多条件筛选
     */
    @GetMapping
    public ResponseEntity<AjaxResult> listUsers(UserQuery query) {
        log.info("管理员正在查询用户列表, 查询参数: {}", query);

        PageDTO<User> userPage = PageDTO.fromPage(userService.searchUsers(query));

        return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, userPage));
    }

    /**
     * 根据用户ID获取单个用户详细信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<AjaxResult> getUserById(@PathVariable String id) {
        log.info("管理员正在查询用户详情, ID: {}", id);
        User user = userService.selectUserById(id);
        if (user != null) {
            return ResponseEntity.ok(AjaxResult.success(Constants.QUERY_SUCCESS, user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.USER_NOT_FOUND));
        }
    }

    /**
     * 更新用户信息 (不允许通过此接口修改角色)
     */
    @PutMapping("/{id}")
    public ResponseEntity<AjaxResult> updateUser(@PathVariable String id, @RequestBody @Valid UpdateUserRequest updateUserRequest) {
        log.info("管理员正在更新用户信息, ID: {}", id);

        User userToUpdate = new User();
        userToUpdate.setId(id);
        userToUpdate.setName(updateUserRequest.getName());
        userToUpdate.setUserType(updateUserRequest.getUserType());
        userToUpdate.setCollege(updateUserRequest.getCollege());
        userToUpdate.setMajor(updateUserRequest.getMajor());

        int result = userService.updateUser(userToUpdate);
        if (result > 0) {
            return ResponseEntity.ok(AjaxResult.success(Constants.UPDATE_SUCCESS));
        } else {
            if (userService.selectUserById(id) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.UPDATE_FAIL_NOT_FOUND));
            } else {
                return ResponseEntity.ok(AjaxResult.success(Constants.DATA_NOT_CHANGED));
            }
        }
    }

    /**
     * 修改指定用户的角色
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<AjaxResult> updateUserRole(@PathVariable String id, @RequestBody User userRoleInfo) {
        String newRole = userRoleInfo.getRole();
        log.info("管理员正在修改用户 '{}' 的角色为 '{}'", id, newRole);

        if (!StringUtils.matches(newRole, List.of(Constants.ROLE_USER, Constants.ROLE_ADMIN, Constants.ROLE_ROOT))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AjaxResult.error("无效的角色名称"));
        }

        User userToUpdate = new User();
        userToUpdate.setId(id);
        userToUpdate.setRole(newRole);

        int result = userService.updateUser(userToUpdate);
        if (result > 0) {
            return ResponseEntity.ok(AjaxResult.success("角色更新成功"));
        } else {
            if (userService.selectUserById(id) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AjaxResult.error(HttpStatus.NOT_FOUND, Constants.USER_NOT_FOUND));
            } else {
                return ResponseEntity.ok(AjaxResult.success(Constants.DATA_NOT_CHANGED));
            }
        }
    }
}