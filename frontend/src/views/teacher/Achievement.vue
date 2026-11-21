<template>
  <div v-loading="loading">
    <PageHeader title="教学成果统计" subtitle="数据实时来自课表、考勤、作业与考级比赛记录" />

    <el-row :gutter="16">
      <el-col :xs="12" :sm="8" :md="4" v-for="c in cards" :key="c.label">
        <el-tooltip placement="top" :show-after="200">
          <template #content>
            <div style="max-width:240px;line-height:1.7">{{ c.tip }}<br/>点击进入「{{ c.targetName }}」</div>
          </template>
          <el-card shadow="hover" class="stat-card jump-card" @click="go(c.to)">
            <div class="num" :style="{ color: c.color }">{{ c.value }}</div>
            <div class="label">{{ c.label }}</div>
          </el-card>
        </el-tooltip>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span class="head">各班考级通过率<el-button link type="primary" size="small" @click="go('/teacher/exam')">查看详情 →</el-button></span></template>
          <el-table :data="examList" stripe size="small" class="jump-table" @row-click="go('/teacher/exam')">
            <el-table-column prop="className" label="班级" />
            <el-table-column prop="examCount" label="考级人数" width="100" />
            <el-table-column prop="passed" label="通过人数" width="100" />
            <el-table-column label="通过率">
              <template #default="{ row }">{{ row.passRate }}%</template>
            </el-table-column>
            <template #empty>暂无考级记录</template>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span class="head">各班比赛获奖<el-button link type="primary" size="small" @click="go('/teacher/exam')">查看详情 →</el-button></span></template>
          <el-table :data="awardList" stripe size="small" class="jump-table" @row-click="go('/teacher/exam')">
            <el-table-column prop="className" label="班级" />
            <el-table-column prop="competitionCount" label="参赛人数" width="100" />
            <el-table-column prop="awardCount" label="获奖人数" width="100" />
            <el-table-column label="获奖率">
              <template #default="{ row }">
                {{ row.competitionCount ? ((row.awardCount / row.competitionCount) * 100).toFixed(1) : 0 }}%
              </template>
            </el-table-column>
            <template #empty>暂无比赛记录</template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top:16px">
      <template #header><span class="head">作业完成情况<el-button link type="primary" size="small" @click="go('/teacher/homework')">前往作业管理 →</el-button></span></template>
      <el-progress
        :percentage="hw.completionRate || 0"
        :stroke-width="18"
        text-inside
        striped
        striped-flow
      />
      <div class="hw-info">
        应提交 <b>{{ hw.homeworkTotal || 0 }}</b> 份，已提交 <b>{{ hw.homeworkSubmitted || 0 }}</b> 份
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAchievementStats,
  getExamPassRateByClass,
  getCompetitionAwardsByClass,
  getHomeworkCompletionRate
} from '@/api/teacher'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const go = (path) => router.push(path)

const loading = ref(false)
const stats = ref({})
const examList = ref([])
const awardList = ref([])
const hw = ref({})

const cards = computed(() => [
  { label: '在读学员', value: stats.value.totalStudents ?? 0, color: '#1685a9', tip: '我所带班级中的在读学员人数（按学员去重）', to: '/teacher/progress', targetName: '学员进度跟踪' },
  { label: '考级人数', value: stats.value.examCount ?? 0, color: '#2c9678', tip: '我推荐/所带学员报名考级的累计人次', to: '/teacher/exam', targetName: '考级与比赛' },
  { label: '考级通过', value: stats.value.examPassed ?? 0, color: '#2c9678', tip: '考级结果为“通过”的学员人次', to: '/teacher/exam', targetName: '考级与比赛' },
  { label: '考级通过率', value: `${stats.value.examPassRate ?? 0}%`, color: '#c89a2e', tip: '考级通过人次 ÷ 考级总人次', to: '/teacher/exam', targetName: '考级与比赛' },
  { label: '比赛获奖', value: stats.value.awardCount ?? 0, color: '#c13a2a', tip: '参加比赛并获得奖项的学员人次', to: '/teacher/exam', targetName: '考级与比赛' },
  { label: '作业完成率', value: `${stats.value.homeworkCompletionRate ?? 0}%`, color: '#9B59B6', tip: '学员已提交份数 ÷ 应提交总份数', to: '/teacher/homework', targetName: '作业管理' }
])

onMounted(async () => {
  loading.value = true
  try {
    const [s, e, a, h] = await Promise.all([
      getAchievementStats(),
      getExamPassRateByClass(),
      getCompetitionAwardsByClass(),
      getHomeworkCompletionRate()
    ])
    stats.value = s.data || {}
    examList.value = e.data || []
    awardList.value = a.data || []
    hw.value = h.data || {}
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card { text-align: center; margin-bottom: 12px; }
.jump-card { cursor: pointer; transition: transform .15s ease; }
.jump-card:hover { transform: translateY(-2px); }
.jump-table :deep(.el-table__row) { cursor: pointer; }
.num { font-size: 26px; font-weight: 800; line-height: 1.2; }
.label { color: #6b5a4a; font-size: 13px; margin-top: 4px; }
.head { font-weight: 600; display: flex; align-items: center; justify-content: space-between; }
.hw-info { color: #6b5a4a; font-size: 14px; margin-top: 10px; }
</style>
