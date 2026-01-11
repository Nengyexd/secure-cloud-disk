<template>
  <div class="reset-password-page">
    <div class="reset-password-container">
      <div class="reset-password-card">
        <div class="header">
          <h2>重置密码</h2>
          <p class="subtitle">通过邮箱重置您的密码</p>
        </div>

        <el-alert
          title="重要提示"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        >
          <p>重置密码将清空您的所有文件和分享，此操作不可恢复。</p>
          <p>如果只是修改密码，请登录后在个人中心进行操作。</p>
        </el-alert>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入注册邮箱"
              :prefix-icon="Message"
            />
          </el-form-item>

          <el-form-item label="验证码" prop="verificationCode">
            <div class="code-input">
              <el-input
                v-model="form.verificationCode"
                placeholder="请输入验证码"
                :prefix-icon="Key"
              />
              <el-button
                :disabled="countdown > 0"
                @click="handleSendCode"
                :loading="sending"
              >
                {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="请输入新密码（6-20位）"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              @click="handleResetPassword"
              :loading="loading"
              style="width: 100%;"
            >
              重置密码
            </el-button>
          </el-form-item>

          <div class="footer-links">
            <router-link to="/login">返回登录</router-link>
            <router-link to="/register">没有账号？立即注册</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Key, Lock } from '@element-plus/icons-vue'
import { sendCode } from '@/api/auth'
import request from '@/utils/request'

const router = useRouter()

const form = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: ''
})

const formRef = ref()
const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)

let timer = null

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.error('请先输入邮箱')
    return
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailPattern.test(form.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  sending.value = true
  try {
    await sendCode(form.email, 3)  // 3: 密码重置

    ElMessage.success('验证码已发送，请查收邮件')

    // 开始倒计时
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    ElMessage.error('发送失败: ' + (error.response?.data?.message || error.message))
  } finally {
    sending.value = false
  }
}

const handleResetPassword = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request({
          url: '/auth/reset-password',
          method: 'post',
          data: {
            email: form.email,
            verificationCode: form.verificationCode,
            newPassword: form.newPassword
          }
        })

        ElMessage.success(res.message || '密码重置成功')

        // 3秒后跳转到登录页
        setTimeout(() => {
          router.push('/login')
        }, 3000)
      } catch (error) {
        ElMessage.error('重置失败: ' + (error.response?.data?.message || error.message))
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.reset-password-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.reset-password-container {
  width: 100%;
  max-width: 500px;
}

.reset-password-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.header {
  text-align: center;
  margin-bottom: 30px;
}

.header h2 {
  margin: 0 0 10px 0;
  font-size: 28px;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.code-input {
  display: flex;
  gap: 10px;
}

.code-input .el-input {
  flex: 1;
}

.code-input .el-button {
  min-width: 120px;
}

.footer-links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  font-size: 14px;
}

.footer-links a {
  color: #409eff;
  text-decoration: none;
}

.footer-links a:hover {
  text-decoration: underline;
}

.el-alert p {
  margin: 5px 0;
  font-size: 13px;
}
</style>
