<template>
  <div class="admin-panel">
    <el-container class="admin-container">
      <el-header class="header">
        <div class="header-left">
          <div class="logo-box">
            <el-icon :size="24" color="#fff"><Setting /></el-icon>
          </div>
          <span class="header-title">系统管理后台</span>
        </div>
        <div class="header-right">
          <el-button @click="goBack" plain class="back-btn" :icon="ArrowLeft">返回前台</el-button>
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-avatar :size="32" :src="userStore.userInfo.avatar ? getAvatarUrl(userStore.userInfo.avatar) : ''" class="admin-avatar">
                {{ userStore.userInfo.username?.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo.username }}</span>
              <el-icon><CaretBottom /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided style="color: var(--danger-color)">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container class="main-body">
        <el-aside width="220px" class="sidebar">
          <el-menu :default-active="currentTab" @select="handleTabSelect" class="admin-menu">
            <el-menu-item index="statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>数据仪表盘</span>
            </el-menu-item>
            <el-menu-item index="users">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item index="files">
              <el-icon><Document /></el-icon>
              <span>文件管理</span>
            </el-menu-item>
            <el-menu-item index="shares">
              <el-icon><Share /></el-icon>
              <span>分享管理</span>
            </el-menu-item>
            <el-menu-item index="logs">
              <el-icon><List /></el-icon>
              <span>系统日志</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="content">
          <!-- 数据统计面板 -->
          <div v-show="currentTab === 'statistics'" class="statistics-panel fade-in">
            <div class="panel-header">
              <h2 class="panel-title">数据概览</h2>
              <span class="update-time">最后更新: {{ formatDateTime(new Date()) }}</span>
            </div>

            <el-row :gutter="20" style="margin-bottom: 24px;">
              <el-col :span="6">
                <el-card shadow="hover" class="stat-card blue-card">
                  <div class="stat-inner">
                    <div class="stat-info">
                      <div class="stat-label">总用户数</div>
                      <div class="stat-value">{{ statistics.totalUsers || 0 }}</div>
                    </div>
                    <div class="stat-icon-bg">
                      <el-icon><User /></el-icon>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="hover" class="stat-card green-card">
                  <div class="stat-inner">
                    <div class="stat-info">
                      <div class="stat-label">总文件数</div>
                      <div class="stat-value">{{ statistics.totalFiles || 0 }}</div>
                    </div>
                    <div class="stat-icon-bg">
                      <el-icon><Document /></el-icon>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="hover" class="stat-card orange-card">
                  <div class="stat-inner">
                    <div class="stat-info">
                      <div class="stat-label">总分享数</div>
                      <div class="stat-value">{{ statistics.totalShares || 0 }}</div>
                    </div>
                    <div class="stat-icon-bg">
                      <el-icon><Share /></el-icon>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="hover" class="stat-card red-card">
                  <div class="stat-inner">
                    <div class="stat-info">
                      <div class="stat-label">存储使用率</div>
                      <div class="stat-value">{{ statistics.storageUsageRate?.toFixed(1) || 0 }}%</div>
                    </div>
                    <div class="stat-icon-bg">
                      <el-icon><PieChart /></el-icon>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-card shadow="never" class="chart-card">
                  <template #header>
                    <div class="card-header">
                      <span>用户分布</span>
                      <el-tag size="small" effect="plain">今日新增: {{ statistics.todayNewUsers || 0 }}</el-tag>
                    </div>
                  </template>
                  <div class="user-dist-grid">
                     <div class="dist-item">
                        <div class="dist-label">普通用户</div>
                        <div class="dist-num">{{ statistics.normalUsers || 0 }}</div>
                        <el-progress :percentage="getUserPercentage(statistics.normalUsers)" :color="'#909399'" :show-text="false" />
                     </div>
                     <div class="dist-item">
                        <div class="dist-label">VIP会员</div>
                        <div class="dist-num">{{ statistics.vipUsers || 0 }}</div>
                        <el-progress :percentage="getUserPercentage(statistics.vipUsers)" :color="'#fb7299'" :show-text="false" />
                     </div>
                     <div class="dist-item">
                        <div class="dist-label">管理员</div>
                        <div class="dist-num">{{ statistics.adminUsers || 0 }}</div>
                        <el-progress :percentage="getUserPercentage(statistics.adminUsers)" :color="'#409eff'" :show-text="false" />
                     </div>
                  </div>
                </el-card>
              </el-col>

              <el-col :span="12">
                <el-card shadow="never" class="chart-card">
                  <template #header>
                    <div class="card-header">今日活动</div>
                  </template>
                  <div class="activity-list">
                    <div class="activity-item">
                      <div class="act-icon blue"><el-icon><Upload /></el-icon></div>
                      <div class="act-info">
                        <div class="act-title">新增文件</div>
                        <div class="act-num">{{ statistics.todayNewFiles || 0 }}</div>
                      </div>
                    </div>
                    <div class="activity-item">
                      <div class="act-icon green"><el-icon><Monitor /></el-icon></div>
                      <div class="act-info">
                        <div class="act-title">系统登录</div>
                        <div class="act-num">{{ statistics.todayLogins || 0 }}</div>
                      </div>
                    </div>
                    <div class="activity-item">
                      <div class="act-icon orange"><el-icon><Coin /></el-icon></div>
                      <div class="act-info">
                        <div class="act-title">存储增长</div>
                        <div class="act-num">{{ formatBytes(statistics.totalStorageUsed) }}</div>
                      </div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <!-- 数据趋势图表 -->
            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="24">
                <el-card shadow="never" class="chart-card">
                  <template #header>
                    <div class="card-header">
                      <span>趋势分析</span>
                      <div class="chart-tabs">
                        <!-- 这里可以添加切换按钮，目前先放着 -->
                      </div>
                    </div>
                  </template>
                  <el-row :gutter="20">
                    <el-col :span="8">
                      <div class="chart-title">用户增长</div>
                      <div ref="userGrowthChart" class="chart-container"></div>
                    </el-col>
                    <el-col :span="8">
                       <div class="chart-title">文件吞吐量</div>
                      <div ref="fileOperationChart" class="chart-container"></div>
                    </el-col>
                    <el-col :span="8">
                       <div class="chart-title">存储消耗</div>
                      <div ref="storageTrendChart" class="chart-container"></div>
                    </el-col>
                  </el-row>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 用户管理 -->
          <div v-show="currentTab === 'users'" class="users-panel fade-in">
            <div class="panel-header">
              <h2 class="panel-title">用户管理</h2>
            </div>

            <div class="table-container">
              <div class="filter-bar">
                <el-form :inline="true" :model="userQuery" class="search-form">
                  <el-form-item>
                    <el-input v-model="userQuery.keyword" placeholder="搜索用户名/邮箱" :prefix-icon="Search" clearable />
                  </el-form-item>
                  <el-form-item>
                    <el-select v-model="userQuery.userType" placeholder="用户类型" clearable style="width: 120px">
                      <el-option label="全部" :value="null" />
                      <el-option label="普通用户" :value="0" />
                      <el-option label="VIP用户" :value="1" />
                      <el-option label="管理员" :value="2" />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <el-select v-model="userQuery.status" placeholder="状态" clearable style="width: 100px">
                      <el-option label="全部" :value="null" />
                      <el-option label="正常" :value="1" />
                      <el-option label="禁用" :value="0" />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="loadUsers" :icon="Search">查询</el-button>
                    <el-button @click="resetUserQuery" :icon="Refresh">重置</el-button>
                  </el-form-item>
                </el-form>
              </div>

              <el-table :data="userList" v-loading="loadingUsers" stripe border class="custom-table">
                 <!-- 表格内容保持不变，仅样式类名变化 -->
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column prop="username" label="用户名" width="150" />
                <el-table-column prop="email" label="邮箱" width="200" />
                <el-table-column label="类型" width="120" align="center">
                  <template #default="{ row }">
                      <el-tag v-if="row.userType === 0" type="info" size="small" effect="plain">普通用户</el-tag>
                      <el-tag v-if="row.userType === 1" type="warning" size="small" effect="dark">VIP会员</el-tag>
                      <el-tag v-if="row.userType === 2" type="danger" size="small" effect="dark">管理员</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="VIP到期" width="160">
                  <template #default="{ row }">
                    <span v-if="row.vipExpireTime" class="date-text">
                      {{ formatDateTime(row.vipExpireTime) }}
                    </span>
                    <span v-else class="text-gray">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.status === 1" type="success" size="small" effect="light">正常</el-tag>
                    <el-tag v-else type="danger" size="small" effect="light">禁用</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="存储使用" width="180">
                  <template #default="{ row }">
                    <div class="storage-cell">
                      <span>{{ formatBytes(row.storageUsed) }} / {{ formatBytes(row.storageQuota) }}</span>
                      <el-progress 
                        :percentage="Math.min(100, Math.round((row.storageUsed / row.storageQuota) * 100))" 
                        :show-text="false"
                        :stroke-width="4"
                        :color="getStorageColor(row.storageUsed, row.storageQuota)"
                      />
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="注册时间" width="160">
                  <template #default="{ row }">
                    <span class="date-text">{{ formatDateTime(row.createdAt) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="250" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button-group>
                      <el-button size="small" :icon="Edit" @click="showEditUserDialog(row)" />
                      <el-button size="small" type="warning" :icon="Trophy" @click="showVipDialog(row)" />
                      <el-button
                        size="small"
                        :type="row.status === 1 ? 'danger' : 'success'"
                        :icon="row.status === 1 ? Lock : Unlock"
                        @click="handleToggleUserStatus(row)"
                        :disabled="row.userType === 2"
                      />
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="userQuery.pageNum"
                  v-model:page-size="userQuery.pageSize"
                  :total="userTotal"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                  @current-change="loadUsers"
                  @size-change="loadUsers"
                  background
                />
              </div>
            </div>
          </div>

          <!-- 文件管理 (Files Panel) -->
          <div v-show="currentTab === 'files'" class="files-panel fade-in">
             <!-- 保持原有逻辑，应用新的 table-container 和 filter-bar 样式 -->
             <div class="panel-header"><h2 class="panel-title">文件管理</h2></div>
             <div class="table-container">
               <div class="filter-bar">
                  <el-form :inline="true" :model="fileQuery">
                    <el-form-item label="用户ID">
                      <el-input v-model="fileQuery.userId" placeholder="输入用户ID" clearable />
                    </el-form-item>
                    <el-form-item label="文件类型">
                      <el-input v-model="fileQuery.fileType" placeholder="如 image/" clearable />
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="loadFiles" :icon="Search">查询</el-button>
                      <el-button @click="resetFileQuery" :icon="Refresh">重置</el-button>
                    </el-form-item>
                  </el-form>
               </div>
               <el-table :data="fileList" v-loading="loadingFiles" stripe border class="custom-table">
                  <el-table-column prop="id" label="ID" width="80" align="center" />
                  <el-table-column prop="username" label="所属用户" width="150" />
                  <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
                  <el-table-column label="大小" width="120">
                    <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
                  </el-table-column>
                  <el-table-column prop="fileType" label="类型" width="150" />
                  <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
                      <el-tag v-else type="danger" size="small">已删除</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="创建时间" width="180">
                    <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="100" fixed="right" align="center">
                    <template #default="{ row }">
                      <el-popconfirm title="确定删除此文件吗？" @confirm="handleDeleteFile(row)">
                        <template #reference>
                          <el-button size="small" type="danger" :icon="Delete" circle :disabled="row.status === 0" />
                        </template>
                      </el-popconfirm>
                    </template>
                  </el-table-column>
               </el-table>
               <div class="pagination-container">
                  <el-pagination
                    v-model:current-page="fileQuery.pageNum"
                    v-model:page-size="fileQuery.pageSize"
                    :total="fileTotal"
                    @current-change="loadFiles"
                    @size-change="loadFiles"
                    background
                    layout="total, prev, pager, next"
                  />
               </div>
             </div>
          </div>

          <!-- 分享管理 (Shares Panel) -->
          <div v-show="currentTab === 'shares'" class="shares-panel fade-in">
             <div class="panel-header"><h2 class="panel-title">分享管理</h2></div>
             <div class="table-container">
               <div class="filter-bar">
                  <el-form :inline="true" :model="shareQuery">
                    <el-form-item label="用户ID"><el-input v-model="shareQuery.userId" clearable /></el-form-item>
                    <el-form-item label="状态">
                       <el-select v-model="shareQuery.status" clearable style="width: 100px">
                          <el-option label="全部" :value="null" />
                          <el-option label="正常" :value="1" />
                          <el-option label="已取消" :value="0" />
                       </el-select>
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="loadShares" :icon="Search">查询</el-button>
                    </el-form-item>
                  </el-form>
               </div>
               <el-table :data="shareList" v-loading="loadingShares" stripe border class="custom-table">
                  <el-table-column prop="id" label="ID" width="80" align="center" />
                  <el-table-column prop="username" label="分享者" width="120" />
                  <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="shareCode" label="分享码" width="100" align="center">
                     <template #default="{ row }"><el-tag type="info" size="small" effect="plain">{{ row.shareCode }}</el-tag></template>
                  </el-table-column>
                  <el-table-column label="浏览/下载" width="120" align="center">
                    <template #default="{ row }">{{ row.viewCount }} / {{ row.downloadCount }}</template>
                  </el-table-column>
                  <el-table-column label="有效期" width="160">
                    <template #default="{ row }">{{ row.expireTime ? formatDateTime(row.expireTime) : '永久有效' }}</template>
                  </el-table-column>
                  <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.status === 1" type="success" size="small">正常</el-tag>
                      <el-tag v-else type="info" size="small">已失效</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="100" fixed="right" align="center">
                    <template #default="{ row }">
                       <el-button size="small" type="danger" link @click="handleCancelShare(row)" :disabled="row.status === 0">取消分享</el-button>
                    </template>
                  </el-table-column>
               </el-table>
               <div class="pagination-container">
                  <el-pagination
                    v-model:current-page="shareQuery.pageNum"
                    v-model:page-size="shareQuery.pageSize"
                    :total="shareTotal"
                    @current-change="loadShares"
                    background
                    layout="total, prev, pager, next"
                  />
               </div>
             </div>
          </div>

          <!-- 登录日志 (Logs Panel) -->
          <div v-show="currentTab === 'logs'" class="logs-panel fade-in">
             <div class="panel-header"><h2 class="panel-title">系统日志</h2></div>
             <div class="table-container">
                <div class="filter-bar">
                   <el-form :inline="true" :model="logQuery">
                      <el-form-item label="用户ID"><el-input v-model="logQuery.userId" clearable /></el-form-item>
                      <el-form-item label="状态">
                         <el-select v-model="logQuery.status" clearable style="width: 100px">
                            <el-option label="全部" :value="null" />
                            <el-option label="成功" :value="1" />
                            <el-option label="失败" :value="0" />
                         </el-select>
                      </el-form-item>
                      <el-form-item>
                        <el-button type="primary" @click="loadLogs" :icon="Search">查询</el-button>
                      </el-form-item>
                   </el-form>
                </div>
                <el-table :data="logList" v-loading="loadingLogs" stripe border class="custom-table">
                   <el-table-column prop="id" label="ID" width="80" align="center" />
                   <el-table-column prop="loginIp" label="IP地址" width="140" />
                   <el-table-column prop="loginLocation" label="地理位置" width="140" />
                   <el-table-column prop="loginDevice" label="设备信息" min-width="200" show-overflow-tooltip />
                   <el-table-column label="状态" width="100" align="center">
                      <template #default="{ row }">
                         <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失败' }}</el-tag>
                      </template>
                   </el-table-column>
                   <el-table-column label="时间" width="180">
                      <template #default="{ row }">{{ formatDateTime(row.loginTime) }}</template>
                   </el-table-column>
                </el-table>
                <div class="pagination-container">
                   <el-pagination
                     v-model:current-page="logQuery.pageNum"
                     v-model:page-size="logQuery.pageSize"
                     :total="logTotal"
                     @current-change="loadLogs"
                     background
                     layout="total, prev, pager, next"
                   />
                </div>
             </div>
          </div>

        </el-main>
      </el-container>
    </el-container>

    <!-- 弹窗保持不变，样式会自动继承 -->
    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editUserVisible" title="编辑用户" width="500px" destroy-on-close>
      <el-form :model="editUserForm" :rules="editUserRules" ref="editUserFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editUserForm.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editUserForm.email" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="editUserForm.userType" style="width: 100%;">
            <el-option label="普通用户" :value="0" />
            <el-option label="VIP用户" :value="1" />
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editUserVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditUser" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- VIP操作对话框 -->
    <el-dialog v-model="vipDialogVisible" title="VIP操作" width="400px">
      <el-form :model="vipForm" label-width="100px">
        <el-form-item label="用户">
          <el-input :value="vipForm.username" disabled />
        </el-form-item>
        <el-form-item label="开通天数">
          <el-input-number v-model="vipForm.days" :min="1" :max="3650" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleOperateVip" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Setting,
  User,
  DataAnalysis,
  Document,
  Share,
  List,
  PieChart,
  Search,
  Refresh,
  ArrowLeft,
  CaretBottom,
  Upload,
  Monitor,
  Coin,
  Edit,
  Trophy,
  Lock,
  Unlock,
  Delete
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  getStatistics,
  getUserList,
  editUser,
  toggleUserStatus,
  adminOperateVip,
  getFileList,
  deleteFile,
  getShareList,
  cancelShare,
  getLoginLogs,
  getUserGrowthTrend,
  getFileOperationTrend,
  getStorageTrend
} from '@/api/admin'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()

// 获取头像URL
const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) {
    return avatar
  }
  return import.meta.env.VITE_API_BASE_URL + avatar
}

