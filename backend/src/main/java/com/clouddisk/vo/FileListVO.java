package com.clouddisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileListVO {
    private List<FileItemVO> files;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private BreadcrumbVO breadcrumb;
}
