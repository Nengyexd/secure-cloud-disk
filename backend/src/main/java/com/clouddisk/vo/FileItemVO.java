package com.clouddisk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileItemVO {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private Integer isFolder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long parentId;
    private String filePath;
    private Boolean isFavorite; // 是否已收藏
    private String thumbnailKey; // 缩略图存储键
}