const currentTab = ref('statistics')
const loadingUsers = ref(false)
const loadingFiles = ref(false)
const loadingShares = ref(false)
const loadingLogs = ref(false)
const saving = ref(false)

// 统计数据
const statistics = reactive({})

// 计算用户比例
const getUserPercentage = (count) => {
  if (!statistics.totalUsers) return 0
  return Math.round((count / statistics.totalUsers) * 100)
}

// 存储进度条颜色
const getStorageColor = (used, quota) => {
  if (!quota) return '#909399'
  const percentage = (used / quota) * 100
  if (percentage < 60) return '#67c23a'
  if (percentage < 85) return '#e6a23c'
  return '#f56c6c'
}

// 图表ref
const userGrowthChart = ref(null)
const fileOperationChart = ref(null)
const storageTrendChart = ref(null)

// 图表实例
let userGrowthChartInstance = null
let fileOperationChartInstance = null
let storageTrendChartInstance = null

// 用户管理
const userQuery = reactive({
  keyword: '',
  userType: null,
  status: null,
  pageNum: 1,
  pageSize: 20
})
const userList = ref([])
const userTotal = ref(0)

// 文件管理
const fileQuery = reactive({
  userId: null,
  fileType: '',
  pageNum: 1,
  pageSize: 20
})
const fileList = ref([])
const fileTotal = ref(0)

