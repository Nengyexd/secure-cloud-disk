package com.clouddisk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
@TableName("operation_log")
public class OperationLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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
     * 操作类型：UPLOAD-上传, DOWNLOAD-下载, DELETE-删除, RESTORE-恢复,
     * PERMANENT_DELETE-永久删除, SHARE-分享, CANCEL_SHARE-取消分享,
     * RENAME-重命名, MOVE-移动, CREATE_FOLDER-创建文件夹, PREVIEW-预览
     */
    private String operationType;

    /**
     * 操作对象类型：FILE-文件, FOLDER-文件夹, SHARE-分享
     */
    private String objectType;

    /**
     * 操作对象ID
     */
    private Long objectId;

    /**
     * 操作对象名称（文件名或文件夹名）
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
     * 操作结果：SUCCESS-成功, FAIL-失败
     */
    private String result;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
}
