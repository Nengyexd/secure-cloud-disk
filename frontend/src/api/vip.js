import request from '@/utils/request'

export const upgradeToVip = (days) => {
  return request({
    url: '/user/vip/upgrade',
    method: 'post',
    data: { days }
  })
}
