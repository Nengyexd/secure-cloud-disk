package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员文件管理VO
 */
@Data
public class AdminFileVO {
    private Long id;
    private Long userId;
    private String username; // 所属用户名
    private String fileName;
    private Long fileSize;
    private String fileType;
    private Integer isFolder;
    private Integer status; // 0=已删除，1=正常
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
