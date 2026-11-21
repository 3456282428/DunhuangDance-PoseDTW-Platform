<template>
  <div>
    <PageHeader title="个人信息" subtitle="我的资料与学习进度">
      <template #actions>
        <el-button @click="openPwdDialog">
          <el-icon style="margin-right:4px"><Lock /></el-icon>修改密码
        </el-button>
        <el-button type="primary" @click="openEdit">
          <el-icon style="margin-right:4px"><Edit /></el-icon>修改个人信息
        </el-button>
      </template>
    </PageHeader>

    <!-- 基本资料卡片 -->
    <el-card shadow="hover" class="profile-card">
      <div class="profile-top">
        <div class="avatar">{{ avatarChar }}</div>
        <div class="profile-basic">
          <div class="name">{{ profile.name || '未填写' }}</div>
          <div class="no">学号：{{ profile.studentNo || '-' }}</div>
          <el-tag size="small" type="success">在读学员</el-tag>
        </div>
      </div>
      <el-divider />
      <el-row :gutter="16">
        <el-col :span="12">
          <div class="info-item"><span class="k">姓名</span><span class="v">{{ profile.name || '-' }}</span></div>
        </el-col>
        <el-col :span="12">
          <div class="info-item"><span class="k">性别</span><span class="v">{{ profile.gender || '-' }}</span></div>
        </el-col>
        <el-col :span="12">
          <div class="info-item"><span class="k">出生日期</span><span class="v">{{ profile.birthDate || '-' }}</span></div>
        </el-col>
        <el-col :span="12">
          <div class="info-item"><span class="k">家长电话</span><span class="v">{{ profile.parentPhone || '-' }}</span></div>
        </el-col>
        <el-col :span="12">
          <div class="info-item"><span class="k">邮箱</span><span class="v">{{ profile.email || '-' }}</span></div>
        </el-col>
        <el-col :span="12">
          <div class="info-item"><span class="k">学号</span><span class="v">{{ profile.studentNo || '-' }}</span></div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 学习进度 -->
    <el-card shadow="hover" class="progress-card">
      <template #header>
        <span class="card-title">学习进度</span>
      </template>
      <el-row :gutter="16" v-if="progress">
        <el-col :span="6" v-for="c in statCards" :key="c.label">
          <el-tooltip placement="top" :show-after="200">
            <template #content>
              <div style="max-width:240px;line-height:1.7">{{ c.tip }}</div>
            </template>
            <div class="stat stat-link" @click="go(c.to)">
              <el-icon class="stat-go"><ArrowRight /></el-icon>
              <div class="num" v-html="c.value"></div>
              <div class="lab">{{ c.label }}</div>
            </div>
          </el-tooltip>
        </el-col>
      </el-row>
      <el-row :gutter="16" v-if="progress" class="progress-second">
        <el-col :span="6">
          <el-tooltip placement="top" :show-after="200">
            <template #content>
              <div style="max-width:240px;line-height:1.7">已完成且成绩通过的考级/比赛数 ÷ 报名总数。点击进入考级与比赛。</div>
            </template>
            <div class="stat stat-link" @click="go('/student/exam')">
              <el-icon class="stat-go"><ArrowRight /></el-icon>
              <div class="num">{{ progress.examPassed }}<span class="sub">/{{ progress.examTotal }}</span></div>
              <div class="lab">考级/比赛通过</div>
            </div>
          </el-tooltip>
        </el-col>
      </el-row>
    </el-card>

    <!-- 修改个人信息弹窗 -->
    <el-dialog v-model="editVisible" title="修改个人信息" width="480px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" placeholder="选择出生日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="家长电话" prop="parentPhone">
          <el-input v-model="form.parentPhone" maxlength="11" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProfile, updateMyProfile, getLearningProgress } from '@/api/student'
import { changePassword } from '@/api/auth'
import PageHeader from '@/components/PageHeader.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const go = (path) => router.push(path)

