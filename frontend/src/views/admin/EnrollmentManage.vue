<template>
  <div>
    <PageHeader title="报名管理" subtitle="学员报名与分班（学号为登录账号）">
      <template #actions>
        <el-button type="primary" @click="openDialog()">新增报名</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" style="margin-bottom:14px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="学号/姓名">
          <el-input v-model="query.studentName" placeholder="学号或姓名" clearable style="width:180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部班级" clearable filterable style="width:180px">
            <el-option v-for="c in classList" :key="c.classId" :label="c.className" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width:120px">
            <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="enrollmentList" stripe v-loading="loading">
        <el-table-column prop="enrollmentId" label="报名ID" width="80" />
        <el-table-column label="学号（账号）" width="130">
          <template #default="{ row }">
            <span style="font-weight:600;color:#1685a9">{{ row.studentUsername || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentName" label="学员" width="100" />
        <el-table-column prop="className" label="班级" min-width="140" show-overflow-tooltip />
        <el-table-column prop="enrollDate" label="报名日期" width="120" />
        <el-table-column label="缴费金额" width="100">
          <template #default="{ row }">¥{{ row.tuitionAmount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-select :model-value="row.status" size="small" style="width:100px" @change="v => changeStatus(row, v)">
              <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
            </el-select>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @pagination="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增报名" width="480px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="学员" prop="studentId">
          <el-select v-model="form.studentId" placeholder="按学号/姓名搜索选择" filterable style="width:100%">
            <el-option v-for="s in students" :key="s.studentId"
              :label="(s.username ? s.username + ' ' : '') + s.name" :value="s.studentId">
              <span style="float:left">{{ s.username || '未分配账号' }} {{ s.name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" filterable style="width:100%" @change="onClassChange">
            <el-option v-for="c in classList" :key="c.classId" :label="c.className" :value="c.classId" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名日期" prop="enrollDate">
          <el-date-picker v-model="form.enrollDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="缴费金额">
          <el-input-number v-model="form.tuitionAmount" :min="0" :precision="2" style="width:100%" />
          <div style="color:#6b5a4a;font-size:12px">选择班级后自动带出学费，可修改</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getEnrollmentList, createEnrollment, updateEnrollmentStatus, deleteEnrollment,
  getStudentOptions, getClassList
} from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusList = ['在读', '退学', '结课']
const statusType = s => ({ '在读': 'success', '结课': 'info', '退学': 'danger' }[s] || 'warning')

const enrollmentList = ref([])
const students = ref([])
const classList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const query = reactive({ studentName: '', classId: null, status: '' })
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ studentId: null, classId: null, enrollDate: new Date().toISOString().slice(0, 10), tuitionAmount: 0, status: '在读' })
const form = ref(emptyForm())
const rules = {
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  enrollDate: [{ required: true, message: '请选择报名日期', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getEnrollmentList({
      pageNum: pageNum.value, pageSize: pageSize.value,
      studentName: query.studentName || undefined,
      classId: query.classId || undefined,
      status: query.status || undefined
    })
    enrollmentList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { pageNum.value = 1; fetchData() }
const resetQuery = () => { query.studentName = ''; query.classId = null; query.status = ''; search() }

const loadOptions = async () => {
  const [s, c] = await Promise.all([getStudentOptions(), getClassList({ pageNum: 1, pageSize: 500 })])
  students.value = s.data || []
  classList.value = c.data.rows || c.data || []
}

const onClassChange = (classId) => {
  const c = classList.value.find(x => x.classId === classId)
  if (c && c.tuition != null) form.value.tuitionAmount = Number(c.tuition)
}

const openDialog = () => {
  form.value = emptyForm()
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await createEnrollment({ ...form.value })
    ElMessage.success('报名已创建')
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

const changeStatus = async (row, status) => {
  await updateEnrollmentStatus(row.enrollmentId, status)
  ElMessage.success('状态已更新')
  fetchData()
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除 ${row.studentName} 的这条报名记录吗？`, '提示', { type: 'warning' })
    await deleteEnrollment(row.enrollmentId)
    ElMessage.success('已删除')
    fetchData()
  } catch (e) {}
}

onMounted(() => { fetchData(); loadOptions() })
</script>
