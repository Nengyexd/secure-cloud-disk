package com.clouddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clouddisk.common.BusinessException;
import com.clouddisk.entity.Favorite;
import com.clouddisk.entity.File;
import com.clouddisk.mapper.FavoriteMapper;
import com.clouddisk.mapper.FileMapper;
import com.clouddisk.vo.FileItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private FileMapper fileMapper;

    /**
     * 添加收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long fileId) {
        // 检查文件是否存在
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 检查文件是否属于该用户
        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("无权收藏该文件");
        }

        // 检查是否已收藏
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getFileId, fileId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已收藏该文件");
        }

        // 添加收藏
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setFileId(fileId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);

        log.info("添加收藏成功: userId={}, fileId={}, fileName={}", userId, fileId, file.getFileName());
    }

    /**
     * 取消收藏
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long fileId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getFileId, fileId);
        int result = favoriteMapper.delete(wrapper);

        if (result == 0) {
            throw new BusinessException("收藏不存在");
        }

        log.info("取消收藏成功: userId={}, fileId={}", userId, fileId);
    }

    /**
     * 获取我的收藏列表
     */
    public List<FileItemVO> getMyFavorites(Long userId) {
        // 查询收藏记录
        LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreatedAt);
        List<Favorite> favorites = favoriteMapper.selectList(favoriteWrapper);

        if (favorites.isEmpty()) {
            return List.of();
        }

        // 查询文件信息
        List<Long> fileIds = favorites.stream().map(Favorite::getFileId).collect(Collectors.toList());
        LambdaQueryWrapper<File> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.in(File::getId, fileIds).eq(File::getStatus, 1); // 只查询正常状态的文件
        List<File> files = fileMapper.selectList(fileWrapper);

        // 转换为VO
        return files.stream().map(file -> {
            FileItemVO vo = new FileItemVO();
            BeanUtils.copyProperties(file, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 检查文件是否已收藏
     */
    public boolean isFavorite(Long userId, Long fileId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getFileId, fileId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}
