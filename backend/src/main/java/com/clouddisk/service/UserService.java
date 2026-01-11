package com.clouddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clouddisk.common.BusinessException;
import com.clouddisk.dto.FixAccountDTO;
import com.clouddisk.dto.ResetPasswordDTO;
import com.clouddisk.dto.UpdatePasswordDTO;
import com.clouddisk.dto.UpdateUserInfoDTO;
import com.clouddisk.dto.UserLoginDTO;
import com.clouddisk.dto.UserRegisterDTO;
import com.clouddisk.entity.File;
import com.clouddisk.entity.LoginLog;
import com.clouddisk.entity.Share;
import com.clouddisk.entity.User;
import com.clouddisk.mapper.FileMapper;
import com.clouddisk.mapper.LoginLogMapper;
import com.clouddisk.mapper.ShareMapper;
import com.clouddisk.mapper.UserMapper;
import com.clouddisk.util.AESUtil;
import com.clouddisk.util.IpUtil;
import com.clouddisk.util.JwtUtil;
import com.clouddisk.vo.LoginVO;
import com.clouddisk.vo.LoginLogVO;
import com.clouddisk.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * 用户服务
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${business.storage.normal-quota}")
    private Long normalQuota;

    @Value("${business.storage.vip-quota}")
    private Long vipQuota;

    // PBKDF2 参数
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @param request     HTTP请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO registerDTO, HttpServletRequest request) {
        String email = registerDTO.getEmail();
        String password = registerDTO.getPassword();
        String code = registerDTO.getVerificationCode();

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(email, code, 1);
        if (!codeValid) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 2. 检查邮箱是否已注册
        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, email);
        User existUser = userMapper.selectOne(emailWrapper);
        if (existUser != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 3. 检查用户名是否已存在
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, registerDTO.getUsername());
        User existUsername = userMapper.selectOne(usernameWrapper);
        if (existUsername != null) {
            throw new BusinessException("该用户名已被使用");
        }

        // 4. 生成密码盐值
        String salt = generateSalt();

        // 5. 使用PBKDF2哈希密码
        String passwordHash = hashPassword(password, salt);

        // 6. 生成用户主密钥（用于加密文件密钥）
        String masterKey = AESUtil.generateAESKey(); // 生成AES-256密钥

        // 7. 使用密码派生密钥加密主密钥
        String passwordDerivedKey = deriveKeyFromPassword(password, salt);
        String masterKeyEncrypted = AESUtil.encrypt(masterKey, passwordDerivedKey);

        // 8. 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        user.setMasterKeyEncrypted(masterKeyEncrypted);
        user.setUserType(0); // 默认普通用户
        user.setStorageQuota(normalQuota); // 5GB
        user.setStorageUsed(0L);
        user.setStatus(1); // 正常状态
        user.setEmailVerified(1); // 注册验证码验证成功，标记邮箱已验证
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        log.info("用户注册成功：username={}, email={}", user.getUsername(), email);
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @param request  HTTP请求
     * @return 登录结果（包含token和用户信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(UserLoginDTO loginDTO, HttpServletRequest request) {
        String account = loginDTO.getAccount();
        String password = loginDTO.getPassword();
        String code = loginDTO.getVerificationCode();

        // 1. 判断account是邮箱还是用户名
        boolean isEmail = account.contains("@");

        // 2. 根据邮箱或用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (isEmail) {
            wrapper.eq(User::getEmail, account);
        } else {
            wrapper.eq(User::getUsername, account);
        }
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(isEmail ? "该邮箱未注册" : "该用户名不存在");
        }

        // 3. 验证验证码（使用用户的邮箱进行验证）
        boolean codeValid = verificationCodeService.verifyCode(user.getEmail(), code, 2);
        if (!codeValid) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 4. 检查账户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账户已被冻结，请联系管理员");
        }

        // 5. 验证密码
        String passwordHash = hashPassword(password, user.getSalt());
        if (!passwordHash.equals(user.getPasswordHash())) {
            // 记录登录失败日志
            recordLoginLog(user.getId(), user.getEmail(), request, false, "密码错误");
            throw new BusinessException("密码错误");
        }

        // 6. 更新最后登录信息
        String loginIp = IpUtil.getIpAddress(request);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        userMapper.updateById(user);

        // 7. 记录登录成功日志
        recordLoginLog(user.getId(), user.getEmail(), request, true, "登录成功");

        // 8. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getUserType());

        // 9. 构造用户信息VO
        com.clouddisk.vo.UserInfoVO userInfoVO = new com.clouddisk.vo.UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setEmail(user.getEmail());
        userInfoVO.setUserType(user.getUserType());
        userInfoVO.setStorageQuota(user.getStorageQuota());
        userInfoVO.setStorageUsed(user.getStorageUsed());
        userInfoVO.setVipExpireTime(user.getVipExpireTime());
        userInfoVO.setStatus(user.getStatus());
        userInfoVO.setEmailVerified(user.getEmailVerified());
        userInfoVO.setCreatedAt(user.getCreatedAt());
        userInfoVO.setLastLoginTime(user.getLastLoginTime());
        userInfoVO.setIsVip(hasVipPrivileges(user));
        userInfoVO.setStorageUsageRate(
                user.getStorageQuota() > 0 ? (user.getStorageUsed() * 100.0 / user.getStorageQuota()) : 0.0);

        // 10. 解密主密钥（用于文件加密）
        String passwordDerivedKey = deriveKeyFromPassword(password, user.getSalt());
        String masterKey = AESUtil.decrypt(user.getMasterKeyEncrypted(), passwordDerivedKey);

        // 11. 构造登录响应VO
        LoginVO loginVO = new LoginVO(token, jwtUtil.getExpiration(), userInfoVO, masterKey);

        log.info("用户登录成功：userId={}, account={}, ip={}", user.getId(), account, loginIp);
        return loginVO;
    }

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    public User getUserByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 生成密码盐值
     *
     * @return Base64编码的盐值
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 使用PBKDF2哈希密码
     *
     * @param password 原始密码
     * @param salt     盐值（Base64编码）
     * @return Base64编码的密码哈希
     */
    private String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 从密码派生加密密钥（用于加密主密钥）
     *
     * @param password 用户密码
     * @param salt     盐值
     * @return Base64编码的密钥
     */
    private String deriveKeyFromPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("密钥派生失败", e);
        }
    }

    /**
     * 记录登录日志
     *
     * @param userId  用户ID
     * @param email   邮箱
     * @param request HTTP请求
     * @param success 是否成功
     * @param message 消息
     */
    private void recordLoginLog(Long userId, String email, HttpServletRequest request,
            boolean success, String message) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setEmail(email);
        loginLog.setLoginIp(IpUtil.getIpAddress(request));
        loginLog.setDeviceInfo(request.getHeader("User-Agent"));
        loginLog.setLoginStatus(success ? 1 : 0);
        loginLog.setFailReason(success ? null : message);
        loginLog.setLoginTime(LocalDateTime.now());

        loginLogMapper.insert(loginLog);
    }

    /**
     * 获取用户详细信息（包含统计数据）
     *
     * @param userId 用户ID
     * @return 用户详细信息
     */
    public UserDetailVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 统计文件数量
        LambdaQueryWrapper<File> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(File::getUserId, userId)
                .eq(File::getStatus, 1)
                .eq(File::getIsFolder, 0);
        Long totalFiles = fileMapper.selectCount(fileWrapper);

        // 统计文件夹数量
        LambdaQueryWrapper<File> folderWrapper = new LambdaQueryWrapper<>();
        folderWrapper.eq(File::getUserId, userId)
                .eq(File::getStatus, 1)
                .eq(File::getIsFolder, 1);
        Long totalFolders = fileMapper.selectCount(folderWrapper);

        // 统计分享数量
        LambdaQueryWrapper<Share> shareWrapper = new LambdaQueryWrapper<>();
        shareWrapper.eq(Share::getUserId, userId)
                .eq(Share::getStatus, 1);
        Long totalShares = shareMapper.selectCount(shareWrapper);

        // 构建返回VO
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setUserType(user.getUserType());
        vo.setStorageUsed(user.getStorageUsed());
        vo.setStorageQuota(user.getStorageQuota());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setVipExpireTime(user.getVipExpireTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setTotalFiles(totalFiles);
        vo.setTotalFolders(totalFolders);
        vo.setTotalShares(totalShares);

        return vo;
    }

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param dto    更新信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UpdateUserInfoDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查用户名是否被其他用户占用
        if (!user.getUsername().equals(dto.getUsername())) {
            LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
            usernameWrapper.eq(User::getUsername, dto.getUsername())
                    .ne(User::getId, userId); // 排除当前用户
            User existUsername = userMapper.selectOne(usernameWrapper);
            if (existUsername != null) {
                throw new BusinessException("该用户名已被使用");
            }
        }

        // 检查邮箱是否被其他用户占用
        if (!user.getEmail().equals(dto.getEmail())) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, dto.getEmail())
                    .ne(User::getId, userId); // 排除当前用户
            User existEmail = userMapper.selectOne(emailWrapper);
            if (existEmail != null) {
                throw new BusinessException("该邮箱已被使用");
            }
            // 更新邮箱时，需要重新验证
            user.setEmailVerified(0);
        }

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        log.info("更新用户信息成功: userId={}", userId);
    }

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param dto    密码信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        String oldPasswordHash = hashPassword(dto.getOldPassword(), user.getSalt());
        if (!oldPasswordHash.equals(user.getPasswordHash())) {
            throw new BusinessException("旧密码错误");
        }

        // 检查新旧密码是否相同
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        // 1. 用旧密码派生密钥解密主密钥
        String oldPasswordDerivedKey = deriveKeyFromPassword(dto.getOldPassword(), user.getSalt());
        String masterKey = AESUtil.decrypt(user.getMasterKeyEncrypted(), oldPasswordDerivedKey);

        // 2. 用新密码派生密钥重新加密主密钥
        String newPasswordDerivedKey = deriveKeyFromPassword(dto.getNewPassword(), user.getSalt());
        String newMasterKeyEncrypted = AESUtil.encrypt(masterKey, newPasswordDerivedKey);

        // 3. 生成新密码哈希
        String newPasswordHash = hashPassword(dto.getNewPassword(), user.getSalt());

        // 4. 更新密码哈希和加密的主密钥
        user.setPasswordHash(newPasswordHash);
        user.setMasterKeyEncrypted(newMasterKeyEncrypted);
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        log.info("修改密码成功: userId={}", userId);
    }

    /**
     * 重置密码（忘记密码）
     * 注意：此操作会生成新的主密钥，导致之前的文件无法解密
     *
     * @param dto 重置密码信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, dto.getEmail());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 重新生成主密钥（旧的文件将无法解密）
        String newMasterKey = AESUtil.generateAESKey();

        // 用新密码派生密钥加密主密钥
        String newPasswordDerivedKey = deriveKeyFromPassword(dto.getNewPassword(), user.getSalt());
        String newMasterKeyEncrypted = AESUtil.encrypt(newMasterKey, newPasswordDerivedKey);

        // 生成新密码哈希
        String newPasswordHash = hashPassword(dto.getNewPassword(), user.getSalt());

        // 更新密码和主密钥
        user.setPasswordHash(newPasswordHash);
        user.setMasterKeyEncrypted(newMasterKeyEncrypted);
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        // 删除用户的所有文件和分享（因为无法用新主密钥解密）
        LambdaQueryWrapper<File> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(File::getUserId, user.getId());
        fileMapper.delete(fileWrapper);

        LambdaQueryWrapper<Share> shareWrapper = new LambdaQueryWrapper<>();
        shareWrapper.eq(Share::getUserId, user.getId());
        shareMapper.delete(shareWrapper);

        // 重置存储使用量
        user.setStorageUsed(0L);
        userMapper.updateById(user);

        log.info("重置密码成功（已清空文件）: userId={}, email={}", user.getId(), dto.getEmail());
    }

    /**
     * 修复账号（用于修复因旧代码修改密码导致的数据损坏）
     * 保留文件，用户需要提供旧密码
     *
     * @param dto 修复账号信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void fixAccount(FixAccountDTO dto) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, dto.getEmail());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 尝试用旧密码解密主密钥
        try {
            String oldPasswordDerivedKey = deriveKeyFromPassword(dto.getOldPassword(), user.getSalt());
            String masterKey = AESUtil.decrypt(user.getMasterKeyEncrypted(), oldPasswordDerivedKey);

            // 用新密码重新加密主密钥
            String newPasswordDerivedKey = deriveKeyFromPassword(dto.getNewPassword(), user.getSalt());
            String newMasterKeyEncrypted = AESUtil.encrypt(masterKey, newPasswordDerivedKey);

            // 生成新密码哈希
            String newPasswordHash = hashPassword(dto.getNewPassword(), user.getSalt());

            // 更新密码和主密钥
            user.setPasswordHash(newPasswordHash);
            user.setMasterKeyEncrypted(newMasterKeyEncrypted);
            user.setUpdatedAt(LocalDateTime.now());

            userMapper.updateById(user);

            log.info("修复账号成功（文件已保留）: userId={}, email={}", user.getId(), dto.getEmail());
        } catch (Exception e) {
            log.error("修复账号失败，旧密码可能不正确: email={}", dto.getEmail(), e);
            throw new BusinessException("修复失败，旧密码不正确或数据已损坏");
        }
    }

    /**
     * 上传用户头像
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return 头像URL
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }

        // 验证文件大小（限制 5MB）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new BusinessException("头像文件大小不能超过 5MB");
        }

        try {
            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成唯一文件名
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString() + extension;

            // 创建存储目录
            String uploadDir = "uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 生成访问URL（相对路径）
            String avatarUrl = "/" + uploadDir + filename;

            // 更新用户头像
            user.setAvatar(avatarUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);

            log.info("上传头像成功: userId={}, avatarUrl={}", userId, avatarUrl);

            return avatarUrl;
        } catch (IOException e) {
            log.error("上传头像失败: userId={}", userId, e);
            throw new BusinessException("头像上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户登录日志
     *
     * @param userId 用户ID
     * @param limit  获取数量限制
     * @return 登录日志列表
     */
    public java.util.List<LoginLogVO> getLoginLogs(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }

        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT " + limit);

        java.util.List<LoginLog> logs = loginLogMapper.selectList(wrapper);

        // 转换为 VO
        java.util.List<LoginLogVO> voList = new java.util.ArrayList<>();
        for (LoginLog log : logs) {
            LoginLogVO vo = new LoginLogVO();
            vo.setId(log.getId());
            vo.setLoginIp(log.getLoginIp());
            vo.setLoginLocation(log.getLoginLocation());
            vo.setLoginDevice(log.getDeviceInfo());
            vo.setSuccess(log.getLoginStatus() != null && log.getLoginStatus() == 1);
            vo.setMessage(log.getLoginStatus() != null && log.getLoginStatus() == 1 ? "登录成功" : log.getFailReason());
            vo.setLoginTime(log.getLoginTime());
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 注销账号
     *
     * @param userId   用户ID
     * @param password 密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证密码
        String passwordHash = hashPassword(password, user.getSalt());
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }

        // 删除用户的所有文件
        LambdaQueryWrapper<File> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(File::getUserId, userId);
        fileMapper.delete(fileWrapper);

        // 删除用户的所有分享
        LambdaQueryWrapper<Share> shareWrapper = new LambdaQueryWrapper<>();
        shareWrapper.eq(Share::getUserId, userId);
        shareMapper.delete(shareWrapper);

        // 删除用户的登录日志
        LambdaQueryWrapper<LoginLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(LoginLog::getUserId, userId);
        loginLogMapper.delete(logWrapper);

        // 删除用户
        userMapper.deleteById(userId);

        log.info("注销账号成功: userId={}, email={}", userId, user.getEmail());
    }

    /**
     * 升级/续费 VIP
     *
     * @param userId 用户ID
     * @param days   天数
     */
    @Transactional(rollbackFor = Exception.class)
    public void upgradeToVip(Long userId, Integer days) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = user.getVipExpireTime();

        // 如果当前已有VIP且未过期，则在原基础上增加天数
        if (expireTime != null && expireTime.isAfter(now)) {
            user.setVipExpireTime(expireTime.plusDays(days));
        } else {
            // 否则从现在开始计算
            user.setVipExpireTime(now.plusDays(days));
        }

        // 更新用户类型和存储空间
        // 注意：如果是管理员（userType=2），则保持管理员身份不变
        if (user.getUserType() != 2) {
            user.setUserType(1);
        }
        // 如果当前配额小于VIP配额，则升级配额
        if (user.getStorageQuota() < vipQuota) {
            user.setStorageQuota(vipQuota);
        }

        user.setUpdatedAt(now);
        userMapper.updateById(user);

        log.info("VIP升级成功: userId={}, days={}, expireTime={}", userId, days, user.getVipExpireTime());
    }

    /**
     * 判断用户是否拥有VIP特权
     * VIP用户（userType=1）或拥有VIP过期时间且未过期的管理员（userType=2）都算有VIP特权
     *
     * @param user 用户对象
     * @return 是否拥有VIP特权
     */
    public boolean hasVipPrivileges(User user) {
        if (user == null) {
            return false;
        }

        // VIP用户直接返回true
        if (user.getUserType() == 1) {
            return true;
        }

        // 管理员如果有VIP过期时间且未过期，也返回true
        if (user.getUserType() == 2 && user.getVipExpireTime() != null) {
            return user.getVipExpireTime().isAfter(LocalDateTime.now());
        }

        return false;
    }
}
