<template>
  <div class="vip-center">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack">返回首页</el-button>
          <span class="header-title">会员中心</span>
        </div>
      </el-header>

      <el-main class="content">
        <!-- 会员状态卡片 -->
        <el-card class="status-card">
          <div class="user-status">
            <el-avatar :size="64" :src="getAvatarUrl(userInfo.avatar)">
              <img src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
            </el-avatar>
            <div class="status-info">
              <div class="username-row">
                <span class="username">{{ userInfo.username }}</span>
                <el-tag v-if="userStore.isVip" type="warning" effect="dark" round>
                  <el-icon><Trophy /></el-icon> VIP会员
                </el-tag>
                <el-tag v-else type="info" effect="plain" round>普通用户</el-tag>
                <el-tag v-if="userInfo.userType === 2" type="danger" effect="dark" round style="margin-left: 8px;">
                  <el-icon><Lock /></el-icon> 管理员
                </el-tag>
              </div>
              <div class="expire-time" v-if="userStore.isVip && userInfo.vipExpireTime">
                <el-icon><Clock /></el-icon>
                VIP有效期至：{{ formatDateTime(userInfo.vipExpireTime) }}
              </div>
              <div class="expire-time" v-else>
                开通VIP，享受更多权益
              </div>
            </div>
          </div>
        </el-card>

        <!-- 权益对比 -->
        <div class="privilege-section">
          <h2 class="section-title">VIP 特权对比</h2>
          <el-row :gutter="20">
            <el-col :span="8" v-for="privilege in privileges" :key="privilege.title">
              <el-card class="privilege-card" shadow="hover">
                <div class="privilege-icon">
                  <el-icon :size="40" :color="privilege.color">
                    <component :is="privilege.icon" />
                  </el-icon>
                </div>
                <h3 class="privilege-title">{{ privilege.title }}</h3>
                <p class="privilege-desc">{{ privilege.description }}</p>
                <div class="privilege-compare">
                  <div class="compare-item normal">
                    <span class="label">普通用户</span>
                    <span class="value">{{ privilege.normal }}</span>
                  </div>
                  <div class="compare-item vip">
                    <span class="label">VIP会员</span>
                    <span class="value">{{ privilege.vip }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 套餐选择 -->
        <div class="pricing-section">
          <h2 class="section-title">开通/续费会员</h2>
          <el-row :gutter="20" justify="center">
            <el-col :span="6" v-for="plan in plans" :key="plan.id">
              <div 
                class="pricing-card" 
                :class="{ active: selectedPlan === plan.id }"
                @click="selectPlan(plan.id)"
              >
                <div class="plan-name">{{ plan.name }}</div>
                <div class="plan-price">
                  <span class="currency">￥</span>
                  <span class="amount">{{ plan.price }}</span>
                </div>
                <div class="plan-days">{{ plan.days }}天</div>
                <div class="plan-tag" v-if="plan.tag">{{ plan.tag }}</div>
              </div>
            </el-col>
          </el-row>
          
          <div class="payment-action">
            <el-button type="warning" size="large" :loading="paying" @click="handlePay" round>
              {{ userStore.isVip ? '立即续费' : '立即开通' }}
            </el-button>
          </div>
        </div>
      </el-main>
    </el-container>

    <!-- 支付模拟对话框 -->
    <el-dialog v-model="payDialogVisible" title="扫码支付" width="400px" center>
      <div class="pay-content">
        <div class="pay-amount">￥{{ getSelectedPlanPrice }}</div>
        <div class="qrcode-placeholder">
          <el-icon :size="150" color="#409eff"><FullScreen /></el-icon>
          <div class="qrcode-tip">支付宝/微信扫码支付（模拟）</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPay" :loading="paying">支付完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { upgradeToVip } from '@/api/vip'
import { getUserInfo } from '@/api/user'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Trophy,
  Clock,
  Files,
  Upload,
  Download,
  Share,
  FullScreen,
  Lock
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = reactive({})

const paying = ref(false)
const payDialogVisible = ref(false)
const selectedPlan = ref(2) // 默认选择季卡

const privileges = [
  {
    title: '存储空间',
    icon: 'Files',
    color: '#409eff',
    normal: '5GB',
    vip: '50GB',
    description: '海量存储空间，轻松存储您的所有重要文件'
  },
  {
    title: '单文件大小',
    icon: 'Upload',
    color: '#e6a23c',
    normal: '500MB',
    vip: '2GB',
    description: '支持超大文件上传，满足各种场景需求'
  },
  {
    title: '分享有效期',
    icon: 'Share',
    color: '#f56c6c',
    normal: '最长7天',
    vip: '永久有效',
    description: '永久分享链接，无需担心链接过期'
  },
  {
    title: '下载速度',
    icon: 'Download',
    color: '#67c23a',
    normal: '标准速度',
    vip: '极速下载',
    description: 'VIP专享高速通道，下载不限速'
  },
  {
    title: '身份标识',
    icon: 'Trophy',
    color: '#ffd700',
    normal: '无',
    vip: '尊贵徽章',
    description: '专属VIP徽章，彰显尊贵身份'
  }
]

const plans = [
  { id: 1, name: '月卡', price: 10, days: 30, tag: '' },
  { id: 2, name: '季卡', price: 25, days: 90, tag: '推荐' },
  { id: 3, name: '年卡', price: 80, days: 365, tag: '超值' }
]

const getSelectedPlanPrice = computed(() => {
  const plan = plans.find(p => p.id === selectedPlan.value)
  return plan ? plan.price : 0
})

onMounted(() => {
  loadUserInfo()
})

const loadUserInfo = async () => {
  // 优先从 store 获取，但也重新请求最新状态
  Object.assign(userInfo, userStore.userInfo)
  try {
    const res = await getUserInfo()
    if (res.code === 200) {
      Object.assign(userInfo, res.data)
      userStore.updateUserInfo(res.data)
    }
  } catch (error) {
    console.error('获取用户信息失败', error)
  }
}

const goBack = () => {
  router.push('/home')
}

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  const date = new Date(datetime)
  return date.toLocaleDateString()
}

