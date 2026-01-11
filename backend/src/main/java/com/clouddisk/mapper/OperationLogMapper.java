package com.clouddisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clouddisk.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
