package com.clouddisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员 - 用户增长趋势VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserGrowthVO {

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 新增用户数
     */
    private Long newUsers;

    /**
     * 累计用户数
     */
    private Long totalUsers;
}
