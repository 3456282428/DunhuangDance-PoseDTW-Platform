<template>
  <div>
    <PageHeader title="预约与申请处理" subtitle="学员请假 / 缓考 / 补考 / 休学等申请审批" />

    <el-card shadow="never" style="margin-bottom:14px">
      <div class="filter">
        <el-input v-model="keyword" placeholder="学员姓名" clearable style="width:180px" @keyup.enter="loadRequests" />
        <el-select v-model="typeFilter" placeholder="申请类型" clearable style="width:130px">
          <el-option v-for="t in types" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width:130px">
          <el-option label="待处理" value="待处理" />
          <el-option label="已批准" value="已批准" />
          <el-option label="已拒绝" value="已拒绝" />
        </el-select>
        <el-button type="primary" @click="loadRequests">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
        <div class="spacer"></div>
        <el-tag v-if="pendingCount" type="danger" effect="light">待处理 {{ pendingCount }} 条</el-tag>
      </div>
    </el-card>

    <el-table :data="filtered" stripe v-loading="loading">
      <el-table-column prop="studentName" label="学员" width="90" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="typeColor(row.requestType)" size="small">{{ row.requestType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="className" label="课程" min-width="120" show-overflow-tooltip />
      <el-table-column prop="requestDate" label="申请日期" width="105" />
      <el-table-column prop="reason" label="理由" min-width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="85">
        <template #default="{ row }">
          <el-tag :type="statusColor(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="replyRemark" label="处理意见" min-width="110" show-overflow-tooltip />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <template v-if="row.status === '待处理'">
            <el-button size="small" type="success" @click="handle(row, 'APPROVE')">批准</el-button>
            <el-button size="small" type="danger" @click="handle(row, 'REJECT')">拒绝</el-button>
          </template>
          <span v-else style="color:#a89880;font-size:12px">已处理</span>
        </template>
      </el-table-column>
      <template #empty>暂无符合条件的申请</template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStudentRequests, handleStudentRequest } from '@/api/teacher'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const all = ref([])
const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const types = ['请假', '缓考', '补考', '休学', '其他']

const pendingCount = computed(() => all.value.filter(r => r.status === '待处理').length)

const filtered = computed(() => all.value.filter(r => {
  if (keyword.value && !(r.studentName || '').includes(keyword.value.trim())) return false
  if (typeFilter.value && r.requestType !== typeFilter.value) return false
  if (statusFilter.value && r.status !== statusFilter.value) return false
  return true
}))

const loadRequests = async () => {
  loading.value = true
  try {
    const res = await getStudentRequests()
    all.value = res.data || []
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  keyword.value = ''; typeFilter.value = ''; statusFilter.value = ''
}

const handle = async (row, action) => {
  try {
    const { value } = await ElMessageBox.prompt(
      action === 'APPROVE' ? '请输入批准意见（可选）' : '请输入拒绝原因',
      action === 'APPROVE' ? '批准申请' : '拒绝申请',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '处理意见' }
    )
    await handleStudentRequest(row.requestId, action, value || '')
    ElMessage.success('处理成功')
    loadRequests()
  } catch (e) {}
}

const typeColor = (t) => ({ '请假': 'warning', '缓考': 'primary', '补考': 'success', '休学': 'danger' }[t] || 'info')
const statusColor = (s) => (s === '已批准' ? 'success' : s === '已拒绝' ? 'danger' : 'warning')

onMounted(loadRequests)
</script>

<style scoped>
.filter { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.spacer { flex: 1; }
</style>
