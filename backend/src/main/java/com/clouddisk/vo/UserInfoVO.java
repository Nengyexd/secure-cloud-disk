package com.clouddisk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息VO
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
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
     * 用户类型：0-普通用户，1-VIP会员，2-管理员
     */
    private Integer userType;

    /**
     * VIP到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
     * 邮箱是否已验证
     */
    private Integer emailVerified;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 是否为VIP会员
     */
    private Boolean isVip;

    /**
     * 存储空间使用率（百分比）
     */
    private Double storageUsageRate;
}
