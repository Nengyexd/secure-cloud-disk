package com.clouddisk.dto;

import lombok.Data;

/**
 * 管理员用户列表查询DTO
 */
@Data
public class AdminUserQueryDTO {
    private String keyword; // 搜索关键字（用户名或邮箱）
    private Integer userType; // 用户类型筛选：0=普通，1=VIP，2=管理员，null=全部
    private Integer status; // 账户状态：0=禁用，1=正常，null=全部
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
