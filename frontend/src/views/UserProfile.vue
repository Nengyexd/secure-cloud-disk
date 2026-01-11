<template>
  <div class="user-profile">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button class="back-btn" :icon="ArrowLeft" @click="goBack" plain>返回首页</el-button>
          <span class="header-title">个人中心</span>
        </div>
      </el-header>

      <el-main class="content">
        <el-row :gutter="24">
          <!-- 左侧：用户信息卡片 -->
          <el-col :span="8">
            <el-card class="profile-card info-card" :body-style="{ padding: '30px' }">
              <div class="user-header-section">
                <div class="user-avatar-container">
                  <el-upload
                    class="avatar-uploader"
                    :action="uploadUrl"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :before-upload="beforeAvatarUpload"
                    :on-success="handleAvatarSuccess"
                    :on-error="handleAvatarError"
                  >
                    <div class="avatar-wrapper">
                      <img v-if="userInfo.avatar" :src="getAvatarUrl(userInfo.avatar)" class="avatar" />
                      <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                      <div class="avatar-mask">
                        <el-icon><Camera /></el-icon>
                      </div>
                    </div>
                  </el-upload>
                  <div class="vip-badge" v-if="userInfo.userType === 1">
                    <el-icon><Trophy /></el-icon>
                  </div>
                </div>
                <h2 class="username">{{ userInfo.username }}</h2>
                <p class="email">{{ userInfo.email }}</p>
                <div class="user-tags">
                  <el-tag :type="userInfo.userType === 1 ? 'warning' : 'info'" effect="dark" round>
                    {{ userInfo.userType === 1 ? 'VIP会员' : '普通用户' }}
                  </el-tag>
                  <el-tag type="success" effect="plain" round>
                    注册于 {{ formatDateTime(userInfo.createdAt).split(' ')[0] }}
                  </el-tag>
                </div>
              </div>

              <el-divider border-style="dashed" />

              <div class="user-details">
                <div v-if="userInfo.vipExpireTime" class="detail-row">
                  <span class="label">VIP到期</span>
                  <span class="value" :style="{ color: vipExpireColor }">
                    {{ formatDateTime(userInfo.vipExpireTime) }}
                    <span v-if="vipDaysRemaining <= 7 && vipDaysRemaining > 0" class="expire-tag warning">
                      剩{{ vipDaysRemaining }}天
                    </span>
                    <span v-else-if="vipDaysRemaining === 0" class="expire-tag danger">
                      已过期
                    </span>
                  </span>
                </div>

                <!-- VIP到期提醒 -->
                <div v-if="userInfo.userType === 1 && vipDaysRemaining !== null && vipDaysRemaining <= 7" class="vip-alert">
                  <div class="alert-content" :class="vipDaysRemaining === 0 ? 'danger' : 'warning'">
                    <el-icon><Warning /></el-icon>
                    <span>{{ vipDaysRemaining === 0 ? 'VIP已过期，部分特权失效' : `VIP仅剩 ${vipDaysRemaining} 天，请及时续费` }}</span>
                  </div>
                  <el-button type="primary" link size="small" @click="goToVip">立即续费</el-button>
                </div>
              </div>

              <div class="action-buttons">
                <el-button type="primary" @click="showEditDialog" class="action-btn" plain>编辑资料</el-button>
                <el-button @click="showPasswordDialog" class="action-btn" plain>修改密码</el-button>
              </div>
            </el-card>

            <!-- 安全设置卡片 -->
            <el-card class="profile-card security-card">
              <template #header>
                <div class="card-header">
                  <div class="header-icon safe"><el-icon><Lock /></el-icon></div>
                  <span>账户安全</span>
                </div>
              </template>

              <div class="security-list">
                <div class="security-row">
                  <div class="sec-info">
                    <div class="sec-title">登录日志</div>
                    <div class="sec-desc">查看最近的登录记录和IP</div>
                  </div>
                  <el-button type="primary" link @click="showLoginLogs">查看</el-button>
                </div>
                
                <div class="security-row">
                  <div class="sec-info">
                    <div class="sec-title">最后登录</div>
                    <div class="sec-desc">{{ formatDateTime(userInfo.lastLoginTime) }} ({{ userInfo.lastLoginIp || '未知IP' }})</div>
                  </div>
                </div>

                <div class="security-row danger">
                  <div class="sec-info">
                    <div class="sec-title">注销账号</div>
                    <div class="sec-desc">永久删除账号及所有数据</div>
                  </div>
                  <el-button type="danger" link @click="showDeleteAccountDialog">注销</el-button>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 右侧：统计与存储 -->
          <el-col :span="16">
            <!-- 顶部统计卡片 -->
            <el-row :gutter="20" class="stat-row">
              <el-col :span="8">
                <div class="stat-box blue">
                  <div class="stat-icon"><el-icon><Document /></el-icon></div>
                  <div class="stat-data">
                    <div class="stat-num">{{ userInfo.totalFiles }}</div>
                    <div class="stat-name">文件总数</div>
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-box green">
                  <div class="stat-icon"><el-icon><Folder /></el-icon></div>
                  <div class="stat-data">
                    <div class="stat-num">{{ userInfo.totalFolders }}</div>
                    <div class="stat-name">文件夹</div>
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-box orange">
                  <div class="stat-icon"><el-icon><Share /></el-icon></div>
                  <div class="stat-data">
                    <div class="stat-num">{{ userInfo.totalShares }}</div>
                    <div class="stat-name">分享记录</div>
                  </div>
                </div>
              </el-col>
            </el-row>

            <!-- 存储空间卡片 -->
            <el-card class="profile-card storage-card">
              <template #header>
                <div class="card-header">
                  <div class="header-icon pink"><el-icon><PieChart /></el-icon></div>
                  <span>存储空间分布</span>
                  <el-tag size="small" type="info" class="header-tag">Total: {{ formatBytes(userInfo.storageQuota) }}</el-tag>
                </div>
              </template>

              <div class="storage-container">
                <div class="storage-chart-area">
                  <el-progress
                    type="dashboard"
                    :percentage="storagePercentage"
                    :width="220"
                    :stroke-width="12"
                    :color="storageColorScale"
                    linecap="round"
                  >
                    <template #default="{ percentage }">
                      <div class="progress-content">
                        <span class="progress-label">已使用</span>
                        <span class="progress-value">{{ percentage }}%</span>
                        <span class="progress-sub">{{ formatBytes(userInfo.storageUsed) }}</span>
                      </div>
                    </template>
                  </el-progress>
                </div>

                <div class="storage-info-list">
                  <div class="storage-item">
                    <div class="item-label">
                      <span class="dot used"></span>
                      <span>已用空间</span>
                    </div>
                    <div class="item-value">{{ formatBytes(userInfo.storageUsed) }}</div>
                  </div>
                  <div class="storage-item">
                    <div class="item-label">
                      <span class="dot free"></span>
                      <span>剩余空间</span>
                    </div>
                    <div class="item-value">{{ formatBytes(userInfo.storageQuota - userInfo.storageUsed) }}</div>
                  </div>
                  
                  <div class="storage-bar-wrapper">
                    <div class="bar-bg">
                      <div class="bar-fill" :style="{ width: storagePercentage + '%', background: getStorageColor }"></div>
                    </div>
                  </div>

                  <div class="storage-advice" v-if="storagePercentage >= 80">
                    <el-icon color="#f56c6c"><Warning /></el-icon>
                    <span>空间不足？</span>
                    <el-link type="primary" @click="goToVip">升级VIP扩容</el-link>
                  </div>
                </div>
              </div>
            </el-card>

            <!-- 近期动态 -->
            <el-card class="profile-card" style="margin-top: 24px;">
              <template #header>
                <div class="card-header">
                  <div class="header-icon purple"><el-icon><Clock /></el-icon></div>
                  <span>账号动态</span>
                  <el-button link type="primary" class="header-tag" @click="goToOperationLog">查看全部</el-button>
                </div>
              </template>
              
              <div v-loading="loadingRecentLogs" class="timeline-container">
                <el-timeline v-if="recentLogs.length > 0">
                  <el-timeline-item
                    v-for="(log, index) in recentLogs"
                    :key="index"
                    :timestamp="formatDateTime(log.operationTime)"
                    :color="getOperationTypeColor(log.operationType)"
                    :hollow="true"
                  >
                    <div class="log-content">
                      <span class="log-type" :style="{ color: getOperationTypeColor(log.operationType) }">
                        {{ log.operationTypeDesc }}
                      </span>
                      <span class="log-desc">{{ log.description }}</span>
                    </div>
                  </el-timeline-item>
                </el-timeline>
                <el-empty v-else description="暂无更多动态" :image-size="100" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>


    <!-- 编辑资料对话框 -->
    <el-dialog v-model="editVisible" title="编辑资料" width="500px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateInfo" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="500px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（6-20位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword" :loading="saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 登录日志对话框 -->
    <el-dialog v-model="loginLogsVisible" title="登录日志" width="700px">
      <el-table :data="loginLogs" stripe style="width: 100%" v-loading="loadingLogs">
        <el-table-column prop="loginIp" label="登录IP" width="150" />
        <el-table-column prop="loginLocation" label="登录地点" width="120">
          <template #default="{ row }">
            {{ row.loginLocation || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="success" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" />
        <el-table-column prop="loginTime" label="登录时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.loginTime) }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="loginLogsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 注销账号确认对话框 -->
    <el-dialog v-model="deleteAccountVisible" title="注销账号" width="400px">
      <div style="margin-bottom: 20px;">
        <el-alert
          title="警告：此操作不可逆！"
          type="error"
          description="注销账号将永久删除您的所有文件、分享记录和个人信息，且无法恢复。请输入密码确认操作。"
          show-icon
          :closable="false"
        />
      </div>
      <el-form :model="deleteAccountForm" :rules="deleteAccountRules" ref="deleteAccountFormRef">
        <el-form-item prop="password">
          <el-input
            v-model="deleteAccountForm.password"
            type="password"
            placeholder="请输入登录密码以确认"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deleteAccountVisible = false">取消</el-button>
        <el-button type="danger" @click="handleDeleteAccount" :loading="deletingAccount">确认注销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  User,
  UserFilled,
  DataAnalysis,
  Document,
  Folder,
  Share,
  PieChart,
  Plus,
  Lock,
  Location,
  Clock,
  Warning,
  Camera,
  Trophy,
  MoreFilled,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import { getUserInfo, updateUserInfo, updatePassword, getLoginLogs, deleteAccount } from '@/api/user'
import { getMyOperationLogs } from '@/api/operationLog'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

// 存储进度条颜色
const storageColorScale = [
  { color: '#67c23a', percentage: 50 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 }
]


// 用户信息
const userInfo = reactive({
  username: '',
  email: '',
  avatar: '',
  userType: 0,
  vipExpireTime: null,
  storageUsed: 0,
  storageQuota: 0,
  totalFiles: 0,
  totalFolders: 0,
  totalShares: 0,
  createdAt: null,
  lastLoginTime: null,
  lastLoginIp: ''
})

// 头像上传
const uploadUrl = import.meta.env.VITE_API_BASE_URL + '/user/avatar'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

// 弹窗控制
const editVisible = ref(false)
const passwordVisible = ref(false)
const loginLogsVisible = ref(false)
const deleteAccountVisible = ref(false)

// 表单数据
const editForm = reactive({ username: '', email: '' })
const editFormRef = ref(null)
const editRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '格式不正确', trigger: 'blur' }]
}

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordFormRef = ref(null)
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, max: 20, message: '长度6-20位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, {
    validator: (rule, value, callback) => {
      if (value !== passwordForm.newPassword) callback(new Error('两次输入不一致'))
      else callback()
    }, trigger: 'blur'
  }]
}

