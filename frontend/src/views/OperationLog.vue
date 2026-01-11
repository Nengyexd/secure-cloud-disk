<template>
  <div class="operation-log-page">
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
          <span class="header-title">操作日志</span>
        </div>
      </el-header>

      <el-main class="content">
        <el-card>
          <el-form :inline="true" :model="queryForm" class="search-form">
            <el-form-item label="操作类型">
              <el-select v-model="queryForm.operationType" placeholder="全部" clearable>
                <el-option label="全部" :value="null" />
                <el-option label="上传" value="UPLOAD" />
                <el-option label="下载" value="DOWNLOAD" />
                <el-option label="删除" value="DELETE" />
                <el-option label="恢复" value="RESTORE" />
                <el-option label="永久删除" value="PERMANENT_DELETE" />
                <el-option label="分享" value="SHARE" />
                <el-option label="取消分享" value="CANCEL_SHARE" />
                <el-option label="重命名" value="RENAME" />
                <el-option label="移动" value="MOVE" />
                <el-option label="创建文件夹" value="CREATE_FOLDER" />
                <el-option label="预览" value="PREVIEW" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadLogs" :icon="Search">查询</el-button>
              <el-button @click="resetQuery" :icon="Refresh">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="logList" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="operationTypeDesc" label="操作类型" width="120" />
            <el-table-column prop="objectTypeDesc" label="对象类型" width="100" />
            <el-table-column prop="objectName" label="对象名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="description" label="操作描述" min-width="150" show-overflow-tooltip />
            <el-table-column prop="ipAddress" label="IP地址" width="140" />
            <el-table-column label="操作时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.operationTime) }}
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="queryForm.pageNum"
            v-model:page-size="queryForm.pageSize"
            :total="total"
            :page-sizes="[20, 50, 100, 200]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadLogs"
            @size-change="loadLogs"
            style="margin-top: 20px; justify-content: center;"
          />
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Search, Refresh } from '@element-plus/icons-vue'
import { getMyOperationLogs } from '@/api/operationLog'

const router = useRouter()
const loading = ref(false)
const logList = ref([])
const total = ref(0)

const queryForm = reactive({
  operationType: null,
  pageNum: 1,
  pageSize: 20
})

onMounted(() => {
  loadLogs()
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getMyOperationLogs(queryForm)
    if (res.code === 200) {
      logList.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载操作日志失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.operationType = null
  queryForm.pageNum = 1
  loadLogs()
}

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  const date = new Date(datetime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.operation-log-page {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  background: #f0f2f5;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.content {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}
</style>
