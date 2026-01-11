package com.clouddisk.dto;

import lombok.Data;

@Data
public class FileListDTO {
    private Long parentId = 0L;
    private String sortBy = "created_at";
    private String sortOrder = "desc";
    private String fileType;
    private String category;
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 50;
}
