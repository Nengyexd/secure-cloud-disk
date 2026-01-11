import request from '@/utils/request'

/**
 * 添加收藏
 */
export const addFavorite = (fileId) => {
  return request({
    url: `/favorite/add/${fileId}`,
    method: 'post'
  })
}

/**
 * 取消收藏
 */
export const removeFavorite = (fileId) => {
  return request({
    url: `/favorite/remove/${fileId}`,
    method: 'delete'
  })
}

/**
 * 获取我的收藏列表
 */
export const getMyFavorites = () => {
  return request({
    url: '/favorite/my',
    method: 'get'
  })
}

/**
 * 检查文件是否已收藏
 */
export const checkFavorite = (fileId) => {
  return request({
    url: `/favorite/check/${fileId}`,
    method: 'get'
  })
}
