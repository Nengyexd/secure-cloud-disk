package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareVO {
    private Long id;
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String shareCode;
    private String shareUrl;
    private Boolean hasPassword;
    private LocalDateTime expireTime;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer status;
    private LocalDateTime createdAt;
}