const deleteAccountForm = reactive({ password: '' })
const deleteAccountFormRef = ref(null)
const deleteAccountRules = {
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 加载状态
const saving = ref(false)
const loadingLogs = ref(false)
const deletingAccount = ref(false)
const loginLogs = ref([])
const recentLogs = ref([])
const loadingRecentLogs = ref(false)

// 计算属性
const vipDaysRemaining = computed(() => {
  if (!userInfo.vipExpireTime) return null
  const now = new Date()
  const expire = new Date(userInfo.vipExpireTime)
  const diff = expire - now
  if (diff < 0) return 0
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
})

const storagePercentage = computed(() => {
  if (!userInfo.storageQuota) return 0
  return Math.min(100, Math.round((userInfo.storageUsed / userInfo.storageQuota) * 100))
})

// VIP到期颜色
const vipExpireColor = computed(() => {
  if (vipDaysRemaining.value === null) return 'var(--text-secondary)'
  if (vipDaysRemaining.value === 0) return 'var(--danger-color)'
  if (vipDaysRemaining.value <= 7) return 'var(--warning-color)'
  return 'var(--text-primary)'
})

const getStorageColor = computed(() => {
  const percentage = storagePercentage.value
  if (percentage < 50) return 'var(--success-color)'
  if (percentage < 80) return 'var(--warning-color)'
  return 'var(--danger-color)'
})


onMounted(() => {
  loadUserInfo()
  loadRecentLogs()
})

const loadUserInfo = async () => {
  try {
    const res = await getUserInfo()
    if (res.code === 200) {
      Object.assign(userInfo, res.data)
      // 同步更新 store 中的用户信息
      userStore.updateUserInfo(res.data)
    }
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  }
}

const loadRecentLogs = async () => {
  loadingRecentLogs.value = true
  try {
    const res = await getMyOperationLogs({
      pageNum: 1,
      pageSize: 5
    })
    if (res.code === 200) {
      recentLogs.value = res.data.records
    }
  } catch (error) {
    console.error('加载动态失败', error)
  } finally {
    loadingRecentLogs.value = false
  }
}

const getOperationTypeColor = (type) => {
  const map = {
    UPLOAD: '#409eff',
    DOWNLOAD: '#67c23a',
    DELETE: '#f56c6c',
    SHARE: '#e6a23c',
    RENAME: '#909399',
    MOVE: '#909399',
    CREATE_FOLDER: '#409eff',
    RESTORE: '#67c23a'
  }
  return map[type] || '#909399'
}

const goToOperationLog = () => {
  router.push('/operation-log')
}

const goBack = () => {
  router.push('/home')
}

const goToVip = () => {
  router.push('/vip')
}

const showEditDialog = () => {
  editForm.username = userInfo.username
  editForm.email = userInfo.email
  editVisible.value = true
}

const showPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

const handleUpdateInfo = async () => {
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        await updateUserInfo(editForm)
        ElMessage.success('更新成功')
        editVisible.value = false
        loadUserInfo()
      } catch (error) {
        ElMessage.error('更新失败: ' + (error.response?.data?.message || error.message))
      } finally {
        saving.value = false
      }
    }
  })
}