const profile = ref({})
const progress = ref(null)
const editVisible = ref(false)
const saving = ref(false)
const form = ref({})
const formRef = ref(null)
const formRules = {
  parentPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入11位有效手机号', trigger: 'blur' }]
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

const statCards = computed(() => {
  const p = progress.value || {}
  return [
    {
      label: '在学课程数',
      value: String(p.totalCourses ?? 0),
      to: '/student/attendance',
      tip: '当前处于“在读”状态的报名班级数量。点击查看在学课程列表。'
    },
    {
      label: '已上课时',
      value: `${p.attendedHours ?? 0}<span class="sub">/${p.totalHours ?? 0}</span>`,
      to: '/student/courses',
      tip: '已上课时由教师端考勤记录汇总，分母为所报班级的计划总课时。点击查看我的课表。'
    },
    {
      label: '出勤率',
      value: `${p.attendanceRate ?? 0}%`,
      to: '/student/attendance-records',
      tip: '出勤率 = 出勤（含迟到）节数 ÷ 已考勤节数。点击查看考勤记录明细。'
    },
    {
      label: '作业完成',
      value: `${p.homeworkSubmitted ?? 0}<span class="sub">/${p.homeworkTotal ?? 0}</span>`,
      to: '/student/homework',
      tip: '已提交作业数 ÷ 教师布置作业总数。点击进入我的作业。'
    }
  ]
})

const avatarChar = computed(() => (profile.value.name ? profile.value.name.charAt(0) : '学'))

const loadProfile = async () => {
  try {
    const res = await getMyProfile()
    profile.value = res.data || {}
  } catch (e) {}
}

const loadProgress = async () => {
  try {
    const res = await getLearningProgress()
    progress.value = res.data
  } catch (e) {}
}

const openEdit = () => {
  form.value = {
    name: profile.value.name,
    gender: profile.value.gender,
    birthDate: profile.value.birthDate,
    parentPhone: profile.value.parentPhone,
    email: profile.value.email
  }
  editVisible.value = true
}

const saveProfile = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await updateMyProfile(form.value)
    ElMessage.success('个人信息已更新')
    editVisible.value = false
    loadProfile()
  } catch (e) {
  } finally {
    saving.value = false
  }
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
  } catch (e) {
  } finally {
    pwdSaving.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadProgress()
})
</script>

<style scoped>
.profile-card, .progress-card {
  margin-bottom: 20px;
  border-radius: 10px;
}
.profile-top {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}
.name {
  font-size: 20px;
  font-weight: 600;
  color: #2c1810;
}
.no {
  font-size: 13px;
  color: #6b5a4a;
  margin: 4px 0;
}
.info-item {
  padding: 8px 0;
  display: flex;
  border-bottom: 1px dashed #f0f2f5;
}
.info-item .k {
  width: 90px;
  color: #6b5a4a;
}
.info-item .v {
  color: #2c1810;
}
.card-title {
  font-weight: 600;
}
.stat {
  position: relative;
  text-align: center;
  padding: 12px 0;
  background: #fafbfc;
  border-radius: 8px;
}
.stat-link {
  cursor: pointer;
  transition: transform .2s, box-shadow .2s, background .2s;
}
.stat-link:hover {
  transform: translateY(-2px);
  background: #eef5ff;
  box-shadow: 0 6px 16px rgba(64,158,255,.15);
}
.stat-go {
  position: absolute;
  right: 8px;
  top: 8px;
  color: #a89880;
  font-size: 14px;
}
.stat-link:hover .stat-go { color: #1685a9; }
.stat .num {
  font-size: 26px;
  font-weight: 700;
  color: #1685a9;
}
.stat .num .sub {
  font-size: 14px;
  color: #a89880;
  font-weight: 400;
}
.stat .lab {
  color: #6b5a4a;
  font-size: 13px;
  margin-top: 6px;
}
.progress-second {
  margin-top: 16px;
}
</style>
