package com.clouddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clouddisk.common.BusinessException;
import com.clouddisk.dto.AccessShareDTO;
import com.clouddisk.dto.CreateShareDTO;
import com.clouddisk.entity.File;
import com.clouddisk.entity.Share;
import com.clouddisk.entity.User;
import com.clouddisk.mapper.FileMapper;
import com.clouddisk.mapper.ShareMapper;
import com.clouddisk.mapper.UserMapper;
import com.clouddisk.vo.ShareAccessVO;
import com.clouddisk.vo.ShareVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShareService {

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OperationLogService operationLogService;

    @Value("${business.share.base-url:http://localhost:5173/share/}")
    private String shareBaseUrl;

    @Value("${business.share.normal-max-days}")
    private Integer normalMaxDays;

    @Value("${business.share.vip-max-days}")
    private Integer vipMaxDays;

    /**
     * 创建分享
     */
    @Transactional(rollbackFor = Exception.class)
    public ShareVO createShare(Long userId, CreateShareDTO dto) {
        // 验证用户权限
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证分享有效期权限
        if (user.getUserType() == 0) { // 普通用户
            if (dto.getExpireDays() == null || dto.getExpireDays() <= 0) {
                throw new BusinessException("普通用户不支持永久分享，请升级VIP");
            }
            if (dto.getExpireDays() > normalMaxDays) {
                throw new BusinessException("普通用户分享有效期最长" + normalMaxDays + "天，请升级VIP");
            }
        }
        // VIP用户不做限制（vipMaxDays 为 -1）

        // 验证文件
        File file = fileMapper.selectById(dto.getFileId());
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException("文件不存在");
        }

        if (file.getStatus() == 0) {
            throw new BusinessException("回收站中的文件无法分享");
        }

        // 生成唯一分享码
        String shareCode = generateShareCode();
        while (shareCodeExists(shareCode)) {
            shareCode = generateShareCode();
        }

        // 计算过期时间
        LocalDateTime expireTime = null;
        if (dto.getExpireDays() != null && dto.getExpireDays() > 0) {
            expireTime = LocalDateTime.now().plusDays(dto.getExpireDays());
        }

        // 创建分享记录
        Share share = new Share();
        share.setUserId(userId);
        share.setFileId(file.getId());
        share.setShareCode(shareCode);
        share.setSharePassword(dto.getSharePassword());
        share.setExpireTime(expireTime);
        share.setViewCount(0);
        share.setDownloadCount(0);
        share.setStatus(1);

        shareMapper.insert(share);

        log.info("创建分享成功: userId={}, fileId={}, shareCode={}", userId, dto.getFileId(), shareCode);

        // 记录操作日志
        operationLogService.log(userId, user.getUsername(), "SHARE", "FILE",
                file.getId(), file.getFileName(), "分享文件: " + file.getFileName());

        // 返回VO
        ShareVO vo = new ShareVO();
        vo.setId(share.getId());
        vo.setFileId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setShareCode(shareCode);
        vo.setShareUrl(shareBaseUrl + shareCode);
        vo.setHasPassword(StringUtils.hasText(dto.getSharePassword()));
        vo.setExpireTime(expireTime);
        vo.setViewCount(0);
        vo.setDownloadCount(0);
        vo.setStatus(1);
        vo.setCreatedAt(share.getCreatedAt());

        return vo;
    }

    /**
     * 获取我的分享列表
     */
    public List<ShareVO> getMyShares(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getUserId, userId)
                .orderByDesc(Share::getCreatedAt);

        Page<Share> page = new Page<>(pageNum, pageSize);
        Page<Share> result = shareMapper.selectPage(page, wrapper);

        return result.getRecords().stream().map(share -> {
            ShareVO vo = new ShareVO();
            BeanUtils.copyProperties(share, vo);

            // 加载文件信息
            File file = fileMapper.selectById(share.getFileId());
            if (file != null) {
                vo.setFileName(file.getFileName());
                vo.setFileType(file.getFileType());
                vo.setFileSize(file.getFileSize());
            }

            vo.setShareUrl(shareBaseUrl + share.getShareCode());
            vo.setHasPassword(StringUtils.hasText(share.getSharePassword()));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取分享基本信息（不需要密码）
     */
    public ShareAccessVO getShareInfo(String shareCode) {
        // 查询分享记录
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getShareCode, shareCode);
        Share share = shareMapper.selectOne(wrapper);

        if (share == null) {
            throw new BusinessException("分享不存在");
        }

        // 检查状态
        if (share.getStatus() == 0) {
            throw new BusinessException("分享已失效");
        }

        // 检查是否过期
        if (share.getExpireTime() != null && LocalDateTime.now().isAfter(share.getExpireTime())) {
            // 更新状态为失效
            share.setStatus(0);
            shareMapper.updateById(share);
            throw new BusinessException("分享已过期");
        }

        // 查询文件信息
        File file = fileMapper.selectById(share.getFileId());
        if (file == null || file.getStatus() == 0) {
            throw new BusinessException("文件不存在或已删除");
        }

        // 查询创建者信息
        User user = userMapper.selectById(share.getUserId());

        // 构建返回信息（不包含敏感信息）
        ShareAccessVO vo = new ShareAccessVO();
        vo.setShareId(share.getId());
        vo.setFileId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setIsFolder(file.getIsFolder());
        vo.setCreatorName(user != null ? user.getUsername() : "未知用户");
        vo.setCreatorUserType(user != null ? user.getUserType() : 0); // 设置分享者用户类型
        vo.setExpireTime(share.getExpireTime());
        vo.setHasPassword(StringUtils.hasText(share.getSharePassword()));
        vo.setStatus(share.getStatus());

        return vo;
    }

    /**
     * 访问分享
     */
    public ShareAccessVO accessShare(AccessShareDTO dto) {
        // 查询分享记录
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getShareCode, dto.getShareCode());
        Share share = shareMapper.selectOne(wrapper);

        if (share == null) {
            throw new BusinessException("分享不存在");
        }

        // 检查状态
        if (share.getStatus() == 0) {
            throw new BusinessException("分享已失效");
        }

        // 检查是否过期
        if (share.getExpireTime() != null && LocalDateTime.now().isAfter(share.getExpireTime())) {
            // 更新状态为失效
            share.setStatus(0);
            shareMapper.updateById(share);
            throw new BusinessException("分享已过期");
        }

        // 检查密码
        if (StringUtils.hasText(share.getSharePassword())) {
            if (!StringUtils.hasText(dto.getSharePassword())) {
                throw new BusinessException("请输入分享密码");
            }
            if (!share.getSharePassword().equals(dto.getSharePassword())) {
                throw new BusinessException("分享密码错误");
            }
        }

        // 增加查看次数
        share.setViewCount(share.getViewCount() + 1);
        shareMapper.updateById(share);

        // 查询文件信息
        File file = fileMapper.selectById(share.getFileId());
        if (file == null || file.getStatus() == 0) {
            throw new BusinessException("文件不存在或已删除");
        }

        // 查询创建者信息
        User user = userMapper.selectById(share.getUserId());

        // 构建返回信息
        ShareAccessVO vo = new ShareAccessVO();
        vo.setShareId(share.getId());
        vo.setFileId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setIsFolder(file.getIsFolder());
        vo.setCreatorName(user != null ? user.getUsername() : "未知用户");
        vo.setCreatorUserType(user != null ? user.getUserType() : 0); // 设置分享者用户类型
        vo.setExpireTime(share.getExpireTime());
        vo.setHasPassword(StringUtils.hasText(share.getSharePassword()));
        vo.setStatus(share.getStatus());

        log.info("访问分享成功: shareCode={}, fileId={}", dto.getShareCode(), file.getId());

        return vo;
    }

    /**
     * 取消分享
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long userId, Long shareId) {
        Share share = shareMapper.selectById(shareId);

        if (share == null || !share.getUserId().equals(userId)) {
            throw new BusinessException("分享不存在");
        }

        // 获取文件信息用于日志记录
        File file = fileMapper.selectById(share.getFileId());
        User user = userMapper.selectById(userId);

        share.setStatus(0);
        shareMapper.updateById(share);

        // 记录操作日志
        if (user != null && file != null) {
            operationLogService.log(userId, user.getUsername(), "CANCEL_SHARE", "SHARE",
                    shareId, file.getFileName(), "取消分享: " + file.getFileName());
        }

        log.info("取消分享成功: userId={}, shareId={}", userId, shareId);
    }

    /**
     * 记录下载次数
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordDownload(String shareCode) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getShareCode, shareCode);
        Share share = shareMapper.selectOne(wrapper);

        if (share != null) {
            share.setDownloadCount(share.getDownloadCount() + 1);
            shareMapper.updateById(share);
        }
    }

    /**
     * 生成分享码
     */
    private String generateShareCode() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    /**
     * 检查分享码是否已存在
     */
    private boolean shareCodeExists(String shareCode) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getShareCode, shareCode);
        return shareMapper.selectCount(wrapper) > 0;
    }
}
