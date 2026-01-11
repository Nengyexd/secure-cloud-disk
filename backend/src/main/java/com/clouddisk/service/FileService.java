package com.clouddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clouddisk.common.BusinessException;
import com.clouddisk.config.MinIOConfig;
import com.clouddisk.dto.CreateFolderDTO;
import com.clouddisk.dto.DeleteFileDTO;
import com.clouddisk.dto.FileListDTO;
import com.clouddisk.dto.RenameFileDTO;
import com.clouddisk.entity.Favorite;
import com.clouddisk.entity.File;
import com.clouddisk.entity.User;
import com.clouddisk.mapper.FavoriteMapper;
import com.clouddisk.mapper.FileMapper;
import com.clouddisk.mapper.UserMapper;
import com.clouddisk.util.AESUtil;
import com.clouddisk.util.FileHashUtil;
import com.clouddisk.vo.*;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.coobird.thumbnailator.Thumbnails;

@Slf4j
@Service
public class FileService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIOConfig minioConfig;

    @Value("${business.storage.normal-quota}")
    private Long normalQuota;

    @Value("${business.storage.vip-quota}")
    private Long vipQuota;

    @Value("${business.upload.normal-max-size}")
    private Long normalMaxSize;

    @Value("${business.upload.vip-max-size}")
    private Long vipMaxSize;

    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO uploadFile(MultipartFile multipartFile, Long userId, Long parentId, String filePath, String masterKey) {
        try {
            log.info("开始文件上传: userId={}", userId);

            String originalFilename = multipartFile.getOriginalFilename();
            Long fileSize = multipartFile.getSize();
            String contentType = multipartFile.getContentType();

            User user = userMapper.selectById(userId);
            log.info("查询用户结果: user={}", user != null ? user.getUsername() : "null");

            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            // 检查单文件大小限制
            boolean hasVip = userService.hasVipPrivileges(user);
            Long maxFileSize = hasVip ? vipMaxSize : normalMaxSize;
            if (fileSize > maxFileSize) {
                throw new BusinessException("文件大小超过限制，当前用户最大支持 " + formatSize(maxFileSize));
            }

            Long maxQuota = hasVip ? vipQuota : normalQuota;
            if (user.getStorageUsed() + fileSize > maxQuota) {
                throw new BusinessException("存储空间不足");
            }

            byte[] fileBytes = multipartFile.getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            String fileHash = FileHashUtil.calculateSHA256(inputStream);

            LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(File::getUserId, userId)
                    .eq(File::getFileHash, fileHash)
                    .eq(File::getStatus, 1)
                    .orderByDesc(File::getCreatedAt)
                    .last("LIMIT 1");

            File existingFile = fileMapper.selectOne(queryWrapper);

            if (existingFile != null) {
                File newFile = new File();
                newFile.setUserId(userId);
                newFile.setFileName(originalFilename);
                newFile.setFilePath(filePath);
                newFile.setFileSize(fileSize);
                newFile.setFileHash(fileHash);
                newFile.setFileType(contentType);
                newFile.setEncryptedKey(existingFile.getEncryptedKey());
                newFile.setStorageKey(existingFile.getStorageKey());
                newFile.setParentId(parentId);
                newFile.setIsFolder(0);
                newFile.setStatus(1);
                fileMapper.insert(newFile);

                // 记录操作日志（秒传）
                operationLogService.log(userId, user.getUsername(), "UPLOAD", "FILE",
                        newFile.getId(), originalFilename, "秒传文件: " + originalFilename);

                return new FileUploadVO(newFile.getId(), originalFilename, fileSize, contentType, true);
            }

            String fileKey = AESUtil.generateAESKey();

            byte[] encryptedBytes = encryptFile(fileBytes, fileKey);

            String storageKey = UUID.randomUUID().toString();
            uploadToMinIO(encryptedBytes, storageKey, contentType);

            String encryptedFileKey = AESUtil.encrypt(fileKey, masterKey);

            // 如果是图片文件,生成缩略图
            String thumbnailKey = null;
            if (contentType != null && contentType.startsWith("image/")) {
                try {
                    byte[] thumbnailBytes = generateThumbnail(fileBytes, contentType);
                    if (thumbnailBytes != null) {
                        // 加密缩略图
                        byte[] encryptedThumbnail = encryptFile(thumbnailBytes, fileKey);
                        // 上传缩略图到MinIO
                        thumbnailKey = "thumb_" + storageKey;
                        uploadToMinIO(encryptedThumbnail, thumbnailKey, contentType);
                        log.info("缩略图生成成功: thumbnailKey={}", thumbnailKey);
                    }
                } catch (Exception e) {
                    log.error("生成或上传缩略图失败,继续上传原文件", e);
                    // 缩略图生成失败不影响主文件上传
                }
            }

            File file = new File();
            file.setUserId(userId);
            file.setFileName(originalFilename);
            file.setFilePath(filePath);
            file.setFileSize(fileSize);
            file.setFileHash(fileHash);
            file.setFileType(contentType);
            file.setEncryptedKey(encryptedFileKey);
            file.setStorageKey(storageKey);
            file.setThumbnailKey(thumbnailKey);  // 设置缩略图key
            file.setParentId(parentId);
            file.setIsFolder(0);
            file.setStatus(1);
            fileMapper.insert(file);

            user.setStorageUsed(user.getStorageUsed() + fileSize);
            userMapper.updateById(user);

            log.info("文件上传成功: userId={}, fileName={}, fileSize={}, isInstant={}",
                    userId, originalFilename, fileSize, false);

            // 记录操作日志
            operationLogService.log(userId, user.getUsername(), "UPLOAD", "FILE",
                    file.getId(), originalFilename, "上传文件: " + originalFilename);

            return new FileUploadVO(file.getId(), originalFilename, fileSize, contentType, false);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    private byte[] encryptFile(byte[] fileBytes, String fileKey) throws Exception {
        byte[] keyBytes = org.apache.commons.codec.binary.Base64.decodeBase64(fileKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[12];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

        byte[] ciphertext = cipher.doFinal(fileBytes);

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);

        return byteBuffer.array();
    }

    /**
     * 生成缩略图
     * @param imageBytes 原始图片字节
     * @param contentType 图片类型
     * @return 缩略图字节数组,如果生成失败返回null
     */
    private byte[] generateThumbnail(byte[] imageBytes, String contentType) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 生成200x200的缩略图,保持宽高比
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .size(200, 200)
                    .outputFormat(getImageFormat(contentType))
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成缩略图失败", e);
            return null;
        }
    }

    /**
     * 根据contentType获取图片格式
     */
    private String getImageFormat(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        if (contentType.contains("png")) {
            return "png";
        } else if (contentType.contains("gif")) {
            return "gif";
        } else if (contentType.contains("webp")) {
            return "webp";
        } else {
            return "jpg";  // 默认使用jpg
        }
    }

    private void uploadToMinIO(byte[] encryptedBytes, String storageKey, String contentType) throws Exception {
        String bucketName = minioConfig.getBucketName();

        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());

        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build());
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .stream(inputStream, encryptedBytes.length, -1)
                            .contentType(contentType)
                            .build());
        }
    }

    public FileListVO getFileList(Long userId, FileListDTO dto) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getStatus, 1);

        // 如果有分类参数，则忽略文件夹ID，查询所有文件
        if (StringUtils.hasText(dto.getCategory())) {
            switch (dto.getCategory()) {
                case "image":
                    wrapper.likeRight(File::getFileType, "image/");
                    break;
                case "video":
                    wrapper.likeRight(File::getFileType, "video/");
                    break;
                case "audio":
                    wrapper.likeRight(File::getFileType, "audio/");
                    break;
                case "doc":
                    wrapper.and(w -> w.likeRight(File::getFileType, "text/")
                            .or().likeRight(File::getFileType, "application/pdf")
                            .or().likeRight(File::getFileType, "application/msword")
                            .or().likeRight(File::getFileType, "application/vnd.openxmlformats-officedocument") // docx, pptx, xlsx
                            .or().likeRight(File::getFileType, "application/vnd.ms-excel")
                            .or().likeRight(File::getFileType, "application/vnd.ms-powerpoint"));
                    break;
                default:
                    // 默认情况
                    break;
            }
        } else {
            // 没有分类参数时，按文件夹浏览
            wrapper.eq(File::getParentId, dto.getParentId());
        }

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.like(File::getFileName, dto.getKeyword());
        }

        if (StringUtils.hasText(dto.getFileType())) {
            wrapper.like(File::getFileType, dto.getFileType());
        }

        if ("file_name".equals(dto.getSortBy())) {
            wrapper.orderBy(true, "asc".equals(dto.getSortOrder()), File::getFileName);
        } else if ("file_size".equals(dto.getSortBy())) {
            wrapper.orderBy(true, "asc".equals(dto.getSortOrder()), File::getFileSize);
        } else if ("updated_at".equals(dto.getSortBy())) {
            wrapper.orderBy(true, "asc".equals(dto.getSortOrder()), File::getUpdatedAt);
        } else {
            wrapper.orderBy(true, "asc".equals(dto.getSortOrder()), File::getCreatedAt);
        }

        Page<File> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<File> result = fileMapper.selectPage(page, wrapper);

        // 批量查询用户的收藏状态
        List<Long> fileIds = result.getRecords().stream()
                .filter(file -> file.getIsFolder() == 0) // 只查询文件的收藏状态
                .map(File::getId)
                .collect(Collectors.toList());

        List<Long> favoriteFileIds = new ArrayList<>();
        if (!fileIds.isEmpty()) {
            LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
            favoriteWrapper.eq(Favorite::getUserId, userId)
                    .in(Favorite::getFileId, fileIds);
            List<Favorite> favorites = favoriteMapper.selectList(favoriteWrapper);
            favoriteFileIds = favorites.stream()
                    .map(Favorite::getFileId)
                    .collect(Collectors.toList());
        }

        final List<Long> finalFavoriteFileIds = favoriteFileIds;
        List<FileItemVO> fileItems = result.getRecords().stream().map(file -> {
            FileItemVO vo = new FileItemVO();
            BeanUtils.copyProperties(file, vo);
            // 设置收藏状态
            vo.setIsFavorite(file.getIsFolder() == 0 && finalFavoriteFileIds.contains(file.getId()));
            return vo;
        }).collect(Collectors.toList());

        BreadcrumbVO breadcrumb = buildBreadcrumb(dto.getParentId());

        return new FileListVO(fileItems, result.getTotal(), dto.getPageNum(), dto.getPageSize(), breadcrumb);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createFolder(Long userId, CreateFolderDTO dto) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, dto.getParentId())
                .eq(File::getFileName, dto.getFolderName())
                .eq(File::getStatus, 1);

        File existingFolder = fileMapper.selectOne(wrapper);
        if (existingFolder != null) {
            throw new BusinessException("文件夹已存在");
        }

        File folder = new File();
        folder.setUserId(userId);
        folder.setFileName(dto.getFolderName());
        folder.setFilePath(dto.getFilePath());
        folder.setFileSize(0L);
        folder.setFileType("folder");
        folder.setParentId(dto.getParentId());
        folder.setIsFolder(1);
        folder.setStatus(1);

        fileMapper.insert(folder);

        // 记录操作日志
        User user = userMapper.selectById(userId);
        if (user != null) {
            operationLogService.log(userId, user.getUsername(), "CREATE_FOLDER", "FOLDER",
                    folder.getId(), dto.getFolderName(), "创建文件夹: " + dto.getFolderName());
        }

        log.info("创建文件夹成功: userId={}, folderName={}", userId, dto.getFolderName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void renameFile(Long userId, RenameFileDTO dto) {
        File file = fileMapper.selectById(dto.getFileId());

        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException("文件不存在");
        }

        if (file.getStatus() == 0) {
            throw new BusinessException("回收站中的文件无法重命名");
        }

        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, file.getParentId())
                .eq(File::getFileName, dto.getNewName())
                .eq(File::getStatus, 1)
                .ne(File::getId, dto.getFileId());

        File existingFile = fileMapper.selectOne(wrapper);
        if (existingFile != null) {
            throw new BusinessException("文件名已存在");
        }

        // 保存旧文件名用于日志记录
        String oldName = file.getFileName();

        file.setFileName(dto.getNewName());
        fileMapper.updateById(file);

        // 记录操作日志
        User user = userMapper.selectById(userId);
        if (user != null) {
            String objectType = file.getIsFolder() == 1 ? "FOLDER" : "FILE";
            operationLogService.log(userId, user.getUsername(), "RENAME", objectType,
                    file.getId(), dto.getNewName(), "重命名: " + oldName + " -> " + dto.getNewName());
        }

        log.info("重命名文件成功: userId={}, fileId={}, newName={}", userId, dto.getFileId(), dto.getNewName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFiles(Long userId, DeleteFileDTO dto) {
        User user = userMapper.selectById(userId);
        String username = user != null ? user.getUsername() : "unknown";

        for (Long fileId : dto.getFileIds()) {
            File file = fileMapper.selectById(fileId);

            if (file == null || !file.getUserId().equals(userId)) {
                continue;
            }

            file.setStatus(0);
            file.setDeletedAt(LocalDateTime.now());
            fileMapper.updateById(file);

            // 记录操作日志
            String objectType = file.getIsFolder() == 1 ? "FOLDER" : "FILE";
            operationLogService.log(userId, username, "DELETE", objectType,
                    fileId, file.getFileName(), "删除" + (file.getIsFolder() == 1 ? "文件夹" : "文件") + ": " + file.getFileName());

            if (file.getIsFolder() == 1) {
                deleteSubFiles(userId, fileId);
            }
        }

        log.info("删除文件成功: userId={}, fileIds={}", userId, dto.getFileIds());
    }

    private void deleteSubFiles(Long userId, Long parentId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, parentId)
                .eq(File::getStatus, 1);

        List<File> subFiles = fileMapper.selectList(wrapper);

        for (File subFile : subFiles) {
            subFile.setStatus(0);
            subFile.setDeletedAt(LocalDateTime.now());
            fileMapper.updateById(subFile);

            if (subFile.getIsFolder() == 1) {
                deleteSubFiles(userId, subFile.getId());
            }
        }
    }

    private BreadcrumbVO buildBreadcrumb(Long currentFolderId) {
        List<BreadcrumbVO.BreadcrumbItem> items = new ArrayList<>();

        items.add(new BreadcrumbVO.BreadcrumbItem(0L, "全部文件", "/"));

        if (currentFolderId != null && currentFolderId > 0) {
            List<File> pathFiles = new ArrayList<>();
            Long folderId = currentFolderId;

            while (folderId != null && folderId > 0) {
                File folder = fileMapper.selectById(folderId);
                if (folder == null) {
                    break;
                }
                pathFiles.add(0, folder);
                folderId = folder.getParentId();
            }

            for (File folder : pathFiles) {
                items.add(new BreadcrumbVO.BreadcrumbItem(
                        folder.getId(),
                        folder.getFileName(),
                        folder.getFilePath()
                ));
            }
        }

        return new BreadcrumbVO(items);
    }

    /**
     * 下载文件 (流式)
     */
    public java.io.InputStream downloadFile(Long userId, Long fileId) {
        File file = fileMapper.selectById(fileId);

        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException("文件不存在");
        }

        if (file.getStatus() == 0) {
            throw new BusinessException("回收站中的文件无法下载");
        }

        if (file.getIsFolder() == 1) {
            throw new BusinessException("文件夹无法下载");
        }

        if (minioClient == null) {
             throw new BusinessException("存储服务不可用");
        }

        try {
            java.io.InputStream stream = downloadFromMinIO(file.getStorageKey());
            if (stream == null) {
                throw new BusinessException("从MinIO获取的文件流为空");
            }
            return stream;
        } catch (Exception e) {
            log.error("下载文件失败: userId={}, fileId={}, storageKey={}", userId, fileId, file.getStorageKey(), e);
            throw new BusinessException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件缩略图 (流式)
     */
    public java.io.InputStream getThumbnail(Long userId, Long fileId) {
        File file = fileMapper.selectById(fileId);

        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException("文件不存在");
        }

        if (file.getStatus() == 0) {
            throw new BusinessException("回收站中的文件无法获取缩略图");
        }

        if (file.getIsFolder() == 1) {
            throw new BusinessException("文件夹无缩略图");
        }

        if (!StringUtils.hasText(file.getThumbnailKey())) {
            throw new BusinessException("该文件无缩略图");
        }

        // 增加对 MinIO 连接的检查
        if (minioClient == null) {
             throw new BusinessException("存储服务不可用");
        }

        try {
            // 从 MinIO 获取缩略图流
            return downloadFromMinIO(file.getThumbnailKey());
        } catch (Exception e) {
            log.error("获取缩略图失败: userId={}, fileId={}, thumbnailKey={}", userId, fileId, file.getThumbnailKey(), e);
            throw new BusinessException("获取缩略图失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件下载信息（包含加密密钥）
     */
    public FileDownloadVO getFileDownloadInfo(Long userId, Long fileId) {
        File file = fileMapper.selectById(fileId);

        if (file == null || !file.getUserId().equals(userId)) {
            throw new BusinessException("文件不存在");
        }

        if (file.getStatus() == 0) {
            throw new BusinessException("回收站中的文件无法下载");
        }

        if (file.getIsFolder() == 1) {
            throw new BusinessException("文件夹无法下载");
        }

        FileDownloadVO vo = new FileDownloadVO();
        vo.setFileId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileSize(file.getFileSize());
        vo.setFileType(file.getFileType());
        vo.setEncryptedKey(file.getEncryptedKey());
        vo.setIsFolder(file.getIsFolder());

        return vo;
    }

    private java.io.InputStream downloadFromMinIO(String storageKey) throws Exception {
        String bucketName = minioConfig.getBucketName();

        return minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storageKey)
                        .build());
    }

    /**
     * 获取最近上传的文件
     */
    public FileListVO getRecentUploadFiles(Long userId, FileListDTO dto) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getStatus, 1)
                .eq(File::getIsFolder, 0);  // 只查询文件，不包括文件夹

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.like(File::getFileName, dto.getKeyword());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(File::getCreatedAt);

        Page<File> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<File> result = fileMapper.selectPage(page, wrapper);

        // 批量查询用户的收藏状态
        List<Long> fileIds = result.getRecords().stream()
                .map(File::getId)
                .collect(Collectors.toList());

        List<Long> favoriteFileIds = new ArrayList<>();
        if (!fileIds.isEmpty()) {
            LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
            favoriteWrapper.eq(Favorite::getUserId, userId)
                    .in(Favorite::getFileId, fileIds);
            List<Favorite> favorites = favoriteMapper.selectList(favoriteWrapper);
            favoriteFileIds = favorites.stream()
                    .map(Favorite::getFileId)
                    .collect(Collectors.toList());
        }

        final List<Long> finalFavoriteFileIds = favoriteFileIds;
        List<FileItemVO> fileItems = result.getRecords().stream().map(file -> {
            FileItemVO vo = new FileItemVO();
            BeanUtils.copyProperties(file, vo);
            vo.setIsFavorite(finalFavoriteFileIds.contains(file.getId()));
            return vo;
        }).collect(Collectors.toList());

        return new FileListVO(fileItems, result.getTotal(), dto.getPageNum(), dto.getPageSize(), null);
    }

    /**
     * 获取回收站文件列表
     */
    public FileListVO getRecycleBinList(Long userId, FileListDTO dto) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getStatus, 0);

        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.like(File::getFileName, dto.getKeyword());
        }

        wrapper.orderByDesc(File::getDeletedAt);

        Page<File> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<File> result = fileMapper.selectPage(page, wrapper);

        List<FileItemVO> fileItems = result.getRecords().stream().map(file -> {
            FileItemVO vo = new FileItemVO();
            BeanUtils.copyProperties(file, vo);
            return vo;
        }).collect(Collectors.toList());

        return new FileListVO(fileItems, result.getTotal(), dto.getPageNum(), dto.getPageSize(), null);
    }

    /**
     * 恢复文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreFiles(Long userId, com.clouddisk.dto.RestoreFileDTO dto) {
        User user = userMapper.selectById(userId);
        String username = user != null ? user.getUsername() : "unknown";

        for (Long fileId : dto.getFileIds()) {
            File file = fileMapper.selectById(fileId);

            if (file == null || !file.getUserId().equals(userId)) {
                continue;
            }

            if (file.getStatus() == 1) {
                throw new BusinessException("文件不在回收站中");
            }

            file.setStatus(1);
            file.setDeletedAt(null);
            fileMapper.updateById(file);

            // 记录操作日志
            String objectType = file.getIsFolder() == 1 ? "FOLDER" : "FILE";
            operationLogService.log(userId, username, "RESTORE", objectType,
                    fileId, file.getFileName(), "恢复" + (file.getIsFolder() == 1 ? "文件夹" : "文件") + ": " + file.getFileName());

            if (file.getIsFolder() == 1) {
                restoreSubFiles(userId, fileId);
            }
        }

        log.info("恢复文件成功: userId={}, fileIds={}", userId, dto.getFileIds());
    }

    private void restoreSubFiles(Long userId, Long parentId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, parentId)
                .eq(File::getStatus, 0);

        List<File> subFiles = fileMapper.selectList(wrapper);

        for (File subFile : subFiles) {
            subFile.setStatus(1);
            subFile.setDeletedAt(null);
            fileMapper.updateById(subFile);

            if (subFile.getIsFolder() == 1) {
                restoreSubFiles(userId, subFile.getId());
            }
        }
    }

    /**
     * 永久删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteFiles(Long userId, com.clouddisk.dto.DeleteFileDTO dto) {
        User user = userMapper.selectById(userId);
        String username = user != null ? user.getUsername() : "unknown";

        for (Long fileId : dto.getFileIds()) {
            File file = fileMapper.selectById(fileId);

            if (file == null || !file.getUserId().equals(userId)) {
                continue;
            }

            if (file.getStatus() == 1) {
                throw new BusinessException("只能永久删除回收站中的文件");
            }

            // 删除子文件
            if (file.getIsFolder() == 1) {
                permanentlyDeleteSubFiles(userId, fileId);
            }

            // 从 MinIO 删除文件
            if (file.getIsFolder() == 0 && StringUtils.hasText(file.getStorageKey())) {
                try {
                    deleteFromMinIO(file.getStorageKey());
                } catch (Exception e) {
                    log.error("从 MinIO 删除文件失败: storageKey={}", file.getStorageKey(), e);
                }
            }

            // 更新用户存储空间
            if (file.getIsFolder() == 0) {
                User storageUser = userMapper.selectById(userId);
                if (storageUser != null) {
                    storageUser.setStorageUsed(Math.max(0, storageUser.getStorageUsed() - file.getFileSize()));
                    userMapper.updateById(storageUser);
                }
            }

            // 记录操作日志
            String objectType = file.getIsFolder() == 1 ? "FOLDER" : "FILE";
            operationLogService.log(userId, username, "PERMANENT_DELETE", objectType,
                    fileId, file.getFileName(), "永久删除" + (file.getIsFolder() == 1 ? "文件夹" : "文件") + ": " + file.getFileName());

            // 从数据库删除
            fileMapper.deleteById(fileId);
        }

        log.info("永久删除文件成功: userId={}, fileIds={}", userId, dto.getFileIds());
    }

    private void permanentlyDeleteSubFiles(Long userId, Long parentId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getUserId, userId)
                .eq(File::getParentId, parentId);

        List<File> subFiles = fileMapper.selectList(wrapper);

        for (File subFile : subFiles) {
            if (subFile.getIsFolder() == 1) {
                permanentlyDeleteSubFiles(userId, subFile.getId());
            }

            // 从 MinIO 删除文件
            if (subFile.getIsFolder() == 0 && StringUtils.hasText(subFile.getStorageKey())) {
                try {
                    deleteFromMinIO(subFile.getStorageKey());
                } catch (Exception e) {
                    log.error("从 MinIO 删除文件失败: storageKey={}", subFile.getStorageKey(), e);
                }
            }

            // 更新用户存储空间
            if (subFile.getIsFolder() == 0) {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    user.setStorageUsed(Math.max(0, user.getStorageUsed() - subFile.getFileSize()));
                    userMapper.updateById(user);
                }
            }

            fileMapper.deleteById(subFile.getId());
        }
    }

    private void deleteFromMinIO(String storageKey) throws Exception {
        String bucketName = minioConfig.getBucketName();

        minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storageKey)
                        .build());
    }

    /**
     * 升级VIP
     */
    @Transactional(rollbackFor = Exception.class)
    public void upgradeToVip(Long userId, Integer days) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setUserType(1); // 1表示VIP
        
        // 计算VIP过期时间
        LocalDateTime now = LocalDateTime.now();
        if (user.getVipExpireTime() == null || user.getVipExpireTime().isBefore(now)) {
            user.setVipExpireTime(now.plusDays(days));
        } else {
            user.setVipExpireTime(user.getVipExpireTime().plusDays(days));
        }

        userMapper.updateById(user);
        log.info("用户升级VIP成功: userId={}, days={}", userId, days);
    }
    @Transactional(rollbackFor = Exception.class)
    public void moveFiles(Long userId, com.clouddisk.dto.MoveFileDTO dto) {
        Long targetParentId = dto.getTargetParentId();

        // 验证目标文件夹
        if (targetParentId != 0) {
            File targetFolder = fileMapper.selectById(targetParentId);
            if (targetFolder == null || !targetFolder.getUserId().equals(userId)) {
                throw new BusinessException("目标文件夹不存在");
            }

            if (targetFolder.getIsFolder() != 1) {
                throw new BusinessException("目标必须是文件夹");
            }

            if (targetFolder.getStatus() == 0) {
                throw new BusinessException("不能移动到回收站中的文件夹");
            }
        }

        for (Long fileId : dto.getFileIds()) {
            File file = fileMapper.selectById(fileId);

            if (file == null || !file.getUserId().equals(userId)) {
                continue;
            }

            if (file.getStatus() == 0) {
                throw new BusinessException("回收站中的文件无法移动");
            }

            // 不能将文件夹移动到自己的子文件夹中
            if (file.getIsFolder() == 1) {
                if (isSubFolder(fileId, targetParentId)) {
                    throw new BusinessException("不能将文件夹移动到自己的子文件夹中");
                }
            }

            // 检查目标位置是否已存在同名文件
            LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(File::getUserId, userId)
                    .eq(File::getParentId, targetParentId)
                    .eq(File::getFileName, file.getFileName())
                    .eq(File::getStatus, 1);

            File existingFile = fileMapper.selectOne(wrapper);
            if (existingFile != null) {
                throw new BusinessException("目标位置已存在同名文件: " + file.getFileName());
            }

            file.setParentId(targetParentId);
            file.setFilePath(buildFilePath(targetParentId));
            fileMapper.updateById(file);
        }

        log.info("移动文件成功: userId={}, fileIds={}, targetParentId={}", userId, dto.getFileIds(), targetParentId);
    }

    private boolean isSubFolder(Long parentId, Long childId) {
        if (childId == 0) {
            return false;
        }

        File current = fileMapper.selectById(childId);
        while (current != null && current.getParentId() != null) {
            if (current.getParentId().equals(parentId)) {
                return true;
            }
            if (current.getParentId() == 0) {
                break;
            }
            current = fileMapper.selectById(current.getParentId());
        }

        return false;
    }

    private String buildFilePath(Long parentId) {
        if (parentId == 0) {
            return "/";
        }

        List<String> pathParts = new ArrayList<>();
        Long currentId = parentId;

        while (currentId != null && currentId > 0) {
            File folder = fileMapper.selectById(currentId);
            if (folder == null) {
                break;
            }
            pathParts.add(0, folder.getFileName());
            currentId = folder.getParentId();
        }

        return "/" + String.join("/", pathParts);
    }

    private String formatSize(Long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2fGB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
