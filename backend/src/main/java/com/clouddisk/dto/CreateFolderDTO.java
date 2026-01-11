package com.clouddisk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateFolderDTO {
    @NotBlank(message = "文件夹名称不能为空")
    private String folderName;

    private Long parentId = 0L;
    private String filePath = "/";
}
