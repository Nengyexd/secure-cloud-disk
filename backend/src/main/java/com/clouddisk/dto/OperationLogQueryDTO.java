package com.clouddisk.dto;

import lombok.Data;

/**
 * 操作日志查询DTO
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
public class OperationLogQueryDTO {
    /**
     * 用户ID（管理员查询时使用）
     */
    private Long userId;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作结果
     */
    private String result;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;
}
