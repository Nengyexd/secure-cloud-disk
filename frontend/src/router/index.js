import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/ResetPassword.vue'),
    meta: { title: '重置密码' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: {
      title: '首页',
      requiresAuth: true  // 需要登录
    }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfile.vue'),
    meta: {
      title: '个人中心',
      requiresAuth: true  // 需要登录
    }
  },
  {
    path: '/vip',
    name: 'VipCenter',
    component: () => import('@/views/VipCenter.vue'),
    meta: {
      title: '会员中心',
      requiresAuth: true  // 需要登录
    }
  },
  {
    path: '/operation-log',
    name: 'OperationLog',
    component: () => import('@/views/OperationLog.vue'),
    meta: {
      title: '操作日志',
      requiresAuth: true  // 需要登录
    }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: {
      title: '管理员后台',
      requiresAuth: true,  // 需要登录
      requiresAdmin: true  // 需要管理员权限
    }
  },
  {
    path: '/share/:shareCode',
    name: 'Share',
    component: () => import('@/views/ShareView.vue'),
    meta: { title: '文件分享' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 安全云盘` : '安全云盘'

  // 检查是否需要登录
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    // 需要登录但没有token，跳转到登录页
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    // 已登录用户访问登录/注册页，跳转到首页
    next('/home')
  } else if (to.meta.requiresAdmin) {
    // 需要管理员权限
    const userStore = useUserStore()
    if (userStore.userInfo.userType !== 2) {
      // 不是管理员，禁止访问
      next('/home')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
