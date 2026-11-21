<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <img v-if="brand.logoUrl" class="brand-logo" :src="brand.logoUrl" alt="logo" />
        <span v-else class="brand-icon">{{ brand.brandChar }}</span>
        <span class="brand-name">{{ brand.shortName }}管理端</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="transparent"
        text-color="#d4c5a9"
        active-text-color="#e8d5a3"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>首页概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/user">
          <el-icon><User /></el-icon>
          <span>用户与角色管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/staff">
          <el-icon><Avatar /></el-icon>
          <span>师资信息管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/category">
          <el-icon><Collection /></el-icon>
          <span>课程产品配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/class">
          <el-icon><School /></el-icon>
          <span>班级管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/schedule">
          <el-icon><Calendar /></el-icon>
          <span>课程排班管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/enrollment">
          <el-icon><List /></el-icon>
          <span>报名管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/payment">
          <el-icon><Money /></el-icon>
          <span>订单与财务</span>
        </el-menu-item>
        <el-menu-item index="/admin/visitor">
          <el-icon><UserFilled /></el-icon>
          <span>游客试听预约</span>
        </el-menu-item>
        <el-menu-item index="/admin/exam">
          <el-icon><Trophy /></el-icon>
          <span>考级比赛管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/statistics">
          <el-icon><TrendCharts /></el-icon>
          <span>运营数据统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/knowledge">
          <el-icon><Reading /></el-icon>
          <span>知识库与AI模型</span>
        </el-menu-item>
        <el-menu-item index="/admin/system">
          <el-icon><Setting /></el-icon>
          <span>系统基础设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧容器 -->
    <el-container>
      <el-header class="header">
        <div class="header-title">运营管理端</div>
        <div class="header-user">
          <NotificationBell />
          <span>{{ userStore.realName || userStore.username }}</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <!-- 桌宠：AI助手（管理端人设·程程） -->
    <DeskPet scope="ADMIN" />
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useBrandStore } from '@/store/brand'
import NotificationBell from '@/components/NotificationBell.vue'
import DeskPet from '@/components/DeskPet.vue'

const router = useRouter()
const userStore = useUserStore()
const brand = useBrandStore()
brand.load()

const handleLogout = () => {
  userStore.logout()
  router.push('/portal')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.sidebar {
  /* 飞天壁画纹理：深茶褐罩层 80%，壁画飘带轮廓隐约可见 */
  background:
    linear-gradient(180deg, rgba(201,168,76,0.06) 0%, transparent 30%),
    radial-gradient(circle at 50% 12%, rgba(200,154,46,0.10) 0%, transparent 40%),
    linear-gradient(rgba(44, 24, 16, 0.80), rgba(44, 24, 16, 0.80)),
    url('/texture/bg_feitian.jpg') center / cover no-repeat;
  border-right: 2px solid rgba(201, 168, 76, 0.35);
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #e8d5a3;
  font-weight: bold;
  font-size: 15px;
  padding: 0 10px;
  border-bottom: 1px solid rgba(201, 168, 76, 0.25);
  position: relative;
}
/* logo 下方朱砂金珠飘带分隔 */
.logo::after {
  content: '';
  position: absolute;
  bottom: -1px; left: 50%;
  transform: translateX(-50%);
  width: 60%; height: 2px;
  background: linear-gradient(90deg, transparent, var(--dh-cinnabar, #c13a2a), var(--dh-earth-yellow, #c89a2e), var(--dh-cinnabar, #c13a2a), transparent);
}
.brand-logo {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.brand-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #a62a20, #c9a84c);
  color: #f5f0e6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  font-family: var(--dh-font-display);
}
.brand-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: var(--dh-font-display);
  letter-spacing: 1px;
}
.el-menu {
  border-right: none;
}
.sidebar :deep(.el-menu-item.is-active) {
  background: rgba(201, 168, 76, 0.16);
  border-right: 3px solid #c9a84c;
}
.sidebar :deep(.el-menu-item:hover) {
  background: rgba(201, 168, 76, 0.1);
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  /* 金线山水壁画：米白罩层 60%，轮廓清晰可见；加深底边与阴影与内容区界限分明 */
  background:
    linear-gradient(rgba(250, 247, 240, 0.60), rgba(250, 247, 240, 0.60)),
    url('/texture/bg_header.jpg') center / cover no-repeat;
  box-shadow: 0 3px 10px rgba(44, 24, 16, 0.15);
  border-bottom: 2px solid rgba(201, 168, 76, 0.55);
}
.header-title {
  font-size: 18px;
  font-weight: 600;
  font-family: var(--dh-font-display);
  color: #a62a20;
  letter-spacing: 2px;
}
.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.main-content {
  /* 丝路山水壁画纹理：米白罩层 90% + 壁画隐约可见 */
  background:
    linear-gradient(rgba(245, 240, 230, 0.90), rgba(245, 240, 230, 0.90)),
    url('/texture/bg_silk.jpg') center / cover fixed no-repeat;
  background-color: #f5f0e6;
}
</style>