// 分享管理
const shareQuery = reactive({
  userId: null,
  status: null,
  pageNum: 1,
  pageSize: 20
})
const shareList = ref([])
const shareTotal = ref(0)

// 登录日志
const logQuery = reactive({
  userId: null,
  status: null,
  pageNum: 1,
  pageSize: 50
})
const logList = ref([])
const logTotal = ref(0)

// 编辑用户
const editUserVisible = ref(false)
const editUserForm = reactive({
  userId: null,
  username: '',
  email: '',
  userType: 0
})
const editUserFormRef = ref()
const editUserRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// VIP操作
const vipDialogVisible = ref(false)
const vipForm = reactive({
  userId: null,
  username: '',
  days: 30
})

onMounted(() => {
  loadStatistics()
})

// 初始化用户增长趋势图
const initUserGrowthChart = (data) => {
  nextTick(() => {
    if (!userGrowthChart.value) return

    if (userGrowthChartInstance) {
      userGrowthChartInstance.dispose()
    }

    userGrowthChartInstance = echarts.init(userGrowthChart.value)

    const option = {
      title: {
        text: '最近30天用户增长趋势'
      },
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['新增用户', '累计用户']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map(item => item.date)
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '新增用户',
          type: 'line',
          data: data.map(item => item.newUsers),
          smooth: true,
          itemStyle: {
            color: '#409eff'
          }
        },
        {
          name: '累计用户',
          type: 'line',
          data: data.map(item => item.totalUsers),
          smooth: true,
          itemStyle: {
            color: '#67c23a'
          }
        }
      ]
    }

    userGrowthChartInstance.setOption(option)
  })
}

