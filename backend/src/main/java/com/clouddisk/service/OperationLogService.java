package com.clouddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clouddisk.dto.OperationLogQueryDTO;
import com.clouddisk.entity.OperationLog;
import com.clouddisk.mapper.OperationLogMapper;
import com.clouddisk.util.IpUtil;
import com.clouddisk.vo.OperationLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志服务
 *
 * @author CloudDisk
 * @since 2024-01-01
 */
@Slf4j
@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired(required = false)
    private HttpServletRequest request;

    // 操作类型描述映射
    private static final Map<String, String> OPERATION_TYPE_MAP = new HashMap<>();
    static {
        OPERATION_TYPE_MAP.put("UPLOAD", "上传");
        OPERATION_TYPE_MAP.put("DOWNLOAD", "下载");
        OPERATION_TYPE_MAP.put("DELETE", "删除");
        OPERATION_TYPE_MAP.put("RESTORE", "恢复");
        OPERATION_TYPE_MAP.put("PERMANENT_DELETE", "永久删除");
        OPERATION_TYPE_MAP.put("SHARE", "分享");
        OPERATION_TYPE_MAP.put("CANCEL_SHARE", "取消分享");
        OPERATION_TYPE_MAP.put("RENAME", "重命名");
        OPERATION_TYPE_MAP.put("MOVE", "移动");
        OPERATION_TYPE_MAP.put("CREATE_FOLDER", "创建文件夹");
        OPERATION_TYPE_MAP.put("PREVIEW", "预览");
    }

    // 操作对象类型描述映射
    private static final Map<String, String> OBJECT_TYPE_MAP = new HashMap<>();
    static {
        OBJECT_TYPE_MAP.put("FILE", "文件");
        OBJECT_TYPE_MAP.put("FOLDER", "文件夹");
        OBJECT_TYPE_MAP.put("SHARE", "分享");
    }

    // 操作结果描述映射
    private static final Map<String, String> RESULT_MAP = new HashMap<>();
    static {
        RESULT_MAP.put("SUCCESS", "成功");
        RESULT_MAP.put("FAIL", "失败");
    }

    /**
     * 记录操作日志（仅记录成功的操作）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param operationType 操作类型
     * @param objectType 操作对象类型
     * @param objectId 操作对象ID
     * @param objectName 操作对象名称
     * @param description 操作描述
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String username, String operationType, String objectType,
                    Long objectId, String objectName, String description) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setOperationType(operationType);
            log.setObjectType(objectType);
            log.setObjectId(objectId);
            log.setObjectName(objectName);
            log.setDescription(description);
            log.setResult("SUCCESS");
            log.setErrorMessage(null);
            log.setOperationTime(LocalDateTime.now());

            // 获取IP地址
            if (request != null) {
                log.setIpAddress(IpUtil.getIpAddress(request));
            }

            operationLogMapper.insert(log);
        } catch (Exception e) {
            // 记录日志失败不影响业务
            OperationLogService.log.error("记录操作日志失败", e);
        }
    }

    /**
     * 查询用户操作日志
     *
     * @param userId 用户ID
     * @param dto 查询条件
     * @return 操作日志分页数据
     */
    public Page<OperationLogVO> getUserOperationLogs(Long userId, OperationLogQueryDTO dto) {
        Page<OperationLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 当前用户的日志
        wrapper.eq(OperationLog::getUserId, userId);

        // 操作类型筛选
        if (StringUtils.hasText(dto.getOperationType())) {
            wrapper.eq(OperationLog::getOperationType, dto.getOperationType());
        }

        // 操作结果筛选
        if (StringUtils.hasText(dto.getResult())) {
            wrapper.eq(OperationLog::getResult, dto.getResult());
        }

        wrapper.orderByDesc(OperationLog::getOperationTime);
        Page<OperationLog> logPage = operationLogMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<OperationLogVO> voPage = new Page<>(dto.getPageNum(), dto.getPageSize());
        voPage.setTotal(logPage.getTotal());
        voPage.setRecords(convertToVO(logPage.getRecords()));

        return voPage;
    }

    /**
     * 查询所有操作日志（管理员）
     *
     * @param dto 查询条件
     * @return 操作日志分页数据
     */
    public Page<OperationLogVO> getAllOperationLogs(OperationLogQueryDTO dto) {
        Page<OperationLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 用户ID筛选
        if (dto.getUserId() != null) {
            wrapper.eq(OperationLog::getUserId, dto.getUserId());
        }

        // 操作类型筛选
        if (StringUtils.hasText(dto.getOperationType())) {
            wrapper.eq(OperationLog::getOperationType, dto.getOperationType());
        }

        // 操作结果筛选
        if (StringUtils.hasText(dto.getResult())) {
            wrapper.eq(OperationLog::getResult, dto.getResult());
        }

        wrapper.orderByDesc(OperationLog::getOperationTime);
        Page<OperationLog> logPage = operationLogMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<OperationLogVO> voPage = new Page<>(dto.getPageNum(), dto.getPageSize());
        voPage.setTotal(logPage.getTotal());
        voPage.setRecords(convertToVO(logPage.getRecords()));

        return voPage;
    }

    /**
     * 转换为VO
     */
    private List<OperationLogVO> convertToVO(List<OperationLog> logs) {
        return logs.stream().map(log -> {
            OperationLogVO vo = new OperationLogVO();
            BeanUtils.copyProperties(log, vo);
            vo.setOperationTypeDesc(OPERATION_TYPE_MAP.getOrDefault(log.getOperationType(), log.getOperationType()));
            vo.setObjectTypeDesc(OBJECT_TYPE_MAP.getOrDefault(log.getObjectType(), log.getObjectType()));
            vo.setResultDesc(RESULT_MAP.getOrDefault(log.getResult(), log.getResult()));
            return vo;
        }).collect(Collectors.toList());
    }
}
