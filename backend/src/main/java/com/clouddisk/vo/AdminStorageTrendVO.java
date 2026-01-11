package com.clouddisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员 - 存储空间使用趋势VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStorageTrendVO {

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 存储使用量（字节）
     */
    private Long storageUsed;

    /**
     * 存储配额（字节）
     */
    private Long storageQuota;
}
