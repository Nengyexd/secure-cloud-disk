package com.clouddisk.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class RestoreFileDTO {
    @NotNull(message = "文件ID不能为空")
    private List<Long> fileIds;
}
