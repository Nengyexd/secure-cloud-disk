package com.clouddisk.controller;

import com.clouddisk.common.Result;
import com.clouddisk.dto.AccessShareDTO;
import com.clouddisk.dto.CreateShareDTO;
import com.clouddisk.service.ShareService;
import com.clouddisk.vo.ShareAccessVO;
import com.clouddisk.vo.ShareVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/share")
public class ShareController {

    @Autowired
    private ShareService shareService;

    @PostMapping("/create")
    public Result<ShareVO> createShare(
            @Validated @RequestBody CreateShareDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("创建分享: userId={}, fileId={}", userId, dto.getFileId());

        ShareVO shareVO = shareService.createShare(userId, dto);

        return Result.success("创建分享成功", shareVO);
    }

    @GetMapping("/myShares")
    public Result<List<ShareVO>> getMyShares(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("查询我的分享: userId={}", userId);

        List<ShareVO> shares = shareService.getMyShares(userId, pageNum, pageSize);

        return Result.success("查询成功", shares);
    }

    @GetMapping("/info/{shareCode}")
    public Result<ShareAccessVO> getShareInfo(@PathVariable String shareCode) {

        log.info("获取分享基本信息: shareCode={}", shareCode);

        ShareAccessVO info = shareService.getShareInfo(shareCode);

        return Result.success("查询成功", info);
    }

    @PostMapping("/access")
    public Result<ShareAccessVO> accessShare(@Validated @RequestBody AccessShareDTO dto) {

        log.info("访问分享: shareCode={}", dto.getShareCode());

        ShareAccessVO accessVO = shareService.accessShare(dto);

        return Result.success("访问成功", accessVO);
    }

    @PostMapping("/cancel/{shareId}")
    public Result<String> cancelShare(
            @PathVariable Long shareId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("取消分享: userId={}, shareId={}", userId, shareId);

        shareService.cancelShare(userId, shareId);

        return Result.success("取消分享成功", null);
    }

    @PostMapping("/download/{shareCode}")
    public Result<String> recordDownload(@PathVariable String shareCode) {

        log.info("记录下载: shareCode={}", shareCode);

        shareService.recordDownload(shareCode);

        return Result.success("记录成功", null);
    }
}
