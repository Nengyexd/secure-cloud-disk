package com.clouddisk.controller;

import com.clouddisk.common.Result;
import com.clouddisk.service.FavoriteService;
import com.clouddisk.vo.FileItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏控制器
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 添加收藏
     */
    @PostMapping("/add/{fileId}")
    public Result<Void> addFavorite(@PathVariable Long fileId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("添加收藏: userId={}, fileId={}", userId, fileId);

        favoriteService.addFavorite(userId, fileId);
        return Result.success("收藏成功", null);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/remove/{fileId}")
    public Result<Void> removeFavorite(@PathVariable Long fileId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("取消收藏: userId={}, fileId={}", userId, fileId);

        favoriteService.removeFavorite(userId, fileId);
        return Result.success("取消收藏成功", null);
    }

    /**
     * 获取我的收藏列表
     */
    @GetMapping("/my")
    public Result<List<FileItemVO>> getMyFavorites(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("查询收藏列表: userId={}", userId);

        List<FileItemVO> favorites = favoriteService.getMyFavorites(userId);
        return Result.success("查询成功", favorites);
    }

    /**
     * 检查文件是否已收藏
     */
    @GetMapping("/check/{fileId}")
    public Result<Boolean> checkFavorite(@PathVariable Long fileId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isFavorite = favoriteService.isFavorite(userId, fileId);
        return Result.success(null, isFavorite);
    }
}