const handleUpdatePassword = async () => {
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        await updatePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })

        passwordVisible.value = false
        ElMessage.success('密码修改成功，3秒后跳转到登录页')

        // 清除登录状态
        setTimeout(() => {
          userStore.logout()
          router.push('/login')
        }, 3000)
      } catch (error) {
        ElMessage.error('修改失败: ' + (error.response?.data?.message || error.message))
      } finally {
        saving.value = false
      }
    }
  })
}

const formatBytes = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  const date = new Date(datetime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 头像上传相关方法
const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) {
    return avatar
  }
  return import.meta.env.VITE_API_BASE_URL + avatar
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('头像图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    userInfo.avatar = response.data
    ElMessage.success('头像上传成功')
    // 重新加载用户信息
    loadUserInfo()
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

const handleAvatarError = (error) => {
  console.error('头像上传失败:', error)
  ElMessage.error('头像上传失败，请重试')
}

// 登录日志相关方法
const showLoginLogs = async () => {
  loginLogsVisible.value = true
  loadingLogs.value = true

  try {
    const res = await getLoginLogs(20)
    if (res.code === 200) {
      loginLogs.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载登录日志失败')
  } finally {
    loadingLogs.value = false
  }
}

// 注销账号相关方法
const showDeleteAccountDialog = () => {
  deleteAccountForm.password = ''
  deleteAccountVisible.value = true
  nextTick(() => {
    if (deleteAccountFormRef.value) {
      deleteAccountFormRef.value.clearValidate()
    }
  })
}

const handleDeleteAccount = () => {
  console.log('点击注销按钮 - 手动校验模式')
  
  // 手动校验密码，避开 form.validate 可能存在的问题
  if (!deleteAccountForm.password || deleteAccountForm.password.trim() === '') {
    ElMessage.warning('请输入密码以确认操作')
    return
  }

  ElMessageBox.confirm(
    '确定要永久注销账号吗？此操作无法撤销！',
    '最终确认',
    {
      confirmButtonText: '确定注销',
      cancelButtonText: '取消',
      type: 'error'
    }
  ).then(async () => {
    deletingAccount.value = true
    try {
      await deleteAccount(deleteAccountForm.password)
      ElMessage.success('账号已注销')
      deleteAccountVisible.value = false
      // 清除登录状态并跳转
      userStore.logout()
      router.push('/login')
    } catch (error) {
      console.error('注销失败:', error)
      ElMessage.error('注销失败: ' + (error.response?.data?.message || error.message || '未知错误'))
    } finally {
      deletingAccount.value = false
    }
  }).catch(() => {
    // 用户点击取消，不做任何操作
  })
}

</script>

<style scoped>
.user-profile {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--bg-gray-50);
}

.header {
  background: var(--bg-white);
  display: flex;
  align-items: center;
  box-shadow: var(--shadow-sm);
  padding: 0 24px;
  position: relative;
  z-index: 10;
}

.header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--primary-color) 0%, var(--accent-blue) 100%);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  border: none;
  font-weight: 500;
  color: var(--text-secondary);
}

