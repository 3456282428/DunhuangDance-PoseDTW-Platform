<template>
  <div>
    <PageHeader title="我的预约" subtitle="请假 / 缓考 / 补考 / 休学 / 其他申请">
      <template #actions>
        <el-button type="primary" @click="openApply">
          <el-icon style="margin-right:4px"><Plus /></el-icon>发起申请
        </el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-table :data="requests" stripe v-loading="loading">
        <el-table-column prop="requestId" label="编号" width="70" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.requestType)">{{ row.requestType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="课程" min-width="140" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="requestDate" label="申请日期" width="120" />
        <el-table-column prop="reason" label="理由" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replyRemark" label="处理意见" min-width="140" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && !requests.length" description="暂无申请记录，点击右上角发起申请" />
    </el-card>

    <!-- 发起申请弹窗 -->
    <el-dialog v-model="applyVisible" title="发起申请" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="申请类型" required>
          <el-select v-model="form.requestType" placeholder="请选择类型" style="width:100%">
            <el-option label="请假" value="请假" />
            <el-option label="缓考" value="缓考" />
            <el-option label="补考" value="补考" />
            <el-option label="休学" value="休学" />
            <el-option label="其他事项" value="其他事项" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择课程" required>
          <el-select v-model="form.classId" placeholder="请选择课程" style="width:100%">
            <el-option v-for="c in courses" :key="c.classId" :label="c.className" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请日期">
          <el-date-picker v-model="form.requestDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="申请理由" required>
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请填写申请理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRequest">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyCourses, submitStudentRequest, getMyRequests } from '@/api/student'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const requests = ref([])
const courses = ref([])
const loading = ref(false)
const applyVisible = ref(false)
const submitting = ref(false)
const form = ref({ requestType: '', classId: null, requestDate: '', reason: '' })

const loadRequests = async () => {
  loading.value = true
  try {
    const res = await getMyRequests()
    requests.value = res.data || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const loadCourses = async () => {
  try {
    const res = await getMyCourses()
    courses.value = res.data || []
  } catch (e) {}
}

const openApply = () => {
  form.value = { requestType: '', classId: null, requestDate: '', reason: '' }
  applyVisible.value = true
}

const submitRequest = async () => {
  if (!form.value.requestType) { ElMessage.warning('请选择申请类型'); return }
  if (!form.value.classId) { ElMessage.warning('请选择课程'); return }
  if (!form.value.reason) { ElMessage.warning('请填写申请理由'); return }
  submitting.value = true
  try {
    await submitStudentRequest(form.value)
    ElMessage.success('申请已提交，等待教师处理')
    applyVisible.value = false
    loadRequests()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

const typeColor = (t) => {
  const map = { 请假: 'warning', 缓考: 'primary', 补考: 'danger', 休学: 'info', 其他事项: 'success' }
  return map[t] || 'primary'
}
const statusColor = (s) => (s === '已批准' ? 'success' : s === '已拒绝' ? 'danger' : 'warning')

onMounted(() => {
  loadRequests()
  loadCourses()
})
</script>
