package com.clouddisk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clouddisk.common.Result;
import com.clouddisk.dto.OperationLogQueryDTO;
import com.clouddisk.service.OperationLogService;
import com.clouddisk.vo.OperationLogVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/operation-log")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取当前用户操作日志
     */
    @GetMapping("/my")
    public Result<Page<OperationLogVO>> getMyOperationLogs(
            OperationLogQueryDTO dto,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Page<OperationLogVO> logs = operationLogService.getUserOperationLogs(userId, dto);
        return Result.success(logs);
    }
}
