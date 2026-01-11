package com.clouddisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clouddisk.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志Mapper接口
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
