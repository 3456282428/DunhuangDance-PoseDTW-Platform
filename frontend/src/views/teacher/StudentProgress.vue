<template>
  <div>
    <PageHeader title="学员进度跟踪" subtitle="查看班级学员学习进度" />
    <el-select v-model="selectedClassId" placeholder="选择班级" @change="fetchData">
      <el-option v-for="c in classes" :key="c.classId" :label="c.className" :value="c.classId" />
    </el-select>

    <el-table :data="students" stripe style="margin-top:20px">
      <el-table-column prop="studentName" label="学员" />
      <el-table-column prop="attendanceRate" label="出勤率" />
      <el-table-column prop="homeworkRate" label="作业完成率" />
      <el-table-column prop="examPassRate" label="考级通过率" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTeacherClasses, getClassStudentsProgress } from '@/api/teacher'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const classes = ref([])
const selectedClassId = ref(null)
const students = ref([])

onMounted(async () => {
  const res = await getTeacherClasses()
  classes.value = res.data || []
})

const fetchData = async () => {
  if (!selectedClassId.value) return
  const res = await getClassStudentsProgress(selectedClassId.value)
  students.value = res.data || []
}

const viewDetail = (row) => {
  ElMessage.info(`学员详情：${row.studentName}`)
}
</script>
