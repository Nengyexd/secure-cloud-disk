package com.clouddisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
@TableName("users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 密码哈希（PBKDF2加密）
     */
    private String passwordHash;

    /**
     * 密码盐值
     */
    private String salt;

    /**
     * 加密后的主密钥（用于加密文件密钥）
     */
    private String masterKeyEncrypted;

    /**
     * 用户类型：0-普通用户，1-VIP会员，2-管理员
     */
    private Integer userType;

    /**
     * VIP到期时间
     */
    private LocalDateTime vipExpireTime;

    /**
     * 存储配额（字节）
     */
    private Long storageQuota;

    /**
     * 已使用存储空间（字节）
     */
    private Long storageUsed;

    /**
     * 账户状态：0-冻结，1-正常
     */
    private Integer status;

    /**
     * 邮箱是否已验证：0-未验证，1-已验证
     */
    private Integer emailVerified;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;
}
