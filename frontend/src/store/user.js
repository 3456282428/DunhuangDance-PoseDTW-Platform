import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') || ''
  }),
  actions: {
    /**
     * 登录成功后保存用户信息
     * @param {Object} data - { token, username, realName, role }
     */
    setLoginData(data) {
      this.token = data.token
      this.username = data.username
      this.realName = data.realName
      this.role = data.role
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('realName', data.realName)
      localStorage.setItem('role', data.role)
    },
    /**
     * 退出登录，清空状态和本地存储
     */
    logout() {
      this.token = ''
      this.username = ''
      this.realName = ''
      this.role = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('realName')
      localStorage.removeItem('role')
    }
  }
})