// 初始化文件操作量图表
const initFileOperationChart = (data) => {
  nextTick(() => {
    if (!fileOperationChart.value) return

    if (fileOperationChartInstance) {
      fileOperationChartInstance.dispose()
    }

    fileOperationChartInstance = echarts.init(fileOperationChart.value)

    const option = {
      title: {
        text: '最近30天文件上传下载量'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: ['上传量', '下载量']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: data.map(item => item.date)
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '上传量',
          type: 'bar',
          data: data.map(item => item.uploadCount),
          itemStyle: {
            color: '#409eff'
          }
        },
        {
          name: '下载量',
          type: 'bar',
          data: data.map(item => item.downloadCount),
          itemStyle: {
            color: '#e6a23c'
          }
        }
      ]
    }

    fileOperationChartInstance.setOption(option)
  })
}

// 初始化存储空间使用趋势图
const initStorageTrendChart = (data) => {
  nextTick(() => {
    if (!storageTrendChart.value) return

    if (storageTrendChartInstance) {
      storageTrendChartInstance.dispose()
    }

    storageTrendChartInstance = echarts.init(storageTrendChart.value)

    const option = {
      title: {
        text: '最近30天存储空间使用趋势'
      },
      tooltip: {
        trigger: 'axis',
        formatter: function(params) {
          let result = params[0].name + '<br/>'
          params.forEach(param => {
            const value = (param.value / (1024 * 1024 * 1024)).toFixed(2)
            result += param.marker + param.seriesName + ': ' + value + ' GB<br/>'
          })
          return result
        }
      },
      legend: {
        data: ['已使用', '总配额']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map(item => item.date)
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: function(value) {
            return (value / (1024 * 1024 * 1024)).toFixed(0) + ' GB'
          }
        }
      },
      series: [
        {
          name: '已使用',
          type: 'line',
          data: data.map(item => item.storageUsed),
          smooth: true,
          areaStyle: {},
          itemStyle: {
            color: '#f56c6c'
          }
        },
        {
          name: '总配额',
          type: 'line',
          data: data.map(item => item.storageQuota),
          smooth: true,
          itemStyle: {
            color: '#67c23a'
          }
        }
      ]
    }

    storageTrendChartInstance.setOption(option)
  })
}

