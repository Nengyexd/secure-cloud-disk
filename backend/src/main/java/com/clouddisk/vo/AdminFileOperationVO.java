package com.clouddisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员 - 文件操作量统计VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminFileOperationVO {

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 上传文件数
     */
    private Long uploadCount;

    /**
     * 下载次数
     */
    private Long downloadCount;
}
