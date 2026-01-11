package com.clouddisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadVO {
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private Boolean isInstantUpload;
}