.back-btn:hover {
  background: var(--bg-gray-100);
  color: var(--primary-color);
}

.header-title {
  font-size: 20px;
  font-weight: bold;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.content {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 24px;
  border-radius: var(--radius-large);
  border: none;
  box-shadow: var(--shadow-base);
  transition: all 0.3s;
  background: var(--bg-white);
}

.profile-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.header-icon.safe { background: var(--success-light); color: var(--success-color); }
.header-icon.pink { background: var(--primary-lighter); color: var(--primary-color); }
.header-icon.purple { background: #f3e5f5; color: #9c27b0; }

.header-tag {
  margin-left: auto;
  border-radius: var(--radius-round);
}

/* 用户头部信息 */
.user-header-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-bottom: 10px;
}

.user-avatar-container {
  position: relative;
  margin-bottom: 16px;
}

.avatar-wrapper {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-blue));
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #fff;
  display: block;
  box-sizing: border-box;
}

.avatar-uploader-icon {
  width: 100%;
  height: 100%;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  font-size: 24px;
}

.avatar-mask {
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.vip-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(255, 165, 0, 0.4);
}

.username {
  font-size: 22px;
  font-weight: bold;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.email {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.user-tags {
  display: flex;
  gap: 8px;
}

/* 用户详情 */
.user-details {
  padding: 10px 0;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  font-size: 14px;
}

.label {
  color: var(--text-secondary);
}

.value {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.expire-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}
.expire-tag.warning { background: var(--warning-light); color: var(--warning-color); }
.expire-tag.danger { background: var(--danger-light); color: var(--danger-color); }

.vip-alert {
  background: var(--warning-light);
  border-radius: var(--radius-medium);
  padding: 10px 16px;
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--warning-color);
}
.alert-content.danger { color: var(--danger-color); }

/* 按钮组 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.action-btn {
  flex: 1;
  border-radius: var(--radius-medium);
}

/* 安全列表 */
.security-list {
  padding: 0 10px;
}

.security-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px dashed var(--bg-gray-200);
}

