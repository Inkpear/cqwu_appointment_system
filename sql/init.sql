DROP DATABASE IF EXISTS cqwu;

CREATE DATABASE cqwu CHARACTER
set
  utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cqwu
DROP TABLE IF EXISTS `venue`;

DROP TABLE IF EXISTS `reservations`;

DROP TABLE IF EXISTS `time_blocks`;

DROP TABLE IF EXISTS `user`;

DROP TABLE IF EXISTS `category_id`;

-- ----------------------------
-- Table structure for venue
-- ----------------------------
CREATE TABLE
  `venue` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '场所名称',
    `location` VARCHAR(200) NULL DEFAULT NULL COMMENT '位置',
    `floor` INT NULL DEFAULT NULL COMMENT '楼层',
    `capacity` INT NULL DEFAULT 0 COMMENT '容量',
    `category_id` BIGINT NULL DEFAULT NULL COMMENT '分类ID, 关联 venue_category 表',
    `equipment` TEXT NULL COMMENT '设备配置 (例如: 投影仪, 白板, 空调)',
    `base_status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '基础状态 (由管理员设置, 如 AVAILABLE, MAINTENANCE, DISABLED)',
    `realtime_status` VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '实时状态 (由定时任务计算, 如 AVAILABLE, OCCUPIED, CLOSED)。UNKNOWN 为初始值',
    `open_time` TIME NULL COMMENT '每日开放时间 (例如: 08:00:00)',
    `close_time` TIME NULL COMMENT '每日关闭时间 (例如: 22:00:00)',
    `requires_approval` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '预约是否需要人工审批 (TRUE: 是, FALSE: 否)',
    `max_booking_duration_minutes` INT NULL COMMENT '单次预约最长时长（分钟）。如果为NULL，则表示无限制',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_base_status` (`base_status`),
    INDEX `idx_realtime_status` (`realtime_status`)
  ) ENGINE = InnoDB CHARACTER
SET
  = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '场所信息表';

-- ----------------------------
-- Table structure for venue_category
-- ----------------------------
CREATE TABLE
  `venue_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称 (e.g., 普通教室, 会议室, 实验室)',
    `usage_rules` TEXT NULL COMMENT '使用规则或策略描述',
    PRIMARY KEY (`id`)
  ) ENGINE = InnoDB CHARACTER
SET
  = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '场所分类表';

-- ----------------------------
-- Table structure for time_blocks
-- ----------------------------
CREATE TABLE
  `time_blocks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `venue_id` BIGINT NOT NULL COMMENT '场所ID, 关联 venue 表',
    `start_datetime` DATETIME NOT NULL COMMENT '占用开始时间',
    `end_datetime` DATETIME NOT NULL COMMENT '占用结束时间',
    `title` VARCHAR(255) NOT NULL COMMENT '占用事件的标题 (例如: 高等数学, 学生活动预约, 管道维修)',
    `block_type` VARCHAR(50) NOT NULL COMMENT '占用类型 (APPOINTMENT: 预约, COURSE: 课程, MAINTENANCE: 维护)',
    `reservation_id` BIGINT NULL COMMENT '如果类型是APPOINTMENT, 这里反向关联到 reservations 表',
    PRIMARY KEY (`id`),
    INDEX `idx_venue_datetime` (`venue_id`, `start_datetime`, `end_datetime`),
    INDEX `idx_reservation_id` (`reservation_id`)
  ) ENGINE = InnoDB CHARACTER
SET
  = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '场所时间占用表（调度核心）';

