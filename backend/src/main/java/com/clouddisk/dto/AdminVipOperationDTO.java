package com.clouddisk.dto;

import lombok.Data;

/**
 * 管理员开通/续费VIP DTO
 */
@Data
public class AdminVipOperationDTO {
    private Long userId;
    private Integer days;
}
