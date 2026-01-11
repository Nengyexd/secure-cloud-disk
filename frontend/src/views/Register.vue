<template>
  <div class="register-container">
    <div class="register-box">
      <!-- 左侧品牌展示区 -->
      <div class="register-left">
        <div class="brand-content">
          <div class="logo-circle">
            <el-icon :size="48" color="#FFFFFF"><UserFilled /></el-icon>
          </div>
          <h1>加入我们</h1>
          <p class="slogan">开启您的安全云存储之旅</p>
          <div class="feature-list">
            <div class="feature-item">
              <el-icon><Monitor /></el-icon>
              <span>多端实时同步</span>
            </div>
            <div class="feature-item">
              <el-icon><Lock /></el-icon>
              <span>私有密钥加密</span>
            </div>
            <div class="feature-item">
              <el-icon><Star /></el-icon>
              <span>会员专属高速通道</span>
            </div>
          </div>
        </div>
        <!-- 装饰性背景圆 -->
        <div class="circle-decor circle-1"></div>
        <div class="circle-decor circle-2"></div>
      </div>

      <!-- 右侧注册表单区 -->
      <div class="register-right">
        <div class="register-header">
          <h2>创建账号</h2>
          <p>只需几步，轻松注册</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="rules"
          class="register-form"
          size="large"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              clearable
              class="custom-input"
            >
              <template #prefix>
                <el-icon class="input-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              clearable
              class="custom-input"
            >
              <template #prefix>
                <el-icon class="input-icon"><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              clearable
              class="custom-input"
              @input="checkPasswordStrength"
            >
              <template #prefix>
                <el-icon class="input-icon"><Lock /></el-icon>
              </template>
            </el-input>
            <!-- 密码强度条 -->
            <div class="password-strength" v-if="registerForm.password">
              <div class="strength-bar">
                <div 
                  class="strength-fill" 
                  :style="{ width: strengthWidth, backgroundColor: strengthColor }"
                ></div>
              </div>
              <span class="strength-text" :style="{ color: strengthColor }">{{ strengthText }}</span>
            </div>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
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
                v-model="registerForm.verificationCode"
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
              @click="handleRegister"
              class="register-button"
            >
              立即注册
            </el-button>
          </el-form-item>

          <div class="register-footer">
            <span class="text-secondary">已有账号？</span>
            <router-link to="/login" class="link-text highlight">立即登录</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, User, Lock, Message, Key, Monitor, Star } from '@element-plus/icons-vue'
import { sendCode } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer = null

// 表单数据
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

// 密码强度相关
const strengthScore = ref(0)

const checkPasswordStrength = (val) => {
  let score = 0
  if (!val) {
    strengthScore.value = 0
    return
  }
  if (val.length >= 6) score++
  if (val.length >= 10) score++
  if (/[A-Z]/.test(val)) score++
  if (/[0-9]/.test(val)) score++
  if (/[^A-Za-z0-9]/.test(val)) score++
  strengthScore.value = score
}

const strengthWidth = computed(() => {
  return (strengthScore.value / 5) * 100 + '%'
})

const strengthColor = computed(() => {
  if (strengthScore.value <= 2) return '#ff4d4f' // 弱
  if (strengthScore.value <= 3) return '#faad14' // 中
  return '#52c41a' // 强
})

const strengthText = computed(() => {
  if (strengthScore.value === 0) return ''
  if (strengthScore.value <= 2) return '弱'
  if (strengthScore.value <= 3) return '中'
  return '强'
})

// 自定义验证规则：确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      min: 3,
      max: 20,
      message: '用户名长度在3-20个字符之间',
      trigger: 'blur'
    },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '用户名只能包含字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ]
}

// 发送验证码
const handleSendCode = async () => {
  // 先验证邮箱
  try {
    await registerFormRef.value.validateField('email')
  } catch {
    return
  }

  sendingCode.value = true

  try {
    await sendCode(registerForm.email, 1) // 1表示注册验证码
    ElMessage.success('验证码已发送到您的邮箱')

    // 开始倒计时
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

// 注册
const handleRegister = async () => {
  // 表单验证
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true

  try {
    await userStore.register(registerForm)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}

// 组件卸载时清除定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.register-container {
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

.register-box {
  width: 1000px;
  height: 700px; /* 注册表单较长，增加高度 */
  background: var(--bg-white);
  border-radius: 20px;
  box-shadow: var(--shadow-xl);
  display: flex;
  overflow: hidden;
  transition: transform 0.3s ease;
}

/* 左侧品牌区 */
.register-left {
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

.register-left h1 {
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
.register-right {
  flex: 1;
  padding: 40px 60px; /* 上下内边距减少以适应长表单 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: var(--bg-white);
  overflow-y: auto; /* 允许小屏幕下滚动 */
}

.register-header {
  margin-bottom: 30px;
}

.register-header h2 {
  font-size: 28px;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.register-header p {
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

/* 密码强度条 */
.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background-color: var(--bg-gray-200);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
}

.strength-text {
  font-size: 12px;
  width: 20px;
  text-align: right;
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
  height: 48px;
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

/* 注册按钮 */
.register-button {
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

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(251, 114, 153, 0.4);
}

.register-button:active {
  transform: translateY(0);
}

/* 底部链接 */
.register-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
}

.text-secondary {
  color: var(--text-secondary);
  font-size: 14px;
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

/* 响应式适配 */
@media (max-width: 900px) {
  .register-box {
    width: 100%;
    max-width: 500px;
    height: auto;
    flex-direction: column;
  }

  .register-left {
    display: none;
  }

  .register-right {
    padding: 40px 30px;
  }
}
</style>
