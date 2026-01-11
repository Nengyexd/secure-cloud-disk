package com.clouddisk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RenameFileDTO {
    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    @NotBlank(message = "新文件名不能为空")
    private String newName;
}
