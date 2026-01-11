package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志VO
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
public class LoginLogVO {
    private Long id;
    private String loginIp;
    private String loginLocation;  // 登录地点（可选）
    private String loginDevice;     // 登录设备（可选）
    private Boolean success;        // 登录是否成功
    private String message;         // 登录消息
    private LocalDateTime loginTime;
}
