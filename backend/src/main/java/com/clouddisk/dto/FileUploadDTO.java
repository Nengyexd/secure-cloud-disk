package com.clouddisk.dto;

import lombok.Data;

@Data
public class FileUploadDTO {
    private Long parentId = 0L;
    private String filePath = "/";
}