// 加载图表数据
const loadCharts = async () => {
  try {
    // 加载用户增长趋势
    const userGrowthRes = await getUserGrowthTrend()
    if (userGrowthRes.code === 200) {
      initUserGrowthChart(userGrowthRes.data)
    }

    // 加载文件操作量趋势
    const fileOpRes = await getFileOperationTrend()
    if (fileOpRes.code === 200) {
      initFileOperationChart(fileOpRes.data)
    }

    // 加载存储空间使用趋势
    const storageTrendRes = await getStorageTrend()
    if (storageTrendRes.code === 200) {
      initStorageTrendChart(storageTrendRes.data)
    }
  } catch (error) {
    console.error('加载图表数据失败:', error)
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getStatistics()
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
    // 加载图表数据
    await loadCharts()
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

// Tab切换
const handleTabSelect = (key) => {
  currentTab.value = key
  if (key === 'statistics') {
    loadStatistics()
  } else if (key === 'users') {
    loadUsers()
  } else if (key === 'files') {
    loadFiles()
  } else if (key === 'shares') {
    loadShares()
  } else if (key === 'logs') {
    loadLogs()
  }
}

// 加载用户列表
const loadUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await getUserList(userQuery)
    if (res.code === 200) {
      userList.value = res.data.records
      userTotal.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

// 重置用户查询
const resetUserQuery = () => {
  userQuery.keyword = ''
  userQuery.userType = null
  userQuery.status = null
  userQuery.pageNum = 1
  loadUsers()
}

// 显示编辑用户对话框
const showEditUserDialog = (row) => {
  editUserForm.userId = row.id
  editUserForm.username = row.username
  editUserForm.email = row.email
  editUserForm.userType = row.userType
  editUserVisible.value = true
}

// 编辑用户
const handleEditUser = async () => {
  await editUserFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        await editUser(editUserForm)
        ElMessage.success('编辑成功')
        editUserVisible.value = false
        loadUsers()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '编辑失败')
      } finally {
        saving.value = false
      }
    }
  })
}

