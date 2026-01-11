package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDetailVO {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private Integer userType;  // 0=普通用户, 1=VIP
    private Long storageUsed;
    private Long storageQuota;
    private LocalDateTime createdAt;
    private LocalDateTime vipExpireTime;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;

    // 统计信息
    private Long totalFiles;     // 总文件数
    private Long totalFolders;   // 总文件夹数
    private Long totalShares;    // 总分享数
}
