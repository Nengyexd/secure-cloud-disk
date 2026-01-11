package com.clouddisk.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class MoveFileDTO {
    @NotNull(message = "文件ID不能为空")
    private List<Long> fileIds;

    @NotNull(message = "目标文件夹ID不能为空")
    private Long targetParentId;
}