const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) {
    return avatar
  }
  return import.meta.env.VITE_API_BASE_URL + avatar
}

const selectPlan = (id) => {
  selectedPlan.value = id
}

const handlePay = () => {
  payDialogVisible.value = true
}

const confirmPay = async () => {
  paying.value = true
  const plan = plans.find(p => p.id === selectedPlan.value)
  
  try {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    await upgradeToVip(plan.days)
    ElMessage.success('开通成功！')
    payDialogVisible.value = false
    // 刷新用户信息
    await loadUserInfo()
  } catch (error) {
    ElMessage.error('支付失败: ' + (error.response?.data?.message || error.message))
  } finally {
    paying.value = false
  }
}
</script>

<style scoped>
.vip-center {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--bg-gray-50);
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  box-shadow: var(--shadow-sm);
  padding: 0 20px;
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
  gap: 15px;
}

.header-title {
  font-size: 20px;
  font-weight: bold;
  color: var(--text-primary);
}

.content {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.status-card {
  margin-bottom: 30px;
  background: linear-gradient(135deg, #2b323b 0%, #1e2329 100%);
  color: #fff;
  border: none;
  border-radius: var(--radius-large);
  box-shadow: var(--shadow-base);
  position: relative;
  overflow: hidden;
}

.status-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(251, 114, 153, 0.15) 0%, transparent 70%);
  border-radius: 50%;
}

.user-status {
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  z-index: 1;
}

.status-info {
  flex: 1;
}

.username-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.username {
  font-size: 24px;
  font-weight: bold;
}

.expire-time {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  gap: 5px;
}

.section-title {
  text-align: center;
  font-size: 24px;
  color: var(--text-primary);
  margin: 40px 0 30px;
  font-weight: 600;
  letter-spacing: 1px;
}

.privilege-card {
  height: 100%;
  text-align: center;
  transition: all 0.3s;
  border: none;
  background: #fff;
  border-radius: var(--radius-large);
  overflow: visible; /* 允许阴影溢出 */
}

.privilege-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-pink);
}

.privilege-icon {
  margin: 20px 0;
  display: inline-flex;
  padding: 16px;
  border-radius: 50%;
  background: var(--bg-gray-50);
  transition: all 0.3s;
}

.privilege-card:hover .privilege-icon {
  background: var(--primary-lighter);
  transform: scale(1.1);
}

.privilege-title {
  font-size: 18px;
  margin-bottom: 10px;
  color: var(--text-primary);
  font-weight: 600;
}

.privilege-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 15px;
  min-height: 36px;
  line-height: 1.6;
}

.privilege-compare {
  background: var(--bg-gray-50);
  border-radius: var(--radius-medium);
  padding: 15px;
}

.compare-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.compare-item.normal {
  color: var(--text-secondary);
  border-bottom: 1px dashed var(--bg-gray-200);
}

.compare-item.vip {
  color: var(--primary-color); /* 粉色强调 */
  font-weight: bold;
  margin-top: 8px;
}

.pricing-section {
  margin-top: 50px;
  padding-bottom: 50px;
}

.pricing-card {
  background: #fff;
  border: 2px solid transparent;
  border-radius: var(--radius-large);
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-base);
}

.pricing-card:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow-md);
  transform: translateY(-4px);
}

.pricing-card.active {
  border-color: var(--primary-color);
  background: var(--primary-lighter);
  transform: scale(1.05);
  box-shadow: var(--shadow-pink);
}

.plan-name {
  font-size: 18px;
  font-weight: bold;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.plan-price {
  margin: 20px 0;
  color: var(--primary-color); /* 主色 */
}

.currency {
  font-size: 20px;
  font-weight: bold;
}

.amount {
  font-size: 44px;
  font-weight: 800;
  font-family: 'DIN Alternate', sans-serif;
}

.plan-days {
  color: var(--text-secondary);
  font-size: 14px;
}

.plan-tag {
  position: absolute;
  top: -12px;
  right: -10px;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 99%, #fecfef 100%); /* 糖果色渐变 */
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px 20px 20px 2px;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

.payment-action {
  text-align: center;
  margin-top: 50px;
}

/* 支付弹窗样式 */
.pay-content {
  text-align: center;
  padding: 20px 0;
}

.pay-amount {
  font-size: 36px;
  font-weight: bold;
  color: var(--primary-color);
  margin-bottom: 20px;
}

.qrcode-placeholder {
  background: var(--bg-gray-50);
  padding: 30px;
  border-radius: var(--radius-large);
  display: inline-block;
  border: 1px solid var(--bg-gray-200);
}

.qrcode-tip {
  margin-top: 15px;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>