import request from '@/utils/request'

/**
 * 获取我的操作日志
 */
export const getMyOperationLogs = (params) => {
  return request({
    url: '/operation-log/my',
    method: 'get',
    params
  })
}

/**
 * 获取所有操作日志（管理员）
 */
export const getAllOperationLogs = (params) => {
  return request({
    url: '/admin/logs/operation',
    method: 'get',
    params
  })
}
