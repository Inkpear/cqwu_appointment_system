DROP DATABASE IF EXISTS cqwu;

CREATE DATABASE cqwu CHARACTER set utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cqwu

DROP TABLE IF EXISTS `venue`;
DROP TABLE IF EXISTS `reservation`;

-- ----------------------------
-- Table structure for venue
-- ----------------------------
CREATE TABLE `venue`  (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场所名称',
  `location` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '位置',
  `floor` INT NULL DEFAULT NULL COMMENT '楼层',
  `capacity` INT NULL DEFAULT 0 COMMENT '容量',
  `category_id` BIGINT NULL DEFAULT NULL COMMENT '分类ID, 关联分类表',
  `equipment` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '设备配置 ',
  `status` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'available' COMMENT '状态 ',
  `open_time` TIME NULL DEFAULT NULL COMMENT '开放开始时间',
  `close_time` TIME NULL DEFAULT NULL COMMENT '开放结束时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '场所信息表' ROW_FORMAT = Dynamic;



-- ----------------------------
-- Table structure for reservation
-- ----------------------------
CREATE TABLE `reservation`  (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `venue_id` BIGINT NOT NULL COMMENT '场所ID, 关联venue表',
  `user_id` BIGINT NOT NULL COMMENT '用户ID, 关联用户表',
  `start_time` DATETIME NOT NULL COMMENT '预约开始时间',
  `end_time` DATETIME NOT NULL COMMENT '预约结束时间',
  `purpose` VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用目的',
  `status` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'confirmed' COMMENT '预约状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `reservation_code` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '预约码，用于核销或查询',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_reservation_code`(`reservation_code`) USING BTREE COMMENT '预约码唯一索引，确保其唯一性',
  INDEX `idx_venue_id`(`venue_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_start_time`(`start_time`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_venue_time`(`venue_id`, `start_time`, `end_time`) USING BTREE COMMENT '用于快速查询某个场所在特定时间段内的预约'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预约记录表' ROW_FORMAT = Dynamic;