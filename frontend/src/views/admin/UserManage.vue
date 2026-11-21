<template>
  <div>
    <PageHeader title="用户管理" subtitle="管理系统所有账号">
      <template #actions>
        <el-button type="primary" @click="openDialog()">新增用户</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" style="margin-bottom:14px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/姓名/手机号" clearable style="width:200px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width:130px">
            <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width:120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="userList" stripe v-loading="loading">
        <el-table-column prop="userId" label="编号" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleType(row.role)">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button v-if="row.role === 'STUDENT' || row.role === 'TEACHER'" size="small" type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.userId ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.userId" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item v-if="!form.userId" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
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
import { ref, onMounted } from 'vue'
import { getUserList, createUser, updateUser, deleteUser, resetUserPassword } from '@/api/admin'
import PageHeader from '@/components/PageHeader.vue'
import Pagination from '@/components/Pagination.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const roles = [
  { label: '管理员', value: 'ADMIN' },
  { label: '教师', value: 'TEACHER' },
  { label: '学员', value: 'STUDENT' },
  { label: '游客', value: 'VISITOR' }
]
const roleLabel = r => roles.find(x => x.value === r)?.label
  || ({ ACADEMIC: '教务', FINANCE: '财务' }[r]) || r || '—'
const roleType = r => ({ ADMIN: 'danger', TEACHER: 'warning', STUDENT: 'success', VISITOR: 'info' }[r] || 'info')

const userList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const query = ref({ keyword: '', role: '', status: null })
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ userId: null, username: '', password: '', realName: '', phone: '', role: 'STUDENT', status: 1 })
const form = ref(emptyForm())
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '初始密码至少6位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      pageNum: pageNum.value, pageSize: pageSize.value,
      keyword: query.value.keyword || undefined,
      role: query.value.role || undefined,
      status: query.value.status === 0 || query.value.status ? query.value.status : undefined
    })
    userList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => { pageNum.value = 1; fetchData() }
const resetQuery = () => { query.value = { keyword: '', role: '', status: null }; search() }

const openDialog = (row) => {
  form.value = row
    ? { userId: row.userId, username: row.username, password: '', realName: row.realName, phone: row.phone, role: row.role, status: row.status }
    : emptyForm()
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.value.userId) {
      await updateUser(form.value.userId, form.value)
      ElMessage.success('用户已更新')
    } else {
      await createUser(form.value)
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, '提示', { type: 'warning' })
    await deleteUser(row.userId)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

const handleResetPwd = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `将为${row.realName || row.username}（${roleLabel(row.role)}）重置登录密码`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputValue: '123456',
        inputPlaceholder: '输入新密码（至少6位）',
        inputValidator: (v) => (v && v.length >= 6) || '新密码至少6位'
      }
    )
    await resetUserPassword(row.userId, value)
    ElMessage.success('密码已重置')
  } catch (e) {}
}

onMounted(fetchData)
</script>