// 切换用户状态
const handleToggleUserStatus = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.status === 1 ? '禁用' : '启用'}该用户吗？`,
      '提示',
      { type: 'warning' }
    )
    await toggleUserStatus(row.id)
    ElMessage.success('操作成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

// 显示VIP操作对话框
const showVipDialog = (row) => {
  vipForm.userId = row.id
  vipForm.username = row.username
  vipForm.days = 30
  vipDialogVisible.value = true
}

// VIP操作
const handleOperateVip = async () => {
  saving.value = true
  try {
    await adminOperateVip({
      userId: vipForm.userId,
      days: vipForm.days
    })
    ElMessage.success('VIP操作成功')
    vipDialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

// 加载文件列表
const loadFiles = async () => {
  loadingFiles.value = true
  try {
    const res = await getFileList(fileQuery)
    if (res.code === 200) {
      fileList.value = res.data.records
      fileTotal.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载文件列表失败')
  } finally {
    loadingFiles.value = false
  }
}

// 重置文件查询
const resetFileQuery = () => {
  fileQuery.userId = null
  fileQuery.fileType = ''
  fileQuery.pageNum = 1
  loadFiles()
}

// 删除文件
const handleDeleteFile = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件"${row.fileName}"吗？此操作不可恢复！`,
      '警告',
      { type: 'warning' }
    )
    await deleteFile(row.id)
    ElMessage.success('删除成功')
    loadFiles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 加载分享列表
