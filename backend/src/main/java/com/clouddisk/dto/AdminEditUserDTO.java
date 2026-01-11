package com.clouddisk.dto;

import lombok.Data;

/**
 * 管理员编辑用户信息DTO
 */
@Data
public class AdminEditUserDTO {
    private Long userId;
    private String username;
    private String email;
    private Integer userType;
}
