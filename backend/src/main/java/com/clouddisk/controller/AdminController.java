package com.clouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clouddisk.common.Result;
import com.clouddisk.dto.AdminEditUserDTO;
import com.clouddisk.dto.AdminUserQueryDTO;
import com.clouddisk.dto.AdminVipOperationDTO;
import com.clouddisk.service.AdminService;
import com.clouddisk.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取系统统计数据
     */
    @GetMapping("/statistics")
    public Result<AdminStatisticsVO> getStatistics(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("管理员查询系统统计: adminId={}", userId);

        AdminStatisticsVO statistics = adminService.getStatistics();
        return Result.success("查询成功", statistics);
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public Result<Page<AdminUserVO>> getUserList(
            @ModelAttribute AdminUserQueryDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("管理员查询用户列表: adminId={}, keyword={}, userType={}",
                userId, dto.getKeyword(), dto.getUserType());

        Page<AdminUserVO> userPage = adminService.getUserList(dto);
        return Result.success("查询成功", userPage);
    }

    /**
     * 编辑用户信息
     */
    @PutMapping("/users")
    public Result<String> editUser(
            @RequestBody AdminEditUserDTO dto,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员编辑用户: adminId={}, userId={}", adminId, dto.getUserId());

        adminService.editUser(adminId, dto);
        return Result.success("编辑成功", null);
    }

    /**
     * 禁用/启用用户
     */
    @PostMapping("/users/{userId}/toggle-status")
    public Result<String> toggleUserStatus(
            @PathVariable Long userId,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员切换用户状态: adminId={}, userId={}", adminId, userId);

        adminService.toggleUserStatus(adminId, userId);
        return Result.success("操作成功", null);
    }

    /**
     * 管理员开通/续费VIP
     */
    @PostMapping("/users/vip")
    public Result<String> operateVip(
            @RequestBody AdminVipOperationDTO dto,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员操作VIP: adminId={}, userId={}, days={}",
                adminId, dto.getUserId(), dto.getDays());

        adminService.operateVip(adminId, dto);
        return Result.success("VIP操作成功", null);
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/files")
    public Result<Page<AdminFileVO>> getFileList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String fileType,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员查询文件列表: adminId={}, userId={}, fileType={}", adminId, userId, fileType);

        Page<AdminFileVO> filePage = adminService.getFileList(pageNum, pageSize, userId, fileType);
        return Result.success("查询成功", filePage);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/files/{fileId}")
    public Result<String> deleteFile(
            @PathVariable Long fileId,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员删除文件: adminId={}, fileId={}", adminId, fileId);

        adminService.deleteFile(adminId, fileId);
        return Result.success("删除成功", null);
    }

    /**
     * 获取分享列表
     */
    @GetMapping("/shares")
    public Result<Page<AdminShareVO>> getShareList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员查询分享列表: adminId={}, userId={}, status={}", adminId, userId, status);

        Page<AdminShareVO> sharePage = adminService.getShareList(pageNum, pageSize, userId, status);
        return Result.success("查询成功", sharePage);
    }

    /**
     * 取消分享
     */
    @PostMapping("/shares/{shareId}/cancel")
    public Result<String> cancelShare(
            @PathVariable Long shareId,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员取消分享: adminId={}, shareId={}", adminId, shareId);

        adminService.cancelShare(adminId, shareId);
        return Result.success("取消成功", null);
    }

    /**
     * 获取登录日志
     */
    @GetMapping("/logs/login")
    public Result<Page<LoginLogVO>> getLoginLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员查询登录日志: adminId={}, userId={}, status={}", adminId, userId, status);

        Page<LoginLogVO> logPage = adminService.getLoginLogs(pageNum, pageSize, userId, status);
        return Result.success("查询成功", logPage);
    }

    /**
     * 获取操作日志（管理员）
     */
    @GetMapping("/logs/operation")
    public Result<Page<OperationLogVO>> getOperationLogs(
            @ModelAttribute com.clouddisk.dto.OperationLogQueryDTO dto,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员查询操作日志: adminId={}, userId={}, operationType={}",
                adminId, dto.getUserId(), dto.getOperationType());

        Page<OperationLogVO> logPage = adminService.getOperationLogs(dto);
        return Result.success("查询成功", logPage);
    }

    /**
     * 获取用户增长趋势
     */
    @GetMapping("/statistics/user-growth")
    public Result<List<AdminUserGrowthVO>> getUserGrowthTrend(Authentication authentication) {
        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员查询用户增长趋势: adminId={}", adminId);

        List<AdminUserGrowthVO> trend = adminService.getUserGrowthTrend();
        return Result.success("查询成功", trend);
    }

    /**
     * 获取文件操作量趋势
     */
    @GetMapping("/statistics/file-operation")
    public Result<List<AdminFileOperationVO>> getFileOperationTrend(Authentication authentication) {
        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员查询文件操作量趋势: adminId={}", adminId);

        List<AdminFileOperationVO> trend = adminService.getFileOperationTrend();
        return Result.success("查询成功", trend);
    }

    /**
     * 获取存储空间使用趋势
     */
    @GetMapping("/statistics/storage-trend")
    public Result<List<AdminStorageTrendVO>> getStorageTrend(Authentication authentication) {
        Long adminId = (Long) authentication.getPrincipal();
        log.info("管理员查询存储空间使用趋势: adminId={}", adminId);

        List<AdminStorageTrendVO> trend = adminService.getStorageTrend();
        return Result.success("查询成功", trend);
    }
}
