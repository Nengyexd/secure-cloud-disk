package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员分享管理VO
 */
@Data
public class AdminShareVO {
    private Long id;
    private Long userId;
    private String username; // 分享者用户名
    private Long fileId;
    private String fileName; // 分享的文件名
    private String shareCode;
    private Boolean hasPassword;
    private LocalDateTime expireTime;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer status; // 0=已取消，1=正常
    private LocalDateTime createdAt;
}
