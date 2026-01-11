-- =============================================
-- 基于AES加密的安全云盘系统 - 数据库初始化脚本
-- 版本: 1.0
-- 更新时间: 2026-01-10
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS secure_cloud_disk DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE secure_cloud_disk;

-- =============================================
-- 1. 用户表 (users)
-- =============================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希（PBKDF2加密）',
  `salt` VARCHAR(64) NOT NULL COMMENT '密码盐值',
  `master_key_encrypted` VARCHAR(512) NOT NULL COMMENT '加密后的主密钥（用于加密文件密钥）',
  `user_type` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '用户类型：0-普通用户，1-VIP会员，2-管理员',
  `vip_expire_time` DATETIME NULL COMMENT 'VIP到期时间',
  `storage_quota` BIGINT(20) NOT NULL DEFAULT 5368709120 COMMENT '存储配额（字节），默认5GB',
  `storage_used` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '已使用存储空间（字节）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账户状态：0-冻结，1-正常',
  `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证：0-未验证，1-已验证',
  `avatar` VARCHAR(255) NULL COMMENT '用户头像URL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` DATETIME NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) NULL COMMENT '最后登录IP',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. 邮箱验证码表 (email_verification_codes)
-- =============================================
DROP TABLE IF EXISTS `email_verification_codes`;
CREATE TABLE `email_verification_codes` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '验证码ID',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
  `code` VARCHAR(10) NOT NULL COMMENT '验证码',
  `code_type` TINYINT(1) NOT NULL COMMENT '验证码类型：1-注册验证，2-登录验证，3-密码重置',
  `used` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已使用：0-未使用，1-已使用',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_email_code` (`email`, `code`, `code_type`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';

-- =============================================
-- 3. 登录日志表 (login_logs)
-- =============================================
DROP TABLE IF EXISTS `login_logs`;
CREATE TABLE `login_logs` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT(20) NULL COMMENT '用户ID（登录失败时可能为空）',
  `username` VARCHAR(50) NULL COMMENT '用户名',
  `email` VARCHAR(100) NULL COMMENT '邮箱',
  `login_ip` VARCHAR(50) NOT NULL COMMENT '登录IP',
  `login_location` VARCHAR(100) NULL COMMENT '登录地点',
  `device_info` VARCHAR(255) NULL COMMENT '设备信息（User-Agent）',
  `login_status` TINYINT(1) NOT NULL COMMENT '登录状态：0-失败，1-成功',
  `fail_reason` VARCHAR(255) NULL COMMENT '失败原因',
  `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_status` (`login_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- =============================================
-- 4. 文件表 (files)
-- =============================================
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT(20) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_path` VARCHAR(500) NOT NULL,
  `file_size` BIGINT(20) NOT NULL,
  `file_hash` VARCHAR(64) NULL COMMENT 'SHA-256文件哈希，用于秒传',
  `file_type` VARCHAR(100) NULL COMMENT '文件MIME类型',
  `encrypted_key` VARCHAR(512) NULL COMMENT '加密后的文件密钥（文件夹可为空）',
  `storage_key` VARCHAR(255) NULL COMMENT 'MinIO存储键（文件夹可为空）',
  `thumbnail_key` VARCHAR(255) NULL COMMENT '缩略图存储键（仅图片文件）',
  `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父文件夹ID，0表示根目录',
  `is_folder` TINYINT(1) DEFAULT 0 COMMENT '是否为文件夹：0-文件，1-文件夹',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-回收站，1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_file_hash` (`file_hash`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- =============================================
-- 5. 文件分享表 (shares)
-- =============================================
DROP TABLE IF EXISTS `shares`;
CREATE TABLE `shares` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `file_id` BIGINT NOT NULL COMMENT '文件ID',
  `share_code` VARCHAR(10) NOT NULL COMMENT '分享码',
  `share_password` VARCHAR(20) DEFAULT NULL COMMENT '分享密码',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `view_count` INT DEFAULT 0 COMMENT '查看次数',
  `download_count` INT DEFAULT 0 COMMENT '下载次数',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-失效',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

-- =============================================
-- 6. 操作日志表 (operation_log)
-- =============================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `operation_type` VARCHAR(30) NOT NULL COMMENT '操作类型',
    `object_type` VARCHAR(20) NOT NULL COMMENT '操作对象类型',
    `object_id` BIGINT COMMENT '操作对象ID',
    `object_name` VARCHAR(255) COMMENT '操作对象名称',
    `description` VARCHAR(500) COMMENT '操作描述',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `result` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
    `error_message` TEXT COMMENT '错误信息',
    `operation_time` DATETIME NOT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =============================================
-- 7. 文件收藏表 (favorite)
-- =============================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `file_id` BIGINT NOT NULL COMMENT '文件ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_file` (`user_id`, `file_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件收藏表';

-- =============================================
-- 8. VIP订单表 (vip_orders)
-- =============================================
DROP TABLE IF EXISTS `vip_orders`;
CREATE TABLE `vip_orders` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    `plan_type` TINYINT NOT NULL COMMENT '套餐类型：1=月卡，2=季卡，3=年卡',
    `days` INT NOT NULL COMMENT '开通天数',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `payment_method` VARCHAR(20) COMMENT '支付方式：alipay/wechat/balance',
    `payment_status` TINYINT DEFAULT 0 COMMENT '支付状态：0=待支付，1=已支付，2=已取消',
    `payment_time` DATETIME COMMENT '支付时间',
    `expire_time` DATETIME NOT NULL COMMENT '到期时间',
    `remark` VARCHAR(500) COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_payment_status` (`payment_status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP订单表';

-- =============================================
-- 9. VIP提醒记录表 (vip_reminders)
-- =============================================
DROP TABLE IF EXISTS `vip_reminders`;
CREATE TABLE `vip_reminders` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提醒ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `remind_type` TINYINT NOT NULL COMMENT '提醒类型：1=7天到期，2=3天到期，3=1天到期，4=已过期',
    `remind_time` DATETIME NOT NULL COMMENT '提醒时间',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：0=未读，1=已读',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_remind_type` (`remind_type`),
    KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP到期提醒记录表';

-- =============================================
-- 索引优化说明
-- =============================================
-- 1. users表：username和email使用唯一索引，支持快速登录查询
-- 2. email_verification_codes表：email+code+code_type组合索引，支持验证码校验
-- 3. login_logs表：user_id和login_time索引，支持用户登录历史查询
-- 4. files表：user_id、parent_id、file_hash、status索引，支持文件列表和秒传查询
-- 5. shares表：share_code唯一索引，user_id和file_id索引，支持分享链接快速查询
-- 6. operation_log表：user_id、operation_type、operation_time索引，支持操作日志查询
-- 7. favorite表：user_id+file_id唯一索引，防止重复收藏
-- 8. vip_orders表：order_no唯一索引，user_id和payment_status索引，支持订单查询
-- 9. vip_reminders表：user_id索引，支持用户提醒记录查询

-- =============================================
-- 完成提示
-- =============================================
SELECT '数据库初始化完成！' AS status;
SELECT '所有表已创建成功。' AS message;
