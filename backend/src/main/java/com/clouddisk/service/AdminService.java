package com.clouddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clouddisk.common.BusinessException;
import com.clouddisk.dto.AdminEditUserDTO;
import com.clouddisk.dto.AdminUserQueryDTO;
import com.clouddisk.dto.AdminVipOperationDTO;
import com.clouddisk.entity.*;
import com.clouddisk.mapper.*;
import com.clouddisk.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员服务类
 */
@Slf4j
@Service
public class AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Value("${business.storage.vip-quota}")
    private Long vipQuota;

    @Value("${business.storage.normal-quota}")
    private Long normalQuota;

    /**
     * 获取系统统计数据
     */
    public AdminStatisticsVO getStatistics() {
        AdminStatisticsVO vo = new AdminStatisticsVO();

        // 用户统计
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        vo.setTotalUsers(userMapper.selectCount(null));

        userWrapper.clear();
        userWrapper.eq(User::getUserType, 0);
        vo.setNormalUsers(userMapper.selectCount(userWrapper));

        userWrapper.clear();
        userWrapper.eq(User::getUserType, 1);
        vo.setVipUsers(userMapper.selectCount(userWrapper));

        userWrapper.clear();
        userWrapper.eq(User::getUserType, 2);
        vo.setAdminUsers(userMapper.selectCount(userWrapper));

        // 今日新增用户
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        userWrapper.clear();
        userWrapper.ge(User::getCreatedAt, todayStart);
        vo.setTodayNewUsers(userMapper.selectCount(userWrapper));

        // 文件统计
        LambdaQueryWrapper<File> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(File::getStatus, 1).eq(File::getIsFolder, 0);
        vo.setTotalFiles(fileMapper.selectCount(fileWrapper));

        fileWrapper.clear();
        fileWrapper.eq(File::getStatus, 1).eq(File::getIsFolder, 1);
        vo.setTotalFolders(fileMapper.selectCount(fileWrapper));

        // 今日新增文件
        fileWrapper.clear();
        fileWrapper.eq(File::getStatus, 1).ge(File::getCreatedAt, todayStart);
        vo.setTodayNewFiles(fileMapper.selectCount(fileWrapper));

        // 存储统计
        List<User> allUsers = userMapper.selectList(null);
        long totalUsed = allUsers.stream().mapToLong(User::getStorageUsed).sum();
        long totalQuota = allUsers.stream().mapToLong(User::getStorageQuota).sum();
        vo.setTotalStorageUsed(totalUsed);
        vo.setTotalStorageQuota(totalQuota);
        vo.setStorageUsageRate(totalQuota > 0 ? (double) totalUsed / totalQuota * 100 : 0.0);

        // 分享统计
        vo.setTotalShares(shareMapper.selectCount(null));
        LambdaQueryWrapper<Share> shareWrapper = new LambdaQueryWrapper<>();
        shareWrapper.eq(Share::getStatus, 1);
        vo.setActiveShares(shareMapper.selectCount(shareWrapper));

        // 登录统计
        LambdaQueryWrapper<LoginLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.ge(LoginLog::getLoginTime, todayStart);
        vo.setTodayLogins(loginLogMapper.selectCount(logWrapper));

        logWrapper.clear();
        logWrapper.ge(LoginLog::getLoginTime, todayStart).eq(LoginLog::getLoginStatus, 1);
        vo.setTodaySuccessLogins(loginLogMapper.selectCount(logWrapper));

        return vo;
    }

    /**
     * 获取用户列表
     */
    public Page<AdminUserVO> getUserList(AdminUserQueryDTO dto) {
        Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键字搜索（用户名或邮箱）
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, dto.getKeyword())
                    .or()
                    .like(User::getEmail, dto.getKeyword()));
        }

        // 用户类型筛选
        if (dto.getUserType() != null) {
            wrapper.eq(User::getUserType, dto.getUserType());
        }

        // 状态筛选
        if (dto.getStatus() != null) {
            wrapper.eq(User::getStatus, dto.getStatus());
        }

        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> userPage = userMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<AdminUserVO> voPage = new Page<>(dto.getPageNum(), dto.getPageSize());
        voPage.setTotal(userPage.getTotal());

        List<AdminUserVO> voList = userPage.getRecords().stream().map(user -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
            vo.setAvatar(user.getAvatar());
            vo.setUserType(user.getUserType());
            vo.setStatus(user.getStatus());
            vo.setStorageUsed(user.getStorageUsed());
            vo.setStorageQuota(user.getStorageQuota());
            vo.setVipExpireTime(user.getVipExpireTime());
            vo.setLastLoginIp(user.getLastLoginIp());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreatedAt(user.getCreatedAt());

            // 统计文件数
            LambdaQueryWrapper<File> fileWrapper = new LambdaQueryWrapper<>();
            fileWrapper.eq(File::getUserId, user.getId()).eq(File::getStatus, 1);
            vo.setTotalFiles(fileMapper.selectCount(fileWrapper));

            // 统计分享数
            LambdaQueryWrapper<Share> shareWrapper = new LambdaQueryWrapper<>();
            shareWrapper.eq(Share::getUserId, user.getId());
            vo.setTotalShares(shareMapper.selectCount(shareWrapper));

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 编辑用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void editUser(Long adminId, AdminEditUserDTO dto) {
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 不能修改管理员自己的用户类型
        if (user.getId().equals(adminId) && dto.getUserType() != null && !dto.getUserType().equals(user.getUserType())) {
            throw new BusinessException("不能修改自己的用户类型");
        }

        // 检查用户名唯一性
        if (StringUtils.hasText(dto.getUsername()) && !dto.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, dto.getUsername()).ne(User::getId, dto.getUserId());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(dto.getUsername());
        }

        // 检查邮箱唯一性
        if (StringUtils.hasText(dto.getEmail()) && !dto.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, dto.getEmail()).ne(User::getId, dto.getUserId());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("邮箱已存在");
            }
            user.setEmail(dto.getEmail());
        }

        // 修改用户类型
        if (dto.getUserType() != null) {
            user.setUserType(dto.getUserType());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员编辑用户信息: adminId={}, userId={}", adminId, dto.getUserId());
    }

    /**
     * 禁用/启用用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long adminId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 不能禁用自己
        if (user.getId().equals(adminId)) {
            throw new BusinessException("不能禁用自己的账户");
        }

        // 不能禁用其他管理员
        if (user.getUserType() == 2) {
            throw new BusinessException("不能禁用管理员账户");
        }

        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员{}用户: adminId={}, userId={}, newStatus={}",
                user.getStatus() == 1 ? "启用" : "禁用", adminId, userId, user.getStatus());
    }

    /**
     * 管理员开通/续费VIP
     */
    @Transactional(rollbackFor = Exception.class)
    public void operateVip(Long adminId, AdminVipOperationDTO dto) {
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (dto.getDays() <= 0) {
            throw new BusinessException("天数必须大于0");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = user.getVipExpireTime();

        // 如果当前已有VIP且未过期，则在原基础上增加天数
        if (expireTime != null && expireTime.isAfter(now)) {
            user.setVipExpireTime(expireTime.plusDays(dto.getDays()));
        } else {
            // 否则从现在开始计算
            user.setVipExpireTime(now.plusDays(dto.getDays()));
        }

        // 更新用户类型和存储空间
        // 注意：如果是管理员（userType=2），则保持管理员身份不变
        if (user.getUserType() != 2) {
            user.setUserType(1);
        }
        if (user.getStorageQuota() < vipQuota) {
            user.setStorageQuota(vipQuota);
        }

        user.setUpdatedAt(now);
        userMapper.updateById(user);

        log.info("管理员操作VIP: adminId={}, userId={}, days={}, expireTime={}",
                adminId, dto.getUserId(), dto.getDays(), user.getVipExpireTime());
    }

    /**
     * 重置用户密码（生成随机密码）
     */
    @Transactional(rollbackFor = Exception.class)
    public String resetUserPassword(Long adminId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        log.warn("管理员重置密码功能暂不实现，建议用户使用忘记密码功能: adminId={}, userId={}", adminId, userId);
        throw new BusinessException("管理员重置密码会导致用户文件丢失，请引导用户使用忘记密码功能");
    }

    /**
     * 获取文件列表
     */
    public Page<AdminFileVO> getFileList(Integer pageNum, Integer pageSize, Long userId, String fileType) {
        Page<File> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();

        // 按用户筛选
        if (userId != null) {
            wrapper.eq(File::getUserId, userId);
        }

        // 按文件类型筛选
        if (StringUtils.hasText(fileType)) {
            wrapper.like(File::getFileType, fileType);
        }

        wrapper.orderByDesc(File::getCreatedAt);
        Page<File> filePage = fileMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<AdminFileVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(filePage.getTotal());

        List<AdminFileVO> voList = filePage.getRecords().stream().map(file -> {
            AdminFileVO vo = new AdminFileVO();
            vo.setId(file.getId());
            vo.setUserId(file.getUserId());

            // 查询用户名
            User user = userMapper.selectById(file.getUserId());
            vo.setUsername(user != null ? user.getUsername() : "未知用户");

            vo.setFileName(file.getFileName());
            vo.setFileSize(file.getFileSize());
            vo.setFileType(file.getFileType());
            vo.setIsFolder(file.getIsFolder());
            vo.setStatus(file.getStatus());
            vo.setCreatedAt(file.getCreatedAt());
            vo.setDeletedAt(file.getDeletedAt());

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 删除文件（管理员强制删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long adminId, Long fileId) {
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 软删除
        file.setStatus(0);
        file.setDeletedAt(LocalDateTime.now());
        fileMapper.updateById(file);

        log.info("管理员删除文件: adminId={}, fileId={}, fileName={}", adminId, fileId, file.getFileName());
    }

    /**
     * 获取分享列表
     */
    public Page<AdminShareVO> getShareList(Integer pageNum, Integer pageSize, Long userId, Integer status) {
        Page<Share> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();

        // 按用户筛选
        if (userId != null) {
            wrapper.eq(Share::getUserId, userId);
        }

        // 按状态筛选
        if (status != null) {
            wrapper.eq(Share::getStatus, status);
        }

        wrapper.orderByDesc(Share::getCreatedAt);
        Page<Share> sharePage = shareMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<AdminShareVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(sharePage.getTotal());

        List<AdminShareVO> voList = sharePage.getRecords().stream().map(share -> {
            AdminShareVO vo = new AdminShareVO();
            vo.setId(share.getId());
            vo.setUserId(share.getUserId());

            // 查询用户名
            User user = userMapper.selectById(share.getUserId());
            vo.setUsername(user != null ? user.getUsername() : "未知用户");

            vo.setFileId(share.getFileId());

            // 查询文件名
            File file = fileMapper.selectById(share.getFileId());
            vo.setFileName(file != null ? file.getFileName() : "文件已删除");

            vo.setShareCode(share.getShareCode());
            vo.setHasPassword(StringUtils.hasText(share.getSharePassword()));
            vo.setExpireTime(share.getExpireTime());
            vo.setViewCount(share.getViewCount());
            vo.setDownloadCount(share.getDownloadCount());
            vo.setStatus(share.getStatus());
            vo.setCreatedAt(share.getCreatedAt());

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 取消分享（管理员强制取消）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(Long adminId, Long shareId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException("分享不存在");
        }

        share.setStatus(0);
        share.setUpdatedAt(LocalDateTime.now());
        shareMapper.updateById(share);

        log.info("管理员取消分享: adminId={}, shareId={}, shareCode={}", adminId, shareId, share.getShareCode());
    }

    /**
     * 获取登录日志
     */
    public Page<LoginLogVO> getLoginLogs(Integer pageNum, Integer pageSize, Long userId, Integer status) {
        Page<LoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();

        // 按用户筛选
        if (userId != null) {
            wrapper.eq(LoginLog::getUserId, userId);
        }

        // 按状态筛选
        if (status != null) {
            wrapper.eq(LoginLog::getLoginStatus, status);
        }

        wrapper.orderByDesc(LoginLog::getLoginTime);
        Page<LoginLog> logPage = loginLogMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<LoginLogVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(logPage.getTotal());

        List<LoginLogVO> voList = logPage.getRecords().stream().map(log -> {
            LoginLogVO vo = new LoginLogVO();
            vo.setId(log.getId());
            vo.setLoginIp(log.getLoginIp());
            vo.setLoginLocation(log.getLoginLocation());
            vo.setLoginDevice(log.getDeviceInfo());
            vo.setSuccess(log.getLoginStatus() != null && log.getLoginStatus() == 1);
            vo.setMessage(log.getLoginStatus() != null && log.getLoginStatus() == 1 ? "登录成功" : log.getFailReason());
            vo.setLoginTime(log.getLoginTime());

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 生成随机密码
     */
    @SuppressWarnings("unused")
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * 获取操作日志（管理员）
     */
    public Page<OperationLogVO> getOperationLogs(com.clouddisk.dto.OperationLogQueryDTO dto) {
        return operationLogService.getAllOperationLogs(dto);
    }

    /**
     * 获取用户增长趋势（最近30天）
     */
    public List<AdminUserGrowthVO> getUserGrowthTrend() {
        List<AdminUserGrowthVO> result = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();

        // 获取所有用户
        List<User> allUsers = userMapper.selectList(null);

        // 统计最近30天的数据
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 统计当天新增用户数
            long newUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null
                            && !u.getCreatedAt().isBefore(dayStart)
                            && !u.getCreatedAt().isAfter(dayEnd))
                    .count();

            // 统计当天的累计用户数
            long totalUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isAfter(dayEnd))
                    .count();

            AdminUserGrowthVO vo = new AdminUserGrowthVO();
            vo.setDate(date.toString());
            vo.setNewUsers(newUsers);
            vo.setTotalUsers(totalUsers);
            result.add(vo);
        }

        return result;
    }

    /**
     * 获取文件操作量趋势（最近30天）
     */
    public List<AdminFileOperationVO> getFileOperationTrend() {
        List<AdminFileOperationVO> result = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();

        // 获取所有操作日志
        List<OperationLog> allLogs = operationLogMapper.selectList(null);

        // 统计最近30天的数据
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 统计上传次数
            long uploadCount = allLogs.stream()
                    .filter(log -> "UPLOAD".equals(log.getOperationType())
                            && log.getOperationTime() != null
                            && !log.getOperationTime().isBefore(dayStart)
                            && !log.getOperationTime().isAfter(dayEnd))
                    .count();

            // 统计下载次数
            long downloadCount = allLogs.stream()
                    .filter(log -> "DOWNLOAD".equals(log.getOperationType())
                            && log.getOperationTime() != null
                            && !log.getOperationTime().isBefore(dayStart)
                            && !log.getOperationTime().isAfter(dayEnd))
                    .count();

            AdminFileOperationVO vo = new AdminFileOperationVO();
            vo.setDate(date.toString());
            vo.setUploadCount(uploadCount);
            vo.setDownloadCount(downloadCount);
            result.add(vo);
        }

        return result;
    }

    /**
     * 获取存储空间使用趋势（最近30天）
     */
    public List<AdminStorageTrendVO> getStorageTrend() {
        List<AdminStorageTrendVO> result = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();

        // 获取所有用户和文件
        List<User> allUsers = userMapper.selectList(null);
        List<File> allFiles = fileMapper.selectList(new LambdaQueryWrapper<File>().eq(File::getStatus, 1));

        // 统计最近30天的数据
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 统计截止当天的存储使用量（所有在当天或之前创建的文件）
            long storageUsed = allFiles.stream()
                    .filter(f -> f.getCreatedAt() != null && !f.getCreatedAt().isAfter(dayEnd) && f.getIsFolder() == 0)
                    .mapToLong(File::getFileSize)
                    .sum();

            // 统计截止当天的存储配额（所有在当天或之前注册的用户）
            long storageQuota = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isAfter(dayEnd))
                    .mapToLong(User::getStorageQuota)
                    .sum();

            AdminStorageTrendVO vo = new AdminStorageTrendVO();
            vo.setDate(date.toString());
            vo.setStorageUsed(storageUsed);
            vo.setStorageQuota(storageQuota);
            result.add(vo);
        }

        return result;
    }
}
