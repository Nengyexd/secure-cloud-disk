package com.clouddisk.controller;

import com.clouddisk.common.BusinessException;
import com.clouddisk.common.Result;
import com.clouddisk.dto.FixAccountDTO;
import com.clouddisk.dto.ResetPasswordDTO;
import com.clouddisk.dto.SendCodeDTO;
import com.clouddisk.dto.UserLoginDTO;
import com.clouddisk.dto.UserRegisterDTO;
import com.clouddisk.entity.User;
import com.clouddisk.service.UserService;
import com.clouddisk.service.VerificationCodeService;
import com.clouddisk.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @PostMapping("/send-code")
    public Result<String> sendCode(@Validated @RequestBody SendCodeDTO sendCodeDTO) {
        log.info("发送验证码请求：account={}, codeType={}", sendCodeDTO.getAccount(), sendCodeDTO.getCodeType());

        String account = sendCodeDTO.getAccount();
        String email;

        // 判断account是邮箱还是用户名
        if (account.contains("@")) {
            // 是邮箱
            email = account;
        } else {
            // 是用户名，需要查询对应的邮箱
            User user = userService.getUserByUsername(account);
            if (user == null) {
                throw new BusinessException("该用户名不存在");
            }
            email = user.getEmail();
        }

        verificationCodeService.sendCode(email, sendCodeDTO.getCodeType());
        return Result.success("验证码发送成功，请查收邮件", null);
    }

    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody UserRegisterDTO registerDTO,
                                  HttpServletRequest request) {
        log.info("用户注册请求：username={}, email={}", registerDTO.getUsername(), registerDTO.getEmail());

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(registerDTO.getUsername());
        dto.setEmail(registerDTO.getEmail());
        dto.setPassword(registerDTO.getPassword());
        dto.setVerificationCode(registerDTO.getVerificationCode());

        userService.register(dto, request);
        return Result.success("注册成功，请登录", null);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody UserLoginDTO loginDTO,
                                  HttpServletRequest request) {
        log.info("用户登录请求：account={}", loginDTO.getAccount());

        UserLoginDTO dto = new UserLoginDTO();
        dto.setAccount(loginDTO.getAccount());
        dto.setPassword(loginDTO.getPassword());
        dto.setVerificationCode(loginDTO.getVerificationCode());

        LoginVO loginVO = userService.login(dto, request);
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@Validated @RequestBody ResetPasswordDTO resetPasswordDTO) {
        log.info("重置密码请求：email={}", resetPasswordDTO.getEmail());

        // 验证验证码（类型3：密码重置）
        boolean codeValid = verificationCodeService.verifyCode(
                resetPasswordDTO.getEmail(),
                resetPasswordDTO.getVerificationCode(),
                3
        );

        if (!codeValid) {
            throw new BusinessException("验证码错误或已过期");
        }

        userService.resetPassword(resetPasswordDTO);
        return Result.success("密码重置成功，之前的文件已清空，请重新登录", null);
    }

    @PostMapping("/fix-account")
    public Result<String> fixAccount(@Validated @RequestBody FixAccountDTO fixAccountDTO) {
        log.info("修复账号请求：email={}", fixAccountDTO.getEmail());

        // 验证验证码（类型3：密码重置）
        boolean codeValid = verificationCodeService.verifyCode(
                fixAccountDTO.getEmail(),
                fixAccountDTO.getVerificationCode(),
                3
        );

        if (!codeValid) {
            throw new BusinessException("验证码错误或已过期");
        }

        userService.fixAccount(fixAccountDTO);
        return Result.success("账号修复成功，文件已保留，请使用新密码登录", null);
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("认证模块测试成功", null);
    }
}
