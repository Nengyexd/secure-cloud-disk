import request from '@/utils/request'

/**
 * 发送验证码
 * @param {String} account - 账号（邮箱或用户名）
 * @param {Number} codeType - 验证码类型：1-注册，2-登录，3-重置密码
 */
export function sendCode(account, codeType) {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data: {
      account,
      codeType
    }
  })
}

/**
 * 用户注册
 * @param {Object} data - 注册信息
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录
 * @param {Object} data - 登录信息
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}
