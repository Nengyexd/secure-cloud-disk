package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志VO
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
public class OperationLogVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作类型描述
     */
    private String operationTypeDesc;

    /**
     * 操作对象类型
     */
    private String objectType;

    /**
     * 操作对象类型描述
     */
    private String objectTypeDesc;

    /**
     * 操作对象ID
     */
    private Long objectId;

    /**
     * 操作对象名称
     */
    private String objectName;

    /**
     * 操作描述
     */
    private String description;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 操作结果
     */
    private String result;

    /**
     * 操作结果描述
     */
    private String resultDesc;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
}
