package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareAccessVO {
    private Long shareId;
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer isFolder;
    private String creatorName;
    private Integer creatorUserType; // 分享者用户类型：0=普通，1=VIP，2=管理员
    private LocalDateTime expireTime;
    private Boolean hasPassword;
    private Integer status;
}
