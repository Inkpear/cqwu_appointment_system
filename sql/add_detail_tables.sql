use cqwu;

-- ===================================================================
-- 新增表：预约扩展信息表 (reservation_detail)
-- ===================================================================
CREATE TABLE `reservation_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reservation_id` BIGINT NOT NULL COMMENT '预约ID，关联 reservations 表',
  `participants` TEXT NULL COMMENT '参与人员名单（前端传入的字符串，信任机制）',
  `participant_count` INT NULL DEFAULT 0 COMMENT '参与人数（前端传入，信任机制）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_reservation_id` (`reservation_id`),
  CONSTRAINT `fk_reservation_detail_reservation` 
    FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) 
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci 
COMMENT='预约扩展信息表（参与人员等）';

-- ===================================================================
-- 新增表：场所扩展信息表 (venue_detail)
-- ===================================================================
CREATE TABLE `venue_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `venue_id` BIGINT NOT NULL COMMENT '场所ID，关联 venue 表',
  `description` TEXT NULL COMMENT '场所描述（支持Markdown格式）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_venue_id` (`venue_id`),
  CONSTRAINT `fk_venue_detail_venue` 
    FOREIGN KEY (`venue_id`) REFERENCES `venue` (`id`) 
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci 
COMMENT='场所扩展信息表（描述等）';
