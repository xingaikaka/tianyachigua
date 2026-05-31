import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid, totpCode) {
  const data = {
    username,
    password,
    code,
    uuid,
    totpCode
  }
  return request({
    url: '/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// TOTP相关接口
export function getTotpQrCode() {
  return request({
    url: '/system/user/profile/totp/qrcode',
    method: 'get'
  })
}

export function bindTotp(data) {
  return request({
    url: '/system/user/profile/totp/bind',
    method: 'post',
    data
  })
}

export function unbindTotp(data) {
  return request({
    url: '/system/user/profile/totp/unbind',
    method: 'delete',
    data
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}