-- ----------------------------------------------------
-- Table structure for: reservations
-- ----------------------------------------------------
CREATE TABLE
  `reservations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约记录的主键ID',
    `venue_id` BIGINT NOT NULL COMMENT '场所ID, 冗余字段用于快速查询',
    `start_datetime` DATETIME NOT NULL COMMENT '申请的开始时间',
    `end_datetime` DATETIME NOT NULL COMMENT '申请的结束时间',
    `title` VARCHAR(255) NOT NULL COMMENT '申请的标题',
    `purpose` TEXT NULL COMMENT '预约的详细事由/目的',
    `time_block_id` BIGINT NULL COMMENT '关联到 time_blocks 表的主键ID (仅在CONFIRMED状态下有效)',
    `creator_user_id` VARCHAR(20) NOT NULL COMMENT '创建者用户ID (学号/工号)',
    `status` VARCHAR(20) NOT NULL COMMENT '状态 (PENDING, CONFIRMED, REJECTED, CANCELLED, EXPIRED)',
    `appointment_code` VARCHAR(50) NULL COMMENT '预约确认码，在状态变为 CONFIRMED 时生成',
    `approver_user_id` VARCHAR(20) NULL COMMENT '审批人用户ID',
    `approval_time` DATETIME NULL COMMENT '审批操作时间',
    `approval_comment` TEXT NULL COMMENT '审批意见',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_time_block_id` (`time_block_id`),
    UNIQUE INDEX `uk_appointment_code` (`appointment_code`),
    INDEX `idx_creator_user_id` (`creator_user_id`),
    INDEX `idx_status` (`status`)
  ) ENGINE = InnoDB CHARACTER
SET
  = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '预约业务表';

-- ----------------------------
-- Table structure for user
-- ----------------------------
CREATE TABLE
  `user` (
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
    INDEX `idx_user_type` (`user_type`) USING BTREE,
    INDEX `idx_role` (`role`) USING BTREE
  ) ENGINE = InnoDB CHARACTER
SET
  = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for venue_category_rules
-- ----------------------------
CREATE TABLE
  `venue_category_rules` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `category_id` BIGINT NOT NULL COMMENT '关联到 venue_category 表',
    `allowed_user_type` TINYINT NOT NULL COMMENT '允许的用户类型 (1:学生, 2:教师, 0:其他)',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    UNIQUE INDEX `uk_category_user_type` (`category_id`, `allowed_user_type`)
  ) ENGINE = InnoDB COMMENT = '场所分类准入规则表';

-- ----------------------------
-- Table structure for frontend_logs
-- ----------------------------
CREATE TABLE
  `frontend_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `log_level` VARCHAR(10) NOT NULL COMMENT '日志级别 (如 INFO, WARN, ERROR)',
    `log_message` TEXT NOT NULL COMMENT '日志内容',
    `log_timestamp` DATETIME NOT NULL COMMENT '前端生成日志的时间',
    `user_id` VARCHAR(20) NULL COMMENT '上报日志的用户名 (学号/工号)',
    `user_agent` TEXT NULL COMMENT '用户的浏览器或客户端信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志入库时间',
    PRIMARY KEY (`id`),
    INDEX `idx_log_level` (`log_level`),
    INDEX `idx_log_timestamp` (`log_timestamp`)
  ) ENGINE = InnoDB CHARACTER
SET
  = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '前端上报日志表';

-- ----------------------------
-- Table structure for app_files
-- ----------------------------
CREATE TABLE
  `app_files` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `related_entity_id` BIGINT NULL COMMENT '关联的业务实体ID (venue.id 或 reservations.id)，创建时可为NULL',
    `entity_type` VARCHAR(50) NOT NULL COMMENT '业务实体类型 (例如: VENUE, RESERVATION)',
    `object_name` VARCHAR(500) NOT NULL COMMENT '文件在MinIO中的对象名',
    `file_category` VARCHAR(50) NOT NULL COMMENT '文件分类 (例如: CAROUSEL, ATTACHMENT)',
    `original_filename` VARCHAR(255) NULL,
    `display_order` INT DEFAULT 0 COMMENT '显示顺序 (仅用于VENUE的CAROUSEL等)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_entity` (`related_entity_id`, `entity_type`)
  ) ENGINE = InnoDB CHARACTER
SET
  utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '全局文件关联表';