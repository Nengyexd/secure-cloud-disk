package com.clouddisk.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 发送验证码DTO
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
public class SendCodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号（邮箱或用户名）
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 验证码类型：1-注册验证，2-登录验证，3-密码重置
     */
    @NotNull(message = "验证码类型不能为空")
    private Integer codeType;
}
