<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="brand">
        <img v-if="brand.logoUrl" class="brand-logo" :src="brand.logoUrl" alt="logo" />
        <span v-else class="brand-icon">{{ brand.brandChar }}</span>
        <h2 class="login-title">{{ brand.brandName }}</h2>
      </div>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button type="primary" native-type="submit" style="width:100%" :loading="loading">
          登录
        </el-button>
      </el-form>
      <div class="login-footer">
        <span>还没有账号？</span>
        <el-link type="primary" @click="goPortal">游客浏览课程</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { useBrandStore } from '@/store/brand'

const router = useRouter()
const userStore = useUserStore()
const brand = useBrandStore()
brand.load()
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })

const goPortal = () => {
  router.push('/portal')
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(loginForm)
    userStore.setLoginData(res.data)
    ElMessage.success('登录成功')
    // 根据角色跳转
    if (res.data.role === 'ADMIN') {
      router.push('/admin/dashboard')
    } else if (res.data.role === 'TEACHER') {
      router.push('/teacher/dashboard')
    } else if (res.data.role === 'STUDENT') {
      router.push('/student/courses')
    } else {
      router.push('/')
    }
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  /* 敦煌壁画背景（AI 生成：米纸底 + 朱砂/石青/沙金飘带 + 藻井纹样） */
  background: url('/images/dunhuang_bg.jpg') center / cover no-repeat;
  position: relative;
}
.login-card {
  width: 400px;
  border-radius: 14px;
  background-color: rgba(250, 247, 240, 0.96);
  border: 1px solid #d4c5a9;
  box-shadow: 0 8px 32px rgba(44, 24, 16, 0.25);
  backdrop-filter: blur(2px);
}
.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 26px;
}
.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  object-fit: cover;
}
.brand-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #a62a20, #c9a84c);
  color: #f5f0e6;
  font-size: 20px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--dh-font-display);
}
.login-title {
  margin: 0;
  color: #2c1810;
  font-size: 24px;
  font-family: var(--dh-font-display);
  font-weight: bold;
  letter-spacing: 2px;
}
.login-footer {
  text-align: center;
  margin-top: 16px;
  color: #6b5a4a;
  font-size: 14px;
}
</style>
