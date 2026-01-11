package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户列表VO
 */
@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private Integer userType; // 0=普通，1=VIP，2=管理员
    private Integer status; // 0=禁用，1=正常
    private Long storageUsed;
    private Long storageQuota;
    private LocalDateTime vipExpireTime;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;

    // 统计信息
    private Long totalFiles;
    private Long totalShares;
}
