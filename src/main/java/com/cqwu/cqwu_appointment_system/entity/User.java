package com.cqwu.cqwu_appointment_system.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class User {

    /**
     * 主键ID, 存储学号或工号
     * SQL约束: VARCHAR(20), NOT NULL
     */
    @NotBlank(message = "用户ID不能为空")
    @Size(max = 20, message = "用户ID长度不能超过20个字符")
    private String id;

    /**
     * 用户类型 (1: 学生, 2: 教师, 0: 其他)
     * SQL约束: TINYINT, NOT NULL
     */
    @NotNull(message = "用户类型不能为空")
    private Byte userType;

    /**
     * 真实姓名
     * SQL约束: VARCHAR(50), NOT NULL
     */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String name;

    /**
     * 性别 (1: 男, 2: 女, 0: 未知)
     * SQL约束: TINYINT, NULL
     */
    private Byte sex;

    /**
     * 所属学院
     * SQL约束: VARCHAR(100), NULL
     */
    @Size(max = 100, message = "所属学院长度不能超过100个字符")
    private String college;

    /**
     * 所属专业
     * SQL约束: VARCHAR(100), NULL
     */
    @Size(max = 100, message = "所属专业长度不能超过100个字符")
    private String major;

    /**
     * 角色 (例如: user, admin)
     * SQL约束: VARCHAR(30), NOT NULL
     */
    @NotBlank(message = "用户角色不能为空")
    @Size(max = 30, message = "角色长度不能超过30个字符")
    private String role;

    /**
     * 最后登录时间
     * SQL约束: DATETIME, NULL
     */
    private LocalDateTime lastLoginTime;

    /**
     * 记录创建时间
     * SQL约束: DATETIME, NOT NULL
     * (通常由数据库自动生成，不在创建时校验)
     */
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     * SQL约束: DATETIME, NOT NULL
     * (通常由数据库自动生成，不在创建时校验)
     */
    private LocalDateTime updateTime;
}