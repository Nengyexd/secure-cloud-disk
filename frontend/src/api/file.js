import request from '@/utils/request'

export const uploadFile = (formData, onProgress) => {
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percentCompleted)
      }
    }
  })
}

export const getFileList = (params) => {
  return request({
    url: '/file/list',
    method: 'get',
    params
  })
}

export const createFolder = (data) => {
  return request({
    url: '/file/createFolder',
    method: 'post',
    data
  })
}

export const renameFile = (data) => {
  return request({
    url: '/file/rename',
    method: 'post',
    data
  })
}

export const deleteFiles = (data) => {
  return request({
    url: '/file/delete',
    method: 'post',
    data
  })
}

export const getFileDownloadInfo = (fileId) => {
  return request({
    url: `/file/downloadInfo/${fileId}`,
    method: 'get',
    timeout: 60000  // 设置1分钟超时
  })
}

export const downloadFile = (fileId) => {
  return request({
    url: `/file/download/${fileId}`,
    method: 'get',
    responseType: 'arraybuffer',
    timeout: 120000,  // 设置2分钟超时，防止大文件下载超时
    headers: {
      'X-Requested-With': 'XMLHttpRequest',  // 防止IDM等下载管理器拦截
      'Cache-Control': 'no-cache'
    },
    // 添加时间戳防止缓存
    params: {
      t: Date.now()
    }
  })
}

// 预览文件专用接口（避免被下载管理器拦截）
export const previewFile = (fileId) => {
  return request({
    url: `/file/preview/${fileId}`,
    method: 'get',
    responseType: 'arraybuffer',
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
      'Cache-Control': 'no-cache'
    },
    params: {
      t: Date.now()
    }
  })
}

export const getRecycleBinList = (params) => {
  return request({
    url: '/file/recycleBin/list',
    method: 'get',
    params
  })
}

export const restoreFiles = (data) => {
  return request({
    url: '/file/recycleBin/restore',
    method: 'post',
    data
  })
}

export const permanentlyDeleteFiles = (data) => {
  return request({
    url: '/file/recycleBin/delete',
    method: 'post',
    data
  })
}

export const moveFiles = (data) => {
  return request({
    url: '/file/move',
    method: 'post',
    data
  })
}

/**
 * 批量下载文件
 */
export const batchDownload = (fileIds) => {
  return request({
    url: '/file/batch-download',
    method: 'post',
    data: fileIds,
    responseType: 'blob',
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 获取文件缩略图
 */
export const getThumbnail = (fileId) => {
  return request({
    url: `/file/thumbnail/${fileId}`,
    method: 'get',
    responseType: 'arraybuffer',
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
      'Cache-Control': 'public, max-age=3600'
    }
  })
}

/**
 * 获取最近上传的文件
 */
export const getRecentUploads = (params) => {
  return request({
    url: '/file/recent-uploads',
    method: 'get',
    params
  })
}
