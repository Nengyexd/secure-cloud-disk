package com.clouddisk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮箱验证码实体类
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
@TableName("email_verification_codes")
public class EmailVerificationCode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 验证码
     */
    private String code;

    /**
     * 验证码类型：1-注册验证，2-登录验证，3-密码重置
     */
    private Integer codeType;

    /**
     * 是否已使用：0-未使用，1-已使用
     */
    private Integer used;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