const loadShares = async () => {
  loadingShares.value = true
  try {
    const res = await getShareList(shareQuery)
    if (res.code === 200) {
      shareList.value = res.data.records
      shareTotal.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载分享列表失败')
  } finally {
    loadingShares.value = false
  }
}

// 重置分享查询
const resetShareQuery = () => {
  shareQuery.userId = null
  shareQuery.status = null
  shareQuery.pageNum = 1
  loadShares()
}

// 取消分享
const handleCancelShare = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消分享码"${row.shareCode}"吗？`,
      '提示',
      { type: 'warning' }
    )
    await cancelShare(row.id)
    ElMessage.success('取消成功')
    loadShares()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '取消失败')
    }
  }
}

// 加载登录日志
const loadLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await getLoginLogs(logQuery)
    if (res.code === 200) {
      logList.value = res.data.records
      logTotal.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载登录日志失败')
  } finally {
    loadingLogs.value = false
  }
}

// 重置日志查询
const resetLogQuery = () => {
  logQuery.userId = null
  logQuery.status = null
  logQuery.pageNum = 1
  loadLogs()
}

// 格式化字节
const formatBytes = (bytes) => {
  if (bytes === 0 || bytes === null || bytes === undefined) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化日期时间
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

// 返回首页
const goBack = () => {
  router.push('/home')
}

// 顶部菜单
const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-panel {
  min-height: 100vh;
  background-color: var(--bg-gray-50);
}

.admin-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.header {
  background: var(--bg-white);
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-sm);
  padding: 0 24px;
  height: 64px;
  position: relative;
  z-index: 10;
  border-bottom: 1px solid var(--bg-gray-200);
}

.header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--primary-color), var(--accent-blue));
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-box {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-pink);
}

.header-title {
  font-size: 20px;
  font-weight: bold;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  border: none;
  color: var(--text-secondary);
}
.back-btn:hover {
  background: var(--bg-gray-100);
  color: var(--primary-color);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: var(--radius-round);
  transition: all 0.3s;
  background: var(--bg-gray-100);
}

.user-dropdown:hover {
  background: var(--primary-lighter);
  color: var(--primary-color);
}

.username {
  font-weight: 500;
  font-size: 14px;
}

/* 侧边栏 */
.main-body {
  flex: 1;
  overflow: hidden;
}

.sidebar {
  background: var(--bg-white);
  border-right: 1px solid var(--bg-gray-200);
  display: flex;
  flex-direction: column;
}

.admin-menu {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  margin: 4px 12px;
  border-radius: var(--radius-medium);
  height: 50px;
  line-height: 50px;
  color: var(--text-secondary);
  border-left: 3px solid transparent;
}

:deep(.el-menu-item:hover) {
  background-color: var(--primary-lighter);
  color: var(--primary-color);
}

:deep(.el-menu-item.is-active) {
  background-color: var(--primary-lighter);
  color: var(--primary-color);
  font-weight: 600;
  border-left: 3px solid var(--primary-color);
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

/* 内容区域 */
.content {
  padding: 24px;
  background: var(--bg-gray-50);
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.panel-title {
  font-size: 24px;
  font-weight: bold;
  color: var(--text-primary);
  margin: 0;
}

.update-time {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* 统计卡片 */
.stat-card {
  border: none;
  border-radius: var(--radius-large);
  overflow: hidden;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}

.stat-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}

.stat-info {
  z-index: 2;
}

.stat-label {
  font-size: 13px;
  color: rgba(255,255,255,0.9);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  font-family: 'DIN Alternate', sans-serif;
}

.stat-icon-bg {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  backdrop-filter: blur(5px);
}

/* 卡片颜色主题 */
.blue-card { background: linear-gradient(135deg, #409eff, #79bbff); }
.green-card { background: linear-gradient(135deg, #67c23a, #95d475); }
.orange-card { background: linear-gradient(135deg, #e6a23c, #f3d19e); }
.red-card { background: linear-gradient(135deg, #f56c6c, #fab6b6); }

/* 图表卡片 */
.chart-card {
  border: none;
  border-radius: var(--radius-large);
  box-shadow: var(--shadow-base);
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
}

.chart-title {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.chart-container {
  width: 100%;
  height: 250px;
}

/* 用户分布 */
.user-dist-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 10px 0;
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dist-label {
  width: 80px;
  font-size: 14px;
  color: var(--text-secondary);
}

.dist-num {
  width: 50px;
  font-weight: bold;
  text-align: right;
}

:deep(.el-progress) {
  flex: 1;
}

/* 今日活动列表 */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: var(--bg-gray-50);
  border-radius: var(--radius-medium);
  transition: transform 0.2s;
}

.activity-item:hover {
  transform: translateX(5px);
  background: var(--bg-gray-100);
}

.act-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}
.act-icon.blue { background: var(--primary-light); color: var(--primary-color); }
.act-icon.green { background: #d1edc4; color: #67c23a; }
.act-icon.orange { background: #f8e3c5; color: #e6a23c; }

.act-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.act-title { font-size: 14px; color: var(--text-primary); }
.act-num { font-size: 18px; font-weight: bold; color: var(--text-primary); }

/* 表格容器通用样式 */
.table-container {
  background: var(--bg-white);
  padding: 24px;
  border-radius: var(--radius-large);
  box-shadow: var(--shadow-base);
}

.filter-bar {
  margin-bottom: 20px;
  background: var(--bg-gray-50);
  padding: 16px;
  border-radius: var(--radius-medium);
}

.search-form .el-form-item {
  margin-bottom: 0;
  margin-right: 12px;
}

/* 表格美化 */
.custom-table {
  border-radius: var(--radius-medium);
  overflow: hidden;
}

:deep(.el-table th.el-table__cell) {
  background-color: var(--bg-gray-50);
  color: var(--text-primary);
  font-weight: 600;
  height: 50px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 动画 */
.fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.text-gray { color: var(--text-tertiary); }
.storage-cell { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.date-text { font-size: 13px; color: var(--text-secondary); }
</style>
