import request from '@/utils/request'

export const getUserInfo = () => {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

export const updateUserInfo = (data) => {
  return request({
    url: '/user/info',
    method: 'put',
    data
  })
}

export const updatePassword = (data) => {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

export const getLoginLogs = (limit = 10) => {
  return request({
    url: '/user/login-logs',
    method: 'get',
    params: { limit }
  })
}

export const deleteAccount = (password) => {
  return request({
    url: '/user/delete',
    method: 'post',
    data: { password }
  })
}
