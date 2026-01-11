<template>
  <div class="share-access-page">
    <div class="share-container">
      <div class="share-header">
        <el-icon :size="60" color="#409eff"><Share /></el-icon>
        <h2>吉亦云盘 - 文件分享</h2>
      </div>

      <div v-if="!accessGranted" class="access-form">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>访问分享文件</span>
            </div>
          </template>

          <el-form :model="accessForm" @submit.prevent="handleAccess">
            <el-form-item label="分享码">
              <el-input v-model="shareCode" disabled />
            </el-form-item>

            <el-form-item v-if="requirePassword" label="访问密码">
              <el-input
                v-model="accessForm.sharePassword"
                type="password"
                placeholder="请输入访问密码"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleAccess" :loading="loading" style="width: 100%;">
                访问
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <div v-else class="file-info">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ fileInfo.isFolder === 1 ? '文件夹信息' : '文件信息' }}</span>
            </div>
          </template>

          <div class="info-item">
            <span class="label">{{ fileInfo.isFolder === 1 ? '文件夹名：' : '文件名：' }}</span>
            <span class="value">{{ fileInfo.fileName }}</span>
          </div>

          <div v-if="fileInfo.isFolder === 0" class="info-item">
            <span class="label">文件大小：</span>
            <span class="value">{{ formatBytes(fileInfo.fileSize) }}</span>
          </div>

          <div class="info-item">
            <span class="label">分享者：</span>
            <span class="value">
              {{ fileInfo.creatorName }}
              <el-tag v-if="fileInfo.creatorUserType === 1" type="warning" size="small" style="margin-left: 8px;">
                <el-icon><Trophy /></el-icon> VIP
              </el-tag>
            </span>
          </div>

          <div class="info-item" v-if="fileInfo.expireTime">
            <span class="label">有效期至：</span>
            <span class="value">{{ formatDateTime(fileInfo.expireTime) }}</span>
          </div>

          <div class="info-item" v-else>
            <span class="label">有效期：</span>
            <span class="value">永久有效</span>
          </div>

          <el-divider />

          <div v-if="fileInfo.isFolder === 1" class="folder-notice">
            <el-alert
              title="文件夹分享说明"
              type="info"
              :closable="false"
              show-icon
            >
              <p>当前分享的是一个文件夹，暂不支持直接下载整个文件夹。</p>
              <p>请联系分享者获取文件夹内的单个文件链接。</p>
            </el-alert>
          </div>

          <div v-else class="action-buttons">
            <el-button type="primary" size="large" @click="handleDownloadShare" :loading="downloading">
              <el-icon><Download /></el-icon>
              下载文件
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Share, Download, Trophy } from '@element-plus/icons-vue'
import { getShareInfo, accessShare, recordDownload } from '@/api/share'
import { getFileDownloadInfo, downloadFile } from '@/api/file'
import { useUserStore } from '@/store/user'
import AESUtil from '@/utils/aes'

const route = useRoute()
const userStore = useUserStore()

const shareCode = ref('')
const accessGranted = ref(false)
const requirePassword = ref(false)
const loading = ref(false)
const downloading = ref(false)

const accessForm = reactive({
  sharePassword: ''
})

const fileInfo = reactive({
  shareId: null,
  fileId: null,
  fileName: '',
  fileSize: 0,
  fileType: '',
  isFolder: 0,
  creatorName: '',
  creatorUserType: 0, // 分享者用户类型
  expireTime: null,
  hasPassword: false
})

onMounted(() => {
  shareCode.value = route.params.shareCode
  loadShareInfo()
})

// 先获取分享基本信息
const loadShareInfo = async () => {
  loading.value = true
  try {
    const res = await getShareInfo(shareCode.value)

    if (res.code === 200) {
      Object.assign(fileInfo, res.data)

      // 如果不需要密码，直接显示文件信息
      if (!res.data.hasPassword) {
        accessGranted.value = true
      } else {
        // 需要密码，显示密码输入框
        requirePassword.value = true
      }
    }
  } catch (error) {
    console.error('获取分享信息失败:', error)
    ElMessage.error('获取分享信息失败: ' + (error.response?.data?.message || error.message || '请重试'))
  } finally {
    loading.value = false
  }
}

const handleAccess = async () => {
  loading.value = true
  try {
    const res = await accessShare({
      shareCode: shareCode.value,
      sharePassword: accessForm.sharePassword
    })

    if (res.code === 200) {
      Object.assign(fileInfo, res.data)
      accessGranted.value = true
      ElMessage.success('访问成功')
    }
  } catch (error) {
    console.error('访问分享失败:', error)
    ElMessage.error('访问失败: ' + (error.response?.data?.message || error.message || '请重试'))
  } finally {
    loading.value = false
  }
}

const handleDownloadShare = async () => {
  // 检查是否为文件夹
  if (fileInfo.isFolder === 1) {
    ElMessage.warning('暂不支持文件夹下载，请联系分享者获取文件夹内的单个文件')
    return
  }

  downloading.value = true

  const loadingMsg = ElMessage({
    message: '正在下载文件...',
    type: 'info',
    duration: 0
  })

  try {
    // 检查是否登录
    if (!userStore.isLoggedIn || !userStore.masterKey) {
      ElMessage.warning('请先登录后再下载文件')
      loadingMsg.close()
      downloading.value = false
      return
    }

    // 获取文件下载信息
    const infoRes = await getFileDownloadInfo(fileInfo.fileId)
    const downloadInfo = infoRes.data

    // 下载加密文件
    const fileRes = await downloadFile(fileInfo.fileId)
    const encryptedData = fileRes.data

    // 解密文件
    const decryptedData = await AESUtil.decryptFile(
      encryptedData,
      downloadInfo.encryptedKey,
      userStore.masterKey
    )

    // 创建 Blob 并下载
    const blob = new Blob([decryptedData], { type: downloadInfo.fileType || 'application/octet-stream' })
    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = downloadInfo.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    URL.revokeObjectURL(url)

    // 记录下载次数
    await recordDownload(shareCode.value)

    loadingMsg.close()
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    loadingMsg.close()
    ElMessage.error('下载失败: ' + (error.response?.data?.message || error.message || '请重试'))
  } finally {
    downloading.value = false
  }
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
</script>

<style scoped>
.share-access-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.share-container {
  width: 100%;
  max-width: 500px;
}

.share-header {
  text-align: center;
  margin-bottom: 30px;
  color: #fff;
}

.share-header h2 {
  margin-top: 20px;
  font-size: 28px;
}

.card-header {
  font-size: 18px;
  font-weight: 500;
}

.access-form,
.file-info {
  animation: fadeInUp 0.5s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.info-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-of-type {
  border-bottom: none;
}

.info-item .label {
  font-weight: 500;
  color: #606266;
  min-width: 100px;
}

.info-item .value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}

.action-buttons {
  display: flex;
  justify-content: center;
  padding-top: 10px;
}

.action-buttons .el-button {
  min-width: 200px;
}

.folder-notice {
  padding-top: 10px;
}

.folder-notice p {
  margin: 5px 0;
}
</style>
