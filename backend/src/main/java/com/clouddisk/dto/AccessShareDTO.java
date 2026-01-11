package com.clouddisk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AccessShareDTO {
    @NotBlank(message = "分享码不能为空")
    private String shareCode;

    private String sharePassword;
}
