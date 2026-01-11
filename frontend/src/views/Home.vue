<template>
  <div class="file-manager">
    <el-container class="outer-container">
      <el-header class="header">
        <div class="header-left">
          <el-icon :size="30" color="#409eff"><Folder /></el-icon>
          <span class="header-title">吉亦云盘</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              {{ userStore.userInfo.username }}
              <el-tag
                v-if="userStore.isVip"
                type="warning"
                size="small"
                style="margin-left: 10px"
                >VIP</el-tag
              >
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="vip">
                  <el-icon><Trophy /></el-icon>
                  会员中心
                </el-dropdown-item>
                <el-dropdown-item command="operation-log">
                  <el-icon><Document /></el-icon>
                  操作日志
                </el-dropdown-item>
                <el-dropdown-item command="admin" v-if="userStore.userInfo.userType === 2">
                  <el-icon><Setting /></el-icon>
                  管理员后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container class="main-container">
        <el-aside width="200px" class="sidebar">
          <el-menu default-active="all-files" @select="handleMenuSelect">
            <el-menu-item index="all-files">
              <el-icon><Folder /></el-icon>
              <span>全部文件</span>
            </el-menu-item>
            <el-menu-item index="image">
              <el-icon><Picture /></el-icon>
              <span>图片</span>
            </el-menu-item>
            <el-menu-item index="video">
              <el-icon><VideoCamera /></el-icon>
              <span>视频</span>
            </el-menu-item>
            <el-menu-item index="audio">
              <el-icon><Headset /></el-icon>
              <span>音频</span>
            </el-menu-item>
            <el-menu-item index="doc">
              <el-icon><DocumentIcon /></el-icon>
              <span>文档</span>
            </el-menu-item>
            <el-menu-item index="upload">
              <el-icon><UploadFilled /></el-icon>
              <span>上传文件</span>
            </el-menu-item>
            <el-menu-item index="favorites">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="recent-uploads">
              <el-icon><Document /></el-icon>
              <span>最近上传</span>
            </el-menu-item>
            <el-menu-item index="my-shares">
              <el-icon><Share /></el-icon>
              <span>我的分享</span>
            </el-menu-item>
            <el-menu-item index="recycle-bin">
              <el-icon><Delete /></el-icon>
              <span>回收站</span>
            </el-menu-item>
          </el-menu>

          <div class="storage-info">
            <div class="storage-title">存储空间</div>
            <el-progress
              :percentage="parseFloat(userStore.storageUsageRate)"
              :color="getProgressColor"
              :stroke-width="8"
            />
            <div class="storage-text">
              {{ formatBytes(userStore.userInfo.storageUsed) }} /
              {{ formatBytes(userStore.userInfo.storageQuota) }}
            </div>
          </div>
        </el-aside>

        <el-main class="content">
          <!-- VIP状态提醒栏 -->
          <el-alert
            v-if="userStore.isVip && vipDaysRemaining !== null"
            :title="vipAlertTitle"
            :type="vipAlertType"
            :closable="false"
            show-icon
            class="vip-alert"
          >
            <template #default>
              <span>{{ vipAlertMessage }}</span>
              <el-button
                v-if="vipDaysRemaining <= 7"
                type="text"
                @click="goToVip"
                style="margin-left: 10px; color: inherit; font-weight: bold;"
              >
                立即续费 →
              </el-button>
            </template>
          </el-alert>

          <div
            v-if="
              currentView === 'all-files' ||
              ['image', 'video', 'audio', 'doc'].includes(currentView)
            "
            class="file-list-container"
            @drop.prevent="handleFileDrop"
            @dragover.prevent="handleFileDragOver"
            @dragleave.prevent="handleFileDragLeave"
          >
            <!-- 拖拽上传提示层 -->
            <div v-if="isDraggingFile" class="drag-upload-overlay">
              <div class="drag-upload-content">
                <el-icon :size="80" color="#409eff"><UploadFilled /></el-icon>
                <p>拖拽文件到此处上传</p>
              </div>
            </div>
            
            <div class="breadcrumb-bar">
              <div class="breadcrumb-wrapper" v-if="currentView === 'all-files'">
                <el-button
                  class="back-btn"
                  :disabled="breadcrumb.length <= 1"
                  :icon="ArrowLeft"
                  @click="handleGoBack"
                  link
                  type="primary"
                >
                  返回上一级
                </el-button>
                <el-divider direction="vertical" class="breadcrumb-divider" />
                <el-breadcrumb separator="/" class="breadcrumb">
                  <el-breadcrumb-item
                    v-for="item in breadcrumb"
                    :key="item.id"
                    @click="navigateToFolder(item.id)"
                    style="cursor: pointer"
                  >
                    {{ item.name }}
                  </el-breadcrumb-item>
                </el-breadcrumb>
              </div>
              <div v-else class="category-header">
                <h2 style="margin: 0; font-weight: normal; color: #303133">
                  {{ getCategoryTitle(currentView) }}
                </h2>
              </div>

              <!-- 视图切换按钮 -->
              <div class="view-toggle">
                <el-tooltip content="列表视图" placement="bottom">
                  <el-button 
                    :type="viewMode === 'list' ? 'primary' : 'default'" 
                    :icon="Menu" 
                    circle 
                    size="small"
                    @click="viewMode = 'list'"
                    plain
                  />
                </el-tooltip>
                <el-tooltip content="网格视图" placement="bottom">
                  <el-button 
                    :type="viewMode === 'grid' ? 'primary' : 'default'" 
                    :icon="Grid" 
                    circle 
                    size="small"
                    @click="viewMode = 'grid'"
                    plain
                  />
                </el-tooltip>
              </div>
            </div>

            <div class="toolbar">
              <div class="toolbar-left">
                <el-button
                  v-if="currentView === 'all-files'"
                  type="primary"
                  @click="showCreateFolderDialog"
                  :icon="FolderAdd"
                  class="btn-primary"
                  >新建文件夹</el-button
                >
                <el-button
                  v-if="currentView === 'all-files'"
                  @click="showUploadDialog"
                  :icon="UploadFilled"
                  class="btn-default"
                  >上传文件</el-button
                >
                <el-button
                  v-if="selectedFiles.length > 0"
                  type="danger"
                  @click="handleBatchDelete"
                  :icon="Delete"
                  plain
                >
                  删除 ({{ selectedFiles.length }})
                </el-button>
                <el-button
                  v-if="selectedFiles.length > 0"
                  type="primary"
                  @click="handleBatchDownload"
                  :icon="Download"
                  plain
                >
                  下载 ({{ selectedFiles.length }})
                </el-button>
              </div>

              <div class="toolbar-right">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索文件"
                  :prefix-icon="Search"
                  @input="handleSearch"
                  clearable
                  class="search-input"
                />
              </div>
            </div>

            <!-- 列表视图 -->
            <el-table
              v-if="viewMode === 'list'"
              :data="fileList"
              @selection-change="handleSelectionChange"
              @sort-change="handleSortChange"
              @row-contextmenu="handleRowContextMenu"
              v-loading="loading"
              style="width: 100%"
              row-class-name="file-table-row"
              :header-cell-style="{ background: 'var(--bg-gray-50)', color: 'var(--text-secondary)' }"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column label="文件名" min-width="300" prop="fileName" sortable="custom">
                <template #default="{ row }">
                  <div
                    class="file-name-cell"
                    :class="{
                      'drag-over':
                        row.isFolder === 1 && dragOverFolder === row.id,
                      dragging: draggedFile && draggedFile.id === row.id,
                    }"
                    draggable="true"
                    @dragstart="handleDragStart($event, row)"
                    @dragover="handleDragOver($event, row)"
                    @dragleave="handleDragLeave"
                    @drop="handleDrop($event, row)"
                    @dragend="handleDragEnd"
                    @dblclick="handleFileDoubleClick(row)"
                  >
                    <!-- 有缩略图的图片文件显示缩略图 -->
                    <img
                      v-if="row.thumbnailKey && getThumbnailUrl(row)"
                      :src="getThumbnailUrl(row)"
                      class="file-thumbnail"
                      :alt="row.fileName"
                    />
                    <!-- 使用新的彩色图标组件 -->
                    <ColorfulFileIcons
                      v-else
                      :is-folder="row.isFolder"
                      :file-type="row.fileType"
                      :size="36"
                      style="margin-right: 12px; flex-shrink: 0;"
                    />
                    <span class="file-name">{{ row.fileName }}</span>
                    
                    <!-- 收藏星标 -->
                    <el-icon v-if="row.isFavorite" class="favorite-icon" color="#f7ba2a"><StarFilled /></el-icon>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="大小" width="120" prop="fileSize" sortable="custom">
                <template #default="{ row }">
                  <span class="text-secondary">{{ row.isFolder ? "-" : formatBytes(row.fileSize) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="修改时间" width="180" prop="updatedAt" sortable="custom">
                <template #default="{ row }">
                  <span class="text-secondary">{{ formatDateTime(row.updatedAt) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" fixed="right">
                <template #default="{ row }">
                  <div class="action-buttons">
                    <el-tooltip content="预览" placement="top" :show-after="500">
                      <el-button v-if="!row.isFolder" link type="primary" :icon="View" @click="handlePreview(row)" />
                    </el-tooltip>
                    <el-tooltip content="下载" placement="top" :show-after="500">
                      <el-button v-if="!row.isFolder" link type="primary" :icon="Download" @click="handleDownload(row)" />
                    </el-tooltip>
                    <el-tooltip :content="row.isFavorite ? '取消收藏' : '收藏'" placement="top" :show-after="500">
                      <el-button v-if="!row.isFolder" link :type="row.isFavorite ? 'warning' : 'info'" :icon="row.isFavorite ? StarFilled : Star" @click="handleFavorite(row)" />
                    </el-tooltip>
                    <el-dropdown trigger="click" @command="(cmd) => handleMoreCommand(cmd, row)">
                      <el-button link type="info" :icon="MoreFilled" />
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="share" :icon="Share">分享</el-dropdown-item>
                          <el-dropdown-item command="rename" :icon="Edit">重命名</el-dropdown-item>
                          <el-dropdown-item command="move" :icon="FolderOpened">移动</el-dropdown-item>
                          <el-dropdown-item command="delete" :icon="Delete" divided style="color: var(--danger-color)">删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <!-- 网格视图 -->
            <div v-else class="grid-view-container" v-loading="loading">
              <el-empty v-if="fileList.length === 0" description="暂无文件" />
              <div 
                v-for="file in fileList" 
                :key="file.id" 
                class="grid-item"
                :class="{ 
                  'is-selected': selectedFiles.some(f => f.id === file.id),
                  'is-folder': file.isFolder
                }"
                @click="toggleSelection(file)"
                @dblclick="handleFileDoubleClick(file)"
                @contextmenu.prevent="handleRowContextMenu(file, null, $event)"
              >
                <div class="grid-icon">
                  <img
                    v-if="file.thumbnailKey && getThumbnailUrl(file)"
                    :src="getThumbnailUrl(file)"
                    class="grid-thumbnail"
                    :alt="file.fileName"
                  />
                  <ColorfulFileIcons
                    v-else
                    :is-folder="file.isFolder"
                    :file-type="file.fileType"
                    :size="64"
                  />
                </div>
                <div class="grid-name" :title="file.fileName">{{ file.fileName }}</div>
                <div class="grid-check">
                  <el-checkbox 
                    :model-value="selectedFiles.some(f => f.id === file.id)" 
                    @change="(val) => toggleSelection(file, val)"
                    @click.stop
                  />
                </div>
              </div>
            </div>

            <el-pagination
              v-if="total > 50"
              class="pagination"
              :current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="handlePageChange"
            />

            <!-- 右键菜单 -->
            <div
              v-if="contextMenuVisible"
              class="context-menu"
              :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
              @click.stop
            >
              <div v-if="!contextMenuFile.isFolder" class="menu-item" @click="handleContextPreview">
                <el-icon><View /></el-icon>
                <span>预览</span>
              </div>
              <div v-if="!contextMenuFile.isFolder" class="menu-item" @click="handleContextDownload">
                <el-icon><Download /></el-icon>
                <span>下载</span>
              </div>
              <div v-if="!contextMenuFile.isFolder" class="menu-item" @click="handleContextFavorite">
                <el-icon><Star /></el-icon>
                <span>{{ contextMenuFile.isFavorite ? '取消收藏' : '收藏' }}</span>
              </div>
              <div class="menu-item" @click="handleContextShare">
                <el-icon><Share /></el-icon>
                <span>分享</span>
              </div>
              <div class="menu-item" @click="handleContextRename">
                <el-icon><Edit /></el-icon>
                <span>重命名</span>
              </div>
              <div class="menu-item" @click="handleContextMove">
                <el-icon><FolderOpened /></el-icon>
                <span>移动</span>
              </div>
              <div class="menu-divider"></div>
              <div class="menu-item danger" @click="handleContextDelete">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </div>
            </div>
          </div>

          <div v-else-if="currentView === 'upload'" class="upload-view">
            <div class="upload-header">
              <el-button
                type="primary"
                :icon="ArrowLeft"
                @click="backToFileList"
                >返回文件列表</el-button
              >
            </div>

            <el-upload
              class="upload-demo"
              drag
              :http-request="customUpload"
              :before-upload="beforeUpload"
              :show-file-list="false"
              multiple
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持多文件上传，普通用户最大500MB，VIP最大2GB
                </div>
              </template>
            </el-upload>

            <div v-if="uploadingFiles.length > 0" class="upload-list">
              <el-divider content-position="left">上传进度</el-divider>
              <div
                v-for="file in uploadingFiles"
                :key="file.uid"
                class="upload-item"
              >
                <div class="upload-item-info">
                  <el-icon><Document /></el-icon>
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ formatBytes(file.size) }}</span>
                  <span v-if="file.speed > 0" class="upload-speed">{{ formatSpeed(file.speed) }}</span>
                </div>
                <el-progress
                  :percentage="file.percentage"
                  :status="file.status"
                />
              </div>
            </div>
          </div>

          <div
            v-else-if="currentView === 'recycle-bin'"
            class="recycle-bin-view"
          >
            <div class="toolbar">
              <el-button
                v-if="selectedFiles.length > 0"
                type="primary"
                @click="handleBatchRestore"
              >
                恢复选中 ({{ selectedFiles.length }})
              </el-button>
              <el-button
                v-if="selectedFiles.length > 0"
                type="danger"
                @click="handleBatchPermanentlyDelete"
              >
                永久删除选中 ({{ selectedFiles.length }})
              </el-button>
            </div>

            <el-table
              :data="fileList"
              @selection-change="handleSelectionChange"
              v-loading="loading"
              style="width: 100%"
            >
              <el-table-column type="selection" width="55" />
              <el-table-column label="文件名" min-width="300">
                <template #default="{ row }">
                  <div class="file-name-cell">
                    <ColorfulFileIcons
                      :is-folder="row.isFolder"
                      :file-type="row.fileType"
                      :size="32"
                      style="margin-right: 12px; flex-shrink: 0;"
                    />
                    <span class="file-name">{{ row.fileName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="大小" width="120">
                <template #default="{ row }">
                  {{ row.isFolder ? "-" : formatBytes(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column label="删除时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.deletedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleRestore(row)"
                    >恢复</el-button
                  >
                  <el-button
                    link
                    type="danger"
                    @click="handlePermanentlyDelete(row)"
                    >永久删除</el-button
                  >
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-if="total > 50"
              class="pagination"
              :current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>

          <div v-else-if="currentView === 'favorites'" class="favorites-view">
            <div class="toolbar">
              <h3 style="margin: 0">我的收藏</h3>
            </div>

            <el-table :data="favoriteList" v-loading="loading" style="width: 100%">
              <el-table-column label="文件名" min-width="300">
                <template #default="{ row }">
                  <div class="file-name-cell" @dblclick="handleFileDoubleClick(row)">
                    <ColorfulFileIcons
                      :is-folder="row.isFolder"
                      :file-type="row.fileType"
                      :size="32"
                      style="margin-right: 12px; flex-shrink: 0;"
                    />
                    <span class="file-name">{{ row.fileName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="大小" width="120">
                <template #default="{ row }">
                  {{ row.isFolder ? "-" : formatBytes(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column label="修改时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="300" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="!row.isFolder"
                    link
                    type="success"
                    @click="handlePreview(row)"
                    >预览</el-button
                  >
                  <el-button
                    v-if="!row.isFolder"
                    link
                    type="primary"
                    @click="handleDownload(row)"
                    >下载</el-button
                  >
                  <el-button
                    link
                    type="danger"
                    @click="handleRemoveFavorite(row)"
                  >
                    <el-icon><Star :style="{ fill: '#f7ba2a' }" /></el-icon>
                    取消收藏
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-else-if="currentView === 'recent-uploads'" class="recent-uploads-view">
            <div class="toolbar">
              <h3 style="margin: 0">最近上传</h3>
            </div>

            <el-table :data="fileList" v-loading="loading" style="width: 100%">
              <el-table-column label="文件名" min-width="300">
                <template #default="{ row }">
                  <div class="file-name-cell" @dblclick="handleFileDoubleClick(row)">
                    <!-- 有缩略图的图片文件显示缩略图 -->
                    <img
                      v-if="row.thumbnailKey && getThumbnailUrl(row)"
                      :src="getThumbnailUrl(row)"
                      class="file-thumbnail"
                      :alt="row.fileName"
                    />
                    <!-- 没有缩略图或非图片文件显示图标 -->
                    <ColorfulFileIcons
                      v-else
                      :is-folder="row.isFolder"
                      :file-type="row.fileType"
                      :size="32"
                      style="margin-right: 12px; flex-shrink: 0;"
                    />
                    <span class="file-name">{{ row.fileName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="大小" width="120">
                <template #default="{ row }">
                  {{ formatBytes(row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column label="上传时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="400" fixed="right">
                <template #default="{ row }">
                  <el-button link type="success" @click="handlePreview(row)">预览</el-button>
                  <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
                  <el-button
                    link
                    :type="row.isFavorite ? 'warning' : 'info'"
                    @click="handleFavorite(row)"
                  >
                    <el-icon><Star :style="{ fill: row.isFavorite ? '#f7ba2a' : 'none' }" /></el-icon>
                    {{ row.isFavorite ? '取消收藏' : '收藏' }}
                  </el-button>
                  <el-button link type="info" @click="handleShare(row)">分享</el-button>
                  <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-if="total > 50"
              class="pagination"
              :current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>

          <div v-else-if="currentView === 'my-shares'" class="my-shares-view">
            <div class="toolbar">
              <h3 style="margin: 0">我的分享</h3>
            </div>

            <el-table :data="shareList" v-loading="loading" style="width: 100%">
              <el-table-column label="文件名" min-width="300">
                <template #default="{ row }">
                  <div class="file-name-cell">
                    <el-icon :size="20" color="#409eff">
                      <Document />
                    </el-icon>
                    <span class="file-name">{{ row.fileName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="分享码" width="120">
                <template #default="{ row }">
                  <el-tag type="success">{{ row.shareCode }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="访问密码" width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.hasPassword" type="warning">已设置</el-tag>
                  <el-tag v-else type="info">无</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="过期时间" width="180">
                <template #default="{ row }">
                  {{
                    row.expireTime ? formatDateTime(row.expireTime) : "永久有效"
                  }}
                </template>
              </el-table-column>
              <el-table-column label="查看/下载" width="120">
                <template #default="{ row }">
                  {{ row.viewCount }} / {{ row.downloadCount }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.status === 1" type="success">启用</el-tag>
                  <el-tag v-else type="danger">失效</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    @click="handleCopyShareLink(row)"
                    >复制链接</el-button
                  >
                  <el-button
                    v-if="row.status === 1"
                    link
                    type="danger"
                    @click="handleCancelShare(row)"
                    >取消分享</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-main>
      </el-container>
    </el-container>

    <el-dialog v-model="createFolderVisible" title="新建文件夹" width="400px">
      <el-form :model="folderForm" :rules="folderRules" ref="folderFormRef">
        <el-form-item label="文件夹名称" prop="folderName">
          <el-input
            v-model="folderForm.folderName"
            placeholder="请输入文件夹名称"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFolderVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameVisible" title="重命名" width="400px">
      <el-form :model="renameForm" :rules="renameRules" ref="renameFormRef">
        <el-form-item label="新名称" prop="newName">
          <el-input v-model="renameForm.newName" placeholder="请输入新名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenameConfirm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveVisible" title="移动文件" width="500px">
      <div class="move-dialog-content">
        <div class="move-info">
          <span>将 "{{ moveForm.fileName }}" 移动到：</span>
        </div>
        <div class="folder-tree">
          <div
            class="folder-item"
            :class="{ selected: moveForm.targetParentId === 0 }"
            @click="selectMoveTarget(0, '全部文件')"
          >
            <el-icon><Folder /></el-icon>
            <span>全部文件</span>
          </div>
          <div
            v-for="folder in folderList"
            :key="folder.id"
            class="folder-item"
            :class="{ selected: moveForm.targetParentId === folder.id }"
            :style="{ paddingLeft: folder.depth * 20 + 10 + 'px' }"
            @click="selectMoveTarget(folder.id, folder.fileName)"
          >
            <el-icon><FolderOpened /></el-icon>
            <span>{{ folder.fileName }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMoveConfirm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shareVisible" title="分享文件" width="500px">
      <div class="share-dialog-content">
        <div class="share-info">
          <span class="share-file-name">{{ shareForm.fileName }}</span>
        </div>

        <el-form :model="shareForm" label-width="100px">
          <el-form-item label="访问密码">
            <el-input
              v-model="shareForm.sharePassword"
              placeholder="留空表示无密码"
              maxlength="20"
              clearable
            />
          </el-form-item>

          <el-form-item label="有效期">
            <el-radio-group v-model="shareForm.expireDays">
              <el-radio :label="null" :disabled="!userStore.isVip">永久有效 <span v-if="!userStore.isVip" style="color: #f56c6c; font-size: 12px">(VIP)</span></el-radio>
              <el-radio :label="1">1天</el-radio>
              <el-radio :label="7">7天</el-radio>
              <el-radio :label="30" :disabled="!userStore.isVip">30天 <span v-if="!userStore.isVip" style="color: #f56c6c; font-size: 12px">(VIP)</span></el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <div v-if="shareResult" class="share-result">
          <el-divider content-position="left">分享链接</el-divider>
          <div class="share-link-box">
            <div class="share-link-item">
              <span class="label">分享码：</span>
              <el-tag type="success" size="large">{{
                shareResult.shareCode
              }}</el-tag>
            </div>
            <div class="share-link-item">
              <span class="label">链接：</span>
              <el-input v-model="shareResult.shareUrl" readonly style="flex: 1">
                <template #append>
                  <el-button @click="copyToClipboard(shareResult.shareUrl)"
                    >复制</el-button
                  >
                </template>
              </el-input>
            </div>
            <div v-if="shareResult.hasPassword" class="share-link-item">
              <span class="label">密码：</span>
              <el-tag type="warning">{{ shareForm.sharePassword }}</el-tag>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="shareVisible = false">关闭</el-button>
        <el-button v-if="!shareResult" type="primary" @click="handleCreateShare"
          >创建分享</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewVisible"
      :title="previewFile.name"
      width="80%"
      top="5vh"
    >
      <div class="preview-container" v-loading="previewLoading">
        <!-- 图片预览 -->
        <div v-if="previewFile.type === 'image'" class="preview-image">
          <img
            :src="previewFile.url"
            :alt="previewFile.name"
            style="max-width: 100%; max-height: 70vh"
          />
        </div>

        <!-- 视频预览 -->
        <div v-else-if="previewFile.type === 'video'" class="preview-video">
          <video
            ref="videoPlayer"
            :src="previewFile.url"
            controls
            style="max-width: 100%; max-height: 70vh"
          ></video>
        </div>

        <!-- 音频预览 -->
        <div v-else-if="previewFile.type === 'audio'" class="preview-audio">
          <audio
            ref="audioPlayer"
            :src="previewFile.url"
            controls
            style="width: 100%"
          ></audio>
        </div>

        <!-- 文本预览 -->
        <div v-else-if="previewFile.type === 'text'" class="preview-text">
          <pre
            style="max-height: 70vh; overflow: auto; white-space: pre-wrap"
            >{{ previewFile.content }}</pre
          >
        </div>

        <!-- 不支持预览 -->
        <div v-else class="preview-unsupported">
          <el-empty description="该文件类型不支持预览">
            <el-button type="primary" @click="handleDownload(currentPreviewRow)"
              >下载文件</el-button
            >
          </el-empty>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessageBox, ElMessage } from "element-plus";
import {
  Folder,
  User,
  SwitchButton,
  UploadFilled,
  Document,
  FolderAdd,
  Delete,
  Download,
  Search,
  FolderOpened,
  Picture,
  VideoCamera,
  Headset,
  Document as DocumentIcon,
  ArrowLeft,
  Share,
  Trophy,
  Setting,
  Star,
  StarFilled,
  View,
  Edit,
  RefreshRight,
  Close,
  Link,
  CircleClose,
  Menu,
  Grid,
  MoreFilled
} from "@element-plus/icons-vue";
import ColorfulFileIcons from "@/components/icons/ColorfulFileIcons.vue"; // 引入彩色图标组件
import { useUserStore } from "@/store/user";
import {
  uploadFile,
  getFileList,
  createFolder,
  renameFile as renameFileApi,
  deleteFiles,
  getFileDownloadInfo,
  downloadFile,
  previewFile as previewFileApi,
  getRecycleBinList,
  restoreFiles,
  permanentlyDeleteFiles,
  moveFiles,
  getThumbnail,
  getRecentUploads,
} from "@/api/file";
import { createShare, getMyShares, cancelShare } from "@/api/share";
import { addFavorite, removeFavorite, getMyFavorites, checkFavorite } from "@/api/favorite";
import AESUtil from "@/utils/aes";
import JSZip from "jszip";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const viewMode = ref('list'); // 'list' 或 'grid'

const currentView = ref("all-files");
const currentFolderId = ref(0);
const fileList = ref([]);
const breadcrumb = ref([{ id: 0, name: "全部文件", path: "/" }]);
const loading = ref(false);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(50);
const searchKeyword = ref("");
const selectedFiles = ref([]);

// 排序参数
const sortBy = ref("created_at");
const sortOrder = ref("desc");

// 拖拽上传状态
const isDraggingFile = ref(false);
let dragCounter = 0; // 用于处理嵌套元素的拖拽事件

// 右键菜单状态
const contextMenuVisible = ref(false);
const contextMenuX = ref(0);
const contextMenuY = ref(0);
const contextMenuFile = ref({});

// 缩略图缓存 (fileId -> blob URL)
const thumbnailCache = reactive({});
const loadingThumbnails = ref(new Set());
const thumbnailQueue = [];
let processingQueue = false;

const uploadingFiles = ref([]);
const uploadRef = ref();

const createFolderVisible = ref(false);
const folderForm = reactive({ folderName: "" });
const folderFormRef = ref();
const folderRules = {
  folderName: [
    { required: true, message: "请输入文件夹名称", trigger: "blur" },
  ],
};

const renameVisible = ref(false);
const renameForm = reactive({ fileId: null, newName: "" });
const renameFormRef = ref();
const renameRules = {
  newName: [{ required: true, message: "请输入新名称", trigger: "blur" }],
};

const moveVisible = ref(false);
const moveForm = reactive({ fileId: null, fileName: "", targetParentId: 0 });
const folderList = ref([]);

const shareVisible = ref(false);
const shareForm = reactive({
  fileId: null,
  fileName: "",
  sharePassword: "",
  expireDays: null,
});
const shareResult = ref(null);
const shareList = ref([]);

const favoriteList = ref([]);

const previewVisible = ref(false);
const previewLoading = ref(false);
const previewFile = reactive({
  name: "",
  type: "",
  url: "",
  content: "",
});
const currentPreviewRow = ref(null);
const videoPlayer = ref(null);
const audioPlayer = ref(null);

// 监听预览对话框关闭，停止播放并清理资源
watch(previewVisible, (newVal) => {
  if (!newVal) {
    // 停止视频播放
    if (videoPlayer.value) {
      videoPlayer.value.pause();
      videoPlayer.value.currentTime = 0;
    }
    // 停止音频播放
    if (audioPlayer.value) {
      audioPlayer.value.pause();
      audioPlayer.value.currentTime = 0;
    }
    // 清理 Blob URL
    if (previewFile.url) {
      URL.revokeObjectURL(previewFile.url);
      previewFile.url = "";
    }
  }
});

const handleMoreCommand = (command, row) => {
  switch (command) {
    case 'share':
      handleShare(row);
      break;
    case 'rename':
      handleRename(row);
      break;
    case 'move':
      handleMove(row);
      break;
    case 'delete':
      handleDelete(row);
      break;
  }
};

const toggleSelection = (file, isSelected) => {
  // 检查是否已经在 selectedFiles 中
  const index = selectedFiles.value.findIndex(f => f.id === file.id);
  
  // 如果提供了 isSelected 参数（来自 checkbox）
  if (isSelected !== undefined) {
    if (isSelected && index === -1) {
      selectedFiles.value.push(file);
    } else if (!isSelected && index !== -1) {
      selectedFiles.value.splice(index, 1);
    }
    return;
  }
  
  // 简单的点击切换逻辑
  if (index === -1) {
    selectedFiles.value.push(file);
  } else {
    selectedFiles.value.splice(index, 1);
  }
};

onMounted(() => {
  // 从URL初始化当前文件夹ID
  if (route.query.folderId) {
    currentFolderId.value = parseInt(route.query.folderId);
  }
  loadFileList();
  // 添加全局点击事件监听器，关闭右键菜单
  document.addEventListener("click", closeContextMenu);
  // 添加滚动事件监听器，滚动时关闭右键菜单
  window.addEventListener("scroll", handleScroll, true); // 使用捕获阶段，确保能捕获所有滚动事件
  // 监听主内容区域的滚动（el-main）
  const mainContent = document.querySelector('.el-main');
  if (mainContent) {
    mainContent.addEventListener("scroll", handleScroll);
  }
});

onUnmounted(() => {
  // 移除全局点击事件监听器
  document.removeEventListener("click", closeContextMenu);
  // 移除滚动事件监听器
  window.removeEventListener("scroll", handleScroll, true);
  const mainContent = document.querySelector('.el-main');
  if (mainContent) {
    mainContent.removeEventListener("scroll", handleScroll);
  }
});

const loadFileList = async () => {
  loading.value = true;
  try {
    let res;
    if (currentView.value === "recycle-bin") {
      res = await getRecycleBinList({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
      });
    } else if (currentView.value === "recent-uploads") {
      res = await getRecentUploads({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
      });
    } else if (["image", "video", "audio", "doc"].includes(currentView.value)) {
      res = await getFileList({
        category: currentView.value,
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
        sortBy: sortBy.value,
        sortOrder: sortOrder.value,
      });
    } else {
      res = await getFileList({
        parentId: currentFolderId.value,
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        keyword: searchKeyword.value,
        sortBy: sortBy.value,
        sortOrder: sortOrder.value,
      });
    }

    if (res.code === 200) {
      fileList.value = res.data.files;
      total.value = res.data.total;
      
      // 更新面包屑，如果后端返回空则重置为根目录
      if (res.data.breadcrumb && res.data.breadcrumb.items && res.data.breadcrumb.items.length > 0) {
        breadcrumb.value = res.data.breadcrumb.items;
      } else {
        // 保持默认根目录或重置
        if (currentFolderId.value === 0) {
           breadcrumb.value = [{ id: 0, name: "全部文件", path: "/" }];
        }
      }

      // 启用缩略图加载
      setTimeout(() => {
        loadThumbnailsInBatch(fileList.value);
      }, 100);
    }
  } catch (error) {
    console.error("加载文件列表失败，详细错误:", error);
    if (error.response) {
      console.error("响应状态:", error.response.status);
      console.error("响应数据:", error.response.data);
    }
    ElMessage.error("加载文件列表失败: " + (error.message || "未知错误"));
  } finally {
    loading.value = false;
  }
};

const navigateToFolder = async (folderId) => {
  try {
    console.log('[Navigation] 准备跳转到文件夹 ID:', folderId);
    // 使用路由跳转，支持浏览器前进后退
    await router.push({
      query: {
        ...route.query,
        folderId: folderId
      }
    });
  } catch (error) {
    console.error('[Navigation] 路由跳转失败:', error);
    // 降级处理：如果路由跳转失败，直接加载文件列表
    if (currentFolderId.value !== folderId) {
      console.warn('[Navigation] 启用降级处理，直接加载列表');
      currentFolderId.value = folderId;
      currentPage.value = 1;
      loadFileList();
    }
  }
};

// 监听路由参数变化，处理浏览器前进/后退
watch(
  () => route.query.folderId,
  (newFolderId) => {
    console.log('[Navigation] 路由参数变化, newFolderId:', newFolderId);
    if (currentView.value === 'all-files') {
      let targetId = 0;
      
      // 安全解析 folderId
      if (newFolderId !== undefined && newFolderId !== null && newFolderId !== '') {
        const parsed = parseInt(newFolderId);
        if (!isNaN(parsed)) {
          targetId = parsed;
        } else {
          console.error('[Navigation] 无效的文件夹ID:', newFolderId);
        }
      }

      // 只有当ID确实改变时才重新加载，避免不必要的请求
      if (currentFolderId.value !== targetId) {
        console.log(`[Navigation] 文件夹ID变更: ${currentFolderId.value} -> ${targetId}, 重新加载列表`);
        currentFolderId.value = targetId;
        currentPage.value = 1;
        loadFileList();
      }
    }
  }
);

const handleGoBack = () => {
  console.log('[Navigation] 点击返回上一级');
  if (!breadcrumb.value || breadcrumb.value.length <= 1) {
    console.warn('[Navigation] 无法返回：面包屑导航长度不足');
    return;
  }
  
  // 获取上一级目录（倒数第二个）
  const parentFolder = breadcrumb.value[breadcrumb.value.length - 2];
  
  if (!parentFolder) {
    console.error('[Navigation] 无法找到上一级目录信息');
    return;
  }
  
  console.log('[Navigation] 返回上一级:', parentFolder.name, 'ID:', parentFolder.id);
  navigateToFolder(parentFolder.id);
};

const handleFileDoubleClick = (row) => {
  if (row.isFolder === 1) {
    navigateToFolder(row.id);
  }
};

const showCreateFolderDialog = () => {
  folderForm.folderName = "";
  createFolderVisible.value = true;
};

const handleCreateFolder = async () => {
  await folderFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createFolder({
          folderName: folderForm.folderName,
          parentId: currentFolderId.value,
          filePath: breadcrumb.value.map((b) => b.name).join("/"),
        });

        ElMessage.success("创建成功");
        createFolderVisible.value = false;
        loadFileList();
      } catch (error) {
        ElMessage.error("创建失败");
      }
    }
  });
};

const showUploadDialog = () => {
  currentView.value = "upload";
};

const beforeUpload = (file) => {
  const maxSize = userStore.isVip ? 2 * 1024 * 1024 * 1024 : 500 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过${userStore.isVip ? "2GB" : "500MB"}`);
    return false;
  }

  const remainingSpace =
    userStore.userInfo.storageQuota - userStore.userInfo.storageUsed;
  if (file.size > remainingSpace) {
    ElMessage.error("存储空间不足，请清理文件或升级VIP");
    return false;
  }

  uploadingFiles.value.push({
    uid: file.uid,
    name: file.name,
    size: file.size,
    percentage: 0,
    status: "",
    speed: 0,  // 上传速度（字节/秒）
    startTime: Date.now(),  // 开始时间
  });

  return true;
};

const customUpload = async (options) => {
  const { file } = options;
  const formData = new FormData();
  formData.append("file", file);
  formData.append("parentId", currentFolderId.value);
  formData.append("filePath", breadcrumb.value.map((b) => b.name).join("/"));
  formData.append("masterKey", userStore.masterKey);

  try {
    const res = await uploadFile(formData, (percentage) => {
      const uploadingFile = uploadingFiles.value.find(
        (f) => f.uid === file.uid
      );
      if (uploadingFile) {
        uploadingFile.percentage = percentage;

        // 计算上传速度
        const currentTime = Date.now();
        const elapsedTime = (currentTime - uploadingFile.startTime) / 1000; // 秒
        if (elapsedTime > 0) {
          const uploadedBytes = (percentage / 100) * file.size;
          uploadingFile.speed = uploadedBytes / elapsedTime; // 字节/秒
        }
      }
    });

    const uploadingFile = uploadingFiles.value.find((f) => f.uid === file.uid);
    if (uploadingFile) {
      uploadingFile.percentage = 100;
      uploadingFile.status = "success";
    }

    if (res.code === 200) {
      const message = res.data.isInstantUpload ? "秒传成功" : "上传成功";
      ElMessage.success(message);

      userStore.userInfo.storageUsed += res.data.fileSize;
      userStore.updateUserInfo(userStore.userInfo);

      setTimeout(() => {
        const index = uploadingFiles.value.findIndex((f) => f.uid === file.uid);
        if (index > -1) {
          uploadingFiles.value.splice(index, 1);
        }
        if (uploadingFiles.value.length === 0) {
          currentView.value = "all-files";
          loadFileList();
        }
      }, 2000);
    }
  } catch (error) {
    const uploadingFile = uploadingFiles.value.find((f) => f.uid === file.uid);
    if (uploadingFile) {
      uploadingFile.status = "exception";
    }
    ElMessage.error("上传失败: " + (error.message || "请重试"));
  }
};

// 拖拽上传处理函数
const handleFileDragOver = (e) => {
  // 区分外部文件拖入和内部元素拖拽
  // 外部文件拖入：dataTransfer.types 包含 "Files"
  // 内部元素拖拽：dataTransfer.types 包含 "text/plain"
  const isExternalFile = e.dataTransfer.types.includes('Files');
  const isInternalDrag = e.dataTransfer.types.includes('text/plain');

  // 只有外部文件拖入才显示上传提示框
  if (isExternalFile && !isInternalDrag) {
    dragCounter++;
    if (dragCounter === 1) {
      isDraggingFile.value = true;
    }
  }
};

const handleFileDragLeave = (e) => {
  const isExternalFile = e.dataTransfer.types.includes('Files');
  const isInternalDrag = e.dataTransfer.types.includes('text/plain');

  if (isExternalFile && !isInternalDrag) {
    dragCounter--;
    if (dragCounter === 0) {
      isDraggingFile.value = false;
    }
  }
};

const handleFileDrop = async (e) => {
  const isExternalFile = e.dataTransfer.types.includes('Files');
  const isInternalDrag = e.dataTransfer.types.includes('text/plain');

  // 如果是内部拖拽，不处理文件上传
  if (isInternalDrag || !isExternalFile) {
    return;
  }

  dragCounter = 0;
  isDraggingFile.value = false;

  if (currentView.value !== 'all-files') {
    ElMessage.warning('请在"全部文件"视图下上传文件');
    return;
  }

  const files = Array.from(e.dataTransfer.files);

  if (files.length === 0) {
    return;
  }

  // 验证文件大小和存储空间
  for (const file of files) {
    const maxSize = userStore.isVip ? 2 * 1024 * 1024 * 1024 : 500 * 1024 * 1024;
    if (file.size > maxSize) {
      ElMessage.error(`文件 ${file.name} 大小超过限制（${userStore.isVip ? "2GB" : "500MB"}）`);
      continue;
    }

    const remainingSpace = userStore.userInfo.storageQuota - userStore.userInfo.storageUsed;
    if (file.size > remainingSpace) {
      ElMessage.error(`存储空间不足，无法上传文件 ${file.name}`);
      continue;
    }

    // 添加到上传队列
    const uid = Date.now() + Math.random();
    uploadingFiles.value.push({
      uid: uid,
      name: file.name,
      size: file.size,
      percentage: 0,
      status: "",
      speed: 0,
      startTime: Date.now(),
    });

    // 开始上传
    uploadFileWithProgress(file, uid);
  }

  // 如果有文件正在上传，切换到上传视图
  if (uploadingFiles.value.length > 0) {
    currentView.value = "upload";
  }
};

const uploadFileWithProgress = async (file, uid) => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("parentId", currentFolderId.value);
  formData.append("filePath", breadcrumb.value.map((b) => b.name).join("/"));
  formData.append("masterKey", userStore.masterKey);

  try {
    const res = await uploadFile(formData, (percentage) => {
      const uploadingFile = uploadingFiles.value.find((f) => f.uid === uid);
      if (uploadingFile) {
        uploadingFile.percentage = percentage;

        // 计算上传速度
        const currentTime = Date.now();
        const elapsedTime = (currentTime - uploadingFile.startTime) / 1000; // 秒
        if (elapsedTime > 0) {
          const uploadedBytes = (percentage / 100) * file.size;
          uploadingFile.speed = uploadedBytes / elapsedTime; // 字节/秒
        }
      }
    });

    const uploadingFile = uploadingFiles.value.find((f) => f.uid === uid);
    if (uploadingFile) {
      uploadingFile.percentage = 100;
      uploadingFile.status = "success";
    }

    if (res.code === 200) {
      const message = res.data.isInstantUpload ? "秒传成功" : "上传成功";
      ElMessage.success(`${file.name} ${message}`);

      userStore.userInfo.storageUsed += res.data.fileSize;
      userStore.updateUserInfo(userStore.userInfo);

      setTimeout(() => {
        const index = uploadingFiles.value.findIndex((f) => f.uid === uid);
        if (index > -1) {
          uploadingFiles.value.splice(index, 1);
        }
        if (uploadingFiles.value.length === 0 && currentView.value === "upload") {
          currentView.value = "all-files";
          loadFileList();
        }
      }, 2000);
    }
  } catch (error) {
    const uploadingFile = uploadingFiles.value.find((f) => f.uid === uid);
    if (uploadingFile) {
      uploadingFile.status = "exception";
    }
    ElMessage.error(`${file.name} 上传失败: ` + (error.message || "请重试"));
  }
};

const handleRename = (row) => {
  renameForm.fileId = row.id;
  renameForm.newName = row.fileName;
  renameVisible.value = true;
};

const handleRenameConfirm = async () => {
  await renameFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await renameFileApi({
          fileId: renameForm.fileId,
          newName: renameForm.newName,
        });

        ElMessage.success("重命名成功");
        renameVisible.value = false;
        loadFileList();
      } catch (error) {
        ElMessage.error("重命名失败");
      }
    }
  });
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除 "${row.fileName}" 吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await deleteFiles({ fileIds: [row.id] });
        ElMessage.success("删除成功");
        loadFileList();
      } catch (error) {
        ElMessage.error("删除失败");
      }
    })
    .catch(() => {});
};

const handleBatchDelete = () => {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedFiles.value.length} 个文件吗？`,
    "提示",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  )
    .then(async () => {
      try {
        await deleteFiles({ fileIds: selectedFiles.value.map((f) => f.id) });
        ElMessage.success("删除成功");
        selectedFiles.value = [];
        loadFileList();
      } catch (error) {
        ElMessage.error("删除失败");
      }
    })
    .catch(() => {});
};

const handleBatchDownload = async () => {
  const files = selectedFiles.value.filter((f) => f.isFolder === 0);

  if (files.length === 0) {
    ElMessage.warning("请选择要下载的文件（文件夹不支持批量下载）");
    return;
  }

  const loadingMsg = ElMessage({
    message: `正在准备下载 ${files.length} 个文件...`,
    type: "info",
    duration: 0,
  });

  try {
    const zip = new JSZip();
    let successCount = 0;
    let failCount = 0;
    const failedFiles = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      try {
        loadingMsg.message = `正在处理 ${i + 1}/${files.length}: ${file.fileName}...`;

        const infoRes = await getFileDownloadInfo(file.id);
        const fileInfo = infoRes.data;

        const fileRes = await downloadFile(file.id);
        const encryptedData = fileRes.data;

        if (!encryptedData || encryptedData.byteLength === 0) {
          throw new Error("下载的文件数据为空");
        }

        const decryptedData = await AESUtil.decryptFile(
          encryptedData,
          fileInfo.encryptedKey,
          userStore.masterKey
        );

        if (!decryptedData || decryptedData.byteLength === 0) {
          throw new Error("解密后的文件数据为空");
        }

        zip.file(file.fileName, decryptedData);
        successCount++;
      } catch (error) {
        console.error(`处理文件失败: ${file.fileName}`, error);
        failCount++;
        failedFiles.push(file.fileName);
      }
    }

    if (successCount === 0) {
      loadingMsg.close();
      ElMessage.error(`所有文件下载失败。失败的文件：${failedFiles.join(", ")}`);
      return;
    }

    loadingMsg.message = "正在打包文件...";
    const zipBlob = await zip.generateAsync({ type: "blob" });

    const url = URL.createObjectURL(zipBlob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `files_${Date.now()}.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    loadingMsg.close();

    if (failCount > 0) {
      ElMessage.warning(`批量下载完成，成功 ${successCount} 个，失败 ${failCount} 个。失败的文件：${failedFiles.join(", ")}`);
    } else {
      ElMessage.success(`批量下载成功，共 ${successCount} 个文件`);
    }
  } catch (error) {
    console.error("批量下载失败:", error);
    loadingMsg.close();
    ElMessage.error("批量下载失败: " + (error.message || "请重试"));
  }
};

// 右键菜单处理函数
const handleRowContextMenu = (row, column, event) => {
  event.preventDefault();
  contextMenuFile.value = row;

  // 菜单尺寸（需要与CSS中的实际尺寸匹配）
  const menuWidth = 160;
  const menuHeight = 280; // 估算菜单高度（根据菜单项数量调整）

  // 获取视口尺寸
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;

  // 计算菜单位置（考虑边界）
  let x = event.clientX;
  let y = event.clientY;

  // 水平边界检查：如果菜单会超出右边界，则向左偏移
  if (x + menuWidth > viewportWidth) {
    x = viewportWidth - menuWidth - 10; // 留10px边距
  }

  // 垂直边界检查：如果菜单会超出下边界，则向上偏移
  if (y + menuHeight > viewportHeight) {
    y = viewportHeight - menuHeight - 10; // 留10px边距
  }

  // 确保不会超出左边界和上边界
  x = Math.max(10, x);
  y = Math.max(10, y);

  contextMenuX.value = x;
  contextMenuY.value = y;
  contextMenuVisible.value = true;
};

const handleContextPreview = () => {
  contextMenuVisible.value = false;
  handlePreview(contextMenuFile.value);
};

const handleContextDownload = () => {
  contextMenuVisible.value = false;
  handleDownload(contextMenuFile.value);
};

const handleContextRename = () => {
  contextMenuVisible.value = false;
  handleRename(contextMenuFile.value);
};

const handleContextMove = () => {
  contextMenuVisible.value = false;
  handleMove(contextMenuFile.value);
};

const handleContextShare = () => {
  contextMenuVisible.value = false;
  handleShare(contextMenuFile.value);
};

const handleContextDelete = () => {
  contextMenuVisible.value = false;
  handleDelete(contextMenuFile.value);
};

const handleContextFavorite = () => {
  contextMenuVisible.value = false;
  handleFavorite(contextMenuFile.value);
};

// 点击外部关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false;
};

// 滚动时关闭右键菜单
const handleScroll = () => {
  if (contextMenuVisible.value) {
    closeContextMenu();
  }
};

// 加载文件缩略图
const loadThumbnail = async (file) => {
  // 只处理有缩略图的图片文件
  if (!file.thumbnailKey || file.isFolder === 1) {
    return null;
  }

  // 检查缓存
  if (thumbnailCache[file.id]) {
    return thumbnailCache[file.id];
  }

  // 检查是否正在加载
  if (loadingThumbnails.value.has(file.id)) {
    return null;
  }

  // 标记为正在加载
  loadingThumbnails.value.add(file.id);

  try {
    // 获取文件信息（包含加密密钥）
    const infoRes = await getFileDownloadInfo(file.id);
    const fileInfo = infoRes.data;

    // 获取缩略图数据
    const thumbnailRes = await getThumbnail(file.id);
    const encryptedData = thumbnailRes.data;

    // 解密缩略图
    const decryptedData = await AESUtil.decryptFile(
      encryptedData,
      fileInfo.encryptedKey,
      userStore.masterKey
    );

    // 创建 Blob URL
    const blob = new Blob([decryptedData], { type: file.fileType });
    const url = URL.createObjectURL(blob);

    // 存入缓存（使用响应式对象）
    thumbnailCache[file.id] = url;
    loadingThumbnails.value.delete(file.id);

    return url;
  } catch (error) {
    // 静默失败，不影响主流程
    // 只在非404错误时输出警告
    if (error.response && error.response.status !== 404) {
      console.warn(`加载缩略图失败 (文件ID: ${file.id}):`, error.message);
    }
    loadingThumbnails.value.delete(file.id);
    return null;
  }
};

// 批量加载缩略图（限制并发数）
const loadThumbnailsInBatch = async (files) => {
  const MAX_CONCURRENT = 3; // 最多同时加载3个缩略图
  const filesToLoad = files.filter(f => f.thumbnailKey && f.isFolder === 0 && !thumbnailCache[f.id]);

  for (let i = 0; i < filesToLoad.length; i += MAX_CONCURRENT) {
    const batch = filesToLoad.slice(i, i + MAX_CONCURRENT);
    await Promise.allSettled(batch.map(file => loadThumbnail(file)));
    // 每批之间稍微延迟，避免服务器压力过大
    if (i + MAX_CONCURRENT < filesToLoad.length) {
      await new Promise(resolve => setTimeout(resolve, 50));
    }
  }
};

// 获取缩略图 URL（同步方法，用于模板中）
const getThumbnailUrl = (file) => {
  if (!file.thumbnailKey || file.isFolder === 1) {
    return null;
  }
  return thumbnailCache[file.id] || null;
};

const handleSelectionChange = (selection) => {
  selectedFiles.value = selection;
};

const handlePageChange = (page) => {
  currentPage.value = page;
  loadFileList();
};

const handleSearch = () => {
  currentPage.value = 1;
  loadFileList();
};

const handleSortChange = ({ column, prop, order }) => {
  // 映射字段名到后端字段名
  const fieldMap = {
    fileName: "file_name",
    fileSize: "file_size",
    updatedAt: "updated_at",
  };

  if (order) {
    sortBy.value = fieldMap[prop] || "created_at";
    sortOrder.value = order === "ascending" ? "asc" : "desc";
  } else {
    // 取消排序，恢复默认
    sortBy.value = "created_at";
    sortOrder.value = "desc";
  }

  currentPage.value = 1;
  loadFileList();
};

const handleMenuSelect = (index) => {
  currentView.value = index;
  if (
    ["all-files", "recycle-bin", "image", "video", "audio", "doc"].includes(
      index
    )
  ) {
    currentPage.value = 1;
    if (["image", "video", "audio", "doc"].includes(index)) {
      currentFolderId.value = 0;
    }
    loadFileList();
  } else if (index === "my-shares") {
    loadShareList();
  } else if (index === "favorites") {
    loadFavoriteList();
  }
};

const loadShareList = async () => {
  loading.value = true;
  try {
    const res = await getMyShares({
      pageNum: 1,
      pageSize: 100,
    });

    if (res.code === 200) {
      shareList.value = res.data;
    }
  } catch (error) {
    ElMessage.error("加载分享列表失败");
  } finally {
    loading.value = false;
  }
};

// 加载收藏列表
const loadFavoriteList = async () => {
  loading.value = true;
  try {
    const res = await getMyFavorites();
    if (res.code === 200) {
      favoriteList.value = res.data;
    }
  } catch (error) {
    ElMessage.error("加载收藏列表失败");
  } finally {
    loading.value = false;
  }
};

// 处理收藏/取消收藏
const handleFavorite = async (row) => {
  try {
    if (row.isFavorite) {
      // 取消收藏
      const res = await removeFavorite(row.id);
      if (res.code === 200) {
        ElMessage.success("已取消收藏");
        row.isFavorite = false;
      }
    } else {
      // 添加收藏
      const res = await addFavorite(row.id);
      if (res.code === 200) {
        ElMessage.success("收藏成功");
        row.isFavorite = true;
      }
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "操作失败");
  }
};

// 从收藏列表中移除
const handleRemoveFavorite = async (row) => {
  try {
    const res = await removeFavorite(row.id);
    if (res.code === 200) {
      ElMessage.success("已取消收藏");
      // 重新加载收藏列表
      loadFavoriteList();
    }
  } catch (error) {
    ElMessage.error("操作失败");
  }
};

// 检查文件收藏状态
const checkFavoriteStatus = async (fileId) => {
  try {
    const res = await checkFavorite(fileId);
    if (res.code === 200) {
      return res.data;
    }
  } catch (error) {
    return false;
  }
  return false;
};

const backToFileList = () => {
  currentView.value = "all-files";
  loadFileList();
};

const handleCommand = (command) => {
  if (command === "profile") {
    router.push("/profile");
  } else if (command === "vip") {
    router.push("/vip");
  } else if (command === "operation-log") {
    router.push("/operation-log");
  } else if (command === "admin") {
    router.push("/admin");
  } else if (command === "logout") {
    ElMessageBox.confirm("确定要退出登录吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    })
      .then(() => {
        userStore.logout();
        ElMessage.success("已退出登录");
      })
      .catch(() => {});
  }
};

const formatBytes = (bytes) => {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + " " + sizes[i];
};

// 格式化速度（字节/秒）
const formatSpeed = (bytesPerSecond) => {
  return formatBytes(bytesPerSecond) + "/s";
};

const formatDateTime = (datetime) => {
  if (!datetime) return "-";
  const date = new Date(datetime);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const getProgressColor = (percentage) => {
  if (percentage < 50) return "#67c23a";
  if (percentage < 80) return "#e6a23c";
  return "#f56c6c";
};

// VIP到期天数计算
const vipDaysRemaining = computed(() => {
  if (!userStore.isVip || !userStore.userInfo.vipExpireTime) {
    return null;
  }
  const expireTime = new Date(userStore.userInfo.vipExpireTime);
  const now = new Date();
  const diffTime = expireTime - now;
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays > 0 ? diffDays : 0;
});

// VIP提醒标题
const vipAlertTitle = computed(() => {
  const days = vipDaysRemaining.value;
  if (days === null) return '';
  if (days === 0) return 'VIP会员已过期';
  if (days <= 3) return 'VIP会员即将到期';
  if (days <= 7) return 'VIP会员到期提醒';
  return 'VIP会员状态';
});

// VIP提醒类型
const vipAlertType = computed(() => {
  const days = vipDaysRemaining.value;
  if (days === null) return 'info';
  if (days === 0) return 'error';
  if (days <= 3) return 'warning';
  if (days <= 7) return 'warning';
  return 'success';
});

// VIP提醒消息
const vipAlertMessage = computed(() => {
  const days = vipDaysRemaining.value;
  if (days === null) return '';
  if (days === 0) {
    return '您的VIP会员已过期，部分功能将受到限制。';
  }
  if (days === 1) {
    return '您的VIP会员还有1天到期，建议尽快续费以继续享受VIP特权。';
  }
  if (days <= 3) {
    return `您的VIP会员还有${days}天到期，建议尽快续费。`;
  }
  if (days <= 7) {
    return `您的VIP会员还有${days}天到期。`;
  }
  return `您的VIP会员还有${days}天到期，感谢您的支持！`;
});

// 跳转到VIP中心
const goToVip = () => {
  router.push('/vip');
};

const getCategoryTitle = (view) => {
  const titles = {
    image: "全部图片",
    video: "全部视频",
    audio: "全部音频",
    doc: "全部文档",
  };
  return titles[view] || "";
};

const getFileIcon = (row) => {
  if (row.isFolder === 1) return FolderOpened;

  const fileType = row.fileType || "";
  if (fileType.startsWith("image/")) return Picture;
  if (fileType.startsWith("video/")) return VideoCamera;
  if (fileType.startsWith("audio/")) return Headset;
  return DocumentIcon;
};

const getFileIconColor = (row) => {
  if (row.isFolder === 1) return "#409eff";

  const fileType = row.fileType || "";
  if (fileType.startsWith("image/")) return "#67c23a";
  if (fileType.startsWith("video/")) return "#e6a23c";
  if (fileType.startsWith("audio/")) return "#f56c6c";
  return "#909399";
};

const handlePreview = async (row) => {
  currentPreviewRow.value = row;
  previewFile.name = row.fileName;
  previewFile.type = "";
  previewFile.url = "";
  previewFile.content = "";
  previewVisible.value = true;
  previewLoading.value = true;

  try {
    // 获取文件下载信息
    const infoRes = await getFileDownloadInfo(row.id);
    console.log("下载信息响应:", infoRes);
    const fileInfo = infoRes.data;

    // 使用预览接口下载加密文件（避免被IDM等下载管理器拦截）
    const fileRes = await previewFileApi(row.id);
    console.log("文件预览响应:", fileRes);
    const encryptedData = fileRes.data; // response.data 是 ArrayBuffer

    // 检查数据是否为空
    if (!encryptedData || encryptedData.byteLength === 0) {
      throw new Error("文件数据为空，可能被下载管理器拦截。请禁用IDM等下载管理器的浏览器扩展后重试。");
    }

    // 解密文件
    console.log("开始解密，加密数据长度:", encryptedData.byteLength);
    console.log("文件密钥:", fileInfo.encryptedKey);
    console.log("主密钥:", userStore.masterKey);

    const decryptedData = await AESUtil.decryptFile(
      encryptedData,
      fileInfo.encryptedKey,
      userStore.masterKey
    );

    // 根据文件类型创建 Blob URL
    const fileType = fileInfo.fileType || "";
    const blob = new Blob([decryptedData], { type: fileType });
    const url = URL.createObjectURL(blob);

    // 设置预览类型和 URL
    if (fileType.startsWith("image/")) {
      previewFile.type = "image";
      previewFile.url = url;
    } else if (fileType.startsWith("video/")) {
      previewFile.type = "video";
      previewFile.url = url;
    } else if (fileType.startsWith("audio/")) {
      previewFile.type = "audio";
      previewFile.url = url;
    } else if (fileType.startsWith("text/")) {
      previewFile.type = "text";
      const textContent = new TextDecoder("utf-8").decode(decryptedData);
      previewFile.content = textContent;
    } else {
      previewFile.type = "unsupported";
    }

    previewLoading.value = false;
  } catch (error) {
    console.error("预览失败详细错误:", error);
    console.error("错误堆栈:", error.stack);
    console.error("错误响应:", error.response);

    let errorMsg = "请求失败";
    if (error.response) {
      errorMsg = `服务器错误: ${error.response.status} ${error.response.statusText}`;
    } else if (error.message) {
      errorMsg = error.message;
    }

    ElMessage.error("预览失败: " + errorMsg);
    previewLoading.value = false;
    previewVisible.value = false;
  }
};

const handleDownload = async (row) => {
  const loadingMsg = ElMessage({
    message: "正在下载文件...",
    type: "info",
    duration: 0,
  });

  try {
    const infoRes = await getFileDownloadInfo(row.id);
    const fileInfo = infoRes.data;

    const fileRes = await downloadFile(row.id);
    const encryptedData = fileRes.data;

    if (!encryptedData || encryptedData.byteLength === 0) {
      throw new Error("下载的文件数据为空");
    }

    if (encryptedData.byteLength < 12) {
      throw new Error(`下载的文件数据太小，无法解密`);
    }

    const decryptedData = await AESUtil.decryptFile(
      encryptedData,
      fileInfo.encryptedKey,
      userStore.masterKey
    );

    const blob = new Blob([decryptedData], {
      type: fileInfo.fileType || "application/octet-stream",
    });
    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = url;
    link.download = fileInfo.fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(url);

    loadingMsg.close();
    ElMessage.success("下载成功");
  } catch (error) {
    console.error("下载失败:", error);
    loadingMsg.close();
    ElMessage.error("下载失败: " + (error.message || "请重试"));
  }
};

// 回收站相关方法
const handleRestore = (row) => {
  ElMessageBox.confirm(`确定要恢复 "${row.fileName}" 吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "info",
  })
    .then(async () => {
      try {
        await restoreFiles({ fileIds: [row.id] });
        ElMessage.success("恢复成功");
        loadFileList();
      } catch (error) {
        ElMessage.error("恢复失败");
      }
    })
    .catch(() => {});
};

const handleBatchRestore = () => {
  ElMessageBox.confirm(
    `确定要恢复选中的 ${selectedFiles.value.length} 个文件吗？`,
    "提示",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "info",
    }
  )
    .then(async () => {
      try {
        await restoreFiles({ fileIds: selectedFiles.value.map((f) => f.id) });
        ElMessage.success("恢复成功");
        selectedFiles.value = [];
        loadFileList();
      } catch (error) {
        ElMessage.error("恢复失败");
      }
    })
    .catch(() => {});
};

const handlePermanentlyDelete = (row) => {
  ElMessageBox.confirm(
    `确定要永久删除 "${row.fileName}" 吗？此操作无法撤销！`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "error",
    }
  )
    .then(async () => {
      try {
        await permanentlyDeleteFiles({ fileIds: [row.id] });
        ElMessage.success("永久删除成功");
        loadFileList();
      } catch (error) {
        ElMessage.error("永久删除失败");
      }
    })
    .catch(() => {});
};

const handleBatchPermanentlyDelete = () => {
  ElMessageBox.confirm(
    `确定要永久删除选中的 ${selectedFiles.value.length} 个文件吗？此操作无法撤销！`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "error",
    }
  )
    .then(async () => {
      try {
        await permanentlyDeleteFiles({
          fileIds: selectedFiles.value.map((f) => f.id),
        });
        ElMessage.success("永久删除成功");
        selectedFiles.value = [];
        loadFileList();
      } catch (error) {
        ElMessage.error("永久删除失败");
      }
    })
    .catch(() => {});
};

// 移动文件相关方法
const loadAllFolders = async (parentId = 0, excludeId = null, depth = 0) => {
  const res = await getFileList({
    parentId: parentId,
    pageNum: 1,
    pageSize: 1000,
  });

  if (res.code !== 200) {
    return [];
  }

  const folders = [];
  const files = res.data.files.filter(
    (f) => f.isFolder === 1 && f.id !== excludeId
  );

  for (const folder of files) {
    folders.push({
      ...folder,
      depth: depth,
    });

    // 递归加载子文件夹
    const subFolders = await loadAllFolders(folder.id, excludeId, depth + 1);
    folders.push(...subFolders);
  }

  return folders;
};

const handleMove = async (row) => {
  moveForm.fileId = row.id;
  moveForm.fileName = row.fileName;
  moveForm.targetParentId = 0;

  // 递归加载所有文件夹列表
  try {
    const folders = await loadAllFolders(0, row.id);
    folderList.value = folders;
  } catch (error) {
    ElMessage.error("加载文件夹列表失败");
  }

  moveVisible.value = true;
};

const selectMoveTarget = (folderId, folderName) => {
  moveForm.targetParentId = folderId;
};

const handleMoveConfirm = async () => {
  try {
    await moveFiles({
      fileIds: [moveForm.fileId],
      targetParentId: moveForm.targetParentId,
    });

    ElMessage.success("移动成功");
    moveVisible.value = false;
    loadFileList();
  } catch (error) {
    ElMessage.error("移动失败");
  }
};

// 分享相关方法
const handleShare = (row) => {
  // 如果是文件夹，给出提示
  if (row.isFolder === 1) {
    ElMessageBox.confirm(
      "当前选择的是文件夹，分享后无法直接下载。建议分享文件夹内的单个文件。确定要继续分享吗？",
      "提示",
      {
        confirmButtonText: "继续分享",
        cancelButtonText: "取消",
        type: "warning",
      }
    )
      .then(() => {
        openShareDialog(row);
      })
      .catch(() => {});
  } else {
    openShareDialog(row);
  }
};

const openShareDialog = (row) => {
  shareForm.fileId = row.id;
  shareForm.fileName = row.fileName;
  shareForm.sharePassword = "";
  shareForm.expireDays = null;
  shareResult.value = null;
  shareVisible.value = true;
};

const handleCreateShare = async () => {
  try {
    const res = await createShare({
      fileId: shareForm.fileId,
      sharePassword: shareForm.sharePassword,
      expireDays: shareForm.expireDays,
    });

    if (res.code === 200) {
      shareResult.value = res.data;
      ElMessage.success("分享创建成功");
    }
  } catch (error) {
    ElMessage.error("创建分享失败");
  }
};

const handleCopyShareLink = (row) => {
  copyToClipboard(row.shareUrl);
};

const handleCancelShare = (row) => {
  ElMessageBox.confirm(`确定要取消分享 "${row.fileName}" 吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await cancelShare(row.id);
        ElMessage.success("取消分享成功");
        loadShareList();
      } catch (error) {
        ElMessage.error("取消分享失败");
      }
    })
    .catch(() => {});
};

const copyToClipboard = (text) => {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard
      .writeText(text)
      .then(() => {
        ElMessage.success("已复制到剪贴板");
      })
      .catch(() => {
        fallbackCopyToClipboard(text);
      });
  } else {
    fallbackCopyToClipboard(text);
  }
};

const fallbackCopyToClipboard = (text) => {
  const textArea = document.createElement("textarea");
  textArea.value = text;
  textArea.style.position = "fixed";
  textArea.style.opacity = "0";
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  try {
    document.execCommand("copy");
    ElMessage.success("已复制到剪贴板");
  } catch (err) {
    ElMessage.error("复制失败");
  }
  document.body.removeChild(textArea);
};

const draggedFile = ref(null);
const dragOverFolder = ref(null);

const handleDragStart = (event, row) => {
  event.stopPropagation();

  draggedFile.value = row;
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("text/plain", row.id.toString());
};

const handleDragOver = (event, row) => {
  if (
    row.isFolder === 1 &&
    draggedFile.value &&
    draggedFile.value.id !== row.id
  ) {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = "move";
    dragOverFolder.value = row.id;
  }
};

const handleDragLeave = (event) => {
  event.stopPropagation();
  dragOverFolder.value = null;
};

const handleDrop = async (event, targetFolder) => {
  event.preventDefault();
  event.stopPropagation();

  dragOverFolder.value = null;

  if (!draggedFile.value || draggedFile.value.id === targetFolder.id) {
    draggedFile.value = null;
    return;
  }

  if (targetFolder.isFolder !== 1) {
    ElMessage.warning("只能拖拽到文件夹上");
    draggedFile.value = null;
    return;
  }

  if (draggedFile.value.isFolder === 1) {
    if (draggedFile.value.id === targetFolder.id) {
      ElMessage.warning("不能将文件夹移动到自己内部");
      draggedFile.value = null;
      return;
    }
  }

  const fileName = draggedFile.value.fileName;
  const folderName = targetFolder.fileName;
  const fileIds = [draggedFile.value.id];

  try {
    await moveFiles({
      fileIds: fileIds,
      targetParentId: targetFolder.id,
    });

    ElMessage.success(`已将 "${fileName}" 移动到 "${folderName}"`);
    loadFileList();
  } catch (error) {
    console.error("拖拽移动失败:", error);
    const errorMsg =
      error.response?.data?.message || error.message || "未知错误";
    ElMessage.error("移动失败: " + errorMsg);
  } finally {
    draggedFile.value = null;
  }
};

const handleDragEnd = (event) => {
  event.stopPropagation();
  draggedFile.value = null;
  dragOverFolder.value = null;
};
</script>

<style scoped>
.file-manager {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.outer-container {
  height: 100%;
  overflow: hidden;
}

.main-container {
  flex: 1;
  overflow: hidden;
  height: 100%;
}

/* 侧边栏优化 */
.sidebar {
  background: var(--bg-white);
  border-right: 1px solid var(--bg-gray-200);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  flex-shrink: 0;
}

/* 侧边栏菜单项优化 */
:deep(.el-menu) {
  border-right: none;
  background-color: transparent;
}

:deep(.el-menu-item) {
  margin: 4px 12px;
  border-radius: var(--radius-medium);
  height: 48px;
  line-height: 48px;
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
  color: var(--text-tertiary);
  transition: color 0.3s;
  font-size: 18px;
}

:deep(.el-menu-item:hover .el-icon),
:deep(.el-menu-item.is-active .el-icon) {
  color: var(--primary-color);
}

/* 顶部导航栏优化 */
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

/* 顶部渐变装饰线 */
.header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-color) 0%, var(--accent-blue) 100%);
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-round);
  transition: var(--transition-base);
  color: var(--text-secondary);
}

.user-dropdown:hover {
  background-color: var(--bg-gray-100);
  color: var(--primary-color);
}

/* 存储空间展示 */
.storage-info {
  padding: 24px 20px;
  margin-top: auto;
  border-top: 1px solid var(--bg-gray-200);
}

.storage-title {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 12px;
  font-weight: 500;
  display: flex;
  justify-content: space-between;
}

.storage-text {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
  text-align: right;
}

/* 内容区域背景 */
.content {
  background: var(--bg-gray-50);
  padding: 24px;
  overflow-y: auto;
  height: 100%;
  box-sizing: border-box;
}

/* 面包屑导航栏 */
.breadcrumb-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  height: 40px;
}

.breadcrumb-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn {
  font-weight: 500;
  transition: all 0.3s;
  padding: 0 4px;
}

.back-btn:hover:not(:disabled) {
  transform: translateX(-2px);
}

.breadcrumb-divider {
  margin: 0 8px;
  height: 1.2em;
}

.breadcrumb {
  font-size: 16px;
}

:deep(.el-breadcrumb__inner) {
  color: var(--text-secondary) !important;
  font-weight: normal !important;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary) !important;
  font-weight: 600 !important;
}

:deep(.el-breadcrumb__inner:hover) {
  color: var(--primary-color) !important;
  cursor: pointer;
}

.view-toggle {
  display: flex;
  gap: 8px;
  background: var(--bg-white);
  padding: 4px;
  border-radius: var(--radius-medium);
  box-shadow: var(--shadow-sm);
}

/* 工具栏优化 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: var(--bg-white);
  padding: 16px;
  border-radius: var(--radius-medium);
  box-shadow: var(--shadow-sm);
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border: none;
  box-shadow: var(--shadow-pink);
  padding: 10px 20px;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(251, 114, 153, 0.4);
}

.btn-default {
  border-color: var(--bg-gray-200);
  color: var(--text-primary);
}

.btn-default:hover {
  border-color: var(--primary-light);
  color: var(--primary-color);
  background-color: var(--primary-lighter);
}

.search-input {
  width: 240px;
  transition: width 0.3s;
}

.search-input:focus-within {
  width: 300px;
}

/* 文件列表容器 */
.file-list-container {
  background: transparent; /* 移除白色背景，直接展示在灰色底色上 */
  box-shadow: none;
  padding: 0;
  min-height: 100%;
}

/* 表格样式优化 */
:deep(.el-table) {
  --el-table-header-bg-color: var(--bg-white);
  --el-table-row-hover-bg-color: var(--primary-lighter);
  border-radius: var(--radius-large);
  overflow: hidden;
  box-shadow: var(--shadow-base);
}

:deep(.el-table th.el-table__cell) {
  font-weight: 600;
  color: var(--text-secondary);
  height: 50px;
  background-color: var(--bg-white) !important;
  border-bottom: 1px solid var(--bg-gray-200);
}

:deep(.el-table td.el-table__cell) {
  height: 64px;
  padding: 0;
  border-bottom: 1px solid var(--bg-gray-100);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
  transition: var(--transition-fast);
  padding: 8px 0;
}

.file-name {
  flex: 1;
  font-weight: 500;
  color: var(--text-primary);
  transition: color 0.2s;
}

.file-name-cell:hover .file-name {
  color: var(--primary-color);
}

/* 网格视图样式 */
.grid-view-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  padding-bottom: 20px;
}

.grid-item {
  background: var(--bg-white);
  border-radius: var(--radius-medium);
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  border: 1px solid transparent;
  box-shadow: var(--shadow-sm);
}

.grid-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary-light);
}

.grid-item.is-selected {
  background-color: var(--primary-lighter);
  border-color: var(--primary-color);
}

.grid-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.grid-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.grid-name {
  font-size: 14px;
  color: var(--text-primary);
  text-align: center;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.grid-check {
  position: absolute;
  top: 8px;
  left: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.grid-item:hover .grid-check,
.grid-item.is-selected .grid-check {
  opacity: 1;
}

/* 操作按钮组 */
.action-buttons {
  display: flex;
  gap: 4px;
  justify-content: flex-end;
  opacity: 0;
  transition: opacity 0.2s;
}

.file-table-row:hover .action-buttons {
  opacity: 1;
}

/* 收藏图标 */
.favorite-icon {
  margin-left: 8px;
  font-size: 16px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.upload-view {
  background: #fff;
  padding: 40px;
  border-radius: 4px;
}

.upload-header {
  margin-bottom: 30px;
}

.upload-demo {
  width: 100%;
}

.upload-list {
  margin-top: 30px;
}

.upload-item {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.upload-item-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
}

.upload-item-info .file-name {
  flex: 1;
  font-weight: 500;
  color: #303133;
}

.upload-item-info .file-size {
  color: #909399;
  font-size: 12px;
}

.upload-item-info .upload-speed {
  color: #409eff;
  font-size: 12px;
  font-weight: 500;
}

.preview-container {
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-image,
.preview-video,
.preview-audio {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-text {
  width: 100%;
}

.preview-text pre {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  font-family: "Courier New", Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
}

.preview-unsupported {
  width: 100%;
  padding: 40px 0;
}

.recycle-bin-view {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.move-dialog-content {
  padding: 10px 0;
}

.move-info {
  margin-bottom: 20px;
  font-size: 14px;
  color: #606266;
}

.folder-tree {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  margin: 5px 0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.folder-item:hover {
  background-color: #f5f7fa;
}

.folder-item.selected {
  background-color: #ecf5ff;
  color: #409eff;
}

.folder-item span {
  flex: 1;
  font-size: 14px;
}

.my-shares-view {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.share-dialog-content {
  padding: 10px 0;
}

.share-info {
  margin-bottom: 20px;
}

.share-file-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.share-result {
  margin-top: 20px;
}

.share-link-box {
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 4px;
}

.share-link-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.share-link-item:last-child {
  margin-bottom: 0;
}

.share-link-item .label {
  font-size: 14px;
  color: #606266;
  min-width: 70px;
}

/* VIP状态提醒栏样式 */
.vip-alert {
  margin-bottom: 20px;
  border-radius: 8px;
}

.vip-alert :deep(.el-alert__content) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

/* 拖拽上传样式 */
.file-list-container {
  position: relative;
}

.drag-upload-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s;
}

.file-list-container:has(.drag-upload-overlay) .drag-upload-overlay {
   /* 只有当 overlay 存在时才应用样式，实际上通过 v-if 控制 */
}

/* 实际上因为 v-if，我们需要对 .drag-upload-overlay 本身应用样式 */
/* 当 v-if 为 true 时，覆盖整个屏幕 */
.drag-upload-overlay {
  pointer-events: none; /* 默认不拦截，JS中判断拖拽类型后设为 auto */
  opacity: 1;
}

.drag-upload-content {
  text-align: center;
  border: 3px dashed var(--primary-color);
  border-radius: var(--radius-large);
  padding: 60px 100px;
  background: var(--primary-lighter);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(251, 114, 153, 0.4); }
  70% { transform: scale(1.05); box-shadow: 0 0 0 20px rgba(251, 114, 153, 0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(251, 114, 153, 0); }
}

.drag-upload-content p {
  margin-top: 24px;
  font-size: 24px;
  color: var(--primary-color);
  font-weight: 600;
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: var(--bg-white);
  border: none;
  border-radius: var(--radius-medium);
  box-shadow: var(--shadow-lg);
  padding: 8px 0;
  z-index: 9999;
  min-width: 160px;
  overflow: hidden;
  /* 添加平滑过渡动画 */
  opacity: 0;
  transform: scale(0.95) translateY(-5px);
  animation: contextMenuFadeIn 0.15s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  /* 确保在移动设备上也能正常显示 */
  -webkit-overflow-scrolling: touch;
  /* 防止文本选择 */
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

/* 右键菜单淡入动画 */
@keyframes contextMenuFadeIn {
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 14px;
  color: var(--text-primary);
  /* 防止文本选择 */
  user-select: none;
  -webkit-user-select: none;
}

.menu-item:hover {
  background-color: var(--primary-lighter);
  color: var(--primary-color);
  /* 添加轻微的缩放效果 */
  transform: translateX(2px);
}

.menu-item:active {
  transform: translateX(2px) scale(0.98);
}

.menu-item.danger {
  color: var(--danger-color);
}

.menu-item.danger:hover {
  background-color: var(--danger-light);
  color: var(--danger-color);
}

.menu-divider {
  height: 1px;
  background-color: var(--bg-gray-200);
  margin: 4px 0;
}

/* 文件缩略图样式 */
.file-thumbnail {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border-radius: 6px;
  margin-right: 12px;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
</style>
