﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div v-loading="loading" class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="hero">
      <div class="hero-text">
        <div class="hero-title">{{ greeting }}，欢迎回来 👋</div>
        <div class="hero-sub">今天是 {{ today }}，艺术培训管理系统运行正常，祝您工作顺利</div>
      </div>
      <div class="hero-deco"><el-icon :size="90"><Sunny /></el-icon></div>
    </div>

    <!-- 核心指标 -->
    <el-row :gutter="16">
      <el-col :xs="12" :sm="6" v-for="c in cards" :key="c.title">
        <div class="stat-card" :style="{ '--accent': c.color }" @click="go(c.to)">
          <div class="stat-icon" :style="{ background: c.bg, color: c.color }">
            <el-icon :size="24"><component :is="c.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value" :class="{ small: String(c.value).length > 6 }" :style="{ color: c.color }">{{ c.value }}</div>
            <div class="stat-title">{{ c.title }}</div>
          </div>
          <el-icon class="stat-go"><ArrowRight /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- 图表第一行：收入构成饼图 + 教师课时柱状图 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="chart-card clickable" @click="go('/admin/payment')">
            <template #header><span class="chart-head">收入构成 <el-button link type="primary" size="small">订单与财务 →</el-button></span></template>
            <div ref="pieRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="chart-card clickable" @click="go('/admin/statistics')">
            <template #header><span class="chart-head">教师授课课时排行 <el-button link type="primary" size="small">运营数据统计 →</el-button></span></template>
            <div ref="teacherRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 月度收入趋势 -->
    <el-card shadow="never" class="chart-card clickable" @click="go('/admin/payment')">
      <template #header><span class="chart-head">月度收入趋势 <el-button link type="primary" size="small">财务统计报表 →</el-button></span></template>
      <div ref="monthRef" class="chart-box-wide"></div>
    </el-card>

    <!-- 授课最多教师 -->
    <div class="top-teacher clickable" @click="go('/admin/statistics')">
      <div class="top-icon"><el-icon :size="26"><Trophy /></el-icon></div>
      <div class="top-info">
        <div class="top-label">本月授课课时最多的教师</div>
        <div class="top-name">{{ d.topTeacherName || '暂无数据' }}</div>
      </div>
      <div class="top-right">
        <span class="top-num">{{ d.topTeacherHours || 0 }}</span><span class="top-unit">课时</span>
        <el-icon class="top-go"><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDashboardOverview, getIncomeStat, getTeacherHours } from '@/api/admin'

const router = useRouter()
const go = (path) => path && router.push(path)

const loading = ref(false)
const d = ref({})

const pieRef = ref(null)
const teacherRef = ref(null)
const monthRef = ref(null)
let pieChart, teacherChart, monthChart

const now = new Date()
const week = ['日', '一', '二', '三', '四', '五', '六']
const today = `${now.getFullYear()}年${now.getMonth()+1}月${now.getDate()}日 星期${week[now.getDay()]}`
const hour = now.getHours()
const greeting = hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好'

const cards = computed(() => [
  { title: '在读学员', icon: 'User', value: d.value.totalStudents ?? 0, color: '#1685a9', bg: 'linear-gradient(135deg,#e6f2f6,#cfe6ee)', to: '/admin/enrollment' },
  { title: '本月新增', icon: 'UserFilled', value: d.value.newStudentsThisMonth ?? 0, color: '#2c9678', bg: 'linear-gradient(135deg,#e7f3ee,#cfe8dc)', to: '/admin/user' },
  { title: '本月收入', icon: 'Money', value: `¥${Number(d.value.monthRevenue || 0).toLocaleString()}`, color: '#c89a2e', bg: 'linear-gradient(135deg,#fbf3df,#f5e6b8)', to: '/admin/payment' },
  { title: '续课率', icon: 'TrendCharts', value: `${(d.value.renewalRate || 0).toFixed(1)}%`, color: '#c13a2a', bg: 'linear-gradient(135deg,#faeae7,#f3cdc6)', to: '/admin/statistics' }
])

const palette = ['#1685a9', '#2c9678', '#c89a2e', '#c13a2a', '#955539', '#5fae9c', '#0eb83a', '#d94028']

