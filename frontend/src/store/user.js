import { defineStore } from 'pinia'
import { login, register } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    masterKey: localStorage.getItem('masterKey') || ''
  }),

  getters: {
    // 是否已登录
    isLoggedIn: (state) => !!state.token,

    // 是否VIP用户（包括拥有VIP特权的管理员）
    isVip: (state) => {
      if (!state.userInfo) return false

      // 如果是普通VIP用户（userType=1）
      if (state.userInfo.userType === 1) return true

      // 如果是管理员（userType=2）且有VIP过期时间
      if (state.userInfo.userType === 2 && state.userInfo.vipExpireTime) {
        const expireTime = new Date(state.userInfo.vipExpireTime)
        const now = new Date()
        return expireTime > now
      }

      return false
    },

    // 存储使用率
    storageUsageRate: (state) => {
      if (!state.userInfo?.storageQuota) return 0
      return (state.userInfo.storageUsed / state.userInfo.storageQuota * 100).toFixed(2)
    }
  },

  actions: {
    // 登录
    async login(loginForm) {
      try {
        const res = await login(loginForm)

        this.token = res.data.token
        this.userInfo = res.data.userInfo
        this.masterKey = res.data.masterKey

        localStorage.setItem('token', res.data.token)
        localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
        localStorage.setItem('masterKey', res.data.masterKey)

        return res
      } catch (error) {
        throw error
      }
    },

    // 注册
    async register(registerForm) {
      try {
        const res = await register(registerForm)
        return res
      } catch (error) {
        throw error
      }
    },

    // 登出
    logout() {
      this.token = ''
      this.userInfo = {}
      this.masterKey = ''
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('masterKey')
      router.push('/login')
    },

    // 更新用户信息
    updateUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    }
  }
})
