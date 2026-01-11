package com.clouddisk.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clouddisk.entity.File;
import com.clouddisk.mapper.FileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站自动清理定时任务
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
@Component
public class RecycleBinCleanTask {

    @Autowired
    private FileMapper fileMapper;

    @Value("${business.recycle-bin.auto-clean-days:30}")
    private Integer autoCleanDays;

    /**
     * 每天凌晨2点执行清理任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredFiles() {
        log.info("开始执行回收站自动清理任务，清理{}天前删除的文件", autoCleanDays);

        try {
            // 计算过期时间
            LocalDateTime expireTime = LocalDateTime.now().minusDays(autoCleanDays);

            // 查询过期的已删除文件
            LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(File::getStatus, 0) // 已删除
                    .le(File::getDeletedAt, expireTime); // 删除时间早于过期时间

            List<File> expiredFiles = fileMapper.selectList(wrapper);

            if (expiredFiles.isEmpty()) {
                log.info("没有需要清理的过期文件");
                return;
            }

            log.info("找到{}个过期文件，开始清理", expiredFiles.size());

            int successCount = 0;
            int failCount = 0;

            for (File file : expiredFiles) {
                try {
                    // 永久删除文件
                    permanentlyDeleteFile(file);
                    successCount++;
                } catch (Exception e) {
                    log.error("清理文件失败: fileId={}, fileName={}", file.getId(), file.getFileName(), e);
                    failCount++;
                }
            }

            log.info("回收站自动清理任务完成: 成功{}个，失败{}个", successCount, failCount);

        } catch (Exception e) {
            log.error("回收站自动清理任务执行失败", e);
        }
    }

    /**
     * 永久删除文件
     */
    private void permanentlyDeleteFile(File file) {
        // 删除数据库记录
        fileMapper.deleteById(file.getId());

        // 注意：实际的MinIO文件删除应该在这里执行
        // 但由于文件可能被多个用户共享（秒传功能），所以这里不删除MinIO中的文件
        // 可以通过定期清理孤儿文件的方式来释放MinIO存储空间

        log.debug("永久删除文件: fileId={}, fileName={}", file.getId(), file.getFileName());
    }
}
