package com.clouddisk.controller;

import com.clouddisk.common.Result;
import com.clouddisk.dto.*;
import com.clouddisk.entity.User;
import com.clouddisk.mapper.UserMapper;
import com.clouddisk.service.FileService;
import com.clouddisk.service.OperationLogService;
import com.clouddisk.vo.FileDownloadVO;
import com.clouddisk.vo.FileListVO;
import com.clouddisk.vo.FileUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/upload")
    public Result<FileUploadVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
            @RequestParam(value = "filePath", defaultValue = "/") String filePath,
            @RequestParam("masterKey") String masterKey,
            Authentication authentication) {

        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        Long userId = (Long) authentication.getPrincipal();
        log.info("文件上传请求: userId={}, fileName={}, fileSize={}", userId, file.getOriginalFilename(), file.getSize());

        FileUploadVO uploadVO = fileService.uploadFile(file, userId, parentId, filePath, masterKey);

        String message = uploadVO.getIsInstantUpload() ? "秒传成功" : "上传成功";
        return Result.success(message, uploadVO);
    }

    @GetMapping("/list")
    public Result<FileListVO> getFileList(
            @ModelAttribute FileListDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("文件列表查询: userId={}, parentId={}", userId, dto.getParentId());

        FileListVO fileList = fileService.getFileList(userId, dto);

        return Result.success("查询成功", fileList);
    }

    @PostMapping("/createFolder")
    public Result<String> createFolder(
            @Validated @RequestBody CreateFolderDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("创建文件夹: userId={}, folderName={}", userId, dto.getFolderName());

        fileService.createFolder(userId, dto);

        return Result.success("创建成功", null);
    }

    @PostMapping("/rename")
    public Result<String> renameFile(
            @Validated @RequestBody RenameFileDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("重命名文件: userId={}, fileId={}, newName={}", userId, dto.getFileId(), dto.getNewName());

        fileService.renameFile(userId, dto);

        return Result.success("重命名成功", null);
    }

    @PostMapping("/delete")
    public Result<String> deleteFiles(
            @Validated @RequestBody DeleteFileDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("删除文件: userId={}, fileIds={}", userId, dto.getFileIds());

        fileService.deleteFiles(userId, dto);

        return Result.success("删除成功", null);
    }

    @GetMapping("/downloadInfo/{fileId}")
    public Result<FileDownloadVO> getDownloadInfo(
            @PathVariable Long fileId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("获取文件下载信息: userId={}, fileId={}", userId, fileId);

        FileDownloadVO downloadInfo = fileService.getFileDownloadInfo(userId, fileId);

        return Result.success("获取成功", downloadInfo);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long fileId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("下载文件: userId={}, fileId={}", userId, fileId);

        try {
            FileDownloadVO downloadInfo = fileService.getFileDownloadInfo(userId, fileId);
            java.io.InputStream fileStream = fileService.downloadFile(userId, fileId);

            byte[] fileBytes = fileStream.readAllBytes();
            fileStream.close();

            String encodedFileName = URLEncoder.encode(downloadInfo.getFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setContentLength(fileBytes.length);  // 设置正确的内容长度
            // 添加防止IDM等下载管理器拦截的响应头
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Download-Options", "noopen");
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");

            // 记录操作日志
            User user = userMapper.selectById(userId);
            if (user != null) {
                operationLogService.log(userId, user.getUsername(), "DOWNLOAD", "FILE",
                        fileId, downloadInfo.getFileName(), "下载文件: " + downloadInfo.getFileName());
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new org.springframework.core.io.ByteArrayResource(fileBytes));

        } catch (com.clouddisk.common.BusinessException e) {
            log.error("下载文件业务异常: userId={}, fileId={}, message={}", userId, fileId, e.getMessage());
            return ResponseEntity.status(500).body(Result.error(500, e.getMessage()));
        } catch (Exception e) {
            log.error("下载文件失败: userId={}, fileId={}", userId, fileId, e);
            return ResponseEntity.internalServerError().body(Result.error(500, "下载失败: " + e.getMessage()));
        }
    }

    /**
     * 预览文件（专用接口，避免被下载管理器拦截）
     */
    @GetMapping("/preview/{fileId}")
    public ResponseEntity<?> previewFile(
            @PathVariable Long fileId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("预览文件: userId={}, fileId={}", userId, fileId);

        try {
            FileDownloadVO downloadInfo = fileService.getFileDownloadInfo(userId, fileId);
            java.io.InputStream fileStream = fileService.downloadFile(userId, fileId);

            // 将InputStream转换为字节数组
            byte[] fileBytes = fileStream.readAllBytes();
            fileStream.close();

            HttpHeaders headers = new HttpHeaders();
            // 使用inline而不是attachment，告诉浏览器这是预览而不是下载
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Content-Disposition", "inline");
            headers.setContentLength(fileBytes.length);
            // 添加AJAX标识，防止下载管理器拦截
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");

            // 记录操作日志
            User user = userMapper.selectById(userId);
            if (user != null) {
                operationLogService.log(userId, user.getUsername(), "PREVIEW", "FILE",
                        fileId, downloadInfo.getFileName(), "预览文件: " + downloadInfo.getFileName());
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new org.springframework.core.io.ByteArrayResource(fileBytes));

        } catch (com.clouddisk.common.BusinessException e) {
            log.error("预览文件业务异常: userId={}, fileId={}, message={}", userId, fileId, e.getMessage());
            return ResponseEntity.status(500).body(Result.error(500, e.getMessage()));
        } catch (Exception e) {
            log.error("预览文件失败: userId={}, fileId={}", userId, fileId, e);
            return ResponseEntity.internalServerError().body(Result.error(500, "预览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件缩略图
     */
    @GetMapping("/thumbnail/{fileId}")
    public ResponseEntity<?> getThumbnail(
            @PathVariable Long fileId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("获取缩略图: userId={}, fileId={}", userId, fileId);

        try {
            FileDownloadVO downloadInfo = fileService.getFileDownloadInfo(userId, fileId);
            java.io.InputStream thumbnailStream = fileService.getThumbnail(userId, fileId);

            // 将InputStream转换为字节数组
            byte[] thumbnailBytes = thumbnailStream.readAllBytes();
            thumbnailStream.close();

            HttpHeaders headers = new HttpHeaders();
            // 根据文件类型设置Content-Type
            if (downloadInfo.getFileType() != null) {
                headers.setContentType(MediaType.parseMediaType(downloadInfo.getFileType()));
            } else {
                headers.setContentType(MediaType.IMAGE_JPEG);
            }
            headers.set("Content-Disposition", "inline");
            headers.setContentLength(thumbnailBytes.length);
            headers.set("Cache-Control", "public, max-age=3600"); // 缓存1小时

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new org.springframework.core.io.ByteArrayResource(thumbnailBytes));

        } catch (com.clouddisk.common.BusinessException e) {
            log.error("获取缩略图业务异常: userId={}, fileId={}, message={}", userId, fileId, e.getMessage());
            return ResponseEntity.status(404).body(Result.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("获取缩略图失败: userId={}, fileId={}", userId, fileId, e);
            return ResponseEntity.internalServerError().body(Result.error(500, "获取缩略图失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最近上传的文件
     */
    @GetMapping("/recent-uploads")
    public Result<FileListVO> getRecentUploads(
            @ModelAttribute FileListDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("查询最近上传: userId={}", userId);

        FileListVO fileList = fileService.getRecentUploadFiles(userId, dto);

        return Result.success("查询成功", fileList);
    }

    @GetMapping("/recycleBin/list")
    public Result<FileListVO> getRecycleBinList(
            @ModelAttribute FileListDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("回收站列表查询: userId={}", userId);

        FileListVO fileList = fileService.getRecycleBinList(userId, dto);

        return Result.success("查询成功", fileList);
    }

    @PostMapping("/recycleBin/restore")
    public Result<String> restoreFiles(
            @Validated @RequestBody RestoreFileDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("恢复文件: userId={}, fileIds={}", userId, dto.getFileIds());

        fileService.restoreFiles(userId, dto);

        return Result.success("恢复成功", null);
    }

    @PostMapping("/recycleBin/delete")
    public Result<String> permanentlyDeleteFiles(
            @Validated @RequestBody DeleteFileDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("永久删除文件: userId={}, fileIds={}", userId, dto.getFileIds());

        fileService.permanentlyDeleteFiles(userId, dto);

        return Result.success("永久删除成功", null);
    }

    @PostMapping("/move")
    public Result<String> moveFiles(
            @Validated @RequestBody MoveFileDTO dto,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("移动文件: userId={}, fileIds={}, targetParentId={}", userId, dto.getFileIds(), dto.getTargetParentId());

        fileService.moveFiles(userId, dto);

        return Result.success("移动成功", null);
    }

    /**
     * 批量下载文件（打包为ZIP）
     */
    @PostMapping("/batch-download")
    public ResponseEntity<?> batchDownload(
            @RequestBody List<Long> fileIds,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        log.info("批量下载文件: userId={}, fileIds={}", userId, fileIds);

        try {
            if (fileIds == null || fileIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error(400, "请选择要下载的文件"));
            }

            // 创建ZIP输出流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // 遍历文件列表，添加到ZIP
            for (Long fileId : fileIds) {
                try {
                    FileDownloadVO downloadInfo = fileService.getFileDownloadInfo(userId, fileId);

                    // 跳过文件夹
                    if (downloadInfo.getIsFolder() == 1) {
                        continue;
                    }

                    java.io.InputStream fileStream = fileService.downloadFile(userId, fileId);

                    // 添加ZIP条目
                    ZipEntry zipEntry = new ZipEntry(downloadInfo.getFileName());
                    zos.putNextEntry(zipEntry);

                    // 写入文件内容
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fileStream.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }

                    zos.closeEntry();
                    fileStream.close();
                } catch (Exception e) {
                    log.error("添加文件到ZIP失败: fileId={}", fileId, e);
                    // 继续处理其他文件
                }
            }

            zos.close();

            // 生成ZIP文件名
            String zipFileName = "files_" + System.currentTimeMillis() + ".zip";
            String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");

            // 记录操作日志
            User user = userMapper.selectById(userId);
            if (user != null) {
                operationLogService.log(userId, user.getUsername(), "DOWNLOAD", "FILE",
                        null, "批量下载", "批量下载 " + fileIds.size() + " 个文件");
            }

            byte[] zipBytes = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipBytes.length)
                    .body(new InputStreamResource(bais));

        } catch (Exception e) {
            log.error("批量下载失败: userId={}, fileIds={}", userId, fileIds, e);
            return ResponseEntity.internalServerError().body(Result.error(500, "批量下载失败: " + e.getMessage()));
        }
    }
}
