package com.clouddisk.controller;

import com.clouddisk.common.Result;
import com.clouddisk.dto.UpdatePasswordDTO;
import com.clouddisk.dto.UpdateUserInfoDTO;
import com.clouddisk.service.UserService;
import com.clouddisk.vo.LoginLogVO;
import com.clouddisk.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    public Result<UserDetailVO> getUserInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("获取用户信息: userId={}", userId);

        UserDetailVO userDetail = userService.getUserDetail(userId);

        return Result.success("查询成功", userDetail);
    }

    @PutMapping("/info")
    public Result<String> updateUserInfo(
            @Validated @RequestBody UpdateUserInfoDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("更新用户信息: userId={}", userId);

        userService.updateUserInfo(userId, dto);

        return Result.success("更新成功", null);
    }

    @PutMapping("/password")
    public Result<String> updatePassword(
            @Validated @RequestBody UpdatePasswordDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("修改密码: userId={}", userId);

        userService.updatePassword(userId, dto);

        return Result.success("密码修改成功", null);
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("上传头像: userId={}", userId);

        String avatarUrl = userService.uploadAvatar(userId, file);

        return Result.success("头像上传成功", avatarUrl);
    }

    @GetMapping("/login-logs")
    public Result<java.util.List<LoginLogVO>> getLoginLogs(
            @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("获取登录日志: userId={}, limit={}", userId, limit);

        java.util.List<LoginLogVO> logs = userService.getLoginLogs(userId, limit);

        return Result.success("查询成功", logs);
    }

    @PostMapping("/delete")
    public Result<String> deleteAccount(
            @RequestBody java.util.Map<String, String> params,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        String password = params.get("password");
        log.info("请求注销账号: userId={}", userId);

        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }

        userService.deleteAccount(userId, password);

        return Result.success("账号已注销", null);
    }

    @PostMapping("/vip/upgrade")
    public Result<String> upgradeToVip(
            @RequestBody java.util.Map<String, Integer> params,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        Integer days = params.get("days");
        log.info("请求升级VIP: userId={}, days={}", userId, days);

        if (days == null || days <= 0) {
            return Result.error("天数必须大于0");
        }

        userService.upgradeToVip(userId, days);

        return Result.success("VIP开通/续费成功", null);
    }
}
