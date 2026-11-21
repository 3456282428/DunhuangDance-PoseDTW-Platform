import { defineStore } from 'pinia'
import { getPortalOrg } from '@/api/portal'

// 全局品牌信息：管理员在“系统基础设置-机构信息”中维护，全站（门户/登录/各工作台）统一生效
export const useBrandStore = defineStore('brand', {
  state: () => ({
    loaded: false,
    org: {}
  }),
  getters: {
    // 完整机构名称，缺省回退
    brandName: (state) => state.org?.name || '艺术培训管理系统',
    // 侧边栏/Logo 处的短名：优先取管理员配置的品牌简称，否则由全称去后缀
    shortName() {
      if (this.org?.shortName) return this.org.shortName
      const n = this.org?.name
      if (!n) return '艺培学堂'
      return n.replace(/艺术培训中心|培训中心|艺术中心/g, '') || n
    },
    // Logo 占位字（未上传 Logo 时显示的单字）
    brandChar: (state) => {
      const s = state.org?.shortName
      if (s) return s.slice(0, 1)
      const n = state.org?.name
      return n ? n.slice(0, 1) : '艺'
    },
    logoUrl: (state) => state.org?.logoUrl || ''
  },
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return
      try {
        const res = await getPortalOrg()
        this.org = res.data || {}
        if (this.org.name) document.title = `${this.org.name}管理系统`
      } catch (e) {
        // 后端未启动时保持默认文案，不影响页面渲染
      } finally {
        this.loaded = true
      }
    },
    // 管理员保存机构信息后即时刷新
    refresh() {
      return this.load(true)
    }
  }
})
