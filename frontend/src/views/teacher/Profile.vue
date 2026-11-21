<template>
  <div>
    <PageHeader title="个人授课主页" subtitle="我的档案与所带班级">
      <template #actions>
        <el-button :icon="Lock" @click="openPwdDialog">修改密码</el-button>
        <el-button type="primary" :icon="Edit" @click="openEdit">修改个人信息</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header><span class="head">教师信息</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ profile.name }}</el-descriptions-item>
            <el-descriptions-item label="工号">{{ profile.teacherNo }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ profile.gender }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ profile.major }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ profile.phone }}</el-descriptions-item>
            <el-descriptions-item label="入职日期">{{ profile.hireDate }}</el-descriptions-item>
            <el-descriptions-item label="授课班级">{{ profile.classCount }} 个</el-descriptions-item>
            <el-descriptions-item label="累计课时">{{ Number(profile.totalHours || 0).toFixed(1) }} 课时</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header><span class="head">我的班级（{{ classes.length }}）</span></template>
          <el-table :data="classes" stripe>
            <el-table-column prop="classId" label="编号" width="60" />
            <el-table-column prop="className" label="班级名称" min-width="130" />
            <el-table-column prop="categoryName" label="课程类别" min-width="100" />
            <el-table-column prop="currentStudents" label="人数" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改信息弹窗 -->
    <el-dialog v-model="editVisible" title="修改个人信息" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom:16px"
        title="姓名、工号等由管理员维护，此处可修改性别、专业与联系电话" />
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" disabled />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" placeholder="如：钢琴" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11位手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="440px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Edit, Lock } from '@element-plus/icons-vue'
import { getTeacherProfile, updateTeacherProfile, getTeacherClasses } from '@/api/teacher'
import { changePassword } from '@/api/auth'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const profile = ref({})
const classes = ref([])
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = ref({})

const rules = {
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号', trigger: 'blur' }]
}

const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdRef = ref(null)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少6位', trigger: 'blur' }],
  confirmPassword: [{
    required: true,
    validator: (rule, value, cb) => {
      if (!value) return cb(new Error('请再次输入新密码'))
      if (value !== pwdForm.value.newPassword) return cb(new Error('两次输入的密码不一致'))
      cb()
    },
    trigger: 'blur'
  }]
}

const openPwdDialog = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdVisible.value = true
}

const savePassword = async () => {
  await pwdRef.value.validate()
  pwdSaving.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，下次登录请使用新密码')
    pwdVisible.value = false
  } finally {
    pwdSaving.value = false
  }
}

const load = async () => {
  const [p, c] = await Promise.all([getTeacherProfile(), getTeacherClasses()])
  profile.value = p.data || {}
  classes.value = c.data || []
}

const openEdit = () => {
  form.value = { ...profile.value }
  editVisible.value = true
}

const save = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await updateTeacherProfile({
      gender: form.value.gender,
      major: form.value.major,
      phone: form.value.phone
    })
    ElMessage.success('个人信息已更新')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head { font-weight: 600; }
</style>
