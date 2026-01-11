<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 左侧品牌展示区 -->
      <div class="login-left">
        <div class="brand-content">
          <div class="logo-circle">
            <el-icon :size="48" color="#FFFFFF"><Cloudy /></el-icon>
          </div>
          <h1>吉亦云盘</h1>
          <p class="slogan">安全 · 高速 · 你的数字资产保险箱</p>
          <div class="feature-list">
            <div class="feature-item">
              <el-icon><Lock /></el-icon>
              <span>端到端AES-256加密</span>
            </div>
            <div class="feature-item">
              <el-icon><Lightning /></el-icon>
              <span>极速秒传体验</span>
            </div>
            <div class="feature-item">
              <el-icon><Share /></el-icon>
              <span>灵活的文件分享</span>
            </div>
          </div>
        </div>
        <!-- 装饰性背景圆 -->
        <div class="circle-decor circle-1"></div>
        <div class="circle-decor circle-2"></div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="login-right">
        <div class="login-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号以继续</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
          size="large"
        >
          <el-form-item prop="account">
            <el-input
              v-model="loginForm.account"
              placeholder="请输入邮箱或用户名"
              clearable
              class="custom-input"
            >
              <template #prefix>
                <el-icon class="input-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              clearable
              class="custom-input"
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="verificationCode">
            <div class="verify-code-container">
              <el-input
                v-model="loginForm.verificationCode"
                placeholder="验证码"
                maxlength="6"
                class="custom-input verify-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Key /></el-icon>
                </template>
              </el-input>
              <el-button
                class="send-code-btn"
                :disabled="countdown > 0"
                :loading="sendingCode"
                @click="handleSendCode"
                plain
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              @click="handleLogin"
              class="login-button"
            >
              立即登录
            </el-button>
          </el-form-item>

          <div class="login-footer">
            <router-link to="/reset-password" class="link-text">忘记密码？</router-link>
            <span class="divider">|</span>
            <router-link to="/register" class="link-text highlight">注册新账号</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, Cloudy, Lightning, Share } from '@element-plus/icons-vue'
import { sendCode } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer = null

// 表单数据
const loginForm = reactive({
  account: '',
  password: '',
  verificationCode: ''
})

// 表单验证规则
const rules = {
  account: [
    { required: true, message: '请输入邮箱或用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符之间', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ]
}

// 发送验证码
const handleSendCode = async () => {
  try {
    await loginFormRef.value.validateField('account')
  } catch {
    return
  }

  sendingCode.value = true

  try {
    await sendCode(loginForm.account, 2) // 2表示登录验证码
    ElMessage.success('验证码已发送到您的邮箱')

    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    sendingCode.value = false
  }
}

// 登录
const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true

  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-gray-50);
  background-image: 
    radial-gradient(at 10% 10%, rgba(251, 114, 153, 0.1) 0px, transparent 50%),
    radial-gradient(at 90% 90%, rgba(0, 161, 214, 0.1) 0px, transparent 50%);
  padding: 20px;
}

.login-box {
  width: 1000px;
  height: 600px;
  background: var(--bg-white);
  border-radius: 20px;
  box-shadow: var(--shadow-xl);
  display: flex;
  overflow: hidden;
  transition: transform 0.3s ease;
}

/* 左侧品牌区 */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.brand-content {
  position: relative;
  z-index: 2;
}

.logo-circle {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  backdrop-filter: blur(10px);
}

.login-left h1 {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.slogan {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 48px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.1);
  padding: 12px 20px;
  border-radius: 12px;
  backdrop-filter: blur(5px);
  transition: transform 0.3s;
}

.feature-item:hover {
  transform: translateX(10px);
  background: rgba(255, 255, 255, 0.2);
}

/* 装饰圆 */
.circle-decor {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -50px;
  right: -50px;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -20px;
  left: -20px;
}

/* 右侧表单区 */
.login-right {
  flex: 1;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: var(--bg-white);
}

.login-header {
  margin-bottom: 40px;
}

.login-header h2 {
  font-size: 28px;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.login-header p {
  color: var(--text-secondary);
  font-size: 15px;
}

/* 输入框样式定制 */
.custom-input :deep(.el-input__wrapper) {
  background-color: var(--bg-gray-50);
  box-shadow: none;
  border: 1px solid transparent;
  transition: all 0.3s;
  padding: 8px 15px;
  border-radius: var(--radius-medium);
}

.custom-input :deep(.el-input__wrapper:hover) {
  background-color: var(--bg-white);
  border-color: var(--text-tertiary);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  background-color: var(--bg-white);
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-lighter);
}

.input-icon {
  font-size: 18px;
  color: var(--text-secondary);
}

/* 验证码区域 */
.verify-code-container {
  display: flex;
  gap: 12px;
}

.verify-input {
  flex: 1;
}

.send-code-btn {
  width: 110px;
  height: 48px; /* 匹配大尺寸输入框高度 */
  border-radius: var(--radius-medium);
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: var(--primary-lighter);
  transition: all 0.3s;
}

.send-code-btn:hover {
  background: var(--primary-color);
  color: #fff;
}

/* 登录按钮 */
.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  border-radius: var(--radius-medium);
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border: none;
  box-shadow: var(--shadow-pink);
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(251, 114, 153, 0.4);
}

.login-button:active {
  transform: translateY(0);
}

/* 底部链接 */
.login-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.link-text {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.link-text:hover {
  color: var(--primary-color);
}

.link-text.highlight {
  color: var(--primary-color);
  font-weight: 500;
}

.divider {
  color: var(--bg-gray-200);
}

/* 响应式适配 */
@media (max-width: 900px) {
  .login-box {
    width: 100%;
    max-width: 500px;
    height: auto;
    flex-direction: column;
  }

  .login-left {
    display: none; /* 移动端隐藏左侧装饰 */
  }

  .login-right {
    padding: 40px 30px;
  }
}
</style>
