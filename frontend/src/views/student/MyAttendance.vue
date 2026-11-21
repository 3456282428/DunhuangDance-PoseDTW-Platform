<template>
  <div>
    <PageHeader title="完成课时" subtitle="考勤记录与出勤统计" />
    <el-card shadow="never" style="margin-bottom:20px">
      <div v-if="summary" class="summary">
        <span>应到：<b>{{ summary.totalSchedules }}</b> 次</span>
        <el-divider direction="vertical" />
        <span>出勤：<b>{{ summary.attended }}</b> 次</span>
        <el-divider direction="vertical" />
        <span>迟到：<b>{{ summary.late }}</b> 次</span>
        <el-divider direction="vertical" />
        <span>请假：<b>{{ summary.leave }}</b> 次</span>
        <el-divider direction="vertical" />
        <span>缺勤：<b>{{ summary.absent }}</b> 次</span>
        <el-divider direction="vertical" />
        <span>出勤率：<b class="rate">{{ summary.attendanceRate }}%</b></span>
      </div>
    </el-card>
    <el-table :data="attendanceList" stripe v-loading="loading">
      <el-table-column prop="className" label="班级" min-width="140" />
      <el-table-column prop="courseDate" label="日期" width="120" />
      <el-table-column prop="startTime" label="开始时间" width="100" />
      <el-table-column prop="endTime" label="结束时间" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" />
    </el-table>
    <el-empty v-if="!loading && !attendanceList.length" description="暂无考勤记录" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyAttendance, getMyAttendanceSummary } from '@/api/student'
import PageHeader from '@/components/PageHeader.vue'

const attendanceList = ref([])
const summary = ref(null)
const loading = ref(false)

const statusType = (s) => ({ 出勤: 'success', 迟到: 'warning', 缺勤: 'danger', 请假: 'info' }[s] || 'info')

onMounted(async () => {
  loading.value = true
  try {
    const [listRes, summaryRes] = await Promise.all([
      getMyAttendance({ pageNum: 1, pageSize: 200 }),
      getMyAttendanceSummary()
    ])
    attendanceList.value = listRes.data.rows || []
    summary.value = summaryRes.data
  } catch (e) {
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.summary { color: #6b5a4a; font-size: 14px; }
.summary b { color: #2c1810; }
.summary .rate { color: #1685a9; font-size: 16px; }
</style>
