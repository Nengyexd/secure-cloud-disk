package com.clouddisk.vo;

import lombok.Data;

/**
 * 管理员系统统计VO
 */
@Data
public class AdminStatisticsVO {
    // 用户统计
    private Long totalUsers; // 总用户数
    private Long normalUsers; // 普通用户数
    private Long vipUsers; // VIP用户数
    private Long adminUsers; // 管理员数
    private Long todayNewUsers; // 今日新增用户

    // 文件统计
    private Long totalFiles; // 总文件数
    private Long totalFolders; // 总文件夹数
    private Long todayNewFiles; // 今日新增文件

    // 存储统计
    private Long totalStorageUsed; // 总存储使用量（字节）
    private Long totalStorageQuota; // 总存储配额（字节）
    private Double storageUsageRate; // 存储使用率（百分比）

    // 分享统计
    private Long totalShares; // 总分享数
    private Long activeShares; // 有效分享数

    // 登录统计
    private Long todayLogins; // 今日登录次数
    private Long todaySuccessLogins; // 今日成功登录次数
}
