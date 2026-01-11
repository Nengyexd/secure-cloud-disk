import request from '@/utils/request'

/**
 * 获取系统统计数据
 */
export const getStatistics = () => {
  return request({
    url: '/admin/statistics',
    method: 'get'
  })
}

/**
 * 获取用户列表
 */
export const getUserList = (params) => {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

/**
 * 编辑用户信息
 */
export const editUser = (data) => {
  return request({
    url: '/admin/users',
    method: 'put',
    data
  })
}

/**
 * 禁用/启用用户
 */
export const toggleUserStatus = (userId) => {
  return request({
    url: `/admin/users/${userId}/toggle-status`,
    method: 'post'
  })
}

/**
 * 管理员开通/续费VIP
 */
export const adminOperateVip = (data) => {
  return request({
    url: '/admin/users/vip',
    method: 'post',
    data
  })
}

/**
 * 获取文件列表
 */
export const getFileList = (params) => {
  return request({
    url: '/admin/files',
    method: 'get',
    params
  })
}

/**
 * 删除文件
 */
export const deleteFile = (fileId) => {
  return request({
    url: `/admin/files/${fileId}`,
    method: 'delete'
  })
}

/**
 * 获取分享列表
 */
export const getShareList = (params) => {
  return request({
    url: '/admin/shares',
    method: 'get',
    params
  })
}

/**
 * 取消分享
 */
export const cancelShare = (shareId) => {
  return request({
    url: `/admin/shares/${shareId}/cancel`,
    method: 'post'
  })
}

/**
 * 获取登录日志
 */
export const getLoginLogs = (params) => {
  return request({
    url: '/admin/logs/login',
    method: 'get',
    params
  })
}

/**
 * 获取用户增长趋势
 */
export const getUserGrowthTrend = () => {
  return request({
    url: '/admin/statistics/user-growth',
    method: 'get'
  })
}

/**
 * 获取文件操作量趋势
 */
export const getFileOperationTrend = () => {
  return request({
    url: '/admin/statistics/file-operation',
    method: 'get'
  })
}

/**
 * 获取存储空间使用趋势
 */
export const getStorageTrend = () => {
  return request({
    url: '/admin/statistics/storage-trend',
    method: 'get'
  })
}
