package com.clouddisk.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CreateShareDTO {
    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    private String sharePassword;

    private Integer expireDays; // 过期天数，null表示永久有效
}