.security-row:last-child {
  border-bottom: none;
}

.sec-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.sec-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.security-row.danger .sec-title { color: var(--danger-color); }

/* 统计盒子 */
.stat-row {
  margin-bottom: 24px;
}

.stat-box {
  background: var(--bg-white);
  border-radius: var(--radius-large);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-base);
  transition: all 0.3s;
}

.stat-box:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-box.blue .stat-icon { background: linear-gradient(135deg, #409eff, #79bbff); box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3); }
.stat-box.green .stat-icon { background: linear-gradient(135deg, #67c23a, #95d475); box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3); }
.stat-box.orange .stat-icon { background: linear-gradient(135deg, #e6a23c, #f3d19e); box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3); }

.stat-data {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 24px;
  font-weight: bold;
  color: var(--text-primary);
  font-family: 'DIN Alternate', sans-serif;
}

.stat-name {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 存储空间 */
.storage-container {
  display: flex;
  align-items: center;
  padding: 20px 0;
}

.storage-chart-area {
  flex: 0 0 240px;
  display: flex;
  justify-content: center;
}

.progress-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-label { font-size: 12px; color: var(--text-secondary); }
.progress-value { font-size: 32px; font-weight: bold; color: var(--text-primary); margin: 4px 0; }
.progress-sub { font-size: 12px; color: var(--text-tertiary); }

.storage-info-list {
  flex: 1;
  padding-left: 40px;
}

.storage-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.item-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.dot.used { background: var(--primary-color); }
.dot.free { background: var(--bg-gray-200); }

.item-value {
  font-weight: 600;
  color: var(--text-primary);
}

.storage-bar-wrapper {
  margin-top: 20px;
  margin-bottom: 20px;
}

.bar-bg {
  height: 8px;
  background: var(--bg-gray-200);
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.storage-advice {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-gray-50);
  padding: 8px 12px;
  border-radius: var(--radius-medium);
}

.timeline-container {
  padding: 10px 20px 0;
}

.log-content {
  font-size: 14px;
}

.log-type {
  font-weight: 600;
  margin-right: 8px;
}

.log-desc {
  color: var(--text-secondary);
}
</style>
