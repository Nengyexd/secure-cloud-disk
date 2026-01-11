package com.clouddisk.vo;

import lombok.Data;

@Data
public class FileDownloadVO {
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String encryptedKey;
    private Integer isFolder;
}
