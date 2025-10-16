-- ----------------------------
-- Table structure for user
-- ----------------------------
USE cqwu

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` VARCHAR(20) NOT NULL COMMENT '主键ID, 存储学号或工号',
  `user_type` TINYINT NOT NULL COMMENT '用户类型 (1: 学生, 2: 教师, 0: 其他)',
  `name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `sex` TINYINT NULL DEFAULT 0 COMMENT '性别 (1: 男, 2: 女, 0: 未知)',
  `college` VARCHAR(100) NULL DEFAULT NULL COMMENT '所属学院',
  `major` VARCHAR(100) NULL DEFAULT NULL COMMENT '所属专业',
  `role` VARCHAR(30) NOT NULL DEFAULT 'user' COMMENT '角色 (例如: user, admin)',
  `last_login_time` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_type`(`user_type`) USING BTREE,
  INDEX `idx_role`(`role`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;