async function renderCharts() {
  await nextTick()
  // 收入构成饼图
  try {
    const res = await getIncomeStat('type')
    const data = res.data || {}
    pieChart = pieChart || echarts.init(pieRef.value)
    const cats = data.categories || [], vals = data.values || []
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      legend: { bottom: 0, icon: 'circle' },
      color: palette,
      series: [{
        type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%' },
        data: cats.map((c, i) => ({ name: c, value: Number(vals[i] || 0) }))
      }]
    }, true)
  } catch (e) {}

  // 教师课时横向柱状图
  try {
    const res = await getTeacherHours()
    const list = (res.data && res.data.teachers) || []
    teacherChart = teacherChart || echarts.init(teacherRef.value)
    const sorted = [...list].sort((a, b) => a.totalHours - b.totalHours)
    teacherChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => `${p[0].name}：${Number(p[0].value).toFixed(1)} 课时` },
      grid: { left: 100, right: 40, top: 16, bottom: 20 },
      xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f5' } } },
      yAxis: { type: 'category', data: sorted.map(t => t.displayName || t.teacherName), axisLine: { show: false } },
      series: [{
        type: 'bar', data: sorted.map(t => Number(t.totalHours.toFixed(1))),
        barWidth: 16,
        itemStyle: { borderRadius: [0, 8, 8, 0], color: new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#79b8ff'},{offset:1,color:'#1685a9'}]) },
        label: { show: true, position: 'right', formatter: '{c}', color: '#6b5a4a' }
      }]
    }, true)
  } catch (e) {}

  // 月度收入柱状图
  try {
    const res = await getIncomeStat('month')
    const data = res.data || {}
    monthChart = monthChart || echarts.init(monthRef.value)
    monthChart.setOption({
      tooltip: { trigger: 'axis', formatter: p => `${p[0].name}：¥${Number(p[0].value).toLocaleString()}` },
      grid: { left: 60, right: 30, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.categories || [], axisLine: { lineStyle: { color: '#d4c5a9' } } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f5' } } },
      series: [{
        type: 'bar', data: (data.values || []).map(v => Number(v || 0)),
        barWidth: '45%',
        itemStyle: { borderRadius: [8, 8, 0, 0], color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#2c9678'},{offset:1,color:'#b3e19d'}]) }
      }]
    }, true)
  } catch (e) {}
}

function resize() { pieChart?.resize(); teacherChart?.resize(); monthChart?.resize() }

onMounted(async () => {
  loading.value = true
  try {
    const res = await getDashboardOverview()
    d.value = res.data || {}
  } catch (e) {} finally { loading.value = false }
  renderCharts()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  pieChart?.dispose(); teacherChart?.dispose(); monthChart?.dispose()
})
</script>

<style scoped>
.hero {
  position: relative; overflow: hidden;
  border-radius: 16px; padding: 24px 28px; margin-bottom: 18px;
  background: linear-gradient(120deg, #4b6cb7 0%, #5b86e5 50%, #6aa6f0 100%);
  color: #fff; box-shadow: 0 8px 24px rgba(75,108,183,.28);
}
.hero-title { font-size: 23px; font-weight: 700; }
.hero-sub { margin-top: 8px; font-size: 14px; opacity: .92; }
.hero-deco { position: absolute; right: 30px; top: 50%; transform: translateY(-50%); opacity: .25; }

.stat-card {
  background: var(--dh-card-bg, #faf7f0); border-radius: 14px; padding: 18px; margin-bottom: 18px;
  display: flex; align-items: center; gap: 11px;
  box-shadow: 0 4px 16px rgba(31,45,61,.06);
  border-top: 3px solid var(--accent);
  transition: transform .2s, box-shadow .2s;
  position: relative; cursor: pointer;
  overflow: hidden;
}
/* 统计卡片内部淡纹理 */
.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url('/texture/bg_silk.jpg') center / cover;
  opacity: 0.04;
  pointer-events: none;
  z-index: 0;
}
.stat-card > * { position: relative; z-index: 1; }
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgba(31,45,61,.12); }
.stat-go { position: absolute; right: 12px; top: 12px; color: #a89880; }
.stat-card:hover .stat-go { color: var(--accent); }
.clickable { cursor: pointer; transition: box-shadow .2s, transform .2s; }
.clickable:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(31,45,61,.12); }
.chart-head { font-weight: 600; color: #2c1810; display:flex; align-items:center; justify-content:space-between; }
.stat-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-body { flex: 1; min-width: 0; padding-right: 14px; }
.stat-value { font-size: 19px; font-weight: 800; line-height: 1.1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; letter-spacing: -.5px; font-variant-numeric: tabular-nums; }
.stat-value.small { font-size: 15px; }
.stat-title { color: #6b5a4a; font-size: 13px; margin-top: 4px; }

.chart-card { margin-bottom: 18px; border-radius: 14px; }
.chart-box { height: 300px; }
.chart-box-wide { height: 280px; }

.top-teacher {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(120deg,#fff8e6,#fff 70%);
  border-radius: 14px; padding: 16px 24px;
  border: 1px solid #fbeecb;
}
.top-teacher.clickable { cursor: pointer; }
.top-right { display: flex; align-items: center; }
.top-go { color: #a89880; margin-left: 8px; }
.top-icon { width: 48px; height: 48px; border-radius: 50%; background: #fdf0d3; color: #c89a2e; display:flex; align-items:center; justify-content:center; }
.top-info { flex: 1; }
.top-label { color: #6b5a4a; font-size: 13px; }
.top-name { font-size: 20px; font-weight: 700; color: #2c1810; margin-top: 2px; }
.top-num { font-size: 28px; font-weight: 800; color: #c89a2e; }
.top-unit { font-size: 14px; color: #6b5a4a; margin-left: 4px; }
</style>
