import request from '@/utils/request'

export const createShare = (data) => {
  return request({
    url: '/share/create',
    method: 'post',
    data
  })
}

export const getMyShares = (params) => {
  return request({
    url: '/share/myShares',
    method: 'get',
    params
  })
}

export const getShareInfo = (shareCode) => {
  return request({
    url: `/share/info/${shareCode}`,
    method: 'get'
  })
}

export const accessShare = (data) => {
  return request({
    url: '/share/access',
    method: 'post',
    data
  })
}

export const cancelShare = (shareId) => {
  return request({
    url: `/share/cancel/${shareId}`,
    method: 'post'
  })
}

export const recordDownload = (shareCode) => {
  return request({
    url: `/share/download/${shareCode}`,
    method: 'post'
  })
}
