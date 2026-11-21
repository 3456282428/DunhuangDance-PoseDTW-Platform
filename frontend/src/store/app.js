import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,   // 侧边栏是否折叠
    loading: false             // 全局加载状态
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    setLoading(status) {
      this.loading = status
    }
  }
})
