package com.clouddisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应VO
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JWT Token
     */
    private String token;

    /**
     * Token类型
     */
    private String tokenType = "Bearer";

    /**
     * Token过期时间（毫秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserInfoVO userInfo;

    /**
     * 解密后的主密钥（用于加密文件）
     */
    private String masterKey;

    public LoginVO(String token, Long expiresIn, UserInfoVO userInfo) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userInfo = userInfo;
    }

    public LoginVO(String token, Long expiresIn, UserInfoVO userInfo, String masterKey) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userInfo = userInfo;
        this.masterKey = masterKey;
    }
}
